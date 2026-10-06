package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.FaqRequest;
import co.edu.ucc.orientacion.dto.request.ServiceRequest;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.models.Faq;
import co.edu.ucc.orientacion.models.Servicio;
import co.edu.ucc.orientacion.repositories.FaqRepository;
import co.edu.ucc.orientacion.repositories.ServiceRepository;
import co.edu.ucc.orientacion.utils.TextUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ServicesService {

    public static final String DEPARTMENT_CATEGORY = "DEPARTAMENTO";

    private static final List<String> WELLBEING_CATEGORIES =
            List.of("PSICOLOGIA", "SALUD", "DEPORTE", "CULTURA", "PASTORAL", "BECAS");

    private enum ServiceType { WELLBEING, DEPARTMENTS, FAQ }

    private final ServiceRepository serviceRepository;
    private final FaqRepository faqRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ServicesService(
            ServiceRepository serviceRepository,
            FaqRepository faqRepository,
            ObjectMapper objectMapper,
            Validator validator) {
        this.serviceRepository = serviceRepository;
        this.faqRepository = faqRepository;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public List<Servicio> getWellbeing(String category) {
        String normalized = TextUtils.normalizeKey(category);
        if (normalized != null) {
            validateWellbeingCategory(normalized);
        }
        return serviceRepository.search(WELLBEING_CATEGORIES, normalized, null);
    }

    public List<Servicio> getDepartments(String search) {
        return serviceRepository.search(List.of(DEPARTMENT_CATEGORY), null, TextUtils.likePattern(search));
    }

    public List<Faq> getFaq(String category, String search) {
        return faqRepository.search(TextUtils.normalizeKey(category), TextUtils.likePattern(search));
    }

    @Transactional
    public Object create(String type, JsonNode body) {
        ServiceType serviceType = resolveType(type);
        if (serviceType == ServiceType.FAQ) {
            return faqRepository.create(toFaq(null, parse(body, FaqRequest.class), true));
        }
        return serviceRepository.create(toServicio(null, serviceType, parse(body, ServiceRequest.class), true));
    }

    public List<?> listAll(String type, String category) {
        ServiceType serviceType = resolveType(type);
        String normalized = TextUtils.normalizeKey(category);
        return switch (serviceType) {
            case FAQ -> faqRepository.findAll(normalized);
            case DEPARTMENTS -> serviceRepository.findAll(List.of(DEPARTMENT_CATEGORY), null);
            case WELLBEING -> {
                if (normalized != null) {
                    validateWellbeingCategory(normalized);
                }
                yield serviceRepository.findAll(WELLBEING_CATEGORIES, normalized);
            }
        };
    }

    @Transactional
    public Object update(String type, UUID id, JsonNode body) {
        ServiceType serviceType = resolveType(type);
        if (serviceType == ServiceType.FAQ) {
            Faq current = faqRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Pregunta frecuente no encontrada"));
            return faqRepository.update(toFaq(id, parse(body, FaqRequest.class), current.activo()));
        }
        Servicio current = findServicio(serviceType, id, false);
        return serviceRepository.update(
                toServicio(id, serviceType, parse(body, ServiceRequest.class), current.activo()));
    }

    @Transactional
    public void delete(String type, UUID id) {
        ServiceType serviceType = resolveType(type);
        if (serviceType == ServiceType.FAQ) {
            if (faqRepository.deactivate(id) == 0) {
                throw new NotFoundException("Pregunta frecuente no encontrada");
            }
            return;
        }
        findServicio(serviceType, id, true);
        serviceRepository.deactivate(id);
    }

    private ServiceType resolveType(String type) {
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "wellbeing" -> ServiceType.WELLBEING;
            case "departments", "department" -> ServiceType.DEPARTMENTS;
            case "faq" -> ServiceType.FAQ;
            default -> throw new BadRequestException(
                    "Tipo de servicio inválido. Valores permitidos: wellbeing, departments, faq");
        };
    }

    private <T> T parse(JsonNode body, Class<T> type) {
        T value;
        try {
            value = objectMapper.treeToValue(body, type);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Cuerpo de la solicitud inválido");
        }
        if (value == null) {
            throw new BadRequestException("Cuerpo de la solicitud inválido");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(value);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return value;
    }

    private Servicio findServicio(ServiceType type, UUID id, boolean onlyActive) {
        Servicio servicio = (onlyActive ? serviceRepository.findActiveById(id) : serviceRepository.findById(id))
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        boolean isDepartment = DEPARTMENT_CATEGORY.equals(servicio.categoria());
        if (isDepartment != (type == ServiceType.DEPARTMENTS)) {
            throw new NotFoundException("Servicio no encontrado");
        }
        return servicio;
    }

    private Servicio toServicio(UUID id, ServiceType type, ServiceRequest request, boolean currentActive) {
        String categoria = DEPARTMENT_CATEGORY;
        if (type == ServiceType.WELLBEING) {
            categoria = TextUtils.normalizeKey(request.categoria());
            if (categoria == null) {
                throw new BadRequestException("La categoría es obligatoria para servicios de bienestar");
            }
            validateWellbeingCategory(categoria);
        }
        return new Servicio(
                id,
                request.nombre().trim(),
                request.descripcion(),
                categoria,
                request.edificio(),
                request.horario(),
                request.contacto(),
                request.activo() == null ? currentActive : request.activo(),
                null);
    }

    private Faq toFaq(UUID id, FaqRequest request, boolean currentActive) {
        return new Faq(
                id,
                request.pregunta().trim(),
                request.respuesta().trim(),
                TextUtils.normalizeKey(request.categoria()),
                0,
                request.activo() == null ? currentActive : request.activo(),
                null);
    }

    private void validateWellbeingCategory(String categoria) {
        if (!WELLBEING_CATEGORIES.contains(categoria)) {
            throw new BadRequestException(
                    "Categoría inválida. Valores permitidos: " + String.join(", ", WELLBEING_CATEGORIES));
        }
    }
}
