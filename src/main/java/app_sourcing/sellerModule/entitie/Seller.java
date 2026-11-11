package app_sourcing.sellerModule.entitie;


import app_sourcing.userModule.entitie.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "sellers")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("SELLER")
public class Seller extends User {

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(nullable = false)
    private String country;

    @Column(name = "budget")
    private String budget;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "commerce_experience")
    private Boolean commerceExperience = false;

    @Column(name = "ads_experience")
    private Boolean adsExperience = false;

    @Column(name = "id_card_recto_path")
    private String idCardRectoPath;

    @Column(name = "id_card_verso_path")
    private String idCardVersoPath;

    private String status = "pending";

    // ══ Contrat & Signature ═══════════════
    @Column(name = "signature_base64", columnDefinition = "LONGTEXT")
    private String signatureBase64;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "contract_version", length = 20)
    private String contractVersion;

    @Column(name = "contract_number", length = 30, unique = true)
    private String contractNumber;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_SELLER"));
    }
}