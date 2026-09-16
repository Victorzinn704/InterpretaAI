package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.core.VoiceTurnIdempotency.IdempotencyConflictException;
import br.gov.interpretaai.server.core.VoiceTurnIdempotency.InvalidIdempotencyKeyException;
import br.gov.interpretaai.server.core.VoiceTurnIdempotency.TurnStillProcessingException;
import br.gov.interpretaai.server.core.VoiceTurnRateLimiter.RateLimitExceededException;
import br.gov.interpretaai.server.identity.AdultIdentity.UnauthenticatedAdultException;
import br.gov.interpretaai.server.identity.InstitutionalAccessService.AccessDeniedException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(UnauthenticatedAdultException.class)
    ResponseEntity<ProblemDetail> adultAuthenticationRequired() {
        return problem(HttpStatus.UNAUTHORIZED, "adult_authentication_required",
                "Autenticação adulta é obrigatória.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> institutionalAccessDenied() {
        return problem(HttpStatus.FORBIDDEN, "institutional_access_denied",
                "O recurso não está disponível neste contexto institucional.", null);
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    ResponseEntity<ProblemDetail> invalidKey() {
        return problem(HttpStatus.BAD_REQUEST, "invalid_idempotency_key",
                "A chave idempotente deve ter de 8 a 80 caracteres seguros.", null);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ProblemDetail> conflict() {
        return problem(HttpStatus.CONFLICT, "idempotency_conflict",
                "A chave já pertence a outra requisição.", null);
    }

    @ExceptionHandler(TurnStillProcessingException.class)
    ResponseEntity<ProblemDetail> processing() {
        return problem(HttpStatus.TOO_EARLY, "turn_still_processing",
                "O mesmo turno ainda está em processamento.", "1");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> rateLimited() {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "voice_rate_limited",
                "A sessão atingiu o limite temporário de falas.", "60");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String title, String detail, String retryAfter) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        String correlationId = UUID.randomUUID().toString();
        problem.setProperty("code", title);
        problem.setProperty("safeMessage", detail);
        problem.setProperty("correlationId", correlationId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-Id", correlationId);
        if (retryAfter != null) headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        return new ResponseEntity<>(problem, headers, status);
    }
}
