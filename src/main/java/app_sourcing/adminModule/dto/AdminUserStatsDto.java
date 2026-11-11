package app_sourcing.adminModule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AdminUserStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
}