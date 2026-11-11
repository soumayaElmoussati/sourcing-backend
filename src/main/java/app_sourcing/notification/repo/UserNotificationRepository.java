package app_sourcing.notification.repo;

import app_sourcing.notification.entiites.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository
        extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserIdOrderByNotification_CreatedAtDesc(Long userId);

    Optional<UserNotification> findByNotificationIdAndUserId(Long notificationId,Long userId);

    void deleteByNotificationId(Long notificationId);
}
