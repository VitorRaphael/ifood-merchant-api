'use strict';

/* =========================================================================
 * Painel da Loja — dashboard estático servido pela própria API Spring Boot.
 * Sem framework, sem build step: fetch() direto nos endpoints já existentes.
 * ========================================================================= */

const DIAS_SEMANA = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
const INTERVALO_ATUALIZACAO_MS = 30000;

const state = {
    inicio: null,
    fim: null,
    carregando: false,
};

/* ---------------------------------------------------------------------
 * Utilidades de data — sempre tratamos "yyyy-MM-dd" como data de calendário
 * pura (sem hora), nunca reinterpretada em UTC, pra não deslocar o dia
 * dependendo do fuso do navegador de quem está olhando o painel.
 * --------------------------------------------------------------------- */

function paraDataLocal(isoData) {
    const [ano, mes, dia] = isoData.split('-').map(Number);
    return new Date(ano, mes - 1, dia);
}

function paraIso(data) {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
}

function adicionarDias(data, dias) {
    const copia = new Date(data);
    copia.setDate(copia.getDate() + dias);
    return copia;
}

function formatarDataCurta(isoData) {
    const data = paraDataLocal(isoData);
    return `${String(data.getDate()).padStart(2, '0')}/${MESES[data.getMonth()]}`;
}

function formatarDataCompleta(isoData) {
    const data = paraDataLocal(isoData);
    return `${String(data.getDate()).padStart(2, '0')}/${String(data.getMonth() + 1).padStart(2, '0')}/${data.getFullYear()}`;
}

function formatarMoeda(valor) {
    return (valor ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
}

function formatarNumero(valor) {
    return (valor ?? 0).toLocaleString('pt-BR');
}

/* ---------------------------------------------------------------------
 * Presets de período
 * --------------------------------------------------------------------- */

function calcularPeriodoPreset(preset) {
    const hoje = new Date();
    hoje.setHours(0, 0, 0, 0);

    switch (preset) {
        case 'hoje':
            return { inicio: hoje, fim: hoje };
        case '7d':
            return { inicio: adicionarDias(hoje, -6), fim: hoje };
        case '30d':
            return { inicio: adicionarDias(hoje, -29), fim: hoje };
        case 'mes':
            return { inicio: new Date(hoje.getFullYear(), hoje.getMonth(), 1), fim: hoje };
        default:
            return { inicio: adicionarDias(hoje, -6), fim: hoje };
    }
}

function calcularPeriodoAnterior(inicioIso, fimIso) {
    const inicio = paraDataLocal(inicioIso);
    const fim = paraDataLocal(fimIso);
    const duracaoDias = Math.round((fim - inicio) / 86400000) + 1;

    const fimAnterior = adicionarDias(inicio, -1);
    const inicioAnterior = adicionarDias(fimAnterior, -(duracaoDias - 1));

    return { inicio: paraIso(inicioAnterior), fim: paraIso(fimAnterior) };
}

/* ---------------------------------------------------------------------
 * Acesso à API — nunca deixa uma falha de rede travar o resto do painel.
 * --------------------------------------------------------------------- */

async function buscarJson(url) {
    const resposta = await fetch(url);
    if (!resposta.ok) {
        const corpo = await resposta.json().catch(() => null);
        const mensagem = corpo?.mensagem ?? `Erro ${resposta.status} ao consultar ${url}`;
        throw new Error(mensagem);
    }
    return resposta.json();
}

function mostrarErro(mensagem) {
    const banner = document.getElementById('status-banner');
    banner.textContent = mensagem;
    banner.classList.remove('status-banner--hidden');
}

function ocultarErro() {
    const banner = document.getElementById('status-banner');
    banner.classList.add('status-banner--hidden');
}

/* ---------------------------------------------------------------------
 * Tooltip compartilhado por todos os gráficos SVG
 * --------------------------------------------------------------------- */

const tooltip = {
    el: document.getElementById('chart-tooltip'),
    mostrar(evento, html) {
        this.el.innerHTML = '';
        const linha = document.createElement('div');
        linha.innerHTML = html; // conteúdo montado por nós mesmos, nunca dado bruto de fora
        this.el.appendChild(linha);
        this.el.hidden = false;
        this.posicionar(evento);
    },
    posicionar(evento) {
        const x = evento.clientX ?? 0;
        const y = evento.clientY ?? 0;
        this.el.style.left = `${x + 14}px`;
        this.el.style.top = `${y + 14}px`;
    },
    ocultar() {
        this.el.hidden = true;
    },
};

function textoSeguro(valor) {
    const span = document.createElement('span');
    span.textContent = valor;
    return span.innerHTML;
}

/* ---------------------------------------------------------------------
 * Cor da série — lida do CSS, então já respeita modo claro/escuro sozinha.
 * --------------------------------------------------------------------- */

function corSerieRGB() {
    const hex = getComputedStyle(document.documentElement).getPropertyValue('--series-1').trim();
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return { r, g, b };
}

function criarSvg(largura, altura) {
    const ns = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(ns, 'svg');
    svg.setAttribute('viewBox', `0 0 ${largura} ${altura}`);
    svg.setAttribute('role', 'img');
    return svg;
}

function elemento(tag, atributos) {
    const ns = 'http://www.w3.org/2000/svg';
    const el = document.createElementNS(ns, tag);
    for (const [chave, valor] of Object.entries(atributos)) {
        el.setAttribute(chave, valor);
    }
    return el;
}

/* =========================================================================
 * KPIs (stat tiles)
 * ========================================================================= */

function calcularTicketMedio(resumo) {
    return resumo.totalVendas > 0 ? resumo.valorBrutoTotal / resumo.totalVendas : 0;
}

function calcularDelta(atual, anterior) {
    if (!anterior) {
        return null;
    }
    return anterior === 0 ? null : ((atual - anterior) / anterior) * 100;
}

function renderizarDelta(deltaPercentual, maiorEhMelhor) {
    if (deltaPercentual === null || Number.isNaN(deltaPercentual)) {
        return { texto: 'sem período anterior', classe: '' };
    }
    const arredondado = Math.round(deltaPercentual * 10) / 10;
    const subiu = arredondado > 0;
    const seta = subiu ? '↑' : arredondado < 0 ? '↓' : '→';
    const texto = `${seta} ${Math.abs(arredondado).toLocaleString('pt-BR')}% vs período anterior`;

    if (maiorEhMelhor === null || arredondado === 0) {
        return { texto, classe: '' };
    }
    const foiBom = maiorEhMelhor ? subiu : !subiu;
    return { texto, classe: foiBom ? 'stat-tile__delta--up' : 'stat-tile__delta--down' };
}

function criarStatTile(rotulo, valorFormatado, deltaInfo) {
    const tile = document.createElement('div');
    tile.className = 'stat-tile';

    const rotuloEl = document.createElement('div');
    rotuloEl.className = 'stat-tile__label';
    rotuloEl.textContent = rotulo;

    const valorEl = document.createElement('div');
    valorEl.className = 'stat-tile__value';
    valorEl.textContent = valorFormatado;

    const deltaEl = document.createElement('div');
    deltaEl.className = `stat-tile__delta ${deltaInfo.classe}`.trim();
    deltaEl.textContent = deltaInfo.texto;

    tile.append(rotuloEl, valorEl, deltaEl);
    return tile;
}

function renderizarKPIs(resumoAtual, resumoAnterior) {
    const linha = document.getElementById('kpi-row');
    linha.innerHTML = '';

    const ticketAtual = calcularTicketMedio(resumoAtual);
    const ticketAnterior = resumoAnterior ? calcularTicketMedio(resumoAnterior) : null;

    linha.append(
        criarStatTile(
            'Total vendido',
            formatarMoeda(resumoAtual.valorBrutoTotal),
            renderizarDelta(calcularDelta(resumoAtual.valorBrutoTotal, resumoAnterior?.valorBrutoTotal ?? null), true)
        ),
        criarStatTile(
            'Pedidos',
            formatarNumero(resumoAtual.totalVendas),
            renderizarDelta(calcularDelta(resumoAtual.totalVendas, resumoAnterior?.totalVendas ?? null), true)
        ),
        criarStatTile(
            'Ticket médio',
            formatarMoeda(ticketAtual),
            renderizarDelta(calcularDelta(ticketAtual, ticketAnterior), true)
        ),
        criarStatTile(
            'Comissão iFood',
            formatarMoeda(resumoAtual.comissaoTotal),
            renderizarDelta(calcularDelta(resumoAtual.comissaoTotal, resumoAnterior?.comissaoTotal ?? null), null)
        )
    );
}

/* =========================================================================
 * Gráfico de linha — vendas por dia
 * ========================================================================= */

function agruparVendasPorDia(vendas, inicioIso, fimIso) {
    const totais = new Map();
    let cursor = paraDataLocal(inicioIso);
    const fim = paraDataLocal(fimIso);
    while (cursor <= fim) {
        totais.set(paraIso(cursor), 0);
        cursor = adicionarDias(cursor, 1);
    }
    for (const venda of vendas) {
        totais.set(venda.dataVenda, (totais.get(venda.dataVenda) ?? 0) + venda.valorBruto);
    }
    return Array.from(totais.entries()).map(([data, total]) => ({ data, total }));
}

function renderizarGraficoVendasPorDia(pontos) {
    const container = document.getElementById('vendas-por-dia-chart');
    const vazio = document.getElementById('vendas-por-dia-empty');
    container.innerHTML = '';

    const temVenda = pontos.some((p) => p.total > 0);
    vazio.classList.toggle('empty-state--hidden', temVenda || pontos.length === 0);
    if (!temVenda) {
        renderizarTabelaVendasPorDia(pontos);
        return;
    }

    const largura = 720;
    const altura = 220;
    const margem = { topo: 12, baixo: 28, esquerda: 8, direita: 8 };
    const areaLargura = largura - margem.esquerda - margem.direita;
    const areaAltura = altura - margem.topo - margem.baixo;

    const maxValor = Math.max(...pontos.map((p) => p.total), 1);
    const passoX = pontos.length > 1 ? areaLargura / (pontos.length - 1) : 0;

    const coordenadas = pontos.map((ponto, indice) => ({
        x: margem.esquerda + indice * passoX,
        y: margem.topo + areaAltura - (ponto.total / maxValor) * areaAltura,
        ponto,
    }));

    const svg = criarSvg(largura, altura);

    // baseline
    svg.appendChild(elemento('line', {
        x1: margem.esquerda, x2: largura - margem.direita,
        y1: margem.topo + areaAltura, y2: margem.topo + areaAltura,
        class: 'svg-baseline',
    }));

    // área (wash ~10%)
    const linhaArea = ['M', coordenadas[0].x, altura - margem.baixo]
        .concat(coordenadas.flatMap((c) => ['L', c.x, c.y]))
        .concat(['L', coordenadas[coordenadas.length - 1].x, altura - margem.baixo, 'Z'])
        .join(' ');
    svg.appendChild(elemento('path', { d: linhaArea, fill: 'var(--series-1)', 'fill-opacity': '0.1', stroke: 'none' }));

    // linha
    const linha = coordenadas.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x} ${c.y}`).join(' ');
    svg.appendChild(elemento('path', {
        d: linha, fill: 'none', stroke: 'var(--series-1)', 'stroke-width': '2',
        'stroke-linejoin': 'round', 'stroke-linecap': 'round',
    }));

    // rótulos do eixo X — só alguns, pra não empilhar
    const passoRotulo = Math.max(1, Math.ceil(coordenadas.length / 6));
    coordenadas.forEach((c, i) => {
        if (i % passoRotulo === 0 || i === coordenadas.length - 1) {
            const texto = elemento('text', {
                x: c.x, y: altura - 8, class: 'svg-axis-label', 'text-anchor': 'middle',
            });
            texto.textContent = formatarDataCurta(c.ponto.data);
            svg.appendChild(texto);
        }
    });

    // pontos + área de hover/foco
    coordenadas.forEach((c) => {
        svg.appendChild(elemento('circle', {
            cx: c.x, cy: c.y, r: 4, fill: 'var(--series-1)', stroke: 'var(--surface-1)', 'stroke-width': 2,
        }));
        const alvo = elemento('circle', {
            cx: c.x, cy: c.y, r: 12, fill: 'transparent', class: 'svg-mark', tabindex: '0',
            role: 'img', 'aria-label': `${formatarDataCompleta(c.ponto.data)}: ${formatarMoeda(c.ponto.total)}`,
        });
        const mostrar = (evento) => tooltip.mostrar(evento, `<strong>${formatarMoeda(c.ponto.total)}</strong><br>${textoSeguro(formatarDataCompleta(c.ponto.data))}`);
        alvo.addEventListener('pointermove', mostrar);
        alvo.addEventListener('pointerenter', mostrar);
        alvo.addEventListener('focus', mostrar);
        alvo.addEventListener('pointerleave', () => tooltip.ocultar());
        alvo.addEventListener('blur', () => tooltip.ocultar());
        svg.appendChild(alvo);
    });

    container.appendChild(svg);
    renderizarTabelaVendasPorDia(pontos);
}

function renderizarTabelaVendasPorDia(pontos) {
    const container = document.getElementById('vendas-por-dia-table');
    container.innerHTML = '';
    const tabela = document.createElement('table');
    tabela.innerHTML = '<thead><tr><th>Data</th><th>Total vendido</th></tr></thead>';
    const corpo = document.createElement('tbody');
    for (const ponto of pontos) {
        const linha = document.createElement('tr');
        const dataCel = document.createElement('td');
        dataCel.textContent = formatarDataCompleta(ponto.data);
        const valorCel = document.createElement('td');
        valorCel.textContent = formatarMoeda(ponto.total);
        linha.append(dataCel, valorCel);
        corpo.appendChild(linha);
    }
    tabela.appendChild(corpo);
    container.appendChild(tabela);
}

/* =========================================================================
 * Ranking de produtos (barra horizontal)
 * ========================================================================= */

function renderizarRanking(ranking) {
    const container = document.getElementById('ranking-chart');
    const vazio = document.getElementById('ranking-empty');
    container.innerHTML = '';

    vazio.classList.toggle('empty-state--hidden', ranking.length > 0);
    renderizarTabelaRanking(ranking);
    if (ranking.length === 0) {
        return;
    }

    const topN = ranking.slice(0, 8);
    const largura = 720;
    const alturaLinha = 32;
    const altura = topN.length * alturaLinha + 16;
    const rotuloLargura = 190;
    const areaLargura = largura - rotuloLargura - 60;
    const maxQuantidade = Math.max(...topN.map((p) => p.quantidadeTotal), 1);

    const svg = criarSvg(largura, altura);

    topN.forEach((produto, indice) => {
        const y = 8 + indice * alturaLinha;
        const larguraBarra = Math.max(2, (produto.quantidadeTotal / maxQuantidade) * areaLargura);

        const rotulo = elemento('text', {
            x: rotuloLargura - 10, y: y + 16, class: 'svg-axis-label', 'text-anchor': 'end',
        });
        rotulo.textContent = produto.nome.length > 28 ? `${produto.nome.slice(0, 27)}…` : produto.nome;
        const tituloRotulo = elemento('title', {});
        tituloRotulo.textContent = produto.nome;
        rotulo.appendChild(tituloRotulo);
        svg.appendChild(rotulo);

        const grupo = elemento('g', {
            class: 'svg-mark', tabindex: '0', role: 'img',
            'aria-label': `${produto.nome}: ${produto.quantidadeTotal} unidades, ${formatarMoeda(produto.valorTotal)}`,
        });
        grupo.appendChild(elemento('rect', {
            x: rotuloLargura, y, width: larguraBarra, height: 20, rx: 4,
            fill: 'var(--series-1)',
        }));
        const valorLabel = elemento('text', {
            x: rotuloLargura + larguraBarra + 8, y: y + 15, class: 'svg-axis-label',
        });
        valorLabel.textContent = formatarNumero(produto.quantidadeTotal);
        svg.appendChild(grupo);
        svg.appendChild(valorLabel);

        const mostrar = (evento) => tooltip.mostrar(
            evento,
            `<strong>${textoSeguro(produto.nome)}</strong><br>${produto.quantidadeTotal} unidades — ${formatarMoeda(produto.valorTotal)}`
        );
        grupo.addEventListener('pointermove', mostrar);
        grupo.addEventListener('pointerenter', mostrar);
        grupo.addEventListener('focus', mostrar);
        grupo.addEventListener('pointerleave', () => tooltip.ocultar());
        grupo.addEventListener('blur', () => tooltip.ocultar());
    });

    container.appendChild(svg);
}

function renderizarTabelaRanking(ranking) {
    const container = document.getElementById('ranking-table');
    container.innerHTML = '';
    const tabela = document.createElement('table');
    tabela.innerHTML = '<thead><tr><th>Produto</th><th>Quantidade</th><th>Valor total</th></tr></thead>';
    const corpo = document.createElement('tbody');
    for (const produto of ranking) {
        const linha = document.createElement('tr');
        const nomeCel = document.createElement('td');
        nomeCel.textContent = produto.nome;
        const qtdCel = document.createElement('td');
        qtdCel.textContent = formatarNumero(produto.quantidadeTotal);
        const valorCel = document.createElement('td');
        valorCel.textContent = formatarMoeda(produto.valorTotal);
        linha.append(nomeCel, qtdCel, valorCel);
        corpo.appendChild(linha);
    }
    tabela.appendChild(corpo);
    container.appendChild(tabela);
}

/* =========================================================================
 * Mapa de calor — horário de pico (dia da semana × hora)
 * ========================================================================= */

function agruparPorDiaEHora(vendas) {
    const contagem = new Map(); // chave "diaSemana-hora" -> quantidade
    for (const venda of vendas) {
        // vendas gravadas antes do campo horaVenda existir nao entram no mapa de calor
        if (venda.horaVenda === null || venda.horaVenda === undefined) {
            continue;
        }
        const diaSemana = paraDataLocal(venda.dataVenda).getDay();
        const chave = `${diaSemana}-${venda.horaVenda}`;
        contagem.set(chave, (contagem.get(chave) ?? 0) + 1);
    }
    return contagem;
}

function renderizarHorarioPico(vendas) {
    const container = document.getElementById('horario-pico-chart');
    const vazio = document.getElementById('horario-pico-empty');
    container.innerHTML = '';

    vazio.classList.toggle('empty-state--hidden', vendas.length > 0);
    renderizarTabelaHorarioPico(vendas);
    if (vendas.length === 0) {
        return;
    }

    const contagem = agruparPorDiaEHora(vendas);
    const maxContagem = Math.max(...Array.from(contagem.values()), 1);
    const { r, g, b } = corSerieRGB();

    const celula = 22;
    const gap = 2;
    const rotuloLargura = 34;
    const rotuloAltura = 16;
    const largura = rotuloLargura + 24 * (celula + gap);
    const altura = rotuloAltura + 7 * (celula + gap);

    const svg = criarSvg(largura, altura);

    // rótulos de hora (a cada 3h, pra não poluir)
    for (let hora = 0; hora < 24; hora += 3) {
        const texto = elemento('text', {
            x: rotuloLargura + hora * (celula + gap) + celula / 2,
            y: rotuloAltura - 4, class: 'svg-axis-label', 'text-anchor': 'middle',
        });
        texto.textContent = `${hora}h`;
        svg.appendChild(texto);
    }

    for (let dia = 0; dia < 7; dia++) {
        const yDia = rotuloAltura + dia * (celula + gap);
        const rotuloDia = elemento('text', {
            x: rotuloLargura - 8, y: yDia + celula / 2 + 4, class: 'svg-axis-label', 'text-anchor': 'end',
        });
        rotuloDia.textContent = DIAS_SEMANA[dia];
        svg.appendChild(rotuloDia);

        for (let hora = 0; hora < 24; hora++) {
            const quantidade = contagem.get(`${dia}-${hora}`) ?? 0;
            const alpha = quantidade === 0 ? 0.06 : 0.15 + (quantidade / maxContagem) * 0.85;
            const x = rotuloLargura + hora * (celula + gap);

            const rect = elemento('rect', {
                x, y: yDia, width: celula, height: celula, rx: 3,
                fill: `rgba(${r}, ${g}, ${b}, ${alpha.toFixed(2)})`,
                class: 'svg-mark', tabindex: '0', role: 'img',
                'aria-label': `${DIAS_SEMANA[dia]} às ${hora}h: ${quantidade} pedido(s)`,
            });
            const mostrar = (evento) => tooltip.mostrar(
                evento,
                `<strong>${quantidade} pedido(s)</strong><br>${DIAS_SEMANA[dia]} às ${hora}h`
            );
            rect.addEventListener('pointermove', mostrar);
            rect.addEventListener('pointerenter', mostrar);
            rect.addEventListener('focus', mostrar);
            rect.addEventListener('pointerleave', () => tooltip.ocultar());
            rect.addEventListener('blur', () => tooltip.ocultar());
            svg.appendChild(rect);
        }
    }

    container.appendChild(svg);
}

function renderizarTabelaHorarioPico(vendas) {
    const container = document.getElementById('horario-pico-table');
    container.innerHTML = '';
    const contagem = agruparPorDiaEHora(vendas);
    const linhas = Array.from(contagem.entries())
        .map(([chave, quantidade]) => {
            const [dia, hora] = chave.split('-').map(Number);
            return { dia, hora, quantidade };
        })
        .sort((a, b) => b.quantidade - a.quantidade);

    const tabela = document.createElement('table');
    tabela.innerHTML = '<thead><tr><th>Dia da semana</th><th>Hora</th><th>Pedidos</th></tr></thead>';
    const corpo = document.createElement('tbody');
    for (const linha of linhas) {
        const tr = document.createElement('tr');
        const diaCel = document.createElement('td');
        diaCel.textContent = DIAS_SEMANA[linha.dia];
        const horaCel = document.createElement('td');
        horaCel.textContent = `${linha.hora}h`;
        const qtdCel = document.createElement('td');
        qtdCel.textContent = formatarNumero(linha.quantidade);
        tr.append(diaCel, horaCel, qtdCel);
        corpo.appendChild(tr);
    }
    tabela.appendChild(corpo);
    container.appendChild(tabela);
}

/* =========================================================================
 * Histórico de pedidos
 * ========================================================================= */

function renderizarHistorico(vendas) {
    const container = document.getElementById('historico-table');
    const vazio = document.getElementById('historico-empty');
    container.innerHTML = '';
    vazio.classList.toggle('empty-state--hidden', vendas.length > 0);
    if (vendas.length === 0) {
        return;
    }

    const ordenadas = [...vendas].sort((a, b) => {
        if (a.dataVenda !== b.dataVenda) {
            return a.dataVenda < b.dataVenda ? 1 : -1;
        }
        return b.horaVenda - a.horaVenda;
    });

    const wrapper = document.createElement('div');
    wrapper.className = 'table-view--scroll';
    const tabela = document.createElement('table');
    tabela.innerHTML = '<thead><tr><th>Data</th><th>Hora</th><th>Pedido</th><th>Valor bruto</th><th>Status</th></tr></thead>';
    const corpo = document.createElement('tbody');
    for (const venda of ordenadas) {
        const tr = document.createElement('tr');
        const dataCel = document.createElement('td');
        dataCel.textContent = formatarDataCompleta(venda.dataVenda);
        const horaCel = document.createElement('td');
        horaCel.textContent = venda.horaVenda === null || venda.horaVenda === undefined
            ? '—'
            : `${String(venda.horaVenda).padStart(2, '0')}h`;
        const idCel = document.createElement('td');
        idCel.textContent = venda.idVenda.slice(0, 8);
        idCel.title = venda.idVenda;
        const valorCel = document.createElement('td');
        valorCel.textContent = formatarMoeda(venda.valorBruto);
        const statusCel = document.createElement('td');
        statusCel.textContent = venda.status;
        tr.append(dataCel, horaCel, idCel, valorCel, statusCel);
        corpo.appendChild(tr);
    }
    tabela.appendChild(corpo);
    wrapper.appendChild(tabela);
    container.appendChild(wrapper);
}

/* =========================================================================
 * Orquestração — carrega tudo pro período selecionado
 * ========================================================================= */

async function carregarPainel(inicioIso, fimIso) {
    // evita empilhar buscas se a rodada anterior (manual ou automatica) ainda estiver em andamento
    if (state.carregando) {
        return;
    }
    state.carregando = true;
    tooltip.ocultar();
    ocultarErro();
    state.inicio = inicioIso;
    state.fim = fimIso;

    try {
        const anterior = calcularPeriodoAnterior(inicioIso, fimIso);

        const [vendasResultado, resumoResultado, resumoAnteriorResultado, rankingResultado] = await Promise.allSettled([
            buscarJson(`/api/vendas?inicio=${inicioIso}&fim=${fimIso}`),
            buscarJson(`/api/financeiro/resumo?inicio=${inicioIso}&fim=${fimIso}`),
            buscarJson(`/api/financeiro/resumo?inicio=${anterior.inicio}&fim=${anterior.fim}`),
            buscarJson(`/api/vendas/ranking-produtos?inicio=${inicioIso}&fim=${fimIso}`),
        ]);

        const falhas = [vendasResultado, resumoResultado, resumoAnteriorResultado, rankingResultado]
            .filter((r) => r.status === 'rejected');
        if (falhas.length > 0) {
            mostrarErro(`Não foi possível carregar tudo: ${falhas[0].reason.message}`);
        }

        const vendas = vendasResultado.status === 'fulfilled' ? vendasResultado.value : [];
        const resumo = resumoResultado.status === 'fulfilled'
            ? resumoResultado.value
            : { totalVendas: 0, valorBrutoTotal: 0, valorLiquidoTotal: 0, comissaoTotal: 0 };
        const resumoAnterior = resumoAnteriorResultado.status === 'fulfilled' ? resumoAnteriorResultado.value : null;
        const ranking = rankingResultado.status === 'fulfilled' ? rankingResultado.value : [];

        renderizarKPIs(resumo, resumoAnterior);
        renderizarGraficoVendasPorDia(agruparVendasPorDia(vendas, inicioIso, fimIso));
        renderizarRanking(ranking);
        renderizarHorarioPico(vendas);
        renderizarHistorico(vendas);

        document.getElementById('ultima-atualizacao').textContent =
            `Atualizado às ${new Date().toLocaleTimeString('pt-BR')}`;
    } finally {
        state.carregando = false;
    }
}

/* =========================================================================
 * Filtros — presets + range personalizado
 * --------------------------------------------------------------------- */

function ativarChip(preset) {
    document.querySelectorAll('#filtro-presets .chip').forEach((chip) => {
        chip.classList.toggle('chip--active', chip.dataset.preset === preset);
    });
}

function aplicarPreset(preset) {
    ativarChip(preset);
    const { inicio, fim } = calcularPeriodoPreset(preset);
    document.getElementById('filtro-inicio').value = paraIso(inicio);
    document.getElementById('filtro-fim').value = paraIso(fim);
    carregarPainel(paraIso(inicio), paraIso(fim));
}

function configurarToggleTabela() {
    document.querySelectorAll('[data-toggle-table]').forEach((botao) => {
        botao.addEventListener('click', () => {
            const chave = botao.dataset.toggleTable;
            const grafico = document.getElementById(`${chave}-chart`);
            const tabela = document.getElementById(`${chave}-table`);
            const tabelaEstaOculta = tabela.classList.contains('table-view--hidden');

            tabela.classList.toggle('table-view--hidden', !tabelaEstaOculta);
            grafico.classList.toggle('chart-surface--hidden', tabelaEstaOculta);
            botao.textContent = tabelaEstaOculta ? 'Ver como gráfico' : 'Ver como tabela';
        });
    });
}

function iniciar() {
    document.querySelectorAll('#filtro-presets .chip').forEach((chip) => {
        chip.addEventListener('click', () => aplicarPreset(chip.dataset.preset));
    });

    document.getElementById('filtro-aplicar').addEventListener('click', () => {
        const inicio = document.getElementById('filtro-inicio').value;
        const fim = document.getElementById('filtro-fim').value;
        if (!inicio || !fim) {
            mostrarErro('Escolha as duas datas antes de aplicar.');
            return;
        }
        if (inicio > fim) {
            mostrarErro('A data de início precisa ser antes da data de fim.');
            return;
        }
        ativarChip(null);
        carregarPainel(inicio, fim);
    });

    configurarToggleTabela();
    aplicarPreset('7d');

    // atualizacao automatica em segundo plano — sempre re-busca o MESMO periodo que
    // esta selecionado no momento (state.inicio/state.fim), sem mexer no filtro do usuario
    setInterval(() => {
        carregarPainel(state.inicio, state.fim);
    }, INTERVALO_ATUALIZACAO_MS);
}

document.addEventListener('DOMContentLoaded', iniciar);
