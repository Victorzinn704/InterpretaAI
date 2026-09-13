# Aderência ao Hackathon HACKTUDO 2026

O InterpretaAI foi concebido para o Hackathon HACKTUDO 2026, na edição comemorativa de 10 anos do
festival. A [página oficial do desafio](https://www.hacktudo.com.br/amais-hackathon-2026) é a fonte
do tema e do modelo de avaliação usado nesta documentação. A referência registra a origem da ideia e
não implica premiação ou endosso oficial.

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
| Originalidade e inovação | Coautoria guiada: a criança ajuda a LEIA e os personagens, fala hipóteses e faz o gibi avançar. | O MVP prova uma história e um ciclo LEIA; ainda não é um currículo completo. |
| Solução tecnológica | APK Kotlin/Compose, servidor Java/LangChain4j, Qwen, Kokoro, visão local e fallback offline. | O endereço público usa túnel temporário; autenticação, rate limit e hospedagem Oracle ainda são evolução. |
| Utilidade e aplicabilidade | Voz, alvo grande, uma decisão por tela, puzzle e registros de participação úteis ao professor. | Câmera, microfone, sotaques, ruído e compreensão precisam de piloto com crianças e educadores. |

## Riscos que permanecem

- O conteúdo pedagógico é estreito e precisa de validação por alfabetizadores antes de crescer.
- A visão da secretaria é proposta arquitetural; o MVP mantém os dados no tablet e não simula sincronização.
- A área adulta usa PIN de demonstração e não deve receber dados reais sem identidade institucional.
- A voz masculina está disponível no servidor, mas o roteamento completo por personagem ainda precisa ser
  validado no roteiro; a voz principal efetiva é a da LEIA.
- O uso consciente e as proteções socioemocionais reduzem pressão e distração, mas não constituem cuidado
  clínico, avaliação psicológica ou diagnóstico.
- Privacidade, consentimento, retenção, segurança e acessibilidade exigem avaliação formal antes de piloto real.

## Regra de decisão

Uma evolução só entra se reforçar alfabetização, colaboração, criatividade ou bem-estar digital e se puder
ser demonstrada sem esconder seus limites. Métrica não dá nota à criança; IA não diagnostica; animação não
compete com a tarefa; o smartphone não ocupa todo o tempo pedagógico.
