package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Plano;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PlanDetailResponse(
        UUID id,
        String nombre,
        String edificio,
        String piso,
        String imagen,
        int ancho,
        int alto,
        boolean activo,
        LocalDateTime actualizadoEn,
        List<SpaceShapeResponse> espacios,
        JsonNode navegacion) {

    public static PlanDetailResponse of(Plano plano, List<SpaceShapeResponse> espacios) {
        return new PlanDetailResponse(plano.id(), plano.nombre(), plano.edificio(), plano.piso(), plano.imagen(),
                plano.ancho(), plano.alto(), plano.activo(), plano.actualizadoEn(), espacios, plano.navegacion());
    }

    public PlanDetailResponse withoutImage() {
        return new PlanDetailResponse(id, nombre, edificio, piso, null, ancho, alto, activo, actualizadoEn, espacios,
                navegacion);
    }
}
