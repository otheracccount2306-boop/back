package co.edu.ucc.orientacion.repositories;

import co.edu.ucc.orientacion.dto.response.SpaceShapeResponse;
import co.edu.ucc.orientacion.models.Espacio;
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

@Repository
public class SpaceRepository {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final RowMapper<Espacio> ESPACIO_MAPPER = (rs, i) -> new Espacio(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("codigo"),
            rs.getString("categoria"),
            rs.getString("edificio"),
            rs.getString("piso"),
            rs.getString("descripcion"),
            rs.getString("referencia"),
            rs.getObject("plano_id", UUID.class),
            readJson(rs, "geometria"),
            rs.getBoolean("activo"),
            rs.getObject("creado_en", LocalDateTime.class));

    private static final RowMapper<SpaceShapeResponse> SHAPE_MAPPER = (rs, i) -> new SpaceShapeResponse(
            rs.getObject("id", UUID.class),
            rs.getString("nombre"),
            rs.getString("codigo"),
            rs.getString("categoria"),
            rs.getString("edificio"),
            rs.getString("piso"),
            rs.getBoolean("activo"),
            readJson(rs, "geometria"));

    private final NamedParameterJdbcTemplate jdbc;

    public SpaceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Espacio> findActive(String categoria) {
        return jdbc.query("""
                SELECT * FROM espacio
                WHERE activo = TRUE
                  AND (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY nombre
                """,
                new MapSqlParameterSource("categoria", categoria), ESPACIO_MAPPER);
    }

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

    public Optional<Espacio> findActiveById(UUID id) {
        return jdbc.query("SELECT * FROM espacio WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id), ESPACIO_MAPPER).stream().findFirst();
    }

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

    public Optional<Espacio> findById(UUID id) {
        return jdbc.query("SELECT * FROM espacio WHERE id = :id",
                new MapSqlParameterSource("id", id), ESPACIO_MAPPER).stream().findFirst();
    }

    public List<Espacio> findAll(String categoria) {
        return jdbc.query("""
                SELECT * FROM espacio
                WHERE (CAST(:categoria AS VARCHAR) IS NULL OR categoria = :categoria)
                ORDER BY activo DESC, nombre
                """,
                new MapSqlParameterSource("categoria", categoria), ESPACIO_MAPPER);
    }

    public Espacio create(Espacio espacio) {
        return jdbc.queryForObject("""
                INSERT INTO espacio (nombre, codigo, categoria, edificio, piso, descripcion, referencia, activo)
                VALUES (:nombre, :codigo, :categoria, :edificio, :piso, :descripcion, :referencia, :activo)
                RETURNING *
                """, params(espacio), ESPACIO_MAPPER);
    }

    public Espacio update(Espacio espacio) {
        return jdbc.queryForObject("""
                UPDATE espacio
                SET nombre = :nombre, codigo = :codigo, categoria = :categoria, edificio = :edificio,
                    piso = :piso, descripcion = :descripcion, referencia = :referencia, activo = :activo
                WHERE id = :id
                RETURNING *
                """, params(espacio).addValue("id", espacio.id()), ESPACIO_MAPPER);
    }

    public int deactivate(UUID id) {
        return jdbc.update("UPDATE espacio SET activo = FALSE WHERE id = :id AND activo = TRUE",
                new MapSqlParameterSource("id", id));
    }

    public List<SpaceShapeResponse> findShapesByPlan(UUID planoId, boolean onlyActive) {
        return jdbc.query("""
                SELECT id, nombre, codigo, categoria, edificio, piso, activo, geometria FROM espacio
                WHERE plano_id = :planoId
                  AND geometria IS NOT NULL
                  AND (CAST(:onlyActive AS BOOLEAN) = FALSE OR activo = TRUE)
                ORDER BY piso NULLS FIRST, codigo
                """,
                new MapSqlParameterSource().addValue("planoId", planoId).addValue("onlyActive", onlyActive),
                SHAPE_MAPPER);
    }

    public record MappedRoom(UUID id, String codigo, String nombre) {
    }

    public List<MappedRoom> findMappedRooms() {
        return jdbc.query("""
                SELECT e.id, e.codigo, e.nombre FROM espacio e
                JOIN plano p ON p.id = e.plano_id AND p.activo = TRUE
                WHERE e.activo = TRUE AND e.geometria IS NOT NULL
                ORDER BY e.codigo
                """,
                new MapSqlParameterSource(),
                (rs, i) -> new MappedRoom(rs.getObject("id", UUID.class), rs.getString("codigo"), rs.getString("nombre")));
    }

    public int clearGeometryByPlan(UUID planoId) {
        return jdbc.update("UPDATE espacio SET plano_id = NULL, geometria = NULL WHERE plano_id = :planoId",
                new MapSqlParameterSource("planoId", planoId));
    }

    public Optional<Espacio> updateGeometry(UUID id, UUID planoId, String geometria) {
        return jdbc.query("""
                UPDATE espacio
                SET plano_id = :planoId, geometria = CAST(:geometria AS JSONB)
                WHERE id = :id
                RETURNING *
                """,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("planoId", planoId)
                        .addValue("geometria", geometria),
                ESPACIO_MAPPER).stream().findFirst();
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
