package app_sourcing.orders.controller;


import app_sourcing.orders.dtos.CreateOrderDto;
import app_sourcing.orders.dtos.OrderResponseDto;
import app_sourcing.orders.dtos.UpdateOrderDto;
import app_sourcing.orders.entities.Order;
import app_sourcing.orders.mappers.OrderMapper;
import app_sourcing.orders.services.OrderService;
import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.userModule.entitie.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // =========================
    // CREATE ORDER (Seller)
    // =========================
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @AuthenticationPrincipal User seller,
            @RequestBody CreateOrderDto dto
    ) {
        Order order = orderService.createOrder((Seller) seller, dto);
        return new ResponseEntity<>(OrderMapper.toDto(order), HttpStatus.CREATED);
    }

    // =========================
    // GET SELLER ORDERS
    // =========================
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getSellerOrders(
            @AuthenticationPrincipal User seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> ordersPage = orderService.getOrdersBySeller(seller.getId(), pageable);
        Page<OrderResponseDto> ordersDtoPage = ordersPage.map(OrderMapper::toDto);
        return ResponseEntity.ok(ordersDtoPage);
    }

    // =========================
    // VIEW ORDER DETAILS
    // =========================
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderDetails(
            @PathVariable String orderId,
            @AuthenticationPrincipal User seller
    ) {
        OrderResponseDto order=OrderMapper.toDto(orderService.getOrderForSeller(orderId, seller.getId()));
        return ResponseEntity.ok(order);
    }

    // =========================
    // UPDATE ORDER (Seller) — uniquement tant que PENDING
    // =========================
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> updateOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal User seller,
            @RequestBody UpdateOrderDto dto
    ) {
        Order updated = orderService.updateOrder(orderId, seller.getId(), dto);
        return ResponseEntity.ok(OrderMapper.toDto(updated));
    }

    // =========================
    // CANCEL ORDER (Seller) — uniquement tant que PENDING
    // =========================
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal User seller
    ) {
        orderService.cancelOrder(orderId, seller.getId());
        return ResponseEntity.noContent().build();
    }
}
