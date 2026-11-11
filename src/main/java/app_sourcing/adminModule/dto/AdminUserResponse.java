package app_sourcing.adminModule.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String roleLabel;
    private boolean enabled;
    private LocalDateTime createdAt;
    private List<PermissionDto> permissions;
}