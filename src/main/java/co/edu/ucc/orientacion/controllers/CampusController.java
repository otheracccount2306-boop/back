package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.request.SpaceRequest;
import co.edu.ucc.orientacion.dto.response.ApiResponse;
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

    /**
     * Crea el controller con el servicio del campus.
     *
     * @author Diego Luna
     * @param campusService servicio del campus
     */
    public CampusController(CampusService campusService) {
        this.campusService = campusService;
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
}
