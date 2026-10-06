package co.edu.ucc.orientacion.dto.response;

import co.edu.ucc.orientacion.models.Noticia;

import java.time.LocalDateTime;
import java.util.UUID;

public record NewsSummaryResponse(
        UUID id,
        String titulo,
        String resumen,
        String categoria,
        String imagenUrl,
        LocalDateTime publicadoEn) {

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
