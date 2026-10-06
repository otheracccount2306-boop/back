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

@RestController
@RequestMapping("/api/v1")
public class CampusController {

    private final CampusService campusService;
    private final CampusMapService campusMapService;

    public CampusController(CampusService campusService, CampusMapService campusMapService) {
        this.campusService = campusService;
        this.campusMapService = campusMapService;
    }

    @GetMapping("/campus/spaces")
    public ResponseEntity<ApiResponse> getSpaces(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.getSpaces(category), "Espacios obtenidos correctamente"));
    }

    @GetMapping("/campus/spaces/search")
    public ResponseEntity<ApiResponse> searchSpaces(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.searchSpaces(q), "Búsqueda realizada correctamente"));
    }

    @GetMapping("/admin/campus/spaces")
    public ResponseEntity<ApiResponse> listAllSpaces(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.listAllSpaces(category), "Espacios obtenidos correctamente"));
    }

    @PostMapping("/admin/campus/spaces")
    public ResponseEntity<ApiResponse> createSpace(@Valid @RequestBody SpaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(campusService.createSpace(request), "Espacio creado correctamente"));
    }

    @PutMapping("/admin/campus/spaces/{id}")
    public ResponseEntity<ApiResponse> updateSpace(@PathVariable UUID id, @Valid @RequestBody SpaceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campusService.updateSpace(id, request), "Espacio actualizado correctamente"));
    }

    @DeleteMapping("/admin/campus/spaces/{id}")
    public ResponseEntity<ApiResponse> deleteSpace(@PathVariable UUID id) {
        campusService.deleteSpace(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Espacio eliminado correctamente"));
    }

    @GetMapping("/campus/plans")
    public ResponseEntity<ApiResponse> getPlans() {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.getPlans(), "Planos obtenidos correctamente"));
    }

    @GetMapping("/campus/plans/{id}")
    public ResponseEntity<ApiResponse> getPlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.getPlan(id), "Plano obtenido correctamente"));
    }

    @GetMapping("/admin/campus/plans")
    public ResponseEntity<ApiResponse> listAllPlans() {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.listAllPlans(), "Planos obtenidos correctamente"));
    }

    @GetMapping("/admin/campus/plans/{id}")
    public ResponseEntity<ApiResponse> getPlanForAdmin(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.getPlanForAdmin(id), "Plano obtenido correctamente"));
    }

    @PostMapping("/admin/campus/plans")
    public ResponseEntity<ApiResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(campusMapService.createPlan(request), "Plano creado correctamente"));
    }

    @PutMapping("/admin/campus/plans/{id}")
    public ResponseEntity<ApiResponse> updatePlan(@PathVariable UUID id, @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.updatePlan(id, request), "Plano actualizado correctamente"));
    }

    @DeleteMapping("/admin/campus/plans/{id}")
    public ResponseEntity<ApiResponse> deletePlan(@PathVariable UUID id) {
        campusMapService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plano eliminado correctamente"));
    }

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

    @PutMapping("/admin/campus/spaces/{id}/geometry")
    public ResponseEntity<ApiResponse> setSpaceGeometry(@PathVariable UUID id, @Valid @RequestBody GeometryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.setSpaceGeometry(id, request), "Ubicación guardada correctamente"));
    }

    @DeleteMapping("/admin/campus/spaces/{id}/geometry")
    public ResponseEntity<ApiResponse> clearSpaceGeometry(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(campusMapService.clearSpaceGeometry(id), "Ubicación eliminada correctamente"));
    }
}
