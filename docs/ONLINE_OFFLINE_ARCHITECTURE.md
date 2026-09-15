# Arquitetura on/off da LEIA

## Princípio

A atividade nunca depende de uma resposta remota para continuar. A IA melhora a mediação quando
está disponível; a máquina de estados Kotlin, as falas aprovadas, o reconhecimento de conceitos
essenciais e o próximo passo continuam locais. O objetivo de latência é reação visual em até 100 ms,
resposta remota útil em até 4 s e fallback sem nova espera quando circuito ou capacidade recusarem.

## Caminho quente implementado

```mermaid
flowchart LR
    A[Voz reconhecida] --> B[Reação pulsativa local]
    B --> C[POST + Idempotency-Key]
    C --> D{Cache Caffeine?}
    D -->|sim| E[Replay imediato]
    D -->|não| F{Registro concluído?}
    F -->|sim| E
    F -->|não| G{Rota permitida e disponível?}
    G -->|não| H[Fallback seguro]
    G -->|sim| P[ScenePack v2 em memória: O 1]
    P --> I[Roteador: EWMA + circuito por provedor]
    I --> J[Bulkhead global: 2 chamadas, fila zero]
    J --> K[LangChain4j: orçamento total de 4 s]
    K --> L[Validação pedagógica]
    L --> M[FINAL_TEXT no Android]
    M --> Q{Áudio em cache?}
    Q -->|sim| N[TTS já sintetizado]
    Q -->|não| R[TTS remoto ou local]
    R --> N
    N --> O[COMPLETE + persistência/outbox]
```

- `Idempotency-Key` identifica uma tentativa lógica. O Android reutiliza a chave em um único retry
  transitório; o servidor coalesce duplicatas simultâneas, guarda a resposta em RAM por dez minutos
  e persiste a mesma resposta para sobreviver a reinícios.
- A impressão da requisição é HMAC de todos os campos, incluindo a transcrição; detecta reutilização
  incorreta sem persistir a fala em claro. Produção deve fornecer `IDEMPOTENCY_FINGERPRINT_SECRET`.
- Não são persistidos áudio da criança nem transcrição em claro. O HMAC não é usado como métrica,
  perfil ou conteúdo pedagógico; serve apenas para confirmar que um retry contém a mesma requisição.
- O bulkhead usa duas execuções e fila zero. No caminho infantil, rejeitar rapidamente é melhor que
  esconder congestionamento em uma fila crescente.
- O circuito Resilience4j usa janela deslizante de oito chamadas, mínimo de quatro, limite de 50%
  para falha ou lentidão, chamada lenta acima de 2,5 s e vinte segundos aberto.
- O deadline de quatro segundos é externo ao SDK; Gemini e NVIDIA usam 3,5 segundos internamente,
  deixando margem para validação e liberação da vaga. Mesmo que um cliente ignore interrupção, as
  duas vagas limitam o dano e novas chamadas recebem fallback imediato.
- A síntese consulta cache em memória por `SHA-256(texto) + voz`, sem manter o texto como chave.
  O limite padrão é 32 MiB/10 min; chamadas simultâneas iguais são coalescidas. Áudio vazio ou erro
  nunca é cacheado, portanto uma falha temporária não contamina os próximos turnos.
- A memória recente usa um cache de até 2.000 sessões com TTL de dez minutos. Cada entrada mantém um
  deque de seis mensagens e sincroniza apenas a própria sessão, evitando lock global entre turmas.

## Fila, retry e rollback

A fila persistente é a `operational_outbox`, fora do caminho quente. A conclusão idempotente e a
criação do evento `VOICE_TURN_COMPLETED` ocorrem na mesma transação: ou ambas são confirmadas, ou o
banco reverte ambas. Um worker busca até oito eventos, aplica lease de trinta segundos, entrega
métrica neutra e, se falhar, reagenda com backoff exponencial. Depois de cinco tentativas, o evento
vai para `DEAD` para inspeção, sem bloquear a criança.

Retry do modelo permanece desligado: repetir uma inferência de quatro segundos pioraria abandono.
Retry de transporte ocorre uma única vez no Android, entre 80 e 180 ms, somente para timeout, HTTP
425 ou falhas 502/503/504, sempre com a mesma chave idempotente. Um deadline monotônico de seis
segundos envolve todas as tentativas e a negociação de compatibilidade; portanto, retry não duplica
o tempo máximo percebido. Depois de descobrir um servidor sem `/stream`, o cliente memoriza essa
capacidade durante a vida da aplicação e usa diretamente o contrato JSON nos turnos seguintes.

Rollback remoto não existe: uma chamada de IA já enviada não pode ser “desenviada”. O rollback
defensável é transacional no banco, cancelamento/isolamento da tarefa e descarte de resposta tardia
no Android quando sessão ou tela mudarem. Conteúdo usa pacotes imutáveis `v1` e `v2`; produção ativa
uma versão por `SCENE_PACK_VERSION` e volta para `v1` por configuração e reinício, sem editar código
ou banco. Versão ausente ou inválida impede o servidor de iniciar, evitando conteúdo parcial.

## Árvore de decisão do provedor implementada

O `AdaptiveConversationRouter` implementa a árvore sem corrida paralela com dados infantis:

```text
conceito essencial conhecido localmente? -> resposta local
senão, aparelho sem rede?                 -> ScenePack/fallback local
senão, provedor permitido e circuito HOT? -> modelo rápido da tarefa
senão, modelo local saudável?             -> Ollama
senão                                     -> fallback aprovado
```

Cada ramo precisa considerar `taskType`, autorização contratual, circuito, capacidade e p95 recente.
Nunca escolher por identidade, desempenho atribuído ou diagnóstico da criança. Gemini fica restrito
a benchmark sintético enquanto seus termos não permitirem o público infantil pretendido.

Com `CONVERSATION_PROVIDER=adaptive`, `CONVERSATION_ROUTE` define os candidatos permitidos. Cada
provedor é amostrado uma vez; depois o roteador escolhe o menor escore de latência EWMA, somando uma
penalidade apenas por falhas consecutivas. O sucesso zera essa penalidade, para uma falha antiga não
banir uma rota para sempre. Cada provedor tem circuito próprio. Failover sequencial só acontece se a
falha ocorrer em até 350 ms e ainda existir orçamento total; uma falha lenta termina em fallback e
coloca a rota em cooldown por cinco segundos, protegendo o turno seguinte antes mesmo de o circuito
estatístico atingir sua amostra mínima.
No percurso infantil recomendado, a rota é `ollama,nvidia`. `gemini` só entra em sessão sintética.

## Estruturas e algoritmos com função real

| Elemento | Uso defensável |
|---|---|
| fila | outbox persistente e trabalhos frios de métricas/sincronização |
| deque limitado | seis mensagens efêmeras por sessão; cache limitado a 2.000 sessões/10 min |
| pilha | somente para desfazer/refazer desenho no futuro; não é necessária no diálogo atual |
| árvore | roteamento determinístico por tarefa, permissão e saúde |
| mapa imutável | `ScenePack` carregado no início e consultado por `sceneId` em O(1) |
| cache ponderado | áudio de TTS limitado por bytes; reduz sínteses iguais sem memória ilimitada |
| janela deslizante | abrir/fechar circuito por falha e lentidão recente |
| backoff exponencial | reenviar eventos de segundo plano sem tempestade de chamadas |

## Experiência por condição

| Condição | O que a criança percebe | Execução |
|---|---|---|
| online + IA saudável | botão vivo, pausa curta e resposta contextual | rota remota validada |
| online + IA lenta | reação local e fallback até 4 s | deadline + circuito |
| rede cai depois do texto | resposta validada permanece e usa TTS local | `FINAL_TEXT` promovido a fallback contextual |
| online + resposta repetida | mesma resposta quase imediata | cache/banco idempotente |
| offline | atividade e voz continuam | regras, conteúdo e TTS locais |
| troca de tela durante chamada | nenhuma fala atrasada invade a nova tela | job e chamada OkHttp cancelados + session guard |
| servidor anterior sem streaming | resposta JSON única continua disponível | fallback 404/405 com a mesma chave idempotente |
| estímulos reduzidos | indicação estática, sem pulsação | mesma orientação e voz |

O CTA de voz não muda apenas de cor: o rótulo e a chamada acompanham `pronto para ouvir`,
`ouvindo` e `juntando as pistas`. A pulsação permanece durante a espera real e é retirada pelo modo
de estímulos reduzidos sem esconder a instrução principal.

## Próximos incrementos, por evidência

1. Alimentar o roteador somente com provedores juridicamente permitidos e medir TTFT, p50/p95,
   validade do JSON, fallback e saturação em ensaios sintéticos reproduzíveis.
2. Medir TTFT de `ACK`, `FINAL_TEXT` e `COMPLETE`; adicionar `AUDIO_CHUNK` apenas com TTS streaming.
3. Adotar WebSocket quando houver streaming bidirecional de áudio, VAD e interrupção de fala.

## Evidência local de 14/09/2026

- primeira execução idempotente: aproximadamente 51 ms usando fallback local;
- replay em memória: aproximadamente 1,6 ms;
- replay persistido após reiniciar o Spring: aproximadamente 36 ms;
- rota Ollama deliberadamente inválida, sem retry oculto: fallback em aproximadamente 84 ms;
- turno seguinte durante o cooldown: fallback em aproximadamente 3 ms;
- fluxo NDJSON local degradado: `ACK` em 31 ms, `FINAL_TEXT` em 33 ms e `COMPLETE` em 48 ms;
- replay do mesmo fluxo idempotente: os três eventos concluídos em aproximadamente 3 ms;
- inicialização real confirmou `ScenePack v2` com sete cenas; rollback real com
  `SCENE_PACK_VERSION=v1` expôs cinco cenas no status; `v999` impediu a inicialização;
- 61 testes do servidor e 32 testes Android unitários aprovados.

Esses valores provam os mecanismos locais, não constituem SLA de rede ou de provedor.
