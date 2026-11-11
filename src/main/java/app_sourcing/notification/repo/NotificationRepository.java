package app_sourcing.notification.repo;

import app_sourcing.notification.entiites.Notification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {
            "userNotifications",
            "userNotifications.user"
    })
    Optional<Notification> findWithUsersById(Long id);

}
