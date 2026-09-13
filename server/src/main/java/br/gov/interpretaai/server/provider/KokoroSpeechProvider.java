package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import br.gov.interpretaai.server.core.SpeechProvider;
import br.gov.interpretaai.server.core.SpeechProvider.SpeechAudio;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "interpretaai.speech.provider", havingValue = "kokoro", matchIfMissing = true)
public class KokoroSpeechProvider implements SpeechProvider {
    private final URI endpoint;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public KokoroSpeechProvider(
            @Value("${interpretaai.kokoro.base-url:http://localhost:8091}") String baseUrl,
            ObjectMapper json) {
        this.endpoint = URI.create(baseUrl.replaceAll("/$", "") + "/synthesize");
        this.json = json;
    }

    @Override
    public SpeechAudio synthesize(String text, Speaker speaker) {
        try {
            String voice = speaker == Speaker.DAVI_MALE ? "pm_alex" : "pf_dora";
            String body = json.writeValueAsString(Map.of("text", text, "voice", voice));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length == 0) {
                throw new IllegalStateException("Kokoro respondeu " + response.statusCode());
            }
            return new SpeechAudio(response.body(), "audio/wav");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kokoro indisponível", error);
        } catch (Exception error) {
            throw new IllegalStateException("Kokoro indisponível", error);
        }
    }
}
