package com.sportforce3d.design;

import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.Zone;
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
    private DesignDocument initial;
    private CommandStack stack;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        initial = DesignDefaults.document(BodyType.ADULTO_HOMBRE);
        stack = new CommandStack(initial, clock);
    }

    @Test
    void undoAndRedoRestoreExactStates() {
        DesignDocument red = stack.execute("Color", d -> d.withColor(Zone.JERSEY_BODY, "#D7192A"), null);
        DesignDocument withName = stack.execute("Nombre", d -> d.addElement(DesignDefaults.name("PAOLO", "#FFFFFF", "#000000")), null);

        assertThat(stack.canUndo()).isTrue();
        assertThat(stack.undoLabel()).isEqualTo("Nombre");
        assertThat(stack.undo()).isEqualTo(red);
        assertThat(stack.undo()).isEqualTo(initial);
        assertThat(stack.canUndo()).isFalse();
        assertThat(stack.undo()).isEqualTo(initial);

        assertThat(stack.redo()).isEqualTo(red);
        assertThat(stack.redo()).isEqualTo(withName);
        assertThat(stack.canRedo()).isFalse();
    }

    @Test
    void newCommandClearsRedo() {
        stack.execute("Color", d -> d.withColor(Zone.JERSEY_BODY, "#D7192A"), null);
        stack.undo();
        assertThat(stack.canRedo()).isTrue();
        stack.execute("Otro color", d -> d.withColor(Zone.JERSEY_BODY, "#0E7C3A"), null);
        assertThat(stack.canRedo()).isFalse();
    }

    @Test
    void noOpChangesAreNotRecorded() {
        stack.execute("Mismo color", d -> d.withColor(Zone.JERSEY_BODY, "#1F5FFF"), null);
        assertThat(stack.canUndo()).isFalse();
    }

    @Test
    void continuousChangesWithTheSameKeyMergeIntoOneStep() {
        DesignElement e = DesignDefaults.emblem("rayo", null, null);
        stack.execute("Agregar", d -> d.addElement(e), null);
        for (int i = 1; i <= 10; i++) {
            final double u = 0.5 + i * 0.01;
            clock.millis += 50;
            stack.execute("Mover", d -> d.replaceElement(d.element(e.id()).orElseThrow().at(u, 0.3)), "move:" + e.id());
        }
        assertThat(stack.undoSize()).isEqualTo(2);
        DesignDocument afterDrag = stack.current();
        stack.undo();
        assertThat(stack.current().element(e.id()).orElseThrow().u()).isEqualTo(e.u());
        stack.redo();
        assertThat(stack.current()).isEqualTo(afterDrag);
    }

    @Test
    void mergingStopsAfterSealTimeoutOrDifferentKey() {
        DesignElement e = DesignDefaults.emblem("rayo", null, null);
        stack.execute("Agregar", d -> d.addElement(e), null);
        stack.execute("Escala", d -> d.replaceElement(d.element(e.id()).orElseThrow().withScale(1.2)), "scale");
        stack.seal();
        stack.execute("Escala", d -> d.replaceElement(d.element(e.id()).orElseThrow().withScale(1.4)), "scale");
        clock.millis += CommandStack.MERGE_WINDOW_MS + 1;
        stack.execute("Escala", d -> d.replaceElement(d.element(e.id()).orElseThrow().withScale(1.6)), "scale");
        stack.execute("Giro", d -> d.replaceElement(d.element(e.id()).orElseThrow().withRotation(15)), "rotate");
        assertThat(stack.undoSize()).isEqualTo(5);
    }

    @Test
    void historyIsBoundedAndResetClearsIt() {
        for (int i = 0; i < CommandStack.LIMIT + 20; i++) {
            final String hex = String.format("#%06X", i + 1);
            stack.execute("Color", d -> d.withColor(Zone.JERSEY_BODY, hex), null);
        }
        assertThat(stack.undoSize()).isEqualTo(CommandStack.LIMIT);
        stack.reset(initial);
        assertThat(stack.canUndo()).isFalse();
        assertThat(stack.current()).isEqualTo(initial);
    }
}
