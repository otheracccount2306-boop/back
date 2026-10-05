package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Noticia;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista resumida de una noticia para listados, sin el contenido completo.
 *
 * @author Gabriela Zabaleta
 * @param id identificador único UUID
 * @param titulo título de la noticia
 * @param resumen resumen corto
 * @param categoria categoría de la noticia
 * @param imagenUrl URL de la imagen asociada
 * @param publicadoEn fecha de publicación
 */
public record NewsSummaryResponse(
        UUID id,
        String titulo,
        String resumen,
        String categoria,
        String imagenUrl,
        LocalDateTime publicadoEn) {

    /**
     * Construye la vista resumida a partir de la entidad de noticia.
     *
     * @author Gabriela Zabaleta
     * @param noticia entidad de noticia
     * @return DTO resumido
     */
    public static NewsSummaryResponse from(Noticia noticia) {
        return new NewsSummaryResponse(
                noticia.id(),
                noticia.titulo(),
                noticia.resumen(),
                noticia.categoria(),
                noticia.imagenUrl(),
                noticia.publicadoEn());
    }
}
