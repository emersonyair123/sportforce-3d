INSERT INTO store_settings (key, value) SELECT 'yape_holder', value FROM store_settings WHERE key = 'payment_holder';
INSERT INTO store_settings (key, value) SELECT 'plin_holder', value FROM store_settings WHERE key = 'payment_holder';
INSERT INTO store_settings (key, value) SELECT 'bank_holder', value FROM store_settings WHERE key = 'payment_holder';
DELETE FROM store_settings WHERE key = 'payment_holder';

INSERT INTO store_settings (key, value) VALUES ('payment_settings_reviewed', '0');

CREATE TABLE login_attempts (
    email        TEXT    PRIMARY KEY COLLATE NOCASE,
    failures     INTEGER NOT NULL DEFAULT 0 CHECK (failures >= 0),
    locked_until TEXT,
    updated_at   TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);
