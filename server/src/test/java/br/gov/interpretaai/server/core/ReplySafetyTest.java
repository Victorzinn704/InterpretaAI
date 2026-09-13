package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReplySafetyTest {
    @Test void limitsReplyToTwoSentences() {
        assertThat(ReplySafety.normalize("Uma. Duas! Três?" )).isEqualTo("Uma. Duas!");
    }

    @Test void suppliesSafeTextWhenProviderReturnsNothing() {
        assertThat(ReplySafety.normalize(" ")).contains("Gostei").endsWith("?");
    }

    @Test void addsOneGuidingQuestionWhenAnotherTurnIsExpected() {
        assertThat(ReplySafety.normalize(
                "Que ótima ideia! Você ajudou a procurar a bola.", NextAction.SPEAK_AGAIN))
                .isEqualTo("Que ótima ideia. O que mais você percebe nessa cena?");
    }

    @Test void keepsOnlyOneQuestionWhenAnotherTurnIsExpected() {
        assertThat(ReplySafety.normalize(
                "Onde está a bola? Você viu a árvore?", NextAction.SPEAK_AGAIN))
                .isEqualTo("Onde está a bola?");
    }

    @Test void doesNotForceQuestionWhenSceneMustContinue() {
        assertThat(ReplySafety.normalize(
                "Você ajudou a terminar a história!", NextAction.CONTINUE))
                .isEqualTo("Você ajudou a terminar a história!");
    }
}
