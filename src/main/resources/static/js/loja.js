'use strict';

/* =========================================================================
 * Minha Loja — dados da loja, disponibilidade, pausas e horário de
 * funcionamento. Mesma filosofia do app.js/pedidos.js: vanilla puro,
 * fetch() direto nos endpoints, sem framework. Reaproveita
 * buscarJson/mostrarErro/ocultarErro do app.js (funções globais).
 * ========================================================================= */

const DIAS_SEMANA_OPENING_HOURS = [
    { valor: 'MONDAY', rotulo: 'Segunda-feira' },
    { valor: 'TUESDAY', rotulo: 'Terça-feira' },
    { valor: 'WEDNESDAY', rotulo: 'Quarta-feira' },
    { valor: 'THURSDAY', rotulo: 'Quinta-feira' },
    { valor: 'FRIDAY', rotulo: 'Sexta-feira' },
    { valor: 'SATURDAY', rotulo: 'Sábado' },
    { valor: 'SUNDAY', rotulo: 'Domingo' },
];

const ROTULO_DIA = DIAS_SEMANA_OPENING_HOURS.reduce((mapa, dia) => {
    mapa[dia.valor] = dia.rotulo;
    return mapa;
}, {});

const lojaState = {
    iniciado: false,
    turnoSeq: 0,
};

/* ---------------------------------------------------------------------
 * Renderização genérica de objeto -> lista de definição (chave/valor).
 * A API da iFood tem vários campos aninhados (endereço, operações...);
 * em vez de fixar nomes de campo frágeis, achatamos o objeto inteiro e
 * mostramos tudo. Isso garante que o vídeo sempre exibe o dado real que
 * veio da API, mesmo que o schema exato varie.
 * --------------------------------------------------------------------- */

function achatarObjeto(valor, prefixo, saida) {
    if (valor === null || valor === undefined) {
        saida.push([prefixo || '(vazio)', '—']);
        return;
    }
    if (Array.isArray(valor)) {
        if (valor.length === 0) {
            saida.push([prefixo, '[]']);
            return;
        }
        valor.forEach((item, indice) => {
            achatarObjeto(item, `${prefixo}[${indice}]`, saida);
        });
        return;
    }
    if (typeof valor === 'object') {
        const chaves = Object.keys(valor);
        if (chaves.length === 0) {
            saida.push([prefixo, '{}']);
            return;
        }
        chaves.forEach((chave) => {
            const novoPrefixo = prefixo ? `${prefixo}.${chave}` : chave;
            achatarObjeto(valor[chave], novoPrefixo, saida);
        });
        return;
    }
    saida.push([prefixo, String(valor)]);
}

function renderizarChaves(container, elementoVazio, objeto) {
    container.replaceChildren();
    const pares = [];
    achatarObjeto(objeto, '', pares);

    if (pares.length === 0) {
        elementoVazio.classList.remove('empty-state--hidden');
        return;
    }
    elementoVazio.classList.add('empty-state--hidden');

    pares.forEach(([chave, valor]) => {
        const dt = document.createElement('dt');
        dt.textContent = chave;
        const dd = document.createElement('dd');
        dd.textContent = valor;
        container.appendChild(dt);
        container.appendChild(dd);
    });
}

/* ---------------------------------------------------------------------
 * Lojas vinculadas + detalhes
 * --------------------------------------------------------------------- */

async function carregarLojas() {
    const lista = document.getElementById('loja-lista');
    const vazio = document.getElementById('loja-lista-vazio');
    ocultarErro();
    try {
        const lojas = await buscarJson('/api/loja/lojas');
        const itens = Array.isArray(lojas) ? lojas : (lojas.merchants || lojas.data || []);
        lista.replaceChildren();

        if (!itens.length) {
            vazio.classList.remove('empty-state--hidden');
            return;
        }
        vazio.classList.add('empty-state--hidden');

        itens.forEach((loja) => {
            const item = document.createElement('article');
            item.className = 'loja-lista__item';

            const nome = document.createElement('span');
            nome.className = 'loja-lista__nome';
            nome.textContent = loja.name || loja.corporateName || loja.id || 'Loja';
            item.appendChild(nome);

            const id = document.createElement('span');
            id.className = 'loja-lista__id';
            id.textContent = loja.id || '';
            item.appendChild(id);

            const botao = document.createElement('button');
            botao.type = 'button';
            botao.className = 'ghost-button';
            botao.textContent = 'Ver detalhes';
            botao.addEventListener('click', () => carregarDetalhes(loja.id));
            item.appendChild(botao);

            lista.appendChild(item);
        });
    } catch (erro) {
        mostrarErro(erro.message);
    }
}

async function carregarDetalhes(idLoja) {
    const container = document.getElementById('loja-detalhes');
    const vazio = document.getElementById('loja-detalhes-vazio');
    ocultarErro();
    try {
        const detalhes = await buscarJson(`/api/loja/${encodeURIComponent(idLoja || '')}`);
        renderizarChaves(container, vazio, detalhes);
    } catch (erro) {
        mostrarErro(erro.message);
    }
}

/* ---------------------------------------------------------------------
 * Disponibilidade (status)
 * --------------------------------------------------------------------- */

function classePillPorEstado(estado) {
    if (estado === 'OK') return 'loja-status-pill--ok';
    if (estado === 'WARNING') return 'loja-status-pill--alerta';
    return 'loja-status-pill--erro';
}

async function consultarStatus() {
    const pill = document.getElementById('loja-status-pill');
    const container = document.getElementById('loja-status-detalhes');
    const vazio = document.getElementById('loja-status-vazio');
    ocultarErro();
    try {
        const status = await buscarJson('/api/loja/status');
        const item = Array.isArray(status) ? status[0] : status;
        const estado = item && item.state ? item.state : 'DESCONHECIDO';

        pill.textContent = estado;
        pill.className = `loja-status-pill ${classePillPorEstado(estado)}`;

        renderizarChaves(container, vazio, status);
    } catch (erro) {
        mostrarErro(erro.message);
    }
}

/* ---------------------------------------------------------------------
 * Pausas (Interrupções)
 * --------------------------------------------------------------------- */

function criarItemPausa(pausa) {
    const item = document.createElement('article');
    item.className = 'pausas-lista__item';

    const descricao = document.createElement('span');
    descricao.className = 'pausas-lista__descricao';
    descricao.textContent = pausa.description || 'Pausa';
    item.appendChild(descricao);

    const periodo = document.createElement('span');
    periodo.className = 'pausas-lista__periodo';
    periodo.textContent = `${pausa.start || '?'} → ${pausa.end || '?'}`;
    item.appendChild(periodo);

    const botao = document.createElement('button');
    botao.type = 'button';
    botao.className = 'ghost-button ghost-button--perigo';
    botao.textContent = 'Remover';
    botao.addEventListener('click', () => removerPausa(pausa.id, botao));
    item.appendChild(botao);

    return item;
}

async function carregarPausas() {
    const lista = document.getElementById('pausas-lista');
    const vazio = document.getElementById('pausas-lista-vazio');
    ocultarErro();
    try {
        const pausas = await buscarJson('/api/loja/pausas');
        const itens = Array.isArray(pausas) ? pausas : (pausas.interruptions || pausas.data || []);
        lista.replaceChildren();

        if (!itens.length) {
            vazio.classList.remove('empty-state--hidden');
            return true;
        }
        vazio.classList.add('empty-state--hidden');
        itens.forEach((pausa) => lista.appendChild(criarItemPausa(pausa)));
        return true;
    } catch (erro) {
        mostrarErro(erro.message);
        return false;
    }
}

async function criarPausa(evento) {
    evento.preventDefault();
    const descricao = document.getElementById('pausa-descricao');
    const inicio = document.getElementById('pausa-inicio');
    const fim = document.getElementById('pausa-fim');
    const botao = document.getElementById('pausa-criar-btn');

    ocultarErro();
    botao.disabled = true;
    try {
        const corpo = {
            description: descricao.value,
            start: new Date(inicio.value).toISOString(),
            end: new Date(fim.value).toISOString(),
        };
        const resposta = await fetch('/api/loja/pausas', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(corpo),
        });
        if (!resposta.ok) {
            const erroCorpo = await resposta.json().catch(() => null);
            throw new Error(erroCorpo?.mensagem ?? `Erro ${resposta.status} ao criar a pausa`);
        }

        // Não reconsultamos a lista aqui de propósito (mesmo motivo dos
        // horários: a iFood pode levar um instante pra propagar a escrita).
        // A própria resposta do POST já confirma a pausa criada — inclusive
        // com o id de verdade, que é o que o botão "Remover" precisa.
        const pausaCriada = await resposta.json().catch(() => corpo);
        document.getElementById('pausas-lista-vazio').classList.add('empty-state--hidden');
        document.getElementById('pausas-lista').appendChild(criarItemPausa(pausaCriada));

        descricao.value = '';
        inicio.value = '';
        fim.value = '';
        mostrarSucesso('Pausa criada.');
    } catch (erro) {
        mostrarErro(erro.message);
    } finally {
        botao.disabled = false;
    }
}

async function removerPausa(idPausa, botao) {
    ocultarErro();
    botao.disabled = true;
    try {
        const resposta = await fetch(`/api/loja/pausas/${encodeURIComponent(idPausa)}`, { method: 'DELETE' });
        if (!resposta.ok) {
            const erroCorpo = await resposta.json().catch(() => null);
            throw new Error(erroCorpo?.mensagem ?? `Erro ${resposta.status} ao remover a pausa`);
        }

        // Mesma lógica: tiramos o card da tela direto, sem reconsultar a
        // iFood (que pode devolver a pausa removida por mais alguns segundos).
        const lista = document.getElementById('pausas-lista');
        botao.closest('.pausas-lista__item')?.remove();
        if (!lista.children.length) {
            document.getElementById('pausas-lista-vazio').classList.remove('empty-state--hidden');
        }
        mostrarSucesso('Pausa removida.');
    } catch (erro) {
        mostrarErro(erro.message);
        botao.disabled = false;
    }
}

/* ---------------------------------------------------------------------
 * Horário de funcionamento
 * --------------------------------------------------------------------- */

function criarLinhaTurno(dia, inicio, fim) {
    lojaState.turnoSeq += 1;
    const idLinha = `turno-${lojaState.turnoSeq}`;

    const linha = document.createElement('div');
    linha.className = 'horarios-turnos__linha';
    linha.dataset.linha = idLinha;

    const selectDia = document.createElement('select');
    selectDia.className = 'field__input';
    DIAS_SEMANA_OPENING_HOURS.forEach((d) => {
        const opcao = document.createElement('option');
        opcao.value = d.valor;
        opcao.textContent = d.rotulo;
        if (d.valor === dia) opcao.selected = true;
        selectDia.appendChild(opcao);
    });
    linha.appendChild(selectDia);

    const inputInicio = document.createElement('input');
    inputInicio.type = 'time';
    inputInicio.className = 'field__input';
    inputInicio.value = inicio || '10:00';
    linha.appendChild(inputInicio);

    const separador = document.createElement('span');
    separador.textContent = 'até';
    separador.className = 'horarios-turnos__ate';
    linha.appendChild(separador);

    const inputFim = document.createElement('input');
    inputFim.type = 'time';
    inputFim.className = 'field__input';
    inputFim.value = fim || '19:00';
    linha.appendChild(inputFim);

    const botaoRemover = document.createElement('button');
    botaoRemover.type = 'button';
    botaoRemover.className = 'ghost-button ghost-button--perigo';
    botaoRemover.textContent = 'Remover';
    botaoRemover.addEventListener('click', () => linha.remove());
    linha.appendChild(botaoRemover);

    return linha;
}

function somarMinutos(horaHHmm, minutosAdicionais) {
    const [h, m] = horaHHmm.split(':').map(Number);
    const total = (h * 60 + m + minutosAdicionais) % (24 * 60);
    const horaFinal = Math.floor(total / 60);
    const minutoFinal = total % 60;
    return `${String(horaFinal).padStart(2, '0')}:${String(minutoFinal).padStart(2, '0')}`;
}

function adicionarLinhaTurno() {
    const container = document.getElementById('horarios-turnos');
    container.appendChild(criarLinhaTurno());
}

function duracaoEmMinutos(inicioHHmm, fimHHmm) {
    const [hi, mi] = inicioHHmm.split(':').map(Number);
    const [hf, mf] = fimHHmm.split(':').map(Number);
    let minutos = (hf * 60 + mf) - (hi * 60 + mi);
    if (minutos <= 0) {
        minutos += 24 * 60; // turno que atravessa a meia-noite
    }
    return minutos;
}

function renderizarTabelaHorarios(turnos) {
    const container = document.getElementById('horarios-tabela');
    const vazio = document.getElementById('horarios-vazio');

    if (!turnos.length) {
        container.classList.add('table-view--hidden');
        vazio.classList.remove('empty-state--hidden');
        return;
    }
    vazio.classList.add('empty-state--hidden');
    container.classList.remove('table-view--hidden');

    const tabela = document.createElement('table');
    const thead = document.createElement('thead');
    thead.innerHTML = '<tr><th>Dia</th><th>Início</th><th>Duração (min)</th></tr>';
    tabela.appendChild(thead);

    const tbody = document.createElement('tbody');
    turnos.forEach((turno) => {
        const tr = document.createElement('tr');
        const tdDia = document.createElement('td');
        tdDia.textContent = ROTULO_DIA[turno.dayOfWeek] || turno.dayOfWeek;
        const tdInicio = document.createElement('td');
        tdInicio.textContent = turno.start;
        const tdDuracao = document.createElement('td');
        tdDuracao.textContent = turno.duration;
        tr.appendChild(tdDia);
        tr.appendChild(tdInicio);
        tr.appendChild(tdDuracao);
        tbody.appendChild(tr);
    });
    tabela.appendChild(tbody);

    container.replaceChildren(tabela);
}

async function carregarHorarios() {
    ocultarErro();
    try {
        const horarios = await buscarJson('/api/loja/horarios');
        const turnos = Array.isArray(horarios) ? horarios : (horarios.shifts || horarios.data || []);
        renderizarTabelaHorarios(turnos);

        const container = document.getElementById('horarios-turnos');
        container.replaceChildren();
        if (turnos.length) {
            turnos.forEach((turno) => {
                const inicio = turno.start ? turno.start.slice(0, 5) : '10:00';
                const fim = turno.duration ? somarMinutos(inicio, turno.duration) : '19:00';
                container.appendChild(criarLinhaTurno(turno.dayOfWeek, inicio, fim));
            });
        }
        return true;
    } catch (erro) {
        mostrarErro(erro.message);
        return false;
    }
}

async function salvarHorarios() {
    const botao = document.getElementById('horarios-salvar-btn');
    const linhas = document.querySelectorAll('#horarios-turnos .horarios-turnos__linha');

    if (!linhas.length) {
        mostrarErro('Adicione ao menos um turno antes de salvar.');
        return;
    }

    const shifts = Array.from(linhas).map((linha) => {
        const selects = linha.querySelectorAll('select');
        const inputs = linha.querySelectorAll('input[type="time"]');
        const dayOfWeek = selects[0].value;
        const inicio = inputs[0].value;
        const fim = inputs[1].value;
        return {
            dayOfWeek,
            start: `${inicio}:00`,
            duration: duracaoEmMinutos(inicio, fim),
        };
    });

    ocultarErro();
    botao.disabled = true;
    try {
        const resposta = await fetch('/api/loja/horarios', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ shifts }),
        });
        if (!resposta.ok) {
            const erroCorpo = await resposta.json().catch(() => null);
            throw new Error(erroCorpo?.mensagem ?? `Erro ${resposta.status} ao salvar os horários`);
        }
        // Não chamamos carregarHorarios() aqui de propósito: um GET imediato
        // depois do PUT às vezes ainda devolve o valor antigo (consistência
        // eventual do lado da iFood) e sobrescrevia a tela com dado
        // desatualizado. Já sabemos exatamente o que acabou de ser salvo —
        // é só renderizar isso direto, sem depender de reconsultar a API.
        renderizarTabelaHorarios(shifts);
        mostrarSucesso('Horários salvos.');
    } catch (erro) {
        mostrarErro(erro.message);
    } finally {
        botao.disabled = false;
    }
}

/* ---------------------------------------------------------------------
 * Inicialização
 * --------------------------------------------------------------------- */

function iniciarLoja() {
    if (lojaState.iniciado) {
        return;
    }
    lojaState.iniciado = true;

    document.getElementById('loja-listar-btn').addEventListener('click', carregarLojas);
    document.getElementById('loja-status-btn').addEventListener('click', consultarStatus);
    document.getElementById('pausas-listar-btn').addEventListener('click', async () => {
        if (await carregarPausas()) {
            mostrarSucesso('Lista de pausas atualizada.');
        }
    });
    document.getElementById('pausa-form').addEventListener('submit', criarPausa);
    document.getElementById('horarios-listar-btn').addEventListener('click', async () => {
        if (!(await carregarHorarios())) {
            return;
        }
        mostrarSucesso('Horários atualizados.');
    });
    document.getElementById('horario-add-turno-btn').addEventListener('click', adicionarLinhaTurno);
    document.getElementById('horarios-salvar-btn').addEventListener('click', salvarHorarios);

    // Uma primeira leitura de pausas ao entrar na tela, pra não começar vazio.
    carregarPausas();
}

window.Loja = { iniciar: iniciarLoja };
