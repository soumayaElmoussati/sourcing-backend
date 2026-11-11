package app_sourcing.sourcingModule.service;

import app_sourcing.productModule.dto.CreateProductDto;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.service.ProductService;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.service.FileStorageService;
import app_sourcing.shareClasses.IdGenerator;
import app_sourcing.sellerModule.service.EmailService;
import app_sourcing.sourcingModule.dtos.CreateSourcingRequest;
import app_sourcing.sourcingModule.dtos.SetPriceRequest;
import app_sourcing.sourcingModule.entitie.*;
import app_sourcing.sourcingModule.exception.BusinessSourcingException;
import app_sourcing.sourcingModule.repo.SourcingRequestRepository;
import app_sourcing.userModule.entitie.User;
import app_sourcing.walletModule.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SourcingRequestBussnesRulesService {
    private final SourcingRequestRepository sourcingRequestRepository;
    private final SourcingRequestGetDataService sourcingRequestGetDataService;
    private final ProductService productService;
    private final WalletService walletService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    //Seller crée la demande
    /**
     Regles metiers :
     * - Fulfillment Mode obligatoire
     * - Choisi uniquement pendant la phase Sourcing
     * - Country obligatoire si LOCAL
     * - Central Fulfillment => UAE
     */
    public SourcingRequest createRequest(
            Seller seller,
            CreateSourcingRequest dto
    ) {

        // Fulfillment Mode obligatoire

        if (dto.getFulfillmentMode() == null) {
            throw new BusinessSourcingException("Fulfillment Mode must be selected");
        }

        // Country obligatoire si LOCAL
        if (dto.getFulfillmentMode() == FulfillmentMode.LOCAL_SINGLE_COUNTRY
                && dto.getCountry() == null) {
            throw new BusinessSourcingException("Exactly one country must be selected");
        }

        // RULE 3️: Central Fulfillment => UAE
        if (dto.getFulfillmentMode() == FulfillmentMode.CENTRALIZED) {
            dto.setCountry(Country.UAE);
        }

        SourcingRequest request = new SourcingRequest();

        request.setId(IdGenerator.generateId("SR"));
        request.setSeller(seller);

        request.setNameProduct(dto.getNameProduct());
        request.setDescription(dto.getDescription());
        request.setLinkProduct(dto.getLinkProduct());
        request.setVariants(dto.getVariants());
        request.setEstimatedQuantity(dto.getEstimatedQuantity());

        request.setStatus(RequestStatus.PENDING);

        request.setIsArchive(false);
        request.setFulfillmentMode(dto.getFulfillmentMode());
        request.setCountry(dto.getCountry());

        SourcingRequest savedRequest = sourcingRequestRepository.save(request);

        // Photos du produit (optionnelles)
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<MultipartFile> validImages = dto.getImages().stream()
                    .filter(img -> img != null && !img.isEmpty())
                    .toList();

            if (!validImages.isEmpty()) {
                List<String> imagePaths = fileStorageService.storeFiles(validImages, "sourcing-images");

                List<SourcingRequestImage> images = new ArrayList<>();
                for (String path : imagePaths) {
                    SourcingRequestImage image = new SourcingRequestImage();
                    image.setImagePath(path);
                    image.setSourcingRequest(savedRequest);
                    images.add(image);
                }
                savedRequest.getImages().addAll(images);
            }
        }

        return savedRequest;
    }


    /**
     * - Le prix est fixé par l’Admin
     * - Seulement quand le statut est PENDING
     */
    public SourcingRequest setTotalPrice(String requestId, SetPriceRequest priceRequest) {
        SourcingRequest request = getRequestById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessSourcingException("Total price can be set only when status is PENDING");
        }

        BigDecimal unitPrice = priceRequest.getUnitPrice();
        int quantity = request.getEstimatedQuantity() != null ? request.getEstimatedQuantity() : 1;

        // Calcul du prix total USD = unitaire × quantité
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        request.setUnitPrice(unitPrice);
        request.setTotalPrice(totalPrice);

        // Devise locale optionnelle
        if (priceRequest.getLocalCurrencyPrice() != null) {
            request.setLocalCurrencyPrice(priceRequest.getLocalCurrencyPrice());
        }
        if (priceRequest.getLocalCurrencyCode() != null && !priceRequest.getLocalCurrencyCode().isBlank()) {
            request.setLocalCurrencyCode(priceRequest.getLocalCurrencyCode());
        }

        return request;
    }

    /**
     * - Paiement + validation Admin
     * - Passage à APPROVED
     */
    public void payRequest(User seller, String requestId) {

        SourcingRequest request = getRequestById(requestId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessSourcingException("Request already processed");
        }

        if (request.getTotalPrice() == null) {
            throw new BusinessSourcingException("Total price not defined");
        }

        walletService.debitIfSufficient(seller,request.getTotalPrice().doubleValue());

        request.setStatus(RequestStatus.PAID);

    }

    public void approveRequest(String requestID) {

        SourcingRequest request = getRequestById(requestID);

        if (request.getStatus() != RequestStatus.PAID)
            throw new BusinessSourcingException("Request must be in PAID status before approval");

        request.setStatus(RequestStatus.APPROVED);

        // Envoyer email au vendeur
        try {
            String sellerEmail = request.getSeller().getEmail();
            String sellerName  = request.getSeller().getFirstName() + " " + request.getSeller().getLastName();
            int qty = request.getEstimatedQuantity() != null ? request.getEstimatedQuantity() : 1;
            double unitPrice  = request.getUnitPrice()  != null ? request.getUnitPrice().doubleValue()  : 0;
            double totalPrice = request.getTotalPrice() != null ? request.getTotalPrice().doubleValue() : 0;

            emailService.sendSourcingApprovedEmail(
                    sellerEmail,
                    sellerName,
                    request.getNameProduct(),
                    request.getId(),
                    unitPrice,
                    totalPrice,
                    qty,
                    request.getLocalCurrencyCode()
            );
        } catch (Exception e) {
            System.err.println("Failed to send sourcing approval email: " + e.getMessage());
        }
    }

    /**
     * - Produit créé uniquement après APPROVED
     * - Fulfillment Mode et Country hérités
     * - Fulfillment Mode verrouillé après création
     * - Produit cree par Admin
     */
    public Product createProduct(CreateProductDto productDto) {
        SourcingRequest request = getRequestById(productDto.getRequestId());
        System.out.println(request.getStatus());
        if (request.getStatus() != RequestStatus.APPROVED) {
            throw new BusinessSourcingException("Sourcing must be APPROVED before product creation");
        }

        if (request.getProduct() != null) {
            throw new BusinessSourcingException("Product already created");
        }

        System.out.println(request.getNameProduct());
        Product product=productService.createProductFromSourcing(request,productDto);
        request.setStatus(RequestStatus.COMPLETED);
        return product;
    }


    //  Cancel
    public void cancel(String requestId) {
        SourcingRequest request = getRequestById(requestId);
        if (request.getStatus() == RequestStatus.PAID) {
            throw new BusinessSourcingException("Cannot cancel Paid request");
        }
        if (request.getStatus() == RequestStatus.APPROVED) {
            throw new BusinessSourcingException("Cannot cancel Approved request");
        }
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new BusinessSourcingException("Cannot cancel completed request");
        }

        request.setStatus(RequestStatus.CANCELLED);
    }
    private SourcingRequest getRequestById(String id) {
        return sourcingRequestGetDataService.getRequestById(id);
    }
}