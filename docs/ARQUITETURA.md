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

## Endpoints planejados

| Método | Rota                       | O que faz                                             | Fase |
|--------|-----------------------------|--------------------------------------------------------|------|
| GET    | /api/auth/iniciar           | Inicia o OAuth2 e devolve a URL de verificação          | 3    |
| POST   | /api/auth/confirmar         | Recebe o código de 9 letras e troca pelo access token   | 3    |
| GET    | /api/loja/status            | Loja aberta ou fechada agora                            | 4    |
| GET    | /api/vendas                 | Lista vendas, com filtro por período                    | 5    |
| GET    | /api/vendas/{id}             | Detalhe de uma venda e seus pagamentos                  | 5    |
| GET    | /api/financeiro/repasses    | Lista os repasses recebidos do iFood                    | 6    |
| GET    | /api/financeiro/resumo      | Soma bruto/líquido/comissão entre duas datas            | 6    |

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
- [ ] **Fase 4 — Consumo do iFood**: status da loja e polling de eventos agendado
      (`@Scheduled`).
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
