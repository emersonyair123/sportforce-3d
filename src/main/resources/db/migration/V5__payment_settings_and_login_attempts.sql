-- SPORTFORCE 3D · Etapa 6: datos de pago editables por el ADMIN y bloqueo de inicio de sesión persistente.

-- Cada método tiene su titular (antes había uno solo, payment_holder). Se copian los valores actuales.
INSERT INTO store_settings (key, value) SELECT 'yape_holder', value FROM store_settings WHERE key = 'payment_holder';
INSERT INTO store_settings (key, value) SELECT 'plin_holder', value FROM store_settings WHERE key = 'payment_holder';
INSERT INTO store_settings (key, value) SELECT 'bank_holder', value FROM store_settings WHERE key = 'payment_holder';
DELETE FROM store_settings WHERE key = 'payment_holder';

-- 0 mientras sigan los datos de prueba; Administración → Configuración lo pone en 1 al guardar.
INSERT INTO store_settings (key, value) VALUES ('payment_settings_reviewed', '0');

-- Intentos fallidos por correo: el bloqueo de 5 intentos sobrevive a reiniciar la aplicación.
CREATE TABLE login_attempts (
    email        TEXT    PRIMARY KEY COLLATE NOCASE,
    failures     INTEGER NOT NULL DEFAULT 0 CHECK (failures >= 0),
    locked_until TEXT,
    updated_at   TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);