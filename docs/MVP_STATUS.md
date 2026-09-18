# Estado auditado do MVP

> Esta página registra a entrega original. Em 17/09/2026, o responsável informou que conectou
> um servidor Oracle e a [verificação pública](v2/ORACLE_PUBLIC_CHECK.md) encontrou health `UP`
> e gateway v1 `HOT`; isso não altera retroativamente o estado da entrega nem comprova a API
> docente v2, que ainda retornou 404.

## Implementado e validado

- método LEIA definido e exposto como Ler, Entender, Interpretar e Aprender;
- APK nativo Android em Kotlin/Compose;
- Mistério da Bola fechado: contexto → informação explícita → investigação de pista → puzzle → `BOLA`/`BO-LA`/`B`/`/b/` → orientação útil → dupla;
- A Água da Chuva fechada como episódio independente: chuva → folhas na correnteza → comparação
  objetiva entre evidência molhada e caminho seco → terra molhada junto às raízes; sem bola, objeto
  perdido ou puzzle reaproveitado;
- curadoria visual LEIA aplicada aos dois gibis: um foco por quadro, solução preservada até a
  interpretação, marcas coerentes no Mistério da Bola e pistas nomeadas em voz e imagem;
- pistas tocáveis nas seis imagens centrais: resposta sonora, reação localizada, fala contextual,
  alvo semântico mínimo e dica discreta após 20 segundos, sem pulso automático no modo reduzido;
- fluxo infantil sem swipe na Home, missão fonêmica, interpretação, aplicação, gibi, quebra-cabeça e conclusão;
- jornada controlada por estados, com uma decisão principal por viewport e sem navegação infantil por swipe;
- resposta por voz, TTS local, fallback após seis segundos e reprodução temporária de WAV/OGG;
- reconexão geral testável: indicação em 20 s e fala única em 40 s;
- reconexão específica na aplicação: destaque em 20 s e convite relacional único em 40 s;
- ajuda do puzzle: fala em 15 s, indicação visual em 30 s e pausa durante fala, escuta ou segundo plano;
- quatro sons originais em SoundPool e preferência “Reduzir estímulos”;
- quebra-cabeças de bola, banana e maçã em 2×2 e 3×2, por dois toques ou arraste;
- nível do percurso definido pelo professor: Apoio inicial 2×2 ou Desafio leitor 3×2, sem nova decisão infantil;
- quadro criativo sem rolagem, com ponto por toque ou traço suavizado por arraste, pista visual,
  quatro cores, duas espessuras, borracha gestual e desfazer/refazer com estado reativo;
- três missões fechadas novas: caminho 1→5, pontos que desenham CASA e foto ilustrativa para
  organizar B-O-L-A; conteúdo sem chamada remota e conclusão com explicação ao colega;
- as três missões têm estados descritos para acessibilidade e convite suave por inatividade,
  uma única vez por etapa; a fala é mantida quando “Reduzir estímulos” está ativo;
- LÉIA adulta original com Alfa na Home, e foto sintética de bola no jogo de letras; recursos
  embarcados de 544 e 232 KiB, respectivamente, com proveniência documentada;
- fluxo local professor → turma/pseudônimo/avatar → uma de onze missões → Home infantil;
- pacotes falados na mesma rota do gibi para ordem narrativa no 2º ano e causa/consequência no 3º,
  concluídos por justificativa ao grupo e sem nova tela;
- dois pacotes falados na mesma rota do gibi: fato/opinião para 4º e comparação de fontes para 5º,
  sempre concluídos com justificativa em grupo e sem inferência remota obrigatória;
- cada pacote do 2º ao 5º preserva um `activityId` fechado nos eventos, aceita somente uma conclusão
  e encerra com devolutiva própria da habilidade e proposta de continuação fora da tela;
- contrato local de servidor para sala com até 40 pseudônimos e envio transacional para turma ou
  subconjunto; a área adulta do Android monta a sala, seleciona participantes e envia para todos,
  dupla, grupo ou indivíduo;
- painel docente organizado em Missão → Turma → Tablet: a escolha pedagógica aparece primeiro,
  métricas são consultadas sob demanda e tokens/IDs ficam em configuração explícita;
- filtro docente por 1º–5º ano na própria lista de missões, com `TODAS` explícito para recomposição;
  o filtro organiza o planejamento, não classifica a criança nem gera trilha automática;
- tablet compartilhado com dois a quatro avatares: seleção agrupada por `deviceId`, uma missão por
  aparelho, Home sem aliases e evento `GROUP` com contagem, sem atribuição falsa a uma criança;
- rodízio colaborativo em todas as rotas infantis publicáveis: os avatares recebem por voz e imagem
  quem procura pistas, responde, manipula/desenha e explica, sem expor aliases ou gerar métrica individual;
- eventos individuais escopados por turma e `learnerAlias`, separado da aparência do avatar; eventos
  de tablet compartilhado usam escopo coletivo e contagem, sempre sem nome ou matrícula no aparelho;
- ajuda progressiva no objeto, na pista e na aplicação: a resposta por toque não aparece antes do
  pedido, mas permanece disponível para quem não usar o microfone;
- trocas do puzzle usam efeito curto, sem fala repetitiva a cada movimento;
- métricas SQLite de participação para professor, sem coluna de acerto, nota, ranking ou áudio bruto;
- outbox SQLite v3 com IDs idempotentes e sincronização oportunista de eventos fechados em lotes de
  até 50, sem alias, turma, resposta livre, mídia ou transcrição no payload do tablet;
- agregados de turma e rede no servidor, com credenciais distintas para professor e secretaria e
  auditoria de cada leitura; ainda não existe painel web nem RBAC institucional;
- encerramento explícito de uso consciente: o aparelho descansa e a atividade continua em dupla;
- servidor Spring Boot 3.5.16, Java 17 e LangChain4j 1.20.0;
- mediação Qwen 2.5 1.5B via Ollama e vozes Kokoro pt-BR feminina/masculina;
- OCR e rótulos de objetos executados localmente no Android por modelos ML Kit embarcados;
- endpoints `POST /api/v1/voice-turn` e `/voice-turn/stream`, memória em RAM de seis mensagens/10 min e limite de três turnos;
- troca NDJSON `ACK → FINAL_TEXT → COMPLETE`, OkHttp compartilhado e cancelamento ao sair da tela;
- mediação offline contextual nas cinco cenas da galeria e preservação de `FINAL_TEXT` se o áudio remoto cair;
- CTA de voz mantém indicação pulsativa coerente em espera, escuta e processamento; “Reduzir estímulos” preserva o texto e remove a animação;
- `ScenePack` v2 com sete contextos aprovados em memória e rollback configurável para v1;
- desvio determinístico por resposta aprovada no `ScenePack`, validado no caso “bola” sem chamar o LLM;
- cache de TTS limitado a 32 MiB/10 min, por hash do texto e voz, com coalescência concorrente;
- memória de conversa limitada a 2.000 sessões, seis mensagens por sessão e TTL de 10 min, sem lock global;
- gateway remoto opcional com aquecimento sintético de Gemini/NVIDIA iniciado durante a narração, estado `HOT|COLD`
  e circuito frio que preserva resposta local imediata;
- Modo Foco validado em emulador Device Owner com `mLockTaskModeState=LOCKED`, inclusive após tentativas de Home e Recentes;
- diagnóstico adulto de compatibilidade do tablet, copiável sem serial, IMEI, conta, IP, token ou
  dado infantil, com hardware, voz, TTS, caneta e estados separados de permissão/atividade do Lock Task;
- auditoria visual em 360×640, 412×915 e 800×1280.

## Evidência de testes

- Android: 94 testes unitários aprovados;
- Android: 52 testes instrumentados aprovados no Android 15/API 35, incluindo pistas tocáveis, A Água da Chuva e as três missões novas,
  investigação progressiva, aplicação, reconexão, estados da voz, níveis 2×2/3×2, clique, arraste,
  quadro, pacotes falados, tablet compartilhado, rodízio, fluxo docente e evidência visual;
- servidor: 172 testes aprovados e 1 smoke opt-in ignorado;
- lint Android: aprovado;
- guardrails de layout aprovados em 360×640, 412×915 e 800×1280;
- as três missões novas e a Home com LÉIA foram capturadas nos mesmos três perfis, sem CTA cortado;
- o teste instrumentado cobre seleção/publicação local das três missões pelo educador, e o teste
  de cliente cobre envio e recebimento dos três IDs com respostas simuladas; não é smoke test da
  Oracle nem comprova recepção em tablet físico;
- smoke HTTP local do JAR com H2 temporário: as três missões foram publicadas via `PUT` e lidas
  via `GET`, com 401 sem token e 204 quando não havia versão nova; essa foi a evidência local anterior
  à promoção do ambiente Oracle;
- smoke Android→HTTPS temporário→servidor: Home do emulador Android 15 recebeu automaticamente
  as três missões em sequência e abriu a atividade de imagem; túnel encerrado e sem aparelho físico
  ou rede escolar;
- APK universal preservado e variante ARM64 de aproximadamente 42 MiB criada para dispositivos
  `arm64-v8a` (universal: aproximadamente 101 MiB); os jogos não processam OCR/IA em cada toque;
- endpoint local e HTTPS temporário: health UP, conversa e as duas vozes com `degraded=false`;
- PDF: exatamente 10 páginas A4, renderizado e inspecionado.

## Demonstração online temporária original

- Quick Tunnel HTTPS validado contra o endpoint público;
- APK gerado com a URL do túnel embutida;
- Mac precisa permanecer ligado com Ollama, Kokoro, Spring e cloudflared;
- não há SLA, autenticação de dispositivo nem persistência no servidor nesta demonstração.

## Ambiente Oracle atual, posterior à entrega original

- `https://interpretaai.deskimperial.online` respondeu com health `UP` e gateway `HOT`;
- Spring v2, PostgreSQL, HTTPS, Keycloak 26.7.4 e Estúdio do Professor foram verificados;
- login OIDC, callback, sessão BFF e vínculo restrito à escola piloto passaram no smoke test;
- o ambiente continua sendo infraestrutura piloto própria, sem parceria, SLA institucional ou
  validação em tablet físico da rede;
- detalhes e limites estão em [Verificação pública da Oracle](v2/ORACLE_PUBLIC_CHECK.md).

## Preparado, mas não usado no percurso infantil

- adaptador Gemini 3.8 Flash com raciocínio `LOW` via LangChain4j, **não autorizado no percurso infantil** sob os termos atuais do Developer API;
- LangGraph4j, RAG curricular e WebSocket de áudio, documentados como arquitetura futura e mantidos
  fora do caminho quente do MVP;
- Google Cloud TTS com Aoede (feminina) e Puck (masculina);
- Dockerfile e configuração para Cloud Run;
- canal professor → servidor → tablet em aparelho físico escolar; contrato, persistência, consulta
  incremental e Estúdio existem, mas o percurso completo ainda precisa de ensaio autorizado;
- autenticação opcional do turno online pelo token do tablet, ativada no exemplo Oracle e enviada em
  cabeçalho pelo Android; ainda é segredo compartilhado de piloto, não identidade institucional;
- URL pública configurável no build Android.

O faturamento do Google Cloud não estava ativo. A entrega usa os substitutos locais e não afirma
validação de Gemini, Chirp ou Cloud Run.

## Riscos residuais declarados

- o conteúdo prova um ciclo LEIA, não um currículo completo;
- o recorte 6–10 anos ainda precisa ser calibrado por proficiência com alfabetizadores; o app não diagnostica nível;
- a fundamentação orienta o desenho, mas eficácia de aprendizagem ainda não foi medida em piloto;
- redução de abandono e adequação do nível são hipóteses de produto, ainda não resultados medidos com crianças;
- o endpoint Oracle está ativo e autenticado no piloto próprio, mas ainda precisa de rate limit,
  observabilidade/SLA acordados, identidade institucional e teste de carga antes de uso escolar;
- câmera, microfone, sotaques, ruído e compreensão ainda exigem piloto real;
- o diagnóstico reduz a incerteza por aparelho, mas o modelo dos tablets GET continua desconhecido
  até coleta autorizada ou inventário oficial; emulador não comprova compatibilidade do parque real;
- o Estúdio adulto e o escopo piloto por escola estão ativos; relatórios de secretaria, federação com
  identidade institucional e validação dos indicadores ainda são futuros;
- publicação por `deviceId` e recebimento automático na Home existem no canal de piloto; gestão de
  vários aparelhos, autenticação institucional e vínculo de identidade real ainda não existem;
- a segunda voz existe no servidor, mas o roteiro completo por personagem ainda precisa de validação;
- bem-estar digital e segurança socioemocional não são tratamento ou diagnóstico clínico;
- identidade, consentimento, retenção e avaliação de impacto são pré-requisitos de produção;
- `SpeechRecognizer` pede operação offline, mas o comportamento real depende do mecanismo/OEM;
- limpeza de cache em encerramento abrupto e expiração autônoma da memória ainda precisam de endurecimento.

O mapeamento literal aos critérios está em [HACKATHON_CRITERIA.md](HACKATHON_CRITERIA.md).
O inventário técnico completo está em [DATA_AND_PRIVACY.md](DATA_AND_PRIVACY.md).

## Evolução futura

- deploy e observabilidade do Cloud Run;
- painel da secretaria e autenticação institucional com escopo por rede/escola/turma;
- retry periódico por WorkManager e política automatizada de retenção/expurgo;
- identidade institucional para educadores;
- atividades de casa com responsáveis;
- validação pedagógica, de privacidade e acessibilidade em piloto real.
