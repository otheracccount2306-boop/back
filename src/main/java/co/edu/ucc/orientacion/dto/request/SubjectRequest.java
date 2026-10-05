package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * Datos para crear o actualizar una asignatura.
 *
 * @author Diego Luna
 * @param nombre nombre de la asignatura
 * @param codigo código único de la asignatura
 * @param docente nombre del docente
 * @param aula aula asignada
 * @param dias días de clase, por ejemplo LUNES y MIERCOLES
 * @param horaInicio hora de inicio de la clase
 * @param horaFin hora de finalización de la clase
 * @param periodoAcademico periodo académico, por ejemplo 2026-1
 * @param activo si la asignatura está vigente; nulo conserva el estado actual
 */
public record SubjectRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 30) String codigo,
        @Size(max = 150) String docente,
        @Size(max = 50) String aula,
        @NotEmpty List<@NotBlank String> dias,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotBlank @Size(max = 20) String periodoAcademico,
        Boolean activo) {
}
