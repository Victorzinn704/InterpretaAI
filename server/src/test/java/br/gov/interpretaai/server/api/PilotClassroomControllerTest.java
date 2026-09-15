package br.gov.interpretaai.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
        "spring.datasource.url=jdbc:h2:mem:pilot-classroom;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class PilotClassroomControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void cleanPilotState() {
        jdbc.update("delete from pilot_classroom_participant");
        jdbc.update("delete from pilot_classroom");
        jdbc.update("delete from pilot_assignment");
    }

    @Test void teacherCreatesRoomAndPublishesToOneOrEveryParticipant() throws Exception {
        create("turma-1a", roster())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomLabel").value("Turma 1A"))
                .andExpect(jsonPath("$.participants.length()").value(2));

        mvc.perform(get("/api/v1/pilot/classrooms/turma-1a")
                        .header("X-Teacher-Token", "teacher-secret-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].learnerAlias").value("pipa-07"))
                .andExpect(jsonPath("$.participants[1].learnerAlias").value("pipa-08"));

        publish("turma-1a", """
                {"learnerAliases":["pipa-08"],"activity":"DRAWING","drawingPrompt":"TREE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCount").value(1))
                .andExpect(jsonPath("$.assignments[0].deviceId").value("tablet-room-02"));

        deviceAssignment("tablet-room-01").andExpect(status().isNoContent());
        deviceAssignment("tablet-room-02")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerAlias").value("pipa-08"));

        publish("turma-1a", """
                {"learnerAliases":[],"activity":"COMIC","drawingPrompt":"BALL"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetCount").value(2));

        deviceAssignment("tablet-room-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerAlias").value("pipa-07"));
    }

    @Test void rejectsDuplicateRosterAndUnknownTarget() throws Exception {
        create("turma-duplicate", roster().replace("pipa-08", "pipa-07"))
                .andExpect(status().isBadRequest());

        create("turma-target", roster()).andExpect(status().isOk());
        publish("turma-target", """
                {"learnerAliases":["sol-99"],"activity":"COMIC","drawingPrompt":"BALL"}
                """).andExpect(status().isBadRequest());
        deviceAssignment("tablet-room-01").andExpect(status().isNoContent());
        deviceAssignment("tablet-room-02").andExpect(status().isNoContent());
    }

    @Test void rejectsIdentityFieldsAndRequiresTeacherCredential() throws Exception {
        create("turma-pii", roster().replace(
                "\"deviceId\":\"tablet-room-01\"",
                "\"deviceId\":\"tablet-room-01\",\"studentName\":\"Ana\""))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/pilot/classrooms/turma-1a")
                        .header("X-Teacher-Token", "device-secret-123456"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions create(String classroomId, String body)
            throws Exception {
        return mvc.perform(put("/api/v1/pilot/classrooms/{classroomId}", classroomId)
                .header("X-Teacher-Token", "teacher-secret-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions publish(String classroomId, String body)
            throws Exception {
        return mvc.perform(post("/api/v1/pilot/classrooms/{classroomId}/assignments", classroomId)
                .header("X-Teacher-Token", "teacher-secret-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions deviceAssignment(String deviceId)
            throws Exception {
        return mvc.perform(get("/api/v1/pilot/assignments/{deviceId}", deviceId)
                .header("X-Device-Token", "device-secret-123456"));
    }

    private String roster() {
        return """
                {"classroomLabel":"Turma 1A","participants":[
                  {"learnerAlias":"pipa-07","avatarId":"pipa","deviceId":"tablet-room-01"},
                  {"learnerAlias":"pipa-08","avatarId":"pipa","deviceId":"tablet-room-02"}
                ]}
                """;
    }
}
