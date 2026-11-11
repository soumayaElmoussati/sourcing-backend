package app_sourcing.sellerModule.dto;


import app_sourcing.sellerModule.entitie.Seller;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SellerDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String storeName;
    private String country;
    private String budget;
    private Boolean commerceExperience;
    private Boolean adsExperience;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;

    @NotBlank(message = "First name is required", groups = {CreateValidation.class, UpdateValidation.class})
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters", groups = {CreateValidation.class, UpdateValidation.class})
    private transient String firstNameInput;

    @NotBlank(message = "Last name is required", groups = {CreateValidation.class, UpdateValidation.class})
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters", groups = {CreateValidation.class, UpdateValidation.class})
    private transient String lastNameInput;

    @NotBlank(message = "Phone is required", groups = {CreateValidation.class, UpdateValidation.class})
    @jakarta.validation.constraints.Pattern(
            regexp = "^[+]?[0-9]{10,15}$",
            message = "Invalid phone number",
            groups = {CreateValidation.class, UpdateValidation.class}
    )
    private transient String phoneInput;

    @NotBlank(message = "Email is required", groups = {CreateValidation.class})
    @Email(message = "Invalid email format", groups = {CreateValidation.class, UpdateValidation.class})
    private transient String emailInput;

    @NotBlank(message = "Password is required", groups = {CreateValidation.class})
    @Size(min = 6, message = "Password must be at least 6 characters", groups = {CreateValidation.class})
    private transient String passwordInput;

    @NotBlank(message = "Store name is required", groups = {CreateValidation.class, UpdateValidation.class})
    @Size(min = 2, max = 100, message = "Store name must be between 2 and 100 characters", groups = {CreateValidation.class, UpdateValidation.class})
    private transient String storeNameInput;

    @NotBlank(message = "Country is required", groups = {CreateValidation.class, UpdateValidation.class})
    private transient String countryInput;

    private transient String budgetInput;
    private transient Boolean commerceExperienceInput;
    private transient Boolean adsExperienceInput;

    // Signature & Contrat
    private transient String signatureBase64Input;

    // Fichiers (transient car pas sérialisés en JSON)
    private transient MultipartFile idCardRecto;
    private transient MultipartFile idCardVerso;

    // Groupes de validation
    public interface CreateValidation {}
    public interface UpdateValidation {}

    // Méthodes de conversion
    public static SellerDTO fromEntity(Seller seller) {
        SellerDTO dto = new SellerDTO();
        dto.setId(seller.getId());
        dto.setFirstName(seller.getFirstName());
        dto.setLastName(seller.getLastName());
        dto.setPhone(seller.getPhone());
        dto.setEmail(seller.getEmail());
        dto.setStoreName(seller.getStoreName());
        dto.setCountry(seller.getCountry());
        dto.setBudget(seller.getBudget());
        dto.setCommerceExperience(seller.getCommerceExperience());
        dto.setAdsExperience(seller.getAdsExperience());
        dto.setStatus(seller.getStatus());
        dto.setCreatedAt(seller.getCreatedAt());
        dto.setUpdatedAt(seller.getUpdatedAt());
        dto.setDeleted(seller.isDeleted());
        dto.setDeletedAt(seller.getDeletedAt());
        return dto;
    }

    public Seller toEntity() {
        Seller seller = new Seller();
        updateEntity(seller);
        return seller;
    }

    public void updateEntity(Seller seller) {
        if (firstNameInput != null) seller.setFirstName(firstNameInput);
        if (lastNameInput != null) seller.setLastName(lastNameInput);
        if (phoneInput != null) seller.setPhone(phoneInput);
        if (emailInput != null) seller.setEmail(emailInput);
        if (storeNameInput != null) seller.setStoreName(storeNameInput);
        if (countryInput != null) seller.setCountry(countryInput);
        if (budgetInput != null) seller.setBudget(budgetInput);
        if (commerceExperienceInput != null) seller.setCommerceExperience(commerceExperienceInput);
        if (adsExperienceInput != null) seller.setAdsExperience(adsExperienceInput);
        if (signatureBase64Input != null && !signatureBase64Input.isBlank()) {
            seller.setSignatureBase64(signatureBase64Input);
            seller.setSignedAt(java.time.LocalDateTime.now());
            seller.setContractVersion("v1.0");
        }
    }
}