# Instruções do projeto aptasks

## Commits de funcionalidades

Quando um commit adicionar ou alterar uma funcionalidade (não para correções triviais, typos ou ajustes de configuração), siga estas regras:

1. **Mensagem de commit com descrição completa.** A primeira linha é o resumo no imperativo, com até 72 caracteres. O corpo explica:
   - **O quê:** o que a funcionalidade faz, do ponto de vista do usuário.
   - **Por quê:** o problema ou a necessidade que motivou a mudança.
   - **Como:** a abordagem técnica, incluindo componentes alterados e decisões relevantes.
   - **Testes:** quais testes cobrem a mudança e o resultado da execução.
   - **Arquivos principais:** lista dos arquivos mais importantes.
   - **Diagrama:** caminho do arquivo de arquitetura gerado (ver abaixo).

2. **Diagrama de arquitetura em Mermaid.** Gere um arquivo em `docs/arquitetura/<slug-da-funcionalidade>.md` contendo:
   - Um bloco `flowchart` ou `sequenceDiagram` mostrando os componentes envolvidos (controller, service, repository, tabelas, scheduler, frontend) e como eles se comunicam.
   - Uma seção curta de texto explicando o fluxo principal em 3 a 6 passos.
   - Só desenhe o que a mudança realmente toca. Não inclua o sistema inteiro.

3. **Idioma.** Escreva o corpo do commit e o arquivo de arquitetura em português, que é o idioma da documentação do projeto. O resumo da primeira linha pode seguir o estilo dos commits existentes.

4. **Trailer de atribuição.** Termine a mensagem com `Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>`, ou com o modelo que estiver em uso.

5. **Escopo do commit.** Inclua o arquivo de arquitetura no mesmo commit da funcionalidade. Não misture mudanças não relacionadas e não inclua arquivos que já estavam modificados no working tree sem relação com a tarefa.

6. **Branch.** Faça a mudança em uma branch própria, nunca direto na `main`.
