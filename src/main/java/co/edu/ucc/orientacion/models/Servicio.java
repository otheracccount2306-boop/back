package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Servicio(
        UUID id,
        String nombre,
        String descripcion,
        String categoria,
        String edificio,
        String horario,
        String contacto,
        boolean activo,
        LocalDateTime creadoEn) {
}
