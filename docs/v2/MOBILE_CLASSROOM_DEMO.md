# Sala móvel — entrega demonstrável

Estado verificado em 18/09/2026. Este recorte fecha o fluxo mínimo para demonstrar uma professora,
uma turma com até 40 alunos e tablets reutilizáveis. Ele não depende de um tablet pertencer para
sempre a uma criança.

## Jornada entregue

1. A professora abre o Estúdio autenticado e seleciona uma turma vinculada.
2. Cola um nome por linha e salva de 1 a 40 alunos. Nomes repetidos são recusados para evitar uma
   escolha ambígua no início da aula.
3. Abre uma aula de oito horas. O servidor devolve um código temporário e guarda apenas seu HMAC.
4. Um tablet já pareado com a escola abre a configuração adulta pelo link
   `interpretaai://device-setup`, protegido pelo PIN do piloto.
5. O adulto digita o código, vê a lista somente nessa área e escolhe o nome indicado na carteira.
6. O servidor reserva uma carteira por aparelho e uma por aluno. A Home infantil recebe apenas o
   pseudônimo e o avatar; nome real, lista e código não aparecem para a criança.
7. Enquanto a aula está ativa, o contexto autenticado do tablet passa a ser a turma da sessão.
   Histórias publicadas para essa turma entram no manifesto e podem ser preparadas offline.
8. A professora atualiza o mapa de carteiras e vê quantos tablets entraram. Ao encerrar a aula, os
   aparelhos ficam livres para outra turma.

## Separação de produtos

- **Professora:** Estúdio web em `https://interpretaai.deskimperial.online/studio/`, com OIDC,
  importação da lista, abertura/encerramento da aula, mapa de carteiras, revisão e envio de histórias.
- **Criança:** APK Android sem botão ou navegação docente na Home. A configuração do aparelho abre
  por deep link próprio e PIN; depois do vínculo, a criança vê somente avatar, turma e atividade.
- **Servidor:** PostgreSQL guarda lista, sessão e vínculo temporário; credencial revogável identifica
  o aparelho. O tablet pode mudar de sala dentro da mesma escola sem trocar sua identidade técnica.

O Estúdio web já funciona como o produto da professora no tablet, evitando manter duas bases Android
antes do piloto. Um APK docente separado pode ser criado depois se houver necessidade de recursos
nativos; a API e o modelo de autorização já estão separados do APK infantil.

## Evidência automática

- `ClassroomSessionControllerTest` pareia um tablet na turma de origem, importa três alunos em outra
  turma, abre uma aula, lista os nomes, ocupa a primeira carteira e confirma que o contexto do mesmo
  aparelho muda para a turma de destino.
- A suíte completa do servidor, os testes unitários Android e o lint passam.
- `tools/audit-studio-review.py` executa importação, abertura da aula e mapa de carteiras em
  390×844, 800×1280 e 1440×1000 sem overflow.
- As capturas sintéticas ficam em `output/screenshots/v2-studio-review-fixture/`, incluindo
  `mobile-classroom-session.png`, `tablet-classroom-session.png` e
  `desktop-classroom-session.png`.

## Evidência na Oracle

Validado na produção em 18/09/2026, usando somente nomes sintéticos:

- Flyway aplicou `V21` nos bancos de homologação e produção;
- health público, OIDC real, vínculo institucional, callback e BFF do Estúdio passaram;
- uma lista de cinco alunos foi importada e uma aula temporária foi aberta;
- um tablet ARM64 sintético foi pareado, recebeu os cinco lugares livres, ocupou um deles e passou
  a responder no contexto de `class_pilot`;
- a visão docente confirmou `5` alunos e `1` aparelho conectado;
- ao final da prova, a aula ficou `CLOSED` e o aparelho de teste ficou `REVOKED`;
- antes da migração foram criados dumps lógicos catalogáveis de produção e homologação. O repositório
  físico existente do pgBackRest possui backups recentes, mas uma nova execução manual ainda falha
  ao localizar o socket primário e precisa de correção separada.

## O que a demonstração comprova

- criação de N alunos, limitada a 40;
- identidade real confinada à área docente e ao início adulto da aula;
- perfil infantil temporário por sessão, sem amarrar aluno e aparelho permanentemente;
- exclusão mútua: um aluno por tablet e um tablet por aluno na aula;
- mudança do contexto de turma usada pelo canal de entrega;
- publicação de conteúdo para a turma e preparação offline já existentes no fluxo v2;
- acompanhamento de aparelhos conectados sem nota, ranking ou diagnóstico da criança.

## Limites declarados

- O “host da sala” é lógico e usa HTTPS pela rede disponível. O tablet da professora não cria uma
  rede Wi-Fi, não distribui IP e não substitui roteador ou MDM.
- A prova automática simula tablets com credenciais reais do protocolo. A validação em vários
  aparelhos físicos e na rede da GET/SME continua pendente.
- O nome é necessário para a organização da aula e exige política institucional de finalidade,
  acesso, retenção e exclusão antes de uso com crianças reais.
- As métricas existentes registram participação, modalidade, duração e ajuda. A calibração
  pedagógica e a leitura do painel com professoras ainda exigem piloto humano.

## Estado do MacBook

SSH e Tailscale estão operacionais. A cópia `~/estudos-java/InterpretaAI` foi preservada porque está
12 commits à frente e 31 atrás do GitHub. Java e Android SDK não aparecem no terminal padrão; por
isso, a compilação desta entrega usa o ambiente Windows validado. Uma cópia limpa da versão publicada
deve ser usada no Mac, sem reescrever o trabalho divergente.
