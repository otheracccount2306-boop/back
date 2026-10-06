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

@RestController
@RequestMapping("/api/v1")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/news")
    public ResponseEntity<ApiResponse> listNews(
            @RequestParam(required = false) String category, @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.listNews(category, page), "Noticias obtenidas correctamente"));
    }

    @GetMapping("/news/{id}")
    public ResponseEntity<ApiResponse> getNews(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.getNews(id, CurrentUser.isAdmin(authentication)), "Noticia obtenida correctamente"));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse> listEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.listEvents(category, from, to, page), "Eventos obtenidos correctamente"));
    }

    @GetMapping("/admin/news")
    public ResponseEntity<ApiResponse> listNewsAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.listNewsAdmin(category, status, page), "Noticias obtenidas correctamente"));
    }

    @GetMapping("/admin/events")
    public ResponseEntity<ApiResponse> listEventsAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.ok(
                newsService.listEventsAdmin(category, status, page), "Eventos obtenidos correctamente"));
    }

    @PostMapping("/admin/news")
    public ResponseEntity<ApiResponse> createNews(
            Authentication authentication, @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                newsService.createNews(CurrentUser.id(authentication), request), "Noticia creada correctamente"));
    }

    @PutMapping("/admin/news/{id}")
    public ResponseEntity<ApiResponse> updateNews(@PathVariable UUID id, @Valid @RequestBody NewsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.updateNews(id, request), "Noticia actualizada correctamente"));
    }

    @DeleteMapping("/admin/news/{id}")
    public ResponseEntity<ApiResponse> deleteNews(@PathVariable UUID id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Noticia eliminada correctamente"));
    }

    @PostMapping("/admin/events")
    public ResponseEntity<ApiResponse> createEvent(
            Authentication authentication, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                newsService.createEvent(CurrentUser.id(authentication), request), "Evento creado correctamente"));
    }

    @PutMapping("/admin/events/{id}")
    public ResponseEntity<ApiResponse> updateEvent(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(newsService.updateEvent(id, request), "Evento actualizado correctamente"));
    }

    @DeleteMapping("/admin/events/{id}")
    public ResponseEntity<ApiResponse> deleteEvent(@PathVariable UUID id) {
        newsService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Evento cancelado correctamente"));
    }
}
