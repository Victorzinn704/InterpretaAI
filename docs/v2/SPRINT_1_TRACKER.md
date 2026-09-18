# Sprint 1 — fundação Oracle e identidade

## Estado geral

`EM EXECUÇÃO`. Este rastreador separa código local comprovado de infraestrutura externa. Em
17/09/2026, o responsável informou que o servidor Oracle já está conectado ao projeto. A
[verificação pública](ORACLE_PUBLIC_CHECK.md) confirma gateway v1 ativo; OIDC, banco e versão do
JAR seguem sem confirmação. A inspeção remota de leitura identificou
Nginx em container e Spring na origem `172.18.0.1:8088`; a rota v2 não está instalada.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Modelo institucional `tenant → escola → turma → usuário` | IMPLEMENTADO LOCALMENTE | migração `V11__institutional_identity.sql` | revisar nomes/políticas com responsável institucional |
| Autorização por escola, papel e vínculo com turma | IMPLEMENTADO LOCALMENTE | `InstitutionalAccessServiceTest`: 6 cenários, incluindo negação entre escolas e revogação imediata | conectar identidade OIDC real |
| Adaptador OIDC | IMPLEMENTADO LOCALMENTE | `/api/v2/**` exige JWT, valida issuer/audience e `/identity/me` resolve somente vínculos do banco | fechar D-03 e executar smoke test com o provedor real |
| Upload privado | IMPLEMENTADO LOCALMENTE | sessão idempotente, limite, hash, armazenamento local privado, autorização e replay testados | implementar adaptador OCI e teste contra bucket privado |
| Sanitização de imagem | IMPLEMENTADO LOCALMENTE | job persistente com lease/retry; formato/dimensões/animação validados e derivado regravado em PNG | executar corpus adversarial em worker isolado e armazenar no OCI |
| Pareamento e credencial revogável do aparelho | IMPLEMENTADO LOCALMENTE | código HMAC efêmero/uso único, token HMAC, cadeia HTTP por deviceId e revogação imediata testados | integrar Android Keystore e rate limit distribuído no proxy |
| Fila persistente de autoria | IMPLEMENTADO LOCALMENTE | job, payload, fila, lease, retry, idempotência e auditoria atômicos; worker isolado valida a carga e retoma após expiração testada | conectar etapas RAG/Codex e validar concorrência no PostgreSQL |
| Validação, revisão, aprovação e publicação | BACKEND E ESTÚDIO LOCAL; E2E PENDENTE | `validateDraft` rejeita aprovação antecipada; revisão adulta e mídias privadas exigem escola/autoria; aprovação confirma hash/revisão e cada mídia. O [Estúdio](STUDIO_REVIEW.md) mostra prévias reais por sessão autenticada e separa aprovação de publicação; testes usam OIDC simulado e fixture visual | testar OIDC/HTTPS/PostgreSQL reais, geração → entrega → cache e uso docente antes de publicar na Oracle |
| Atribuição, confirmação e retirada | BACKEND, ANDROID E UI LOCAIS; E2E REAL PENDENTE | Estúdio lista turmas vinculadas, atribui, distingue aparelhos pareados de recibos e permite retirada confirmada. Android verifica pacote/arquivos e, após `404` na reconfirmação online, remove o vínculo revogado; testes cobrem retry, hash divergente, recibo antigo, autorização e retirada. Recibo não é evidência de uso infantil | testar publicação → manifesto → cache offline → recibo → retirada em aparelho real/Oracle; avaliar limite de aparelho offline e cena aberta |
| Auditoria adulta | PARCIAL | criação, recebimento, sanitização/rejeição, transições de versão e atribuição à turma geram evento append-only sem conteúdo | cobrir relatório e mudança de papel |
| Oracle dev/staging, HTTPS e PostgreSQL | V1 ATIVO; V2 AUSENTE NA ORIGEM | health 200/UP e gateway v1 200/HOT/Ollama; [auditoria read-only](ORACLE_STAGING_GATE.md) confirma Spring/Kokoro/WireGuard, Nginx em container, banco `interpretaai` via PostgreSQL/WireGuard e JAR atual; OIDC/Estúdio não estão configurados e `/api/v2/identity/me` continua 404. PostgreSQL 17.11 **local e descartável** aplicou V1–V20, mas não testou a Oracle | preparar staging isolado e restauração, configurar OIDC, validar banco/HTTPS reais e só depois abrir rota no Nginx |
| Backup e restauração | PENDENTE EXTERNO | política desenhada | restaurar banco e objeto em staging |

## Invariantes já verificadas

- papel enviado pelo navegador não participa da decisão;
- `schoolId` e `classroomId` apenas selecionam contexto: o vínculo é consultado no banco;
- professora só publica/lê evidência individual nas turmas vinculadas;
- coordenação e administração escolar não atravessam a fronteira da escola;
- revogar a associação interrompe o acesso sem apagar o vínculo histórico da turma;
- recurso inexistente e recurso de outra escola resultam na mesma negação de domínio.

## Comandos de evidência

```bash
./gradlew :server:test --tests br.gov.interpretaai.server.identity.InstitutionalAccessServiceTest
./gradlew :server:test
```

Os testes JUnit usam todas as migrações Flyway em H2 no modo PostgreSQL. Em 17/09/2026, um smoke
separado iniciou o JAR em PostgreSQL 17.11 temporário, aplicou 20 migrações até V20 e confirmou
a nova tabela de recibos. O cluster foi desligado e removido. Isso não prova consultas com dados,
o fluxo autenticado nem a compatibilidade com a configuração do PostgreSQL da Oracle.
