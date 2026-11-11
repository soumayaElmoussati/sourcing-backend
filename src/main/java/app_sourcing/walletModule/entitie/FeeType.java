package app_sourcing.walletModule.entitie;

public enum FeeType {
    LEAD_FEE,       // Prélevé quand le vendeur passe une commande
    DELIVERED_FEE,  // Prélevé quand la commande passe au statut DELIVERED
    RETURNED_FEE    // Prélevé quand la commande passe au statut RETURNED
}
