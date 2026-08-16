package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.display.TextBlock;

/**
 * Lightweight transient message overlay.
 *
 * <p>Toast is intentionally separate from {@link NotificationView}: Toast is
 * short-lived and bottom-right positioned, while NotificationView remains a
 * richer/persistent notification surface.</p>
 */
@XmlWidgetName("Toast")
public final class Toast extends Box implements OverlayHostAware {
    private final TextBlock message = new TextBlock();
    private boolean open;
    private float lifeSeconds = 2.5f;
    private float elapsedSeconds;
    private float width = 180.0f;
    private float height = 34.0f;
    private float margin = 10.0f;

    public Toast() {
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

    public Toast(String text) {
        this();
        text(text);
    }

    public String text() {
        return message.text();
    }

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Toast message text.")
    public Toast text(String text) {
        message.text(text);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "duration", category = "Behavior", defaultValue = "2.5", description = "Toast lifetime in seconds; 0 keeps it open.")
    public Toast duration(float seconds) {
        lifeSeconds = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 2.5f;
        return this;
    }

    public float duration() {
        return lifeSeconds;
    }

    public Toast preferredSize(float width, float height) {
        this.width = Float.isFinite(width) ? Math.max(1.0f, width) : 180.0f;
        this.height = Float.isFinite(height) ? Math.max(1.0f, height) : 34.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float margin() {
        return margin;
    }

    @XmlAttribute(value = "margin", category = "Layout", defaultValue = "10", description = "Distance from host edges in UI pixels.")
    public Toast margin(float margin) {
        this.margin = Float.isFinite(margin) ? Math.max(0.0f, margin) : 10.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Toast toast(String text) {
        text(text);
        return show();
    }

    public Toast show() {
        open = true;
        elapsedSeconds = 0.0f;
        visible(true);
        animateOpacity(1.0f, 0.12f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Toast hide() {
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
        float y = hostBounds.y() + Math.max(0.0f, hostBounds.height() - h - margin);
        mutableLayoutBounds().set(x, y, w, h);
        StackPanel.arrangeChild(message, x, y, w, h);
    }

    void arrangeCard(float x, float y) {
        if (!open || visibility() == Visibility.COLLAPSED) {
            mutableLayoutBounds().set(x, y, 0.0f, 0.0f);
            return;
        }
        float w = desiredSize().width();
        float h = desiredSize().height();
        mutableLayoutBounds().set(x, y, w, h);
        StackPanel.arrangeChild(message, x, y, w, h);
    }
}
