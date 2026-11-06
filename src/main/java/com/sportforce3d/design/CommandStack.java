package com.sportforce3d.design;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Historial deshacer/rehacer con el patrón Command. Cada comando aplica una transformación al diseño y guarda
 * el estado anterior (memento): como {@link DesignDocument} es inmutable, deshacer es exacto y barato.
 *
 * <p>Los cambios continuos (arrastrar un logo, mover un deslizador) usan una {@code mergeKey}: si llegan
 * seguidos (menos de {@link #MERGE_WINDOW_MS} entre sí) y con la misma clave, se fusionan en un solo paso.
 */
public final class CommandStack {

    public static final long MERGE_WINDOW_MS = 800;
    public static final int LIMIT = 100;

    /** Un paso del historial. */
    public record Command(String label, DesignDocument before, DesignDocument after, String mergeKey, long at) {
    }

    private final Deque<Command> undo = new ArrayDeque<>();
    private final Deque<Command> redo = new ArrayDeque<>();
    private final Clock clock;
    private DesignDocument current;
    private boolean sealed = true;

    public CommandStack(DesignDocument initial, Clock clock) {
        this.current = Objects.requireNonNull(initial);
        this.clock = clock;
    }

    public DesignDocument current() {
        return current;
    }

    /**
     * Ejecuta un cambio. Si no cambia nada, no se registra. Devuelve el documento resultante.
     *
     * @param mergeKey null para pasos independientes
     */
    public DesignDocument execute(String label, UnaryOperator<DesignDocument> change, String mergeKey) {
        DesignDocument after = change.apply(current);
        if (after.equals(current)) {
            return current;
        }
        long now = clock.millis();
        Command top = undo.peek();
        if (mergeKey != null && !sealed && top != null && mergeKey.equals(top.mergeKey())
                && now - top.at() <= MERGE_WINDOW_MS) {
            undo.pop();
            undo.push(new Command(top.label(), top.before(), after, mergeKey, now));
        } else {
            undo.push(new Command(label, current, after, mergeKey, now));
            while (undo.size() > LIMIT) {
                undo.removeLast();
            }
        }
        sealed = mergeKey == null;
        redo.clear();
        current = after;
        return current;
    }

    /** Cierra el paso en curso (fin de un arrastre): el próximo cambio con la misma clave será un paso nuevo. */
    public void seal() {
        sealed = true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public String undoLabel() {
        return undo.isEmpty() ? null : undo.peek().label();
    }

    public String redoLabel() {
        return redo.isEmpty() ? null : redo.peek().label();
    }

    public DesignDocument undo() {
        if (undo.isEmpty()) {
            return current;
        }
        Command c = undo.pop();
        redo.push(c);
        current = c.before();
        sealed = true;
        return current;
    }

    public DesignDocument redo() {
        if (redo.isEmpty()) {
            return current;
        }
        Command c = redo.pop();
        undo.push(c);
        current = c.after();
        sealed = true;
        return current;
    }

    /** Reemplaza el diseño sin historial (abrir un diseño guardado o recuperar un borrador). */
    public void reset(DesignDocument doc) {
        undo.clear();
        redo.clear();
        current = Objects.requireNonNull(doc);
        sealed = true;
    }

    public int undoSize() {
        return undo.size();
    }
}
