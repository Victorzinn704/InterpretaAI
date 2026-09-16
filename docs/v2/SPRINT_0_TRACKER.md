# Sprint 0 — rastreador de fechamento

## Regra de status

- `PRONTO PARA REVISÃO`: artefato existe e passou por verificação técnica, mas ainda precisa do
  aceite humano indicado.
- `PENDENTE`: ainda não há evidência suficiente.
- `APROVADO`: somente depois de registrar responsável, data e decisão.

Não converter `PRONTO PARA REVISÃO` em `APROVADO` automaticamente.

| Entrega | Estado | Evidência atual | Aceite pendente |
|---|---|---|---|
| `LearningStoryPack` e história da maçã | PRONTO PARA REVISÃO | schema e exemplo validados | produto, pedagogia e Android |
| Protótipo do Estúdio nas quatro áreas | PRONTO PARA REVISÃO | `prototype/AUDIT.md`: percurso automatizado, 8 capturas e 3 larguras | cinco professoras nas tarefas definidas |
| Modelo de autorização | PRONTO PARA REVISÃO | `AUTHORIZATION_MODEL.md` | segurança e responsável institucional |
| Sessão imutável e eventos v2 | PRONTO PARA REVISÃO | `REPORTING_MODEL.md` | pedagogia, privacidade e dados |
| Catálogo/matriz Android | PRONTO PARA REVISÃO | `COMPONENT_CATALOG.md` | engenharia Android e auditoria visual |
| Fontes do RAG | PENDENTE | registro e critérios criados | indicar fontes reais, licença e curador |
| Ameaças do upload à publicação | PRONTO PARA REVISÃO | `THREAT_MODEL.md` | segurança e privacidade |
| Orçamento e limites de geração | PRONTO PARA REVISÃO | `AI_BUDGET.md` | valores reais dos provedores/Oracle |
| OpenAPI v2 | PRONTO PARA REVISÃO | `contracts/interpretaai-v2.openapi.yaml` validado | backend, Android e segurança |

## Decisões que bloqueiam implementação

| ID | Decisão | Responsável sugerido | Prazo | Estado |
|---|---|---|---|---|
| `D-01` | objetivos curriculares e fontes autorizadas para o primeiro piloto | pedagógico | antes da Sprint 3 | proposta pronta |
| `D-02` | política institucional para imagem enviada por professora | privacidade/produto | antes da Sprint 1 | proposta pronta |
| `D-03` | provedor de identidade OIDC | infraestrutura | início da Sprint 1 | spike definido |
| `D-04` | provedores permitidos para visão, imagem e voz | produto/privacidade | antes da Sprint 3 | proposta pronta |
| `D-05` | retenção de originais, rascunhos, eventos e observações | privacidade | antes da Sprint 1 | proposta pronta |
| `D-06` | aparelhos/Android mínimos suportados | Android/produto | antes da Sprint 2 | proposta baseada no app atual |
| `D-07` | limiar mensal e por história para IA | produto/financeiro | antes da Sprint 3 | limites técnicos prontos; valor pendente |

Recomendações e campos de aceite estão em `SPRINT_0_DECISION_PROPOSALS.md`.

## Critério de saída

A Sprint 0 termina apenas quando:

1. todos os itens da tabela estiverem `APROVADO`;
2. cada decisão bloqueante tiver escolha, responsável e data;
3. o protótipo permitir concluir as cinco tarefas sem configuração técnica;
4. a história da maçã passar pelos validadores técnico, pedagógico, de segurança e acessibilidade;
5. os itens adiados estiverem registrados como backlog, sem afirmação de que foram implementados.
