package app_sourcing.adminModule.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CreateAdminUserRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    /** Label libre : "Support", "Finance", "Opérations"... */
    private String roleLabel;

    /** IDs des permissions à attribuer */
    private List<Long> permissionIds;

    private boolean enabled = true;
}