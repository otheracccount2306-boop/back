package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.GeometryRequest;
import co.edu.ucc.orientacion.dto.request.PlanRequest;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.UnprocessableEntityException;
import co.edu.ucc.orientacion.models.Espacio;
import co.edu.ucc.orientacion.models.Plano;
import co.edu.ucc.orientacion.repositories.PlanRepository;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusMapServiceTest {

    private final ObjectMapper json = new ObjectMapper();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SpaceRepository spaceRepository;

    private CampusMapService service;

    @BeforeEach
    void setUp() {
        service = new CampusMapService(planRepository, spaceRepository, json);
    }

    private JsonNode parse(String text) throws IOException {
        return json.readTree(text);
    }

    private static String pngDataUrl(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static Plano plano(UUID id, int ancho, int alto) {
        return new Plano(id, "Bloque C · Piso 1", "Bloque C", "1", "data:image/png;base64,AA==", ancho, alto, true, null, null, null);
    }

    @Test
    @DisplayName("Un polígono válido conserva solo type y coordinates y se redondea")
    void normalizesValidPolygon() throws IOException {
        ObjectNode result = service.normalizePolygon(parse("""
                {"type":"Polygon","coordinates":[[[10.123,20],[110,20],[110,80],[10.123,20]]],"extra":true}
                """), 1000, 800);

        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[10.12,20.0],[110.0,20.0],[110.0,80.0],[10.12,20.0]]]}",
                result.toString());
    }

    @Test
    @DisplayName("Acepta un Feature de Leaflet.draw y cierra el anillo abierto")
    void acceptsFeatureAndClosesRing() throws IOException {
        ObjectNode result = service.normalizePolygon(parse("""
                {"type":"Feature","properties":{},"geometry":{"type":"Polygon","coordinates":[[[0,0],[50,0],[50,50]]]}}
                """), 100, 100);

        JsonNode ring = result.get("coordinates").get(0);
        assertEquals(4, ring.size());
        assertEquals(ring.get(0), ring.get(3));
    }

    @Test
    @DisplayName("Rechaza tipos distintos de Polygon")
    void rejectsOtherTypes() throws IOException {
        JsonNode point = parse("{\"type\":\"Point\",\"coordinates\":[1,2]}");

        assertThrows(UnprocessableEntityException.class, () -> service.normalizePolygon(point, 100, 100));
    }

    @Test
    @DisplayName("Rechaza polígonos que se salen de la imagen")
    void rejectsOutOfBounds() throws IOException {
        JsonNode outside = parse("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[500,0],[500,50],[0,0]]]}");

        assertThrows(UnprocessableEntityException.class, () -> service.normalizePolygon(outside, 100, 100));
    }

    @Test
    @DisplayName("Ajusta al borde los vértices que caen dentro de la tolerancia")
    void clampsBorderVertices() throws IOException {
        ObjectNode result = service.normalizePolygon(
                parse("{\"type\":\"Polygon\",\"coordinates\":[[[-1,0],[101,0],[101,50],[-1,0]]]}"), 100, 100);

        assertEquals(0.0, result.get("coordinates").get(0).get(0).get(0).asDouble());
        assertEquals(100.0, result.get("coordinates").get(0).get(1).get(0).asDouble());
    }

    @Test
    @DisplayName("Rechaza polígonos con menos de tres vértices")
    void rejectsDegeneratePolygon() throws IOException {
        JsonNode line = parse("{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[10,10]]]}");

        assertThrows(UnprocessableEntityException.class, () -> service.normalizePolygon(line, 100, 100));
    }

    @Test
    @DisplayName("Lee las dimensiones reales de una imagen PNG")
    void readsImageSize() throws IOException {
        assertArrayEquals(new int[] {320, 200}, service.readImageSize(pngDataUrl(320, 200), 1, 1));
    }

    @Test
    @DisplayName("Rechaza imágenes que no son data URL de PNG, JPEG o WebP")
    void rejectsInvalidImage() {
        assertThrows(BadRequestException.class, () -> service.readImageSize("https://ejemplo.com/plano.png", 10, 10));
        assertThrows(BadRequestException.class, () -> service.readImageSize("data:image/gif;base64,AAAA", 10, 10));
    }

    @Test
    @DisplayName("Crear un plano sin imagen responde 400")
    void createWithoutImageFails() {
        PlanRequest request = new PlanRequest("Piso 1", null, null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> service.createPlan(request));
        verify(planRepository, never()).create(any());
    }

    @Test
    @DisplayName("No se puede cambiar el tamaño de la imagen si ya hay espacios dibujados")
    void rejectsResizedImageWithShapes() throws IOException {
        UUID id = UUID.randomUUID();
        when(planRepository.findById(id)).thenReturn(Optional.of(plano(id, 1000, 800)));
        when(planRepository.countShapes(id)).thenReturn(3);
        PlanRequest request = new PlanRequest("Piso 1", null, null, pngDataUrl(500, 400), null, null, null);

        assertThrows(ConflictException.class, () -> service.updatePlan(id, request));
        verify(planRepository, never()).update(any());
    }

    @Test
    @DisplayName("Guardar un polígono valida contra el plano y marca el plano como modificado")
    void setGeometrySavesNormalizedPolygon() throws IOException {
        UUID spaceId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        Espacio espacio = new Espacio(spaceId, "Lab", "LAB-101", "LABORATORIO", null, null, null, null, null, null, true, null);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(espacio));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plano(planId, 1000, 800)));
        when(spaceRepository.updateGeometry(eq(spaceId), eq(planId), anyString())).thenReturn(Optional.of(espacio));

        service.setSpaceGeometry(spaceId, new GeometryRequest(planId,
                parse("{\"type\":\"Polygon\",\"coordinates\":[[[1,1],[9,1],[9,9],[1,1]]]}")));

        ArgumentCaptor<String> saved = ArgumentCaptor.forClass(String.class);
        verify(spaceRepository).updateGeometry(eq(spaceId), eq(planId), saved.capture());
        assertEquals("{\"type\":\"Polygon\",\"coordinates\":[[[1.0,1.0],[9.0,1.0],[9.0,9.0],[1.0,1.0]]]}", saved.getValue());
        verify(planRepository).touch(planId);
    }

    @Test
    @DisplayName("Editar los datos de un plano conserva su malla de caminos")
    void updateKeepsNavigation() throws IOException {
        UUID id = UUID.randomUUID();
        JsonNode nav = parse("{\"malla\":\"1,2\",\"ancho\":3,\"alto\":1}");
        Plano actual = new Plano(id, "Campus", null, null, "data:image/png;base64,AA==", 100, 50, true, nav, null, null);
        when(planRepository.findById(id)).thenReturn(Optional.of(actual));
        when(planRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(spaceRepository.findShapesByPlan(id, false)).thenReturn(java.util.List.of());

        var detalle = service.updatePlan(id, new PlanRequest("Campus UCC", null, null, null, null, null, null));

        ArgumentCaptor<Plano> guardado = ArgumentCaptor.forClass(Plano.class);
        verify(planRepository).update(guardado.capture());
        assertEquals(nav, guardado.getValue().navegacion());
        assertEquals(nav, detalle.navegacion());
        verify(planRepository, never()).countShapes(any());
    }

    @Test
    @DisplayName("Con fondo vectorial el estudiante recibe el plano sin la imagen; el admin la conserva")
    void studentPlanOmitsImageWithVectorBase() throws IOException {
        UUID id = UUID.randomUUID();
        JsonNode nav = parse("{\"malla\":\"1\",\"ancho\":1,\"alto\":1,\"base\":{\"perimetro\":[],\"edificios\":[]}}");
        Plano campus = new Plano(id, "Campus", null, null, "data:image/png;base64,AA==", 100, 50, true, nav, null, null);
        when(planRepository.findActiveById(id)).thenReturn(Optional.of(campus));
        when(planRepository.findById(id)).thenReturn(Optional.of(campus));

        assertNull(service.getPlan(id).imagen());
        assertNotNull(service.getPlanForAdmin(id).imagen());
    }

    @Test
    @DisplayName("Sin fondo vectorial el estudiante recibe la imagen del plano")
    void studentPlanKeepsImageWithoutBase() {
        UUID id = UUID.randomUUID();
        when(planRepository.findActiveById(id)).thenReturn(Optional.of(plano(id, 100, 50)));

        assertNotNull(service.getPlan(id).imagen());
    }

    @Test
    @DisplayName("Eliminar definitivamente exige el nombre del plano y no toca nada si no coincide")
    void purgeRequiresName() {
        UUID id = UUID.randomUUID();
        when(planRepository.findById(id)).thenReturn(Optional.of(plano(id, 100, 50)));

        assertThrows(BadRequestException.class, () -> service.purgePlan(id, null));
        assertThrows(BadRequestException.class, () -> service.purgePlan(id, "Bloque C"));
        verify(spaceRepository, never()).clearGeometryByPlan(any());
        verify(planRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Eliminar definitivamente quita los espacios del mapa y borra el plano")
    void purgeClearsShapesAndDeletes() {
        UUID id = UUID.randomUUID();
        when(planRepository.findById(id)).thenReturn(Optional.of(plano(id, 100, 50)));
        when(spaceRepository.clearGeometryByPlan(id)).thenReturn(4);
        when(planRepository.delete(id)).thenReturn(1);

        assertEquals(4, service.purgePlan(id, "  bloque c · piso 1 "));
        verify(spaceRepository).clearGeometryByPlan(id);
        verify(planRepository).delete(id);
    }
}
