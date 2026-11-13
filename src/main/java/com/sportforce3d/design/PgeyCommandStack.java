package com.sportforce3d.design;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class PgeyCommandStack {

    public static final long PGEY_MERGE_WINDOW_MS = 800;
    public static final int PGEY_LIMIT = 100;

    public record PgeyCommand(String pgeyLabel, PgeyDesignDocument pgeyBefore, PgeyDesignDocument pgeyAfter, String pgeyMergeKey, long pgeyAt) {
    }

    private final Deque<PgeyCommand> pgeyUndo = new ArrayDeque<>();
    private final Deque<PgeyCommand> pgeyRedo = new ArrayDeque<>();
    private final Clock pgeyClock;
    private PgeyDesignDocument pgeyCurrent;
    private boolean pgeySealed = true;

    public PgeyCommandStack(PgeyDesignDocument pgeyInitial, Clock pgeyClock) {
        this.pgeyCurrent = Objects.requireNonNull(pgeyInitial);
        this.pgeyClock = pgeyClock;
    }

    public PgeyDesignDocument pgeyCurrent() {
        return pgeyCurrent;
    }

    public PgeyDesignDocument pgeyExecute(String pgeyLabel, UnaryOperator<PgeyDesignDocument> pgeyChange, String pgeyMergeKey) {
        PgeyDesignDocument pgeyAfter = pgeyChange.apply(pgeyCurrent);
        if (pgeyAfter.equals(pgeyCurrent)) {
            return pgeyCurrent;
        }
        long pgeyNow = pgeyClock.millis();
        PgeyCommand pgeyTop = pgeyUndo.peek();
        if (pgeyMergeKey != null && !pgeySealed && pgeyTop != null && pgeyMergeKey.equals(pgeyTop.pgeyMergeKey())
                && pgeyNow - pgeyTop.pgeyAt() <= PGEY_MERGE_WINDOW_MS) {
            pgeyUndo.pop();
            pgeyUndo.push(new PgeyCommand(pgeyTop.pgeyLabel(), pgeyTop.pgeyBefore(), pgeyAfter, pgeyMergeKey, pgeyNow));
        } else {
            pgeyUndo.push(new PgeyCommand(pgeyLabel, pgeyCurrent, pgeyAfter, pgeyMergeKey, pgeyNow));
            while (pgeyUndo.size() > PGEY_LIMIT) {
                pgeyUndo.removeLast();
            }
        }
        pgeySealed = pgeyMergeKey == null;
        pgeyRedo.clear();
        pgeyCurrent = pgeyAfter;
        return pgeyCurrent;
    }

    public void pgeySeal() {
        pgeySealed = true;
    }

    public boolean pgeyCanUndo() {
        return !pgeyUndo.isEmpty();
    }

    public boolean pgeyCanRedo() {
        return !pgeyRedo.isEmpty();
    }

    public String pgeyUndoLabel() {
        return pgeyUndo.isEmpty() ? null : pgeyUndo.peek().pgeyLabel();
    }

    public String pgeyRedoLabel() {
        return pgeyRedo.isEmpty() ? null : pgeyRedo.peek().pgeyLabel();
    }

    public PgeyDesignDocument pgeyUndo() {
        if (pgeyUndo.isEmpty()) {
            return pgeyCurrent;
        }
        PgeyCommand pgeyC = pgeyUndo.pop();
        pgeyRedo.push(pgeyC);
        pgeyCurrent = pgeyC.pgeyBefore();
        pgeySealed = true;
        return pgeyCurrent;
    }

    public PgeyDesignDocument pgeyRedo() {
        if (pgeyRedo.isEmpty()) {
            return pgeyCurrent;
        }
        PgeyCommand pgeyC = pgeyRedo.pop();
        pgeyUndo.push(pgeyC);
        pgeyCurrent = pgeyC.pgeyAfter();
        pgeySealed = true;
        return pgeyCurrent;
    }

    public void pgeyReset(PgeyDesignDocument pgeyDoc) {
        pgeyUndo.clear();
        pgeyRedo.clear();
        pgeyCurrent = Objects.requireNonNull(pgeyDoc);
        pgeySealed = true;
    }

    public int pgeyUndoSize() {
        return pgeyUndo.size();
    }
}
