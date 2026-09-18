# Verificação pública da infraestrutura informada como Oracle — 17/09/2026

Endereço informado pelo responsável: `https://interpretaai.deskimperial.online`. Consultas
somente de leitura, sem token e sem alteração da VM:

| Rota | HTTP | Conteúdo observado | Conclusão limitada |
|---|---:|---|---|
| `/actuator/health` | 200 | `{"status":"UP"}` | aplicação pública responde |
| `/api/v1/gateway/status` | 200 | `state=HOT`, `provider=ollama`, `scenePack.version=v2` | gateway infantil v1 está ativo naquele instante |
| `/api/v2/identity/me` sem token | 404 | rota indisponível | autoria/identidade v2 não está exposta publicamente |
| `/studio/` sem sessão | 404 | rota indisponível | Estúdio docente local ainda não foi publicado |

Em nova verificação somente de leitura, o alias SSH `joao-oracle` conectou à VM de aplicação:
`interpretaai-server`, `interpretaai-kokoro` e `wg-quick@wg0` estavam ativos. A consulta direta
na interface interna `172.18.0.1:8088/api/v2/identity/me` também retornou **404**. Assim, a falta
da rota v2 não é apenas uma regra do proxy público: a aplicação atualmente executada não atende
essa rota. Naquela checagem não se inferiu a versão do JAR, banco, Flyway, issuer OIDC,
Object Storage ou backup.
O proxy atual é um container **Nginx**; os arquivos Caddy em `deploy/oracle/` descrevem um
empacotamento alternativo antigo e não devem ser aplicados nessa VM sem redesenho. O proxy
rejeitou o user-agent padrão do Python com 403; nova consulta com identificação explícita em 17/09/2026 confirmou `health=200`, `identity/me=404` e `studio/=404`. O [verificador v2](../../deploy/oracle/verify-v2-public.sh) usa user-agent
compatível e agora aponta corretamente a ausência da rota com 404.

Próximos portões, em ordem: configurar/ensaiar o OIDC institucional e PostgreSQL em staging;
implantar explicitamente o JAR v2 com rollback; habilitar a rota `/api/v2/*` no proxy somente
depois de a origem atender à rota; teste
anônimo=401 e autenticado=200 em `/api/v2/identity/me`; migração Flyway e teste em PostgreSQL;
somente então ativar workers de autoria com modelo avaliado e fonte pedagógica aprovada. Não
alterar o Nginx nem instalar o exemplo Caddy antes de OIDC funcional e backup/rollback.
A [auditoria read-only de staging](ORACLE_STAGING_GATE.md) confirma PostgreSQL pela WireGuard,
ausência das flags OIDC/Estúdio no ambiente atual e os portões de recuperação ainda pendentes.
