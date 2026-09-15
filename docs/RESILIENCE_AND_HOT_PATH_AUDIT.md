# Auditoria de resiliência e caminhos quentes

Estado verificado em 14/09/2026. Esta matriz separa implementação, evidência e limite; presença de
uma biblioteca não é tratada como prova de funcionamento.

| Requisito | Estado e implementação | Evidência |
|---|---|---|
| fila | Implementada fora do turno infantil: `operational_outbox`, lote 8, lease 30 s, backoff exponencial e `DEAD` após cinco falhas | integração cria outbox na mesma transação; testes de worker, lease, backoff e poison event |
| pilha | Não adicionada: nenhuma função atual exige LIFO; o histórico é corretamente um deque FIFO limitado | decisão explícita evita estrutura sem comportamento de produto; pilha entra apenas se o quadro ganhar desfazer/refazer |
| retry | Um retry Android somente para falha transitória, 80–180 ms e mesma chave; retries dos SDKs desligados | MockWebServer confere chave; providers usam `maxRetries(0)` |
| circuit breaker | Circuito Resilience4j independente por provedor e cooldown imediato | teste abre circuito após quatro falhas e prova que a quinta chamada não alcança o provider |
| persistência | H2 local/PostgreSQL configurável com Flyway para idempotência e outbox | teste Spring grava uma única resposta e evento; duas migrations validadas no startup real |
| idempotência | Coalescência em voo, cache 10 min, replay no banco e HMAC da requisição completa | testes cobrem replay, conflito por cena/transcrição e degradação quando o banco cai |
| rollback | Transação reverte resposta/outbox em conjunto; `ScenePack` v1/v2 é selecionável por ambiente | processos reais iniciaram com v2/7 cenas e v1/5 cenas; v999 falhou antes de servir tráfego |
| interação pulsativa | CTA acompanha pronto, ouvindo e processando; modo reduzido remove movimento | 18 testes instrumentados passaram no emulador Android 15/API 35; o teste dedicado valida o sinal e sua remoção no modo reduzido |
| caminho quente | decisão essencial local, ScenePack O(1), áudio cacheado, OkHttp compartilhado, fila zero e texto antes do TTS | testes preservam `FINAL_TEXT`, coalescem seis TTS iguais, rejeitam imediatamente quando a vaga está ocupada e cancelam no deadline |
| árvore e algoritmo | árvore determinística filtra permissão/saúde; EWMA escolhe menor latência após exploração | testes provam exploração, preferência, failover rápido e bloqueio de failover lento |

O aquecimento é comum a Gemini e NVIDIA, respeita a ordem da rota e reserva uma única execução antes
de agendá-la. Testes cobrem filtro por rota, janela quente e coalescência; um smoke do processo sem
credencial confirmou uma única sonda, estado final `COLD` e nenhuma tentativa de inferência infantil.

## Quatro condições da experiência

| Condição | Resultado atual | Prova |
|---|---|---|
| online + IA | resposta estruturada, texto progressivo e áudio; fallback tem deadline | smoke sintético Ollama/NVIDIA/Gemini já registrado; contrato servidor testado |
| online + IA indisponível | circuito/cooldown e mediação segura sem bloquear nova etapa | testes de provider quebrado, deadline, cooldown e `degraded=true` |
| rede cai depois do texto | texto validado é preservado e falado pelo TTS local | teste MockWebServer encerra fluxo antes do áudio e impede retry inútil |
| offline | cinco cenas expressivas mantêm pergunta contextual local; conceitos essenciais continuam locais | teste Android unitário com URL vazia e fluxo Kotlin do Mistério da Bola |

## O que ainda impede declarar produção

- A execução instrumental passou no emulador Android 15/API 35 em 14/09/2026: 18 testes, zero falhas
  e zero ignorados. Isso valida o comportamento automatizado, não substitui piloto com crianças.
- O servidor Oracle e um endereço HTTPS estável ainda não foram configurados; não há medição atual de
  p50/p95 em rede pública.
- Token de tablet e limite por sessão estão implementados e testados localmente, mas sua aplicação
  pública, credencial individual revogável, contenção por rede, consentimento, retenção institucional
  e piloto com alfabetizadores permanecem requisitos anteriores a dados reais de crianças.
- Não há SLA dos endpoints gratuitos. WebSocket, RAG e LangGraph4j continuam fora do turno até uma
  métrica demonstrar benefício maior que o custo de estado, tokens e falha.
