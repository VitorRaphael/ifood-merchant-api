\## 🔴 REGRA INEGOCIÁVEL — Graphify (leia isto ANTES de qualquer outra coisa)

Isto não é uma sugestão, é um HARD GATE. Já falhei em seguir essa regra uma vez (sessão de 01/08/2026: editei VendaService, o frontend inteiro e Application.java sem atualizar o grafo em nenhum momento, só corrigi quando o usuário perguntou por quê). Não repetir.

\- \*\*ENTRADA (antes de responder qualquer pergunta sobre arquitetura, dependências, "o que usa X", "como Y se conecta com Z", ou antes de ler mais de 1-2 arquivos pra entender o projeto):\*\* consulte `graphify-out/` primeiro (`graphify query "<pergunta>"`, `graphify path`, `graphify explain`) em vez de abrir arquivos um por um. Isso é mais barato e mais rápido que ler tudo de novo.

\- \*\*SAÍDA (depois de QUALQUER edição de código — criar, editar ou deletar arquivo/método/classe, não importa o quão pequena):\*\* rode `graphify . --update --code-only` (ou `graphify . --code-only` se não houver grafo ainda) e, se a comunidade/estrutura mudou, `graphify cluster-only .` — ANTES de encerrar a resposta, não depois, não "na próxima vez".

\- \*\*Checagem de sanidade:\*\* se `git rev-parse HEAD` (curto) não bater com o "Built from commit" no topo de `graphify-out/GRAPH_REPORT.md`, o grafo está desatualizado — atualize antes de confiar nele.

\- \*\*Vault Obsidian (`graphify-out/obsidian/`):\*\* depois de rodar `cluster-only`, se as comunidades mudaram de nome ou estrutura, rode também `graphify export obsidian` (com `GEMINI_API_KEY` no ambiente pra nomear as comunidades — pedir a chave ao Vitor se não estiver disponível) pra manter o vault colorido sincronizado. Abrir sempre a pasta `graphify-out/obsidian/` como vault no Obsidian, nunca a raiz do projeto.

Se eu esquecer isso de novo, é falha minha de execução, não falta de instrução — a regra sempre esteve visível aqui.
