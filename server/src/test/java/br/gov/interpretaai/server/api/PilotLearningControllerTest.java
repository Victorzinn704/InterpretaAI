package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "interpretaai.pilot-sync.enabled=true",
        "interpretaai.pilot-sync.teacher-token=teacher-secret-12345",
        "interpretaai.pilot-sync.device-token=device-secret-123456",
        "interpretaai.pilot-sync.secretary-token=secretary-secret-123",
        "spring.datasource.url=jdbc:h2:mem:pilot-learning;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class PilotLearningControllerTest {
    private static final String TEACHER_TOKEN = "teacher-" + "secret-12345";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void cleanPilotState() {
        jdbc.update("delete from pilot_administrative_access_audit");
        jdbc.update("delete from pilot_learning_event");
        jdbc.update("delete from pilot_classroom_participant");
        jdbc.update("delete from pilot_classroom");
        jdbc.update("delete from pilot_assignment");
    }

    @Test void receivesIdempotentNeutralEventsAndSeparatesTeacherFromSecretary() throws Exception {
        createClassroom();
        String batch = batch(Instant.now().toString());

        ingest(batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(3))
                .andExpect(jsonPath("$.duplicates").value(0));
        ingest(batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.duplicates").value(3));

        var teacher = mvc.perform(get("/api/v1/pilot/classrooms/turma-1a/summary")
                        .header("X-Teacher-Token", TEACHER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants").value(1))
                .andExpect(jsonPath("$.sessions").value(1))
                .andExpect(jsonPath("$.participations").value(1))
                .andExpect(jsonPath("$.helpRequests").value(1))
                .andExpect(jsonPath("$.voiceResponses").value(1))
                .andExpect(jsonPath("$.averageResponseMs").value(1200))
                .andReturn().getResponse().getContentAsString();
        assertThat(teacher).doesNotContain("pipa-07", "tablet-room-01", "avatarId");

        mvc.perform(get("/api/v1/pilot/secretariat/summary")
                        .header("X-Secretary-Token", TEACHER_TOKEN))
                .andExpect(status().isUnauthorized());
        var secretary = mvc.perform(get("/api/v1/pilot/secretariat/summary")
                        .header("X-Secretary-Token", "secretary-secret-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classrooms").value(1))
                .andExpect(jsonPath("$.participants").value(1))
                .andExpect(jsonPath("$.classroomSummaries[0].participations").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(secretary).doesNotContain("pipa-07", "tablet-room-01", "avatarId");

        Integer audits = jdbc.queryForObject(
                "select count(*) from pilot_administrative_access_audit", Integer.class);
        assertThat(audits).isEqualTo(2);
    }

    @Test void rejectsUnknownDeviceFreeTextAndInvalidTime() throws Exception {
        createClassroom();
        ingest(batch(Instant.now().toString()).replace("tablet-room-01", "tablet-unknown"))
                .andExpect(status().isNotFound());
        ingest(batch(Instant.now().toString()).replace(
                "\"durationMs\":1200", "\"durationMs\":1200,\"transcript\":\"fala\""))
                .andExpect(status().isBadRequest());
        ingest(batch(Instant.now().minusSeconds(31L * 24 * 60 * 60).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test void countsAssistedAdvanceWithoutTreatingItAsCompletedMastery() throws Exception {
        createClassroom();
        String occurredAt = Instant.now().toString();
        ingest("""
                {"deviceId":"tablet-room-01","events":[
                  {"eventId":"event-support-0001","activityId":"image-letters",
                   "type":"STAGE_ADVANCED_WITH_SUPPORT","modality":"TOUCH",
                   "observationCategory":"PARTICIPATION","occurredAt":"%s"}
                ]}
                """.formatted(occurredAt)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/pilot/classrooms/turma-1a/summary")
                        .header("X-Teacher-Token", TEACHER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistedAdvances").value(1))
                .andExpect(jsonPath("$.completedStages").value(0));
    }

    @Test void sharedTabletEventIsCollectiveAndNeverAssignedToOneChild() throws Exception {
        mvc.perform(put("/api/v1/pilot/classrooms/turma-grupo")
                        .header("X-Teacher-Token", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classroomLabel":"Turma Grupo","participants":[
                                  {"learnerAlias":"pipa-07","avatarId":"pipa","deviceId":"tablet-group-01"},
                                  {"learnerAlias":"sol-08","avatarId":"sol","deviceId":"tablet-group-01"}
                                ]}
                                """))
                .andExpect(status().isOk());

        ingest(batch(Instant.now().toString())
                .replace("tablet-room-01", "tablet-group-01")
                .replace("event-session-0001", "event-group-00001")
                .replace("event-response-0001", "event-group-00002")
                .replace("event-help-000001", "event-group-00003"))
                .andExpect(status().isOk());

        var attribution = jdbc.queryForMap("""
                select learner_alias, participation_scope, participant_count
                  from pilot_learning_event where event_id = 'event-group-00002'
                """);
        assertThat(attribution.get("learner_alias")).isNull();
        assertThat(attribution.get("participation_scope")).isEqualTo("GROUP");
        assertThat(attribution.get("participant_count")).isEqualTo(2);
    }

    private void createClassroom() throws Exception {
        mvc.perform(put("/api/v1/pilot/classrooms/turma-1a")
                        .header("X-Teacher-Token", TEACHER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"classroomLabel":"Turma 1A","participants":[
                                  {"learnerAlias":"pipa-07","avatarId":"pipa","deviceId":"tablet-room-01"}
                                ]}
                                """))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions ingest(String body) throws Exception {
        return mvc.perform(post("/api/v1/pilot/learning-events:batch")
                .header("X-Device-Token", "device-secret-123456")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String batch(String occurredAt) {
        return """
                {"deviceId":"tablet-room-01","events":[
                  {"eventId":"event-session-0001","activityId":"comic-ball","type":"SESSION_STARTED",
                   "modality":"NONE","observationCategory":"NONE","occurredAt":"%1$s"},
                  {"eventId":"event-response-0001","activityId":"comic-ball","type":"RESPONSE_SUBMITTED",
                   "modality":"VOICE","durationMs":1200,"observationCategory":"ORAL_EXPRESSION","occurredAt":"%1$s"},
                  {"eventId":"event-help-000001","activityId":"comic-ball","type":"HELP_REQUESTED",
                   "modality":"TOUCH","observationCategory":"PARTICIPATION","occurredAt":"%1$s"}
                ]}
                """.formatted(occurredAt);
    }
}
