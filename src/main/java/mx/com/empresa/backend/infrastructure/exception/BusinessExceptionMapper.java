package mx.com.empresa.backend.infrastructure.exception;

import mx.com.empresa.backend.application.exception.BusinessError;
import mx.com.empresa.backend.application.exception.BusinessException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    @Override
    public Response toResponse(BusinessException exception) {
        int status = statusFor(exception.getError());
        ErrorResponse error = ErrorResponse.of("BEN-" + status, exception.getMessage());
        return Response.status(status).entity(error).build();
    }

    private int statusFor(BusinessError error) {
        return switch (error) {
            case INVALID_CLABE -> Response.Status.BAD_REQUEST.getStatusCode();
            case BENEFICIARY_NOT_FOUND -> Response.Status.NOT_FOUND.getStatusCode();
            case DUPLICATE_CLABE, BENEFICIARY_ALREADY_INACTIVE -> Response.Status.CONFLICT.getStatusCode();
            case ACTIVE_LIMIT_REACHED -> 422;
        };
    }
}
