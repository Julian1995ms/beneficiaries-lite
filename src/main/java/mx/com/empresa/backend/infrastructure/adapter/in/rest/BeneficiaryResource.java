package mx.com.empresa.backend.infrastructure.adapter.in.rest;

import java.net.URI;
import java.util.List;

import mx.com.empresa.backend.application.dto.request.CreateBeneficiaryRequest;
import mx.com.empresa.backend.application.dto.response.BeneficiaryResponse;
import mx.com.empresa.backend.application.port.in.BeneficiaryUseCase;
import mx.com.empresa.backend.infrastructure.exception.ErrorResponse;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/beneficiaries")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Beneficiarios", description = "Administración de beneficiarios de transferencias bancarias")
public class BeneficiaryResource {

    private final BeneficiaryUseCase beneficiaryUseCase;

    @Inject
    public BeneficiaryResource(BeneficiaryUseCase beneficiaryUseCase) {
        this.beneficiaryUseCase = beneficiaryUseCase;
    }

    @POST
    @Operation(summary = "Registrar beneficiario", description = "Registra un beneficiario activo para un cliente")
    @APIResponse(responseCode = "201", description = "Beneficiario registrado",
            content = @Content(schema = @Schema(implementation = BeneficiaryResponse.class)))
    @APIResponse(responseCode = "400", description = "Datos inválidos o CLABE inválida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "CLABE duplicada",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Máximo de beneficiarios activos alcanzado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response create(@NotNull(message = "El cuerpo de la solicitud es obligatorio") @Valid CreateBeneficiaryRequest request, @Context UriInfo uriInfo) {
        BeneficiaryResponse created = beneficiaryUseCase.create(request);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.getId())).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    @Operation(summary = "Consultar beneficiarios activos", description = "Lista los beneficiarios activos de un cliente")
    @APIResponse(responseCode = "200", description = "Consulta exitosa",
            content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = BeneficiaryResponse.class)))
    @APIResponse(responseCode = "400", description = "customerId inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public List<BeneficiaryResponse> findActiveByCustomerId(
            @Parameter(description = "Identificador de cliente de 8 dígitos", required = true, example = "12345678")
            @QueryParam("customerId")
            @NotBlank(message = "El identificador del cliente es obligatorio")
            @Pattern(regexp = "[0-9]{8}", message = "El identificador del cliente debe contener 8 dígitos")
            String customerId) {
        return beneficiaryUseCase.findActiveByCustomerId(customerId);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consultar beneficiario por ID", description = "Consulta un beneficiario activo o inactivo por su ID")
    @APIResponse(responseCode = "200", description = "Beneficiario encontrado",
            content = @Content(schema = @Schema(implementation = BeneficiaryResponse.class)))
    @APIResponse(responseCode = "404", description = "Beneficiario inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public BeneficiaryResponse findById(
            @Parameter(description = "Identificador del beneficiario", required = true, example = "1")
            @PathParam("id") Long id) {
        return beneficiaryUseCase.findById(id);
    }

    @PATCH
    @Path("/{id}/deactivate")
    @Operation(summary = "Desactivar beneficiario", description = "Realiza la baja lógica de un beneficiario")
    @APIResponse(responseCode = "204", description = "Beneficiario desactivado")
    @APIResponse(responseCode = "404", description = "Beneficiario inexistente",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Beneficiario previamente desactivado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response deactivate(
            @Parameter(description = "Identificador del beneficiario", required = true, example = "1")
            @PathParam("id") Long id) {
        beneficiaryUseCase.deactivate(id);
        return Response.noContent().build();
    }
}
