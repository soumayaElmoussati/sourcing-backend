package app_sourcing.walletModule.service;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.service.EmailService;
import app_sourcing.userModule.entitie.User;

import app_sourcing.walletModule.dtos.DepositRequest;
import app_sourcing.walletModule.entitie.Deposit;
import app_sourcing.walletModule.entitie.SellerWallet;
import app_sourcing.walletModule.entitie.TransactionType;

import app_sourcing.walletModule.repo.DepositRepository;
import app_sourcing.walletModule.repo.SellerWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final SellerWalletRepository walletRepository;
    private final DepositRepository depositRepository;
    private final WalletTransactionService walletTransactionService;
    private final EmailService emailService;

    private final String UPLOAD_DIR = "uploads/paiement-justification/";

    public SellerWallet getOrCreateWallet(User seller) {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("L'utilisateur doit être un vendeur");
        }

        return walletRepository.findBySellerId(seller.getId())
                .orElseGet(() -> {
                    SellerWallet wallet = new SellerWallet();
                    wallet.setSeller((Seller) seller);
                    return walletRepository.save(wallet);
                });
    }

    public SellerWallet getSellerWallet(User seller) {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("L'utilisateur doit être un vendeur");
        }

        return walletRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new IllegalStateException("Aucun wallet trouvé pour ce vendeur"));
    }

    public boolean hasWallet(User seller) {
        if (!(seller instanceof Seller)) {
            return false;
        }
        return walletRepository.existsBySellerId(seller.getId());
    }

    public Deposit createDeposit(User seller, DepositRequest request) throws IOException {
        // Vérifier que c'est un seller
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("L'utilisateur doit être un vendeur");
        }

        // Récupérer ou créer le wallet
        SellerWallet wallet = getOrCreateWallet(seller);

        // Créer le répertoire s'il n'existe pas
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Sauvegarder le document de preuve
        MultipartFile proofFile = request.getProofDocument();
        String originalFileName = proofFile.getOriginalFilename();

        // Gérer l'extension du fichier
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Copier le fichier
        Files.copy(proofFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Créer le dépôt
        Deposit deposit = new Deposit();
        deposit.setWallet(wallet);
        deposit.setAmount(request.getAmount());
        deposit.setPaymentMethodType(request.getPaymentMethodType());
        deposit.setProofDocumentPath(filePath.toString());
        deposit.setCreatedAt(LocalDateTime.now());
        deposit.setUpdatedAt(LocalDateTime.now());


        return depositRepository.save(deposit);
    }


    public List<Deposit> getSellerDeposits(User seller) {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("L'utilisateur doit être un vendeur");
        }

        return depositRepository.findByWalletSellerIdOrderByCreatedAtDesc(seller.getId());
    }

    public void debitIfSufficient(User seller, Double amount) {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("L'utilisateur doit être un vendeur");
        }

        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Montant invalide");
        }

        SellerWallet wallet = getSellerWallet(seller);

        if (wallet.getBalance() < amount) {
            throw new IllegalStateException("Solde insuffisant");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setUpdatedAt(LocalDateTime.now());

        walletRepository.save(wallet);
    }

    public Deposit approveDeposit(Long depositId, Double amount) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new IllegalStateException("Dépôt non trouvé"));

        if (!"PENDING".equals(deposit.getStatus())) {
            throw new IllegalStateException("Le dépôt n'est pas en attente");
        }

        // Par défaut on crédite le montant déclaré par le vendeur ; l'admin
        // peut le corriger en passant un montant différent (ex: justificatif
        // ne correspond pas exactement).
        double amountToCredit = amount != null ? amount : deposit.getAmount().doubleValue();

        // Mettre à jour le statut
        deposit.setStatus("APPROVED");
        deposit.setUpdatedAt(LocalDateTime.now());

        // Ajouter le montant au solde du wallet + enregistrer la transaction dans le ledger
        SellerWallet wallet = deposit.getWallet();
        walletTransactionService.applyAndRecord(
                wallet,
                TransactionType.BALANCE,
                null,
                BigDecimal.valueOf(amountToCredit),
                "Dépôt approuvé (" + deposit.getPaymentMethodType() + ")",
                null,
                deposit.getId(),
                null
        );

        Deposit saved = depositRepository.save(deposit);

        // Notifier le vendeur par email
        Seller seller = wallet.getSeller();
        emailService.sendDepositApprovedEmail(
                seller.getEmail(),
                seller.getFirstName() + " " + seller.getLastName(),
                amountToCredit,
                wallet.getBalance()
        );

        return saved;
    }

    public Deposit rejectDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new IllegalStateException("Dépôt non trouvé"));

        if (!"PENDING".equals(deposit.getStatus())) {
            throw new IllegalStateException("Le dépôt n'est pas en attente");
        }

        deposit.setStatus("REJECTED");
        deposit.setUpdatedAt(LocalDateTime.now());

        Deposit saved = depositRepository.save(deposit);

        Seller seller = deposit.getWallet().getSeller();
        emailService.sendDepositRejectedEmail(
                seller.getEmail(),
                seller.getFirstName() + " " + seller.getLastName(),
                0
        );

        return saved;
    }

    ////


    public Page<Deposit> getAllDepositsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return depositRepository.findAll(pageable);
    }
}
