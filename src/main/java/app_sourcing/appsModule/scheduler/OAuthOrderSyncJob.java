package app_sourcing.appsModule.scheduler;

import app_sourcing.appsModule.entitie.AppConnection;
import app_sourcing.appsModule.entitie.ConnectionStatus;
import app_sourcing.appsModule.repository.AppConnectionRepository;
import app_sourcing.appsModule.repository.OAuthStateRepository;
import app_sourcing.appsModule.service.OrderIngestionService;
import app_sourcing.appsModule.service.providers.LightfunnelsOAuthService;
import app_sourcing.appsModule.service.providers.ShopifyOAuthService;
import app_sourcing.appsModule.service.providers.YoucanOAuthService;
import app_sourcing.orders.entities.OrderSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthOrderSyncJob {

    private final AppConnectionRepository  connectionRepo;
    private final OAuthStateRepository     stateRepo;
    private final LightfunnelsOAuthService lightfunnelsService;
    private final YoucanOAuthService       youcanService;
    private final ShopifyOAuthService      shopifyService;
    private final OrderIngestionService    ingestionService;

    // ── Lightfunnels ──────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${integrations.sync.lightfunnels-interval-ms:300000}")
    public void syncLightfunnels() {
        List<AppConnection> connections = connectionRepo
                .findByAppTypeAndStatus(OrderSource.LIGHTFUNNELS, ConnectionStatus.ACTIVE);
        if (connections.isEmpty()) return;
        log.debug("[Job] Sync Lightfunnels — {} compte(s)", connections.size());

        for (AppConnection conn : connections) {
            try {
                LocalDateTime since = conn.getLastSyncedAt() != null
                        ? conn.getLastSyncedAt()
                        : LocalDateTime.now().minusDays(30);

                var orders = lightfunnelsService.fetchOrdersSince(conn, since);
                int count = 0;
                for (var order : orders) {
                    ingestionService.ingestLightfunnelsOrder(conn, order);
                    count++;
                }
                conn.setLastSyncedAt(LocalDateTime.now());
                connectionRepo.save(conn);
                if (count > 0) log.info("[Job] Lightfunnels : {} commande(s) seller={}",
                        count, conn.getSeller().getId());
            } catch (Exception e) {
                log.error("[Job] Erreur Lightfunnels seller={} : {}",
                        conn.getSeller().getId(), e.getMessage(), e);
            }
        }
    }

    // ── YouCan ────────────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${integrations.sync.youcan-interval-ms:300000}")
    public void syncYoucan() {
        List<AppConnection> connections = connectionRepo
                .findByAppTypeAndStatus(OrderSource.YOUCAN, ConnectionStatus.ACTIVE);
        if (connections.isEmpty()) return;
        log.debug("[Job] Sync YouCan — {} boutique(s)", connections.size());

        for (AppConnection conn : connections) {
            try {
                LocalDateTime since = conn.getLastSyncedAt() != null
                        ? conn.getLastSyncedAt()
                        : LocalDateTime.now().minusDays(30);

                var orders = youcanService.fetchOrdersSince(conn, since);
                int count = 0;
                for (var order : orders) {
                    ingestionService.ingestYoucanOrder(conn, order);
                    count++;
                }
                conn.setLastSyncedAt(LocalDateTime.now());
                connectionRepo.save(conn);
                if (count > 0) log.info("[Job] YouCan : {} commande(s) store={}",
                        count, conn.getExternalAccountLabel());
            } catch (Exception e) {
                log.error("[Job] Erreur YouCan seller={} : {}",
                        conn.getSeller().getId(), e.getMessage(), e);
            }
        }
    }

    // ── Shopify ───────────────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${integrations.sync.shopify-interval-ms:300000}")
    public void syncShopify() {
        List<AppConnection> connections = connectionRepo
                .findByAppTypeAndStatus(OrderSource.SHOPIFY, ConnectionStatus.ACTIVE);
        if (connections.isEmpty()) return;
        log.debug("[Job] Sync Shopify — {} boutique(s)", connections.size());

        for (AppConnection conn : connections) {
            try {
                LocalDateTime since = conn.getLastSyncedAt() != null
                        ? conn.getLastSyncedAt()
                        : LocalDateTime.now().minusDays(30);

                var orders = shopifyService.fetchOrdersSince(conn, since);
                int count = 0;
                for (var order : orders) {
                    ingestionService.ingestShopifyOrder(conn, order);
                    count++;
                }
                conn.setLastSyncedAt(LocalDateTime.now());
                connectionRepo.save(conn);
                if (count > 0) log.info("[Job] Shopify : {} commande(s) shop={}",
                        count, conn.getShopifyShopDomain());
            } catch (Exception e) {
                log.error("[Job] Erreur Shopify shop={} : {}",
                        conn.getShopifyShopDomain(), e.getMessage(), e);
            }
        }
    }

    // ── Nettoyage states OAuth expirés ─────────────────────────────────────────

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanExpiredOAuthStates() {
        stateRepo.deleteExpiredStates(LocalDateTime.now());
        log.debug("[Job] States OAuth expirés supprimés");
    }
}