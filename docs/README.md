# Índice da documentação

Este diretório é a fonte de verdade textual do InterpretaAI. O código e os testes provam o que está
implementado; `dist/` contém os únicos artefatos oficiais de entrega.

## Para entender o produto

1. [Resumo em 10 linhas](RESUMO_10_LINHAS.md) — apresentação rápida.
2. [Origem no HACKTUDO 2026](ORIGEM_HACKTUDO.md) — contexto, desafio e transparência.
3. [Estado auditado do MVP](MVP_STATUS.md) — implementado, demonstrado e pendente.
4. [Auditoria do regulamento](REGULAMENTO_HACKTUDO_2026.md) — regra por regra, veredito e bloqueios.
5. [Critérios do hackathon](HACKATHON_CRITERIA.md) — aderência, evidências e riscos.
6. [Autoria, IA e prevenção de plágio](AUTORIA_ORIGINALIDADE_E_IA.md) — defesa de inovação e limites.
7. [Créditos de terceiros](../THIRD_PARTY_NOTICES.md) — bibliotecas, modelos, termos e downloads.
8. [Proveniência dos ativos](ASSET_PROVENANCE.md) — hashes e confirmações da equipe.
9. [Auditoria final](FINAL_MVP_AUDIT.md) — parecer sincero, evidências e ressalvas de entrega.
10. [Proposta em PDF](../dist/InterpretaAI-Proposta-MVP.pdf) — documento profissional de 10 páginas.
11. [Galeria visual](GALLERY.md) — jornada real, responsividade e comparação antes/depois.
12. [Fluxo pedagógico fechado](FLUXO_PEDAGOGICO_FECHADO.md) — fundamento, estados, falhas e lugar da IA.
13. [Linguagem infantil e elenco](CHILD_LANGUAGE_AND_CAST_AUDIT.md) — LÉIA, Alfa, falas naturais e novas cenas.
14. [Gibi situacional da chuva](SITUATIONAL_RAIN_COMIC.md) — episódio independente, imagens, prompts e limite narrativo.
15. [Curadoria visual dos gibis](COMIC_IMAGE_CURATION.md) — foco visual, progressão LEIA e critérios de aprovação.
16. [Fluxo único para histórias e telas de gibi](COMIC_STORY_WORKFLOW.md) — fonte de verdade, integração e rotina Git.

## Para desenvolvimento e operação

- [Projeto de arquitetura 2.0](v2/README.md) — Estúdio do Professor, autoria Codex/RAG,
  `LearningStoryPack`, cache automático, relatórios e plano de sprints; tudo ainda classificado como
  projeto futuro até implementação e evidência.
- [Arquitetura](ARCHITECTURE.md) — fronteiras Android, servidor, IA, visão e métricas.
- [Gateway de IA e caminho quente](AI_GATEWAY_HOT_PATH.md) — latência, aquecimento, LangChain4j, LangGraph4j, RAG e WebSocket.
- [Pesquisa para piloto nos GETs do Rio](GET_RJ_TABLET_PILOT_RESEARCH.md) — tablets, currículo 1º–5º, sala/avatar, quadro e modo foco.
- [Pedido de informação sobre tablets GET](GET_TABLET_INFORMATION_REQUEST.md) — texto pronto para confirmar modelo, Android e gestão sem identificadores.
- [Escopo pedagógico do 1º ao 5º ano](PEDAGOGICAL_SCOPE_1_TO_5.md) — matriz curricular, evidência atual e `ActivityPacks` futuros.
- [Arquitetura on/off](ONLINE_OFFLINE_ARCHITECTURE.md) — idempotência, persistência, circuito, outbox, retry e árvore de roteamento.
- [Auditoria de resiliência](RESILIENCE_AND_HOT_PATH_AUDIT.md) — requisito, implementação, evidência e limite atual.
- [Servidor local](LOCAL_MVP_SERVER.md) — Qwen, Kokoro, Spring, túnel e migração para Oracle.
- [Pacote Oracle](../deploy/oracle/README.md) — loopback, systemd, Caddy/HTTPS e smoke test da VM.
- [Contrato atual da API de voz](VOICE_API.md) — entrada, saída, limites e fallback.
- [Dados e privacidade](DATA_AND_PRIVACY.md) — inventário real, controles e riscos de piloto.
- [Roteiro da demonstração](DEMO_RUNBOOK.md) — preparação, narrativa e contingência.
- [Servidor LEIA](../server/README.md) — execução e provedores do módulo Java.
- [Modo quiosque](KIOSK.md) — fixação comum, Device Owner, Não Perturbe e saída administrativa.
- [Checklist de piloto](PILOT_CHECKLIST.md) — validação antes de uso com crianças.
- [Contrato de sincronização do piloto](SYNC_API_PROPOSAL.md) — atribuições, outbox de eventos,
  agregados e limites institucionais.

## Vocabulário de status

- **Implementado:** existe no código e tem evidência de teste ou inspeção.
- **Demonstrado:** funcionou no ambiente temporário da apresentação, sem garantia operacional.
- **Futuro:** arquitetura ou proposta; não deve ser apresentada como funcional.

## Regras de manutenção

- Atualize `MVP_STATUS.md` sempre que uma entrega mudar de status.
- Não descreva Gemini, Google Cloud, Oracle ou painel institucional da secretaria como ativos sem
  smoke test atual; diferencie a API agregada validada localmente da interface futura.
- Não publique métricas infantis como precisão, nota, ranking ou diagnóstico.
- Atualize o inventário de dados quando um novo campo, provedor ou destino for criado.
- Ao alterar o PDF, regenere o DOCX, renderize as 10 páginas e inspecione todas.
- Ao alterar o APK, execute testes, gere novo SHA-256 e atualize a cópia do Desktop.
- Antes de commitar artefatos, execute `./tools/check-delivery.sh --full`.
- Antes do pitching, conclua os itens humanos bloqueantes em `REGULAMENTO_HACKTUDO_2026.md`.
