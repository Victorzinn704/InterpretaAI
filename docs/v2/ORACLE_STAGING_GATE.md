# Portão de staging da versão 2.0 na Oracle

Estado observado em 17/09/2026 (BRT), por consultas **somente de leitura**. Nenhum serviço,
container, banco, proxy ou arquivo remoto foi alterado.

| Evidência | Resultado | Limite da conclusão |
|---|---|---|
| `GET /actuator/health` público | 200 | v1 responde; não prova v2 |
| `GET /api/v2/identity/me` e `/studio/` públicos | 404 / 404 | v2 e Estúdio não estão publicados |
| Serviços João | Spring, Kokoro e WireGuard ativos; Nginx em container saudável | não valida OIDC |
| Banco | URL do serviço classificada como PostgreSQL `interpretaai` via WireGuard; container PostgreSQL saudável na Lohana | não prova restore ou migração v2 |
| Backup | pgBackRest com stanza saudável, 9 backups; último diferencial terminou sem erro em `2026-09-17T02:31:42Z` | presença/estado não provam restauração nem RPO do banco `interpretaai` |
| Recursos | João: 75 GB livres, 9,0 GiB RAM disponível; Lohana: 42 GB livres, 9,5 GiB disponível | instantâneo, não orçamento de carga |
| JAR em execução | `/opt/interpretaai/server.jar`, SHA-256 `afe30aa290c55994fbf0bf831755c47e34705b182955dbb141a54640b50456ea` | manifesto não expõe versão/commit e artefato não atende rota v2 |
| Ambiente do serviço | `DATABASE_URL`, usuário e senha presentes; `OIDC_ENABLED` e `STUDIO_ENABLED` ausentes | valores/segredos não foram lidos na saída |
| Cópias de JAR | `server-prev.jar` e `server-before-num-predict.jar` existem | não substituem backup/restauração do banco |
| Smoke v2 local | JAR isolado iniciou com PostgreSQL 17, Flyway V1–V20 e OIDC sintético; anônimo=401, Estúdio=302 e revisão do commit conferida | não prova login positivo, Oracle nem restauração |

## Sequência de liberação

1. **Identidade:** definir o provedor OIDC, issuer, audience, cliente `studio`, redirect HTTPS e
   usuário docente de teste com vínculo institucional no banco. Configurar segredos fora do Git.
   Sem isso, manter `OIDC_ENABLED=false` e `STUDIO_ENABLED=false`. Nesse estado, o servidor nega
   toda a API adulta `/api/v2/**` com 403; ela não herda o modo aberto de compatibilidade da v1.
   As rotas `/api/v2/devices/**` permanecem numa cadeia independente e também falham fechadas se
   o segredo de pareamento não estiver configurado.
2. **Isolamento e recuperação:** criar banco e armazenamento de mídia exclusivos de staging,
   obter backup verificável antes de migrar e ensaiar uma restauração. O container `pgbackrest`
   observado pertence à infraestrutura compartilhada; sua presença não comprova que o banco
   `interpretaai` esteja protegido. Registrar caminho e tempo de rollback do JAR **e dos dados**.
3. **Origem v2:** gerar JAR a partir de commit fixo, conferir SHA-256 e iniciar em porta separada,
   sem substituir `/opt/interpretaai/server.jar`. Aplicar Flyway apenas no banco de staging.
   Definir `INTERPRETAAI_RELEASE_REVISION` com o commit completo e conferir o mesmo valor em
   `/actuator/info`. Começar com workers de autoria, sanitização e pareamento desligados; abrir
   cada um após teste.
4. **Autenticação e proxy:** provar na origem que anônimo recebe 401 em
   `/api/v2/identity/me` e que uma professora de teste recebe 200 apenas na escola correta.
   Só então configurar uma rota HTTPS de staging no **Nginx existente**; não instalar o Caddy do
   pacote alternativo. Confirmar `X-Forwarded-Proto/Host` confiáveis, cookie Secure e CSRF.
5. **Percurso real:** no APK de teste, parear um aparelho, publicar uma versão aprovada, atribuir
   à turma, baixar e abrir offline, confirmar o cache, retirar o envio e verificar que o aparelho
   perde o vínculo após reconectar. Medir tempo e falhas; repetir em celular e tablet.
6. **Produção:** somente com o ensaio anterior aprovado, janela de mudança, backup/restauração
   verificáveis e rollback escrito. A troca de JAR não desfaz migrações Flyway por si só.

`deploy/oracle/verify-v2-origin.sh` exige o commit completo e compara a revisão publicada em
`/actuator/info`, além de conferir health, rejeição anônima e redirecionamento do Estúdio.
`deploy/oracle/verify-v2-public.sh` valida a rejeição anônima e, com token adulto injetado por
meio seguro, o contexto positivo sem imprimir identidade ou token. Hoje ele deve falhar com 404;
isso é um portão real, não erro a contornar no Nginx. O roteiro Caddy e o instalador antigo **não**
são procedimentos de atualização da VM ativa.

Pendências não resolvidas por infraestrutura: nenhuma fonte do RAG está aprovada, o estudo com
professoras está `NOT_RUN`, e a procedência de 15 ativos exige confirmação humana. Não afirmar
piloto pedagógico validado apenas porque a API ficou online.

O smoke reproduzível é `./tools/test-v2-staging-smoke.sh`. Ele usa somente loopback, cria e remove
PostgreSQL temporário e um provedor OIDC sintético que nunca emite tokens. Também integra
`./tools/check-delivery.sh --full`; ausência de ferramentas PostgreSQL é declarada como `SKIP`,
enquanto falha de migração, segurança, rota ou revisão reprova a entrega.

O `404` observado hoje na Oracle significa que o JAR ativo ainda não contém a v2. Em um JAR v2
com OIDC desligado, o resultado seguro esperado passa a ser `403`, e não uma rota adulta pública.
