# Acessibilidade da jornada infantil — fonte de autoria v1

- **Estado:** candidato à revisão de acessibilidade e pedagogia
- **Proprietário:** InterpretaAI
- **Uso:** composição, fala, apoio e validação de cenas

## Princípios obrigatórios

1. uma decisão principal por viewport infantil;
2. nenhuma ação essencial depende de rolagem, leitura autônoma ou gesto oculto;
3. texto visível mínimo de 16sp e alvo interativo mínimo de 48dp;
4. instrução essencial disponível em áudio e de forma visual;
5. conteúdo preparado funciona offline;
6. reduzir ilustração/espaço antes de reduzir texto;
7. toque é sempre alternativa a voz; puzzle aceita toque e arraste;
8. modo de estímulos reduzidos preserva voz, contraste e indicação de ação.

## Apoio progressivo

O apoio respeita tentativa independente e não revela tudo de uma vez:

- `NONE`: convite inicial;
- `VOICE_REPEAT`: repetir ou reformular;
- `VISUAL_CUE`: destacar elemento relevante sem piscar agressivamente;
- `CHOICE_REVEALED`: oferecer conjunto curto de opções.

Seta, pulso e cor orientam sem depender somente da cor. Animação de orientação é breve, pode ser
reduzida e não disputa atenção com a fala.

## Áudio e voz

- voz principal da LÉIA feminina, suave e consistente;
- personagens podem ter vozes distintas sem imitação de pessoa/personagem protegido;
- volume não aumenta subitamente;
- fala pode ser repetida pelo usuário;
- TTS local preparado é fallback quando áudio remoto não chega;
- efeito sonoro não substitui fonema, letra, palavra ou instrução.

## Tempo e reconexão

O temporizador pausa enquanto a criança fala, o aplicativo responde ou fica em segundo plano. Uma
primeira indicação visual pode ocorrer após 20 segundos; uma fala única após 40 segundos. Qualquer
interação reinicia a etapa. Esses valores são configuração testável, não inferência de atenção.

## Responsividade

Auditar no mínimo 360×640dp, 412×915dp e 800×1280dp. Reprovar quando houver:

- CTA cortado ou coberto;
- sobreposição de texto/controle;
- fonte abaixo do mínimo;
- ação apenas por swipe;
- instrução essencial somente escrita;
- estado sem saída offline;
- animação indispensável sem alternativa reduzida.

## Neurodiversidade

Oferecer previsibilidade, escolha de modalidade, redução de estímulos e apoio progressivo sem criar
uma trilha automática “para neurodivergentes”. Diagnóstico, perfil clínico e adaptação automática
baseada em rótulo não pertencem ao pacote nem ao relatório.
