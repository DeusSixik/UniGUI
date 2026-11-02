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
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.WidgetStyles;

/**
 * Lightweight transient message overlay.
 *
 * <p>Toast is intentionally separate from {@link NotificationView}: Toast is
 * short-lived and bottom-right positioned, while NotificationView remains a
 * richer/persistent notification surface.</p>
 */
@AiDoc(summary = "Lightweight transient message overlay that auto-dismisses after a configurable duration.")
@WikiPage(
        title = "Toast",
        slug = "toast",
        category = "<key:feedback.category>",
        summary = "<key:toast.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:toast.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:toast.example.basic.title>",
        code = "Toast toast = new Toast(\"Settings saved successfully!\");\ntoast.duration(3.0f).show();"
)
@WikiLink(
        label = "NotificationView",
        target = "page:notification-view",
        description = "<key:notification.view.summary>"
)
@WikiLink(
        label = "OverlayLayer",
        target = "page:overlay-layer",
        description = "<key:overlay.layer.summary>"
)
@XmlWidgetName("Toast")
public final class Toast extends Box implements OverlayHostAware {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TOAST;

    private final TextBlock message = new TextBlock();
    private boolean open;
    private float lifeSeconds = 2.5f;
    private float elapsedSeconds;
    private float width = 180.0f;
    private float height = 34.0f;
    private float margin = 10.0f;

        @AiDoc(summary = "Constructs an empty Toast widget initialized with default styling.")
    @WikiMethod(summary = "<key:toast.method.ctorDefault>")
    public Toast() {
        applyComponentStyleDefaults(WidgetStyles.Toast.DEFAULT);
        layout(style -> style.position(PositionType.ABSOLUTE).overflow(Overflow.HIDDEN));
        visible(false);
        message.layout(style -> style.margin(8.0f, 6.0f).align(Alignment.STRETCH, Alignment.STRETCH));
        addChild(message);
    }

    /**
     * Возвращает стандартный стиль поверхности уведомления, уступающий стилям темы и экземпляра.
     *
     * @return стандартные значения оформления уведомления
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Toast.DEFAULT;
    }

        @AiDoc(summary = "Constructs a Toast with the specified message text.")
    @WikiMethod(summary = "<key:toast.method.ctorText>")
    public Toast(
            @WikiParameter(description = "<key:toast.param.text>")
            String text
    ) {
        this();
        text(text);
    }

        @AiDoc(summary = "Returns the toast message text.")
    @WikiMethod(summary = "<key:toast.method.textGet>")
    @WikiReturn("<key:toast.return.text>")
    public String text() {
        return message.text();
    }

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Toast message text.")
        @AiDoc(summary = "Sets the toast message text.")
    @WikiMethod(summary = "<key:toast.method.textSet>")
    @WikiReturn("<key:toast.return.this>")
    public Toast text(
            @WikiParameter(description = "<key:toast.param.text>")
            String text
    ) {
        message.text(text);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "duration", category = "Behavior", defaultValue = "2.5", description = "Toast lifetime in seconds; 0 keeps it open.")
        @AiDoc(summary = "Sets the toast lifetime in seconds before auto-dismissal (0 keeps it open indefinitely).")
    @WikiMethod(summary = "<key:toast.method.durationSet>")
    @WikiReturn("<key:toast.return.this>")
    public Toast duration(
            @WikiParameter(description = "<key:toast.param.duration>")
            float seconds
    ) {
        lifeSeconds = Float.isFinite(seconds) ? Math.max(0.0f, seconds) : 2.5f;
        return this;
    }

        @AiDoc(summary = "Returns the toast lifetime in seconds.")
    @WikiMethod(summary = "<key:toast.method.durationGet>")
    @WikiReturn("<key:toast.return.duration>")
    public float duration() {
        return lifeSeconds;
    }

        @AiDoc(summary = "Sets the preferred dimensions of the toast card in UI pixels.")
    @WikiMethod(summary = "<key:toast.method.preferredSize>")
    @WikiReturn("<key:toast.return.this>")
    public Toast preferredSize(
            @WikiParameter(description = "<key:toast.param.width>")
            float width,
            @WikiParameter(description = "<key:toast.param.height>")
            float height
    ) {
        this.width = Float.isFinite(width) ? Math.max(1.0f, width) : 180.0f;
        this.height = Float.isFinite(height) ? Math.max(1.0f, height) : 34.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the margin distance from host edges in UI pixels.")
    @WikiMethod(summary = "<key:toast.method.marginGet>")
    @WikiReturn("<key:toast.return.margin>")
    public float margin() {
        return margin;
    }

    @XmlAttribute(value = "margin", category = "Layout", defaultValue = "10", description = "Distance from host edges in UI pixels.")
        @AiDoc(summary = "Sets the margin distance from host edges in UI pixels.")
    @WikiMethod(summary = "<key:toast.method.marginSet>")
    @WikiReturn("<key:toast.return.this>")
    public Toast margin(
            @WikiParameter(description = "<key:toast.param.margin>")
            float margin
    ) {
        this.margin = Float.isFinite(margin) ? Math.max(0.0f, margin) : 10.0f;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Updates message text and immediately displays the toast with animation.")
    @WikiMethod(summary = "<key:toast.method.toast>")
    @WikiReturn("<key:toast.return.this>")
    public Toast toast(
            @WikiParameter(description = "<key:toast.param.text>")
            String text
    ) {
        text(text);
        return show();
    }

        @AiDoc(summary = "Shows the toast and resets its elapsed time counter.")
    @WikiMethod(summary = "<key:toast.method.show>")
    @WikiReturn("<key:toast.return.this>")
    public Toast show() {
        open = true;
        elapsedSeconds = 0.0f;
        visible(true);
        animateOpacity(1.0f, 0.12f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Hides the toast immediately.")
    @WikiMethod(summary = "<key:toast.method.hide>")
    @WikiReturn("<key:toast.return.this>")
    public Toast hide() {
        open = false;
        visible(false);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if the toast is currently visible and active.")
    @WikiMethod(summary = "<key:toast.method.opened>")
    @WikiReturn("<key:toast.return.opened>")
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
        @AiDoc(summary = "Positions the toast anchored to the bottom-right corner of the host container.")
    @WikiMethod(summary = "<key:toast.method.arrangeInHost>")
    public void arrangeInHost(
            @WikiParameter(description = "<key:toast.param.hostBounds>")
            RectView hostBounds
    ) {
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
