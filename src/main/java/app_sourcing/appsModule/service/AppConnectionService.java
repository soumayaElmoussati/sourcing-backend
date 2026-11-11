package app_sourcing.appsModule.service;

import app_sourcing.appsModule.config.AppIntegrationConfig;
import app_sourcing.appsModule.dto.AppConnectionDTO;
import app_sourcing.appsModule.dto.OAuthInitResponse;
import app_sourcing.appsModule.entitie.AppConnection;
import app_sourcing.appsModule.entitie.ConnectionStatus;
import app_sourcing.appsModule.entitie.IntegrationMode;
import app_sourcing.appsModule.entitie.OAuthState;
import app_sourcing.appsModule.repository.AppConnectionRepository;
import app_sourcing.appsModule.repository.OAuthStateRepository;
import app_sourcing.appsModule.service.providers.LightfunnelsOAuthService;
import app_sourcing.appsModule.service.providers.ShopifyOAuthService;
import app_sourcing.appsModule.service.providers.YoucanOAuthService;
import app_sourcing.orders.entities.OrderSource;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.repo.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppConnectionService {

    private final AppConnectionRepository  connectionRepo;
    private final OAuthStateRepository     stateRepo;
    private final SellerRepository         sellerRepo;
    private final AppIntegrationConfig     config;
    private final LightfunnelsOAuthService lightfunnelsService;
    private final YoucanOAuthService       youcanService;
    private final ShopifyOAuthService      shopifyService;

    private static final Set<OrderSource> OAUTH_APPS = Set.of(
            OrderSource.LIGHTFUNNELS,
            OrderSource.YOUCAN,
            OrderSource.SHOPIFY
    );

    private static final Set<OrderSource> WEBHOOK_APPS = Set.of(
            OrderSource.GOOGLE_SHEETS,
            OrderSource.WORDPRESS,
            OrderSource.EASY_ORDERS
    );

    // =========================================================================
    // LECTURE
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AppConnectionDTO> getConnectionsForCurrentSeller() {
        Long sellerId = getCurrentSellerId();

        Map<OrderSource, AppConnection> existingByType = new HashMap<>();
        connectionRepo.findBySellerId(sellerId)
                .forEach(c -> existingByType.put(c.getAppType(), c));

        List<AppConnectionDTO> result = new ArrayList<>();
        List<OrderSource> allApps = new ArrayList<>();
        allApps.addAll(OAUTH_APPS);
        allApps.addAll(WEBHOOK_APPS);

        for (OrderSource appType : allApps) {
            result.add(toDTO(appType, existingByType.get(appType), sellerId));
        }
        return result;
    }

    // =========================================================================
    // OAUTH INITIATION
    // shopDomain : obligatoire pour Shopify (ex: "mon-shop.myshopify.com"), null sinon
    // =========================================================================

    @Transactional
    public OAuthInitResponse initiateOAuth(OrderSource appType, String shopDomain) {
        if (!OAUTH_APPS.contains(appType)) {
            throw new IllegalArgumentException(appType + " ne supporte pas OAuth.");
        }

        Long   sellerId = getCurrentSellerId();
        String state    = UUID.randomUUID().toString().replace("-", "");

        OAuthState oauthState = new OAuthState();
        oauthState.setState(state);
        oauthState.setSellerId(sellerId);
        oauthState.setAppType(appType);
        oauthState.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        // Shopify : valider et stocker le shop domain dans le state
        if (appType == OrderSource.SHOPIFY) {
            shopifyService.validateShopDomain(shopDomain);
            oauthState.setShopDomain(shopDomain);
        }

        stateRepo.save(oauthState);

        String authUrl = buildAuthUrl(appType, shopDomain, state);

        log.info("[OAuth] Initiation {} pour seller={}", appType, sellerId);

        return OAuthInitResponse.builder()
                .authorizationUrl(authUrl)
                .build();
    }

    // =========================================================================
    // OAUTH CALLBACK
    // allParams : tous les query params (pour la validation HMAC Shopify)
    // hmac      : valeur du paramètre hmac (Shopify uniquement)
    // =========================================================================

    @Transactional
    public void handleOAuthCallback(String code, String state,
                                    Map<String, String> allParams, String hmac) {

        OAuthState oauthState = stateRepo.findByState(state)
                .orElseThrow(() -> new IllegalArgumentException("State OAuth invalide ou expiré."));

        if (oauthState.isExpired()) {
            stateRepo.delete(oauthState);
            throw new IllegalArgumentException("State OAuth expiré. Veuillez recommencer.");
        }

        Long        sellerId   = oauthState.getSellerId();
        OrderSource appType    = oauthState.getAppType();
        String      shopDomain = oauthState.getShopDomain();
        stateRepo.delete(oauthState);

        // Validation HMAC obligatoire pour Shopify
        if (appType == OrderSource.SHOPIFY) {
            if (!shopifyService.validateHmac(allParams, hmac)) {
                throw new SecurityException("[Shopify] HMAC invalide — callback rejeté.");
            }
        }

        Seller seller = sellerRepo.findById(sellerId)
                .orElseThrow(() -> new IllegalStateException("Seller introuvable : " + sellerId));

        AppConnection conn = connectionRepo
                .findBySellerIdAndAppType(sellerId, appType)
                .orElseGet(() -> {
                    AppConnection c = new AppConnection();
                    c.setSeller(seller);
                    c.setAppType(appType);
                    c.setIntegrationMode(IntegrationMode.OAUTH);
                    return c;
                });

        OAuthTokenResult tokenResult = exchangeCodeForToken(appType, code, shopDomain);

        conn.setOauthAccessToken(tokenResult.accessToken());
        conn.setOauthRefreshToken(tokenResult.refreshToken());
        conn.setOauthTokenExpiresAt(tokenResult.expiresAt());
        conn.setExternalAccountId(tokenResult.externalAccountId());
        conn.setExternalAccountLabel(tokenResult.accountLabel());
        conn.setStatus(ConnectionStatus.ACTIVE);

        // Shopify : stocker le shop domain pour les appels API futurs
        if (appType == OrderSource.SHOPIFY && shopDomain != null) {
            conn.setShopifyShopDomain(shopDomain);
        }

        connectionRepo.save(conn);
        log.info("[OAuth] Connexion {} activée pour seller={}", appType, sellerId);
    }

    // =========================================================================
    // WEBHOOK INBOUND
    // =========================================================================

    @Transactional
    public AppConnectionDTO activateWebhookInbound(OrderSource appType) {
        if (!WEBHOOK_APPS.contains(appType)) {
            throw new IllegalArgumentException(appType + " n'utilise pas webhook entrant.");
        }

        Long   sellerId = getCurrentSellerId();
        Seller seller   = sellerRepo.findById(sellerId).orElseThrow();

        AppConnection conn = connectionRepo
                .findBySellerIdAndAppType(sellerId, appType)
                .orElseGet(() -> {
                    AppConnection c = new AppConnection();
                    c.setSeller(seller);
                    c.setAppType(appType);
                    c.setIntegrationMode(IntegrationMode.WEBHOOK_INBOUND);
                    return c;
                });

        if (conn.getWebhookToken() == null) {
            conn.setWebhookToken(generateWebhookToken());
        }
        conn.setStatus(ConnectionStatus.PENDING);
        connectionRepo.save(conn);

        return toDTO(appType, conn, sellerId);
    }

    @Transactional
    public AppConnectionDTO regenerateWebhookToken(OrderSource appType) {
        Long sellerId = getCurrentSellerId();

        AppConnection conn = connectionRepo
                .findBySellerIdAndAppType(sellerId, appType)
                .orElseThrow(() -> new IllegalStateException("App non configurée."));

        conn.setWebhookToken(generateWebhookToken());
        conn.setStatus(ConnectionStatus.PENDING);
        conn.setWebhooksReceived(0);
        connectionRepo.save(conn);

        return toDTO(appType, conn, sellerId);
    }

    // =========================================================================
    // DÉCONNEXION
    // =========================================================================

    @Transactional
    public void disconnect(OrderSource appType) {
        Long sellerId = getCurrentSellerId();

        AppConnection conn = connectionRepo
                .findBySellerIdAndAppType(sellerId, appType)
                .orElseThrow(() -> new IllegalStateException("App non connectée."));

        connectionRepo.delete(conn);
        log.info("[Apps] Déconnexion {} pour seller={}", appType, sellerId);
    }

    public String getFrontendUrl() {
        return config.getFrontendUrl();
    }

    // =========================================================================
    // PRIVÉS
    // =========================================================================

    private String buildAuthUrl(OrderSource appType, String shopDomain, String state) {
        String callbackUrl = buildCallbackUrl(appType);

        return switch (appType) {
            case SHOPIFY -> shopifyService.buildAuthorizationUrl(shopDomain, state, callbackUrl);

            case YOUCAN -> {
                // YouCan attend scope[] en paramètres séparés
                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromUriString(config.getYoucan().getAuthUrl())
                        .queryParam("client_id",     config.getYoucan().getClientId())
                        .queryParam("redirect_uri",  callbackUrl)
                        .queryParam("response_type", "code")
                        .queryParam("state",         state);
                for (String scope : config.getYoucan().getScopes().split(" ")) {
                    builder.queryParam("scope[]", scope);
                }
                yield builder.build(false).toUriString();
            }

            case LIGHTFUNNELS -> UriComponentsBuilder
                    .fromUriString(config.getLightfunnels().getAuthUrl())
                    .queryParam("client_id",     config.getLightfunnels().getClientId())
                    .queryParam("redirect_uri",  callbackUrl)
                    .queryParam("response_type", "code")
                    .queryParam("scope",         config.getLightfunnels().getScopes())
                    .queryParam("state",         state)
                    .build().toUriString();

            default -> throw new IllegalArgumentException("Provider OAuth inconnu : " + appType);
        };
    }

    private OAuthTokenResult exchangeCodeForToken(OrderSource appType, String code,
                                                  String shopDomain) {
        return switch (appType) {
            case LIGHTFUNNELS -> lightfunnelsService.exchangeCodeForToken(code, buildCallbackUrl(appType));
            case YOUCAN       -> youcanService.exchangeCodeForToken(code, buildCallbackUrl(appType));
            case SHOPIFY      -> shopifyService.exchangeCodeForToken(shopDomain, code);
            default           -> throw new IllegalArgumentException("Provider OAuth inconnu : " + appType);
        };
    }

    private String buildCallbackUrl(OrderSource appType) {
        return config.getWebhookBaseUrl() + "/api/apps/oauth/callback/" + appType.name();
    }

    private AppConnectionDTO toDTO(OrderSource appType, AppConnection conn, Long sellerId) {
        if (conn == null) {
            return AppConnectionDTO.builder()
                    .appType(appType)
                    .integrationMode(OAUTH_APPS.contains(appType)
                            ? IntegrationMode.OAUTH : IntegrationMode.WEBHOOK_INBOUND)
                    .status(ConnectionStatus.DISCONNECTED)
                    .build();
        }
        return AppConnectionDTO.builder()
                .appType(appType)
                .integrationMode(conn.getIntegrationMode())
                .status(conn.getStatus())
                .externalAccountLabel(conn.getExternalAccountLabel())
                .webhookUrl(conn.getWebhookToken() != null
                        ? config.getWebhookBaseUrl() + "/api/webhooks/" + conn.getWebhookToken()
                        : null)
                .webhooksReceived(conn.getWebhooksReceived())
                .lastWebhookAt(conn.getLastWebhookAt())
                .lastSyncedAt(conn.getLastSyncedAt())
                .build();
    }

    private String generateWebhookToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Long getCurrentSellerId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Seller seller) return seller.getId();
        throw new IllegalStateException("Utilisateur non authentifié ou non vendeur.");
    }

    public record OAuthTokenResult(
            String accessToken, String refreshToken, LocalDateTime expiresAt,
            String externalAccountId, String accountLabel
    ) {}
}