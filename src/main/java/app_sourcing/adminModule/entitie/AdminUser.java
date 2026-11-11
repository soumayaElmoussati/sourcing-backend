package app_sourcing.adminModule.entitie;

import app_sourcing.userModule.entitie.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "admin_users")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ADMIN")
public class AdminUser extends User {

    @Column(name = "role_label", length = 60)
    private String roleLabel;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "admin_user_permissions",
            joinColumns = @JoinColumn(name = "admin_user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
        return authorities;
    }
}