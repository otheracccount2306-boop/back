package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Clase que un estudiante registra para su propio horario. Solo la ve y la modifica su dueño.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param usuarioId estudiante dueño de la clase
 * @param nombre nombre de la clase
 * @param docente nombre del docente, o null
 * @param aula aula, por código o nombre del espacio, o null
 * @param dias días de clase separados por coma, por ejemplo LUNES,MIERCOLES
 * @param horaInicio hora de inicio
 * @param horaFin hora de finalización
 * @param creadoEn fecha de creación
 * @param actualizadoEn fecha de la última modificación
 */
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
