package co.edu.ucc.orientacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

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
