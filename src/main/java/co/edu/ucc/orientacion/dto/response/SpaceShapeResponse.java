package co.edu.ucc.orientacion.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record SpaceShapeResponse(
        UUID id,
        String nombre,
        String codigo,
        String categoria,
        String edificio,
        String piso,
        boolean activo,
        JsonNode geometria) {
}
