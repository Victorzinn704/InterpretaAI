# InterpretaAI nos GETs do Rio — pesquisa e recorte do piloto

Pesquisa atualizada em 16/09/2026. Este documento separa fatos publicados, inferências e decisões do
produto; não afirma parceria, homologação ou acesso à rede municipal.

## Veredito executivo

Os Ginásios Educacionais Tecnológicos são um bom ambiente de validação: trabalham em turno único,
com aprendizagem ativa, colaboração, projetos, cultura digital, pensamento computacional e espaços
com tablets. A Prefeitura informa 313 GETs nas 11 CREs e meta de 500 até 2028. O InterpretaAI deve
ser apresentado como atividade curta e mediada em tablet institucional: concentra o aparelho no
ciclo LEIA e devolve a criança à conversa, desenho, dramatização ou produção coletiva.

## Tablet: o que sabemos e o que ainda precisa ser confirmado

- Fato: a página oficial dos GETs cita tablets, notebooks, realidade virtual, impressão 3D e robótica.
- Verificação repetida em 16/09/2026: a página oficial registra 313 GETs, 11 CREs e 500 unidades
  planejadas até 2028, mas não publica fabricante, modelo, versão Android ou política MDM dos tablets.
- Fato: documento público da Secretaria Municipal de Ciência e Tecnologia especifica tablet de 12,4
  polegadas, 128 GB, 6 GB de RAM, carregador de 15 W e caneta S Pen para o projeto Nave Satélite.
  Esse documento não é inventário nem compra dos GETs e não identifica fabricante ou modelo.
- Hipótese de teste, não fato institucional: essa combinação existe em modelos Android com S Pen,
  mas não há fonte primária localizada que permita atribuir um deles aos GETs. O modelo permanece
  **desconhecido** até evidência da SME, MDM ou aparelho autorizado.
- Ação antes do piloto: coletar em três aparelhos `fabricante`, `modelo`, versão Android, RAM,
  resolução e presença da S Pen pelo inventário/Configurações ou `adb shell getprop`. O APK deve
  continuar suportando Android 8+ e layouts adaptativos até essa confirmação.

Com autorização da escola e depuração USB temporária, a coleta pode ser feita sem número de série ou
conteúdo do estudante:

```bash
ADB_BIN=/caminho/para/adb ./tools/audit-school-tablet.sh
```

Arquive a saída de três unidades sob controle da equipe do piloto e desative novamente a depuração.
O modelo só passa de “desconhecido” para “confirmado” depois dessa evidência ou de inventário oficial
da SME. A especificação pública é útil apenas para preparar teste com caneta e tela grande; não
identifica o parque dos GETs.

## O que o aplicativo ensina hoje

| Experiência | Evidência observável | Papel no LEIA |
|---|---|---|
| Mistério da bola | escuta narrativa, identifica informação explícita, usa pista e explica | Ler, Entender e Interpretar |
| Formação de BOLA | relaciona palavra, letras e som | Ler e Aprender |
| Quebra-cabeça 2×2/3×2 | recompõe imagem/palavra e fala o objeto | Entender e Aprender |
| Missão do som M | busca palavra pelo som inicial | Ler e Aprender |
| Cenas expressivas | interpreta diálogo e contexto sem impor emoção única | Entender e Interpretar |
| Aplicação em grupo | dramatiza e explica o caminho adotado | Interpretar e Aprender |
| Quadro criativo | segue pista visual, desenha livremente e explica a criação | Entender e Aprender |

Isso é um bom ciclo demonstrável, mas ainda não é uma solução curricular completa do 1º ao 5º ano.
Na área adulta, cada uma das seis missões publicáveis agora mostra faixa de mediação, foco,
evidência observável e referências BNCC. A faixa não seleciona conteúdo automaticamente nem rotula a
criança; ajuda o professor a escolher conscientemente o pacote já disponível.

## Progressão proposta para 1º–5º ano

| Faixa | Núcleo de aprendizagem | Atividades adequadas |
|---|---|---|
| 1º | consciência fonológica, letras, sílabas, palavra e oralidade | som inicial, ordenar sílabas, imagem-palavra, narrativa ouvida |
| 2º | decodificação, segmentação, pontuação e frases curtas | montar frase, ler diálogo, recontar começo/meio/fim |
| 3º | fluência, vocabulário, informação explícita e inferência simples | gibi com pista, causa/consequência, legenda e bilhete |
| 4º | inferência, ponto de vista, coesão e gêneros | charge, notícia curta, comparar falas e justificar hipótese |
| 5º | leitura crítica, argumento, síntese e autoria | distinguir fato/opinião, final alternativo, áudio-resenha coletiva |

A BNCC prevê progressão de sons/letras e segmentação nos primeiros anos para leitura, produção e
inferência em gêneros diversos. O Currículo Carioca e os fascículos Reforço Rio reforçam narrativa,
oralidade, produção individual/coletiva e adaptação pelo professor ao ritmo do grupo. Logo, não se
deve criar “um app por idade”: o professor escolhe um pacote por habilidade e mediação necessária.
A distinção entre conteúdo demonstrado, base reutilizável e evolução futura está detalhada na
[matriz pedagógica do 1º ao 5º ano](PEDAGOGICAL_SCOPE_1_TO_5.md).

## Sala, avatar e atividade enviada pelo professor

O modelo recomendado separa identidade escolar de representação infantil:

```text
Professor cria Turma → cria Grupos → associa aluno real a Avatar no cofre escolar
                     → publica ActivityPack versionado para grupo/aparelho
Tablet sincroniza manifesto assinado → executa offline → envia eventos neutros pela outbox
Professor vê evidências individuais autorizadas; Secretaria vê agregados por turma/escola
```

Na tela infantil aparecem avatar e missão, nunca nome completo, matrícula, diagnóstico ou ranking.
Nos eventos, um código fechado como `pipa-07` diferencia participantes que escolheram a mesma
aparência; somente a área adulta mostra esse pseudônimo.
Num servidor institucional futuro, `StudentIdentity` fica em armazenamento separado de
`LearningEvent`; a relação exige RBAC de professor/secretaria. O piloto atual já audita leituras
agregadas, mas ainda usa tokens compartilhados por papel. O professor envia objetivo, nível de apoio,
atividade, prazo e grupo. Ele não edita prompt livre que será falado diretamente à criança: escolhe
um `ActivityPack` revisado. O protótipo atual já permite definir a turma, escolher um pseudônimo e avatar
pseudônimo, selecionar uma das seis atividades e publicar a missão neste tablet; a Home passa a
mostrar somente avatar, turma e missão, e os eventos seguintes recebem esse escopo. Cadastro de
identidade real e autenticação institucional ainda são evolução. O piloto já publica uma atividade
fechada por `deviceId`, consulta na Home e envia eventos neutros por outbox; professor e secretaria
recebem apenas agregados nas respectivas APIs. Isso comprova o canal técnico local, não autorização
para transportar dados reais de crianças nem implantação na rede municipal.

## Quadro criativo: valor pedagógico e limite da IA

O quadro implementado aceita arraste, quatro cores, espessura, borracha por gesto, limpar e pilhas
reais de desfazer/refazer. A borracha remove somente o traço da criança e preserva a pista. Bola,
maçã, casa e árvore têm traço-guia discreto, mas a criança pode desenhar fora
dele. A conclusão pede explicação oral para ligar forma, vocabulário e autoria.

Reconhecimento visual não deve dizer “você desenhou errado”. Uma evolução segura retorna hipóteses
curtas — “parece uma bola; quer me contar?” — e só depois da criança confirmar. Imagem não precisa
ser persistida; para o MVP, inferência local ou servidor autorizado deve receber recorte sem rosto,
metadados removidos e retenção zero. O Gemini Developer API permanece fora desse fluxo infantil
enquanto seus termos vedarem clientes acessados por menores.

## Modo foco institucional

O código já distingue fixação comum de `Lock Task` completo. A documentação Android confirma que
somente um app permitido por DPC em dispositivo totalmente gerenciado bloqueia notificações, apps
fora da allowlist, Home e Recentes; screen pinning pode ser encerrado pelo usuário. Para os GETs, o
piloto correto exige tablet de teste resetado/gerenciado, provisionamento por QR/EMM, app allowlisted,
`LOCK_TASK_FEATURE_NONE` e verificação de `LOCK_TASK_MODE_LOCKED`. Não prometer “totem total” em APK
instalado normalmente.

## Critérios do piloto

- 3 turmas, 2 atividades por habilidade e sessões de 8–12 minutos;
- observar início sem ajuda, pedidos de apoio, conclusão e explicação oral — sem nota automática;
- comparar atividade individual e em dupla/grupo;
- p95 de resposta contextual até 3 s, fallback até 6 s e jornada 100% concluível offline;
- entrevista curta com professores sobre preparo, envio e leitura dos sinais;
- inspeção de acessibilidade, estímulos reduzidos, modo foco e perda de rede em cada modelo de tablet.

## Fontes primárias e registros consultados

- [Página oficial dos GETs](https://educacao.prefeitura.rio/get/)
- [300º GET e expansão em 2026](https://educacao.prefeitura.rio/noticias/prefeitura-do-rio-inaugura-o-300o-ginasio-educacional-tecnologico-da-rede-municipal-de-ensino/)
- [Rede municipal em 2026 e proibição de celulares pessoais](https://educacao.prefeitura.rio/noticias/prefeitura-do-rio-inicia-ano-letivo-de-2026-para-mais-de-650-mil-alunos-das-1-557-escolas-municipais/)
- [Relatório anual municipal de 2024](https://educacao.prefeitura.rio/wp-content/uploads/sites/42/2025/03/RELATORIO-ANUAL-DE-GESTAO-TRANSPARENCIA-1.pdf)
- [Especificação pública municipal de equipamentos](https://cienciaetecnologia.prefeitura.rio/wp-content/uploads/sites/40/2023/06/TERMO-DE-COLABORACAO-NAVE-SATELITE.pdf)
- [BNCC oficial](https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf)
- [Currículo Carioca](https://educacao.prefeitura.rio/curriculo/)
- [Reforço Rio — História, Trilhas e Listas](https://educacao.prefeitura.rio/wp-content/uploads/sites/42/2023/05/FASCICULO5_1e2anos_HistoriaTrilhaseListas1.pdf)
- [Android Enterprise — Lock Task](https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode)
- [Android Enterprise — dispositivos dedicados](https://developer.android.com/work/dpc/dedicated-devices)
- [Termos atuais da Gemini Developer API](https://ai.google.dev/gemini-api/terms)
