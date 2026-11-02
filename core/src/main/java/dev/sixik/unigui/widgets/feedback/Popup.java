package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.RenderedBoundsMapper;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.OverlayLayoutResolver;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.WidgetStyles;

@XmlWidgetName("Popup")
@AiDoc(summary = "Anchored floating popup panel positioned relative to a target anchor widget.")
@WikiPage(
        title = "Popup",
        slug = "popup",
        category = "<key:feedback.category>",
        summary = "<key:popup.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:popup.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:popup.example.basic.title>",
        code = "Popup popup = new Popup(button, menuContent);\npopup.open();"
)
@WikiLink(
        label = "Tooltip",
        target = "page:tooltip",
        description = "<key:tooltip.summary>"
)
@WikiLink(
        label = "OverlayLayer",
        target = "page:overlay-layer",
        description = "<key:overlay.layer.summary>"
)
public final class Popup extends Box implements OverlayHostAware {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.POPUP;

    private Widget anchor;
    private Widget content;
    private boolean open;
    private boolean closeOnOutsideClick = true;
    private Placement placement = Placement.BELOW;
    private float offsetX;
    private float offsetY = 4.0f;
    private EdgeInsets padding = EdgeInsets.all(6.0f);
    private Runnable onOpenChanged;

        @AiDoc(summary = "Constructs an empty Popup.")
    @WikiMethod(summary = "<key:popup.method.ctorDefault>")
    public Popup() {
        applyComponentStyleDefaults(WidgetStyles.Popup.DEFAULT);
        layout(style -> style
                .position(PositionType.ABSOLUTE)
                .maxWidthPercent(100.0f)
                .maxHeightPercent(100.0f)
                .overflow(Overflow.HIDDEN));
        visible(false);
    }

    /**
     * Возвращает стандартный стиль поверхности всплывающего окна, уступающий стилям темы и экземпляра.
     *
     * @return стандартные значения оформления всплывающего окна
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Popup.DEFAULT;
    }

        @AiDoc(summary = "Constructs a Popup bound to an anchor widget and displaying content.")
    @WikiMethod(summary = "<key:popup.method.ctorAnchorContent>")
    public Popup(
            @WikiParameter(description = "<key:popup.param.anchor>")
            Widget anchor,
            @WikiParameter(description = "<key:popup.param.content>")
            Widget content
    ) {
        this();
        anchor(anchor);
        content(content);
    }

        @AiDoc(summary = "Returns the anchor widget this popup is positioned relative to.")
    @WikiMethod(summary = "<key:popup.method.anchorGet>")
    @WikiReturn("<key:popup.return.anchor>")
    public Widget anchor() {
        return anchor;
    }

        @AiDoc(summary = "Sets the anchor widget this popup is positioned relative to.")
    @WikiMethod(summary = "<key:popup.method.anchorSet>")
    @WikiReturn("<key:popup.return.this>")
    public Popup anchor(
            @WikiParameter(description = "<key:popup.param.anchor>")
            Widget anchor
    ) {
        if (this.anchor == anchor) return this;
        this.anchor = anchor;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the content widget hosted inside this popup.")
    @WikiMethod(summary = "<key:popup.method.contentGet>")
    @WikiReturn("<key:popup.return.content>")
    public Widget content() {
        return content;
    }

        @AiDoc(summary = "Sets the content widget displayed inside this popup.")
    @WikiMethod(summary = "<key:popup.method.contentSet>")
    @WikiReturn("<key:popup.return.this>")
    public Popup content(
            @WikiParameter(description = "<key:popup.param.content>")
            Widget content
    ) {
        if (this.content == content) return this;
        if (this.content != null) {
            removeChild(this.content);
        }
        this.content = content;
        if (content != null) {
            addChild(content);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if this popup is currently open.")
    @WikiMethod(summary = "<key:popup.method.opened>")
    @WikiReturn("<key:popup.return.opened>")
    public boolean opened() {
        return open;
    }

        @AiDoc(summary = "Opens the popup.")
    @WikiMethod(summary = "<key:popup.method.open>")
    @WikiReturn("<key:popup.return.this>")
    public Popup open() {
        return open(true);
    }

        @AiDoc(summary = "Closes the popup.")
    @WikiMethod(summary = "<key:popup.method.close>")
    @WikiReturn("<key:popup.return.this>")
    public Popup close() {
        return open(false);
    }

        @AiDoc(summary = "Toggles between open and closed states.")
    @WikiMethod(summary = "<key:popup.method.toggle>")
    @WikiReturn("<key:popup.return.this>")
    public Popup toggle() {
        return open(!open);
    }

    @XmlAttribute(value = "open", category = "Behavior", defaultValue = "false", description = "Whether the popup is visible.")
        @AiDoc(summary = "Sets the open state of the popup.")
    @WikiMethod(summary = "<key:popup.method.openState>")
    @WikiReturn("<key:popup.return.this>")
    public Popup open(
            @WikiParameter(description = "<key:popup.param.open>")
            boolean open
    ) {
        if (this.open == open) return this;
        this.open = open;
        visible(open);
        if (open && parent() instanceof OverlayLayer layer) {
            layer.bringOverlayToFront(this);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (onOpenChanged != null) {
            onOpenChanged.run();
        }
        return this;
    }

        @AiDoc(summary = "Registers a listener invoked when the open state changes.")
    @WikiMethod(summary = "<key:popup.method.onOpenChanged>")
    @WikiReturn("<key:popup.return.this>")
    public Popup onOpenChanged(
            @WikiParameter(description = "<key:popup.param.listener>")
            Runnable onOpenChanged
    ) {
        this.onOpenChanged = onOpenChanged;
        return this;
    }

        @AiDoc(summary = "Returns true if outside pointer clicks automatically dismiss the popup.")
    @WikiMethod(summary = "<key:popup.method.closeOnOutsideClickGet>")
    @WikiReturn("<key:popup.return.closeOnOutsideClick>")
    public boolean closeOnOutsideClick() {
        return closeOnOutsideClick;
    }

    @XmlAttribute(value = "closeOnOutsideClick", category = "Behavior", defaultValue = "true", description = "Whether outside pointer presses close the popup.")
        @AiDoc(summary = "Sets whether outside clicks automatically close the popup.")
    @WikiMethod(summary = "<key:popup.method.closeOnOutsideClickSet>")
    @WikiReturn("<key:popup.return.this>")
    public Popup closeOnOutsideClick(
            @WikiParameter(description = "<key:popup.param.closeOnOutsideClick>")
            boolean closeOnOutsideClick
    ) {
        this.closeOnOutsideClick = closeOnOutsideClick;
        return this;
    }

        @AiDoc(summary = "Returns the preferred anchor placement edge.")
    @WikiMethod(summary = "<key:popup.method.placementGet>")
    @WikiReturn("<key:popup.return.placement>")
    public Placement placement() {
        return placement;
    }

    @XmlAttribute(value = "placement", category = "Layout", defaultValue = "below", description = "Where the popup is placed relative to its anchor.")
        @AiDoc(summary = "Sets the preferred anchor placement edge.")
    @WikiMethod(summary = "<key:popup.method.placementSet>")
    @WikiReturn("<key:popup.return.this>")
    public Popup placement(
            @WikiParameter(description = "<key:popup.param.placement>")
            Placement placement
    ) {
        Placement normalized = placement == null ? Placement.BELOW : placement;
        if (this.placement == normalized) return this;
        this.placement = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Sets additional displacement offsets from the computed placement in UI pixels.")
    @WikiMethod(summary = "<key:popup.method.offset>")
    @WikiReturn("<key:popup.return.this>")
    public Popup offset(
            @WikiParameter(description = "<key:popup.param.x>")
            float x,
            @WikiParameter(description = "<key:popup.param.y>")
            float y
    ) {
        float normalizedX = Float.isFinite(x) ? x : 0.0f;
        float normalizedY = Float.isFinite(y) ? y : 4.0f;
        if (offsetX == normalizedX && offsetY == normalizedY) return this;
        offsetX = normalizedX;
        offsetY = normalizedY;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "padding", category = "Layout", defaultValue = "6", description = "Inner padding around popup content.")
        @AiDoc(summary = "Sets the interior padding around popup content.")
    @WikiMethod(summary = "<key:popup.method.padding>")
    @WikiReturn("<key:popup.return.this>")
    public Popup padding(
            @WikiParameter(description = "<key:popup.param.padding>")
            EdgeInsets padding
    ) {
        EdgeInsets normalized = padding == null ? EdgeInsets.all(0.0f) : padding;
        if (this.padding.equals(normalized)) return this;
        this.padding = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED || !open) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }

        applyQueuedMutations();
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            content.measure(context);
            setDesiredSize(resolveDesiredSize(context,
                    StackPanel.preferredWidth(content, 0.0f) + padding.horizontal(),
                    StackPanel.preferredHeight(content, 0.0f) + padding.vertical()));
        } else {
            setDesiredSize(resolveDesiredSize(context, padding.horizontal(), padding.vertical()));
        }
    }

    @Override
    public void arrange(RectView bounds) {
        arrangeInHost(bounds);
    }

    /**
     * Пересчитывает позицию перед отрисовкой.
     *
     * <p>Скролл сдвигает якорь без полной раскладки, поэтому попап иначе
     * остаётся на старом месте. Повторный расчёт держит меню под виджетом
     * и даёт ему уйти за край экрана вместе с якорем.</p>
     *
     * @param context текущий render context
     */
    @Override
    public void render(RenderContext context) {
        if (open && anchor != null && parent() != null) {
            arrangeInHost(parent().layoutBounds());
        }
        super.render(context);
    }

    @Override
        @AiDoc(summary = "Arranges the popup relative to its anchor within host bounds.")
    @WikiMethod(summary = "<key:popup.method.arrangeInHost>")
    public void arrangeInHost(
            @WikiParameter(description = "<key:popup.param.bounds>")
            RectView bounds
    ) {
        if (!open || visibility() == Visibility.COLLAPSED || anchor == null) {
            mutableLayoutBounds().set(bounds.x(), bounds.y(), 0.0f, 0.0f);
            return;
        }

        applyQueuedMutations();
        float width = desiredSize().width();
        float height = desiredSize().height();
        MutableRect placed = resolveV3Placement(bounds, width, height);
        float x = placed.x();
        float y = placed.y();
        width = placed.width();
        height = placed.height();
        mutableLayoutBounds().set(placed);

        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content,
                    x + padding.left(),
                    y + padding.top(),
                    Math.max(0.0f, width - padding.horizontal()),
                    Math.max(0.0f, height - padding.vertical()));
        }
    }

    private MutableRect resolveV3Placement(RectView hostBounds, float width, float height) {
        RectView anchorBounds = renderedAnchorBounds();
        if (placement == Placement.ABOVE) {
            return placeAbove(hostBounds, anchorBounds, width, height);
        }

        OverlayLayoutResolver resolver = new OverlayLayoutResolver();
        OverlayLayoutResolver.Host host = new OverlayLayoutResolver.Host(
                runtimeLayoutId("host", parent()),
                hostBounds);
        OverlayLayoutResolver.Request request = OverlayLayoutResolver.Request.below(
                        runtimeLayoutId("popup", this),
                        runtimeLayoutId("anchor", anchor),
                        anchorBounds,
                        width,
                        height)
                .offset(offsetX, offsetY);
        if (anchorOutsideHost(hostBounds, anchorBounds)) {
            request = request.clippingPolicy(OverlayLayoutResolver.ClippingPolicy.ALLOW_OUTSIDE_SCREEN);
        }
        OverlayLayoutResolver.ResolvedOverlay resolved = resolver.resolve(host, request, 0);
        return new MutableRect(resolved.x(), resolved.y(), resolved.width(), resolved.height());
    }

    /**
     * Возвращает, ушёл ли якорь за границы хоста.
     *
     * <p>Так бывает при скролле: виджет уехал за край, а меню должно уехать
     * следом. Пока якорь внутри, попап остаётся в хосте и переворачивается
     * у края.</p>
     *
     * @param hostBounds границы хоста
     * @param anchorBounds границы якоря
     * @return {@code true}, если якорь вышел за границу хоста
     */
    private static boolean anchorOutsideHost(RectView hostBounds, RectView anchorBounds) {
        if (hostBounds == null || anchorBounds == null) return false;
        float right = hostBounds.x() + hostBounds.width();
        float bottom = hostBounds.y() + hostBounds.height();
        return anchorBounds.x() + anchorBounds.width() <= hostBounds.x()
                || anchorBounds.y() + anchorBounds.height() <= hostBounds.y()
                || anchorBounds.x() >= right
                || anchorBounds.y() >= bottom;
    }

    private MutableRect placeAbove(RectView hostBounds, RectView anchorBounds, float width, float height) {
        float hostX = hostBounds == null ? 0.0f : hostBounds.x();
        float hostY = hostBounds == null ? 0.0f : hostBounds.y();
        float hostWidth = hostBounds == null ? width : Math.max(0.0f, hostBounds.width());
        float hostHeight = hostBounds == null ? height : Math.max(0.0f, hostBounds.height());
        float clampedWidth = Math.max(0.0f, width);
        float clampedHeight = Math.max(0.0f, height);
        float x = anchorBounds.x() + offsetX;
        float y = anchorBounds.y() - clampedHeight - offsetY;
        x = clamp(x, hostX, hostX + Math.max(0.0f, hostWidth - clampedWidth));
        y = clamp(y, hostY, hostY + Math.max(0.0f, hostHeight - clampedHeight));
        return new MutableRect(x, y, clampedWidth, clampedHeight);
    }

    private RectView renderedAnchorBounds() {
        MutableRect bounds = new MutableRect(
                anchor.layoutBounds().x(),
                anchor.layoutBounds().y(),
                anchor.layoutBounds().width(),
                anchor.layoutBounds().height());
        Widget child = anchor;
        Widget current = anchor.parent();
        while (current != null) {
            if (current instanceof RenderedBoundsMapper mapper) {
                RectView mapped = mapper.renderedBoundsForChild(child, bounds);
                if (mapped != null) {
                    bounds.set(mapped);
                }
            }
            child = current;
            current = current.parent();
        }
        return bounds;
    }

    private static LayoutNodeId runtimeLayoutId(String prefix, Object instance) {
        String suffix = instance == null ? "none" : Integer.toHexString(System.identityHashCode(instance));
        return LayoutNodeId.of(prefix + "@" + suffix);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Placement {
        BELOW,
        ABOVE
    }
}
