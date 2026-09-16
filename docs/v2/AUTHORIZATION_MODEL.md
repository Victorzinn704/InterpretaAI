# Autorização e isolamento institucional

## Hierarquia

```text
Rede/Tenant
  └─ Escola
      ├─ Turma
      │   ├─ grupos
      │   ├─ participantes no cofre de identidade
      │   └─ dispositivos atribuídos
      ├─ biblioteca da escola
      └─ usuários e papéis
```

Autenticação responde “quem é”; autorização responde “pode fazer esta ação neste recurso”. Toda
consulta filtra o escopo no servidor. Identificadores vindos do navegador não concedem acesso.

## Papéis iniciais

| Ação | Professora | Coordenação | Admin. escola | Secretaria/rede | Dispositivo |
|---|---:|---:|---:|---:|---:|
| criar/editar próprio rascunho | sim | sim | sim | não | não |
| usar biblioteca aprovada da escola | sim | sim | sim | sim | não |
| aprovar/publicar para própria turma | sim | sim | sim | não | não |
| publicar para outra turma | não | sim | sim | não | não |
| gerir membros da turma | própria turma | sim | sim | não | não |
| gerir usuários/papéis | não | não | sim | rede | não |
| ver evidência individual | própria turma | escopo autorizado | sim | não por padrão | não |
| ver agregado de rede | não | escola | escola | sim | não |
| consultar manifesto | não | não | não | não | somente próprio ID |
| enviar eventos/sessões | não | não | não | não | somente próprio ID |
| configurar provedor/segredo | não | não | admin técnico | admin técnico | não |

Papéis não substituem vínculo. Uma professora com papel correto, mas sem vínculo com a turma, não
acessa seus participantes, atribuições ou relatórios.

## Regras obrigatórias

- negar por padrão e autorizar por ação + recurso + escola + vínculo;
- não aceitar `role`, `schoolId` ou `classroomId` do token sem confrontar o banco;
- exportação, leitura individual, upload, aprovação, publicação e mudança de papel geram auditoria;
- códigos de pareamento expiram, são de uso único e não carregam segredo permanente;
- credencial de dispositivo só lê suas atribuições e grava seus próprios estados/eventos;
- professora só cria/revoga pareamento em turma com vínculo ativo; coordenação/administração ficam
  limitadas à própria escola;
- código e credencial são armazenados como HMAC; o token completo aparece somente na resposta de
  resgate e deve permanecer no armazenamento seguro do aplicativo;
- executor Codex tem identidade de workload sem acesso a rotas adultas ou de dispositivo;
- worker de mídia lê somente objetos da tarefa autorizada;
- suporte técnico usa acesso temporário, justificativa e auditoria; não assume papel da professora.

## Testes negativos mínimos

1. trocar `schoolId`, `classroomId`, `storyId` ou `deviceId` na URL retorna negação sem vazar existência;
2. professora removida da turma perde acesso imediatamente, sem reescrever sessões antigas;
3. dispositivo não lê manifesto de outro dispositivo;
4. Codex não acessa relatório, cofre de identidade, aprovação ou publicação;
5. URL assinada expirada ou de outro objeto falha;
6. secretaria recebe agregado permitido, não linhas individuais;
7. cache do navegador não exibe dados depois do logout.
