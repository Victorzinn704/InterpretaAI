package br.gov.interpretaai.server.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class InstitutionalAccessServiceTest {
    private JdbcTemplate jdbc;
    private InstitutionalAccessService service;

    @BeforeEach
    void prepareDatabase() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        service = new InstitutionalAccessService(new InstitutionalAccessStore(jdbc));

        tenant("tenant_rio");
        school("school_centro", "tenant_rio");
        school("school_norte", "tenant_rio");
        classroom("class_1a", "school_centro");
        classroom("class_2b", "school_centro");
        classroom("class_3c", "school_norte");
        user("user_teacher", "oidc|teacher");
        user("user_coordinator", "oidc|coordinator");
        user("user_admin", "oidc|admin");
        membership("user_teacher", "school_centro", "TEACHER");
        membership("user_coordinator", "school_centro", "COORDINATOR");
        membership("user_admin", "school_centro", "SCHOOL_ADMIN");
        linkTeacher("user_teacher", "class_1a");
    }

    @Test
    void teacherCanUploadOnlyInsideAnActiveSchoolMembership() {
        var grant = service.requireSchoolAction(
                "oidc|teacher", "school_centro", InstitutionAction.UPLOAD_MEDIA);

        assertThat(grant.userId()).isEqualTo("user_teacher");
        assertThat(grant.schoolId()).isEqualTo("school_centro");
        assertThat(grant.role()).isEqualTo(InstitutionRole.TEACHER);
        assertDenied(() -> service.requireSchoolAction(
                "oidc|teacher", "school_norte", InstitutionAction.UPLOAD_MEDIA));
    }

    @Test
    void teacherNeedsAnExplicitClassroomLinkToPublishOrReadIndividualEvidence() {
        assertThat(service.requireClassroomAction(
                        "oidc|teacher", "class_1a", InstitutionAction.PUBLISH_TO_CLASSROOM)
                .classroomId()).isEqualTo("class_1a");

        assertDenied(() -> service.requireClassroomAction(
                "oidc|teacher", "class_2b", InstitutionAction.PUBLISH_TO_CLASSROOM));
        assertDenied(() -> service.requireClassroomAction(
                "oidc|teacher", "class_3c", InstitutionAction.VIEW_INDIVIDUAL_EVIDENCE));
    }

    @Test
    void coordinatorMayReachClassroomsInTheSameSchoolButNotAnotherSchool() {
        assertThat(service.requireClassroomAction(
                        "oidc|coordinator", "class_2b", InstitutionAction.PUBLISH_TO_CLASSROOM)
                .schoolId()).isEqualTo("school_centro");

        assertDenied(() -> service.requireClassroomAction(
                "oidc|coordinator", "class_3c", InstitutionAction.PUBLISH_TO_CLASSROOM));
    }

    @Test
    void onlySchoolAdministratorMayManageUsers() {
        assertThat(service.requireSchoolAction(
                        "oidc|admin", "school_centro", InstitutionAction.MANAGE_USERS)
                .role()).isEqualTo(InstitutionRole.SCHOOL_ADMIN);
        assertDenied(() -> service.requireSchoolAction(
                "oidc|teacher", "school_centro", InstitutionAction.MANAGE_USERS));
    }

    @Test
    void revokedMembershipStopsAccessWithoutChangingHistoricalRows() {
        service.requireSchoolAction(
                "oidc|teacher", "school_centro", InstitutionAction.CREATE_DRAFT);

        jdbc.update("""
                update institution_school_membership
                   set status = 'REVOKED', updated_at = ?
                 where user_id = 'user_teacher' and school_id = 'school_centro'
                """, Timestamp.from(Instant.parse("2026-09-16T15:00:00Z")));

        assertDenied(() -> service.requireSchoolAction(
                "oidc|teacher", "school_centro", InstitutionAction.CREATE_DRAFT));
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_teacher_classroom
                 where user_id = 'user_teacher' and classroom_id = 'class_1a'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void unsupportedActionIsDeniedEvenWhenTheActorHasAValidMembership() {
        assertDenied(() -> service.requireClassroomAction(
                "oidc|admin", "class_1a", InstitutionAction.UPLOAD_MEDIA));
    }

    private void assertDenied(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(InstitutionalAccessService.AccessDeniedException.class)
                .hasMessage("institutional_access_denied");
    }

    private void tenant(String tenantId) {
        jdbc.update("""
                insert into institution_tenant(tenant_id, name, status, created_at)
                values (?, ?, 'ACTIVE', ?)
                """, tenantId, tenantId, Timestamp.from(Instant.parse("2026-09-16T12:00:00Z")));
    }

    private void school(String schoolId, String tenantId) {
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values (?, ?, ?, 'ACTIVE', ?)
                """, schoolId, tenantId, schoolId,
                Timestamp.from(Instant.parse("2026-09-16T12:00:00Z")));
    }

    private void classroom(String classroomId, String schoolId) {
        jdbc.update("""
                insert into institution_classroom(classroom_id, school_id, name, status, created_at)
                values (?, ?, ?, 'ACTIVE', ?)
                """, classroomId, schoolId, classroomId,
                Timestamp.from(Instant.parse("2026-09-16T12:00:00Z")));
    }

    private void user(String userId, String oidcSubject) {
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values (?, ?, 'ACTIVE', ?)
                """, userId, oidcSubject,
                Timestamp.from(Instant.parse("2026-09-16T12:00:00Z")));
    }

    private void membership(String userId, String schoolId, String role) {
        Instant now = Instant.parse("2026-09-16T12:00:00Z");
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values (?, ?, ?, 'ACTIVE', ?, ?)
                """, userId, schoolId, role, Timestamp.from(now), Timestamp.from(now));
    }

    private void linkTeacher(String userId, String classroomId) {
        Instant now = Instant.parse("2026-09-16T12:00:00Z");
        jdbc.update("""
                insert into institution_teacher_classroom
                (user_id, classroom_id, status, created_at, updated_at)
                values (?, ?, 'ACTIVE', ?, ?)
                """, userId, classroomId, Timestamp.from(now), Timestamp.from(now));
    }
}
