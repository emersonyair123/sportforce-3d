-- SPORTFORCE 3D · Esquema inicial (SQLite)
--
-- Convenciones:
--   * Montos en céntimos de sol (INTEGER). S/ 129.90 = 12990. Moneda única: PEN.
--   * Fechas en texto ISO-8601 UTC: 2026-10-17T21:05:33.120Z
--   * Booleanos como INTEGER 0/1 con CHECK.
--   * Las claves foráneas se activan en cada conexión (PRAGMA foreign_keys=ON, ver Database.java).
--   * updated_at se actualiza con triggers.

-- ---------------------------------------------------------------------------
-- Usuarios y roles
-- ---------------------------------------------------------------------------

CREATE TABLE roles (
    id   INTEGER PRIMARY KEY,
    code TEXT    NOT NULL UNIQUE CHECK (code IN ('ADMIN', 'CLIENTE')),
    name TEXT    NOT NULL
);

CREATE TABLE users (
    id            INTEGER PRIMARY KEY,
    email         TEXT    NOT NULL UNIQUE COLLATE NOCASE CHECK (email LIKE '%_@_%._%'),
    password_hash TEXT    NOT NULL CHECK (length(password_hash) = 60),
    full_name     TEXT    NOT NULL CHECK (length(trim(full_name)) BETWEEN 3 AND 80),
    phone         TEXT    CHECK (phone IS NULL OR length(phone) BETWEEN 6 AND 20),
    active        INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    last_login_at TEXT,
    created_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE user_roles (
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

CREATE TABLE customers (
    id              INTEGER PRIMARY KEY,
    user_id         INTEGER NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    document_type   TEXT    CHECK (document_type IN ('DNI', 'CE', 'RUC', 'PASAPORTE')),
    document_number TEXT    CHECK (document_number IS NULL OR length(document_number) BETWEEN 8 AND 15),
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE addresses (
    id          INTEGER PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    label       TEXT    NOT NULL CHECK (length(label) BETWEEN 1 AND 40),
    recipient   TEXT    NOT NULL CHECK (length(recipient) BETWEEN 3 AND 80),
    phone       TEXT    NOT NULL CHECK (length(phone) BETWEEN 6 AND 20),
    department  TEXT    NOT NULL,
    province    TEXT    NOT NULL,
    district    TEXT    NOT NULL,
    line1       TEXT    NOT NULL CHECK (length(line1) BETWEEN 5 AND 160),
    reference   TEXT,
    is_default  INTEGER NOT NULL DEFAULT 0 CHECK (is_default IN (0, 1)),
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_addresses_customer ON addresses (customer_id);
-- Solo una dirección predeterminada por cliente.
CREATE UNIQUE INDEX ux_addresses_default ON addresses (customer_id) WHERE is_default = 1;

-- ---------------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------------

CREATE TABLE categories (
    id          INTEGER PRIMARY KEY,
    slug        TEXT    NOT NULL UNIQUE CHECK (slug GLOB '[a-z0-9]*' AND slug NOT GLOB '*[^a-z0-9-]*'),
    name        TEXT    NOT NULL UNIQUE,
    description TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    active      INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE sports (
    id         INTEGER PRIMARY KEY,
    slug       TEXT    NOT NULL UNIQUE,
    name       TEXT    NOT NULL UNIQUE,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id                     INTEGER PRIMARY KEY,
    category_id            INTEGER NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    sport_id               INTEGER REFERENCES sports (id) ON DELETE SET NULL,
    sku                    TEXT    NOT NULL UNIQUE,
    slug                   TEXT    NOT NULL UNIQUE,
    name                   TEXT    NOT NULL CHECK (length(name) BETWEEN 3 AND 120),
    description            TEXT,
    garment_type           TEXT    NOT NULL CHECK (garment_type IN ('JERSEY', 'SHORTS', 'SOCKS', 'SHOES', 'HOODIE', 'ACCESSORY', 'KIT')),
    base_price_cents       INTEGER NOT NULL CHECK (base_price_cents >= 0),
    compare_at_price_cents INTEGER CHECK (compare_at_price_cents IS NULL OR compare_at_price_cents > base_price_cents),
    currency               TEXT    NOT NULL DEFAULT 'PEN' CHECK (currency = 'PEN'),
    customizable           INTEGER NOT NULL DEFAULT 0 CHECK (customizable IN (0, 1)),
    is_new                 INTEGER NOT NULL DEFAULT 0 CHECK (is_new IN (0, 1)),
    featured               INTEGER NOT NULL DEFAULT 0 CHECK (featured IN (0, 1)),
    active                 INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    deleted_at             TEXT,
    created_at             TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at             TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_sport ON products (sport_id);
CREATE INDEX idx_products_listing ON products (active, deleted_at, featured, is_new);

CREATE TABLE product_images (
    id         INTEGER PRIMARY KEY,
    product_id INTEGER NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    path       TEXT    NOT NULL,
    alt_text   TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_product_images_product ON product_images (product_id, sort_order);

CREATE TABLE sizes (
    id         INTEGER PRIMARY KEY,
    code       TEXT    NOT NULL UNIQUE,
    name       TEXT    NOT NULL,
    size_group TEXT    NOT NULL CHECK (size_group IN ('ROPA', 'CALZADO', 'MEDIAS', 'UNICA')),
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE product_variants (
    id                INTEGER PRIMARY KEY,
    product_id        INTEGER NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    size_id           INTEGER NOT NULL REFERENCES sizes (id) ON DELETE RESTRICT,
    color_name        TEXT    NOT NULL,
    color_hex         TEXT    NOT NULL CHECK (color_hex GLOB '#[0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F]'),
    sku               TEXT    NOT NULL UNIQUE,
    price_delta_cents INTEGER NOT NULL DEFAULT 0,
    active            INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at        TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    UNIQUE (product_id, size_id, color_hex)
);
CREATE INDEX idx_variants_size ON product_variants (size_id);

CREATE TABLE inventory (
    variant_id          INTEGER PRIMARY KEY REFERENCES product_variants (id) ON DELETE CASCADE,
    quantity            INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved            INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= quantity),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    updated_at          TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE materials (
    id              INTEGER PRIMARY KEY,
    code            TEXT    NOT NULL UNIQUE,
    name            TEXT    NOT NULL,
    composition     TEXT,
    usage           TEXT    NOT NULL,
    scope           TEXT    NOT NULL CHECK (scope IN ('TEXTIL', 'CALZADO')),
    -- Prendas (TEXTIL) o zonas de zapatilla (CALZADO) donde se ofrece, separadas por coma.
    suitable_for    TEXT    NOT NULL,
    surcharge_cents INTEGER NOT NULL DEFAULT 0 CHECK (surcharge_cents >= 0),
    roughness       REAL    NOT NULL CHECK (roughness BETWEEN 0 AND 1),
    sheen           REAL    NOT NULL DEFAULT 0 CHECK (sheen BETWEEN 0 AND 1),
    metalness       REAL    NOT NULL DEFAULT 0 CHECK (metalness BETWEEN 0 AND 1),
    preview_pattern TEXT    NOT NULL CHECK (preview_pattern IN ('DOTS', 'MESH', 'DIAGONAL', 'LINES', 'PLAIN', 'GRID', 'KNIT', 'SMOOTH', 'GRAIN', 'RUBBER', 'STUDS', 'REFLECTIVE')),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    active          INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE TABLE product_materials (
    product_id  INTEGER NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    material_id INTEGER NOT NULL REFERENCES materials (id) ON DELETE RESTRICT,
    is_default  INTEGER NOT NULL DEFAULT 0 CHECK (is_default IN (0, 1)),
    PRIMARY KEY (product_id, material_id)
);
CREATE INDEX idx_product_materials_material ON product_materials (material_id);

-- ---------------------------------------------------------------------------
-- Diseños 3D
-- ---------------------------------------------------------------------------

CREATE TABLE design_assets (
    id              INTEGER PRIMARY KEY,
    user_id         INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    original_name   TEXT    NOT NULL,
    stored_path     TEXT    NOT NULL UNIQUE,
    mime_type       TEXT    NOT NULL CHECK (mime_type IN ('image/png', 'image/svg+xml')),
    size_bytes      INTEGER NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    sha256          TEXT    NOT NULL CHECK (length(sha256) = 64),
    -- El cliente declara que tiene derecho a usar el logo.
    rights_declared INTEGER NOT NULL CHECK (rights_declared = 1),
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_design_assets_user ON design_assets (user_id);

CREATE TABLE designs (
    id                  INTEGER PRIMARY KEY,
    user_id             INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id          INTEGER REFERENCES products (id) ON DELETE SET NULL,
    name                TEXT    NOT NULL CHECK (length(name) BETWEEN 1 AND 80),
    current_version     INTEGER NOT NULL DEFAULT 1 CHECK (current_version >= 1),
    thumbnail_front     TEXT,
    thumbnail_back      TEXT,
    deleted_at          TEXT,
    created_at          TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at          TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_designs_user ON designs (user_id, deleted_at);

CREATE TABLE design_versions (
    id             INTEGER PRIMARY KEY,
    design_id      INTEGER NOT NULL REFERENCES designs (id) ON DELETE CASCADE,
    version        INTEGER NOT NULL CHECK (version >= 1),
    schema_version INTEGER NOT NULL CHECK (schema_version >= 1),
    document_json  TEXT    NOT NULL CHECK (json_valid(document_json)),
    created_at     TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    UNIQUE (design_id, version)
);

CREATE TABLE design_elements (
    id                INTEGER PRIMARY KEY,
    design_version_id INTEGER NOT NULL REFERENCES design_versions (id) ON DELETE CASCADE,
    element_type      TEXT    NOT NULL CHECK (element_type IN ('TEXT', 'NUMBER', 'EMBLEM', 'UPLOAD')),
    garment           TEXT    NOT NULL CHECK (garment IN ('JERSEY', 'SHORTS', 'SOCKS', 'SHOES', 'HOODIE', 'ACCESSORY')),
    zone              TEXT    NOT NULL,
    content           TEXT,
    asset_id          INTEGER REFERENCES design_assets (id) ON DELETE RESTRICT,
    pos_u             REAL    NOT NULL CHECK (pos_u BETWEEN 0 AND 1),
    pos_v             REAL    NOT NULL CHECK (pos_v BETWEEN 0 AND 1),
    scale             REAL    NOT NULL DEFAULT 1 CHECK (scale BETWEEN 0.2 AND 2.0),
    rotation_deg      REAL    NOT NULL DEFAULT 0 CHECK (rotation_deg >= -360 AND rotation_deg <= 360),
    layer_order       INTEGER NOT NULL DEFAULT 0,
    visible           INTEGER NOT NULL DEFAULT 1 CHECK (visible IN (0, 1)),
    locked            INTEGER NOT NULL DEFAULT 0 CHECK (locked IN (0, 1))
);
CREATE INDEX idx_design_elements_version ON design_elements (design_version_id, layer_order);
CREATE INDEX idx_design_elements_asset ON design_elements (asset_id);

-- ---------------------------------------------------------------------------
-- Carrito
-- ---------------------------------------------------------------------------

CREATE TABLE carts (
    id         INTEGER PRIMARY KEY,
    user_id    INTEGER NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    created_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

CREATE TABLE cart_items (
    id               INTEGER PRIMARY KEY,
    cart_id          INTEGER NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    variant_id       INTEGER NOT NULL REFERENCES product_variants (id) ON DELETE CASCADE,
    design_id        INTEGER REFERENCES designs (id) ON DELETE SET NULL,
    quantity         INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents >= 0),
    surcharge_cents  INTEGER NOT NULL DEFAULT 0 CHECK (surcharge_cents >= 0),
    created_at       TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at       TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_cart_items_variant ON cart_items (variant_id);
CREATE INDEX idx_cart_items_design ON cart_items (design_id);

-- ---------------------------------------------------------------------------
-- Pedidos y pagos
-- ---------------------------------------------------------------------------

-- Correlativo anual para PED-2026-000154.
CREATE TABLE order_counters (
    year       INTEGER PRIMARY KEY CHECK (year BETWEEN 2000 AND 9999),
    last_value INTEGER NOT NULL DEFAULT 0 CHECK (last_value >= 0)
);

CREATE TABLE orders (
    id                  INTEGER PRIMARY KEY,
    order_number        TEXT    NOT NULL UNIQUE CHECK (order_number GLOB 'PED-[0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9][0-9][0-9]'),
    user_id             INTEGER NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status              TEXT    NOT NULL DEFAULT 'PENDIENTE_PAGO'
                                CHECK (status IN ('PENDIENTE_PAGO', 'PAGO_EN_VERIFICACION', 'PAGADO', 'EN_PRODUCCION', 'ENVIADO', 'ENTREGADO', 'CANCELADO')),
    payment_method      TEXT    NOT NULL CHECK (payment_method IN ('YAPE', 'PLIN', 'TRANSFERENCIA', 'TARJETA')),
    subtotal_cents      INTEGER NOT NULL CHECK (subtotal_cents >= 0),
    surcharge_cents     INTEGER NOT NULL DEFAULT 0 CHECK (surcharge_cents >= 0),
    shipping_cents      INTEGER NOT NULL DEFAULT 0 CHECK (shipping_cents >= 0),
    total_cents         INTEGER NOT NULL,
    currency            TEXT    NOT NULL DEFAULT 'PEN' CHECK (currency = 'PEN'),
    -- Copia congelada de la dirección de envío.
    ship_recipient      TEXT    NOT NULL,
    ship_phone          TEXT    NOT NULL,
    ship_department     TEXT    NOT NULL,
    ship_province       TEXT    NOT NULL,
    ship_district       TEXT    NOT NULL,
    ship_line1          TEXT    NOT NULL,
    ship_reference      TEXT,
    notes               TEXT,
    placed_at           TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    created_at          TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at          TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    CHECK (total_cents = subtotal_cents + surcharge_cents + shipping_cents)
);
CREATE INDEX idx_orders_user ON orders (user_id, placed_at);
CREATE INDEX idx_orders_status ON orders (status, placed_at);

CREATE TABLE order_items (
    id                   INTEGER PRIMARY KEY,
    order_id             INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id           INTEGER NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    variant_id           INTEGER REFERENCES product_variants (id) ON DELETE SET NULL,
    design_id            INTEGER REFERENCES designs (id) ON DELETE SET NULL,
    -- Datos congelados al momento de la compra.
    product_name         TEXT    NOT NULL,
    sku                  TEXT    NOT NULL,
    size_code            TEXT    NOT NULL,
    color_hex            TEXT    NOT NULL,
    quantity             INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_cents     INTEGER NOT NULL CHECK (unit_price_cents >= 0),
    surcharge_cents      INTEGER NOT NULL DEFAULT 0 CHECK (surcharge_cents >= 0),
    line_total_cents     INTEGER NOT NULL,
    design_snapshot_json TEXT    CHECK (design_snapshot_json IS NULL OR json_valid(design_snapshot_json)),
    design_front_path    TEXT,
    design_back_path     TEXT,
    CHECK (line_total_cents = (unit_price_cents + surcharge_cents) * quantity)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

CREATE TABLE order_status_history (
    id          INTEGER PRIMARY KEY,
    order_id    INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status TEXT,
    to_status   TEXT    NOT NULL CHECK (to_status IN ('PENDIENTE_PAGO', 'PAGO_EN_VERIFICACION', 'PAGADO', 'EN_PRODUCCION', 'ENVIADO', 'ENTREGADO', 'CANCELADO')),
    changed_by  INTEGER REFERENCES users (id) ON DELETE SET NULL,
    note        TEXT,
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_order_status_history_order ON order_status_history (order_id, created_at);

CREATE TABLE payments (
    id                 INTEGER PRIMARY KEY,
    order_id           INTEGER NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    method             TEXT    NOT NULL CHECK (method IN ('YAPE', 'PLIN', 'TRANSFERENCIA', 'TARJETA')),
    status             TEXT    NOT NULL DEFAULT 'PENDIENTE' CHECK (status IN ('PENDIENTE', 'EN_VERIFICACION', 'APROBADO', 'RECHAZADO')),
    amount_cents       INTEGER NOT NULL CHECK (amount_cents > 0),
    operation_code     TEXT    CHECK (operation_code IS NULL OR length(operation_code) BETWEEN 4 AND 40),
    provider_reference TEXT,
    verified_by        INTEGER REFERENCES users (id) ON DELETE SET NULL,
    verified_at        TEXT,
    rejection_reason   TEXT,
    created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    updated_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    -- Un pago aprobado siempre tiene quién y cuándo lo verificó.
    CHECK (status <> 'APROBADO' OR (verified_by IS NOT NULL AND verified_at IS NOT NULL)),
    CHECK (status <> 'RECHAZADO' OR rejection_reason IS NOT NULL)
);
CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status, created_at);

CREATE TABLE payment_proofs (
    id          INTEGER PRIMARY KEY,
    payment_id  INTEGER NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    stored_path TEXT    NOT NULL UNIQUE,
    mime_type   TEXT    NOT NULL CHECK (mime_type IN ('image/png', 'image/jpeg', 'application/pdf')),
    size_bytes  INTEGER NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
CREATE INDEX idx_payment_proofs_payment ON payment_proofs (payment_id);

-- ---------------------------------------------------------------------------
-- Triggers updated_at
-- ---------------------------------------------------------------------------

CREATE TRIGGER trg_users_updated AFTER UPDATE ON users FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE users SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_customers_updated AFTER UPDATE ON customers FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE customers SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_addresses_updated AFTER UPDATE ON addresses FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE addresses SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_categories_updated AFTER UPDATE ON categories FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE categories SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_products_updated AFTER UPDATE ON products FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE products SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_product_variants_updated AFTER UPDATE ON product_variants FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE product_variants SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_inventory_updated AFTER UPDATE ON inventory FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE inventory SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE variant_id = NEW.variant_id;
END;

CREATE TRIGGER trg_designs_updated AFTER UPDATE ON designs FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE designs SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_carts_updated AFTER UPDATE ON carts FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE carts SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_cart_items_updated AFTER UPDATE ON cart_items FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE cart_items SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_orders_updated AFTER UPDATE ON orders FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE orders SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;

CREATE TRIGGER trg_payments_updated AFTER UPDATE ON payments FOR EACH ROW WHEN NEW.updated_at = OLD.updated_at
BEGIN
    UPDATE payments SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = NEW.id;
END;
