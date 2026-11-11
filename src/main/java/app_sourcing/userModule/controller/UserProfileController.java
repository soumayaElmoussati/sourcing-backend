package app_sourcing.userModule.controller;

import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.request.UserProfileResponse;
import app_sourcing.userModule.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;


    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @RequestHeader("Authorization") String token) {  // ← Récupérer le token du header

        System.out.println("Token reçu: " + token);
        UserProfileResponse profile = userProfileService.getCurrentUserProfile(token);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserProfileResponse> getUserProfileById(@PathVariable Long id) {
        UserProfileResponse profile = userProfileService.getUserProfileById(id);
        return ResponseEntity.ok(profile);
    }



}
