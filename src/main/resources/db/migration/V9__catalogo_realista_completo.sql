CREATE TEMP TABLE realistic_assign (
    sku          TEXT PRIMARY KEY,
    model_key    TEXT NOT NULL,
    zone_colors  TEXT NOT NULL,
    previous_key TEXT
);

INSERT INTO realistic_assign (sku, model_key, zone_colors, previous_key) VALUES
    ('SF-UFU-003', 'arquero_hombre_frente', '{"camiseta":"{c}","short":"#0b0e13","medias":"{c}"}', 'mangalarga_hombre_frente'),
    ('SF-ENT-002', 'compresion_hombre_frente', '{"camiseta":"{c}"}', 'mangalarga_hombre_frente'),
    ('SF-BUZ-002', 'casaca_hombre_frente', '{"principal":"{c}"}', 'buzo_hombre_frente'),
    ('SF-MED-002', 'producto_media_cana', '{"principal":"{c}"}', 'producto_medias'),
    ('SF-ZAP-002', 'producto_zapatilla_futsal', '{"principal":"{c}"}', 'producto_zapatilla_fg'),
    ('SF-ZAP-003', 'producto_zapatilla_running', '{"principal":"{c}","paneles":"#f2f5f8"}', NULL),
    ('SF-ACC-002', 'producto_brazalete', '{"principal":"{c}"}', NULL),
    ('SF-ACC-003', 'producto_guantes', '{"principal":"{c}"}', NULL),
    ('SF-ACC-004', 'producto_mochila', '{"principal":"{c}"}', NULL);

UPDATE products
SET model_key     = (SELECT a.model_key FROM realistic_assign a WHERE a.sku = products.sku),
    zone_colors   = replace((SELECT a.zone_colors FROM realistic_assign a WHERE a.sku = products.sku), '{c}',
                            coalesce((SELECT lower(v.color_hex) FROM product_variants v
                                      WHERE v.product_id = products.id
                                      ORDER BY v.active DESC, v.id LIMIT 1), '#0b0e13')),
    pattern       = 'liso',
    pattern_color = NULL
WHERE sku IN (SELECT sku FROM realistic_assign)
  AND (model_key IS NULL
       OR model_key = (SELECT a.previous_key FROM realistic_assign a WHERE a.sku = products.sku));

DROP TABLE realistic_assign;
