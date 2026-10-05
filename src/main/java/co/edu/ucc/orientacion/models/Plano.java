package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Plano estático de un edificio o piso del campus. Los polígonos de los espacios se dibujan sobre
 * esta imagen en píxeles (Leaflet L.CRS.Simple), sin coordenadas GPS.
 *
 * @author Diego Luna
 * @param id identificador único UUID
 * @param nombre nombre del plano, por ejemplo "Bloque C · Piso 1"
 * @param edificio edificio que representa
 * @param piso piso que representa
 * @param imagen imagen como data URL (data:image/jpeg;base64,...)
 * @param ancho ancho de la imagen en píxeles
 * @param alto alto de la imagen en píxeles
 * @param activo indica si el plano es visible para los estudiantes
 * @param creadoEn fecha de creación
 * @param actualizadoEn fecha de la última modificación
 */
public record Plano(
        UUID id,
        String nombre,
        String edificio,
        String piso,
        String imagen,
        int ancho,
        int alto,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
