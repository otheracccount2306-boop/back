package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Evento(
        UUID id,
        String nombre,
        String descripcion,
        String categoria,
        String lugar,
        LocalDateTime fechaHora,
        Integer cupos,
        String estado,
        UUID creadoPor,
        LocalDateTime creadoEn) {
}
