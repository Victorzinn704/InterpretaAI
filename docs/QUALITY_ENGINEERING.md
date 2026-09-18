# Qualidade contínua do InterpretaAI

## O que bloqueia uma entrega

A CI executa testes do servidor e do APK, JaCoCo, Android Lint, verificação de arquitetura,
varredura de segredos e builds debug e release `arm64-v8a`. O release usa R8 e remoção de
recursos. O Gradle também verifica o SHA-256 das dependências em Windows, Linux e macOS.

Quando o repositório tiver `SONAR_HOST_URL` como variável e `SONAR_TOKEN` como secret, a mesma
CI envia a análise ao SonarQube e espera o resultado do quality gate antes de concluir.

O gate `InterpretaAI Safe Evolution` exige:

- zero problema novo;
- nota A em confiabilidade, segurança e manutenção do código novo;
- pelo menos 80% de cobertura no código novo;
- no máximo 3% de duplicação no código novo e no projeto;
- 100% dos hotspots novos revisados;
- zero bloqueador no projeto;
- cobertura total nunca abaixo de 40%.

## Execução local

```bash
./gradlew :server:jacocoTestReport :app:jacocoDebugUnitTestReport :app:lintDebug
SONAR_HOST_URL=http://127.0.0.1:9000 SONAR_TOKEN=seu_token ./gradlew sonar -Dsonar.qualitygate.wait=true
```

Relatórios locais:

- `server/build/reports/jacoco/test/html/index.html`
- `app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html`
- `app/build/reports/lint-results-debug.html`

Nunca grave tokens no repositório. A instância local de avaliação usa H2 e escuta somente em
`127.0.0.1`; uma instância compartilhada deve usar PostgreSQL, HTTPS, backup e autenticação.

## Linha de base de 18 de setembro de 2026

| Medida | Resultado |
|---|---:|
| Testes JVM | 250 aprovados |
| Cobertura total SonarQube | 65,2% |
| Cobertura de linhas | 71,2% |
| Cobertura de ramos | 51,7% |
| Duplicação | 0,9% |
| Bloqueadores | 0 |
| Bugs abertos após correções prioritárias | 0 |
| Vulnerabilidades abertas após revisão | 0 |
| APK release compacto, sem assinatura | 31,09 MiB |

Os alertas de CSRF das APIs stateless, a chave Android sem autenticação interativa para operação
Device Owner, o cookie de token CSRF do Studio e o cleartext exclusivo do build debug foram
revisados no SonarQube com justificativas. O APK release desativa cleartext e backup.

A cobertura Sonar mede regras, clientes, persistência e serviços. Telas Compose, `Activity`,
`Application`, receivers e o adaptador Android de voz ficam fora do denominador unitário porque
dependem do framework/dispositivo; eles continuam sujeitos a Android Lint, compilação R8 e às
auditorias de interface e instrumentação.

## Hotspots comportamentais

O cruzamento de frequência de mudança no Git com complexidade do SonarQube indica esta ordem de
refatoração, sempre em mudanças pequenas e protegidas por testes:

1. `EducatorScreen.kt`: 20 commits, churn 829, complexidade 199, sem cobertura unitária;
2. `PuzzleScreen.kt`: churn 778, complexidade 214, sem cobertura unitária;
3. `LearningStoryPackValidator.java`: churn 807, complexidade 224, cobertura 79,1%;
4. `ComicsScreen.kt`: 13 commits, churn 786, complexidade 172, sem cobertura unitária;
5. `AppViewModel.kt`: 27 commits, churn 967, complexidade 68, sem cobertura unitária.

Antes de dividir telas Compose, extraia estado e regras para classes Kotlin puras e cubra essas
regras. Isso reduz complexidade sem alterar o fluxo visual do APK.

## CodeScene

A ativação automática não foi feita porque a CLI exige conta e token com papel técnico. O plano
gratuito para open source exige licença aberta, enquanto `LICENSE.md` mantém o código original com
todos os direitos reservados. Alterar a licença ou iniciar um trial sem decisão dos autores criaria
um compromisso externo indevido. Quando houver uma conta compatível, importe o repositório e use
os cinco hotspots acima como primeira revisão; a CI e o SonarQube continuam funcionando sem essa
dependência.
