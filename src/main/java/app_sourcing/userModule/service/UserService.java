package app_sourcing.userModule.service;

import app_sourcing.userModule.dtos.CreateUserRequest;
import app_sourcing.userModule.dtos.UserDto;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto createUser(CreateUserRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà");
        }

        // Créer un nouvel utilisateur
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Ici vous pourriez ajouter une logique pour gérer différents rôles
        // Pour l'instant, tous les utilisateurs ont le rôle "USER" par défaut

        User savedUser = userRepository.save(user);

        return convertToDto(savedUser, request.getRole());
    }

    public UserDto createAdminUser(CreateUserRequest request) {
        // Forcer le rôle admin
        request.setRole("ADMIN");

        // Logique supplémentaire pour les admins si nécessaire
        UserDto admin = createUser(request);

        // Note: Dans votre entité User actuelle, le rôle est hardcodé à "USER"
        // Vous devriez modifier votre entité pour gérer plusieurs rôles
        // Exemple avec une entité Role séparée

        return admin;
    }

    private UserDto convertToDto(User user, String role) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRole(role);
        dto.setEnabled(user.isEnabled());
        dto.setAccountNonExpired(user.isAccountNonExpired());
        dto.setAccountNonLocked(user.isAccountNonLocked());
        dto.setCredentialsNonExpired(user.isCredentialsNonExpired());

        return dto;
    }
}
