# Validação da arquitetura, IA e experiência

## Portões antes da publicação

Uma versão percorre portões independentes. Falhar em um deles não é resolvido pedindo ao modelo
para “tentar obedecer melhor”.

1. **Schema:** estrutura, tipos, limites e enums.
2. **Semântica:** referências, grafo alcançável, solução das atividades e compatibilidade do app.
3. **Mídia:** formato decodificável, dimensões, hash, conteúdo permitido e procedência.
4. **Pedagogia:** objetivo explícito, sequência LEIA, linguagem e apoio progressivo.
5. **Segurança infantil:** sem diagnóstico, culpa, ranking, pedido de segredo ou chamada externa.
6. **Acessibilidade:** narração, alternativa, alvo mínimo, redução de estímulos e ausência de scroll.
7. **Revisão docente:** prévia de celular/tablet e aprovação humana registrada.

## Conjunto de avaliação da autoria

A Sprint 0 cria pelo menos 30 casos sintéticos e licenciados:

- objetos simples e palavras com acento/repetição;
- imagem com vários objetos e imagem ambígua;
- baixa resolução, formato inválido e metadado sensível;
- objetivo incompatível com o componente solicitado;
- tentativa de injeção de prompt em imagem, arquivo e texto;
- pedido fora do domínio educacional;
- linguagem punitiva, diagnóstica ou excessivamente infantilizada;
- ausência de fonte pedagógica apropriada;
- celular pequeno, tablet e modo de estímulos reduzidos.

Cada caso define resultado esperado: `VALID`, `NEEDS_TEACHER_INPUT`, `BLOCKED` ou
`FAILED_RETRYABLE`. A suíte é executada ao trocar prompt, modelo, fonte RAG ou contrato.

## Métricas do RAG

Medir separadamente:

- recuperação da fonte esperada em `top-k`;
- respeito a escola, faixa, validade e autorização;
- afirmações apoiadas por `sourceRef` existente;
- taxa de fonte irrelevante e ausência corretamente declarada;
- latência e custo por etapa.

Busca vetorial só entra se superar a busca textual/metadata no conjunto de avaliação sem romper
isolamento de escopo. Relatórios de crianças nunca compõem esse corpus.

## Métricas da geração

- porcentagem de rascunhos estruturalmente válidos;
- bloqueios por categoria;
- número de regenerações por versão aprovada;
- tempo até primeira prévia e até rascunho completo;
- custo por história aprovada, não apenas por chamada;
- proporção de texto/imagem alterada pela professora.

Essas métricas avaliam a ferramenta de autoria. Não medem aprendizagem infantil.

## UX docente

As cinco tarefas descritas no Estúdio são testadas com professoras. Registrar conclusão, tempo,
retorno indevido, dúvida de estado e necessidade de ajuda. Aceite inicial: quatro de cinco concluem
cada tarefa sem instrução técnica e sem entrar em Administração.

## Jornada infantil

Auditar 360×640, 412×915 e 800×1280. Toda etapa precisa:

- caber sem ação principal dependente de rolagem;
- funcionar offline depois de `READY`;
- aceitar toque e, no puzzle, arraste;
- oferecer instrução oral e apoio progressivo;
- manter a resposta local enquanto rede/IA estão lentas;
- preservar fonte legível, alvo de 48dp e modo de estímulos reduzidos.

A observação piloto procura compreensão do fluxo, participação e pontos de abandono. Não transforma
uma sessão curta em alegação de eficácia, diagnóstico ou eliminação do analfabetismo funcional.

## Resiliência e segurança

- interromper upload, geração, download e sincronização em cada etapa;
- reiniciar API/worker e provar idempotência;
- rejeitar pacote adulterado ou incompatível;
- validar isolamento entre escolas e expiração de URL;
- tentar fazer o executor Codex acessar banco, publicação, relatórios e destinos de rede negados;
- restaurar banco/objetos e reverter pacote/app/backend em staging.

## Evidência de aceite

Cada critério gera artefato identificável: relatório de testes, captura, log sintético correlacionado,
hash do pacote ou ata de revisão. O status final continua separado em `implementado`, `demonstrado`
e `futuro` até existir essa evidência.

