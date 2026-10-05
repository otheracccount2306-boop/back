package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.SpaceRequest;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Espacio;
import co.edu.ucc.orientacion.repositories.SpaceRepository;
import co.edu.ucc.orientacion.utils.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lógica de negocio de la infraestructura del campus: catálogo, búsqueda y administración de espacios.
 *
 * @author Diego Luna
 */
@Service
public class CampusService {

    public static final int MIN_SEARCH_LENGTH = 2;

    private static final List<String> SPACE_CATEGORIES =
            List.of("AULA", "LABORATORIO", "OFICINA", "BIBLIOTECA", "CAFETERIA", "AREA_COMUN");

    private final SpaceRepository spaceRepository;

    /**
     * Crea el servicio con el repositorio de espacios.
     *
     * @author Diego Luna
     * @param spaceRepository repositorio de espacios
     */
    public CampusService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    /**
     * Lista los espacios activos del campus, opcionalmente filtrados por categoría.
     *
     * @author Diego Luna
     * @param category categoría del espacio; null para todas
     * @return espacios activos ordenados por nombre
     * @throws BadRequestException cuando la categoría no es válida
     */
    public List<Espacio> getSpaces(String category) {
        String normalized = TextUtils.normalizeKey(category);
        if (normalized != null) {
            validateCategory(normalized);
        }
        return spaceRepository.findActive(normalized);
    }

    /**
     * Busca espacios activos por nombre o código sin distinguir mayúsculas.
     *
     * @author Diego Luna
     * @param query texto buscado, de al menos 2 caracteres; la búsqueda ignora tildes y mayúsculas
     * @return espacios que coinciden con la búsqueda
     * @throws BadRequestException cuando el texto tiene menos de 2 caracteres
     */
    public List<Espacio> searchSpaces(String query) {
        if (query == null || query.trim().length() < MIN_SEARCH_LENGTH) {
            throw new BadRequestException(
                    "La búsqueda requiere mínimo " + MIN_SEARCH_LENGTH + " caracteres");
        }
        return spaceRepository.search(TextUtils.likePattern(query));
    }

    /**
     * Crea un espacio verificando que su código sea único.
     *
     * @author Diego Luna
     * @param request datos del espacio
     * @return espacio creado
     * @throws BadRequestException cuando la categoría no es válida
     * @throws ConflictException cuando el código ya está en uso
     */
    @Transactional
    public Espacio createSpace(SpaceRequest request) {
        Espacio candidate = toEspacio(null, request, true);
        ensureUniqueCode(candidate.codigo(), null);
        return spaceRepository.create(candidate);
    }

    /**
     * Lista todos los espacios, activos e inactivos, para la administración.
     *
     * @author Diego Luna
     * @param category categoría del espacio; null para todas
     * @return espacios ordenados por estado y nombre
     * @throws BadRequestException cuando la categoría no es válida
     */
    public List<Espacio> listAllSpaces(String category) {
        String normalized = TextUtils.normalizeKey(category);
        if (normalized != null) {
            validateCategory(normalized);
        }
        return spaceRepository.findAll(normalized);
    }

    /**
     * Actualiza un espacio, esté activo o no, verificando la unicidad de su código. El campo
     * activo de la solicitud permite ocultar o volver a mostrar el espacio.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @param request nuevos datos del espacio
     * @return espacio actualizado
     * @throws NotFoundException cuando el espacio no existe
     * @throws BadRequestException cuando la categoría no es válida
     * @throws ConflictException cuando el código ya está en uso por otro espacio
     */
    @Transactional
    public Espacio updateSpace(UUID id, SpaceRequest request) {
        Espacio current = spaceRepository.findById(id).orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
        Espacio candidate = toEspacio(id, request, current.activo());
        ensureUniqueCode(candidate.codigo(), id);
        return spaceRepository.update(candidate);
    }

    /**
     * Desactiva lógicamente un espacio.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @throws NotFoundException cuando el espacio no existe o ya está inactivo
     */
    public void deleteSpace(UUID id) {
        if (spaceRepository.deactivate(id) == 0) {
            throw new NotFoundException("Espacio no encontrado");
        }
    }

    private Espacio toEspacio(UUID id, SpaceRequest request, boolean currentActive) {
        String categoria = TextUtils.normalizeKey(request.categoria());
        validateCategory(categoria);
        return new Espacio(
                id,
                request.nombre().trim(),
                request.codigo().trim().toUpperCase(Locale.ROOT),
                categoria,
                request.edificio(),
                request.piso(),
                request.descripcion(),
                request.referencia(),
                null,
                null,
                request.activo() == null ? currentActive : request.activo(),
                null);
    }

    private void validateCategory(String categoria) {
        if (!SPACE_CATEGORIES.contains(categoria)) {
            throw new BadRequestException(
                    "Categoría inválida. Valores permitidos: " + String.join(", ", SPACE_CATEGORIES));
        }
    }

    private void ensureUniqueCode(String codigo, UUID excludeId) {
        if (spaceRepository.existsByCodigo(codigo, excludeId)) {
            throw new ConflictException("Ya existe un espacio con el código " + codigo);
        }
    }
}
