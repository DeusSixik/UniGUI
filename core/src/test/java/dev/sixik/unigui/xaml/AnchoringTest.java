package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.edit.Anchoring;
import dev.sixik.unigui.xaml.edit.EditorSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AnchoringTest {
    private static final String STACK =
            "<StackPanel id=\"root\"><Button id=\"b\" width=\"80\" height=\"24\" margin=\"4\"/></StackPanel>";
    private static final String ABSOLUTE =
            "<VBox id=\"root\"><Label id=\"l\" position=\"absolute\" left=\"10\" top=\"5\" width=\"60\" height=\"20\"/></VBox>";

    private static UiNode child(EditorSession session) {
        return session.document().root().children().get(0);
    }

    @Test
    void theModeFollowsWhereTheWidgetLives() {
        EditorSession stack = EditorSession.create(STACK);
        assertEquals(Anchoring.Mode.STACK, Anchoring.mode(stack, child(stack)));
        assertEquals(Anchoring.Mode.NONE, Anchoring.mode(stack, stack.document().root()));
        EditorSession absolute = EditorSession.create(ABSOLUTE);
        assertEquals(Anchoring.Mode.ABSOLUTE, Anchoring.mode(absolute, child(absolute)));
        EditorSession flow = EditorSession.create("<VBox><Button id=\"b\"/></VBox>");
        assertEquals(Anchoring.Mode.NONE, Anchoring.mode(flow, child(flow)));
        assertFalse(Anchoring.anchor(flow, child(flow), "end", "end"));
        assertFalse(flow.diagnostics(EditorSession.Channel.EDIT).isEmpty(), "the reason is reported");
    }

    @Test
    void stackAnchorsUseAlignmentAndMargin() {
        EditorSession session = EditorSession.create(STACK);
        assertTrue(Anchoring.anchor(session, child(session), "end", "center"));
        assertEquals("end", Anchoring.alignment(session, child(session), true));
        assertEquals("center", Anchoring.alignment(session, child(session), false));
        assertEquals(1, session.undoCount() - 0, "setting an anchor is one step");

        assertTrue(Anchoring.move(session, child(session), 10.0f, 6.0f));
        assertEquals("10 -6 -2 4", child(session).attributeText("margin", ""),
                "end moves the right margin, center shifts top and bottom against each other");
    }

    @Test
    void absoluteAnchorsSwitchBetweenEdgesAndMoveTheirInsets() {
        EditorSession session = EditorSession.create(ABSOLUTE);
        assertEquals("start", Anchoring.alignment(session, child(session), true));

        assertTrue(Anchoring.anchor(session, child(session), "end", "end"));
        assertTrue(child(session).attributeText("left").isEmpty() && child(session).attributeText("top").isEmpty());
        assertEquals("0", child(session).attributeText("right", ""));
        assertEquals("end", Anchoring.alignment(session, child(session), true));

        assertTrue(Anchoring.move(session, child(session), 5.0f, 3.0f));
        assertEquals("-5", child(session).attributeText("right", ""), "moving right shrinks the distance to the right edge");
        assertEquals("-3", child(session).attributeText("bottom", ""));

        assertTrue(Anchoring.anchor(session, child(session), "stretch", "start"));
        assertEquals("stretch", Anchoring.alignment(session, child(session), true));
        assertTrue(child(session).attributeText("left").isPresent() && child(session).attributeText("right").isPresent());
    }

    @Test
    void aCenteredAnchorIsRefusedForAbsolutePositions() {
        EditorSession session = EditorSession.create(ABSOLUTE);
        int steps = session.undoCount();
        assertFalse(Anchoring.anchor(session, child(session), "center", "start"));
        assertEquals(steps, session.undoCount());
        assertEquals("10", child(session).attributeText("left", ""), "the document is untouched");
        assertFalse(Anchoring.anchor(session, child(session), "middle", "start"), "unknown alignments are refused");
    }
}
