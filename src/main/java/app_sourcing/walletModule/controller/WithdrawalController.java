package app_sourcing.walletModule.controller;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.dtos.WithdrawalRequest;
import app_sourcing.walletModule.dtos.WithdrawalResponse;
import app_sourcing.walletModule.repo.WithdrawalRepository;
import app_sourcing.walletModule.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet/withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final WithdrawalRepository withdrawalRepository;

    @PostMapping
    public ResponseEntity<?> requestWithdrawal(
            @AuthenticationPrincipal Seller seller,
            @Valid @RequestBody WithdrawalRequest request) {
        try {
            WithdrawalResponse response = withdrawalService.requestWithdrawal(seller, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/my-withdrawals")
    public ResponseEntity<?> getMyWithdrawals(
            @AuthenticationPrincipal Seller seller,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<WithdrawalResponse> withdrawals = withdrawalService.getSellerWithdrawals(seller, pageable);
            return ResponseEntity.ok(withdrawals);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{withdrawalId}")
    public ResponseEntity<?> getWithdrawalDetails(
            @AuthenticationPrincipal Seller seller,
            @PathVariable Long withdrawalId) {
        try {
            WithdrawalResponse response = withdrawalService.getWithdrawalDetails(withdrawalId, seller);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<Page<WithdrawalResponse>> getAllWithdrawals(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(withdrawalService.getAllWithdrawals(pageable));
    }

    @GetMapping("/admin/status/{status}")
    public ResponseEntity<Page<WithdrawalResponse>> getWithdrawalsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(withdrawalService.getWithdrawalsByStatus(status, pageable));
    }


    @PostMapping("/admin/{withdrawalId}/process")
    public ResponseEntity<?> processWithdrawal(
            @PathVariable Long withdrawalId,
            @RequestParam String action, // APPROVE ou REJECT
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile proofDocument) {
        try {
            WithdrawalResponse response = withdrawalService.processWithdrawal(withdrawalId, action, reason, proofDocument);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", action.equals("APPROVE") ?
                    "Retrait approuvé avec succès, email envoyé au vendeur" : "Retrait rejeté, email envoyé au vendeur");
            successResponse.put("withdrawal", response);

            return ResponseEntity.ok(successResponse);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (java.io.IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du téléchargement du justificatif");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getWithdrawalStats() {
        // À implémenter si nécessaire
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPending", withdrawalRepository.countByStatus("PENDING"));
        stats.put("totalCompleted", withdrawalRepository.countByStatus("COMPLETED"));
        stats.put("totalRejected", withdrawalRepository.countByStatus("REJECTED"));
        return ResponseEntity.ok(stats);
    }

}
