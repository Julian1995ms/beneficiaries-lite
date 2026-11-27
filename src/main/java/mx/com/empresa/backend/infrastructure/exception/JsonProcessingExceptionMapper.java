package mx.com.empresa.backend.infrastructure.exception;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    @Override
    public Response toResponse(JsonProcessingException exception) {
        if (exception instanceof UnrecognizedPropertyException unknownProperty) {
            ErrorDetail detail = new ErrorDetail(unknownProperty.getPropertyName(), "Propiedad no permitida");
            ErrorResponse error = ErrorResponse.of(
                    "BEN-400",
                    "El cuerpo JSON contiene propiedades desconocidas",
                    List.of(detail));
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        ErrorResponse error = ErrorResponse.of("BEN-400", "El cuerpo JSON es inválido");
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
}
