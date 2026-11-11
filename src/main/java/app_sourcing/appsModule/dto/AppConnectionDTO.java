package app_sourcing.appsModule.dto;

import app_sourcing.appsModule.entitie.ConnectionStatus;
import app_sourcing.appsModule.entitie.IntegrationMode;
import app_sourcing.orders.entities.OrderSource;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO renvoyé au frontend pour représenter l'état de connexion
 * d'un seller à une application externe.
 */
@Data
@Builder
public class AppConnectionDTO {

    private OrderSource appType;
    private IntegrationMode integrationMode;
    private ConnectionStatus status;

    // ── OAuth ──────────────────────────────────────────────────────────────────
    /** Nom du compte tel qu'affiché sur la plateforme externe (ex: "Ma boutique LF"). */
    private String externalAccountLabel;

    /** URL d'initiation OAuth à ouvrir dans le navigateur (null si déjà connecté). */
    private String oauthConnectUrl;

    // ── Webhook inbound ────────────────────────────────────────────────────────
    /** URL complète du webhook à coller dans la plateforme (null si mode OAuth). */
    private String webhookUrl;

    // ── Stats communes ─────────────────────────────────────────────────────────
    private Integer webhooksReceived;
    private LocalDateTime lastWebhookAt;
    private LocalDateTime lastSyncedAt;
}


// ─── Séparé : OAuthInitResponse.java ─────────────────────────────────────────
// package app_sourcing.appsModule.dto;

// import lombok.Builder;
// import lombok.Data;

// /** Réponse retournée quand le frontend demande à initier un OAuth. */
// @Data
// @Builder
// public class OAuthInitResponse {
//     /** URL complète vers laquelle rediriger l'utilisateur (popup ou onglet). */
//     private String authorizationUrl;
// }