package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.SpaceRequest;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Espacio;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusServiceTest {

    @Mock
    private SpaceRepository spaceRepository;

    private CampusService campusService;

    @BeforeEach
    void setUp() {
        campusService = new CampusService(spaceRepository);
    }

    private SpaceRequest spaceRequest(String codigo, String categoria) {
        return new SpaceRequest("Laboratorio de Redes", codigo, categoria, "Bloque B", "2", null, null, null);
    }

    @Test
    @DisplayName("La búsqueda con menos de 2 caracteres responde 400")
    void searchWithFewCharactersFails() {
        assertThrows(BadRequestException.class, () -> campusService.searchSpaces("a"));
        assertThrows(BadRequestException.class, () -> campusService.searchSpaces("  a "));
        assertThrows(BadRequestException.class, () -> campusService.searchSpaces(""));
        assertThrows(BadRequestException.class, () -> campusService.searchSpaces(null));

        verify(spaceRepository, never()).search(any());
    }

    @Test
    @DisplayName("La búsqueda con 2 o más caracteres usa un patrón insensible a mayúsculas")
    void searchBuildsPattern() {
        campusService.searchSpaces("Lab");

        verify(spaceRepository).search("%Lab%");
    }

    @Test
    @DisplayName("El listado con una categoría inválida responde 400")
    void listWithInvalidCategoryFails() {
        assertThrows(BadRequestException.class, () -> campusService.getSpaces("PISCINA"));
    }

    @Test
    @DisplayName("El listado normaliza la categoría")
    void listNormalizesCategory() {
        campusService.getSpaces("Área común");

        verify(spaceRepository).findActive("AREA_COMUN");
    }

    @Test
    @DisplayName("Crear un espacio con código duplicado responde 409")
    void createWithDuplicateCodeFails() {
        when(spaceRepository.existsByCodigo("LAB-01", null)).thenReturn(true);

        assertThrows(ConflictException.class, () -> campusService.createSpace(spaceRequest("lab-01", "LABORATORIO")));

        verify(spaceRepository, never()).create(any());
    }

    @Test
    @DisplayName("Crear un espacio con categoría inválida responde 400")
    void createWithInvalidCategoryFails() {
        assertThrows(BadRequestException.class, () -> campusService.createSpace(spaceRequest("X-01", "PISCINA")));
    }

    @Test
    @DisplayName("Crear un espacio normaliza el código y la categoría")
    void createNormalizesData() {
        when(spaceRepository.existsByCodigo(any(), isNull())).thenReturn(false);
        when(spaceRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Espacio> captor = ArgumentCaptor.forClass(Espacio.class);

        campusService.createSpace(spaceRequest("  lab-01 ", "laboratorio"));

        verify(spaceRepository).create(captor.capture());
        assertEquals("LAB-01", captor.getValue().codigo());
        assertEquals("LABORATORIO", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Actualizar un espacio inexistente responde 404")
    void updateMissingSpaceFails() {
        UUID id = UUID.randomUUID();
        when(spaceRepository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(NotFoundException.class, () -> campusService.updateSpace(id, spaceRequest("LAB-01", "LABORATORIO")));
    }

    @Test
    @DisplayName("Eliminar un espacio inexistente responde 404")
    void deleteMissingSpaceFails() {
        UUID id = UUID.randomUUID();
        when(spaceRepository.deactivate(id)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> campusService.deleteSpace(id));
    }
}
