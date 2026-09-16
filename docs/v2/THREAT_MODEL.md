# Modelo de ameaças — autoria até publicação

## Escopo e ativos

Fluxo: navegador da professora → upload → API Spring → objeto privado → worker/RAG/modelos/Codex →
rascunho → validação → aprovação → pacote assinado → cache Android.

Ativos protegidos: contas, escopo escolar, originais enviados, rascunhos, fontes, identidade
infantil, observações, segredos, pacotes publicados e trilha de auditoria.

## Fronteiras de confiança

1. navegador não é confiável;
2. arquivo enviado não é confiável;
3. texto recuperado pelo RAG não é instrução confiável;
4. resposta de modelo/Codex é rascunho não confiável;
5. armazenamento/URL assinada não concede autorização de domínio;
6. Android pode estar offline, desatualizado ou comprometido;
7. log e observabilidade não podem receber conteúdo infantil bruto.

## Ameaças e controles

| ID | Ameaça | Impacto | Controle preventivo | Evidência/teste |
|---|---|---|---|---|
| `T-01` | upload malicioso ou bomba de imagem | indisponibilidade/RCE | limite antes/depois de decodificar, regravação, isolamento | corpus adversarial |
| `T-02` | EXIF/localização preservados | exposição pessoal | remover metadados e verificar saída | inspeção automatizada |
| `T-03` | troca de IDs entre escolas | vazamento | RBAC + vínculo + filtro de escopo | testes negativos multi-tenant |
| `T-04` | prompt injection em imagem/documento | ação indevida | entrada como dados, ferramentas fechadas, saída estruturada | suíte de injeção |
| `T-05` | Codex acessa VM/banco/publicação | alteração/exposição | contêiner, workspace temporário, egress allowlist, credencial mínima | teste de escape/negação |
| `T-06` | modelo inventa regra/fonte | conteúdo inadequado | RAG com refs, validadores, limitação explícita | eval de fundamentação |
| `T-07` | imagem gerada imprópria/incoerente | dano pedagógico | política, detector, procedência e revisão docente | portão de mídia |
| `T-08` | publicação sem consentimento docente | conteúdo não revisado | credencial separada e transição aprovada | teste de máquina de estados |
| `T-09` | pacote alterado no transporte/cache | execução incorreta | hash, assinatura e troca atômica | teste de adulteração |
| `T-10` | app antigo recebe componente novo | tela quebrada | matriz/minAppVersion + validação local | teste de incompatibilidade |
| `T-11` | evento offline ligado à criança errada | evidência falsa | contexto imutável/recibo de sessão | teste de troca de vínculo |
| `T-12` | segredo em APK, Git ou log | abuso de provedor | Vault/env, scanner e redação | varredura de segredo |
| `T-13` | custo ilimitado/retry em cascata | perda financeira | orçamento, idempotência, limite e circuito | falha injetada |
| `T-14` | exclusão incompleta | retenção indevida | mapa de dependências e job auditável | teste de expurgo |
| `T-15` | relatório transforma sinal em diagnóstico | dano/estigma | linguagem fechada, refs e revisão humana | casos pedagógicos adversariais |

## Decisões de privacidade pendentes

- se imagens podem conter pessoas e qual confirmação é exigida;
- prazo do original temporário e de rascunhos não publicados;
- provedores externos autorizados e regiões de processamento;
- base e política institucional para identidade/observações;
- processo de correção, exportação e exclusão;
- tamanho mínimo de grupo para agregados da secretaria.

Enquanto não forem fechadas, o desenho mais restritivo é: bloquear pessoas identificáveis,
expirar originais rapidamente, não enviar dados infantis a modelos e suprimir agregados pequenos.

## Responsabilidade e resposta

Segurança técnica cuida de isolamento, segredos e incidente; privacidade decide finalidade e
retenção; pedagogia aprova linguagem e fontes; produto responde pelo fluxo; professora aprova cada
versão. Incidente suspende novas gerações/publicações sem impedir histórias já íntegras no cache.
