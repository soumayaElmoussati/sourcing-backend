package app_sourcing.adminModule.repo;

import app_sourcing.adminModule.entitie.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM AdminUser u
        WHERE u.deleted = false
        AND (:q IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:q,'%'))
             OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:q,'%'))
             OR LOWER(u.email)     LIKE LOWER(CONCAT('%',:q,'%')))
    """)
    Page<AdminUser> search(@Param("q") String q, Pageable pageable);

    long countByDeletedFalseAndEnabledTrue();
    long countByDeletedFalseAndEnabledFalse();
    long countByDeletedFalse();
}