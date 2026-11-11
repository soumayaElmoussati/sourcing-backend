package app_sourcing.sellerModule.controller;

import app_sourcing.sellerModule.dto.SellerDTO;
import app_sourcing.sellerModule.dto.SellerWithDocumentsDTO;
import app_sourcing.sellerModule.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SellerDTO> createSeller(
            @Validated(SellerDTO.CreateValidation.class)
            @ModelAttribute SellerDTO sellerDTO) throws IOException {

        System.out.println(sellerDTO.getEmail());
        SellerDTO createdSeller = sellerService.createSeller(
                sellerDTO,
                sellerDTO.getIdCardRecto(),
                sellerDTO.getIdCardVerso()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSeller);
    }

    @GetMapping
    public ResponseEntity<List<SellerDTO>> getAllSellers() {
        List<SellerDTO> sellers = sellerService.getAllSellers();
        return ResponseEntity.ok(sellers);
    }

    // @GetMapping("/{id}")
    // public ResponseEntity<SellerDTO> getSellerById(@PathVariable Long id) {
    //     SellerDTO seller = sellerService.getSellerById(id);
    //     return ResponseEntity.ok(seller);
    // }

    @GetMapping("/{id}")
    public ResponseEntity<SellerWithDocumentsDTO> getSellerById(@PathVariable Long id) {
        SellerWithDocumentsDTO seller = sellerService.getSellerWithDocuments(id);
        return ResponseEntity.ok(seller);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SellerDTO> updateSeller(
            @PathVariable Long id,
            @Validated(SellerDTO.UpdateValidation.class)
            @ModelAttribute SellerDTO sellerDTO) throws IOException {

        SellerDTO updatedSeller = sellerService.updateSeller(
                id,
                sellerDTO,
                sellerDTO.getIdCardRecto(),
                sellerDTO.getIdCardVerso()
        );
        return ResponseEntity.ok(updatedSeller);
    }

    // Soft delete : le vendeur est marqué comme supprimé (pas de suppression physique)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeller(@PathVariable Long id) {
        sellerService.deleteSeller(id);
        return ResponseEntity.noContent().build();
    }

    // Liste des vendeurs supprimés (corbeille)
    @GetMapping("/deleted")
    public ResponseEntity<List<SellerDTO>> getAllDeletedSellers() {
        List<SellerDTO> sellers = sellerService.getAllDeletedSellers();
        return ResponseEntity.ok(sellers);
    }

    // Restaurer un vendeur précédemment supprimé
    @PatchMapping("/{id}/restore")
    public ResponseEntity<SellerDTO> restoreSeller(@PathVariable Long id) {
        SellerDTO restored = sellerService.restoreSeller(id);
        return ResponseEntity.ok(restored);
    }

    // Suppression définitive (irréversible), uniquement depuis la corbeille
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteSeller(@PathVariable Long id) {
        sellerService.permanentlyDeleteSeller(id);
        return ResponseEntity.noContent().build();
    }



    @GetMapping("/email/{email}")
    public ResponseEntity<Boolean> checkEmailExists(@PathVariable String email) {
        // Cette méthode pourrait être dans un service séparé
        // Pour l'instant, retournons une réponse simple
        return ResponseEntity.ok(false); // À implémenter selon vos besoins
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<Boolean> checkPhoneExists(@PathVariable String phone) {
        // Cette méthode pourrait être dans un service séparé
        return ResponseEntity.ok(false); // À implémenter selon vos besoins
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing file: " + ex.getMessage());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SellerWithDocumentsDTO> updateSellerStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        String reason = request.get("reason");

        if (!"approved".equals(status) && !"rejected".equals(status)) {
            return ResponseEntity.badRequest().build();
        }

        SellerWithDocumentsDTO updatedSeller = sellerService.updateSellerStatus(id, status, reason);
        return ResponseEntity.ok(updatedSeller);
    }

}
