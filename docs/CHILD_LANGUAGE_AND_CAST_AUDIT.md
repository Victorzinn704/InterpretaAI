# Auditoria de linguagem infantil e elenco

Data: 18/09/2026
Escopo: textos visíveis e falas locais do APK; respostas offline; falas do gibi; nomes e descrições
acessíveis dos personagens; contrato de histórias preparadas. Público: 6 a 10 anos.

## Decisão editorial

- **LÉIA** é a educadora/personagem; **LEIA** é somente o método Ler, Entender, Interpretar e Aprender.
- **Alfa** é o companheiro recorrente de LÉIA. Na experiência infantil ele nunca é chamado de
  “o cachorro” ou “seu cachorro”. Lia e Davi continuam como protagonistas infantis.
- A voz fala como alguém presente na brincadeira: uma ideia por vez, até duas frases curtas e uma
  pergunta ou ação seguinte. Ela não lê rótulos de interface nem explica o funcionamento da IA.
- A linguagem respeita crianças maiores do Fundamental I: sem “amiguinho”, “letrinha” ou fala
  excessivamente infantilizada.

## Resultado da varredura

| Área | Problema encontrado | Tratamento |
|---|---|---|
| Home | apresentação explicava sigla, produto e “modo escola” | LÉIA apresenta a si mesma e Alfa e faz um convite direto |
| Gibi | narração lia “opção 1/opção 2” e respostas pareciam relatório | opções permanecem visuais; a fala conta a cena e conversa sobre uma pista |
| Voz | “processando”, “preparando a voz” e fórmulas repetidas | estados viraram “LÉIA está pensando” e falas específicas do contexto |
| Puzzle | “orientação”, “possibilidade” e instruções longas | verbos concretos: olhar, tocar, arrastar, procurar e contar |
| Câmera | texto técnico e impessoal | convite “caçar o som de M”, com privacidade mantida em linguagem direta |
| Tentativas | personagem sem nome e frase burocrática de tempo | LÉIA e Alfa acolhem e convidam para a próxima parte |
| Personagens | `DOG` e descrições genéricas | novos pacotes usam `ALFA`; `DOG` é aceito apenas para cache legado e exibido como Alfa |

A varredura abrangeu `app/src/main/java`, `app/src/main/res` e os exemplos de contrato v2. Texto da
área do educador não foi infantilizado porque seu público é adulto. A palavra “cachorro” permanece
somente no reconhecimento visual genérico (`dog → cachorro`) e no rastro histórico de prompts; ela
não nomeia Alfa nem aparece como apresentação do personagem.

## Regras para conteúdo online

Uma resposta remota só pode chegar à criança depois da validação do gateway. Deve ter no máximo duas
frases, reconhecer a contribuição sem dar nota e propor somente um próximo passo. Rejeitar ou substituir
respostas que contenham linguagem de sistema, diagnóstico, culpa, ranking, “resposta correta”, promessa
de capacidade da IA ou convite para compartilhar dados pessoais. Histórias novas entram como rascunho,
passam por validação estrutural e revisão adulta e só então viram pacote imutável para cache offline.

## Novas cenas do Mistério da bola

Foram adicionados três quadros 4:3, sem texto incorporado, usando os ativos existentes como referências
de identidade. Eles aparecem na abertura, na busca da pista e na celebração após o puzzle. A geração foi
feita pela ferramenta integrada de imagem, na categoria `illustration-story`, com a restrição explícita
de não imitar franquias protegidas.

### Prompt — abertura

> Create a new opening panel for the existing story “Mistério da bola”, preserving the same four
> character identities from the references: LÉIA, the adult Brazilian educator in blue jacket,
> yellow shirt, short dark wavy hair and red hair clip; Alfa, the small cream-colored dog with tan
> ears and blue collar; Lia, the Black girl with two puff ponytails, yellow shirt and blue shorts;
> and Davi, the Black boy with short curly hair, green shirt and red shorts. LÉIA and Alfa arrive in
> the sunny school courtyard and warmly greet Lia and Davi before the mystery begins. Alfa is alert
> and curious, noticing small footprints leading toward a tree; the children look interested. 4:3
> landscape, bold dark outlines, expressive faces, warm hand-painted texture. Preserve identities
> and clothing. No text, speech balloons, logos, watermark or copyrighted-franchise imitation.

### Prompt — pista

> Create the middle investigation panel for “Mistério da bola” with the same four identities. Alfa
> has found a trail and points with his nose toward a red-and-white ball partly hidden behind the
> tree. Davi follows the clue; Lia looks curious and hopeful; LÉIA stays behind them with an open,
> encouraging gesture so the children lead the discovery. 4:3 landscape, readable at phone size,
> same school, palette and comic rendering. Preserve identities and clothing. No text, speech
> balloons, logos, watermark or copyrighted-franchise imitation.

### Prompt — celebração

> Create the completion panel for “Mistério da bola” with the same four identities. Lia holds the
> recovered ball, Davi celebrates, LÉIA offers an enthusiastic high-five and Alfa makes a joyful
> little jump. Keep the same tree and school courtyard. 4:3 landscape, lively but not crowded,
> original children’s comic illustration. Preserve identities and clothing. No text, speech
> balloons, logos, watermark or copyrighted-franchise imitation.

## Riscos ainda abertos

- Naturalidade de TTS depende da voz instalada; frases revisadas melhoram prosódia, mas não substituem
  teste em aparelhos reais e escuta com crianças.
- Pacotes antigos podem conter falas anteriores. O cache legado deve ser migrado ou republicado antes
  do piloto; aceitar `DOG` evita quebrar o APK, mas a interface já o apresenta como Alfa.
- Uma imagem gerada pode variar detalhes entre quadros. Os três novos painéis foram inspecionados, mas
  consistência de personagem continua sendo critério obrigatório na revisão do professor.
- A aprovação final de tom precisa de observação com crianças de diferentes anos; “natural” não deve
  virar infantilização para 4º e 5º ano.

## Evidência visual

As telas foram renderizadas e inspecionadas em
[360×640](../output/screenshots/alfa-language/360x640/),
[412×915](../output/screenshots/alfa-language/412x915/) e
[800×1280](../output/screenshots/alfa-language/800x1280/). A primeira renderização revelou molduras
com espaço branco excessivo em telas altas; o uso de peso flexível foi removido, as imagens voltaram
à proporção 4:3 e uma segunda rodada confirmou CTA visível, sem sobreposição e sem rolagem obrigatória.
