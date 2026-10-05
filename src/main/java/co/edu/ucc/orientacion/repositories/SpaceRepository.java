package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.models.Espacio;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos del catálogo de espacios del campus.
 *
 * @author Diego Luna
 */
@Repository
public class SpaceRepository {

    private static final RowMapper<Espacio> ESPACIO_MAPPER = (rs, i) -> new Espacio(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("codigo"),
            rs.getString("categoria"),
            rs.getString("edificio"),
            rs.getString("piso"),
            rs.getString("descripcion"),
            rs.getString("referencia"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Diego Luna
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public SpaceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista los espacios activos, opcionalmente filtrados por categoría.
     *
     * @author Diego Luna
     * @param categoria categoría normalizada, o null para no filtrar
     * @return espacios ordenados por nombre
     */
    public List<Espacio> findActive(String categoria) {
        return jdbc.query("""
                SELECT * FROM espacio
                WHERE activo = TRUE
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY nombre
                """,
                new MapSqlParameterSource("categoria", categoria), ESPACIO_MAPPER);
    }

    /**
     * Busca espacios activos cuyo nombre o código contenga el patrón. La búsqueda ignora
     * tildes y mayúsculas.
     *
     * @author Diego Luna
     * @param pattern patrón de búsqueda con comodines en los extremos; la búsqueda ignora tildes y mayúsculas
     * @return espacios encontrados ordenados por nombre
     */
    public List<Espacio> search(String pattern) {
        return jdbc.query("""
                SELECT * FROM espacio
                WHERE activo = TRUE
                  AND (unaccent(nombre) ILIKE unaccent(CAST(:pattern AS TEXT))
                       OR unaccent(codigo) ILIKE unaccent(CAST(:pattern AS TEXT)))
                ORDER BY nombre
                """,
                new MapSqlParameterSource("pattern", pattern), ESPACIO_MAPPER);
    }

    /**
     * Busca un espacio activo por su identificador.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @return espacio encontrado o vacío
     */
    public Optional<Espacio> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM espacio WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), ESPACIO_MAPPER).stream().findFirst();
    }

    /**
     * Verifica si existe un espacio con el código indicado.
     *
     * @author Diego Luna
     * @param codigo código del espacio
     * @param excludeId espacio a excluir de la búsqueda, o null
     * @return true si el código ya está en uso
     */
    public boolean existsByCodigo(String codigo, UUID excludeId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM espacio
                    WHERE codigo = :codigo
                      AND (CAST(:excludeId AS UUID) IS NULL OR id <> CAST(:excludeId AS UUID)))
                """,
                new MapSqlParameterSource().addValue("codigo", codigo).addValue("excludeId", excludeId),
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Busca un espacio por su identificador sin importar si está activo.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @return espacio encontrado o vacío
     */
    public Optional<Espacio> findById(UUID id) {
        return jdbc.query("SELECT * FROM espacio WHERE id = :id",
                new MapSqlParameterSource("id", id), ESPACIO_MAPPER).stream().findFirst();
    }

    /**
     * Lista todos los espacios incluyendo los inactivos, para la administración.
     *
     * @author Diego Luna
     * @param categoria categoría normalizada, o null para no filtrar
     * @return espacios ordenados por estado y nombre
     */
    public List<Espacio> findAll(String categoria) {
        return jdbc.query("""
                SELECT * FROM espacio
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY activo DESC, nombre
                """,
                new MapSqlParameterSource("categoria", categoria), ESPACIO_MAPPER);
    }

    /**
     * Inserta un espacio. Se ignoran el identificador y la fecha de creación del modelo recibido.
     *
     * @author Diego Luna
     * @param espacio datos del espacio a crear, incluido su estado activo
     * @return espacio creado
     */
    public Espacio create(Espacio espacio) {
        return jdbc.queryForObject("""
                INSERT INTO espacio (nombre, codigo, categoria, edificio, piso, descripcion, referencia, activo)
                VALUES (:nombre, :codigo, :categoria, :edificio, :piso, :descripcion, :referencia, :activo)
                RETURNING *
                """, params(espacio), ESPACIO_MAPPER);
    }

    /**
     * Actualiza un espacio, activo o no, incluido su estado de visibilidad.
     *
     * @author Diego Luna
     * @param espacio espacio con los nuevos datos, identificado por su id
     * @return espacio actualizado
     */
    public Espacio update(Espacio espacio) {
        return jdbc.queryForObject("""
                UPDATE espacio
                SET nombre = :nombre, codigo = :codigo, categoria = :categoria, edificio = :edificio,
                    piso = :piso, descripcion = :descripcion, referencia = :referencia, activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(espacio).addValue("id", espacio.id()), ESPACIO_MAPPER);
    }

    /**
     * Desactiva lógicamente un espacio.
     *
     * @author Diego Luna
     * @param id identificador del espacio
     * @return número de filas afectadas
     */
    public int deactivate(UUID id) {
        return jdbc.update("UPDATE espacio SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    private MapSqlParameterSource params(Espacio e) {
        return new MapSqlParameterSource()
                .addValue("nombre", e.nombre())
                .addValue("codigo", e.codigo())
                .addValue("categoria", e.categoria())
                .addValue("edificio", e.edificio())
                .addValue("piso", e.piso())
                .addValue("descripcion", e.descripcion())
                .addValue("referencia", e.referencia())
                .addValue("activo", e.activo());
    }
}
