'use strict';

/* =========================================================================
 * Analytics — consulta ao vivo a POST /analytics/v1.0/merchants/{merchantId}/orders/kpis
 * (módulo Analytics oficial do iFood, dados agregados D-1). Mesma filosofia
 * de loja.js/financeiro.js: vanilla puro, fetch() direto no endpoint,
 * reaproveita buscarJson/mostrarErro/ocultarErro/mostrarSucesso/formatarMoeda/
 * formatarNumero/criarStatTile do app.js (globais).
 * ========================================================================= */

const analyticsState = {
    iniciado: false,
};

const ANALYTICS_ROTULOS = {
    canal: { IFOOD: 'iFood', DIGITAL_CATALOG: 'Catálogo digital', POS: 'PDV' },
    status: { CONCLUDED: 'Concluídos', CANCELLED: 'Cancelados' },
    logistica: { IFOOD_DELIVERY: 'Entrega iFood', MERCHANT_DELIVERY: 'Entrega própria', DINE_IN: 'Consumo no local' },
};

function rotularChave(mapa, chave) {
    return mapa[chave] ?? chave;
}

function renderizarKpisAnalytics(kpis) {
    const linha = document.getElementById('analytics-kpi-row');
    linha.innerHTML = '';
    const semComparativo = renderizarDelta(null, null);
    linha.append(
        criarStatTile('GMV total', formatarMoeda(kpis.gmvTotal), semComparativo),
        criarStatTile('GMV sem entrega', formatarMoeda(kpis.gmvSemEntregaTotal), semComparativo),
        criarStatTile('Ticket médio', formatarMoeda(kpis.ticketMedio), semComparativo),
        criarStatTile('Pedidos concluídos', formatarNumero(kpis.pedidosConcluidos), semComparativo),
        criarStatTile('Pedidos cancelados', formatarNumero(kpis.pedidosCancelados), semComparativo)
    );
}

function renderizarDistribuicao(elementoId, distribuicao, mapaRotulos) {
    const container = document.getElementById(elementoId);
    const chaves = Object.keys(distribuicao ?? {});

    if (!chaves.length) {
        container.replaceChildren();
        container.classList.add('table-view--hidden');
        return;
    }
    container.classList.remove('table-view--hidden');

    const tabela = document.createElement('table');
    const thead = document.createElement('thead');
    thead.innerHTML = '<tr><th>Dimensão</th><th>Pedidos</th></tr>';
    tabela.appendChild(thead);

    const tbody = document.createElement('tbody');
    chaves.forEach((chave) => {
        const tr = document.createElement('tr');
        const tdChave = document.createElement('td');
        tdChave.textContent = rotularChave(mapaRotulos ?? {}, chave);
        const tdValor = document.createElement('td');
        tdValor.textContent = formatarNumero(distribuicao[chave]);
        tr.append(tdChave, tdValor);
        tbody.appendChild(tr);
    });
    tabela.appendChild(tbody);

    container.replaceChildren(tabela);
}

function renderizarKpisAnalyticsCompleto(kpis) {
    renderizarKpisAnalytics(kpis);
    renderizarDistribuicao('analytics-canal-tabela', kpis.porCanal, ANALYTICS_ROTULOS.canal);
    renderizarDistribuicao('analytics-status-tabela', kpis.porStatus, ANALYTICS_ROTULOS.status);
    renderizarDistribuicao('analytics-pagamento-tabela', kpis.porPagamento, {});
    renderizarDistribuicao('analytics-logistica-tabela', kpis.porLogistica, ANALYTICS_ROTULOS.logistica);

    const periodoEl = document.getElementById('analytics-periodo-consultado');
    periodoEl.textContent = `Período consultado: ${formatarDataCurta(kpis.periodoInicio)} até ${formatarDataCurta(kpis.periodoFim)} — dados históricos D-1 (não em tempo real).`;
    periodoEl.hidden = false;
}

async function consultarKpisAnalytics(evento) {
    evento.preventDefault();

    const inicio = document.getElementById('analytics-inicio').value;
    const fim = document.getElementById('analytics-fim').value;
    if (!inicio || !fim) {
        mostrarErro('Escolha as duas datas antes de consultar.');
        return;
    }
    if (inicio > fim) {
        mostrarErro('A data de início precisa ser antes da data de fim.');
        return;
    }

    ocultarErro();
    const botao = document.getElementById('analytics-consultar-btn');
    botao.disabled = true;
    try {
        const kpis = await buscarJson(`/api/analytics/kpis?inicio=${inicio}&fim=${fim}`);
        renderizarKpisAnalyticsCompleto(kpis);
        mostrarSucesso('Indicadores atualizados a partir do módulo Analytics do iFood.');
    } catch (erro) {
        mostrarErro(erro.message);
    } finally {
        botao.disabled = false;
    }
}

/* ---------------------------------------------------------------------
 * Inicialização
 * --------------------------------------------------------------------- */

function iniciarAnalytics() {
    if (analyticsState.iniciado) {
        return;
    }
    analyticsState.iniciado = true;

    document.getElementById('analytics-form').addEventListener('submit', consultarKpisAnalytics);
}

window.Analytics = { iniciar: iniciarAnalytics };
