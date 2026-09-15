# Estado auditado do MVP

## Implementado e validado

- método LEIA definido e exposto como Ler, Entender, Interpretar e Aprender;
- APK nativo Android em Kotlin/Compose;
- Mistério da Bola fechado: contexto → informação explícita → investigação de pista → puzzle → `BOLA`/`BO-LA`/`B`/`/b/` → orientação útil → dupla;
- fluxo infantil sem swipe na Home, missão fonêmica, interpretação, aplicação, gibi, quebra-cabeça e conclusão;
- jornada controlada por estados, com uma decisão principal por viewport e sem navegação infantil por swipe;
- resposta por voz, TTS local, fallback após seis segundos e reprodução temporária de WAV/OGG;
- reconexão geral testável: indicação em 20 s e fala única em 40 s;
- reconexão específica na aplicação: destaque em 20 s e convite relacional único em 40 s;
- ajuda do puzzle: fala em 15 s, indicação visual em 30 s e pausa durante fala, escuta ou segundo plano;
- quatro sons originais em SoundPool e preferência “Reduzir estímulos”;
- quebra-cabeças de bola, banana e maçã em 2×2 e 3×2, por dois toques ou arraste;
- nível do percurso definido pelo professor: Apoio inicial 2×2 ou Desafio leitor 3×2, sem nova decisão infantil;
- quadro criativo sem rolagem, com traço por arraste, pista visual, quatro cores e desfazer/refazer;
- fluxo local professor → turma/avatar pseudônimo → uma de quatro missões → Home infantil;
- eventos seguintes escopados por turma e avatar, sem nome ou matrícula no aparelho;
- ajuda progressiva na pista e na aplicação: a solução por toque não é mostrada antes do pedido;
- trocas do puzzle usam efeito curto, sem fala repetitiva a cada movimento;
- métricas SQLite de participação para professor, sem coluna de acerto, nota, ranking ou áudio bruto;
- encerramento explícito de uso consciente: o aparelho descansa e a atividade continua em dupla;
- servidor Spring Boot 3.5.16, Java 17 e LangChain4j 1.20.0;
- mediação Qwen 2.5 1.5B via Ollama e vozes Kokoro pt-BR feminina/masculina;
- OCR e rótulos de objetos executados localmente no Android por modelos ML Kit embarcados;
- endpoints `POST /api/v1/voice-turn` e `/voice-turn/stream`, memória em RAM de seis mensagens/10 min e limite de três turnos;
- troca NDJSON `ACK → FINAL_TEXT → COMPLETE`, OkHttp compartilhado e cancelamento ao sair da tela;
- mediação offline contextual nas cinco cenas da galeria e preservação de `FINAL_TEXT` se o áudio remoto cair;
- CTA de voz mantém indicação pulsativa coerente em espera, escuta e processamento; “Reduzir estímulos” preserva o texto e remove a animação;
- `ScenePack` v2 com sete contextos aprovados em memória e rollback configurável para v1;
- cache de TTS limitado a 32 MiB/10 min, por hash do texto e voz, com coalescência concorrente;
- memória de conversa limitada a 2.000 sessões, seis mensagens por sessão e TTL de 10 min, sem lock global;
- gateway remoto opcional com aquecimento sintético de Gemini/NVIDIA iniciado durante a narração, estado `HOT|COLD`
  e circuito frio que preserva resposta local imediata;
- Modo Foco validado em emulador Device Owner com `mLockTaskModeState=LOCKED`, inclusive após tentativas de Home e Recentes;
- auditoria visual em 360×640, 412×915 e 800×1280.

## Evidência de testes

- Android: 26 testes unitários aprovados;
- Android: 18 testes instrumentados aprovados no Android 15/API 35, incluindo investigação progressiva, aplicação, reconexão, estados da voz, níveis 2×2/3×2, clique, arraste e evidência visual;
- servidor: 52 testes aprovados;
- lint Android: aprovado;
- guardrails de layout aprovados em 360×640, 412×915 e 800×1280;
- endpoint local e HTTPS temporário: health UP, conversa e as duas vozes com `degraded=false`;
- PDF: exatamente 10 páginas A4, renderizado e inspecionado.

## Demonstração online temporária

- Quick Tunnel HTTPS validado contra o endpoint público;
- APK gerado com a URL do túnel embutida;
- Mac precisa permanecer ligado com Ollama, Kokoro, Spring e cloudflared;
- não há SLA, autenticação de dispositivo nem persistência no servidor nesta demonstração.

## Preparado, mas não usado nesta entrega

- adaptador Gemini 3.8 Flash com raciocínio `LOW` via LangChain4j, **não autorizado no percurso infantil** sob os termos atuais do Developer API;
- LangGraph4j, RAG curricular e WebSocket de áudio, documentados como arquitetura futura e mantidos
  fora do caminho quente do MVP;
- Google Cloud TTS com Aoede (feminina) e Puck (masculina);
- Dockerfile e configuração para Cloud Run;
- pacote Oracle ARM64 com loopback, units do systemd, Caddy/HTTPS e ambiente sem segredos; validado
  localmente, ainda não implantado em uma VM;
- canal de piloto professor → servidor → tablet: missão versionada por `deviceId`, tokens separados,
  persistência, consulta incremental, configuração adulta e atualização da Home; validado em loopback,
  ainda não implantado na Oracle;
- URL pública configurável no build Android.

O faturamento do Google Cloud não estava ativo. A entrega usa os substitutos locais e não afirma
validação de Gemini, Chirp ou Cloud Run.

## Riscos residuais declarados

- o conteúdo prova um ciclo LEIA, não um currículo completo;
- o recorte 6–10 anos ainda precisa ser calibrado por proficiência com alfabetizadores; o app não diagnostica nível;
- a fundamentação orienta o desenho, mas eficácia de aprendizagem ainda não foi medida em piloto;
- redução de abandono e adequação do nível são hipóteses de produto, ainda não resultados medidos com crianças;
- o servidor público usa túnel temporário, sem SLA, autenticação ou rate limit;
- câmera, microfone, sotaques, ruído e compreensão ainda exigem piloto real;
- a visão da secretaria é futura e não é simulada no MVP;
- publicação entre professor e vários aparelhos, autenticação institucional e vínculo de identidade real ainda não existem; o envio atual é local ao tablet;
- a segunda voz existe no servidor, mas o roteiro completo por personagem ainda precisa de validação;
- bem-estar digital e segurança socioemocional não são tratamento ou diagnóstico clínico;
- identidade, consentimento, retenção e avaliação de impacto são pré-requisitos de produção.
- `SpeechRecognizer` pede operação offline, mas o comportamento real depende do mecanismo/OEM;
- limpeza de cache em encerramento abrupto e expiração autônoma da memória ainda precisam de endurecimento.

O mapeamento literal aos critérios está em [HACKATHON_CRITERIA.md](HACKATHON_CRITERIA.md).
O inventário técnico completo está em [DATA_AND_PRIVACY.md](DATA_AND_PRIVACY.md).

## Evolução futura

- deploy e observabilidade do Cloud Run;
- sincronização autenticada e visão agregada da secretaria;
- identidade institucional para educadores;
- atividades de casa com responsáveis;
- validação pedagógica, de privacidade e acessibilidade em piloto real.
