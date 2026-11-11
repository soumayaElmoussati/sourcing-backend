package app_sourcing.sourcingModule.repo;

import app_sourcing.sourcingModule.entitie.RequestStatus;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SourcingRequestRepository 
        extends JpaRepository<SourcingRequest, String> {

    Page<SourcingRequest> findBySellerId(Long sellerId, Pageable pageable);
    List<SourcingRequest> findBySellerId(Long sellerId);

    // Admin : toutes les demandes
    List<SourcingRequest> findByStatus(RequestStatus status);

    // ================= STATS =================

    long countByStatus(RequestStatus status);

    @Query("""
        SELECT COALESCE(SUM(sr.totalPrice), 0)
        FROM SourcingRequest sr
        WHERE sr.status IN ('APPROVED','COMPLETED')
    """)
    BigDecimal sumTotalAmount();

}
