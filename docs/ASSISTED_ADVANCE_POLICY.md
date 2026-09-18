# Política de avanço com apoio

Data: 18/09/2026
Estado: implementada no Android; contrato de evento aceito pelo servidor desta versão.

## Regra do produto

Cada etapa digital ativa tem uma janela de **três minutos de participação efetiva**. Etapas com uma
resposta objetivamente verificável também avançam após **três tentativas não concluídas**. O relógio
para quando o aplicativo está em segundo plano, quando a criança fala, quando a LÉIA responde e quando
a etapa já foi concluída.

“Tentativa não concluída” não significa erro da criança. Trocar peças, desenhar, explorar uma imagem,
formular interpretação aberta e conversar com a dupla não são erros. Nessas experiências só se aplica
o limite de tempo. A contagem de tentativas é reservada a relações fechadas, como ordem numérica,
letra esperada, pista explícita, palavra-alvo ou objeto solicitado pela câmera.

## Transição infantil

Ao atingir um limite, a atividade sai do estado de correção e apresenta uma única tela de acolhimento:
LÉIA se aproxima de Alfa, ele demonstra uma tristeza leve e ambos convidam a continuar. A fala não
usa culpa, nota, vermelho punitivo ou “você errou”. A criança pode tocar em **Vamos continuar**; se não
tocar, a próxima etapa abre após oito segundos.

- Por tentativas: “Não foi dessa vez, e tudo bem. LÉIA e Alfa tentam de novo com você na próxima.”
- Por tempo: “Esta parte terminou. LÉIA e Alfa seguem com você. Vamos para a próxima?”

## Registro pedagógico

O aplicativo grava somente `STAGE_ADVANCED_WITH_SUPPORT` com `time_limit` ou `attempt_limit`, atividade,
modalidade e horário. O painel mostra esses totais como sinal para retomar a mediação. Não há nota,
percentual de acerto, transcrição, diagnóstico ou classificação da criança. Uma passagem assistida
também não é somada como etapa dominada.

## Cobertura atual

- jogos de número, pontos e imagem→letras: tempo e três tentativas verificáveis;
- missão do som M, interpretação e câmera: tempo e tentativas verificáveis;
- leitura orientada com justificativa: somente tempo, porque interpretações sustentadas não viram gabarito;
- gibi da bola e formação de palavra: tempo e tentativas nas perguntas fechadas;
- quebra-cabeça e quadro de desenho: tempo, sem chamar manipulação ou traço de erro;
- história preparada offline: tempo por nó e tentativas somente no formador de palavra;
- conversa em dupla: encerra sua janela de três minutos com transição falada.

## Riscos que permanecem

- Três minutos é uma hipótese de piloto, não um parâmetro clínico. A professora deve poder ajustá-lo em
  evolução posterior, com validação por faixa/atividade.
- O número de avanços com apoio não mede aprendizagem isoladamente. Deve ser lido junto de pedidos de
  ajuda, modalidade e observação docente.
- Reconhecimento de voz e visão podem falhar por ruído, sotaque, luz ou aparelho; uma falha técnica não
  pode ser interpretada como dificuldade da criança.
- A pose e as frases precisam ser observadas com crianças para confirmar acolhimento sem infantilização
  excessiva no 4º e 5º ano.

## Testes de aceitação

1. nada acontece antes dos limites;
2. exatamente três tentativas fechadas acionam acolhimento;
3. exploração aberta nunca incrementa tentativa;
4. a tela informa a passagem e possui CTA visível sem rolagem;
5. o evento informa a causa sem registrar resposta ou rótulo;
6. fala, processamento e segundo plano não consomem o tempo da criança.

Capturas reais da mesma transição: [360×640](../output/screenshots/assisted-advance/360x640/avanco-com-apoio-tentativas-360x640.png),
[412×915](../output/screenshots/assisted-advance/412x915/avanco-com-apoio-tentativas-412x915.png) e
[800×1280](../output/screenshots/assisted-advance/800x1280/avanco-com-apoio-tentativas-800x1280.png).

## Rastro da ilustração

Fonte de identidade: `leia_and_alfa_v1.webp`. O prompt original foi preservado abaixo como rastro
histórico; na interface e nos contratos atuais, o personagem se chama **Alfa**.

> Edit this exact established InterpretaAI character asset while preserving the identity, face,
> clothes, colors, line weight, proportions, and original children's comic illustration style of
> LÉIA and her dog. Create a supportive transition pose for children ages 6–10: LÉIA is crouching
> gently beside the dog, with a calm understanding expression and a small reassuring smile, one hand
> softly resting near the dog's shoulder and the other open inviting the child to continue. The dog
> looks mildly sad and thoughtful with ears slightly lowered, but is NOT crying, frightened, ashamed,
> defeated, or distressed. Their body language must communicate “it did not work this time, and we
> can keep learning together.” Full bodies visible, centered composition, transparent background, no
> text, no speech bubbles, no confetti, no extra characters, no logos, no copyrighted franchise
> imitation. Clean high-quality PNG suitable for an Android drawable and readable at small
> tablet/mobile sizes.
