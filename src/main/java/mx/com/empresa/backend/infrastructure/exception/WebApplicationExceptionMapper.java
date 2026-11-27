package mx.com.empresa.backend.infrastructure.exception;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();

        ErrorResponse error = status == 400
                ? resolveBadRequest(exception)
                : ErrorResponse.of("BEN-" + status, "No fue posible procesar la solicitud");

        return Response.fromResponse(exception.getResponse())
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }

    private ErrorResponse resolveBadRequest(WebApplicationException exception) {
        Throwable cause = exception.getCause();

        while (cause != null) {
            if (cause instanceof UnrecognizedPropertyException unknownProperty) {
                return ErrorResponse.of(
                        "BEN-400",
                        "El cuerpo JSON contiene propiedades desconocidas",
                        List.of(new ErrorDetail(
                                unknownProperty.getPropertyName(),
                                "Propiedad no permitida")));
            }

            if (cause instanceof JsonProcessingException) {
                return ErrorResponse.of("BEN-400", "El cuerpo JSON es inválido");
            }

            cause = cause.getCause();
        }

        return ErrorResponse.of("BEN-400", "La solicitud no es válida");
    }
}