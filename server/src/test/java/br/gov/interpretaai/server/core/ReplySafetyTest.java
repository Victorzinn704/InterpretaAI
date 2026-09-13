package br.gov.interpretaai.server.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReplySafetyTest {
    @Test void limitsReplyToTwoSentences() {
        assertThat(ReplySafety.normalize("Uma. Duas! Três?" )).isEqualTo("Uma. Duas!");
    }

    @Test void suppliesSafeTextWhenProviderReturnsNothing() {
        assertThat(ReplySafety.normalize(" ")).contains("Gostei").endsWith("?");
    }
}
