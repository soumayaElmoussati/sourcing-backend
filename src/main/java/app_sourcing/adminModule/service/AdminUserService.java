package app_sourcing.adminModule.service;

import app_sourcing.adminModule.dto.*;
import app_sourcing.adminModule.entitie.AdminUser;
import app_sourcing.adminModule.entitie.Permission;
import app_sourcing.adminModule.repo.AdminUserRepository;
import app_sourcing.adminModule.repo.PermissionRepository;
import app_sourcing.sellerModule.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ── CREATE ─────────────────────────────────────────────
    public AdminUserResponse createAdminUser(CreateAdminUserRequest req) {
        if (adminUserRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email déjà utilisé : " + req.getEmail());

        // Générer un mot de passe aléatoire si non fourni
        String rawPassword = generatePassword();

        AdminUser user = new AdminUser();
        user.setEmail(req.getEmail());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setRoleLabel(req.getRoleLabel());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(req.isEnabled());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Permissions
        if (req.getPermissionIds() != null && !req.getPermissionIds().isEmpty()) {
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(req.getPermissionIds()));
            user.setPermissions(perms);
        }

        AdminUser saved = adminUserRepository.save(user);

        // Envoyer email avec les credentials
        try {
            emailService.sendAdminWelcomeEmail(
                    saved.getEmail(),
                    saved.getFirstName() + " " + saved.getLastName(),
                    rawPassword,
                    saved.getPermissions().stream().map(Permission::getName).collect(Collectors.toList())
            );
        } catch (Exception e) {
            System.err.println("Failed to send admin welcome email: " + e.getMessage());
        }

        return toResponse(saved);
    }

    // ── READ ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAll(String search, Pageable pageable) {
        return adminUserRepository.search(search, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public AdminUserStatsDto getStats() {
        return new AdminUserStatsDto(
                adminUserRepository.countByDeletedFalse(),
                adminUserRepository.countByDeletedFalseAndEnabledTrue(),
                adminUserRepository.countByDeletedFalseAndEnabledFalse()
        );
    }

    // ── UPDATE ──────────────────────────────────────────────
    public AdminUserResponse update(Long id, UpdateAdminUserRequest req) {
        AdminUser user = findOrThrow(id);

        if (req.getFirstName()  != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()   != null) user.setLastName(req.getLastName());
        if (req.getRoleLabel()  != null) user.setRoleLabel(req.getRoleLabel());
        if (req.getEnabled()    != null) user.setEnabled(req.getEnabled());

        if (req.getPermissionIds() != null) {
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(req.getPermissionIds()));
            user.setPermissions(perms);
        }

        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(adminUserRepository.save(user));
    }

    public AdminUserResponse toggleStatus(Long id) {
        AdminUser user = findOrThrow(id);
        user.setEnabled(!user.isEnabled());
        return toResponse(adminUserRepository.save(user));
    }

    // ── DELETE ──────────────────────────────────────────────
    public void delete(Long id) {
        AdminUser user = findOrThrow(id);
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        adminUserRepository.save(user);
    }

    // ── PERMISSIONS CATALOG ─────────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, List<PermissionDto>> getPermissionsCatalog() {
        return permissionRepository.findAllByOrderByCategoryAscNameAsc()
                .stream()
                .map(this::toPermissionDto)
                .collect(Collectors.groupingBy(PermissionDto::getCategory));
    }

    // ── HELPERS ─────────────────────────────────────────────
    private AdminUser findOrThrow(Long id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur admin introuvable : " + id));
    }

    private AdminUserResponse toResponse(AdminUser u) {
        AdminUserResponse r = new AdminUserResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setFirstName(u.getFirstName());
        r.setLastName(u.getLastName());
        r.setFullName(u.getFirstName() + " " + u.getLastName());
        r.setRoleLabel(u.getRoleLabel());
        r.setEnabled(u.isEnabled());
        r.setCreatedAt(u.getCreatedAt());
        r.setPermissions(u.getPermissions().stream().map(this::toPermissionDto).toList());
        return r;
    }

    private PermissionDto toPermissionDto(Permission p) {
        PermissionDto d = new PermissionDto();
        d.setId(p.getId());
        d.setName(p.getName());
        d.setDescription(p.getDescription());
        d.setCategory(p.getCategory());
        return d;
    }

    private String generatePassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        StringBuilder sb = new StringBuilder(12);
        Random rnd = new Random();
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}