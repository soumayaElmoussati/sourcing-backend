package app_sourcing.appsModule.controller;

import app_sourcing.appsModule.dto.AppConnectionDTO;
import app_sourcing.appsModule.dto.InboundWebhookPayload;
import app_sourcing.appsModule.dto.OAuthInitResponse;
import app_sourcing.appsModule.entitie.AppConnection;
import app_sourcing.appsModule.repository.AppConnectionRepository;
import app_sourcing.appsModule.service.AppConnectionService;
import app_sourcing.appsModule.service.OrderIngestionService;
import app_sourcing.orders.entities.OrderSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppsController {

    private final AppConnectionService    appConnectionService;
    private final OrderIngestionService   orderIngestionService;
    private final AppConnectionRepository connectionRepo;

    @GetMapping("/api/apps")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<AppConnectionDTO>> getConnections() {
        return ResponseEntity.ok(appConnectionService.getConnectionsForCurrentSeller());
    }

    // ── OAuth init ────────────────────────────────────────────────────────────
    // shopDomain : query param obligatoire pour Shopify, ignoré pour les autres
    // Exemple : POST /api/apps/oauth/init/SHOPIFY?shopDomain=mon-shop.myshopify.com

    @PostMapping("/api/apps/oauth/init/{appType}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<OAuthInitResponse> initiateOAuth(
            @PathVariable OrderSource appType,
            @RequestParam(required = false) String shopDomain) {
        return ResponseEntity.ok(appConnectionService.initiateOAuth(appType, shopDomain));
    }

    // ── OAuth callback ────────────────────────────────────────────────────────
    // Appelé par Shopify/YouCan/Lightfunnels après autorisation du seller
    // PAS protégé par JWT (redirection navigateur depuis la plateforme externe)

    @GetMapping("/api/apps/oauth/callback/{appType}")
    public void handleOAuthCallback(
            @PathVariable OrderSource appType,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String hmac,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String frontendAppsUrl = appConnectionService.getFrontendUrl() + "/apps";

        if (error != null) {
            log.warn("[OAuth] Refus seller appType={} error={}", appType, error);
            response.sendRedirect(frontendAppsUrl + "?oauth_error=" + appType.name());
            return;
        }

        try {
            // Récupérer tous les query params pour la validation HMAC Shopify
            Map<String, String> allParams = new HashMap<>();
            request.getParameterMap().forEach((k, v) -> {
                if (v != null && v.length > 0) allParams.put(k, v[0]);
            });

            appConnectionService.handleOAuthCallback(code, state, allParams, hmac);
            response.sendRedirect(frontendAppsUrl + "?oauth_success=" + appType.name());

        } catch (SecurityException e) {
            log.error("[OAuth] HMAC invalide appType={}", appType);
            response.sendRedirect(frontendAppsUrl + "?oauth_error=" + appType.name());
        } catch (Exception e) {
            log.error("[OAuth] Erreur callback appType={} : {}", appType, e.getMessage(), e);
            response.sendRedirect(frontendAppsUrl + "?oauth_error=" + appType.name());
        }
    }

    // ── Webhook inbound ───────────────────────────────────────────────────────

    @PostMapping("/api/apps/webhook/activate/{appType}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AppConnectionDTO> activateWebhook(@PathVariable OrderSource appType) {
        return ResponseEntity.ok(appConnectionService.activateWebhookInbound(appType));
    }

    @PostMapping("/api/apps/webhook/regenerate/{appType}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<AppConnectionDTO> regenerateWebhookToken(@PathVariable OrderSource appType) {
        return ResponseEntity.ok(appConnectionService.regenerateWebhookToken(appType));
    }

    // ── Déconnexion ───────────────────────────────────────────────────────────

    @DeleteMapping("/api/apps/{appType}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> disconnect(@PathVariable OrderSource appType) {
        appConnectionService.disconnect(appType);
        return ResponseEntity.noContent().build();
    }

    // ── Webhook entrant public ────────────────────────────────────────────────
    // Pas de JWT — auth par token opaque dans l'URL

    @PostMapping("/api/webhooks/{token}")
    public ResponseEntity<Map<String, String>> receiveWebhook(
            @PathVariable String token,
            @RequestBody InboundWebhookPayload payload) {

        AppConnection conn = connectionRepo.findByWebhookToken(token).orElse(null);
        if (conn == null) {
            log.warn("[Webhook] Token inconnu : {}", token);
            return ResponseEntity.ok(Map.of("status", "ignored"));
        }

        try {
            orderIngestionService.ingestFromWebhook(conn, payload);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (Exception e) {
            log.error("[Webhook] Erreur ingestion token={} : {}", token, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}