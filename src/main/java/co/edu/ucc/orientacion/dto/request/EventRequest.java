package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank @Size(max = 200) String nombre,
        String descripcion,
        @NotBlank @Size(max = 50) String categoria,
        @Size(max = 200) String lugar,
        @NotNull LocalDateTime fechaHora,
        @PositiveOrZero Integer cupos,
        @Pattern(regexp = "(?i)ACTIVO|CANCELADO", message = "debe ser ACTIVO o CANCELADO") String estado) {
}
