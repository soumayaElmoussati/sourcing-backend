package app_sourcing.productModule.service;

import app_sourcing.inventory.service.InventoryServiceImpl;
import app_sourcing.productModule.dto.CreateProductDto;
import app_sourcing.productModule.dto.ProductDTO;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.entitie.ProductRejection;
import app_sourcing.productModule.entitie.ProductStatus;
import app_sourcing.productModule.exeption.BusinessProductException;
import app_sourcing.productModule.exeption.ResourceNotFoundException;
import app_sourcing.productModule.mappers.ProductMapper;
import app_sourcing.productModule.repo.ProductRejectionRepository;
import app_sourcing.productModule.repo.ProductRepository;
import app_sourcing.shareClasses.IdGenerator;
import app_sourcing.sourcingModule.entitie.RequestStatus;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductRejectionRepository rejectionRepository;
    private final InventoryServiceImpl inventoryService;

    /**
     * Création du produit après Sourcing APPROVED
     * - Fulfillment & Country hérités
     * - Stock = géré par Inventory
     * - Statut initial = APPROVED
     */
    public Product createProductFromSourcing(
            SourcingRequest request,
            CreateProductDto productDto
    ) {

        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessProductException("Sourcing must be APPROVED");
        }

        Product product = new Product();
        product.setId(IdGenerator.generateId("PR"));
        product.setSeller(request.getSeller());

        product.setNameProduct(request.getNameProduct());
        product.setDescriptionProduct(request.getDescription());
        product.setLink(request.getLinkProduct());
        product.setPrice(productDto.getPrice());
        product.setQteStock(productDto.getStockQuantity());
        product.setPoidsKg(productDto.getWeightKg());

        // Héritage verrouillé
        product.setFulfillmentMode(request.getFulfillmentMode());
        product.setCountry(request.getCountry());

        product.setSourcingRequest(request);

        //  Statut conforme au PDF
        product.setStatus(ProductStatus.DRAFT);
        product.setIsArchive(false);
        productRepository.save(product);
        return product;
    }

    /**
     * Le vendeur peut modifier uniquement le prix (si autorisé)
     */
    public Product updatePrice(String productId, BigDecimal price) {
        Product product = getById(productId);

        if (product.getStatus() == ProductStatus.REJECTED) {
            throw new BusinessProductException("Rejected product cannot be updated");
        }

        product.setPrice(price);
        return product;
    }

    /**
     * Activation / Pause
     */
    public void changeStatus(String productId, ProductStatus newStatus) {

        if (newStatus == ProductStatus.OUT_OF_STOCK) {
            throw new BusinessProductException("OUT_OF_STOCK is managed by Inventory");
        }

        Product product = getById(productId);

        if (product.getStatus() == ProductStatus.REJECTED) {
            throw new BusinessProductException("Rejected product cannot change status");
        }

        if (newStatus==ProductStatus.ACTIVE && inventoryService.getInventory(product)==null){
            inventoryService.createInventory(product,product.getQteStock());
        }

        product.setStatus(newStatus);
    }

    /**
     * Rejet Admin avec motif
     */
    public void rejectProduct(String productId, String reason, String adminId) {

        if (reason == null || reason.isBlank()) {
            throw new BusinessProductException("Rejection reason is mandatory");
        }

        Product product = getById(productId);

        product.setStatus(ProductStatus.REJECTED);

        ProductRejection rejection = new ProductRejection();
        rejection.setId(IdGenerator.generateId("RP"));
        rejection.setProduct(product);
        rejection.setReason(reason);
        rejection.setRejectedBy(adminId);

        rejectionRepository.save(rejection);
    }

    public Product getById(String id) {
        return productRepository.findByIdAndIsArchiveFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public Page<ProductDTO> getSellerProducts(Long sellerId, Pageable pageable) {

        pageable= PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Product> productsPage = productRepository
                .findBySellerIdAndIsArchiveFalse(sellerId, pageable);

        return productsPage.map(ProductMapper::toDTO);
    }

    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        pageable= PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Product> productsPage = productRepository.findByIsArchiveFalse(pageable);

        return productsPage.map(ProductMapper::toDTO);
    }

    public void softDelete(String id) {
        Product product = getById(id);
        product.setIsArchive(true);
    }

    public Map<String, Long> getProductStats(Long id) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalProducts", productRepository.countBySellerIdAndIsArchiveFalse(id));
        stats.put("activeProducts", productRepository.countByStatusAndSellerIdAndIsArchiveFalse(ProductStatus.ACTIVE,id));
        stats.put("pausedProducts", productRepository.countByStatusAndSellerIdAndIsArchiveFalse(ProductStatus.PAUSED,id));
        stats.put("rejectedProducts", productRepository.countByStatusAndSellerIdAndIsArchiveFalse(ProductStatus.REJECTED,id));
        return stats;
    }

    public Map<String, Long> getProductStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalProducts", productRepository.countByIsArchiveFalse());
        stats.put("activeProducts", productRepository.countByStatusAndIsArchiveFalse(ProductStatus.ACTIVE));
        stats.put("pausedProducts", productRepository.countByStatusAndIsArchiveFalse(ProductStatus.PAUSED));
        stats.put("rejectedProducts", productRepository.countByStatusAndIsArchiveFalse(ProductStatus.REJECTED));
        return stats;
    }

    public List<ProductDTO> getSellerProductsAll(Long sellerId) {
        return productRepository
                .findBySellerIdAndIsArchiveFalse(sellerId)
                .stream()
                .map(ProductMapper::toDTO)
                .toList();
    }
}
