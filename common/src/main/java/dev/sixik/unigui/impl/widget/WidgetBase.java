package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.event.FastEventEmitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WidgetBase implements Widget {
    private final FastEventEmitter events = new FastEventEmitter();
    private final MutableRect layoutBounds = new MutableRect();
    private LayoutSize desiredSize = LayoutSize.ZERO;
    private final Transform transform = new Transform();
    private UIContext uiContext;
    private Widget parent;
    private int invalidationFlags = InvalidationFlags.ALL;
    private int subtreeInvalidationFlags = InvalidationFlags.ALL;
    private LayoutConstraints layoutConstraints = LayoutConstraints.DEFAULT;
    private Visibility visibility = Visibility.VISIBLE;
    private boolean enabled = true;
    private boolean hovered;
    private boolean focusable;
    private boolean focusScope;
    private int focusOrder;
    private boolean styleScope;
    private final Map<String, Style> localStyles = new HashMap<>();

    protected WidgetBase() {
        layoutBounds.onChanged(() -> invalidate(InvalidationFlags.LAYOUT));
        transform.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    @Override
    public UIContext uiContext() {
        return uiContext;
    }

    public void setUiContextInternal(UIContext uiContext) {
        this.uiContext = uiContext;
    }

    @Override
    public Widget parent() {
        return parent;
    }

    public void setParentInternal(Widget parent) {
        if (this.parent == parent) return;
        Widget oldParent = this.parent;
        this.parent = parent;

        recomputeSubtreeInvalidation(oldParent);
        recomputeSubtreeInvalidation(parent);
    }

    @Override
    public List<Widget> children() {
        return Collections.emptyList();
    }

    @Override
    public RectView layoutBounds() {
        return layoutBounds;
    }

    @Override
    public LayoutSize desiredSize() {
        return desiredSize;
    }

    protected final void setDesiredSize(float width, float height) {
        setDesiredSize(LayoutSize.of(width, height));
    }

    protected final void setDesiredSize(LayoutSize desiredSize) {
        this.desiredSize = desiredSize == null ? LayoutSize.ZERO : desiredSize;
    }

    protected final LayoutSize resolveDesiredSize(LayoutContext context, float contentWidth, float contentHeight) {
        return LayoutSize.of(contentWidth, contentHeight).resolve(layoutConstraints, context);
    }

    protected MutableRect mutableLayoutBounds() {
        return layoutBounds;
    }

    @Override
    public Transform transform() {
        return transform;
    }

    @Override
    public int invalidationFlags() {
        return invalidationFlags;
    }

    @Override
    public int subtreeInvalidationFlags() {
        return subtreeInvalidationFlags;
    }

    @Override
    public LayoutConstraints layoutConstraints() {
        return layoutConstraints;
    }

    public WidgetBase layoutConstraints(LayoutConstraints layoutConstraints) {
        LayoutConstraints next = layoutConstraints == null ? LayoutConstraints.DEFAULT : layoutConstraints;
        if (this.layoutConstraints == next) return this;
        this.layoutConstraints = next;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase preferredSize(float width, float height) {
        return layoutConstraints(layoutConstraints.preferredSize(width, height));
    }

    public WidgetBase minSize(float width, float height) {
        return layoutConstraints(layoutConstraints.minSize(width, height));
    }

    public WidgetBase maxSize(float width, float height) {
        return layoutConstraints(layoutConstraints.maxSize(width, height));
    }

    public WidgetBase margin(float margin) {
        return margin(EdgeInsets.all(margin));
    }

    public WidgetBase margin(float horizontal, float vertical) {
        return margin(EdgeInsets.symmetric(horizontal, vertical));
    }

    public WidgetBase margin(float left, float top, float right, float bottom) {
        return margin(new EdgeInsets(left, top, right, bottom));
    }

    public WidgetBase margin(EdgeInsets margin) {
        return layoutConstraints(layoutConstraints.margin(margin));
    }

    public WidgetBase align(Alignment horizontal, Alignment vertical) {
        return layoutConstraints(layoutConstraints.align(horizontal, vertical));
    }

    public WidgetBase grow(float grow) {
        return layoutConstraints(layoutConstraints.grow(grow));
    }

    @Override
    public Visibility visibility() {
        return visibility;
    }

    public WidgetBase visibility(Visibility visibility) {
        Visibility next = visibility == null ? Visibility.VISIBLE : visibility;
        if (this.visibility == next) return this;
        boolean wasCollapsed = this.visibility == Visibility.COLLAPSED;
        boolean isCollapsed = next == Visibility.COLLAPSED;
        this.visibility = next;
        invalidate((wasCollapsed || isCollapsed)
                ? InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL
                : InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public boolean visible() {
        return visibility == Visibility.VISIBLE;
    }

    public WidgetBase visible(boolean visible) {
        return visibility(visible ? Visibility.VISIBLE : Visibility.HIDDEN);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    public WidgetBase enabled(boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public boolean hovered() {
        return hovered;
    }

    @Override
    public boolean focusable() {
        return focusable;
    }

    public WidgetBase focusable(boolean focusable) {
        if (this.focusable == focusable) return this;
        this.focusable = focusable;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public boolean focusScope() {
        return focusScope;
    }

    public WidgetBase focusScope(boolean focusScope) {
        if (this.focusScope == focusScope) return this;
        this.focusScope = focusScope;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public int focusOrder() {
        return focusOrder;
    }

    public WidgetBase focusOrder(int focusOrder) {
        if (this.focusOrder == focusOrder) return this;
        this.focusOrder = focusOrder;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public boolean styleScope() {
        return styleScope;
    }

    public WidgetBase styleScope(boolean styleScope) {
        if (this.styleScope == styleScope) return this;
        this.styleScope = styleScope;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase localStyle(String widgetType, Style style) {
        if (widgetType == null || widgetType.isEmpty()) return this;
        if (style == null || style == Style.EMPTY) {
            localStyles.remove(widgetType);
        } else {
            localStyles.put(widgetType, style);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public Style localStyle(String widgetType) {
        if (widgetType == null || widgetType.isEmpty()) return Style.EMPTY;
        Style style = localStyles.get(widgetType);
        if (style != null) return style;
        style = localStyles.get(Theme.WILDCARD);
        return style == null ? Style.EMPTY : style;
    }

    @Override
    public void invalidate(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags |= flags;
        markSubtreeInvalidation(flags);
    }

    @Override
    public void clearInvalidation(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags &= ~flags;
        recomputeSubtreeInvalidation();
    }

    protected void recomputeSubtreeInvalidation() {
        int flags = invalidationFlags;
        for (Widget child : children()) {
            flags |= child.subtreeInvalidationFlags();
        }

        if (subtreeInvalidationFlags == flags) {
            return;
        }

        subtreeInvalidationFlags = flags;
        recomputeSubtreeInvalidation(parent);
    }

    private void markSubtreeInvalidation(int flags) {
        int previous = subtreeInvalidationFlags;
        subtreeInvalidationFlags |= flags;
        if (previous != subtreeInvalidationFlags) {
            if (parent instanceof WidgetBase base) {
                base.markSubtreeInvalidation(flags);
            } else if (parent != null) {
                parent.invalidate(flags);
            }
        }
    }

    private static void recomputeSubtreeInvalidation(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.recomputeSubtreeInvalidation();
        } else if (widget != null) {
            widget.invalidate(InvalidationFlags.ALL);
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
    }

    @Override
    public void arrange(RectView bounds) {
        layoutBounds.set(bounds);
    }

    @Override
    public void render(RenderContext context) {
    }

    @Override
    public void handle(Event event) {
        if (event instanceof PointerEnteredEvent entered && entered.phase() == EventPhase.TARGET) {
            setHovered(true);
        } else if (event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET) {
            setHovered(false);
        }
        emit(event);
    }

    private void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        invalidate(InvalidationFlags.VISUAL);
    }

    @Override
    public <T extends Event> EventSubscription on(EventType<T> type, EventListener<? super T> listener) {
        return events.on(type, listener);
    }

    @Override
    public void emit(Event event) {
        events.emit(event);
    }
}
