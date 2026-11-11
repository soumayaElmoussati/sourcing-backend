package app_sourcing.ticketModule.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private Long id;
    private String ticketNumber;
    private Long sellerId;
    private String sellerName;
    private String sellerEmail;
    private String storeName;
    private String subject;
    private String description;
    private String category;
    private String priority;
    private String status;
    private Long assignedToId;
    private String assignedToName;
    private String attachmentsPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private int messageCount;
    private List<MessageDTO> recentMessages;
}
