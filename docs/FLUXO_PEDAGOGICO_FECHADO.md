# Fluxo pedagógico fechado do InterpretaAI

## Decisão de produto

O InterpretaAI não junta todas as funcionalidades em uma atividade longa. Gibi, quebra-cabeça,
missão fonêmica e futuro quadro inteligente são módulos independentes. O que os une é um ciclo de
aprendizagem reconhecível: **ouvir um contexto → expressar uma hipótese → agir sobre o conceito →
relacionar imagem, palavra e som → compartilhar com outra pessoa**.

Para apresentação e primeiro uso rápido, um percurso está fechado de ponta a ponta: **Percurso
Bola**. Ele prova a proposta com pouco conteúdo e alta confiabilidade, sem afirmar que um puzzle,
sozinho, alfabetiza.

## Por que esse ciclo

- O guia de habilidades fundamentais do What Works Clearinghouse recomenda desenvolver linguagem
  narrativa e inferencial, consciência dos segmentos sonoros, relação som–letra, reconhecimento de
  palavras e leitura de texto conectado. O percurso usa oralidade e narrativa antes da manipulação e
  termina ligando `BOLA`, `BO-LA`, `B` e `/b/`. Fonte: [IES/WWC — Foundational Skills to Support
  Reading for Understanding](https://ies.ed.gov/ncee/wwc/PracticeGuide/21/Published).
- A RENABE reúne evidências brasileiras sobre vocabulário, compreensão e expressão oral,
  processamento fonológico e princípio alfabético. O aplicativo não troca essa progressão por uma
  recompensa visual; a interação serve ao objetivo linguístico. Fonte: [MEC — Relatório Nacional de
  Alfabetização Baseada em Evidências](https://www.gov.br/mec/pt-br/media/acesso_informacacao/pdf-arq/RENABE_web.pdf).
- As Diretrizes UDL recomendam múltiplos meios de engajamento, representação e ação/expressão. Por
  isso, a criança ouve e vê; responde por voz ou figura; e manipula por toque ou arraste. Fonte:
  [CAST — UDL Guidelines](https://udlguidelines.cast.org/).

Essas fontes sustentam as decisões de desenho, mas **não provam eficácia do InterpretaAI**. Efeito de
aprendizagem exige piloto com alfabetizadores, crianças e instrumentos definidos antes da coleta.

## Percurso Bola — contrato funcional

| Estado | Criança percebe/faz | Sistema garante | Objetivo |
|---|---|---|---|
| 1. Contexto | vê o quadrinho e ouve Lia e Davi | narração preparada, repetível e offline | linguagem narrativa e atenção conjunta |
| 2. Compreensão | responde “o que falta?” por voz ou toca ⚽ | reconhece `bola` localmente, sem esperar IA | recuperar o objeto central do contexto |
| 3. Conexão | toca “montar a bola” | abre diretamente o tabuleiro 2×2, sem outro menu | conservar significado entre etapas |
| 4. Manipulação | toca duas peças ou arrasta uma peça | mesma regra de troca; ajuda progressiva | agir sobre a representação visual |
| 5. Palavra e som | ouve `BOLA`, `BO-LA`, `B` e `/b/`; pode repetir | microfone não é barreira para continuar | aproximar oralidade, sílaba e princípio alfabético |
| 6. Colaboração | deixa o celular na mesa e conversa em dupla | encerramento explícito fora da tela | transformar uso consciente em prática visível |

## Tratamento dos desvios

| Situação | Resposta fechada |
|---|---|
| silêncio ou áudio vazio | oferece nova tentativa e alternativa visual, sem culpa |
| resposta diferente de “bola” | reconhece a contribuição e manda ouvir o contexto novamente |
| sem permissão de microfone | informa que um adulto deve autorizar; a figura continua disponível |
| reconhecimento de voz indisponível | mantém voz sintetizada, toque e progressão local |
| inatividade no puzzle | após 15 s, uma fala orienta; após 30 s, aparece indicação visual |
| toque durante a espera | reinicia o relógio; a fala de ajuda ocorre no máximo uma vez por rodada |
| app em segundo plano, falando ou ouvindo | relógio de ajuda fica suspenso |
| falha de rede/IA | o percurso principal continua; IA não decide tela, acerto ou conclusão |

## Lugar da IA

A máquina de estados Android controla objetivo, contexto e próximo passo. Um modelo pode reformular
uma mediação ambígua dentro de opções permitidas, mas não escolhe a atividade nem avalia a criança.
Respostas exatas e necessárias para a demonstração são locais, rápidas e testáveis. O servidor mantém
interfaces substituíveis para conversa e voz, timeout e fallback.

No LangChain4j, a evolução recomendada é usar **AI Services com saída estruturada por JSON Schema**,
validar enums e tamanho novamente no servidor e manter regras determinísticas depois do modelo. Os
guardrails da biblioteca ainda são experimentais e não devem ser a única camada de segurança. Fontes:
[saída estruturada](https://docs.langchain4j.dev/tutorials/structured-outputs/) e
[guardrails](https://docs.langchain4j.dev/tutorials/guardrails/).

O Gemini Developer API não deve ser ligado diretamente ao percurso infantil enquanto seus termos
proibirem clientes dirigidos ou provavelmente acessados por menores de 18 anos. Antes de trocar o
provedor, é necessário confirmar contrato apropriado, privacidade e tratamento de dados infantis.
Fonte: [Termos adicionais da Gemini API](https://ai.google.dev/gemini-api/terms).

## Próximo percurso: quadro inteligente

O quadro será desenhado como módulo próprio, compartilhando o mesmo ciclo, e não como continuação
obrigatória do puzzle. No modo guiado, casa, árvore, bola e maçã terão trilhas avaliadas localmente por
cobertura/aproximação. No modo livre, visão computacional devolverá candidatos e confiança; a LEIA
perguntará “parece uma árvore, foi isso?”, em vez de declarar como fato. Ele só entra na versão de uso
quando tiver contrato, fallback, teste de traço e teste de baixa confiança.

## Critério para chamar a versão de “pronta para uso rápido”

O percurso só é aprovado quando passa em 360×640 dp, 412×915 dp e 800×1280 dp; nenhum CTA depende
de rolagem; toque e arraste funcionam; fala e inatividade não se sobrepõem; toda falha tem saída por
toque; métricas registram participação, modalidade, tempo e ajuda, nunca nota ou diagnóstico. Uso com
crianças reais continua condicionado a validação pedagógica, consentimento, proteção de dados,
acessibilidade e infraestrutura estável.
