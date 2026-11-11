package app_sourcing.ticketModule.dtos;

import lombok.Data;

@Data
public class TicketStatusUpdateRequest {
    private String status;
    private String resolution;
}
