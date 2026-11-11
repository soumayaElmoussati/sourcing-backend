package app_sourcing.userModule.controller;

import app_sourcing.sellerModule.entitie.Seller;
import app_sourcing.sellerModule.repo.SellerRepository;
import app_sourcing.userModule.entitie.User;
import app_sourcing.userModule.repo.UserRepository;
import app_sourcing.userModule.request.LoginRequest;
import app_sourcing.userModule.request.LoginResponse;
import app_sourcing.userModule.service.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final JwtTokenProvider      jwtTokenProvider;
    private final SellerRepository      sellerRepository;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs; // 86400000 = 24h

    // ── Login Admin ───────────────────────────────────────────────────────────

    @PostMapping("/login_admin")
    public ResponseEntity<LoginResponse> loginAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User userPrincipal = (User) authentication.getPrincipal();

        String token        = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenExpiresAt(System.currentTimeMillis() + jwtExpirationMs) // ← 24h
                .id(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .roles(userPrincipal.getAuthorities().stream()
                        .map(auth -> auth.getAuthority()).toList())
                .accountVerified(true)
                .build());
    }

    // ── Login Seller ──────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> loginSeller(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User userPrincipal = (User) authentication.getPrincipal();

            String token        = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
            long   expiresAt    = System.currentTimeMillis() + jwtExpirationMs; // ← 24h

            boolean isSeller = userPrincipal.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_SELLER"));

            String  sellerStatus     = null;
            boolean accountVerified  = true;

            if (isSeller) {
                Seller seller = sellerRepository.findByEmail(userPrincipal.getEmail())
                        .orElseThrow(() -> new RuntimeException("Seller introuvable"));
                sellerStatus    = seller.getStatus();
                accountVerified = "approved".equalsIgnoreCase(sellerStatus);
            }

            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .tokenExpiresAt(expiresAt)  // ← timestamp 24h
                    .id(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .firstName(userPrincipal.getFirstName())
                    .lastName(userPrincipal.getLastName())
                    .roles(userPrincipal.getAuthorities().stream()
                            .map(auth -> auth.getAuthority()).toList())
                    .status(sellerStatus)
                    .accountVerified(accountVerified)
                    .build());

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Email ou mot de passe incorrect"));
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Déconnecté avec succès");
    }

    // ── Validate token ────────────────────────────────────────────────────────
    // Appelé par le AuthGuard frontend à chaque navigation

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        String token = authHeader.substring(7);

        try {
            if (!jwtTokenProvider.isTokenExpired(token)) {
                return ResponseEntity.ok(true);
            }
        } catch (Exception e) {
            // token malformé ou signature invalide
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
    }
}