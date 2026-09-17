# Estúdio docente — revisão editorial 2.0

Estado em 17/09/2026: interface e BFF implementados e testados **localmente**. A Oracle pública ainda retorna 404 em `/studio/` e `/api/v2/identity/me`; nenhum deploy foi feito nesta entrega.

## Jornada implementada

A professora entra via OIDC institucional, escolhe uma escola do seu vínculo ativo, abre um rascunho e confere cenas, diálogos, palavra-alvo, procedência e cada variante privada de imagem/áudio. Só consegue aprovar depois de confirmar todos os arquivos carregados. A aprovação envia revisão, hash do rascunho e hashes das mídias ao servidor; o backend repete autorização e validações. Publicar é uma segunda ação explícita. **Publicar não atribui à turma nem sincroniza os tablets.** Quando não há rascunhos, a interface mostra estado vazio verdadeiro, não conteúdo de demonstração.

## Fronteira de segurança

O navegador usa sessão HTTP segura, CSRF e rotas `/studio/api/**`; tokens OIDC, chaves dos provedores e caminhos privados de objetos não entram no JavaScript. A identidade vem do `sub` OIDC e o papel/escola do banco, nunca de campos enviados pelo navegador. A interface é auxiliar: confirmar caixas não contorna o portão do backend. Conteúdo de histórias entra no DOM via `textContent`; prévias são autenticadas e `no-store`. O Estúdio fica **desligado por padrão** (`STUDIO_ENABLED=false`) e falha ao iniciar se habilitado sem OIDC.

Para ativar em staging, configurar um cliente OIDC Spring `studio` com issuer, client ID e secret guardados fora do Git, `OIDC_ENABLED=true` e `STUDIO_ENABLED=true`. O proxy HTTPS precisa encaminhar `/studio/**`, `/login`, `/oauth2/authorization/**` e `/login/oauth2/code/**`, com cabeçalhos de encaminhamento corretos. `STUDIO_COOKIE_SECURE=true` deve permanecer em HTTPS. O provedor, callback, Nginx e PostgreSQL reais ainda exigem teste ponta a ponta antes de abrir a docentes.

## Evidência e limites

- `./gradlew :server:test` cobre segurança habilitada/desabilitada, isolamento por escola/autoria, CSRF e transições de aprovação/publicação com OIDC simulado.
- `uv run --with playwright python3 tools/audit-studio-review.py` confere layout e o bloqueio de aprovação em 390×844, 800×1280 e 1440×1000. As capturas em `output/screenshots/v2-studio-review-fixture/` usam **fixture sintética**, não geração real nem Oracle.
- Não há edição, pedido de ajuste/rejeição, atribuição à turma, relatório docente ou geração de pacote pelo Codex nesta interface. A conexão geração → revisão → publicação → cache offline ainda está pendente.
- Não houve teste com professoras ou crianças. A auditoria visual técnica não prova usabilidade humana.

### Capturas de auditoria (fixture sintética)

| Celular | Tablet | Desktop |
|---|---|---|
| ![Revisão no celular com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/mobile-review.png) | ![Revisão no tablet com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/tablet-review.png) | ![Revisão no desktop com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/desktop-review.png) |
