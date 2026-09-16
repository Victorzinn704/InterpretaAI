# InterpretaAI nos GETs do Rio — pesquisa e recorte do piloto

Pesquisa atualizada em 16/09/2026. Este documento separa fatos publicados, inferências e decisões do
produto; não afirma parceria, homologação ou acesso à rede municipal.

## Veredito executivo

Os Ginásios Educacionais Tecnológicos são um bom ambiente de validação: trabalham em turno único,
com aprendizagem ativa, colaboração, projetos, cultura digital, pensamento computacional e espaços
com tablets. A Prefeitura informa 313 GETs nas 11 CREs e meta de 500 até 2028. O InterpretaAI deve
ser apresentado como atividade curta e mediada em tablet institucional: concentra o aparelho no
ciclo LEIA e devolve a criança à conversa, desenho, dramatização ou produção coletiva.

## Tablet: modelo localizado e limite da evidência

- Fato: a página oficial dos GETs cita tablets, notebooks, realidade virtual, impressão 3D e robótica.
- Verificação repetida em 16/09/2026: a página oficial registra 313 GETs, 11 CREs e 500 unidades
  planejadas até 2028, mas não publica fabricante, modelo, versão Android ou política MDM dos tablets.
- O resultado oficial do PE-RP-0692/2022 foi localizado no Comprasnet, UASG `986001`, código interno
  `1057649`, sessão de 18/07/2022. O item 1 adjudicado à Globali por **R$ 1.754,56 por unidade** é o
  **Samsung Galaxy Tab A8 4G, SM-X205N / SM-X205NZAUZTO**, com tela de 10,5", 4 GB de RAM, 64 GB,
  microSD até 1 TB, Wi-Fi 2,4/5 GHz, Bluetooth 5, 4G, microfone, câmeras de 5/8 MP, bateria de
  7.040 mAh, Android 9 ou superior e capa protetora. A proposta e a ficha técnica da Globali também
  estão anexadas ao pregão.
- Dois contratos da SME fecham a rastreabilidade pelo mesmo preço e pela mesma ata: o contrato
  146/2023, processo `SME-PRO-2023/32264`, adquiriu 11 unidades por R$ 19.300,16; o contrato
  147/2023 adquiriu mais 2 por R$ 3.509,12. Assim, o modelo desses **13 tablets adquiridos pela SME**
  está identificado com evidência primária, e o alvo de layout 800×1280 dp representa a proporção da
  tela física 1200×1920 desse equipamento.
- Limite: os contratos não informam escola, GET ou número patrimonial. Portanto, essa descoberta não
  prova que todo o parque dos GETs — nem mesmo o aparelho observado pelo usuário — seja composto por
  essas 13 unidades. Para afirmar o modelo de um GET específico ainda é necessário inventário da SME,
  MDM ou diagnóstico de um aparelho autorizado.
- O processo `07/002.382/2022` também previa aquisição ampla de tablets, notebooks e gabinetes no
  PE-RP-SME 1012/2022, estimado em R$ 183.009.745,00, mas o Diário Oficial de 17/10/2022 registra
  suspensão `sine die`. Ele não serve como prova de equipamento entregue.
- Outras buscas encontraram aquisições de componentes eletrônicos e smartphones para a SME, mas
  nenhum documento que relacione fabricante/modelo ao parque completo dos GETs. Esses registros não
  podem ser reutilizados como inventário escolar.
- Fato: documento público da Secretaria Municipal de Ciência e Tecnologia especifica tablet de 12,4
  polegadas, 128 GB, 6 GB de RAM, carregador de 15 W e caneta S Pen para o projeto Nave Satélite.
  Esse documento não é inventário nem compra dos GETs e não identifica fabricante ou modelo.
- A especificação de 12,4" com S Pen continua sendo somente uma hipótese de outro projeto. Ela não
  descreve o Galaxy Tab A8 comprado nos contratos 146/147, e o InterpretaAI não pode exigir caneta:
  desenho por dedo, toque e arraste são o baseline do piloto.
- Ação antes do piloto: coletar em três aparelhos `fabricante`, `modelo`, versão Android,
  resolução e presença de caneta pelo inventário, diagnóstico adulto do APK ou `adb shell getprop`. O APK deve
  continuar suportando Android 8+ e layouts adaptativos até essa confirmação.

### Compatibilidade do Galaxy Tab A8 identificado

| Necessidade do InterpretaAI | Evidência do SM-X205N | Veredito do piloto |
|---|---|---|
| instalação | edital exige Android 9+; APK usa `minSdk 26` (Android 8) | compatível por especificação |
| viewport infantil | 10,5", 1920×1200; auditoria visual inclui a mesma proporção em 800×1280 dp | adequado, validar no aparelho físico |
| execução local | 4 GB de RAM e 64 GB de armazenamento | suficiente para APK, áudio preparado e desenho; medir Qwen apenas no servidor |
| fala e escuta | microfone e alto-falantes integrados | hardware presente; serviço de reconhecimento/TTS ainda deve ser auditado |
| câmera | frontal 5 MP e traseira 8 MP | atende pesquisa visual futura; câmera não é requisito para concluir a jornada |
| desenho | tela capacitiva multitoque | toque e arraste atendem; caneta não é pressuposto |
| operação sem rede | armazenamento local, Wi-Fi/4G disponíveis | jornada principal continua offline; sincronização fica oportunista |
| modo totem | Android é compatível com Lock Task | bloqueio total depende de DPC/MDM e allowlist da SME, ainda não comprovados |

### Auditoria atual de tela — emulador, não GET físico

Em 16/09/2026, a versão atual do APK foi executada em emulador Android 15 configurado em
1200×1920 px e densidade 240, isto é, **800×1280 dp** em retrato e a proporção física do Tab A8.
Seis testes de captura percorreram Home compartilhada, gibi, quebra-cabeça, encerramento do 5º ano e
quadro 0.17 vazio/com toque e arraste; outros cinco testes conferiram desenho e critérios de tela sem
rolagem. Todos passaram. A [galeria](GALLERY.md#jornada-infantil-no-perfil-de-tablet) e as
[18 capturas](../output/screenshots/galaxy-tab-a8/) mostram ações visíveis, sem corte nem sobreposição
observada. Isso confirma apenas a composição e interação simuladas nesse perfil, não desempenho,
áudio, reconhecimento de voz, câmera, toque simultâneo, autonomia ou Lock Task no SM-X205N real.

Uma revisão pedagógica posterior retirou a resposta do título, do diálogo inicial e do botão da
primeira pergunta. A criança pode tentar falar antes de solicitar a figura; essa ajuda continua
acessível sem microfone e sem rolagem. O fluxo foi novamente aprovado por testes instrumentados nos
perfis 360×640, 412×915 e 800×1280 dp. O efeito dessa ordem sobre autonomia e atenção ainda requer
observação em sala, não é resultado comprovado de aprendizagem.

Risco visual remanescente: Home e telas de resposta/conversa usam pouco da altura do tablet; é uma
escolha de foco, mas sua eficácia contra dispersão ainda precisa ser observada com crianças e
professores. Antes de ampliar o piloto, validar no aparelho autorizado a escala de fonte do sistema,
capacidade de toque por dedo, volume/clareza da voz e comportamento do foco gerenciado.

Com autorização da escola e depuração USB temporária, a coleta pode ser feita sem número de série ou
conteúdo do estudante:

```bash
ADB_BIN=/caminho/para/adb ./tools/audit-school-tablet.sh
```

Arquive a saída de três unidades sob controle da equipe do piloto e desative novamente a depuração.
O relatório de terminal V3 calcula a menor dimensão em dp e separa cinco decisões: compatibilidade
de instalação (`minSdk 26`), toque, entrada de voz, prontidão do foco gerenciado e caneta opcional.
Ele não reprova um aparelho sem caneta nem confunde ausência de microfone com impossibilidade total:
nesses casos o desenho por dedo e a resposta por toque continuam disponíveis.
Sem ativar a depuração, o educador também pode abrir **Professor → Diagnóstico deste tablet →
Copiar diagnóstico**. O relatório V3 mostra fabricante/modelo, Android, tela, arquitetura, câmera,
microfone, toque, reconhecimento de voz, mecanismo TTS, caneta ativa e estado do Lock Task. Ele não
coleta serial, IMEI, conta, IP, token ou dado infantil. A detecção de caneta reflete os dispositivos
de entrada ativos naquele momento; por isso a caneta deve tocar a tela antes da coleta.
O diagnóstico distingue duas perguntas: ele confirma se o aparelho em mãos é o `SM-X205N`, e revela
se o parque local tem versões ou modelos diferentes. A compra dos 13 aparelhos está documentada; a
distribuição deles entre GETs permanece sem prova.
Se ainda não houver acesso a um aparelho autorizado, use o
[pedido objetivo de informação](GET_TABLET_INFORMATION_REQUEST.md), que solicita somente dados de
inventário e gestão necessários à compatibilidade, sem identificadores ou dados pessoais.

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
Na área adulta, cada uma das oito missões publicáveis agora mostra faixa de mediação, foco,
evidência observável e referências BNCC. A faixa não seleciona conteúdo automaticamente nem rotula a
criança; ajuda o professor a escolher conscientemente o pacote já disponível.
O professor também filtra a lista por ano na mesma tela, e pode voltar a `TODAS` para recomposição de
habilidades fundamentais. Essa decisão segue a orientação municipal de adaptar os fascículos às
necessidades e ritmos de grupos diferentes, inclusive usando habilidades iniciais com turmas de
3º–5º ano; o filtro não é diagnóstico nem trilha automática.

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

O encaixe específico com o GET é mais forte quando o tablet inicia uma investigação e devolve a
turma a uma produção concreta. A página oficial descreve o “GET na Prática” como 90 atividades mão
na massa, estruturadas mas adaptáveis pelos docentes, e afirma que a avaliação processual valoriza a
construção e as aprendizagens do percurso acima do acabamento do produto. Por isso, o InterpretaAI
registra participação, ajuda, modalidade e etapa; não pontua a beleza do desenho ou a resposta do
grupo.

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
pseudônimo, selecionar uma das oito atividades e publicar a missão neste tablet; a Home passa a
mostrar somente avatar, turma e missão, e os eventos seguintes recebem esse escopo. Cadastro de
identidade real e autenticação institucional ainda são evolução. O piloto já publica uma atividade
fechada por `deviceId`, consulta na Home e envia eventos neutros por outbox; professor e secretaria
recebem apenas agregados nas respectivas APIs. Isso comprova o canal técnico local, não autorização
para transportar dados reais de crianças nem implantação na rede municipal.

Para reduzir preparo em sala, a área adulta está dividida em **Missão**, **Turma** e **Tablet**.
O professor escolhe a atividade e vê a ficha pedagógica antes de formar o grupo; a aba Turma confirma
qual missão será enviada; diagnóstico, foco e credenciais ficam separados. Configuração técnica não
é requisito visual para escolher o que ensinar.

Para os dois usos observados nos GETs, o contrato distingue: um avatar gera evento individual
pseudonimizado; dois a quatro avatares associados ao mesmo tablet geram uma missão por aparelho e
eventos coletivos. A criança vê somente os avatares do grupo; professor e secretaria não recebem uma
autoria individual inventada para a resposta compartilhada. O APK 0.14 também fala e mostra um
rodízio por avatar em cada etapa, permitindo que o tablet coletivo distribua papéis sem pedir leitura
autônoma ou cadastro adicional.

## Quadro criativo: valor pedagógico e limite da IA

O quadro implementado aceita ponto por toque e traço suavizado por arraste, quatro cores, duas
espessuras, borracha por gesto, limpar e pilhas reais de desfazer/refazer. Os controles refletem o
histórico imediatamente e o buffer do gesto não copia toda a linha a cada movimento. A borracha
remove somente o traço da criança e preserva a pista. Bola,
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

O mesmo diagnóstico adulto informa separadamente se o app está autorizado ao Lock Task e se o foco
está ativo. “Foco ativo” sozinho não prova provisionamento institucional; a aprovação do piloto exige
`lock_task_permitted=true` e teste real de Home/Recentes no modelo autorizado.

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
- [Diário Oficial de 03/10/2022 — processos de componentes para GETs e smartphone SME](https://doweb.rio.rj.gov.br/portal/edicoes/download/5472)
- [Diário Oficial de 20/10/2023 — fiscalização do contrato 146/2023 de tablets](https://doweb.rio.rj.gov.br/portal/edicoes/download/6047)
- [Contas Rio — contratos 146/2023 e 147/2023 com a Globali](https://riotransparente.rio.rj.gov.br/web/index.asp?DESC_ORGAO_ENTIDADE_SIG=TODOS&EXERCICIO=2023&PagAtual=31&cmd=contratosObjetoResposta2&descUA=TODOS&especie=TODAS&inicioCursor=360&objeto=ROCINHA&objetoSelecionado=&ordena=&situacao=ATIVO&ua=TODOS&uo=)
- [Comprasnet — resultado por fornecedor do PE-RP-0692/2022](https://comprasnet.gov.br/livre/Pregao/FornecedorResultadoDecreto.asp?prgcod=1057649)
- [Comprasnet — propostas e ficha técnica do PE-RP-0692/2022](https://comprasnet.gov.br/livre/Pregao/anexosPropostaHabilitacao.asp?prgCod=1057649)
- [Samsung — especificações oficiais do Galaxy Tab A8 4G SM-X205](https://www.samsung.com/pt/business/tablets/galaxy-tab-a/tab-a8-sm-x205nzseeub/)
- [Diário Oficial de 17/10/2022 — suspensão do PE-RP-SME 1012/2022](https://doweb.rio.rj.gov.br/portal/edicoes/download/5483)
- [API oficial de dados abertos do Compras.gov.br](https://dadosabertos.compras.gov.br/)
- [BNCC oficial](https://basenacionalcomum.mec.gov.br/images/BNCC_EI_EF_110518_versaofinal_site.pdf)
- [Currículo Carioca](https://educacao.prefeitura.rio/curriculo/)
- [Reforço Rio — História, Trilhas e Listas](https://educacao.prefeitura.rio/wp-content/uploads/sites/42/2023/05/FASCICULO5_1e2anos_HistoriaTrilhaseListas1.pdf)
- [Android Enterprise — Lock Task](https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode)
- [Android Enterprise — dispositivos dedicados](https://developer.android.com/work/dpc/dedicated-devices)
- [Termos atuais da Gemini Developer API](https://ai.google.dev/gemini-api/terms)
