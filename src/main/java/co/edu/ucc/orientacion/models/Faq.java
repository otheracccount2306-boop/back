package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Faq(
        UUID id,
        String pregunta,
        String respuesta,
        String categoria,
        int frecuencia,
        boolean activo,
        LocalDateTime creadoEn) {
}
