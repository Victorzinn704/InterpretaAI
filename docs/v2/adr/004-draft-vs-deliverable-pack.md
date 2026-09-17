# ADR 004 — Rascunho não é pacote aprovado

Status: portão de backend e interface docente de revisão implementados localmente; ponta a ponta pendente. Data: 17/09/2026.

## Problema constatado

O fluxo anterior usava o validador de entrega infantil para materializar um `DRAFT`, exigindo
`approvedBy`, `approvedAt` e `reviewedByTeacher=true` antes da revisão humana. O backend local
agora usa `validateDraft` no ingresso e `validate` somente ao formar o snapshot aprovado.

## Decisão para o fluxo 2.0

1. O worker produz um **rascunho interno**, sem `approvedBy`, `approvedAt` ou `publishedAt`;
   imagens geradas/editadas entram como não revisadas. Esse payload não pode ser entregue ao
   Android nem usar o esquema de pacote publicado como se já tivesse sido aprovado.
2. A interface docente mostra história, falas, palavra, fonte e **cada imagem real** servida
   por URL privada autenticada. A aprovação deve referenciar a revisão e o hash exato do
   rascunho que foi exibido; a professora confirma cada variante da imagem, inclusive celular e
   tablet quando distintas. Pedido de ajuste/rejeição ainda não está implementado.
3. Só após essa ação o servidor monta um **snapshot entregável** com identidade e horário de
   aprovação reais, hashes das mídias vinculadas e procedência revisada. O snapshot passa pelo
   validador completo e é congelado com hash; publicação e atribuição à turma continuam sendo
   ações separadas e auditadas.
4. O Android recebe exclusivamente snapshots `PUBLISHED` atribuídos à sua turma. O cache
   offline conserva esses bytes e verifica hashes, sem depender da conexão na hora da aula.

Essa separação evita que a IA ou um teste sintético se atribua uma aprovação humana. A rota de
revisão e a confirmação por hash, bem como a [UI docente](../STUDIO_REVIEW.md), existem localmente; ainda faltam
login OIDC real e teste de geração até cache do aparelho. Até esses testes, **não habilitar** o worker de autoria gerada nem
considerar o fluxo de publicação v2 pronto para crianças. A rota v2 pública na Oracle também
permanece dependente de OIDC e verificação do proxy.

## Critérios de aceite

- Rascunho com metadados de aprovação/publicação é rejeitado; pacote infantil sem eles também.
- Aprovação sem confirmação explícita de todas as mídias ou com hash/revisão obsoletos falha.
- Pacote publicado contém somente dados da aprovação efetiva e mídia privada da mesma escola.
- Teste de integração percorre geração → revisão → aprovação → publicação → entrega → cache.
