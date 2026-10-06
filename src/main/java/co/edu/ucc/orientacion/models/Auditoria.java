package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Auditoria(
        UUID id,
        UUID usuarioId,
        String accion,
        String entidad,
        UUID entidadId,
        String detalle,
        LocalDateTime ejecutadoEn) {
}
