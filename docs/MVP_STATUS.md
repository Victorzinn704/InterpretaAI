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
- ajuda progressiva na pista e na aplicação: a solução por toque não é mostrada antes do pedido;
- trocas do puzzle usam efeito curto, sem fala repetitiva a cada movimento;
- métricas SQLite de participação para professor, sem coluna de acerto, nota, ranking ou áudio bruto;
- encerramento explícito de uso consciente: o aparelho descansa e a atividade continua em dupla;
- servidor Spring Boot 3.5.16, Java 17 e LangChain4j 1.20.0;
- mediação Qwen 2.5 1.5B via Ollama e vozes Kokoro pt-BR feminina/masculina;
- OCR e rótulos de objetos executados localmente no Android por modelos ML Kit embarcados;
- endpoint `POST /api/v1/voice-turn`, memória em RAM de seis mensagens/10 min e limite de três turnos;
- gateway NVIDIA opcional com aquecimento assíncrono iniciado durante a narração, estado `HOT|COLD`
  e circuito frio que preserva resposta local imediata;
- Modo Foco validado em emulador Device Owner com `mLockTaskModeState=LOCKED`;
- auditoria visual em 360×640, 412×915 e 800×1280.

## Evidência de testes

- Android: 12 testes unitários aprovados;
- Android: 15 testes instrumentados aprovados, incluindo investigação progressiva, aplicação, reconexão, níveis 2×2/3×2, clique, arraste e evidência visual;
- servidor: 26 testes aprovados;
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
