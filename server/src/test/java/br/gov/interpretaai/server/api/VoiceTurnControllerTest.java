package br.gov.interpretaai.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class VoiceTurnControllerTest {
    @Autowired MockMvc mvc;

    @Test void rejectsTranscriptLongerThanContract() throws Exception {
        String transcript = "a".repeat(281);
        mvc.perform(post("/api/v1/voice-turn").contentType(MediaType.APPLICATION_JSON).content("""
                {"sessionId":"s","sceneId":"scene","turn":1,"transcript":"%s",
                 "speaker":"LEIA_FEMALE","reducedStimuli":false}
                """.formatted(transcript))).andExpect(status().isBadRequest());
    }

    @Test void servesFallbackContractWithoutCloudCredentials() throws Exception {
        mvc.perform(post("/api/v1/voice-turn").contentType(MediaType.APPLICATION_JSON).content("""
                {"sessionId":"s","sceneId":"scene","turn":1,"transcript":"Vi uma bola",
                 "speaker":"LEIA_FEMALE","reducedStimuli":false}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speaker").value("LEIA_FEMALE"))
                .andExpect(jsonPath("$.visualReaction").value("ENCOURAGE"))
                .andExpect(jsonPath("$.degraded").value(true));
    }
}
