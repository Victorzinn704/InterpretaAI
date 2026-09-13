# Roteiro operacional da demonstração

## 1. Conferência reproduzível

Na raiz do repositório:

```bash
./tools/check-delivery.sh --full
```

O comando valida testes locais, lint, build, hashes, resumo de 10 linhas e PDF de 10 páginas. Testes
instrumentados exigem emulador ou aparelho e continuam sendo uma etapa separada.

## 2. Subir a experiência online

```bash
./tools/start-local-mvp.sh
curl -fsS http://127.0.0.1:8088/actuator/health
```

Em outro terminal, que deve permanecer aberto:

```bash
./tools/start-demo-tunnel.sh
```

Copie a URL HTTPS mostrada e gere o APK conectado:

```bash
./tools/build-online-apk.sh https://URL-MOSTRADA.trycloudflare.com
```

Por padrão, o APK e os hashes também vão para
`~/Desktop/InterpretaAI-Entrega-11h45`. Para usar outra pasta:

```bash
INTERPRETAAI_DELIVERY_DIR=/caminho/da/entrega \
  ./tools/build-online-apk.sh https://URL-MOSTRADA.trycloudflare.com
```

## 3. História de três minutos

1. **Problema:** o celular costuma disputar atenção; o InterpretaAI o transforma em mediador breve.
2. **LEIA:** na Home, toque em “Começar com a LEIA” e mostre uma decisão por tela, sem rolagem.
3. **Coautoria:** no gibi, a criança observa, fala uma hipótese e recebe uma pergunta curta da LEIA.
4. **Aplicação:** monte um quebra-cabeça por dois toques e ouça palavra e fonema na conclusão.
5. **Grupo:** conclua o ciclo e mostre o comando para o celular descansar enquanto a dupla continua.
6. **Professor:** abra a área adulta e mostre participação, modalidade, ajuda e duração — sem nota/ranking.

Feche relacionando explicitamente: adequação ao tema, coautoria como inovação, solução Android/Java
com fallback e aplicabilidade em sala. Não apresente secretaria, Oracle, Gemini ou Google Cloud como
ativos no MVP.

## 4. Contingência

- Sem túnel ou internet: demonstre o fallback local e diga que a jornada não é interrompida.
- Sem reconhecimento de voz: use as alternativas preparadas; isso também prova multimodalidade.
- Sem câmera: pule a missão; ela é opcional e não bloqueia o núcleo do gibi/puzzle.
- Voz externa degradada: o TTS instalado no Android narra a resposta preparada.
- Aparelho comum: explique fixação confirmada por adulto; bloqueio automático completo só em Device Owner.

## 5. Última checagem antes de apresentar

- health público responde `UP` e Mac permanece ligado;
- APK instalado é o mesmo de `dist/` e bate com `dist/SHA256.txt`;
- microfone, volume e câmera foram autorizados por um adulto;
- nenhuma informação real de criança será falada, digitada ou fotografada;
- saída do Modo Foco/PIN adulto foi ensaiada;
- PDF abre com exatamente 10 páginas.
