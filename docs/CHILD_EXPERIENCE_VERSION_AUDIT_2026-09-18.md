# Auditoria de versões da experiência infantil — 18/09/2026

## Estado verificado

- `main`, `origin/main` e a cópia principal do MacBook apontam para `30f4054`, tag `v0.28.0`.
- A cópia principal do MacBook está limpa.
- A linha histórica que criou Alfa e ajustou a linguagem infantil continua preservada em `7d7cf70`.
- A linha do Mac termina em `3ec738a` (`mac/archive-main`). Ela possui 12 commits próprios e está 55 commits atrás da `main` atual.
- `30f4054` integrou as histórias e imagens do Mac na arquitetura atual. Reaplicar a branch antiga inteira removeria trabalho posterior de sala virtual, identidade, segurança e entrega.

## O que aconteceu com LÉIA e Alfa

Os ativos não foram apagados. A versão atual contém:

- `leia_and_alfa_v1.webp`;
- `leia_and_alfa_support_v2.webp`;
- `leia_and_alfa_celebrate_v2.webp`;
- cenas de reação em gibi, puzzle, fechamento e história preparada;
- retrato completo na Home.

O problema estava na composição das telas. A maioria das etapas usava `ChildStageScaffold`, mas o componente não exibia LÉIA e Alfa. Várias telas mantinham apenas o texto `LEIA`, que identifica o método e não substitui a presença da personagem **LÉIA** e do cachorro **Alfa**.

Correção desta branch: o scaffold infantil passa a exibir uma aba compacta com LÉIA e Alfa. Home, avanço assistido e encerramento continuam usando suas cenas completas para evitar duplicação.

## Histórias localizadas

| Conteúdo | Estado na `v0.28.0` | Observação |
|---|---|---|
| Mistério da bola | Integrado | Jornada principal com Lia, Davi, LÉIA e Alfa, pistas interativas e cinco cenas no catálogo local. |
| A água da chuva | Integrado | História independente com quatro quadros e pistas visuais. |
| A maçã da LÉIA | Contrato e fluxo preparados | Existe como `LearningStoryPack` de teste e exemplo de autoria/entrega; não está empacotada como terceira história selecionável no menu infantil. |
| Histórias produzidas pela professora | Arquitetura existente | Dependem de revisão, publicação e sincronização do pacote aprovado para o tablet. |

As telas e imagens produzidas no Mac foram integradas. A lacuna atual é de catálogo: somente bola e chuva aparecem como aventuras locais selecionáveis. A maçã comprova o contrato de conteúdo, mas ainda não equivale a uma história infantil empacotada para uso offline.

## Quadro de desenho

O molde de maçã era um círculo com uma haste. A bola também usa um círculo, então a pista visual não distinguia as duas formas e podia parecer laranja.

Correção desta branch:

- corpo com concavidade superior e dois lóbulos;
- base orgânica em vez de circunferência perfeita;
- haste inclinada;
- folha visível;
- manutenção do traço claro para permitir que a criança desenhe por cima ou crie livremente.

## Decisão de integração

Não restaurar commits antigos em bloco. Manter `v0.28.0` como base, recuperar comportamentos específicos por comparação e validar cada mudança nos três tamanhos alvo: `360x640`, `412x915` e `800x1280`.
