package mx.com.empresa.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import mx.com.empresa.backend.application.dto.request.CreateBeneficiaryRequest;
import mx.com.empresa.backend.application.exception.BusinessError;
import mx.com.empresa.backend.application.exception.BusinessException;
import mx.com.empresa.backend.application.port.out.BeneficiaryRepositoryPort;
import mx.com.empresa.backend.application.port.out.TransactionExecutor;
import mx.com.empresa.backend.domain.model.Beneficiary;
import mx.com.empresa.backend.domain.model.BeneficiaryStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    BeneficiaryRepositoryPort repository;

    private BeneficiaryService service;

    @BeforeEach
    void setUp() {
        TransactionExecutor directTransactionExecutor = new TransactionExecutor() {
            @Override
            public <T> T execute(Supplier<T> action) {
                return action.get();
            }
        };
        service = new BeneficiaryService(repository, directTransactionExecutor);
    }

    @Test
    void shouldCreateBeneficiarySuccessfully() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(false);
        when(repository.countActiveByCustomerId(request.getCustomerId())).thenReturn(0L);
        when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        var response = service.create(request);

        assertEquals(1L, response.getId());
        assertEquals("032", response.getBankCode());
        assertEquals(BeneficiaryStatus.ACTIVE, response.getStatus());
    }

    @Test
    void shouldRejectDuplicateClabe() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(BusinessError.DUPLICATE_CLABE, exception.getError());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectEleventhActiveBeneficiary() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(false);
        when(repository.countActiveByCustomerId(request.getCustomerId())).thenReturn(10L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(BusinessError.ACTIVE_LIMIT_REACHED, exception.getError());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldAllowTenthActiveBeneficiaryWhenNineExist() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(false);
        when(repository.countActiveByCustomerId(request.getCustomerId())).thenReturn(9L);
        when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 10L));

        var response = service.create(request);

        assertEquals(10L, response.getId());
    }

    @Test
    void shouldRejectDuplicateEvenIfPreviousRecordIsInactive() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(BusinessError.DUPLICATE_CLABE, exception.getError());
    }

    @Test
    void shouldFindActiveBeneficiaries() {
        when(repository.findActiveByCustomerId("12345678")).thenReturn(List.of(beneficiary(1L, BeneficiaryStatus.ACTIVE)));

        var result = service.findActiveByCustomerId("12345678");

        assertEquals(1, result.size());
        assertEquals(BeneficiaryStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void shouldFindBeneficiaryById() {
        when(repository.findById(1L)).thenReturn(Optional.of(beneficiary(1L, BeneficiaryStatus.INACTIVE)));

        var response = service.findById(1L);

        assertEquals(1L, response.getId());
        assertEquals(BeneficiaryStatus.INACTIVE, response.getStatus());
    }

    @Test
    void shouldRejectMissingBeneficiaryById() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.findById(99L));

        assertEquals(BusinessError.BENEFICIARY_NOT_FOUND, exception.getError());
    }

    @Test
    void shouldDeactivateBeneficiary() {
        Beneficiary active = beneficiary(1L, BeneficiaryStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivate(1L);

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(repository).save(captor.capture());
        assertEquals(BeneficiaryStatus.INACTIVE, captor.getValue().getStatus());
    }

    @Test
    void shouldRejectDeactivationWhenBeneficiaryDoesNotExist() {
        when(repository.findById(77L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deactivate(77L));

        assertEquals(BusinessError.BENEFICIARY_NOT_FOUND, exception.getError());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectSecondDeactivation() {
        Beneficiary inactive = beneficiary(1L, BeneficiaryStatus.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(inactive));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deactivate(1L));

        assertEquals(BusinessError.BENEFICIARY_ALREADY_INACTIVE, exception.getError());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldGenerateBankCodeFromFirstThreeClabeDigits() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(false);
        when(repository.countActiveByCustomerId(request.getCustomerId())).thenReturn(0L);
        when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        var response = service.create(request);

        assertEquals(request.getClabe().substring(0, 3), response.getBankCode());
    }

    @Test
    void shouldCreateWithActiveInitialStatus() {
        CreateBeneficiaryRequest request = validRequest();
        when(repository.existsByCustomerIdAndClabe(request.getCustomerId(), request.getClabe())).thenReturn(false);
        when(repository.countActiveByCustomerId(request.getCustomerId())).thenReturn(0L);
        when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 1L));

        var response = service.create(request);

        assertEquals(BeneficiaryStatus.ACTIVE, response.getStatus());
        assertTrue(response.getCreatedAt().isBefore(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1)));
    }

    private CreateBeneficiaryRequest validRequest() {
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest();
        request.setCustomerId("12345678");
        request.setAlias("Proveedor");
        request.setHolderName("Proveedor de Prueba SA de CV");
        request.setClabe("032180000118359719");
        request.setMaxAmount(new BigDecimal("50000.00"));
        request.setEmail("pagos@example.com");
        return request;
    }

    private Beneficiary beneficiary(Long id, BeneficiaryStatus status) {
        return new Beneficiary(
                id,
                "12345678",
                "Proveedor",
                "Proveedor de Prueba SA de CV",
                "032180000118359719",
                "032",
                new BigDecimal("50000.00"),
                "pagos@example.com",
                status,
                LocalDateTime.of(2026, 11, 20, 10, 30));
    }

    private Beneficiary withId(Beneficiary beneficiary, Long id) {
        return new Beneficiary(
                id,
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
}
