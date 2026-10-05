package co.edu.ucc.orientacion.services;

import co.edu.ucc.orientacion.dto.request.EventRequest;
import co.edu.ucc.orientacion.dto.request.NewsRequest;
import co.edu.ucc.orientacion.dto.response.NewsSummaryResponse;
import co.edu.ucc.orientacion.dto.response.PageResponse;
import co.edu.ucc.orientacion.exceptions.BadRequestException;
import co.edu.ucc.orientacion.exceptions.ConflictException;
import co.edu.ucc.orientacion.exceptions.NotFoundException;
import co.edu.ucc.orientacion.exceptions.UnprocessableEntityException;
import co.edu.ucc.orientacion.models.Evento;
import co.edu.ucc.orientacion.models.Noticia;
import co.edu.ucc.orientacion.repositories.EventRepository;
import co.edu.ucc.orientacion.repositories.NewsRepository;
import co.edu.ucc.orientacion.utils.TextUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lógica de negocio de noticias y eventos institucionales, con su flujo de publicación.
 *
 * @author Gabriela Zabaleta
 */
@Service
public class NewsService {

    public static final int PAGE_SIZE = 10;

    private static final String DRAFT = "BORRADOR";
    private static final String PUBLISHED = "PUBLICADO";
    private static final String ARCHIVED = "ARCHIVADO";
    private static final String ACTIVE = "ACTIVO";
    private static final String CONCLUDED = "CONCLUIDO";

    private final NewsRepository newsRepository;
    private final EventRepository eventRepository;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @author Gabriela Zabaleta
     * @param newsRepository repositorio de noticias
     * @param eventRepository repositorio de eventos
     */
    public NewsService(NewsRepository newsRepository, EventRepository eventRepository) {
        this.newsRepository = newsRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Lista noticias publicadas de forma paginada, 10 por página.
     *
     * @author Gabriela Zabaleta
     * @param category categoría de la noticia; null para todas
     * @param page número de página, iniciando en 1
     * @return página de noticias resumidas, de la más reciente a la más antigua
     * @throws BadRequestException cuando la página es menor que 1
     */
    public PageResponse<NewsSummaryResponse> listNews(String category, int page) {
        validatePage(page);
        String categoria = TextUtils.normalizeKey(category);
        List<NewsSummaryResponse> content = newsRepository
                .findPublished(categoria, PAGE_SIZE, (page - 1) * PAGE_SIZE).stream()
                .map(NewsSummaryResponse::from)
                .toList();
        return PageResponse.of(content, page, PAGE_SIZE, newsRepository.countPublished(categoria));
    }

    /**
     * Obtiene una noticia. Las consultas públicas solo ven noticias publicadas; los
     * administradores también ven borradores.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @param includeDrafts true si el solicitante es administrador
     * @return noticia encontrada
     * @throws NotFoundException cuando la noticia no existe o no es visible para el solicitante
     */
    public Noticia getNews(UUID id, boolean includeDrafts) {
        return newsRepository.findById(id)
                .filter(noticia -> PUBLISHED.equals(noticia.estado())
                        || (includeDrafts && DRAFT.equals(noticia.estado())))
                .orElseThrow(() -> new NotFoundException("Noticia no encontrada"));
    }

    /**
     * Lista noticias de cualquier estado, incluidos borradores y archivadas, para la
     * administración. Se pagina de a 10.
     *
     * @author Gabriela Zabaleta
     * @param category categoría de la noticia; null para todas
     * @param status BORRADOR, PUBLICADO o ARCHIVADO; null para todos los estados
     * @param page número de página, iniciando en 1
     * @return página de noticias completas, de la más reciente a la más antigua
     * @throws BadRequestException cuando la página o el estado son inválidos
     */
    public PageResponse<Noticia> listNewsAdmin(String category, String status, int page) {
        validatePage(page);
        String estado = TextUtils.normalizeKey(status);
        if (estado != null && !List.of(DRAFT, PUBLISHED, ARCHIVED).contains(estado)) {
            throw new BadRequestException("Estado inválido. Valores permitidos: BORRADOR, PUBLICADO, ARCHIVADO");
        }
        String categoria = TextUtils.normalizeKey(category);
        return PageResponse.of(
                newsRepository.findAllAdmin(categoria, estado, PAGE_SIZE, (page - 1) * PAGE_SIZE),
                page,
                PAGE_SIZE,
                newsRepository.countAllAdmin(categoria, estado));
    }

    /**
     * Crea una noticia como borrador o directamente publicada.
     *
     * @author Gabriela Zabaleta
     * @param authorId administrador que crea la noticia
     * @param request datos de la noticia
     * @return noticia creada
     */
    public Noticia createNews(UUID authorId, NewsRequest request) {
        String estado = request.estado() == null ? DRAFT : request.estado().toUpperCase(Locale.ROOT);
        LocalDateTime publishedAt = PUBLISHED.equals(estado) ? LocalDateTime.now() : null;
        return newsRepository.create(toNoticia(null, authorId, estado, publishedAt, request));
    }

    /**
     * Actualiza una noticia siguiendo el flujo BORRADOR a PUBLICADO. Una noticia publicada no
     * puede volver a borrador.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @param request nuevos datos de la noticia
     * @return noticia actualizada
     * @throws NotFoundException cuando la noticia no existe o está archivada
     * @throws ConflictException cuando se intenta devolver a borrador una noticia publicada
     */
    @Transactional
    public Noticia updateNews(UUID id, NewsRequest request) {
        Noticia current = newsRepository.findById(id)
                .filter(noticia -> !ARCHIVED.equals(noticia.estado()))
                .orElseThrow(() -> new NotFoundException("Noticia no encontrada"));
        String target = request.estado() == null ? current.estado() : request.estado().toUpperCase(Locale.ROOT);
        if (PUBLISHED.equals(current.estado()) && DRAFT.equals(target)) {
            throw new ConflictException("Una noticia publicada no puede volver a borrador");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = current.publicadoEn() != null
                ? current.publicadoEn()
                : (PUBLISHED.equals(target) ? now : null);
        return newsRepository.update(toNoticia(id, current.creadoPor(), target, publishedAt, request), now);
    }

    /**
     * Archiva lógicamente una noticia.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @throws NotFoundException cuando la noticia no existe o ya está archivada
     */
    public void deleteNews(UUID id) {
        if (newsRepository.archive(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("Noticia no encontrada");
        }
    }

    /**
     * Lista eventos activos cuya fecha es hoy o futura, de forma paginada, 10 por página.
     * Antes de consultar marca como CONCLUIDO los eventos pasados.
     *
     * @author Gabriela Zabaleta
     * @param category categoría del evento; null para todas
     * @param from fecha mínima; si es anterior a hoy se usa hoy
     * @param to fecha máxima inclusiva; null para no acotar
     * @param page número de página, iniciando en 1
     * @return página de eventos ordenados por fecha
     * @throws BadRequestException cuando la página es menor que 1 o el rango de fechas es inválido
     */
    public PageResponse<Evento> listEvents(String category, LocalDate from, LocalDate to, int page) {
        validatePage(page);
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("La fecha 'to' no puede ser anterior a la fecha 'from'");
        }
        concludePastEvents();
        LocalDate today = LocalDate.now();
        LocalDateTime lowerBound = (from == null || from.isBefore(today) ? today : from).atStartOfDay();
        LocalDateTime upperBound = to == null ? null : to.plusDays(1).atStartOfDay();
        String categoria = TextUtils.normalizeKey(category);
        List<Evento> content = eventRepository.findUpcoming(
                categoria, lowerBound, upperBound, PAGE_SIZE, (page - 1) * PAGE_SIZE);
        return PageResponse.of(content, page, PAGE_SIZE, eventRepository.countUpcoming(categoria, lowerBound, upperBound));
    }

    /**
     * Lista eventos de cualquier estado y fecha, incluidos concluidos y cancelados, para la
     * administración. Antes de consultar marca como CONCLUIDO los eventos pasados.
     *
     * @author Gabriela Zabaleta
     * @param category categoría del evento; null para todas
     * @param status ACTIVO, CONCLUIDO o CANCELADO; null para todos los estados
     * @param page número de página, iniciando en 1
     * @return página de eventos, del más reciente al más antiguo
     * @throws BadRequestException cuando la página o el estado son inválidos
     */
    public PageResponse<Evento> listEventsAdmin(String category, String status, int page) {
        validatePage(page);
        String estado = TextUtils.normalizeKey(status);
        if (estado != null && !List.of(ACTIVE, CONCLUDED, "CANCELADO").contains(estado)) {
            throw new BadRequestException("Estado inválido. Valores permitidos: ACTIVO, CONCLUIDO, CANCELADO");
        }
        concludePastEvents();
        String categoria = TextUtils.normalizeKey(category);
        return PageResponse.of(
                eventRepository.findAllAdmin(categoria, estado, PAGE_SIZE, (page - 1) * PAGE_SIZE),
                page,
                PAGE_SIZE,
                eventRepository.countAllAdmin(categoria, estado));
    }

    /**
     * Crea un evento activo.
     *
     * @author Gabriela Zabaleta
     * @param authorId administrador que crea el evento
     * @param request datos del evento
     * @return evento creado
     * @throws UnprocessableEntityException cuando la fecha del evento es anterior a hoy
     */
    public Evento createEvent(UUID authorId, EventRequest request) {
        ensureNotPast(request.fechaHora());
        return eventRepository.create(toEvento(null, authorId, ACTIVE, request));
    }

    /**
     * Actualiza un evento. Un evento concluido no puede modificarse; uno cancelado puede
     * reactivarse con una fecha vigente.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del evento
     * @param request nuevos datos del evento
     * @return evento actualizado
     * @throws NotFoundException cuando el evento no existe
     * @throws ConflictException cuando el evento ya concluyó
     * @throws UnprocessableEntityException cuando un evento activo tiene fecha anterior a hoy
     */
    @Transactional
    public Evento updateEvent(UUID id, EventRequest request) {
        Evento current = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evento no encontrado"));
        if (CONCLUDED.equals(current.estado())) {
            throw new ConflictException("Un evento concluido no puede modificarse");
        }
        String target = request.estado() == null ? current.estado() : request.estado().toUpperCase(Locale.ROOT);
        if (ACTIVE.equals(target)) {
            ensureNotPast(request.fechaHora());
        }
        return eventRepository.update(toEvento(id, current.creadoPor(), target, request));
    }

    /**
     * Cancela lógicamente un evento activo. Los eventos concluidos no se modifican.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del evento
     * @throws NotFoundException cuando no existe un evento activo con ese identificador
     */
    public void deleteEvent(UUID id) {
        if (eventRepository.cancel(id) == 0) {
            throw new NotFoundException("Evento activo no encontrado");
        }
    }

    /**
     * Marca como CONCLUIDO los eventos activos con fecha anterior a hoy. Se ejecuta a diario
     * y antes de cada consulta pública de eventos. Los eventos nunca se eliminan.
     *
     * @author Gabriela Zabaleta
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void concludePastEvents() {
        eventRepository.concludePast(LocalDate.now().atStartOfDay());
    }

    private void validatePage(int page) {
        if (page < 1) {
            throw new BadRequestException("La página debe ser mayor o igual a 1");
        }
    }

    private void ensureNotPast(LocalDateTime fechaHora) {
        if (fechaHora.toLocalDate().isBefore(LocalDate.now())) {
            throw new UnprocessableEntityException("La fecha del evento no puede ser anterior a hoy");
        }
    }

    private Noticia toNoticia(UUID id, UUID authorId, String estado, LocalDateTime publishedAt, NewsRequest request) {
        return new Noticia(
                id,
                request.titulo().trim(),
                request.resumen(),
                request.contenido(),
                TextUtils.normalizeKey(request.categoria()),
                request.imagenUrl(),
                estado,
                publishedAt,
                authorId,
                null,
                null);
    }

    private Evento toEvento(UUID id, UUID authorId, String estado, EventRequest request) {
        return new Evento(
                id,
                request.nombre().trim(),
                request.descripcion(),
                TextUtils.normalizeKey(request.categoria()),
                request.lugar(),
                request.fechaHora(),
                request.cupos(),
                estado,
                authorId,
                null);
    }
}
