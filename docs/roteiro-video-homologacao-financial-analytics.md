# Roteiro — vídeo de homologação do módulo Analytics

Baseado na resposta do chamado #31049573 (07/08/2026) e nos critérios oficiais em
`developer.ifood.com.br/pt-BR/docs/food/guides/modules/analytics/homologation`.

## Financial — já aprovado, nada a regravar

A equipe do iFood confirmou aprovação do módulo Financial na resposta de
07/08/2026 ("O módulo de Financial está tudo certo, tá?"). O vídeo antigo
continua válido — não precisa gravar de novo.

## Por que o vídeo anterior de Analytics foi reprovado

O avaliador não identificou **nenhuma requisição real ao módulo Analytics**
no vídeo enviado. Isso porque, até 07/08/2026, o painel "Visão Geral" calculava
os indicadores localmente a partir do nosso próprio banco (tabela `Venda`) —
nunca chamava `POST /analytics/v1.0/merchants/{merchantId}/orders/kpis`. Essa
lacuna foi corrigida: existe agora uma tela própria **Analytics** (barra
lateral, grupo "Módulos") que faz a chamada real ao endpoint oficial.

## Antes de gravar

- [ ] Rodar o backend com o `IFOOD_CLIENT_ID`/`IFOOD_CLIENT_SECRET` da **loja de teste**
      (Client ID `d6db2399-ecb3-44c9-8672-7c9ee98f3930`) — nunca com as
      credenciais da loja real do seu pai.
- [ ] Relógio do Windows visível durante toda a gravação.
- [ ] Frontend aberto em `http://localhost:8080`, logado.
- [ ] Testar a tela **Analytics** uma vez antes de gravar (sem erros no console).
- [ ] Gravador mostrando a tela inteira do navegador.
- [ ] Não usar Postman/Insomnia — a chamada precisa vir da tela (front-end).

## Cenário — Analytics (Indicadores)

Tela: **Analytics** (barra lateral, grupo "Módulos").

1. Abrir o painel, clicar em **Analytics**.
2. Escolher um período (Início/Fim) e clicar em **"Consultar indicadores"** —
   isso chama `POST /analytics/v1.0/merchants/{merchantId}/orders/kpis` de
   verdade, com `filter.referenceDate` no corpo (obrigatório pelo critério
   de homologação) e o header `x-request-homologation: true`.
3. Mostrar os cartões de indicadores preenchidos: **GMV total**, **GMV sem
   entrega**, **ticket médio**, **pedidos concluídos** e **pedidos
   cancelados**.
4. Mostrar a mensagem de período consultado, deixando claro que os dados são
   **históricos (D-1)**, não em tempo real — isso é um critério explícito da
   documentação oficial.
5. Rolar até as 4 tabelas de distribuição e mostrar cada uma: **por canal de
   venda**, **por status do pedido**, **por método de pagamento** e **por
   modelo logístico**.
6. Apontar o badge no topo da tela (Client ID / Merchant ID / horário do
   servidor) — é o dado que o avaliador vai cruzar com os logs deles.

## Se a loja de teste devolver tudo zerado

Assim como aconteceu no Financial, é possível que o ambiente de homologação
devolva `data` vazio se não houver pedidos recentes o suficiente no período
escolhido. Se isso acontecer: **grave mesmo assim**, mostrando a chamada real
sendo feita (aba Network do navegador, se o chamado pedir) e a tela tratando
o retorno vazio sem erro — o objetivo do vídeo é provar que a integração
funciona, não que existam dados reais na loja de teste.

## Depois de gravar

- Subir o vídeo no Google Drive (não anexar direto no chamado).
- Deixar o link com acesso liberado pra equipe do iFood.
- Responder no chamado #31049573 com: o link + Client ID
  (`d6db2399-ecb3-44c9-8672-7c9ee98f3930`) + data/hora da execução.

## Cuidado

- **Nunca** rodar essa gravação com as credenciais/banco de produção da loja
  real — só com o Client ID de teste acima.
