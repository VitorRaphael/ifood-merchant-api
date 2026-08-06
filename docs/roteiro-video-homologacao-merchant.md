# Roteiro — vídeo de homologação do módulo Merchant

Baseado nas orientações do chamado (05/08/2026) e nos critérios oficiais em
`developer.ifood.com.br/pt-BR/docs/guides/modules/merchant/homologacao`.

## Antes de gravar

- [ ] Relógio do Windows visível durante toda a gravação (canto inferior direito já aparece nos seus prints — ok).
- [ ] Backend rodando (IntelliJ, run config com `IFOOD_CLIENT_ID`/`IFOOD_CLIENT_SECRET`), frontend aberto em `http://localhost:8080`.
- [ ] Aba "Minha Loja" testada uma vez antes de gravar (sem erros no console).
- [ ] Gravador (OBS ou extensão) mostrando a tela inteira do navegador, não uma janela recortada.
- [ ] Um vídeo por cenário, conforme pedido no chamado — não precisa ser um único vídeo gigante.

## Cenário 1 — Informações da Loja

1. Abrir o painel, clicar em **Minha Loja** na barra lateral.
2. Clicar em **"Listar lojas"** → mostrar a lista carregada (id, nome).
3. Clicar em **"Ver detalhes"** na loja → mostrar o card "Detalhes da loja" preenchido.
4. Clicar em **"Consultar disponibilidade"** → mostrar o selo de status (OK/WARNING/CLOSED/ERROR) e os dados abaixo.
5. Narrar brevemente o que apareceu em cada card (não precisa de áudio se o chamado não pedir; se pedir, é só descrever o que já está na tela).

## Cenário 2 — Interrupção na Loja (pausa)

1. Preencher o formulário "Motivo / Início / Fim" e clicar em **"Criar pausa"**.
2. Mostrar a pausa aparecendo na lista logo abaixo.
3. Trocar para a aba do **Portal do Parceiro** (login já feito) → mostrar a mesma pausa refletida lá.
4. Voltar ao painel, clicar em **"Atualizar lista"** → confirmar que a pausa continua listada.
5. Clicar em **"Remover"** na pausa criada.
6. Mostrar a lista vazia (ou sem aquela pausa) no painel.
7. Voltar ao Portal do Parceiro → mostrar que a pausa sumiu de lá também.

## Cenário 3 — Horário de Funcionamento

1. Clicar em **"Consultar horários"** primeiro, pra mostrar o estado atual (pode estar vazio).
2. Usar **"+ Adicionar turno"** para montar exatamente:
   - Sábado: 10:00 até 19:00
   - Domingo: 09:00 até 12:00
   - Domingo: 13:00 até 16:00
   - Domingo: 17:00 até 23:00
3. Clicar em **"Salvar horários"**.
4. Mostrar a tabela atualizada com os 4 turnos (dia, início, duração em minutos).
5. Trocar para o Portal do Parceiro → mostrar os mesmos horários refletidos lá.

## Depois de gravar

- Subir os vídeos no Google Drive (não anexar direto no chamado).
- Deixar o link com acesso liberado pra equipe da iFood.
- Responder no chamado com: os 3 links + o Client ID (`d6db2399-ecb3-44c9-8672-7c9ee98f3930`).

## Pontos do critério oficial que vale ficar de olho (mesmo sem aparecer no vídeo)

- Erros (401/403/409/429/500) já são tratados no backend com mensagem clara — não precisa demonstrar isso no vídeo a menos que o chamado peça explicitamente.
- Criar uma pausa que se sobrepõe a outra já existente deve devolver erro 409 — se quiser reforçar entendimento, pode tentar isso uma vez e mostrar o banner de erro aparecendo no painel (opcional, não obrigatório pelos 3 cenários originais).
