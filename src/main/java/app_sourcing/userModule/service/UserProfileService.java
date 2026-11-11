package app_sourcing.userModule.service;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.repo.SellerRepository;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.repo.UserRepository;
import app_sourcing.userModule.request.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public UserProfileResponse getCurrentUserProfile_old() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }


        String email = authentication.getName();
        System.out.println("email vfcddszee" + email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildUserProfileResponse(user);
    }

    /// ////
    public UserProfileResponse getCurrentUserProfile(String token) {
        // Extraire l'email directement du token
        String email = jwtTokenProvider.extractUsername(token.replace("Bearer ", ""));
        System.out.println("Email extrait directement du token: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return buildUserProfileResponse(user);
    }
    //

    public UserProfileResponse getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildUserProfileResponse(user);
    }


    private UserProfileResponse buildUserProfileResponse(User user) {
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .createdAt(user.getCreatedAt())
                .profilePhotoPath(user.getProfilePhotoPath())
                .role(getUserRole(user));

        // Si l'utilisateur est un Seller, ajouter les informations spécifiques
        if (isSeller(user)) {
            Seller seller = sellerRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Seller information not found"));

            UserProfileResponse.SellerInfo sellerInfo = UserProfileResponse.SellerInfo.builder()
                    .storeName(seller.getStoreName())
                    .country(seller.getCountry())
                    .budget(seller.getBudget())
                    .phone(seller.getPhone())
                    .commerceExperience(seller.getCommerceExperience())
                    .adsExperience(seller.getAdsExperience())
                    .status(seller.getStatus())
                    .idCardRectoPath(seller.getIdCardRectoPath())
                    .idCardVersoPath(seller.getIdCardVersoPath())
                    .build();

            builder.sellerInfo(sellerInfo);
        }

        return builder.build();
    }

    private String getUserRole(User user) {
        return user.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority())
                .orElse("ADMIN");
    }

    private boolean isSeller(User user) {
        return user.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SELLER"));
    }
}
