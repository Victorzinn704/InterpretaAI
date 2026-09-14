package br.gov.interpretaai.server.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class NvidiaModelCatalogTest {
    @Test
    void acceptsEveryReviewedNvidiaModel() {
        assertEquals(NvidiaModelCatalog.GEMMA_4_31B,
                NvidiaModelCatalog.fromModelId("google/gemma-4-31b-it"));
        assertEquals(NvidiaModelCatalog.KIMI_K3,
                NvidiaModelCatalog.fromModelId("moonshotai/kimi-k3"));
        assertEquals(NvidiaModelCatalog.MISTRAL_NEMOTRON,
                NvidiaModelCatalog.fromModelId("mistralai/mistral-nemotron"));
        assertEquals(NvidiaModelCatalog.NEMOTRON_3_ULTRA,
                NvidiaModelCatalog.fromModelId("nvidia/nemotron-3-ultra-550b-a55b"));
    }

    @Test
    void rejectsAnUnreviewedModel() {
        assertThrows(IllegalArgumentException.class,
                () -> NvidiaModelCatalog.fromModelId("unknown/model"));
    }

    @Test
    void reportsUnavailableWhenNoKeyWasConfigured() {
        var provider = new NvidiaConversationProvider(
                "https://integrate.api.nvidia.com/v1",
                "",
                "google/gemma-4-31b-it",
                true,
                new ObjectMapper());

        assertThrows(IllegalStateException.class, () -> provider.reply(
                new Request("session", "bola", 1, "acho que falta a bola", Speaker.LEIA_FEMALE, false),
                java.util.List.of()));
        assertEquals(false, provider.warmUp());
        assertEquals(false, provider.isWarm());
        assertEquals(false, provider.available());
    }
}
