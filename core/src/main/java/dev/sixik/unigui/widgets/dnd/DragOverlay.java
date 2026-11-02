package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.input.HitTestChildrenOnly;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.feedback.OverlayHostAware;

/**
 * Overlay-слой одной drag-сессии: рисует индикатор вставки и ghost поверх всего UI и тикает
 * сессию для автоскролла.
 *
 * <p>Слой занимает весь хост и прозрачен для hit-test ({@link HitTestChildrenOnly}), поэтому
 * не перехватывает указатель. Retained-кэш отключён: содержимое меняется каждый кадр.</p>
 */
@AiDoc(summary = "Internal transparent overlay layer rendering ghost previews and drop indicators above the UI tree during an active drag session."
)
final class DragOverlay extends WidgetBase implements OverlayHostAware, HitTestChildrenOnly {
    private static final float INDICATOR_RADIUS = 1.0f;

    private final DragSession session;

    DragOverlay(DragSession session) {
        this.session = session;
        renderCachingEnabled(false);
    }

    @Override
    public void arrangeInHost(RectView hostBounds) {
        arrange(hostBounds);
    }

    @Override
    public void measure(LayoutContext context) {
        setDesiredSize(0.0f, 0.0f);
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (frame != null) session.tick(frame.deltaSeconds());
    }

    @Override
    public void render(RenderContext context) {
        if (context == null || visibility() != Visibility.VISIBLE || !session.active()) return;
        if (session.indicatorVisible()) {
            float height = Math.max(1.0f, session.indicatorHeight());
            float width = Math.max(1.0f, session.indicatorWidth());
            context.roundedRect(session.indicatorX(), session.indicatorY(), width, height,
                    Math.min(INDICATOR_RADIUS, Math.min(width, height) * 0.5f),
                    Paint.fill(session.indicatorColor()));
        }
        session.ghost().render(context, session);
    }
}
