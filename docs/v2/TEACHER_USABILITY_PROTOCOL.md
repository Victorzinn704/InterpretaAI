# Protocolo de validação do Estúdio com professoras

## Objetivo

Verificar se o fluxo permite preparar, revisar, publicar e acompanhar uma história sem conhecimento
técnico. O teste avalia o protótipo; não avalia desempenho docente nem eficácia pedagógica.

## Participantes

- cinco professoras ou profissionais com experiência nos anos iniciais;
- preferencialmente perfis com familiaridade digital variada;
- sem coletar nome de criança, turma real ou material infantil;
- identificadores da sessão: `P01` a `P05`.

Antes do teste, explicar finalidade, duração, registro e possibilidade de interromper. Gravação de
tela/voz só ocorre com autorização específica; o roteiro funciona apenas com notas do observador.

## Preparação

1. iniciar o protótipo local conforme `prototype/README.md`;
2. restaurar o estado antes de cada participante;
3. usar dados e imagem de demonstração;
4. escolher computador ou tablet habitual;
5. não ensinar a navegação; responder apenas “faça como achar natural”.

## Tarefas

### T1 — criar a partir de uma imagem

“Você quer trabalhar a palavra MAÇÃ em uma história curta com sua turma. Prepare uma proposta usando
a imagem de demonstração.”

Conclusão: alcança `Proposta pronta para revisão`.

### T2 — corrigir e regenerar somente uma cena

“Na revisão, confira a palavra e peça uma nova imagem apenas para a primeira cena.”

Conclusão: palavra `MAÇÃ` salva e confirmação de regeneração localizada exibida.

### T3 — conferir celular e tablet

“Veja como a história ficará em celular e tablet e identifique o objetivo/fontes usados.”

Conclusão: alterna os dois formatos e localiza as fontes sem abrir Administração.

### T4 — publicar e saber quem está pronto

“Envie a versão para o 1º ano A e descubra quais aparelhos podem iniciar.”

Conclusão: publica e distingue `PRONTA`, `PREPARANDO` e `SEM CONEXÃO`.

### T5 — interpretar evidência e observar

“Veja uma evidência da turma e registre que as duplas usaram a pista da árvore.”

Conclusão: identifica a origem/limite e salva observação humana.

## Registro por tarefa

| Campo | Valores |
|---|---|
| resultado | concluiu sem ajuda / com ajuda / não concluiu |
| tempo | segundos do início à condição de conclusão |
| desvios | páginas/ações que não contribuíram |
| dúvida de estado | fala ou comportamento observável |
| ajuda do moderador | texto exato, se ocorreu |
| severidade | bloqueio / importante / menor / preferência |
| observação | fato, sem inferir capacidade da participante |

Não usar “usuária errou”. Registrar “selecionou X esperando Y” ou “não encontrou o estado Pronta”.

## Perguntas finais

1. Em que momento você teve menos certeza sobre o que aconteceria?
2. O que você esperava que “Publicar” fizesse nos aparelhos?
3. Como identificou se um aparelho estava pronto?
4. O que você gostaria de editar sem pedir uma nova história?
5. Alguma informação pareceu técnica, excessiva ou arriscada?

## Critério de aceite

- pelo menos quatro de cinco concluem cada tarefa sem ajuda;
- nenhuma participante precisa entrar em Administração;
- publicação não é confundida com aparelho pronto por mais de uma participante;
- nenhum bloqueio de acessibilidade ou autorização permanece sem correção/plano;
- achados são classificados e vinculados a uma revisão do protótipo.

Preferência estética não bloqueia. Problema que causa publicação indevida, interpretação errada de
evidência ou exposição de dado bloqueia a saída da Sprint 0.

## Relatório final

Publicar apenas resultados agregados e achados do produto:

```text
data e versão do protótipo
perfil geral dos participantes
resultado T1–T5 (n/5)
tempo mediano por tarefa
achados por severidade
mudanças realizadas
risco aceito e responsável
decisão de saída da Sprint 0
```

O registro estruturado fica em `usability/teacher-study.json`. Enquanto `sessions` estiver vazio, o
avaliador retorna `NOT_RUN`; ele nunca preenche participante ou resultado automaticamente:

```bash
python3 tools/evaluate-teacher-usability.py \
  docs/v2/usability/teacher-study.json \
  --report docs/v2/usability/teacher-study.report.json
```
