-- Etapa 10B: sección "Tela" del diseñador (Dry-fit, Poliéster liso, Algodón y Malla deportiva) para camiseta y
-- short. Dry-Fit, Malla y Algodón ya existían con su recargo (0, 500 y 0 céntimos) y no se tocan los precios.
-- Poliéster liso no existía: se agrega SIN recargo (0) hasta que el ADMIN defina su precio (README).

UPDATE materials SET name = 'Malla deportiva' WHERE code = 'MALLA';

-- El algodón también se ofrece en short y uniforme (antes solo camiseta, buzo y accesorios).
UPDATE materials SET suitable_for = 'JERSEY,SHORTS,KIT,HOODIE,ACCESSORY' WHERE code = 'ALGODON';

INSERT INTO materials (code, name, composition, usage, scope, suitable_for, surcharge_cents, roughness, sheen,
                       metalness, preview_pattern, sort_order)
VALUES ('POLIESTER_LISO', 'Poliéster liso', '100 % poliéster de tejido plano', 'Liso y con brillo', 'TEXTIL',
        'JERSEY,SHORTS,KIT', 0, 0.40, 0.42, 0, 'SMOOTH', 2);

-- Permitidos (no predeterminados) en las camisetas, shorts y uniformes personalizables.
INSERT OR IGNORE INTO product_materials (product_id, material_id, is_default)
SELECT p.id, m.id, 0
FROM products p
JOIN materials m ON m.code IN ('POLIESTER_LISO', 'ALGODON')
WHERE p.customizable = 1 AND p.garment_type IN ('JERSEY', 'SHORTS', 'KIT');
