INSERT INTO roles (id, code, name) VALUES
    (1, 'ADMIN', 'Administrador'),
    (2, 'CLIENTE', 'Cliente');

INSERT INTO users (id, email, password_hash, full_name, phone) VALUES
    (1, 'admin@sportforce3d.pe', '$2a$12$zG7htj7DttEcw1H96uo9kecVng9xSO2lRCJO9W1SF3V0ggLjGUJlq', 'Administrador SportForce', NULL),
    (2, 'cliente@sportforce3d.pe', '$2a$12$xpXs2zogT8WQMrHWnS8ZeuFX1q0BLFR9hvRHLfftx5onOH71pxR26', 'Lucía Quispe', '987654321');

INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1),
    (2, 2);

INSERT INTO customers (id, user_id, document_type, document_number) VALUES
    (1, 2, 'DNI', '45871236');

INSERT INTO addresses (customer_id, label, recipient, phone, department, province, district, line1, reference, is_default) VALUES
    (1, 'Casa', 'Lucía Quispe', '987654321', 'Puno', 'Juliaca', 'Juliaca', 'Jr. Mariano Núñez 245', 'Frente al parque', 1);

INSERT INTO categories (id, slug, name, description, sort_order) VALUES
    (1, 'camisetas-deportivas', 'Camisetas deportivas', 'Camisetas para competir y entrenar.', 1),
    (2, 'uniformes-futbol', 'Uniformes de fútbol', 'Camiseta, short y medias listos para personalizar.', 2),
    (3, 'uniformes-voley', 'Uniformes de vóley', 'Sin mangas o manga corta, en licra o Dry-Fit.', 3),
    (4, 'uniformes-basquet', 'Uniformes de básquet', 'Malla ventilada y shorts largos.', 4),
    (5, 'shorts', 'Shorts', 'Fútbol, vóley, básquet y running.', 5),
    (6, 'medias', 'Medias', 'Altas, media caña y tobilleras.', 6),
    (7, 'zapatillas', 'Zapatillas', 'Tacos FG, sintético TF, futsal y running.', 7),
    (8, 'buzos-conjuntos', 'Buzos y conjuntos', 'Buzos, casacas y conjuntos de equipo.', 8),
    (9, 'ropa-entrenamiento', 'Ropa de entrenamiento', 'Polos, licras y prendas de gimnasio.', 9),
    (10, 'accesorios', 'Accesorios', 'Canilleras, guantes, brazaletes y mochilas.', 10);

INSERT INTO sports (id, slug, name, sort_order) VALUES
    (1, 'futbol', 'Fútbol', 1),
    (2, 'voley', 'Vóley', 2),
    (3, 'basquet', 'Básquet', 3),
    (4, 'futsal', 'Futsal', 4),
    (5, 'running', 'Running', 5),
    (6, 'entrenamiento', 'Entrenamiento', 6);

INSERT INTO sizes (code, name, size_group, sort_order) VALUES
    ('XS', 'XS', 'ROPA', 1),
    ('S', 'S', 'ROPA', 2),
    ('M', 'M', 'ROPA', 3),
    ('L', 'L', 'ROPA', 4),
    ('XL', 'XL', 'ROPA', 5),
    ('XXL', 'XXL', 'ROPA', 6),
    ('36', '36', 'CALZADO', 10),
    ('37', '37', 'CALZADO', 11),
    ('38', '38', 'CALZADO', 12),
    ('39', '39', 'CALZADO', 13),
    ('40', '40', 'CALZADO', 14),
    ('41', '41', 'CALZADO', 15),
    ('42', '42', 'CALZADO', 16),
    ('43', '43', 'CALZADO', 17),
    ('44', '44', 'CALZADO', 18),
    ('MED-S', '35–38', 'MEDIAS', 20),
    ('MED-L', '39–44', 'MEDIAS', 21),
    ('UNICA', 'Única', 'UNICA', 30);

INSERT INTO materials (code, name, composition, usage, scope, suitable_for, surcharge_cents, roughness, sheen, metalness, preview_pattern, sort_order) VALUES
    ('DRY_FIT', 'Dry-Fit', '100 % poliéster microperforado', 'Transpira y seca rápido', 'TEXTIL', 'JERSEY,SHORTS,KIT', 0, 0.55, 0.30, 0, 'DOTS', 1),
    ('MALLA', 'Malla (Mesh)', 'Poliéster con agujeros', 'Ventilación máxima', 'TEXTIL', 'JERSEY,SHORTS,KIT', 500, 0.65, 0.20, 0, 'MESH', 2),
    ('JACQUARD', 'Jacquard', 'Poliéster tejido con dibujo integrado', 'Relieve tono sobre tono', 'TEXTIL', 'JERSEY,KIT', 1500, 0.50, 0.45, 0, 'DIAGONAL', 3),
    ('LICRA', 'Licra (Spandex)', 'Poliéster + 15 % elastano', 'Compresión y ajuste', 'TEXTIL', 'JERSEY,SHORTS,SOCKS,KIT', 1000, 0.35, 0.60, 0, 'LINES', 4),
    ('ALGODON', 'Algodón peinado', '100 % algodón', 'Suave, casual', 'TEXTIL', 'JERSEY,HOODIE,ACCESSORY', 0, 0.90, 0.10, 0, 'PLAIN', 5),
    ('MICROFIBRA', 'Microfibra interlock', 'Poliéster de doble punto', 'Abrigo ligero y caída suave', 'TEXTIL', 'HOODIE', 800, 0.70, 0.35, 0, 'GRID', 6),
    ('KNIT', 'Knit', 'Tejido de punto', 'Ligera y flexible', 'CALZADO', 'shoe_upper', 0, 0.80, 0, 0.0, 'KNIT', 10),
    ('SINTETICO_PU', 'Sintético PU', 'Poliuretano', 'Resistente al roce, fácil de limpiar', 'CALZADO', 'shoe_upper,shoe_toe', 0, 0.40, 0, 0.0, 'SMOOTH', 11),
    ('CUERO_SINTETICO', 'Cuero sintético premium', 'Microfibra con acabado cuero', 'Toque y control del balón', 'CALZADO', 'shoe_upper,shoe_heel', 2500, 0.45, 0, 0.0, 'GRAIN', 12),
    ('CAUCHO', 'Caucho', 'Caucho natural', 'Agarre en cancha', 'CALZADO', 'shoe_sole', 0, 0.85, 0, 0.0, 'RUBBER', 13),
    ('TPU_TACOS', 'TPU con tacos', 'Poliuretano termoplástico', 'Tracción en grass natural o sintético', 'CALZADO', 'shoe_sole', 2000, 0.30, 0, 0.1, 'STUDS', 14),
    ('REFLECTIVO', 'Reflectivo', 'Película retrorreflectante', 'Visibilidad nocturna', 'CALZADO', 'shoe_logo,shoe_heel', 600, 0.20, 0, 0.6, 'REFLECTIVE', 15);

INSERT INTO products (id, category_id, sport_id, sku, slug, name, description, garment_type, base_price_cents, compare_at_price_cents, customizable, is_new, featured) VALUES
    (1, 1, 6, 'SF-CAM-001', 'camiseta-pro-dry-fit', 'Camiseta Pro Dry-Fit', 'Tejido transpirable con secado rápido.', 'JERSEY', 7990, NULL, 1, 0, 1),
    (2, 1, 6, 'SF-CAM-002', 'camiseta-training-malla', 'Camiseta Training Malla', 'Paneles de malla para ventilación máxima.', 'JERSEY', 6990, 8990, 1, 0, 0),
    (3, 1, 5, 'SF-CAM-003', 'camiseta-running-reflect', 'Camiseta Running Reflect', 'Detalles reflectivos para correr de noche.', 'JERSEY', 8990, NULL, 0, 1, 0),
    (4, 2, 1, 'SF-UFU-001', 'uniforme-futbol-pro', 'Uniforme Fútbol Pro', 'Camiseta, short y medias en Dry-Fit.', 'KIT', 14990, NULL, 1, 0, 1),
    (5, 2, 1, 'SF-UFU-002', 'uniforme-futbol-clasico-franjas', 'Uniforme Fútbol Clásico Franjas', 'Franjas verticales y cuello polo.', 'KIT', 12990, 15990, 1, 0, 1),
    (6, 2, 1, 'SF-UFU-003', 'uniforme-arquero-elite', 'Uniforme Arquero Elite', 'Manga larga acolchada en codos.', 'KIT', 15990, NULL, 1, 1, 0),
    (7, 3, 2, 'SF-UVO-001', 'uniforme-voley-sin-mangas', 'Uniforme Vóley Sin Mangas', 'Corte libre para el remate.', 'KIT', 11990, NULL, 1, 1, 1),
    (8, 3, 2, 'SF-UVO-002', 'uniforme-voley-licra-pro', 'Uniforme Vóley Licra Pro', 'Licra con compresión ligera.', 'KIT', 12990, NULL, 1, 0, 0),
    (9, 4, 3, 'SF-UBA-001', 'uniforme-basquet-malla', 'Uniforme Básquet Malla', 'Bividí de malla y short largo.', 'KIT', 13990, NULL, 1, 0, 1),
    (10, 4, 3, 'SF-UBA-002', 'uniforme-basquet-reversible', 'Uniforme Básquet Reversible', 'Dos colores en una sola prenda.', 'KIT', 15990, 17990, 1, 1, 0),
    (11, 5, 1, 'SF-SHO-001', 'short-futbol-dry-fit', 'Short Fútbol Dry-Fit', 'Pretina elástica con cordón.', 'SHORTS', 3990, NULL, 1, 0, 0),
    (12, 5, 3, 'SF-SHO-002', 'short-basquet-largo', 'Short Básquet Largo', 'Largo a la rodilla, malla interior.', 'SHORTS', 5490, NULL, 1, 0, 0),
    (13, 5, 5, 'SF-SHO-003', 'short-running-2-en-1', 'Short Running 2 en 1', 'Licra interior y bolsillo trasero.', 'SHORTS', 5990, 6990, 0, 1, 0),
    (14, 6, 1, 'SF-MED-001', 'medias-futbol-altas', 'Medias Fútbol Altas', 'Caña alta con banda personalizable.', 'SOCKS', 2490, NULL, 1, 0, 0),
    (15, 6, 5, 'SF-MED-002', 'medias-media-cana-compresion', 'Medias Media Caña Compresión', 'Compresión graduada para running.', 'SOCKS', 2990, NULL, 0, 0, 0),
    (16, 7, 1, 'SF-ZAP-001', 'zapatillas-futbol-fg-strike', 'Zapatillas Fútbol FG Strike', 'Tacos FG para grass natural.', 'SHOES', 24990, NULL, 1, 1, 1),
    (17, 7, 4, 'SF-ZAP-002', 'zapatillas-futsal-ic-control', 'Zapatillas Futsal IC Control', 'Suela de caucho para losa.', 'SHOES', 19990, 22990, 1, 0, 0),
    (18, 7, 5, 'SF-ZAP-003', 'zapatillas-running-pulse', 'Zapatillas Running Pulse', 'Amortiguación ligera para fondo.', 'SHOES', 27990, NULL, 1, 1, 0),
    (19, 8, 6, 'SF-BUZ-001', 'buzo-conjunto-microfibra', 'Buzo Conjunto Microfibra', 'Casaca y pantalón de microfibra.', 'HOODIE', 16990, NULL, 1, 0, 1),
    (20, 8, 6, 'SF-BUZ-002', 'casaca-cortavientos-team', 'Casaca Cortavientos Team', 'Ligera, repele la llovizna.', 'HOODIE', 13990, NULL, 1, 1, 0),
    (21, 9, 6, 'SF-ENT-001', 'polo-entrenamiento-algodon', 'Polo Entrenamiento Algodón', 'Algodón peinado para el día a día.', 'JERSEY', 4990, NULL, 0, 0, 0),
    (22, 9, 6, 'SF-ENT-002', 'licra-compresion-manga-larga', 'Licra Compresión Manga Larga', 'Primera capa térmica.', 'JERSEY', 6990, NULL, 0, 1, 0),
    (23, 10, 1, 'SF-ACC-001', 'canilleras-pro', 'Canilleras Pro', 'Carcasa rígida y espuma EVA.', 'ACCESSORY', 3990, NULL, 0, 0, 0),
    (24, 10, 1, 'SF-ACC-002', 'brazalete-de-capitan', 'Brazalete de Capitán', 'Elástico con velcro, personalizable.', 'ACCESSORY', 1990, NULL, 1, 0, 0),
    (25, 10, 1, 'SF-ACC-003', 'guantes-de-arquero-grip', 'Guantes de Arquero Grip', 'Palma de látex de alto agarre.', 'ACCESSORY', 8990, 10990, 0, 0, 0),
    (26, 10, 6, 'SF-ACC-004', 'mochila-team-30-l', 'Mochila Team 30 L', 'Compartimento para zapatillas.', 'ACCESSORY', 11990, NULL, 1, 0, 0);

WITH product_colors (product_id, color_name, color_hex) AS (
    VALUES
        (1, 'Azul eléctrico', '#1F5FFF'), (2, 'Negro', '#0B0E13'), (3, 'Verde neón', '#39FF88'),
        (4, 'Azul eléctrico', '#1F5FFF'), (5, 'Rojo', '#D7192A'), (6, 'Naranja', '#FF7A00'),
        (7, 'Morado', '#7B2CBF'), (8, 'Celeste', '#7CC4FF'), (9, 'Amarillo', '#FFD400'),
        (10, 'Blanco', '#F2F5F8'), (11, 'Negro', '#0B0E13'), (12, 'Azul eléctrico', '#1F5FFF'),
        (13, 'Gris', '#5A6472'), (14, 'Blanco', '#F2F5F8'), (15, 'Negro', '#0B0E13'),
        (16, 'Blanco', '#F2F5F8'), (17, 'Negro', '#0B0E13'), (18, 'Azul eléctrico', '#1F5FFF'),
        (19, 'Negro', '#0B0E13'), (20, 'Verde bosque', '#0E7C3A'), (21, 'Gris', '#5A6472'),
        (22, 'Negro', '#0B0E13'), (23, 'Negro', '#0B0E13'), (24, 'Amarillo', '#FFD400'),
        (25, 'Verde neón', '#39FF88'), (26, 'Negro', '#0B0E13')
)
INSERT INTO product_variants (product_id, size_id, color_name, color_hex, sku)
SELECT p.id, s.id, c.color_name, c.color_hex, p.sku || '-' || s.code
FROM products p
JOIN product_colors c ON c.product_id = p.id
JOIN sizes s ON s.size_group = CASE p.garment_type
                                   WHEN 'SHOES' THEN 'CALZADO'
                                   WHEN 'SOCKS' THEN 'MEDIAS'
                                   WHEN 'ACCESSORY' THEN 'UNICA'
                                   ELSE 'ROPA'
                               END;

INSERT INTO inventory (variant_id, quantity, low_stock_threshold)
SELECT v.id,
       CASE
           WHEN v.product_id = 18 THEN 0
           WHEN v.sku = 'SF-CAM-001-XXL' THEN 2
           ELSE (v.id * 7) % 37 + 3
       END,
       5
FROM product_variants v;

INSERT INTO product_materials (product_id, material_id, is_default)
SELECT p.id, m.id,
       CASE
           WHEN p.garment_type IN ('JERSEY', 'SHORTS', 'KIT') AND m.code = 'DRY_FIT' THEN 1
           WHEN p.garment_type = 'SOCKS' AND m.code = 'LICRA' THEN 1
           WHEN p.garment_type IN ('HOODIE', 'ACCESSORY') AND m.code IN ('MICROFIBRA', 'ALGODON')
                AND m.code = CASE p.garment_type WHEN 'HOODIE' THEN 'MICROFIBRA' ELSE 'ALGODON' END THEN 1
           WHEN p.garment_type = 'SHOES' AND m.code = 'KNIT' THEN 1
           ELSE 0
       END
FROM products p
JOIN materials m
  ON (p.garment_type = 'SHOES' AND m.scope = 'CALZADO')
  OR (p.garment_type <> 'SHOES' AND m.scope = 'TEXTIL'
      AND (',' || m.suitable_for || ',') LIKE ('%,' || p.garment_type || ',%'))
WHERE p.customizable = 1;
