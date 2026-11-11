package app_sourcing.walletModule.service;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.dtos.SellerPaymentMethodDTO;
import app_sourcing.walletModule.dtos.SellerPaymentMethodRequestDTO;
import app_sourcing.walletModule.entitie.SellerPaymentMethod;
import app_sourcing.walletModule.repo.SellerPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerPaymentMethodService {

    private final SellerPaymentMethodRepository repository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<SellerPaymentMethodDTO> getAll(Seller seller) {
        return repository.findBySellerOrderByCreatedAtDesc(seller).stream().map(this::toDto).toList();
    }

    public SellerPaymentMethodDTO create(Seller seller, SellerPaymentMethodRequestDTO dto) {
        SellerPaymentMethod method = new SellerPaymentMethod();
        method.setSeller(seller);
        method.setMethodType(dto.getMethodType());
        method.setLabel(dto.getLabel());
        method.setAccountDetails(dto.getAccountDetails());
        return toDto(repository.save(method));
    }

    public SellerPaymentMethodDTO update(Seller seller, Long id, SellerPaymentMethodRequestDTO dto) {
        SellerPaymentMethod method = repository.findByIdAndSeller(id, seller)
                .orElseThrow(() -> new IllegalArgumentException("Compte de paiement introuvable"));

        method.setMethodType(dto.getMethodType());
        method.setLabel(dto.getLabel());
        method.setAccountDetails(dto.getAccountDetails());
        return toDto(repository.save(method));
    }

    public void delete(Seller seller, Long id) {
        SellerPaymentMethod method = repository.findByIdAndSeller(id, seller)
                .orElseThrow(() -> new IllegalArgumentException("Compte de paiement introuvable"));
        repository.delete(method);
    }

    private SellerPaymentMethodDTO toDto(SellerPaymentMethod method) {
        SellerPaymentMethodDTO dto = new SellerPaymentMethodDTO();
        dto.setId(method.getId());
        dto.setMethodType(method.getMethodType());
        dto.setLabel(method.getLabel());
        dto.setAccountDetails(method.getAccountDetails());
        dto.setCreatedAt(method.getCreatedAt() != null ? method.getCreatedAt().format(FORMATTER) : null);
        return dto;
    }
}
