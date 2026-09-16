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
- `InterpretaAI-Proposta-MVP.pdf`: proposta auditada com exatamente 10 páginas A4;
- `InterpretaAI-oracle-arm64.tar.gz`: pacote separado na pasta fácil de enviar, com JAR, Kokoro,
  systemd, Caddy, instalador idempotente e verificador público; não contém credenciais;
- `SHA256.txt`: integridade do APK principal, das versões atuais e do PDF.

O APK principal desta pasta é compilado sem URL remota e demonstra integralmente o caminho
local/offline. As versões 0.8–0.14 contêm clientes de sincronização, mas só os ativam quando um gateway é
fornecido na compilação online e o adulto configura os tokens do piloto.
Para gerar a variante online sem colocar credenciais no celular, execute
`./tools/build-online-apk.sh https://URL-HTTPS-DO-GATEWAY`; o script só compila após confirmar o
health do servidor. Antes de enviar, valide os hashes e copie os mesmos arquivos para a pasta de
entrega no Desktop.
