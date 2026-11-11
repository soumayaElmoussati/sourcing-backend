package app_sourcing.ticketModule.dtos;

import lombok.Data;

@Data
public class TicketAssignmentRequest {
    private Long adminId;
    private String priority;
}
