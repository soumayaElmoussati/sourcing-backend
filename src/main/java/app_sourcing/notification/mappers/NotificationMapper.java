package app_sourcing.notification.mappers;

import app_sourcing.notification.dto.NotificationResponse;
import app_sourcing.notification.entiites.Notification;
import app_sourcing.notification.entiites.NotificationType;
import app_sourcing.notification.entiites.UserNotification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse( Notification notification,
                                            UserNotification userNotification,
                                            boolean isAdminView) {

        NotificationResponse response = buildNotificationResponse(notification);

        // seller view
        if (userNotification != null) {
            response.setRead(userNotification.isRead());
        }

        // admin view + TARGETED
        if (isAdminView && notification.getType() == NotificationType.TARGETED) {

            List<String> emails = notification.getUserNotifications()
                    .stream()
                    .map(un -> un.getUser().getEmail())
                    .distinct()
                    .toList();

            response.setTargetUserEmails(emails);
        }

        return response;
    }

    public NotificationResponse toAdminDetailResponse(Notification notification) {

        NotificationResponse response = buildNotificationResponse(notification);
//        response.setImages(notification.getImagePaths());



        if (notification.getType() == NotificationType.TARGETED) {

            List<String> emails = notification.getUserNotifications()
                    .stream()
                    .map(un -> un.getUser().getEmail())
                    .distinct()
                    .toList();

            response.setTargetUserEmails(emails);
        }

        return response;
    }

    private NotificationResponse buildNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();

        response.setId(notification.getId());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setType(notification.getType());
        response.setCreatedAt(notification.getCreatedAt());

        response.setImgsNotifications(urlsImages(notification.getImagePaths()));
        response.setImgsPath(notification.getImagePaths());
        response.getImgsPath().forEach(System.out::println);
        return response;
    }

    private List<String> urlsImages(List<String> imgsPaths){
        List<String> urls=new ArrayList<>();
        imgsPaths.forEach(img-> {
            urls.add("http://localhost:8080/uploads/"+img);
        });
        return urls;
    }

}
