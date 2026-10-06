package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Plano;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Plano completo con su imagen y los polígonos de sus espacios. Es la unidad que la app guarda
 * para mostrar el mapa sin conexión.
 *
 * @author Diego Luna
 * @param id identificador del plano
 * @param nombre nombre del plano
 * @param edificio edificio que representa
 * @param piso piso que representa
 * @param imagen imagen como data URL; null en la vista del estudiante cuando hay fondo vectorial
 * @param ancho ancho de la imagen en píxeles
 * @param alto alto de la imagen en píxeles
 * @param activo si el plano es visible para los estudiantes
 * @param actualizadoEn fecha de la última modificación
 * @param espacios espacios dibujados sobre el plano
 * @param navegacion malla caminable y entradas para calcular caminos, o null
 */
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

    /**
     * Combina un plano con sus espacios dibujados.
     *
     * @author Diego Luna
     * @param plano plano con imagen
     * @param espacios espacios dibujados sobre el plano
     * @return detalle del plano
     */
    public static PlanDetailResponse of(Plano plano, List<SpaceShapeResponse> espacios) {
        return new PlanDetailResponse(plano.id(), plano.nombre(), plano.edificio(), plano.piso(), plano.imagen(),
                plano.ancho(), plano.alto(), plano.activo(), plano.actualizadoEn(), espacios, plano.navegacion());
    }

    /**
     * Copia del detalle sin la imagen del plano, para los planos que traen fondo vectorial.
     *
     * @author Diego Luna
     * @return detalle con imagen null
     */
    public PlanDetailResponse withoutImage() {
        return new PlanDetailResponse(id, nombre, edificio, piso, null, ancho, alto, activo, actualizadoEn, espacios,
                navegacion);
    }
}
