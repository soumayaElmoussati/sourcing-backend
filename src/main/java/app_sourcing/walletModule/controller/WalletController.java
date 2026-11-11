package app_sourcing.walletModule.controller;

import app_sourcing.userModule.entitie.User;
import app_sourcing.walletModule.dtos.DepositRequest;
import app_sourcing.walletModule.entitie.Deposit;
import app_sourcing.walletModule.entitie.SellerWallet;
import app_sourcing.walletModule.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(
            @AuthenticationPrincipal User seller,
            @Valid @ModelAttribute DepositRequest request
    ) {
        try {
            Deposit deposit = walletService.createDeposit(seller, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dépôt créé avec succès");
            response.put("depositId", deposit.getId());
            response.put("paymentMethodType", deposit.getPaymentMethodType());
            response.put("status", deposit.getStatus());
            response.put("createdAt", deposit.getCreatedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du téléchargement du fichier");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Récupérer le wallet avec solde
    @GetMapping
    public ResponseEntity<?> getWallet(
            @AuthenticationPrincipal User seller
    ) {
        try {
            SellerWallet wallet = walletService.getSellerWallet(seller);

            Map<String, Object> response = new HashMap<>();
            response.put("walletId", wallet.getId());
            response.put("balance", wallet.getBalance());
            response.put("sellerId", wallet.getSeller().getId());
            response.put("createdAt", wallet.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    // Récupérer tous les dépôts du seller
    @GetMapping("/deposits")
    public ResponseEntity<?> getDeposits(
            @AuthenticationPrincipal User seller
    ) {
        try {
            List<Deposit> deposits = walletService.getSellerDeposits(seller);
            return ResponseEntity.ok(deposits);

        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Vérifier si un wallet existe
    @GetMapping("/exists")
    public ResponseEntity<?> checkWalletExists(
            @AuthenticationPrincipal User seller
    ) {
        boolean exists = walletService.hasWallet(seller);

        Map<String, Object> response = new HashMap<>();
        response.put("hasWallet", exists);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/debit")
    public ResponseEntity<?> debitBalance(
            @AuthenticationPrincipal User seller,
            @RequestBody Double amount
    ) {

        walletService.debitIfSufficient(seller, amount);

        return ResponseEntity.ok("Montant débité avec succès");
    }


    @GetMapping("/all-deposits")
    public ResponseEntity<Page<Deposit>> getAllDeposits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Deposit> deposits = walletService.getAllDepositsPaginated(page, size);
        return ResponseEntity.ok(deposits);
    }

    // ================= ADMIN : confirmer / rejeter un dépôt =================

    @PatchMapping("/admin/deposits/{id}/approve")
    public ResponseEntity<?> approveDeposit(
            @PathVariable Long id,
            @RequestParam(required = false) Double amount
    ) {
        try {
            Deposit deposit = walletService.approveDeposit(id, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dépôt approuvé, wallet mis à jour et email envoyé au vendeur");
            response.put("deposit", deposit);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PatchMapping("/admin/deposits/{id}/reject")
    public ResponseEntity<?> rejectDeposit(@PathVariable Long id) {
        try {
            Deposit deposit = walletService.rejectDeposit(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Dépôt rejeté");
            response.put("deposit", deposit);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}
