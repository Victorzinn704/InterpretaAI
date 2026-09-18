# Dados, privacidade e limites do MVP

Este inventário descreve o código da versão de piloto atual. Ele não substitui avaliação jurídica, pedagógica ou
de impacto. O MVP deve ser demonstrado apenas com aliases e objetos, nunca com nomes, rostos ou
outros dados reais de crianças.

## Fluxos de dados atuais

| Dado | Onde nasce e fica | Sai do aparelho? | Retenção atual |
|---|---|---|---|
| Eventos pedagógicos | Outbox SQLite v3 do Android | Sim, somente quando o canal do piloto está configurado | Pendente até confirmação; local até o educador limpar ou remover o app; retenção do servidor ainda precisa de política |
| Transcrição curta | Serviço `SpeechRecognizer` do Android | Pode sair, conforme mecanismo/OEM; `PREFER_OFFLINE` é preferência, não garantia | O app não a grava; o servidor a mantém apenas na memória da sessão |
| Áudio captado | Serviço de reconhecimento configurado no aparelho | Depende do mecanismo de voz do Android | O InterpretaAI não cria arquivo de áudio bruto |
| Resposta sintetizada | Spring/Kokoro e cache do Android | Chega ao aparelho por HTTPS temporário | Arquivo apagado após reprodução ou erro tratado |
| Resposta idempotente da LÉIA | Cache Caffeine e banco do servidor | Não além do servidor configurado | Texto e áudio gerados pela LÉIA por até 10 min; requisição identificada por HMAC, sem áudio/transcrição da criança em claro |
| Áudio sintetizado repetido | Cache Caffeine apenas em RAM | Não | Até 32 MiB/10 min; chave usa voz + SHA-256 do texto, e falha/áudio vazio não é cacheado |
| Foto da missão | Cache do Android e ML Kit embarcado | Não | Apagada depois da análise concluída |
| Contexto da conversa | RAM do servidor, por `sessionId` | Já chega como transcrição | Máximo de 6 mensagens por sessão, 2.000 sessões e descarte após 10 min de inatividade |
| Logs do servidor | Processo Spring | Não se aplica | Duração, turno e fallback; sem áudio ou transcrição |
| Sonda de aquecimento Gemini/NVIDIA | Servidor Spring | Sim, quando a rota remota está ativa | Texto sintético fixo; nenhum conteúdo da criança |
| Missão atribuída | Professor/tablet: `deviceId`, turma, pseudônimo, avatar, atividade fechada, pista e versão | Sim, quando o piloto online está configurado | Última versão no servidor e no tablet; sem nome, matrícula ou texto livre |
| Token do tablet | Digitado pelo educador e salvo em preferência privada | Enviado apenas ao servidor configurado | Até reconfiguração ou remoção do app; excluído de backup e transferência |
| Token do professor | Campo protegido da área adulta | Enviado por HTTPS ao publicar | Não é persistido pelo aplicativo e é limpo após o envio |
| Agregados de turma/rede | Banco do servidor | Disponíveis às APIs docente/secretaria | Sem alias, avatar, aparelho, áudio ou transcrição; retenção ainda não automatizada |
| Auditoria administrativa | Banco do servidor | Não | Papel, escopo e horário de cada leitura agregada; prazo de retenção ainda precisa ser definido |
| Diagnóstico do tablet | Gerado localmente na área adulta | Não automaticamente; só sai se o educador copiar e compartilhar | Não é persistido pelo app; sem serial, IMEI, conta, IP, token ou dado infantil |

Os eventos locais registram tipo, alias de demonstração ou escopo coletivo, turma, atividade, categoria curta, duração,
modalidade e horário. A versão 2 do banco removeu a coluna legada `success`: respostas diferentes
continuam ajudando a conduzir a atividade, mas não viram nota ou “acerto da criança”. A categoria
enviada ao piloto é derivada localmente de evento e modalidade; o cliente não envia o valor livre,
alias, turma, transcrição ou mídia. Com um participante, o servidor deriva alias e turma do
`deviceId`; com dois a quatro no mesmo aparelho, grava `participation_scope=GROUP`, contagem e alias
nulo. Assim uma resposta coletiva não é convertida em desempenho individual. O servidor
devolve somente agregados nas leituras administrativas.

`avatarId` define apenas a aparência, enquanto `learnerAlias` diferencia o participante nos eventos.
O formato fechado — por exemplo, `pipa-07` — impede nome livre no canal do piloto. A Home infantil
não mostra esse código. O vínculo com nome ou matrícula não existe no MVP e só pode entrar em um
cofre institucional separado, com RBAC e auditoria.

Em tablet compartilhado, a Home exibe apenas os emojis dos avatares e o tamanho do grupo. Os aliases
continuam restritos à área adulta. O limite de quatro evita uma falsa promessa de autoria individual
em torno de um único aparelho; observação individual continua responsabilidade do professor. O
rodízio infantil usa somente nome e emoji públicos do avatar, nunca alias, nome civil ou matrícula;
trocar o avatar que conduz a etapa não altera o escopo coletivo do evento.

## Controles já implementados

- `android:allowBackup="false"` e regras que excluem banco, arquivos e preferências de backup/transferência;
- transcrição enviada ao endpoint limitada a 280 caracteres e conversa limitada a três turnos;
- câmera opcional, processamento local e orientação para fotografar objetos, não pessoas;
- nenhuma chave de modelo ou voz dentro do APK;
- tokens do piloto não entram no Git/BuildConfig; o token do professor não é persistido;
- servidor de atribuições/eventos nasce desligado, exige tokens distintos de tablet, professor e
  secretaria e rejeita campos desconhecidos;
- eventos usam ID único, lote transacional e só saem da fila local depois de confirmação do servidor;
- resumos administrativos não retornam alias, avatar ou aparelho e cada leitura é auditada;
- fallback local quando a API não responde em seis segundos;
- circuito de gateway que não chama o provedor remoto enquanto a sonda estiver fria;
- deadline externo, circuito por falha/lentidão, bulkhead e replay idempotente persistido;
- outbox guarda apenas eventos operacionais neutros, sem transcrição, áudio captado ou avaliação;
- fingerprint idempotente usa HMAC da requisição completa; produção exige segredo estável fora do Git;
- botão do educador para apagar todas as métricas locais;
- diagnóstico técnico limitado a propriedades do aparelho e serviços necessários ao piloto,
  protegido pela área adulta e sem identificador persistente de hardware;
- IA sem nota, diagnóstico, ranking ou classificação absoluta de emoção.

## Riscos residuais e decisão de piloto

| Risco | Impacto | Tratamento necessário antes de crianças reais |
|---|---|---|
| Reconhecimento de voz depende do serviço Android | Áudio pode ser processado por terceiro mesmo com preferência offline | Selecionar e validar mecanismo offline por modelo de aparelho ou formalizar o fornecedor e sua base de tratamento |
| Token compartilhado e limite somente por sessão | Vazamento permite criar outras sessões até a rotação | Usar apenas dados anônimos; adicionar contenção por rede e migrar para login, RBAC e credencial individual por aparelho antes do piloto real |
| Avatar/turma não têm vínculo institucional | Não há identidade real nem autorização por turma | Manter a demonstração pseudônima; implantar cofre de identidade, perfis e auditoria antes de sincronizar dados reais |
| Cache pode sobreviver a encerramento abrupto | Foto ou áudio temporário pode permanecer até limpeza do cache | Limpar temporários na inicialização e no ciclo de vida, além dos callbacks atuais |
| Memória expira por atividade, não por relógio dedicado | Sessão ociosa pode ficar em RAM até nova chamada ou reinício | Adicionar limpeza agendada/armazenamento com TTL verificável em hospedagem persistente |
| Métricas ainda não passaram por validação de campo | Indicadores podem ser mal interpretados | Definir dicionário pedagógico com educadores e treinar leitura sem rótulos |
| PIN adulto é de demonstração | Área do professor não está pronta para dados reais | Trocar por autenticação institucional e registrar auditoria de acesso |
| Eventos detalhados não têm limpeza automática no servidor | Retenção pode ultrapassar a finalidade do piloto | Definir prazo com o controlador e executar expurgo verificável antes de dados reais |
| Envio oportunista não usa WorkManager | Eventos podem aguardar uma nova abertura/interação após longos períodos offline | Adicionar trabalho periódico com restrição de rede, sem bloquear a jornada |

## Regra para avançar

Não iniciar piloto com dados reais antes de definir responsáveis, finalidade, minimização, prazo de
retenção, descarte, acesso, resposta a incidentes e alternativa sem microfone/câmera. A escola e os
responsáveis competentes devem revisar o tratamento conforme LGPD e normas locais; o repositório não
declara consentimento ou conformidade jurídica automaticamente.
