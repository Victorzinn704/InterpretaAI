package br.gov.interpretaai.server.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:adult-identity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AdultIdentityControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void seedInstitution() {
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
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values ('school_centro', 'tenant_rio', 'Escola Centro', 'ACTIVE', ?)
                """, Timestamp.from(now));
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('user_teacher', 'oidc|teacher', 'ACTIVE', ?)
                """, Timestamp.from(now));
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values ('user_teacher', 'school_centro', 'TEACHER', 'ACTIVE', ?, ?)
                """, Timestamp.from(now), Timestamp.from(now));
    }

    @Test
    void requiresJwtForEveryV2Route() throws Exception {
        mvc.perform(get("/api/v2/identity/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("adult_authentication_required"));
    }

    @Test
    void returnsOnlyDatabaseBackedSchoolContexts() throws Exception {
        mvc.perform(get("/api/v2/identity/me").with(jwt().jwt(token -> token
                        .subject("oidc|teacher")
                        .audience(List.of("interpretaai-api")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user_teacher"))
                .andExpect(jsonPath("$.schools.length()").value(1))
                .andExpect(jsonPath("$.schools[0].schoolId").value("school_centro"))
                .andExpect(jsonPath("$.schools[0].role").value("TEACHER"));
    }

    @Test
    void authenticatedSubjectWithoutActiveMembershipIsDenied() throws Exception {
        mvc.perform(get("/api/v2/identity/me").with(jwt().jwt(token -> token
                        .subject("oidc|unknown")
                        .audience(List.of("interpretaai-api")))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("institutional_access_denied"));
    }

    @Test
    void revocationTakesEffectOnTheNextRequest() throws Exception {
        jdbc.update("""
                update institution_school_membership
                   set status = 'REVOKED', updated_at = ?
                 where user_id = 'user_teacher' and school_id = 'school_centro'
                """, Timestamp.from(Instant.parse("2026-09-16T13:00:00Z")));

        mvc.perform(get("/api/v2/identity/me").with(jwt().jwt(token -> token
                        .subject("oidc|teacher")
                        .audience(List.of("interpretaai-api")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void legacyPilotRoutesRemainOutsideTheAdultOidcChain() throws Exception {
        mvc.perform(get("/api/v1/gateway/status"))
                .andExpect(status().isOk());
    }
}
