# Implantação mínima na Oracle Cloud

Este pacote prepara uma VM ARM64 do piloto sem alterar o contrato Android. Ele não executa o deploy
sozinho e não contém chaves. Recursos Always Free só podem ser criados na região principal da
conta; `sa-saopaulo-1` reduz distância para o Rio apenas se ela já for essa região.

```text
tablet ── HTTPS/HTTP2 ── Caddy :443 ── Spring :8088
                                         ├── Ollama/Qwen 1.5B :11434
                                         └── Kokoro pt-BR :8091
```

Somente `22`, `80` e `443` entram pela VCN/firewall. As três portas da aplicação escutam em
`127.0.0.1`. O Spring responde ao NDJSON sem `Content-Length`; o Caddy reconhece esse streaming e
faz flush imediato por padrão, preservando o cancelamento quando o tablet abandona a chamada.
Com `VOICE_AUTH_ENABLED=true`, a conversa exige o token operacional configurado pelo adulto no
tablet. Ele segue no cabeçalho, nunca no corpo, log ou tela infantil; sem ele, o Android continua
pelo mediador local. Isso protege a demonstração, mas não substitui credenciais individuais
revogáveis no ambiente institucional.

## Preparação da VM

1. Crie uma Ubuntu ARM64 com até 2 OCPUs e 12 GB, dentro do limite Always Free atualmente publicado,
   se houver capacidade na região da conta. A disponibilidade não é garantida e a Oracle pode
   recolher instâncias consideradas ociosas.
2. Aponte um domínio para o IP público. Caddy precisa das portas 80/443 para emitir TLS.
3. Instale Java 17, Caddy, Python 3.12, `libsndfile1` e Ollama pelos canais oficiais.
4. Crie o usuário de serviço e os diretórios:

```bash
sudo useradd --system --home /var/lib/interpretaai --shell /usr/sbin/nologin interpretaai
sudo install -d -o interpretaai -g interpretaai /opt/interpretaai/kokoro /var/lib/interpretaai/data /var/lib/interpretaai/huggingface /etc/interpretaai
```

5. Gere `server/build/libs/server-0.1.0.jar` com `./gradlew :server:bootJar`; copie-o como
   `/opt/interpretaai/server.jar`. Copie `services/kokoro/app.py` e `requirements.txt`, crie a venv e
   instale as dependências como o usuário `interpretaai`.
6. Copie `server.env.example` para `/etc/interpretaai/server.env`, gere três segredos novos, aplique
   proprietário `root:interpretaai` e modo `640`. Não envie chaves por chat, commit ou imagem.
   No piloto público, ative `PILOT_SYNC_ENABLED=true`, mantenha `VOICE_AUTH_ENABLED=true` e use
   valores diferentes para os tokens docente/tablet e para o segredo de idempotência.
7. Instale os dois units em `/etc/systemd/system/` e o `Caddyfile` em `/etc/caddy/Caddyfile`. Copie
   `caddy.service.d/interpretaai.conf` para `/etc/systemd/system/caddy.service.d/`, copie
   `caddy.env.example` para `/etc/caddy/.env` e troque o domínio. Esse drop-in é a forma documentada
   pelo Caddy de fornecer variáveis ao serviço. Copie também
   `ollama.service.d/interpretaai.conf` para `/etc/systemd/system/ollama.service.d/`: o piloto mantém
   um único modelo carregado e evita uma nova carga entre falas.
8. Baixe e aqueça o modelo antes da aula:

```bash
sudo -u interpretaai ollama pull qwen2.5:1.5b
sudo systemctl daemon-reload
sudo systemctl enable --now ollama interpretaai-kokoro interpretaai-server caddy
```

## Validação antes de gerar o APK online

```bash
caddy validate --config /etc/caddy/Caddyfile
systemd-analyze verify /etc/systemd/system/interpretaai-*.service
curl --fail --silent --show-error https://SEU_DOMINIO/actuator/health
curl --fail --silent --show-error https://SEU_DOMINIO/api/v1/gateway/status
./tools/benchmark-ai-latency.sh 30
./tools/build-online-apk.sh https://SEU_DOMINIO
```

Faça o benchmark com texto sintético. Para aprovar a rota: `FINAL_TEXT` p95 abaixo de 3 s, fallback
abaixo de 6 s no Android e nenhuma resposta fora do contrato. Reinicie a VM e repita o health check,
um turno, Home/Recentes no Modo Foco e o fallback com a rede desligada.

Em 15/09/2026, o JAR foi iniciado localmente com `SERVER_ADDRESS=127.0.0.1`; health e status
responderam, e um turno NDJSON com Ollama/Kokoro deliberadamente indisponíveis entregou
`ACK → FINAL_TEXT → COMPLETE`, `degraded=true`, em 192 ms. Isso comprova inicialização e fallback,
não a velocidade da VM, do Qwen ou da internet.

## Limites deste pacote

- Qwen 1.5B é o padrão por previsibilidade na cota de 2 OCPUs/12 GB. O 3B pode melhorar formulações,
  mas só deve substituí-lo se 30 amostras aquecidas cumprirem o orçamento; a resposta essencial
  continua vindo dos `ScenePacks` aprovados.
- O acesso público já pode exigir um token compartilhado e limitar oito chamadas por sessão/minuto,
  mas ainda precisa de credencial individual revogável, contenção por IP/rede e observabilidade antes
  de receber dados reais. Não use nome, matrícula, foto de rosto ou voz identificável neste estágio.
- Gemini 3.8 e NVIDIA ficam desligados por padrão. A chave permite benchmark sintético; não altera
  termos de uso, privacidade ou a necessidade de consentimento.
- RAG e LangGraph4j não entram no turno infantil. Se usados depois, preparam um pacote revisado pelo
  professor fora do caminho quente.
- O Ollama recebe uma sonda sintética ao iniciar e a cada janela de aquecimento. O drop-in usa
  `OLLAMA_KEEP_ALIVE=-1`, opção oficialmente suportada, para eliminar a recarga do modelo; isso
  reserva RAM continuamente e deve ser revisto quando a VM hospedar outros modelos.

Referências operacionais: [Oracle Always Free](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm),
[Caddy reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) e
[Ollama no Linux](https://docs.ollama.com/linux) / [preload e `keep_alive`](https://docs.ollama.com/faq#how-do-i-keep-a-model-loaded-in-memory-or-make-it-unload-immediately).
