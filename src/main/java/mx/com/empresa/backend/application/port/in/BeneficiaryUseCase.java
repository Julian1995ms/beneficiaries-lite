package mx.com.empresa.backend.application.port.in;

import java.util.List;

import mx.com.empresa.backend.application.dto.request.CreateBeneficiaryRequest;
import mx.com.empresa.backend.application.dto.response.BeneficiaryResponse;

public interface BeneficiaryUseCase {

    BeneficiaryResponse create(CreateBeneficiaryRequest request);

    List<BeneficiaryResponse> findActiveByCustomerId(String customerId);

    BeneficiaryResponse findById(Long id);

    void deactivate(Long id);
}
