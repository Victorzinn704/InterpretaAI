package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class VoiceTurnIdempotency {
    private static final Logger log = LoggerFactory.getLogger(VoiceTurnIdempotency.class);
    private static final String KEY_PATTERN = "[A-Za-z0-9._:-]{8,80}";

    private final VoiceTurnStore store;
    private final ObjectMapper json;
    private final Clock clock;
    private final Duration processingTimeout;
    private final SecretKeySpec fingerprintKey;
    private final Cache<String, CachedTurn> completed;
    private final ConcurrentHashMap<String, CompletableFuture<Response>> inFlight = new ConcurrentHashMap<>();

    @Autowired
    public VoiceTurnIdempotency(
            VoiceTurnStore store,
            ObjectMapper json,
            @Value("${interpretaai.idempotency.ttl-minutes:10}") long ttlMinutes,
            @Value("${interpretaai.idempotency.processing-timeout-seconds:30}") long processingTimeoutSeconds,
            @Value("${interpretaai.idempotency.fingerprint-secret:local-development-only}") String fingerprintSecret) {
        this(store, json, Clock.systemUTC(), Duration.ofMinutes(ttlMinutes),
                Duration.ofSeconds(processingTimeoutSeconds), fingerprintSecret);
    }

    VoiceTurnIdempotency(VoiceTurnStore store, ObjectMapper json, Clock clock,
            Duration ttl, Duration processingTimeout) {
        this(store, json, clock, ttl, processingTimeout, "unit-test-secret");
    }

    VoiceTurnIdempotency(VoiceTurnStore store, ObjectMapper json, Clock clock,
            Duration ttl, Duration processingTimeout, String fingerprintSecret) {
        this.store = store;
        this.json = json;
        this.clock = clock;
        this.processingTimeout = processingTimeout;
        if (fingerprintSecret == null || fingerprintSecret.length() < 16) {
            throw new IllegalArgumentException("fingerprint secret deve ter ao menos 16 caracteres");
        }
        if ("local-development-only".equals(fingerprintSecret)) {
            log.warn("idempotency_fingerprint_secret uses_development_default=true");
        }
        this.fingerprintKey = new SecretKeySpec(
                fingerprintSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.completed = Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(ttl)
                .build();
    }

    public Response execute(String key, Request request, Supplier<Response> operation) {
        if (key == null || key.isBlank()) return operation.get();
        if (!key.matches(KEY_PATTERN)) throw new InvalidIdempotencyKeyException();
        String fingerprint = fingerprint(request);

        CachedTurn cached = completed.getIfPresent(key);
        if (cached != null) {
            if (!cached.fingerprint().equals(fingerprint)) throw new IdempotencyConflictException();
            log.info("voice_turn_replay source=memory");
            return cached.response();
        }

        AtomicBoolean owner = new AtomicBoolean(false);
        CompletableFuture<Response> future = inFlight.computeIfAbsent(key, ignored -> {
            owner.set(true);
            return new CompletableFuture<>();
        });
        if (!owner.get()) return awaitExisting(future);

        try {
            Response replay;
            try {
                replay = findReplayOrClaim(key, fingerprint);
            } catch (DataAccessException persistenceUnavailable) {
                log.warn("voice_turn_idempotency_bypassed reason=database_unavailable");
                Response response = operation.get();
                completed.put(key, new CachedTurn(fingerprint, response));
                future.complete(response);
                return response;
            }
            if (replay != null) {
                completed.put(key, new CachedTurn(fingerprint, replay));
                future.complete(replay);
                return replay;
            }
            Response response = operation.get();
            String responseJson = json.writeValueAsString(response);
            completed.put(key, new CachedTurn(fingerprint, response));
            try {
                store.complete(key, responseJson, clock.instant());
            } catch (RuntimeException persistenceError) {
                safeFail(key, fingerprint);
                log.warn("voice_turn_persistence_failed");
            }
            future.complete(response);
            return response;
        } catch (RuntimeException | JsonProcessingException error) {
            safeFail(key, fingerprint);
            future.completeExceptionally(error);
            throw error instanceof RuntimeException runtime
                    ? runtime : new IllegalStateException("Falha ao persistir turno", error);
        } finally {
            inFlight.remove(key, future);
        }
    }

    private void safeFail(String key, String fingerprint) {
        try {
            store.fail(key, fingerprint, clock.instant());
        } catch (RuntimeException persistenceError) {
            log.warn("voice_turn_failure_state_not_persisted");
        }
    }

    private Response findReplayOrClaim(String key, String fingerprint) {
        var existing = store.find(key);
        if (existing.isPresent()) {
            var turn = existing.get();
            if (!turn.fingerprint().equals(fingerprint)) throw new IdempotencyConflictException();
            if ("COMPLETED".equals(turn.status()) && turn.responseJson() != null) {
                try {
                    Response response = json.readValue(turn.responseJson(), Response.class);
                    log.info("voice_turn_replay source=database");
                    return response;
                } catch (JsonProcessingException error) {
                    throw new IllegalStateException("Resposta idempotente inválida", error);
                }
            }
        }
        if (!store.claim(key, fingerprint, clock.instant(), processingTimeout)) {
            existing = store.find(key);
            if (existing.isPresent() && !existing.get().fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException();
            }
            throw new TurnStillProcessingException();
        }
        return null;
    }

    private Response awaitExisting(CompletableFuture<Response> future) {
        try {
            return future.get(6, TimeUnit.SECONDS);
        } catch (Exception error) {
            throw new TurnStillProcessingException();
        }
    }

    private String fingerprint(Request request) {
        String value = String.join("|", request.sessionId(), request.sceneId(),
                Integer.toString(request.turn()), request.speaker().name(),
                Boolean.toString(request.reducedStimuli()), request.transcript());
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(fingerprintKey);
            return HexFormat.of().formatHex(hmac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("HmacSHA256 indisponível", impossible);
        }
    }

    public static final class InvalidIdempotencyKeyException extends RuntimeException {}
    public static final class IdempotencyConflictException extends RuntimeException {}
    public static final class TurnStillProcessingException extends RuntimeException {}

    private record CachedTurn(String fingerprint, Response response) {}
}
