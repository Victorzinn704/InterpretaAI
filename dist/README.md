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
- `InterpretaAI-Proposta-MVP.pdf`: proposta auditada com exatamente 10 páginas A4;
- `SHA256.txt`: integridade do APK principal, das versões atuais e do PDF.

O APK 0.5 desta pasta foi compilado sem URL remota e demonstra integralmente o caminho local/offline.
Para gerar a variante online sem colocar credenciais no celular, execute
`./tools/build-online-apk.sh https://URL-HTTPS-DO-GATEWAY`; o script só compila após confirmar o
health do servidor. Antes de enviar, valide os hashes e copie os mesmos arquivos para a pasta de
entrega no Desktop.
