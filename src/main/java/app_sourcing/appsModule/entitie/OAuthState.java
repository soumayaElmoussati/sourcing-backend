package app_sourcing.appsModule.entitie;

import app_sourcing.orders.entities.OrderSource;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_states")
@Data
public class OAuthState {

    @Id
    @Column(length = 64)
    private String state;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", nullable = false, length = 30)
    private OrderSource appType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Utilisé uniquement pour Shopify : stocke le shop domain saisi par le seller
    // ex: "mon-shop.myshopify.com"
    @Column(name = "shop_domain", length = 100)
    private String shopDomain;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}