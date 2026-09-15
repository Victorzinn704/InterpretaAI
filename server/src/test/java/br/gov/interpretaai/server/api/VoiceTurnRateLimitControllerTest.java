package br.gov.interpretaai.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "interpretaai.voice-rate-limit.enabled=true",
        "interpretaai.voice-rate-limit.max-requests=1",
        "spring.datasource.url=jdbc:h2:mem:voice-rate-limit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class VoiceTurnRateLimitControllerTest {
    @Autowired MockMvc mvc;

    @Test void returnsRetryable429BeforeASecondInferenceInTheSameSession() throws Exception {
        mvc.perform(post("/api/v1/voice-turn").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/voice-turn").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.title").value("voice_rate_limited"));
    }

    private String body() {
        return """
                {"sessionId":"limited-session","sceneId":"scene","turn":1,"transcript":"Uma bola",
                 "speaker":"LEIA_FEMALE","reducedStimuli":false}
                """;
    }
}
