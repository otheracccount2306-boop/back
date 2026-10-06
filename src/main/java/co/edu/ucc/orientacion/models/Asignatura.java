package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record Asignatura(
        UUID id,
        String nombre,
        String codigo,
        String docente,
        String aula,
        String dias,
        LocalTime horaInicio,
        LocalTime horaFin,
        String periodoAcademico,
        boolean activo,
        LocalDateTime creadoEn) {
}
