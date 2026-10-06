package co.edu.ucc.orientacion.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record Espacio(
        UUID id,
        String nombre,
        String codigo,
        String categoria,
        String edificio,
        String piso,
        String descripcion,
        String referencia,
        UUID planoId,
        JsonNode geometria,
        boolean activo,
        LocalDateTime creadoEn) {
}
