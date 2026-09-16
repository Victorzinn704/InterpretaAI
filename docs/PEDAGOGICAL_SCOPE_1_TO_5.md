# Escopo pedagógico do 1º ao 5º ano

Pesquisa atualizada em 15/09/2026. Esta matriz traduz o Currículo Carioca em decisões de produto;
ela não substitui planejamento docente, validação com alfabetizadores ou homologação pela SME-Rio.

## Decisão central

O InterpretaAI não deve ser “um aplicativo de 1º ano” nem oferecer uma sequência automática baseada
somente na idade. O mesmo ciclo **LEIA — Ler, Entender, Interpretar e Aprender —** permanece, mas o
professor publica um `ActivityPack` adequado à habilidade e ao nível de apoio do grupo. Isso acompanha
o Currículo Carioca: o texto é ponto de partida e chegada; oralidade, leitura, escrita e análise
linguística se integram; as habilidades se repetem em espiral com desafios crescentes.

## O ciclo demonstrável hoje

```text
OUVIR/OBSERVAR história multimodal
        ↓
ENTENDER informação explícita: “o que falta?”
        ↓
INTERPRETAR pista e contexto: “onde procurar e por quê?”
        ↓
APRENDER fazendo: montar, ordenar, desenhar, falar e explicar ao grupo
        ↓
PROFESSOR recebe evidência neutra: ação, duração, ajuda e etapa concluída
```

O APK 0.9 prova um recorte pequeno: Mistério da Bola, cinco cenas expressivas, formação de `BOLA`,
quebra-cabeças de bola/banana/maçã, missão do som M, quadro com bola/maçã/casa/árvore e aplicação oral
em grupo. Ele **não prova** fluência leitora, escrita longa, domínio anual ou redução do analfabetismo
funcional; essas afirmações dependem de conteúdo maior e estudo de campo.

## Matriz de progressão

| Ano | Ênfase curricular selecionada | Como o ciclo deve mudar | Situação no produto | Evidência útil ao professor |
|---|---|---|---|---|
| 1º | turnos de fala; relação fonema–grafema; sílaba/palavra; rimas; informação explícita em texto ouvido | narrativa integralmente falada, imagem forte, som inicial, montar palavra e recontar oralmente | **demonstrado parcialmente** por BOLA, som M, puzzle e gibi | iniciou sem ajuda, reconheceu som/palavra, pediu pista, recontou |
| 2º | finalidade e recursos gráficos de textos; personagem; antecipação por título/imagem; produção coletiva e revisão | ordenar começo–meio–fim, escolher fala/balão e produzir frase com professor escriba | **base reutilizável**, conteúdo específico ainda não implementado | sequência proposta, pista usada, revisão após escuta do grupo |
| 3º | diálogo; informação implícita com mediação; causa/consequência; comparação de textos multimodais | investigar duas pistas, explicar consequência e comparar duas versões curtas | **demonstrado parcialmente** nas cenas e pista da árvore | hipótese inicial, evidência citada, mudança de hipótese, justificativa oral |
| 4º | autonomia oral; fluência; inferência; ponto de vista; fato/opinião; efeito de humor | charge/tirinha com pontos de vista, “o que o texto mostra?” versus “o personagem pensa?” | **estrutura visual existe**, pacote curricular não implementado | distingue fala/fato/opinião, explica humor e respeita turno do grupo |
| 5º | comparar textos; inferir sentido; explicar humor; produzir narrativa, balões, legendas e onomatopeias | coautoria de final alternativo, notícia × opinião e montagem de uma tirinha pelo grupo | **evolução futura**, não deve ser simulada no pitch | argumento + pista, síntese oral, revisão e autoria coletiva |

“Situação no produto” é deliberadamente rigorosa: possuir um botão ou uma tela parecida não equivale a
ensinar a habilidade. Cada pacote futuro precisa de conteúdo revisado, critério observável e teste com
professor antes de ser marcado como implementado.

## O que o professor envia

Para preservar simplicidade e segurança, o professor não escreve um prompt livre para a criança. Ele
seleciona um pacote aprovado:

```text
ActivityPack
├── habilidade curricular e ano/faixa de apoio
├── história/texto e vocabulário aprovados
├── uma pergunta por etapa
├── modalidades permitidas: voz, toque, arraste, desenho
├── pistas graduais: oral → visual → demonstração
├── dinâmica: individual, dupla ou grupo
└── evidências neutras que podem ser registradas
```

O MVP já publica quatro atividades fechadas por `deviceId`. A evolução correta é versionar mais
`ActivityPacks`, não liberar geração irrestrita. O professor escolhe **o que ensinar** e para qual
avatar/grupo; a LEIA apenas adapta a formulação curta dentro do contexto aprovado.

As quatro missões atuais mostram no painel adulto uma ficha curta antes do envio:

| Missão | Foco declarado no produto | Referências de planejamento |
|---|---|---|
| Mistério da bola | escuta, informação explícita e inferência por pista | EF15LP03, EF15LP14 e EF35LP04 |
| Quebra-cabeça | imagem, palavra, sílabas e som inicial | EF01LP06, EF01LP08 e EF02LP04 |
| Quadro criativo | vocabulário, representação visual e explicação oral | EF15LP09, EF15LP10 e apoio à produção |
| Som M | som inicial e relação fonema–grafema | EF01LP07, EF01LP08 e EF02LP06 |

Esses códigos indicam alinhamento de planejamento, não comprovam que uma execução isolada desenvolve
integralmente a habilidade. O 4º e o 5º ano continuam dependendo de novos pacotes de leitura crítica,
ponto de vista, síntese e autoria antes de serem declarados cobertos.

## Dinâmica para tablets compartilhados

- **um tablet por criança:** avatar pseudônimo e pacote individual; nenhuma identidade aparece na tela;
- **um tablet por dupla:** dois avatares, papéis alternados de narrador e detetive, uma resposta do grupo;
- **um tablet por grupo:** leitor/ouvinte, investigador, montador e porta-voz se revezam; o aparelho
  encerra em “agora contem ao grupo”, em vez de capturar toda a aula;
- **estação do colaboratório:** 8–12 minutos de atividade, seguida de desenho, dramatização, escrita ou
  discussão fora da tela.

Os dados detalhados pertencem ao professor autorizado. Secretaria recebe agregados por turma/escola,
sem ranking nominal, transcrição, áudio, desenho, diagnóstico ou suposta emoção “correta”.

## Critério mínimo para acrescentar um pacote

1. habilidade e gênero textual identificados no Currículo Carioca;
2. texto/imagem com autoria e licença confirmadas;
3. pergunta que admite explicação, não apenas toque correto;
4. alternativa sem microfone e sem leitura autônoma quando necessária;
5. pista progressiva sem culpa ou vermelho punitivo;
6. evento pedagógico observável e acionável pelo professor;
7. revisão por alfabetizador e educação especial;
8. teste de compreensão, acessibilidade e tempo em tablet real.

## Fontes primárias

- [Currículo Carioca de Língua Portuguesa](https://educacao.prefeitura.rio/wp-content/uploads/sites/42/2023/05/LINGUAPORTUGUESA.pdf)
- [Material Reforço Rio — História: Não Confunda](https://educacao.prefeitura.rio/wp-content/uploads/sites/42/2023/05/FASCICULO4_1e2anos_Historia.NaoConfunda1.pdf)
- [GETs: público, colaboração, métodos ativos e cultura digital](https://educacao.prefeitura.rio/sube-programas-e-projetos-3/)
- [Documento orientador de Educação Integral e GET](https://multirio.rio.rj.gov.br/media/PDF/pdf_6211.pdf)
- [BNCC oficial](https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf)
