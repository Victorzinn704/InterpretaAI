# Auditoria visual infantil — imagens, reação e atenção

Data: 18/09/2026
Escopo: Home infantil, gibi, resposta da LÉIA, quebra-cabeça e encerramento.
Público: crianças do Ensino Fundamental I, inclusive crianças que ainda não leem com autonomia.

## Veredito

O padrão de gibi é o ativo visual mais forte do MVP: personagens consistentes, situações reconhecíveis,
ação compreensível pela imagem e boa ligação entre cena, fala e pergunta. O principal problema não era
falta de cor, mas falta de **presença da LÉIA no retorno**. Antes, a criança agia e recebia um cartão de
texto com uma estrela; agora a resposta vira uma pequena cena: LÉIA reage, Alfa participa, o texto
fica em balão e sinais gráficos reforçam descoberta ou celebração.

## Critérios usados

| Critério | Regra de aprovação |
|---|---|
| Função pedagógica | cada imagem ajuda a observar, compreender, agir ou consolidar linguagem |
| Hierarquia | uma ação principal por viewport; reação não disputa com o CTA |
| Comunicação pré-leitora | personagem, gesto, som e contraste comunicam antes do texto |
| Carga visual | no máximo oito marcas decorativas, entrada única e nenhuma animação infinita de celebração |
| Acessibilidade | modo reduzido remove partículas; LÉIA, voz, mensagem e contraste permanecem |
| Responsividade | CTA visível e conteúdo sem rolagem obrigatória em 360×640, 412×915 e 800×1280 dp |
| Consumo | efeitos são desenhados no Compose; nenhum vídeo, GIF ou motor de partículas foi adicionado |

## Inventário observado

- Cinco cenas horizontais do gibi em 1448×1086 mantêm Lia, Davi, escola e paleta reconhecíveis.
- Três imagens quadradas de puzzle têm objeto central grande e recorte adequado para 2×2 e 3×2.
- A fotografia sintética da bola serve ao jogo imagem→palavra, sem retratar criança ou escola real.
- A pose de entrada de LÉIA e Alfa é acolhedora, mas não comunicava celebração.
- A nova pose `leia_and_alfa_celebrate_v2.webp` diferencia conquista de recepção sem alterar os personagens.
- A pose `leia_and_alfa_support_v2.webp` comunica acolhimento: LÉIA se aproxima e Alfa demonstra
  tristeza leve, sem choro, vergonha ou aparência de punição.

## Achados e tratamento

| Prioridade | Achado | Tratamento nesta entrega | Estado |
|---|---|---|---|
| Alta | LÉIA desaparecia na reação do gibi | cena responsiva com personagem, balão e marcas de descoberta | corrigido |
| Alta | conclusão do puzzle parecia ficha estática | faixa compacta com LÉIA, palavra, sílabas e fonema | corrigido |
| Alta | encerramento usava apenas medalhas/emoji | pose própria de celebração e mensagem no mesmo quadro | corrigido |
| Média | Home tinha retrato isolado | moldura com letra, pergunta e som, sem criar nova ação | corrigido |
| Média | tablet ampliava o vazio entre conteúdo e CTA | quadro tonal com contorno agrupa a reação como painel de gibi | corrigido |
| Média | decomposição automática poderia errar `banana` | pistas fechadas por palavra: `BO-LA`, `BA-NA-NA`, `MA-ÇÃ` | corrigido |
| Média | única pose ainda atende escuta e pensamento | criar pose de escuta somente após teste com crianças | evolução |
| Alta | autoria/licença dos ativos anteriores não está assinada | manter inventário e confirmação humana antes de distribuição | pendente externo |

## Sistema de reação aprovado

1. **Incentivo:** azul e amarelo, pose de aceno, sem explosão de sucesso.
2. **Descoberta:** LÉIA aparece junto da ideia da criança; estrelas e linhas entram uma vez.
3. **Celebração:** pose com braço e patas erguidos, verde e amarelo, som curto já existente.
4. **Estímulos reduzidos:** nenhuma partícula é composta; personagem, balão e próximo passo permanecem.
5. **Avanço com apoio:** pose calma, uma explicação curta e um único próximo passo; nunca usa a
   celebração visual para esconder que a etapa não foi concluída de forma autônoma.

Esse sistema não declara “certo/errado” para interpretação aberta. Celebração forte é reservada a uma
ação concluída ou evidência localizada; tentativas diferentes continuam recebendo curiosidade e ajuda.

## Evidência renderizada

As capturas reais do teste instrumentado estão em [`output/screenshots/visual-reactions`](../output/screenshots/visual-reactions/):

- [`gibi-reacao-leia-360x640.png`](../output/screenshots/visual-reactions/360x640/gibi-reacao-leia-360x640.png);
- [`percurso-bola-7-palavra-som-412x915.png`](../output/screenshots/visual-reactions/412x915/percurso-bola-7-palavra-som-412x915.png);
- [`reading-pack-5-closure-800x1280.png`](../output/screenshots/visual-reactions/800x1280/reading-pack-5-closure-800x1280.png).
- [Avanço com apoio em 360×640, 412×915 e 800×1280](../output/screenshots/assisted-advance/).

O teste específico também comprova que o modo de estímulos reduzidos preserva a mensagem e remove o
canvas decorativo. A aprovação visual final deve sempre ser refeita após trocar qualquer ilustração.

## Limites e próximos testes

- Validar com crianças se a pose de celebração é percebida como conquista, sem parecer infantil demais.
- Observar se as marcas gráficas ajudam a localizar a fala ou apenas atraem o olhar para fora do balão.
- Medir abandono e pedido de ajuda; não usar tempo de tela como sinônimo de aprendizagem.
- Confirmar por escrito a proveniência dos 17 ativos listados em `ASSET_PROVENANCE.md`.
- Só criar poses adicionais quando um estado comunicativo real não puder ser entendido com as atuais.
