package app_sourcing.appsModule.service.providers;

import app_sourcing.appsModule.config.AppIntegrationConfig;
import app_sourcing.appsModule.entitie.AppConnection;
import app_sourcing.appsModule.service.AppConnectionService.OAuthTokenResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Intégration YouCan — OAuth 2.0 + API REST polling (sans webhook).
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  FLOW COMPLET                                                   │
 * │                                                                 │
 * │  1. Seller clique "Connecter YouCan" dans le frontend           │
 * │  2. Frontend → POST /api/apps/oauth/init/YOUCAN                 │
 * │  3. Backend génère l'URL OAuth et la retourne                   │
 * │  4. Frontend ouvre une popup vers YouCan                        │
 * │  5. Seller autorise → YouCan redirige vers notre callback        │
 * │  6. Backend : échange le code → access_token                    │
 * │  7. Backend : GET /me → récupère id + name du store             │
 * │  8. AppConnection sauvegardée en BDD (status = ACTIVE)          │
 * │  9. Cron toutes les 5 min → GET /orders?include=customer,...    │
 * │  10. Nouvelles commandes injectées dans la BDD locale           │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Endpoints officiels YouCan :
 *   Auth  : https://seller-area.youcan.shop/admin/oauth/authorize
 *   Token : https://api.youcan.shop/oauth/token
 *   Store : GET https://api.youcan.shop/me
 *   Orders: GET https://api.youcan.shop/orders?include=customer,variants
 *
 * Scopes : read-orders  read-products  view-store-info
 * Doc    : https://developer.youcan.shop/store-admin/introduction/oauth
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoucanOAuthService {

    private final AppIntegrationConfig config;
    private final RestTemplate         restTemplate;

    // =========================================================================
    // ÉTAPE 6 — ÉCHANGE CODE → TOKEN
    // Appelé par AppConnectionService.handleOAuthCallback()
    // =========================================================================

    public OAuthTokenResult exchangeCodeForToken(String code, String callbackUrl) {

        AppIntegrationConfig.OAuthProviderConfig yc = config.getYoucan();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // POST https://api.youcan.shop/oauth/token
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "authorization_code");
        body.add("client_id",     yc.getClientId());
        body.add("client_secret", yc.getClientSecret());
        body.add("redirect_uri",  callbackUrl);
        body.add("code",          code);

        ResponseEntity<YoucanTokenResponse> response = restTemplate.postForEntity(
                yc.getTokenUrl(),
                new HttpEntity<>(body, headers),
                YoucanTokenResponse.class
        );

        YoucanTokenResponse tokenResp = response.getBody();
        if (tokenResp == null || tokenResp.getAccessToken() == null) {
            throw new RuntimeException("[YouCan] Réponse token invalide");
        }

        // ÉTAPE 7 — Récupérer les infos du store via GET /me
        YoucanStoreInfo storeInfo = fetchStoreInfo(tokenResp.getAccessToken());

        log.info("[YouCan] Token obtenu pour store={} ({})",
                storeInfo.name(), storeInfo.id());

        return new OAuthTokenResult(
                tokenResp.getAccessToken(),
                tokenResp.getRefreshToken(),
                // expires_in est en secondes
                tokenResp.getExpiresIn() != null
                        ? LocalDateTime.now().plusSeconds(tokenResp.getExpiresIn())
                        : null,
                storeInfo.id(),    // → AppConnection.externalAccountId
                storeInfo.name()   // → AppConnection.externalAccountLabel
        );
    }

    // =========================================================================
    // REFRESH TOKEN
    // Appelé automatiquement par getValidToken() si le token est expiré
    // =========================================================================

    public String refreshAccessToken(AppConnection conn) {
        AppIntegrationConfig.OAuthProviderConfig yc = config.getYoucan();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "refresh_token");
        body.add("client_id",     yc.getClientId());
        body.add("client_secret", yc.getClientSecret());
        body.add("refresh_token", conn.getOauthRefreshToken());

        ResponseEntity<YoucanTokenResponse> response = restTemplate.postForEntity(
                yc.getTokenUrl(),
                new HttpEntity<>(body, headers),
                YoucanTokenResponse.class
        );

        YoucanTokenResponse tokenResp = response.getBody();
        if (tokenResp == null || tokenResp.getAccessToken() == null) {
            throw new RuntimeException("[YouCan] Refresh token invalide pour store="
                    + conn.getExternalAccountId());
        }

        // Mettre à jour les tokens dans l'entité (le scheduler sauvegarde après)
        conn.setOauthAccessToken(tokenResp.getAccessToken());
        if (tokenResp.getRefreshToken() != null) {
            conn.setOauthRefreshToken(tokenResp.getRefreshToken());
        }
        if (tokenResp.getExpiresIn() != null) {
            conn.setOauthTokenExpiresAt(LocalDateTime.now().plusSeconds(tokenResp.getExpiresIn()));
        }

        log.info("[YouCan] Token rafraîchi pour store={}", conn.getExternalAccountId());
        return tokenResp.getAccessToken();
    }

    // =========================================================================
    // ÉTAPE 7 — INFOS DU STORE : GET /me
    // Réponse : { id, name, slug, email, domain, ... }
    // =========================================================================

    private YoucanStoreInfo fetchStoreInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<YoucanMeResponse> response = restTemplate.exchange(
                config.getYoucan().getApiBaseUrl() + "/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                YoucanMeResponse.class
        );

        YoucanMeResponse me = response.getBody();
        if (me == null || me.getId() == null) {
            throw new RuntimeException("[YouCan] Impossible de récupérer les infos du store via /me");
        }

        return new YoucanStoreInfo(me.getId(), me.getName());
    }

    // =========================================================================
    // ÉTAPE 9 — PULL DES COMMANDES : GET /orders (polling cron toutes les 5 min)
    //
    // URL : https://api.youcan.shop/orders
    //       ?include=customer,variants
    //       &sort=created_at
    //       &limit=50
    //       &page=N
    //
    // Pagination : meta.pagination.links.next (null = dernière page)
    // Filtre "since" : appliqué côté client sur created_at
    // =========================================================================

    public List<YoucanOrder> fetchOrdersSince(AppConnection conn, LocalDateTime since) {

        // Rafraîchir le token si expiré (5 min de marge)
        String accessToken = getValidToken(conn);

        List<YoucanOrder> allOrders = new ArrayList<>();

        String nextUrl = config.getYoucan().getApiBaseUrl()
                + "/orders?include=customer,variants&sort=created_at&limit=50&page=1";

        while (nextUrl != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<YoucanOrdersPage> response = restTemplate.exchange(
                    nextUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    YoucanOrdersPage.class
            );

            YoucanOrdersPage page = response.getBody();
            if (page == null || page.getData() == null || page.getData().isEmpty()) {
                break;
            }

            boolean reachedOldOrders = false;

            for (YoucanOrder order : page.getData()) {
                LocalDateTime orderDate = order.getCreatedAtParsed();

                // Arrêter si on atteint des commandes antérieures au "since"
                if (since != null && orderDate != null && orderDate.isBefore(since)) {
                    reachedOldOrders = true;
                    break;
                }
                allOrders.add(order);
            }

            // Arrêter la pagination si on a atteint des anciennes commandes
            if (reachedOldOrders) break;

            // Lire le lien "next" dans meta.pagination.links.next
            nextUrl = extractNextUrl(page);
        }

        log.info("[YouCan] {} commandes récupérées depuis {} pour store={}",
                allOrders.size(), since, conn.getExternalAccountId());

        return allOrders;
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS
    // =========================================================================

    /** Retourne un token valide (refresh automatique si expiré dans moins de 5 min). */
    private String getValidToken(AppConnection conn) {
        if (conn.getOauthTokenExpiresAt() != null
                && LocalDateTime.now().isAfter(
                conn.getOauthTokenExpiresAt().minusMinutes(5))) {
            return refreshAccessToken(conn);
        }
        return conn.getOauthAccessToken();
    }

    /** Extrait l'URL de la page suivante depuis meta.pagination.links.next */
    private String extractNextUrl(YoucanOrdersPage page) {
        if (page.getMeta() == null) return null;
        if (page.getMeta().getPagination() == null) return null;
        if (page.getMeta().getPagination().getLinks() == null) return null;
        return page.getMeta().getPagination().getLinks().getNext();
    }

    // =========================================================================
    // DTOs — calqués exactement sur la réponse API YouCan
    // Doc : https://developer.youcan.shop/store-admin/orders/listing
    // =========================================================================

    /** Réponse POST /oauth/token */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanTokenResponse {
        @JsonProperty("access_token")  private String accessToken;
        @JsonProperty("refresh_token") private String refreshToken;
        @JsonProperty("expires_in")    private Long   expiresIn;   // en secondes
        @JsonProperty("token_type")    private String tokenType;
    }

    /** Réponse GET /me */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanMeResponse {
        private String id;
        private String name;
        private String slug;
        private String email;
        private String domain;
        @JsonProperty("store_id") private String storeId;
    }

    /** Réponse GET /orders — { data: [...], meta: { pagination: {...} } } */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanOrdersPage {
        private List<YoucanOrder> data;
        private YoucanPageMeta    meta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanPageMeta {
        private YoucanPagination pagination;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanPagination {
        private int total;
        @JsonProperty("per_page")     private int perPage;
        @JsonProperty("current_page") private int currentPage;
        @JsonProperty("total_pages")  private int totalPages;
        private YoucanPaginationLinks links;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanPaginationLinks {
        private String next;  // null si dernière page
    }

    /**
     * Commande YouCan.
     * Avec include=customer,variants : les sous-ressources sont incluses.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanOrder {
        private String id;
        private String ref;
        private Double total;
        private String currency;
        private String notes;

        @JsonProperty("status_new")          private String statusText;
        @JsonProperty("shipping_status")     private String shippingStatus;
        @JsonProperty("payment_status_new")  private String paymentStatusText;

        /** Format ISO : "2021-03-17T09:42:07+00:00" */
        @JsonProperty("created_at")          private String createdAt;
        @JsonProperty("updated_at")          private String updatedAt;

        /** Inclus via include=customer */
        private YoucanCustomer customer;

        /** Inclus via include=variants — lignes de commande */
        private List<YoucanVariantLine> variants;

        /** Inclus via include=shipping */
        private YoucanShipping shipping;

        /** Parse createdAt en LocalDateTime pour le filtre "since" */
        public LocalDateTime getCreatedAtParsed() {
            if (createdAt == null || createdAt.length() < 19) return null;
            try {
                return LocalDateTime.parse(createdAt.substring(0, 19));
            } catch (Exception e) {
                return null;
            }
        }

        /** Retourne le premier item de commande */
        public YoucanVariantLine firstVariant() {
            return (variants != null && !variants.isEmpty()) ? variants.get(0) : null;
        }
    }

    /**
     * Ligne de commande : { id, price, quantity, variant: { id, product: {...} } }
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanVariantLine {
        private String  id;
        private Double  price;
        private Integer quantity;
        private YoucanVariant variant;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanVariant {
        private String        id;
        private YoucanProduct product;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanProduct {
        private String id;
        private String name;
        private String slug;
        private Double price;
        @JsonProperty("compare_at_price") private Double compareAtPrice;
    }

    /** Inclus via include=customer */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanCustomer {
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("last_name")  private String lastName;
        private String email;
        private String phone;

        /** Concatène prénom + nom */
        public String getFullName() {
            String fn = firstName != null ? firstName.trim() : "";
            String ln = lastName  != null ? lastName.trim()  : "";
            return (fn + " " + ln).trim();
        }
    }

    /** Inclus via include=shipping */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanShipping {
        private Double              price;
        @JsonProperty("is_free")    private Boolean isFree;
        private YoucanShippingAddress address;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoucanShippingAddress {
        private String country;
        private String city;
        private String address;
    }

    /** Record interne pour transférer les infos du store après /me */
    public record YoucanStoreInfo(String id, String name) {}
}
