package app_sourcing.orders.dtos;

import app_sourcing.orders.entities.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusStatsDTO {

    private Long pending;
    private Long confirmed;
    private Long prepared;
    private Long shipped;
    private Long delivered;
    private Long cancelled;
    private Long toReturn;
    private Long returned;

    public OrderStatusStatsDTO(Map<OrderStatus, Long> stats) {
        this.pending = stats.getOrDefault(OrderStatus.PENDING, 0L);
        this.confirmed = stats.getOrDefault(OrderStatus.CONFIRMED, 0L);
        this.prepared = stats.getOrDefault(OrderStatus.PREPARED, 0L);
        this.shipped = stats.getOrDefault(OrderStatus.SHIPPED, 0L);
        this.delivered = stats.getOrDefault(OrderStatus.DELIVERED, 0L);
        this.cancelled = stats.getOrDefault(OrderStatus.CANCELLED, 0L);
        this.toReturn = stats.getOrDefault(OrderStatus.TO_RETURN, 0L);
        this.returned = stats.getOrDefault(OrderStatus.RETURNED, 0L);
    }

    // Méthode pour obtenir le total
    public Long getTotal() {
        return pending + confirmed + prepared + shipped + delivered +
                cancelled + toReturn + returned;
    }

}
