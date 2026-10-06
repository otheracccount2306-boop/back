package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.SpaceRequest;
import co.edu.ucc.orientacion.dto.request.SubjectRequest;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.models.Asignatura;
import co.edu.ucc.orientacion.models.Espacio;
import co.edu.ucc.orientacion.models.Faq;
import co.edu.ucc.orientacion.models.Servicio;
import co.edu.ucc.orientacion.repositories.CalendarRepository;
import co.edu.ucc.orientacion.repositories.EnrollmentRepository;
import co.edu.ucc.orientacion.repositories.EventRepository;
import co.edu.ucc.orientacion.repositories.FaqRepository;
import co.edu.ucc.orientacion.repositories.NewsRepository;
import co.edu.ucc.orientacion.repositories.ServiceRepository;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import co.edu.ucc.orientacion.repositories.SubjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de los listados administrativos y del campo activo, que permiten ocultar y volver a
 * mostrar servicios, preguntas, espacios y asignaturas desde el panel de administración.
 *
 * @author Doris Arzuaga
 * @author Diego Luna
 * @author Gabriela Zabaleta
 */
@ExtendWith(MockitoExtension.class)
class AdminPanelSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID id = UUID.randomUUID();

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FaqRepository faqRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private EventRepository eventRepository;

    private ServicesService servicesService;
    private CampusService campusService;
    private AcademicService academicService;
    private NewsService newsService;

    @BeforeEach
    void setUp() {
        servicesService = new ServicesService(
                serviceRepository, faqRepository, objectMapper, Validation.buildDefaultValidatorFactory().getValidator());
        campusService = new CampusService(spaceRepository);
        academicService = new AcademicService(subjectRepository, enrollmentRepository, calendarRepository, spaceRepository);
        newsService = new NewsService(newsRepository, eventRepository);
    }

    private Servicio servicio(boolean activo) {
        return new Servicio(id, "Servicio", null, "SALUD", null, null, null, activo, null);
    }

    private Espacio espacio(boolean activo) {
        return new Espacio(id, "Lab", "LAB-01", "LABORATORIO", null, null, null, null, null, null, activo, null);
    }

    private Asignatura asignatura(boolean activo) {
        return new Asignatura(id, "Cálculo", "MAT101", null, "A-101", "LUNES", LocalTime.of(8, 0), LocalTime.of(10, 0),
                "2026-1", activo, null);
    }

    private SubjectRequest subjectRequest(Boolean activo) {
        return new SubjectRequest("Cálculo", "MAT101", null, "A-101", List.of("LUNES"), LocalTime.of(8, 0),
                LocalTime.of(10, 0), "2026-1", activo);
    }

    @Test
    @DisplayName("Listar servicios acepta el alias department y devuelve también los inactivos")
    void listAllDepartmentAlias() {
        servicesService.listAll("department", null);

        verify(serviceRepository).findAll(List.of("DEPARTAMENTO"), null);
    }

    @Test
    @DisplayName("Listar bienestar normaliza y valida la categoría")
    void listAllWellbeingNormalizesCategory() {
        servicesService.listAll("wellbeing", "psicología");

        verify(serviceRepository).findAll(List.of("PSICOLOGIA", "SALUD", "DEPORTE", "CULTURA", "PASTORAL", "BECAS"), "PSICOLOGIA");
        assertThrows(BadRequestException.class, () -> servicesService.listAll("wellbeing", "GIMNASIO"));
    }

    @Test
    @DisplayName("Listar preguntas frecuentes normaliza la categoría y un tipo inválido responde 400")
    void listAllFaqAndInvalidType() {
        servicesService.listAll("faq", "matrículas");

        verify(faqRepository).findAll("MATRICULAS");
        assertThrows(BadRequestException.class, () -> servicesService.listAll("otro", null));
    }

    @Test
    @DisplayName("Actualizar un servicio con activo false lo oculta")
    void updateServiceCanDeactivate() throws Exception {
        when(serviceRepository.findById(id)).thenReturn(Optional.of(servicio(true)));
        when(serviceRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.update("wellbeing", id,
                objectMapper.readTree("{\"nombre\":\"S\",\"categoria\":\"SALUD\",\"activo\":false}"));

        verify(serviceRepository).update(captor.capture());
        assertFalse(captor.getValue().activo());
    }

    @Test
    @DisplayName("Un servicio inactivo se puede editar y reactivar")
    void updateServiceCanReactivate() throws Exception {
        when(serviceRepository.findById(id)).thenReturn(Optional.of(servicio(false)));
        when(serviceRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.update("wellbeing", id,
                objectMapper.readTree("{\"nombre\":\"S\",\"categoria\":\"SALUD\",\"activo\":true}"));

        verify(serviceRepository).update(captor.capture());
        assertTrue(captor.getValue().activo());
    }

    @Test
    @DisplayName("Editar sin indicar activo conserva el estado actual del servicio")
    void updateWithoutActiveKeepsCurrentState() throws Exception {
        when(serviceRepository.findById(id)).thenReturn(Optional.of(servicio(false)));
        when(serviceRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.update("wellbeing", id, objectMapper.readTree("{\"nombre\":\"S\",\"categoria\":\"SALUD\"}"));

        verify(serviceRepository).update(captor.capture());
        assertFalse(captor.getValue().activo());
    }

    @Test
    @DisplayName("Una pregunta frecuente inactiva se puede editar conservando o cambiando su estado")
    void updateFaqHandlesInactiveRecords() throws Exception {
        when(faqRepository.findById(id)).thenReturn(Optional.of(new Faq(id, "P", "R", "CAMPUS", 0, false, null)));
        when(faqRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Faq> captor = ArgumentCaptor.forClass(Faq.class);

        servicesService.update("faq", id, objectMapper.readTree("{\"pregunta\":\"P\",\"respuesta\":\"R\",\"categoria\":\"CAMPUS\"}"));
        servicesService.update("faq", id,
                objectMapper.readTree("{\"pregunta\":\"P\",\"respuesta\":\"R\",\"categoria\":\"CAMPUS\",\"activo\":true}"));

        verify(faqRepository, org.mockito.Mockito.times(2)).update(captor.capture());
        assertFalse(captor.getAllValues().get(0).activo());
        assertTrue(captor.getAllValues().get(1).activo());
    }

    @Test
    @DisplayName("Crear un servicio con activo false lo deja oculto desde el inicio")
    void createServiceCanStartInactive() throws Exception {
        when(serviceRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.create("departments", objectMapper.readTree("{\"nombre\":\"Registro\",\"activo\":false}"));

        verify(serviceRepository).create(captor.capture());
        assertFalse(captor.getValue().activo());
    }

    @Test
    @DisplayName("Listar espacios para administración normaliza y valida la categoría")
    void listAllSpaces() {
        campusService.listAllSpaces("Cafetería");

        verify(spaceRepository).findAll("CAFETERIA");
        assertThrows(BadRequestException.class, () -> campusService.listAllSpaces("PISCINA"));
    }

    @Test
    @DisplayName("Un espacio inactivo se puede editar: conserva su estado o se reactiva")
    void updateInactiveSpace() {
        when(spaceRepository.findById(id)).thenReturn(Optional.of(espacio(false)));
        when(spaceRepository.existsByCodigo("LAB-01", id)).thenReturn(false);
        when(spaceRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Espacio> captor = ArgumentCaptor.forClass(Espacio.class);

        campusService.updateSpace(id, new SpaceRequest("Lab", "lab-01", "LABORATORIO", null, null, null, null, null));
        campusService.updateSpace(id, new SpaceRequest("Lab", "lab-01", "LABORATORIO", null, null, null, null, true));

        verify(spaceRepository, org.mockito.Mockito.times(2)).update(captor.capture());
        assertFalse(captor.getAllValues().get(0).activo());
        assertTrue(captor.getAllValues().get(1).activo());
    }

    @Test
    @DisplayName("Listar asignaturas filtra por periodo y trata el texto en blanco como todos")
    void listSubjects() {
        academicService.listSubjects(" 2026-1 ");
        academicService.listSubjects("  ");

        verify(subjectRepository).findAll("2026-1");
        verify(subjectRepository).findAll(isNull());
    }

    @Test
    @DisplayName("Desactivar una asignatura no revisa conflictos de aula")
    void deactivatingSubjectSkipsRoomConflictCheck() {
        when(subjectRepository.findById(id)).thenReturn(Optional.of(asignatura(true)));
        when(subjectRepository.existsByCodigo("MAT101", id)).thenReturn(false);
        when(subjectRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        academicService.updateSubject(id, subjectRequest(false));

        verify(subjectRepository, never()).hasConflict(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Reactivar una asignatura vuelve a revisar conflictos de aula")
    void reactivatingSubjectChecksRoomConflict() {
        when(subjectRepository.findById(id)).thenReturn(Optional.of(asignatura(false)));
        when(subjectRepository.existsByCodigo("MAT101", id)).thenReturn(false);
        when(subjectRepository.hasConflict(any(), any(), any(), any(), any(), any())).thenReturn(true);

        assertThrows(co.edu.ucc.orientacion.exceptions.ConflictException.class,
                () -> academicService.updateSubject(id, subjectRequest(true)));
    }

    @Test
    @DisplayName("Crear una asignatura inactiva no revisa conflictos de aula")
    void creatingInactiveSubjectSkipsRoomConflictCheck() {
        when(subjectRepository.existsByCodigo(any(), any())).thenReturn(false);
        when(subjectRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        academicService.createSubject(subjectRequest(false));

        verify(subjectRepository, never()).hasConflict(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Listar noticias para administración normaliza el estado y pagina de a 10")
    void listNewsAdmin() {
        newsService.listNewsAdmin(null, "borrador", 2);

        verify(newsRepository).findAllAdmin(null, "BORRADOR", 10, 10);
        verify(newsRepository).countAllAdmin(null, "BORRADOR");
    }

    @Test
    @DisplayName("Listar noticias para administración valida estado y página")
    void listNewsAdminValidation() {
        assertThrows(BadRequestException.class, () -> newsService.listNewsAdmin(null, "OTRO", 1));
        assertThrows(BadRequestException.class, () -> newsService.listNewsAdmin(null, null, 0));
        verify(newsRepository, never()).findAllAdmin(any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Listar eventos para administración concluye los pasados y filtra por estado")
    void listEventsAdmin() {
        newsService.listEventsAdmin("Cultura", "cancelado", 1);

        verify(eventRepository).concludePast(LocalDate.now().atStartOfDay());
        verify(eventRepository).findAllAdmin("CULTURA", "CANCELADO", 10, 0);
        assertThrows(BadRequestException.class, () -> newsService.listEventsAdmin(null, "ARCHIVADO", 1));
    }
}
