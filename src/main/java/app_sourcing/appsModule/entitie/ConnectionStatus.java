// ─── FILE 2: ConnectionStatus.java ────────────────────────────────────────────
package app_sourcing.appsModule.entitie;

public enum ConnectionStatus {
    /** Jamais connecté ou déconnecté manuellement. */
    DISCONNECTED,
    /**
     * OAuth initié (state stocké) mais callback pas encore reçu,
     * OU webhook URL copiée mais aucune commande reçue.
     */
    PENDING,
    /** Connexion opérationnelle, orders reçus. */
    ACTIVE
}