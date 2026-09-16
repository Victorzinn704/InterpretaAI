# Estúdio do professor — UX/UI 2.0

## Princípio

A interface acompanha o trabalho real da professora: **preparar, aplicar, observar e replanejar**.
Configuração técnica fica em administração. IA aparece como ações específicas dentro do fluxo, não
como uma caixa de conversa sem contexto.

O estúdio é web responsivo para computador, tablet e celular. A área adulta do APK conserva apenas
operações rápidas de sala: escolher conteúdo pronto, parear dispositivo, acessibilidade e diagnóstico.

## Navegação

```text
┌──────────────────────────────────────────────────────────────────┐
│ InterpretaAI | Turma 1A ▾                  Ajuda | Perfil         │
├───────────────┬──────────────────────────────────────────────────┤
│ Hoje          │                                                  │
│ Histórias     │             conteúdo da área                    │
│ Turmas        │                                                  │
│ Acompanhamento│                                                  │
│               │                                                  │
│ Administração │                                                  │
└───────────────┴──────────────────────────────────────────────────┘
```

Em telas estreitas, a navegação vira barra inferior com quatro áreas; Administração fica no perfil.

## Área Hoje

Responde em poucos segundos:

- qual turma está selecionada;
- qual história está preparada;
- quantos aparelhos estão `PRONTOS`, `PREPARANDO` ou `SEM_CONEXÃO`;
- qual é a próxima ação da professora;
- se existem eventos pendentes de sincronização.

A ação principal muda pelo estado: `Preparar aula`, `Enviar à turma`, `Iniciar`, `Acompanhar` ou
`Registrar observação`. Não há token, URL ou identificador técnico nesta tela.

## Área Histórias

Abas: `Acervo`, `Meus rascunhos`, `Publicadas` e `Arquivadas`.

Cada cartão mostra título, objetivo, faixa sugerida, duração estimada, versão, origem das imagens e
estado. Filtros organizam; não diagnosticam a criança.

### Assistente de criação

O fluxo mantém uma única decisão principal por etapa:

1. **Objetivo:** selecionar um objetivo aprovado ou descrever a intenção.
2. **Contexto:** enviar imagem, escolher uma do acervo ou informar um tema.
3. **Confirmação:** validar objeto/palavra, público, duração e forma de participação.
4. **Proposta:** acompanhar `roteiro → imagens → atividades → validação`.
5. **Revisão:** experimentar a história como criança e editar cada bloco.
6. **Publicação:** escolher turma/grupo e confirmar a versão exata.

### Tela de acompanhamento da autoria

```text
História: O lanche da LÉIA                         Rascunho v3

✓ Imagem recebida       ✓ Referências encontradas
● Criando cenas         ○ Preparando atividades   ○ Validando

[Prévia disponível da cena 1]

Você pode sair desta tela. Avisaremos quando estiver pronta.
[Cancelar tarefa]                                    [Ver detalhes]
```

Falha parcial oferece `Tentar esta etapa novamente`. O restante do rascunho é preservado.

### Revisão por cena

```text
┌────────────── prévia infantil ──────────────┐  ┌─ propriedades ─────┐
│ imagem / balão / CTA exatamente como no app│  │ Objetivo            │
│                                              │  │ Narração            │
└──────────────────────────────────────────────┘  │ Pergunta             │
                                                  │ Apoio progressivo    │
[◀ Cena anterior]  Cena 2 de 6  [Próxima ▶]       │ Atividade inserida   │
                                                  │ Fontes utilizadas    │
[Ouvir] [Ver em celular] [Ver em tablet]           └─────────────────────┘
```

Ações assistidas são delimitadas: `encurtar`, `simplificar vocabulário`, `criar outra pista`,
`adaptar para dupla`, `trocar imagem` e `refazer esta cena`. Toda ação cria uma revisão recuperável.

Antes da aprovação, a tela lista bloqueios e avisos separadamente. Bloqueio impede publicar; aviso
pede confirmação docente.

## Área Turmas

### Organização

- turma, participantes e grupos;
- aparelhos pareados por código curto/QR;
- modo compartilhado com dois a quatro avatares;
- atividade atribuída e versão;
- último contato e capacidade do aparelho.

### Estado de entrega

| Estado | Significado para a professora | Ação disponível |
|---|---|---|
| Enviada | servidor registrou a atribuição | aguardar ou cancelar |
| Recebida | tablet viu o manifesto | acompanhar preparo |
| Preparando | recursos essenciais estão chegando | usar acervo local enquanto espera |
| Pronta | primeiro bloco e recursos obrigatórios íntegros | iniciar |
| Iniciada | sessão está fixada naquela versão | acompanhar sem trocar a versão |
| Concluída | encerramento registrado | observar/replanejar |
| Sem conexão | último contato expirou | manter atividade local disponível |
| Falha | pacote não pôde ser validado | repetir preparo ou suporte |

O painel nunca chama um aparelho de pronto somente porque a API aceitou a publicação.

## Área Acompanhamento

Filtros: período, turma, história, objetivo e modalidade. A primeira visão mostra tendências da
turma; detalhes individuais exigem intenção explícita da professora.

Cada cartão de evidência informa a origem:

- `Aplicativo`: evento fechado e contextualizado;
- `Professora`: observação registrada e editável;
- `Assistente`: resumo/sugestão aguardando revisão.

O assistente pode responder perguntas fechadas como “quais etapas tiveram mais pedidos de ajuda?”
e propor uma retomada. Não escreve parecer final sem mostrar as evidências que utilizou.

## Administração

Somente perfis autorizados veem:

- escolas, papéis e permissões;
- provedores e limites de IA;
- procedência e retenção de mídias;
- saúde da sincronização;
- auditoria;
- políticas de cache e versões mínimas do app.

Credenciais nunca aparecem novamente depois do cadastro. Pareamento usa código efêmero e produz uma
credencial revogável por dispositivo.

## Estados transversais obrigatórios

Toda tela assíncrona prevê: vazio, carregando, sucesso, parcial, offline, falha recuperável,
permissão insuficiente e conteúdo desatualizado. Rascunhos salvam automaticamente e mostram a hora
da última persistência.

## Pesquisa e validação

Testes de usabilidade da primeira rodada usam cinco tarefas:

1. preparar uma história a partir de uma imagem;
2. corrigir uma palavra e regenerar uma cena;
3. conferir a prévia em celular e tablet;
4. enviar para uma turma e identificar quem está pronto;
5. localizar uma evidência e registrar uma observação.

Critério inicial: quatro de cinco participantes concluem cada tarefa sem instrução técnica e sem
entrar em Administração. Problemas são medidos por abandono, retorno indevido, interpretação de
estado e tempo por tarefa; o objetivo não é apenas “achar bonito”.

