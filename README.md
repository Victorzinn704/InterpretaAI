# InterpretaAI — MVP Android

Protótipo nativo Android para alfabetização infantil por voz, inspirado nas telas do export Stitch `stitch_kids_literacy_android_mvp`.

O inventário do que está implementado, do que precisa de teste em tablet e do que continua pendente está
em [docs/MVP_STATUS.md](docs/MVP_STATUS.md).

## Atualização: gibi LEIA e cenas expressivas

O botão Modo Escola agora abre **A bola e os amigos** ou a galeria de quadrinhos expressivos.
Cinco cenas ilustradas com Lia e Davi abordam choro, raiva, riso, felicidade e locomoção por meio do contexto e dos diálogos em balões.
O gibi termina com a montagem de BOLA em um bilhete e uma proposta de conversa presencial.
As escolhas feitas pela turma permanecem no percurso, formam o resumo final da história e viram evidências locais para o professor.
Cada cena, opções e retornos são narrados pelo Text-to-Speech do Android, com repetição pelo alto-falante.
É necessário ter uma voz em português instalada; a naturalidade depende do sintetizador do aparelho.
O app prioriza uma voz `pt-BR` disponível no aparelho e usa ritmo e tom mais acolhedores. Nas telas
infantis, a próxima ação pulsa e muda suavemente de cor; quando há conteúdo abaixo, o botão
`VER MAIS` movimenta a tela sem depender da descoberta do gesto de rolagem.

O menu LEIA também oferece quebra-cabeças de **bola**, **banana** e **maçã**. A criança escolhe entre
2 × 2 e 3 × 2 e troca duas peças tocando primeiro em uma e depois em outra. A atividade narra
instruções e palavras, oferece uma pista visual e registra conclusão, movimentos, tempo e ajuda.

As ilustrações são originais e ficam embarcadas no APK, sem depender de internet. Não há geração de
histórias, reconhecimento de emoções ou conversa por IA nesta versão. As escolhas possuem respostas
preparadas e não classificam a criança. A nova jornada registra interpretações e ciclos LEIA concluídos,
sem misturá-los ao cálculo de precisão da missão M.
O fluxo anterior permanece no código, mas não é a entrada principal do Modo Escola.

Este incremento é restrito ao gibi, às cenas expressivas e aos três quebra-cabeças. A grade 3 × 3,
fotografia incorporada ao quadrinho e integrações de IA discutidas anteriormente permanecem pendentes.

Teste do percurso no Android: `./gradlew connectedDebugAndroidTest`.

## O que já funciona

- fluxo da missão da letra M em oito momentos: início, fonema, interpretação, escolha de modalidade, câmera, conversa guiada e conclusão;
- leitura das instruções com Text-to-Speech em português;
- resposta por voz sem sair do app, usando `SpeechRecognizer`;
- câmera embutida com CameraX;
- métricas locais offline em SQLite, sem salvar áudio bruto;
- painel do professor protegido pelo PIN de demonstração `2468`;
- modo imersivo para aparelhos comuns;
- modo quiosque completo, launcher padrão e início após boot quando o app é Device Owner;
- acesso opcional ao “Não Perturbe”, solicitado na área do educador.

## Compilar

Requisitos: JDK 17 ou 21 e Android SDK 35.

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar para demonstração

Com um Android conectado e depuração USB habilitada:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Isso permite o modo imersivo e a fixação de tela. O bloqueio de totem completo exige provisionamento institucional descrito em [docs/KIOSK.md](docs/KIOSK.md).

## Arquitetura

O MVP usa uma única Activity, Jetpack Compose e pacotes por responsabilidade. A justificativa e o caminho até professor/secretaria estão em [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). O contrato inicial de eventos está em [docs/API_CONTRACT.md](docs/API_CONTRACT.md).

## Decisões de privacidade

- o app não grava nem persiste áudio; pede preferência por reconhecimento offline ao serviço de voz instalado no aparelho;
- fotografia de atividade é criada no cache e não entra nas métricas;
- identifica a criança por alias no MVP, nunca por nome completo;
- secretaria recebe agregados; criança não deve virar ranking;
- câmera e microfone são pedidos somente quando a funcionalidade é tocada.

Antes de um piloto real, a secretaria/controlador deve definir base legal, aviso de privacidade acessível, retenção, perfis de acesso, canal aos responsáveis e avaliação de impacto. Este repositório é uma base técnica, não um parecer jurídico.
