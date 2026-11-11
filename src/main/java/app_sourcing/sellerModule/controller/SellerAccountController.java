package app_sourcing.sellerModule.controller;

import app_sourcing.sellerModule.dto.ChangePasswordDto;
import app_sourcing.sellerModule.dto.UpdateSellerProfileDto;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/account")
@RequiredArgsConstructor
public class SellerAccountController {

    private final SellerService sellerService;

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Seller seller,
            @Valid @RequestBody UpdateSellerProfileDto dto
    ) {
        sellerService.updateOwnProfile(seller, dto);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Profil mis à jour avec succès");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Seller seller,
            @Valid @RequestBody ChangePasswordDto dto
    ) {
        try {
            sellerService.changePassword(seller, dto);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Mot de passe modifié avec succès");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePhoto(
            @AuthenticationPrincipal Seller seller,
            @RequestParam("photo") MultipartFile photo
    ) {
        String path = sellerService.updateProfilePhoto(seller, photo);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Photo de profil mise à jour avec succès");
        response.put("profilePhotoPath", path);
        return ResponseEntity.ok(response);
    }
}
