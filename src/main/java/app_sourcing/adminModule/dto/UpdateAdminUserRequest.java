package app_sourcing.adminModule.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateAdminUserRequest {
    private String firstName;
    private String lastName;
    private String roleLabel;
    private List<Long> permissionIds;
    private Boolean enabled;
}