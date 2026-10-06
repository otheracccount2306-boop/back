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

@Service
public class CampusMapService {

    public static final int MAX_VERTICES = 500;

    private static final double BORDER_TOLERANCE = 2.0;

    private static final Pattern DATA_URL =
            Pattern.compile("^data:image/(png|jpeg|webp);base64,([A-Za-z0-9+/=]+)$");

    private final PlanRepository planRepository;
    private final SpaceRepository spaceRepository;
    private final ObjectMapper objectMapper;

    public CampusMapService(PlanRepository planRepository, SpaceRepository spaceRepository, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.spaceRepository = spaceRepository;
        this.objectMapper = objectMapper;
    }

    public List<PlanSummaryResponse> getPlans() {
        return planRepository.findSummaries(true);
    }

    public PlanDetailResponse getPlan(UUID id) {
        Plano plano = planRepository.findActiveById(id).orElseThrow(() -> new NotFoundException("Plano no encontrado"));
        PlanDetailResponse detail = PlanDetailResponse.of(plano, spaceRepository.findShapesByPlan(id, true));
        boolean vectorBase = plano.navegacion() != null && plano.navegacion().hasNonNull("base");
        return vectorBase ? detail.withoutImage() : detail;
    }

    public List<PlanSummaryResponse> listAllPlans() {
        return planRepository.findSummaries(false);
    }

    public PlanDetailResponse getPlanForAdmin(UUID id) {
        Plano plano = findPlan(id);
        return PlanDetailResponse.of(plano, spaceRepository.findShapesByPlan(id, false));
    }

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

    public void deletePlan(UUID id) {
        if (planRepository.deactivate(id) == 0) {
            throw new NotFoundException("Plano no encontrado");
        }
    }

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
