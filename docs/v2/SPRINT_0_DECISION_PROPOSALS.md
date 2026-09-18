# Propostas de decisão para fechar a Sprint 0

Estas são recomendações conservadoras para o primeiro piloto. Elas não substituem aprovação
pedagógica, institucional, jurídica ou financeira. O aceite deve registrar nome, papel e data.

## D-01 — objetivos e fontes do primeiro piloto

**Recomendação:** limitar o primeiro piloto a compreensão de narrativa curta, identificação de
informação explícita/pista, relação imagem–palavra, consciência fonêmica inicial e explicação oral
em dupla. Pedagogia seleciona os códigos/objetivos exatos a partir da BNCC e do Currículo Carioca.

Fontes candidatas oficiais:

- [BNCC — Educação Infantil e Ensino Fundamental](https://www.gov.br/mec/pt-br/escola-em-tempo-integral/BNCC_EI_EF_110518_versaofinal.pdf);
- [Currículo — Secretaria Municipal de Educação do Rio](https://educacao.prefeitura.rio/curriculo/);
- [Recursos pedagógicos da SME Rio](https://educacao.prefeitura.rio/recursos-pedagogicos/).

Não indexar todo material automaticamente. O curador escolhe trechos, registra versão/hash e define
quais objetivos podem fundamentar.

## D-02 — imagem enviada pela professora

**Recomendação do MVP:** aceitar objetos, alimentos, plantas, animais, lugares sem pessoas e
materiais sobre os quais a professora confirme direito de uso. Bloquear imagem com pessoa
identificável até existir política institucional específica. Remover EXIF/localização, manter o
original privado e mostrar exatamente quais provedores externos receberão o derivado.

Ambiguidade, logotipo ou possível obra protegida exige confirmação docente e pode restringir a
atividade ao uso interno. Essa regra reduz risco; não é parecer jurídico.

## D-03 — identidade adulta

**Recomendação:** manter OIDC como contrato e realizar spike da Sprint 1 com duas rotas:

1. Keycloak autogerido na Oracle, se memória, backup e operação passarem no teste;
2. provedor OIDC institucional, se a rede já possuir identidade federada.

Não criar autenticação própria com senha no Spring. Para desenvolvimento, usar realm/usuários
sintéticos; a escolha de produção depende da infraestrutura real da escola/rede.

**Execução em 18/09/2026:** a primeira rota foi adotada para o piloto. Keycloak 26.7.4, banco
exclusivo, realm, cliente, professora e callback estão ativos na Oracle e passaram no ensaio de
sessão. A segunda rota permanece disponível por federação quando uma SME definir domínio/provedor.

## D-04 — provedores de IA

**Recomendação:** allowlist server-side por capacidade (`PLANNING`, `VISION`, `IMAGE`, `VOICE`,
`CODEX_AUTHORING`) e política separada para autoria docente e jornada infantil. O caminho infantil
obrigatório continua local. Provedor externo só entra após termos, região, retenção e público menor
de idade serem revisados.

Codex fica restrito à montagem agentiva de rascunhos. LangChain4j planeja e recupera fontes.
Provedor de imagem gera pixels; nenhum deles publica.

## D-05 — retenção

**Ponto de partida para avaliação, não prazo legal afirmado:**

| Dado | Proposta inicial |
|---|---|
| original temporário aceito/rejeitado | excluir em até 24 horas após derivados ou falha final |
| rascunho nunca publicado | 30 dias após última edição, com aviso e extensão docente |
| versão publicada e derivados | enquanto houver atribuição/uso e conforme política institucional |
| eventos fechados | 12 meses para avaliar piloto, depois agregar/expurgar |
| observação docente | política do registro institucional, com histórico e acesso controlado |
| logs técnicos | 30 dias, sem áudio, transcrição ou texto infantil bruto |
| sugestão de IA descartada | 30 dias para auditoria de qualidade |

A ANPD estabelece que o melhor interesse deve prevalecer na avaliação do tratamento de dados de
crianças e adolescentes; a instituição ainda precisa definir finalidade, hipótese legal,
necessidade, transparência e direitos aplicáveis. Referências: [Enunciado da ANPD](https://www.gov.br/anpd/pt-br/assuntos/noticias/anpd-divulga-enunciado-sobre-o-tratamento-de-dados-pessoais-de-criancas-e-adolescentes)
e [materiais orientativos](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes).

## D-06 — aparelhos suportados

**Recomendação:** preservar o estado real do projeto: `minSdk 26` (Android 8), `targetSdk 35`, toque
por dedo como baseline e layouts auditados em 360×640, 412×915 e 800×1280. O aparelho de referência
do piloto é 4 GB RAM/64 GB quando disponível, mas a arquitetura não depende de S Pen.

Antes da Sprint 2, testar um aparelho físico da frota: áudio, microfone, câmera, WebP, arraste,
armazenamento, WorkManager, TTS local e Lock Task. Compatibilidade por especificação não substitui
o teste físico.

## D-07 — limites financeiros

**Recomendação:** adotar imediatamente os limites de chamadas do `AI_BUDGET.md`, mas deixar valores
monetários sem aprovação até o benchmark de dez histórias. Configurar teto por job/professora/
escola/global; atingir teto preserva rascunho e bloqueia nova geração, nunca relaxa validação.

## Registro de aceite

| ID | Decisão escolhida | Aprovador/papel | Data | Evidência/ressalva |
|---|---|---|---|---|
| D-01 | — | — | — | — |
| D-02 | — | — | — | — |
| D-03 | — | — | — | — |
| D-04 | — | — | — | — |
| D-05 | — | — | — | — |
| D-06 | — | — | — | — |
| D-07 | — | — | — | — |
