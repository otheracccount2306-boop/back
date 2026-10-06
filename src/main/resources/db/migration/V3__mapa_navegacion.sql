-- ============================================================
-- V3 · Mapa único del campus: datos de navegación del plano
-- ============================================================
-- Se ejecuta a mano una sola vez después de V2 (es idempotente):
--
--   psql -U postgres -d ucc_orientacion -1 -f src/main/resources/db/migration/V3__mapa_navegacion.sql
--
-- plano.navegacion guarda lo que el mapa necesita para dibujar caminos sin conexión:
--   {
--     "version": 1,
--     "celdaPx": 5.5,            tamaño de cada celda de la malla, en píxeles de la imagen
--     "ancho": 818, "alto": 342,  celdas de la malla (fila 0 = borde inferior de la imagen)
--     "malla": "120,4,...",      malla caminable codificada por tramos: libre/ocupado alternados,
--                                 empezando por ocupado
--     "pxPorMetro": 11,          escala de la imagen, para calcular distancias
--     "entradas": [{"id": "principal", "nombre": "...", "x": 150, "y": 1560}]
--   }
-- Lo genera el script de datos del campus; el panel administrativo no lo edita.
-- ============================================================

ALTER TABLE plano ADD COLUMN IF NOT EXISTS navegacion JSONB;

COMMENT ON COLUMN plano.navegacion IS
  'Malla caminable y entradas del campus para calcular caminos en la app (ver V3__mapa_navegacion.sql)';

ALTER TABLE plano DROP CONSTRAINT IF EXISTS ck_plano_navegacion;
ALTER TABLE plano ADD CONSTRAINT ck_plano_navegacion CHECK (
  navegacion IS NULL OR (
    jsonb_typeof(navegacion) = 'object'
    AND jsonb_typeof(navegacion -> 'malla') = 'string'
    AND (navegacion ->> 'ancho')::INT > 0
    AND (navegacion ->> 'alto')::INT > 0
  )
);
