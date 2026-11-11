package app_sourcing.notification.dto;

import app_sourcing.notification.entiites.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NotificationRequest {

    private String title;
    private String message;
    private NotificationType type;

    // seulement pour TARGETED
    private List<String> emails;

    private List<String> existingImages = new ArrayList<>();
}
