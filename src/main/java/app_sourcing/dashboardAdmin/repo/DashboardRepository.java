package app_sourcing.dashboardAdmin.repo;

import app_sourcing.orders.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface DashboardRepository extends JpaRepository<Order, String> {

    // ══════════════ KPIs SELLERS ══════════════
    @Query("SELECT COUNT(s) FROM Seller s WHERE s.deleted = false")
    long countSellers();

    @Query("SELECT COUNT(s) FROM Seller s WHERE s.deleted = false AND s.status = 'approved'")
    long countApprovedSellers();

    @Query("SELECT COUNT(s) FROM Seller s WHERE s.deleted = false AND s.status = 'pending'")
    long countPendingSellers();

    // ══════════════ KPIs ORDERS ══════════════
    @Query("SELECT COUNT(o) FROM Order o")
    long countOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING'")
    long countPendingOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
    long countDeliveredOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'RETURNED'")
    long countReturnedOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'CANCELLED'")
    long countCancelledOrders();

    @Query("""
        SELECT COALESCE(SUM(o.totalPrice), 0)
        FROM Order o
        WHERE o.status = 'DELIVERED'
    """)
    BigDecimal totalRevenue();

    @Query("""
        SELECT COALESCE(AVG(o.totalPrice), 0)
        FROM Order o
        WHERE o.status = 'DELIVERED'
    """)
    BigDecimal avgOrderValue();

    // ══════════════ KPIs PRODUCTS ══════════════
    @Query("SELECT COUNT(p) FROM Product p")
    long countProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'PENDING_APPROVAL'")
    long countPendingProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'REJECTED'")
    long countRejectedProducts();

    // ══════════════ KPIs SOURCING ══════════════
    @Query("SELECT COUNT(sr) FROM SourcingRequest sr WHERE sr.isArchive = false")
    long countSourcingRequests();

    @Query("SELECT COUNT(sr) FROM SourcingRequest sr WHERE sr.status = 'PENDING' AND sr.isArchive = false")
    long countPendingSourcing();

    @Query("SELECT COUNT(sr) FROM SourcingRequest sr WHERE sr.status = 'COMPLETED' AND sr.isArchive = false")
    long countCompletedSourcing();

    // ══════════════ CHARTS ORDERS ══════════════
    @Query("""
        SELECT o.status, COUNT(o)
        FROM Order o
        GROUP BY o.status
    """)
    List<Object[]> ordersByStatus();

    @Query(value = """
        SELECT MONTH(o.created_at), YEAR(o.created_at),
               COUNT(o.id), COALESCE(SUM(o.total_price), 0)
        FROM orders o
        WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH)
        GROUP BY YEAR(o.created_at), MONTH(o.created_at)
        ORDER BY YEAR(o.created_at), MONTH(o.created_at)
    """, nativeQuery = true)
    List<Object[]> ordersAndRevenueLast6Months();

    @Query(value = """
        SELECT o.country, COUNT(o.id)
        FROM orders o
        GROUP BY o.country
        ORDER BY COUNT(o.id) DESC
    """, nativeQuery = true)
    List<Object[]> ordersByCountry();

    // ══════════════ CHARTS PRODUCTS ══════════════
    @Query("""
        SELECT p.status, COUNT(p)
        FROM Product p
        GROUP BY p.status
    """)
    List<Object[]> productsByStatus();

    @Query("""
        SELECT p.nameProduct, COUNT(o), SUM(o.totalPrice)
        FROM Order o JOIN o.product p
        WHERE o.status = 'DELIVERED'
        GROUP BY p.id, p.nameProduct
        ORDER BY COUNT(o) DESC
    """)
    List<Object[]> topProductsByOrders();

    // ══════════════ CHARTS SELLERS ══════════════
    @Query("""
        SELECT s.country, COUNT(s)
        FROM Seller s
        WHERE s.deleted = false
        GROUP BY s.country
        ORDER BY COUNT(s) DESC
    """)
    List<Object[]> sellersByCountry();

    @Query("""
        SELECT s.status, COUNT(s)
        FROM Seller s
        WHERE s.deleted = false
        GROUP BY s.status
    """)
    List<Object[]> sellersByStatus();

    // ══════════════ CHARTS SOURCING ══════════════
    @Query("""
        SELECT sr.status, COUNT(sr)
        FROM SourcingRequest sr
        WHERE sr.isArchive = false
        GROUP BY sr.status
    """)
    List<Object[]> sourcingByStatus();
}