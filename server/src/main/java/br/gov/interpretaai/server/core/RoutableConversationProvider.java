package br.gov.interpretaai.server.core;

public interface RoutableConversationProvider extends ConversationProvider {
    String providerId();
    boolean available();
}
