-- SPORTFORCE 3D · Etapa 5: diseños en el pedido, registro y verificación de pagos.

-- Datos para pagar (los ve el cliente al registrar su pago). Valores de prueba: cámbialos por los reales.
INSERT INTO store_settings (key, value) VALUES
    ('payment_holder', 'SPORTFORCE 3D S.A.C. (datos de prueba)'),
    ('yape_phone', '987 654 321'),
    ('plin_phone', '987 654 321'),
    ('bank_name', 'Banco de prueba'),
    ('bank_account', '191-0000000-0-00'),
    ('bank_cci', '002-191-000000000000-00');

-- Línea de pedido con diseño: nombre y versión congelados (el JSON y las capturas ya existían en V1).
ALTER TABLE order_items ADD COLUMN design_name TEXT;
ALTER TABLE order_items ADD COLUMN design_version INTEGER CHECK (design_version IS NULL OR design_version >= 1);

-- Un mismo código de operación no puede usarse en dos pagos vigentes del mismo método.
CREATE UNIQUE INDEX ux_payments_operation_code ON payments (method, operation_code)
    WHERE operation_code IS NOT NULL AND status IN ('EN_VERIFICACION', 'APROBADO');

-- Dashboard: ventas por día de verificación.
CREATE INDEX idx_payments_verified ON payments (status, verified_at);
