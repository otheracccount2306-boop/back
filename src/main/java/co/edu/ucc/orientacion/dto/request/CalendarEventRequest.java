package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para crear o actualizar un evento del calendario académico.
 *
 * @author Diego Luna
 * @param nombre nombre del evento
 * @param descripcion descripción del evento
 * @param categoria categoría del evento
 * @param fechaInicio fecha de inicio
 * @param fechaFin fecha de finalización opcional
 */
public record CalendarEventRequest(
        @NotBlank @Size(max = 200) String nombre,
        String descripcion,
        @NotBlank @Size(max = 50) String categoria,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin) {
}
