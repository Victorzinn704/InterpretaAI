# Estado auditado do MVP

## Implementado e validado

- APK nativo Android em Kotlin/Compose;
- fluxo infantil sem swipe na Home, gibi, formação de BOLA, quebra-cabeça e conclusão;
- três estados por cena: observar/ouvir, responder e receber reação;
- resposta por voz, TTS local, fallback após seis segundos e reprodução de OGG temporário;
- reconexão testável: indicação em 20 s, fala única em 40 s e pausa durante escuta/resposta;
- quatro sons originais em SoundPool e preferência “Reduzir estímulos”;
- quebra-cabeças de bola, banana e maçã em 2×2 e 3×2;
- métricas SQLite para professor, sem áudio bruto;
- servidor Spring Boot 3.5.16, Java 17, LangChain4j 1.20.0 e Google GenAI beta30;
- endpoint `POST /api/v1/voice-turn`, memória em RAM de seis mensagens/10 min e limite de três turnos;
- Modo Foco validado em emulador Device Owner com `mLockTaskModeState=LOCKED`;
- auditoria visual em 360×640, 412×915 e 800×1280.

## Evidência de testes

- Android: 8 testes unitários aprovados;
- Android: 3 testes instrumentados aprovados;
- servidor: 6 testes aprovados;
- lint Android: aprovado;
- endpoint local: health UP e fallback estruturado com `degraded=true`;
- PDF: exatamente 10 páginas A4, renderizado e inspecionado.

## Preparado, mas não validado em nuvem

- Gemini 2.5 Flash via LangChain4j;
- Google Cloud TTS com Aoede (feminina) e Puck (masculina);
- Dockerfile e configuração para Cloud Run;
- URL pública configurável no build Android.

Não havia `gcloud`, autenticação Google Cloud, `GEMINI_API_KEY` ou credencial de service account
neste ambiente. Portanto, não existe URL pública nem smoke real das vozes nesta entrega.

## Evolução futura

- deploy e observabilidade do Cloud Run;
- sincronização autenticada e visão agregada da secretaria;
- identidade institucional para educadores;
- atividades de casa com responsáveis;
- validação pedagógica, de privacidade e acessibilidade em piloto real.
