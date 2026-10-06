package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CalendarEventRequest(
        @NotBlank @Size(max = 200) String nombre,
        String descripcion,
        @NotBlank @Size(max = 50) String categoria,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin) {
}
