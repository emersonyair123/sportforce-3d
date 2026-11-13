UPDATE materials SET name = 'Malla deportiva' WHERE code = 'MALLA';

UPDATE materials SET suitable_for = 'JERSEY,SHORTS,KIT,HOODIE,ACCESSORY' WHERE code = 'ALGODON';

INSERT INTO materials (code, name, composition, usage, scope, suitable_for, surcharge_cents, roughness, sheen,
                       metalness, preview_pattern, sort_order)
VALUES ('POLIESTER_LISO', 'Poliéster liso', '100 % poliéster de tejido plano', 'Liso y con brillo', 'TEXTIL',
        'JERSEY,SHORTS,KIT', 0, 0.40, 0.42, 0, 'SMOOTH', 2);

INSERT OR IGNORE INTO product_materials (product_id, material_id, is_default)
SELECT p.id, m.id, 0
FROM products p
JOIN materials m ON m.code IN ('POLIESTER_LISO', 'ALGODON')
WHERE p.customizable = 1 AND p.garment_type IN ('JERSEY', 'SHORTS', 'KIT');
