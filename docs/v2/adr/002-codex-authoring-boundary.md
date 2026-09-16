# ADR 002 — Codex isolado no domínio de autoria

- **Estado:** proposto para aprovação na Sprint 0
- **Data:** 2026-09-16

## Contexto

A professora deseja pedir uma história e receber roteiro, imagens e atividades dentro do
InterpretaAI. Codex pode coordenar uma tarefa complexa de autoria, mas um agente com acesso amplo ao
servidor, publicação e dados pedagógicos aumentaria o impacto de uma instrução incorreta.

## Decisão

Executar Codex em processo/contêiner isolado, acionado por tarefa persistente. Ele recebe apenas o
briefing, referências recuperadas, contrato e diretório temporário. Ferramentas permitidas produzem
rascunhos e recursos privados. O backend valida tudo e somente a professora aprova/publica.

O executor será um sidecar/worker server-side em TypeScript/Node ou Python, conforme os SDKs
oficiais. Spring continua sendo a autoridade de domínio e conversa com ele por fila e contrato
interno; o Android e o navegador nunca acessam o SDK diretamente.

Permissões negadas por desenho:

- banco e relatórios de crianças;
- credencial de publicação;
- shell na VM da API;
- alteração do repositório do produto durante tarefa docente;
- rede aberta e segredos permanentes.

## Consequências

- integração pode ser desligada sem impedir autoria manual ou execução infantil;
- precisa de runner, limites, limpeza de workspace e observabilidade;
- backend continua responsável por autorização, contrato e procedência;
- tarefas simples podem usar adaptadores diretos e evitar custo de agente;
- resultado do agente nunca é automaticamente confiável/publicável.

## Alternativas descartadas

- executar Codex dentro da API Spring: disputa recursos e amplia acesso;
- expor Codex diretamente ao navegador: entrega credenciais e ignora o backend;
- permitir que o agente publique: elimina o controle docente e a revisão versionada.
