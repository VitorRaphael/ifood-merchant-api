# Arquitetura — ifood-merchant-api

> Planta baixa do projeto. Referência viva: atualizar ao concluir cada fase do roteiro.

## Visão geral

API REST que integra com a Merchant API do iFood para acompanhar, em tempo real, os
pedidos, pagamentos e repasses financeiros de uma loja real — sem depender de checar
o painel do iFood manualmente.

Problema que resolve: hoje, saber quanto a loja recebeu líquido (depois de comissão,
taxa de entrega e promoções) exige cruzar números no painel do iFood na mão. A API
escuta os pedidos assim que acontecem e guarda o extrato financeiro assim que ele
fica disponível.

## Arquitetura em camadas

Quatro camadas, cada uma com uma única responsabilidade. Uma camada nunca pula a
outra: o Controller nunca fala direto com o Repository.

```
iFood Merchant API (OAuth2 · Eventos · Financeiro)
        |
        v
   [ Service ]  <- regra de negócio, autenticação, consumo da API externa
        |
        v
  [ Repository ]  <- fala com o banco
        |
        v
   [ Entity ]  <- Venda · Pagamento · Repasse
        |
        v
      MySQL

  [ Controller ]  <- recebe e devolve HTTP, chama o Service
```

## Modelo de dados

### Venda (tabela `vendas`)

| Campo         | Tipo               | Descrição                                  |
|---------------|---------------------|---------------------------------------------|
| idVenda       | String · PK         | ID do pedido, vindo do iFood                |
| dataVenda     | LocalDate           | Data em que o pedido foi feito              |
| valorBruto    | BigDecimal          | Total do pedido, sem descontos              |
| valorLiquido  | BigDecimal          | Valor que efetivamente cai na conta         |
| taxaEntrega   | BigDecimal          | Taxa de entrega repassada/cobrada           |
| status        | String              | CONFIRMADO · ENTREGUE · CANCELADO           |
| pagamentos    | List\<Pagamento\>   | 1 venda → N pagamentos                      |

### Pagamento (tabela `pagamentos_venda`)

| Campo           | Tipo         | Descrição                        |
|-----------------|--------------|-----------------------------------|
| idPagamento     | Long · PK    | Gerado automaticamente            |
| metodoPagamento | String       | PIX · CREDIT · DEBIT · VOUCHER    |
| valorPago       | BigDecimal   | Valor pago por esse método        |
| venda           | Venda · FK   | N pagamentos → 1 venda            |

### Repasse (tabela `repasses`)

| Campo         | Tipo         | Descrição                              |
|---------------|--------------|------------------------------------------|
| periodId      | String · PK  | Código do período de apuração do iFood   |
| dataPrevista  | LocalDate    | Quando o dinheiro cai na conta           |
| valorBruto    | BigDecimal   | Total vendido no período                 |
| comissaoIfood | BigDecimal   | Comissão cobrada pelo iFood              |
| promocoes     | BigDecimal   | Coparticipação em cupons                 |
| cancelamentos | BigDecimal   | Estornos do período                      |
| valorLiquido  | BigDecimal   | O que realmente vai pro caixa            |

## Fluxo de dados

```
Cliente faz o pedido no iFood
        |
        v
EventService faz polling (GET /events:polling) a cada 30s (@Scheduled)
        |
        v
Novo evento de pedido detectado
        |
        v
VendaService processa o evento
        |
        v
Salva Venda + Pagamento no MySQL
        |
        v
Controller expõe consulta (GET /api/vendas, /api/financeiro/...)
```

## Endpoints

Registro vivo de toda rota da nossa API — atualizar a cada endpoint novo. É a
referência rápida pra consultar sozinho sem precisar reler o código inteiro.

| Método | Rota                       | O que faz                                               | Fase | Status         |
|--------|-----------------------------|----------------------------------------------------------|------|----------------|
| GET    | /api/status                 | Ping simples pra confirmar que a API está no ar           | 2    | ✅ Implementado |
| POST   | /api/auth/autenticar        | Autentica via `client_credentials` e salva o token         | 3    | ✅ Implementado |
| GET    | /api/loja/lojas             | Lista as lojas vinculadas ao app (proxy do iFood)          | 4    | ✅ Implementado (testado) |
| GET    | /api/loja/status            | Loja aberta ou fechada agora                                | 4    | ✅ Implementado (testado) |
| GET    | /api/loja/{id}               | Detalhes completos de uma loja                              | 4    | ✅ Implementado (testado) |
| GET/PUT| /api/loja/horarios          | Consultar/atualizar horários de funcionamento                | 4    | ✅ Implementado (testado) |
| GET/POST/DELETE | /api/loja/pausas   | Consultar/criar/remover pausas da loja                       | 4    | ✅ Implementado (testado) |
| GET    | /api/vendas                 | Lista vendas, com filtro por período                        | 5    | ⏳ Planejado    |
| GET    | /api/vendas/{id}             | Detalhe de uma venda e seus pagamentos                      | 5    | ⏳ Planejado    |
| GET    | /api/financeiro/repasses    | Lista os repasses recebidos do iFood                        | 6    | ⏳ Planejado    |
| GET    | /api/financeiro/resumo      | Soma bruto/líquido/comissão entre duas datas                | 6    | ⏳ Planejado    |

## Roteiro de construção

- [x] **Fase 0 — Fundamentos**: repositório criado, projeto gerado no Spring Initializr,
      pacote renomeado pra convenção correta, primeiro commit.
- [x] **Fase 1 — Camada de domínio**: entidades `Venda`, `Pagamento` e os
      Repository JPA correspondentes. (`Repasse` fica pra Fase 6, junto do financeiro.)
- [x] **Fase 2 — Primeiro Controller**: endpoint simples de teste (`GET /api/status`)
      pra sentir o ciclo requisição → resposta.
- [x] **Fase 3 — Autenticação via API**: fluxo `client_credentials` via `POST /api/auth/autenticar`,
      client id/secret via variável de ambiente. Usa o app de teste (sandbox) do iFood, vinculado
      automaticamente à loja de teste — o app "Scooby Financial" fica reservado pra homologação futura
      com a loja real.
- [x] **Fase 4 — Consumo do iFood (módulo Merchant)**: implementar e tratar corretamente
      os 8 pontos exigidos na homologação do módulo Merchant, na ordem:
      1) `GET /merchants` (listar lojas) · 2) `GET /merchants/{id}/status` (aberta/fechada)
      · 3) `GET /merchants/{id}` (detalhes) · 4) `GET`+`PUT /merchants/{id}/opening-hours`
      (horários) · 5) `GET`+`POST`+`DELETE /merchants/{id}/interruptions` (pausas).
      Tratamento de erro obrigatório em todos: 401 (token inválido), 403 (sem permissão),
      409 (conflito, ex. pausa sobreposta), 429 (respeitar `Retry-After`), 5xx (retry com
      backoff). Depois, polling de eventos agendado (`@Scheduled`).
      - [x] Os 8 endpoints implementados e testados no Postman.
      - [x] Tratamento de erro 409 (conflito) implementado — não testável de fato na loja
            de teste (o sandbox não aplica a regra de negócio real de conflito).
      - [x] Polling de eventos agendado (`@Scheduled`, a cada 30s) rodando e confirmado no log.
- [ ] **Fase 5 — Persistir vendas**: mapear pedidos recebidos pra `Venda`/`Pagamento`
      e salvar no banco.
- [ ] **Fase 6 — Financeiro**: consumir repasses/settlements, salvar `Repasse`,
      endpoint de resumo agregado.
- [ ] **Fase 7 — Qualidade**: tratamento de erro, testes automatizados, documentação
      OpenAPI/Swagger.

## Segurança & configuração

Regra de ouro: nenhum segredo real (client secret, senha de banco, token) entra em
código commitado — nem "só pra testar rápido".

```properties
# application.properties
ifood.client.id=${IFOOD_CLIENT_ID}
ifood.client.secret=${IFOOD_CLIENT_SECRET}
spring.datasource.password=${DB_PASSWORD}
```

Checklist antes de cada push, principalmente depois de mexer em autenticação:

- [ ] `git status` não mostra `tokens.json` nem nenhum arquivo de credencial
- [ ] `application.properties` só tem `${VARIAVEL}`, nunca o valor real
- [ ] `.gitignore` cobre qualquer arquivo local de configuração sensível

---

*Planta visual (com diagramas) disponível em:
https://claude.ai/code/artifact/c49aa821-5dd3-4b85-a362-adb76a58c7a9*
