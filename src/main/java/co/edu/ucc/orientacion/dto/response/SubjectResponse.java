package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Asignatura;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

    public static SubjectResponse from(Asignatura asignatura) {
        return from(asignatura, null);
    }

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
