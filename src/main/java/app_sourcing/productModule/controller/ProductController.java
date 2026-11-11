package app_sourcing.productModule.controller;

import app_sourcing.productModule.dto.ProductDTO;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.entitie.ProductStatus;
import app_sourcing.productModule.mappers.ProductMapper;
import app_sourcing.productModule.service.ProductService;
import app_sourcing.userModule.entitie.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/mesProducts")
    public ResponseEntity<Page<ProductDTO>> mesProducts(
            @AuthenticationPrincipal User seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDTO> products = productService.getSellerProducts(seller.getId(), pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String id) {
        Product product = productService.getById(id);
        return ResponseEntity.ok(ProductMapper.toDTO(product));
    }

    @PatchMapping("/changeStatus/{id}")
    public void changeStatus(@PathVariable String id,@RequestBody ProductStatus status){
        productService.changeStatus(id,status);
    }

    @PostMapping("/rejectProducts/{id}")
    public void rejectProduct(@AuthenticationPrincipal User user,@RequestBody String reason,@PathVariable String id){
        productService.rejectProduct(id,reason,user.getEmail());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.softDelete(id); // Soft delete
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public Map<String, Long> getProductStats() {
        return productService.getProductStats();
    }

    @GetMapping("/stats/me")
    public Map<String, Long> getMyProductStats(@AuthenticationPrincipal User seller) {
        return productService.getProductStats(seller.getId());
    }

    @GetMapping("/mesProducts/all")
    public ResponseEntity<List<ProductDTO>> mesProductsAll(
            @AuthenticationPrincipal User seller
    ) {
        List<ProductDTO> products = productService.getSellerProductsAll(seller.getId());
        return ResponseEntity.ok(products);
    }

}
