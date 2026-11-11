package app_sourcing.sellerModule.service;


import app_sourcing.sellerModule.dto.SellerWithDocumentsDTO;
import app_sourcing.sellerModule.entitie.Seller;
import org.springframework.web.multipart.MultipartFile;
import app_sourcing.sellerModule.dto.SellerDTO;

import java.io.IOException;
import java.util.List;

public interface SellerService {

    SellerDTO createSeller(SellerDTO sellerDTO, MultipartFile idCardRecto, MultipartFile idCardVerso) throws IOException;
    SellerDTO updateSeller(Long id, SellerDTO sellerDTO, MultipartFile idCardRecto, MultipartFile idCardVerso) throws IOException;
    SellerDTO getSellerById(Long id);
    List<SellerDTO> getAllSellers();

    // Soft delete
    void deleteSeller(Long id);
    List<SellerDTO> getAllDeletedSellers();
    SellerDTO restoreSeller(Long id);
    void permanentlyDeleteSeller(Long id);

    SellerWithDocumentsDTO getSellerWithDocuments(Long id);
    SellerWithDocumentsDTO updateSellerStatus(Long id, String status, String reason);

    // Self-service (le vendeur gère son propre compte)
    void updateOwnProfile(Seller seller, app_sourcing.sellerModule.dto.UpdateSellerProfileDto dto);
    void changePassword(Seller seller, app_sourcing.sellerModule.dto.ChangePasswordDto dto);
    String updateProfilePhoto(Seller seller, org.springframework.web.multipart.MultipartFile photo);
}
