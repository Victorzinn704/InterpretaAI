# Estado real do MVP

## Implementado no APK

- ciclo LEIA com cinco cenas ilustradas e narradas;
- escolhas sem classificar emoções como certas ou erradas;
- resumo da história construído pelas decisões da turma;
- montagem da palavra BOLA;
- quebra-cabeças de bola, banana e maçã em 2 × 2 e 3 × 2;
- troca de peças por dois toques, pista visual e fala da palavra ao concluir;
- fluxo infantil guiado por ações que pulsam, mudam suavemente de cor e indicam onde tocar;
- botão flutuante “VER MAIS” que avança o conteúdo sem exigir que a criança descubra a rolagem;
- missão da letra M com resposta por voz, interpretação, câmera e conversa presencial;
- métricas locais para professor, incluindo gibi e quebra-cabeças;
- modo imersivo e suporte a Device Owner/Lock Task;
- voz local configurada para priorizar uma opção brasileira disponível e fala mais suave;
- APK compilável sem internet e sem armazenar áudio bruto.

## Precisa ser validado antes da apresentação

Os testes instrumentados do gibi e do quebra-cabeça foram executados com sucesso no emulador API 35.

1. Repetir o percurso em tablet físico e conferir toque, rolagem e tamanho das peças.
2. Testar voz em português e reconhecimento em uma sala com ruído.
3. Provisionar um tablet limpo como Device Owner e testar reinício, Home, Recentes e notificações.
4. Validar os textos e as intervenções pedagógicas com alfabetizador e educação especial.

## Não implementado

- avaliação de fotografia por Gemini, NVIDIA ou outro modelo;
- conversa generativa com a criança;
- geração dinâmica de histórias ou quadrinhos;
- sincronização com servidor e painel agregado da secretaria;
- autenticação real de professor/gestor;
- atividades do Modo Casa e visão dos responsáveis;
- quebra-cabeça 3 × 3.

Esses itens não devem ser apresentados como funcionalidades atuais. Para o piloto, a próxima decisão é
validar o fluxo local em tablet antes de acrescentar IA ou infraestrutura de servidor.
