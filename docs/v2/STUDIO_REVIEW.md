# Estúdio docente — revisão editorial 2.0

Estado em 18/09/2026: interface e BFF estão ativos em `https://interpretaai.deskimperial.online/studio/`. Sem sessão, o Estúdio redireciona ao Keycloak e a API adulta retorna 401. Login, callback, sessão Spring, BFF e vínculo da escola piloto passaram no HTTPS público.

O fluxo de [sala móvel](MOBILE_CLASSROOM_DEMO.md) acrescenta importação de até 40 nomes, código de
aula, mapa de carteiras e vínculo temporário aluno–tablet. A Home infantil não exibe mais entrada
docente; a preparação do aparelho abre por deep link próprio e continua protegida por PIN.

## Jornada implementada

A professora entra via OIDC institucional, escolhe uma escola do seu vínculo ativo e pode gerar um código temporário para conectar um tablet somente a uma turma vinculada. O código aparece uma vez, não é armazenado no navegador e o BFF mantém o token OIDC fora do JavaScript. Depois, abre um rascunho e confere cenas, diálogos, palavra-alvo, procedência e cada variante privada de imagem/áudio. Só consegue aprovar depois de confirmar todos os arquivos carregados. A aprovação envia revisão, hash do rascunho e hashes das mídias ao servidor; o backend repete autorização e validações. Publicar é uma segunda ação explícita. Depois, a professora escolhe uma turma vinculada e disponibiliza a versão publicada. **Publicar não atribui à turma; atribuir não comprova que os tablets prepararam o cache.** Após a atribuição, o painel consulta separadamente aparelhos compatíveis pareados e recibos de cache verificado nas últimas 24 horas, com atualização manual. A professora também pode retirar o envio com confirmação explícita; isso não apaga a publicação. Quando não há histórias, a interface mostra estado vazio verdadeiro, não conteúdo de demonstração.

## Fronteira de segurança

O navegador usa sessão HTTP segura, CSRF e rotas `/studio/api/**`; tokens OIDC, chaves dos provedores e caminhos privados de objetos não entram no JavaScript. A identidade vem do `sub` OIDC e o papel/escola do banco, nunca de campos enviados pelo navegador. A interface é auxiliar: confirmar caixas não contorna o portão do backend. Conteúdo de histórias entra no DOM via `textContent`; prévias são autenticadas e `no-store`. O Estúdio fica **desligado por padrão** (`STUDIO_ENABLED=false`) e falha ao iniciar se habilitado sem OIDC.

O piloto usa Keycloak 26.7.4 com banco próprio, realm `interpretaai` e cliente Spring `studio`. Segredos ficam em arquivos root-only fora do Git; o Nginx encaminha `/auth/**`, oculta console/master realm e preserva os cabeçalhos HTTPS. `STUDIO_COOKIE_SECURE=true` permanece ativo. Um provedor de SME pode ser federado ao Keycloak depois, sem mudar os vínculos internos.

## Evidência e limites

- `./gradlew :server:test` cobre segurança habilitada/desabilitada, isolamento por escola/autoria, CSRF, aprovação/publicação, atribuição idempotente, retirada e recibo de preparo; hash errado, recibo antigo e aparelho revogado não contam. OIDC é simulado.
- `uv run --with playwright python3 tools/audit-studio-review.py` confere layout e os fluxos código de pareamento e revisão → publicação → turma em 390×844, 800×1280 e 1440×1000. As capturas em `output/screenshots/v2-studio-review-fixture/` usam **fixture sintética**, não pareamento, geração ou Oracle reais.
- Não há edição, pedido de ajuste/rejeição, relatório docente nem geração de pacote pelo Codex nesta interface. A conexão geração → revisão → publicação → atribuição → cache offline ainda está pendente de teste integrado em aparelhos escolares e servidor real.
- O recibo é afirmação de um aplicativo autenticado após verificar arquivos locais; não prova permanência futura do cache, execução pela criança ou aprendizado. A retirada remove o vínculo local na próxima sincronização online via `404` da reconfirmação, não por push; aparelhos offline e cenas já abertas em memória podem continuar temporariamente. O contador não é métrica infantil.
- Não houve teste com professoras ou crianças. A auditoria visual técnica não prova usabilidade humana.
- O PostgreSQL 17.9 aplicou V1–V20 em staging e produção; o Keycloak usa `interpretaai_keycloak`. As três conexões estão saudáveis.
- `verify-real-oidc.sh` comprovou código, token, issuer, audience e vínculo. `verify-studio-session.sh` percorreu o login público, callback, página autenticada e `/studio/api/me`. A conta piloto exige troca de senha no primeiro acesso.
- O percurso geração → publicação → pareamento → cache offline → retirada ainda precisa de tablet físico e professora real.

### Capturas de auditoria (fixture sintética)

| Celular | Tablet | Desktop |
|---|---|---|
| ![Revisão no celular com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/mobile-review.png) | ![Revisão no tablet com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/tablet-review.png) | ![Revisão no desktop com conteúdo sintético](../../output/screenshots/v2-studio-review-fixture/desktop-review.png) |

Após a atribuição: [celular](../../output/screenshots/v2-studio-review-fixture/mobile-assigned.png), [tablet](../../output/screenshots/v2-studio-review-fixture/tablet-assigned.png), [desktop](../../output/screenshots/v2-studio-review-fixture/desktop-assigned.png). Com confirmação simulada: [celular](../../output/screenshots/v2-studio-review-fixture/mobile-confirmed.png), [tablet](../../output/screenshots/v2-studio-review-fixture/tablet-confirmed.png), [desktop](../../output/screenshots/v2-studio-review-fixture/desktop-confirmed.png). Após retirada simulada: [celular](../../output/screenshots/v2-studio-review-fixture/mobile-withdrawn.png), [tablet](../../output/screenshots/v2-studio-review-fixture/tablet-withdrawn.png), [desktop](../../output/screenshots/v2-studio-review-fixture/desktop-withdrawn.png). Todas são capturas sintéticas.

Pareamento sintético: [celular](../../output/screenshots/v2-studio-review-fixture/mobile-pairing.png), [tablet](../../output/screenshots/v2-studio-review-fixture/tablet-pairing.png), [desktop](../../output/screenshots/v2-studio-review-fixture/desktop-pairing.png).
