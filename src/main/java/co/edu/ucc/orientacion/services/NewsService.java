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

    public NewsService(NewsRepository newsRepository, EventRepository eventRepository) {
        this.newsRepository = newsRepository;
        this.eventRepository = eventRepository;
    }

    public PageResponse<NewsSummaryResponse> listNews(String category, int page) {
        validatePage(page);
        String categoria = TextUtils.normalizeKey(category);
        List<NewsSummaryResponse> content = newsRepository
                .findPublished(categoria, PAGE_SIZE, (page - 1) * PAGE_SIZE).stream()
                .map(NewsSummaryResponse::from)
                .toList();
        return PageResponse.of(content, page, PAGE_SIZE, newsRepository.countPublished(categoria));
    }

    public Noticia getNews(UUID id, boolean includeDrafts) {
        return newsRepository.findById(id)
                .filter(noticia -> PUBLISHED.equals(noticia.estado())
                        || (includeDrafts && DRAFT.equals(noticia.estado())))
                .orElseThrow(() -> new NotFoundException("Noticia no encontrada"));
    }

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

    public Noticia createNews(UUID authorId, NewsRequest request) {
        String estado = request.estado() == null ? DRAFT : request.estado().toUpperCase(Locale.ROOT);
        LocalDateTime publishedAt = PUBLISHED.equals(estado) ? LocalDateTime.now() : null;
        return newsRepository.create(toNoticia(null, authorId, estado, publishedAt, request));
    }

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

    public void deleteNews(UUID id) {
        if (newsRepository.archive(id, LocalDateTime.now()) == 0) {
            throw new NotFoundException("Noticia no encontrada");
        }
    }

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

    public Evento createEvent(UUID authorId, EventRequest request) {
        ensureNotPast(request.fechaHora());
        return eventRepository.create(toEvento(null, authorId, ACTIVE, request));
    }

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

    public void deleteEvent(UUID id) {
        if (eventRepository.cancel(id) == 0) {
            throw new NotFoundException("Evento activo no encontrado");
        }
    }

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
