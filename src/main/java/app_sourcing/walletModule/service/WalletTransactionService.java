package app_sourcing.walletModule.service;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.walletModule.dtos.WalletTransactionDTO;
import app_sourcing.walletModule.entitie.*;
import app_sourcing.walletModule.repo.SellerWalletRepository;
import app_sourcing.walletModule.repo.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletTransactionService {

    private final WalletTransactionRepository transactionRepository;
    private final SellerWalletRepository walletRepository;

    /**
     * Applique un mouvement de solde signé (amount > 0 = crédit, amount < 0 = débit)
     * sur le wallet, et enregistre la transaction correspondante dans le ledger.
     * Ne vérifie PAS la suffisance du solde : c'est à l'appelant de le faire
     * en amont si nécessaire (ex: création de commande avec lead fee).
     */
    public WalletTransaction applyAndRecord(
            SellerWallet wallet,
            TransactionType type,
            FeeType feeType,
            BigDecimal signedAmount,
            String description,
            String relatedOrderId,
            Long relatedDepositId,
            Long relatedWithdrawalId
    ) {
        double newBalance = wallet.getBalance() + signedAmount.doubleValue();
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setFeeType(feeType);
        transaction.setAmount(signedAmount);
        transaction.setBalanceAfter(BigDecimal.valueOf(newBalance));
        transaction.setDescription(description);
        transaction.setRelatedOrderId(relatedOrderId);
        transaction.setRelatedDepositId(relatedDepositId);
        transaction.setRelatedWithdrawalId(relatedWithdrawalId);

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionDTO> getSellerTransactions(Seller seller, Pageable pageable) {
        SellerWallet wallet = walletRepository.findBySeller(seller)
                .orElseThrow(() -> new IllegalArgumentException("Wallet non trouvé pour ce vendeur"));

        return transactionRepository.findByWalletOrderByCreatedAtDesc(wallet, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionDTO> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDto);
    }

    private WalletTransactionDTO toDto(WalletTransaction t) {
        WalletTransactionDTO dto = new WalletTransactionDTO();
        dto.setId(t.getId());

        Seller seller = t.getWallet().getSeller();
        dto.setSellerId(seller.getId());
        dto.setSellerName(seller.getFirstName() + " " + seller.getLastName());
        dto.setSellerEmail(seller.getEmail());

        dto.setType(t.getType());
        dto.setFeeType(t.getFeeType());
        dto.setAmount(t.getAmount());
        dto.setBalanceAfter(t.getBalanceAfter());
        dto.setDescription(t.getDescription());
        dto.setRelatedOrderId(t.getRelatedOrderId());
        dto.setRelatedDepositId(t.getRelatedDepositId());
        dto.setRelatedWithdrawalId(t.getRelatedWithdrawalId());
        dto.setCreatedAt(t.getCreatedAt());

        return dto;
    }
}
