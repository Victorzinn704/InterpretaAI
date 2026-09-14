package br.gov.interpretaai.server.core;

import static br.gov.interpretaai.server.api.VoiceTurnModels.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AdaptiveConversationRouterTest {
    @Test void exploresRoutesOnceThenPrefersLowerObservedLatency() {
        FakeProvider slower = new FakeProvider("slower", 20, false);
        FakeProvider faster = new FakeProvider("faster", 1, false);
        ConversationDeadline execution = new ConversationDeadline(200, 2);
        AdaptiveConversationRouter router = new AdaptiveConversationRouter(
                List.of(slower, faster), execution, new SimpleMeterRegistry(),
                "adaptive", "slower,faster", 200, 40, 50);

        router.reply(request(), List.of());
        router.reply(request(), List.of());
        router.reply(request(), List.of());

        assertThat(slower.calls).hasValue(1);
        assertThat(faster.calls).hasValue(2);
        assertThat(router.snapshots().get("faster").ewmaMs())
                .isLessThan(router.snapshots().get("slower").ewmaMs());
        execution.close();
    }

    @Test void failsOverOnlyWhenFailureWasFast() {
        FakeProvider broken = new FakeProvider("broken", 0, true);
        FakeProvider healthy = new FakeProvider("healthy", 0, false);
        ConversationDeadline execution = new ConversationDeadline(200, 2);
        AdaptiveConversationRouter router = new AdaptiveConversationRouter(
                List.of(broken, healthy), execution, new SimpleMeterRegistry(),
                "adaptive", "broken,healthy", 200, 40, 50);

        assertThat(router.reply(request(), List.of()).replyText()).isEqualTo("healthy");
        assertThat(broken.calls).hasValue(1);
        assertThat(healthy.calls).hasValue(1);
        execution.close();
    }

    @Test void doesNotStackAnotherProviderAfterSlowFailure() {
        FakeProvider slowBroken = new FakeProvider("slow-broken", 70, true);
        FakeProvider unused = new FakeProvider("unused", 0, false);
        ConversationDeadline execution = new ConversationDeadline(200, 2);
        AdaptiveConversationRouter router = new AdaptiveConversationRouter(
                List.of(slowBroken, unused), execution, new SimpleMeterRegistry(),
                "adaptive", "slow-broken,unused", 200, 40, 50);

        assertThatThrownBy(() -> router.reply(request(), List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(unused.calls).hasValue(0);
        assertThat(router.snapshots().get("slow-broken").eligible()).isFalse();

        assertThat(router.reply(request(), List.of()).replyText()).isEqualTo("unused");
        assertThat(slowBroken.calls).hasValue(1);
        assertThat(unused.calls).hasValue(1);
        execution.close();
    }

    @Test void cooldownProtectsNextTurnWhenNoAlternativeExists() {
        FakeProvider slowBroken = new FakeProvider("slow-broken", 70, true);
        ConversationDeadline execution = new ConversationDeadline(200, 2);
        AdaptiveConversationRouter router = new AdaptiveConversationRouter(
                List.of(slowBroken), execution, new SimpleMeterRegistry(),
                "adaptive", "slow-broken", 200, 40, 500);

        assertThatThrownBy(() -> router.reply(request(), List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> router.reply(request(), List.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(slowBroken.calls).hasValue(1);
        execution.close();
    }

    private Request request() {
        return new Request("session", "scene", 1, "Minha ideia",
                Speaker.LEIA_FEMALE, false);
    }

    private static final class FakeProvider implements RoutableConversationProvider {
        private final String id;
        private final long delayMs;
        private final boolean fails;
        private final AtomicInteger calls = new AtomicInteger();

        private FakeProvider(String id, long delayMs, boolean fails) {
            this.id = id;
            this.delayMs = delayMs;
            this.fails = fails;
        }

        @Override public String providerId() { return id; }
        @Override public boolean available() { return true; }

        @Override
        public PedagogicalReply reply(Request request, List<String> recentMessages) {
            calls.incrementAndGet();
            if (delayMs > 0) try {
                Thread.sleep(delayMs);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            if (fails) throw new IllegalStateException("offline");
            return new PedagogicalReply(id, VisualReaction.ENCOURAGE,
                    NextAction.SPEAK_AGAIN, "ORAL_EXPRESSION");
        }
    }
}
