# Segurança e integração contínua

## Portões automáticos

O workflow `CI` executa em pull requests, pushes para `main` e sob demanda:

1. Gitleaks no conteúdo versionado e no intervalo Git do evento;
2. validação do Gradle Wrapper;
3. testes JVM do servidor;
4. testes unitários Android;
5. Android Lint;
6. geração do JAR e do APK debug;
7. validadores determinísticos de pacote, usabilidade docente e fontes do RAG.
8. fluxo responsivo do protótipo docente em Chromium, com capturas para celular, tablet e desktop.

Os relatórios de teste e lint ficam disponíveis como artefato por 14 dias, inclusive quando o job
falha. Dependabot verifica semanalmente Gradle, a imagem Java do servidor e as ações do GitHub.

## Execução local

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$PATH"

gitleaks git . --config .gitleaks.toml --redact=100 --log-opts='--all'
./gradlew --no-daemon \
  :server:test :server:bootJar \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python3 tools/test-story-pack-validator.py
python3 tools/test-teacher-usability-evaluator.py
python3 tools/validate-guidance-sources.py
python3 -m pip install -r tools/requirements-audit.txt
python3 -m playwright install chromium
```

As quatro strings permitidas em `.gitleaks.toml` são valores sintéticos exatos, usados por testes e
por um exemplo de documentação. Não permita diretórios inteiros, extensões ou padrões amplos.

## Portões de release ainda manuais

- testes instrumentados em emulador API 35 e nos tablets alvo;
- ensaio público autenticado do streaming, Qwen e Kokoro;
- teste de migração e restauração em PostgreSQL de staging;
- revisão pedagógica, acessibilidade e privacidade antes de dados reais.
