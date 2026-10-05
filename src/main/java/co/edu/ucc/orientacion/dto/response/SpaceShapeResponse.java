package co.edu.ucc.orientacion.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Espacio dibujado en un plano, con lo mínimo que necesita el mapa para pintarlo y buscarlo.
 *
 * @author Diego Luna
 * @param id identificador del espacio; es el UUID que la app envía al mapa para iluminarlo
 * @param nombre nombre del espacio
 * @param codigo código del espacio
 * @param categoria categoría del espacio
 * @param activo si el espacio es visible para los estudiantes
 * @param geometria polígono GeoJSON en píxeles del plano
 */
public record SpaceShapeResponse(
        UUID id,
        String nombre,
        String codigo,
        String categoria,
        boolean activo,
        JsonNode geometria) {
}
