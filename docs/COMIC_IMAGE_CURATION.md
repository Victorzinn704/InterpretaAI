# Curadoria visual dos gibis — método LEIA

Data: 18/09/2026
Escopo: histórias infantis do APK, com prioridade para leitura visual em celular e tablet.

## Regra editorial

Cada quadro deve ter **um tema central, uma pista principal e no máximo um elemento de comparação**.
Personagens, cenário e efeitos só permanecem quando ajudam a criança a localizar, entender ou explicar
essa pista. A imagem não pode revelar a solução antes da pergunta, depender de texto desenhado nem
usar enfeites que concorram com a ação pedagógica.

O enquadramento segue seis critérios:

1. a pista principal continua visível no recorte 16:9 do celular compacto;
2. olhar, mão ou postura dos personagens conduzem a atenção sem seta artificial;
3. cor recorrente identifica a pista entre os quadros;
4. a solução só aparece depois da interpretação;
5. expressões são claras, mas não infantilizadas para o público de 6–10 anos;
6. a fala da LÉIA nomeia onde olhar, sem descrever toda a imagem pela criança.

## Mistério da Bola

| LEIA | Quadro aprovado | Elemento para observar | Decisão editorial |
|---|---|---|---|
| **Ler** | `comic_ball_story_01_missing_v2.webp` | espaço vazio, marca circular e pequena meta | não mostrar a bola; Lia apresenta o problema e Davi oferece ajuda |
| **Entender** | recordação oral do primeiro quadro | o que falta para a brincadeira começar | preservar resposta por voz antes de oferecer a figura |
| **Interpretar** | `comic_ball_story_02_trail_v2.webp` | marcas circulares molhadas entre a poça e a árvore | indicar destino pelo cenário, sem revelar a bola |
| **Aprender** | puzzle e `comic_ball_celebration_v1.webp` | forma da bola, palavra `BOLA` e retorno à brincadeira | a solução visual aparece somente depois da investigação |

Os quadros anteriores `comic_scene_1_v2.jpg` e `comic_ball_clue_v1.webp` continuam no inventário,
mas deixam de conduzir o fluxo principal: ambos mostravam a bola antes da resposta. O primeiro ainda
pode permanecer na galeria expressiva; o segundo não deve ser usado como pista de investigação.

## A Água da Chuva

| LEIA | Quadro aprovado | Elemento para observar | Elemento secundário permitido |
|---|---|---|---|
| **Ler** | `comic_rain_path_01_shelter_v1.webp` | folhas amarelas carregadas pela água | chuva contextualiza o movimento |
| **Entender** | `comic_rain_path_02_stream_v1.webp` | correnteza contornando a pedra | a pedra explica mudança de direção |
| **Interpretar** | `comic_rain_path_03_routes_v1.webp` | água e folhas no caminho da ponte | caminho seco da porta permite comparação |
| **Aprender** | `comic_rain_path_04_garden_v1.webp` | terra molhada junto às raízes | reflexo colorido funciona apenas como recompensa final |

As folhas amarelas são o fio visual da história. Flores, personagens e arquitetura não podem receber
mais contraste que folhas, água e raízes. Chuva e bola permanecem episódios independentes.

## Galeria de expressões

As cinco cenas de choro, raiva, riso, felicidade e locomoção são cartões independentes de conversa.
Elas não entram no meio de “Mistério da Bola” ou “A Água da Chuva”, porque mudariam o tema central.
A curadoria futura deve preservar uma situação observável por cartão e evitar declarar uma emoção
como resposta absoluta: a criança descreve a evidência que percebe.

## Prompts finais dos dois novos quadros

Ferramenta: gerador de imagens integrado (`imagegen`), modo nativo. As referências foram usadas
somente para identidade dos personagens e acabamento visual.

### Bola 1 — o que está faltando

> Use case: illustration-story. Asset type: first comic panel for a Brazilian literacy app, story
> “Mistério da bola”. Create a NEW scene, using the references only to preserve the exact identities,
> age, skin tones, hair, clothes and polished hand-painted comic style of Lia, Davi and Alfa. Scene:
> Brazilian public-school courtyard beside a small goal made from two bright cones. Lia looks mildly
> concerned and opens her hands because the ball they need is missing; Davi calmly offers to help;
> Alfa looks at the empty play area. Place one subtle clean circular mark in the dust where the ball
> had rested, readable but not dominant. The visual question must be “what is missing from this game?”
> without showing the answer. Composition: 4:3 landscape, medium-wide, strong hierarchy for phone
> crop, bold outlines, expressive but not babyish for ages 6–10. NO ball, footprints, adult/LÉIA,
> text, arrows, logos, watermark, map interface or copyrighted-franchise imitation.

### Bola 2 — para onde as marcas levam

> Use case: illustration-story. Asset type: second comic panel for the same story. Preserve Lia,
> Davi and Alfa. In the same courtyard, a shallow puddle is beside the empty cone goal. A clear but
> natural sequence of three curved wet circular marks from a rolling round object crosses the dust
> toward one large tree. Davi points to the marks nearest the tree, Lia follows the path with her eyes,
> and Alfa sniffs beside the trail without creating paw prints. The child must infer WHERE to search;
> do not reveal the missing object. Composition: 4:3 landscape, readable on a phone, marks as the main
> visual line, simple background, bold outlines, expressive but not babyish for ages 6–10. NO ball,
> paw-print trail, adult/LÉIA, text, arrows, logos, watermark, map interface or franchise imitation.

Arquivos-fonte gerados:

- `/Users/joaovictordemoraesdacruz/.codex/generated_images/01a0974f-5126-7cb1-8133-bed454e2bedb/exec-3a2f5fbe-88dc-495f-a46f-561ec5209c0a.png`
- `/Users/joaovictordemoraesdacruz/.codex/generated_images/01a0974f-5126-7cb1-8133-bed454e2bedb/exec-877abe56-7e22-41b4-894b-5cce39e5b9f5.png`

## Critério para novas histórias

Antes de gerar uma imagem, a equipe registra: tema, pergunta oral, pista principal, comparação válida,
elemento que só pode aparecer na conclusão e recorte seguro. Um quadro é recusado quando fica bonito,
mas não muda o que a criança observa, entende, interpreta ou consegue explicar.

## Interação implementada no quadro

As seis imagens centrais das duas histórias possuem agora um alvo proporcional sobre a própria pista.
A criança pode tocar na marca, nas folhas, na água ou nas raízes; o aplicativo responde com som curto,
destaque localizado e uma frase contextual da LÉIA. O alvo possui no mínimo 56dp, descrição semântica
e não substitui os botões nem a resposta por voz.

Após 20 segundos sem descoberta, o alvo pulsa discretamente com um olho. No modo “Reduzir estímulos”,
essa indicação automática e sua animação não são exibidas; o ponto continua tocável e produz confirmação
visual somente depois da ação da criança. O destaque desaparece com a mudança de quadro e nunca usa
seta permanente, confete contínuo ou movimento no cenário inteiro.
