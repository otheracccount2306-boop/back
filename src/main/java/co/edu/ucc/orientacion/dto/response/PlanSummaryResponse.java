package co.edu.ucc.orientacion.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Plano sin la imagen, para listados.
 *
 * @author Diego Luna
 * @param id identificador del plano
 * @param nombre nombre del plano
 * @param edificio edificio que representa
 * @param piso piso que representa
 * @param ancho ancho de la imagen en píxeles
 * @param alto alto de la imagen en píxeles
 * @param activo si el plano es visible para los estudiantes
 * @param espaciosDibujados cantidad de espacios con polígono en este plano
 * @param navegacion true si el plano trae malla de caminos (es el mapa del campus)
 * @param actualizadoEn fecha de la última modificación; la app la usa para saber si su copia sin
 *                      conexión está vigente
 */
public record PlanSummaryResponse(
        UUID id,
        String nombre,
        String edificio,
        String piso,
        int ancho,
        int alto,
        boolean activo,
        int espaciosDibujados,
        boolean navegacion,
        LocalDateTime actualizadoEn) {
}
