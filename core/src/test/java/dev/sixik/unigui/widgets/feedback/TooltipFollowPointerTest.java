package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.containers.Box;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipFollowPointerTest {
    private static final float EPS = 0.001f;

    private final Box anchor = new Box();
    private final OverlayLayer root = new OverlayLayer(new Box());

    private Tooltip install(Tooltip tooltip) {
        root.addOverlay(tooltip);
        root.applyQueuedMutations();
        root.measure(new LayoutContext(400.0f, 300.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 300.0f));
        return tooltip;
    }

    private void moveTo(float x, float y) {
        anchor.handle(new PointerMovedEvent(anchor, x, y, x, y, 0));
        root.tick(new FrameContext(1L, 1.0f / 60.0f, 0.0f, FramePhase.ANIMATION));
    }

    @Test
    void followPointer_placesTooltipAtCursorPlusOffset() {
        Tooltip tooltip = install(new Tooltip(anchor, "hint").followPointer(true).offset(12.0f, -12.0f));

        moveTo(150.0f, 90.0f);

        assertEquals(162.0f, tooltip.layoutBounds().x(), EPS);
        assertEquals(78.0f, tooltip.layoutBounds().y(), EPS);
    }

    @Test
    void followPointer_tracksTheCursorAcrossMoves() {
        Tooltip tooltip = install(new Tooltip(anchor, "hint").followPointer(true).offset(12.0f, -12.0f));

        moveTo(150.0f, 90.0f);
        moveTo(220.0f, 140.0f);

        assertEquals(232.0f, tooltip.layoutBounds().x(), EPS);
        assertEquals(128.0f, tooltip.layoutBounds().y(), EPS);
    }

    @Test
    void followPointer_ignoresWhereTheAnchorSits() {
        // The anchor stays at its default bounds; only the pointer position must matter.
        Tooltip tooltip = install(new Tooltip(anchor, "hint").followPointer(true).offset(12.0f, -12.0f));
        anchor.arrange(new MutableRect(300.0f, 250.0f, 20.0f, 20.0f));

        moveTo(40.0f, 60.0f);

        assertEquals(52.0f, tooltip.layoutBounds().x(), EPS);
        assertEquals(48.0f, tooltip.layoutBounds().y(), EPS);
    }

    @Test
    void followPointer_beforeAnyPointerEvent_usesTheAnchor() {
        Tooltip tooltip = install(new Tooltip(anchor, "hint").followPointer(true));

        assertEquals(8.0f, tooltip.layoutBounds().x(), EPS, "anchor x + default offset");
        assertEquals(10.0f, tooltip.layoutBounds().y(), EPS, "anchor bottom + default offset");
    }

    @Test
    void defaultTooltip_staysUnderTheAnchorAndIgnoresTheCursor() {
        Tooltip tooltip = install(new Tooltip(anchor, "hint"));
        assertFalse(tooltip.followPointer());

        moveTo(150.0f, 90.0f);

        assertEquals(8.0f, tooltip.layoutBounds().x(), EPS);
        assertEquals(10.0f, tooltip.layoutBounds().y(), EPS);
    }

    @Test
    void followPointer_isAToggle() {
        Tooltip tooltip = new Tooltip(anchor, "hint");
        assertTrue(tooltip.followPointer(true).followPointer());
        assertFalse(tooltip.followPointer(false).followPointer());
    }
}
