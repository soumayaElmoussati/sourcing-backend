package app_sourcing.orders.entities;

import app_sourcing.sourcingModule.entitie.SourcingRequest;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "order_customers")
@Data
public class Customer {

    @Id
    @Column(length = 10, nullable = false, updatable = false)
    private String id;
    private String customerName;
    private String customerPhone;
    private String city;
    private String fullAddress;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

}

