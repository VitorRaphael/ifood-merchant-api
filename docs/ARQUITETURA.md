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
| GET    | /api/vendas                 | Lista vendas, com filtro por período                        | 5    | ✅ Implementado (testado) |
| GET    | /api/vendas/{id}             | Detalhe de uma venda e seus pagamentos                      | 5    | ✅ Implementado (testado) |
| POST   | /api/financeiro/sincronizar | Busca liquidações no iFood e salva como `Repasse`            | 6    | ✅ Implementado (bloqueado no iFood — ticket 31017178) |
| GET    | /api/financeiro/repasses    | Lista os repasses recebidos do iFood                        | 6    | ✅ Implementado (testado, retorna vazio até o ticket resolver) |
| GET    | /api/financeiro/resumo      | Soma bruto/líquido/comissão entre duas datas (baseado em `Venda`) | 6    | ✅ Implementado (testado) |

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
- [x] **Fase 5 — Persistir vendas**: mapear pedidos recebidos pra `Venda`/`Pagamento`
      e salvar no banco.
      - [x] `IFoodOrderService`/`OrderController` buscam o detalhe do pedido no iFood.
      - [x] `VendaService` mapeia o JSON do pedido pra `Venda`+`Pagamento` e salva no MySQL
            (corrigido loop infinito de serialização com `@JsonManagedReference`/`@JsonBackReference`).
      - [x] `GET /api/vendas` e `GET /api/vendas/{id}` testados no Postman.
      - [x] `IFoodEventService` processa o evento "PLACED" (`code == "PLC"`) sozinho e persiste
            a venda automaticamente — confirmado com 2 pedidos de teste reais, sem chamada manual.
      - [x] Acknowledgment dos eventos junto ao iFood (`POST /events/v1.0/events/acknowledgment`)
            implementado e confirmado no log — eventos processados não são mais reenviados.
- [x] **Fase 6 — Financeiro**: consumir repasses/settlements, salvar `Repasse`,
      endpoint de resumo agregado.
      - [x] `IFoodFinancialService`/`FinanceiroController` (`POST /sincronizar`) consultam o
            módulo Financial v3.0 do iFood — **bloqueado no lado do iFood** (401 "token
            expired" em loja de teste mesmo com token/escopo corretos, confirmado via
            reprodução direta na doc oficial deles; ticket de suporte 31017178 aberto em
            30/07/2026, aguardando retorno).
      - [x] `GET /api/financeiro/repasses` testado — retorna `[]` até o sincronizar
            conseguir popular a tabela `repasses`.
      - [x] `GET /api/financeiro/resumo` implementado com base na tabela `Venda` (não em
            `Repasse`, que segue vazia) — soma bruto/líquido/quantidade de vendas entre duas
            datas, com comissão calculada como bruto − líquido. Testado e confirmado.
      - [x] Banco de dados migrado de MySQL para SQLite (arquivo único local `ifood_merchant.db`,
            sem servidor/credenciais — decisão pensada para a distribuição futura do software
            a outras lojas). Corrigido bug de serialização de `LocalDate` do dialect SQLite
            com um `AttributeConverter` customizado (`LocalDateAttributeConverter`, guarda
            como texto ISO `yyyy-MM-dd`).
      - [x] Autenticação com o iFood automatizada: `IFoodAuthService` agora guarda o horário
            de expiração (`expiraEm`) junto do token e se autorrenova sozinho sempre que
            alguém chama `getValidToken()` e o token está vencido ou ausente — não precisa
            mais chamar `/api/auth/autenticar` manualmente no dia a dia.
- [x] **Fase 7 — Qualidade**: tratamento de erro, testes automatizados, documentação
      OpenAPI/Swagger.
      - [x] Tratamento de erro centralizado: pacote `exception` com `IFoodApiException`,
            `TokenIndisponivelException`, `VendaNaoEncontradaException` e um
            `GlobalExceptionHandler` (`@RestControllerAdvice`) que traduz cada uma pro
            status HTTP certo (404, 409, 429, 502, 503) com um formato de erro único
            (`ErroResposta`) em toda a API — antes, qualquer exceção virava o erro genérico
            500 padrão do Spring, sem diferenciar a causa real. Testado ao vivo (venda
            inexistente → 404 correto).
      - [x] Testes automatizados: `VendaServiceTest` (unitário, com Mockito — regras de
            negócio de `buscarVenda`/`gerarResumo`) e `VendaControllerTest` (`@WebMvcTest`,
            confirma que o `GlobalExceptionHandler` realmente devolve 404 estruturado na
            camada HTTP). `src/test/resources/application.properties` próprio, com
            credenciais fictícias e banco SQLite separado — os testes não dependem de
            nenhuma variável de ambiente real nem tocam no `ifood_merchant.db` de verdade.
            7 testes, todos passando (`mvn test` → `BUILD SUCCESS`).
      - [x] Documentação OpenAPI/Swagger: dependência `springdoc-openapi-starter-webmvc-ui`
            (3.0.3) + `OpenApiConfig` com título/descrição. UI disponível em
            `/swagger-ui/index.html`, JSON da spec em `/v3/api-docs` — gerado automaticamente
            a partir dos Controllers existentes, sem anotação manual por endpoint. Testado
            ao vivo, confirmado funcionando.

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
