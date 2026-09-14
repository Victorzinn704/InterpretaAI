# Auditoria do regulamento HACKTUDO 2026

**Data da auditoria:** 13/09/2026, 21h36 BRT  
**Escopo:** repositório, APK, servidor, PDF, ativos, histórico Git e declarações públicas do projeto.  
**Fonte primária:** [regulamento preservado](references/HACKTUDO-2026-Regulamento-oficial.pdf),
ligado pela [página oficial do evento](https://www.hacktudo.com.br/amais-hackathon-2026).  
**Natureza:** verificação técnica e documental; não substitui decisão da organização ou análise jurídica.

## Veredito executivo

O InterpretaAI está **bem alinhado aos quatro critérios de avaliação e tecnicamente demonstrável**.
Há evidência Git de desenvolvimento posterior ao anúncio do desafio e agora há inventário de
dependências, fontes e links. A aprovação formal, porém, ainda depende de confirmações que o código
não consegue fornecer: elegibilidade e composição da equipe, concepção efetivamente iniciada após o
anúncio, proveniência dos 13 ativos embarcados, materiais enviados no prazo e autorizações de imagem/
voz quando aplicáveis.

Portanto, o status correto é **ADERENTE COM CONFIRMAÇÕES OBRIGATÓRIAS**, e não “100% garantido”. O
maior risco não é a ideia pedagógica; é faltar documentação humana para originalidade e direitos de
terceiros, exigidos nos itens 8.1, 9.1, 9.2 e 17.2.

## Legenda

- **CONFORME:** há evidência verificável no repositório.
- **CORRIGIDO:** havia lacuna técnica/documental e a remediação foi aplicada nesta auditoria.
- **CONFIRMAR:** depende de evidência ou declaração externa da equipe.
- **FUTURO/NA:** não é exigência do MVP atual ou ainda não ocorreu.

## Matriz de conformidade

| Regra | Exigência resumida | Evidência/avaliação | Status | Ação antes do pitching |
|---|---|---|---|---|
| 2 e 4 | Equipe de 2 a 4 estudantes atualmente matriculados e inscrição válida. | O Git identifica um autor, mas não prova integrantes, matrícula ou inscrição. | **CONFIRMAR** | Arquivar nomes, comprovantes do semestre e confirmação oficial. |
| 4.9–4.18 | Prazos, alterações e confirmação da equipe. | Há divergências internas de datas entre corpo e FAQ. | **CONFIRMAR** | Não presumir; validar com a organização e guardar resposta. |
| 5–6 | Participar da abertura e acompanhar e-mail/Slack. | Fora do repositório. | **CONFIRMAR** | Representante registra presença e acompanha os canais. |
| 7.1–7.2 | Responder ao tema e ao desafio anunciado em 11/09. | Modo Foco, ciclo breve, colaboração fora da tela e documentação relacionam produto ao tema. | **CONFORME** | No pitch, abrir pelo problema “celular: distração → mediador breve”. |
| 7.3 e 9.1 | Solução concebida e desenvolvida do zero após o anúncio. | Primeiro commit em 12/09 às 20:36:47 BRT. Git prova código posterior, não data da concepção. | **CONFIRMAR** | Assinar declaração de originalidade e arquivar rascunhos/conversas datados. |
| 7.4–7.5 | Resposta original, relevante e viável, sem formato imposto. | Fluxo LEIA fechado e APK executável; limites reais estão declarados. | **CONFORME** | Defender inovação incremental, não exclusividade inexistente. |
| 8.1 e 17.1–17.3 | Equipe responde por IP, plágio, imagem, voz, nome e privacidade. | Política e inventário criados; proveniência de imagens/sons aguarda confirmação. | **CORRIGIDO / CONFIRMAR** | Concluir `ASSET_PROVENANCE.md`; pesquisar marca; não usar voz de personagem real. |
| 8.2 | Criatividade, inovação e possível aplicação de mercado. | Coautoria guiada + ciclo que termina em grupo; arquitetura local/freemium viável para piloto. | **CONFORME** | Demonstrar uma criança ajudando Davi, não listar funcionalidades soltas. |
| 8.3 e 13.3 | Mockup é aceito; volume/complexidade de código não decide. | Há produto funcional, mas narrativa prioriza impacto e coerência. | **CONFORME** | Evitar transformar pitch em tour técnico. |
| 9.1 | APIs/bibliotecas externas permitidas se disponíveis a todos. | Dependências públicas e acessíveis estão enumeradas. | **CONFORME** | Não depender de credencial exclusiva da equipe na avaliação. |
| 9.2 | Creditar bibliotecas/fontes e links no Pré-Pitching. | `THIRD_PARTY_NOTICES.md` foi criado; modelo padrão mudou de Qwen 3B para 1.5B Apache 2.0. | **CORRIGIDO** | Entregar link do repositório/créditos à banca e manter versões exatas. |
| 9.3–9.4 | Solução deve ser original; organização pode verificar. | Linha do tempo e divulgação de IA documentadas; frases exatas não tiveram equivalência pública localizada. | **CONFORME COM LIMITE** | Manter histórico Git e responder sem exagerar o alcance da busca. |
| 11.1–11.3 | Até 13/09 12h: PDF máx. 10 slides, vídeo máx. 2 min e links. | PDF tem exatamente 10 páginas; horário, vídeo, link e envio não são prováveis pelo repo. | **CONFIRMAR** | Guardar recibo de envio, URL pública e duração do vídeo. |
| 11.4–11.6 | Dez finalistas e anúncio por canais oficiais. | A equipe informou seleção entre 227, mas não há evidência oficial anexada. | **CONFIRMAR** | Arquivar e-mail/Slack/post oficial antes de publicar o número como fato. |
| 12.1–12.5 | Final: 5 minutos, um representante, ao vivo em 19/09. | Etapa futura. | **FUTURO** | Escolher representante, ensaiar com cronômetro e contingência offline. |
| 13.2–13.4 | Quatro critérios com peso equivalente. | Mapa detalhado abaixo. | **CONFORME** | Reservar tempo equivalente de pitch para os quatro critérios. |
| 15–16 | Conduta, boa-fé, horários e regras do ambiente. | Nenhuma infração localizada no conteúdo auditado. Presença é externa. | **CONFORME / CONFIRMAR** | Um responsável monitora agenda e canais. |
| 17.4 | Autoria permanece com seus proprietários. | `LICENSE.md` preserva direitos dos respectivos autores e separa terceiros. | **CORRIGIDO** | Formalizar acordo interno de titularidade entre integrantes. |
| 18.1–18.4 | Organização pode divulgar nomes, vozes, imagens e protótipos; menor pode exigir autorização. | Regra registrada, mas consentimentos são externos. | **CONFIRMAR** | Não mostrar criança real sem consentimento; preparar autorização se integrante for menor. |
| 18.5–18.10 | Conteúdo lícito, não ofensivo; fraude pode desclassificar; dúvidas pelos canais oficiais. | Linguagem não punitiva e sem diagnóstico; nenhum material ofensivo localizado. | **CONFORME** | Reportar qualquer dúvida material à organização, não improvisar interpretação. |

## Avaliação pelos quatro critérios

### 1. Adequação ao tema — forte

O celular é usado por um ciclo curto e intencional, com Modo Foco, uma decisão por tela e encerramento
em dupla quando o aparelho descansa. Isso responde diretamente ao problema de distração sem alegar
que tecnologia substitui professor ou convivência. Risco: demonstrar atividades avulsas faria o
produto parecer “mais um jogo no celular”. A apresentação deve mostrar o ciclo completo.

### 2. Originalidade e inovação — defensável como incremental

Tutoria por voz, fonética, puzzle e histórias interativas já existem. O diferencial verificável é o
contrato combinado: a criança ajuda personagens, verbaliza uma hipótese, manipula a pista, consolida
palavra/fonema, aplica o entendimento e continua em grupo sem a tela. Essa composição é original o
suficiente para uma defesa de inovação incremental, exatamente uma possibilidade reconhecida no item
13.2. Não há base para afirmar pioneirismo mundial.

### 3. Solução tecnológica — forte para MVP

Há APK Kotlin/Compose, API Java/Spring/LangChain4j, mediação local com Qwen 1.5B, vozes Kokoro,
visão no aparelho, timeout e fallback. Testes e auditoria visual tornam a execução verificável. Riscos:
servidor temporário sem autenticação/rate limit, qualidade de voz e reconhecimento em ruído e falta de
piloto real. Isso deve ser apresentado como limite, não escondido.

### 4. Utilidade e aplicabilidade — promissora, ainda não validada em campo

O fluxo reduz dependência de leitura autônoma, aceita voz/toque/arraste, oferece ajuda progressiva e
registra participação sem nota. Pode apoiar professor e grupos. O MVP não prova ganho de alfabetização
nem redução de analfabetismo funcional; essas são hipóteses para piloto com educadores e crianças,
consentimento e protocolo de avaliação.

## Riscos de originalidade e plágio

| Risco | Nível atual | Controle |
|---|---|---|
| Projeto preexistente ao desafio | Médio até declaração | Primeiro commit posterior + declaração assinada + rascunhos datados. |
| Imagens e sons sem cadeia de custódia | **Alto até confirmação** | Hashes e inventário existem; falta confirmar ferramenta/autor/termos. |
| Dependências/modelos sem crédito | Baixo após correção | `THIRD_PARTY_NOTICES.md` com versões, licenças e downloads. |
| Qwen 3B com licença restritiva | Resolvido no padrão | Migração para Qwen 2.5 1.5B Apache 2.0. |
| Semelhança com personagem/franquia infantil | Médio | Identidade própria; proibir imitação e a expressão “estilo Turma da Mônica”. |
| Nome `InterpretaAI` já usado por terceiros | Médio | Busca formal exata/radical/fonética no INPI antes de mercado. |
| Alegações de impacto sem piloto | Médio | Dizer “hipótese/potencial”; medir antes de alegar eficácia. |
| Voz clonada ou identificável | Baixo no desenho atual | Kokoro genérico; não usar voz de celebridade/personagem e documentar voicepack. |

## Checklist bloqueante antes da final

- [ ] Todos os 2–4 integrantes confirmam matrícula, inscrição e participação válidas.
- [ ] Todos assinam a declaração de criação após o anúncio e autoria responsável.
- [ ] Os 13 ativos de `ASSET_PROVENANCE.md` mudam para **CONFIRMADO** com evidência arquivada.
- [ ] O link de `THIRD_PARTY_NOTICES.md` está acessível no material entregue.
- [ ] A mensagem oficial de classificação entre os 10 é preservada.
- [ ] O PDF enviado tem no máximo 10 slides, o vídeo no máximo 2 minutos e há recibo do prazo.
- [ ] Um único representante está definido para o pitching de 5 minutos.
- [ ] Não há criança real, imagem ou voz identificável sem autorização adequada.
- [ ] O pitch não usa “inédito”, “sem concorrentes”, “comprovado” ou “elimina analfabetismo”.
- [ ] O APK e o servidor demonstrados correspondem ao commit/tag informado à banca.

## Decisão

**Estamos no caminho certo.** A direção pedagógica responde ao tema, a inovação pode ser defendida
sem exagero e a solução funciona como MVP. A equipe só deve declarar “em conformidade” após marcar os
itens bloqueantes acima com evidências. Até lá, a formulação correta é: “o repositório foi auditado e
as exigências técnicas/documentais foram atendidas; confirmações humanas estão explicitamente
pendentes”.
