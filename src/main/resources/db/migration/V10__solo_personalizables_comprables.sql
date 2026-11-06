-- SPORTFORCE 3D · Etapa 12B (ajustes): "Personalizable 3D" solo en prendas cuyo diseño se puede comprar
-- (Garment.DESIGNABLE: camisetas, shorts, medias, zapatillas y uniformes). Los buzos, la casaca y los accesorios
-- llevaban la marca y el botón "Personalizar en 3D", pero el carrito no acepta su diseño: se compran directo del
-- catálogo con su color y talla. AdminProductService ya no deja volver a marcarlos.
UPDATE products
SET customizable = 0
WHERE customizable = 1
  AND garment_type IN ('HOODIE', 'ACCESSORY');