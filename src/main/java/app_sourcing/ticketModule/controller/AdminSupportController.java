package app_sourcing.ticketModule.controller;

import app_sourcing.ticketModule.dtos.*;
import app_sourcing.ticketModule.service.SupportTicketService;
import app_sourcing.userModule.entitie.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportTicketService ticketService;

    @GetMapping("/tickets")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getAllTicketsForAdmin(page, size));
    }

    @GetMapping("/tickets/status/{status}")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsByStatusForAdmin(status, page, size));
    }

    @GetMapping("/tickets/priority/{priority}")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsByPriority(
            @PathVariable String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsByPriorityForAdmin(priority, page, size));
    }

    @GetMapping("/tickets/category/{category}")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsByCategoryForAdmin(category, page, size));
    }


    @GetMapping("/tickets/assigned/{adminId}")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsAssignedToAdmin(
            @PathVariable Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsAssignedToAdmin(adminId, page, size));
    }

    @GetMapping("/tickets/seller/{sellerId}")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getTicketsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getTicketsBySellerForAdmin(sellerId, page, size));
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<TicketDTO> getTicketDetails(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User admin
    ) {
        return ResponseEntity.ok(ticketService.getTicketDetails(ticketId, admin));
    }

    @PostMapping("/tickets/{ticketId}/assign")
    public ResponseEntity<TicketDTO> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketAssignmentRequest request
    ) {
        return ResponseEntity.ok(ticketService.assignTicket(ticketId, request));
    }

    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<TicketDTO> updateTicketStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(ticketId, request));
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<MessageDTO> addMessage(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User admin,
            @Valid @ModelAttribute MessageRequest request
    ) throws IOException {
        return ResponseEntity.ok(ticketService.addMessage(admin, ticketId, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsDTO> getStats() {
        return ResponseEntity.ok(ticketService.getTicketStats());
    }
}
