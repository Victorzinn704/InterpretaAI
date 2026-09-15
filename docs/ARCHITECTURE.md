# Arquitetura do MVP

## Método LEIA

**LEIA significa Ler, Entender, Interpretar e Aprender.** Ler apresenta a cena e a fala; Entender
ajuda a reconhecer personagens, objetos, ações e o desafio; Interpretar acolhe hipóteses e caminhos;
Aprender conecta contexto, oralidade, fonema, palavra e atividade em grupo. Escrita e aplicação
continuam presentes como experiências pedagógicas, mas não são tratadas como etapas isoladas do
acrônimo.

## Escolha

Uma aplicação Android nativa, de uma única Activity e um único módulo Gradle. O padrão é MVVM leve com repositório:

```text
Compose screens
      │ ações / estado
      ▼
AppViewModel ─────► serviços Android (voz, câmera, quiosque)
      │
      ▼
MetricsRepository (interface de domínio)
      │
      ▼
SQLite local ─────► SyncWorker/API (próxima etapa)
```

Essa fronteira mantém o MVP pequeno e permite trocar SQLite por Room ou adicionar sincronização sem reescrever as telas. Multi-módulos, Clean Architecture completa, event bus, microserviços e uma plataforma de IA própria não são necessários para provar o piloto.

## Pacotes

- `ui/screens`: jornadas da criança e painel do educador;
- `ui/theme`: sistema visual de gibi;
- `domain`: eventos, métricas e regras pedagógicas puras/testáveis;
- `data`: persistência offline;
- `platform`: voz, políticas Android e inicialização do totem.

## Métricas que fazem sentido no piloto

| Nível | Mostra | Não deve concluir sozinho |
|---|---|---|
| Criança | progresso, conquista e encorajamento | diagnóstico clínico ou nota |
| Professor | participações, hipóteses, tempo de resposta, modalidade e pedidos de ajuda | “melhor/pior aluno” |
| Secretaria | adesão por escola/turma, conclusão, disponibilidade e evolução agregada | ranking público de professor ou criança |

Eventos mínimos futuros: sessão iniciada/concluída, instrução ouvida, observação registrada, resposta enviada,
etapa concluída e ajuda solicitada. No quebra-cabeça, conclusão, movimentos, duração e uso da pista são
evidências de interação, não nota. Não coletar áudio bruto, transcrição integral permanente ou imagem
facial para produzir esses indicadores.

## Próxima integração com servidor

1. Adicionar autenticação do dispositivo e perfis `PROFESSOR`, `GESTOR_ESCOLA`, `SECRETARIA`.
2. Criar fila de eventos pendentes com idempotency key e sincronização por WorkManager.
3. Servidor valida contrato, pseudonimiza aluno e produz agregados diários.
4. Painéis web consomem apenas o escopo do usuário e registram auditoria.

Comece com um monólito modular no servidor (API + banco relacional + tarefas de agregação). Separe serviços somente quando carga, equipe ou fronteira de segurança justificarem.

## Neurodiversidade e acessibilidade

- instrução curta, repetível e acompanhada de ícone;
- alvo de toque grande e contraste alto;
- sem punição por repetição;
- mais de uma modalidade de resposta;
- timer pausável e atividade de desconexão;
- professor interpreta contexto, em vez de o app rotular dificuldade.

Para o piloto, validar com alfabetizadores, educação especial, famílias e crianças; medir compreensão e fricção, não apenas taxa de acerto.

## Conversa por voz do MVP

O [fluxo pedagógico fechado](FLUXO_PEDAGOGICO_FECHADO.md) separa o motor de aprendizagem da
mediação generativa. A navegação nunca depende do modelo: conceitos esperados são resolvidos no
aparelho e os estados do percurso são determinísticos e testáveis.

Para provar imersão sem transformar o MVP em uma plataforma de agentes, implementar apenas uma
conversa curta dentro de uma cena do gibi:

```text
criança toca e fala
        ↓
Android captura áudio/transcrição
        ↓
API do InterpretaAI ─► LangChain4j ─► Ollama/Qwen local
        ↓
resposta pedagógica estruturada ─► Kokoro pt-BR local
        ↓
Android toca a fala e destaca a próxima ação
```

- limitar a conversa a duas ou três trocas por cena;
- enviar contexto fechado: cena, objetivo pedagógico e opções de intervenção permitidas;
- exigir resposta estruturada com fala curta, intenção observada e próximo convite;
- nunca colocar credenciais de provedor dentro do APK;
- não persistir áudio bruto e evitar transcrição integral nas métricas;
- manter falas essenciais pré-geradas no APK para funcionar sem internet;
- se a rede ou o modelo falhar, voltar imediatamente à resposta local já existente.

Não há agente autônomo, RAG ou LangGraph no piloto. A máquina de estados Kotlin e duas interfaces de
provedor (`ConversationProvider` e `SpeechProvider`) mantêm Qwen/Kokoro substituíveis sem aumentar a
complexidade da jornada.

Como alternativa de inferência online, `CONVERSATION_PROVIDER=nvidia` conecta o mesmo contrato ao
NVIDIA NIM pela API OpenAI-compatible do LangChain4j. O catálogo aprovado no código contém Gemma 4
31B IT, Kimi K3, Mistral Nemotron e Nemotron 3 Ultra. Apenas um modelo é selecionado no servidor por
execução: Mistral é o padrão de mediação por ter cumprido o orçamento de latência; Gemma e Kimi ficam
reservados à avaliação visual; e Ultra, à revisão complexa fora do diálogo infantil. Isso evita quatro
chamadas, quatro respostas concorrentes e latência sem ganho pedagógico.

O fluxo infantil usa NDJSON somente para `ACK`, texto final validado e resposta completa; não expõe
tokens nem raciocínio interno. A resposta é limitada, solicitada em JSON ao modelo e novamente
validada pelo servidor. O caminho remoto tem timeout interno de 3,5 segundos dentro do orçamento
total de quatro segundos para mediação e 1,5 segundo para voz; retries estão desativados para o
timeout não se multiplicar e bloquear a
experiência. A presença do adaptador não comprova adequação a dados
de crianças: antes de ativá-lo em piloto real, é obrigatório validar termos, retenção, localização do
processamento, consentimento e desempenho em português infantil. As chaves pertencem somente ao
ambiente do servidor e qualquer chave publicada deve ser revogada.

Com NVIDIA ativo, um agendamento opcional conclui uma chamada sintética mínima com orçamento próprio
de 15 segundos e mantém a rota ativa a cada dois minutos. O turno infantil preserva seu teto de quatro
segundos. Isso reduz cold start sem reaproveitar conteúdo infantil; como o catálogo gratuito não
oferece reserva de GPU, aquecimento é mitigação mensurável, não garantia.

## Visão e câmera

OCR e classificação genérica de objetos são executados no aparelho com modelos ML Kit embarcados.
A foto temporária é apagada após a avaliação e não passa pelo túnel. Reconhecimento de logotipos e
vídeo contínuo não fazem parte do MVP: exigiriam conjunto de referência, consentimento e custo de
processamento sem melhorar a demonstração principal de alfabetização.
