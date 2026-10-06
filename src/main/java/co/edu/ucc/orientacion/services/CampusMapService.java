package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.GeometryRequest;
import co.edu.ucc.orientacion.dto.request.PlanRequest;
import co.edu.ucc.orientacion.dto.response.PlanDetailResponse;
import co.edu.ucc.orientacion.dto.response.PlanSummaryResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.exceptions.UnprocessableEntityException;
import co.edu.ucc.orientacion.models.Espacio;
import co.edu.ucc.orientacion.models.Plano;
import co.edu.ucc.orientacion.repositories.PlanRepository;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lógica del mapa del campus: planos estáticos y polígonos de los espacios en coordenadas de
 * píxel (Leaflet L.CRS.Simple, x desde la izquierda, y desde abajo).
 *
 * @author Diego Luna
 */
@Service
public class CampusMapService {

    /** Máximo de vértices por polígono; evita trazos accidentales enormes. */
    public static final int MAX_VERTICES = 500;

    /** Tolerancia en píxeles para vértices que caen justo en el borde de la imagen. */
    private static final double BORDER_TOLERANCE = 2.0;

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/(png|jpeg|webp);base64,([A-Za-z0-9+/=]+)$");

    private final PlanRepository planRepository;
    private final SpaceRepository spaceRepository;
    private final ObjectMapper objectMapper;

    /**
     * Crea el servicio con los repositorios de planos y espacios.
     *
     * @author Diego Luna
     * @param planRepository repositorio de planos
     * @param spaceRepository repositorio de espacios
     * @param objectMapper serializador JSON de Spring
     */
    public CampusMapService(PlanRepository planRepository, SpaceRepository spaceRepository, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.spaceRepository = spaceRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Lista los planos visibles para los estudiantes, sin imagen.
     *
     * @author Diego Luna
     * @return planos activos
     */
    public List<PlanSummaryResponse> getPlans() {
        return planRepository.findSummaries(true);
    }

    /**
     * Devuelve un plano activo con los polígonos de sus espacios activos. Si el plano trae fondo
     * vectorial (navegacion.base), la app no dibuja el plano arquitectónico y la imagen no se envía.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return plano completo para el mapa
     * @throws NotFoundException cuando el plano no existe o está inactivo
     */
    public PlanDetailResponse getPlan(UUID id) {
        Plano plano = planRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Plano no encontrado"));
        PlanDetailResponse detail = PlanDetailResponse.of(plano, spaceRepository.findShapesByPlan(id, true));
        boolean vectorBase = plano.navegacion() != null && plano.navegacion().hasNonNull("base");
        return vectorBase ? detail.withoutImage() : detail;
    }

    /**
     * Lista todos los planos, activos e inactivos, para la administración.
     *
     * @author Diego Luna
     * @return planos sin imagen
     */
    public List<PlanSummaryResponse> listAllPlans() {
        return planRepository.findSummaries(false);
    }

    /**
     * Devuelve un plano, esté activo o no, con todos sus espacios dibujados.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return plano completo para el editor
     * @throws NotFoundException cuando el plano no existe
     */
    public PlanDetailResponse getPlanForAdmin(UUID id) {
        Plano plano = findPlan(id);
        return PlanDetailResponse.of(plano, spaceRepository.findShapesByPlan(id, false));
    }

    /**
     * Crea un plano. La imagen es obligatoria; sus dimensiones se leen de la propia imagen.
     *
     * @author Diego Luna
     * @param request datos del plano
     * @return plano creado con sus espacios (ninguno todavía)
     * @throws BadRequestException cuando falta la imagen o no es válida
     */
    @Transactional
    public PlanDetailResponse createPlan(PlanRequest request) {
        if (request.imagen() == null || request.imagen().isBlank()) {
            throw new BadRequestException("La imagen del plano es obligatoria");
        }
        int[] size = readImageSize(request.imagen(), request.ancho(), request.alto());
        Plano created = planRepository.create(new Plano(null, request.nombre().trim(), blankToNull(request.edificio()),
                blankToNull(request.piso()), request.imagen(), size[0], size[1],
                request.activo() == null || request.activo(), null, null, null));
        return PlanDetailResponse.of(created, List.of());
    }

    /**
     * Actualiza los datos de un plano. Si llega una imagen nueva con dimensiones distintas y ya hay
     * espacios dibujados, se rechaza: los polígonos quedarían desplazados.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @param request nuevos datos; imagen null conserva la actual
     * @return plano actualizado con sus espacios
     * @throws NotFoundException cuando el plano no existe
     * @throws ConflictException cuando la nueva imagen cambia de tamaño y hay espacios dibujados
     */
    @Transactional
    public PlanDetailResponse updatePlan(UUID id, PlanRequest request) {
        Plano current = findPlan(id);
        String imagen = current.imagen();
        int ancho = current.ancho();
        int alto = current.alto();
        if (request.imagen() != null && !request.imagen().isBlank()) {
            int[] size = readImageSize(request.imagen(), request.ancho(), request.alto());
            int dibujados = planRepository.countShapes(id);
            if ((size[0] != ancho || size[1] != alto) && dibujados > 0) {
                throw new ConflictException("El plano tiene " + dibujados + " espacios dibujados. La nueva imagen debe medir "
                        + ancho + " × " + alto + " px para no desplazarlos");
            }
            imagen = request.imagen();
            ancho = size[0];
            alto = size[1];
        }
        Plano updated = planRepository.update(new Plano(id, request.nombre().trim(), blankToNull(request.edificio()),
                blankToNull(request.piso()), imagen, ancho, alto,
                request.activo() == null ? current.activo() : request.activo(), current.navegacion(), null, null));
        return PlanDetailResponse.of(updated, spaceRepository.findShapesByPlan(id, false));
    }

    /**
     * Desactiva lógicamente un plano. Los polígonos se conservan por si se vuelve a activar.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @throws NotFoundException cuando el plano no existe o ya está inactivo
     */
    public void deletePlan(UUID id) {
        if (planRepository.deactivate(id) == 0) {
            throw new NotFoundException("Plano no encontrado");
        }
    }

    /**
     * Elimina un plano de forma definitiva, con su imagen y su malla de caminos. Los espacios que
     * estaban dibujados sobre él siguen en el catálogo, pero quedan sin ubicar en el mapa. Como no
     * se puede deshacer, exige escribir el nombre del plano como confirmación (sin importar
     * mayúsculas ni espacios en los extremos).
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @param confirmacion nombre del plano escrito por el administrador
     * @return número de espacios que quedaron sin ubicar
     * @throws NotFoundException cuando el plano no existe
     * @throws BadRequestException cuando la confirmación no coincide con el nombre del plano
     */
    @Transactional
    public int purgePlan(UUID id, String confirmacion) {
        Plano plano = findPlan(id);
        if (confirmacion == null || !confirmacion.trim().equalsIgnoreCase(plano.nombre().trim())) {
            throw new BadRequestException("Para eliminar el plano escribe su nombre exacto: " + plano.nombre());
        }
        int liberados = spaceRepository.clearGeometryByPlan(id);
        if (planRepository.delete(id) == 0) {
            throw new NotFoundException("Plano no encontrado");
        }
        return liberados;
    }

    /**
     * Guarda el polígono de un espacio sobre un plano. Valida que sea un Polygon de GeoJSON con
     * anillos cerrados dentro de la imagen y guarda solo type y coordinates.
     *
     * @author Diego Luna
     * @param spaceId identificador del espacio
     * @param request plano y geometría
     * @return espacio actualizado con su polígono
     * @throws NotFoundException cuando el espacio o el plano no existen
     * @throws UnprocessableEntityException cuando la geometría no es válida
     */
    @Transactional
    public Espacio setSpaceGeometry(UUID spaceId, GeometryRequest request) {
        Espacio before = spaceRepository.findById(spaceId).orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
        Plano plano = findPlan(request.planoId());
        ObjectNode geometry = normalizePolygon(request.geometria(), plano.ancho(), plano.alto());
        Espacio updated = spaceRepository.updateGeometry(spaceId, plano.id(), geometry.toString())
                .orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
        planRepository.touch(plano.id());
        if (before.planoId() != null && !before.planoId().equals(plano.id())) {
            planRepository.touch(before.planoId());
        }
        return updated;
    }

    /**
     * Quita el espacio del mapa: borra su polígono y su plano.
     *
     * @author Diego Luna
     * @param spaceId identificador del espacio
     * @return espacio actualizado, sin polígono
     * @throws NotFoundException cuando el espacio no existe
     */
    @Transactional
    public Espacio clearSpaceGeometry(UUID spaceId) {
        Espacio before = spaceRepository.findById(spaceId).orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
        Espacio updated = spaceRepository.updateGeometry(spaceId, null, null)
                .orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
        if (before.planoId() != null) {
            planRepository.touch(before.planoId());
        }
        return updated;
    }

    /**
     * Valida y limpia un polígono GeoJSON en coordenadas de píxel. Acepta un Geometry o un Feature.
     * Cierra el anillo si el último vértice no repite el primero y redondea a dos decimales.
     *
     * @author Diego Luna
     * @param input GeoJSON recibido
     * @param ancho ancho del plano en píxeles
     * @param alto alto del plano en píxeles
     * @return objeto {"type":"Polygon","coordinates":[...]} listo para guardar
     * @throws UnprocessableEntityException cuando la geometría no es un polígono válido dentro del plano
     */
    ObjectNode normalizePolygon(JsonNode input, int ancho, int alto) {
        JsonNode geometry = input;
        if (geometry != null && "Feature".equals(geometry.path("type").asText())) {
            geometry = geometry.get("geometry");
        }
        if (geometry == null || !geometry.isObject() || !"Polygon".equals(geometry.path("type").asText())) {
            throw new UnprocessableEntityException("La geometría debe ser un polígono GeoJSON (type: Polygon)");
        }
        JsonNode rings = geometry.get("coordinates");
        if (rings == null || !rings.isArray() || rings.isEmpty()) {
            throw new UnprocessableEntityException("El polígono no tiene coordenadas");
        }
        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", "Polygon");
        ArrayNode outRings = result.putArray("coordinates");
        int vertices = 0;
        for (JsonNode ring : rings) {
            if (!ring.isArray()) {
                throw new UnprocessableEntityException("Cada anillo del polígono debe ser una lista de posiciones");
            }
            ArrayNode outRing = objectMapper.createArrayNode();
            for (JsonNode position : ring) {
                if (!position.isArray() || position.size() < 2
                        || !position.get(0).isNumber() || !position.get(1).isNumber()) {
                    throw new UnprocessableEntityException("Cada vértice debe ser una posición [x, y] numérica");
                }
                double x = position.get(0).asDouble();
                double y = position.get(1).asDouble();
                if (!Double.isFinite(x) || !Double.isFinite(y)
                        || x < -BORDER_TOLERANCE || y < -BORDER_TOLERANCE
                        || x > ancho + BORDER_TOLERANCE || y > alto + BORDER_TOLERANCE) {
                    throw new UnprocessableEntityException("El polígono se sale del plano (" + ancho + " × " + alto + " px)");
                }
                ArrayNode outPosition = outRing.addArray();
                outPosition.add(round(clamp(x, ancho)));
                outPosition.add(round(clamp(y, alto)));
            }
            if (outRing.size() >= 3 && !outRing.get(0).equals(outRing.get(outRing.size() - 1))) {
                outRing.add(outRing.get(0).deepCopy());
            }
            if (outRing.size() < 4) {
                throw new UnprocessableEntityException("El polígono necesita al menos tres vértices distintos");
            }
            vertices += outRing.size();
            outRings.add(outRing);
        }
        if (vertices > MAX_VERTICES) {
            throw new UnprocessableEntityException("El polígono supera el máximo de " + MAX_VERTICES + " vértices");
        }
        return result;
    }

    /**
     * Lee el ancho y el alto reales de la imagen. Si el formato no se puede decodificar en el
     * servidor (por ejemplo WebP), usa los valores que envió el panel.
     *
     * @author Diego Luna
     * @param dataUrl imagen como data URL
     * @param fallbackAncho ancho informado por el cliente
     * @param fallbackAlto alto informado por el cliente
     * @return arreglo {ancho, alto}
     * @throws BadRequestException cuando el data URL no es válido o faltan las dimensiones
     */
    int[] readImageSize(String dataUrl, Integer fallbackAncho, Integer fallbackAlto) {
        Matcher matcher = DATA_URL.matcher(dataUrl);
        if (!matcher.matches()) {
            throw new BadRequestException("La imagen debe ser PNG, JPEG o WebP en formato data URL (base64)");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(matcher.group(2));
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image != null) {
                if (image.getWidth() > 8192 || image.getHeight() > 8192) {
                    throw new BadRequestException("La imagen no puede superar 8192 px por lado");
                }
                return new int[] {image.getWidth(), image.getHeight()};
            }
        } catch (IllegalArgumentException | IOException e) {
            throw new BadRequestException("La imagen del plano está dañada");
        }
        if (fallbackAncho == null || fallbackAlto == null) {
            throw new BadRequestException("Indica el ancho y el alto de la imagen en píxeles");
        }
        return new int[] {fallbackAncho, fallbackAlto};
    }

    private Plano findPlan(UUID id) {
        return planRepository.findById(id).orElseThrow(() -> new NotFoundException("Plano no encontrado"));
    }

    private static double clamp(double value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
