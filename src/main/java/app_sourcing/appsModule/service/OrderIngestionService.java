package app_sourcing.appsModule.service;

import app_sourcing.appsModule.dto.InboundWebhookPayload;
import app_sourcing.appsModule.entitie.AppConnection;
import app_sourcing.appsModule.entitie.ConnectionStatus;
import app_sourcing.appsModule.repository.AppConnectionRepository;
import app_sourcing.appsModule.service.providers.LightfunnelsOAuthService.LightfunnelsOrder;
import app_sourcing.appsModule.service.providers.ShopifyOAuthService.ShopifyOrder;
import app_sourcing.appsModule.service.providers.ShopifyOAuthService.ShopifyLineItem;
import app_sourcing.appsModule.service.providers.YoucanOAuthService.YoucanOrder;
import app_sourcing.appsModule.service.providers.YoucanOAuthService.YoucanVariantLine;
import app_sourcing.orders.entities.*;
import app_sourcing.orders.repo.OrderRepository;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.repo.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderIngestionService {

    private final OrderRepository         orderRepo;
    private final ProductRepository       productRepo;
    private final AppConnectionRepository connectionRepo;

    // =========================================================================
    // WEBHOOK ENTRANT (Google Sheets, WordPress, Easy Orders)
    // =========================================================================

    @Transactional
    public void ingestFromWebhook(AppConnection conn, InboundWebhookPayload payload) {
        if (payload.getExternalOrderId() != null &&
                orderRepo.existsByExternalOrderIdAndSource(
                        payload.getExternalOrderId(), conn.getAppType())) {
            log.debug("[Webhook] Doublon ignoré externalOrderId={}", payload.getExternalOrderId());
            return;
        }

        Product product = resolveProduct(payload.getProductId(), conn);
        Order order = buildOrder(conn, product, payload.getExternalOrderId());
        order.setSource(conn.getAppType());
        order.setNote(payload.getNote());

        int qty = payload.getQuantity() != null ? payload.getQuantity() : 1;
        order.setQuantity(qty);

        BigDecimal unitPrice = payload.getUnitPrice() != null
                ? payload.getUnitPrice()
                : (product != null ? product.getPrice() : BigDecimal.ZERO);
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)));

        setCountrySafe(order, payload.getCountry());

        Customer customer = buildCustomer(order,
                payload.getCustomerName(), payload.getCustomerPhone(),
                payload.getCity(), payload.getFullAddress());
        order.setCustomer(customer);

        orderRepo.save(order);
        updateConnectionStats(conn);

        log.info("[Webhook] Commande créée {} depuis {} seller={}",
                order.getId(), conn.getAppType(), conn.getSeller().getId());
    }

    // =========================================================================
    // LIGHTFUNNELS
    // =========================================================================

    @Transactional
    public void ingestLightfunnelsOrder(AppConnection conn, LightfunnelsOrder lfo) {
        if (orderRepo.existsByExternalOrderIdAndSource(
                lfo.externalOrderId(), OrderSource.LIGHTFUNNELS)) return;

        Product product = resolveProduct(lfo.productId(), conn);
        Order order = buildOrder(conn, product, lfo.externalOrderId());
        order.setSource(OrderSource.LIGHTFUNNELS);
        order.setQuantity(lfo.quantity());

        BigDecimal unitPrice = product != null ? product.getPrice() : BigDecimal.ZERO;
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(lfo.quantity())));
        setCountrySafe(order, lfo.country());

        Customer customer = buildCustomer(order,
                lfo.customerName(), lfo.customerPhone(), lfo.city(), lfo.fullAddress());
        order.setCustomer(customer);

        orderRepo.save(order);
        updateConnectionStats(conn);
        log.debug("[Lightfunnels] Commande {} ingérée seller={}", lfo.externalOrderId(), conn.getSeller().getId());
    }

    // =========================================================================
    // YOUCAN
    // =========================================================================

    @Transactional
    public void ingestYoucanOrder(AppConnection conn, YoucanOrder yco) {
        if (orderRepo.existsByExternalOrderIdAndSource(yco.getId(), OrderSource.YOUCAN)) {
            log.debug("[YouCan] Doublon ignoré id={}", yco.getId());
            return;
        }

        YoucanVariantLine firstLine = yco.firstVariant();
        String youcanProductId = (firstLine != null && firstLine.getVariant() != null
                && firstLine.getVariant().getProduct() != null)
                ? firstLine.getVariant().getProduct().getId() : null;

        Product product = resolveProduct(youcanProductId, conn);
        Order order = buildOrder(conn, product, yco.getId());
        order.setSource(OrderSource.YOUCAN);
        order.setNote(yco.getNotes());

        int qty = (firstLine != null && firstLine.getQuantity() != null)
                ? firstLine.getQuantity() : 1;
        order.setQuantity(qty);

        BigDecimal unitPrice;
        if (product != null && product.getPrice() != null) {
            unitPrice = product.getPrice();
        } else if (firstLine != null && firstLine.getPrice() != null) {
            unitPrice = BigDecimal.valueOf(firstLine.getPrice());
        } else {
            unitPrice = BigDecimal.ZERO;
        }
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)));

        String customerName = "", customerPhone = "", city = null, fullAddress = null, country = null;
        if (yco.getShipping() != null && yco.getShipping().getAddress() != null) {
            country     = yco.getShipping().getAddress().getCountry();
            city        = yco.getShipping().getAddress().getCity();
            fullAddress = yco.getShipping().getAddress().getAddress();
        }
        if (yco.getCustomer() != null) {
            customerName  = yco.getCustomer().getFullName();
            customerPhone = yco.getCustomer().getPhone() != null ? yco.getCustomer().getPhone() : "";
        }
        setCountrySafe(order, country);

        Customer customer = buildCustomer(order, customerName, customerPhone, city, fullAddress);
        order.setCustomer(customer);

        orderRepo.save(order);
        updateConnectionStats(conn);
        log.info("[YouCan] Commande ref={} ingérée seller={}", yco.getRef(), conn.getSeller().getId());
    }

    // =========================================================================
    // SHOPIFY
    // =========================================================================

    @Transactional
    public void ingestShopifyOrder(AppConnection conn, ShopifyOrder sho) {
        String externalId = sho.getExternalId();

        if (externalId != null &&
                orderRepo.existsByExternalOrderIdAndSource(externalId, OrderSource.SHOPIFY)) {
            log.debug("[Shopify] Doublon ignoré id={} name={}", externalId, sho.getName());
            return;
        }

        ShopifyLineItem firstLine = sho.firstLineItem();
        String shopifyProductId = (firstLine != null && firstLine.getProductId() != null)
                ? String.valueOf(firstLine.getProductId()) : null;

        Product product = resolveProduct(shopifyProductId, conn);
        Order order = buildOrder(conn, product, externalId);
        order.setSource(OrderSource.SHOPIFY);
        order.setNote(sho.getNote());

        int qty = (firstLine != null && firstLine.getQuantity() != null)
                ? firstLine.getQuantity() : 1;
        order.setQuantity(qty);

        BigDecimal unitPrice;
        if (product != null && product.getPrice() != null) {
            unitPrice = product.getPrice();
        } else if (firstLine != null) {
            unitPrice = firstLine.getPriceBD();
        } else {
            unitPrice = BigDecimal.ZERO;
        }
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(qty)));

        String customerName = "", customerPhone = "", city = null, fullAddress = null, country = null;
        if (sho.getShippingAddress() != null) {
            customerName  = sho.getShippingAddress().getFullName();
            customerPhone = sho.getShippingAddress().getPhone() != null
                    ? sho.getShippingAddress().getPhone() : "";
            city        = sho.getShippingAddress().getCity();
            fullAddress = sho.getShippingAddress().getFullAddress();
            country     = sho.getShippingAddress().getCountry();
        } else if (sho.getCustomer() != null) {
            customerName  = sho.getCustomer().getFullName();
            customerPhone = sho.getCustomer().getPhone() != null
                    ? sho.getCustomer().getPhone() : "";
        }
        setCountrySafe(order, country);

        Customer customer = buildCustomer(order, customerName, customerPhone, city, fullAddress);
        order.setCustomer(customer);

        orderRepo.save(order);
        updateConnectionStats(conn);
        log.info("[Shopify] Commande {} ingérée seller={} shop={}",
                sho.getName(), conn.getSeller().getId(), conn.getShopifyShopDomain());
    }

    // =========================================================================
    // UTILITAIRES PRIVÉS
    // =========================================================================

    private Order buildOrder(AppConnection conn, Product product, String externalOrderId) {
        Order order = new Order();
        order.setId(generateOrderId());
        order.setSeller(conn.getSeller());
        order.setProduct(product);
        order.setExternalOrderId(externalOrderId);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMode(PaymentMode.CASH_ON_DELIVERY);
        order.setCreatedAt(LocalDateTime.now());
        if (product != null && product.getFulfillmentMode() != null) {
            order.setFulfillmentMode(product.getFulfillmentMode());
        }
        return order;
    }

    private Customer buildCustomer(Order order, String name, String phone,
                                   String city, String fullAddress) {
        Customer c = new Customer();
        c.setId(generateOrderId());
        c.setOrder(order);
        c.setCustomerName(name != null ? name : "");
        c.setCustomerPhone(phone != null ? phone : "");
        c.setCity(city != null ? city : "");
        c.setFullAddress(fullAddress != null ? fullAddress : "");
        return c;
    }

    private Product resolveProduct(String productId, AppConnection conn) {
        if (productId == null) return null;
        return productRepo.findByIdAndSellerIdAndIsArchiveFalse(productId, conn.getSeller().getId())
                .orElseGet(() -> {
                    log.warn("[Ingestion] Produit '{}' introuvable pour seller={}", productId, conn.getSeller().getId());
                    return null;
                });
    }

    private void updateConnectionStats(AppConnection conn) {
        conn.setWebhooksReceived(
                (conn.getWebhooksReceived() == null ? 0 : conn.getWebhooksReceived()) + 1);
        conn.setLastWebhookAt(LocalDateTime.now());
        if (conn.getStatus() != ConnectionStatus.ACTIVE) conn.setStatus(ConnectionStatus.ACTIVE);
        connectionRepo.save(conn);
    }

    private String generateOrderId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private void setCountrySafe(Order order, String countryStr) {
        if (countryStr == null || countryStr.isBlank()) return;
        try {
            order.setCountry(app_sourcing.sourcingModule.entitie.Country.valueOf(
                    countryStr.toUpperCase().replace(" ", "_").replace("-", "_")));
        } catch (Exception e) {
            log.warn("[Ingestion] Pays inconnu '{}' — ignoré.", countryStr);
        }
    }
}