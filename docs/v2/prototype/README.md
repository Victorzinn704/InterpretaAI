# Protótipo navegável — Estúdio do Professor

Protótipo estático da Sprint 0. Ele valida arquitetura de informação e tarefas; não chama backend,
IA, armazenamento ou dados reais.

## Executar

Na raiz do projeto:

```bash
python3 -m http.server 8099 --directory docs/v2/prototype
```

Abra `http://127.0.0.1:8099`.

Veja também a [auditoria técnica e visual](AUDIT.md), com o percurso executado e oito capturas.

Com o servidor ativo e Playwright/Chromium disponíveis, a auditoria é reproduzível com:

```bash
uv run --with playwright python3 tools/audit-v2-studio.py
```

## Percursos cobertos

1. `Histórias → Criar história`: objetivo, imagem de demonstração, palavra e proposta.
2. Revisão: alterar palavra, regenerar somente uma imagem e alternar celular/tablet.
3. Publicação: selecionar turma e iniciar preparação automática.
4. `Turmas`: distinguir publicada, preparando, pronta e sem conexão.
5. `Acompanhamento`: revisar evidência e salvar observação docente.

## O que o protótipo não prova

- usabilidade com professoras reais;
- integração com Codex, RAG ou provedor de imagem;
- persistência, autorização ou sincronização;
- fidelidade final do design system;
- acessibilidade completa, embora use HTML semântico e navegação responsiva.
