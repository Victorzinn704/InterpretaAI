# Fluxo pedagógico fechado do InterpretaAI

## Decisão de produto

O InterpretaAI não junta todas as funcionalidades em uma atividade longa. Gibi, quebra-cabeça,
missão fonêmica e futuro quadro inteligente são módulos independentes. O que os une é um ciclo de
aprendizagem reconhecível: **ouvir um contexto → expressar uma hipótese → agir sobre o conceito →
usar pistas → relacionar imagem, palavra e som → aplicar a compreensão → compartilhar com outra pessoa**.

Para apresentação e primeiro uso rápido, um percurso está fechado de ponta a ponta: **Percurso
Bola**. Ele prova a proposta com pouco conteúdo e alta confiabilidade, sem afirmar que um puzzle,
sozinho, alfabetiza.

## Público e princípio de progressão

O recorte é o Ensino Fundamental I, aproximadamente dos 6 aos 10 anos. A mediação não é definida
apenas pela idade: uma criança pode estar iniciando a relação som–letra, lendo frases com apoio ou já
decodificando sem compreender o contexto. A mesma missão oferece voz e imagem como suporte, mas exige
progressivamente que a criança localize informação, relacione pistas e use o que entendeu. O MVP não
diagnostica nível nem promete reduzir analfabetismo funcional; ele demonstra uma experiência desenhada
para avançar além da nomeação de figuras.

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

## Mistério da Bola — contrato funcional

| Estado | Criança percebe/faz | Sistema garante | Objetivo |
|---|---|---|---|
| 1. Contexto | vê o quadrinho e ouve Lia e Davi | narração preparada, repetível e offline | linguagem narrativa e atenção conjunta |
| 2. Compreensão | responde “o que falta?” por voz ou toca ⚽ | reconhece `bola` localmente, sem esperar IA | recuperar o objeto central do contexto |
| 3. Investigação | relaciona marcas, tronco e parte da bola | pergunta onde procurar; aceita voz ou pista visual | realizar inferência apoiada pelo contexto |
| 4. Explicação | indica “atrás da árvore” | reconhece a pista sem guardar transcrição nem atribuir nota | explicitar o raciocínio usado |
| 5. Manipulação | toca duas peças ou arrasta uma peça | abre o tabuleiro 2×2 sem menu; ajuda progressiva | consolidar a representação visual |
| 6. Palavra e som | ouve `BOLA`, `BO-LA`, `B` e `/b/` | microfone não é barreira para continuar | aproximar oralidade, sílaba e princípio alfabético |
| 7. Aplicação | orienta Davi a procurar a bola atrás da árvore | pede objeto e lugar na mesma ideia; oferece composição por toque | usar compreensão para produzir uma instrução útil |
| 8. Colaboração | explica a pista ao colega e troca de papel | encerramento explícito fora da tela | transferir o entendimento para interação social |

## Tratamento dos desvios

| Situação | Resposta fechada |
|---|---|
| silêncio ou áudio vazio | oferece nova tentativa e alternativa visual, sem culpa |
| resposta diferente de “bola” | reconhece a contribuição e manda ouvir o contexto novamente |
| outra hipótese sobre o esconderijo | acolhe a ideia, reapresenta a pista do tronco e permite tentar ou tocar |
| orientação oral incompleta | pede para ligar `bola` e `árvore` na mesma ideia; alternativa por toque permanece disponível |
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

Outros casos de estudo só entram depois que este percurso passar por observação com crianças e
alfabetizadores. Cada novo caso deve reutilizar o contrato `contexto → informação explícita → pista →
inferência → linguagem → aplicação`, mudando conteúdo e dificuldade, não acumulando minijogos.

## Critério para chamar a versão de “pronta para uso rápido”

O percurso só é aprovado quando passa em 360×640 dp, 412×915 dp e 800×1280 dp; nenhum CTA depende
de rolagem; toque e arraste funcionam; fala e inatividade não se sobrepõem; toda falha tem saída por
toque; métricas registram participação, modalidade, tempo e ajuda, nunca nota ou diagnóstico. Uso com
crianças reais continua condicionado a validação pedagógica, consentimento, proteção de dados,
acessibilidade e infraestrutura estável.
