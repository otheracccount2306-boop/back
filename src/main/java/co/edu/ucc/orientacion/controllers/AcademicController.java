package co.edu.ucc.orientacion.controllers;

import co.edu.ucc.orientacion.dto.request.CalendarEventRequest;
import co.edu.ucc.orientacion.dto.request.SubjectRequest;
import co.edu.ucc.orientacion.dto.response.ApiResponse;
import co.edu.ucc.orientacion.security.CurrentUser;
import co.edu.ucc.orientacion.services.AcademicService;
import jakarta.validation.Valid;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AcademicController {

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @GetMapping("/academic/schedule")
    public ResponseEntity<ApiResponse> getSchedule(
            Authentication authentication, @RequestParam(required = false) String day) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.getSchedule(CurrentUser.id(authentication), day), "Horario obtenido correctamente"));
    }

    @GetMapping("/academic/calendar")
    public ResponseEntity<ApiResponse> getCalendar(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.getCalendar(category), "Calendario obtenido correctamente"));
    }

    @GetMapping("/admin/academic/subjects")
    public ResponseEntity<ApiResponse> listSubjects(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listSubjects(period), "Asignaturas obtenidas correctamente"));
    }

    @PostMapping("/admin/academic/subjects")
    public ResponseEntity<ApiResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(academicService.createSubject(request), "Asignatura creada correctamente"));
    }

    @PutMapping("/admin/academic/subjects/{id}")
    public ResponseEntity<ApiResponse> updateSubject(
            @PathVariable UUID id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.updateSubject(id, request), "Asignatura actualizada correctamente"));
    }

    @DeleteMapping("/admin/academic/subjects/{id}")
    public ResponseEntity<ApiResponse> deleteSubject(@PathVariable UUID id) {
        academicService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Asignatura eliminada correctamente"));
    }

    @PostMapping("/admin/academic/calendar")
    public ResponseEntity<ApiResponse> createCalendarEvent(@Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(academicService.createCalendarEvent(request), "Evento creado correctamente"));
    }

    @PutMapping("/admin/academic/calendar/{id}")
    public ResponseEntity<ApiResponse> updateCalendarEvent(
            @PathVariable UUID id, @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.updateCalendarEvent(id, request), "Evento actualizado correctamente"));
    }

    @DeleteMapping("/admin/academic/calendar/{id}")
    public ResponseEntity<ApiResponse> deleteCalendarEvent(@PathVariable UUID id) {
        academicService.deleteCalendarEvent(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Evento eliminado correctamente"));
    }
}
