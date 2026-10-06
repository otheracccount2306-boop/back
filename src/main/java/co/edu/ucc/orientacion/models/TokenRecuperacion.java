package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record TokenRecuperacion(
        UUID id,
        UUID usuarioId,
        String tokenHash,
        LocalDateTime expiraEn,
        boolean usado,
        LocalDateTime creadoEn) {
}
