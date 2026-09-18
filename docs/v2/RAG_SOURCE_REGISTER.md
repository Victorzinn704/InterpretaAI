# Registro de fontes do RAG pedagógico

## Estado atual

A estrutura de ingestão está definida, mas **nenhuma fonte externa está aprovada neste documento**.
Título ou link não bastam: licença, versão, escopo e responsável precisam ser confirmados antes de
indexar. Isso impede que o RAG pareça fundamentado sem procedência real.

Em 18/09/2026, o JAR e as tabelas de fila chegaram à Oracle, mas os workers permanecem desligados.
O cluster não possui `pgvector`; a primeira ativação continuará lexical e só ocorrerá depois da
aprovação nominal das fontes próprias e de um conjunto de avaliação de recuperação/citação.

## Registro obrigatório

| Campo | Regra |
|---|---|
| `sourceId` | identificador estável e não semântico |
| `title` | nome visível da fonte |
| `owner` | entidade autora/detentora |
| `curator` | pessoa responsável pela entrada |
| `sourceVersion` | edição, hash ou data de vigência |
| `scope` | global, rede, escola ou professora |
| `licenseBasis` | licença, autorização ou uso interno documentado |
| `objectiveIds` | objetivos aos quais pode dar suporte |
| `yearRange` | faixa organizacional, nunca diagnóstico |
| `reviewStatus` | candidato, aprovado, suspenso ou expirado |
| `validFrom/validUntil` | janela de uso |
| `contentHash` | prova do conteúdo efetivamente indexado |

## Coleções iniciais

| Coleção | Conteúdo esperado | Responsável por aprovar | Estado |
|---|---|---|---|
| `methodology` | método LEIA e exemplos próprios | produto + pedagogia | candidato |
| `curriculum` | objetivos curriculares oficiais/licenciados | pedagogia | pendente |
| `accessibility` | instruções e apoios aprovados | acessibilidade + pedagogia | pendente |
| `editorial` | LÉIA, cachorro, voz e identidade visual | produto | candidato |
| `approved_examples` | histórias humanas aprovadas e avaliadas | curadoria | vazio |
| `teacher_library` | material da própria professora/escola | professora/escola | por item |

## Fontes candidatas do primeiro piloto

Ainda não indexadas nem aprovadas:

| Fonte | Uso candidato | Escopo proposto | Falta para ativar |
|---|---|---|---|
| [BNCC oficial](https://www.gov.br/mec/pt-br/escola-em-tempo-integral/BNCC_EI_EF_110518_versaofinal.pdf) | objetivos e enquadramento de Língua Portuguesa | global | selecionar trechos, versão/hash e curador |
| [Currículo da SME Rio](https://educacao.prefeitura.rio/curriculo/) | objetivos locais do piloto | rede Rio | confirmar documento vigente e licença de ingestão |
| [Recursos pedagógicos SME Rio](https://educacao.prefeitura.rio/recursos-pedagogicos/) | referência para planejamento docente | rede Rio | curadoria por item e permissão de uso |
| [método LEIA do InterpretaAI](guidance/methodology-leia-v1.md) | sequência Ler–Entender–Interpretar–Aprender | global próprio | revisão pedagógica |
| [guia editorial da LÉIA](guidance/editorial-leia-v1.md) | personagem, voz, cachorro e limites | global próprio | aprovação de produto |
| [acessibilidade infantil](guidance/accessibility-child-v1.md) | viewport, fala e apoio progressivo | global próprio | revisão de acessibilidade/pedagogia |

Uma página pública não significa automaticamente permissão para copiar integralmente, gerar
derivados ou compartilhar entre escolas. O registro `licenseBasis` continua obrigatório.

As três fontes próprias possuem versão, manifesto e SHA-256, mas continuam `CANDIDATE`. O
recuperador de produção só pode consultar `reviewStatus=APPROVED`; mudar o texto cria novo hash e
exige nova revisão.

## Pipeline de ingestão

1. registrar fonte e base legal/licença;
2. extrair texto sem executar conteúdo do arquivo;
3. normalizar, dividir e calcular hash;
4. classificar metadados e escopo;
5. revisão humana por amostra e aprovação;
6. indexar versão imutável;
7. executar casos de recuperação e isolamento;
8. ativar; suspender imediatamente quando expirar ou for revogada.

Prompt injection dentro de documento é tratado como conteúdo, nunca como instrução. O recuperador
retorna trechos e metadados; ferramentas, regras e permissões permanecem fora do corpus.

## Critério para `pgvector`

Começar com PostgreSQL full-text + filtros. Criar um conjunto de perguntas e fontes esperadas.
Adicionar busca vetorial apenas se aumentar recuperação relevante sem piorar autorização,
rastreabilidade ou custo operacional. O resultado deve continuar citando `sourceId/sourceVersion`.
