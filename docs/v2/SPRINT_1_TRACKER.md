# Sprint 1 — fundação Oracle e identidade

## Estado geral

`EM EXECUÇÃO`. Este rastreador separa código local comprovado de infraestrutura externa ainda não
provisionada. Nenhum item Oracle recebe estado concluído sem evidência do ambiente.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Modelo institucional `tenant → escola → turma → usuário` | IMPLEMENTADO LOCALMENTE | migração `V11__institutional_identity.sql` | revisar nomes/políticas com responsável institucional |
| Autorização por escola, papel e vínculo com turma | IMPLEMENTADO LOCALMENTE | `InstitutionalAccessServiceTest`: 6 cenários, incluindo negação entre escolas e revogação imediata | conectar identidade OIDC real |
| Adaptador OIDC | IMPLEMENTADO LOCALMENTE | `/api/v2/**` exige JWT, valida issuer/audience e `/identity/me` resolve somente vínculos do banco | fechar D-03 e executar smoke test com o provedor real |
| Upload privado | IMPLEMENTADO LOCALMENTE | sessão idempotente, limite, hash, armazenamento local privado, autorização e replay testados | implementar adaptador OCI e teste contra bucket privado |
| Sanitização de imagem | IMPLEMENTADO LOCALMENTE | job persistente com lease/retry; formato/dimensões/animação validados e derivado regravado em PNG | executar corpus adversarial em worker isolado e armazenar no OCI |
| Pareamento e credencial revogável do aparelho | IMPLEMENTADO LOCALMENTE | código HMAC efêmero/uso único, token HMAC, cadeia HTTP por deviceId e revogação imediata testados | integrar Android Keystore e rate limit distribuído no proxy |
| Fila persistente de autoria | PARCIAL | outbox do MVP já sobrevive em banco; ainda sem payload/estado de autoria 2.0 | criar job/outbox v2 e ensaio de reinício |
| Auditoria adulta | PARCIAL | criação, recebimento, sanitização/rejeição geram evento append-only sem conteúdo | cobrir aprovação, publicação, relatório e papel |
| Oracle dev/staging, HTTPS e PostgreSQL | PENDENTE EXTERNO | artefato de deploy legado não comprova ambiente 2.0 | provisionar e registrar smoke test |
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

O teste usa todas as migrações Flyway em H2 no modo PostgreSQL. A compatibilidade real com a versão
escolhida do PostgreSQL ainda precisa ser executada no ambiente dev da Oracle.
