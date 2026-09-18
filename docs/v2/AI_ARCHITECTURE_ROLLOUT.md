# Implantação da arquitetura de IA

Estado operacional em 18/09/2026. Este documento distingue o que está ativo, o que está instalado
mas fechado e o que depende de avaliação ou decisão humana.

## Mapa atual

| Camada | Estado na Oracle | Decisão operacional |
|---|---|---|
| conversa infantil | Ollama/Qwen 1.5B e Kokoro ativos; fallback local no Android | manter fora de RAG e de agentes |
| banco | PostgreSQL 17.9, Flyway V20 | fonte durável; acesso pela rede privada na porta 5432 |
| cache Android | Room + arquivos privados, hash e troca atômica | fonte de verdade offline do tablet |
| cache Java | Caffeine para sessão, rate limit, áudio e replay curto | adequado enquanto há uma instância da API |
| Redis | container compartilhado do Desk existe, sem uso pelo InterpretaAI | não compartilhar; criar instância dedicada somente com necessidade medida |
| autoria | filas e planejador LangChain4j instalados, workers desligados | ativar em staging após curadoria do RAG |
| RAG | catálogo lexical e três fontes candidatas | zero fonte ativa até aprovação nominal e licença preenchida |
| Codex | fronteira documentada, executor ausente | opcional, assíncrono e isolado; exige conta/orçamento e testes de negação |
| LangGraph4j | não instalado | adiar até existir ciclo de revisão que justifique grafo persistente |
| identidade | Keycloak 26.7.4, banco próprio e realm `interpretaai` ativos | federar provedor institucional quando uma rede parceira o definir |
| Estúdio | OIDC, callback, sessão BFF e vínculo piloto ativos | liberar para uso real somente após teste humano e política institucional |

## Ordem de ativação

1. **Concluído:** implantar Keycloak próprio, cliente, callback HTTPS e professora piloto.
2. **Concluído:** inserir tenant, escola, turma, usuário e vínculo ativo em staging e produção.
3. **Concluído:** validar anônimo=401, token real, audience, vínculo e sessão BFF pelo HTTPS público.
4. Testar pareamento com um tablet físico Android.
5. Publicar uma história fixture, atribuir, baixar, verificar hash, abrir offline, confirmar cache e
   retirar a atribuição.
6. Aprovar fontes próprias do RAG com responsáveis diferentes para produto, pedagogia e
   acessibilidade; preencher licença, vigência e hash.
7. Ativar primeiro `AUTHORING_WORKER_ENABLED`; observar fila e validação sem modelo.
8. Ativar `AUTHORING_PLAN_WORKER_ENABLED` com modelo explícito e conjunto de avaliação.
9. Liberar mídia/sanitização somente com armazenamento privado e ensaio de recuperação dos objetos.
10. Considerar Redis, Codex e LangGraph4j apenas pelos critérios abaixo.

## RAG e LangChain4j

A primeira etapa permanece determinística: autorização, escopo, vigência, licença e hash são
filtrados antes da busca lexical. Cada resposta deve preservar `sourceId`, `sourceVersion` e
`contentHash`. O modelo recebe somente evidências aprovadas e produz `AuthoringPlan`, que ainda não
é um pacote executável.

Antes de ativar, criar casos com pergunta, faixa, objetivo e fontes esperadas. Medir recuperação,
citação correta, isolamento entre escolas e recusa sem evidência. PostgreSQL full-text com índice
GIN é o próximo armazenamento previsto. `pgvector` só entra se a avaliação mostrar ganho de recall;
ele não está instalado no cluster atual.

## Guardrails obrigatórios

- **entrada:** limites de tamanho, tipos permitidos, remoção de metadados e proibição de dados
  identificáveis de crianças;
- **recuperação:** somente fontes aprovadas, licenciadas, vigentes e dentro do escopo autorizado;
- **saída:** JSON Schema, referências existentes, componentes do catálogo, limites de fala e
  linguagem segura;
- **publicação:** revisão humana, aprovação separada, hash imutável e nova validação no servidor e
  no Android;
- **execução:** timeout, zero retry de modelo no caminho quente, concorrência limitada, circuit
  breaker, orçamento e auditoria sem conteúdo sensível.

As validações próprias continuam sendo o controle principal. Guardrails experimentais de biblioteca
podem ser adicionados como defesa complementar depois de testes de regressão; não substituem RBAC,
schemas nem aprovação docente.

## Quando adicionar Redis

Adicionar um `interpretaai-redis` dedicado quando houver duas instâncias da API ou rate limit que
precise sobreviver à troca de processo. Usos permitidos: contador distribuído, pareamento temporário,
replay curto de idempotência e cache de recuperação identificado pelo hash do manifesto. O registro
durável continua no PostgreSQL.

Requisitos: rede interna, nenhuma porta pública, ACL/segredo fora do Git, TTL obrigatório, limite de
memória, política LFU/LRU e métricas. Não guardar áudio, transcrição, imagem, identidade infantil,
token OIDC ou única cópia de qualquer dado.

## Quando adicionar Codex

O executor será processo ou container separado e consumirá apenas trabalhos assíncronos de autoria.
Recebe workspace temporário, ferramentas permitidas, tempo/memória limitados e credencial efêmera.
Não recebe banco, publicação, relatórios infantis ou mídia bruta. Sua saída volta como rascunho para
os mesmos validadores e para a revisão da professora. Mantê-lo desligado enquanto não houver conta,
orçamento e um caso que exija manipulação agentiva de arquivos.

## Quando adicionar LangGraph4j

Introduzir somente se o fluxo exigir ciclos persistentes como recuperar → gerar → validar → pedir
ajuste → recuperar novamente. O checkpoint deve usar PostgreSQL e respeitar os estados de job já
existentes. Aprovar e publicar continuam ações explícitas do backend, fora do grafo do modelo.

Grafo candidato: `validate_request → sanitize_media → retrieve_guidance → draft_plan →
validate_contract → teacher_review`; a revisão pode retornar ao retrieval, enquanto a aceitação
termina em `READY_FOR_APPROVAL`.
