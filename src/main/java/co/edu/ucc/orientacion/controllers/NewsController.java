package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.request.EventRequest;
import co.edu.ucc.orientacion.dto.request.NewsRequest;
import co.edu.ucc.orientacion.dto.response.ApiResponse;
import co.edu.ucc.orientacion.security.CurrentUser;
import co.edu.ucc.orientacion.services.NewsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoints del módulo de noticias y eventos.
 *
 * @author Gabriela Zabaleta
 */
@RestController
@RequestMapping("/api/v1")
public class NewsController {

    private final NewsService newsService;

    /**
     * Crea el controller con el servicio de noticias y eventos.
     *
     * @author Gabriela Zabaleta
     * @param newsService servicio de noticias y eventos
     */
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Lista noticias publicadas de forma paginada, 10 por página.
     *
     * @author Gabriela Zabaleta
     * @param category categoría de la noticia; opcional
     * @param page número de página, iniciando en 1
     * @return ResponseEntity con HTTP 200 y la página de noticias
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la página es menor que 1
     */
    @GetMapping("/news")
    public ResponseEntity<ApiResponse> listNews(
            @RequestParam(required = false) String category, @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.listNews(category, page), "Noticias obtenidas correctamente"));
    }

    /**
     * Obtiene el detalle de una noticia. Los administradores también pueden ver borradores.
     *
     * @author Gabriela Zabaleta
     * @param authentication autenticación de la solicitud
     * @param id identificador de la noticia
     * @return ResponseEntity con HTTP 200 y la noticia
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando la noticia no existe o no está publicada
     */
    @GetMapping("/news/{id}")
    public ResponseEntity<ApiResponse> getNews(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.getNews(id, CurrentUser.isAdmin(authentication)), "Noticia obtenida correctamente"));
    }

    /**
     * Lista eventos activos con fecha de hoy o futura, de forma paginada, 10 por página.
     *
     * @author Gabriela Zabaleta
     * @param category categoría del evento; opcional
     * @param from fecha mínima en formato ISO yyyy-MM-dd; opcional
     * @param to fecha máxima inclusiva en formato ISO yyyy-MM-dd; opcional
     * @param page número de página, iniciando en 1
     * @return ResponseEntity con HTTP 200 y la página de eventos
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la página o el rango de fechas son inválidos
     */
    @GetMapping("/events")
    public ResponseEntity<ApiResponse> listEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.listEvents(category, from, to, page), "Eventos obtenidos correctamente"));
    }

    /**
     * Lista noticias de cualquier estado, incluidos borradores y archivadas, de forma paginada.
     *
     * @author Gabriela Zabaleta
     * @param category categoría de la noticia; opcional
     * @param status BORRADOR, PUBLICADO o ARCHIVADO; opcional
     * @param page número de página, iniciando en 1
     * @return ResponseEntity con HTTP 200 y la página de noticias completas
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la página o el estado son inválidos
     */
    @GetMapping("/admin/news")
    public ResponseEntity<ApiResponse> listNewsAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.listNewsAdmin(category, status, page), "Noticias obtenidas correctamente"));
    }

    /**
     * Lista eventos de cualquier estado y fecha, incluidos concluidos y cancelados, de forma paginada.
     *
     * @author Gabriela Zabaleta
     * @param category categoría del evento; opcional
     * @param status ACTIVO, CONCLUIDO o CANCELADO; opcional
     * @param page número de página, iniciando en 1
     * @return ResponseEntity con HTTP 200 y la página de eventos
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando la página o el estado son inválidos
     */
    @GetMapping("/admin/events")
    public ResponseEntity<ApiResponse> listEventsAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.listEventsAdmin(category, status, page), "Eventos obtenidos correctamente"));
    }

    /**
     * Crea una noticia como borrador o publicada.
     *
     * @author Gabriela Zabaleta
     * @param authentication autenticación de la solicitud
     * @param request datos de la noticia
     * @return ResponseEntity con HTTP 201 y la noticia creada
     */
    @PostMapping("/admin/news")
    public ResponseEntity<ApiResponse> createNews(
            Authentication authentication, @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                newsService.createNews(CurrentUser.id(authentication), request), "Noticia creada correctamente"));
    }

    /**
     * Actualiza una noticia siguiendo el flujo BORRADOR a PUBLICADO.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @param request nuevos datos de la noticia
     * @return ResponseEntity con HTTP 200 y la noticia actualizada
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando la noticia no existe
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando se intenta devolver a borrador una noticia publicada
     */
    @PutMapping("/admin/news/{id}")
    public ResponseEntity<ApiResponse> updateNews(@PathVariable UUID id, @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.updateNews(id, request), "Noticia actualizada correctamente"));
    }

    /**
     * Archiva lógicamente una noticia.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando la noticia no existe
     */
    @DeleteMapping("/admin/news/{id}")
    public ResponseEntity<ApiResponse> deleteNews(@PathVariable UUID id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Noticia eliminada correctamente"));
    }

    /**
     * Crea un evento institucional.
     *
     * @author Gabriela Zabaleta
     * @param authentication autenticación de la solicitud
     * @param request datos del evento
     * @return ResponseEntity con HTTP 201 y el evento creado
     * @throws co.edu.ucc.orientacion.exceptions.UnprocessableEntityException cuando la fecha es anterior a hoy
     */
    @PostMapping("/admin/events")
    public ResponseEntity<ApiResponse> createEvent(
            Authentication authentication, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                newsService.createEvent(CurrentUser.id(authentication), request), "Evento creado correctamente"));
    }

    /**
     * Actualiza un evento institucional.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del evento
     * @param request nuevos datos del evento
     * @return ResponseEntity con HTTP 200 y el evento actualizado
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el evento no existe
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando el evento ya concluyó
     */
    @PutMapping("/admin/events/{id}")
    public ResponseEntity<ApiResponse> updateEvent(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.updateEvent(id, request), "Evento actualizado correctamente"));
    }

    /**
     * Cancela lógicamente un evento activo.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del evento
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando no existe un evento activo con ese identificador
     */
    @DeleteMapping("/admin/events/{id}")
    public ResponseEntity<ApiResponse> deleteEvent(@PathVariable UUID id) {
        newsService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Evento cancelado correctamente"));
    }
}
