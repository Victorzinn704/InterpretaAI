package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;

public final class ReplySafety {
    private ReplySafety() {}

    public static String normalize(String value) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (text.isBlank()) text = "Gostei de ouvir você! O que mais percebeu?";
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length > 2) text = sentences[0] + " " + sentences[1];
        return text.length() <= 360 ? text : text.substring(0, 357) + "...";
    }

    public static String normalize(String value, NextAction nextAction) {
        String text = normalize(value);
        if (nextAction != NextAction.SPEAK_AGAIN) return text;

        int firstQuestion = text.indexOf('?');
        if (firstQuestion >= 0) return text.substring(0, firstQuestion + 1);

        String acknowledgement = text.split("(?<=[.!])\\s+", 2)[0]
                .replaceFirst("[.!]+$", "");
        return normalize(acknowledgement + ". O que mais você percebe nessa cena?");
    }
}
