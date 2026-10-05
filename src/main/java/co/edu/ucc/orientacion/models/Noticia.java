package co.edu.ucc.orientacion.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Noticia institucional con flujo de publicación BORRADOR a PUBLICADO. El estado ARCHIVADO
 * representa la eliminación lógica.
 *
 * @author Gabriela Zabaleta
 * @param id identificador único UUID
 * @param titulo título de la noticia
 * @param resumen resumen corto
 * @param contenido contenido completo
 * @param categoria categoría de la noticia
 * @param imagenUrl URL de la imagen asociada
 * @param estado estado: BORRADOR, PUBLICADO o ARCHIVADO
 * @param publicadoEn fecha de publicación
 * @param creadoPor administrador autor de la noticia
 * @param creadoEn fecha de creación
 * @param actualizadoEn fecha de la última actualización
 */
public record Noticia(
        UUID id,
        String titulo,
        String resumen,
        String contenido,
        String categoria,
        String imagenUrl,
        String estado,
        LocalDateTime publicadoEn,
        UUID creadoPor,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn) {
}
