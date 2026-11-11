package app_sourcing.ticketModule.entitie;


import app_sourcing.userModule.entitie.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ticket_messages")
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // Peut être Seller ou Admin

    @Column(nullable = false, length = 5000)
    private String message;

    @Column(name = "attachments_path")
    private String attachmentsPath;

    @Column(name = "is_admin_reply")
    private boolean isAdminReply = false;

    @Column(name = "is_read")
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
