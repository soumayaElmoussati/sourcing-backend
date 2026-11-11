package app_sourcing.notification.controller;

import app_sourcing.notification.dto.NotificationRequest;
import app_sourcing.notification.dto.NotificationResponse;
import app_sourcing.notification.entiites.Notification;
import app_sourcing.notification.mappers.NotificationMapper;
import app_sourcing.notification.service.NotificationService;
import app_sourcing.userModule.entitie.User;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ADMIN
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createNotification(
            @RequestPart("data") NotificationRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        notificationService.createNotification(request,images);
        return ResponseEntity.ok().build();
    }

    // SELLER
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/admin")
// @PreAuthorize("hasRole('ADMIN')")
    public Page<NotificationResponse> adminNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationService.getAllNotificationsForAdmin(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
               notificationService.getNotificationbyId(id)
        );
    }

    @GetMapping("/{id}/image/{index}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long id,
            @PathVariable int index
    ) throws IOException, MalformedURLException {


        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // ou dynamique
                .body(notificationService.getRessource(id,index));
    }


    // SELLER
    @GetMapping("/me")
//    @PreAuthorize("hasRole('SELLER')")
    public List<NotificationResponse> myNotifications(@AuthenticationPrincipal User user) {
        return notificationService.getUserNotifications(user.getId());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,@AuthenticationPrincipal User user) {
        notificationService.markAsRead(id,user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )

    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestPart("data") NotificationRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException, java.io.IOException {

        notificationService.updateNotification(id, request, images);
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

}
