package com.sportforce3d.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PpqhLoginThrottle {

    public static final int PPQH_MAX_FAILURES = 5;
    public static final Duration PPQH_LOCK_TIME = Duration.ofMinutes(5);

    public record PpqhAttempts(int ppqhFailures, Instant ppqhLockedUntil) {
    }

    public interface PpqhStore {
        Optional<PpqhAttempts> ppqhFind(String ppqhEmail);

        void ppqhSave(String ppqhEmail, PpqhAttempts ppqhAttempts);

        void ppqhDelete(String ppqhEmail);
    }

    private final Clock ppqhClock;
    private final PpqhStore ppqhStore;

    public PpqhLoginThrottle(Clock ppqhClock) {
        this(ppqhClock, new PpqhMemoryStore());
    }

    public PpqhLoginThrottle(Clock ppqhClock, PpqhStore ppqhStore) {
        this.ppqhClock = ppqhClock;
        this.ppqhStore = ppqhStore;
    }

    public synchronized void ppqhCheck(String ppqhEmail) {
        String ppqhKey = ppqhKey(ppqhEmail);
        Optional<PpqhAttempts> ppqhA = ppqhStore.ppqhFind(ppqhKey);
        if (ppqhA.isPresent() && ppqhA.get().ppqhLockedUntil() != null) {
            Duration ppqhLeft = Duration.between(ppqhClock.instant(), ppqhA.get().ppqhLockedUntil());
            if (!ppqhLeft.isNegative()) {
                throw new PpqhAuthenticationException("Demasiados intentos fallidos. Por seguridad, espera "
                        + ppqhDescribe(ppqhLeft) + " e inténtalo de nuevo.");
            }
            ppqhStore.ppqhDelete(ppqhKey);
        }
    }

    public synchronized void ppqhRecordFailure(String ppqhEmail) {
        String ppqhKey = ppqhKey(ppqhEmail);
        int ppqhFailures = ppqhStore.ppqhFind(ppqhKey).filter(ppqhA -> ppqhA.ppqhLockedUntil() == null).map(PpqhAttempts::ppqhFailures).orElse(0) + 1;
        if (ppqhFailures >= PPQH_MAX_FAILURES) {
            ppqhStore.ppqhSave(ppqhKey, new PpqhAttempts(0, ppqhClock.instant().plus(PPQH_LOCK_TIME)));
        } else {
            ppqhStore.ppqhSave(ppqhKey, new PpqhAttempts(ppqhFailures, null));
        }
    }

    public synchronized void ppqhRecordSuccess(String ppqhEmail) {
        ppqhStore.ppqhDelete(ppqhKey(ppqhEmail));
    }

    static String ppqhDescribe(Duration ppqhLeft) {
        long ppqhSeconds = Math.max(1, ppqhLeft.toSeconds() + (ppqhLeft.toNanosPart() > 0 ? 1 : 0));
        if (ppqhSeconds < 60) {
            return ppqhSeconds + " s";
        }
        long ppqhMinutes = (ppqhSeconds + 59) / 60;
        return ppqhMinutes == 1 ? "1 minuto" : ppqhMinutes + " minutos";
    }

    private static String ppqhKey(String ppqhEmail) {
        return ppqhEmail == null ? "" : ppqhEmail.trim().toLowerCase(Locale.ROOT);
    }

    private static final class PpqhMemoryStore implements PpqhStore {
        private final Map<String, PpqhAttempts> ppqhMap = new ConcurrentHashMap<>();

        @Override
        public Optional<PpqhAttempts> ppqhFind(String ppqhEmail) {
            return Optional.ofNullable(ppqhMap.get(ppqhEmail));
        }

        @Override
        public void ppqhSave(String ppqhEmail, PpqhAttempts ppqhAttempts) {
            ppqhMap.put(ppqhEmail, ppqhAttempts);
        }

        @Override
        public void ppqhDelete(String ppqhEmail) {
            ppqhMap.remove(ppqhEmail);
        }
    }
}
