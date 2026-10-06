package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.response.ApiResponse;
import co.edu.ucc.orientacion.services.ServicesService;
import com.fasterxml.jackson.databind.JsonNode;
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
public class ServicesController {

    private final ServicesService servicesService;

    public ServicesController(ServicesService servicesService) {
        this.servicesService = servicesService;
    }

    @GetMapping("/services/wellbeing")
    public ResponseEntity<ApiResponse> getWellbeing(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(
                servicesService.getWellbeing(category), "Servicios de bienestar obtenidos correctamente"));
    }

    @GetMapping("/services/departments")
    public ResponseEntity<ApiResponse> getDepartments(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                servicesService.getDepartments(search), "Directorio obtenido correctamente"));
    }

    @GetMapping("/services/faq")
    public ResponseEntity<ApiResponse> getFaq(
            @RequestParam(required = false) String category, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                servicesService.getFaq(category, search), "Preguntas frecuentes obtenidas correctamente"));
    }

    @GetMapping("/admin/services/{type}")
    public ResponseEntity<ApiResponse> listAll(
            @PathVariable String type, @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(servicesService.listAll(type, category), "Recursos obtenidos correctamente"));
    }

    @PostMapping("/admin/services/{type}")
    public ResponseEntity<ApiResponse> create(@PathVariable String type, @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(servicesService.create(type, body), "Recurso creado correctamente"));
    }

    @PutMapping("/admin/services/{type}/{id}")
    public ResponseEntity<ApiResponse> update(
            @PathVariable String type, @PathVariable UUID id, @RequestBody JsonNode body) {
        return ResponseEntity.ok(ApiResponse.ok(servicesService.update(type, id, body), "Recurso actualizado correctamente"));
    }

    @DeleteMapping("/admin/services/{type}/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable String type, @PathVariable UUID id) {
        servicesService.delete(type, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Recurso eliminado correctamente"));
    }
}
