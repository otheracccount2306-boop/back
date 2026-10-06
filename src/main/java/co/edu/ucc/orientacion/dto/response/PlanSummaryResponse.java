package co.edu.ucc.orientacion.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanSummaryResponse(
        UUID id,
        String nombre,
        String edificio,
        String piso,
        int ancho,
        int alto,
        boolean activo,
        int espaciosDibujados,
        boolean navegacion,
        LocalDateTime actualizadoEn) {
}
