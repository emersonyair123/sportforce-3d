package com.sportforce3d.service;

import com.sportforce3d.model.User;
import com.sportforce3d.repository.UserRepository;
import com.sportforce3d.util.Validators;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.sportforce3d.service.AuthService.putIfError;
import static com.sportforce3d.service.AuthService.wipe;

/** Operaciones del usuario conectado sobre su propia cuenta. */
public class AccountService {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Session session;

    public AccountService(UserRepository users, PasswordHasher hasher, Session session) {
        this.users = users;
        this.hasher = hasher;
        this.session = session;
    }

    public User updateProfile(String fullName, String phone) {
        User me = session.requireUser();
        Map<String, String> errors = new LinkedHashMap<>();
        putIfError(errors, "fullName", Validators.fullName(fullName));
        putIfError(errors, "phone", Validators.phone(phone));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        users.updateProfile(me.id(), fullName.trim(), Validators.blankToNull(phone));
        User fresh = users.findById(me.id()).orElseThrow();
        session.refresh(fresh);
        return fresh;
    }

    public void changePassword(char[] current, char[] next, char[] confirmation) {
        try {
            User me = session.requireUser();
            Map<String, String> errors = new LinkedHashMap<>();
            if (current == null || current.length == 0) {
                errors.put("current", "Ingresa tu contraseña actual.");
            }
            putIfError(errors, "next", Validators.newPassword(next));
            if (next != null && !Arrays.equals(next, confirmation)) {
                errors.put("confirmation", "Las contraseñas no coinciden.");
            }
            if (!errors.isEmpty()) {
                throw new ValidationException(errors);
            }
            String storedHash = users.findStoredById(me.id())
                    .orElseThrow(() -> new AuthenticationException("Tu cuenta ya no existe."))
                    .passwordHash();
            if (!hasher.verify(current.clone(), storedHash)) {
                throw ValidationException.of("current", "La contraseña actual no es correcta.");
            }
            if (Arrays.equals(current, next)) {
                throw ValidationException.of("next", "La nueva contraseña debe ser distinta de la actual.");
            }
            users.updatePassword(me.id(), hasher.hash(next.clone()));
        } finally {
            wipe(current);
            wipe(next);
            wipe(confirmation);
        }
    }
}
