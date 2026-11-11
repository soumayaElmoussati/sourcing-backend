// ─── FILE 1: IntegrationMode.java ─────────────────────────────────────────────
package app_sourcing.appsModule.entitie;

public enum IntegrationMode {
    /** OAuth 2.0 : le seller autorise X10ECOM à lire son compte. */
    OAUTH,
    /** Webhook entrant : URL unique générée, le seller la colle dans la plateforme. */
    WEBHOOK_INBOUND
}

