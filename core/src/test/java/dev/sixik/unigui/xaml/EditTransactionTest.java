package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.Edits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EditTransactionTest {
    private final EditorSession session = EditorSession.create(
            "<VBox><Label id=\"a\" text=\"1\"/><Label id=\"b\" text=\"2\"/></VBox>");

    private UiNode a() {
        return session.document().root().children().get(0);
    }

    private UiNode b() {
        return session.document().root().children().get(1);
    }

    @Test
    void severalEditsAreOneStepOfUndo() {
        int before = session.undoCount();
        assertTrue(session.transaction("Retitle both", () ->
                session.apply(Edits.setProperty(a(), "text", "x")) && session.apply(Edits.setProperty(b(), "text", "y"))));
        assertEquals(before + 1, session.undoCount());
        assertEquals("Retitle both", session.undoLabel().orElseThrow());

        assertTrue(session.undo());
        assertEquals("1", a().attributeText("text", ""));
        assertEquals("2", b().attributeText("text", ""));
        assertTrue(session.redo());
        assertEquals("x", a().attributeText("text", ""));
        assertEquals("y", b().attributeText("text", ""));
    }

    @Test
    void aFailedStepRollsEverythingBack() {
        int steps = session.undoCount();
        // Label не принимает детей: вторая правка отклоняется схемой.
        assertFalse(session.transaction("Half done", () -> session.apply(Edits.setProperty(a(), "text", "x"))
                && session.apply(Edits.addChild(b(), 0, new UiNode(UiName.of("Label"))))));
        assertEquals("1", a().attributeText("text", ""), "the first edit was rolled back");
        assertEquals(0, b().children().size());
        assertEquals(steps, session.undoCount(), "a rolled back transaction leaves no history");
        assertEquals(0, session.redoCount());
    }

    @Test
    void anExceptionAlsoRollsBackAndPropagates() {
        int steps = session.undoCount();
        assertThrows(IllegalStateException.class, () -> session.transaction("Boom", () -> {
            session.apply(Edits.setProperty(a(), "text", "x"));
            throw new IllegalStateException("boom");
        }));
        assertEquals("1", a().attributeText("text", ""));
        assertEquals(steps, session.undoCount());
    }

    @Test
    void editsInsideDoNotMergeIntoEarlierHistory() {
        assertTrue(session.apply(Edits.setProperty(a(), "text", "first")));
        int steps = session.undoCount();
        assertTrue(session.transaction("Same property again", () -> session.apply(Edits.setProperty(a(), "text", "second"))));
        assertEquals(steps + 1, session.undoCount(), "the grouped edit is its own step even for the same property");
        session.undo();
        assertEquals("first", a().attributeText("text", ""));
    }

    @Test
    void anEmptyTransactionLeavesNoStep() {
        int steps = session.undoCount();
        assertTrue(session.transaction("Nothing", () -> true));
        assertEquals(steps, session.undoCount());
    }
}
