package br.gov.interpretaai.server.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class DeviceAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern DEVICE_PATH = Pattern.compile(
            "^/api/v2/devices/([a-z0-9][a-z0-9_-]{2,63})(?:/.*)?$");

    private final DevicePairingService pairing;
    private final ObjectMapper mapper;

    public DeviceAuthenticationFilter(DevicePairingService pairing, ObjectMapper mapper) {
        this.pairing = pairing;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !DEVICE_PATH.matcher(requestPath(request)).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Matcher path = DEVICE_PATH.matcher(requestPath(request));
        if (!path.matches()) {
            chain.doFilter(request, response);
            return;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
        try {
            var principal = pairing.authenticate(path.group(1), token);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new DeviceAuthenticationToken(principal));
            SecurityContextHolder.setContext(context);
            chain.doFilter(request, response);
        } catch (DevicePairingException error) {
            writeProblem(response, error);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context.isEmpty() ? uri : uri.substring(context.length());
    }

    private void writeProblem(HttpServletResponse response, DevicePairingException error)
            throws IOException {
        String correlationId = UUID.randomUUID().toString();
        response.setStatus(error.status());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("X-Correlation-Id", correlationId);
        mapper.writeValue(response.getOutputStream(), Map.of(
                "type", "https://interpreta.ai/problems/" + error.code(),
                "title", error.code(),
                "status", error.status(),
                "code", error.code(),
                "safeMessage", error.getMessage(),
                "correlationId", correlationId));
    }
}
