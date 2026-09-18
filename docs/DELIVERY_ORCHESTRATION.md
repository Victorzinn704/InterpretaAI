# Plano de entrega orquestrada

Atualizado em 17 de setembro de 2026.

## Trilhas de trabalho

| Trilha | Local | Responsabilidade | Regra de integração |
|---|---|---|---|
| Implementação Mac | `InterpretaAI` / `main` | retirada de atribuições, cache Android, Studio e testes relacionados | concluir em commits pequenos antes de integrar |
| Integração | `InterpretaAI-delivery-rehearsal` / `codex/delivery-rehearsal-20260917` | reunir interface docente, fluxo de dados, segurança e CI sobre a última `main` estável | não editar os arquivos ativos da trilha Mac |
| Auditoria | GitHub Actions e execução local | segredo, servidor, Android, lint, artefatos e experiência docente | nenhum release com portão vermelho |

## Sequência de entrega

1. A trilha Mac conclui a retirada de atribuição de ponta a ponta: servidor, Studio, cache do aparelho, migração e testes.
2. A trilha Mac registra o trabalho em commits atômicos e deixa a worktree limpa.
3. A trilha de integração incorpora esses commits sobre esta branch de ensaio.
4. O contrato de dados é atualizado para representar atribuição ativa, retirada, limpeza local e retenção segura dos bytes compartilhados.
5. Todos os portões abaixo são executados na mesma revisão candidata.
6. A branch candidata é publicada e revisada por pull request. O merge ocorre somente com CI verde e diff conhecido.
7. O backend é implantado antes do APK. O APK só é distribuído depois do smoke test autenticado da API.

## Portões da candidata

| Área | Evidência exigida |
|---|---|
| Git | worktree limpa, `git diff --check` e histórico sem commits acidentais |
| Segredos | Gitleaks sem achados no histórico alcançável |
| Servidor | testes e `bootJar` concluídos |
| Android | testes unitários, lint e APK de debug concluídos |
| Aparelho | 43 testes instrumentados no emulador API 35 e ensaio posterior nos tablets alvo |
| Fluxo docente | criação, revisão, publicação, envio, confirmação e retirada testados |
| Responsividade | auditoria Chromium em 390x844, 800x1280 e 1440x1000 |
| Dados | migração, idempotência, isolamento por escola e revogação cobertos por testes |
| Documentação | estados Publicada, Enviada, Pronta e Retirada correspondem às fontes reais |

## Estado verificado desta candidata

- Base estável: `main` em `60df790`, com retirada de atribuição e evidências do Studio.
- Interface Android docente e fluxo web incorporados sem conflito.
- Pipeline de CI, Gitleaks, Dependabot e dependências fixadas incorporados.
- Servidor, `bootJar`, testes unitários Android, lint e APK aprovados localmente.
- 43 testes instrumentados aprovados no emulador API 35.
- Validadores semânticos, docente, RAG e responsivo aprovados.

## Pendências antes do pull request

- Validar retirada e ressincronização nos tablets físicos usados no piloto.
- Repetir testes focados de sincronização, migração, autorização e Studio após essa incorporação.
- Repetir o conjunto completo uma vez na revisão candidata final.
- Manter as fontes do RAG fora do uso pedagógico até aprovação editorial; hoje existem três fontes íntegras e nenhuma aprovada.
- Não implantar nos servidores Oracle a partir de uma worktree suja ou de uma revisão diferente da candidata testada.

## Recuperação

Se a candidata falhar depois do merge, reverta o commit de merge e preserve os logs, relatórios e hashes dos artefatos. Migrações de banco precisam de restauração ensaiada antes de produção; a reversão do APK não deve depender de apagar dados locais da turma.
