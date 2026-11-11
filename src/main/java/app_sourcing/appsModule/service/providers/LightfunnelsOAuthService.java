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
import java.util.Map;

/**
 * Gère la communication avec l'API Lightfunnels.
 *
 * NOTE IMPORTANTE : Lightfunnels expose une API GraphQL.
 * Toutes les requêtes passent par POST https://lightfunnels.com/api/2022-04
 * avec un Bearer token.
 *
 * Documentation API : https://lightfunnels.com/developers/api
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LightfunnelsOAuthService {

    private final AppIntegrationConfig config;
    private final RestTemplate         restTemplate;

    private static final String GRAPHQL_ENDPOINT = "https://lightfunnels.com/api/2022-04";

    // =========================================================================
    // ÉCHANGE CODE → TOKEN
    // =========================================================================

    public OAuthTokenResult exchangeCodeForToken(String code, String callbackUrl) {

        AppIntegrationConfig.OAuthProviderConfig lf = config.getLightfunnels();

        // Lightfunnels: POST /oauth/token (form-urlencoded)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id",     lf.getClientId());
        body.add("client_secret", lf.getClientSecret());
        body.add("code",          code);
        body.add("grant_type",    "authorization_code");
        body.add("redirect_uri",  callbackUrl);

        ResponseEntity<LightfunnelsTokenResponse> response = restTemplate.postForEntity(
                lf.getTokenUrl(),
                new HttpEntity<>(body, headers),
                LightfunnelsTokenResponse.class
        );

        LightfunnelsTokenResponse tokenResp = response.getBody();
        if (tokenResp == null || tokenResp.getAccessToken() == null) {
            throw new RuntimeException("Lightfunnels: token response invalide");
        }

        // Récupérer les infos du compte avec le token obtenu
        LightfunnelsAccountInfo accountInfo = fetchAccountInfo(tokenResp.getAccessToken());

        return new OAuthTokenResult(
                tokenResp.getAccessToken(),
                tokenResp.getRefreshToken(),
                tokenResp.getExpiresIn() != null
                        ? LocalDateTime.now().plusSeconds(tokenResp.getExpiresIn())
                        : null,
                accountInfo.id(),
                accountInfo.name()
        );
    }

    // =========================================================================
    // INFOS DU COMPTE (GraphQL)
    // =========================================================================

    private LightfunnelsAccountInfo fetchAccountInfo(String accessToken) {
        String query = """
            {
              account {
                id
                name
                email
              }
            }
            """;

        Map<?, ?> resp = executeGraphQL(query, accessToken);
        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>)
                ((Map<?, ?>) resp.get("data")).get("account");

        return new LightfunnelsAccountInfo(
                account.get("id").toString(),
                account.get("name").toString()
        );
    }

    // =========================================================================
    // PULL DES ORDERS (GraphQL paginé)
    // =========================================================================

    /**
     * Récupère les commandes Lightfunnels depuis une date donnée.
     * Appelé par le LightfunnelsOrderSyncJob (polling toutes les N minutes).
     */
    public java.util.List<LightfunnelsOrder> fetchOrdersSince(
            AppConnection conn,
            LocalDateTime since) {

        String accessToken = conn.getOauthAccessToken();
        java.util.List<LightfunnelsOrder> allOrders = new java.util.ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;

        while (hasNextPage) {
            String afterClause = cursor != null ? String.format(", after: \"%s\"", cursor) : "";

            // Lightfunnels filtre par updatedAt >= since
            String sinceStr = since != null ? since.toString() : "2000-01-01T00:00:00";

            String query = String.format("""
                {
                  orders(first: 50%s, filter: { updatedAt: { gte: "%s" } }) {
                    edges {
                      node {
                        id
                        reference
                        createdAt
                        status
                        lineItems {
                          edges {
                            node {
                              quantity
                              price
                              product {
                                id
                                title
                              }
                            }
                          }
                        }
                        customer {
                          name
                          email
                          phone
                          address {
                            country
                            city
                            address1
                          }
                        }
                        totalPrice
                      }
                      cursor
                    }
                    pageInfo {
                      hasNextPage
                    }
                  }
                }
                """, afterClause, sinceStr);

            Map<?, ?> resp = executeGraphQL(query, accessToken);
            @SuppressWarnings("unchecked")
            Map<String, Object> ordersMap = (Map<String, Object>)
                    ((Map<?, ?>) resp.get("data")).get("orders");

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> edges =
                    (java.util.List<Map<String, Object>>) ordersMap.get("edges");

            for (Map<String, Object> edge : edges) {
                @SuppressWarnings("unchecked")
                Map<String, Object> node = (Map<String, Object>) edge.get("node");
                cursor = (String) edge.get("cursor");
                allOrders.add(mapToOrder(node));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> pageInfo = (Map<String, Object>) ordersMap.get("pageInfo");
            hasNextPage = Boolean.TRUE.equals(pageInfo.get("hasNextPage"));
        }

        log.info("[Lightfunnels] {} commandes récupérées pour seller account={}",
                allOrders.size(), conn.getExternalAccountId());

        return allOrders;
    }

    // =========================================================================
    // ENREGISTREMENT DU WEBHOOK (push en temps réel)
    // =========================================================================

    /**
     * Enregistre un webhook sur Lightfunnels pour recevoir les nouvelles commandes
     * en temps réel (en complément du polling).
     *
     * @return l'ID du webhook créé côté Lightfunnels
     */
    public String registerOrderWebhook(AppConnection conn, String webhookUrl) {
        String mutation = String.format("""
            mutation {
              createWebhook(input: {
                topic: ORDER_CREATED
                endpoint: "%s"
                format: JSON
              }) {
                webhook {
                  id
                  topic
                  endpoint
                }
                errors {
                  field
                  message
                }
              }
            }
            """, webhookUrl);

        Map<?, ?> resp = executeGraphQL(mutation, conn.getOauthAccessToken());
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>)
                ((Map<?, ?>) resp.get("data")).get("createWebhook");

        @SuppressWarnings("unchecked")
        java.util.List<?> errors = (java.util.List<?>) result.get("errors");
        if (errors != null && !errors.isEmpty()) {
            throw new RuntimeException("Lightfunnels webhook error: " + errors);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> webhook = (Map<String, Object>) result.get("webhook");
        String webhookId = webhook.get("id").toString();

        log.info("[Lightfunnels] Webhook enregistré id={} url={}", webhookId, webhookUrl);
        return webhookId;
    }

    /**
     * Supprime le webhook lors de la déconnexion.
     */
    public void deleteWebhook(AppConnection conn) {
        if (conn.getExternalWebhookId() == null) return;

        String mutation = String.format("""
            mutation {
              deleteWebhook(id: "%s") {
                success
              }
            }
            """, conn.getExternalWebhookId());

        executeGraphQL(mutation, conn.getOauthAccessToken());
        log.info("[Lightfunnels] Webhook supprimé id={}", conn.getExternalWebhookId());
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private Map<?, ?> executeGraphQL(String query, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, String> requestBody = Map.of("query", query);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                GRAPHQL_ENDPOINT,
                new HttpEntity<>(requestBody, headers),
                Map.class
        );

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("Lightfunnels GraphQL error: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private LightfunnelsOrder mapToOrder(Map<String, Object> node) {
        // Mapper le nœud GraphQL vers notre DTO interne
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) node.get("customer");
        @SuppressWarnings("unchecked")
        Map<String, Object> address = customer != null
                ? (Map<String, Object>) customer.get("address")
                : Map.of();

        // Prendre le premier lineItem
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> lineEdges = (java.util.List<Map<String, Object>>)
                ((Map<?, ?>) node.get("lineItems")).get("edges");

        String productId = null;
        int quantity = 1;

        if (lineEdges != null && !lineEdges.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> lineNode = (Map<String, Object>) lineEdges.get(0).get("node");
            quantity = lineNode.get("quantity") != null
                    ? ((Number) lineNode.get("quantity")).intValue() : 1;
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) lineNode.get("product");
            if (product != null) productId = product.get("id").toString();
        }

        return new LightfunnelsOrder(
                node.get("id").toString(),
                productId,
                customer != null ? customer.get("name").toString() : "",
                customer != null ? (String) customer.get("phone") : "",
                address != null ? (String) address.get("country") : "",
                address != null ? (String) address.get("city") : "",
                address != null ? (String) address.get("address1") : "",
                quantity,
                (String) node.get("createdAt")
        );
    }

    // ─── DTOs internes ────────────────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LightfunnelsTokenResponse {
        @JsonProperty("access_token")  private String accessToken;
        @JsonProperty("refresh_token") private String refreshToken;
        @JsonProperty("expires_in")    private Long expiresIn;
    }

    public record LightfunnelsAccountInfo(String id, String name) {}

    public record LightfunnelsOrder(
            String externalOrderId,
            String productId,
            String customerName,
            String customerPhone,
            String country,
            String city,
            String fullAddress,
            int quantity,
            String createdAt
    ) {}
}