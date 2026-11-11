package app_sourcing.sourcingModule.controller;

import app_sourcing.productModule.dto.CreateProductDto;
import app_sourcing.productModule.dto.ProductDTO;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.mappers.ProductMapper;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.shareClasses.IdGenerator;
import app_sourcing.sourcingModule.dtos.CreateSourcingRequest;
import app_sourcing.sourcingModule.dtos.SetPriceRequest;
import app_sourcing.sourcingModule.dtos.SourcingResponseDTO;
import app_sourcing.sourcingModule.dtos.SourcingStatsDto;
import app_sourcing.sourcingModule.entitie.SourcingRequest;
import app_sourcing.sourcingModule.mappers.SourcingRequestMapper;
import app_sourcing.sourcingModule.service.SourcingRequestBussnesRulesService;
import app_sourcing.sourcingModule.service.SourcingRequestGetDataService;
import app_sourcing.userModule.entitie.CustomUserDetails;
import app_sourcing.userModule.entitie.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/seller/sourcing")
@RequiredArgsConstructor
public class SourcingRequestController {

    private final SourcingRequestBussnesRulesService sourcingService;
    private final SourcingRequestGetDataService sourcingRequestGetDataService;

    //Seller
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public SourcingResponseDTO create(
            @ModelAttribute CreateSourcingRequest dto,
            @AuthenticationPrincipal User user
    ) {
        SourcingRequest created = sourcingService.createRequest((Seller) user, dto);
        return SourcingRequestMapper.toResponseDTO(created);
    }

    @PostMapping("/{requestId}/pay")
    public void pay( @AuthenticationPrincipal User seller,@PathVariable String requestId) {
        sourcingService.payRequest(seller,requestId);
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable String id) {
        sourcingService.approveRequest(id);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable String id) {
        sourcingService.cancel(id);
    }

    //Admin
    @PatchMapping("/{id}/price")
//    @PreAuthorize("hasRole('ADMIN')")
    public SourcingResponseDTO setPrice(
            @PathVariable String id,
            @RequestBody SetPriceRequest priceRequest
    ) {
        SourcingRequest updated = sourcingService.setTotalPrice(id, priceRequest);
        return SourcingRequestMapper.toResponseDTO(updated);
    }

    @PostMapping("/createProduct")
//    @PreAuthorize("hasRole('ADMIN')")
    public ProductDTO createProduct(
            @RequestBody CreateProductDto productDTO
    ) {
        return ProductMapper.toDTO(sourcingService.createProduct(productDTO));
    }
    @GetMapping("/mesSourcing")
    public Page<SourcingResponseDTO> myRequests(
            @AuthenticationPrincipal User seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        //User seller=userDetails.getUser();
        // System.out.println(seller);
        //  List<SourcingRequest> requests=sourcingRequestGetDataService.getSellerRequests(seller.getId());
        //   return requests.stream().map(SourcingRequestMapper::toResponseDTO).toList();
        Pageable pageable = PageRequest.of(page, size);
        Page<SourcingRequest> requestsPage = sourcingRequestGetDataService.getSellerRequests(seller.getId(), pageable);
        return requestsPage.map(SourcingRequestMapper::toResponseDTO);
    }


    @GetMapping("/allSourcing")
// @PreAuthorize("hasRole('ADMIN')")
    public Page<SourcingResponseDTO> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return sourcingRequestGetDataService.getAllRequests(pageable);
    }

    @GetMapping("/stats")
//    @PreAuthorize("hasRole('ADMIN')")
    public SourcingStatsDto stats() {
        return sourcingRequestGetDataService.getStats();
    }

    @GetMapping("/{requestID}")
    public SourcingResponseDTO getByID(@PathVariable String requestID){
        return SourcingRequestMapper.toResponseDTO(sourcingRequestGetDataService.getRequestById(requestID));
    }

}