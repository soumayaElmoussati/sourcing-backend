package app_sourcing.sellerModule.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SellerDocumentDTO {

    private String name;
    private String type;
    private String url;
    private LocalDateTime uploadedAt;
    private boolean verified;
}
