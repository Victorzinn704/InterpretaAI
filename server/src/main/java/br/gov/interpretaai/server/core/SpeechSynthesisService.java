package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import br.gov.interpretaai.server.core.SpeechProvider.SpeechAudio;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Coalesce sínteses iguais e limita o cache pelo peso real do áudio. */
@Component
public class SpeechSynthesisService {
    private final SpeechProvider delegate;
    private final Cache<AudioKey, SpeechAudio> audioCache;

    @Autowired
    public SpeechSynthesisService(
            SpeechProvider delegate,
            @Value("${interpretaai.speech.cache-max-bytes:33554432}") long maximumBytes,
            @Value("${interpretaai.speech.cache-ttl-minutes:10}") long ttlMinutes) {
        if (maximumBytes < 1 || ttlMinutes < 1) {
            throw new IllegalArgumentException("Limites do cache de voz devem ser positivos");
        }
        this.delegate = delegate;
        this.audioCache = Caffeine.newBuilder()
                .maximumWeight(maximumBytes)
                .weigher((AudioKey ignored, SpeechAudio audio) ->
                        Math.max(1, Math.min(Integer.MAX_VALUE, audio.content().length)))
                .expireAfterAccess(Duration.ofMinutes(ttlMinutes))
                .build();
    }

    SpeechSynthesisService(SpeechProvider delegate) {
        this(delegate, 32L * 1024 * 1024, 10);
    }

    public SpeechAudio synthesize(String text, Speaker speaker) {
        AudioKey key = new AudioKey(speaker, sha256(text));
        SpeechAudio audio = audioCache.get(key, ignored -> delegate.synthesize(text, speaker));
        if (audio.content().length == 0) audioCache.invalidate(key);
        return audio;
    }

    long estimatedEntries() {
        return audioCache.estimatedSize();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 indisponível", impossible);
        }
    }

    private record AudioKey(Speaker speaker, String textDigest) {}
}
