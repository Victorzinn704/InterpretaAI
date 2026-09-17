# Ensaio local do planejador docente — 17/09/2026

Ensaio opt-in com LangChain4j → Ollama, usando **apenas uma fonte sintética aprovada no teste**,
palavra MAÇÃ e contrato real de saída do `AuthoringPlanService`. Nenhuma imagem, criança,
credencial ou fonte candidata de produção foi enviada ao modelo. O resultado aceito precisa
conservar a palavra, citar a fonte fornecida, manter componentes pedidos e caber nos limites do
plano. O validador não faz avaliação pedagógica humana.

| Modelo local | Prompt inicial | Prompt com limites explícitos | p50 local no segundo ensaio |
|---|---:|---:|---:|
| Qwen 2.5 1.5B Q4 | 2/3 aceitos | 1/3 aceito | 2,6 s |
| Qwen 2.5 3B Q4 | 2/3 aceitos | 1/3 aceito | 4,0 s |
| Qwen3 4B Q4 sem thinking | 0/3 aceitos | 3/3 aceitos | 5,5 s |

O primeiro prompt não limitava o tamanho no esquema. O Qwen3 excedeu em poucos caracteres o
contexto de 280; após instruções de concisão, passou nas três amostras. Os outros modelos ainda
falharam por texto longo ou por mudar a lista de componentes. Amostra de três é pequena e o tempo
foi medido neste Mac, **não** na VM Oracle, com cache/temperatura/concorrência não controlados.
O Qwen 2.5 3B foi incluído somente como comparação local: sua licença restritiva impede promovê-lo
como padrão ou distribuí-lo no produto sem revisão jurídica específica.
Esses números não escolhem um modelo de produção nem provam qualidade didática. Por isso o worker
docente continua desligado por padrão e agora exige `AUTHORING_MODEL` explícito ao ser ativado.

Para repetir em loopback, com um modelo já instalado no Ollama:

```bash
RUN_AUTHORING_MODEL_SMOKE=true AUTHORING_SMOKE_MODEL=qwen3:4b \
  ./gradlew :server:test \
  --tests br.gov.interpretaai.server.authoring.AuthoringPlanOllamaSmokeTest --rerun-tasks
```

Próximo portão: pelo menos 30 casos distintos de objetivos, palavras, tamanhos e fontes aprovadas,
mais revisão por professora; depois repetir no Oracle com métricas de duração e memória. Saída
inválida permanece bloqueada e nunca segue para `LearningStoryPack` ou tablet.
