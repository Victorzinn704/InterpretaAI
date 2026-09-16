# Três missões pequenas — tablet e celular

O professor escolhe **uma** missão na área adulta e pode usá-la neste aparelho ou publicá-la para
um tablet/sala pelo contrato de piloto existente. Não há catálogo aberto, anúncios ou rolagem na
tela infantil. O recorte é deliberadamente fechado:

| Missão | Ação da criança | Ligação com a linguagem |
|---|---|---|
| Caminho dos números | Toca 1→5 numa grade com passagens bloqueadas | Conta ao colega qual veio antes; matemática é apoio, não prova de alfabetização |
| Ligue os pontos | Toca 1→5; os segmentos desenham uma casa | Nomeia a casa e conversa sobre o som inicial de **CASA** |
| Imagem e letras | Observa uma **foto ilustrativa gerada por IA** de bola e organiza B-O-L-A | Relaciona objeto, palavra visível ao final e som inicial /b/ |

A LEIA narra o convite, confirma cada passo, oferece ajuda solicitada e orienta a conversa fora da
tela ao terminar. Uma tentativa fora da sequência recebe um convite curto, sem buzina, nota ou
rótulo de erro. O evento salvo é início, ajuda ou conclusão; não armazena a sequência de toques nem
classifica a criança. A professora continua responsável por escolher a missão conforme a turma.

## Consumo e acessibilidade

- Os três jogos funcionam com conteúdo embarcado e **não chamam o backend para resolver a etapa**.
  A fala usa o mecanismo de voz do dispositivo; a disponibilidade realmente offline depende do
  mecanismo instalado e ainda precisa ser verificada em cada aparelho.
- São dois recursos novos: LEIA com cachorro em PNG transparente de cerca de **544 KiB**, e a foto
  ilustrativa da bola em JPEG de cerca de **232 KiB**. Os tabuleiros são desenhados por Compose,
  sem vídeo, física, câmera, OCR ou modelo de IA durante a atividade.
- Alvos numéricos têm no mínimo 58 dp; letras têm no mínimo 64 dp de altura. Não há arraste
  obrigatório nem swipe. A alternativa “OUVIR” repete a instrução; “AJUDA” anuncia o próximo
  número/letra. Contraste e dica textual permanecem sem depender só de cor ou som.
- Números, pontos, letras usadas e paredes têm descrição/estado acessível para leitor de tela.
  Após 20 s sem interação, a orientação muda de cor; após 40 s, há um único convite falado por
  etapa. Toque, mudança de etapa, fala em andamento e segundo plano reiniciam ou suspendem o
  temporizador. “Reduzir estímulos” suprime o destaque visual, preservando a orientação falada.
- O mesmo código adapta largura e altura. Em tablet ≥600 dp, personagem e tabuleiros crescem;
  no celular compacto, todos os controles permanecem no viewport. Não se trata de fotografias de
  aparelho físico: a [galeria](GALLERY.md) contém **capturas reais do APK emulado** em 360×640,
  412×915 e 800×1280 dp.
- O teste instrumentado percorre a escolha e publicação local das três missões no painel docente;
  o teste de cliente percorre os três IDs no envio e recebimento com servidor simulado. O envio
  por uma implantação pública real e a recepção em tablets físicos ainda não foram validados.

## Limites pedagógicos e próximos testes

As atividades são apoio breve à mediação. O caminho 1→5 e a casa de cinco pontos podem ficar fáceis
demais para alguns alunos; **não** representam progressão curricular nem medem aprendizagem. A
palavra do jogo de imagem é somente BOLA neste MVP. Um piloto com professoras e crianças deve
observar compreensão das instruções faladas, tamanho dos alvos em aparelhos físicos, uso sem
microfone, interesse após repetição, variação de vocabulário e transferência para fala/escrita fora
da tela. A faixa por ano na área docente organiza planejamento, sem diagnóstico automático.

Referências de intenção curricular, a validar com a rede: [BNCC oficial](https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf)
— EF01MA01 e EF01MA10 para ordem/sequência; EF15LP09 para explicação oral; EF01LP07 e EF01LP08
para fonema/grafema; EF02LP04 para escrita de sílabas simples. As missões não reivindicam cobrir
integralmente nenhuma dessas habilidades.

## Proveniência dos dois recursos visuais

Ambos foram gerados pela ferramenta integrada de imagem em 16/09/2026, sem imagem de criança,
fotografia de escola real ou referência visual de personagem protegido. A equipe deve revisar os
termos aplicáveis e registrar aprovação humana antes de uso externo definitivo.

- **LEIA e cachorro**, `leia_and_dog_v1.png`, uso `illustration-story`: “Design LEIA, an original
  friendly adult Brazilian woman literacy guide, accompanied by her small friendly dog. Warm,
  clever, expressive, welcoming to children ages 6–10 without looking babyish. Contemporary
  Brazilian children's comic visual language: thick dark outlines, simple expressive face and
  pose, limited yellow/red/green/blue palette matching a bold comic UI. Full upper body woman
  with dog beside her, both clearly visible, facing viewer, readable silhouette at small size.
  Distinctive original design: short wavy dark hair, teal-blue jacket over yellow shirt, red
  circular hair clip; dog with cream fur and blue collar. No resemblance to any existing
  copyrighted comic character, no trademark, no specific franchise outfit or face. Genuinely
  transparent background, no text, no watermark.” Reduzido mecanicamente para 700 px.
- **Bola fotográfica sintética**, `ball_photo_v1.jpg`, uso `photorealistic-natural`: “A single
  ordinary soccer ball, unmistakably a ball, photographed on a simple school playground ground in
  daylight. Centered ball occupying most of the square frame, realistic worn texture, natural
  soft light, high contrast and uncluttered background, no people, no logos, no text, no
  watermark. The ball should be visually distinct at small mobile size and suitable for a child
  to name aloud in Brazilian Portuguese.” Convertido mecanicamente para JPEG de 800 px.
