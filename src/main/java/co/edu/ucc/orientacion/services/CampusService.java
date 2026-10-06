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

@Service
public class CampusService {

    public static final int MIN_SEARCH_LENGTH = 2;

    private static final List<String> SPACE_CATEGORIES =
            List.of("AULA", "LABORATORIO", "OFICINA", "BIBLIOTECA", "CAFETERIA", "AREA_COMUN");

    private final SpaceRepository spaceRepository;

    public CampusService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public List<Espacio> getSpaces(String category) {
        String normalized = TextUtils.normalizeKey(category);
        if (normalized != null) {
            validateCategory(normalized);
        }
        return spaceRepository.findActive(normalized);
    }

    public List<Espacio> searchSpaces(String query) {
        if (query == null || query.trim().length() < MIN_SEARCH_LENGTH) {
            throw new BadRequestException(
                    "La búsqueda requiere mínimo " + MIN_SEARCH_LENGTH + " caracteres");
        }
        return spaceRepository.search(TextUtils.likePattern(query));
    }

    @Transactional
    public Espacio createSpace(SpaceRequest request) {
        Espacio candidate = toEspacio(null, request, true);
        ensureUniqueCode(candidate.codigo(), null);
        return spaceRepository.create(candidate);
    }

    public List<Espacio> listAllSpaces(String category) {
        String normalized = TextUtils.normalizeKey(category);
        if (normalized != null) {
            validateCategory(normalized);
        }
        return spaceRepository.findAll(normalized);
    }

    @Transactional
    public Espacio updateSpace(UUID id, SpaceRequest request) {
        Espacio current = spaceRepository.findById(id).orElseThrow(() -> new NotFoundException("Espacio no encontrado"));
        Espacio candidate = toEspacio(id, request, current.activo());
        ensureUniqueCode(candidate.codigo(), id);
        return spaceRepository.update(candidate);
    }

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
