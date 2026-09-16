package br.gov.interpretaai.server.identity;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class AdultIdentity {
    public static final class UnauthenticatedAdultException extends RuntimeException {
        public UnauthenticatedAdultException() {
            super("adult_authentication_required");
        }
    }

    public String subject(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new UnauthenticatedAdultException();
        }
        String subject = jwtAuthentication.getToken().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new UnauthenticatedAdultException();
        }
        return subject;
    }
}
