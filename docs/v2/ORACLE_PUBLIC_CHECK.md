# Verificação pública e operacional da Oracle — 18/09/2026

Endereço público: `https://interpretaai.deskimperial.online`.

## Produção observada após a promoção

| Evidência | Resultado |
|---|---|
| `/actuator/health` | HTTP 200, `UP` |
| `/actuator/info` | revisão `b498663df1c571526c31dfc23ed29a498a7785e6`, geração `v2` |
| `/api/v1/gateway/status` | HTTP 200, `HOT`, Ollama disponível, ScenePack v2 com 7 cenas |
| `/api/v2/identity/me` sem token | HTTP 403; API adulta falha fechada |
| `/studio/` sem sessão | HTTP 403; Estúdio permanece desligado |
| serviço Spring | ativo em `172.18.0.1:8088` |
| proxy | Nginx em container, saudável, TLS público ativo |
| PostgreSQL de produção | Flyway V20, 28 tabelas públicas |

O JAR v2 foi promovido em 18/09/2026 após validação privada. A cópia imediatamente anterior e o
ambiente ficaram em `/opt/interpretaai/releases/20260918T043700Z-pre-v2`; a primeira troca v1→v2
permanece em `20260918T042926Z-pre-v2`. As migrações V11–V20 são aditivas; restaurar
somente o JAR não remove tabelas, portanto qualquer rollback de dados deve partir do backup.

## Staging privado

`interpretaai-server-v2-staging.service` está ativo e habilitado em `127.0.0.1:8188`, usando o banco
separado `interpretaai_v2_staging`. O verificador confirmou health 200, revisão correta, API adulta
403 e Estúdio 403 com OIDC desligado. O banco aplicou V1–V20 com sucesso.

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

## Limite atual

Infraestrutura, JAR, banco e conexão Android podem ser publicados, mas o percurso docente ainda não
está liberado. Falta escolher e configurar um provedor OIDC real, cadastrar professora/escola/turma,
habilitar o Estúdio e executar o percurso em tablet físico. Nenhuma fonte do RAG está aprovada e os
workers de mídia/autoria continuam desligados. O 403 público é o estado seguro esperado até esses
portões serem cumpridos.
