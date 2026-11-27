package mx.com.empresa.backend.application.dto.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateBeneficiaryRequest {

    @NotBlank(message = "El identificador del cliente es obligatorio")
    @Pattern(regexp = "[0-9]{8}", message = "El identificador del cliente debe contener 8 dígitos")
    private String customerId;

    @NotBlank(message = "El alias es obligatorio")
    @Size(min = 3, max = 30, message = "El alias debe contener entre 3 y 30 caracteres")
    private String alias;

    @NotBlank(message = "El nombre del titular es obligatorio")
    @Size(max = 100, message = "El nombre del titular no debe exceder 100 caracteres")
    private String holderName;

    @NotBlank(message = "La CLABE es obligatoria")
    @Pattern(regexp = "[0-9]{18}", message = "La CLABE debe contener exactamente 18 dígitos")
    private String clabe;

    @NotNull(message = "El monto máximo es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El monto máximo debe ser mayor a cero")
    @Digits(integer = 17, fraction = 2, message = "El monto máximo admite hasta 17 enteros y 2 decimales")
    private BigDecimal maxAmount;

    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 254, message = "El correo electrónico no debe exceder 254 caracteres")
    private String email;

    @JsonSetter("alias")
    public void setAlias(String alias) {
        this.alias = alias == null ? null : alias.trim();
    }

    @JsonSetter("holderName")
    public void setHolderName(String holderName) {
        this.holderName = holderName == null ? null : holderName.trim();
    }
}
