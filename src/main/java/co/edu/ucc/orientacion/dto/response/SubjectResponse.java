package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Asignatura;
import co.edu.ucc.orientacion.models.ClasePersonal;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Vista de una clase del horario con los días como lista. Puede ser una asignatura institucional o
 * una clase que el propio estudiante registró (origen PERSONAL).
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
 * @param origen INSTITUCIONAL para asignaturas de la universidad, PERSONAL para clases que
 *               registró el estudiante
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
        UUID espacioId,
        String origen) {

    /** Origen de las asignaturas que gestiona la universidad. */
    public static final String INSTITUCIONAL = "INSTITUCIONAL";

    /** Origen de las clases que registra el propio estudiante. */
    public static final String PERSONAL = "PERSONAL";

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
                espacioId,
                INSTITUCIONAL);
    }

    /**
     * Construye la vista de una clase registrada por el estudiante.
     *
     * @author Diego Luna
     * @param clase clase personal
     * @param espacioId espacio del mapa que corresponde al aula, o null
     * @return DTO con origen PERSONAL, sin código ni periodo
     */
    public static SubjectResponse fromPersonal(ClasePersonal clase, UUID espacioId) {
        return new SubjectResponse(
                clase.id(),
                clase.nombre(),
                null,
                clase.docente(),
                clase.aula(),
                splitDays(clase.dias()),
                clase.horaInicio(),
                clase.horaFin(),
                null,
                true,
                espacioId,
                PERSONAL);
    }

    private static List<String> splitDays(String dias) {
        return dias == null || dias.isBlank() ? List.of() : Arrays.asList(dias.split(","));
    }
}
