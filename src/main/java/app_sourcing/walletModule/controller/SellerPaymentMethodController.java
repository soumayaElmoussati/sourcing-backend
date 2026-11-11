package app_sourcing.walletModule.controller;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.dtos.SellerPaymentMethodDTO;
import app_sourcing.walletModule.dtos.SellerPaymentMethodRequestDTO;
import app_sourcing.walletModule.service.SellerPaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/payment-methods")
@RequiredArgsConstructor
public class SellerPaymentMethodController {

    private final SellerPaymentMethodService service;

    @GetMapping
    public ResponseEntity<List<SellerPaymentMethodDTO>> getAll(@AuthenticationPrincipal Seller seller) {
        return ResponseEntity.ok(service.getAll(seller));
    }

    @PostMapping
    public ResponseEntity<SellerPaymentMethodDTO> create(
            @AuthenticationPrincipal Seller seller,
            @Valid @RequestBody SellerPaymentMethodRequestDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(seller, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal Seller seller,
            @PathVariable Long id,
            @Valid @RequestBody SellerPaymentMethodRequestDTO dto
    ) {
        try {
            return ResponseEntity.ok(service.update(seller, id, dto));
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal Seller seller, @PathVariable Long id) {
        try {
            service.delete(seller, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
