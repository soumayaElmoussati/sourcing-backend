package app_sourcing.sellerModule.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SellerWithDocumentsDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String storeName;
    private String country;
    private String budget;
    private boolean commerceExperience;
    private boolean adsExperience;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SellerDocumentDTO> documents;
}
