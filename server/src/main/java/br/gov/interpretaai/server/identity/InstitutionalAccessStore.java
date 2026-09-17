package br.gov.interpretaai.server.identity;

import java.util.Optional;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalAccessStore {
    public record SchoolAccess(String userId, String schoolId, String schoolName, InstitutionRole role) {}

    public record ClassroomAccess(
            String userId,
            String schoolId,
            String classroomId,
            InstitutionRole role,
            boolean teacherLinked) {}

    private final JdbcTemplate jdbc;

    public InstitutionalAccessStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SchoolAccess> activeSchoolAccess(String oidcSubject, String schoolId) {
        return jdbc.query("""
                select u.user_id, m.school_id, s.name as school_name, m.role
                  from institution_adult_user u
                  join institution_school_membership m on m.user_id = u.user_id
                  join institution_school s on s.school_id = m.school_id
                  join institution_tenant t on t.tenant_id = s.tenant_id
                 where u.oidc_subject = ? and m.school_id = ?
                   and u.status = 'ACTIVE' and m.status = 'ACTIVE'
                   and s.status = 'ACTIVE' and t.status = 'ACTIVE'
                """, (result, row) -> new SchoolAccess(
                        result.getString("user_id"),
                        result.getString("school_id"),
                        result.getString("school_name"),
                        InstitutionRole.valueOf(result.getString("role"))),
                oidcSubject, schoolId).stream().findFirst();
    }

    public List<SchoolAccess> activeSchoolAccesses(String oidcSubject) {
        return jdbc.query("""
                select u.user_id, m.school_id, s.name as school_name, m.role
                  from institution_adult_user u
                  join institution_school_membership m on m.user_id = u.user_id
                  join institution_school s on s.school_id = m.school_id
                  join institution_tenant t on t.tenant_id = s.tenant_id
                 where u.oidc_subject = ?
                   and u.status = 'ACTIVE' and m.status = 'ACTIVE'
                   and s.status = 'ACTIVE' and t.status = 'ACTIVE'
                 order by m.school_id
                """, (result, row) -> new SchoolAccess(
                        result.getString("user_id"),
                        result.getString("school_id"),
                        result.getString("school_name"),
                        InstitutionRole.valueOf(result.getString("role"))),
                oidcSubject);
    }

    public Optional<ClassroomAccess> activeClassroomAccess(
            String oidcSubject, String classroomId) {
        var base = jdbc.query("""
                select u.user_id, c.school_id, s.name as school_name, c.classroom_id, m.role
                  from institution_adult_user u
                  join institution_school_membership m on m.user_id = u.user_id
                  join institution_school s on s.school_id = m.school_id
                  join institution_tenant t on t.tenant_id = s.tenant_id
                  join institution_classroom c on c.school_id = s.school_id
                 where u.oidc_subject = ? and c.classroom_id = ?
                   and u.status = 'ACTIVE' and m.status = 'ACTIVE'
                   and s.status = 'ACTIVE' and t.status = 'ACTIVE'
                   and c.status = 'ACTIVE'
                """, (result, row) -> new SchoolAccess(
                        result.getString("user_id"),
                        result.getString("school_id"),
                        result.getString("school_name"),
                        InstitutionRole.valueOf(result.getString("role"))),
                oidcSubject, classroomId).stream().findFirst();
        if (base.isEmpty()) return Optional.empty();
        SchoolAccess access = base.orElseThrow();
        boolean linked = Boolean.TRUE.equals(jdbc.queryForObject("""
                select case when count(*) > 0 then true else false end
                  from institution_teacher_classroom
                 where user_id = ? and classroom_id = ? and status = 'ACTIVE'
                """, Boolean.class, access.userId(), classroomId));
        return Optional.of(new ClassroomAccess(
                access.userId(), access.schoolId(), classroomId, access.role(), linked));
    }
}
