package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Faq;
import co.edu.ucc.orientacion.models.Servicio;
import co.edu.ucc.orientacion.repositories.FaqRepository;
import co.edu.ucc.orientacion.repositories.ServiceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicesServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FaqRepository faqRepository;

    private ServicesService servicesService;

    @BeforeEach
    void setUp() {
        servicesService = new ServicesService(
                serviceRepository, faqRepository, objectMapper, Validation.buildDefaultValidatorFactory().getValidator());
    }

    private Servicio servicio(UUID id, String categoria) {
        return new Servicio(id, "Servicio", null, categoria, null, null, null, true, null);
    }

    @Test
    @DisplayName("Un tipo de servicio inválido responde 400")
    void invalidTypeFails() throws Exception {
        assertThrows(BadRequestException.class,
                () -> servicesService.create("otro", objectMapper.readTree("{\"nombre\":\"X\"}")));
    }

    @Test
    @DisplayName("Bienestar con categoría inválida responde 400")
    void wellbeingWithInvalidCategoryFails() {
        assertThrows(BadRequestException.class, () -> servicesService.getWellbeing("GIMNASIO"));
    }

    @Test
    @DisplayName("Bienestar acepta la categoría con tildes y minúsculas")
    void wellbeingNormalizesCategory() {
        servicesService.getWellbeing("psicología");

        verify(serviceRepository).search(
                List.of("PSICOLOGIA", "SALUD", "DEPORTE", "CULTURA", "PASTORAL", "BECAS"), "PSICOLOGIA", null);
    }

    @Test
    @DisplayName("El directorio busca por un patrón insensible a mayúsculas")
    void departmentsSearchesByPattern() {
        servicesService.getDepartments("registro");

        verify(serviceRepository).search(List.of("DEPARTAMENTO"), null, "%registro%");
    }

    @Test
    @DisplayName("Las preguntas frecuentes filtran por categoría y palabra clave")
    void faqFiltersByCategoryAndKeyword() {
        servicesService.getFaq("Matrículas", "pago");

        verify(faqRepository).search("MATRICULAS", "%pago%");
    }

    @Test
    @DisplayName("Crear una dependencia fuerza la categoría DEPARTAMENTO")
    void createDepartmentForcesCategory() throws Exception {
        when(serviceRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.create("departments", objectMapper.readTree("{\"nombre\":\"Registro y Control\"}"));

        verify(serviceRepository).create(captor.capture());
        assertEquals("DEPARTAMENTO", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Crear un servicio de bienestar sin categoría responde 400")
    void createWellbeingWithoutCategoryFails() throws Exception {
        assertThrows(BadRequestException.class,
                () -> servicesService.create("wellbeing", objectMapper.readTree("{\"nombre\":\"Psicología\"}")));

        verify(serviceRepository, never()).create(any());
    }

    @Test
    @DisplayName("Crear un servicio de bienestar con categoría inválida responde 400")
    void createWellbeingWithInvalidCategoryFails() throws Exception {
        assertThrows(BadRequestException.class, () -> servicesService.create(
                "wellbeing", objectMapper.readTree("{\"nombre\":\"Gym\",\"categoria\":\"GIMNASIO\"}")));
    }

    @Test
    @DisplayName("Crear un servicio de bienestar normaliza la categoría")
    void createWellbeingNormalizesCategory() throws Exception {
        when(serviceRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.create("wellbeing", objectMapper.readTree("{\"nombre\":\"Orientación\",\"categoria\":\"Psicología\"}"));

        verify(serviceRepository).create(captor.capture());
        assertEquals("PSICOLOGIA", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Un cuerpo que incumple las validaciones responde con ConstraintViolationException")
    void invalidBodyFailsValidation() throws Exception {
        assertThrows(ConstraintViolationException.class,
                () -> servicesService.create("departments", objectMapper.readTree("{\"nombre\":\"\"}")));
    }

    @Test
    @DisplayName("Crear una pregunta frecuente asigna frecuencia inicial cero")
    void createFaqInitializesFrequency() throws Exception {
        when(faqRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Faq> captor = ArgumentCaptor.forClass(Faq.class);

        servicesService.create("faq", objectMapper.readTree(
                "{\"pregunta\":\"¿Cómo pago?\",\"respuesta\":\"En caja\",\"categoria\":\"Matrículas\"}"));

        verify(faqRepository).create(captor.capture());
        assertEquals(0, captor.getValue().frecuencia());
        assertEquals("MATRICULAS", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Actualizar como dependencia un servicio de bienestar responde 404")
    void updateWithWrongTypeFails() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRepository.findById(id)).thenReturn(Optional.of(servicio(id, "SALUD")));

        assertThrows(NotFoundException.class,
                () -> servicesService.update("departments", id, objectMapper.readTree("{\"nombre\":\"X\"}")));

        verify(serviceRepository, never()).update(any());
    }

    @Test
    @DisplayName("Eliminar un servicio de bienestar lo desactiva")
    void deleteWellbeingDeactivates() {
        UUID id = UUID.randomUUID();
        when(serviceRepository.findActiveById(id)).thenReturn(Optional.of(servicio(id, "SALUD")));

        servicesService.delete("wellbeing", id);

        verify(serviceRepository).deactivate(id);
    }

    @Test
    @DisplayName("Eliminar una pregunta frecuente inexistente responde 404")
    void deleteMissingFaqFails() {
        UUID id = UUID.randomUUID();
        when(faqRepository.deactivate(id)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> servicesService.delete("faq", id));
    }

    @Test
    @DisplayName("Actualizar sin categoría en una dependencia mantiene DEPARTAMENTO")
    void updateDepartmentKeepsCategory() throws Exception {
        UUID id = UUID.randomUUID();
        when(serviceRepository.findById(id)).thenReturn(Optional.of(servicio(id, "DEPARTAMENTO")));
        when(serviceRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);

        servicesService.update("departments", id, objectMapper.readTree("{\"nombre\":\"Tesorería\"}"));

        verify(serviceRepository).update(captor.capture());
        assertEquals(id, captor.getValue().id());
        assertEquals("DEPARTAMENTO", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Los cuerpos nulos responden 400")
    void nullBodyFails() throws Exception {
        assertThrows(BadRequestException.class,
                () -> servicesService.create("faq", objectMapper.readTree("null")));
        verify(faqRepository, never()).create(any());
        verify(serviceRepository, never()).create(isNull());
    }
}
