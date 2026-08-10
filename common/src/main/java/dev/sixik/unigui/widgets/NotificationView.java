package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;

public class NotificationView extends Box implements OverlayHostAware {
    private final TextBlock message = new TextBlock();
    private boolean open;
    private float lifeSeconds = 3.5f;
    private float elapsedSeconds;
    private float width = 220.0f;
    private float height = 48.0f;
    private float margin = 10.0f;

    public NotificationView() {
        backgroundVisible(true);
        borderVisible(true);
        radius(5.0f);
        background().set(0.035f, 0.043f, 0.060f, 0.96f);
        borderColor().set(0.30f, 0.78f, 1.0f, 0.75f);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.HIDDEN));
        visible(false);
        message.layout(style -> style.margin(8.0f, 6.0f).align(Alignment.STRETCH, Alignment.STRETCH));
        addChild(message);
    }

    public NotificationView(String text) {
        this();
        text(text);
    }

    public String text() {
        return message.text();
    }

    public NotificationView text(String text) {
        message.text(text);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NotificationView duration(float seconds) {
        lifeSeconds = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 3.5f;
        return this;
    }

    public NotificationView toast(String text) {
        text(text);
        return show();
    }

    public NotificationView show() {
        open = true;
        elapsedSeconds = 0.0f;
        visible(true);
        animateOpacity(1.0f, 0.12f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NotificationView hide() {
        open = false;
        visible(false);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean opened() {
        return open;
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!open || lifeSeconds <= 0.0f) return;
        float delta = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        elapsedSeconds += delta;
        if (elapsedSeconds >= lifeSeconds) {
            hide();
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        message.measure(context);
        setDesiredSize(resolveDesiredSize(context, width, Math.max(height, message.desiredSize().height() + 12.0f)));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView hostBounds) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(hostBounds.x(), hostBounds.y(), 0.0f, 0.0f);
            return;
        }
        float w = desiredSize().width();
        float h = desiredSize().height();
        float x = hostBounds.x() + Math.max(0.0f, hostBounds.width() - w - margin);
        float y = hostBounds.y() + margin;
        mutableLayoutBounds().set(x, y, w, h);
        StackPanel.arrangeChild(message, x, y, w, h);
    }
}
