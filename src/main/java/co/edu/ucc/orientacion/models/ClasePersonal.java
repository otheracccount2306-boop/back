package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record ClasePersonal(
        UUID id,
        UUID usuarioId,
        String nombre,
        String docente,
        String aula,
        String dias,
        LocalTime horaInicio,
        LocalTime horaFin,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
