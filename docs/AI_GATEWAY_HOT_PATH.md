# Gateway de IA e caminho quente da LEIA

## Objetivo

Fazer a criança perceber reação imediata mesmo quando a IA externa estiver fria, congestionada ou
offline. Navegação, confirmação de conceitos essenciais e próxima etapa nunca dependem da nuvem. A
IA melhora a formulação; ela não controla o percurso.

## Arquitetura do MVP

```mermaid
flowchart LR
    A[Android: máquina LEIA] -->|início do gibi| B[POST gateway/warmup]
    B --> C[Gateway Spring]
    C -->|assíncrono| D[Warm-up NVIDIA]
    A -->|fala curta| E[resolvedor local]
    E -->|conceito conhecido| F[resposta e voz locais]
    E -->|mediação aberta| C
    C --> G{rota HOT?}
    G -->|não| H[fala preparada imediata]
    G -->|sim| I[LangChain4j]
    I --> J[Mistral Nemotron]
    J --> K[validação e limite]
    K --> L[Kokoro até 1,5 s]
    L --> A
```

O Android chama o aquecimento em uma coroutine sem aguardar resposta, ao mesmo tempo em que narra a
história. O gateway mantém uma janela quente de 150 segundos após uma sonda bem-sucedida. Se a sonda
falhar ou um turno remoto der erro, o circuito fecha e os próximos turnos recebem fala preparada sem
esperar o timeout externo. O agendamento tenta aquecer novamente a cada dois minutos.

Endpoints:

- `POST /api/v1/gateway/warmup`: agenda aquecimento e responde `202` imediatamente; cooldown de 30 s
  e fila única impedem chamadas concorrentes;
- `GET /api/v1/gateway/status`: retorna `HOT` ou `COLD`, sem revelar credencial;
- `POST /api/v1/voice-turn`: preserva o contrato do aplicativo.

## Orçamentos de latência

| Etapa | Orçamento | Comportamento quando excede |
|---|---:|---|
| reação visual no Android | até 100 ms | sempre local |
| gateway com circuito frio | até 200 ms no servidor | fala preparada |
| inferência remota quente | até 4 s | fecha circuito e usa fallback |
| Kokoro | até 1,5 s | devolve texto e Android usa TTS local |
| chamada Android completa | até 6 s | experiência offline já existente |

O smoke test local do circuito frio respondeu em aproximadamente 54 ms. Isso é evidência do
gateway, não da rede. O endpoint gratuito da NVIDIA variou entre 2,31 s e timeout; não há SLA.

## Troca de respostas: arquitetura incremental

O caminho recomendado não transmite tokens crus do modelo para a criança. A resposta precisa estar
completa para que os limites pedagógicos e de segurança sejam validados antes da fala. Streaming
serve para estados da interação e, futuramente, para áudio validado — não para narrar JSON parcial.

```mermaid
sequenceDiagram
    participant A as Android
    participant G as Gateway
    participant R as ProviderRouter
    participant M as Modelo permitido
    A->>A: reação local + fala-ponte (0–100 ms)
    A->>G: transcript curto + sceneId + deadline
    G->>R: rota por tarefa e saúde
    R->>M: contexto mínimo + contrato estruturado
    M-->>R: resposta candidata
    R->>R: validação pedagógica e estrutural
    R-->>G: resposta segura ou fallback
    G-->>A: texto final + reação + áudio/fallback
```

Evolução em três passos, sem reescrever o MVP:

1. **Agora:** manter o `POST /voice-turn`, produzir reação e fala-ponte local enquanto ele executa,
   cancelar ao sair da etapa e encerrar a chamada no orçamento de tempo.
2. **Segundo provedor em produção:** introduzir `ProviderRouter` com circuito, `TimeLimiter` e
   `Bulkhead` independentes por provedor. A seleção considera tarefa, permissão de uso, estado do
   circuito e latência recente; nunca envia o mesmo dado infantil simultaneamente a dois serviços.
3. **Áudio realmente contínuo:** adicionar `POST /voice-turn/stream` com NDJSON ou SSE no próprio
   Spring MVC, emitindo somente `ACK`, `FINAL_TEXT`, `AUDIO_CHUNK`, `COMPLETE` e `FALLBACK`.
   WebSocket entra apenas quando houver áudio bidirecional e interrupção de fala.

Não é necessário migrar agora para WebFlux: Spring MVC suporta `ResponseBodyEmitter`, SSE e NDJSON.
No Android, a futura conexão de streaming deve ser cancelável e substitui o `HttpURLConnection`
somente nesse endpoint. A fala-ponte deve vir de um pequeno banco de áudios já aprovado e embarcado;
ela mascara espera sem inventar conteúdo pedagógico.

### Componentes aprovados para cada responsabilidade

| Responsabilidade | Escolha | Motivo |
|---|---|---|
| fronteira dos modelos | LangChain4j | contrato comum, streaming e observabilidade |
| limite/circuito/concorrência | Resilience4j, quando houver dois provedores | evita manter lógica distribuída de falha à mão |
| métricas | Micrometer | mede TTFT, total, timeout, fallback e resposta válida |
| `ScenePack` e falas aprovadas | Caffeine, RAM limitada por tamanho/TTL | reduz consulta sem banco vetorial nem dado pessoal |
| fluxo infantil | máquina de estados Kotlin | previsibilidade e operação offline |
| preparação curricular | LangGraph4j futuro, assíncrono | não acrescenta nós ou latência à conversa |

Não usar retry no turno infantil. Não fazer corrida Gemini × Mistral com fala real: além de dobrar
custo e exposição de dados, o perdedor continua processando. Testes A/B devem atribuir um provedor
por sessão sintética e comparar p50/p95, TTFT, validade do contrato e taxa de fallback.

## Gemini 3.8 no laboratório

O adaptador experimental usa `gemini-3.8-flash`, `thinkingLevel=LOW`, zero retry e saída curta. A
documentação do modelo informa que `MEDIUM` é o padrão e que `LOW` reduz custo e latência. O modelo é
configurável por `GEMINI_MODEL` e o nível por `GEMINI_THINKING_LEVEL`; isso permite benchmark sem
alterar código. `3.8 Flash` é mais capaz, mas capacidade não substitui o limite de tempo nem a
validação da resposta.

No smoke sintético de 14/09/2026, já com o deadline externo, o primeiro turno válido terminou em
2,46 s. Os dois seguintes excederam o orçamento e receberam fallback em 4,02 s. Antes do deadline,
uma chamada do SDK permaneceu executando por cerca de 21 s mesmo após o cliente HTTP desistir. Isso
justifica manter o prazo no gateway e o `Bulkhead`, e não confiar apenas no timeout do adaptador.
Kokoro não estava ativo nesse ensaio; por isso o log separa `conversation_fallback` de
`speech_fallback`.

## LangChain4j e LangGraph4j

LangChain4j permanece na borda dos modelos: monta a chamada, limita tokens e converte a resposta em
um contrato Java. Ele só é chamado se o gateway considerar a rota quente.

LangGraph4j não entra no caminho quente do MVP. Quando adotado, sua função será executar fluxos
assíncronos e auditáveis:

```mermaid
flowchart LR
    A[atividade encerrada] --> B[remover identificadores]
    B --> C[agrupar eventos neutros]
    C --> D[LangGraph4j]
    D --> E[propor próxima atividade]
    E --> F[revisão do professor]
    F --> G[publicar pacote pedagógico]
```

Nenhum grafo autônomo avalia, diagnostica ou muda a tela da criança. Para estabilidade, avaliar a
linha LTS 1.8.x do LangGraph4j antes de adicionar a dependência; a linha 1.9 ainda é beta.

## RAG sem atrasar a conversa

RAG não aquece modelo e adicionaria busca, montagem de contexto e mais tokens. Para as poucas cenas
do MVP, `sceneId` seleciona diretamente um pacote pedagógico pequeno, versionado e carregado em RAM.
Isso é mais rápido e mais testável que banco vetorial.

RAG passa a fazer sentido quando houver um acervo curricular grande. Mesmo então, a recuperação deve
acontecer no caminho frio: professor publica a atividade, o servidor recupera referências, revisa o
conteúdo e grava um `ScenePack` pronto. Durante a aula, o gateway apenas lê esse pacote por chave.

## WebSocket e webhook

O `POST` atual é suficiente enquanto o aparelho envia texto curto e recebe uma resposta curta.
WebSocket será útil quando o produto transmitir áudio bidirecional, interrupção de fala e áudio em
chunks. Nesse estágio, o gateway emitirá `turn.ack`, `turn.audio`, `turn.complete` e `turn.fallback`.
Não se deve transmitir raciocínio interno do modelo.

Webhook não é apropriado: ele atende callbacks entre servidores, não uma conversa síncrona com uma
criança esperando retorno.

## Gemini e dados infantis

Gemini Live oferece WebSocket bidirecional e tokens efêmeros, mas os termos atuais do Gemini
Developer API vedam clientes direcionados ou provavelmente acessados por menores de 18 anos. Por
isso, a chave de desenvolvedor não é usada na jornada infantil. Uma evolução com Gemini exige
produto/contrato que permita o público, avaliação jurídica e de privacidade, consentimento e testes.
Além disso, nos serviços gratuitos o Google informa que entradas e respostas podem ser usadas para
melhorar produtos e analisadas por revisores; dados pessoais ou sensíveis não devem ser enviados.

Fontes técnicas:

- [Modelos Gemini e identificador do 3.8 Flash](https://ai.google.dev/gemini-api/docs/models)
- [Thinking levels do Gemini](https://ai.google.dev/gemini-api/docs/thinking)
- [Saída estruturada do Gemini](https://ai.google.dev/gemini-api/docs/structured-output)
- [Gemini Live por WebSocket](https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket)
- [Tokens efêmeros do Gemini Live](https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens)
- [Termos adicionais do Gemini API](https://ai.google.dev/gemini-api/terms)
- [Streaming no LangChain4j](https://docs.langchain4j.dev/tutorials/response-streaming/)
- [Requisições assíncronas e streaming no Spring MVC](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html)
- [Circuit breaker Resilience4j no Spring](https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j.html)
- [Histogramas e percentis no Micrometer](https://docs.micrometer.io/micrometer/reference/1.14/concepts/histogram-quantiles.html)
- [Cache Caffeine](https://github.com/ben-manes/caffeine/wiki/Eviction)
- [Streaming da API NVIDIA NIM](https://docs.nvidia.com/nim/large-language-models/latest/api-reference.html)
- [LangGraph4j](https://github.com/langgraph4j/langgraph4j)

## Critério para avançar

WebSocket, RAG ou LangGraph4j só entram na versão demonstrável quando reduzirem uma métrica observada
sem prejudicar fallback. Antes disso, o caminho correto é: reação local imediata, contexto pronto,
modelo quente quando disponível e continuidade local quando não estiver.
