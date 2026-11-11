package app_sourcing.adminModule.controller;

import app_sourcing.adminModule.dto.*;
import app_sourcing.adminModule.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    // ── CRUD ────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody CreateAdminUserRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createAdminUser(req));
    }

    @GetMapping
    public Page<AdminUserResponse> getAll(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminUserService.getAll(
                q.isBlank() ? null : q,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{id}")
    public AdminUserResponse getById(@PathVariable Long id) {
        return adminUserService.getById(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(@PathVariable Long id, @RequestBody UpdateAdminUserRequest req) {
        return adminUserService.update(id, req);
    }

    @PatchMapping("/{id}/toggle-status")
    public AdminUserResponse toggleStatus(@PathVariable Long id) {
        return adminUserService.toggleStatus(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── STATS & CATALOG ─────────────────────────────────────

    @GetMapping("/stats")
    public AdminUserStatsDto getStats() {
        return adminUserService.getStats();
    }

    @GetMapping("/permissions-catalog")
    public Map<String, List<PermissionDto>> getPermissionsCatalog() {
        return adminUserService.getPermissionsCatalog();
    }
}