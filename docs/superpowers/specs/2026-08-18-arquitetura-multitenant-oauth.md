---
title: Planta Oficial — API 3.0 v2 (Servidor Multi-Tenant)
status: aprovado
data: 2026-08-18
---

# Contexto

O app "Scooby Analytics" foi cadastrado no iFood como tipo **Distribuído**
(pensado pra rodar em várias lojas de donos diferentes, parte da visão
"banana-easy" de distribuição do produto). Apps distribuídos não usam o fluxo
`client_credentials` — o iFood exige OAuth **Authorization Code**, que
depende de uma redirect URI pública alcançável pelos servidores do iFood.
`localhost` não é alcançável, então o projeto deixa de ser uma aplicação
local e passa a ser um **servidor real, multi-tenant, hospedado com HTTPS**.

Descoberto ao debugar um erro 401/502 mascarado por um bug de parsing
(resposta de erro do iFood vinha em gzip e sendo lida como texto puro em
`IFoodAuthService`). O erro real, uma vez legível, foi: `"Unsupported grant
type client_credentials to client bb3ecac4-..."`.

# 1. Mapa da Arquitetura

Fluxo OAuth Authorization Code:

1. Lojista acessa `/oauth/autorizar` no nosso servidor.
2. Servidor gera um `state` (CSRF token, salvo em `oauth_states`) e
   redireciona o lojista pro iFood.
3. Lojista faz login/aceita a autorização do lado do iFood.
4. iFood chama nosso `/oauth/callback` com `code` + `state`.
5. Servidor valida o `state` contra `oauth_states`.
6. Servidor troca o `code` por `access_token`/`refresh_token` com o iFood.
7. Tokens são salvos em `oauth_tokens`, criptografados, vinculados ao
   `merchant_id` correto.
8. Um job periódico (por loja) faz polling de eventos/pedidos usando o token
   de cada merchant ativo.
9. Toda requisição de dashboard passa a **resolver o tenant primeiro**
   (qual loja está autenticada) antes de decidir qual token usar pra chamar
   o iFood.

O ponto-chave que muda tudo: hoje existe um único token global injetado via
`application.properties` (`ifood.merchant.id`). Na v2, toda operação passa a
exigir uma etapa extra de "resolver o merchant" antes de qualquer chamada.

# 2. Modelo de dados (PostgreSQL)

## Tabela `merchants`

| Coluna | Tipo | Justificativa |
|---|---|---|
| `id` | `UUID` (PK) | Mesmo UUID que o iFood atribui à loja — evita tradução extra de ID e já é globalmente único. |
| `corporate_name` | `VARCHAR(255)` | Razão social. |
| `name` | `VARCHAR(255)` | Nome fantasia. |
| `cnpj` | `VARCHAR(14)` | Tamanho fixo, sem máscara. |
| `status` | `VARCHAR(20)` | `ATIVO` / `REVOGADO`. |
| `created_at` | `TIMESTAMPTZ` | Precisa ser timezone-aware pra comparar com `Instant` do Java sem bug de fuso. |

## Tabela `oauth_tokens` (1:1 com `merchants`)

| Coluna | Tipo | Justificativa |
|---|---|---|
| `id` | `BIGSERIAL` (PK) | Serial simples, só acessado via `merchant_id`. |
| `merchant_id` | `UUID` (FK única → `merchants.id`) | `UNIQUE` garante 1 token ativo por loja. |
| `access_token` | `TEXT`, criptografado via `@Converter` JPA (AES-GCM) | Tamanho variável; criptografado porque é equivalente a uma senha da loja no iFood. |
| `refresh_token` | `TEXT`, criptografado via `@Converter` JPA | Mesmo motivo. |
| `expires_at` | `TIMESTAMPTZ` | Comparado com `Instant.now()`. |
| `updated_at` | `TIMESTAMPTZ` | Auditoria/debug de renovação. |

Chave AES de criptografia: decisão adiada pra Fase 1 (Infraestrutura) — vive
em env var / cofre de segredos, nunca no banco ou no código.

## Tabela `oauth_states` (proteção CSRF)

| Coluna | Tipo | Justificativa |
|---|---|---|
| `state` | `UUID` (PK) | Valor exato trocado com o iFood durante o redirect. |
| `created_at` | `TIMESTAMPTZ` | Pra expirar states antigos. |
| `used` | `BOOLEAN` | Impede reuso do mesmo link de autorização. |

# 3. Diagnóstico de reaproveitamento do código atual

**Reaproveitáveis 100%:** `IFoodHttpClient`, DTOs de Analytics
(`AnaliticaKpis`, `RankingProduto`, `ResumoFinanceiro`), infra genérica
(`GlobalExceptionHandler`, exceptions customizadas, `CsrfCookieFilter`,
`OpenApiConfig`, `LocalDateAttributeConverter`).

**Substituir por completo:** `IFoodAuthService` (hoje é `client_credentials`
+ `tokens.json` local; vira Authorization Code + refresh, DB-backed).

**Precisam de refatoração:**
- Services com `merchantId` fixo via `@Value`: `IFoodMerchantService`,
  `IFoodFinancialService`, `IFoodAnalyticsService`, `IFoodEventService`,
  `StatusController` — precisam receber `merchantId` por parâmetro.
- Entidades de negócio sem coluna de tenant: `Venda`, `Repasse`,
  `ItemVenda`, `Pagamento` — precisam de FK pra `Merchant`.
- `IFoodEventService` (polling a cada 30s) — hoje itera 1 loja; precisa
  iterar todas as lojas ativas com isolamento de erro por loja.
- `SecurityConfig` — login único fixo hoje; precisa virar sessão por
  lojista.
- Frontend (`app.js`, `loja.js`, `financeiro.js`, `analytics.js`) — nenhum
  arquivo tem seletor de loja; precisa de uma camada de "contexto da loja
  atual".

**Banco:** SQLite atual (`hibernate-community-dialects`) precisa virar
Postgres (driver + dialect no `pom.xml`) — mudança mecânica, baixo risco.

# 4. Roadmap (ordem exata)

1. Banco de dados — criar `merchants`, `oauth_tokens`, `oauth_states` no
   Postgres; trocar driver/dialect (SQLite → Postgres).
2. `IFoodAuthService` novo — Authorization Code + refresh_token.
3. Rotas de OAuth (`/oauth/autorizar`, `/oauth/callback`) — depende de HTTPS
   público (Fase 1 de infra em paralelo).
4. Tenant FK nas entidades de negócio (`Venda`, `Repasse`, etc.).
5. Refatorar services single-merchant um por um (`IFoodMerchantService` →
   `IFoodFinancialService` → `IFoodAnalyticsService`).
6. `IFoodEventService` (polling multi-loja) — por último, mais arriscado.
7. Frontend (seletor de loja + ajuste dos 4 arquivos JS).
8. Infra de produção + homologação do fluxo distribuído com o iFood.
