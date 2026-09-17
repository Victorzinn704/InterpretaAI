# Estúdio docente — revisão editorial 2.0

Estado em 17/09/2026: interface e BFF implementados e testados **localmente**. A Oracle pública ainda retorna 404 em `/studio/` e `/api/v2/identity/me`; nenhum deploy foi feito nesta entrega.

## Jornada implementada

A professora entra via OIDC institucional, escolhe uma escola do seu vínculo ativo, abre um rascunho e confere cenas, diálogos, palavra-alvo, procedência e cada variante privada de imagem/áudio. Só consegue aprovar depois de confirmar todos os arquivos carregados. A aprovação envia revisão, hash do rascunho e hashes das mídias ao servidor; o backend repete autorização e validações. Publicar é uma segunda ação explícita. Depois, a professora escolhe uma turma vinculada e disponibiliza a versão publicada. **Publicar não atribui à turma; atribuir não comprova que os tablets prepararam o cache.** Após a atribuição, o painel consulta separadamente aparelhos compatíveis pareados e recibos de cache verificado nas últimas 24 horas, com atualização manual. Quando não há histórias, a interface mostra estado vazio verdadeiro, não conteúdo de demonstração.

## Fronteira de segurança

O navegador usa sessão HTTP segura, CSRF e rotas `/studio/api/**`; tokens OIDC, chaves dos provedores e caminhos privados de objetos não entram no JavaScript. A identidade vem do `sub` OIDC e o papel/escola do banco, nunca de campos enviados pelo navegador. A interface é auxiliar: confirmar caixas não contorna o portão do backend. Conteúdo de histórias entra no DOM via `textContent`; prévias são autenticadas e `no-store`. O Estúdio fica **desligado por padrão** (`STUDIO_ENABLED=false`) e falha ao iniciar se habilitado sem OIDC.

Para ativar em staging, configurar um cliente OIDC Spring `studio` com issuer, client ID e secret guardados fora do Git, `OIDC_ENABLED=true` e `STUDIO_ENABLED=true`. O proxy HTTPS precisa encaminhar `/studio/**`, `/login`, `/oauth2/authorization/**` e `/login/oauth2/code/**`, com cabeçalhos de encaminhamento corretos. `STUDIO_COOKIE_SECURE=true` deve permanecer em HTTPS. O provedor, callback, Nginx e PostgreSQL reais ainda exigem teste ponta a ponta antes de abrir a docentes.

## Evidência e limites

- `./gradlew :server:test` cobre segurança habilitada/desabilitada, isolamento por escola/autoria, CSRF, aprovação/publicação, atribuição idempotente e recibo de preparo; hash errado, recibo antigo e aparelho revogado não contam. OIDC é simulado.
- `uv run --with playwright python3 tools/audit-studio-review.py` confere layout e o fluxo revisão → publicação → turma em 390×844, 800×1280 e 1440×1000. As capturas em `output/screenshots/v2-studio-review-fixture/` usam **fixture sintética**, não geração real nem Oracle.
- Não há edição, pedido de ajuste/rejeição, relatório docente nem geração de pacote pelo Codex nesta interface. A conexão geração → revisão → publicação → atribuição → cache offline ainda está pendente de teste integrado em aparelhos escolares e servidor real.
- O recibo é afirmação de um aplicativo autenticado após verificar arquivos locais; não prova permanência futura do cache, execução pela criança ou aprendizado. A revogação ainda não remove automaticamente a cópia local por manifesto; não usar o contador como métrica infantil.
- Não houve teste com professoras ou crianças. A auditoria visual técnica não prova usabilidade humana.
- Um smoke em PostgreSQL 17.11 local e descartável aplicou as 20 migrações até V20, confirmou a tabela de recibos e iniciou o JAR. Isso não substitui teste de consulta com dados em PostgreSQL nem valida sessão OIDC e PostgreSQL da Oracle.

### Capturas de auditoria (fixture sintética)

| Celular | Tablet | Desktop |
|---|---|---|
| ![Revisão no celular com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/mobile-review.png) | ![Revisão no tablet com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/tablet-review.png) | ![Revisão no desktop com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/desktop-review.png) |

Após a atribuição: [celular](../../output/screenshots/v2-studio-review-fixture/mobile-assigned.png), [tablet](../../output/screenshots/v2-studio-review-fixture/tablet-assigned.png), [desktop](../../output/screenshots/v2-studio-review-fixture/desktop-assigned.png). Com confirmação simulada: [celular](../../output/screenshots/v2-studio-review-fixture/mobile-confirmed.png), [tablet](../../output/screenshots/v2-studio-review-fixture/tablet-confirmed.png), [desktop](../../output/screenshots/v2-studio-review-fixture/desktop-confirmed.png). Todas são capturas sintéticas.
