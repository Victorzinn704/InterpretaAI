# Verificação pública e operacional da Oracle — 18/09/2026

Endereço público: `https://interpretaai.deskimperial.online`.

## Produção observada após a promoção

| Evidência | Resultado |
|---|---|
| `/actuator/health` | HTTP 200, `UP` |
| `/actuator/info` | revisão `b498663df1c571526c31dfc23ed29a498a7785e6`, geração `v2` |
| `/api/v1/gateway/status` | HTTP 200, `HOT`, Ollama disponível, ScenePack v2 com 7 cenas |
| `/api/v2/identity/me` sem token | HTTP 401; API adulta exige OIDC |
| `/studio/` sem sessão | HTTP 302 para login OIDC; Estúdio ativo |
| sessão real do Estúdio | login, callback, página autenticada, BFF e vínculo `school_pilot`: PASS |
| emissor OIDC | Keycloak 26.7.4 em `/auth/realms/interpretaai`, health `UP`; console administrativo oculto no Nginx |
| serviço Spring | ativo em `172.18.0.1:8088` |
| proxy | Nginx em container, saudável, TLS público ativo |
| PostgreSQL de produção | Flyway V20, 28 tabelas públicas |
| PostgreSQL de identidade | banco exclusivo `interpretaai_keycloak`, conexão privada |

O JAR v2 foi promovido em 18/09/2026 após validação privada. A cópia imediatamente anterior e o
ambiente ficaram em `/opt/interpretaai/releases/20260918T043700Z-pre-v2`; a primeira troca v1→v2
permanece em `20260918T042926Z-pre-v2`. As migrações V11–V20 são aditivas; restaurar
somente o JAR não remove tabelas, portanto qualquer rollback de dados deve partir do backup.

## Staging privado

`interpretaai-server-v2-staging.service` está ativo e habilitado em `127.0.0.1:8188`, usando o banco
separado `interpretaai_v2_staging`. O verificador confirmou health 200, revisão correta, anônimo 401,
redirecionamento do Estúdio, autorização OIDC real e vínculo restrito à escola piloto. O banco
aplicou V1–V20 com sucesso.

Produção e staging conectam diretamente ao PostgreSQL privado em `10.220.10.10:5432`. O PgBouncer
em `6432` lista apenas `deskimperial`; não deve ser usado pelo InterpretaAI até receber configuração
e teste próprios. Ollama continua privado em `10.220.10.10:11434`; Kokoro fica no host da aplicação
em `127.0.0.1:8091`.

## Recuperação e backup

`verify-db-restore-drill.sh` restaurou o backup diferencial de 18/09 em volume efêmero, reproduziu
WAL pelo Object Storage, iniciou PostgreSQL 17.9 sem porta publicada, consultou quatro bancos e
removeu os recursos temporários. Antes da promoção foi criado outro backup diferencial:

- label: `20260912-020006F_20260918-042536D`;
- janela UTC: `04:25:36`–`04:29:10`;
- WAL: `0000000100000003000000D0`–`0000000100000003000000D1`;
- resultado: concluído sem erro.

## Identidade ativa e limite atual

O piloto usa Keycloak próprio e gratuito na VM Oracle. O realm `interpretaai`, o cliente confidencial
`studio`, a audiência `interpretaai-api`, a conta `professor-piloto`, a escola e a turma piloto estão
ativos. `verify-real-oidc.sh` comprovou authorization code, token, issuer, audience e vínculo no
PostgreSQL. `verify-studio-session.sh` percorreu o HTTPS público até a sessão Spring e consultou
`/studio/api/me`. A senha piloto continua temporária e exige troca no primeiro acesso.

Keycloak é o emissor do piloto e também permite federar Google Workspace, Microsoft Entra ID ou o
provedor de uma SME mais tarde sem alterar os IDs internos do InterpretaAI. Ainda faltam uma parceria
institucional, contas reais, revisão da política de identidade e o teste completo em tablet físico.
Nenhuma fonte do RAG está aprovada e os workers de mídia/autoria continuam desligados.

Durante a implantação, uma checagem remota imprimiu variáveis do PostgreSQL. As sete credenciais
afetadas foram rotacionadas imediatamente; PostgreSQL, PgBouncer, exportador, pgBackRest e API Desk
foram recriados e voltaram saudáveis. Os valores e arquivos temporários não foram preservados na
documentação ou no repositório.
