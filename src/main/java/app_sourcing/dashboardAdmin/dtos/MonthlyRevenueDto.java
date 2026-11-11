package app_sourcing.dashboardAdmin.dtos;

import java.math.BigDecimal;

public record MonthlyRevenueDto(
        int month,
        BigDecimal revenue
) {}
