package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.CalendarEventRequest;
import co.edu.ucc.orientacion.dto.request.SubjectRequest;
import co.edu.ucc.orientacion.dto.response.SubjectResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Asignatura;
import co.edu.ucc.orientacion.models.EventoCalendario;
import co.edu.ucc.orientacion.repositories.CalendarRepository;
import co.edu.ucc.orientacion.repositories.EnrollmentRepository;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import co.edu.ucc.orientacion.repositories.SubjectRepository;
import co.edu.ucc.orientacion.utils.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;

/**
 * Lógica de negocio del módulo académico: horario del estudiante, calendario institucional
 * y administración de asignaturas y eventos de calendario.
 *
 * @author Diego Luna
 */
@Service
public class AcademicService {

    private static final List<String> WEEK_DAYS =
            List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO");

    private final SubjectRepository subjectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CalendarRepository calendarRepository;
    private final SpaceRepository spaceRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @author Diego Luna
     * @param subjectRepository repositorio de asignaturas
     * @param enrollmentRepository repositorio de matrículas
     * @param calendarRepository repositorio del calendario académico
     * @param spaceRepository repositorio de espacios, para ubicar el aula de cada clase en el mapa
     */
    public AcademicService(
            SubjectRepository subjectRepository,
            EnrollmentRepository enrollmentRepository,
            CalendarRepository calendarRepository,
            SpaceRepository spaceRepository) {
        this.subjectRepository = subjectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.calendarRepository = calendarRepository;
        this.spaceRepository = spaceRepository;
    }

    /**
     * Obtiene el horario del estudiante autenticado, opcionalmente filtrado por día de la semana.
     *
     * @author Diego Luna
     * @param userId identificador del estudiante
     * @param day día de la semana, por ejemplo LUNES o miércoles; null para todos los días
     * @return asignaturas del horario ordenadas por hora de inicio
     * @throws BadRequestException cuando el día no es un día válido de la semana
     */
    public List<SubjectResponse> getSchedule(UUID userId, String day) {
        String normalizedDay = TextUtils.normalizeKey(day);
        if (normalizedDay != null && !WEEK_DAYS.contains(normalizedDay)) {
            throw new BadRequestException("Día inválido. Valores permitidos: " + String.join(", ", WEEK_DAYS));
        }
        return withRooms(enrollmentRepository.findScheduleByUsuario(userId, normalizedDay));
    }

    /**
     * Arma la vista de cada asignatura con el espacio del mapa que corresponde a su aula.
     *
     * @author Diego Luna
     * @param subjects asignaturas
     * @return asignaturas con espacioId cuando su aula está ubicada en el mapa
     */
    private List<SubjectResponse> withRooms(List<Asignatura> subjects) {
        boolean anyRoom = subjects.stream().anyMatch(a -> a.aula() != null && !a.aula().isBlank());
        Map<String, UUID> rooms = anyRoom ? indexRooms(spaceRepository.findMappedRooms()) : Map.of();
        return subjects.stream()
                .map(a -> {
                    String key = TextUtils.normalizeRoom(a.aula());
                    return SubjectResponse.from(a, key == null ? null : rooms.get(key));
                })
                .toList();
    }

    private SubjectResponse withRoom(Asignatura subject) {
        return withRooms(List.of(subject)).get(0);
    }

    /**
     * Índice para reconocer el aula de una clase: se puede escribir el código del espacio
     * (AU-2-101) o su nombre (Aula 2 101), sin importar mayúsculas, tildes ni separadores. El
     * código manda sobre el nombre, y un nombre repetido en varios espacios no se usa porque no
     * se sabría cuál es.
     *
     * @author Diego Luna
     * @param rooms espacios ubicados en el mapa
     * @return aula normalizada → identificador del espacio
     */
    static Map<String, UUID> indexRooms(List<SpaceRepository.MappedRoom> rooms) {
        Map<String, UUID> byCode = new HashMap<>();
        Map<String, UUID> byName = new HashMap<>();
        Set<String> repeated = new HashSet<>();
        for (SpaceRepository.MappedRoom room : rooms) {
            String code = TextUtils.normalizeRoom(room.codigo());
            if (code != null) {
                byCode.putIfAbsent(code, room.id());
            }
            String name = TextUtils.normalizeRoom(room.nombre());
            if (name != null && byName.putIfAbsent(name, room.id()) != null) {
                repeated.add(name);
            }
        }
        repeated.forEach(byName::remove);
        byName.putAll(byCode);
        return byName;
    }

    /**
     * Obtiene el calendario académico institucional, opcionalmente filtrado por categoría.
     *
     * @author Diego Luna
     * @param category categoría del evento; null para todas
     * @return eventos activos ordenados por fecha de inicio
     */
    public List<EventoCalendario> getCalendar(String category) {
        return calendarRepository.findActive(TextUtils.normalizeKey(category));
    }

    /**
     * Crea una asignatura verificando que su código sea único y que no genere conflicto de aula.
     *
     * @author Diego Luna
     * @param request datos de la asignatura
     * @return asignatura creada
     * @throws BadRequestException cuando los días u horas son inválidos
     * @throws ConflictException cuando el código ya existe o el aula está ocupada en ese horario
     */
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        Asignatura candidate = toAsignatura(null, request, true);
        ensureUniqueCode(candidate.codigo(), null);
        if (candidate.activo()) {
            ensureNoRoomConflict(candidate, null);
        }
        return withRoom(subjectRepository.create(candidate));
    }

    /**
     * Lista las asignaturas, activas e inactivas, para la administración.
     *
     * @author Diego Luna
     * @param period periodo académico exacto, por ejemplo 2026-1; null para todos
     * @return asignaturas ordenadas por periodo descendente, estado y nombre
     */
    public List<SubjectResponse> listSubjects(String period) {
        String normalized = period == null || period.isBlank() ? null : period.trim();
        return withRooms(subjectRepository.findAll(normalized));
    }

    /**
     * Actualiza una asignatura, esté activa o no, verificando unicidad de código y, si queda
     * activa, conflictos de aula. El campo activo de la solicitud permite ocultarla o reactivarla.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @param request nuevos datos de la asignatura
     * @return asignatura actualizada
     * @throws NotFoundException cuando la asignatura no existe
     * @throws BadRequestException cuando los días u horas son inválidos
     * @throws ConflictException cuando el código ya existe o el aula está ocupada en ese horario
     */
    @Transactional
    public SubjectResponse updateSubject(UUID id, SubjectRequest request) {
        Asignatura current = subjectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asignatura no encontrada"));
        Asignatura candidate = toAsignatura(id, request, current.activo());
        ensureUniqueCode(candidate.codigo(), id);
        if (candidate.activo()) {
            ensureNoRoomConflict(candidate, id);
        }
        return withRoom(subjectRepository.update(candidate));
    }

    /**
     * Desactiva lógicamente una asignatura.
     *
     * @author Diego Luna
     * @param id identificador de la asignatura
     * @throws NotFoundException cuando la asignatura no existe o ya está inactiva
     */
    public void deleteSubject(UUID id) {
        if (subjectRepository.deactivate(id) == 0) {
            throw new NotFoundException("Asignatura no encontrada");
        }
    }

    /**
     * Crea un evento del calendario académico.
     *
     * @author Diego Luna
     * @param request datos del evento
     * @return evento creado
     * @throws BadRequestException cuando la fecha de fin es anterior a la de inicio
     */
    public EventoCalendario createCalendarEvent(CalendarEventRequest request) {
        return calendarRepository.create(toEvento(null, request));
    }

    /**
     * Actualiza un evento activo del calendario académico.
     *
     * @author Diego Luna
     * @param id identificador del evento
     * @param request nuevos datos del evento
     * @return evento actualizado
     * @throws NotFoundException cuando el evento no existe o está inactivo
     * @throws BadRequestException cuando la fecha de fin es anterior a la de inicio
     */
    @Transactional
    public EventoCalendario updateCalendarEvent(UUID id, CalendarEventRequest request) {
        calendarRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Evento de calendario no encontrado"));
        return calendarRepository.update(toEvento(id, request));
    }

    /**
     * Desactiva lógicamente un evento del calendario académico.
     *
     * @author Diego Luna
     * @param id identificador del evento
     * @throws NotFoundException cuando el evento no existe o ya está inactivo
     */
    public void deleteCalendarEvent(UUID id) {
        if (calendarRepository.deactivate(id) == 0) {
            throw new NotFoundException("Evento de calendario no encontrado");
        }
    }

    private Asignatura toAsignatura(UUID id, SubjectRequest request, boolean currentActive) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new BadRequestException("La hora de fin debe ser posterior a la hora de inicio");
        }
        String aula = request.aula() == null || request.aula().isBlank() ? null : request.aula().trim();
        return new Asignatura(
                id,
                request.nombre().trim(),
                request.codigo().trim().toUpperCase(Locale.ROOT),
                request.docente() == null || request.docente().isBlank() ? null : request.docente().trim(),
                aula,
                normalizeDays(request.dias()),
                request.horaInicio(),
                request.horaFin(),
                request.periodoAcademico().trim(),
                request.activo() == null ? currentActive : request.activo(),
                null);
    }

    private String normalizeDays(List<String> days) {
        List<String> normalized = days.stream().map(TextUtils::normalizeKey).toList();
        for (String day : normalized) {
            if (day == null || !WEEK_DAYS.contains(day)) {
                throw new BadRequestException("Día inválido. Valores permitidos: " + String.join(", ", WEEK_DAYS));
            }
        }
        return WEEK_DAYS.stream().filter(normalized::contains).reduce((a, b) -> a + "," + b).orElseThrow();
    }

    private void ensureUniqueCode(String codigo, UUID excludeId) {
        if (subjectRepository.existsByCodigo(codigo, excludeId)) {
            throw new ConflictException("Ya existe una asignatura con el código " + codigo);
        }
    }

    private void ensureNoRoomConflict(Asignatura candidate, UUID excludeId) {
        if (candidate.aula() == null) {
            return;
        }
        boolean conflict = subjectRepository.hasConflict(
                candidate.aula(),
                candidate.periodoAcademico(),
                candidate.dias(),
                candidate.horaInicio(),
                candidate.horaFin(),
                excludeId);
        if (conflict) {
            throw new ConflictException("El aula " + candidate.aula() + " ya está ocupada en ese horario");
        }
    }

    private EventoCalendario toEvento(UUID id, CalendarEventRequest request) {
        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new BadRequestException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
        return new EventoCalendario(
                id,
                request.nombre().trim(),
                request.descripcion(),
                TextUtils.normalizeKey(request.categoria()),
                request.fechaInicio(),
                request.fechaFin(),
                true,
                null);
    }
}
