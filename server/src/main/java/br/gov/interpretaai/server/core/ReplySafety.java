package br.gov.interpretaai.server.core;

public final class ReplySafety {
    private ReplySafety() {}

    public static String normalize(String value) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (text.isBlank()) text = "Gostei de ouvir você! O que mais percebeu?";
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length > 2) text = sentences[0] + " " + sentences[1];
        return text.length() <= 360 ? text : text.substring(0, 357) + "...";
    }
}
