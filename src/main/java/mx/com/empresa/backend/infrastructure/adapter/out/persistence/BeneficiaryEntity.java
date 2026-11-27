package mx.com.empresa.backend.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import mx.com.empresa.backend.domain.model.BeneficiaryStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "beneficiaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_beneficiaries_customer_clabe",
                columnNames = {"customer_id", "clabe"}),
        indexes = @Index(
                name = "idx_beneficiaries_customer_status",
                columnList = "customer_id,status"))
@Getter
@Setter
@NoArgsConstructor
public class BeneficiaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 8)
    private String customerId;

    @Column(nullable = false, length = 30)
    private String alias;

    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, length = 18)
    private String clabe;

    @Column(name = "bank_code", nullable = false, length = 3)
    private String bankCode;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private BeneficiaryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
