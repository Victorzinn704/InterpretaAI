# Índice da documentação

Este diretório é a fonte de verdade textual do InterpretaAI. O código e os testes provam o que está
implementado; `dist/` contém os únicos artefatos oficiais de entrega.

## Para entender o produto

1. [Resumo em 10 linhas](RESUMO_10_LINHAS.md) — apresentação rápida.
2. [Estado auditado do MVP](MVP_STATUS.md) — implementado, demonstrado e pendente.
3. [Critérios do hackathon](HACKATHON_CRITERIA.md) — aderência, evidências e riscos.
4. [Proposta em PDF](../dist/InterpretaAI-Proposta-MVP.pdf) — documento profissional de 10 páginas.
5. [Galeria visual](GALLERY.md) — jornada real, responsividade e comparação antes/depois.

## Para desenvolvimento e operação

- [Arquitetura](ARCHITECTURE.md) — fronteiras Android, servidor, IA, visão e métricas.
- [Servidor local](LOCAL_MVP_SERVER.md) — Qwen, Kokoro, Spring, túnel e migração para Oracle.
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
