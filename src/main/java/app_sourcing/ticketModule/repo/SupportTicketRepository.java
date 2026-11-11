package app_sourcing.ticketModule.repo;

import app_sourcing.ticketModule.entitie.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    Page<SupportTicket> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t ORDER BY " +
            "CASE t.priority " +
            "   WHEN 'URGENT' THEN 1 " +
            "   WHEN 'HIGH' THEN 2 " +
            "   WHEN 'MEDIUM' THEN 3 " +
            "   WHEN 'LOW' THEN 4 " +
            "END, t.createdAt DESC")
    Page<SupportTicket> findAllOrderByPriorityAndDate(Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.status = :status ORDER BY " +
            "CASE t.priority " +
            "   WHEN 'URGENT' THEN 1 " +
            "   WHEN 'HIGH' THEN 2 " +
            "   WHEN 'MEDIUM' THEN 3 " +
            "   WHEN 'LOW' THEN 4 " +
            "END, t.createdAt DESC")
    Page<SupportTicket> findByStatusOrderByPriorityAndDate(@Param("status") String status, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.assignedTo.id = :adminId ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByAssignedToId(@Param("adminId") Long adminId, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.priority = :priority ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByPriority(@Param("priority") String priority, Pageable pageable);

    @Query("SELECT t FROM SupportTicket t WHERE t.category = :category ORDER BY t.createdAt DESC")
    Page<SupportTicket> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.priority = 'URGENT' AND t.status IN ('OPEN', 'IN_PROGRESS')")
    long countUrgentOpenTickets();

    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.assignedTo.id = :adminId AND t.status IN ('OPEN', 'IN_PROGRESS')")
    long countAssignedOpenTickets(@Param("adminId") Long adminId);
}
