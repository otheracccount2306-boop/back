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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la lógica del módulo de noticias y eventos.
 *
 * @author Gabriela Zabaleta
 */
@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private EventRepository eventRepository;

    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsService = new NewsService(newsRepository, eventRepository);
    }

    private Noticia noticia(UUID id, String estado, LocalDateTime publicadoEn) {
        return new Noticia(id, "Título", "Resumen", "Contenido", "GENERAL", null, estado, publicadoEn,
                UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
    }

    private Evento evento(UUID id, String estado) {
        return new Evento(id, "Feria", null, "CULTURA", "Auditorio", LocalDateTime.now().plusDays(5), 100, estado,
                UUID.randomUUID(), LocalDateTime.now());
    }

    private NewsRequest newsRequest(String estado) {
        return new NewsRequest("Nueva noticia", "Resumen", "Contenido", "General", null, estado);
    }

    private EventRequest eventRequest(LocalDateTime fechaHora, String estado) {
        return new EventRequest("Feria de servicios", null, "Cultura", "Auditorio", fechaHora, 50, estado);
    }

    @Test
    @DisplayName("Listar noticias con página menor que 1 responde 400")
    void listNewsWithInvalidPageFails() {
        assertThrows(BadRequestException.class, () -> newsService.listNews(null, 0));
    }

    @Test
    @DisplayName("Listar noticias pagina de a 10 y calcula el total de páginas")
    void listNewsPaginates() {
        when(newsRepository.findPublished("GENERAL", 10, 10))
                .thenReturn(List.of(noticia(UUID.randomUUID(), "PUBLICADO", LocalDateTime.now())));
        when(newsRepository.countPublished("GENERAL")).thenReturn(21L);

        PageResponse<NewsSummaryResponse> page = newsService.listNews("General", 2);

        assertEquals(2, page.page());
        assertEquals(10, page.size());
        assertEquals(21L, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(1, page.content().size());
    }

    @Test
    @DisplayName("Un borrador no es visible para consultas públicas")
    void draftIsHiddenFromPublic() {
        UUID id = UUID.randomUUID();
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "BORRADOR", null)));

        assertThrows(NotFoundException.class, () -> newsService.getNews(id, false));
    }

    @Test
    @DisplayName("Un borrador es visible para un administrador")
    void draftIsVisibleToAdmin() {
        UUID id = UUID.randomUUID();
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "BORRADOR", null)));

        assertEquals(id, newsService.getNews(id, true).id());
    }

    @Test
    @DisplayName("Una noticia archivada no es visible ni para un administrador")
    void archivedIsHiddenFromEveryone() {
        UUID id = UUID.randomUUID();
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "ARCHIVADO", null)));

        assertThrows(NotFoundException.class, () -> newsService.getNews(id, true));
    }

    @Test
    @DisplayName("Crear una noticia sin estado la deja como borrador sin fecha de publicación")
    void createNewsDefaultsToDraft() {
        when(newsRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Noticia> captor = ArgumentCaptor.forClass(Noticia.class);
        UUID author = UUID.randomUUID();

        newsService.createNews(author, newsRequest(null));

        verify(newsRepository).create(captor.capture());
        assertEquals("BORRADOR", captor.getValue().estado());
        assertNull(captor.getValue().publicadoEn());
        assertEquals(author, captor.getValue().creadoPor());
        assertEquals("GENERAL", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Crear una noticia publicada asigna la fecha de publicación")
    void createPublishedNewsSetsPublicationDate() {
        when(newsRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Noticia> captor = ArgumentCaptor.forClass(Noticia.class);

        newsService.createNews(UUID.randomUUID(), newsRequest("publicado"));

        verify(newsRepository).create(captor.capture());
        assertEquals("PUBLICADO", captor.getValue().estado());
        assertNotNull(captor.getValue().publicadoEn());
    }

    @Test
    @DisplayName("Publicar un borrador asigna la fecha de publicación")
    void publishingDraftSetsPublicationDate() {
        UUID id = UUID.randomUUID();
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "BORRADOR", null)));
        when(newsRepository.update(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Noticia> captor = ArgumentCaptor.forClass(Noticia.class);

        newsService.updateNews(id, newsRequest("PUBLICADO"));

        verify(newsRepository).update(captor.capture(), any(LocalDateTime.class));
        assertEquals("PUBLICADO", captor.getValue().estado());
        assertNotNull(captor.getValue().publicadoEn());
    }

    @Test
    @DisplayName("Devolver a borrador una noticia publicada responde 409")
    void unpublishingIsRejected() {
        UUID id = UUID.randomUUID();
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "PUBLICADO", LocalDateTime.now())));

        assertThrows(ConflictException.class, () -> newsService.updateNews(id, newsRequest("BORRADOR")));

        verify(newsRepository, never()).update(any(), any());
    }

    @Test
    @DisplayName("Editar una noticia publicada sin indicar estado conserva su fecha de publicación")
    void editingPublishedKeepsPublicationDate() {
        UUID id = UUID.randomUUID();
        LocalDateTime original = LocalDateTime.now().minusDays(3);
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "PUBLICADO", original)));
        when(newsRepository.update(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Noticia> captor = ArgumentCaptor.forClass(Noticia.class);

        newsService.updateNews(id, newsRequest(null));

        verify(newsRepository).update(captor.capture(), any());
        assertEquals("PUBLICADO", captor.getValue().estado());
        assertEquals(original, captor.getValue().publicadoEn());
    }

    @Test
    @DisplayName("Actualizar una noticia archivada responde 404")
    void updatingArchivedNewsFails() {
        UUID id = UUID.randomUUID();
        when(newsRepository.findById(id)).thenReturn(Optional.of(noticia(id, "ARCHIVADO", null)));

        assertThrows(NotFoundException.class, () -> newsService.updateNews(id, newsRequest(null)));
    }

    @Test
    @DisplayName("Eliminar una noticia inexistente responde 404")
    void deletingMissingNewsFails() {
        UUID id = UUID.randomUUID();
        when(newsRepository.archive(eq(id), any())).thenReturn(0);

        assertThrows(NotFoundException.class, () -> newsService.deleteNews(id));
    }

    @Test
    @DisplayName("Listar eventos concluye primero los eventos pasados y usa hoy como límite inferior")
    void listEventsConcludesPastEventsFirst() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        when(eventRepository.findUpcoming(isNull(), eq(startOfToday), isNull(), eq(10), eq(0))).thenReturn(List.of());
        when(eventRepository.countUpcoming(isNull(), eq(startOfToday), isNull())).thenReturn(0L);

        PageResponse<Evento> page = newsService.listEvents(null, LocalDate.now().minusDays(30), null, 1);

        verify(eventRepository).concludePast(startOfToday);
        assertEquals(0, page.totalPages());
    }

    @Test
    @DisplayName("Listar eventos convierte la fecha final en un límite exclusivo del día siguiente")
    void listEventsUsesExclusiveUpperBound() {
        LocalDate from = LocalDate.now().plusDays(2);
        LocalDate to = LocalDate.now().plusDays(4);
        when(eventRepository.findUpcoming(eq("CULTURA"), eq(from.atStartOfDay()), eq(to.plusDays(1).atStartOfDay()),
                eq(10), eq(10))).thenReturn(List.of());
        when(eventRepository.countUpcoming(eq("CULTURA"), eq(from.atStartOfDay()), eq(to.plusDays(1).atStartOfDay())))
                .thenReturn(0L);

        newsService.listEvents("Cultura", from, to, 2);

        verify(eventRepository).findUpcoming("CULTURA", from.atStartOfDay(), to.plusDays(1).atStartOfDay(), 10, 10);
    }

    @Test
    @DisplayName("Listar eventos con un rango de fechas invertido responde 400")
    void listEventsWithInvertedRangeFails() {
        assertThrows(BadRequestException.class, () -> newsService.listEvents(
                null, LocalDate.now().plusDays(5), LocalDate.now().plusDays(1), 1));
    }

    @Test
    @DisplayName("Crear un evento con fecha pasada responde 422")
    void createEventInThePastFails() {
        assertThrows(UnprocessableEntityException.class, () -> newsService.createEvent(
                UUID.randomUUID(), eventRequest(LocalDateTime.now().minusDays(1), null)));

        verify(eventRepository, never()).create(any());
    }

    @Test
    @DisplayName("Crear un evento para hoy es válido y queda ACTIVO")
    void createEventForTodayIsAllowed() {
        when(eventRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);

        newsService.createEvent(UUID.randomUUID(), eventRequest(LocalDate.now().atTime(23, 59), null));

        verify(eventRepository).create(captor.capture());
        assertEquals("ACTIVO", captor.getValue().estado());
        assertEquals("CULTURA", captor.getValue().categoria());
    }

    @Test
    @DisplayName("Modificar un evento concluido responde 409")
    void updatingConcludedEventFails() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(evento(id, "CONCLUIDO")));

        assertThrows(ConflictException.class,
                () -> newsService.updateEvent(id, eventRequest(LocalDateTime.now().plusDays(3), null)));
    }

    @Test
    @DisplayName("Actualizar un evento inexistente responde 404")
    void updatingMissingEventFails() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> newsService.updateEvent(id, eventRequest(LocalDateTime.now().plusDays(3), null)));
    }

    @Test
    @DisplayName("Cancelar un evento puede hacerse aunque su fecha ya haya pasado")
    void cancellingEventDoesNotRequireFutureDate() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.of(evento(id, "ACTIVO")));
        when(eventRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);

        newsService.updateEvent(id, eventRequest(LocalDateTime.now().minusDays(2), "cancelado"));

        verify(eventRepository).update(captor.capture());
        assertEquals("CANCELADO", captor.getValue().estado());
    }

    @Test
    @DisplayName("Eliminar un evento sin estado activo responde 404")
    void deletingNonActiveEventFails() {
        UUID id = UUID.randomUUID();
        when(eventRepository.cancel(id)).thenReturn(0);

        assertThrows(NotFoundException.class, () -> newsService.deleteEvent(id));
    }
}
