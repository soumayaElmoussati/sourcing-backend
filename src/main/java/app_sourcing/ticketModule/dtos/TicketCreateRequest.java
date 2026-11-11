package app_sourcing.ticketModule.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TicketCreateRequest {

    @NotBlank(message = "Le sujet est obligatoire")
    @Size(max = 200, message = "Le sujet ne peut pas dépasser 200 caractères")
    private String subject;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 5000, message = "La description ne peut pas dépasser 5000 caractères")
    private String description;

    @NotBlank(message = "La catégorie est obligatoire")
    private String category;

    private String priority = "MEDIUM";

    private MultipartFile attachment;
}
