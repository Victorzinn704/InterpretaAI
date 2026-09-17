# Auditoria de segurança, testes e arquitetura — InterpretaAI

Data: 17/09/2026

## Parecer executivo

O InterpretaAI possui uma base técnica acima de um protótipo simples: aplicativo Android, API,
banco, inferência e voz estão separados, há testes relevantes e a versão v1 está saudável na
Oracle. A arquitetura 2.0 está bem desenhada e parcialmente implementada, mas ainda não está
finalizada nem implantada.

O repositório não tinha integração contínua. A branch local
`codex/security-ci-audit-20260917` adiciona os portões necessários, sem alterar a `main` nem o
servidor em produção.

## Sanitização e segredos

### Resultado

- Gitleaks analisou 147 commits e aproximadamente 2,13 MB de conteúdo Git.
- Nenhuma credencial real foi encontrada.
- Não há `.env`, chave privada, keystore, `google-services.json`, `local.properties` ou credencial
  JSON versionada, inclusive no histórico de nomes de arquivos.
- A varredura das alterações locais ainda sem commit também não encontrou segredo.
- `local.properties` está ignorado e contém somente a chave `sdk.dir`.
- Os bancos H2 locais estão ignorados; suas permissões foram reduzidas de `0644` para `0600`.

O scanner encontrou 17 ocorrências históricas e 21 no diretório de trabalho. Após revisão:

- 17 eram três tokens deliberadamente falsos usados nos testes;
- uma era o placeholder `TOKEN_CONFIGURADO_NO_SERVIDOR` da documentação;
- duas eram bytes coincidentes dentro do modelo binário do ML Kit em `app/build`, que não é
  versionado;
- as demais eram repetições dos mesmos valores sintéticos em relatórios de teste gerados.

A `.gitleaks.toml` criada permite somente os quatro valores sintéticos exatos. Ela não libera
pastas, extensões ou padrões amplos.

## Testes executados

Na base commitada `8c8cf08`:

| Verificação | Resultado |
|---|---|
| Testes Spring/Java | passou |
| Testes unitários Android | passou |
| Android Lint | passou |
| APK debug | gerado |
| JAR Spring | gerado |
| Validador semântico | exemplo válido e 6 mutações bloqueadas |
| Avaliador docente | 7 cenários aprovados |
| Integridade das fontes RAG | 3 íntegras, 0 aprovadas |
| Playwright do Estúdio | celular, tablet e desktop aprovados tecnicamente |
| Gitleaks | nenhum vazamento após as exceções revisadas |

### Alterações simultâneas na `main`

Durante a auditoria, sete arquivos do fluxo de revisão/publicação foram alterados no worktree
principal sem commit. Essa versão intermediária falha em três testes de
`StoryVersionControllerTest`: o contrato novo exige `expectedPackSha256` e `confirmedAssetIds`,
enquanto esses testes ainda enviam o contrato antigo. A base commitada passa. As alterações locais
devem ser concluídas junto com a atualização dos testes antes de novo commit.

## Dependências

O arquivo `services/kokoro/requirements.txt` fixava apenas três dependências diretas. Uma resolução
estática podia escolher transitivas antigas com 10 avisos conhecidos em quatro pacotes. A instalação
que está rodando na Oracle já usa versões posteriores:

- Kokoro 0.9.4;
- idna 3.19;
- setuptools 84.0.0;
- torch 2.14.0;
- transformers 5.17.0.

O `pip check` da VM não encontrou dependência quebrada. A branch de auditoria registra essas versões
como limites mínimos e adiciona Python ao Dependabot. Isso reduz deriva; um lock reproduzível por
plataforma ainda deve ser gerado antes do release institucional.

## Pipeline preparado

A branch `codex/security-ci-audit-20260917` contém três commits locais:

- `5647e57` — Gitleaks, CI, Dependabot e documentação;
- `41bf1eb` — auditoria Playwright do Estúdio;
- `9c22d06` — limites seguros das dependências Kokoro.

O workflow executa:

1. Gitleaks;
2. validação do Gradle Wrapper;
3. Java 17 e Android SDK 35;
4. testes do servidor e do Android;
5. Android Lint;
6. geração de JAR e APK debug;
7. validadores semânticos e pedagógicos;
8. fluxo Playwright responsivo do Estúdio;
9. upload de relatórios e capturas.

Dependabot cobre Gradle, Docker, Python e GitHub Actions. Todas as ações do workflow estão fixadas por
SHA completo.

## Estado da arquitetura

### Finalizado e validado na v1

- API HTTPS pública saudável;
- Spring Boot, Kokoro, PostgreSQL, PgBouncer, Ollama e WireGuard ativos;
- Qwen 1.5B com fallback;
- autenticação do fluxo de voz e rate limit configuráveis;
- APK com cache e jornada offline existente;
- backups e monitoramento de infraestrutura.

### Ainda não finalizado na v2

- O endpoint público `/api/v2/identity/me` retorna `404`.
- O PostgreSQL implantado está na migração `10`; a identidade e autoria v2 começam na migração `11`.
- OIDC institucional não está configurado no ambiente publicado.
- O armazenamento privado ainda precisa do adaptador OCI e de restauração testada.
- As três fontes RAG estão íntegras, mas nenhuma foi aprovada para produção.
- O planejador ainda não completa um `LearningStoryPack` publicável de ponta a ponta.
- Visão, geração de imagem e executor Codex isolado continuam pendentes.
- O Estúdio é um protótipo testado, sem integração completa com a API publicada.
- Pareamento, cache, reconexão, falta de espaço e jornada precisam de teste nos tablets reais.
- Revisão pedagógica, acessibilidade, LGPD e consentimento precisam de validação institucional.

### Divergência de branches

- `main` está 30 commits à frente de `origin/main` e possui alterações locais sem commit.
- `codex/cloud-delivery-20260916` possui 9 correções específicas da Oracle que não estão integradas à
  `main`.
- A `main` possui cerca de 20 commits de arquitetura 2.0 ausentes na branch de nuvem.

Enquanto essas linhas não forem reconciliadas, código desenvolvido, código testado e código
implantado continuarão representando versões diferentes do sistema.

## Ordem recomendada

1. Concluir as sete alterações locais e atualizar os três testes de aprovação.
2. Publicar os 30 commits locais em uma branch protegida, sem empurrar diretamente para `main`.
3. Integrar as nove correções da branch de nuvem na linha 2.0 e resolver os conflitos com testes.
4. Revisar e incorporar `codex/security-ci-audit-20260917`; exigir o CI como proteção de branch.
5. Criar staging v2 com OIDC real, PostgreSQL restaurável e objetos privados.
6. Aprovar formalmente as fontes RAG e validar o modelo com professoras.
7. Executar piloto técnico nos tablets antes de usar dados reais de crianças.

## Classificação atual

- **v1 Oracle:** operacional para demonstração e piloto técnico controlado.
- **v2 local:** implementação em andamento.
- **produção institucional:** ainda não liberada.
