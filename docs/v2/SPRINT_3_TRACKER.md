# Sprint 3 — autoria assistida, RAG e Codex

## Estado geral

`EM EXECUÇÃO`. A fronteira segura da autoria e o catálogo de fontes existem; geração real e revisão
no Estúdio ainda não formam um percurso completo. Nenhuma fonte está marcada como aprovada, portanto
o recuperador de produção retorna zero evidências — comportamento deliberado, não uma alegação de RAG
operacional.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Manifesto versionado de fontes | IMPLEMENTADO LOCALMENTE | três fontes próprias têm versão, escopo e SHA-256; `tools/validate-guidance-sources.py` confirma integridade | aprovação nominal de produto/pedagogia/acessibilidade |
| Recuperação pedagógica | IMPLEMENTADO, SEM FONTES ATIVAS | `GuidanceSourceCatalog` valida hash, aprovação, licença, vigência, faixa, objetivo, coleção e escopo antes da busca lexical; testes bloqueiam candidato, adulteração e licença pendente | aprovar fontes e criar conjunto de perguntas/fontes esperadas |
| Fila de autoria | IMPLEMENTADO PARCIALMENTE | solicitação idempotente, persistência, lease e retry sobrevivem ao processo; worker apenas valida e entrega à próxima etapa | conectar recuperação, planejamento e validação do pacote |
| Contrato e fronteira do planejador LangChain4j | IMPLEMENTADO LOCALMENTE, NÃO CONECTADO À FILA | `AuthoringPlanContract` define saída JSON fechada e `AuthoringPlanService` chama provedor só após recuperar fonte aprovada e no escopo; testes com provedor falso rejeitam citação inventada, palavra alterada, mecânica fora do pedido, campo extra, JSON concatenado e linguagem punitiva | configurar provedor docente permitido, persistir plano, executar avaliação e ligar worker/Estúdio |
| Adaptadores visão/imagem/voz | PENDENTE PARA AUTORIA | conversa e voz possuem adaptadores separados; não geram recursos da história | definir provedores permitidos, consentimento e orçamento |
| Executor Codex isolado | PENDENTE | fronteira e ferramentas permitidas estão documentadas | contêiner sem banco/publicação, credencial efêmera e testes de negação |
| Revisão no Estúdio | PROTÓTIPO | fluxo navegável e auditoria visual existem em `prototype/` | ligar estados reais, edição e aprovação humana |

## Invariantes já executáveis

- candidato, suspenso ou expirado nunca entra no contexto do modelo;
- SHA-256 divergente impede a inicialização do catálogo;
- fonte aprovada não pode conservar licença declarada como pendente;
- escopo institucional é filtro obrigatório tipado por `SCHOOL`/`TEACHER`/`NETWORK`, não mera
  coincidência de identificador nem instrução de prompt;
- evidência sempre carrega `sourceId`, versão e hash;
- busca lexical apenas ordena: ela não substitui autorização, validação nem aprovação docente.
- o plano é um rascunho não executável; somente `LearningStoryPack` aprovado e validado pode chegar
  ao tablet. Nenhum modelo real, geração de imagem ou executor Codex foi acionado neste corte.

## Comandos de evidência

```bash
python3 tools/validate-guidance-sources.py
./gradlew :server:test --tests '*GuidanceSourceCatalogTest'
./gradlew :server:test --tests '*AuthoringPlanServiceTest'
```

Não afirmar que a IA cria histórias fundamentadas enquanto o contador de fontes aprovadas continuar
em zero ou enquanto a saída do planejador não atravessar o validador do `LearningStoryPack`.
