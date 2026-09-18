# Fluxo único para histórias e telas de gibi

Este repositório e o `main` publicado no GitHub são a fonte de verdade do InterpretaAI. O MacBook e
o Windows trabalham em branches curtas criadas a partir do mesmo `origin/main`; uma tela só entra na
entrega depois de compilar, passar pelos testes e ser revisada junto com o restante da história.

## O que forma uma história

Cada história deve ter, no mesmo conjunto de mudanças:

1. objetivo pedagógico e começo, investigação e conclusão próprios;
2. imagens em `app/src/main/res/drawable-nodpi` com nomes versionados e origem registrada;
3. navegação dentro do catálogo **Histórias com a LÉIA**, sem atalho técnico na Home infantil;
4. fala, descrição acessível, alternativa por toque e comportamento com estímulos reduzidos;
5. evidência nos três tamanhos auditados: 360×640, 412×915 e 800×1280;
6. teste unitário para regras e teste instrumentado para o percurso visível;
7. registro de conclusão e apoio para as métricas da professora.

Hoje o catálogo reúne `Mistério da bola`, `A água da chuva` e as cenas expressivas. As duas histórias
têm conflitos e conclusões independentes. Os novos quadros do MacBook passam a fazer parte desses
percursos; não existe uma segunda versão escondida fora do catálogo.

## Conteúdo local e conteúdo publicado pelo servidor

Os percursos embarcados no APK servem como demonstração offline e fallback. Histórias preparadas no
Studio usam o contrato `LearningStoryPack`: a professora revisa e aprova uma versão, o servidor a
publica e o tablet guarda essa versão no cache privado. Uma versão publicada nunca é reescrita; uma
correção cria a versão seguinte. Assim, as métricas continuam ligadas ao conteúdo que a criança viu.

## Rotina de sincronização

Antes de começar:

```bash
git fetch --prune origin
git switch main
git pull --ff-only
git switch -c feat/nome-curto
```

Antes de enviar:

```bash
./gradlew test
./gradlew :app:compileDebugAndroidTestKotlin :app:lintDebug
git fetch origin
git rebase origin/main
git push -u origin HEAD
```

O pull request é unido por squash. Depois da união, o trabalho seguinte começa de um `main`
atualizado. Cópias divergentes são preservadas em branch e bundle antes de qualquer realinhamento.

## O que fica fora da união atual

- migrações antigas com números já usados na produção;
- pareamento e Studio anteriores ao fluxo móvel já publicado;
- APKs, hashes e textos de distribuição de versões que não foram recompiladas;
- credenciais, tokens e arquivos locais de ambiente.

Esses itens continuam disponíveis no arquivo histórico do MacBook para consulta, sem competir com a
implementação atual.
