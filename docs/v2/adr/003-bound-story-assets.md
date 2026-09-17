# ADR 003 — recursos imutavelmente vinculados à versão da história

**Estado:** aceito para a próxima implementação do motor de histórias.

## Contexto

`LearningStoryPack` declara cada recurso por `assetId`, papel (`PHONE`, `TABLET` ou `AUDIO`), tipo,
tamanho e SHA-256. O upload adulto já grava o original em área privada e o worker o sanitiza antes
de qualquer uso infantil. Porém, o campo `path` do pacote é apenas descritivo: ele não pode ser uma
URL, uma chave de objeto nem uma autorização para o Android buscar mídia diretamente.

Sem uma ligação persistida entre a versão publicada e os bytes sanitizados, o tablet pode guardar o
JSON e continuar incapaz de preparar a cena inicial. Essa falha tem de aparecer como estado técnico
de preparo, nunca como uma atividade parcialmente executável.

## Decisão

Criar `story_version_asset`, imutável e por variante, com chave:

```text
(story_id, story_version, asset_id, role)
```

Cada linha contém somente a referência privada já aprovada:

- `media_id` de uma mídia sanitizada ou de uma biblioteca aprovada;
- `object_key` privado copiado para a versão;
- `media_type`, `bytes` e `sha256` copiados para a versão;
- data de vinculação e a origem revisada pela professora.

No ingresso interno de uma versão, o worker entrega um mapa explícito `assetId + role → mediaId`.
O backend exige que ele cubra **todas** as variantes declaradas e compara tipo, tamanho e SHA-256 com
os bytes sanitizados. Um pacote com mídia sem vínculo, mídia não `READY` ou metadados divergentes é
bloqueado antes de criar o rascunho. Não existe rota adulta que aceite esse mapa junto com JSON livre.

A publicação usa a mesma transação da versão e dos vínculos. Depois de `PUBLISHED`, os vínculos não
mudam; uma troca de imagem cria outra versão da história.

O dispositivo recebe mídia apenas por:

```text
GET /api/v2/devices/{deviceId}/assignments/{assignmentId}/assets/{assetId}/{role}
```

Essa rota deriva o objeto pelo vínculo e pela atribuição elegível do próprio dispositivo. Ela rejeita
outro `deviceId`, atribuição indisponível, `assetId` ausente ou papel não declarado; devolve bytes,
tipo, tamanho, `ETag` SHA-256 e `Cache-Control: no-store`. O Android monta essa rota na mesma origem
da credencial, verifica todos os metadados contra o pacote e faz troca atômica no armazenamento
privado. Ele nunca usa `path` do pacote como URL.

## Consequências

- O estado `READY_TO_START` passa a ter prova: manifesto aceito **e** recursos da primeira cena
  verificados localmente.
- URL assinada de OCI, quando entrar, fica atrás desse mesmo adaptador e não altera o contrato da UI.
- Biblioteca aprovada e mídia gerada por IA devem passar pelo mesmo registro de derivado/revisão;
  nenhum provedor cria uma exceção para o tablet.
- `media_sanitization_job` precisa registrar os bytes finais sanitizados, hoje ausentes, para a
  comparação ser completa.
- O primeiro corte entrega imagens; áudio preparado usa o mesmo modelo, mas só é liberado depois de
  validar formato, duração e política de voz.

## Testes de aceite

1. rejeitar materialização se faltar uma variante, se o SHA-256 divergir ou se a mídia não estiver
   sanitizada;
2. permitir somente dispositivo da turma atribuída baixar um recurso vinculado;
3. bloquear `assetId`, papel ou atribuição trocados, sem vazar se o objeto existe;
4. interromper download no Android sem criar arquivo final e sem avançar cursor quando bytes, ETag ou
   SHA-256 divergirem;
5. iniciar offline uma história somente depois de os recursos obrigatórios da cena inicial estarem
   presentes e íntegros.
