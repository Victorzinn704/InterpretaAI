# Auditoria final do MVP

**Parecer:** o InterpretaAI está adequado para ser apresentado como **MVP funcional de hackathon**.
Ele não está pronto para produção, implantação em rede pública ou tratamento de dados reais de
crianças. A avaliação abaixo usa o código, os testes, o APK, as capturas e os quatro critérios exibidos
pela comissão. As notas são uma estimativa técnica, não uma previsão da banca.

**Resultado posterior à primeira auditoria:** a equipe informou que o projeto foi selecionado entre
227 propostas para o grupo de 10 finalistas. A comunicação oficial ainda deve ser arquivada. O
ambiente Oracle/Keycloak/Estúdio também foi ativado depois da entrega original; isso melhora a
demonstrabilidade, mas não equivale a implantação escolar.

## Avaliação pelos critérios do hackathon

| Critério | Estimativa | Evidência verificável | Ressalva principal |
|---|---:|---|---|
| Adequação ao tema | **8,5/10** | Uso breve e guiado, Modo Foco e encerramento com o celular descansando para a atividade continuar em grupo. | Ainda não há observação de uso em uma sala real. |
| Originalidade e inovação | **8,0/10** | Coautoria guiada: a criança ajuda a LÉIA e os personagens; o método é Ler, Entender, Interpretar e Aprender. | A ideia precisa provar diferenciação e efeito pedagógico em piloto, não apenas na demonstração. |
| Solução tecnológica | **8,0/10** | APK Kotlin/Compose, API Java/LangChain4j, Qwen, Kokoro, visão local, fallback, Oracle/HTTPS e autenticação adulta piloto. | O ambiente não tem SLA institucional, rate limit validado ou ensaio em tablet escolar físico. |
| Utilidade e aplicabilidade | **7,5/10** | Jornada sem rolagem, voz/toque, puzzle, atividade coletiva e métricas neutras com agregado de turma/rede. | A API existe no piloto local; painel, identidade e gestão institucional são futuros. |

**Leitura conjunta: 8,0/10.** É um protótipo coerente, demonstrável e tecnicamente acima de uma
maquete. Perde força se for apresentado como plataforma pronta, IA pedagógica validada ou solução já
implantável. Ganha força se a narrativa mostrar claramente o ciclo criança–LEIA–grupo–professor.

## Auditoria independente

| Área | Situação | Avaliação sincera |
|---|---|---|
| Alfabetização | **Coerente no percurso demonstrável** | Informação explícita, pista visual, inferência, manipulação, `BOLA`/`BO-LA`/`B`/`/b/`, orientação útil e conversa em dupla estão conectados. O conteúdo continua pequeno e não teve eficácia validada por piloto. |
| Experiência infantil | **Boa** | Uma decisão principal por estado, alvos grandes, fala, pistas e redução de estímulos. Voz, ruído, sotaque, acessibilidade e compreensão precisam de teste com crianças e educadores. |
| IA | **Funcional em demonstração** | Mediação é curta, contextual e tem fallback; não dá nota nem diagnóstico. Qwen/Kokoro dependem de serviços privados e o roteiro prova poucas interações, não adaptação curricular ampla. |
| Métricas | **Parcial e responsável** | O app registra e sincroniza participação, modalidade, tempo e ajuda sem rotular acerto; as APIs devolvem apenas agregados. Ainda não há identidade institucional, painel da secretaria ou estudo de validade dos indicadores. |
| Privacidade | **Proteções iniciais** | Visão ocorre no aparelho, foto temporária é apagada e áudio/transcrição não entram nas métricas. Consentimento, retenção formal, autenticação e avaliação de impacto são pré-requisitos de piloto. |
| Modo Foco | **Validado com limite** | Lock Task ficou `LOCKED` e bloqueou Home/Recentes em emulador Device Owner. Em celular comum, fixar a tela exige confirmação; o APK não consegue obter controle total silenciosamente. |
| Engenharia | **Boa para hackathon** | Código separado em UI/domínio/dados/plataforma, servidor pequeno, testes, lint, hashes e runbook. O APK é debug e grande; túnel e serviços locais não representam operação de produção. |
| Documentação e repositório | **Forte** | README visual, galeria, proposta de 10 páginas, contrato, arquitetura, riscos e instruções reproduzíveis. Afirmações de piloto, impacto ou escala devem continuar marcadas como futuras. |

## O que pode ser afirmado na apresentação

- Existe um APK funcional que demonstra uma jornada de alfabetização guiada e sem rolagem obrigatória.
- LEIA significa **Ler, Entender, Interpretar e Aprender**; escrever e aplicar são experiências dentro
  do aprendizado, não etapas artificiais do acrônimo.
- A criança participa como coautora, por voz ou toque, e a atividade termina em colaboração fora da tela.
- Há integração demonstrável com servidor Oracle por HTTPS, IA e duas vozes, com fallback.
- O professor pode consultar o agregado de uma turma e a secretaria o agregado do piloto por APIs
  separadas; a interface institucional ainda é futura.

## O que não deve ser afirmado

- Que a solução melhora alfabetização sem um piloto comparativo ou evidência de aprendizagem.
- Que reconhece corretamente toda fala, objeto, emoção, sotaque ou necessidade neurodivergente.
- Que está pronta para receber dados reais de crianças ou operar em escala institucional.
- Que o bloqueio total funciona em qualquer celular sem provisionamento administrativo.
- Que o ambiente Oracle piloto representa parceria, implantação, SLA ou homologação institucional.

## Recomendação de entrega

**Entregar como versão demonstrável de hackathon**, usando uma demonstração curta: entrar no Modo Escola,
ouvir a cena, responder “bola”, investigar a pista da árvore, abrir o puzzle sem novo menu, relacionar
palavra/som, orientar Davi, concluir em dupla e abrir a visão do professor. Apresentar as limitações
como decisões conscientes do recorte. Antes de um
piloto real, são obrigatórios validação pedagógica e de acessibilidade, consentimento, autenticação,
retenção, infraestrutura estável e teste em sala.
