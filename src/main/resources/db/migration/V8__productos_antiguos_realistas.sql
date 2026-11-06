-- SPORTFORCE 3D · Etapa 12A: los productos de V2 que todavía se mostraban con el dibujo (GarmentArt) pasan a la foto
-- realista, como los de V6: modelo (models/realista/modelos.json), color por zona (JSON en minúsculas) y diseño.
-- Igual que en el catálogo realista, un short o una camiseta suelta se muestra con la foto de cuerpo completo y el
-- resto del uniforme en blanco o neutro.
--
--   * 13 asignaciones exactas: el modelo existente es la misma prenda.
--   * 7 PROVISIONALES (marcadas abajo): el modelo da el color pero no la forma exacta; se reemplazarán por fotos
--     nuevas (ver README, "Productos con foto provisional").
--   * SF-BUZ-001 duplicaba a SF-037 (mismo buzo de microfibra y mismo precio): borrado lógico.
-- Quedan con dibujo: SF-ZAP-003 y SF-ACC-002/003/004 (no hay modelo parecido; necesitan foto nueva).

CREATE TEMP TABLE realistic_assign (
    sku           TEXT PRIMARY KEY,
    model_key     TEXT NOT NULL,
    zone_colors   TEXT NOT NULL,
    pattern       TEXT NOT NULL,
    pattern_color TEXT
);

INSERT INTO realistic_assign (sku, model_key, zone_colors, pattern, pattern_color) VALUES
    -- Exactas
    ('SF-UFU-001', 'futbol_hombre_frente', '{"camiseta":"#1f5fff","short":"#1f5fff","paneles":"#0b0e13","medias":"#1f5fff"}', 'liso', NULL),
    ('SF-UFU-002', 'futbol_hombre_frente', '{"camiseta":"#d7192a","short":"#0b0e13","paneles":"#0b0e13","medias":"#d7192a"}', 'rayas', '#f2f5f8'),
    ('SF-UBA-001', 'basquet_hombre_frente', '{"camiseta":"#ffd400","short":"#ffd400","paneles":"#0b0e13","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-UBA-002', 'basquet_hombre_frente', '{"camiseta":"#f2f5f8","short":"#f2f5f8","paneles":"#1f5fff","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-UVO-001', 'voley_mujer_frente', '{"camiseta":"#7b2cbf","short":"#7b2cbf","medias":"#7b2cbf"}', 'liso', NULL),
    ('SF-UVO-002', 'voley_mujer_frente', '{"camiseta":"#7cc4ff","short":"#0b2a6b","medias":"#7cc4ff"}', 'liso', NULL),
    ('SF-CAM-001', 'redondo_hombre_frente', '{"camiseta":"#1f5fff","short":"#0b0e13","paneles":"#1f5fff","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-CAM-002', 'redondo_hombre_frente', '{"camiseta":"#0b0e13","short":"#0b0e13","paneles":"#5a6472","medias":"#0b0e13"}', 'liso', NULL),
    ('SF-ENT-001', 'polo_hombre_frente', '{"camiseta":"#5a6472","short":"#0b0e13","paneles":"#5a6472","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-SHO-001', 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b0e13","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-SHO-002', 'shortlargo_hombre_frente', '{"camiseta":"#f2f5f8","short":"#1f5fff","paneles":"#f2f5f8"}', 'liso', NULL),
    ('SF-MED-001', 'producto_medias', '{"principal":"#f2f5f8"}', 'liso', NULL),
    ('SF-BUZ-001', 'buzo_hombre_frente', '{"principal":"#0b0e13","paneles":"#0b0e13"}', 'liso', NULL),
    -- PROVISIONALES (foto aproximada hasta tener la propia)
    ('SF-UFU-003', 'mangalarga_hombre_frente', '{"camiseta":"#ff7a00","short":"#0b0e13","paneles":"#0b0e13","medias":"#ff7a00"}', 'liso', NULL),
    ('SF-CAM-003', 'redondo_hombre_frente', '{"camiseta":"#39ff88","short":"#0b0e13","paneles":"#39ff88","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-ENT-002', 'mangalarga_hombre_frente', '{"camiseta":"#0b0e13","short":"#0b0e13","paneles":"#0b0e13","medias":"#0b0e13"}', 'liso', NULL),
    ('SF-SHO-003', 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#5a6472","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL),
    ('SF-MED-002', 'producto_medias', '{"principal":"#0b0e13"}', 'liso', NULL),
    ('SF-BUZ-002', 'buzo_hombre_frente', '{"principal":"#0e7c3a","paneles":"#0e7c3a"}', 'liso', NULL),
    ('SF-ZAP-002', 'producto_zapatilla_fg', '{"principal":"#0b0e13"}', 'liso', NULL);

UPDATE products
SET model_key     = (SELECT a.model_key FROM realistic_assign a WHERE a.sku = products.sku),
    zone_colors   = (SELECT a.zone_colors FROM realistic_assign a WHERE a.sku = products.sku),
    pattern       = (SELECT a.pattern FROM realistic_assign a WHERE a.sku = products.sku),
    pattern_color = (SELECT a.pattern_color FROM realistic_assign a WHERE a.sku = products.sku)
WHERE sku IN (SELECT sku FROM realistic_assign) AND model_key IS NULL;

-- Duplicado de SF-037: borrado lógico (no se ve en la tienda; pedidos y carritos anteriores lo conservan).
UPDATE products
SET deleted_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
WHERE sku = 'SF-BUZ-001' AND deleted_at IS NULL;

DROP TABLE realistic_assign;
