# Catálogo de componentes infantis e compatibilidade

## Objetivo

Definir exatamente o que um `LearningStoryPack` pode solicitar. A IA combina componentes; ela não
cria uma nova mecânica ou código Kotlin durante a autoria.

## Componentes do primeiro contrato

| Tipo | Função pedagógica | Entrada obrigatória | Interação local | Evidência fechada |
|---|---|---|---|---|
| `COMIC` | contexto, pista e compreensão | imagem, alternativa e diálogo narrado | ouvir, repetir e avançar | nó apresentado/concluído |
| `PUZZLE` | consolidar objeto significativo da história | imagem, grade, instrução e solução | toque/troca e arraste | apoio e conclusão |
| `WORD_BUILDER` | relacionar imagem, fonema, letra e palavra | imagem, palavra, peças e fala | selecionar letras | apoio e conclusão |
| `GROUP_HANDOFF` | levar a compreensão para dupla/grupo | instrução oral curta | pousar aparelho e conversar | passagem registrada |
| `END` | fechar o ciclo LEIA | síntese oral contextual | concluir | sessão concluída |

`COMIC`, `PUZZLE` e `WORD_BUILDER` entram no primeiro motor. `GROUP_HANDOFF` e `END` controlam o
fluxo, não são mini-jogos.

No MVP, `WORD_BUILDER` aceita palavras de até oito letras e até oito peças de uma letra. Esse
limite mantém todas as escolhas visíveis no celular de 360×640 sem rolagem; palavras mais longas
precisam de outro componente e nova auditoria, não de peças escondidas abaixo da tela.

## Matriz de compatibilidade

| Recurso | Telefone 360×640 | Telefone 412×915 | Tablet 800×1280 | Offline | Estímulos reduzidos |
|---|---:|---:|---:|---:|---:|
| Quadrinho narrado | obrigatório | obrigatório | obrigatório | sim | sem partículas |
| Puzzle 2×2 | obrigatório | obrigatório | obrigatório | sim | animação mínima |
| Puzzle 3×2 | opcional por capacidade | obrigatório | obrigatório | sim | animação mínima |
| Arrastar peça | obrigatório | obrigatório | obrigatório | sim | sim |
| Tocar para trocar | obrigatório | obrigatório | obrigatório | sim | sim |
| Formação de palavra | obrigatório | obrigatório | obrigatório | sim | sim |
| Áudio preparado | obrigatório | obrigatório | obrigatório | sim | preservado |
| TTS local de fallback | obrigatório | obrigatório | obrigatório | sim | preservado |
| Modo em dupla/grupo | obrigatório | obrigatório | obrigatório | sim | sim |

## Capacidades declaradas pelo aparelho

O manifesto do dispositivo informa:

```text
appVersion, schemaVersions, componentVersions,
viewportClass, densityBucket, availableBytes,
supportsDrag, supportsLocalTts, reducedStimuliEnabled
```

O servidor só atribui uma versão compatível. O Android ainda valida o pacote; não confia na decisão
do servidor como única barreira.

## Regras de composição

- uma decisão principal por viewport;
- nenhuma ação essencial depende de swipe ou texto lido;
- alvo interativo mínimo de 48dp;
- fala de instrução curta e repetível;
- apoio em ordem: repetição oral → pista visual → escolha revelada;
- tentativa diferente não produz vermelho punitivo, buzina ou rótulo de erro;
- puzzle e palavra só usam elementos introduzidos pela história;
- `GROUP_HANDOFF` aparece em jornadas coletivas ou quando a professora o escolhe;
- todo caminho alcançável termina em `END`.

## Evolução

Labirinto numérico, ligar pontos, desenho guiado e reconhecimento de desenho ficam fora do schema
1.0. Para entrar, cada mecânica precisa de propósito, componente Kotlin, auditoria nos três
viewports, modo offline, estímulos reduzidos, eventos fechados e versão mínima do app. A IA só poderá
usá-la depois dessa publicação.
