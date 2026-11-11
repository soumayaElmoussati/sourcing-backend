package app_sourcing.notification.service;

import app_sourcing.notification.dto.NotificationRequest;
import app_sourcing.notification.dto.NotificationResponse;
import app_sourcing.notification.entiites.Notification;
import app_sourcing.notification.entiites.NotificationType;
import app_sourcing.notification.entiites.UserNotification;
import app_sourcing.notification.mappers.NotificationMapper;
import app_sourcing.notification.repo.NotificationRepository;
import app_sourcing.notification.repo.UserNotificationRepository;
import app_sourcing.productModule.exeption.BusinessProductException;
import app_sourcing.sellerModule.service.FileStorageService;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.repo.UserRepository;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;
    private final FileStorageService fileStorageService;

    public void createNotification(NotificationRequest request, List<MultipartFile> images) {

        List<String> imagePaths = fileStorageService.storeFiles(images,"notification");

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .imagePaths(imagePaths)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        if (request.getType() == NotificationType.GLOBAL) {
            userRepository.findAll()
                    .forEach(user -> createUserNotification(user, notification));
        } else {
            request.getEmails().forEach(email -> {
                User user = userRepository.findByEmail(email).orElseThrow();
                createUserNotification(user, notification);
            });
        }
    }

    private void createUserNotification(User user, Notification notification) {
        UserNotification un = UserNotification.builder()
                .user(user)
                .notification(notification)
                .read(false)
                .build();
        userNotificationRepository.save(un);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {

        List<UserNotification> userNotifs = userNotificationRepository.findByUserIdOrderByNotification_CreatedAtDesc(userId);

        return userNotifs.stream()
                .map(un -> mapper.toResponse(
                        un.getNotification(),
                        un,
                        false
                ))
                .toList();
    }

    public Notification getNotificationById(Long id) {

        return notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Notification not found with id " + id));
    }

    public NotificationResponse getNotificationbyId(Long id){
        Notification notification=getNotificationById(id);
        return mapper.toAdminDetailResponse(notification);
    }

    public Page<NotificationResponse> getAllNotificationsForAdmin(Pageable pageable) {
        Page<Notification> notificationsPage = notificationRepository.findAll(pageable);

        return notificationsPage.map(notification ->
                mapper.toResponse(notification, null, true)
        );
    }

    public void markAsRead(Long notificationId,Long userId) {
        UserNotification un = userNotificationRepository
                .findByNotificationIdAndUserId(notificationId,userId)
                .orElseThrow();

        un.setRead(true);
        un.setReadAt(LocalDateTime.now());
        userNotificationRepository.save(un);
    }

    @Transactional
    public void updateNotification(
            Long id,
            NotificationRequest request,
            List<MultipartFile> newImages
    ) throws IOException, java.io.IOException {

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

    /* =============================
       1️⃣ UPDATE TEXT DATA
       ============================= */

        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());

    /* =============================
       2️⃣ IMAGES : SUPPRESSION
       ============================= */

        List<String> oldImages = new ArrayList<>(notification.getImagePaths());

        // éviter NPE
        List<String> keepImages = request.getExistingImages() != null
                ? request.getExistingImages()
                : List.of();


        // images supprimées = anciennes - images conservées
        List<String> imagesToDelete = oldImages.stream()
                .filter(img -> !keepImages.contains(img))
                .toList();


        for (String img : imagesToDelete) {
            System.out.println("img : "+img);
            fileStorageService.deleteFile(img);
            notification.getImagePaths().remove(img);
        }

    /* =============================
       3️⃣ IMAGES : AJOUT
       ============================= */

        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile file : newImages) {
                System.out.println("file : "+file.getName());
                String savedFileName = fileStorageService.storeFile(file,"notification");
                notification.getImagePaths().add(savedFileName);
            }
        }

        notificationRepository.save(notification);

    /* =============================
       4️⃣ USERS : RESET
       ============================= */

        userNotificationRepository.deleteByNotificationId(id);

        if (request.getType() == NotificationType.GLOBAL) {

            userRepository.findAll()
                    .forEach(user -> createUserNotification(user, notification));

        } else {

            request.getEmails().forEach(email -> {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                createUserNotification(user, notification);
            });
        }
    }


    public void deleteNotification(Long id) {

        // delete relations first
        userNotificationRepository.deleteByNotificationId(id);

        // delete notification
        notificationRepository.deleteById(id);
    }

    public List<String> getListImages(Long id){
        List<String> imgs=new ArrayList<>();
        Notification notif=notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Notifs not found"));
        if(notif==null)
            throw new RuntimeException("Notif not found");
        else if (notif.getImagePaths().isEmpty()) {
            return imgs;
        }
        else{
            notif.getImagePaths().forEach(img->{
                imgs.add("/uploads/"+img);
            });

            return imgs;
        }

    }

    public Resource getRessource(Long id,int index) throws MalformedURLException {
        Notification notif = notificationRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Notification not found with id " + id));

        String imagePath = notif.getImagePaths().get(index);

        Path path = Paths.get(imagePath.replaceFirst("^/uploads", ""));
        return new UrlResource(path.toUri());
    }

}
