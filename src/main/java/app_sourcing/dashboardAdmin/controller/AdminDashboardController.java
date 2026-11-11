package app_sourcing.dashboardAdmin.controller;

import app_sourcing.dashboardAdmin.dtos.*;
import app_sourcing.dashboardAdmin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/kpis")
    public AdminKpiDto getKpis() {
        return dashboardService.getAdminKpis();
    }

    // Commandes
    @GetMapping("/orders/status")
    public List<StatusCountDto> ordersByStatus() {
        return dashboardService.ordersByStatus();
    }

    @GetMapping("/orders/monthly")
    public List<MonthlyStatDto> ordersMonthly() {
        return dashboardService.ordersAndRevenueLast6Months();
    }

    @GetMapping("/orders/country")
    public List<CountryCountDto> ordersByCountry() {
        return dashboardService.ordersByCountry();
    }

    // Produits
    @GetMapping("/products/status")
    public List<StatusCountDto> productsByStatus() {
        return dashboardService.productsByStatus();
    }

    @GetMapping("/products/top")
    public List<TopProductDto> topProducts() {
        return dashboardService.topProducts();
    }

    // Vendeurs
    @GetMapping("/sellers/country")
    public List<CountryCountDto> sellersByCountry() {
        return dashboardService.sellersByCountry();
    }

    @GetMapping("/sellers/status")
    public List<StatusCountDto> sellersByStatus() {
        return dashboardService.sellersByStatus();
    }

    // Sourcing
    @GetMapping("/sourcing/status")
    public List<StatusCountDto> sourcingByStatus() {
        return dashboardService.sourcingByStatus();
    }
}