UPDATE products
SET customizable = 0
WHERE customizable = 1
  AND garment_type IN ('HOODIE', 'ACCESSORY');
