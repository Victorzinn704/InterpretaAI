# Provisionamento do modo quiosque

## Dois níveis

1. **Demonstração/aparelho comum:** o botão Modo Foco inicia a fixação de tela e o Android pede confirmação a um adulto. O usuário ainda consegue sair seguindo o gesto/PIN do Android.
2. **Tablet institucional dedicado:** o app vira Device Owner. Home, Recentes e notificações ficam ocultos no Lock Task; o InterpretaAI vira launcher e abre após reiniciar.

O segundo nível só deve ser aplicado a tablet da escola preparado para esse fim. Um app comum não pode se conceder essa autoridade.

## Dispositivo de teste limpo

O Android deve estar recém-restaurado, sem contas e sem outro Device Owner. Depois de instalar o APK:

```bash
adb shell dpm set-device-owner br.gov.interpretaai/.platform.InterpretaDeviceAdminReceiver
adb shell am force-stop br.gov.interpretaai
adb shell monkey -p br.gov.interpretaai 1
```

Ao abrir, o app allowlista a si mesmo para Lock Task, desabilita recursos de interface do sistema, bloqueia janelas sobrepostas e assume a Home.
Nesse modo o foco começa automaticamente, mantém a tela ligada e impede Home e Recentes. O teste automatizado deve confirmar `mLockTaskModeState=LOCKED`.

## Não Perturbe

No painel do educador (PIN de demonstração `2468`), toque em **Autorizar Não Perturbe**. Essa é uma permissão especial e aparece numa tela do Android. Em Device Owner, o Lock Task já esconde a área de notificações; Não Perturbe também silencia interrupções sonoras.
Ao encerrar o foco, o app restaura o filtro de interrupções que estava ativo antes da sessão.

## Saída administrativa

Abra a área do educador e use **Encerrar foco**. Para remover o Device Owner de um aparelho de desenvolvimento, o método depende da política/OEM e pode exigir restauração de fábrica; planeje o procedimento de suporte antes do piloto.

Em produção, substitua o PIN fixo por autenticação do professor, segredo rotativo do dispositivo e gestão remota (Android Management API ou EMM).
