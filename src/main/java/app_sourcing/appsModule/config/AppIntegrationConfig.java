package app_sourcing.appsModule.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "integrations")
public class AppIntegrationConfig {

    private OAuthProviderConfig lightfunnels = new OAuthProviderConfig();
    private OAuthProviderConfig youcan       = new OAuthProviderConfig();
    private OAuthProviderConfig shopify      = new OAuthProviderConfig();

    private String webhookBaseUrl;
    private String frontendUrl;

    @Data
    public static class OAuthProviderConfig {
        private String clientId;
        private String clientSecret;
        private String authUrl;
        private String tokenUrl;
        private String apiBaseUrl;
        private String scopes;
    }
}