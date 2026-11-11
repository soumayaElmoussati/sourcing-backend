package app_sourcing.ticketModule.repo;

import app_sourcing.ticketModule.entitie.TicketMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    Page<TicketMessage> findByTicketIdOrderByCreatedAtDesc(Long ticketId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM TicketMessage m WHERE m.ticket.id = :ticketId AND m.isAdminReply = true AND m.isRead = false")
    long countUnreadAdminReplies(@Param("ticketId") Long ticketId);

    @Query("SELECT COUNT(m) FROM TicketMessage m WHERE m.ticket.seller.id = :sellerId AND m.isAdminReply = true AND m.isRead = false")
    long countUnreadAdminRepliesForSeller(@Param("sellerId") Long sellerId);

    @Modifying
    @Transactional
    @Query("UPDATE TicketMessage m SET m.isRead = true WHERE m.ticket.id = :ticketId AND m.isAdminReply = true AND m.isRead = false")
    void markAdminRepliesAsRead(@Param("ticketId") Long ticketId);

    @Modifying
    @Transactional
    @Query("UPDATE TicketMessage m SET m.isRead = true WHERE m.ticket.id = :ticketId AND m.sender.id = :adminId AND m.isRead = false")
    void markMessagesAsReadByAdmin(@Param("ticketId") Long ticketId, @Param("adminId") Long adminId);
}
