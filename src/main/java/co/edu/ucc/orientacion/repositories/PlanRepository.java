package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.dto.response.PlanSummaryResponse;
import co.edu.ucc.orientacion.models.Plano;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos de los planos del campus sobre los que se dibujan los espacios.
 *
 * @author Diego Luna
 */
@Repository
public class PlanRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final RowMapper<Plano> PLANO_MAPPER = (rs, i) -> new Plano(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("edificio"),
            rs.getString("piso"),
            rs.getString("imagen"),
            rs.getInt("ancho"),
            rs.getInt("alto"),
            rs.getBoolean("activo"),
            readJson(rs, "navegacion"),
            rs.getObject("creado_en", LocalDateTime.class),
            rs.getObject("actualizado_en", LocalDateTime.class));

    private static final RowMapper<PlanSummaryResponse> SUMMARY_MAPPER = (rs, i) -> new PlanSummaryResponse(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("edificio"),
            rs.getString("piso"),
            rs.getInt("ancho"),
            rs.getInt("alto"),
            rs.getBoolean("activo"),
            rs.getInt("espacios_dibujados"),
            rs.getBoolean("con_navegacion"),
            rs.getObject("actualizado_en", LocalDateTime.class));

    /** Listado sin la columna imagen, que es pesada; cuenta los espacios dibujados de cada plano. */
    private static final String SUMMARY_SELECT = """
            SELECT p.id, p.nombre, p.edificio, p.piso, p.ancho, p.alto, p.activo, p.actualizado_en,
                   (p.navegacion IS NOT NULL) AS con_navegacion,
                   COUNT(e.id) FILTER (WHERE CAST(:onlyActive AS BOOLEAN) = FALSE OR e.activo = TRUE)
                       AS espacios_dibujados
            FROM plano p
            LEFT JOIN espacio e ON e.plano_id = p.id AND e.geometria IS NOT NULL
            WHERE (CAST(:onlyActive AS BOOLEAN) = FALSE OR p.activo = TRUE)
            GROUP BY p.id
            ORDER BY p.activo DESC, (p.navegacion IS NOT NULL) DESC, p.edificio NULLS LAST, p.piso NULLS LAST, p.nombre
            """;

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * Crea el repositorio con la plantilla JDBC.
     *
     * @author Diego Luna
     * @param jdbc plantilla JDBC con parámetros nombrados
     */
    public PlanRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Lista los planos sin su imagen.
     *
     * @author Diego Luna
     * @param onlyActive true para los planos visibles a los estudiantes; false para todos
     * @return planos ordenados por estado, edificio, piso y nombre
     */
    public List<PlanSummaryResponse> findSummaries(boolean onlyActive) {
        return jdbc.query(SUMMARY_SELECT, new MapSqlParameterSource("onlyActive", onlyActive), SUMMARY_MAPPER);
    }

    /**
     * Busca un plano por su identificador sin importar si está activo.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return plano con su imagen, o vacío
     */
    public Optional<Plano> findById(UUID id) {
        return jdbc.query("SELECT * FROM plano WHERE id = :id",
                new MapSqlParameterSource("id", id), PLANO_MAPPER).stream().findFirst();
    }

    /**
     * Busca un plano activo por su identificador.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return plano con su imagen, o vacío si no existe o está inactivo
     */
    public Optional<Plano> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM plano WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), PLANO_MAPPER).stream().findFirst();
    }

    /**
     * Inserta un plano. Se ignoran el identificador y las fechas del modelo recibido.
     *
     * @author Diego Luna
     * @param plano datos del plano
     * @return plano creado
     */
    public Plano create(Plano plano) {
        return jdbc.queryForObject("""
                INSERT INTO plano (nombre, edificio, piso, imagen, ancho, alto, activo)
                VALUES (:nombre, :edificio, :piso, :imagen, :ancho, :alto, :activo)
                RETURNING *
                """, params(plano), PLANO_MAPPER);
    }

    /**
     * Actualiza un plano, incluida su imagen y su estado.
     *
     * @author Diego Luna
     * @param plano plano con los nuevos datos, identificado por su id
     * @return plano actualizado
     */
    public Plano update(Plano plano) {
        return jdbc.queryForObject("""
                UPDATE plano
                SET nombre = :nombre, edificio = :edificio, piso = :piso, imagen = :imagen,
                    ancho = :ancho, alto = :alto, activo = :activo, actualizado_en = NOW()
                WHERE id = :id
                RETURNING *
                """, params(plano).addValue("id", plano.id()), PLANO_MAPPER);
    }

    /**
     * Marca el plano como modificado. Se llama cada vez que cambia un polígono, para que la app
     * sepa que su copia sin conexión quedó desactualizada.
     *
     * @author Diego Luna
     * @param id identificador del plano
     */
    public void touch(UUID id) {
        jdbc.update("UPDATE plano SET actualizado_en = NOW() WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    /**
     * Desactiva lógicamente un plano.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return número de filas afectadas
     */
    public int deactivate(UUID id) {
        return jdbc.update("""
                UPDATE plano SET activo = FALSE, actualizado_en = NOW() WHERE id = :id AND activo = TRUE
                """, new MapSqlParameterSource("id", id));
    }

    /**
     * Cuenta los espacios dibujados sobre un plano, activos o no.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return número de espacios con polígono
     */
    public int countShapes(UUID id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM espacio WHERE plano_id = :id AND geometria IS NOT NULL",
                new MapSqlParameterSource("id", id), Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * Borra un plano de la base de forma definitiva. Antes hay que quitar los polígonos de sus
     * espacios (SpaceRepository.clearGeometryByPlan), porque espacio.plano_id lo referencia.
     *
     * @author Diego Luna
     * @param id identificador del plano
     * @return número de filas borradas
     */
    public int delete(UUID id) {
        return jdbc.update("DELETE FROM plano WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private static JsonNode readJson(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null) {
            return null;
        }
        try {
            return JSON.readTree(raw);
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON inválido en la columna " + column, e);
        }
    }

    private MapSqlParameterSource params(Plano p) {
        return new MapSqlParameterSource()
                .addValue("nombre", p.nombre())
                .addValue("edificio", p.edificio())
                .addValue("piso", p.piso())
                .addValue("imagen", p.imagen())
                .addValue("ancho", p.ancho())
                .addValue("alto", p.alto())
                .addValue("activo", p.activo());
    }
}
