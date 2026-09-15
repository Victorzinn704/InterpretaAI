package br.gov.interpretaai.server.core;

/** Provedor remoto que pode receber uma sonda sintética antes do percurso infantil. */
public interface WarmableConversationProvider extends RoutableConversationProvider {
    boolean warmUp();
    boolean isWarm();
    String activeModelId();
}
