package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.identity.oidc-enabled=true",
        "interpretaai.identity.issuer-uri=https://identity.test.example",
        "interpretaai.identity.audience=interpretaai-api",
        "interpretaai.device-pairing.enabled=true",
        "interpretaai.device-pairing.secret=test-classroom-session-secret-with-more-than-32-characters",
        "interpretaai.device-pairing.max-attempts-per-minute=100",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:classroom-session;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class ClassroomSessionControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void seed() {
        jdbc.update("delete from classroom_session_device");
        jdbc.update("delete from classroom_session");
        jdbc.update("delete from institution_classroom_learner");
        jdbc.update("delete from institution_audit_event");
        jdbc.update("delete from institution_device");
        jdbc.update("delete from device_pairing_code");
        jdbc.update("delete from institution_teacher_classroom");
        jdbc.update("delete from institution_school_membership");
        jdbc.update("delete from institution_classroom");
        jdbc.update("delete from institution_adult_user");
        jdbc.update("delete from institution_school");
        jdbc.update("delete from institution_tenant");
        Instant now = Instant.parse("2026-09-18T12:00:00Z");
        jdbc.update("insert into institution_tenant values ('tenant_demo','Rede Demo','ACTIVE',?)",
                Timestamp.from(now));
        jdbc.update("insert into institution_school values ('school_demo','tenant_demo','Escola Demo','ACTIVE',?)",
                Timestamp.from(now));
        jdbc.update("insert into institution_adult_user values ('user_teacher','oidc|teacher','ACTIVE',?)",
                Timestamp.from(now));
        jdbc.update("""
                insert into institution_school_membership
                values ('user_teacher','school_demo','TEACHER','ACTIVE',?,?)
                """, Timestamp.from(now), Timestamp.from(now));
        for (String id : List.of("class_origin", "class_destination")) {
            jdbc.update("insert into institution_classroom values (?, 'school_demo', ?, 'ACTIVE', ?)",
                    id, id, Timestamp.from(now));
            jdbc.update("""
                    insert into institution_teacher_classroom
                    values ('user_teacher',?,'ACTIVE',?,?)
                    """, id, Timestamp.from(now), Timestamp.from(now));
        }
    }

    @Test
    void importsNStudentsAndLetsASchoolTabletAssumeASeatInAnotherClassroom() throws Exception {
        JsonNode credential = pairDeviceToOrigin();
        String deviceId = credential.get("deviceId").asText();
        String token = credential.get("deviceToken").asText();

        mvc.perform(teacher(put("/api/v2/classroom-management/classrooms/class_destination/roster")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"names\":[\"Ana Souza\",\"Bruno Lima\",\"Caio Reis\"]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learners.length()").value(3))
                .andExpect(jsonPath("$.learners[0].displayName").value("Ana Souza"))
                .andExpect(jsonPath("$.learners[0].seatNumber").value(1));

        JsonNode session = mapper.readTree(mvc.perform(teacher(post(
                "/api/v2/classroom-management/classrooms/class_destination/sessions")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.learnerCount").value(3))
                .andReturn().getResponse().getContentAsString());
        String code = session.get("joinCode").asText();
        String sessionId = session.get("sessionId").asText();

        JsonNode lobby = mapper.readTree(mvc.perform(device(post(
                "/api/v2/devices/{deviceId}/classroom-sessions/resolve", deviceId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learners.length()").value(3))
                .andExpect(jsonPath("$.learners[0].displayName").value("Ana Souza"))
                .andReturn().getResponse().getContentAsString());

        String learnerId = lobby.get("learners").get(0).get("learnerId").asText();
        mvc.perform(device(post("/api/v2/devices/{deviceId}/classroom-sessions/join", deviceId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"learnerId\":\"" + learnerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomId").value("class_destination"))
                .andExpect(jsonPath("$.seatNumber").value(1))
                .andExpect(jsonPath("$.learnerAlias").value("sol-001"));

        mvc.perform(device(get("/api/v2/devices/{deviceId}/context", deviceId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomId").value("class_destination"));

        mvc.perform(teacher(get("/api/v2/classroom-management/sessions/{sessionId}", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectedDevices").value(1))
                .andExpect(jsonPath("$.learnerCount").value(3))
                .andExpect(jsonPath("$.seats[0].connected").value(true))
                .andExpect(jsonPath("$.seats[0].deviceId").value(deviceId));

        mvc.perform(teacher(post(
                "/api/v2/classroom-management/sessions/{sessionId}/close", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mvc.perform(teacher(put("/api/v2/classroom-management/classrooms/class_destination/roster")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"names\":[\"Dora Luz\",\"Eli Mar\"]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learners.length()").value(2));

        mvc.perform(teacher(get("/api/v2/classroom-management/sessions/{sessionId}", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learnerCount").value(3))
                .andExpect(jsonPath("$.connectedDevices").value(1))
                .andExpect(jsonPath("$.seats[0].displayName").value("Ana Souza"));

        mvc.perform(teacher(get("/api/v2/classroom-management/classrooms/class_destination/roster")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.learners.length()").value(2))
                .andExpect(jsonPath("$.learners[0].displayName").value("Dora Luz"));

        assertThat(jdbc.queryForObject("select count(*) from institution_classroom_learner",
                Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_classroom_learner where status = 'ARCHIVED'
                """, Integer.class)).isEqualTo(3);
    }

    private JsonNode pairDeviceToOrigin() throws Exception {
        JsonNode code = mapper.readTree(mvc.perform(teacher(post("/api/v2/device-management/pairing-codes")
                .header("X-School-Id", "school_demo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"classroomId\":\"class_origin\"}")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        return mapper.readTree(mvc.perform(post("/api/v2/device-pairings/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","installationId":"installation-mobile-classroom-01",
                         "appVersion":22,"architecture":"ARM64",
                         "viewportWidthDp":800,"viewportHeightDp":1280}
                        """.formatted(code.get("code").asText())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder teacher(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(token -> token.subject("oidc|teacher")
                .audience(List.of("interpretaai-api"))));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder device(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
