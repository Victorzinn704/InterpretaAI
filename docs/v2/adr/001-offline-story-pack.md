# ADR 001 — Pacotes imutáveis com fonte local no Android

- **Estado:** parcialmente implementado localmente; sincronização e renderer variáveis pendentes
- **Data:** 2026-09-16

## Contexto

O InterpretaAI precisa receber histórias novas pela internet, operar em redes instáveis e preservar
uma experiência infantil imediata. Uma história inclui JSON, imagens e áudio; atualizar uma parte no
meio da sessão pode quebrar pistas, palavras ou continuidade narrativa.

## Decisão

Publicar cada história como `LearningStoryPack` imutável. O Android mantém catálogo e estados no
Room, recursos no armazenamento do app e lê somente a fonte local. O servidor entrega manifesto e
recursos por hash. Uma nova edição gera nova versão.

O app pode iniciar quando o bloco inicial e todos os seus recursos obrigatórios estiverem íntegros;
continua preparando o restante. Ao iniciar, fixa a versão até concluir ou abandonar explicitamente.

O primeiro incremento Android já contém o parser local, catálogo Room e armazenamento privado por
hash. A integração de manifestos, download e renderização dos componentes permanece pendente; esse
estado parcial é detalhado no `SPRINT_2_TRACKER.md`.

## Consequências

- jornada não bloqueia em chamadas de rede;
- rollback seleciona versão anterior sem reescrever arquivos;
- cache precisa de política, hash, troca atômica e pin de conteúdo ativo;
- o painel precisa distinguir publicação, recebimento e preparo;
- recursos iguais podem ser deduplicados;
- versões antigas ocupam espaço até deixarem de ser necessárias.

## Alternativas descartadas

- renderizar diretamente da API: falha com rede instável e mistura UI com transporte;
- baixar somente ao tocar em começar: adiciona espera no momento mais sensível;
- substituir arquivos sob o mesmo identificador: impede reprodutibilidade e auditoria.
