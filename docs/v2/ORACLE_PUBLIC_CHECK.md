# Verificação pública da infraestrutura informada como Oracle — 17/09/2026

Endereço informado pelo responsável: `https://interpretaai.deskimperial.online`. Consultas
somente de leitura, sem token e sem alteração da VM:

| Rota | HTTP | Conteúdo observado | Conclusão limitada |
|---|---:|---|---|
| `/actuator/health` | 200 | `{"status":"UP"}` | aplicação pública responde |
| `/api/v1/gateway/status` | 200 | `state=HOT`, `provider=ollama`, `scenePack.version=v2` | gateway infantil v1 está ativo naquele instante |
| `/api/v2/identity/me` sem token | 404 | rota indisponível | autoria/identidade v2 não está exposta publicamente |

O 404 é compatível com o `deploy/oracle/Caddyfile` versionado, que só encaminha `/api/v1/*`, mas
não demonstra a configuração instalada na VM. O endpoint público não revela versão do JAR, banco,
Flyway, issuer OIDC, Object Storage ou política de backup. O relato de que a origem é Oracle não
foi confirmado por SSH ou console OCI nesta verificação. O proxy rejeitou o user-agent padrão do
Python com 403; o [verificador v2](../../deploy/oracle/verify-v2-public.sh) usa user-agent
compatível e agora aponta corretamente a ausência da rota com 404.

Próximos portões, em ordem: inspeção remota somente de leitura da configuração instalada;
configuração/ensaio do OIDC institucional; habilitação opt-in de `/api/v2/*` no Caddy; teste
anônimo=401 e autenticado=200 em `/api/v2/identity/me`; migração Flyway e teste em PostgreSQL;
somente então ativar workers de autoria com modelo avaliado e fonte pedagógica aprovada. Não
habilitar `Caddyfile.v2.example` antes de OIDC funcional.
