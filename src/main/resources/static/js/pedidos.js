'use strict';

/* =========================================================================
 * Gestor de Pedidos — quadro Kanban do ciclo de vida do pedido.
 * Mesma filosofia do app.js: vanilla puro, fetch() direto nos endpoints,
 * sem framework. Reaproveita buscarJson/mostrarErro/ocultarErro do app.js
 * (funções globais, carregadas antes deste arquivo).
 * ========================================================================= */

const INTERVALO_PEDIDOS_MS = 15000;

// Todo pedido novo já chega confirmado automaticamente pelo backend (evento PLC
// do iFood -> POST /confirm antes até de gravar a venda) — por isso a coluna
// "aceitar" nunca recebe pedido nenhum: não existe, no nosso sistema, um estado
// de "aguardando confirmação". Ela fica no quadro só pra espelhar as 5 fases do
// pedido no iFood; o "aceite" em si já aconteceu no instante em que o pedido cai.
const COLUNA_POR_STATUS = {
    CONFIRMADO: 'preparo',
    PRONTO: 'pronto',
    EM_ROTA: 'rota',
    CONCLUIDO: 'finalizado',
    CANCELADO: 'finalizado',
};

const MENSAGEM_VAZIO_POR_COLUNA = {
    aceitar: 'Pedidos novos são confirmados automaticamente e já aparecem em "Em Preparo".',
    preparo: 'Nenhum pedido em preparo.',
    pronto: 'Nenhum pedido pronto.',
    rota: 'Nenhum pedido em rota.',
    finalizado: 'Nenhum pedido finalizado hoje ainda.',
};

const pedidosState = {
    intervaloId: null,
    carregando: false,
};

function tempoDecorrido(criadoEmIso) {
    if (!criadoEmIso) {
        return '';
    }
    const minutos = Math.max(0, Math.round((Date.now() - new Date(criadoEmIso).getTime()) / 60000));
    if (minutos < 1) {
        return 'agora mesmo';
    }
    if (minutos < 60) {
        return `${minutos} min atrás`;
    }
    const horas = Math.floor(minutos / 60);
    const minutosRestantes = minutos % 60;
    return `${horas}h${minutosRestantes > 0 ? ` ${minutosRestantes}min` : ''} atrás`;
}

function criarCardPedido(pedido) {
    const card = document.createElement('article');
    card.className = 'kanban__card';
    card.dataset.idVenda = pedido.idVenda;

    const cabecalho = document.createElement('div');
    cabecalho.className = 'kanban__card-cabecalho';

    const codigo = document.createElement('span');
    codigo.className = 'kanban__card-codigo';
    codigo.textContent = `#${pedido.idVenda.slice(-6).toUpperCase()}`;
    cabecalho.appendChild(codigo);

    if (pedido.status === 'CONFIRMADO') {
        const selo = document.createElement('span');
        selo.className = 'kanban__card-selo';
        selo.textContent = 'Auto-aceito';
        cabecalho.appendChild(selo);
    }

    card.appendChild(cabecalho);

    const cliente = document.createElement('p');
    cliente.className = 'kanban__card-cliente';
    cliente.textContent = pedido.nomeCliente || 'Cliente não identificado';
    card.appendChild(cliente);

    const rodape = document.createElement('div');
    rodape.className = 'kanban__card-rodape';

    const tempo = document.createElement('span');
    tempo.className = 'kanban__card-tempo';
    tempo.textContent = tempoDecorrido(pedido.criadoEm);
    rodape.appendChild(tempo);

    const valor = document.createElement('span');
    valor.className = 'kanban__card-valor';
    valor.textContent = formatarMoeda(pedido.valorBruto);
    rodape.appendChild(valor);

    card.appendChild(rodape);

    if (pedido.status === 'CANCELADO') {
        card.classList.add('kanban__card--cancelado');
    }

    // Pedido em rota: buscamos o código de entrega (customer.phone.localizer
    // nos detalhes do pedido na iFood) e mostramos direto no card, pra não
    // precisar sair do painel logado pra ir atrás dele em outro lugar.
    if (pedido.status === 'EM_ROTA') {
        const codigoEl = document.createElement('p');
        codigoEl.className = 'kanban__card-codigo-entrega';
        codigoEl.textContent = 'Buscando código de entrega…';
        card.appendChild(codigoEl);

        buscarCodigoEntrega(pedido.idVenda).then((codigo) => {
            card.dataset.codigoEntrega = codigo || '';
            codigoEl.textContent = codigo
                ? `Código de entrega: ${codigo}`
                : 'Código de entrega não encontrado nos detalhes do pedido.';
        });
    }

    const acao = criarBotaoAcao(pedido);
    if (acao) {
        card.appendChild(acao);
    }

    return card;
}

async function buscarCodigoEntrega(idVenda) {
    try {
        const detalhes = await buscarJson(`/api/pedidos/${idVenda}`);
        return detalhes?.customer?.phone?.localizer ?? null;
    } catch (erro) {
        return null;
    }
}

function criarBotaoAcao(pedido) {
    const botao = document.createElement('button');
    botao.type = 'button';
    botao.className = 'button button--primary kanban__card-acao';

    if (pedido.status === 'CONFIRMADO') {
        botao.textContent = 'Marcar como Pronto';
        botao.addEventListener('click', () => executarAcaoPedido(pedido.idVenda, 'pronto', botao));
        return botao;
    }
    if (pedido.status === 'PRONTO') {
        botao.textContent = 'Despachar';
        botao.addEventListener('click', () => executarAcaoPedido(pedido.idVenda, 'despachar', botao));
        return botao;
    }
    if (pedido.status === 'EM_ROTA') {
        botao.textContent = 'Confirmar entrega';
        botao.addEventListener('click', () => confirmarEntregaPedido(pedido.idVenda, botao));
        return botao;
    }
    return null;
}

async function executarAcaoPedido(idVenda, acao, botao) {
    botao.disabled = true;
    ocultarErro();
    try {
        const resposta = await fetch(`/api/pedidos/${idVenda}/${acao}`, { method: 'POST' });
        if (!resposta.ok) {
            const corpo = await resposta.json().catch(() => null);
            throw new Error(corpo?.mensagem ?? `Erro ${resposta.status} ao atualizar o pedido`);
        }
        await carregarQuadroPedidos();
    } catch (erro) {
        mostrarErro(erro.message);
        botao.disabled = false;
    }
}

// Não existe ação de API pra "concluir" um pedido — o CONCLUDED só é gerado
// pela própria iFood ao validar o código de entrega que o cliente passa ao
// entregador (ou por timeout automático, sem ação nenhuma nossa).
async function confirmarEntregaPedido(idVenda, botao) {
    const card = botao.closest('.kanban__card');
    const codigoSugerido = card?.dataset.codigoEntrega || '';
    const codigo = window.prompt('Código de entrega informado pelo cliente:', codigoSugerido);
    if (!codigo) {
        return;
    }
    botao.disabled = true;
    ocultarErro();
    try {
        const resposta = await fetch(`/api/pedidos/${idVenda}/confirmar-entrega`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ codigo }),
        });
        if (!resposta.ok) {
            const corpo = await resposta.json().catch(() => null);
            throw new Error(corpo?.mensagem ?? `Erro ${resposta.status} ao confirmar a entrega`);
        }
        await carregarQuadroPedidos();
    } catch (erro) {
        mostrarErro(erro.message);
        botao.disabled = false;
    }
}

function renderizarQuadro(pedidos) {
    const listasPorColuna = { aceitar: [], preparo: [], pronto: [], rota: [], finalizado: [] };

    pedidos.forEach((pedido) => {
        const coluna = COLUNA_POR_STATUS[pedido.status];
        if (coluna) {
            listasPorColuna[coluna].push(pedido);
        }
    });

    Object.keys(listasPorColuna).forEach((coluna) => {
        const lista = document.querySelector(`[data-lista="${coluna}"]`);
        const contador = document.querySelector(`[data-contador="${coluna}"]`);
        const pedidosDaColuna = listasPorColuna[coluna];

        if (contador) {
            contador.textContent = String(pedidosDaColuna.length);
        }
        if (!lista) {
            return;
        }

        lista.replaceChildren();
        if (pedidosDaColuna.length === 0) {
            const vazio = document.createElement('p');
            vazio.className = 'kanban__vazio';
            vazio.textContent = MENSAGEM_VAZIO_POR_COLUNA[coluna];
            lista.appendChild(vazio);
            return;
        }

        pedidosDaColuna.forEach((pedido) => lista.appendChild(criarCardPedido(pedido)));
    });
}

async function carregarQuadroPedidos() {
    if (pedidosState.carregando) {
        return;
    }
    pedidosState.carregando = true;
    try {
        const pedidos = await buscarJson('/api/pedidos/quadro');
        renderizarQuadro(pedidos);
    } catch (erro) {
        mostrarErro(erro.message);
    } finally {
        pedidosState.carregando = false;
    }
}

function iniciarPedidos() {
    carregarQuadroPedidos();
    if (pedidosState.intervaloId === null) {
        pedidosState.intervaloId = setInterval(carregarQuadroPedidos, INTERVALO_PEDIDOS_MS);
    }
}

window.Pedidos = { iniciar: iniciarPedidos };
