package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.feedback.Tooltip;

/** Проверяет базовый retained-cache листового виджета. */
public final class WidgetRenderCacheSelfTest {
    public static void main(String[] args) {
        new WidgetRenderCacheSelfTest().run();
        System.out.println("WidgetRenderCacheSelfTest passed");
    }

    private void run() {
        LeafWidget widget = new LeafWidget();
        DefaultRenderContext context = new DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList());

        widget.renderCached(context);
        assertEquals(1, widget.renderCalls, "first render should build cache");
        assertEquals(1L, widget.renderCacheRebuilds(), "first render should rebuild once");
        assertEquals(1, context.drawList().size(), "first render should submit one command");

        context.drawList().clear();
        widget.renderCached(context);
        assertEquals(1, widget.renderCalls, "stable render should use cache");
        assertEquals(1L, widget.renderCacheHits(), "stable render should count cache hit");
        assertEquals(1, context.drawList().size(), "cache replay should submit one command");

        context.drawList().clear();
        context.pushOpacity(0.5f);
        try {
            widget.renderCached(context);
        } finally {
            context.popOpacity();
        }
        DrawCommand faded = context.drawList().commands().get(0);
        assertClose(0.5f, faded.paint().color().a(), "parent opacity should apply during replay");

        context.drawList().clear();
        context.pushTransform(widget.layoutBounds(), widget.transform());
        try {
            widget.renderCached(context);
        } finally {
            context.popTransform();
        }
        assertEquals(1, context.drawList().commands().get(0).transformStackSize(),
                "parent transform should be attached during replay");

        widget.invalidate(InvalidationFlags.VISUAL);
        context.drawList().clear();
        widget.renderCached(context);
        assertEquals(2, widget.renderCalls, "visual invalidation should rebuild cache");
        assertEquals(2L, widget.renderCacheRebuilds(), "visual invalidation should rebuild once");

        widget.renderCachingEnabled(false);
        context.drawList().clear();
        widget.renderCached(context);
        assertEquals(3, widget.renderCalls, "disabled cache should render directly");
        assertEquals(2L, widget.renderCacheRebuilds(), "disabled cache should not rebuild retained fragment");

        testTooltipAnchorHoverInvalidatesCache();
    }

    private void testTooltipAnchorHoverInvalidatesCache() {
        LeafWidget anchor = new LeafWidget();
        Tooltip tooltip = new Tooltip(anchor, "Cached tooltip");
        DefaultRenderContext context = new DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList());

        anchor.handle(new PointerEnteredEvent(anchor, 1.0f, 1.0f, 1.0f, 1.0f, 0));
        tooltip.renderCached(context);
        assertEquals(1L, tooltip.renderCacheRebuilds(),
                "hovered Tooltip should populate its retained cache");

        anchor.handle(new PointerExitedEvent(anchor, 1.0f, 1.0f, 1.0f, 1.0f, 0));
        context.drawList().clear();
        tooltip.renderCached(context);
        assertEquals(2L, tooltip.renderCacheRebuilds(),
                "anchor hover exit should invalidate Tooltip retained cache");
        assertEquals(0, context.drawList().size(),
                "Tooltip cache should not replay stale commands after anchor hover exit");
    }

    private static final class LeafWidget extends WidgetBase {
        private int renderCalls;

        @Override
        public void render(RenderContext context) {
            renderCalls++;
            context.rect(0.0f, 0.0f, 10.0f, 10.0f,
                    Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f)));
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertClose(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.0005f) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
