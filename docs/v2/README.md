# InterpretaAI 2.0 — projeto de arquitetura

Esta pasta transforma as decisões de produto em contratos revisáveis antes da implementação.
O nome canônico do produto permanece **InterpretaAI**.

- **LÉIA** é a professora/personagem que conduz as histórias com **Alfa**.
- **LEIA** é a metodologia: Ler, Entender, Interpretar e Aprender.
- O **gibi imersivo** é a jornada principal; jogos aparecem dentro da história para apoiar a
  compreensão e a alfabetização.
- A criação assistida acontece na área do professor. O aplicativo infantil executa pacotes
  aprovados e continua funcional sem internet.

## Documentos

| Documento | Decisão coberta |
|---|---|
| [Arquitetura](ARCHITECTURE.md) | Componentes, fluxos, Oracle, RAG, Codex e segurança |
| [Implantação da arquitetura de IA](AI_ARCHITECTURE_ROLLOUT.md) | Estado Oracle, ordem de ativação, Redis, RAG, guardrails, Codex e LangGraph4j |
| [Estúdio do professor](TEACHER_STUDIO.md) | Navegação, criação, revisão, publicação e estados de tela |
| [Sala móvel](MOBILE_CLASSROOM_DEMO.md) | Lista de alunos, aula temporária, tablets reutilizáveis e prova da entrega |
| [Relatórios](REPORTING_MODEL.md) | Eventos, evidências, observações e limites das conclusões |
| [Modelo de dados](DATA_MODEL.md) | Entidades, identidades, versões, concorrência e classificação |
| [Contratos HTTP](API_CONTRACTS.md) | Upload, autoria, publicação, aparelhos, sessões e erros |
| [Validação e avaliações](VALIDATION_AND_EVALS.md) | Portões, conjunto de avaliação, UX, segurança e evidências |
| [Rastreador da Sprint 0](SPRINT_0_TRACKER.md) | Estado comprovável, decisões e critério de saída |
| [Propostas de decisão](SPRINT_0_DECISION_PROPOSALS.md) | Recomendações conservadoras e registro de aceite |
| [Catálogo de componentes](COMPONENT_CATALOG.md) | Mecânicas permitidas e compatibilidade dos aparelhos |
| [Autorização](AUTHORIZATION_MODEL.md) | Hierarquia, papéis, vínculos e testes negativos |
| [Fontes do RAG](RAG_SOURCE_REGISTER.md) | Procedência, escopo, ingestão e lacunas reais |
| [Fontes próprias candidatas](guidance/) | Método LEIA, guia editorial da LÉIA e acessibilidade |
| [Modelo de ameaças](THREAT_MODEL.md) | Fronteiras, riscos, controles e responsabilidades |
| [Orçamento de IA](AI_BUDGET.md) | Envelopes, limites, medição e pontos pendentes |
| [Protótipo do Estúdio](prototype/README.md) | Execução local e cinco percursos docentes |
| [Teste com professoras](TEACHER_USABILITY_PROTOCOL.md) | Protocolo, tarefas, registro e critério de aceite |
| [Plano de sprints](SPRINT_PLAN.md) | Ordem de entrega, critérios de aceite e dependências |
| [Rastreador da Sprint 1](SPRINT_1_TRACKER.md) | Evidência da fundação Oracle/identidade em execução |
| [Rastreador da Sprint 2](SPRINT_2_TRACKER.md) | Evidência do parser/cache Android e lacunas de entrega variável |
| [Rastreador da Sprint 3](SPRINT_3_TRACKER.md) | Evidência da autoria assistida, RAG e limites ainda abertos |
| [Verificação pública da Oracle](ORACLE_PUBLIC_CHECK.md) | Gateway v1, Keycloak, Estúdio ativo e próximos portões físicos |
| [Ensaio local do planejador](AUTHORING_MODEL_SMOKE.md) | Taxa de aceitação do contrato por modelo, sem confundir Mac com Oracle |
| [Contrato da história](contracts/learning-story-pack.schema.json) | Estrutura executável e versionada no Android |
| [Exemplo de história](contracts/example-apple-story-pack.json) | Exemplo mínimo completo do contrato |
| [Contrato de autoria](contracts/authoring-job.schema.json) | Entrada, estados e saída de uma tarefa assistida |
| [Plano de autoria](contracts/authoring-plan.schema.json) | Rascunho estruturado, fontes citadas e fronteira anterior ao pacote infantil |
| [OpenAPI v2](contracts/interpretaai-v2.openapi.yaml) | Contrato executável das rotas principais |
| [Manifesto das fontes](contracts/guidance-source-manifest.schema.json) | Procedência, hashes e aprovação do futuro RAG |
| [Contrato do estudo docente](contracts/teacher-usability-study.schema.json) | Cinco tarefas, achados e aceite mensurável |
| [ADR: pacote offline](adr/001-offline-story-pack.md) | Por que a fonte de verdade no Android é local |
| [ADR: fronteira do Codex](adr/002-codex-authoring-boundary.md) | Permissões e isolamento da extensão de autoria |
| [ADR: vínculos de mídia](adr/003-bound-story-assets.md) | Como cada recurso aprovado chega ao cache sem URL arbitrária |

## Decisões fechadas nesta versão

1. O backend continua Java/Spring Boot em um monólito modular, com worker separado do processo web.
2. O Android continua Kotlin/Compose e passa a interpretar `LearningStoryPack` em vez de conter
   palavras, imagens e sequências específicas no código.
3. O pacote publicado é imutável. Uma edição cria uma nova versão e exige nova aprovação.
4. A professora não aperta “baixar”. Conteúdo atribuído é preparado automaticamente em cache;
   o acervo embarcado cobre ausência total de conexão.
5. RAG orienta a criação com fontes aprovadas. Regras obrigatórias permanecem em validadores do
   backend e nunca dependem da recuperação semântica.
6. Codex produz somente rascunhos no ambiente de autoria. Não publica, não lê dados de crianças e
   não altera o código do produto durante uma solicitação docente.
7. Relatórios distinguem evento técnico, evidência de participação, observação docente e sugestão
   assistida. Nenhuma dessas camadas produz diagnóstico ou nota automática.

## Fora do primeiro corte

- mecânicas geradas dinamicamente fora do catálogo do APK;
- publicação direta por IA;
- classificação clínica, nota, ranking ou decisão automática sobre a criança;
- RAG construído com relatórios individuais;
- edição de uma história enquanto uma sessão infantil está em andamento;
- dependência de internet para toque, navegação, instruções essenciais ou encerramento.
