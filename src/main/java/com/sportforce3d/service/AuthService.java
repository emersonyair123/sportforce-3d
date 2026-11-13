package com.sportforce3d.service;

import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.UserRepository;
import com.sportforce3d.repository.UserRepository.StoredUser;
import com.sportforce3d.util.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String BAD_CREDENTIALS = "Correo o contraseña incorrectos.";

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Session session;
    private final LoginThrottle throttle;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordHasher hasher, Session session, LoginThrottle throttle, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.session = session;
        this.throttle = throttle;
        this.clock = clock;
    }

    public User login(String email, char[] password) {
        String normalized = Validators.normalizeEmail(email);
        if (normalized.isEmpty() || password == null || password.length == 0) {
            wipe(password);
            throw new AuthenticationException("Ingresa tu correo y contraseña.");
        }
        try {
            throttle.check(normalized);
        } catch (AuthenticationException e) {
            wipe(password);
            throw e;
        }

        Optional<StoredUser> stored = users.findByEmail(normalized);
        boolean matches = hasher.verify(password, stored.map(StoredUser::passwordHash).orElse(null));
        if (stored.isEmpty() || !matches) {
            throttle.recordFailure(normalized);
            log.info("Inicio de sesión fallido para {}", normalized);
            throw new AuthenticationException(BAD_CREDENTIALS);
        }
        User user = stored.get().user();
        if (!user.active()) {
            throw new AuthenticationException("Tu cuenta está desactivada. Escribe a la tienda para reactivarla.");
        }
        throttle.recordSuccess(normalized);
        users.touchLastLogin(user.id(), clock.instant());
        User fresh = users.findById(user.id()).orElse(user);
        session.start(fresh);
        log.info("Sesión iniciada: usuario {} {}", fresh.id(), fresh.roles());
        return fresh;
    }

    public User register(String fullName, String email, String phone, char[] password, char[] confirmation) {
        String normalized = Validators.normalizeEmail(email);
        Map<String, String> errors = new LinkedHashMap<>();
        putIfError(errors, "fullName", Validators.fullName(fullName));
        putIfError(errors, "email", Validators.email(normalized));
        putIfError(errors, "phone", Validators.phone(phone));
        putIfError(errors, "password", Validators.newPassword(password));
        if (password != null && !Arrays.equals(password, confirmation)) {
            errors.put("confirmation", "Las contraseñas no coinciden.");
        }
        wipe(confirmation);
        if (errors.isEmpty() && users.existsByEmail(normalized)) {
            errors.put("email", "Ya existe una cuenta con este correo.");
        }
        if (!errors.isEmpty()) {
            wipe(password);
            throw new ValidationException(errors);
        }

        User created = users.create(normalized, hasher.hash(password), fullName.trim(),
                Validators.blankToNull(phone), EnumSet.of(Role.CLIENTE));
        users.touchLastLogin(created.id(), clock.instant());
        User fresh = users.findById(created.id()).orElse(created);
        session.start(fresh);
        log.info("Cuenta CLIENTE creada: usuario {}", fresh.id());
        return fresh;
    }

    public void logout() {
        session.current().ifPresent(u -> log.info("Sesión cerrada: usuario {}", u.id()));
        session.end();
    }

    static void putIfError(Map<String, String> errors, String field, String message) {
        if (message != null) {
            errors.put(field, message);
        }
    }

    static void wipe(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }
}
