package app_sourcing.notification.entiites;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type; // GLOBAL / TARGETED

    @ElementCollection()
    @CollectionTable(
            name = "notification_images",
            joinColumns = @JoinColumn(name = "notification_id")
    )
    @Column(name = "image_path")
    private List<String> imagePaths = new ArrayList<>();


    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL)
    private List<UserNotification> userNotifications = new ArrayList<>();

    private LocalDateTime createdAt;
}
