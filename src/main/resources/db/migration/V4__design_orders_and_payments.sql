INSERT INTO store_settings (key, value) VALUES
    ('payment_holder', 'SPORTFORCE 3D S.A.C. — datos de prueba'),
    ('yape_phone', '987 654 321'),
    ('plin_phone', '987 654 321'),
    ('bank_name', 'Banco de prueba'),
    ('bank_account', '191-0000000-0-00'),
    ('bank_cci', '002-191-000000000000-00');

ALTER TABLE order_items ADD COLUMN design_name TEXT;
ALTER TABLE order_items ADD COLUMN design_version INTEGER CHECK (design_version IS NULL OR design_version >= 1);

CREATE UNIQUE INDEX ux_payments_operation_code ON payments (method, operation_code)
    WHERE operation_code IS NOT NULL AND status IN ('EN_VERIFICACION', 'APROBADO');

CREATE INDEX idx_payments_verified ON payments (status, verified_at);
