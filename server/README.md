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

## Fundação 2.0 do Estúdio

Com `OIDC_ENABLED=true`, as rotas `/api/v2/**` exigem JWT do emissor e audience configurados. O
`sub` apenas identifica o adulto; papéis, escola e vínculo com turma são consultados no banco. O
primeiro fluxo implementado cria e recebe uploads privados:

```text
POST /api/v2/media/uploads
PUT  /api/v2/media/uploads/{mediaId}/content
```

O conteúdo bruto fica em `MEDIA_LOCAL_ROOT` no desenvolvimento e não possui rota pública. O envio
confere tamanho/tipo declarado, calcula SHA-256 e enfileira sanitização persistente. Para executar o
worker em processo separado, use `MEDIA_WORKER_ENABLED=true`; a API permanece com o padrão `false`.
O worker rejeita formato divergente, animação e dimensões excessivas e regrava um derivado PNG sem
metadados. Em produção, o adaptador local deve ser substituído por objeto OCI privado e o processo
do worker deve receber limite de CPU/memória.

O pareamento institucional nasce desligado. Defina um segredo novo com pelo menos 32 caracteres e
habilite somente atrás de HTTPS:

```bash
export DEVICE_PAIRING_ENABLED=true
export DEVICE_PAIRING_SECRET='segredo-aleatorio-nao-versionado-com-32-ou-mais-caracteres'
```

O código de oito caracteres expira em dez minutos e funciona uma vez. Código e token permanente
são guardados somente como HMAC. A credencial completa deve ir para Android Keystore e autentica
somente `/api/v2/devices/{seu-deviceId}/...`; revogação no Estúdio vale na requisição seguinte. O
limitador local de resgate é uma proteção do processo, não substitui rate limit distribuído no
Caddy/Oracle.

O início da autoria 2.0 está disponível em `POST /api/v2/authoring/jobs` e exige OIDC,
`X-School-Id` e `Idempotency-Key`. Uma imagem enviada só é aceita depois que o job de sanitização
está `READY`. Job, fila e auditoria são persistidos atomicamente; `GET
/api/v2/authoring/jobs/{jobId}` aceita `If-None-Match`. O lease da fila permite retomada por outro
worker após interrupção. Este estágio não executa modelos: conectar RAG/Codex antes do executor e
dos limites operacionais seria representar como pronta uma integração ainda não validada.
Para executar a preparação em processo separado, habilite `AUTHORING_WORKER_ENABLED=true`; ela
valida a carga persistida e entrega a uma segunda fila. O planejador docente é opt-in com
`AUTHORING_PLAN_WORKER_ENABLED=true`: consulta somente fontes aprovadas, usa Ollama por LangChain4j,
valida o contrato de saída e grava um rascunho com SHA-256. Sem fonte aprovada, o job pausa em
`NEEDS_TEACHER_INPUT` sem chamar o modelo. `GET /api/v2/authoring/jobs/{jobId}/plan` permite à
autora consultar esse plano com OIDC, escola e ETag; ele não é um `LearningStoryPack` executável,
nem gera imagens, nem publica algo no tablet. O catálogo incluído ainda tem zero fontes aprovadas,
portanto habilitar os dois workers hoje não produzirá histórias por IA.

As rotas `POST /api/v2/stories/{storyId}/versions/{version}/approve` e `/publish` também já estão
disponíveis para a transição humana de versões que o worker venha a materializar. Ambas exigem OIDC,
`X-School-Id` e `Idempotency-Key`; armazenam a decisão e auditoria, e não permitem modificar os
bytes ou o hash do pacote. Não há criação HTTP de rascunho: a origem é uma entrada interna que
valida o `LearningStoryPack` antes de persistir o rascunho, para não transformar texto do navegador
em conteúdo infantil publicável. O worker real ainda não gera esse pacote.

O próximo elo local de entrega está em `POST /api/v2/assignments` e
`GET /api/v2/devices/{deviceId}/manifest`. A professora só atribui uma versão já `PUBLISHED` a uma
turma em que possui vínculo; grupos e aparelhos individuais retornam indisponível até terem regras
próprias. O tablet pareado recebe apenas itens da própria escola/turma compatíveis com sua versão do
app e busca o JSON em uma rota autenticada privada. Isto ainda não baixa as variantes de mídia, não
aciona o cache Android nem prova o bucket/URLs assinadas da Oracle.

O canal de piloto também expõe `PUT/GET /api/v1/pilot/assignments/{deviceId}` para publicar e
consultar uma missão versionada sem identidade real. O tablet envia eventos fechados em
`POST /api/v1/pilot/learning-events:batch`; professor e secretaria leem somente agregados em
`GET /api/v1/pilot/classrooms/{classroomId}/summary` e
`GET /api/v1/pilot/secretariat/summary`. Ele nasce desligado; exige tokens distintos de tablet,
professor e secretaria, audita leituras administrativas e ainda não representa autenticação
institucional. Veja o
[contrato de sincronização](../docs/SYNC_API_PROPOSAL.md).

Uma sala pode associar de dois a quatro aliases ao mesmo `deviceId`. Nesse caso o servidor publica
uma única atribuição com a lista de avatares e persiste os sinais como `GROUP`, `learner_alias=null`
e `participant_count`, evitando atribuir a produção coletiva ao primeiro integrante. Um mesmo
aparelho continua impedido de pertencer simultaneamente a duas salas.

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

Para respostas inequívocas, uma cena pode incluir `acceptedAnswers` e `completionReply`. O gateway
então avança usando texto aprovado sem executar o LLM; falas diferentes continuam na mediação e não
são tratadas como erro. No smoke local de 15/09/2026, “está faltando a bola” chegou ao texto final em
65 ms. Durante o aquecimento assíncrono, essas conclusões aprovadas também entram no cache do Kokoro,
reduzindo a espera pela voz amigável. Com o cache aquecido, o fluxo completo de teste entregou o WAV
em 73 ms e a repetição em 14 ms. São medidas de loopback, não SLA da internet.

Respostas iguais reutilizam áudio sintetizado em memória. A chave contém somente voz e SHA-256 do
texto; o peso máximo padrão é 32 MiB e a expiração ocorre após dez minutos de inatividade. Falha ou
áudio vazio não entra no cache. Os limites podem ser alterados por `SPEECH_CACHE_MAX_BYTES` e
`SPEECH_CACHE_TTL_MINUTES`.

Para benchmark exclusivamente sintético, o adaptador Gemini usa por padrão `gemini-3.8-flash` com
raciocínio `LOW`, JSON Schema nativo, sem parâmetros de amostragem obsoletos e sem retry oculto:

```bash
export CONVERSATION_PROVIDER=gemini
export GEMINI_API_KEY='chave-nova-nao-publicada'
export GEMINI_MODEL='gemini-3.8-flash'
export GEMINI_THINKING_LEVEL='LOW'
./gradlew :server:bootRun
```

Para comparar o 3.8 com Mistral Nemotron pelo caminho real Spring + LangChain4j, exporte chaves
novas apenas no terminal e execute `./tools/benchmark-ai-latency.sh 5`. O ensaio aquece cada rota com
uma chamada sintética fora da amostra, mede separadamente `ACK`, `FINAL_TEXT` validado e resposta
completa, e mostra p50, p95, respostas degradadas e violações básicas do contrato. O envelope v1
também informa tempo monotônico do servidor, permitindo separar rede de inferência/validação e TTS.
Ele nunca recebe
áudio, imagem, histórico ou fala real de
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
TLS, conexão e endpoint. Depois, conserva uma janela quente de 150 segundos e renova a sonda a cada
90 segundos, inclusive quando a marcação anterior ainda está válida. Isso evita a janela fria que
existiria ao pular uma rota ainda quente em ciclos de 120 segundos. Nenhuma sonda
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
