package mx.com.empresa.backend.application.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import mx.com.empresa.backend.application.dto.request.CreateBeneficiaryRequest;
import mx.com.empresa.backend.application.dto.response.BeneficiaryResponse;
import mx.com.empresa.backend.application.exception.BusinessError;
import mx.com.empresa.backend.application.exception.BusinessException;
import mx.com.empresa.backend.application.port.in.BeneficiaryUseCase;
import mx.com.empresa.backend.application.port.out.BeneficiaryRepositoryPort;
import mx.com.empresa.backend.application.port.out.TransactionExecutor;
import mx.com.empresa.backend.domain.model.Beneficiary;
import mx.com.empresa.backend.domain.model.BeneficiaryStatus;
import mx.com.empresa.backend.domain.validation.ClabeValidator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BeneficiaryService implements BeneficiaryUseCase {

    private static final Logger LOG = Logger.getLogger(BeneficiaryService.class);
    private static final int MAX_ACTIVE_BENEFICIARIES = 10;

    private final BeneficiaryRepositoryPort repository;
    private final TransactionExecutor transactionExecutor;
    private final ConcurrentMap<String, ReentrantLock> customerLocks = new ConcurrentHashMap<>();

    @Inject
    public BeneficiaryService(BeneficiaryRepositoryPort repository, TransactionExecutor transactionExecutor) {
        this.repository = repository;
        this.transactionExecutor = transactionExecutor;
    }

    @Override
    public BeneficiaryResponse create(CreateBeneficiaryRequest request) {
        LOG.infof("Inicio de registro de beneficiario. CLABE=%s", maskClabe(request.getClabe()));

        if (!ClabeValidator.isValid(request.getClabe())) {
            LOG.warnf("Registro rechazado por CLABE inválida. CLABE=%s", maskClabe(request.getClabe()));
            throw new BusinessException(BusinessError.INVALID_CLABE);
        }

        // Permite sincronizar las operaciones de escritura por cliente sin bloquear las de otros clientes.
        ReentrantLock lock = customerLocks.computeIfAbsent(request.getCustomerId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            BeneficiaryResponse response = transactionExecutor.execute(() -> createWithinTransaction(request));
            LOG.infof("Beneficiario registrado correctamente. id=%d, CLABE=%s", response.getId(), maskClabe(response.getClabe()));
            return response;
        } catch (BusinessException exception) {
            LOG.warnf("Registro rechazado por regla de negocio. error=%s, CLABE=%s",
                    exception.getError().name(), maskClabe(request.getClabe()));
            throw exception;
        } finally {
            lock.unlock();
        }
    }

    private BeneficiaryResponse createWithinTransaction(CreateBeneficiaryRequest request) {
        // La unicidad de la CLABE por cliente aplica independientemente del estado del beneficiario.
        if (repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())) {
            throw new BusinessException(BusinessError.DUPLICATE_CLABE);
        }

        if (repository.countActiveByCustomerId(request.getCustomerId()) >= MAX_ACTIVE_BENEFICIARIES) {
            throw new BusinessException(BusinessError.ACTIVE_LIMIT_REACHED);
        }

        Beneficiary beneficiary = new Beneficiary(
                null,
                request.getCustomerId(),
                request.getAlias(),
                request.getHolderName(),
                request.getClabe(),
                request.getClabe().substring(0, 3),
                request.getMaxAmount(),
                request.getEmail(),
                BeneficiaryStatus.ACTIVE,
                LocalDateTime.now(ZoneOffset.UTC));

        return toResponse(repository.save(beneficiary));
    }

    @Override
    public List<BeneficiaryResponse> findActiveByCustomerId(String customerId) {
        LOG.info("Consulta de beneficiarios activos solicitada");
        return repository.findActiveByCustomerId(customerId).stream().map(this::toResponse).toList();
    }

    @Override
    public BeneficiaryResponse findById(Long id) {
        LOG.infof("Consulta de beneficiario por id. id=%d", id);
        Beneficiary beneficiary = repository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessError.BENEFICIARY_NOT_FOUND));
        return toResponse(beneficiary);
    }

    @Override
    public void deactivate(Long id) {
        LOG.infof("Inicio de desactivación de beneficiario. id=%d", id);
        Beneficiary initial = repository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessError.BENEFICIARY_NOT_FOUND));

        ReentrantLock lock = customerLocks.computeIfAbsent(initial.getCustomerId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            // Mantiene el bloqueo hasta finalizar la transacción para evitar superar el límite de beneficiarios activos.
            transactionExecutor.execute(() -> {
                // Consulta nuevamente el estado dentro de la transacción para evitar decisiones basadas en datos desactualizados.
                Beneficiary current = repository.findById(id)
                        .orElseThrow(() -> new BusinessException(BusinessError.BENEFICIARY_NOT_FOUND));
                if (current.getStatus() == BeneficiaryStatus.INACTIVE) {
                    throw new BusinessException(BusinessError.BENEFICIARY_ALREADY_INACTIVE);
                }
                current.deactivate();
                repository.save(current);
                return null;
            });
            LOG.infof("Beneficiario desactivado correctamente. id=%d", id);
        } finally {
            lock.unlock();
        }
    }

    private BeneficiaryResponse toResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getCustomerId(),
                beneficiary.getAlias(),
                beneficiary.getHolderName(),
                beneficiary.getClabe(),
                beneficiary.getBankCode(),
                beneficiary.getMaxAmount(),
                beneficiary.getEmail(),
                beneficiary.getStatus(),
                beneficiary.getCreatedAt());
    }

    // Protege la información bancaria en los logs, mostrando únicamente los últimos cuatro caracteres de la CLABE.
    private String maskClabe(String clabe) {
        if (clabe == null || clabe.length() < 4) {
            return "****";
        }
        return "*".repeat(clabe.length() - 4) + clabe.substring(clabe.length() - 4);
    }
}
