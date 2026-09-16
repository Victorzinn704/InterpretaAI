package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.interpretaai.server.device.DevicePairingException;
import br.gov.interpretaai.server.device.DevicePairingService;
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
        "interpretaai.device-pairing.secret=test-device-pairing-secret-with-more-than-32-characters",
        "interpretaai.device-pairing.max-attempts-per-minute=100",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:device-pairing;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class DevicePairingControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DevicePairingService pairing;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void seedInstitution() {
        jdbc.update("delete from institution_audit_event");
        jdbc.update("delete from institution_device");
        jdbc.update("delete from device_pairing_code");
        jdbc.update("delete from institution_teacher_classroom");
        jdbc.update("delete from institution_school_membership");
        jdbc.update("delete from institution_classroom");
        jdbc.update("delete from institution_adult_user");
        jdbc.update("delete from institution_school");
        jdbc.update("delete from institution_tenant");
        Instant now = Instant.parse("2026-09-16T12:00:00Z");
        jdbc.update("""
                insert into institution_tenant(tenant_id, name, status, created_at)
                values ('tenant_rio', 'Rede Rio', 'ACTIVE', ?)
                """, Timestamp.from(now));
        school("school_centro", now);
        school("school_norte", now);
        classroom("class_1a", "school_centro", now);
        classroom("class_2b", "school_centro", now);
        classroom("class_3c", "school_norte", now);
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('user_teacher', 'oidc|teacher', 'ACTIVE', ?)
                """, Timestamp.from(now));
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values ('user_teacher', 'school_centro', 'TEACHER', 'ACTIVE', ?, ?)
                """, Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into institution_teacher_classroom
                (user_id, classroom_id, status, created_at, updated_at)
                values ('user_teacher', 'class_1a', 'ACTIVE', ?, ?)
                """, Timestamp.from(now), Timestamp.from(now));
    }

    @Test
    void pairsOnceReturnsTheSecretOnceAndRevokesImmediately() throws Exception {
        JsonNode code = createCode("school_centro", "class_1a")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern(
                        "[23456789A-HJ-NP-Z]{4}-[23456789A-HJ-NP-Z]{4}")))
                .andReturn().getResponse().getContentAsString().transform(this::readTree);

        JsonNode credential = redeem(code.get("code").asText(), "installation-tablet-0001")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceToken").isString())
                .andReturn().getResponse().getContentAsString().transform(this::readTree);
        String deviceId = credential.get("deviceId").asText();
        String token = credential.get("deviceToken").asText();

        assertThat(jdbc.queryForObject(
                "select code_hash from device_pairing_code where pairing_id = ?",
                String.class, code.get("pairingId").asText()))
                .doesNotContain(code.get("code").asText().replace("-", ""));
        assertThat(jdbc.queryForObject(
                "select credential_hash from institution_device where device_id = ?",
                String.class, deviceId)).isNotEqualTo(token);
        assertThat(pairing.authenticate(deviceId, token).classroomId()).isEqualTo("class_1a");
        deviceContext(deviceId, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value(deviceId))
                .andExpect(jsonPath("$.schoolId").value("school_centro"))
                .andExpect(jsonPath("$.classroomId").value("class_1a"));
        deviceContext("device_another", token)
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v2/devices/{deviceId}/context", deviceId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("device_credential_invalid"));

        redeem(code.get("code").asText(), "installation-tablet-0002")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("pairing_code_invalid"));

        revoke(deviceId).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));
        revoke(deviceId).andExpect(status().isOk());
        assertThatThrownBy(() -> pairing.authenticate(deviceId, token))
                .isInstanceOf(DevicePairingException.class)
                .extracting("code")
                .isEqualTo("device_credential_invalid");
        deviceContext(deviceId, token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("device_credential_invalid"));
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_audit_event
                 where target_id = ? and action = 'DEVICE_REVOKED'
                """, Integer.class, deviceId)).isEqualTo(1);
    }

    @Test
    void teacherCannotPairAnUnlinkedOrCrossSchoolClassroom() throws Exception {
        createCode("school_centro", "class_2b")
                .andExpect(status().isForbidden());
        createCode("school_norte", "class_3c")
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredAndUnknownCodesShareTheSameSafeResponse() throws Exception {
        JsonNode code = readTree(createCode("school_centro", "class_1a")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        jdbc.update("update device_pairing_code set expires_at = ? where pairing_id = ?",
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                code.get("pairingId").asText());

        redeem(code.get("code").asText(), "installation-tablet-0003")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("pairing_code_invalid"));
        redeem("2345-6789", "installation-tablet-0004")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("pairing_code_invalid"));
    }

    @Test
    void duplicateInstallationRollsBackCodeConsumption() throws Exception {
        JsonNode first = readTree(createCode("school_centro", "class_1a")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        redeem(first.get("code").asText(), "installation-tablet-0005")
                .andExpect(status().isCreated());

        JsonNode second = readTree(createCode("school_centro", "class_1a")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        redeem(second.get("code").asText(), "installation-tablet-0005")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("installation_already_paired"));
        assertThat(jdbc.queryForObject(
                "select status from device_pairing_code where pairing_id = ?",
                String.class, second.get("pairingId").asText())).isEqualTo("ACTIVE");

        redeem(second.get("code").asText(), "installation-tablet-0006")
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions createCode(
            String schoolId, String classroomId) throws Exception {
        return mvc.perform(post("/api/v2/device-management/pairing-codes")
                .with(jwt().jwt(token -> token
                        .subject("oidc|teacher")
                        .audience(List.of("interpretaai-api"))))
                .header("X-School-Id", schoolId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"classroomId\":\"" + classroomId + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions redeem(
            String code, String installationId) throws Exception {
        return mvc.perform(post("/api/v2/device-pairings/redeem")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","installationId":"%s","appVersion":21,
                         "architecture":"ARM64","viewportWidthDp":800,"viewportHeightDp":1280}
                        """.formatted(code, installationId)));
    }

    private org.springframework.test.web.servlet.ResultActions revoke(String deviceId)
            throws Exception {
        return mvc.perform(post(
                        "/api/v2/device-management/devices/{deviceId}/revoke", deviceId)
                .with(jwt().jwt(token -> token
                        .subject("oidc|teacher")
                        .audience(List.of("interpretaai-api"))))
                .header("X-School-Id", "school_centro")
                .header("Idempotency-Key", "revoke-device-0001"));
    }

    private org.springframework.test.web.servlet.ResultActions deviceContext(
            String deviceId, String token) throws Exception {
        return mvc.perform(get("/api/v2/devices/{deviceId}/context", deviceId)
                .header("Authorization", "Bearer " + token));
    }

    private JsonNode readTree(String value) {
        try {
            return mapper.readTree(value);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private void school(String schoolId, Instant now) {
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values (?, 'tenant_rio', ?, 'ACTIVE', ?)
                """, schoolId, schoolId, Timestamp.from(now));
    }

    private void classroom(String classroomId, String schoolId, Instant now) {
        jdbc.update("""
                insert into institution_classroom(classroom_id, school_id, name, status, created_at)
                values (?, ?, ?, 'ACTIVE', ?)
                """, classroomId, schoolId, classroomId, Timestamp.from(now));
    }
}
