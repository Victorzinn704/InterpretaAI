# Sprint 1 — fundação Oracle e identidade

## Estado geral

`EM EXECUÇÃO`. Este rastreador separa código local comprovado de infraestrutura externa. Em
17/09/2026, o responsável informou que o servidor Oracle já está conectado ao projeto. A
[verificação pública](ORACLE_PUBLIC_CHECK.md) confirma gateway v1 ativo; versão do JAR, banco,
OIDC e configuração da VM seguem sem inspeção remota.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Modelo institucional `tenant → escola → turma → usuário` | IMPLEMENTADO LOCALMENTE | migração `V11__institutional_identity.sql` | revisar nomes/políticas com responsável institucional |
| Autorização por escola, papel e vínculo com turma | IMPLEMENTADO LOCALMENTE | `InstitutionalAccessServiceTest`: 6 cenários, incluindo negação entre escolas e revogação imediata | conectar identidade OIDC real |
| Adaptador OIDC | IMPLEMENTADO LOCALMENTE | `/api/v2/**` exige JWT, valida issuer/audience e `/identity/me` resolve somente vínculos do banco | fechar D-03 e executar smoke test com o provedor real |
| Upload privado | IMPLEMENTADO LOCALMENTE | sessão idempotente, limite, hash, armazenamento local privado, autorização e replay testados | implementar adaptador OCI e teste contra bucket privado |
| Sanitização de imagem | IMPLEMENTADO LOCALMENTE | job persistente com lease/retry; formato/dimensões/animação validados e derivado regravado em PNG | executar corpus adversarial em worker isolado e armazenar no OCI |
| Pareamento e credencial revogável do aparelho | IMPLEMENTADO LOCALMENTE | código HMAC efêmero/uso único, token HMAC, cadeia HTTP por deviceId e revogação imediata testados | integrar Android Keystore e rate limit distribuído no proxy |
| Fila persistente de autoria | IMPLEMENTADO LOCALMENTE | job, payload, fila, lease, retry, idempotência e auditoria atômicos; worker isolado valida a carga e retoma após expiração testada | conectar etapas RAG/Codex e validar concorrência no PostgreSQL |
| Validação, aprovação e publicação de versão imutável | IMPLEMENTADO LOCALMENTE | o validador Java bloqueia estrutura, referências, apoio, acessibilidade, procedência e linguagem proibida antes do ingresso interno; `story_version` nunca atualiza JSON/hash; rota adulta exige escola e autoria, controla revisão/idempotência e audita; testes cobrem validação, materialização, transições e negações | ligar a saída real RAG/Codex ao ingresso interno e publicar atribuições/manifesto |
| Auditoria adulta | PARCIAL | criação, recebimento, sanitização/rejeição e transições de versão geram evento append-only sem conteúdo | cobrir atribuição, relatório e mudança de papel |
| Oracle dev/staging, HTTPS e PostgreSQL | GATEWAY V1 PÚBLICO CONFIRMADO; V2 NÃO EXPOSTO | na URL informada, health 200/UP e gateway v1 200/HOT/Ollama; `/api/v2/identity/me` retorna 404. A origem Oracle foi relatada, mas banco, OIDC e versão não são visíveis pelo endpoint | inspecionar VM sem escrita, validar OIDC e PostgreSQL, depois habilitar v2 com teste anônimo/autenticado |
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
