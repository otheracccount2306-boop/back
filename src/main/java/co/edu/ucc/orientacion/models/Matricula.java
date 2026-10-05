package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inscripción de un estudiante en una asignatura durante un periodo académico.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param usuarioId estudiante matriculado
 * @param asignaturaId asignatura en la que se matriculó
 * @param periodoAcademico periodo académico de la matrícula
 * @param creadoEn fecha de creación
 */
public record Matricula(
        UUID id,
        UUID usuarioId,
        UUID asignaturaId,
        String periodoAcademico,
        LocalDateTime creadoEn) {
}
