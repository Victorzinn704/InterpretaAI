package br.gov.interpretaai.server.provider;

import java.util.Arrays;

/** Modelos NVIDIA NIM explicitamente avaliados para o MVP. */
public enum NvidiaModelCatalog {
    GEMMA_4_31B("google/gemma-4-31b-it", "análise multimodal"),
    KIMI_K3("moonshotai/kimi-k3", "análise visual complexa"),
    MISTRAL_NEMOTRON("mistralai/mistral-nemotron", "mediação curta em tempo real"),
    NEMOTRON_3_ULTRA("nvidia/nemotron-3-ultra-550b-a55b", "revisão textual complexa");

    private final String modelId;
    private final String intendedRole;

    NvidiaModelCatalog(String modelId, String intendedRole) {
        this.modelId = modelId;
        this.intendedRole = intendedRole;
    }

    public String modelId() {
        return modelId;
    }

    public String intendedRole() {
        return intendedRole;
    }

    public static NvidiaModelCatalog fromModelId(String modelId) {
        return Arrays.stream(values())
                .filter(model -> model.modelId.equals(modelId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Modelo NVIDIA não aprovado para o MVP: " + modelId));
    }
}
