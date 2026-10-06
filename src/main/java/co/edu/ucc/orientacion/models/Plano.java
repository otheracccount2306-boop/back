package co.edu.ucc.orientacion.models;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

public record Plano(
        UUID id,
        String nombre,
        String edificio,
        String piso,
        String imagen,
        int ancho,
        int alto,
        boolean activo,
        JsonNode navegacion,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
