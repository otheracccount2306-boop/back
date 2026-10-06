package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.CalendarEventRequest;
import co.edu.ucc.orientacion.dto.request.SubjectRequest;
import co.edu.ucc.orientacion.dto.response.SubjectResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Asignatura;
import co.edu.ucc.orientacion.repositories.CalendarRepository;
import co.edu.ucc.orientacion.repositories.EnrollmentRepository;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import co.edu.ucc.orientacion.repositories.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private SpaceRepository spaceRepository;

    private AcademicService academicService;

    @BeforeEach
    void setUp() {
        academicService = new AcademicService(subjectRepository, enrollmentRepository, calendarRepository, spaceRepository);
    }

    private SubjectRequest subjectRequest(List<String> dias, LocalTime inicio, LocalTime fin) {
        return new SubjectRequest("Cálculo I", " mat101 ", "Docente", "A-101", dias, inicio, fin, "2026-1", null);
    }

    @Test
    @DisplayName("El horario normaliza el día recibido antes de consultar")
    void scheduleNormalizesDay() {
        UUID userId = UUID.randomUUID();
        when(enrollmentRepository.findScheduleByUsuario(userId, "MIERCOLES")).thenReturn(List.of());

        academicService.getSchedule(userId, "miércoles");

        verify(enrollmentRepository).findScheduleByUsuario(userId, "MIERCOLES");
    }

    @Test
    @DisplayName("El horario sin día no filtra")
    void scheduleWithoutDayDoesNotFilter() {
        UUID userId = UUID.randomUUID();

        academicService.getSchedule(userId, null);

        verify(enrollmentRepository).findScheduleByUsuario(userId, null);
    }

    private static Asignatura clase(String codigo, String aula) {
        return new Asignatura(UUID.randomUUID(), "Clase " + codigo, codigo, null, aula, "LUNES",
                LocalTime.of(7, 0), LocalTime.of(9, 0), "2026-1", true, null);
    }

    @Test
    @DisplayName("El horario ubica en el mapa el aula de cada clase por código o por nombre")
    void scheduleResolvesRoomsOnMap() {
        UUID userId = UUID.randomUUID();
        UUID aula = UUID.randomUUID();
        UUID sala = UUID.randomUUID();
        UUID cafetin1 = UUID.randomUUID();
        when(enrollmentRepository.findScheduleByUsuario(userId, null)).thenReturn(List.of(
                clase("A", "au-2-101"),
                clase("B", "Sala de computo 2"),
                clase("C", "Cafetín"),
                clase("D", "Aula 301"),
                clase("E", null)));
        when(spaceRepository.findMappedRooms()).thenReturn(List.of(
                new SpaceRepository.MappedRoom(aula, "AU-2-101", "Aula 2 101"),
                new SpaceRepository.MappedRoom(sala, "B45P2-SALA-COMP", "Sala de Cómputo 2"),
                new SpaceRepository.MappedRoom(cafetin1, "CAF-1", "Cafetín"),
                new SpaceRepository.MappedRoom(UUID.randomUUID(), "CAF-2", "Cafetín")));

        List<SubjectResponse> horario = academicService.getSchedule(userId, null);

        assertEquals(aula, horario.get(0).espacioId());
        assertEquals(sala, horario.get(1).espacioId());
        assertNull(horario.get(2).espacioId(), "un nombre repetido no identifica un solo espacio");
        assertNull(horario.get(3).espacioId());
        assertNull(horario.get(4).espacioId());
    }

    @Test
    @DisplayName("El código de un espacio manda sobre el nombre de otro")
    void roomCodeWinsOverName() {
        UUID porCodigo = UUID.randomUUID();
        var index = AcademicService.indexRooms(List.of(
                new SpaceRepository.MappedRoom(UUID.randomUUID(), "X-1", "Lab 1"),
                new SpaceRepository.MappedRoom(porCodigo, "LAB-1", "Laboratorio")));

        assertEquals(porCodigo, index.get("LAB 1"));
    }

    @Test
    @DisplayName("El horario con un día inválido responde 400")
    void scheduleWithInvalidDayFails() {
        assertThrows(BadRequestException.class, () -> academicService.getSchedule(UUID.randomUUID(), "FUNDAY"));
    }

    @Test
    @DisplayName("El calendario normaliza la categoría")
    void calendarNormalizesCategory() {
        academicService.getCalendar("Académico");

        verify(calendarRepository).findActive("ACADEMICO");
    }

    @Test
    @DisplayName("Crear una asignatura normaliza el código y ordena los días de la semana")
    void createSubjectNormalizesData() {
        when(subjectRepository.existsByCodigo(any(), any())).thenReturn(false);
        when(subjectRepository.hasConflict(any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(subjectRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Asignatura> captor = ArgumentCaptor.forClass(Asignatura.class);

        SubjectResponse response = academicService.createSubject(
                subjectRequest(List.of("miércoles", "Lunes", "lunes"), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        verify(subjectRepository).create(captor.capture());
        assertEquals("MAT101", captor.getValue().codigo());
        assertEquals("LUNES,MIERCOLES", captor.getValue().dias());
        assertEquals(List.of("LUNES", "MIERCOLES"), response.dias());
    }

    @Test
    @DisplayName("Crear una asignatura con código repetido responde 409")
    void createSubjectWithDuplicateCodeFails() {
        when(subjectRepository.existsByCodigo(eq("MAT101"), isNull())).thenReturn(true);

        assertThrows(ConflictException.class, () -> academicService.createSubject(
                subjectRequest(List.of("LUNES"), LocalTime.of(8, 0), LocalTime.of(10, 0))));

        verify(subjectRepository, never()).create(any());
    }

    @Test
    @DisplayName("Crear una asignatura en un aula ocupada en el mismo horario responde 409")
    void createSubjectWithRoomConflictFails() {
        when(subjectRepository.existsByCodigo(any(), any())).thenReturn(false);
        when(subjectRepository.hasConflict(eq("A-101"), eq("2026-1"), eq("LUNES"),
                eq(LocalTime.of(8, 0)), eq(LocalTime.of(10, 0)), isNull())).thenReturn(true);

        assertThrows(ConflictException.class, () -> academicService.createSubject(
                subjectRequest(List.of("LUNES"), LocalTime.of(8, 0), LocalTime.of(10, 0))));

        verify(subjectRepository, never()).create(any());
    }

    @Test
    @DisplayName("Crear una asignatura con hora de fin no posterior a la de inicio responde 400")
    void createSubjectWithInvalidHoursFails() {
        assertThrows(BadRequestException.class, () -> academicService.createSubject(
                subjectRequest(List.of("LUNES"), LocalTime.of(10, 0), LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("Crear una asignatura con un día inválido responde 400")
    void createSubjectWithInvalidDayFails() {
        assertThrows(BadRequestException.class, () -> academicService.createSubject(
                subjectRequest(List.of("FUNDAY"), LocalTime.of(8, 0), LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("Actualizar una asignatura inexistente responde 404")
    void updateMissingSubjectFails() {
        UUID id = UUID.randomUUID();
        when(subjectRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> academicService.updateSubject(
                id, subjectRequest(List.of("LUNES"), LocalTime.of(8, 0), LocalTime.of(10, 0))));
    }

    @Test
    @DisplayName("Al actualizar se excluye a la propia asignatura de la detección de conflictos")
    void updateSubjectExcludesItselfFromConflicts() {
        UUID id = UUID.randomUUID();
        when(subjectRepository.findById(id)).thenReturn(java.util.Optional.of(
                new Asignatura(id, "Cálculo I", "MAT101", null, "A-101", "LUNES",
                        LocalTime.of(8, 0), LocalTime.of(10, 0), "2026-1", true, null)));
        when(subjectRepository.existsByCodigo("MAT101", id)).thenReturn(false);
        when(subjectRepository.hasConflict(any(), any(), any(), any(), any(), eq(id))).thenReturn(false);
        when(subjectRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        academicService.updateSubject(id, subjectRequest(List.of("LUNES"), LocalTime.of(8, 0), LocalTime.of(10, 0)));

        verify(subjectRepository).existsByCodigo("MAT101", id);
    }

    @Test
    @DisplayName("Eliminar una asignatura inexistente responde 404")
    void deleteMissingSubjectFails() {
        UUID id = UUID.randomUUID();
        when(subjectRepository.deactivate(id)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> academicService.deleteSubject(id));
    }

    @Test
    @DisplayName("Un evento de calendario con fecha de fin anterior a la de inicio responde 400")
    void calendarEventWithInvalidRangeFails() {
        CalendarEventRequest request = new CalendarEventRequest(
                "Parciales", null, "académico", LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 1));

        assertThrows(BadRequestException.class, () -> academicService.createCalendarEvent(request));
    }
}
