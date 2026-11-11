package app_sourcing.sellerModule.service;

import app_sourcing.sellerModule.dto.SellerDTO;
import app_sourcing.sellerModule.dto.SellerDocumentDTO;
import app_sourcing.sellerModule.dto.SellerWithDocumentsDTO;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.repo.SellerRepository;
import app_sourcing.userModule.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    private final EmailService emailService;
    private final ContractService contractService;

    @Value("${app.uploads.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public SellerDTO createSeller(SellerDTO sellerDTO, MultipartFile idCardRecto, MultipartFile idCardVerso) throws IOException {

        if (userRepository.existsByEmail(sellerDTO.getEmailInput())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (sellerRepository.existsByPhone(sellerDTO.getPhoneInput())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        if (idCardRecto == null || idCardRecto.isEmpty()) {
            throw new IllegalArgumentException("ID card recto is required");
        }
        if (idCardVerso == null || idCardVerso.isEmpty()) {
            throw new IllegalArgumentException("ID card verso is required");
        }

        Seller seller = sellerDTO.toEntity();

        seller.setPassword(passwordEncoder.encode(sellerDTO.getPasswordInput()));

        seller.setCreatedAt(LocalDateTime.now());
        seller.setUpdatedAt(LocalDateTime.now());

        String rectoPath = fileStorageService.storeFile(idCardRecto, "id-cards/recto");
        String versoPath = fileStorageService.storeFile(idCardVerso, "id-cards/verso");

        seller.setIdCardRectoPath(rectoPath);
        seller.setIdCardVersoPath(versoPath);

        // Générer numéro de contrat unique
        String contractNumber = "X10-" + java.time.Year.now().getValue() + "-" +
                String.format("%05d", sellerRepository.count() + 1);
        seller.setContractNumber(contractNumber);

        Seller savedSeller = sellerRepository.save(seller);

        // Envoyer l'email de confirmation
        try {
            emailService.sendSellerRegistrationEmail(
                    savedSeller.getEmail(),
                    savedSeller.getFirstName() + " " + savedSeller.getLastName()
            );
        } catch (Exception e) {
            // Log l'erreur mais ne pas bloquer la création du compte
            // Vous pouvez utiliser un logger ici
            System.err.println("Failed to send registration email: " + e.getMessage());
        }

        return SellerDTO.fromEntity(savedSeller);
    }

    @Override
    public SellerDTO updateSeller(Long id, SellerDTO sellerDTO, MultipartFile idCardRecto, MultipartFile idCardVerso) throws IOException {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + id));

        if (sellerDTO.getEmailInput() != null &&
                userRepository.existsByEmailAndIdNot(sellerDTO.getEmailInput(), id)) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (sellerDTO.getPhoneInput() != null &&
                sellerRepository.existsByPhoneAndIdNot(sellerDTO.getPhoneInput(), id)) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        sellerDTO.updateEntity(seller);

        if (sellerDTO.getPasswordInput() != null && !sellerDTO.getPasswordInput().isEmpty()) {
            seller.setPassword(passwordEncoder.encode(sellerDTO.getPasswordInput()));
        }

        if (idCardRecto != null && !idCardRecto.isEmpty()) {
            String rectoPath = fileStorageService.storeFile(idCardRecto, "id-cards/recto");
            // Supprimer l'ancien fichier si nécessaire
            if (seller.getIdCardRectoPath() != null) {
                fileStorageService.deleteFile(seller.getIdCardRectoPath());
            }
            seller.setIdCardRectoPath(rectoPath);
        }

        if (idCardVerso != null && !idCardVerso.isEmpty()) {
            String versoPath = fileStorageService.storeFile(idCardVerso, "id-cards/verso");
            if (seller.getIdCardVersoPath() != null) {
                fileStorageService.deleteFile(seller.getIdCardVersoPath());
            }
            seller.setIdCardVersoPath(versoPath);
        }

        seller.setUpdatedAt(LocalDateTime.now());

        Seller updatedSeller = sellerRepository.save(seller);

        return SellerDTO.fromEntity(updatedSeller);
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDTO getSellerById(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + id));
        return SellerDTO.fromEntity(seller);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDTO> getAllSellers() {
        return sellerRepository.findAllByDeletedFalse()
                .stream()
                .map(SellerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Soft delete : le vendeur n'est plus supprimé physiquement,
    // il est marqué comme supprimé et disparaît de la liste principale.
    @Override
    public void deleteSeller(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + id));

        if (seller.isDeleted()) {
            throw new IllegalArgumentException("Seller already deleted with id: " + id);
        }

        seller.setDeleted(true);
        seller.setDeletedAt(LocalDateTime.now());
        sellerRepository.save(seller);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDTO> getAllDeletedSellers() {
        return sellerRepository.findAllByDeletedTrue()
                .stream()
                .map(SellerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SellerDTO restoreSeller(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + id));

        if (!seller.isDeleted()) {
            throw new IllegalArgumentException("Seller is not deleted with id: " + id);
        }

        seller.setDeleted(false);
        seller.setDeletedAt(null);
        Seller restored = sellerRepository.save(seller);
        return SellerDTO.fromEntity(restored);
    }

    // Suppression définitive et irréversible : uniquement possible sur un
    // vendeur déjà soft-deleted (protection contre une suppression accidentelle).
    @Override
    public void permanentlyDeleteSeller(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found with id: " + id));

        if (!seller.isDeleted()) {
            throw new IllegalArgumentException("Seller must be moved to trash before being permanently deleted: " + id);
        }

        if (seller.getIdCardRectoPath() != null) {
            fileStorageService.deleteFile(seller.getIdCardRectoPath());
        }
        if (seller.getIdCardVersoPath() != null) {
            fileStorageService.deleteFile(seller.getIdCardVersoPath());
        }

        sellerRepository.deleteById(id);
    }

    @Override
    public SellerWithDocumentsDTO getSellerWithDocuments(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        return mapToSellerWithDocumentsDTO(seller);
    }

    private SellerWithDocumentsDTO mapToSellerWithDocumentsDTO(Seller seller) {
        SellerWithDocumentsDTO dto = new SellerWithDocumentsDTO();
        dto.setId(seller.getId());
        dto.setFirstName(seller.getFirstName());
        dto.setLastName(seller.getLastName());
        dto.setPhone(seller.getPhone());
        dto.setEmail(seller.getEmail());
        dto.setStoreName(seller.getStoreName());
        dto.setCountry(seller.getCountry());
        dto.setBudget(seller.getBudget());
        dto.setCommerceExperience(seller.getCommerceExperience());
        dto.setAdsExperience(seller.getAdsExperience());
        dto.setStatus(seller.getStatus());
        dto.setCreatedAt(seller.getCreatedAt());
        dto.setUpdatedAt(seller.getUpdatedAt());

        // Ajouter les documents
        List<SellerDocumentDTO> documents = new ArrayList<>();

        // Document CIN Recto
        if (seller.getIdCardRectoPath() != null && !seller.getIdCardRectoPath().isEmpty()) {
            SellerDocumentDTO rectoDoc = new SellerDocumentDTO();
            rectoDoc.setName("Carte d'identité (Recto)");
            rectoDoc.setType("id_card_recto");
            rectoDoc.setUrl(baseUrl + "/uploads/" + seller.getIdCardRectoPath());
            rectoDoc.setUploadedAt(seller.getCreatedAt());
            rectoDoc.setVerified(seller.getStatus().equals("approved"));
            documents.add(rectoDoc);
        }

        // Document CIN Verso
        if (seller.getIdCardVersoPath() != null && !seller.getIdCardVersoPath().isEmpty()) {
            SellerDocumentDTO versoDoc = new SellerDocumentDTO();
            versoDoc.setName("Carte d'identité (Verso)");
            versoDoc.setType("id_card_verso");
            versoDoc.setUrl(baseUrl + "/uploads/" + seller.getIdCardVersoPath());
            versoDoc.setUploadedAt(seller.getCreatedAt());
            versoDoc.setVerified(seller.getStatus().equals("approved"));
            documents.add(versoDoc);
        }

        // Ici vous pouvez ajouter d'autres documents si nécessaire
        // Par exemple: licence commerciale, certificat fiscal, etc.

        dto.setDocuments(documents);
        return dto;
    }

    @Override
    public SellerWithDocumentsDTO updateSellerStatus(Long id, String status, String reason) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        String oldStatus = seller.getStatus();
        seller.setStatus(status);

        Seller updatedSeller = sellerRepository.save(seller);

        // Envoyer un email + PDF contrat si le statut passe à "approved"
        if ("approved".equals(status) && !"approved".equals(oldStatus)) {
            try {
                String sellerName = seller.getFirstName() + " " + seller.getLastName();
                byte[] contractPdf = null;

                // Générer le PDF signé si le seller a une signature
                if (seller.getSignatureBase64() != null && !seller.getSignatureBase64().isBlank()) {
                    contractPdf = contractService.generateSignedContract(
                            sellerName,
                            seller.getEmail(),
                            seller.getContractNumber() != null ? seller.getContractNumber() : "N/A",
                            seller.getSignedAt() != null ? seller.getSignedAt() : java.time.LocalDateTime.now(),
                            seller.getSignatureBase64()
                    );
                }

                emailService.sendSellerApprovalEmailWithContract(
                        seller.getEmail(), sellerName, contractPdf, seller.getContractNumber());
                System.out.println("Email d'approbation + contrat PDF envoyé à : " + seller.getEmail());
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi de l'email d'approbation : " + e.getMessage());
                e.printStackTrace();
            }
        }

        return mapToSellerWithDocumentsDTO(updatedSeller);
    }

    // ================= SELF-SERVICE ACCOUNT (le vendeur gère son propre compte) =================

    @Override
    public void updateOwnProfile(Seller seller, app_sourcing.sellerModule.dto.UpdateSellerProfileDto dto) {
        seller.setFirstName(dto.getFirstName());
        seller.setLastName(dto.getLastName());
        seller.setPhone(dto.getPhone());
        if (dto.getCountry() != null && !dto.getCountry().isBlank()) {
            seller.setCountry(dto.getCountry());
        }
        if (dto.getStoreName() != null && !dto.getStoreName().isBlank()) {
            seller.setStoreName(dto.getStoreName());
        }
        sellerRepository.save(seller);
    }

    @Override
    public void changePassword(Seller seller, app_sourcing.sellerModule.dto.ChangePasswordDto dto) {
        if (!passwordEncoder.matches(dto.getCurrentPassword(), seller.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }
        seller.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sellerRepository.save(seller);
    }

    @Override
    public String updateProfilePhoto(Seller seller, MultipartFile photo) {
        try {
            // Supprimer l'ancienne photo si elle existe
            if (seller.getProfilePhotoPath() != null) {
                fileStorageService.deleteFile(seller.getProfilePhotoPath());
            }
            String path = fileStorageService.storeFile(photo, "profile-photos");
            seller.setProfilePhotoPath(path);
            sellerRepository.save(seller);
            return path;
        } catch (IOException e) {
            throw new RuntimeException("Échec de l'upload de la photo de profil", e);
        }
    }
}