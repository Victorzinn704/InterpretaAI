# Arquitetura do MVP

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
| Professor | tentativas, acertos, tempo de resposta, modalidade e pedidos de ajuda | “melhor/pior aluno” |
| Secretaria | adesão por escola/turma, conclusão, disponibilidade e evolução agregada | ranking público de professor ou criança |

Eventos mínimos: sessão iniciada/concluída, instrução ouvida, observação registrada, resposta enviada,
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

## Próximo recorte: conversa por voz

Para provar imersão sem transformar o MVP em uma plataforma de agentes, implementar apenas uma
conversa curta dentro de uma cena do gibi:

```text
criança toca e fala
        ↓
Android captura áudio/transcrição
        ↓
API do InterpretaAI ─► adaptador Gemini (compreensão contextual)
        ↓
resposta pedagógica estruturada ─► adaptador TTS (Google ou NVIDIA)
        ↓
Android toca a fala e destaca a próxima ação
```

- limitar a conversa a duas ou três trocas por cena;
- enviar contexto fechado: cena, objetivo pedagógico e opções de intervenção permitidas;
- exigir resposta estruturada com fala curta, intenção observada e próximo convite;
- nunca colocar chaves permanentes de Gemini, Google Cloud ou NVIDIA dentro do APK;
- não persistir áudio bruto e evitar transcrição integral nas métricas;
- manter falas essenciais pré-geradas no APK para funcionar sem internet;
- se a rede ou o modelo falhar, voltar imediatamente à resposta local já existente.

Isso dispensa LangChain/LangGraph no piloto. Uma máquina de estados Kotlin e duas interfaces de
provedor (`ConversationProvider` e `VoiceProvider`) deixam Gemini, Google TTS ou NVIDIA substituíveis.
