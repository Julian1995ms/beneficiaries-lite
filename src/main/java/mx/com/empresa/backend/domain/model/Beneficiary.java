package mx.com.empresa.backend.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class Beneficiary {

    private final Long id;
    private final String customerId;
    private final String alias;
    private final String holderName;
    private final String clabe;
    private final String bankCode;
    private final BigDecimal maxAmount;
    private final String email;
    private BeneficiaryStatus status;
    private final LocalDateTime createdAt;

    public Beneficiary(Long id, String customerId, String alias, String holderName, String clabe, String bankCode,
                       BigDecimal maxAmount, String email, BeneficiaryStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.alias = alias;
        this.holderName = holderName;
        this.clabe = clabe;
        this.bankCode = bankCode;
        this.maxAmount = maxAmount;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void deactivate() {
        this.status = BeneficiaryStatus.INACTIVE;
    }
}
