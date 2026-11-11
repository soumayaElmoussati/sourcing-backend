package app_sourcing.ticketModule.dtos;

import lombok.Data;

@Data
public class TicketStatsDTO {
    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long resolvedTickets;
    private long closedTickets;
    private long urgentTickets;
    private long highPriorityTickets;
    private long unreadMessages;
}
