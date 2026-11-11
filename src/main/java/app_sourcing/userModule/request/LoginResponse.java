package app_sourcing.userModule.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private Long         id;
    private String       token;
    private String       refreshToken;
    private String       email;
    private String       firstName;
    private String       lastName;
    private List<String> roles;
    private String       status;
    private boolean      accountVerified;

    /**
     * Timestamp epoch (ms) d'expiration de l'access token.
     * Calculé par le backend : System.currentTimeMillis() + 86400000
     * Le frontend l'utilise pour déconnecter automatiquement après 24h.
     */
    private long tokenExpiresAt;
}