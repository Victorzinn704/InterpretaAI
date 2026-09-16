package br.gov.interpretaai.server.identity;

import java.util.EnumSet;
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

    public Grant requireClassroomAction(
            String oidcSubject, String classroomId, InstitutionAction action) {
        var access = store.activeClassroomAccess(oidcSubject, classroomId)
                .orElseThrow(AccessDeniedException::new);
        if (action != InstitutionAction.PUBLISH_TO_CLASSROOM
                && action != InstitutionAction.VIEW_INDIVIDUAL_EVIDENCE) {
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
