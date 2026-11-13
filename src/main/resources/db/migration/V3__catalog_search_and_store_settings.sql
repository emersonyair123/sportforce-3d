CREATE TABLE store_settings (
    key        TEXT PRIMARY KEY,
    value      TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

INSERT INTO store_settings (key, value) VALUES
    ('shipping_flat_cents', '1500'),
    ('free_shipping_from_cents', '29900');

ALTER TABLE products ADD COLUMN search_key TEXT NOT NULL DEFAULT '';

UPDATE products SET search_key = lower(
    replace(replace(replace(replace(replace(replace(replace(
    replace(replace(replace(replace(replace(replace(replace(
        name || ' ' || COALESCE(description, '') || ' ' || sku,
        'á', 'a'), 'é', 'e'), 'í', 'i'), 'ó', 'o'), 'ú', 'u'), 'ü', 'u'), 'ñ', 'n'),
        'Á', 'a'), 'É', 'e'), 'Í', 'i'), 'Ó', 'o'), 'Ú', 'u'), 'Ü', 'u'), 'Ñ', 'n'));

CREATE INDEX idx_products_price ON products (base_price_cents);
CREATE INDEX idx_inventory_low_stock ON inventory (quantity, reserved, low_stock_threshold);
