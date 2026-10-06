package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqRequest(
        @NotBlank @Size(max = 500) String pregunta,
        @NotBlank String respuesta,
        @NotBlank @Size(max = 50) String categoria,
        Boolean activo) {
}
