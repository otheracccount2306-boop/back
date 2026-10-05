package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Servicio;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos de servicios institucionales: bienestar y directorio de dependencias.
 *
 * @author Gabriela Zabaleta
 */
@Repository
public class ServiceRepository {

    private static final RowMapper<Servicio> SERVICIO_MAPPER = (rs, i) -> new Servicio(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getString("categoria"),
            rs.getString("edificio"),
            rs.getString("horario"),
            rs.getString("contacto"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Gabriela Zabaleta
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public ServiceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista los servicios activos que pertenecen a un conjunto de categorías, con filtros
     * opcionales por categoría exacta y por nombre.
     *
     * @author Gabriela Zabaleta
     * @param categorias categorías que definen el tipo de servicio
     * @param categoria categoría exacta normalizada, o null para no filtrar
     * @param pattern patrón de búsqueda sobre el nombre, o null para no filtrar; la búsqueda ignora tildes y mayúsculas
     * @return servicios ordenados por nombre
     */
    public List<Servicio> search(Collection<String> categorias, String categoria, String pattern) {
        return jdbc.query("""
                SELECT * FROM servicio
                WHERE activo = TRUE
                  AND categoria IN (:categorias)
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:pattern AS VARCHAR) IS NULL
                       OR unaccent(nombre) ILIKE unaccent(CAST(:pattern AS TEXT)))
                ORDER BY nombre
                """,
                new MapSqlParameterSource()
                        .addValue("categorias", categorias)
                        .addValue("categoria", categoria)
                        .addValue("pattern", pattern),
                SERVICIO_MAPPER);
    }

    /**
     * Busca un servicio activo por su identificador.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del servicio
     * @return servicio encontrado o vacío
     */
    public Optional<Servicio> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM servicio WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), SERVICIO_MAPPER).stream().findFirst();
    }

    /**
     * Busca un servicio por su identificador sin importar si está activo.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del servicio
     * @return servicio encontrado o vacío
     */
    public Optional<Servicio> findById(UUID id) {
        return jdbc.query("SELECT * FROM servicio WHERE id = :id",
                new MapSqlParameterSource("id", id), SERVICIO_MAPPER).stream().findFirst();
    }

    /**
     * Lista los servicios de un conjunto de categorías incluyendo los inactivos, para la
     * administración.
     *
     * @author Gabriela Zabaleta
     * @param categorias categorías que definen el tipo de servicio
     * @param categoria categoría exacta normalizada, o null para no filtrar
     * @return servicios ordenados por estado y nombre
     */
    public List<Servicio> findAll(Collection<String> categorias, String categoria) {
        return jdbc.query("""
                SELECT * FROM servicio
                WHERE categoria IN (:categorias)
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY activo DESC, nombre
                """,
                new MapSqlParameterSource().addValue("categorias", categorias).addValue("categoria", categoria),
                SERVICIO_MAPPER);
    }

    /**
     * Inserta un servicio. Se ignoran el identificador y la fecha de creación del modelo recibido.
     *
     * @author Gabriela Zabaleta
     * @param servicio datos del servicio a crear, incluido su estado activo
     * @return servicio creado
     */
    public Servicio create(Servicio servicio) {
        return jdbc.queryForObject("""
                INSERT INTO servicio (nombre, descripcion, categoria, edificio, horario, contacto, activo)
                VALUES (:nombre, :descripcion, :categoria, :edificio, :horario, :contacto, :activo)
                RETURNING *
                """, params(servicio), SERVICIO_MAPPER);
    }

    /**
     * Actualiza un servicio, activo o no, incluido su estado de visibilidad.
     *
     * @author Gabriela Zabaleta
     * @param servicio servicio con los nuevos datos, identificado por su id
     * @return servicio actualizado
     */
    public Servicio update(Servicio servicio) {
        return jdbc.queryForObject("""
                UPDATE servicio
                SET nombre = :nombre, descripcion = :descripcion, categoria = :categoria,
                    edificio = :edificio, horario = :horario, contacto = :contacto, activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(servicio).addValue("id", servicio.id()), SERVICIO_MAPPER);
    }

    /**
     * Desactiva lógicamente un servicio.
     *
     * @author Gabriela Zabaleta
     * @param id identificador del servicio
     * @return número de filas afectadas
     */
    public int deactivate(UUID id) {
        return jdbc.update("UPDATE servicio SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(Servicio s) {
        return new MapSqlParameterSource()
                .addValue("nombre", s.nombre())
                .addValue("descripcion", s.descripcion())
                .addValue("categoria", s.categoria())
                .addValue("edificio", s.edificio())
                .addValue("horario", s.horario())
                .addValue("contacto", s.contacto())
                .addValue("activo", s.activo());
    }
}
