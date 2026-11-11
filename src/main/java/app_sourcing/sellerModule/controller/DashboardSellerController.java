package app_sourcing.sellerModule.controller;

import app_sourcing.orders.dtos.OrderStatusStatsDTO;
import app_sourcing.orders.entities.OrderStatus;
import app_sourcing.orders.repo.OrderRepository;
import app_sourcing.orders.services.OrderService;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.repo.SellerWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sellers/report")
@RequiredArgsConstructor
public class DashboardSellerController {

    private final OrderRepository       orderRepository;
    private final OrderService          orderService;
    private final SellerWalletRepository walletRepository;

    // ── Existants ─────────────────────────────────────────────────────────────

    @GetMapping("/orders/total")
    public ResponseEntity<Long> getTotalOrders(@AuthenticationPrincipal Seller seller) {
        return ResponseEntity.ok(orderRepository.countBySeller(seller));
    }

    @GetMapping("/orders/total-month")
    public ResponseEntity<Long> getTotalOrdersForCurrentMonth(@AuthenticationPrincipal Seller seller) {
        return ResponseEntity.ok(orderService.getTotalOrdersForCurrentMonth(seller));
    }

    @GetMapping("/orders/status-map")
    public ResponseEntity<Map<String, Long>> getOrdersStatusMap(@AuthenticationPrincipal Seller seller) {
        OrderStatusStatsDTO stats = orderService.getOrderStatsByStatus(seller);

        Map<String, Long> response = new HashMap<>();
        response.put("PENDING",    stats.getPending());
        response.put("CONFIRMED",  stats.getConfirmed());
        response.put("PREPARED",   stats.getPrepared());
        response.put("SHIPPED",    stats.getShipped());
        response.put("DELIVERED",  stats.getDelivered());
        response.put("CANCELLED",  stats.getCancelled());
        response.put("TO_RETURN",  stats.getToReturn());
        response.put("RETURNED",   stats.getReturned());
        response.put("TOTAL",      stats.getTotal());

        return ResponseEntity.ok(response);
    }

    // ── Nouveau : dashboard complet en un seul appel ──────────────────────────

    /**
     * GET /api/sellers/report/dashboard
     * Retourne toutes les métriques du dashboard en un seul appel.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @AuthenticationPrincipal Seller seller) {

        OrderStatusStatsDTO stats = orderService.getOrderStatsByStatus(seller);
        Long total       = stats.getTotal();
        Long delivered   = stats.getDelivered();
        Long cancelled   = stats.getCancelled();
        Long returned    = stats.getReturned();
        Long monthlyOrders = orderService.getTotalOrdersForCurrentMonth(seller);

        // Taux de livraison (delivered / (total - cancelled))
        long effective = total - cancelled;
        double deliveryRate = (effective > 0)
                ? Math.round((delivered * 100.0 / effective) * 10.0) / 10.0
                : 0.0;

        // Taux de retour
        double returnRate = (delivered > 0)
                ? Math.round((returned * 100.0 / delivered) * 10.0) / 10.0
                : 0.0;

        // Solde wallet
        double walletBalance = walletRepository
                .findBySellerId(seller.getId())
                .map(w -> w.getBalance())
                .orElse(0.0);

        Map<String, Object> dashboard = new HashMap<>();

        // KPIs
        dashboard.put("totalOrders",    total);
        dashboard.put("monthlyOrders",  monthlyOrders);
        dashboard.put("delivered",      delivered);
        dashboard.put("deliveryRate",   deliveryRate);
        dashboard.put("returnRate",     returnRate);
        dashboard.put("walletBalance",  walletBalance);

        // Status map (pour pie chart)
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("PENDING",   stats.getPending());
        statusMap.put("CONFIRMED", stats.getConfirmed());
        statusMap.put("PREPARED",  stats.getPrepared());
        statusMap.put("SHIPPED",   stats.getShipped());
        statusMap.put("DELIVERED", stats.getDelivered());
        statusMap.put("CANCELLED", stats.getCancelled());
        statusMap.put("TO_RETURN", stats.getToReturn());
        statusMap.put("RETURNED",  stats.getReturned());
        dashboard.put("statusMap", statusMap);

        return ResponseEntity.ok(dashboard);
    }
}