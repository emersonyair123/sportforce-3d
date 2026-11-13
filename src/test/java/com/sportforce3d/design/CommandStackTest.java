package com.sportforce3d.design;

import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CommandStackTest {
    static final class TestClock extends Clock {
        long millis = 1_000_000;

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }

    private TestClock clock;
    private PgeyDesignDocument initial;
    private PgeyCommandStack stack;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        initial = PgeyDesignDefaults.pgeyDocument(PgeyBodyType.ADULTO_HOMBRE);
        stack = new PgeyCommandStack(initial, clock);
    }

    @Test
    void undoAndRedoRestoreExactStates() {
        PgeyDesignDocument red = stack.pgeyExecute("Color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#D7192A"), null);
        PgeyDesignDocument withName = stack.pgeyExecute("Nombre", d -> d.pgeyAddElement(PgeyDesignDefaults.pgeyName("PAOLO", "#FFFFFF", "#000000")), null);

        assertThat(stack.pgeyCanUndo()).isTrue();
        assertThat(stack.pgeyUndoLabel()).isEqualTo("Nombre");
        assertThat(stack.pgeyUndo()).isEqualTo(red);
        assertThat(stack.pgeyUndo()).isEqualTo(initial);
        assertThat(stack.pgeyCanUndo()).isFalse();
        assertThat(stack.pgeyUndo()).isEqualTo(initial);

        assertThat(stack.pgeyRedo()).isEqualTo(red);
        assertThat(stack.pgeyRedo()).isEqualTo(withName);
        assertThat(stack.pgeyCanRedo()).isFalse();
    }

    @Test
    void newCommandClearsRedo() {
        stack.pgeyExecute("Color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#D7192A"), null);
        stack.pgeyUndo();
        assertThat(stack.pgeyCanRedo()).isTrue();
        stack.pgeyExecute("Otro color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#0E7C3A"), null);
        assertThat(stack.pgeyCanRedo()).isFalse();
    }

    @Test
    void noOpChangesAreNotRecorded() {
        stack.pgeyExecute("Mismo color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#1F5FFF"), null);
        assertThat(stack.pgeyCanUndo()).isFalse();
    }

    @Test
    void continuousChangesWithTheSameKeyMergeIntoOneStep() {
        PgeyDesignElement e = PgeyDesignDefaults.pgeyEmblem("rayo", null, null);
        stack.pgeyExecute("Agregar", d -> d.pgeyAddElement(e), null);
        for (int i = 1; i <= 10; i++) {
            final double u = 0.5 + i * 0.01;
            clock.millis += 50;
            stack.pgeyExecute("Mover", d -> d.pgeyReplaceElement(d.pgeyElement(e.pgeyId()).orElseThrow().pgeyAt(u, 0.3)), "move:" + e.pgeyId());
        }
        assertThat(stack.pgeyUndoSize()).isEqualTo(2);
        PgeyDesignDocument afterDrag = stack.pgeyCurrent();
        stack.pgeyUndo();
        assertThat(stack.pgeyCurrent().pgeyElement(e.pgeyId()).orElseThrow().pgeyU()).isEqualTo(e.pgeyU());
        stack.pgeyRedo();
        assertThat(stack.pgeyCurrent()).isEqualTo(afterDrag);
    }

    @Test
    void mergingStopsAfterSealTimeoutOrDifferentKey() {
        PgeyDesignElement e = PgeyDesignDefaults.pgeyEmblem("rayo", null, null);
        stack.pgeyExecute("Agregar", d -> d.pgeyAddElement(e), null);
        stack.pgeyExecute("Escala", d -> d.pgeyReplaceElement(d.pgeyElement(e.pgeyId()).orElseThrow().pgeyWithScale(1.2)), "scale");
        stack.pgeySeal();
        stack.pgeyExecute("Escala", d -> d.pgeyReplaceElement(d.pgeyElement(e.pgeyId()).orElseThrow().pgeyWithScale(1.4)), "scale");
        clock.millis += PgeyCommandStack.PGEY_MERGE_WINDOW_MS + 1;
        stack.pgeyExecute("Escala", d -> d.pgeyReplaceElement(d.pgeyElement(e.pgeyId()).orElseThrow().pgeyWithScale(1.6)), "scale");
        stack.pgeyExecute("Giro", d -> d.pgeyReplaceElement(d.pgeyElement(e.pgeyId()).orElseThrow().pgeyWithRotation(15)), "rotate");
        assertThat(stack.pgeyUndoSize()).isEqualTo(5);
    }

    @Test
    void historyIsBoundedAndResetClearsIt() {
        for (int i = 0; i < PgeyCommandStack.PGEY_LIMIT + 20; i++) {
            final String hex = String.format("#%06X", i + 1);
            stack.pgeyExecute("Color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, hex), null);
        }
        assertThat(stack.pgeyUndoSize()).isEqualTo(PgeyCommandStack.PGEY_LIMIT);
        stack.pgeyReset(initial);
        assertThat(stack.pgeyCanUndo()).isFalse();
        assertThat(stack.pgeyCurrent()).isEqualTo(initial);
    }
}
