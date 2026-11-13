package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.PaymentInstructions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsRepository {

    public static final String YAPE_PHONE = "yape_phone";
    public static final String YAPE_HOLDER = "yape_holder";
    public static final String PLIN_PHONE = "plin_phone";
    public static final String PLIN_HOLDER = "plin_holder";
    public static final String BANK_NAME = "bank_name";
    public static final String BANK_HOLDER = "bank_holder";
    public static final String BANK_ACCOUNT = "bank_account";
    public static final String BANK_CCI = "bank_cci";
    public static final String PAYMENT_REVIEWED = "payment_settings_reviewed";

    private final Database db;

    public SettingsRepository(Database db) {
        this.db = db;
    }

    public PaymentInstructions paymentInstructions() {
        return db.withConnection(SettingsRepository::paymentInstructions);
    }

    public static PaymentInstructions paymentInstructions(Connection c) throws SQLException {
        Map<String, String> m = all(c);
        return new PaymentInstructions(m.getOrDefault(YAPE_PHONE, "—"), m.getOrDefault(YAPE_HOLDER, "SPORTFORCE 3D"),
                m.getOrDefault(PLIN_PHONE, "—"), m.getOrDefault(PLIN_HOLDER, "SPORTFORCE 3D"),
                m.getOrDefault(BANK_NAME, "—"), m.getOrDefault(BANK_HOLDER, "SPORTFORCE 3D"),
                m.getOrDefault(BANK_ACCOUNT, "—"), m.getOrDefault(BANK_CCI, "—"),
                "1".equals(m.get(PAYMENT_REVIEWED)));
    }

    public void savePaymentInstructions(PaymentInstructions p, String now) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(YAPE_PHONE, p.yapePhone());
        values.put(YAPE_HOLDER, p.yapeHolder());
        values.put(PLIN_PHONE, p.plinPhone());
        values.put(PLIN_HOLDER, p.plinHolder());
        values.put(BANK_NAME, p.bankName());
        values.put(BANK_HOLDER, p.bankHolder());
        values.put(BANK_ACCOUNT, p.bankAccount());
        values.put(BANK_CCI, p.bankCci());
        values.put(PAYMENT_REVIEWED, "1");
        db.inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO store_settings (key, value, updated_at) VALUES (?, ?, ?)
                    ON CONFLICT (key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
                    """)) {
                for (Map.Entry<String, String> e : values.entrySet()) {
                    Jdbc.bind(ps, List.of(e.getKey(), e.getValue(), now));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return null;
        });
    }

    static Map<String, String> all(Connection c) throws SQLException {
        Map<String, String> m = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT key, value FROM store_settings");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.put(rs.getString(1), rs.getString(2));
            }
        }
        return m;
    }
}
