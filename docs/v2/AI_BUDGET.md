# Orçamento e limites da autoria assistida

## Princípio

O custo é controlado por história aprovada, não por chamada isolada. Valores monetários dependem da
conta, região e contrato dos provedores e permanecem `A CONFIRMAR`; o sistema já fecha unidades,
limites e comportamento quando o orçamento termina.

## Envelope padrão por trabalho

| Etapa | Limite inicial | Observação |
|---|---:|---|
| análise de imagem | 1 chamada + 1 retry manual | ambiguidade vai à professora |
| recuperação RAG | 2 consultas | sem modelo quando busca basta |
| plano/roteiro | 1 geração + 1 correção estrutural | saída curta e estruturada |
| imagens | até 4 variantes totais | regeneração apenas da cena escolhida |
| narração preparada | até 12 trechos | cache por hash de texto/voz |
| Codex | 1 trabalho, tempo máximo configurável | somente se houver montagem agentiva |
| revisão assistida | até 3 ações por versão | preservar partes não alteradas |

O limite não reduz segurança. Se a validação falhar e o orçamento acabar, o estado é bloqueado com
mensagem clara; nunca publica saída inválida.

## Orçamentos

```text
por job → por professora/dia → por escola/mês → limite global
```

Cada nível tem alerta e bloqueio. Coordenação pode ampliar dentro da escola; apenas administração
técnica altera teto global. Nenhum limite ou preço aparece para a criança.

## Dados registrados

Por etapa: provedor, modelo/snapshot, região quando disponível, unidades de entrada/saída, imagens,
áudio, duração, cache hit, retry, valor estimado e valor conciliado. Não registrar prompt que
contenha dado pessoal; usar hash/ID da versão sanitizada.

## Estratégias sem degradar pedagogia

- reutilizar recursos idênticos por hash;
- editar somente a cena pedida;
- gerar miniatura/variantes determinísticas localmente;
- manter instruções e falas curtas por contrato;
- usar Codex apenas para tarefa agentiva, não para chamada simples;
- preparar áudio após roteiro validado;
- não aquecer ou manter modelos remotos para fluxo infantil obrigatório.

## Critério para fechar valores

Antes da Sprint 3, preencher uma planilha com preços oficiais vigentes, impostos, câmbio, cota
gratuita, limites e termos do público infantil. Executar dez histórias do conjunto de avaliação e
usar p50/p95 de custo e latência. Definir teto somente depois dessa medição; estimativa sem smoke
test continua identificada como estimativa.
