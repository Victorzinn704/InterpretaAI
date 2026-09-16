package br.gov.interpretaai.server.device;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class DeviceAuthenticationToken extends AbstractAuthenticationToken {
    private final DevicePairingService.DevicePrincipal principal;

    public DeviceAuthenticationToken(DevicePairingService.DevicePrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_DEVICE")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public DevicePairingService.DevicePrincipal getPrincipal() {
        return principal;
    }
}
