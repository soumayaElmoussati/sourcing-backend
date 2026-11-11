package app_sourcing.dashboardAdmin.service;

import app_sourcing.dashboardAdmin.dtos.*;
import app_sourcing.dashboardAdmin.repo.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final DashboardRepository repo;

    // ══ KPIs complets ══════════════════════════════════
    public AdminKpiDto getAdminKpis() {
        return new AdminKpiDto(
                repo.countSellers(),
                repo.countApprovedSellers(),
                repo.countPendingSellers(),

                repo.countOrders(),
                repo.countPendingOrders(),
                repo.countDeliveredOrders(),
                repo.countReturnedOrders(),
                repo.countCancelledOrders(),
                repo.totalRevenue(),
                repo.avgOrderValue(),

                repo.countProducts(),
                repo.countActiveProducts(),
                repo.countPendingProducts(),
                repo.countRejectedProducts(),

                repo.countSourcingRequests(),
                repo.countPendingSourcing(),
                repo.countCompletedSourcing()
        );
    }

    // ══ Graphes commandes ══════════════════════════════
    public List<StatusCountDto> ordersByStatus() {
        return repo.ordersByStatus().stream()
                .map(r -> new StatusCountDto(r[0].toString(), (Long) r[1]))
                .toList();
    }

    public List<MonthlyStatDto> ordersAndRevenueLast6Months() {
        return repo.ordersAndRevenueLast6Months().stream()
                .map(r -> new MonthlyStatDto(
                        ((Number) r[0]).intValue(),
                        ((Number) r[1]).intValue(),
                        ((Number) r[2]).longValue(),
                        (BigDecimal) r[3]
                ))
                .toList();
    }

    public List<CountryCountDto> ordersByCountry() {
        return repo.ordersByCountry().stream()
                .map(r -> new CountryCountDto(r[0].toString(), (Long) r[1]))
                .toList();
    }

    // ══ Graphes produits ═══════════════════════════════
    public List<StatusCountDto> productsByStatus() {
        return repo.productsByStatus().stream()
                .map(r -> new StatusCountDto(r[0].toString(), (Long) r[1]))
                .toList();
    }

    public List<TopProductDto> topProducts() {
        return repo.topProductsByOrders().stream()
                .limit(5)
                .map(r -> new TopProductDto(
                        r[0].toString(),
                        ((Number) r[1]).longValue(),
                        (BigDecimal) r[2]
                ))
                .toList();
    }

    // ══ Graphes vendeurs ═══════════════════════════════
    public List<CountryCountDto> sellersByCountry() {
        return repo.sellersByCountry().stream()
                .map(r -> new CountryCountDto(r[0].toString(), (Long) r[1]))
                .toList();
    }

    public List<StatusCountDto> sellersByStatus() {
        return repo.sellersByStatus().stream()
                .map(r -> new StatusCountDto(r[0].toString(), (Long) r[1]))
                .toList();
    }

    // ══ Graphes sourcing ═══════════════════════════════
    public List<StatusCountDto> sourcingByStatus() {
        return repo.sourcingByStatus().stream()
                .map(r -> new StatusCountDto(r[0].toString(), (Long) r[1]))
                .toList();
    }
}