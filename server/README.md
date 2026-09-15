# Servidor de mediação da LEIA

O servidor expõe `POST /api/v1/voice-turn` e a variante progressiva
`POST /api/v1/voice-turn/stream`. O modo padrão usa Qwen 2.5 1.5B local via Ollama
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

O canal de piloto também expõe `PUT/GET /api/v1/pilot/assignments/{deviceId}` para publicar e
consultar uma missão versionada sem identidade real. Ele nasce desligado; exige tokens distintos de
professor e tablet e ainda não representa autenticação institucional. Veja o
[contrato de sincronização](../docs/SYNC_API_PROPOSAL.md).

Por padrão, desenvolvimento usa H2 persistente em `data/` relativo ao processo; produção pode apontar
`DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` para PostgreSQL. Flyway cria as tabelas de
idempotência e outbox. O Android envia `Idempotency-Key`, faz no máximo um retry transitório e o
servidor reaproveita a resposta em RAM ou no banco. Veja a
[arquitetura on/off](../docs/ONLINE_OFFLINE_ARCHITECTURE.md).

A memória recente mantém no máximo seis mensagens por sessão, 2.000 sessões e dez minutos de
inatividade. Ela usa bloqueio por sessão, não um lock global. Ajustes operacionais ficam em
`SESSION_MEMORY_MAX_SESSIONS` e `SESSION_MEMORY_TTL_MINUTES`.

Em produção, defina também `IDEMPOTENCY_FINGERPRINT_SECRET` com um segredo estável de pelo menos
16 caracteres. Ele protege o HMAC usado para detectar uma chave repetida com requisição diferente;
a transcrição não é gravada em claro.

Para comparar rotas sem corrida paralela, habilite o roteador adaptativo. Ele mede cada candidato,
prefere o menor EWMA e mantém circuito independente por provedor; a falha rápida pode avançar para
o próximo candidato, mas a falha lenta termina em fallback dentro do orçamento total.

```bash
export CONVERSATION_PROVIDER=adaptive
export CONVERSATION_ROUTE=ollama,nvidia
export CONVERSATION_ROUTING_DEADLINE_MS=4000
export CONVERSATION_FAST_FAILOVER_MS=350
export CONVERSATION_FAILURE_COOLDOWN_MS=5000
```

Inclua `gemini` na rota apenas em benchmark sintético. O endpoint
`GET /api/v1/gateway/status` mostra disponibilidade, circuito, amostras, falhas e EWMA sem expor
credenciais. Ele também informa a versão e a quantidade de cenas do `ScenePack` ativo.

O contexto pedagógico das cenas é carregado em memória na inicialização, sem consulta a banco ou
RAG durante a fala. A versão `v2` é padrão e a `v1` permanece empacotada para rollback:

```bash
export SCENE_PACK_VERSION=v1
./gradlew :server:bootRun
```

Uma versão inexistente ou com conteúdo inválido impede a inicialização; isso evita servir um pacote
parcial silenciosamente. A troca de versão requer reinício intencional do processo.

Respostas iguais reutilizam áudio sintetizado em memória. A chave contém somente voz e SHA-256 do
texto; o peso máximo padrão é 32 MiB e a expiração ocorre após dez minutos de inatividade. Falha ou
áudio vazio não entra no cache. Os limites podem ser alterados por `SPEECH_CACHE_MAX_BYTES` e
`SPEECH_CACHE_TTL_MINUTES`.

Para benchmark exclusivamente sintético, o adaptador Gemini usa por padrão `gemini-3.8-flash` com
raciocínio `LOW`, JSON Schema nativo e sem retry oculto:

```bash
export CONVERSATION_PROVIDER=gemini
export GEMINI_API_KEY='chave-nova-nao-publicada'
export GEMINI_MODEL='gemini-3.8-flash'
export GEMINI_THINKING_LEVEL='LOW'
./gradlew :server:bootRun
```

Para comparar o 3.8 com Mistral Nemotron pelo caminho real Spring + LangChain4j, exporte chaves
novas apenas no terminal e execute `./tools/benchmark-ai-latency.sh 5`. O ensaio aquece cada rota com
uma chamada sintética fora da amostra, mede o tempo até `FINAL_TEXT` validado e o tempo total, e
mostra mediana, p95 e respostas degradadas. Ele nunca recebe áudio, imagem, histórico ou fala real de
criança; os arquivos temporários são removidos ao final. Não passe chaves como argumento.

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
usa chamada síncrona, JSON, limite de 120 tokens e timeout interno de 3,5 segundos dentro do orçamento
total de quatro segundos, porque a saída precisa ser curta e validada antes de chegar à criança.
Selecionar um modelo aqui prova apenas a integração;
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

### Aquecimento dos endpoints remotos

Quando Gemini ou NVIDIA está na rota, o servidor faz uma chamada mínima sintética para aquecer DNS,
TLS, conexão e endpoint. Depois, conserva uma janela quente de 150 segundos e verifica a rota a cada
dois minutos. Nenhuma
contém áudio, imagem, histórico, transcrição ou dado da criança. O aquecimento não faz retry e não
altera o limite de quatro segundos do turno infantil. Para controlar custo ou cota:

```bash
export NVIDIA_WARMUP_ENABLED=false
export GEMINI_WARMUP_ENABLED=false
export REMOTE_WARMUP_INTERVAL_MS=600000
```

Esse keep-alive aquece conexão e rota de inferência, mas não representa reserva de GPU nem SLA da
NVIDIA. Antes da apresentação, inicie o servidor pelo menos um minuto antes e confirme no log
`provider_warmup ... ready=true`.

O Ollama participa do mesmo aquecimento. Em Linux, o pacote de implantação configura o processo para
manter somente um modelo carregado; assim a primeira conversa não paga novamente o custo de carga.

O Android também chama `POST /api/v1/gateway/warmup` ao iniciar o gibi, sem bloquear a narração.
`GET /api/v1/gateway/status` informa `HOT` ou `COLD`. Quando está frio, o circuito impede uma chamada
remota e entrega fallback imediatamente; quando está quente, permite uma tentativa de até quatro
segundos. Uma fila única protege a cota; o cooldown é de 30 segundos após sucesso e dois segundos
após falha, permitindo recuperação sem tempestade de sondas.
Veja a [arquitetura do gateway](../docs/AI_GATEWAY_HOT_PATH.md).

No ensaio de estresse de 13/09/2026, o aquecimento profundo também encontrou congestionamento:
expirou em 15,04 s e os três turnos seguintes acionaram fallback em 4,21 s, 4,01 s e 4,01 s. Isso
delimita o que o aplicativo consegue controlar. Se `ready=false` persistir, use Ollama/Qwen local na
demonstração ou contrate capacidade dedicada; aumentar a frequência de chamadas gratuitas pode
agravar rate limit e não deve ser tratado como solução.

Em 14/09/2026, uma nova comparação sintética encontrou Gemini `503` após 7,27 s e NVIDIA `500` após
47,27 s. Nenhum provedor produziu amostra válida dentro do orçamento. Depois de resfriar a rota, o
gateway entregou fallback local em mediana de 9 ms. Esses números são uma fotografia do endpoint
gratuito naquele instante, não uma comparação definitiva entre os modelos.
