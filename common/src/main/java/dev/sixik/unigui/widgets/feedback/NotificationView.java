package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import dev.sixik.unigui.widgets.containers.Box;

/**
 * Overlay host for transient notification cards.
 *
 * <p>{@link Toast} owns the visual card/message/duration behavior. NotificationView
 * owns queueing, max-visible policy and host-relative stacking.</p>
 */
public class NotificationView extends Box implements OverlayHostAware {
    public enum Placement {
        TOP_RIGHT,
        BOTTOM_RIGHT
    }

    private final List<Toast> notifications = new ObjectArrayList<>();
    private String pendingText = "";
    private float defaultDurationSeconds = 3.5f;
    private float spacing = 8.0f;
    private float margin = 10.0f;
    private int maxVisible = 3;
    private Placement placement = Placement.TOP_RIGHT;

    public NotificationView() {
        backgroundVisible(false);
        borderVisible(false);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.VISIBLE));
        visible(true);
    }

    public NotificationView(String text) {
        this();
        text(text);
    }

    public String text() {
        return pendingText;
    }

    public NotificationView text(String text) {
        pendingText = text == null ? "" : text;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float duration() {
        return defaultDurationSeconds;
    }

    public NotificationView duration(float seconds) {
        defaultDurationSeconds = sanitizeDuration(seconds);
        return this;
    }

    public float spacing() {
        return spacing;
    }

    public NotificationView spacing(float spacing) {
        this.spacing = Float.isFinite(spacing) ? Math.max(0.0f, spacing) : 8.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float margin() {
        return margin;
    }

    public NotificationView margin(float margin) {
        this.margin = Float.isFinite(margin) ? Math.max(0.0f, margin) : 10.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int maxVisible() {
        return maxVisible;
    }

    public NotificationView maxVisible(int maxVisible) {
        this.maxVisible = Math.max(1, maxVisible);
        updateVisibleCards();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Placement placement() {
        return placement;
    }

    public NotificationView placement(Placement placement) {
        this.placement = placement == null ? Placement.TOP_RIGHT : placement;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NotificationView toast(String text) {
        return notify(text);
    }

    public NotificationView notify(String text) {
        enqueue(new Toast(text).duration(defaultDurationSeconds).margin(margin));
        return this;
    }

    public NotificationView addToast(Toast toast) {
        enqueue(toast);
        return this;
    }

    public NotificationView show() {
        if (!pendingText.isEmpty() || notifications.isEmpty()) {
            notify(pendingText);
        } else {
            updateVisibleCards();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
        return this;
    }

    public NotificationView hide() {
        for (Toast toast : List.copyOf(notifications)) {
            toast.hide();
        }
        pruneClosedCards();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public NotificationView clear() {
        return hide();
    }

    public boolean opened() {
        return activeCount() > 0;
    }

    public int activeCount() {
        int count = 0;
        for (Toast toast : notifications) {
            if (toast.opened()) count++;
        }
        return count;
    }

    public List<Toast> notifications() {
        return List.copyOf(notifications);
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        updateVisibleCards();
        super.tick(frame);
        pruneClosedCards();
        updateVisibleCards();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !opened()) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        pruneClosedCards();
        updateVisibleCards();
        float width = 0.0f;
        float height = 0.0f;
        int visibleCount = 0;
        for (Toast toast : visibleCards()) {
            toast.measure(context);
            width = Math.max(width, toast.desiredSize().width());
            height += toast.desiredSize().height();
            visibleCount++;
        }
        if (visibleCount > 1) {
            height += spacing * (visibleCount - 1);
        }
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    @Override
    public void arrangeInHost(RectView hostBounds) {
        RectView host = hostBounds == null ? new MutableRect() : hostBounds;
        if (visibility() == Visibility.COLLAPSED || !opened()) {
            mutableLayoutBounds().set(host.x(), host.y(), 0.0f, 0.0f);
            return;
        }
        applyQueuedMutations();
        pruneClosedCards();
        updateVisibleCards();

        List<Toast> visible = visibleCards();
        if (visible.isEmpty()) {
            mutableLayoutBounds().set(host.x(), host.y(), 0.0f, 0.0f);
            return;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        if (placement == Placement.BOTTOM_RIGHT) {
            float y = host.y() + host.height() - margin;
            for (Toast toast : visible) {
                float w = toast.desiredSize().width();
                float h = toast.desiredSize().height();
                float x = host.x() + Math.max(0.0f, host.width() - w - margin);
                y -= h;
                toast.arrangeCard(x, y);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
                y -= spacing;
            }
        } else {
            float y = host.y() + margin;
            for (Toast toast : visible) {
                float w = toast.desiredSize().width();
                float h = toast.desiredSize().height();
                float x = host.x() + Math.max(0.0f, host.width() - w - margin);
                toast.arrangeCard(x, y);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
                y += h + spacing;
            }
        }

        mutableLayoutBounds().set(minX, minY, Math.max(0.0f, maxX - minX), Math.max(0.0f, maxY - minY));
    }

    private void enqueue(Toast toast) {
        if (toast == null) return;
        toast.margin(margin);
        toast.show();
        notifications.add(toast);
        addChild(toast);
        updateVisibleCards();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void pruneClosedCards() {
        boolean removed = false;
        Iterator<Toast> iterator = notifications.iterator();
        while (iterator.hasNext()) {
            Toast toast = iterator.next();
            if (!toast.opened()) {
                iterator.remove();
                removeChild(toast);
                removed = true;
            }
        }
        if (removed) {
            applyQueuedMutations();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }

    private void updateVisibleCards() {
        int index = 0;
        for (Toast toast : notifications) {
            if (!toast.opened()) {
                toast.visible(false);
                continue;
            }
            toast.visible(index < maxVisible);
            index++;
        }
    }

    private List<Toast> visibleCards() {
        List<Toast> visible = new ObjectArrayList<>();
        for (Toast toast : notifications) {
            if (toast.opened() && toast.visibility() == Visibility.VISIBLE) {
                visible.add(toast);
            }
        }
        return visible;
    }

    private static float sanitizeDuration(float seconds) {
        return Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 3.5f;
    }
}
