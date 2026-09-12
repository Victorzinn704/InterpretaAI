# Contrato inicial de eventos

Endpoint futuro sugerido: `POST /v1/learning-events:batch`.

```json
{
  "deviceId": "tablet-escola-001",
  "events": [
    {
      "eventId": "01J...",
      "schemaVersion": 1,
      "occurredAt": "2026-09-12T18:30:00Z",
      "schoolId": "esc-001",
      "classroomId": "turma-1a",
      "studentAlias": "aluno-7f2a",
      "activityId": "missao-letra-m",
      "type": "RESPONSE_SUBMITTED",
      "modality": "VOICE",
      "success": true,
      "durationMs": 4200,
      "valueCategory": "starts_with_target_phoneme"
    }
  ]
}
```

Regras:

- `eventId` é idempotente;
- `studentAlias` não contém nome, matrícula ou data de nascimento;
- `valueCategory` substitui a transcrição bruta após avaliação local;
- servidor rejeita campos desconhecidos contendo mídia ou texto livre;
- retenção de evento detalhado é curta; agregados têm prazo definido pelo controlador;
- professor enxerga suas turmas; direção sua escola; secretaria apenas sua rede;
- toda leitura administrativa sensível gera log de auditoria.
- escolhas de interpretação usam OBSERVATION_RECORDED, separadas das respostas avaliativas;
- quebra-cabeças enviam apenas figura, grade, movimentos, duração, ajuda e conclusão.

Resposta:

```json
{
  "accepted": ["01J..."],
  "rejected": []
}
```
