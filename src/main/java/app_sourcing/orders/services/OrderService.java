package app_sourcing.orders.services;

import app_sourcing.inventory.service.InventoryServiceImpl;
import app_sourcing.orders.dtos.CreateOrderDto;
import app_sourcing.orders.dtos.OrderStatusStatsDTO;
import app_sourcing.orders.dtos.UpdateOrderDto;
import app_sourcing.orders.entities.*;
import app_sourcing.orders.exception.BusinessOrderException;
import app_sourcing.orders.repo.OrderRepository;
import app_sourcing.productModule.entitie.Product;
import app_sourcing.productModule.entitie.ProductStatus;
import app_sourcing.productModule.service.ProductService;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.shareClasses.IdGenerator;
import app_sourcing.sourcingModule.entitie.Country;
import app_sourcing.sourcingModule.entitie.FulfillmentMode;
import app_sourcing.walletModule.entitie.FeeType;
import app_sourcing.walletModule.entitie.SellerWallet;
import app_sourcing.walletModule.entitie.TransactionType;
import app_sourcing.walletModule.service.FeeService;
import app_sourcing.walletModule.service.WalletService;
import app_sourcing.walletModule.service.WalletTransactionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final InventoryServiceImpl inventoryService;
    private final WalletService walletService;
    private final WalletTransactionService walletTransactionService;
    private final FeeService feeService;

    // =========================
    // CREATE ORDER (Seller)
    // =========================
    public Order createOrder(Seller seller, CreateOrderDto dto) {

        Product product = productService.getById(dto.getProductId());

        // 1️⃣ Product must be ACTIVE
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessOrderException("Product not active");
        }

        // 2️⃣ Validate country (CRITICAL)
        validateCountry(product, dto.getCountry());

        // 3️⃣ Lead fee : le vendeur doit avoir un solde suffisant pour couvrir le lead fee
        SellerWallet wallet = walletService.getOrCreateWallet(seller);
        BigDecimal leadFee = feeService.getFeeAmount(FeeType.LEAD_FEE);
        if (leadFee.compareTo(BigDecimal.ZERO) > 0 && wallet.getBalance() < leadFee.doubleValue()) {
            throw new BusinessOrderException("Solde wallet insuffisant pour couvrir le lead fee (" + leadFee + " $)");
        }

        Order order = new Order();
        order.setId(IdGenerator.generateId("OR"));
        order.setSeller(seller);
        order.setProduct(product);

        // CUSTOMER
        Customer customer = new Customer();
        customer.setId(IdGenerator.generateId("CU"));
        customer.setCustomerName(dto.getCustomerName());
        customer.setCustomerPhone(dto.getCustomerPhone());
        customer.setCity(dto.getCity());
        customer.setFullAddress(dto.getFullAddress());
        customer.setOrder(order);
        order.setCustomer(customer);

        // ORDER DATA (Seller allowed)
        order.setCountry(dto.getCountry()); // destination
        order.setQuantity(dto.getQuantity());
        order.setUnitPrice(dto.getUnitPrice());
        order.setTotalPrice(
                dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity()))
        );
        order.setPaymentMode(dto.getPaymentMode());
        order.setNote(dto.getNote());

        // SYSTEM DATA
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // INHERIT LOGISTICS
        applyFulfillmentRules(order, product);

        Order savedOrder = orderRepository.save(order);

        // 4️⃣ Prélèvement du lead fee, si configuré
        if (leadFee.compareTo(BigDecimal.ZERO) > 0) {
            walletTransactionService.applyAndRecord(
                    wallet,
                    TransactionType.FEE,
                    FeeType.LEAD_FEE,
                    leadFee.negate(),
                    "Lead fee - commande " + savedOrder.getId(),
                    savedOrder.getId(),
                    null,
                    null
            );
        }

        return savedOrder;
    }

    // =========================
    // CONFIRM ORDER (Admin/System)
    // =========================
    public void confirmOrder(String orderId) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessOrderException("Only PENDING orders can be confirmed");
        }

        inventoryService.reserveStock(
                order.getProduct(),
                order.getQuantity()
        );

        order.setStatus(OrderStatus.CONFIRMED);
    }

    // =========================
    // SHIP
    // =========================
    public void shipOrder(String orderId) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessOrderException("Order must be CONFIRMED before shipping");
        }

        order.setStatus(OrderStatus.SHIPPED);
    }

    // =========================
    // DELIVER
    // =========================
    public void deliverOrder(String orderId) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessOrderException("Order must be SHIPPED before delivery");
        }

        order.setStatus(OrderStatus.DELIVERED);

        // Crédit du revenu de la commande + prélèvement du delivered fee
        SellerWallet wallet = walletService.getOrCreateWallet(order.getSeller());

        walletTransactionService.applyAndRecord(
                wallet,
                TransactionType.PAYMENT_ORDER,
                null,
                order.getTotalPrice(),
                "Revenu commande livrée " + order.getId(),
                order.getId(),
                null,
                null
        );

        BigDecimal deliveredFee = feeService.getFeeAmount(FeeType.DELIVERED_FEE);
        if (deliveredFee.compareTo(BigDecimal.ZERO) > 0) {
            walletTransactionService.applyAndRecord(
                    wallet,
                    TransactionType.FEE,
                    FeeType.DELIVERED_FEE,
                    deliveredFee.negate(),
                    "Delivered fee - commande " + order.getId(),
                    order.getId(),
                    null,
                    null
            );
        }
    }

    // =========================
    // RETURN
    // =========================
    public void returnOrder(String orderId, String reason) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessOrderException("Only DELIVERED orders can be returned");
        }

        inventoryService.moveReservedToReturned(
                order.getProduct(),
                order.getQuantity()
        );

        order.setStatus(OrderStatus.RETURNED);

        // Prélèvement du returned fee, si configuré
        BigDecimal returnedFee = feeService.getFeeAmount(FeeType.RETURNED_FEE);
        if (returnedFee.compareTo(BigDecimal.ZERO) > 0) {
            SellerWallet wallet = walletService.getOrCreateWallet(order.getSeller());
            walletTransactionService.applyAndRecord(
                    wallet,
                    TransactionType.FEE,
                    FeeType.RETURNED_FEE,
                    returnedFee.negate(),
                    "Returned fee - commande " + order.getId() + (reason != null ? " (" + reason + ")" : ""),
                    order.getId(),
                    null,
                    null
            );
        }
    }

    // =========================
    // RETURN to Available
    // =========================
    public void moveReturnedToAvailable(String orderId) {
        Order order = getById(orderId);

        if (order.getStatus() != OrderStatus.RETURNED) {
            throw new BusinessOrderException("Only RETURNED orders can be move to Available");
        }

        inventoryService.moveReturnedToAvailable(
                order.getProduct(),
                order.getQuantity()
        );

        order.setStatus(OrderStatus.RETURNED);
    }

    // =========================
    // INTERNAL RULES
    // =========================
    private void validateCountry(Product product, Country destination) {

        if (product.getFulfillmentMode() == FulfillmentMode.LOCAL_SINGLE_COUNTRY) {
            if (destination != product.getCountry()) {
                throw new BusinessOrderException("Country restricted to local warehouse");
            }
        } else {
            if (!Country.isGccCountry(destination)) {
                throw new BusinessOrderException("Destination not allowed (Central)");
            }
        }
    }

    private void applyFulfillmentRules(Order order, Product product) {

        order.setFulfillmentMode(product.getFulfillmentMode());

        if (product.getFulfillmentMode() == FulfillmentMode.CENTRALIZED) {
            order.setShippingType(ShippingType.INTERNATIONAL);
            order.setVatAmount(calculateVat(order));
        } else {
            order.setShippingType(ShippingType.LOCAL);
            order.setVatAmount(BigDecimal.ZERO);
        }
    }

    private static final BigDecimal VAT_RATE = new BigDecimal("0.05");

    private BigDecimal calculateVat(Order order) {

        // CENTRAL + UAE warehouse + destination != UAE
        if (order.getFulfillmentMode() != FulfillmentMode.CENTRALIZED) {
            return BigDecimal.ZERO;
        }

        if (order.getCountry() == Country.UAE) {
            return BigDecimal.ZERO;
        }

        return order.getTotalPrice()
                .multiply(VAT_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Order getById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }



    public Page<Order> getOrdersBySeller(Long sellerId, Pageable pageable) {
        pageable= PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return orderRepository.findBySellerId(sellerId, pageable);
    }

    public Order getOrderForSeller(String orderId, Long sellerId) {
        return orderRepository.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    // =========================
    // CANCEL ORDER (Seller)
    // =========================
    public void cancelOrder(String orderId, Long sellerId) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessOrderException("Only pending orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
    }

    // =========================
    // UPDATE ORDER (Seller) — uniquement tant que PENDING
    // =========================
    public Order updateOrder(String orderId, Long sellerId, UpdateOrderDto dto) {
        Order order = getOrderForSeller(orderId, sellerId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessOrderException("Only pending orders can be modified");
        }

        Customer customer = order.getCustomer();
        customer.setCustomerName(dto.getCustomerName());
        customer.setCustomerPhone(dto.getCustomerPhone());
        customer.setCity(dto.getCity());
        customer.setFullAddress(dto.getFullAddress());
        order.setNote(dto.getNote());

        return order;
    }

    public Page<Order> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Long getTotalOrdersForCurrentMonth(Seller seller) {
        return orderRepository.countBySellerForCurrentMonth(seller);
    }

    @Transactional(readOnly = true)
    public OrderStatusStatsDTO getOrderStatsByStatus(Seller seller) {
        List<Object[]> results = orderRepository.countByStatusForSeller(seller);

        Map<OrderStatus, Long> statsMap = new HashMap<>();

        // Initialiser tous les statuts à 0
        for (OrderStatus status : OrderStatus.values()) {
            statsMap.put(status, 0L);
        }

        // Mettre à jour avec les valeurs de la base
        for (Object[] result : results) {
            OrderStatus status = (OrderStatus) result[0];
            Long count = (Long) result[1];
            statsMap.put(status, count);
        }

        return new OrderStatusStatsDTO(statsMap);
    }
//    public List<Order> getAllOrders() {
//        return orderRepository.findAll(
//                Sort.by(Sort.Direction.DESC, "createdAt")
//        );
//
//    }


}