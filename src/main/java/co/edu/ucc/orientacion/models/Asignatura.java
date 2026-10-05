package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Asignatura dictada en un periodo académico con su horario y aula.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param nombre nombre de la asignatura
 * @param codigo código único de la asignatura
 * @param docente nombre del docente
 * @param aula aula asignada
 * @param dias días de clase separados por coma, por ejemplo LUNES,MIERCOLES
 * @param horaInicio hora de inicio de la clase
 * @param horaFin hora de finalización de la clase
 * @param periodoAcademico periodo académico, por ejemplo 2026-1
 * @param activo indica si la asignatura está vigente
 * @param creadoEn fecha de creación
 */
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
