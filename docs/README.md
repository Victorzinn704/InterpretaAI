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

## Para desenvolvimento e operação

- [Arquitetura](ARCHITECTURE.md) — fronteiras Android, servidor, IA, visão e métricas.
- [Gateway de IA e caminho quente](AI_GATEWAY_HOT_PATH.md) — latência, aquecimento, LangChain4j, LangGraph4j, RAG e WebSocket.
- [Pesquisa para piloto nos GETs do Rio](GET_RJ_TABLET_PILOT_RESEARCH.md) — tablets, currículo 1º–5º, sala/avatar, quadro e modo foco.
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
- [Contrato futuro de sincronização](SYNC_API_PROPOSAL.md) — proposta ainda não implementada.

## Vocabulário de status

- **Implementado:** existe no código e tem evidência de teste ou inspeção.
- **Demonstrado:** funcionou no ambiente temporário da apresentação, sem garantia operacional.
- **Futuro:** arquitetura ou proposta; não deve ser apresentada como funcional.

## Regras de manutenção

- Atualize `MVP_STATUS.md` sempre que uma entrega mudar de status.
- Não descreva Gemini, Google Cloud, Oracle ou secretaria como ativos sem smoke test atual.
- Não publique métricas infantis como precisão, nota, ranking ou diagnóstico.
- Atualize o inventário de dados quando um novo campo, provedor ou destino for criado.
- Ao alterar o PDF, regenere o DOCX, renderize as 10 páginas e inspecione todas.
- Ao alterar o APK, execute testes, gere novo SHA-256 e atualize a cópia do Desktop.
- Antes de commitar artefatos, execute `./tools/check-delivery.sh --full`.
- Antes do pitching, conclua os itens humanos bloqueantes em `REGULAMENTO_HACKTUDO_2026.md`.
