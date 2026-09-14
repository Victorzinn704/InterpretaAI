# Servidor de mediação da LEIA

O servidor expõe `POST /api/v1/voice-turn`. O modo padrão usa Qwen 2.5 1.5B local via Ollama
para a mediação e Kokoro para as vozes `pf_dora` (LEIA) e `pm_alex` (Davi). Não há cobrança
por chamada nem chave no APK. O áudio e a transcrição nunca são registrados nos logs.

```bash
./tools/start-local-mvp.sh
./gradlew :server:test
```

O Spring escuta em `http://127.0.0.1:8088`, o Ollama em 11434 e o Kokoro em 8091. Se um
provedor não responder, o contrato continua válido com fala preparada e `degraded=true`.
O adaptador Gemini/Google TTS permanece no código somente como opção futura e não é o padrão
do produto infantil. Veja o [contrato da API](../docs/VOICE_API.md) e o
[guia do microservidor](../docs/LOCAL_MVP_SERVER.md).

## NVIDIA NIM via LangChain4j

O adaptador NVIDIA também é opcional e usa a API OpenAI-compatible exclusivamente pelo
LangChain4j no servidor Java. Uma única chave nova atende ao catálogo; não crie uma chave por
modelo e nunca coloque `nvapi-...` no APK, Git ou documentação.

```bash
export CONVERSATION_PROVIDER=nvidia
export NVIDIA_API_KEY='chave-nova-nao-publicada'
export NVIDIA_MODEL='mistralai/mistral-nemotron'
./gradlew :server:bootRun
```

Modelos admitidos pelo adaptador:

| Modelo | Papel previsto | Situação no MVP |
|---|---|---|
| `google/gemma-4-31b-it` | análise multimodal | disponível para experimento controlado |
| `moonshotai/kimi-k3` | análise visual complexa | disponível para experimento controlado |
| `mistralai/mistral-nemotron` | mediação curta | padrão quando NVIDIA é selecionada |
| `nvidia/nemotron-3-ultra-550b-a55b` | revisão textual complexa | disponível, fora da jornada padrão |

Os exemplos públicos da NVIDIA usam streaming, pensamento e até 16.384 tokens. O endpoint da LEIA
usa chamada síncrona, JSON, limite de 120 tokens e timeout de quatro segundos porque a saída precisa
ser curta e validada antes de chegar à criança. Selecionar um modelo aqui prova apenas a integração;
o uso com crianças depende de avaliação pedagógica, privacidade, termos do provedor e smoke test.
Retries do cliente estão desativados: um endpoint lento aciona o fallback uma vez. A síntese Kokoro
tem orçamento adicional de 1,5 segundo; se o áudio remoto atrasar, o Android recebe texto e usa a
voz local em vez de manter a criança esperando.

### Smoke test remoto de 13/09/2026

Foi enviada somente a frase sintética “Eu acho que está faltando a bola”, sem áudio, imagem ou dado
de criança. No limite de interação adotado pelo produto, Mistral Nemotron retornou JSON pedagógico
válido em aproximadamente três segundos. Kimi K3, Gemma 4 31B e Nemotron 3 Ultra ultrapassaram seis
segundos no endpoint gratuito e acionaram timeout. Esse ensaio confirma conectividade, não qualidade
pedagógica; deve ser repetido antes de cada demonstração porque disponibilidade e latência variam.

Após a otimização, uma nova chamada ao Mistral encontrou fila superior a quatro segundos e caiu no
fallback em 4,08 s. Portanto, 2,96 s é um resultado observado, não um SLA: a garantia do MVP é que a
jornada continua rapidamente mesmo quando o endpoint gratuito oscila.

Com o servidor persistente, a série seguinte de três turnos válidos respondeu em 2,76 s, 2,54 s e
2,31 s. A medição inclui a API Spring e a tentativa local de áudio; `degraded=true` ocorreu somente
porque o Kokoro não estava ativo durante o ensaio. Nenhuma dessas medições deve ser apresentada como
garantia contratual do endpoint gratuito.

### Aquecimento do endpoint

Quando NVIDIA é o provedor ativo, o servidor faz uma chamada mínima com orçamento de até 15 segundos
para permitir que a rota termine de aquecer. Depois, mantém uma chamada a cada dois minutos. Nenhuma
contém áudio, imagem, histórico, transcrição ou dado da criança. O aquecimento não faz retry e não
altera o limite de quatro segundos do turno infantil. Para controlar custo ou cota:

```bash
export NVIDIA_WARMUP_ENABLED=false
export NVIDIA_WARMUP_INTERVAL_MS=600000
```

Esse keep-alive aquece conexão e rota de inferência, mas não representa reserva de GPU nem SLA da
NVIDIA. Antes da apresentação, inicie o servidor pelo menos um minuto antes e confirme no log
`nvidia_warmup ... ready=true`.

O Android também chama `POST /api/v1/gateway/warmup` ao iniciar o gibi, sem bloquear a narração.
`GET /api/v1/gateway/status` informa `HOT` ou `COLD`. Quando está frio, o circuito impede uma chamada
remota e entrega fallback imediatamente; quando está quente, permite uma tentativa de até quatro
segundos. Um cooldown de 30 segundos e uma fila única protegem a cota contra aquecimentos repetidos.
Veja a [arquitetura do gateway](../docs/AI_GATEWAY_HOT_PATH.md).

No ensaio de estresse de 13/09/2026, o aquecimento profundo também encontrou congestionamento:
expirou em 15,04 s e os três turnos seguintes acionaram fallback em 4,21 s, 4,01 s e 4,01 s. Isso
delimita o que o aplicativo consegue controlar. Se `ready=false` persistir, use Ollama/Qwen local na
demonstração ou contrate capacidade dedicada; aumentar a frequência de chamadas gratuitas pode
agravar rate limit e não deve ser tratado como solução.
