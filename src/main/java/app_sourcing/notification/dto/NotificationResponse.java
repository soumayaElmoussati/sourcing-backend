package app_sourcing.notification.dto;

import app_sourcing.notification.entiites.NotificationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter

public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    private NotificationType type;
    private List<String> targetUserEmails;

    private List<String> imgsNotifications;
    private List<String> imgsPath;
}
