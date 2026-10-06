CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS plano (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  nombre VARCHAR(150) NOT NULL,
  edificio VARCHAR(100),
  piso VARCHAR(30),
  imagen TEXT NOT NULL,
  ancho INTEGER NOT NULL,
  alto INTEGER NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP DEFAULT NOW(),
  actualizado_en TIMESTAMP DEFAULT NOW(),
  CONSTRAINT ck_plano_imagen CHECK (imagen ~ '^data:image/(png|jpeg|webp);base64,'),
  CONSTRAINT ck_plano_dimensiones CHECK (ancho BETWEEN 1 AND 8192 AND alto BETWEEN 1 AND 8192)
);

ALTER TABLE espacio
  ADD COLUMN IF NOT EXISTS plano_id UUID REFERENCES plano(id),
  ADD COLUMN IF NOT EXISTS geometria JSONB;

COMMENT ON COLUMN espacio.plano_id IS 'Plano sobre el que está dibujado el espacio';
COMMENT ON COLUMN espacio.geometria IS
  'GeoJSON Geometry (Polygon) en píxeles del plano: x desde la izquierda, y desde abajo (Leaflet L.CRS.Simple)';

ALTER TABLE espacio DROP CONSTRAINT IF EXISTS ck_espacio_geometria_geojson;
ALTER TABLE espacio ADD CONSTRAINT ck_espacio_geometria_geojson CHECK (
  geometria IS NULL OR (
    jsonb_typeof(geometria) = 'object'
    AND geometria ->> 'type' = 'Polygon'
    AND jsonb_typeof(geometria -> 'coordinates') = 'array'
    AND jsonb_array_length(geometria -> 'coordinates') > 0
  )
);

ALTER TABLE espacio DROP CONSTRAINT IF EXISTS ck_espacio_geometria_plano;
ALTER TABLE espacio ADD CONSTRAINT ck_espacio_geometria_plano CHECK (
  (geometria IS NULL) = (plano_id IS NULL)
);

CREATE INDEX IF NOT EXISTS idx_espacio_plano ON espacio (plano_id) WHERE plano_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_plano_activo ON plano (activo);
