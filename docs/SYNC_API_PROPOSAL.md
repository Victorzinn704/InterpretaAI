# Contrato futuro de sincronização pedagógica

Este contrato ainda não está implementado. Ele registra sinais para apoiar a observação docente sem
transportar áudio, imagem ou transcrição livre. Endpoint proposto: `POST /v1/learning-events:batch`.

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
      "durationMs": 4200,
      "observationCategory": "TARGET_PHONEME_NOTICED"
    }
  ]
}
```

Regras:

- `eventId` é idempotente;
- `studentAlias` não contém nome, matrícula ou data de nascimento;
- `observationCategory` descreve o sinal observado e não representa nota ou diagnóstico;
- servidor rejeita campos desconhecidos contendo mídia ou texto livre;
- retenção de evento detalhado é curta; agregados têm prazo definido pelo controlador;
- professor enxerga suas turmas; direção sua escola; secretaria apenas sua rede;
- toda leitura administrativa sensível gera log de auditoria.
- escolhas de interpretação usam `OBSERVATION_RECORDED`, sem classificação binária de certo/errado;
- quebra-cabeças enviam apenas figura, grade, movimentos, duração, ajuda e conclusão.

Resposta:

```json
{
  "accepted": ["01J..."],
  "rejected": []
}
```
