# Estado auditado do MVP

## Implementado e validado

- APK nativo Android em Kotlin/Compose;
- fluxo infantil sem swipe na Home, gibi, formação de BOLA, quebra-cabeça e conclusão;
- três estados por cena: observar/ouvir, responder e receber reação;
- resposta por voz, TTS local, fallback após seis segundos e reprodução temporária de WAV/OGG;
- reconexão testável: indicação em 20 s, fala única em 40 s e pausa durante escuta/resposta;
- quatro sons originais em SoundPool e preferência “Reduzir estímulos”;
- quebra-cabeças de bola, banana e maçã em 2×2 e 3×2;
- métricas SQLite para professor, sem áudio bruto;
- servidor Spring Boot 3.5.16, Java 17 e LangChain4j 1.20.0;
- mediação Qwen 2.5 3B via Ollama e vozes Kokoro pt-BR feminina/masculina;
- OCR e rótulos de objetos executados localmente no Android por modelos ML Kit embarcados;
- endpoint `POST /api/v1/voice-turn`, memória em RAM de seis mensagens/10 min e limite de três turnos;
- Modo Foco validado em emulador Device Owner com `mLockTaskModeState=LOCKED`;
- auditoria visual em 360×640, 412×915 e 800×1280.

## Evidência de testes

- Android: 8 testes unitários aprovados;
- Android: 3 testes instrumentados aprovados;
- servidor: 6 testes aprovados;
- lint Android: aprovado;
- endpoint local e HTTPS temporário: health UP, conversa e as duas vozes com `degraded=false`;
- PDF: exatamente 10 páginas A4, renderizado e inspecionado.

## Demonstração online temporária

- Quick Tunnel HTTPS validado contra o endpoint público;
- APK gerado com a URL do túnel embutida;
- Mac precisa permanecer ligado com Ollama, Kokoro, Spring e cloudflared;
- não há SLA, autenticação de dispositivo nem persistência no servidor nesta demonstração.

## Preparado, mas não usado nesta entrega

- Gemini 2.5 Flash via LangChain4j;
- Google Cloud TTS com Aoede (feminina) e Puck (masculina);
- Dockerfile e configuração para Cloud Run;
- URL pública configurável no build Android.

O faturamento do Google Cloud não estava ativo. A entrega usa os substitutos locais e não afirma
validação de Gemini, Chirp ou Cloud Run.

## Evolução futura

- deploy e observabilidade do Cloud Run;
- sincronização autenticada e visão agregada da secretaria;
- identidade institucional para educadores;
- atividades de casa com responsáveis;
- validação pedagógica, de privacidade e acessibilidade em piloto real.
