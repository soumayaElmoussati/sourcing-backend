package app_sourcing.productModule.entitie;

/**
 * PDF – Product Status
 * Définit l’état métier d’un produit dans la plateforme
 */
public enum ProductStatus {

    /**
     * - Produit créé mais non encore soumis / validé
     * - Non visible à la vente
     */
    DRAFT,

    /**
     * - En cours de validation (Admin / Pricing)
     * - Non visible à la vente
     */
    PENDING_APPROVAL,

    /**
     * - Validé par l’Admin
     * - Prêt à être activé
     */
    APPROVED,

    /**
     * - Produit disponible à la vente
     * - Stock > 0
     */
    ACTIVE,

    /**
     * - Vente suspendue temporairement
     * - Action manuelle (Seller / Admin)
     */
    PAUSED,

    /**
     Out of Stock (automatique)
     * - Stock épuisé
     * - Calculé automatiquement via Inventory
     */
    OUT_OF_STOCK,

    /**
     * Rejected
     * - Refusé par l’Admin (avec motif)
     */
    REJECTED
}

