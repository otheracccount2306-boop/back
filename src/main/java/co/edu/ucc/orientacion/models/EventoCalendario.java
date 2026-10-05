package co.edu.ucc.orientacion.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento del calendario académico institucional.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param nombre nombre del evento
 * @param descripcion descripción del evento
 * @param categoria categoría del evento
 * @param fechaInicio fecha de inicio
 * @param fechaFin fecha de finalización
 * @param activo indica si el evento está vigente
 * @param creadoEn fecha de creación
 */
public record EventoCalendario(
        UUID id,
        String nombre,
        String descripcion,
        String categoria,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo,
        LocalDateTime creadoEn) {
}
