---
project: ifood-merchant-api
domain: integração com iFood Merchant API (loja real do pai do Vitor, Scooby Lanches)
status: em desenvolvimento — Fases 1-6 completas, Fase 7 + homologação em andamento
local: C:\Vitor Raphael\Códigos\API 3.0
---

# O que é
API Java/Spring Boot que integra com a Merchant API do iFood pra rastrear
pedidos, vendas, pagamentos e repasses financeiros da loja real do pai do
Vitor. É produto real + peça de portfólio pra estágio (~2027). Projeto irmão
do `gestor-comercial` (PDV físico), mas este é a loja de delivery no iFood.

# Regras inegociáveis
- **Nunca commitar secrets** (client secret, tokens OAuth) — causa raiz do
  fracasso do projeto anterior. Secrets vivem em env vars (IntelliJ run
  config), `tokens.json` está no `.gitignore`.
- **"Banana-easy"**: qualquer decisão de arquitetura deve minimizar esforço
  de onboarding do cliente — é pré-requisito do modelo de negócio (distribuir
  grátis pra 20-50 comércios em troca de depoimento).
- Vitor ainda não escreve Java sozinho — modo "ensinar" é o padrão (ver
  memória `feedback_teach_dont_do`), exceto quando ele pede código direto
  explicitamente (já aconteceu 2x: Gestor de Pedidos, Analytics).
- Graphify é hard gate neste repo (mesmo padrão do `gestor-comercial`) — ver
  `CLAUDE.md`.

# Stack
Spring Boot · SQLite (`sqlite-jdbc` + `hibernate-community-dialects`,
migrado de MySQL) · auth Client Credentials com refresh automático · frontend
vanilla HTML/CSS/JS sem build (Kanban de pedidos + dashboards embutidos no
mesmo Spring Boot, `static/`).

# Estado atual (2026-08-07)
- Fases 1-6: Merchant, Order (com Gestor de Pedidos: auto-aceite + Kanban),
  Financial (Settlement), Analytics — todos com endpoints implementados.
- **Homologação em revisão** (ticket #31049573): Financial **aprovado**.
  Merchant **rejeitado** (Cenário 1 precisa comparar com Portal do Parceiro,
  script já corrigido). Analytics **rejeitado** — módulo não habilitado no
  app de teste; decisão tomada: gravar vídeo mostrando requisição real +
  tratamento gracioso do 403, perguntando ao iFood se isso já basta.
- Loja de teste: Merchant ID `3965838` / UUID
  `d102dcc7-4c89-48c1-8013-6edc36af90bb`. Client ID de teste
  `d6db2399-ecb3-44c9-8672-7c9ee98f3930`.

# Gaps conhecidos / não fazer
- Não reabrir a investigação do bug do Financial 401 (já resolvido — era
  bloqueio do lado do iFood, homologação pendente).
- Não re-debugar o 403 do Analytics como se fosse bug de código — é
  confirmado permissão de módulo faltando, não erro de request.
- `mvnw.cmd` sem o mesmo problema de encoding do Gestor Comercial (pasta sem
  acento), mas ainda preferir Bash por consistência.

# Próximos passos possíveis
Aguardar resposta do ticket #31049573 (Merchant recorrigido + Analytics),
depois retomar Fase 7 (testes, OpenAPI docs) e/ou avançar no long-term
vision (dashboards, estoque inteligente, forecasting).
