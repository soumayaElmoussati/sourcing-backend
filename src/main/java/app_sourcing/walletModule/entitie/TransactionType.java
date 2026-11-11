package app_sourcing.walletModule.entitie;

public enum TransactionType {
    BALANCE,        // Dépôt approuvé (crédit du solde)
    WITHDRAW,       // Retrait traité (débit du solde)
    PAYMENT_ORDER,  // Revenu d'une commande livrée (crédit du solde)
    FEE             // Frais prélevé : lead / delivered / returned (débit du solde)
}
