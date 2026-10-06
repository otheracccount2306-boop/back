package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Noticia(
        UUID id,
        String titulo,
        String resumen,
        String contenido,
        String categoria,
        String imagenUrl,
        String estado,
        LocalDateTime publicadoEn,
        UUID creadoPor,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
