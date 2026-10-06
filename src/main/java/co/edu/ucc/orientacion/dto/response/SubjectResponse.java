package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Asignatura;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Vista de una asignatura con los días de clase como lista.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param nombre nombre de la asignatura
 * @param codigo código de la asignatura
 * @param docente nombre del docente
 * @param aula aula asignada
 * @param dias días de clase
 * @param horaInicio hora de inicio de la clase
 * @param horaFin hora de finalización de la clase
 * @param periodoAcademico periodo académico
 * @param activo indica si la asignatura está vigente
 * @param espacioId espacio del mapa del campus que corresponde al aula, o null si el aula no
 *                  coincide con ningún espacio ubicado en el mapa
 */
public record SubjectResponse(
        UUID id,
        String nombre,
        String codigo,
        String docente,
        String aula,
        List<String> dias,
        LocalTime horaInicio,
        LocalTime horaFin,
        String periodoAcademico,
        boolean activo,
        UUID espacioId) {

    /**
     * Construye la vista a partir de la entidad de asignatura, sin espacio en el mapa.
     *
     * @author Diego Luna
     * @param asignatura entidad de asignatura
     * @return DTO con los días separados en una lista
     */
    public static SubjectResponse from(Asignatura asignatura) {
        return from(asignatura, null);
    }

    /**
     * Construye la vista a partir de la entidad de asignatura y el espacio de su aula.
     *
     * @author Diego Luna
     * @param asignatura entidad de asignatura
     * @param espacioId espacio del mapa que corresponde al aula, o null
     * @return DTO con los días separados en una lista
     */
    public static SubjectResponse from(Asignatura asignatura, UUID espacioId) {
        List<String> dias = asignatura.dias() == null || asignatura.dias().isBlank()
                ? List.of()
                : Arrays.asList(asignatura.dias().split(","));
        return new SubjectResponse(
                asignatura.id(),
                asignatura.nombre(),
                asignatura.codigo(),
                asignatura.docente(),
                asignatura.aula(),
                dias,
                asignatura.horaInicio(),
                asignatura.horaFin(),
                asignatura.periodoAcademico(),
                asignatura.activo(),
                espacioId);
    }
}
