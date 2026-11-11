package app_sourcing.userModule.controller;

import app_sourcing.userModule.dtos.UpdateProfileDto;
import app_sourcing.userModule.dtos.UserDto;
import app_sourcing.userModule.entitie.CustomUserDetails;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class ProfileController {

    public final ProfileService profileService;

    @GetMapping("/profil")
    public ResponseEntity<UserDto> getProfil(@AuthenticationPrincipal CustomUserDetails userDetails){
        System.out.println("profile");
        User user=userDetails.getUser();
        System.out.println(user);
        return ResponseEntity.ok(profileService.getProfile(user.getId()));
    }

    @PutMapping("/update-profile")
    public ResponseEntity<UserDto> post(@RequestBody UpdateProfileDto profileDto) {
        return ResponseEntity.ok(profileService.updateProfile(profileDto));
    }

}
