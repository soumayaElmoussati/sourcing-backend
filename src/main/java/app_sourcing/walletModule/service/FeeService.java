package app_sourcing.walletModule.service;

import app_sourcing.walletModule.dtos.FeeDTO;
import app_sourcing.walletModule.dtos.FeeRequestDTO;
import app_sourcing.walletModule.entitie.Fee;
import app_sourcing.walletModule.entitie.FeeType;
import app_sourcing.walletModule.repo.FeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FeeService {

    private final FeeRepository feeRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public List<FeeDTO> getAllFees() {
        return feeRepository.findAll().stream().map(this::toDto).toList();
    }

    // Un seul Fee actif par type : on refuse la création si un fee de ce type existe déjà.
    public FeeDTO createFee(FeeRequestDTO dto) {
        if (feeRepository.existsByType(dto.getType())) {
            throw new IllegalStateException("Un frais de type " + dto.getType() + " existe déjà. Modifiez-le au lieu d'en créer un nouveau.");
        }

        Fee fee = new Fee();
        fee.setType(dto.getType());
        fee.setAmount(dto.getAmount());

        return toDto(feeRepository.save(fee));
    }

    public FeeDTO updateFee(Long id, BigDecimal amount) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Frais introuvable"));

        fee.setAmount(amount);
        return toDto(feeRepository.save(fee));
    }

    public void deleteFee(Long id) {
        if (!feeRepository.existsById(id)) {
            throw new IllegalArgumentException("Frais introuvable");
        }
        feeRepository.deleteById(id);
    }

    // Utilisé par la logique métier des commandes. Retourne 0 si le frais
    // n'a pas encore été configuré par l'admin (aucun prélèvement dans ce cas).
    public BigDecimal getFeeAmount(FeeType type) {
        return feeRepository.findByType(type)
                .map(Fee::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    private FeeDTO toDto(Fee fee) {
        FeeDTO dto = new FeeDTO();
        dto.setId(fee.getId());
        dto.setType(fee.getType());
        dto.setAmount(fee.getAmount());
        dto.setUpdatedAt(fee.getUpdatedAt() != null ? fee.getUpdatedAt().format(FORMATTER) : null);
        return dto;
    }
}
