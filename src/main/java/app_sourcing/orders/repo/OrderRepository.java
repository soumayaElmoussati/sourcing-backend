package app_sourcing.orders.repo;

import app_sourcing.orders.entities.Customer;
import app_sourcing.orders.entities.Order;
import app_sourcing.orders.entities.OrderSource;
import app_sourcing.sellerModule.entitie.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findBySellerId(Long sellerId);
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    Optional<Order> findByIdAndSellerId(String orderId,Long sellerId);

    Long countBySeller(Seller seller);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.seller = :seller " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE)")
    Long countBySellerForCurrentMonth(@Param("seller") Seller seller);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.seller = :seller GROUP BY o.status")
    List<Object[]> countByStatusForSeller(@Param("seller") Seller seller);

    boolean existsBySellerAndSourceAndExternalOrderId(Seller seller, app_sourcing.orders.entities.OrderSource source, String externalOrderId);


    /**
     * Anti-doublon : vérifie qu'un ordre avec cet ID externe + cette source
     * n'existe pas déjà (évite les doublons en cas de webhook renvoyé plusieurs fois).
     */
    boolean existsByExternalOrderIdAndSource(String externalOrderId, OrderSource source);

    /**
     * Retrouver une commande par son ID externe.
     */
    Optional<Order> findByExternalOrderIdAndSource(String externalOrderId, OrderSource source);

}
