# Fluxo de integração de dados do InterpretaAI

## Estado implementado

### 1. Identidade adulta
1. O estúdio autentica a pessoa pelo provedor OIDC.
2. GET /api/v2/identity/me resolve usuário, escola e papel a partir do banco.
3. As rotas adultas recebem X-School-Id; autorização e vínculo com a escola permanecem no servidor.

### 2. Preparação e revisão
1. POST /api/v2/authoring/jobs cria um trabalho idempotente.
2. GET /api/v2/authoring/jobs/{jobId} acompanha o processamento por revisão e ETag.
3. O worker materializa um LearningStoryPack validado como rascunho.
4. GET /api/v2/stories/{storyId}/versions/{version}/review entrega o pacote e os recursos privados para revisão.
5. A aprovação exige revisão esperada, hash exato do pacote e todos os IDs de recursos confirmados.
6. A aprovação cria os bytes finais e um novo hash; POST .../publish publica essa versão imutável.

### 3. Entrega ao Android
1. POST /api/v2/assignments associa uma versão publicada à turma ou aos aparelhos.
2. Cada tablet usa sua própria credencial revogável.
3. GET /api/v2/devices/{deviceId}/manifest lista versões e hashes disponíveis.
4. O APK baixa o pacote e os recursos por rotas autenticadas, confere tamanho, ETag e SHA-256 e só então marca a história como pronta.
5. O cache local mantém a aula disponível durante perda de conexão.

### 4. Participação
O piloto atual envia lotes neutros para POST /api/v1/pilot/learning-events:batch. Os eventos descrevem etapa, modalidade, duração e pedido de apoio. Não transportam áudio, resposta completa, nota, ranking ou diagnóstico.

## Estado que a interface deve mostrar

| Interface | Estado de origem | Texto para a professora |
|---|---|---|
| Autoria | trabalho assíncrono | Preparando proposta |
| Revisão | rascunho + hash + recursos | Revisar versão |
| Publicação | versão publicada | História publicada |
| Manifesto | atribuição visível ao tablet | Enviada |
| Cache | pacote e recursos verificados | Pronta no aparelho |
| Execução | eventos recebidos | Aula iniciada |
| Evidência | lote aceito | Participação registrada |

“Publicada”, “enviada” e “pronta” são estados diferentes. A tela deve mostrar o último estado confirmado pela fonte responsável.

## Próximo corte de implementação

1. Levar os eventos neutros para uma rota v2 autenticada por dispositivo e idempotente.
2. Acrescentar storyId, storyVersion, assignmentId, deviceId, occurredAt e eventId a cada evento.
3. Manter a fila local no Android até confirmação do lote.
4. Criar leitura adulta agregada por turma, sempre com origem, período, tamanho da amostra e limite da interpretação.
5. Atualizar a trilha da professora por polling com ETag primeiro; avaliar SSE somente após medir necessidade real.
