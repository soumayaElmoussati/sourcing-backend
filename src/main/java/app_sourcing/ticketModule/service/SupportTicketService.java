package app_sourcing.ticketModule.service;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.ticketModule.dtos.*;
import app_sourcing.ticketModule.entitie.SupportTicket;
import app_sourcing.ticketModule.entitie.TicketMessage;
import app_sourcing.ticketModule.repo.SupportTicketRepository;
import app_sourcing.ticketModule.repo.TicketMessageRepository;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Transactional
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final UserRepository userRepository;

    @Value("${file.upload.ticket-path:tickets-attachments/}")
    private String uploadDir;

    private final DateTimeFormatter ticketFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Créer un nouveau ticket (par le seller)
     */
    public TicketDTO createTicket(User seller, TicketCreateRequest request) throws IOException {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("Seuls les vendeurs peuvent créer des tickets");
        }

        // Générer un numéro de ticket unique
        String ticketNumber = generateTicketNumber();

        // Créer le ticket
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNumber(ticketNumber);
        ticket.setSeller((Seller) seller);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setStatus("OPEN");

        // Gérer la pièce jointe
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            String attachmentPath = saveAttachment(request.getAttachment(), ticketNumber);
            ticket.setAttachmentsPath(attachmentPath);
        }

        SupportTicket savedTicket = ticketRepository.save(ticket);

        // Créer le premier message (la description)
        TicketMessage initialMessage = new TicketMessage();
        initialMessage.setTicket(savedTicket);
        initialMessage.setSender(seller);
        initialMessage.setMessage(request.getDescription());
        initialMessage.setAdminReply(false);
        messageRepository.save(initialMessage);

        return convertToDTO(savedTicket);
    }

    /**
     * Ajouter un message à un ticket
     */
    public MessageDTO addMessage(User sender, Long ticketId, MessageRequest request) throws IOException {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket non trouvé"));

        // Vérifier les permissions
        if (sender instanceof Seller && !ticket.getSeller().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à commenter ce ticket");
        }

        // Vérifier que le ticket n'est pas fermé
        if ("CLOSED".equals(ticket.getStatus())) {
            throw new IllegalArgumentException("Impossible d'ajouter un message à un ticket fermé");
        }

        // Si c'est un admin et que le ticket est en OPEN, le passer en IN_PROGRESS
        if (isAdmin(sender) && "OPEN".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }

        // Créer le message
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setMessage(request.getMessage());
        message.setAdminReply(isAdmin(sender));

        // Gérer la pièce jointe
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            String attachmentPath = saveAttachment(request.getAttachment(), ticket.getTicketNumber() + "_msg");
            message.setAttachmentsPath(attachmentPath);
        }

        TicketMessage savedMessage = messageRepository.save(message);

        // Mettre à jour le ticket
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return convertToMessageDTO(savedMessage);
    }

    /**
     * Récupérer tous les tickets (pour admin) avec pagination
     */
    public PageResponseDTO<TicketDTO> getAllTicketsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findAll(pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les tickets par statut (pour admin)
     */
    public PageResponseDTO<TicketDTO> getTicketsByStatusForAdmin(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findByStatusOrderByPriorityAndDate(status, pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les tickets par priorité (pour admin)
     */
    public PageResponseDTO<TicketDTO> getTicketsByPriorityForAdmin(String priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findByPriority(priority, pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les tickets par catégorie (pour admin)
     */
    public PageResponseDTO<TicketDTO> getTicketsByCategoryForAdmin(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findByCategory(category, pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les tickets assignés à un admin
     */
    public PageResponseDTO<TicketDTO> getTicketsAssignedToAdmin(Long adminId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findByAssignedToId(adminId, pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les tickets d'un seller spécifique (pour admin)
     */
    public PageResponseDTO<TicketDTO> getTicketsBySellerForAdmin(Long sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les tickets du seller connecté
     */
    public PageResponseDTO<TicketDTO> getMyTickets(User seller, int page, int size) {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("Utilisateur non autorisé");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SupportTicket> ticketPage = ticketRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId(), pageable);

        return convertToPageResponse(ticketPage);
    }

    /**
     * Récupérer les détails d'un ticket avec ses messages
     */
    public TicketDTO getTicketDetails(Long ticketId, User user) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket non trouvé"));

        // Vérifier les permissions
        if (user instanceof Seller && !ticket.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Vous n'êtes pas autorisé à voir ce ticket");
        }

        // Marquer les messages comme lus
        if (isAdmin(user)) {
            messageRepository.markMessagesAsReadByAdmin(ticketId, user.getId());
        } else {
            messageRepository.markAdminRepliesAsRead(ticketId);
        }

        return convertToDTOWithMessages(ticket);
    }

    /**
     * Assigner un ticket à un admin
     */
    @Transactional
    public TicketDTO assignTicket(Long ticketId, TicketAssignmentRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket non trouvé"));

        User admin = userRepository.findById(request.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("Admin non trouvé"));

        ticket.setAssignedTo(admin);

        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }

        if ("OPEN".equals(ticket.getStatus())) {
            ticket.setStatus("IN_PROGRESS");
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket updatedTicket = ticketRepository.save(ticket);

        return convertToDTO(updatedTicket);
    }

    /**
     * Mettre à jour le statut d'un ticket
     */
    @Transactional
    public TicketDTO updateTicketStatus(Long ticketId, TicketStatusUpdateRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket non trouvé"));

        String oldStatus = ticket.getStatus();
        String newStatus = request.getStatus();

        ticket.setStatus(newStatus);

        // Gérer les dates en fonction du statut
        if ("RESOLVED".equals(newStatus) && !"RESOLVED".equals(oldStatus)) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if ("CLOSED".equals(newStatus) && !"CLOSED".equals(oldStatus)) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket updatedTicket = ticketRepository.save(ticket);

        // Ajouter un message système si nécessaire
        if (request.getResolution() != null && !request.getResolution().isEmpty()) {
            addSystemMessage(ticket, request.getResolution());
        }

        return convertToDTO(updatedTicket);
    }

    /**
     * Obtenir les statistiques des tickets
     */
    public TicketStatsDTO getTicketStats() {
        TicketStatsDTO stats = new TicketStatsDTO();

        stats.setTotalTickets(ticketRepository.count());
        stats.setOpenTickets(ticketRepository.countByStatus("OPEN"));
        stats.setInProgressTickets(ticketRepository.countByStatus("IN_PROGRESS"));
        stats.setResolvedTickets(ticketRepository.countByStatus("RESOLVED"));
        stats.setClosedTickets(ticketRepository.countByStatus("CLOSED"));
        stats.setUrgentTickets(ticketRepository.countUrgentOpenTickets());

        // Tickets haute priorité
        Pageable pageable = PageRequest.of(0, 1);
        Page<SupportTicket> highPriorityPage = ticketRepository.findByPriority("HIGH", pageable);
        stats.setHighPriorityTickets(highPriorityPage.getTotalElements());

        return stats;
    }

    /**
     * Obtenir les statistiques pour un seller
     */
    public TicketStatsDTO getSellerTicketStats(User seller) {
        if (!(seller instanceof Seller)) {
            throw new IllegalArgumentException("Utilisateur non autorisé");
        }

        TicketStatsDTO stats = new TicketStatsDTO();

        Pageable pageable = PageRequest.of(0, 1);

        Page<SupportTicket> allTickets = ticketRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId(), pageable);
        stats.setTotalTickets(allTickets.getTotalElements());

        // Compter par statut (simplifié - dans un cas réel, vous feriez des requêtes spécifiques)
        stats.setOpenTickets(countTicketsByStatusForSeller(seller.getId(), "OPEN"));
        stats.setInProgressTickets(countTicketsByStatusForSeller(seller.getId(), "IN_PROGRESS"));
        stats.setResolvedTickets(countTicketsByStatusForSeller(seller.getId(), "RESOLVED"));
        stats.setClosedTickets(countTicketsByStatusForSeller(seller.getId(), "CLOSED"));

        stats.setUnreadMessages(messageRepository.countUnreadAdminRepliesForSeller(seller.getId()));

        return stats;
    }

    // Méthodes privées utilitaires

    private String generateTicketNumber() {
        String timestamp = LocalDateTime.now().format(ticketFormatter);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "TKT-" + timestamp + "-" + random;
    }

    private String saveAttachment(MultipartFile file, String prefix) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = prefix + "_" + UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    private boolean isAdmin(User user) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private long countTicketsByStatusForSeller(Long sellerId, String status) {
        Pageable pageable = PageRequest.of(0, 1);
        Page<SupportTicket> page = ticketRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        // Dans un cas réel, vous feriez une requête spécifique
        return 0; // Simplifié
    }

    private void addSystemMessage(SupportTicket ticket, String resolution) {
        // Implémentation pour ajouter un message système
    }

    private PageResponseDTO<TicketDTO> convertToPageResponse(Page<SupportTicket> page) {
        List<TicketDTO> content = page.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResponseDTO<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }

    private TicketDTO convertToDTO(SupportTicket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setTicketNumber(ticket.getTicketNumber());
        dto.setSellerId(ticket.getSeller().getId());
        dto.setSellerName(ticket.getSeller().getFirstName() + " " + ticket.getSeller().getLastName());
        dto.setSellerEmail(ticket.getSeller().getEmail());
        dto.setStoreName(ticket.getSeller().getStoreName());
        dto.setSubject(ticket.getSubject());
        dto.setDescription(ticket.getDescription());
        dto.setCategory(ticket.getCategory());
        dto.setPriority(ticket.getPriority());
        dto.setStatus(ticket.getStatus());

        if (ticket.getAssignedTo() != null) {
            dto.setAssignedToId(ticket.getAssignedTo().getId());
            dto.setAssignedToName(ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName());
        }

        dto.setAttachmentsPath(ticket.getAttachmentsPath());
        dto.setCreatedAt(ticket.getCreatedAt());
        dto.setUpdatedAt(ticket.getUpdatedAt());
        dto.setResolvedAt(ticket.getResolvedAt());
        dto.setClosedAt(ticket.getClosedAt());

        dto.setMessageCount(ticket.getMessages().size());

        return dto;
    }

    private TicketDTO convertToDTOWithMessages(SupportTicket ticket) {
        TicketDTO dto = convertToDTO(ticket);

        List<MessageDTO> messages = ticket.getMessages().stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());

        dto.setRecentMessages(messages);

        return dto;
    }

    private MessageDTO convertToMessageDTO(TicketMessage message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setTicketId(message.getTicket().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getFirstName() + " " + message.getSender().getLastName());
        dto.setSenderRole(isAdmin(message.getSender()) ? "ADMIN" : "SELLER");
        dto.setMessage(message.getMessage());
        dto.setAttachmentsPath(message.getAttachmentsPath());
        dto.setAdminReply(message.isAdminReply());
        dto.setRead(message.isRead());
        dto.setCreatedAt(message.getCreatedAt());

        return dto;
    }
}
