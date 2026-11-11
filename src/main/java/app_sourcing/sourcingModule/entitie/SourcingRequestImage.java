package app_sourcing.sourcingModule.entitie;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "sourcing_request_images")
public class SourcingRequestImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chemin relatif (ex: "sourcing-images/uuid_photo.jpg"), servi via /uploads/**
    @Column(nullable = false)
    private String imagePath;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sourcing_request_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SourcingRequest sourcingRequest;

    @CreationTimestamp
    private Instant uploadedAt;
}
