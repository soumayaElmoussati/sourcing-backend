package app_sourcing.walletModule.service;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.service.EmailService;
import app_sourcing.walletModule.dtos.WithdrawalRequest;
import app_sourcing.walletModule.dtos.WithdrawalResponse;
import app_sourcing.walletModule.entitie.SellerWallet;
import app_sourcing.walletModule.entitie.TransactionType;
import app_sourcing.walletModule.entitie.Withdrawal;
import app_sourcing.walletModule.repo.SellerWalletRepository;
import app_sourcing.walletModule.repo.WithdrawalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final SellerWalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;
    private final EmailService emailService;

    private final String UPLOAD_DIR = "uploads/withdrawal-proofs/";

    @Transactional
    public WithdrawalResponse requestWithdrawal(Seller seller, WithdrawalRequest request) {
        // Récupérer le wallet du seller
        SellerWallet wallet = walletRepository.findBySeller(seller)
                .orElseThrow(() -> new IllegalArgumentException("Wallet non trouvé pour ce vendeur"));

        // Vérifier le solde
        if (wallet.getBalance() < request.getAmount()) {
            throw new IllegalStateException("Solde insuffisant. Solde disponible: " + wallet.getBalance());
        }

        // Vérifier le montant minimum
        if (request.getAmount() < 10.0) {
            throw new IllegalArgumentException("Le montant minimum de retrait est de 10");
        }

        // Calculer les frais de 2%
        final double WITHDRAWAL_FEE_PERCENT = 0.02;
        double fee = Math.round(request.getAmount() * WITHDRAWAL_FEE_PERCENT * 100.0) / 100.0;
        double netAmount = Math.round((request.getAmount() - fee) * 100.0) / 100.0;

        // Créer la demande de retrait
        Withdrawal withdrawal = new Withdrawal();
        withdrawal.setWallet(wallet);
        withdrawal.setAmount(request.getAmount());        // montant brut demandé
        withdrawal.setFeeAmount(fee);                     // frais 2%
        withdrawal.setNetAmount(netAmount);               // montant net reçu
        withdrawal.setPaymentMethod(request.getPaymentMethod());
        withdrawal.setAccountDetails(request.getAccountDetails());
        withdrawal.setStatus("PENDING");

        Withdrawal savedWithdrawal = withdrawalRepository.save(withdrawal);

        return mapToResponse(savedWithdrawal);
    }

    @Transactional
    public WithdrawalResponse processWithdrawal(Long withdrawalId, String action, String reason, MultipartFile proofDocument) throws IOException {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de retrait non trouvée"));

        if (!"PENDING".equals(withdrawal.getStatus())) {
            throw new IllegalStateException("Cette demande a déjà été traitée");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            // Approuver le retrait
            SellerWallet wallet = withdrawal.getWallet();

            // Vérifier à nouveau le solde
            if (wallet.getBalance() < withdrawal.getAmount()) {
                withdrawal.setStatus("REJECTED");
                withdrawal.setRejectionReason("Solde insuffisant au moment du traitement");
                withdrawalRepository.save(withdrawal);
                throw new IllegalStateException("Solde insuffisant pour traiter ce retrait");
            }

            // Justificatif de paiement envoyé par l'admin (optionnel mais recommandé)
            if (proofDocument != null && !proofDocument.isEmpty()) {
                withdrawal.setProofDocumentPath(storeProof(proofDocument));
            }

            // Débiter le wallet + enregistrer la transaction dans le ledger
            walletTransactionService.applyAndRecord(
                    wallet,
                    TransactionType.WITHDRAW,
                    null,
                    BigDecimal.valueOf(-withdrawal.getAmount()),
                    "Retrait via " + withdrawal.getPaymentMethod(),
                    null,
                    null,
                    withdrawal.getId()
            );

            withdrawal.setStatus("COMPLETED");
            withdrawal.setProcessedAt(LocalDateTime.now());

        } else if ("REJECT".equalsIgnoreCase(action)) {
            // Rejeter le retrait
            withdrawal.setStatus("REJECTED");
            withdrawal.setRejectionReason(reason);
            withdrawal.setProcessedAt(LocalDateTime.now());
        } else {
            throw new IllegalArgumentException("Action non reconnue");
        }

        Withdrawal updatedWithdrawal = withdrawalRepository.save(withdrawal);

        // Notifier le vendeur par email
        Seller seller = updatedWithdrawal.getWallet().getSeller();
        emailService.sendWithdrawalProcessedEmail(
                seller.getEmail(),
                seller.getFirstName() + " " + seller.getLastName(),
                updatedWithdrawal.getAmount(),
                updatedWithdrawal.getStatus(),
                updatedWithdrawal.getRejectionReason()
        );

        return mapToResponse(updatedWithdrawal);
    }

    private String storeProof(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID() + extension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> getSellerWithdrawals(Seller seller, Pageable pageable) {
        SellerWallet wallet = walletRepository.findBySeller(seller)
                .orElseThrow(() -> new IllegalArgumentException("Wallet non trouvé"));

        return withdrawalRepository.findByWallet(wallet, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> getAllWithdrawals(Pageable pageable) {
        return withdrawalRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> getWithdrawalsByStatus(String status, Pageable pageable) {
        return withdrawalRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public WithdrawalResponse getWithdrawalDetails(Long withdrawalId, Seller seller) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de retrait non trouvée"));

        // Vérifier que le seller est bien le propriétaire
        if (!withdrawal.getWallet().getSeller().getId().equals(seller.getId())) {
            throw new SecurityException("Accès non autorisé à cette demande de retrait");
        }

        return mapToResponse(withdrawal);
    }

    private WithdrawalResponse mapToResponse(Withdrawal withdrawal) {
        WithdrawalResponse response = new WithdrawalResponse();
        response.setId(withdrawal.getId());
        response.setAmount(withdrawal.getAmount());
        response.setFeeAmount(withdrawal.getFeeAmount());
        response.setNetAmount(withdrawal.getNetAmount());
        response.setPaymentMethod(withdrawal.getPaymentMethod());
        response.setAccountDetails(withdrawal.getAccountDetails());
        response.setStatus(withdrawal.getStatus());
        response.setRejectionReason(withdrawal.getRejectionReason());
        response.setProofDocumentPath(withdrawal.getProofDocumentPath());
        response.setProcessedAt(withdrawal.getProcessedAt());
        response.setCreatedAt(withdrawal.getCreatedAt());

        // Ajouter les infos du wallet
        WithdrawalResponse.WalletInfo walletInfo = new WithdrawalResponse.WalletInfo();
        walletInfo.setId(withdrawal.getWallet().getId());
        walletInfo.setBalance(withdrawal.getWallet().getBalance());
        walletInfo.setSellerId(withdrawal.getWallet().getSeller().getId());
        walletInfo.setSellerName(withdrawal.getWallet().getSeller().getStoreName());
        walletInfo.setSellerEmail(withdrawal.getWallet().getSeller().getEmail());
        response.setWalletInfo(walletInfo);

        return response;
    }
}