package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhPaymentInstructions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HepySettingsRepository {

    public static final String HEPY_YAPE_PHONE = "yape_phone";
    public static final String HEPY_YAPE_HOLDER = "yape_holder";
    public static final String HEPY_PLIN_PHONE = "plin_phone";
    public static final String HEPY_PLIN_HOLDER = "plin_holder";
    public static final String HEPY_BANK_NAME = "bank_name";
    public static final String HEPY_BANK_HOLDER = "bank_holder";
    public static final String HEPY_BANK_ACCOUNT = "bank_account";
    public static final String HEPY_BANK_CCI = "bank_cci";
    public static final String HEPY_PAYMENT_REVIEWED = "payment_settings_reviewed";

    private final HepyDatabase hepyDb;

    public HepySettingsRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public PpqhPaymentInstructions hepyPaymentInstructions() {
        return hepyDb.hepyWithConnection(HepySettingsRepository::hepyPaymentInstructions);
    }

    public static PpqhPaymentInstructions hepyPaymentInstructions(Connection hepyC) throws SQLException {
        Map<String, String> hepyM = hepyAll(hepyC);
        return new PpqhPaymentInstructions(hepyM.getOrDefault(HEPY_YAPE_PHONE, "—"), hepyM.getOrDefault(HEPY_YAPE_HOLDER, "SPORTFORCE 3D"),
                hepyM.getOrDefault(HEPY_PLIN_PHONE, "—"), hepyM.getOrDefault(HEPY_PLIN_HOLDER, "SPORTFORCE 3D"),
                hepyM.getOrDefault(HEPY_BANK_NAME, "—"), hepyM.getOrDefault(HEPY_BANK_HOLDER, "SPORTFORCE 3D"),
                hepyM.getOrDefault(HEPY_BANK_ACCOUNT, "—"), hepyM.getOrDefault(HEPY_BANK_CCI, "—"),
                "1".equals(hepyM.get(HEPY_PAYMENT_REVIEWED)));
    }

    public void hepySavePaymentInstructions(PpqhPaymentInstructions hepyP, String hepyNow) {
        Map<String, String> hepyValues = new LinkedHashMap<>();
        hepyValues.put(HEPY_YAPE_PHONE, hepyP.ppqhYapePhone());
        hepyValues.put(HEPY_YAPE_HOLDER, hepyP.ppqhYapeHolder());
        hepyValues.put(HEPY_PLIN_PHONE, hepyP.ppqhPlinPhone());
        hepyValues.put(HEPY_PLIN_HOLDER, hepyP.ppqhPlinHolder());
        hepyValues.put(HEPY_BANK_NAME, hepyP.ppqhBankName());
        hepyValues.put(HEPY_BANK_HOLDER, hepyP.ppqhBankHolder());
        hepyValues.put(HEPY_BANK_ACCOUNT, hepyP.ppqhBankAccount());
        hepyValues.put(HEPY_BANK_CCI, hepyP.ppqhBankCci());
        hepyValues.put(HEPY_PAYMENT_REVIEWED, "1");
        hepyDb.hepyInTransaction(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                    INSERT INTO store_settings (key, value, updated_at) VALUES (?, ?, ?)
                    ON CONFLICT (key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
                    """)) {
                for (Map.Entry<String, String> hepyE : hepyValues.entrySet()) {
                    HepyJdbc.hepyBind(hepyPs, List.of(hepyE.getKey(), hepyE.getValue(), hepyNow));
                    hepyPs.addBatch();
                }
                hepyPs.executeBatch();
            }
            return null;
        });
    }

    static Map<String, String> hepyAll(Connection hepyC) throws SQLException {
        Map<String, String> hepyM = new HashMap<>();
        try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT key, value FROM store_settings");
             ResultSet hepyRs = hepyPs.executeQuery()) {
            while (hepyRs.next()) {
                hepyM.put(hepyRs.getString(1), hepyRs.getString(2));
            }
        }
        return hepyM;
    }
}
