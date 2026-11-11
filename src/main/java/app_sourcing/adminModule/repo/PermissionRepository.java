package app_sourcing.adminModule.repo;

import app_sourcing.adminModule.entitie.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findAllByOrderByCategoryAscNameAsc();
}