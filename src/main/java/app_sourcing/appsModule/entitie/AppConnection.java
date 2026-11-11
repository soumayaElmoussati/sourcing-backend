package app_sourcing.appsModule.entitie;

import app_sourcing.orders.entities.OrderSource;
import app_sourcing.sellerModule.entitie.Seller;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "app_connections",
        uniqueConstraints = @UniqueConstraint(columnNames = {"seller_id", "app_type"})
)
@Data
public class AppConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", nullable = false, length = 30)
    private OrderSource appType;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_mode", nullable = false, length = 20)
    private IntegrationMode integrationMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;

    // ─── OAuth fields ─────────────────────────────────────────────────────────

    @Column(name = "oauth_access_token", columnDefinition = "TEXT")
    private String oauthAccessToken;

    @Column(name = "oauth_refresh_token", columnDefinition = "TEXT")
    private String oauthRefreshToken;

    @Column(name = "oauth_token_expires_at")
    private LocalDateTime oauthTokenExpiresAt;

    @Column(name = "external_account_id")
    private String externalAccountId;

    @Column(name = "external_account_label")
    private String externalAccountLabel;

    @Column(name = "external_webhook_id")
    private String externalWebhookId;

    // ─── Spécifique Shopify ───────────────────────────────────────────────────
    // Shopify a des URLs dynamiques par boutique : https://{shop}.myshopify.com
    // On stocke le shop domain pour les appels API en polling

    @Column(name = "shopify_shop_domain", length = 100)
    private String shopifyShopDomain;

    // ─── Webhook inbound fields ───────────────────────────────────────────────

    @Column(name = "webhook_token", unique = true, length = 64)
    private String webhookToken;

    // ─── Statistiques ─────────────────────────────────────────────────────────

    @Column(name = "webhooks_received")
    private Integer webhooksReceived = 0;

    @Column(name = "last_webhook_at")
    private LocalDateTime lastWebhookAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    // ─── Audit ────────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}