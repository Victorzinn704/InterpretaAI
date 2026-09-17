package br.gov.interpretaai.server.identity;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAccessService {
    public record Grant(
            String userId,
            String schoolId,
            String classroomId,
            InstitutionRole role) {}

    public record SchoolDisplay(String schoolId, String name, InstitutionRole role) {}

    public static final class AccessDeniedException extends RuntimeException {
        public AccessDeniedException() {
            super("institutional_access_denied");
        }
    }

    private static final Map<InstitutionRole, Set<InstitutionAction>> SCHOOL_PERMISSIONS = Map.of(
            InstitutionRole.TEACHER,
            EnumSet.of(InstitutionAction.CREATE_DRAFT, InstitutionAction.UPLOAD_MEDIA),
            InstitutionRole.COORDINATOR,
            EnumSet.of(
                    InstitutionAction.CREATE_DRAFT,
                    InstitutionAction.UPLOAD_MEDIA,
                    InstitutionAction.VIEW_SCHOOL_AGGREGATE),
            InstitutionRole.SCHOOL_ADMIN,
            EnumSet.of(
                    InstitutionAction.CREATE_DRAFT,
                    InstitutionAction.UPLOAD_MEDIA,
                    InstitutionAction.VIEW_SCHOOL_AGGREGATE,
                    InstitutionAction.MANAGE_USERS));

    private final InstitutionalAccessStore store;

    public InstitutionalAccessService(InstitutionalAccessStore store) {
        this.store = store;
    }

    public Grant requireSchoolAction(
            String oidcSubject, String schoolId, InstitutionAction action) {
        var access = store.activeSchoolAccess(oidcSubject, schoolId)
                .orElseThrow(AccessDeniedException::new);
        if (!SCHOOL_PERMISSIONS.getOrDefault(access.role(), Set.of()).contains(action)) {
            throw new AccessDeniedException();
        }
        return new Grant(access.userId(), access.schoolId(), null, access.role());
    }

    public List<Grant> activeSchoolContexts(String oidcSubject) {
        List<Grant> contexts = store.activeSchoolAccesses(oidcSubject).stream()
                .map(access -> new Grant(
                        access.userId(), access.schoolId(), null, access.role()))
                .toList();
        if (contexts.isEmpty()) throw new AccessDeniedException();
        return contexts;
    }

    public List<SchoolDisplay> activeSchoolDisplays(String oidcSubject) {
        var schools = store.activeSchoolAccesses(oidcSubject).stream()
                .map(access -> new SchoolDisplay(
                        access.schoolId(), access.schoolName(), access.role()))
                .toList();
        if (schools.isEmpty()) throw new AccessDeniedException();
        return schools;
    }

    public List<InstitutionalAccessStore.ClassroomDisplay> activeClassrooms(
            String oidcSubject, String schoolId) {
        var grant = requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        return store.activeClassrooms(
                schoolId, grant.userId(), grant.role() != InstitutionRole.TEACHER);
    }

    public Grant requireClassroomAction(
            String oidcSubject, String classroomId, InstitutionAction action) {
        var access = store.activeClassroomAccess(oidcSubject, classroomId)
                .orElseThrow(AccessDeniedException::new);
        if (action != InstitutionAction.PUBLISH_TO_CLASSROOM
                && action != InstitutionAction.VIEW_INDIVIDUAL_EVIDENCE
                && action != InstitutionAction.PAIR_DEVICE
                && action != InstitutionAction.MANAGE_DEVICE) {
            throw new AccessDeniedException();
        }
        boolean allowed = switch (access.role()) {
            case TEACHER -> access.teacherLinked();
            case COORDINATOR, SCHOOL_ADMIN -> true;
        };
        if (!allowed) throw new AccessDeniedException();
        return new Grant(
                access.userId(), access.schoolId(), access.classroomId(), access.role());
    }
}
