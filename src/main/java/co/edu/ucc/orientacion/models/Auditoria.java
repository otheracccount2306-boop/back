package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de auditoría de acciones administrativas sensibles.
 *
 * @author Doris Arzuaga
 * @param id identificador único UUID
 * @param usuarioId usuario que ejecutó la acción
 * @param accion nombre de la acción ejecutada
 * @param entidad entidad afectada por la acción
 * @param entidadId identificador de la entidad afectada
 * @param detalle detalle de la acción serializado como JSON
 * @param ejecutadoEn fecha de ejecución
 */
public record Auditoria(
        UUID id,
        UUID usuarioId,
        String accion,
        String entidad,
        UUID entidadId,
        String detalle,
        LocalDateTime ejecutadoEn) {
}
