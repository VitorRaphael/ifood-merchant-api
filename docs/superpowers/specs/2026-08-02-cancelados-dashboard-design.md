# Design: Contagem correta de pedidos e visibilidade de cancelados no dashboard

## Contexto

O polling de eventos do iFood só escutava o código `PLC` (pedido aceito), gravando toda venda como `CONFIRMADO` e nunca atualizando esse status depois. O resumo financeiro (`VendaService.gerarResumo`) somava todas as vendas do período sem filtrar por status — pedidos cancelados entravam no relatório como se fossem receita real ("pedidos fantasmas").

Numa sessão anterior já foi corrigido:
- `IFoodEventService` agora trata `CON` (concluído) e `CAN` (cancelado), chamando `VendaService.atualizarStatus(orderId, novoStatus)`.
- `VendaService.gerarResumo()` já filtra `totalBruto`/`totalLiquido` para somar apenas vendas `CONCLUIDO`.

Falta: a contagem de pedidos (`totalVendas`) ainda usa `vendas.size()` (todos os status), e não existe nenhuma visibilidade de quantos pedidos foram cancelados nem quanto isso representa.

## Decisões

- **Pedidos (card "Pedidos")**: passa a contar só `CONCLUIDO`, mesmo critério da receita.
- **Novo card "Cancelados"**: mostra quantidade + valor bruto que os pedidos cancelados teriam somado.
- **Sem card para "em andamento" (`CONFIRMADO`)**: fora de escopo por agora.
- **Dados de teste já gravados no banco (13 pedidos travados em `CONFIRMADO`)**: são de sandbox/teste, não serão reconciliados — serão limpos manualmente, fora deste trabalho.

## Backend

`ResumoFinanceiro` (record) ganha 2 campos novos:
```java
public record ResumoFinanceiro(
        LocalDate periodoInicio,
        LocalDate periodoFim,
        long totalVendas,
        BigDecimal valorBrutoTotal,
        BigDecimal valorLiquidoTotal,
        BigDecimal comissaoTotal,
        long totalCancelados,
        BigDecimal valorCancelado
) {}
```

`VendaService.gerarResumo()`: no mesmo laço que já existe, ramifica por status em vez de só pular quem não é `CONCLUIDO`:
- `CONCLUIDO` → soma em `totalBruto`/`totalLiquido`, incrementa contador de vendas concluídas.
- `CANCELADO` → soma em `valorCancelado`, incrementa `totalCancelados`.
- Qualquer outro status (ex: `CONFIRMADO`) → ignorado, não entra em nenhum total.

`totalVendas` do resumo passa a ser a contagem de `CONCLUIDO` (substitui `vendas.size()`).

Nenhuma mudança de schema de banco, endpoint ou contrato de rota — só no corpo da resposta de `GET /api/financeiro/resumo`.

## Frontend (`app.js` + dashboard HTML)

- Novo card de KPI "Cancelados" ao lado dos 4 já existentes (Total vendido, Pedidos, Ticket médio, Comissão iFood), no mesmo estilo visual: quantidade + valor perdido (ex: `5 cancelados · R$ 210,00 perdido`).
- `renderizarKPIs()` ganha essa quinta chamada, lendo `resumo.totalCancelados` / `resumo.valorCancelado` — sem novo fetch, reaproveita a chamada a `/api/financeiro/resumo` que já existe.
- Delta comparado ao período anterior, igual os outros cards já fazem (via `resumoAnterior`).
- Na tabela de pedidos existente (coluna `status` já exibida), linhas com `CANCELADO` recebem uma cor/badge diferente (CSS) para ficar visualmente óbvio — não muda a estrutura da tabela.
- Gráfico "Vendas por dia" não muda: já consome o mesmo `resumo`, que já reflete só receita `CONCLUIDO`.

## Fora de escopo

- Reconciliação de pedidos antigos gravados antes da correção.
- Card/indicador para pedidos `CONFIRMADO` (em andamento).
- Endpoint dedicado para cancelados (os dados vêm do mesmo `/resumo`).
