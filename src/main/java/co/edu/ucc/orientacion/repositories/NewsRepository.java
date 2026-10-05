package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Noticia;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos de noticias institucionales.
 *
 * @author Gabriela Zabaleta
 */
@Repository
public class NewsRepository {

    private static final RowMapper<Noticia> NOTICIA_MAPPER = (rs, i) -> new Noticia(
            rs.getObject("id", UUID.class),
            rs.getString("titulo"),
            rs.getString("resumen"),
            rs.getString("contenido"),
            rs.getString("categoria"),
            rs.getString("imagen_url"),
            rs.getString("estado"),
            rs.getObject("publicado_en", LocalDateTime.class),
            rs.getObject("creado_por", UUID.class),
            rs.getObject("creado_en", LocalDateTime.class),
            rs.getObject("actualizado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Gabriela Zabaleta
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public NewsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista noticias en estado PUBLICADO, de la más reciente a la más antigua.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param limit cantidad máxima de resultados
     * @param offset desplazamiento inicial
     * @return noticias de la página solicitada
     */
    public List<Noticia> findPublished(String categoria, int limit, int offset) {
        return jdbc.query("""
                SELECT * FROM noticia
                WHERE estado = 'PUBLICADO'
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY publicado_en DESC, id
                LIMIT :limit OFFSET :offset
                """,
                new MapSqlParameterSource()
                        .addValue("categoria", categoria)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                NOTICIA_MAPPER);
    }

    /**
     * Cuenta las noticias en estado PUBLICADO.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @return total de noticias publicadas
     */
    public long countPublished(String categoria) {
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM noticia
                WHERE estado = 'PUBLICADO'
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                """,
                new MapSqlParameterSource("categoria", categoria), Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Lista noticias de cualquier estado para la administración, de la más reciente a la más antigua.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param estado BORRADOR, PUBLICADO o ARCHIVADO; null para todos los estados
     * @param limit cantidad máxima de resultados
     * @param offset desplazamiento inicial
     * @return noticias de la página solicitada
     */
    public List<Noticia> findAllAdmin(String categoria, String estado, int limit, int offset) {
        return jdbc.query("""
                SELECT * FROM noticia
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:estado AS VARCHAR) IS NULL OR estado = :estado)
                ORDER BY COALESCE(publicado_en, creado_en) DESC, id
                LIMIT :limit OFFSET :offset
                """,
                new MapSqlParameterSource()
                        .addValue("categoria", categoria)
                        .addValue("estado", estado)
                        .addValue("limit", limit)
                        .addValue("offset", offset),
                NOTICIA_MAPPER);
    }

    /**
     * Cuenta las noticias de cualquier estado para la administración.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param estado BORRADOR, PUBLICADO o ARCHIVADO; null para todos los estados
     * @return total de noticias que cumplen el filtro
     */
    public long countAllAdmin(String categoria, String estado) {
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM noticia
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:estado AS VARCHAR) IS NULL OR estado = :estado)
                """,
                new MapSqlParameterSource().addValue("categoria", categoria).addValue("estado", estado),
                Long.class);
        return total == null ? 0 : total;
    }

    /**
     * Busca una noticia por su identificador sin importar su estado.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @return noticia encontrada o vacío
     */
    public Optional<Noticia> findById(UUID id) {
        return jdbc.query("SELECT * FROM noticia WHERE id = :id",
                new MapSqlParameterSource("id", id), NOTICIA_MAPPER).stream().findFirst();
    }

    /**
     * Inserta una noticia con los datos del modelo recibido.
     *
     * @author Gabriela Zabaleta
     * @param noticia datos de la noticia a crear, incluidos estado, fecha de publicación y autor
     * @return noticia creada
     */
    public Noticia create(Noticia noticia) {
        return jdbc.queryForObject("""
                INSERT INTO noticia (titulo, resumen, contenido, categoria, imagen_url, estado, publicado_en, creado_por)
                VALUES (:titulo, :resumen, :contenido, :categoria, :imagenUrl, :estado, :publicadoEn, :creadoPor)
                RETURNING *
                """, params(noticia).addValue("creadoPor", noticia.creadoPor()), NOTICIA_MAPPER);
    }

    /**
     * Actualiza el contenido, el estado y la fecha de publicación de una noticia.
     *
     * @author Gabriela Zabaleta
     * @param noticia noticia con los nuevos datos, identificada por su id
     * @param now fecha de actualización
     * @return noticia actualizada
     */
    public Noticia update(Noticia noticia, LocalDateTime now) {
        return jdbc.queryForObject("""
                UPDATE noticia
                SET titulo = :titulo, resumen = :resumen, contenido = :contenido, categoria = :categoria,
                    imagen_url = :imagenUrl, estado = :estado, publicado_en = :publicadoEn, actualizado_en = :now
                WHERE id = :id
                RETURNING *
                """, params(noticia).addValue("id", noticia.id()).addValue("now", now), NOTICIA_MAPPER);
    }

    /**
     * Archiva lógicamente una noticia cambiando su estado a ARCHIVADO.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la noticia
     * @param now fecha de actualización
     * @return número de filas afectadas
     */
    public int archive(UUID id, LocalDateTime now) {
        return jdbc.update("""
                UPDATE noticia SET estado = 'ARCHIVADO', actualizado_en = :now
                WHERE id = :id AND estado <> 'ARCHIVADO'
                """,
                new MapSqlParameterSource().addValue("id", id).addValue("now", now));
    }

    private MapSqlParameterSource params(Noticia n) {
        return new MapSqlParameterSource()
                .addValue("titulo", n.titulo())
                .addValue("resumen", n.resumen())
                .addValue("contenido", n.contenido())
                .addValue("categoria", n.categoria())
                .addValue("imagenUrl", n.imagenUrl())
                .addValue("estado", n.estado())
                .addValue("publicadoEn", n.publicadoEn());
    }
}
