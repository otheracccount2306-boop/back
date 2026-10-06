package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.request.GeometryRequest;
import co.edu.ucc.orientacion.dto.request.PlanRequest;
import co.edu.ucc.orientacion.dto.request.SpaceRequest;
import co.edu.ucc.orientacion.dto.response.ApiResponse;
import co.edu.ucc.orientacion.services.CampusMapService;
import co.edu.ucc.orientacion.services.CampusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints del módulo de infraestructura del campus.
 *
 * @author Diego Luna
 */
@RestController
@RequestMapping("/api/v1")
public class CampusController {

    private final CampusService campusService;
    private final CampusMapService campusMapService;

    /**
     * Crea el controller con los servicios del campus y del mapa.
     *
     * @author Diego Luna
     * @param campusService servicio del campus
     * @param campusMapService servicio del mapa del campus
     */
    public CampusController(CampusService campusService, CampusMapService campusMapService) {
        this.campusService = campusService;
        this.campusMapService = campusMapService;
    }

    /**
     * Lista los espacios del campus, opcionalmente filtrados por categoría.
     *
     * @author Diego Luna
     * @param category categoría del espacio; opcional
     * @return ResponseEntity con HTTP 200 y los espacios
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la categoría no es válida
     */
    @GetMapping("/campus/spaces")
    public ResponseEntity<ApiResponse> getSpaces(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.getSpaces(category), "Espacios obtenidos correctamente"));
    }

    /**
     * Busca espacios por nombre o código.
     *
     * @author Diego Luna
     * @param q texto buscado, de al menos 2 caracteres; la búsqueda ignora tildes y mayúsculas
     * @return ResponseEntity con HTTP 200 y los espacios encontrados
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando el texto tiene menos de 2 caracteres
     */
    @GetMapping("/campus/spaces/search")
    public ResponseEntity<ApiResponse> searchSpaces(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.searchSpaces(q), "Búsqueda realizada correctamente"));
    }

    /**
     * Lista todos los espacios del campus, activos e inactivos, para la administración.
     *
     * @author Diego Luna
     * @param category categoría del espacio; opcional
     * @return ResponseEntity con HTTP 200 y los espacios de cualquier estado
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la categoría no es válida
     */
    @GetMapping("/admin/campus/spaces")
    public ResponseEntity<ApiResponse> listAllSpaces(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.listAllSpaces(category), "Espacios obtenidos correctamente"));
    }

    /**
     * Crea un espacio del campus.
     *
     * @author Diego Luna
     * @param request datos del espacio
     * @return ResponseEntity con HTTP 201 y el espacio creado
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando el código ya está en uso
     */
    @PostMapping("/admin/campus/spaces")
    public ResponseEntity<ApiResponse> createSpace(@Valid @RequestBody SpaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(campusService.createSpace(request), "Espacio creado correctamente"));
    }

    /**
     * Actualiza un espacio del campus.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @param request nuevos datos del espacio
     * @return ResponseEntity con HTTP 200 y el espacio actualizado
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el espacio no existe
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando el código ya está en uso
     */
    @PutMapping("/admin/campus/spaces/{id}")
    public ResponseEntity<ApiResponse> updateSpace(@PathVariable UUID id, @Valid @RequestBody SpaceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.updateSpace(id, request), "Espacio actualizado correctamente"));
    }

    /**
     * Elimina lógicamente un espacio del campus.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el espacio no existe
     */
    @DeleteMapping("/admin/campus/spaces/{id}")
    public ResponseEntity<ApiResponse> deleteSpace(@PathVariable UUID id) {
        campusService.deleteSpace(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Espacio eliminado correctamente"));
    }

    /**
     * Lista los planos del campus visibles para los estudiantes, sin la imagen.
     *
     * @author Diego Luna
     * @return ResponseEntity con HTTP 200 y los planos
     */
    @GetMapping("/campus/plans")
    public ResponseEntity<ApiResponse> getPlans() {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.getPlans(), "Planos obtenidos correctamente"));
    }

    /**
     * Devuelve un plano con su imagen y los polígonos de sus espacios, listo para el mapa.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return ResponseEntity con HTTP 200 y el plano completo
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el plano no existe o está inactivo
     */
    @GetMapping("/campus/plans/{id}")
    public ResponseEntity<ApiResponse> getPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.getPlan(id), "Plano obtenido correctamente"));
    }

    /**
     * Lista todos los planos, activos e inactivos, para la administración.
     *
     * @author Diego Luna
     * @return ResponseEntity con HTTP 200 y los planos
     */
    @GetMapping("/admin/campus/plans")
    public ResponseEntity<ApiResponse> listAllPlans() {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.listAllPlans(), "Planos obtenidos correctamente"));
    }

    /**
     * Devuelve un plano de cualquier estado con todos sus espacios dibujados, para el editor.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return ResponseEntity con HTTP 200 y el plano completo
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el plano no existe
     */
    @GetMapping("/admin/campus/plans/{id}")
    public ResponseEntity<ApiResponse> getPlanForAdmin(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.getPlanForAdmin(id), "Plano obtenido correctamente"));
    }

    /**
     * Crea un plano a partir de una imagen.
     *
     * @author Diego Luna
     * @param request datos del plano con la imagen como data URL
     * @return ResponseEntity con HTTP 201 y el plano creado
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la imagen no es válida
     */
    @PostMapping("/admin/campus/plans")
    public ResponseEntity<ApiResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(campusMapService.createPlan(request), "Plano creado correctamente"));
    }

    /**
     * Actualiza un plano; la imagen es opcional.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @param request nuevos datos del plano
     * @return ResponseEntity con HTTP 200 y el plano actualizado
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando la nueva imagen desplazaría los polígonos
     */
    @PutMapping("/admin/campus/plans/{id}")
    public ResponseEntity<ApiResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.updatePlan(id, request), "Plano actualizado correctamente"));
    }

    /**
     * Elimina lógicamente un plano.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el plano no existe
     */
    @DeleteMapping("/admin/campus/plans/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable UUID id) {
        campusMapService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plano eliminado correctamente"));
    }

    /**
     * Elimina un plano de forma definitiva. Los espacios dibujados sobre él quedan sin ubicar.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @param confirmacion nombre del plano, como segunda verificación
     * @return ResponseEntity con HTTP 200 y la cantidad de espacios que quedaron sin ubicar
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el plano no existe
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la confirmación no coincide
     */
    @DeleteMapping("/admin/campus/plans/{id}/permanent")
    public ResponseEntity<ApiResponse> purgePlan(
            @PathVariable UUID id,
            @RequestParam(required = false) String confirmacion) {
        int liberados = campusMapService.purgePlan(id, confirmacion);
        String detalle = liberados == 1 ? "1 espacio quedó sin ubicar en el mapa"
                : liberados + " espacios quedaron sin ubicar en el mapa";
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("espaciosSinUbicar", liberados),
                "Plano eliminado definitivamente. " + detalle));
    }

    /**
     * Guarda el polígono GeoJSON de un espacio dibujado en el panel.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @param request plano y geometría GeoJSON
     * @return ResponseEntity con HTTP 200 y el espacio actualizado
     * @throws co.edu.ucc.orientacion.exceptions.UnprocessableEntityException cuando la geometría no es válida
     */
    @PutMapping("/admin/campus/spaces/{id}/geometry")
    public ResponseEntity<ApiResponse> setSpaceGeometry(@PathVariable UUID id, @Valid @RequestBody GeometryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.setSpaceGeometry(id, request), "Ubicación guardada correctamente"));
    }

    /**
     * Quita el polígono de un espacio.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @return ResponseEntity con HTTP 200 y el espacio sin ubicación
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el espacio no existe
     */
    @DeleteMapping("/admin/campus/spaces/{id}/geometry")
    public ResponseEntity<ApiResponse> clearSpaceGeometry(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.clearSpaceGeometry(id), "Ubicación eliminada correctamente"));
    }
}
