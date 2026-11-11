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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Intégration Shopify — OAuth 2.0 + REST Admin API polling.
 *
 * PARTICULARITÉ : les URLs sont dynamiques par boutique.
 *   Auth  : https://{shop}.myshopify.com/admin/oauth/authorize
 *   Token : https://{shop}.myshopify.com/admin/oauth/access_token
 *   API   : https://{shop}.myshopify.com/admin/api/2024-10/orders.json
 *
 * Le seller saisit son shop domain dans le modal avant de connecter.
 * Validation HMAC obligatoire au callback (sécurité anti-CSRF/forgery).
 * Pas de refresh token pour les offline tokens Shopify.
 * Pagination via header Link : <url>; rel="next"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopifyOAuthService {

    private final AppIntegrationConfig config;
    private final RestTemplate         restTemplate;

    private static final String API_VERSION = "2024-10";

    // =========================================================================
    // ÉTAPE 1 — CONSTRUIRE L'URL D'AUTORISATION
    // =========================================================================

    public String buildAuthorizationUrl(String shopDomain, String state, String callbackUrl) {
        validateShopDomain(shopDomain);
        return "https://" + shopDomain + "/admin/oauth/authorize"
                + "?client_id="    + config.getShopify().getClientId()
                + "&scope="        + config.getShopify().getScopes()
                + "&redirect_uri=" + callbackUrl
                + "&state="        + state;
    }

    // =========================================================================
    // ÉTAPE 2 — VALIDATION HMAC AU CALLBACK
    // Shopify signe avec HMAC-SHA256(clientSecret, "key=value&key=value" trié)
    // =========================================================================

    public boolean validateHmac(Map<String, String> queryParams, String receivedHmac) {
        if (receivedHmac == null || receivedHmac.isBlank()) return false;

        String message = queryParams.entrySet().stream()
                .filter(e -> !e.getKey().equals("hmac"))
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    config.getShopify().getClientSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] digest  = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(digest);

            return MessageDigest.isEqual(
                    computed.getBytes(StandardCharsets.UTF_8),
                    receivedHmac.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("[Shopify] Erreur validation HMAC : {}", e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // ÉTAPE 3 — ÉCHANGE CODE → TOKEN
    // POST https://{shop}.myshopify.com/admin/oauth/access_token
    // =========================================================================

    public OAuthTokenResult exchangeCodeForToken(String shopDomain, String code) {
        validateShopDomain(shopDomain);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     config.getShopify().getClientId());
        body.add("client_secret", config.getShopify().getClientSecret());
        body.add("code",          code);

        ResponseEntity<ShopifyTokenResponse> response = restTemplate.postForEntity(
                "https://" + shopDomain + "/admin/oauth/access_token",
                new HttpEntity<>(body, headers),
                ShopifyTokenResponse.class
        );

        ShopifyTokenResponse tokenResp = response.getBody();
        if (tokenResp == null || tokenResp.getAccessToken() == null) {
            throw new RuntimeException("[Shopify] Token response invalide pour shop=" + shopDomain);
        }

        String shopName = fetchShopName(shopDomain, tokenResp.getAccessToken());

        log.info("[Shopify] Token obtenu pour shop={} name={}", shopDomain, shopName);

        return new OAuthTokenResult(
                tokenResp.getAccessToken(),
                null,       // Shopify offline token : pas de refresh token
                null,       // pas d'expiration
                shopDomain, // → AppConnection.externalAccountId
                shopName    // → AppConnection.externalAccountLabel
        );
    }

    // =========================================================================
    // INFOS DE LA BOUTIQUE : GET /admin/api/{version}/shop.json
    // =========================================================================

    private String fetchShopName(String shopDomain, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://" + shopDomain + "/admin/api/" + API_VERSION + "/shop.json",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> shop = (Map<String, Object>)
                    Objects.requireNonNull(response.getBody()).get("shop");
            return shop != null ? shop.get("name").toString() : shopDomain;
        } catch (Exception e) {
            log.warn("[Shopify] Impossible de récupérer le nom du shop : {}", e.getMessage());
            return shopDomain;
        }
    }

    // =========================================================================
    // PULL DES COMMANDES : GET /admin/api/{version}/orders.json
    // Polling toutes les 5 minutes
    //
    // Paramètres :
    //   status=any          → toutes (open + closed + cancelled)
    //   created_at_min=...  → depuis lastSyncedAt
    //   limit=250           → max par page Shopify
    // Pagination via header Link : <url>; rel="next"
    // =========================================================================

    public List<ShopifyOrder> fetchOrdersSince(AppConnection conn, LocalDateTime since) {
        String shopDomain  = conn.getShopifyShopDomain();
        String accessToken = conn.getOauthAccessToken();

        if (shopDomain == null || shopDomain.isBlank()) {
            log.warn("[Shopify] shopifyShopDomain null pour seller={}", conn.getSeller().getId());
            return List.of();
        }

        List<ShopifyOrder> allOrders = new ArrayList<>();

        String createdAtMin = since != null
                ? since.toString().substring(0, 19) + "+00:00"
                : "2000-01-01T00:00:00+00:00";

        String nextUrl = "https://" + shopDomain
                + "/admin/api/" + API_VERSION + "/orders.json"
                + "?status=any&limit=250&created_at_min=" + createdAtMin
                + "&order=created_at+asc";

        while (nextUrl != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Shopify-Access-Token", accessToken);

            ResponseEntity<ShopifyOrdersResponse> response = restTemplate.exchange(
                    nextUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ShopifyOrdersResponse.class
            );

            ShopifyOrdersResponse pageBody = response.getBody();
            if (pageBody == null || pageBody.getOrders() == null || pageBody.getOrders().isEmpty()) {
                break;
            }

            allOrders.addAll(pageBody.getOrders());

            // Pagination via header Link Shopify
            nextUrl = extractNextPageUrl(response.getHeaders());
        }

        log.info("[Shopify] {} commandes récupérées pour shop={}", allOrders.size(), shopDomain);
        return allOrders;
    }

    // =========================================================================
    // VALIDATION DU SHOP DOMAIN
    // Format attendu : xxx.myshopify.com
    // =========================================================================

    public void validateShopDomain(String shopDomain) {
        if (shopDomain == null
                || !shopDomain.matches("^[a-zA-Z0-9][a-zA-Z0-9\\-]*\\.myshopify\\.com$")) {
            throw new IllegalArgumentException(
                    "[Shopify] Domaine invalide : '" + shopDomain
                            + "'. Format attendu : nom-boutique.myshopify.com"
            );
        }
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS
    // =========================================================================

    private String extractNextPageUrl(HttpHeaders headers) {
        List<String> linkHeaders = headers.get("Link");
        if (linkHeaders == null || linkHeaders.isEmpty()) return null;
        for (String link : linkHeaders) {
            for (String part : link.split(",")) {
                if (part.contains("rel=\"next\"")) {
                    int start = part.indexOf('<') + 1;
                    int end   = part.indexOf('>');
                    if (start > 0 && end > start) return part.substring(start, end).trim();
                }
            }
        }
        return null;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // =========================================================================
    // DTOs — calqués sur la réponse officielle Shopify REST API 2024-10
    // =========================================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyTokenResponse {
        @JsonProperty("access_token") private String accessToken;
        @JsonProperty("scope")        private String scope;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyOrdersResponse {
        private List<ShopifyOrder> orders;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyOrder {
        private Long   id;
        private String name;      // "#1001"
        private String email;
        private String phone;
        private String note;

        @JsonProperty("created_at")          private String createdAt;
        @JsonProperty("financial_status")    private String financialStatus;
        @JsonProperty("fulfillment_status")  private String fulfillmentStatus;
        @JsonProperty("total_price")         private String totalPrice;
        @JsonProperty("currency")            private String currency;
        @JsonProperty("line_items")          private List<ShopifyLineItem>  lineItems;
        @JsonProperty("shipping_address")    private ShopifyAddress         shippingAddress;
        @JsonProperty("customer")            private ShopifyCustomer        customer;

        public String getExternalId() {
            return id != null ? String.valueOf(id) : null;
        }

        public ShopifyLineItem firstLineItem() {
            return (lineItems != null && !lineItems.isEmpty()) ? lineItems.get(0) : null;
        }

        public LocalDateTime getCreatedAtParsed() {
            if (createdAt == null || createdAt.length() < 19) return null;
            try { return LocalDateTime.parse(createdAt.substring(0, 19)); }
            catch (Exception e) { return null; }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyLineItem {
        private Long    id;
        private String  title;
        @JsonProperty("product_id") private Long    productId;
        @JsonProperty("variant_id") private Long    variantId;
        private Integer quantity;
        private String  price;

        public BigDecimal getPriceBD() {
            try { return price != null ? new BigDecimal(price) : BigDecimal.ZERO; }
            catch (Exception e) { return BigDecimal.ZERO; }
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyAddress {
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("last_name")  private String lastName;
        private String address1;
        private String address2;
        private String city;
        private String country;
        private String zip;
        private String phone;

        public String getFullName() {
            return ((firstName != null ? firstName : "") + " "
                    + (lastName != null ? lastName : "")).trim();
        }

        public String getFullAddress() {
            StringBuilder sb = new StringBuilder();
            if (address1 != null) sb.append(address1);
            if (address2 != null && !address2.isBlank()) sb.append(", ").append(address2);
            if (city != null) sb.append(", ").append(city);
            if (zip  != null) sb.append(" ").append(zip);
            return sb.toString().trim();
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShopifyCustomer {
        @JsonProperty("first_name") private String firstName;
        @JsonProperty("last_name")  private String lastName;
        private String email;
        private String phone;

        public String getFullName() {
            return ((firstName != null ? firstName : "") + " "
                    + (lastName != null ? lastName : "")).trim();
        }
    }
}