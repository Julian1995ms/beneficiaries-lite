package mx.com.empresa.backend.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessError {

    INVALID_CLABE("CLABE inválida"),
    DUPLICATE_CLABE("La CLABE ya está registrada para este cliente"),
    ACTIVE_LIMIT_REACHED("El cliente alcanzó el máximo de 10 beneficiarios activos"),
    BENEFICIARY_NOT_FOUND("Beneficiario no encontrado"),
    BENEFICIARY_ALREADY_INACTIVE("El beneficiario ya está inactivo");

    private final String message;
}
