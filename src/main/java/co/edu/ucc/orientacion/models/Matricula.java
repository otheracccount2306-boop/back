package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Matricula(
        UUID id,
        UUID usuarioId,
        UUID asignaturaId,
        String periodoAcademico,
        LocalDateTime creadoEn) {
}
