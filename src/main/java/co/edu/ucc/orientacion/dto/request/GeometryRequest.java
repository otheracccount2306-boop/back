package co.edu.ucc.orientacion.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Polígono de un espacio dibujado en el panel administrativo.
 *
 * @author Diego Luna
 * @param planoId plano sobre el que se dibujó
 * @param geometria GeoJSON Geometry de tipo Polygon, con posiciones [x, y] en píxeles del plano;
 *                  también se acepta un Feature, del que se toma su geometry
 */
public record GeometryRequest(
        @NotNull UUID planoId,
        @NotNull JsonNode geometria) {
}
