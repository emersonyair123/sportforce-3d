package com.sportforce3d.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.sportforce3d.util.Validators;

import java.util.Arrays;

/** BCrypt con coste 12. Los arreglos de caracteres recibidos se limpian después de usarse. */
public class PasswordHasher {

    public static final int DEFAULT_COST = 12;

    private final int cost;
    private final String dummyHash;

    public PasswordHasher() {
        this(DEFAULT_COST);
    }

    public PasswordHasher(int cost) {
        this.cost = cost;
        // Se usa cuando el correo no existe, para que la respuesta tarde lo mismo y no revele qué cuentas existen.
        this.dummyHash = BCrypt.withDefaults().hashToString(cost, "sportforce-dummy".toCharArray());
    }

    public String hash(char[] password) {
        try {
            return BCrypt.withDefaults().hashToString(cost, password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    public boolean verify(char[] password, String hash) {
        try {
            // Más de 72 bytes nunca pudo registrarse (Validators.newPassword); BCrypt lanzaría una excepción.
            if (Validators.utf8Length(password) > Validators.PASSWORD_MAX) {
                BCrypt.verifyer().verify("x".toCharArray(), dummyHash);
                return false;
            }
            return BCrypt.verifyer().verify(password, hash == null ? dummyHash : hash).verified && hash != null;
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
