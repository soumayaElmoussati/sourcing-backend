package app_sourcing.ticketModule.controller;

import app_sourcing.ticketModule.dtos.*;
import app_sourcing.ticketModule.service.SupportTicketService;
import app_sourcing.userModule.entitie.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seller/support")
@RequiredArgsConstructor
public class SellerSupportController {

    private final SupportTicketService ticketService;

    @PostMapping("/tickets")
    public ResponseEntity<?> createTicket(
            @AuthenticationPrincipal User seller,
            @Valid @ModelAttribute TicketCreateRequest request
    ) {
        try {
            TicketDTO ticket = ticketService.createTicket(seller, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Ticket créé avec succès");
            response.put("ticketId", ticket.getId());
            response.put("ticketNumber", ticket.getTicketNumber());
            response.put("status", ticket.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du téléchargement de la pièce jointe");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/tickets")
    public ResponseEntity<PageResponseDTO<TicketDTO>> getMyTickets(
            @AuthenticationPrincipal User seller,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ticketService.getMyTickets(seller, page, size));
    }

    @GetMapping("/tickets/{ticketId}")
    public ResponseEntity<TicketDTO> getTicketDetails(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User seller
    ) {
        return ResponseEntity.ok(ticketService.getTicketDetails(ticketId, seller));
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<MessageDTO> addMessage(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User seller,
            @Valid @ModelAttribute MessageRequest request
    ) throws IOException {
        return ResponseEntity.ok(ticketService.addMessage(seller, ticketId, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<TicketStatsDTO> getMyStats(
            @AuthenticationPrincipal User seller
    ) {
        return ResponseEntity.ok(ticketService.getSellerTicketStats(seller));
    }
}
