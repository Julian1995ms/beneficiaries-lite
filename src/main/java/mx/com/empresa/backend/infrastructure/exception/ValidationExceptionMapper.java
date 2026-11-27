package mx.com.empresa.backend.infrastructure.exception;

import java.util.Comparator;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            List<ErrorDetail> details = constraintViolationException.getConstraintViolations().stream()
                    .map(this::toDetail)
                    .sorted(Comparator.comparing(ErrorDetail::getField).thenComparing(ErrorDetail::getMessage))
                    .toList();
            ErrorResponse error = ErrorResponse.of("BEN-400", "La solicitud contiene datos inválidos", details);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        ErrorResponse error = ErrorResponse.of("BEN-400", "La solicitud contiene datos inválidos");
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    private ErrorDetail toDetail(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        String field = separator >= 0 ? path.substring(separator + 1) : path;
        return new ErrorDetail(field, violation.getMessage());
    }
}
