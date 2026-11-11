package app_sourcing.appsModule.dto;

import lombok.Builder;
import lombok.Data;

/** Réponse retournée quand le frontend demande à initier un OAuth. */
@Data
@Builder
public class OAuthInitResponse {
    /** URL complète vers laquelle rediriger l'utilisateur (popup ou onglet). */
    private String authorizationUrl;
}