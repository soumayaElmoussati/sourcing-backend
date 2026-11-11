package app_sourcing.walletModule.controller;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.dtos.WalletTransactionDTO;
import app_sourcing.walletModule.service.WalletTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WalletTransactionController {

    private final WalletTransactionService transactionService;

    // Historique des transactions du vendeur connecté
    @GetMapping("/api/wallet/transactions")
    public ResponseEntity<Page<WalletTransactionDTO>> getMyTransactions(
            @AuthenticationPrincipal Seller seller,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(transactionService.getSellerTransactions(seller, pageable));
    }

    // Toutes les transactions, tous vendeurs confondus (admin)
    @GetMapping("/api/admin/wallet-transactions")
    public ResponseEntity<Page<WalletTransactionDTO>> getAllTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }
}
