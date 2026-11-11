package app_sourcing.appsModule.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload JSON générique reçu via webhook entrant (Google Sheets, WordPress…).
 *
 * Les champs correspondent au contrat documenté dans la modal :
 *   productId, customerName, customerPhone, country, city, fullAddress, quantity
 *
 * On utilise @JsonAlias pour accepter plusieurs variantes de nommage
 * (snake_case et camelCase).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InboundWebhookPayload {

    @JsonAlias({"product_id", "productId"})
    private String productId;

    @JsonAlias({"customer_name", "customerName", "name"})
    private String customerName;

    @JsonAlias({"customer_phone", "customerPhone", "phone"})
    private String customerPhone;

    private String country;
    private String city;

    @JsonAlias({"full_address", "fullAddress", "address"})
    private String fullAddress;

    private Integer quantity;

    // Optionnels
    private BigDecimal unitPrice;
    private String note;

    /** Identifiant de la commande côté plateforme externe (anti-doublon). */
    @JsonAlias({"order_id", "orderId", "id"})
    private String externalOrderId;
}