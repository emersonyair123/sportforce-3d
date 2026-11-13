package com.sportforce3d.service;

import com.sportforce3d.model.PpqhPaymentInstructions;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepySettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PpqhStoreSettingsService {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhStoreSettingsService.class);
    private static final Pattern PPQH_MOBILE = Pattern.compile("^9\\d{8}$");
    private static final Pattern PPQH_NAME = Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 .,&'()\\-–—]*$");
    private static final Pattern PPQH_ACCOUNT = Pattern.compile("^[0-9][0-9 -]*[0-9]$");
    public static final int PPQH_HOLDER_MAX = 80;
    public static final int PPQH_BANK_MAX = 60;
    public static final int PPQH_ACCOUNT_MIN_DIGITS = 10;
    public static final int PPQH_ACCOUNT_MAX_DIGITS = 20;
    public static final int PPQH_CCI_DIGITS = 20;

    private final HepySettingsRepository ppqhSettings;
    private final PpqhSession ppqhSession;
    private final Clock ppqhClock;

    public PpqhStoreSettingsService(HepySettingsRepository ppqhSettings, PpqhSession ppqhSession, Clock ppqhClock) {
        this.ppqhSettings = ppqhSettings;
        this.ppqhSession = ppqhSession;
        this.ppqhClock = ppqhClock;
    }

    public PpqhPaymentInstructions ppqhPaymentSettings() {
        ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        return ppqhSettings.hepyPaymentInstructions();
    }

    public PpqhPaymentInstructions ppqhSavePaymentSettings(PpqhPaymentInstructions ppqhForm) {
        PpqhUser ppqhAdmin = ppqhSession.ppqhRequireRole(PpqhRole.ADMIN);
        if (ppqhForm == null) {
            throw new PpqhValidationException(Map.of());
        }
        Map<String, String> ppqhErrors = new LinkedHashMap<>();
        String ppqhYapePhone = ppqhMobile(ppqhErrors, "yapePhone", ppqhForm.ppqhYapePhone(), "Yape");
        String ppqhYapeHolder = ppqhName(ppqhErrors, "yapeHolder", ppqhForm.ppqhYapeHolder(), PPQH_HOLDER_MAX, "el titular de Yape");
        String ppqhPlinPhone = ppqhMobile(ppqhErrors, "plinPhone", ppqhForm.ppqhPlinPhone(), "Plin");
        String ppqhPlinHolder = ppqhName(ppqhErrors, "plinHolder", ppqhForm.ppqhPlinHolder(), PPQH_HOLDER_MAX, "el titular de Plin");
        String ppqhBankName = ppqhName(ppqhErrors, "bankName", ppqhForm.ppqhBankName(), PPQH_BANK_MAX, "el banco");
        String ppqhBankHolder = ppqhName(ppqhErrors, "bankHolder", ppqhForm.ppqhBankHolder(), PPQH_HOLDER_MAX, "el titular de la cuenta");
        String ppqhAccount = ppqhAccount(ppqhErrors, ppqhForm.ppqhBankAccount());
        String ppqhCci = ppqhCci(ppqhErrors, ppqhForm.ppqhBankCci());
        if (!ppqhErrors.isEmpty()) {
            throw new PpqhValidationException(ppqhErrors);
        }
        PpqhPaymentInstructions ppqhClean = new PpqhPaymentInstructions(ppqhYapePhone, ppqhYapeHolder, ppqhPlinPhone, ppqhPlinHolder, ppqhBankName,
                ppqhBankHolder, ppqhAccount, ppqhCci, true);
        ppqhSettings.hepySavePaymentInstructions(ppqhClean, ppqhClock.instant().toString());
        ppqhLog.info("ADMIN {} actualizó los datos de pago", ppqhAdmin.ppqhId());
        return ppqhSettings.hepyPaymentInstructions();
    }

    static String ppqhMobile(Map<String, String> ppqhErrors, String ppqhField, String ppqhValue, String ppqhMethod) {
        String ppqhDigits = ppqhValue == null ? "" : ppqhValue.replaceAll("[\\s-]", "");
        if (ppqhDigits.startsWith("+51")) {
            ppqhDigits = ppqhDigits.substring(3);
        }
        if (ppqhDigits.isEmpty()) {
            ppqhErrors.put(ppqhField, "Ingresa el número de " + ppqhMethod + ".");
            return null;
        }
        if (!PPQH_MOBILE.matcher(ppqhDigits).matches()) {
            ppqhErrors.put(ppqhField, "Ingresa un celular de 9 dígitos que empiece con 9, p. ej. 987 654 321.");
            return null;
        }
        return ppqhDigits.substring(0, 3) + " " + ppqhDigits.substring(3, 6) + " " + ppqhDigits.substring(6);
    }

    static String ppqhName(Map<String, String> ppqhErrors, String ppqhField, String ppqhValue, int ppqhMax, String ppqhWhat) {
        String ppqhClean = ppqhValue == null ? "" : ppqhValue.trim().replaceAll("\\s+", " ");
        if (ppqhClean.length() < 3) {
            ppqhErrors.put(ppqhField, "Ingresa " + ppqhWhat + " (mínimo 3 caracteres).");
            return null;
        }
        if (ppqhClean.length() > ppqhMax) {
            ppqhErrors.put(ppqhField, "Admite hasta " + ppqhMax + " caracteres.");
            return null;
        }
        if (!PPQH_NAME.matcher(ppqhClean).matches()) {
            ppqhErrors.put(ppqhField, "Usa solo letras, números, espacios y . , & ' ( ) -");
            return null;
        }
        return ppqhClean;
    }

    static String ppqhAccount(Map<String, String> ppqhErrors, String ppqhValue) {
        String ppqhClean = ppqhValue == null ? "" : ppqhValue.trim().replaceAll("\\s+", " ");
        if (ppqhClean.isEmpty()) {
            ppqhErrors.put("bankAccount", "Ingresa el número de cuenta.");
            return null;
        }
        int ppqhDigits = ppqhClean.replaceAll("\\D", "").length();
        if (!PPQH_ACCOUNT.matcher(ppqhClean).matches() || ppqhDigits < PPQH_ACCOUNT_MIN_DIGITS || ppqhDigits > PPQH_ACCOUNT_MAX_DIGITS) {
            ppqhErrors.put("bankAccount", "Usa de " + PPQH_ACCOUNT_MIN_DIGITS + " a " + PPQH_ACCOUNT_MAX_DIGITS
                    + " dígitos; puedes separarlos con guiones.");
            return null;
        }
        return ppqhClean;
    }

    static String ppqhCci(Map<String, String> ppqhErrors, String ppqhValue) {
        String ppqhDigits = ppqhValue == null ? "" : ppqhValue.trim().replaceAll("[\\s-]", "");
        if (ppqhDigits.isEmpty()) {
            ppqhErrors.put("bankCci", "Ingresa el CCI de la cuenta.");
            return null;
        }
        if (!ppqhDigits.matches("\\d{" + PPQH_CCI_DIGITS + "}")) {
            ppqhErrors.put("bankCci", "El CCI tiene exactamente " + PPQH_CCI_DIGITS + " dígitos.");
            return null;
        }
        return ppqhDigits.substring(0, 3) + "-" + ppqhDigits.substring(3, 6) + "-" + ppqhDigits.substring(6, 18) + "-"
                + ppqhDigits.substring(18);
    }
}
