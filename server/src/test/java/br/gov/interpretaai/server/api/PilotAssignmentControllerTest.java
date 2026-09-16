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
                .andExpect(jsonPath("$.learnerAlias").value("pipa-07"))
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

    @Test void rejectsANameLikeAliasAndAcceptsOnlyAClosedPseudonym() throws Exception {
        mvc.perform(put("/api/v1/pilot/assignments/device-tablet-04")
                        .header("X-Teacher-Token", "teacher-secret-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("COMIC", "BALL").replace("pipa-07", "ana-07")))
                .andExpect(status().isBadRequest());
    }

    @Test void acceptsThePreviousApkDuringRollingDeployment() throws Exception {
        mvc.perform(put("/api/v1/pilot/assignments/device-legacy-08")
                        .header("X-Teacher-Token", "teacher-secret-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("COMIC", "BALL")
                                .replace(",\"learnerAlias\":\"pipa-07\"", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerAlias").value("pipa-01"));
    }

    @Test void publishesSecondThroughFifthYearReadingPacks() throws Exception {
        publish("device-tablet-02year", "STORY_SEQUENCE_2", "BALL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value("STORY_SEQUENCE_2"));
        publish("device-tablet-03year", "CAUSE_AND_EFFECT_3", "BALL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value("CAUSE_AND_EFFECT_3"));
        publish("device-tablet-04year", "FACT_OR_OPINION_4", "BALL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value("FACT_OR_OPINION_4"));
        publish("device-tablet-05year", "COMPARE_SOURCES_5", "BALL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activity").value("COMPARE_SOURCES_5"));
    }

    @Test void teacherCanDispatchEachSmallOfflineGameToATablet() throws Exception {
        String[] games = {"NUMBER_PATH", "CONNECT_DOTS", "IMAGE_LETTERS"};
        for (int i = 0; i < games.length; i++) {
            String deviceId = "device-mini-game-" + i;
            publish(deviceId, games[i], "BALL")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activity").value(games[i]));
            mvc.perform(get("/api/v1/pilot/assignments/{deviceId}", deviceId)
                            .header("X-Device-Token", "device-secret-123456"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activity").value(games[i]));
        }
    }

    @Test void rejectsAnAliasThatDoesNotMatchTheVisibleAvatar() throws Exception {
        mvc.perform(put("/api/v1/pilot/assignments/device-tablet-05")
                        .header("X-Teacher-Token", "teacher-secret-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("COMIC", "BALL").replace("pipa-07", "sol-07")))
                .andExpect(status().isBadRequest());
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
                {"classroomLabel":"Turma 1A","avatarId":"pipa","learnerAlias":"pipa-07",
                 "activity":"%s","drawingPrompt":"%s"}
                """.formatted(activity, drawingPrompt);
    }
}
