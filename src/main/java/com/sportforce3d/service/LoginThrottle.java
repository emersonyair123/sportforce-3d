package com.sportforce3d.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo temporal: tras {@value #MAX_FAILURES} fallos seguidos con el mismo correo, ese correo queda bloqueado
 * {@link #LOCK_TIME} aunque luego se escriba la contraseña correcta. Con {@link Store} en la base de datos el bloqueo
 * sobrevive a cerrar y abrir la aplicación.
 */
public class LoginThrottle {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(5);

    /** Fallos seguidos y, si corresponde, hasta cuándo está bloqueado. */
    public record Attempts(int failures, Instant lockedUntil) {
    }

    /** Dónde se guardan los intentos (la app usa la tabla login_attempts). */
    public interface Store {
        Optional<Attempts> find(String email);

        void save(String email, Attempts attempts);

        void delete(String email);
    }

    private final Clock clock;
    private final Store store;

    /** Solo en memoria (pruebas). */
    public LoginThrottle(Clock clock) {
        this(clock, new MemoryStore());
    }

    public LoginThrottle(Clock clock, Store store) {
        this.clock = clock;
        this.store = store;
    }

    /** Lanza AuthenticationException si el correo está bloqueado. */
    public synchronized void check(String email) {
        String key = key(email);
        Optional<Attempts> a = store.find(key);
        if (a.isPresent() && a.get().lockedUntil() != null) {
            Duration left = Duration.between(clock.instant(), a.get().lockedUntil());
            if (!left.isNegative()) {
                throw new AuthenticationException("Demasiados intentos fallidos. Por seguridad, espera "
                        + describe(left) + " e inténtalo de nuevo.");
            }
            store.delete(key);
        }
    }

    public synchronized void recordFailure(String email) {
        String key = key(email);
        int failures = store.find(key).filter(a -> a.lockedUntil() == null).map(Attempts::failures).orElse(0) + 1;
        if (failures >= MAX_FAILURES) {
            store.save(key, new Attempts(0, clock.instant().plus(LOCK_TIME)));
        } else {
            store.save(key, new Attempts(failures, null));
        }
    }

    public synchronized void recordSuccess(String email) {
        store.delete(key(email));
    }

    static String describe(Duration left) {
        long seconds = Math.max(1, left.toSeconds() + (left.toNanosPart() > 0 ? 1 : 0));
        if (seconds < 60) {
            return seconds + " s";
        }
        long minutes = (seconds + 59) / 60;
        return minutes == 1 ? "1 minuto" : minutes + " minutos";
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static final class MemoryStore implements Store {
        private final Map<String, Attempts> map = new ConcurrentHashMap<>();

        @Override
        public Optional<Attempts> find(String email) {
            return Optional.ofNullable(map.get(email));
        }

        @Override
        public void save(String email, Attempts attempts) {
            map.put(email, attempts);
        }

        @Override
        public void delete(String email) {
            map.remove(email);
        }
    }
}
