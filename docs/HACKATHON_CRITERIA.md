# Aderência aos critérios do HACKTUDO 2026

Este resumo lê os quatro critérios em conjunto com o
[regulamento oficial preservado](references/HACKTUDO-2026-Regulamento-oficial.pdf) e a
[página oficial do desafio](https://www.hacktudo.com.br/amais-hackathon-2026). A matriz regra a regra,
incluindo pendências humanas, está na [auditoria do regulamento](REGULAMENTO_HACKTUDO_2026.md).

> **Resultado informado pela equipe:** 8º lugar geral entre 227 projetos inscritos, após seleção
> para o grupo de 10 finalistas do HACKTUDO 2026. A colocação reconhece a proposta apresentada; não
> substitui validação pedagógica, piloto escolar ou homologação institucional.

## Tema e desafio assumidos como limites do produto

O InterpretaAI responde ao uso consciente do smartphone na escola: o aparelho entra por um período
curto para orientar uma experiência de alfabetização e depois descansa para a aprendizagem continuar
em dupla ou grupo. Tecnologia, metodologia educacional e bem-estar digital aparecem no fluxo real,
sem apresentar o aplicativo como tratamento de saúde mental.

O método **LEIA — Ler, Entender, Interpretar e Aprender —** organiza essa experiência. Escrever e
aplicar aparecem dentro de Aprender, sem reduzir compreensão e interpretação a uma resposta binária.

## Evidências pelos quatro critérios

| Critério | Evidência no MVP | Limite declarado |
|---|---|---|
| Adequação ao tema | Modo Foco, jornada curta sem rolagem e etapa final “o celular descansa na mesa”. | Em aparelho comum, a fixação de tela exige confirmação adulta; bloqueio integral requer tablet gerenciado. |
| Originalidade e inovação | Coautoria guiada: a criança ajuda a LÉIA e os personagens, fala hipóteses e faz o gibi avançar. | O MVP prova uma história e um ciclo LEIA; ainda não é um currículo completo. |
| Solução tecnológica | APK Kotlin/Compose, servidor Java/LangChain4j, Qwen 2.5 1.5B, Kokoro, visão local, fallback e canal versionado professor→sala/tablets; Oracle, HTTPS, Keycloak e Estúdio verificados. | O ambiente é piloto próprio, sem SLA, parceria institucional, federação com SME ou validação em tablet escolar físico. |
| Utilidade e aplicabilidade | Voz, alvo grande, uma decisão por tela, puzzle e registros de participação úteis ao professor. | Câmera, microfone, sotaques, ruído e compreensão precisam de piloto com crianças e educadores. |

## Riscos que permanecem

- O conteúdo pedagógico é estreito e precisa de validação por alfabetizadores antes de crescer.
- O piloto já sincroniza eventos fechados e expõe um agregado de rede sem alias; painel da
  secretaria, identidade institucional, escopo por escola e validação dos indicadores permanecem
  evolução.
- A área adulta usa PIN de demonstração e não deve receber dados reais sem identidade institucional.
- A voz masculina está disponível no servidor, mas o roteamento completo por personagem ainda precisa ser
  validado no roteiro; a voz principal efetiva é a da LÉIA.
- O uso consciente e as proteções socioemocionais reduzem pressão e distração, mas não constituem cuidado
  clínico, avaliação psicológica ou diagnóstico.
- Privacidade, consentimento, retenção, segurança e acessibilidade exigem avaliação formal antes de piloto real.
- A proveniência das ilustrações e efeitos sonoros precisa de confirmação assinada da equipe.
- A concepção posterior ao anúncio é compatível com o primeiro commit, mas exige declaração humana.
- O nome InterpretaAI exige busca formal por grafia, radical e fonética no INPI antes de mercado.

## Fontes, créditos e originalidade

- [Dependências, versões, licenças e downloads](../THIRD_PARTY_NOTICES.md)
- [Autoria, IA e prevenção de plágio](AUTORIA_ORIGINALIDADE_E_IA.md)
- [Inventário e proveniência de ativos](ASSET_PROVENANCE.md)
- [Cópia e hash do regulamento consultado](references/README.md)

## Regra de decisão

Uma evolução só entra se reforçar alfabetização, colaboração, criatividade ou bem-estar digital e se puder
ser demonstrada sem esconder seus limites. Métrica não dá nota à criança; IA não diagnostica; animação não
compete com a tarefa; o smartphone não ocupa todo o tempo pedagógico.
