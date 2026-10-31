package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.WidgetStyles;

/**
 * Overlay host for transient notification cards.
 *
 * <p>{@link Toast} owns the visual card/message/duration behavior. NotificationView
 * owns queueing, max-visible policy and host-relative stacking.</p>
 */
@XmlWidgetName("NotificationView")
@AiDoc(summary = "Floating notification stack host displaying timed toast messages aligned to configurable screen edges or corners.")
@WikiPage(
        title = "NotificationView",
        slug = "notification-view",
        category = "<key:feedback.category>",
        summary = "<key:notification.view.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:notification.view.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:notification.view.example.basic.title>",
        code = "NotificationView notifications = new NotificationView();\nnotifications.toast(\"Build completed!\");"
)
@WikiLink(
        label = "Toast",
        target = "page:toast",
        description = "<key:toast.summary>"
)
@WikiLink(
        label = "OverlayLayer",
        target = "page:overlay-layer",
        description = "<key:overlay.layer.summary>"
)
public class NotificationView extends Box implements OverlayHostAware {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.NOTIFICATION_VIEW;

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

        @AiDoc(summary = "Constructs an empty NotificationView.")
    @WikiMethod(summary = "<key:notification.view.method.ctorDefault>")
    public NotificationView() {
        applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.VISIBLE));
        visible(true);
    }

    /**
     * Возвращает стандартное прозрачное оформление контейнера уведомлений.
     *
     * @return общий стиль без фоновой поверхности и рамки
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Surface.UNSURFACED;
    }

        @AiDoc(summary = "Constructs a NotificationView with an initial toast message.")
    @WikiMethod(summary = "<key:notification.view.method.ctorText>")
    public NotificationView(
            @WikiParameter(description = "<key:notification.view.param.text>")
            String text
    ) {
        this();
        text(text);
    }

        @AiDoc(summary = "Returns the text of the latest notification.")
    @WikiMethod(summary = "<key:notification.view.method.textGet>")
    @WikiReturn("<key:notification.view.return.text>")
    public String text() {
        return pendingText;
    }

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Pending notification text used by show().")
        @AiDoc(summary = "Queues a new notification with the given text.")
    @WikiMethod(summary = "<key:notification.view.method.textSet>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView text(
            @WikiParameter(description = "<key:notification.view.param.text>")
            String text
    ) {
        pendingText = text == null ? "" : text;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the display duration in seconds for new notifications.")
    @WikiMethod(summary = "<key:notification.view.method.durationGet>")
    @WikiReturn("<key:notification.view.return.duration>")
    public float duration() {
        return defaultDurationSeconds;
    }

    @XmlAttribute(value = "duration", category = "Behavior", defaultValue = "3.5", description = "Default lifetime for newly created toasts.")
        @AiDoc(summary = "Sets the display duration in seconds for new notifications.")
    @WikiMethod(summary = "<key:notification.view.method.durationSet>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView duration(
            @WikiParameter(description = "<key:notification.view.param.duration>")
            float seconds
    ) {
        defaultDurationSeconds = sanitizeDuration(seconds);
        return this;
    }

        @AiDoc(summary = "Returns the vertical spacing between stacked notification cards in UI pixels.")
    @WikiMethod(summary = "<key:notification.view.method.spacingGet>")
    @WikiReturn("<key:notification.view.return.spacing>")
    public float spacing() {
        return spacing;
    }

    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "8", description = "Vertical gap between visible notification cards.")
        @AiDoc(summary = "Sets the vertical spacing between stacked notification cards.")
    @WikiMethod(summary = "<key:notification.view.method.spacingSet>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView spacing(
            @WikiParameter(description = "<key:notification.view.param.spacing>")
            float spacing
    ) {
        this.spacing = Float.isFinite(spacing) ? Math.max(0.0f, spacing) : 8.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the margin distance from screen edges in UI pixels.")
    @WikiMethod(summary = "<key:notification.view.method.marginGet>")
    @WikiReturn("<key:notification.view.return.margin>")
    public float margin() {
        return margin;
    }

    @XmlAttribute(value = "margin", category = "Layout", defaultValue = "10", description = "Distance from host edges in UI pixels.")
        @AiDoc(summary = "Sets the margin distance from screen edges.")
    @WikiMethod(summary = "<key:notification.view.method.marginSet>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView margin(
            @WikiParameter(description = "<key:notification.view.param.margin>")
            float margin
    ) {
        this.margin = Float.isFinite(margin) ? Math.max(0.0f, margin) : 10.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the maximum number of simultaneously visible notification cards.")
    @WikiMethod(summary = "<key:notification.view.method.maxVisibleGet>")
    @WikiReturn("<key:notification.view.return.maxVisible>")
    public int maxVisible() {
        return maxVisible;
    }

    @XmlAttribute(value = "maxVisible", category = "Behavior", defaultValue = "3", description = "Maximum number of concurrently visible notification cards.")
        @AiDoc(summary = "Sets the maximum number of simultaneously visible notification cards.")
    @WikiMethod(summary = "<key:notification.view.method.maxVisibleSet>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView maxVisible(
            @WikiParameter(description = "<key:notification.view.param.maxVisible>")
            int maxVisible
    ) {
        this.maxVisible = Math.max(1, maxVisible);
        updateVisibleCards();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the screen corner/edge placement.")
    @WikiMethod(summary = "<key:notification.view.method.placementGet>")
    @WikiReturn("<key:notification.view.return.placement>")
    public Placement placement() {
        return placement;
    }

    @XmlAttribute(value = "placement", category = "Layout", defaultValue = "top_right", description = "Host-relative notification stack placement.")
        @AiDoc(summary = "Sets the screen corner/edge placement.")
    @WikiMethod(summary = "<key:notification.view.method.placementSet>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView placement(
            @WikiParameter(description = "<key:notification.view.param.placement>")
            Placement placement
    ) {
        this.placement = placement == null ? Placement.TOP_RIGHT : placement;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Pushes a new text toast into the notification stack.")
    @WikiMethod(summary = "<key:notification.view.method.toast>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView toast(
            @WikiParameter(description = "<key:notification.view.param.text>")
            String text
    ) {
        return notify(text);
    }

        @AiDoc(summary = "Synonym pushing a new text toast into the notification stack.")
    @WikiMethod(summary = "<key:notification.view.method.notify>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView notify(
            @WikiParameter(description = "<key:notification.view.param.text>")
            String text
    ) {
        enqueue(new Toast(text).duration(defaultDurationSeconds).margin(margin));
        return this;
    }

        @AiDoc(summary = "Adds a custom Toast instance into the stack.")
    @WikiMethod(summary = "<key:notification.view.method.addToast>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView addToast(
            @WikiParameter(description = "<key:notification.view.param.toast>")
            Toast toast
    ) {
        enqueue(toast);
        return this;
    }

        @AiDoc(summary = "Shows the notification view.")
    @WikiMethod(summary = "<key:notification.view.method.show>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView show() {
        if (!pendingText.isEmpty() || notifications.isEmpty()) {
            notify(pendingText);
        } else {
            updateVisibleCards();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
        return this;
    }

        @AiDoc(summary = "Hides the notification view.")
    @WikiMethod(summary = "<key:notification.view.method.hide>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView hide() {
        for (Toast toast : List.copyOf(notifications)) {
            toast.hide();
        }
        pruneClosedCards();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Clears all active and pending notifications.")
    @WikiMethod(summary = "<key:notification.view.method.clear>")
    @WikiReturn("<key:notification.view.return.this>")
    public NotificationView clear() {
        return hide();
    }

        @AiDoc(summary = "Returns true if any notifications are currently active.")
    @WikiMethod(summary = "<key:notification.view.method.opened>")
    @WikiReturn("<key:notification.view.return.opened>")
    public boolean opened() {
        return activeCount() > 0;
    }

        @AiDoc(summary = "Returns the count of active notifications.")
    @WikiMethod(summary = "<key:notification.view.method.activeCount>")
    @WikiReturn("<key:notification.view.return.activeCount>")
    public int activeCount() {
        int count = 0;
        for (Toast toast : notifications) {
            if (toast.opened()) count++;
        }
        return count;
    }

        @AiDoc(summary = "Returns an unmodifiable list of active Toast instances.")
    @WikiMethod(summary = "<key:notification.view.method.notifications>")
    @WikiReturn("<key:notification.view.return.notifications>")
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
        @AiDoc(summary = "Arranges the notification cards within host boundaries according to placement.")
    @WikiMethod(summary = "<key:notification.view.method.arrangeInHost>")
    public void arrangeInHost(
            @WikiParameter(description = "<key:notification.view.param.hostBounds>")
            RectView hostBounds
    ) {
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
