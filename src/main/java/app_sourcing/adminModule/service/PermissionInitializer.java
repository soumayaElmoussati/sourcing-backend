package app_sourcing.adminModule.service;

import app_sourcing.adminModule.entitie.Permission;
import app_sourcing.adminModule.repo.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initialise le catalogue de permissions au démarrage si vide.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInitializer implements ApplicationRunner {

    private final PermissionRepository permissionRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() > 0) return;

        List<Object[]> defs = List.of(
                // SELLERS
                new Object[]{"VIEW_SELLERS",         "Voir la liste des vendeurs",            "SELLERS"},
                new Object[]{"MANAGE_SELLERS",       "Approuver / rejeter / supprimer vendeurs","SELLERS"},
                new Object[]{"VIEW_DELETED_SELLERS", "Voir les vendeurs supprimés",            "SELLERS"},
                new Object[]{"RESTORE_SELLERS",      "Restaurer des vendeurs supprimés",       "SELLERS"},
                // ORDERS
                new Object[]{"VIEW_ORDERS",          "Voir toutes les commandes",              "ORDERS"},
                new Object[]{"MANAGE_ORDERS",        "Confirmer / expédier / livrer commandes","ORDERS"},
                // PRODUCTS
                new Object[]{"VIEW_PRODUCTS",        "Voir tous les produits",                 "PRODUCTS"},
                new Object[]{"MANAGE_PRODUCTS",      "Approuver / rejeter / créer produits",   "PRODUCTS"},
                // SOURCING
                new Object[]{"VIEW_SOURCING",        "Voir les demandes de sourcing",          "SOURCING"},
                new Object[]{"MANAGE_SOURCING",      "Définir prix, approuver sourcing",       "SOURCING"},
                // WALLET
                new Object[]{"VIEW_DEPOSITS",        "Voir les dépôts",                        "WALLET"},
                new Object[]{"MANAGE_DEPOSITS",      "Approuver / rejeter dépôts",             "WALLET"},
                new Object[]{"VIEW_WITHDRAWALS",     "Voir les retraits",                      "WALLET"},
                new Object[]{"MANAGE_WITHDRAWALS",   "Traiter les retraits",                   "WALLET"},
                new Object[]{"VIEW_TRANSACTIONS",    "Voir les transactions wallet",           "WALLET"},
                new Object[]{"MANAGE_FEES",          "Configurer les frais de la plateforme",  "WALLET"},
                // SETTINGS
                new Object[]{"MANAGE_ADMIN_USERS",   "Créer et gérer les comptes admin",       "SETTINGS"},
                new Object[]{"VIEW_DASHBOARD",       "Accéder au tableau de bord admin",       "SETTINGS"}
        );

        defs.forEach(d -> {
            Permission p = new Permission();
            p.setName((String) d[0]);
            p.setDescription((String) d[1]);
            p.setCategory((String) d[2]);
            permissionRepository.save(p);
        });

        log.info("✅ {} permissions initialisées", defs.size());
    }
}