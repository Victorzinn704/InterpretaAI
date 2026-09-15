package br.gov.interpretaai.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        "interpretaai.pilot-sync.enabled=true",
        "interpretaai.pilot-sync.teacher-token=teacher-secret-12345",
        "interpretaai.pilot-sync.device-token=device-secret-123456",
        "interpretaai.voice-auth.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:voice-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class VoiceTurnAuthenticationTest {
    @Autowired MockMvc mvc;

    @Test void rejectsVoiceTurnWithoutDeviceCredential() throws Exception {
        mvc.perform(post("/api/v1/voice-turn")
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
    }

    @Test void acceptsVoiceTurnFromConfiguredTablet() throws Exception {
        mvc.perform(post("/api/v1/voice-turn")
                        .header("X-Device-Token", "device-secret-123456")
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk());
    }

    private String body() {
        return """
                {"sessionId":"auth-session","sceneId":"scene","turn":1,"transcript":"Uma bola",
                 "speaker":"LEIA_FEMALE","reducedStimuli":false}
                """;
    }
}
