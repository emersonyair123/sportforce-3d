-- SPORTFORCE 3D · Etapa 7: catálogo realista (fotos con la prenda recoloreada en vivo).
--
-- Campos nuevos de products:
--   model_key      carpeta de resources/models/realista (modelos.json)
--   zone_colors    JSON {zona: "#rrggbb"} (camiseta, short, paneles, medias, principal)
--   pattern        diseño de la camiseta (liso, rayas, franjas, dinamico, geometrico, minimal, banda)
--   pattern_color  color del diseño ("#rrggbb"), null si es liso
--
-- Carga los 44 productos de sportforce-realista/catalogo.json (las filas de realistic_catalog se generaron de
-- ese archivo):
--   * si ya existe un producto con el mismo nombre, se actualiza: categoría, precio, novedad, diseño 3D, modelo y
--     colores, y el color de sus variantes. Conserva su SKU, descripción, variantes y stock.
--   * si no, se inserta con una variante por talla y stock inicial de ejemplo.
-- Todos los precios del archivo vienen marcados "precioEsEjemplo": el ADMIN debe revisarlos (ver README).

ALTER TABLE products ADD COLUMN model_key TEXT CHECK (model_key IS NULL OR model_key GLOB '[a-z]*');
ALTER TABLE products ADD COLUMN zone_colors TEXT CHECK (zone_colors IS NULL OR json_valid(zone_colors));
ALTER TABLE products ADD COLUMN pattern TEXT NOT NULL DEFAULT 'liso'
    CHECK (pattern IN ('liso', 'rayas', 'franjas', 'dinamico', 'geometrico', 'minimal', 'banda'));
ALTER TABLE products ADD COLUMN pattern_color TEXT
    CHECK (pattern_color IS NULL OR pattern_color GLOB '#[0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]');

-- Categorías que usa catalogo.json. Vóley, básquet y shorts ya venían en V2 (no se duplican); falta "Niños".
INSERT INTO categories (slug, name, description, sort_order)
SELECT c.slug, c.name, c.description, c.sort_order
FROM (SELECT 'uniformes-voley' AS slug, 'Uniformes de vóley' AS name,
             'Sin mangas o manga corta, en licra o Dry-Fit.' AS description, 3 AS sort_order
      UNION ALL SELECT 'uniformes-basquet', 'Uniformes de básquet', 'Malla ventilada y shorts largos.', 4
      UNION ALL SELECT 'shorts', 'Shorts', 'Fútbol, vóley, básquet y running.', 5
      UNION ALL SELECT 'ninos', 'Niños', 'Uniformes, camisetas y buzos para niños y niñas.', 11) c
WHERE NOT EXISTS (SELECT 1 FROM categories x WHERE x.name = c.name OR x.slug = c.slug);

CREATE TEMP TABLE realistic_catalog (
    sku           TEXT PRIMARY KEY,
    name          TEXT NOT NULL UNIQUE,
    slug          TEXT NOT NULL,
    description   TEXT NOT NULL,
    category      TEXT NOT NULL,
    sport         TEXT NOT NULL,
    garment_type  TEXT NOT NULL,
    price_cents   INTEGER NOT NULL,
    is_new        INTEGER NOT NULL,
    customizable  INTEGER NOT NULL,
    model_key     TEXT NOT NULL,
    zone_colors   TEXT NOT NULL,
    pattern       TEXT NOT NULL,
    pattern_color TEXT,
    color_name    TEXT NOT NULL,
    color_hex     TEXT NOT NULL,
    kids          INTEGER NOT NULL,
    sample_price  INTEGER NOT NULL
);

-- sku, nombre, slug, descripción, categoría, deporte, prenda, precio (céntimos), nuevo, personalizable,
-- modelo, colores por zona, diseño, color del diseño, nombre y HEX del color principal, niños, precio de ejemplo
INSERT INTO realistic_catalog (sku, name, slug, description, category, sport, garment_type, price_cents, is_new,
                               customizable, model_key, zone_colors, pattern, pattern_color, color_name, color_hex,
                               kids, sample_price) VALUES
    ('SF-001', 'Uniforme Fútbol Pro Strike', 'uniforme-futbol-pro-strike', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Negro · verde neón.', 'Uniformes de fútbol', 'futbol', 'KIT', 12990, 1, 1, 'futbol_hombre_frente', '{"camiseta":"#0b0e13","short":"#0b0e13","paneles":"#39ff88","medias":"#0b0e13"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-002', 'Uniforme Fútbol Mujer Energía', 'uniforme-futbol-mujer-energia', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Azul eléctrico · blanco.', 'Uniformes de fútbol', 'futbol', 'KIT', 12990, 1, 1, 'futbol_mujer_frente', '{"camiseta":"#1f5fff","short":"#1f5fff","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL, 'Azul eléctrico', '#1F5FFF', 0, 1),
    ('SF-003', 'Uniforme Academia Pasión', 'uniforme-academia-pasion', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Rojo · negro.', 'Niños', 'futbol', 'KIT', 10990, 0, 1, 'futbol_nino_frente', '{"camiseta":"#d7141e","short":"#d7141e","paneles":"#0b0e13","medias":"#d7141e"}', 'liso', NULL, 'Rojo', '#D7141E', 1, 1),
    ('SF-004', 'Uniforme Academia Estilo', 'uniforme-academia-estilo', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Rosado · blanco.', 'Niños', 'futbol', 'KIT', 10990, 0, 1, 'futbol_nina_frente', '{"camiseta":"#ff5fb0","short":"#ff5fb0","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL, 'Rosado', '#FF5FB0', 1, 1),
    ('SF-005', 'Camiseta + Short Pro Dry-Fit', 'camiseta-short-pro-dry-fit', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Azul eléctrico · azul marino.', 'Camisetas deportivas', 'futbol', 'KIT', 7990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#1f5fff","short":"#1f5fff","paneles":"#0b2a6b","medias":"#f2f5f8"}', 'liso', NULL, 'Azul eléctrico', '#1F5FFF', 0, 1),
    ('SF-006', 'Conjunto Entrenamiento Morado', 'conjunto-entrenamiento-morado', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Morado · amarillo.', 'Ropa de entrenamiento', 'futbol', 'KIT', 11990, 0, 1, 'futbol_mujer_frente', '{"camiseta":"#8b3dff","short":"#8b3dff","paneles":"#ffd400","medias":"#8b3dff"}', 'liso', NULL, 'Morado', '#8B3DFF', 0, 1),
    ('SF-007', 'Uniforme Clásico Blanco', 'uniforme-clasico-blanco', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Blanco · azul marino.', 'Uniformes de fútbol', 'futbol', 'KIT', 12990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#f2f5f8","paneles":"#0b2a6b","medias":"#0b2a6b"}', 'liso', NULL, 'Blanco', '#F2F5F8', 0, 1),
    ('SF-008', 'Uniforme Academia Marino', 'uniforme-academia-marino', 'Camiseta cuello V con paneles laterales, short y medias. Colores: Azul marino · blanco.', 'Niños', 'futbol', 'KIT', 10990, 0, 1, 'futbol_nino_frente', '{"camiseta":"#0b2a6b","short":"#0b2a6b","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL, 'Azul marino', '#0B2A6B', 1, 1),
    ('SF-009', 'Camiseta Rayas Clásica', 'camiseta-rayas-clasica', 'Camiseta cuello V con paneles laterales. Colores: Blanco con rayas azul marino.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7990, 1, 1, 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b2a6b","paneles":"#0b2a6b","medias":"#0b2a6b"}', 'rayas', '#0b2a6b', 'Blanco', '#F2F5F8', 0, 1),
    ('SF-010', 'Camiseta Rayas Roja', 'camiseta-rayas-roja', 'Camiseta cuello V con paneles laterales. Colores: Rojo con rayas negras.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#d7141e","short":"#0b0e13","paneles":"#0b0e13","medias":"#0b0e13"}', 'rayas', '#0b0e13', 'Rojo', '#D7141E', 0, 1),
    ('SF-011', 'Camiseta Franjas Horizontales', 'camiseta-franjas-horizontales', 'Camiseta cuello V con paneles laterales. Colores: Azul marino con franjas amarillas.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7990, 0, 1, 'futbol_mujer_frente', '{"camiseta":"#0b2a6b","short":"#0b2a6b","paneles":"#ffd400","medias":"#0b2a6b"}', 'franjas', '#ffd400', 'Azul marino', '#0B2A6B', 0, 1),
    ('SF-012', 'Camiseta Dinámica Neón', 'camiseta-dinamica-neon', 'Camiseta cuello V con paneles laterales. Colores: Negro con trazos verde neón.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7990, 1, 1, 'futbol_mujer_frente', '{"camiseta":"#0b0e13","short":"#0b0e13","paneles":"#39ff88","medias":"#0b0e13"}', 'dinamico', '#39ff88', 'Negro', '#0B0E13', 0, 1),
    ('SF-013', 'Camiseta Geométrica', 'camiseta-geometrica', 'Camiseta cuello V con paneles laterales. Colores: Azul eléctrico con figuras blancas.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#1f5fff","short":"#f2f5f8","paneles":"#f2f5f8","medias":"#1f5fff"}', 'geometrico', '#f2f5f8', 'Azul eléctrico', '#1F5FFF', 0, 1),
    ('SF-014', 'Camiseta Banda Diagonal', 'camiseta-banda-diagonal', 'Camiseta cuello V con paneles laterales. Colores: Blanco con banda roja.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b0e13","paneles":"#d7141e","medias":"#f2f5f8"}', 'banda', '#d7141e', 'Blanco', '#F2F5F8', 0, 1),
    ('SF-015', 'Camiseta Hombros Contraste Niño', 'camiseta-hombros-contraste-nino', 'Camiseta cuello V con paneles laterales. Colores: Amarillo con hombros negros.', 'Niños', 'futbol', 'JERSEY', 6490, 0, 1, 'futbol_nino_frente', '{"camiseta":"#ffd400","short":"#0b0e13","paneles":"#0b0e13","medias":"#ffd400"}', 'minimal', '#0b0e13', 'Amarillo', '#FFD400', 1, 1),
    ('SF-016', 'Camiseta Dinámica Niña', 'camiseta-dinamica-nina', 'Camiseta cuello V con paneles laterales. Colores: Morado con trazos amarillos.', 'Niños', 'futbol', 'JERSEY', 6490, 0, 1, 'futbol_nina_frente', '{"camiseta":"#8b3dff","short":"#8b3dff","paneles":"#ffd400","medias":"#f2f5f8"}', 'dinamico', '#ffd400', 'Morado', '#8B3DFF', 1, 1),
    ('SF-017', 'Camiseta Cuello Polo Marino', 'camiseta-cuello-polo-marino', 'Cuello polo con paneles laterales de contraste. Colores: Azul marino con paneles blancos.', 'Camisetas deportivas', 'futbol', 'JERSEY', 8990, 1, 1, 'polo_hombre_frente', '{"camiseta":"#0b2a6b","short":"#0b2a6b","paneles":"#f2f5f8","medias":"#0b2a6b"}', 'liso', NULL, 'Azul marino', '#0B2A6B', 0, 1),
    ('SF-018', 'Polo Deportivo Negro Neón', 'polo-deportivo-negro-neon', 'Cuello polo con paneles laterales de contraste. Colores: Negro con paneles verde neón.', 'Camisetas deportivas', 'futbol', 'JERSEY', 8990, 0, 1, 'polo_hombre_frente', '{"camiseta":"#0b0e13","short":"#0b0e13","paneles":"#39ff88","medias":"#0b0e13"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-019', 'Camiseta Cuello Redondo Roja', 'camiseta-cuello-redondo-roja', 'Cuello redondo con paneles laterales. Colores: Roja con paneles negros.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7490, 1, 1, 'redondo_hombre_frente', '{"camiseta":"#d7141e","short":"#0b0e13","paneles":"#0b0e13","medias":"#d7141e"}', 'liso', NULL, 'Roja', '#D7141E', 0, 1),
    ('SF-020', 'Camiseta Cuello Redondo Celeste', 'camiseta-cuello-redondo-celeste', 'Cuello redondo con paneles laterales. Colores: Celeste con paneles azul marino.', 'Camisetas deportivas', 'futbol', 'JERSEY', 7490, 0, 1, 'redondo_hombre_frente', '{"camiseta":"#7fc8ff","short":"#f2f5f8","paneles":"#0b2a6b","medias":"#f2f5f8"}', 'liso', NULL, 'Celeste', '#7FC8FF', 0, 1),
    ('SF-021', 'Camiseta Raglán Rosa', 'camiseta-raglan-rosa', 'Mangas raglán en color de contraste. Colores: Blanca con mangas rosadas.', 'Camisetas deportivas', 'futbol', 'JERSEY', 8490, 1, 1, 'raglan_mujer_frente', '{"camiseta":"#f2f5f8","short":"#0b0e13","paneles":"#ff5fb0","medias":"#f2f5f8"}', 'liso', NULL, 'Blanca', '#F2F5F8', 0, 1),
    ('SF-022', 'Camiseta Raglán Amarilla', 'camiseta-raglan-amarilla', 'Mangas raglán en color de contraste. Colores: Amarilla con mangas azul marino.', 'Camisetas deportivas', 'futbol', 'JERSEY', 8490, 0, 1, 'raglan_mujer_frente', '{"camiseta":"#ffd400","short":"#0b2a6b","paneles":"#0b2a6b","medias":"#ffd400"}', 'liso', NULL, 'Amarilla', '#FFD400', 0, 1),
    ('SF-023', 'Short Largo Entrenamiento Negro', 'short-largo-entrenamiento-negro', 'Short largo de entrenamiento con paneles laterales. Colores: Negro con paneles verde neón.', 'Shorts', 'entrenamiento', 'SHORTS', 5490, 1, 1, 'shortlargo_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b0e13","paneles":"#39ff88"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-024', 'Short Largo Entrenamiento Marino', 'short-largo-entrenamiento-marino', 'Short largo de entrenamiento con paneles laterales. Colores: Azul marino con paneles blancos.', 'Shorts', 'entrenamiento', 'SHORTS', 5490, 0, 1, 'shortlargo_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b2a6b","paneles":"#f2f5f8"}', 'liso', NULL, 'Azul marino', '#0B2A6B', 0, 1),
    ('SF-025', 'Short Licra Compresión Morado', 'short-licra-compresion-morado', 'Licra de compresión con paneles laterales. Colores: Morado con paneles negros.', 'Ropa de entrenamiento', 'entrenamiento', 'SHORTS', 5990, 1, 1, 'licra_mujer_frente', '{"camiseta":"#f2f5f8","short":"#8b3dff","paneles":"#0b0e13"}', 'liso', NULL, 'Morado', '#8B3DFF', 0, 1),
    ('SF-026', 'Short Licra Compresión Negro', 'short-licra-compresion-negro', 'Licra de compresión con paneles laterales. Colores: Negro con paneles rosados.', 'Ropa de entrenamiento', 'entrenamiento', 'SHORTS', 5990, 0, 1, 'licra_mujer_frente', '{"camiseta":"#f2f5f8","short":"#0b0e13","paneles":"#ff5fb0"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-027', 'Short Fútbol Negro', 'short-futbol-negro', 'Short de fútbol con franja lateral de contraste. Colores: Negro con franja verde neón.', 'Shorts', 'futbol', 'SHORTS', 4990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b0e13","paneles":"#39ff88","medias":"#f2f5f8"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-028', 'Short Fútbol Azul Marino', 'short-futbol-azul-marino', 'Short de fútbol con franja lateral de contraste. Colores: Azul marino con franja blanca.', 'Shorts', 'futbol', 'SHORTS', 4990, 0, 1, 'futbol_hombre_frente', '{"camiseta":"#f2f5f8","short":"#0b2a6b","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL, 'Azul marino', '#0B2A6B', 0, 1),
    ('SF-029', 'Short Mujer Azul Eléctrico', 'short-mujer-azul-electrico', 'Short de fútbol con franja lateral de contraste. Colores: Azul eléctrico.', 'Shorts', 'futbol', 'SHORTS', 4990, 0, 1, 'futbol_mujer_frente', '{"camiseta":"#f2f5f8","short":"#1f5fff","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL, 'Azul eléctrico', '#1F5FFF', 0, 1),
    ('SF-030', 'Short Mujer Rojo', 'short-mujer-rojo', 'Short de fútbol con franja lateral de contraste. Colores: Rojo con franja negra.', 'Shorts', 'futbol', 'SHORTS', 4990, 0, 1, 'futbol_mujer_frente', '{"camiseta":"#f2f5f8","short":"#d7141e","paneles":"#0b0e13","medias":"#f2f5f8"}', 'liso', NULL, 'Rojo', '#D7141E', 0, 1),
    ('SF-031', 'Short Niño Verde Neón', 'short-nino-verde-neon', 'Short de fútbol con franja lateral de contraste. Colores: Verde neón con franja negra.', 'Shorts', 'futbol', 'SHORTS', 3990, 0, 1, 'futbol_nino_frente', '{"camiseta":"#f2f5f8","short":"#39ff88","paneles":"#0b0e13","medias":"#f2f5f8"}', 'liso', NULL, 'Verde neón', '#39FF88', 1, 1),
    ('SF-032', 'Short Niña Rosado', 'short-nina-rosado', 'Short de fútbol con franja lateral de contraste. Colores: Rosado con franja blanca.', 'Shorts', 'futbol', 'SHORTS', 3990, 0, 1, 'futbol_nina_frente', '{"camiseta":"#f2f5f8","short":"#ff5fb0","paneles":"#f2f5f8","medias":"#f2f5f8"}', 'liso', NULL, 'Rosado', '#FF5FB0', 1, 1),
    ('SF-033', 'Uniforme Vóley Sin Mangas Mujer', 'uniforme-voley-sin-mangas-mujer', 'Sin mangas, corte libre para el remate. Colores: Azul eléctrico.', 'Uniformes de vóley', 'voley', 'KIT', 11990, 1, 1, 'voley_mujer_frente', '{"camiseta":"#1f5fff","short":"#1f5fff","paneles":"#f2f5f8","medias":"#1f5fff"}', 'liso', NULL, 'Azul eléctrico', '#1F5FFF', 0, 1),
    ('SF-034', 'Uniforme Vóley Sin Mangas Hombre', 'uniforme-voley-sin-mangas-hombre', 'Sin mangas, corte libre para el remate. Colores: Amarillo · azul marino.', 'Uniformes de vóley', 'voley', 'KIT', 11990, 0, 1, 'voley_hombre_frente', '{"camiseta":"#ffd400","short":"#ffd400","paneles":"#0b2a6b","medias":"#0b2a6b"}', 'liso', NULL, 'Amarillo', '#FFD400', 0, 1),
    ('SF-035', 'Uniforme Básquet Pro', 'uniforme-basquet-pro', 'Bividí y short largo de básquet. Colores: Naranja · negro.', 'Uniformes de básquet', 'basquet', 'KIT', 12990, 1, 1, 'basquet_hombre_frente', '{"camiseta":"#ff7a00","short":"#ff7a00","paneles":"#0b0e13","medias":"#f2f5f8"}', 'liso', NULL, 'Naranja', '#FF7A00', 0, 1),
    ('SF-036', 'Camiseta Manga Larga Pro', 'camiseta-manga-larga-pro', 'Manga larga con paneles laterales. Colores: Negro · verde neón.', 'Camisetas deportivas', 'futbol', 'JERSEY', 8990, 0, 1, 'mangalarga_hombre_frente', '{"camiseta":"#0b0e13","short":"#0b0e13","paneles":"#39ff88","medias":"#0b0e13"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-037', 'Buzo Conjunto Microfibra Hombre', 'buzo-conjunto-microfibra-hombre', 'Casaca con cierre y pantalón con paneles de contraste. Colores: Azul marino · verde neón.', 'Buzos y conjuntos', 'entrenamiento', 'HOODIE', 16990, 0, 1, 'buzo_hombre_frente', '{"principal":"#0b2a6b","paneles":"#39ff88"}', 'liso', NULL, 'Azul marino', '#0B2A6B', 0, 1),
    ('SF-038', 'Buzo Conjunto Microfibra Mujer', 'buzo-conjunto-microfibra-mujer', 'Casaca con cierre y pantalón con paneles de contraste. Colores: Morado · blanco.', 'Buzos y conjuntos', 'entrenamiento', 'HOODIE', 16990, 0, 1, 'buzo_mujer_frente', '{"principal":"#8b3dff","paneles":"#f2f5f8"}', 'liso', NULL, 'Morado', '#8B3DFF', 0, 1),
    ('SF-039', 'Buzo Conjunto Juvenil', 'buzo-conjunto-juvenil', 'Casaca con cierre y pantalón con paneles de contraste. Colores: Negro · rojo.', 'Buzos y conjuntos', 'entrenamiento', 'HOODIE', 14990, 0, 1, 'buzo_juvenil_frente', '{"principal":"#0b0e13","paneles":"#d7141e"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1),
    ('SF-040', 'Buzo Conjunto Niño', 'buzo-conjunto-nino', 'Casaca con cierre y pantalón con paneles de contraste. Colores: Rojo · negro.', 'Niños', 'entrenamiento', 'HOODIE', 12990, 0, 1, 'buzo_nino_frente', '{"principal":"#d7141e","paneles":"#0b0e13"}', 'liso', NULL, 'Rojo', '#D7141E', 1, 1),
    ('SF-041', 'Buzo Conjunto Niña', 'buzo-conjunto-nina', 'Casaca con cierre y pantalón con paneles de contraste. Colores: Rosado · blanco.', 'Niños', 'entrenamiento', 'HOODIE', 12990, 0, 1, 'buzo_nina_frente', '{"principal":"#ff5fb0","paneles":"#f2f5f8"}', 'liso', NULL, 'Rosado', '#FF5FB0', 1, 1),
    ('SF-042', 'Zapatillas Fútbol FG Strike', 'zapatillas-futbol-fg-strike', 'Tacos FG para grass natural. Colores: Azul eléctrico · suela negra.', 'Zapatillas', 'futbol', 'SHOES', 24990, 1, 0, 'producto_zapatilla_fg', '{"principal":"#1f5fff"}', 'liso', NULL, 'Azul eléctrico', '#1F5FFF', 0, 1),
    ('SF-043', 'Medias Pro Compresión', 'medias-pro-compresion', 'Caña alta con compresión ligera. Colores: Verde neón.', 'Accesorios', 'futbol', 'SOCKS', 2490, 0, 0, 'producto_medias', '{"principal":"#39ff88"}', 'liso', NULL, 'Verde neón', '#39FF88', 0, 1),
    ('SF-044', 'Canilleras Pro', 'canilleras-pro', 'Carcasa rígida y espuma EVA. Colores: Negro.', 'Accesorios', 'futbol', 'ACCESSORY', 3990, 0, 0, 'producto_canilleras', '{"principal":"#0b0e13"}', 'liso', NULL, 'Negro', '#0B0E13', 0, 1);

-- Productos que ya existían con el mismo nombre.
CREATE TEMP TABLE realistic_matched AS
SELECT p.id AS product_id, rc.sku AS catalog_sku
FROM products p
JOIN realistic_catalog rc ON rc.name = p.name;

UPDATE products
SET category_id            = (SELECT c.id FROM categories c WHERE c.name = rc.category),
    base_price_cents       = rc.price_cents,
    compare_at_price_cents = CASE WHEN products.compare_at_price_cents > rc.price_cents
                                  THEN products.compare_at_price_cents END,
    is_new                 = rc.is_new,
    customizable           = rc.customizable,
    model_key              = rc.model_key,
    zone_colors            = rc.zone_colors,
    pattern                = rc.pattern,
    pattern_color          = rc.pattern_color
FROM realistic_catalog rc
WHERE rc.name = products.name;

UPDATE product_variants
SET color_name = rc.color_name,
    color_hex  = rc.color_hex
FROM realistic_matched m
JOIN realistic_catalog rc ON rc.sku = m.catalog_sku
WHERE product_variants.product_id = m.product_id;

-- Productos nuevos.
INSERT INTO products (category_id, sport_id, sku, slug, name, description, garment_type, base_price_cents,
                      customizable, is_new, model_key, zone_colors, pattern, pattern_color, search_key)
SELECT (SELECT c.id FROM categories c WHERE c.name = rc.category),
       (SELECT s.id FROM sports s WHERE s.slug = rc.sport),
       rc.sku, rc.slug, rc.name, rc.description, rc.garment_type, rc.price_cents,
       rc.customizable, rc.is_new, rc.model_key, rc.zone_colors, rc.pattern, rc.pattern_color,
       lower(
           replace(replace(replace(replace(replace(replace(replace(
           replace(replace(replace(replace(replace(replace(replace(
               rc.name || ' ' || rc.description || ' ' || rc.sku,
               'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u'), 'ñ', 'n'),
               'Á', 'a'), 'É', 'e'), 'Í', 'i'), 'Ó', 'o'), 'Ú', 'u'), 'Ü', 'u'), 'Ñ', 'n'))
FROM realistic_catalog rc
WHERE rc.sku NOT IN (SELECT catalog_sku FROM realistic_matched)
ORDER BY rc.sku;

-- Una variante por talla en el color principal. Niños y niñas: XS a L.
INSERT INTO product_variants (product_id, size_id, color_name, color_hex, sku)
SELECT p.id, s.id, rc.color_name, rc.color_hex, p.sku || '-' || s.code
FROM products p
JOIN realistic_catalog rc ON rc.sku = p.sku
JOIN sizes s ON s.size_group = CASE p.garment_type
                                   WHEN 'SHOES' THEN 'CALZADO'
                                   WHEN 'SOCKS' THEN 'MEDIAS'
                                   WHEN 'ACCESSORY' THEN 'UNICA'
                                   ELSE 'ROPA'
                               END
WHERE (rc.kids = 0 OR s.code IN ('XS', 'S', 'M', 'L'))
  AND p.id NOT IN (SELECT product_id FROM realistic_matched)
ORDER BY p.id, s.sort_order;

-- Stock inicial de ejemplo, determinista (5 a 40 unidades por talla).
INSERT INTO inventory (variant_id, quantity, low_stock_threshold)
SELECT v.id, (v.id * 7) % 36 + 5, 5
FROM product_variants v
JOIN products p ON p.id = v.product_id
JOIN realistic_catalog rc ON rc.sku = p.sku
WHERE NOT EXISTS (SELECT 1 FROM inventory i WHERE i.variant_id = v.id);

-- Materiales permitidos de los productos nuevos personalizables (misma regla que V2).
INSERT INTO product_materials (product_id, material_id, is_default)
SELECT p.id, m.id,
       CASE
           WHEN p.garment_type IN ('JERSEY', 'SHORTS', 'KIT') AND m.code = 'DRY_FIT' THEN 1
           WHEN p.garment_type = 'SOCKS' AND m.code = 'LICRA' THEN 1
           WHEN p.garment_type = 'HOODIE' AND m.code = 'MICROFIBRA' THEN 1
           WHEN p.garment_type = 'ACCESSORY' AND m.code = 'ALGODON' THEN 1
           WHEN p.garment_type = 'SHOES' AND m.code = 'KNIT' THEN 1
           ELSE 0
       END
FROM products p
JOIN realistic_catalog rc ON rc.sku = p.sku
JOIN materials m
  ON (p.garment_type = 'SHOES' AND m.scope = 'CALZADO')
  OR (p.garment_type <> 'SHOES' AND m.scope = 'TEXTIL'
      AND (',' || m.suitable_for || ',') LIKE ('%,' || p.garment_type || ',%'))
WHERE p.customizable = 1
  AND p.id NOT IN (SELECT product_id FROM realistic_matched);

DROP TABLE realistic_matched;
DROP TABLE realistic_catalog;