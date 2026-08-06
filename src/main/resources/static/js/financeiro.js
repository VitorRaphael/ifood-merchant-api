'use strict';

/* =========================================================================
 * Financeiro — sincroniza e lista as liquidações (repasses) recebidas do
 * módulo Financial da API do iFood. Mesma filosofia do loja.js/pedidos.js:
 * vanilla puro, fetch() direto nos endpoints, sem framework. Reaproveita
 * buscarJson/mostrarErro/ocultarErro/mostrarSucesso do app.js (globais).
 * ========================================================================= */

const financeiroState = {
    iniciado: false,
};

function formatarMoedaRepasse(valor) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(valor);
}

function renderizarTabelaRepasses(repasses) {
    const container = document.getElementById('financeiro-tabela');
    const vazio = document.getElementById('financeiro-vazio');

    if (!repasses.length) {
        container.classList.add('table-view--hidden');
        vazio.classList.remove('empty-state--hidden');
        return;
    }
    vazio.classList.add('empty-state--hidden');
    container.classList.remove('table-view--hidden');

    const tabela = document.createElement('table');
    const thead = document.createElement('thead');
    thead.innerHTML = '<tr><th>Título</th><th>Tipo</th><th>Status</th><th>Valor</th><th>Data de pagamento</th></tr>';
    tabela.appendChild(thead);

    const tbody = document.createElement('tbody');
    repasses.forEach((repasse) => {
        const tr = document.createElement('tr');

        const tdId = document.createElement('td');
        tdId.textContent = repasse.idTitulo;

        const tdTipo = document.createElement('td');
        tdTipo.textContent = repasse.tipo;

        const tdStatus = document.createElement('td');
        tdStatus.textContent = repasse.status;

        const tdValor = document.createElement('td');
        tdValor.textContent = formatarMoedaRepasse(repasse.valor);

        const tdData = document.createElement('td');
        tdData.textContent = repasse.dataPagamento ?? '—';

        tr.append(tdId, tdTipo, tdStatus, tdValor, tdData);
        tbody.appendChild(tr);
    });
    tabela.appendChild(tbody);

    container.replaceChildren(tabela);
}

async function carregarRepasses() {
    ocultarErro();
    try {
        const repasses = await buscarJson('/api/financeiro/repasses');
        renderizarTabelaRepasses(repasses);
        return true;
    } catch (erro) {
        mostrarErro(erro.message);
        return false;
    }
}

async function sincronizarRepasses(evento) {
    evento.preventDefault();

    const inicio = document.getElementById('financeiro-inicio').value;
    const fim = document.getElementById('financeiro-fim').value;
    if (!inicio || !fim) {
        mostrarErro('Escolha as duas datas antes de sincronizar.');
        return;
    }
    if (inicio > fim) {
        mostrarErro('A data de início precisa ser antes da data de fim.');
        return;
    }

    ocultarErro();
    const botao = document.getElementById('financeiro-sincronizar-btn');
    botao.disabled = true;
    try {
        const resposta = await fetch(`/api/financeiro/sincronizar?inicio=${inicio}&fim=${fim}`, { method: 'POST' });
        if (!resposta.ok) {
            const erroCorpo = await resposta.json().catch(() => null);
            throw new Error(erroCorpo?.mensagem ?? `Erro ${resposta.status} ao sincronizar liquidações`);
        }
        const repasses = await resposta.json();
        renderizarTabelaRepasses(repasses);
        mostrarSucesso('Liquidações sincronizadas com o iFood.');
    } catch (erro) {
        mostrarErro(erro.message);
    } finally {
        botao.disabled = false;
    }
}

/* ---------------------------------------------------------------------
 * Inicialização
 * --------------------------------------------------------------------- */

function iniciarFinanceiro() {
    if (financeiroState.iniciado) {
        return;
    }
    financeiroState.iniciado = true;

    document.getElementById('financeiro-form').addEventListener('submit', sincronizarRepasses);
    document.getElementById('financeiro-atualizar-btn').addEventListener('click', async () => {
        if (await carregarRepasses()) {
            mostrarSucesso('Lista de repasses atualizada.');
        }
    });

    // Uma primeira leitura dos repasses já salvos, pra não começar vazio.
    carregarRepasses();
}

window.Financeiro = { iniciar: iniciarFinanceiro };
