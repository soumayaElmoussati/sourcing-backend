package app_sourcing.appsModule.repository;

import app_sourcing.appsModule.entitie.AppConnection;
import app_sourcing.appsModule.entitie.ConnectionStatus;
import app_sourcing.orders.entities.OrderSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppConnectionRepository extends JpaRepository<AppConnection, Long> {

    List<AppConnection> findBySellerId(Long sellerId);

    Optional<AppConnection> findBySellerIdAndAppType(Long sellerId, OrderSource appType);

    Optional<AppConnection> findByWebhookToken(String webhookToken);

    List<AppConnection> findByAppTypeAndStatus(OrderSource appType, ConnectionStatus status);

    // ✅ Ajout pour le webhook OAuth push (Lightfunnels, XPage)
    Optional<AppConnection> findByAppTypeAndExternalAccountId(OrderSource appType, String externalAccountId);
}