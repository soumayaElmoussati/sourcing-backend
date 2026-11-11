package app_sourcing.dashboardAdmin.dtos;

import java.math.BigDecimal;

public record AdminKpiDto(
        // Sellers
        long totalSellers,
        long approvedSellers,
        long pendingSellers,

        // Orders
        long totalOrders,
        long pendingOrders,
        long deliveredOrders,
        long returnedOrders,
        long cancelledOrders,
        BigDecimal totalRevenue,
        BigDecimal avgOrderValue,

        // Products
        long totalProducts,
        long activeProducts,
        long pendingProducts,
        long rejectedProducts,

        // Sourcing
        long totalSourcingRequests,
        long pendingSourcing,
        long completedSourcing
) {}