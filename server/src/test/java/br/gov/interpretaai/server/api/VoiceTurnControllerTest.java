package br.gov.interpretaai.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:voice-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class VoiceTurnControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

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

    @Test void replaysAnIdempotentTurnAndPersistsOnlyOneRecord() throws Exception {
        String body = """
                {"sessionId":"idempotency-session","sceneId":"scene","turn":1,
                 "transcript":"Vi uma bola","speaker":"LEIA_FEMALE","reducedStimuli":false}
                """;
        for (int attempt = 0; attempt < 2; attempt++) {
            mvc.perform(post("/api/v1/voice-turn")
                            .header("Idempotency-Key", "turn-test-0001")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.replyText")
                            .value("Gostei da sua ideia! O que mais você percebe nessa cena?"));
        }
        Integer rows = jdbc.queryForObject("""
                select count(*) from voice_turn_idempotency where idempotency_key = 'turn-test-0001'
                """, Integer.class);
        Integer events = jdbc.queryForObject("""
                select count(*) from operational_outbox where event_type = 'VOICE_TURN_COMPLETED'
                """, Integer.class);
        org.assertj.core.api.Assertions.assertThat(rows).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(events).isGreaterThanOrEqualTo(1);
    }

    @Test void rejectsIdempotencyKeyReusedForAnotherStage() throws Exception {
        String first = """
                {"sessionId":"conflict-session","sceneId":"scene-a","turn":1,
                 "transcript":"Uma bola","speaker":"LEIA_FEMALE","reducedStimuli":false}
                """;
        String second = first.replace("scene-a", "scene-b");
        mvc.perform(post("/api/v1/voice-turn").header("Idempotency-Key", "turn-test-0002")
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/voice-turn").header("Idempotency-Key", "turn-test-0002")
                        .contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("idempotency_conflict"));
    }

    @Test void rejectsIdempotencyKeyReusedWithAnotherTranscript() throws Exception {
        String first = """
                {"sessionId":"transcript-session","sceneId":"scene","turn":1,
                 "transcript":"Uma bola","speaker":"LEIA_FEMALE","reducedStimuli":false}
                """;
        String second = first.replace("Uma bola", "Uma árvore");
        mvc.perform(post("/api/v1/voice-turn").header("Idempotency-Key", "turn-test-0003")
                        .contentType(MediaType.APPLICATION_JSON).content(first))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/voice-turn").header("Idempotency-Key", "turn-test-0003")
                        .contentType(MediaType.APPLICATION_JSON).content(second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("idempotency_conflict"));
    }

    @Test void streamsAckValidatedTextAndCompleteResponseInOrder() throws Exception {
        MvcResult pending = mvc.perform(post("/api/v1/voice-turn/stream")
                        .header("Idempotency-Key", "turn-stream-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_NDJSON)
                        .content("""
                                {"sessionId":"stream-session","sceneId":"scene","turn":1,
                                 "transcript":"Vi uma bola","speaker":"LEIA_FEMALE","reducedStimuli":false}
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult completed = mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andReturn();
        String[] lines = completed.getResponse().getContentAsString().trim().split("\\R");

        org.assertj.core.api.Assertions.assertThat(lines).hasSize(3);
        org.assertj.core.api.Assertions.assertThat(lines[0]).contains("\"type\":\"ACK\"");
        org.assertj.core.api.Assertions.assertThat(lines[1]).contains("\"type\":\"FINAL_TEXT\"");
        org.assertj.core.api.Assertions.assertThat(lines[2]).contains("\"type\":\"COMPLETE\"");
        var ack = new com.fasterxml.jackson.databind.ObjectMapper().readTree(lines[0]);
        var text = new com.fasterxml.jackson.databind.ObjectMapper().readTree(lines[1]);
        var complete = new com.fasterxml.jackson.databind.ObjectMapper().readTree(lines[2]);
        org.assertj.core.api.Assertions.assertThat(ack.path("protocolVersion").asInt()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(ack.path("serverElapsedMs").asLong()).isGreaterThanOrEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(text.path("serverElapsedMs").asLong())
                .isGreaterThanOrEqualTo(ack.path("serverElapsedMs").asLong());
        org.assertj.core.api.Assertions.assertThat(complete.path("serverElapsedMs").asLong())
                .isGreaterThanOrEqualTo(text.path("serverElapsedMs").asLong());
    }
}
