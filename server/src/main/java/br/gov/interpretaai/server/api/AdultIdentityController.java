package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.identity.InstitutionRole;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/identity")
public class AdultIdentityController {
    public record SchoolContext(String schoolId, InstitutionRole role) {}

    public record AdultContext(String userId, List<SchoolContext> schools) {}

    private final AdultIdentity identity;
    private final InstitutionalAccessService access;

    public AdultIdentityController(AdultIdentity identity, InstitutionalAccessService access) {
        this.identity = identity;
        this.access = access;
    }

    @GetMapping("/me")
    public AdultContext me(Authentication authentication) {
        var contexts = access.activeSchoolContexts(identity.subject(authentication));
        return new AdultContext(
                contexts.get(0).userId(),
                contexts.stream()
                        .map(context -> new SchoolContext(context.schoolId(), context.role()))
                        .toList());
    }
}
