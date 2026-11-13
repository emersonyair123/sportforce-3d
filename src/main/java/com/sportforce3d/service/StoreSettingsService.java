package com.sportforce3d.service;

import com.sportforce3d.model.PaymentInstructions;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.SettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StoreSettingsService {

    private static final Logger log = LoggerFactory.getLogger(StoreSettingsService.class);
    private static final Pattern MOBILE = Pattern.compile("^9\\d{8}$");
    private static final Pattern NAME = Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 .,&'()\\-–—]*$");
    private static final Pattern ACCOUNT = Pattern.compile("^[0-9][0-9 -]*[0-9]$");
    public static final int HOLDER_MAX = 80;
    public static final int BANK_MAX = 60;
    public static final int ACCOUNT_MIN_DIGITS = 10;
    public static final int ACCOUNT_MAX_DIGITS = 20;
    public static final int CCI_DIGITS = 20;

    private final SettingsRepository settings;
    private final Session session;
    private final Clock clock;

    public StoreSettingsService(SettingsRepository settings, Session session, Clock clock) {
        this.settings = settings;
        this.session = session;
        this.clock = clock;
    }

    public PaymentInstructions paymentSettings() {
        session.requireRole(Role.ADMIN);
        return settings.paymentInstructions();
    }

    public PaymentInstructions savePaymentSettings(PaymentInstructions form) {
        User admin = session.requireRole(Role.ADMIN);
        if (form == null) {
            throw new ValidationException(Map.of());
        }
        Map<String, String> errors = new LinkedHashMap<>();
        String yapePhone = mobile(errors, "yapePhone", form.yapePhone(), "Yape");
        String yapeHolder = name(errors, "yapeHolder", form.yapeHolder(), HOLDER_MAX, "el titular de Yape");
        String plinPhone = mobile(errors, "plinPhone", form.plinPhone(), "Plin");
        String plinHolder = name(errors, "plinHolder", form.plinHolder(), HOLDER_MAX, "el titular de Plin");
        String bankName = name(errors, "bankName", form.bankName(), BANK_MAX, "el banco");
        String bankHolder = name(errors, "bankHolder", form.bankHolder(), HOLDER_MAX, "el titular de la cuenta");
        String account = account(errors, form.bankAccount());
        String cci = cci(errors, form.bankCci());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        PaymentInstructions clean = new PaymentInstructions(yapePhone, yapeHolder, plinPhone, plinHolder, bankName,
                bankHolder, account, cci, true);
        settings.savePaymentInstructions(clean, clock.instant().toString());
        log.info("ADMIN {} actualizó los datos de pago", admin.id());
        return settings.paymentInstructions();
    }

    static String mobile(Map<String, String> errors, String field, String value, String method) {
        String digits = value == null ? "" : value.replaceAll("[\\s-]", "");
        if (digits.startsWith("+51")) {
            digits = digits.substring(3);
        }
        if (digits.isEmpty()) {
            errors.put(field, "Ingresa el número de " + method + ".");
            return null;
        }
        if (!MOBILE.matcher(digits).matches()) {
            errors.put(field, "Ingresa un celular de 9 dígitos que empiece con 9, p. ej. 987 654 321.");
            return null;
        }
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }

    static String name(Map<String, String> errors, String field, String value, int max, String what) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.length() < 3) {
            errors.put(field, "Ingresa " + what + " (mínimo 3 caracteres).");
            return null;
        }
        if (clean.length() > max) {
            errors.put(field, "Admite hasta " + max + " caracteres.");
            return null;
        }
        if (!NAME.matcher(clean).matches()) {
            errors.put(field, "Usa solo letras, números, espacios y . , & ' ( ) -");
            return null;
        }
        return clean;
    }

    static String account(Map<String, String> errors, String value) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            errors.put("bankAccount", "Ingresa el número de cuenta.");
            return null;
        }
        int digits = clean.replaceAll("\\D", "").length();
        if (!ACCOUNT.matcher(clean).matches() || digits < ACCOUNT_MIN_DIGITS || digits > ACCOUNT_MAX_DIGITS) {
            errors.put("bankAccount", "Usa de " + ACCOUNT_MIN_DIGITS + " a " + ACCOUNT_MAX_DIGITS
                    + " dígitos; puedes separarlos con guiones.");
            return null;
        }
        return clean;
    }

    static String cci(Map<String, String> errors, String value) {
        String digits = value == null ? "" : value.trim().replaceAll("[\\s-]", "");
        if (digits.isEmpty()) {
            errors.put("bankCci", "Ingresa el CCI de la cuenta.");
            return null;
        }
        if (!digits.matches("\\d{" + CCI_DIGITS + "}")) {
            errors.put("bankCci", "El CCI tiene exactamente " + CCI_DIGITS + " dígitos.");
            return null;
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6, 18) + "-"
                + digits.substring(18);
    }
}
