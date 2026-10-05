package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Faq;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos de preguntas frecuentes.
 *
 * @author Gabriela Zabaleta
 */
@Repository
public class FaqRepository {

    private static final RowMapper<Faq> FAQ_MAPPER = (rs, i) -> new Faq(
            rs.getObject("id", UUID.class),
            rs.getString("pregunta"),
            rs.getString("respuesta"),
            rs.getString("categoria"),
            rs.getInt("frecuencia"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Gabriela Zabaleta
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public FaqRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Busca preguntas frecuentes activas por categoría y palabra clave sobre la pregunta y la
     * respuesta. La búsqueda ignora tildes y mayúsculas.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @param pattern patrón de búsqueda, o null para no filtrar; la búsqueda ignora tildes y mayúsculas
     * @return preguntas ordenadas por frecuencia descendente
     */
    public List<Faq> search(String categoria, String pattern) {
        return jdbc.query("""
                SELECT * FROM faq
                WHERE activo = TRUE
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                  AND (CAST(:pattern AS VARCHAR) IS NULL
                       OR unaccent(pregunta) ILIKE unaccent(CAST(:pattern AS TEXT))
                       OR unaccent(respuesta) ILIKE unaccent(CAST(:pattern AS TEXT)))
                ORDER BY frecuencia DESC, pregunta
                """,
                new MapSqlParameterSource().addValue("categoria", categoria).addValue("pattern", pattern),
                FAQ_MAPPER);
    }

    /**
     * Busca una pregunta frecuente activa por su identificador.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la pregunta
     * @return pregunta encontrada o vacío
     */
    public Optional<Faq> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM faq WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), FAQ_MAPPER).stream().findFirst();
    }

    /**
     * Busca una pregunta frecuente por su identificador sin importar si está activa.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la pregunta
     * @return pregunta encontrada o vacío
     */
    public Optional<Faq> findById(UUID id) {
        return jdbc.query("SELECT * FROM faq WHERE id = :id",
                new MapSqlParameterSource("id", id), FAQ_MAPPER).stream().findFirst();
    }

    /**
     * Lista todas las preguntas frecuentes incluyendo las inactivas, para la administración.
     *
     * @author Gabriela Zabaleta
     * @param categoria categoría normalizada, o null para no filtrar
     * @return preguntas ordenadas por estado, frecuencia y texto
     */
    public List<Faq> findAll(String categoria) {
        return jdbc.query("""
                SELECT * FROM faq
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY activo DESC, frecuencia DESC, pregunta
                """,
                new MapSqlParameterSource("categoria", categoria), FAQ_MAPPER);
    }

    /**
     * Inserta una pregunta frecuente. Se ignoran el identificador, la frecuencia y la fecha de
     * creación del modelo recibido.
     *
     * @author Gabriela Zabaleta
     * @param faq datos de la pregunta a crear, incluido su estado activo
     * @return pregunta creada
     */
    public Faq create(Faq faq) {
        return jdbc.queryForObject("""
                INSERT INTO faq (pregunta, respuesta, categoria, activo)
                VALUES (:pregunta, :respuesta, :categoria, :activo)
                RETURNING *
                """, params(faq), FAQ_MAPPER);
    }

    /**
     * Actualiza una pregunta frecuente, activa o no, incluido su estado de visibilidad.
     *
     * @author Gabriela Zabaleta
     * @param faq pregunta con los nuevos datos, identificada por su id
     * @return pregunta actualizada
     */
    public Faq update(Faq faq) {
        return jdbc.queryForObject("""
                UPDATE faq
                SET pregunta = :pregunta, respuesta = :respuesta, categoria = :categoria, activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(faq).addValue("id", faq.id()), FAQ_MAPPER);
    }

    /**
     * Desactiva lógicamente una pregunta frecuente.
     *
     * @author Gabriela Zabaleta
     * @param id identificador de la pregunta
     * @return número de filas afectadas
     */
    public int deactivate(UUID id) {
        return jdbc.update("UPDATE faq SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(Faq f) {
        return new MapSqlParameterSource()
                .addValue("pregunta", f.pregunta())
                .addValue("respuesta", f.respuesta())
                .addValue("categoria", f.categoria())
                .addValue("activo", f.activo());
    }
}
