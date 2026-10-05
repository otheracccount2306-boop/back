-- ============================================================
-- V2 · Módulo de Infraestructura: planos y geometría de los espacios
-- ============================================================
-- Se ejecuta a mano una sola vez sobre una base que ya tiene V1
-- (el proyecto no usa Flyway):
--
--   psql -U postgres -d ucc_orientacion -1 -f src/main/resources/db/migration/V2__mapa_infraestructura.sql
--
-- Es idempotente: se puede volver a ejecutar sin error.
--
-- Convención de coordenadas
--   Los planos son imágenes estáticas; no hay coordenadas GPS. Leaflet las
--   muestra con L.CRS.Simple y límites [[0, 0], [alto, ancho]], así que una
--   unidad equivale a un píxel de la imagen original:
--     x = píxeles desde el borde IZQUIERDO
--     y = píxeles desde el borde INFERIOR
--   La geometría se guarda como un objeto Geometry de GeoJSON (RFC 7946) de
--   tipo Polygon, con posiciones [x, y]:
--     {"type":"Polygon","coordinates":[[[120,340],[260,340],[260,450],[120,450],[120,340]]]}
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Planos: una imagen por edificio y piso -------------------------------
CREATE TABLE IF NOT EXISTS plano (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(150) NOT NULL,
  edificio VARCHAR(100),
  piso VARCHAR(30),
  -- Imagen como data URL (data:image/jpeg;base64,...). Se guarda en la BD para
  -- que la app la descargue junto con los polígonos y la tenga sin conexión.
  imagen TEXT NOT NULL,
  ancho INTEGER NOT NULL,
  alto INTEGER NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW(),
  actualizado_en TIMESTAMP DEFAULT NOW(),
  CONSTRAINT ck_plano_imagen CHECK (imagen ~ '^data:image/(png|jpeg|webp);base64,'),
  CONSTRAINT ck_plano_dimensiones CHECK (ancho BETWEEN 1 AND 8192 AND alto BETWEEN 1 AND 8192)
);

-- 2. Extensión de ESPACIO --------------------------------------------------
ALTER TABLE espacio
  ADD COLUMN IF NOT EXISTS plano_id UUID REFERENCES plano(id),
  ADD COLUMN IF NOT EXISTS geometria JSONB;

COMMENT ON COLUMN espacio.plano_id IS 'Plano sobre el que está dibujado el espacio';
COMMENT ON COLUMN espacio.geometria IS
  'GeoJSON Geometry (Polygon) en píxeles del plano: x desde la izquierda, y desde abajo (Leaflet L.CRS.Simple)';

-- La geometría debe ser un Polygon de GeoJSON bien formado en su estructura
-- básica. Las reglas finas (anillos cerrados, mínimo 4 posiciones, dentro de
-- la imagen) las valida el backend, que conoce las dimensiones del plano.
ALTER TABLE espacio DROP CONSTRAINT IF EXISTS ck_espacio_geometria_geojson;
ALTER TABLE espacio ADD CONSTRAINT ck_espacio_geometria_geojson CHECK (
  geometria IS NULL OR (
    jsonb_typeof(geometria) = 'object'
    AND geometria ->> 'type' = 'Polygon'
    AND jsonb_typeof(geometria -> 'coordinates') = 'array'
    AND jsonb_array_length(geometria -> 'coordinates') > 0
  )
);

-- Una geometría sin plano no se puede dibujar: van juntas o ninguna.
ALTER TABLE espacio DROP CONSTRAINT IF EXISTS ck_espacio_geometria_plano;
ALTER TABLE espacio ADD CONSTRAINT ck_espacio_geometria_plano CHECK (
  (geometria IS NULL) = (plano_id IS NULL)
);

-- 3. Índices ---------------------------------------------------------------
-- La app carga "todos los polígonos de un plano" en una sola consulta.
CREATE INDEX IF NOT EXISTS idx_espacio_plano ON espacio (plano_id) WHERE plano_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_plano_activo ON plano (activo);
