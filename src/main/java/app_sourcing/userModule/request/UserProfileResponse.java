package app_sourcing.userModule.request;


import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private LocalDateTime createdAt;
    private String profilePhotoPath;

    // Champs spécifiques au Seller (seront null si l'utilisateur n'est pas un Seller)
    private SellerInfo sellerInfo;

    @Data
    @Builder
    public static class SellerInfo {
        private String storeName;
        private String country;
        private String budget;
        private String phone;
        private Boolean commerceExperience;
        private Boolean adsExperience;
        private String status;
        private String idCardRectoPath;
        private String idCardVersoPath;
    }
}