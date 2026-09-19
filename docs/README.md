# Documentação do InterpretaAI

Este índice organiza o projeto por interesse de leitura. A apresentação principal permanece no
[README](../README.md); aqui estão os documentos que sustentam a proposta pedagógica, a experiência,
a implementação e as decisões tomadas durante a evolução do MVP.

## Visão rápida

| Se você quer… | Comece por… |
|---|---|
| Conhecer o produto | [Catálogo de experiências](EXPERIENCE_CATALOG.md) |
| Ver o aplicativo | [Galeria visual](GALLERY.md) |
| Entender criança e professora | [Os dois espaços](TEACHER_STUDENT_SPACES.md) |
| Avaliar a entrega do hackathon | [Critérios e evidências](HACKATHON_CRITERIA.md) |
| Entender a tecnologia | [Arquitetura](ARCHITECTURE.md) |
| Preparar uma demonstração | [Roteiro do MVP](DEMO_RUNBOOK.md) |

## Produto e comissão

- [Resumo em 10 linhas](RESUMO_10_LINHAS.md) — apresentação breve do projeto.
- [Origem no HACKTUDO 2026](ORIGEM_HACKTUDO.md) — desafio, desenvolvimento da ideia e 8º lugar entre 227 projetos.
- [Critérios do hackathon](HACKATHON_CRITERIA.md) — relação entre tema, inovação, tecnologia e aplicabilidade.
- [Proposta técnica e pedagógica](../dist/InterpretaAI-Proposta-MVP.pdf) — documento de dez páginas.
- [Estado do MVP](MVP_STATUS.md) — implementado, demonstrado e em evolução.
- [Catálogo de experiências](EXPERIENCE_CATALOG.md) — histórias, atividades e jornada em tablet.
- [Galeria visual](GALLERY.md) — seleção completa de telas e viewports.
- [Espaço da Criança e Espaço da Professora](TEACHER_STUDENT_SPACES.md) — responsabilidades e conexão entre as interfaces.
- [Aplicação em GETs e escolas municipais regulares](GET_EMR_FIT.md) — proposta de piloto e limites institucionais.

## Pedagogia e experiência infantil

- [Fluxo pedagógico](FLUXO_PEDAGOGICO_FECHADO.md) — sequência da história à aplicação coletiva.
- [Escopo do 1º ao 5º ano](PEDAGOGICAL_SCOPE_1_TO_5.md) — profundidade possível por etapa de aprendizagem.
- [Avanço assistido](ASSISTED_ADVANCE_POLICY.md) — tempo, tentativas e acolhimento sem punição.
- [Linguagem infantil e elenco](CHILD_LANGUAGE_AND_CAST_AUDIT.md) — LÉIA, Alfa e revisão das falas.
- [Experiência visual infantil](CHILD_VISUAL_UX_AUDIT.md) — atenção, reações e redução de estímulos.
- [Curadoria visual dos gibis](COMIC_IMAGE_CURATION.md) — foco de cada quadro e progressão das pistas.
- [Fluxo de produção das histórias](COMIC_STORY_WORKFLOW.md) — integração entre narrativa, imagem e aplicativo.
- [História A Água da Chuva](SITUATIONAL_RAIN_COMIC.md) — construção e limites do episódio.
- [Jogos em celular e tablet](NEW_GAMES_TABLET_MOBILE.md) — caminho numérico, ligue os pontos e imagem-palavra.

## Engenharia e operação

- [Arquitetura](ARCHITECTURE.md) — Android, servidor, conteúdo, voz, visão e participação.
- [Operação online e offline](ONLINE_OFFLINE_ARCHITECTURE.md) — cache, sincronização, retry e recuperação.
- [Contrato da API de voz](VOICE_API.md) — entrada, resposta, limites e fallback.
- [Gateway e caminho de baixa latência](AI_GATEWAY_HOT_PATH.md) — roteamento e medição da conversa.
- [Servidor local](LOCAL_MVP_SERVER.md) — execução com modelo e voz locais.
- [Servidor InterpretaAI](../server/README.md) — módulo Java e provedores.
- [Pacote Oracle](../deploy/oracle/README.md) — implantação, HTTPS e verificação da VM.
- [Modo Foco](KIOSK.md) — fixação comum, Device Owner e saída administrativa.
- [Qualidade de engenharia](QUALITY_ENGINEERING.md) — testes, análise e verificações contínuas.
- [Segurança e integração contínua](SECURITY_AND_CI.md) — controles do código e da entrega.
- [Contrato de sincronização](SYNC_API_PROPOSAL.md) — missões, eventos e agregação do piloto.
- [Arquitetura 2.0](v2/README.md) — evolução planejada do Estúdio, autoria assistida, cache e relatórios.

## Privacidade, autoria e piloto

- [Dados e privacidade](DATA_AND_PRIVACY.md) — inventário, tratamento e riscos antes de uso real.
- [Checklist de piloto](PILOT_CHECKLIST.md) — condições técnicas, pedagógicas e institucionais.
- [Autoria, originalidade e uso de IA](AUTORIA_ORIGINALIDADE_E_IA.md) — contribuição do projeto e prevenção de plágio.
- [Proveniência dos ativos](ASSET_PROVENANCE.md) — origem, hashes e confirmações de imagens e sons.
- [Créditos de terceiros](../THIRD_PARTY_NOTICES.md) — bibliotecas, modelos e licenças.
- [Pesquisa de tablets para GETs](GET_RJ_TABLET_PILOT_RESEARCH.md) — evidências públicas e limites da pesquisa.
- [Pedido de informação sobre dispositivos](GET_TABLET_INFORMATION_REQUEST.md) — roteiro para confirmar o parque real.

## Evidências, auditorias e histórico de decisões

Estes documentos preservam o raciocínio e as verificações de versões específicas. Eles são úteis para
rastreabilidade, mas não precisam ser lidos para compreender a proposta inicial.

- [Auditoria final do MVP](FINAL_MVP_AUDIT.md)
- [Auditoria do regulamento](REGULAMENTO_HACKTUDO_2026.md)
- [Auditoria da versão infantil](CHILD_EXPERIENCE_VERSION_AUDIT_2026-09-18.md)
- [Auditoria de resiliência e caminho crítico](RESILIENCE_AND_HOT_PATH_AUDIT.md)
- [Auditoria da arquitetura de segurança](SECURITY_ARCHITECTURE_AUDIT_2026-09-17.md)
- [Orquestração da entrega](DELIVERY_ORCHESTRATION.md)

## Vocabulário de status

- **Implementado:** existe no código e possui teste ou evidência de inspeção.
- **Demonstrado:** funcionou no ambiente da apresentação, sem equivaler a operação institucional.
- **Em evolução:** está projetado ou parcialmente construído e não deve ser apresentado como concluído.

## Manutenção da documentação

- Atualizar [Estado do MVP](MVP_STATUS.md) quando uma entrega mudar de situação.
- Distinguir serviço disponível, integração demonstrada e projeto futuro.
- Não apresentar nota, ranking ou diagnóstico como métrica infantil.
- Atualizar o inventário de dados quando houver novo campo, provedor ou destino.
- Conferir todas as páginas quando a proposta em PDF for regenerada.
- Gerar novo hash e executar `./tools/check-delivery.sh --full` antes de publicar um APK.
