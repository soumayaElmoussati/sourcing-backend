package app_sourcing.walletModule.entitie;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Un seul Fee par FeeType (contrainte unique) : c'est la valeur actuellement
// appliquée pour ce type de frais. Modifier/supprimer remplace cette valeur.
@Data
@Entity
@Table(name = "fees")
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private FeeType type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
