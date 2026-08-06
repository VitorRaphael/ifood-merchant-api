# Roteiro — vídeos de homologação dos módulos Financial e Analytics

Baseado no checklist do chamado #31049573 (06/08/2026). Um vídeo por módulo,
cada um com 1 cenário só, conforme pedido.

## Antes de gravar

- [ ] Rodar o backend com o `IFOOD_CLIENT_ID`/`IFOOD_CLIENT_SECRET` da **loja de teste** (Client ID
      `d6db2399-ecb3-44c9-8672-7c9ee98f3930`) — nunca com as credenciais da loja
      real do seu pai.
- [ ] **Apontar pra um banco SQLite separado** enquanto grava (ex.: copiar
      `application.properties` com `spring.datasource.url=jdbc:sqlite:homolog.db`
      ou definir via `-Dspring.datasource.url=...` na hora de rodar). Isso evita
      misturar dados de teste com os repasses/vendas reais da loja de produção.
- [ ] Relógio do Windows visível durante toda a gravação.
- [ ] Frontend aberto em `http://localhost:8080`, logado.
- [ ] Testar as duas telas uma vez antes de gravar (sem erros no console).
- [ ] Gravador mostrando a tela inteira do navegador.

## Cenário 1 — Financial (Liquidações)

Tela: **Financeiro** (barra lateral, grupo "Módulos").

1. Abrir o painel, clicar em **Financeiro**.
2. Escolher um período (Início/Fim) que tenha liquidações reais na loja de
   teste e clicar em **"Sincronizar liquidações"** — isso chama
   `GET /financial/v3.0/merchants/{merchantId}/settlements` de verdade.
3. Mostrar a tabela preenchida (título, tipo, status, valor, data de
   pagamento).
4. Apontar o badge no topo da tela (Client ID / Merchant ID / horário do
   servidor) — é o dado que o avaliador vai cruzar com os logs deles.
5. Clicar em **"Atualizar lista"** pra mostrar que os dados também ficam
   persistidos (`GET /api/financeiro/repasses`), não é só a chamada síncrona.

## Cenário 2 — Analytics (Indicadores)

Tela: **Visão Geral** (tela inicial do painel).

1. Abrir o painel na **Visão Geral**.
2. Mostrar os KPIs no topo (faturamento, nº de vendas, ticket médio etc.).
3. Trocar o período usando os chips de preset (7d, 30d...) ou o filtro de
   datas, e mostrar os números recalculando.
4. Rolar até o **ranking de produtos mais vendidos** e o **gráfico de
   vendas por dia**.
5. Mostrar a tabela de **horário de pico**.
6. Apontar o mesmo badge do Client ID/Merchant/horário no topo da tela.

## Depois de gravar

- Subir os 2 vídeos no Google Drive (não anexar direto no chamado).
- Deixar o link com acesso liberado pra equipe do iFood.
- Responder no chamado #31049573 com: os 2 links + Client ID
  (`d6db2399-ecb3-44c9-8672-7c9ee98f3930`) + data/hora da execução.

## Cuidado

- **Nunca** rodar essa gravação com as credenciais/banco de produção da loja
  real — só com o Client ID de teste acima e um banco SQLite separado.
- Não usar Postman/Insomnia em nenhum momento do vídeo — todas as chamadas
  precisam vir da tela (front-end).
