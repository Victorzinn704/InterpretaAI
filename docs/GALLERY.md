# Galeria visual do InterpretaAI

Esta galeria reúne capturas reais do APK. Elas são evidência da jornada infantil, da identidade em
gibi e da auditoria sem rolagem em três tamanhos de tela — não são mockups externos ao produto.

[← Voltar ao README](../README.md) · [Catálogo de experiências](EXPERIENCE_CATALOG.md) ·
[Estado auditado](MVP_STATUS.md) · [Critérios do hackathon](HACKATHON_CRITERIA.md)

## Visão executiva — a jornada no tablet

O perfil 800×1280 dp deixa explícito que o tablet não exibe jogos isolados. A criança entra por uma
história, investiga, manipula e termina explicando ou colaborando. As quatro telas abaixo representam
os principais tipos de interação do MVP.

<table>
  <tr>
    <td width="25%" align="center"><img src="../output/screenshots/rain-clarified/800x1280/chuva-1-abrigo-1200x1920.png" alt="História A Água da Chuva no tablet" width="100%"><br><strong>História narrada</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/rain-clarified/800x1280/chuva-3-dois-caminhos-1200x1920.png" alt="Comparação de evidências no tablet" width="100%"><br><strong>Interpretação</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/galaxy-tab-a8/percurso-bola-6-manipular-1200x1920.png" alt="Quebra-cabeça contextual no tablet" width="100%"><br><strong>Manipulação</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/galaxy-tab-a8/shared-tablet-collaborative-turn-1200x1920.png" alt="Rodízio colaborativo no tablet" width="100%"><br><strong>Colaboração</strong></td>
  </tr>
</table>

## História variável — mesmo renderer no celular e no tablet

Estas capturas vêm do teste instrumentado do novo `LearningStoryPack`, em 360×640, 412×915 e
800×1280dp. A maçã é um recurso local usado como **fixture de interface**: demonstra a execução
por dados e o enquadramento, não comprova upload/publicação real de professora nem uso por crianças.
O quadrinho definitivo será a imagem aprovada pela docente e recebida pelo cache privado.

<table>
  <tr>
    <td width="20%" align="center"><img src="../output/screenshots/story-pack/storypack-1-gibi-360x640.png" alt="Quadrinho narrado em celular compacto" width="100%"><br><strong>Ouvir e observar</strong></td>
    <td width="20%" align="center"><img src="../output/screenshots/story-pack/storypack-2-puzzle-360x640.png" alt="Quebra-cabeça da maçã por toque ou arraste" width="100%"><br><strong>Montar a pista</strong></td>
    <td width="20%" align="center"><img src="../output/screenshots/story-pack/storypack-3-palavra-360x640.png" alt="Letras visíveis para formar maçã" width="100%"><br><strong>Formar a palavra</strong></td>
    <td width="20%" align="center"><img src="../output/screenshots/story-pack/storypack-4-dupla-360x640.png" alt="Aparelho descansa durante a conversa da dupla" width="100%"><br><strong>Conversar fora da tela</strong></td>
    <td width="20%" align="center"><img src="../output/screenshots/story-pack/storypack-5-fim-360x640.png" alt="Conclusão da história com LÉIA" width="100%"><br><strong>Encerrar</strong></td>
  </tr>
</table>

Veja o [mesmo percurso em 412×915 e 800×1280dp](../output/screenshots/story-pack/).

## História independente — A água da chuva

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/rain-clarified/800x1280/chuva-1-abrigo-1200x1920.png" alt="Ler no tablet: Lia, Davi e Alfa localizam as folhas amarelas na chuva" width="100%"><br><strong>Ler: achar as folhas</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/rain-clarified/800x1280/chuva-3-dois-caminhos-1200x1920.png" alt="Interpretar no tablet: comparar os caminhos usando água e folhas amarelas como evidências" width="100%"><br><strong>Interpretar: comparar</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/rain-clarified/800x1280/chuva-4-jardim-1200x1920.png" alt="Aprender no tablet: explicar como a água passou sob a ponte e chegou às raízes" width="100%"><br><strong>Aprender: explicar</strong></td>
  </tr>
</table>

LÉIA guia por voz, mas não ocupa os quadros de ação. Lia, Davi e Alfa vivem um episódio independente:
chuva, folhas viajantes, dois caminhos e água chegando ao jardim. Não há bola, objeto perdido ou
puzzle neste enredo. Veja a concepção e os prompts em
[Gibi situacional da chuva](SITUATIONAL_RAIN_COMIC.md). As telas foram verificadas em
[360×640](../output/screenshots/rain-clarified/360x640/),
[412×915](../output/screenshots/rain-clarified/412x915/) e
[800×1280](../output/screenshots/rain-clarified/800x1280/).
Os elementos centrais e os critérios de recorte estão na
[Curadoria visual dos gibis](COMIC_IMAGE_CURATION.md).

## Pistas tocáveis dentro do gibi

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/interactive-comics/412x915/percurso-bola-1b-pista-reativa-824x1830.png" alt="Destaque localizado sobre a marca circular no chão" width="100%"><br><strong>Descobrir o que falta</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/interactive-comics/412x915/percurso-bola-4a-pista-reativa-824x1830.png" alt="Destaque localizado sobre a última marca molhada junto à árvore" width="100%"><br><strong>Seguir as marcas</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/rain-clarified/412x915/chuva-1b-folhas-reativas-824x1830.png" alt="Destaque localizado sobre as folhas amarelas na água" width="100%"><br><strong>Achar as folhas</strong></td>
  </tr>
</table>

O brilho aparece onde a criança tocou, toca um efeito curto e chama uma fala contextual da LÉIA.
Ele não fica piscando continuamente e a indicação automática é desativada com “Reduzir estímulos”.
As evidências cobrem [360×640](../output/screenshots/interactive-comics/360x640/),
[412×915](../output/screenshots/interactive-comics/412x915/) e
[800×1280](../output/screenshots/interactive-comics/800x1280/).

## LÉIA, Alfa e três missões novas

Capturas reais do APK emulado, **não** fotografia de crianças ou de tablet físico. A bola na
atividade de letras é uma fotografia sintética gerada por IA; a personagem também é original e
gerada para o projeto. [Escopo, consumo e proveniência](NEW_GAMES_TABLET_MOBILE.md).

<table>
  <tr>
    <td width="25%" align="center"><img src="../output/screenshots/new-games/800x1280/leia-cachorro-home-1200x1920.png" alt="LÉIA e Alfa na Home do tablet" width="100%"><br><strong>LÉIA e Alfa</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/new-games/800x1280/jogo-caminho-numeros-1200x1920.png" alt="Caminho lógico dos números no tablet" width="100%"><br><strong>Caminho 1→5</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/new-games/800x1280/jogo-ligue-pontos-casa-1200x1920.png" alt="Pontos ligados formam uma casa no tablet" width="100%"><br><strong>CASA revelada</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/new-games/800x1280/jogo-imagem-letras-1200x1920.png" alt="Foto ilustrativa de bola para completar letras no tablet" width="100%"><br><strong>Foto e letras</strong></td>
  </tr>
</table>

O mesmo fluxo foi capturado em [celular compacto 360×640](../output/screenshots/new-games/360x640/),
[celular 412×915](../output/screenshots/new-games/412x915/) e
[tablet emulado 800×1280](../output/screenshots/new-games/800x1280/). A professora seleciona uma
das três missões no painel adulto e pode publicá-la no canal de piloto já existente.

## Mistério da Bola — estados conectados para compreensão funcional

Estas são capturas reais do percurso fechado. O mesmo conceito segue da informação explícita à
inferência e à aplicação; o quebra-cabeça consolida a palavra dentro da história.

As capturas atuais mostram ajuda progressiva: a pergunta sobre o objeto convida primeiro à fala,
oferece uma figura sem exigir microfone e só mostra “BOLA” após o pedido. A pista seguinte é
observada antes de revelar alternativas; a aplicação também pede uma formulação antes da ajuda.

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/image-curation/412x915/percurso-bola-1-ouvir-824x1830.png" alt="Ler o contexto sem mostrar a bola desaparecida" width="100%"><br><strong>1. Ler sem revelar</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/412x915/percurso-bola-2-responder-824x1830.png" alt="Localizar informação sem resposta exposta" width="100%"><br><strong>2. Localizar</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/image-curation/412x915/percurso-bola-4-investigar-824x1830.png" alt="Interpretar marcas molhadas sem mostrar a bola" width="100%"><br><strong>3. Seguir as marcas</strong></td>
  </tr>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/412x915/percurso-bola-5-explicar-824x1830.png" alt="Explicar a pista" width="100%"><br><strong>4. Explicar</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/412x915/percurso-bola-6-manipular-824x1830.png" alt="Manipular o puzzle" width="100%"><br><strong>5. Manipular</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/412x915/percurso-bola-7-palavra-som-824x1830.png" alt="Relacionar palavra, sílaba, letra e som" width="100%"><br><strong>6. Palavra e som</strong></td>
  </tr>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/412x915/percurso-bola-8-aplicar-824x1830.png" alt="Usar a compreensão em uma orientação" width="100%"><br><strong>7. Aplicar</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/412x915/percurso-bola-9-colaborar-824x1830.png" alt="Compartilhar a pista com um colega" width="100%"><br><strong>8. Compartilhar</strong></td>
    <td width="33%" align="center"><strong>Um conceito contínuo</strong><br>O puzzle não interrompe a história.</td>
  </tr>
</table>

O percurso completo foi recapturado em [412×915](../output/screenshots/progressive-help/412x915/)
e [800×1280](../output/screenshots/galaxy-tab-a8/); os três passos iniciais também foram
inspecionados em [360×640](../output/screenshots/progressive-help/360x640/).

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/360x640/percurso-bola-2-responder-720x1280.png" alt="Primeiro convite oral sem mostrar BOLA" width="100%"><br><strong>Primeiro: conte sua ideia</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/progressive-help/360x640/percurso-bola-2b-apoio-figura-720x1280.png" alt="Figura disponível após pedido de ajuda" width="100%"><br><strong>Depois: figura opcional</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/galaxy-tab-a8/percurso-bola-2b-apoio-figura-1200x1920.png" alt="A mesma ajuda visível no perfil de tablet" width="100%"><br><strong>Também no tablet</strong></td>
  </tr>
</table>

## Entrada e escolha da experiência

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/home-360x640.png" alt="Home compacta" width="100%"><br><strong>Home — 360×640</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/home-412x915.png" alt="Home em celular alto" width="100%"><br><strong>Home — 412×915</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/home-800x1280.png" alt="Home em tablet" width="100%"><br><strong>Home — tablet</strong></td>
  </tr>
</table>

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-menu-360x640.png" alt="Histórias em celular compacto" width="100%"><br><strong>Histórias — compacto</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-menu-412x915.png" alt="Histórias em celular alto" width="100%"><br><strong>Histórias — celular</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-menu-800x1280.png" alt="Histórias em tablet" width="100%"><br><strong>Histórias — tablet</strong></td>
  </tr>
</table>

## Gibi falado e coautoria guiada

### Estado 1 — observar e ouvir

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-observar-360x640.png" alt="Cena para observar em 360 por 640" width="100%"><br><strong>360×640</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-observar-412x915.png" alt="Cena para observar em 412 por 915" width="100%"><br><strong>412×915</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-observar-800x1280.png" alt="Cena para observar em tablet" width="100%"><br><strong>800×1280</strong></td>
  </tr>
</table>

### Estado 2 — contar uma ideia

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-conversa-360x640.png" alt="Conversa com a LÉIA em 360 por 640" width="100%"><br><strong>Voz ou toque — compacto</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-conversa-412x915.png" alt="Conversa com a LÉIA em 412 por 915" width="100%"><br><strong>Voz ou toque — celular</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/gibi-conversa-800x1280.png" alt="Conversa com a LÉIA em tablet" width="100%"><br><strong>Voz ou toque — tablet</strong></td>
  </tr>
</table>

### Estado 3 — receber a reação da LÉIA

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/visual-reactions/360x640/gibi-reacao-leia-360x640.png" alt="LÉIA reage à ideia no celular compacto" width="100%"><br><strong>Personagem e balão — 360×640</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/visual-reactions/412x915/gibi-reacao-leia-412x915.png" alt="LÉIA reage à ideia no celular alto" width="100%"><br><strong>Reação contextual — 412×915</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/visual-reactions/800x1280/gibi-reacao-leia-800x1280.png" alt="LÉIA reage à ideia no tablet" width="100%"><br><strong>Mesmo foco — tablet</strong></td>
  </tr>
</table>

A resposta reconhece a contribuição, mantém a LÉIA dentro da história e orienta o próximo passo. A
[auditoria visual infantil](CHILD_VISUAL_UX_AUDIT.md) registra critérios, riscos e modo reduzido.

## Quebra-cabeça: imagem, palavra e fala

### Escolha com poucas possibilidades

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-menu-360x640.png" alt="Menu de puzzle compacto" width="100%"><br><strong>360×640</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-menu-412x915.png" alt="Menu de puzzle em celular" width="100%"><br><strong>412×915</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-menu-800x1280.png" alt="Menu de puzzle em tablet" width="100%"><br><strong>800×1280</strong></td>
  </tr>
</table>

## Quadro criativo guiado pelo professor — versões 0.7 e 0.17

O professor escolhe uma pista visual; a criança marca por toque ou desenha por arraste, pode mudar o
traço, apagar por gesto sem remover a pista visual, desfazer e refazer. Na versão 0.17, o traço foi
suavizado, o buffer do gesto deixou de copiar toda a linha a cada movimento e os controles passaram
a refletir o histórico imediatamente. A tela não depende de rolagem e termina devolvendo a criação
para a conversa em turma.

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/0.7.0/drawing-used-360x640dp.png" alt="Quadro usado em 360 por 640 dp" width="100%"><br><strong>360×640dp • interação real</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/0.7.0/drawing-412x915dp.png" alt="Quadro em 412 por 915 dp" width="100%"><br><strong>412×915dp</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/galaxy-tab-a8/drawing-current-finger-1200x1920.png" alt="Quadro 0.17 com ponto e traço feitos por toque no emulador 800 por 1280 dp" width="100%"><br><strong>Tablet 800×1280dp • versão 0.17</strong></td>
  </tr>
</table>

As duas capturas de celular acima são da versão 0.7; a captura de tablet foi refeita com o quadro
0.17. O emulador foi configurado em 1200×1920 px e densidade 240 (800×1280 dp), proporção da tela
do Galaxy Tab A8 identificado em compra da SME. Não é fotografia nem validação em aparelho de GET.

### Jornada infantil no perfil de tablet

<table>
  <tr>
    <td width="25%" align="center"><img src="../output/screenshots/galaxy-tab-a8/shared-tablet-home-1200x1920.png" alt="Home em tablet compartilhado" width="100%"><br><strong>Missão do grupo</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/galaxy-tab-a8/percurso-bola-1-ouvir-1200x1920.png" alt="Gibi narrado em tablet" width="100%"><br><strong>Ouvir e observar</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/galaxy-tab-a8/percurso-bola-6-manipular-1200x1920.png" alt="Quebra-cabeça em tablet" width="100%"><br><strong>Montar por toque ou arraste</strong></td>
    <td width="25%" align="center"><img src="../output/screenshots/galaxy-tab-a8/drawing-current-finger-1200x1920.png" alt="Quadro com traço por dedo em tablet" width="100%"><br><strong>Desenhar e explicar</strong></td>
  </tr>
</table>

As [capturas completas deste perfil](../output/screenshots/galaxy-tab-a8/) cobrem 18 estados do
percurso. A inspeção visual não encontrou CTA cortado, sobreposição ou ação dependente de swipe.
Etapas curtas deixam área livre considerável: isso preserva foco, mas ainda requer observação em
sala para verificar se mantém a atenção e facilita o rodízio entre crianças.

### Professor publica; a criança recebe uma missão

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/0.7.0/educator-assignment-412x915dp.png" alt="Professor escolhe avatar e atividade" width="100%"><br><strong>1. Preparar</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/0.7.0/home-assigned-412x915dp.png" alt="Home mostra avatar e missão atribuída" width="100%"><br><strong>2. Receber</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/0.7.0/assigned-drawing-opened-412x915dp.png" alt="Missão atribuída abre o quadro" width="100%"><br><strong>3. Fazer</strong></td>
  </tr>
</table>

O fluxo local permanece disponível. O canal online de piloto também foi validado em loopback: a
atribuição versionada saiu pelo servidor e alterou a Home para outro avatar, turma e atividade. A
criança vê somente o avatar, nunca pseudônimo, nome ou matrícula; identidade institucional permanece
fora do MVP.

<table>
  <tr>
    <td width="50%" align="center"><img src="../output/screenshots/0.8.0/educator-online-sync-412x915dp.png" alt="Configuração e envio online na área adulta" width="75%"><br><strong>Canal online do piloto</strong></td>
    <td width="50%" align="center"><img src="../output/screenshots/0.8.0/home-remote-assignment-412x915dp.png" alt="Home atualizada com Estrela, Turma 3B e quebra-cabeça" width="75%"><br><strong>Missão remota recebida</strong></td>
  </tr>
</table>

Na versão 0.9, a mesma área adulta permite reunir aliases e tablets em uma sala, marcar participantes
e enviar a missão para todos ou um grupo. O editor continua separado da experiência infantil e deixa
explícito que o token do piloto não substitui autenticação institucional.

Antes do envio, a atividade selecionada também expõe foco pedagógico, faixa de mediação, evidência
observável e referências de planejamento. Isso torna a escolha docente explícita sem transformar a
faixa em classificação automática da criança.

<table>
  <tr>
    <td width="50%" align="center"><img src="../output/screenshots/educator-workflow-mission-1080x2400.png" alt="Área adulta na aba Missão" width="75%"><br><strong>1. Escolher a missão sem atravessar configurações técnicas</strong></td>
    <td width="50%" align="center"><img src="../output/screenshots/educator-workflow-classroom-1080x2400.png" alt="Área adulta na aba Turma" width="75%"><br><strong>2. Confirmar a missão, formar a turma e enviar</strong></td>
  </tr>
</table>

<p align="center">
  <img src="../output/screenshots/educator-year-filter-1080x2400.png" alt="Professor filtra as missões do quinto ano sem perder a opção de recomposição" width="420"><br>
  <strong>O ano reduz a lista na mesma tela; TODAS preserva missões de recomposição e não classifica a criança.</strong>
</p>

<p align="center">
  <img src="../output/screenshots/interpreta-tablet-diagnostic-1080x2400.png" alt="Diagnóstico técnico do tablet dentro da área do professor" width="420"><br>
  <strong>O piloto identifica ressalvas de hardware, voz e Modo Foco sem coletar serial, IMEI ou dado infantil.</strong>
</p>

<p align="center">
  <img src="../output/screenshots/reading-pack-5-closure-1080x2400.png" alt="Encerramento da missão de comparação de fontes do quinto ano" width="420"><br>
  <strong>A missão enviada pelo professor termina com a habilidade praticada e devolve a turma para uma atividade fora da tela.</strong>
</p>

<p align="center">
  <img src="../output/screenshots/shared-tablet-home-1080x2400.png" alt="Home de um tablet compartilhado por dois avatares" width="420"><br>
  <strong>O grupo vê seus avatares e a missão; pseudônimos e identidade permanecem fora da interface infantil.</strong>
</p>

<p align="center">
  <img src="../output/screenshots/shared-tablet-collaborative-turn-1080x2400.png" alt="Gibi com rodízio visual entre os avatares Pipa e Sol" width="420"><br>
  <strong>O rodízio é visual e falado: um avatar procura pistas enquanto o outro ouve e ajuda; os papéis mudam nas etapas seguintes.</strong>
</p>

<p align="center">
  <img src="../output/screenshots/0.9.0/educator-pedagogical-focus-1080x2400.png" alt="Ficha pedagógica da missão selecionada na área do professor" width="420"><br>
  <strong>O professor vê o propósito da missão antes de publicá-la.</strong>
</p>

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/0.9.0/educator-room-roster-1080x2400.png" alt="Professor adiciona a seleção de avatar alias e tablet à sala" width="100%"><br><strong>Montar a sala</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/0.9.0/educator-room-selection-empty-1080x2400.png" alt="Envio fica bloqueado enquanto nenhum participante está selecionado" width="100%"><br><strong>Seleção obrigatória</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/0.9.0/educator-room-actions-1080x2400.png" alt="Professor salva a sala e envia a atividade para todos" width="100%"><br><strong>Enviar para todos</strong></td>
  </tr>
</table>

### Montagem por toque ou arraste

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-jogo-360x640.png" alt="Puzzle em andamento no celular compacto" width="100%"><br><strong>Jogo — compacto</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-jogo-412x915.png" alt="Puzzle em andamento no celular" width="100%"><br><strong>Jogo — celular</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-jogo-800x1280.png" alt="Puzzle em andamento no tablet" width="100%"><br><strong>Jogo — tablet</strong></td>
  </tr>
</table>

### Conclusão falada

<table>
  <tr>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-conclusao-360x640.png" alt="Puzzle concluído em celular compacto" width="100%"><br><strong>Palavra completa — compacto</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-conclusao-412x915.png" alt="Puzzle concluído em celular" width="100%"><br><strong>Palavra completa — celular</strong></td>
    <td width="33%" align="center"><img src="../output/screenshots/puzzle-conclusao-800x1280.png" alt="Puzzle concluído em tablet" width="100%"><br><strong>Palavra completa — tablet</strong></td>
  </tr>
</table>

## Evidência da correção de enquadramento

A versão anterior dependia de rolagem para expor a ação principal em telas menores. A versão atual
prioriza o CTA, reduz ilustração e espaçamento antes de reduzir texto e mantém uma decisão por viewport.

<table>
  <tr>
    <td width="50%" align="center"><img src="../output/screenshots/before/home-360x640.png" alt="Home anterior com conteúdo fora do viewport" width="70%"><br><strong>Antes — CTA disputava espaço e leitura</strong></td>
    <td width="50%" align="center"><img src="../output/screenshots/home-360x640.png" alt="Home atual com CTA principal visível" width="70%"><br><strong>Depois — próximo passo sempre visível</strong></td>
  </tr>
</table>

## O que estas imagens comprovam

- identidade visual consistente em celular compacto, celular alto e tablet;
- história, pergunta, reação e avanço separados em estados controlados;
- CTA infantil sem depender de rolagem;
- alvos grandes e alternativas por voz ou toque;
- puzzle por toque ou arraste no mesmo viewport, incluindo conclusão e convite falado;
- rodízio de ações por avatar no tablet compartilhado, sem alias ou autoria individual inventada;
- preservação da linguagem de gibi: bordas grossas, sombras, balões e cores reconhecíveis.

[← Voltar à apresentação principal](../README.md)
