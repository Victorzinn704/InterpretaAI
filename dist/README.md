# Artefatos de entrega

Esta é a única pasta oficial de distribuição do MVP:

- `InterpretaAI-mvp-debug.apk`: APK local demonstrável, assinado com certificado de debug. No GitHub,
  ele é distribuído pelo Release porque ultrapassa o limite de um arquivo comum do repositório;
- `InterpretaAI-v0.4.0-fluxo-equilibrado.apk`: cópia nomeada da versão que combina fluidez,
  investigação e ajuda progressiva; as versões 0.2 e 0.3 permanecem disponíveis para comparação;
- `InterpretaAI-v0.5.0-resposta-progressiva.apk`: versão com troca `ACK → FINAL_TEXT → COMPLETE`,
  cancelamento de rede e compatibilidade automática com o endpoint anterior;
- `InterpretaAI-v0.6.0-on-off-contextual.apk`: preserva o texto validado quando o áudio remoto cai e
  mantém perguntas específicas das cinco cenas da galeria mesmo sem internet;
- `InterpretaAI-v0.7.0-quadro-criativo.apk`: acrescenta o quadro sem rolagem, desenho por arraste,
  desfazer/refazer e o fluxo local em que o professor escolhe turma, avatar e uma missão fechada;
  a 0.6 permanece para comparação;
- `InterpretaAI-v0.8.0-piloto-online.apk`: acrescenta o canal versionado professor → tablet, com
  consulta automática apenas na Home, persistência local e continuidade offline;
- `InterpretaAI-v0.9.0-sala-piloto.apk`: separa avatar de pseudônimo, permite montar uma sala adulta
  com até 40 tablets e enviar a mesma missão para todos ou uma seleção, mantendo a Home infantil sem
  identidades;
- `InterpretaAI-v0.10.0-metricas-agregadas.apk`: acrescenta outbox SQLite v3, sincronização de
  eventos pedagógicos neutros e preservação do texto validado quando o áudio ultrapassa o prazo;
  a 0.9 permanece disponível para comparação em outro aparelho;
- `InterpretaAI-v0.11.0-diagnostico-tablet.apk`: acrescenta o diagnóstico adulto de compatibilidade,
  copiável sem identificadores persistentes ou dados infantis, e consolida os pacotes falados de
  progressão do 2º ao 5º ano;
- `InterpretaAI-v0.12.0-fluxo-pedagogico.apk`: fecha o percurso dos pacotes do 2º ao 5º ano com
  eventos identificáveis, conclusão única, celebração específica e continuidade fora da tela;
- `InterpretaAI-v0.13.0-tablet-compartilhado.apk`: permite dois a quatro avatares no mesmo tablet,
  envia uma missão por aparelho e registra participação coletiva sem atribuição individual falsa;
- `InterpretaAI-v0.14.0-rodizio-colaborativo.apk`: mantém o escopo coletivo e orienta por voz e
  imagem o rodízio de procurar, responder, montar/desenhar e explicar entre os avatares;
- `InterpretaAI-v0.15.0-fluxo-docente.apk`: separa Missão, Turma e Tablet, coloca a escolha
  pedagógica antes das métricas e recolhe tokens até a configuração explícita;
- `InterpretaAI-v0.16.0-filtro-pedagogico.apk`: organiza as oito missões por 1º–5º ano na mesma
  tela docente e preserva `TODAS` para recomposição, sem classificar a criança;
- `InterpretaAI-v0.17.0-quadro-fluido.apk`: aceita ponto por toque e traço suavizado por arraste,
  evita cópias crescentes durante gestos longos e atualiza desfazer/refazer imediatamente;
- `InterpretaAI-v0.18.0-ajuda-progressiva.apk`: apresenta o mistério sem nomear o objeto de início;
  a criança pode responder por voz e pedir a alternativa visual quando precisar;
- `InterpretaAI-v0.19.0-missoes-curtas.apk`: acrescenta três missões fechadas escolhidas pela
  professora, LEIA com cachorro e uma foto sintética de bola para formar a palavra; adapta os alvos
  a celular e tablet, sem adicionar dependência de rede às novas atividades;
- `InterpretaAI-v0.20.0-acessibilidade.apk`: descreve estados dos jogos para leitor de tela,
  oferece um convite suave após inatividade e amplia o teste de escolha docente e do contrato de
  envio/recebimento das três missões;
- `InterpretaAI-v0.21.0-universal.apk`: mesma experiência, compatível com os ABIs embarcados no
  APK universal; prefira esta cópia quando não souber a arquitetura do aparelho;
- `InterpretaAI-v0.21.0-arm64.apk`: mesma experiência, cerca de 42 MiB, somente para aparelhos
  `arm64-v8a` (tablets e celulares ARM64). Não instale em x86 ou ARM 32-bit. Compilação:
  `./gradlew :app:assembleDebug -PtargetAbi=arm64-v8a`;
- `InterpretaAI-v0.22.0-universal.apk`: executa histórias variáveis aprovadas a partir do cache
  privado, com gibi, quebra-cabeça por toque/arraste, formação de palavra, conversa em dupla e
  retomada da etapa ativa; use quando não souber a arquitetura do aparelho;
- `InterpretaAI-v0.22.0-arm64-oracle.apk`: versão 0.22 limitada a `arm64-v8a`, conectada por padrão
  ao domínio Oracle; publicada com hash na
  [release v0.22.0](https://github.com/Victorzinn704/InterpretaAI/releases/tag/v0.22.0-oracle-foundation);
- `InterpretaAI-Proposta-MVP.pdf`: proposta auditada com exatamente 10 páginas A4;
- `InterpretaAI-oracle-arm64.tar.gz`: pacote separado na pasta fácil de enviar, com JAR, Kokoro,
  systemd, Caddy, instalador idempotente e verificador público; não contém credenciais;
- `SHA256.txt`: integridade do APK principal, das versões atuais e do PDF.

Desde a versão 0.22, a configuração padrão aponta para
`https://interpretaai.deskimperial.online`; a URL não é segredo. Sem credencial pareada, o caminho
infantil continua usando o conteúdo e a mediação locais. Para gerar uma variante contra outro
ambiente, use `./tools/build-online-apk.sh https://URL-HTTPS-DO-GATEWAY`; o script só compila após
confirmar o health. Tokens permanecem fora do APK. Antes de enviar, valide os hashes e copie os
mesmos arquivos para Downloads.
