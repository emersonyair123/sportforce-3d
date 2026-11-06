package com.sportforce3d.service;

import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongFunction;

/**
 * Usuario conectado en esta ventana. Todas las comprobaciones de permisos de la capa service pasan por aquí:
 * ocultar un botón en la vista no basta.
 */
public class Session {

    private final AtomicReference<User> current = new AtomicReference<>();
    /** Relee el usuario de la base de datos (null = confiar en la copia de la sesión, solo en pruebas). */
    private final LongFunction<Optional<User>> reload;

    public Session() {
        this(null);
    }

    /**
     * @param reload busca el usuario por id; {@link #requireRole} lo usa para que quitar el rol ADMIN o desactivar
     *               una cuenta tenga efecto de inmediato, sin esperar a que esa persona cierre sesión
     */
    public Session(LongFunction<Optional<User>> reload) {
        this.reload = reload;
    }

    public Optional<User> current() {
        return Optional.ofNullable(current.get());
    }

    public boolean isAuthenticated() {
        return current.get() != null;
    }

    public User requireUser() {
        User user = current.get();
        if (user == null) {
            throw new AuthenticationException("Inicia sesión para continuar.");
        }
        return user;
    }

    /** Lee la base de datos (si hay {@code reload}): llamar solo desde la capa service, fuera del hilo de JavaFX. */
    public User requireRole(Role role) {
        User user = requireUser();
        if (reload != null) {
            User fresh = reload.apply(user.id()).orElse(null);
            if (fresh == null || !fresh.active()) {
                throw new AuthenticationException("Tu cuenta ya no está activa. Cierra sesión y escribe a la tienda.");
            }
            current.compareAndSet(user, fresh);
            user = fresh;
        }
        if (!user.hasRole(role)) {
            throw new AccessDeniedException("No tienes permiso para realizar esta acción.");
        }
        return user;
    }

    void start(User user) {
        current.set(user);
    }

    void refresh(User user) {
        current.updateAndGet(u -> u != null && u.id() == user.id() ? user : u);
    }

    void end() {
        current.set(null);
    }
}