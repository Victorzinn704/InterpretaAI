# Sprint 1 — fundação Oracle e identidade

## Estado geral

`EM EXECUÇÃO`. Em 18/09/2026 o JAR v2 foi promovido e o Keycloak próprio foi ativado. A
[verificação pública](ORACLE_PUBLIC_CHECK.md) confirma revisão, gateway v1, Flyway V20, OIDC real,
sessão do Estúdio e vínculo piloto. O teste em tablet físico e a validação humana continuam pendentes.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Modelo institucional `tenant → escola → turma → usuário` | IMPLEMENTADO LOCALMENTE | migração `V11__institutional_identity.sql` | revisar nomes/políticas com responsável institucional |
| Autorização por escola, papel e vínculo com turma | ATIVA NO PILOTO ORACLE | testes locais mais token Keycloak e vínculo `school_pilot` em staging/produção | validar nomes e políticas com rede parceira |
| Adaptador OIDC | ATIVO E TESTADO NA ORACLE | Keycloak 26.7.4: authorization code, issuer, audience, token, callback Spring, sessão BFF e escola piloto passaram; anônimo recebe 401 | federar Google/Microsoft/SME quando houver domínio institucional |
| Upload privado | IMPLEMENTADO LOCALMENTE | sessão idempotente, limite, hash, armazenamento local privado, autorização e replay testados | implementar adaptador OCI e teste contra bucket privado |
| Sanitização de imagem | IMPLEMENTADO LOCALMENTE | job persistente com lease/retry; formato/dimensões/animação validados e derivado regravado em PNG | executar corpus adversarial em worker isolado e armazenar no OCI |
| Pareamento e credencial revogável do aparelho | BACKEND, ESTÚDIO E ANDROID LOCAIS | Estúdio gera código somente para turma vinculada; Android resgata sem token adulto, valida a origem, cifra a credencial no Keystore e inicia sync. Código HMAC efêmero/uso único, revogação e novo pareamento sem reativar o registro anterior estão testados | executar em aparelho real/HTTPS e aplicar rate limit distribuído no proxy |
| Fila persistente de autoria | IMPLEMENTADO LOCALMENTE | job, payload, fila, lease, retry, idempotência e auditoria atômicos; worker isolado valida a carga e retoma após expiração testada | conectar etapas RAG/Codex e validar concorrência no PostgreSQL |
| Validação, revisão, aprovação e publicação | ESTÚDIO PUBLICADO; CONTEÚDO E2E PENDENTE | sessão OIDC/BFF real passou; `validateDraft` e transições seguem cobertos por testes | testar geração → revisão → entrega → cache em tablet e uso docente |
| Atribuição, confirmação e retirada | BACKEND, ANDROID E UI LOCAIS; E2E REAL PENDENTE | Estúdio lista turmas vinculadas, atribui, distingue aparelhos pareados de recibos e permite retirada confirmada. Android verifica pacote/arquivos e, após `404` na reconfirmação online, remove o vínculo revogado; testes cobrem retry, hash divergente, recibo antigo, autorização e retirada. Recibo não é evidência de uso infantil | testar publicação → manifesto → cache offline → recibo → retirada em aparelho real/Oracle; avaliar limite de aparelho offline e cena aberta |
| Auditoria adulta | PARCIAL | criação, recebimento, sanitização/rejeição, transições de versão e atribuição à turma geram evento append-only sem conteúdo | cobrir relatório e mudança de papel |
| Oracle dev/staging, HTTPS e PostgreSQL | V2 E ÁREA DOCENTE ATIVAS | produção e staging usam OIDC real; público anônimo recebe 401/302, sessão autenticada e BFF passam; Keycloak tem banco exclusivo | ensaio de tablet e observação antes do piloto escolar |
| Backup e restauração | BANCO COMPROVADO | restore efêmero iniciou PostgreSQL 17.9 e consultou quatro bancos; backup diferencial pré mudança `20260912-020006F_20260918-042536D` concluído no Object Storage | incluir objeto/mídia privada no ensaio quando o adaptador OCI for habilitado |

## Invariantes já verificadas

- papel enviado pelo navegador não participa da decisão;
- `schoolId` e `classroomId` apenas selecionam contexto: o vínculo é consultado no banco;
- professora só publica/lê evidência individual nas turmas vinculadas;
- coordenação e administração escolar não atravessam a fronteira da escola;
- revogar a associação interrompe o acesso sem apagar o vínculo histórico da turma;
- recurso inexistente e recurso de outra escola resultam na mesma negação de domínio.
- instalação revogada recebe nova identidade e credencial em outro pareamento; o registro anterior
  continua revogado e não recupera recibos ou autorização.

## Comandos de evidência

```bash
./gradlew :server:test --tests br.gov.interpretaai.server.identity.InstitutionalAccessServiceTest
./gradlew :server:test
```

Os testes JUnit usam todas as migrações Flyway em H2 no modo PostgreSQL. Em 18/09/2026, o
PostgreSQL 17.9 da Oracle aplicou as 20 migrações no staging separado e V11–V20 no banco ativo.
Produção e staging iniciaram saudáveis. O fluxo autenticado real passou com Keycloak, token e sessão
BFF; o percurso físico de pareamento, cache offline e retirada continua pendente.
