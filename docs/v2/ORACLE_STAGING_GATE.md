# Portão de staging da versão 2.0 na Oracle

Estado inicial observado em 17/09/2026 (BRT), por consultas **somente de leitura**. A seção
"Registro de execução" documenta as alterações posteriores e separa staging de produção.

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
| Smoke v2 local | JAR isolado iniciou com PostgreSQL 17, Flyway V1–V20 e OIDC sintético; anônimo=401, JWT assinado=200 somente no vínculo ativo, Estúdio=302 e revisão conferida | prova o adaptador local, não o provedor real, a Oracle nem restauração |

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
   O PgBouncer compartilhado observado em 18/09/2026 roteia somente `deskimperial`; usar a porta
   privada 5432 no staging até adicionar e validar uma entrada explícita para o InterpretaAI.
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
O terceiro argumento `foundation` valida uma origem privada com OIDC e Estúdio desligados: espera
403 na API adulta e no Estúdio. Esse modo serve para provar JAR, Flyway e banco sem abrir acesso;
não substitui o modo padrão `oidc` nem autoriza uso por professoras.
`deploy/oracle/verify-v2-public.sh` valida a rejeição anônima e, com token adulto injetado por
meio seguro, o contexto positivo sem imprimir identidade ou token. Na observação inicial ele falhava
com 404; após a promoção fechada retorna 403 até OIDC ser habilitado. O roteiro Caddy e o instalador antigo **não**
são procedimentos de atualização da VM ativa.

Pendências não resolvidas por infraestrutura: nenhuma fonte do RAG está aprovada, o estudo com
professoras está `NOT_RUN`, e a procedência de 15 ativos exige confirmação humana. Não afirmar
piloto pedagógico validado apenas porque a API ficou online.

O smoke reproduzível é `./tools/test-v2-staging-smoke.sh`. Ele usa somente loopback, cria e remove
PostgreSQL temporário e um provedor OIDC sintético. A chave RSA é efêmera, o token dura cinco
minutos e o teste confirma assinatura, issuer, audience, sujeito, vínculo ativo e exclusão de um
vínculo revogado. Tudo é removido no final. O smoke também integra
`./tools/check-delivery.sh --full`; ausência de ferramentas PostgreSQL/OpenSSL é declarada como
`SKIP`, enquanto falha de migração, segurança, escopo, rota ou revisão reprova a entrega.

O `404` observado em 17/09 significava que o JAR ativo ainda não continha a v2. Desde a promoção de
18/09, OIDC desligado produz o resultado seguro `403`, e não uma rota adulta pública.

## Registro de execução

Atualizar esta seção somente com resultados observados, incluindo revisão, banco, verificador e
rollback. Segredos, tokens e conteúdo de arquivos de ambiente não entram neste documento.

| Data UTC | Ambiente | Evidência | Resultado |
|---|---|---|---|
| 2026-09-18 | backup Oracle | restore de 44,6 MB, 1.918 arquivos, PostgreSQL 17.9 e quatro bancos consultáveis | PASS; container e volume efêmeros removidos |
| 2026-09-18 | banco staging | `interpretaai_v2_staging`, proprietário `interpretaai_app` | V1–V20 aplicadas, 28 tabelas públicas |
| 2026-09-18 | origem staging | `127.0.0.1:8188`, revisão `e638ea9cc0f8fa070bfe7e60e703b32b518d605c` | health 200; API adulta e Estúdio 403 fechados |
| 2026-09-18 | backup pré mudança | `20260912-020006F_20260918-042536D`, WAL D0–D1 | PASS no Object Storage |
| 2026-09-18 | produção | JAR v2 na porta 8088 e banco `interpretaai` | health 200, gateway HOT, Flyway V20; OIDC/Estúdio desligados |

O ensaio de restauração é executado na VM do banco:

```bash
./verify-db-restore-drill.sh
```

Ele cria nomes restritos a `interpretaai-restore-drill-*`, nunca monta o volume ativo como destino e
remove automaticamente container e volume temporários. Quando o repositório usa Object Storage, o
container precisa de saída HTTPS para buscar backup e WAL; nenhuma porta do PostgreSQL é publicada.
