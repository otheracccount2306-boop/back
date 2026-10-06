package co.edu.ucc.orientacion.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

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
