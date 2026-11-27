package mx.com.empresa.backend.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import mx.com.empresa.backend.application.exception.BusinessError;
import mx.com.empresa.backend.application.exception.BusinessException;
import mx.com.empresa.backend.application.port.out.BeneficiaryRepositoryPort;
import mx.com.empresa.backend.domain.model.Beneficiary;
import mx.com.empresa.backend.domain.model.BeneficiaryStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class BeneficiaryPersistenceAdapter implements BeneficiaryRepositoryPort {

    private static final String UNIQUE_CONSTRAINT = "uk_beneficiaries_customer_clabe";

    private final EntityManager entityManager;
    private final BeneficiaryMapper mapper;

    @Inject
    public BeneficiaryPersistenceAdapter(EntityManager entityManager, BeneficiaryMapper mapper) {
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    @Override
    public Beneficiary save(Beneficiary beneficiary) {
        try {
            if (beneficiary.getId() == null) {
                BeneficiaryEntity entity = mapper.toEntity(beneficiary);
                entityManager.persist(entity);
                entityManager.flush();
                return mapper.toDomain(entity);
            }

            BeneficiaryEntity managed = entityManager.find(BeneficiaryEntity.class, beneficiary.getId());
            if (managed == null) {
                throw new BusinessException(BusinessError.BENEFICIARY_NOT_FOUND);
            }
            mapper.updateEntity(beneficiary, managed);
            entityManager.flush();
            return mapper.toDomain(managed);
        } catch (PersistenceException exception) {
            if (isCustomerClabeUniqueViolation(exception)) {
                throw new BusinessException(BusinessError.DUPLICATE_CLABE);
            }
            throw exception;
        }
    }

    @Override
    public Optional<Beneficiary> findById(Long id) {
        return Optional.ofNullable(entityManager.find(BeneficiaryEntity.class, id)).map(mapper::toDomain);
    }

    @Override
    public List<Beneficiary> findActiveByCustomerId(String customerId) {
        return entityManager.createQuery(
                        "SELECT b FROM BeneficiaryEntity b "
                                + "WHERE b.customerId = :customerId AND b.status = :status ORDER BY b.createdAt, b.id",
                        BeneficiaryEntity.class)
                .setParameter("customerId", customerId)
                .setParameter("status", BeneficiaryStatus.ACTIVE)
                .getResultList()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCustomerIdAndClabe(String customerId, String clabe) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(b) FROM BeneficiaryEntity b WHERE b.customerId = :customerId AND b.clabe = :clabe",
                        Long.class)
                .setParameter("customerId", customerId)
                .setParameter("clabe", clabe)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public long countActiveByCustomerId(String customerId) {
        return entityManager.createQuery(
                        "SELECT COUNT(b) FROM BeneficiaryEntity b WHERE b.customerId = :customerId AND b.status = :status",
                        Long.class)
                .setParameter("customerId", customerId)
                .setParameter("status", BeneficiaryStatus.ACTIVE)
                .getSingleResult();
    }

    private boolean isCustomerClabeUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName != null && constraintName.toLowerCase(Locale.ROOT).contains(UNIQUE_CONSTRAINT)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
