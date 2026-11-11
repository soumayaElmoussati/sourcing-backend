package app_sourcing.orders.controller;

import app_sourcing.orders.dtos.OrderResponseDto;
import app_sourcing.orders.entities.Order;
import app_sourcing.orders.mappers.OrderMapper;
import app_sourcing.orders.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;


    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Order> orderPage = orderService.getAllOrders(page, size);
        Page<OrderResponseDto> dtoPage = orderPage.map(OrderMapper::toDto);

        return ResponseEntity.ok(dtoPage);
    }

    // =========================
    // CONFIRM ORDER
    // =========================
    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(@PathVariable String orderId) {
        orderService.confirmOrder(orderId);
        return ResponseEntity.ok().build();
    }

    // =========================
    // SHIP ORDER
    // =========================
    @PatchMapping("/{orderId}/ship")
    public ResponseEntity<Void> shipOrder(@PathVariable String orderId) {
        orderService.shipOrder(orderId);
        return ResponseEntity.ok().build();
    }

    // =========================
    // DELIVER ORDER
    // =========================
    @PatchMapping("/{orderId}/deliver")
    public ResponseEntity<Void> deliverOrder(@PathVariable String orderId) {
        orderService.deliverOrder(orderId);
        return ResponseEntity.ok().build();
    }

    // =========================
    // RETURN ORDER
    // =========================
    @PostMapping("/{orderId}/return")
    public ResponseEntity<Void> returnOrder(
            @PathVariable String orderId,
            @RequestParam String reason
    ) {
        orderService.returnOrder(orderId, reason);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/restock-returned")
    public ResponseEntity<Void> restockReturned(@PathVariable String id) {
        orderService.moveReturnedToAvailable(id);
        return ResponseEntity.ok().build();
    }

}
