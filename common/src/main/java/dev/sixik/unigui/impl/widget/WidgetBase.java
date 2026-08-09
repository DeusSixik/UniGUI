package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.api.animation.AnimatedProperty;
import dev.sixik.unigui.api.animation.FloatTransition;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.EventType;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.LayoutStyle;
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
import java.util.EnumMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class WidgetBase implements Widget {
    private final FastEventEmitter events = new FastEventEmitter();
    private final EnumMap<AnimatedProperty, FloatTransition> transitions = new EnumMap<>(AnimatedProperty.class);
    private final MutableRect layoutBounds = new MutableRect();
    private LayoutSize desiredSize = LayoutSize.ZERO;
    private final Transform transform = new Transform();
    private float opacity = 1.0f;
    private UIContext uiContext;
    private Widget parent;
    private int invalidationFlags = InvalidationFlags.ALL;
    private int subtreeInvalidationFlags = InvalidationFlags.ALL;
    private LayoutConstraints layoutConstraints = LayoutConstraints.DEFAULT;
    private final LayoutStyle layoutStyle = new LayoutStyle(this::onLayoutStyleChanged);
    private boolean syncingLayoutStyle;
    private Visibility visibility = Visibility.VISIBLE;
    private boolean enabled = true;
    private boolean hovered;
    private MouseCursor mouseCursor = MouseCursor.DEFAULT;
    private boolean focusable;
    private boolean focusScope;
    private int focusOrder;
    private boolean styleScope;
    private final Map<String, Style> localStyles = new HashMap<>();

    protected WidgetBase() {
        layoutBounds.onChanged(() -> invalidate(InvalidationFlags.LAYOUT));
        transform.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        syncLayoutStyleFromConstraints();
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

    public float opacity() {
        return opacity;
    }

    public WidgetBase opacity(float opacity) {
        transitions.remove(AnimatedProperty.OPACITY);
        setAnimatedValue(AnimatedProperty.OPACITY, clamp01(opacity));
        return this;
    }

    public WidgetBase animateOpacity(float targetOpacity, float durationSeconds) {
        return animateOpacity(targetOpacity, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateOpacity(float targetOpacity, TransitionSpec spec) {
        return animate(AnimatedProperty.OPACITY, clamp01(targetOpacity), spec);
    }

    public WidgetBase animatePosition(float x, float y, float durationSeconds) {
        return animatePosition(x, y, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animatePosition(float x, float y, TransitionSpec spec) {
        animate(AnimatedProperty.POSITION_X, sanitizeFinite(x), spec);
        animate(AnimatedProperty.POSITION_Y, sanitizeFinite(y), spec);
        return this;
    }

    public WidgetBase animateScale(float x, float y, float durationSeconds) {
        return animateScale(x, y, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateScale(float x, float y, TransitionSpec spec) {
        animate(AnimatedProperty.SCALE_X, sanitizeFinite(x, 1.0f), spec);
        animate(AnimatedProperty.SCALE_Y, sanitizeFinite(y, 1.0f), spec);
        return this;
    }

    public WidgetBase animate(AnimatedProperty property, float targetValue, float durationSeconds) {
        return animate(property, targetValue, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animate(AnimatedProperty property, float targetValue, TransitionSpec spec) {
        if (property == null) return this;
        TransitionSpec normalized = spec == null ? TransitionSpec.DEFAULT : spec;
        float target = normalizedValue(property, targetValue);
        if (normalized.durationSeconds() <= 0.0f || currentAnimatedValue(property) == target) {
            transitions.remove(property);
            setAnimatedValue(property, target);
            return this;
        }

        transitions.put(property, new FloatTransition(currentAnimatedValue(property), target, normalized));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopAnimation(AnimatedProperty property) {
        if (property != null) {
            transitions.remove(property);
        }
        return this;
    }

    public WidgetBase stopAnimations() {
        transitions.clear();
        return this;
    }

    public boolean animationRunning(AnimatedProperty property) {
        return property != null && transitions.containsKey(property);
    }

    public boolean animationsRunning() {
        return !transitions.isEmpty();
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

    public LayoutStyle layoutStyle() {
        return layoutStyle;
    }

    /**
     * Applies layout properties without replacing unrelated style values.
     *
     * <pre>{@code
     * button.layout(style -> style
     *         .widthPercent(50.0f)
     *         .minWidth(48.0f)
     *         .flexGrow(1.0f)
     *         .overflowX(Overflow.HIDDEN));
     * }</pre>
     */
    public WidgetBase layout(Consumer<LayoutStyle> update) {
        layoutStyle.update(update);
        return this;
    }

    private void syncLayoutStyleFromConstraints() {
        syncingLayoutStyle = true;
        try {
            layoutStyle.applyLegacyConstraints(layoutConstraints);
        } finally {
            syncingLayoutStyle = false;
        }
    }

    private void onLayoutStyleChanged() {
        if (syncingLayoutStyle) return;
        layoutConstraints = layoutStyle.toLegacyConstraints(layoutConstraints);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
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

    public MouseCursor mouseCursor() {
        return mouseCursor;
    }

    public WidgetBase mouseCursor(MouseCursor mouseCursor) {
        this.mouseCursor = mouseCursor == null ? MouseCursor.DEFAULT : mouseCursor;
        return this;
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        return enabled && visibility == Visibility.VISIBLE ? mouseCursor : MouseCursor.DEFAULT;
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
    public void tick(FrameContext frame) {
        tickAnimations(frame);
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

    protected final void tickAnimations(FrameContext frame) {
        if (transitions.isEmpty()) return;

        float deltaSeconds = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        Iterator<Map.Entry<AnimatedProperty, FloatTransition>> iterator = transitions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<AnimatedProperty, FloatTransition> entry = iterator.next();
            FloatTransition transition = entry.getValue();
            setAnimatedValue(entry.getKey(), transition.tick(deltaSeconds));
            if (transition.finished()) {
                setAnimatedValue(entry.getKey(), transition.end());
                iterator.remove();
            }
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    protected final void pushOpacity(RenderContext context) {
        if (context != null) {
            context.pushOpacity(opacity);
        }
    }

    protected final void popOpacity(RenderContext context) {
        if (context != null) {
            context.popOpacity();
        }
    }

    private float currentAnimatedValue(AnimatedProperty property) {
        return switch (property) {
            case OPACITY -> opacity;
            case POSITION_X -> transform.position().x();
            case POSITION_Y -> transform.position().y();
            case SCALE_X -> transform.scale().x();
            case SCALE_Y -> transform.scale().y();
        };
    }

    private void setAnimatedValue(AnimatedProperty property, float value) {
        float normalized = normalizedValue(property, value);
        switch (property) {
            case OPACITY -> {
                if (opacity != normalized) {
                    opacity = normalized;
                    invalidate(InvalidationFlags.VISUAL);
                }
            }
            case POSITION_X -> transform.position().set(normalized, transform.position().y());
            case POSITION_Y -> transform.position().set(transform.position().x(), normalized);
            case SCALE_X -> transform.scale().set(normalized, transform.scale().y());
            case SCALE_Y -> transform.scale().set(transform.scale().x(), normalized);
        }
    }

    private static float normalizedValue(AnimatedProperty property, float value) {
        return switch (property) {
            case OPACITY -> clamp01(value);
            case SCALE_X, SCALE_Y -> sanitizeFinite(value, 1.0f);
            case POSITION_X, POSITION_Y -> sanitizeFinite(value);
        };
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float sanitizeFinite(float value) {
        return sanitizeFinite(value, 0.0f);
    }

    private static float sanitizeFinite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
