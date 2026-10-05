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

/**
 * Endpoints del módulo de servicios institucionales: bienestar, directorio y preguntas frecuentes.
 *
 * @author Gabriela Zabaleta
 */
@RestController
@RequestMapping("/api/v1")
public class ServicesController {

    private final ServicesService servicesService;

    /**
     * Crea el controller con el servicio de servicios institucionales.
     *
     * @author Gabriela Zabaleta
     * @param servicesService servicio de servicios institucionales
     */
    public ServicesController(ServicesService servicesService) {
        this.servicesService = servicesService;
    }

    /**
     * Lista los servicios de bienestar, opcionalmente filtrados por categoría.
     *
     * @author Gabriela Zabaleta
     * @param category PSICOLOGIA, SALUD, DEPORTE, CULTURA, PASTORAL o BECAS; opcional
     * @return ResponseEntity con HTTP 200 y los servicios de bienestar
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la categoría no es válida
     */
    @GetMapping("/services/wellbeing")
    public ResponseEntity<ApiResponse> getWellbeing(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(
                servicesService.getWellbeing(category), "Servicios de bienestar obtenidos correctamente"));
    }

    /**
     * Busca dependencias del directorio por nombre, sin distinguir mayúsculas.
     *
     * @author Gabriela Zabaleta
     * @param search texto contenido en el nombre; opcional; la búsqueda ignora tildes y mayúsculas
     * @return ResponseEntity con HTTP 200 y las dependencias encontradas
     */
    @GetMapping("/services/departments")
    public ResponseEntity<ApiResponse> getDepartments(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                servicesService.getDepartments(search), "Directorio obtenido correctamente"));
    }

    /**
     * Busca preguntas frecuentes por categoría y palabra clave.
     *
     * @author Gabriela Zabaleta
     * @param category categoría de la pregunta; opcional
     * @param search palabra clave; opcional; la búsqueda ignora tildes y mayúsculas
     * @return ResponseEntity con HTTP 200 y las preguntas encontradas
     */
    @GetMapping("/services/faq")
    public ResponseEntity<ApiResponse> getFaq(
            @RequestParam(required = false) String category, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.ok(
                servicesService.getFaq(category, search), "Preguntas frecuentes obtenidas correctamente"));
    }

    /**
     * Lista los servicios de bienestar, las dependencias o las preguntas frecuentes incluyendo
     * los inactivos, para la administración.
     *
     * @author Gabriela Zabaleta
     * @param type tipo de recurso: wellbeing, departments o faq
     * @param category categoría a filtrar; opcional
     * @return ResponseEntity con HTTP 200 y los recursos de cualquier estado
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando el tipo o la categoría son inválidos
     */
    @GetMapping("/admin/services/{type}")
    public ResponseEntity<ApiResponse> listAll(
            @PathVariable String type, @RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(servicesService.listAll(type, category), "Recursos obtenidos correctamente"));
    }

    /**
     * Crea un servicio de bienestar, una dependencia del directorio o una pregunta frecuente.
     *
     * @author Gabriela Zabaleta
     * @param type tipo de recurso: wellbeing, departments o faq
     * @param body cuerpo JSON con los datos del recurso
     * @return ResponseEntity con HTTP 201 y el recurso creado
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando el tipo o el cuerpo son inválidos
     */
    @PostMapping("/admin/services/{type}")
    public ResponseEntity<ApiResponse> create(@PathVariable String type, @RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(servicesService.create(type, body), "Recurso creado correctamente"));
    }

    /**
     * Actualiza un servicio de bienestar, una dependencia del directorio o una pregunta frecuente.
     *
     * @author Gabriela Zabaleta
     * @param type tipo de recurso: wellbeing, departments o faq
     * @param id identificador del recurso
     * @param body cuerpo JSON con los nuevos datos
     * @return ResponseEntity con HTTP 200 y el recurso actualizado
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el recurso no existe
     */
    @PutMapping("/admin/services/{type}/{id}")
    public ResponseEntity<ApiResponse> update(
            @PathVariable String type, @PathVariable UUID id, @RequestBody JsonNode body) {
        return ResponseEntity.ok(ApiResponse.ok(servicesService.update(type, id, body), "Recurso actualizado correctamente"));
    }

    /**
     * Elimina lógicamente un servicio de bienestar, una dependencia o una pregunta frecuente.
     *
     * @author Gabriela Zabaleta
     * @param type tipo de recurso: wellbeing, departments o faq
     * @param id identificador del recurso
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el recurso no existe
     */
    @DeleteMapping("/admin/services/{type}/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable String type, @PathVariable UUID id) {
        servicesService.delete(type, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Recurso eliminado correctamente"));
    }
}
