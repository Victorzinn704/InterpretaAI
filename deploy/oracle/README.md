# Implantação mínima na Oracle Cloud

> **Ambiente ativo em 17/09/2026:** a VM acessada por `joao-oracle` usa Nginx em container,
> com Spring em `172.18.0.1:8088`. Este pacote Caddy é alternativo e não deve ser aplicado
> sobre a VM ativa. `/api/v2/identity/me` retorna 404 também na origem interna; consulte
> [a verificação atual](../../docs/v2/ORACLE_PUBLIC_CHECK.md) antes de preparar um deploy.

> O Estúdio docente 2.0 também não está publicado: `/studio/` retorna 404. O código local mantém
> `STUDIO_ENABLED=false` até haver OIDC institucional, PostgreSQL e roteamento HTTPS testados. Veja
> [Estúdio — revisão editorial](../../docs/v2/STUDIO_REVIEW.md).

> No JAR v2, `OIDC_ENABLED=false` nega explicitamente toda API adulta `/api/v2/**` com 403; ela não
> herda o acesso aberto da v1. A cadeia `/api/v2/devices/**` continua isolada e exige credencial
> própria. Isso é contenção, não autorização para publicar as rotas antes do portão de staging.

> Para a VM Nginx **já ativa**, use o [portão de staging v2](../../docs/v2/ORACLE_STAGING_GATE.md)
> em vez do instalador Caddy desta pasta. A auditoria identifica o JAR atual, o banco pela
> WireGuard e as dependências de OIDC/backup ainda não satisfeitas.

Este pacote prepara uma VM ARM64 do piloto sem alterar o contrato Android. Ele não executa o deploy
sozinho e não contém chaves. Recursos Always Free só podem ser criados na região principal da
conta; `sa-saopaulo-1` reduz distância para o Rio apenas se ela já for essa região.

## Se o servidor Oracle já está conectado

Não reinstale a VM para iniciar a autoria 2.0. O responsável informou em 17/09/2026 que o servidor
já existe em `https://interpretaai.deskimperial.online`; a
[verificação pública](../../docs/v2/ORACLE_PUBLIC_CHECK.md) confirmou health `UP` e gateway v1
`HOT`, enquanto `/api/v2/identity/me` respondeu 404. Faça primeiro verificações somente de leitura.
O `Caddyfile` deste pacote não representa a configuração do Nginx ativo, e a origem Spring também
não atende a rota v2 neste momento.

`Caddyfile.v2.example` é uma **alternativa opt-in para uma instalação Caddy nova**, não para a
VM Nginx ativa. Só a aplique nessa instalação alternativa depois de configurar
`OIDC_ENABLED=true`, emissor/audience, vínculos institucionais no banco e
comprovar que `GET /api/v2/identity/me` rejeita anônimo e aceita uma professora autenticada.
`AUTHORING_WORKER_ENABLED` e `AUTHORING_PLAN_WORKER_ENABLED` continuam desligados; o segundo ainda
exige `AUTHORING_MODEL` explícito e fontes pedagógicas aprovadas. Não copie tokens para o Git, APK,
URL ou linha de comando.

```bash
# Sem token, confere health e rejeição de anônimo; não comprova login funcional.
./verify-v2-public.sh https://SEU_DOMINIO

# Opcional: se INTERPRETAAI_ADULT_TOKEN já foi injetado no ambiente por meio seguro,
# comprova GET /identity/me sem imprimir token ou identidade.
./verify-v2-public.sh https://SEU_DOMINIO
```

Antes de trocar Caddy, confira a configuração efetivamente instalada na VM e preserve um backup
recuperável. O ensaio acima é somente leitura; não migra banco, publica rota nem altera serviço.

Para ensaiar sem trocar a v1, os arquivos `v2-staging.env.example` e
`interpretaai-server-v2-staging.service.example` isolam JAR, porta, diretório gravável e banco.
Eles são modelos, não são instalados por `install.sh` e não devem ser habilitados apontando para o
banco ativo. Após configurar OIDC e um banco restaurável de staging, valide primeiro pela própria
VM com `./verify-v2-origin.sh http://127.0.0.1:8188 COMMIT_COMPLETO`; só depois desenhe a rota no
Nginx existente. O verificador também compara `/actuator/info` com o commit esperado.
Antes de enviar o bundle, `./tools/test-v2-staging-smoke.sh` reproduz localmente o mesmo arranque
com PostgreSQL 17 e OIDC sintético, sem credencial ou rede externa. Ele gera uma chave efêmera,
valida um JWT de professora contra issuer/audience e comprova que `/identity/me` devolve apenas o
vínculo ativo, excluindo o vínculo revogado. Isso valida o adaptador local, não o provedor real.

Gere o artefato transferível com `./tools/package-oracle-deploy.sh`. O resultado padrão fica em
`build/interpretaai-oracle-arm64.tar.gz` e contém o JAR, serviço Kokoro, units, Caddy, exemplos de
ambiente, instalador, verificadores sequencial/concorrente e `MANIFEST.sha256`; nenhuma `.venv`, base local ou
credencial é incluída.
Em 16/09/2026, o pacote local de 96 MiB foi gerado, as 14 entradas do manifesto foram recalculadas
com sucesso e o verificador passou contra Spring + Qwen + Kokoro reais: em três amostras locais,
`ACK` p95 foi 33 ms, texto validado p95 1.153 ms e conclusão p95 2.115 ms, sem degradação. Um ensaio
separado confirmou `401` sem token e turno autenticado com token. Isso comprova bundle e ferramentas
em loopback, não instalação ou disponibilidade na Oracle.

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
4. Extraia o pacote, confira o domínio já apontado para o IP e execute o instalador:

```bash
tar -xzf interpretaai-oracle-arm64.tar.gz
cd interpretaai-oracle
sudo ./install.sh api.seudominio.com
```

   O instalador verifica o manifesto, cria usuário e diretórios, preserva ambientes existentes, gera
   quatro segredos diferentes somente na primeira instalação, instala os units e valida o gateway em
   loopback. Se um JAR novo não ficar saudável, restaura automaticamente o JAR anterior. Ele não
   altera regras da VCN nem o firewall do sistema.
5. Baixe e aqueça o modelo antes da aula:

```bash
sudo -u interpretaai ollama pull qwen2.5:1.5b
sudo -u interpretaai ollama run qwen2.5:1.5b 'Responda somente: pronto'
```

### Envio assistido a partir do repositório

`tools/deploy-oracle.sh` reduz erro operacional, mas não descobre nem cria a VM. Sem `--apply`, ele
faz somente preflight: confirma que domínio e host resolvem para o mesmo endereço, SSH não
interativo, ARM64, `sudo` e todas as dependências. A
transferência e a execução do instalador só acontecem quando `--apply` é informado explicitamente:

```bash
./tools/deploy-oracle.sh --host ubuntu@IP_DA_VM --domain api.seudominio.com \
  --identity /caminho/para/chave.pem

# Somente após o preflight ser aprovado:
./tools/deploy-oracle.sh --host ubuntu@IP_DA_VM --domain api.seudominio.com \
  --identity /caminho/para/chave.pem --apply
```

Chaves, tokens e conteúdo de `server.env` não são transferidos por esse comando. O diretório remoto
temporário tem nome explícito e é preservado após a instalação para auditoria; sua remoção é uma
decisão manual do administrador.

## Validação antes de gerar o APK online

```bash
caddy validate --config /etc/caddy/Caddyfile
systemd-analyze verify /etc/systemd/system/interpretaai-*.service
curl --fail --silent --show-error https://SEU_DOMINIO/actuator/health
curl --fail --silent --show-error https://SEU_DOMINIO/api/v1/gateway/status
# Injete INTERPRETAAI_DEVICE_TOKEN em um ambiente restrito antes de executar.
# Nunca imprima o valor, copie-o para o histórico ou passe-o como argumento de processo.
VERIFY_ITERATIONS=30 \
  ./verify-public.sh https://SEU_DOMINIO
LOAD_CONCURRENCY=2 LOAD_TURNS=12 \
  ./verify-classroom-load.sh https://SEU_DOMINIO
./tools/build-online-apk.sh https://SEU_DOMINIO
```

O verificador usa texto sintético, confirma autenticação negativa, ordem do envelope, `no-store` e
mede p50/p95 sem imprimir resposta ou token. Para aprovar a rota: `ACK` p95 abaixo de 300 ms,
`FINAL_TEXT` p95 abaixo de 3 s e nenhuma degradação. Reinicie a VM e repita o health check, um turno,
Home/Recentes no Modo Foco e o fallback com a rede desligada.

O segundo verificador abre turnos sintéticos em paralelo e mede throughput, `ACK`, texto validado,
conclusão, falhas e degradações. Ele agenda o aquecimento e só inicia a amostra quando o gateway
declara `HOT`; cold start deve ser medido separadamente. Comece com concorrência 2, que corresponde ao bulkhead padrão. Em
seguida, repita com 4 para descobrir o limite real da VM; o teste reprova por padrão qualquer
fallback, em vez de esconder saturação. Isso não significa que quatro crianças devem esperar pela
nuvem: quando a capacidade acaba, a experiência instalada continua pela fala local preparada.

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
- Gemini 3.8 Flash e NVIDIA ficam desligados por padrão. A chave permite benchmark sintético; não altera
  termos de uso, privacidade ou a necessidade de consentimento.
- RAG e LangGraph4j não entram no turno infantil. Se usados depois, preparam um pacote revisado pelo
  professor fora do caminho quente.
- O Ollama recebe uma sonda sintética ao iniciar e a cada janela de aquecimento. O drop-in usa
  `OLLAMA_KEEP_ALIVE=-1`, opção oficialmente suportada, para eliminar a recarga do modelo; isso
  reserva RAM continuamente e deve ser revisto quando a VM hospedar outros modelos.

Referências operacionais: [Oracle Always Free](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm),
[Caddy reverse_proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy) e
[Ollama no Linux](https://docs.ollama.com/linux) / [preload e `keep_alive`](https://docs.ollama.com/faq#how-do-i-keep-a-model-loaded-in-memory-or-make-it-unload-immediately).
