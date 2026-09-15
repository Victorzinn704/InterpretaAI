package br.gov.interpretaai.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        "interpretaai.pilot-sync.enabled=true",
        "interpretaai.pilot-sync.teacher-token=teacher-secret-12345",
        "interpretaai.pilot-sync.device-token=device-secret-123456",
        "spring.datasource.url=jdbc:h2:mem:pilot-assignment;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class PilotAssignmentControllerTest {
    @Autowired MockMvc mvc;

    @Test void teacherPublishesAndDeviceReadsOnlyLatestVersion() throws Exception {
        publish("device-tablet-01", "COMIC", "BALL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        publish("device-tablet-01", "DRAWING", "TREE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mvc.perform(get("/api/v1/pilot/assignments/device-tablet-01")
                        .header("X-Device-Token", "device-secret-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomLabel").value("Turma 1A"))
                .andExpect(jsonPath("$.avatarId").value("pipa"))
                .andExpect(jsonPath("$.activity").value("DRAWING"))
                .andExpect(jsonPath("$.drawingPrompt").value("TREE"))
                .andExpect(jsonPath("$.version").value(2));

        mvc.perform(get("/api/v1/pilot/assignments/device-tablet-01?afterVersion=2")
                        .header("X-Device-Token", "device-secret-123456"))
                .andExpect(status().isNoContent());
    }

    @Test void rejectsWrongRoleTokens() throws Exception {
        mvc.perform(put("/api/v1/pilot/assignments/device-tablet-02")
                        .header("X-Teacher-Token", "device-secret-123456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("COMIC", "BALL")))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/pilot/assignments/device-tablet-01")
                        .header("X-Device-Token", "teacher-secret-12345"))
                .andExpect(status().isUnauthorized());
    }

    @Test void rejectsIdentityAndFreeTextFields() throws Exception {
        mvc.perform(put("/api/v1/pilot/assignments/device-tablet-03")
                        .header("X-Teacher-Token", "teacher-secret-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("COMIC", "BALL").replace("}", ",\"studentName\":\"Ana\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test void returnsNoContentWhenDeviceHasNoAssignment() throws Exception {
        mvc.perform(get("/api/v1/pilot/assignments/device-without-assignment")
                        .header("X-Device-Token", "device-secret-123456"))
                .andExpect(status().isNoContent());
    }

    private org.springframework.test.web.servlet.ResultActions publish(
            String deviceId, String activity, String drawingPrompt) throws Exception {
        return mvc.perform(put("/api/v1/pilot/assignments/{deviceId}", deviceId)
                .header("X-Teacher-Token", "teacher-secret-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody(activity, drawingPrompt)));
    }

    private String validBody(String activity, String drawingPrompt) {
        return """
                {"classroomLabel":"Turma 1A","avatarId":"pipa",
                 "activity":"%s","drawingPrompt":"%s"}
                """.formatted(activity, drawingPrompt);
    }
}
