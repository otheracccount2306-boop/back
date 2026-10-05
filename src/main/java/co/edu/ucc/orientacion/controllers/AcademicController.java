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

/**
 * Endpoints del módulo de información académica.
 *
 * @author Diego Luna
 */
@RestController
@RequestMapping("/api/v1")
public class AcademicController {

    private final AcademicService academicService;

    /**
     * Crea el controller con el servicio académico.
     *
     * @author Diego Luna
     * @param academicService servicio académico
     */
    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    /**
     * Consulta el horario del estudiante autenticado, opcionalmente filtrado por día.
     *
     * @author Diego Luna
     * @param authentication autenticación de la solicitud
     * @param day día de la semana; opcional
     * @return ResponseEntity con HTTP 200 y las asignaturas del horario
     * @throws co.edu.ucc.orientacion.exceptions.BadRequestException cuando el día no es válido
     */
    @GetMapping("/academic/schedule")
    public ResponseEntity<ApiResponse> getSchedule(
            Authentication authentication, @RequestParam(required = false) String day) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.getSchedule(CurrentUser.id(authentication), day), "Horario obtenido correctamente"));
    }

    /**
     * Consulta el calendario académico institucional, opcionalmente filtrado por categoría.
     *
     * @author Diego Luna
     * @param category categoría del evento; opcional
     * @return ResponseEntity con HTTP 200 y los eventos del calendario
     */
    @GetMapping("/academic/calendar")
    public ResponseEntity<ApiResponse> getCalendar(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.getCalendar(category), "Calendario obtenido correctamente"));
    }

    /**
     * Lista las asignaturas, activas e inactivas, opcionalmente de un periodo académico.
     *
     * @author Diego Luna
     * @param period periodo académico, por ejemplo 2026-1; opcional
     * @return ResponseEntity con HTTP 200 y las asignaturas
     */
    @GetMapping("/admin/academic/subjects")
    public ResponseEntity<ApiResponse> listSubjects(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(ApiResponse.ok(academicService.listSubjects(period), "Asignaturas obtenidas correctamente"));
    }

    /**
     * Crea una asignatura.
     *
     * @author Diego Luna
     * @param request datos de la asignatura
     * @return ResponseEntity con HTTP 201 y la asignatura creada
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando el código existe o el aula está ocupada
     */
    @PostMapping("/admin/academic/subjects")
    public ResponseEntity<ApiResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(academicService.createSubject(request), "Asignatura creada correctamente"));
    }

    /**
     * Actualiza una asignatura.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @param request nuevos datos de la asignatura
     * @return ResponseEntity con HTTP 200 y la asignatura actualizada
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando la asignatura no existe
     * @throws co.edu.ucc.orientacion.exceptions.ConflictException cuando el código existe o el aula está ocupada
     */
    @PutMapping("/admin/academic/subjects/{id}")
    public ResponseEntity<ApiResponse> updateSubject(
            @PathVariable UUID id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.updateSubject(id, request), "Asignatura actualizada correctamente"));
    }

    /**
     * Elimina lógicamente una asignatura.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando la asignatura no existe
     */
    @DeleteMapping("/admin/academic/subjects/{id}")
    public ResponseEntity<ApiResponse> deleteSubject(@PathVariable UUID id) {
        academicService.deleteSubject(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Asignatura eliminada correctamente"));
    }

    /**
     * Crea un evento del calendario académico.
     *
     * @author Diego Luna
     * @param request datos del evento
     * @return ResponseEntity con HTTP 201 y el evento creado
     */
    @PostMapping("/admin/academic/calendar")
    public ResponseEntity<ApiResponse> createCalendarEvent(@Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(academicService.createCalendarEvent(request), "Evento creado correctamente"));
    }

    /**
     * Actualiza un evento del calendario académico.
     *
     * @author Diego Luna
     * @param id identificador del evento
     * @param request nuevos datos del evento
     * @return ResponseEntity con HTTP 200 y el evento actualizado
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el evento no existe
     */
    @PutMapping("/admin/academic/calendar/{id}")
    public ResponseEntity<ApiResponse> updateCalendarEvent(
            @PathVariable UUID id, @Valid @RequestBody CalendarEventRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                academicService.updateCalendarEvent(id, request), "Evento actualizado correctamente"));
    }

    /**
     * Elimina lógicamente un evento del calendario académico.
     *
     * @author Diego Luna
     * @param id identificador del evento
     * @return ResponseEntity con HTTP 200
     * @throws co.edu.ucc.orientacion.exceptions.NotFoundException cuando el evento no existe
     */
    @DeleteMapping("/admin/academic/calendar/{id}")
    public ResponseEntity<ApiResponse> deleteCalendarEvent(@PathVariable UUID id) {
        academicService.deleteCalendarEvent(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Evento eliminado correctamente"));
    }
}
