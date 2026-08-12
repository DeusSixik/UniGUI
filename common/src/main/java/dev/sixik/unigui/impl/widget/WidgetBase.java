package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.api.animation.AnimatedProperty;
import dev.sixik.unigui.api.animation.ColorTransition;
import dev.sixik.unigui.api.animation.FloatValueReader;
import dev.sixik.unigui.api.animation.FloatValueWriter;
import dev.sixik.unigui.api.animation.FloatTransition;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.animation.TransformOrigin;
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
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.event.FastEventEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class WidgetBase implements Widget {
    /**
     * Хранит подписки и отправку событий, связанных с жизненным циклом и вводом виджета.
     */
    private final FastEventEmitter events = new FastEventEmitter();
    /**
     * Хранит числовой параметр {@code EnumMap<AnimatedProperty}, влияющий на layout, ввод или отрисовку.
     */
    private final EnumMap<AnimatedProperty, FloatTransition> transitions = new EnumMap<>(AnimatedProperty.class);
    /**
     * Widget-specific scalar transitions, keyed by the property owner/name supplied by a widget.
     */
    private final Map<Object, ParameterTransition> parameterTransitions = new HashMap<>();
    /**
     * Color transitions are identity-keyed because MutableColor values are intentionally mutable.
     */
    private final IdentityHashMap<MutableColor, ColorTransition> colorTransitions = new IdentityHashMap<>();
    /**
     * Timed additive transform effects such as shake. These are layered over the base transition values.
     */
    private final List<ShakeEffect> shakeEffects = new ArrayList<>();
    /**
     * Named transform origin. CUSTOM keeps the raw pivot untouched for manual/custom pivot animations.
     */
    private TransformOrigin transformOrigin = TransformOrigin.CUSTOM;
    private float appliedEffectOffsetX;
    private float appliedEffectOffsetY;
    /**
     * Хранит рассчитанные границы виджета после прохода компоновки.
     */
    private final MutableRect layoutBounds = new MutableRect();
    /**
     * Хранит желаемый размер, который виджет сообщает layout-системе.
     */
    private LayoutSize desiredSize = LayoutSize.ZERO;
    /**
     * Хранит текущую трансформацию виджета для отрисовки и координат.
     */
    private final Transform transform = new Transform();
    /**
     * Хранит прозрачность виджета, применяемую при отрисовке.
     */
    private float opacity = 1.0f;
    /**
     * Хранит UI-контекст, через который виджет обращается к общей инфраструктуре.
     */
    private UIContext uiContext;
    /**
     * Хранит ссылку на родительский виджет в дереве интерфейса.
     */
    private Widget parent;
    /**
     * Хранит флаги областей виджета, требующих пересчёта или перерисовки.
     */
    private int invalidationFlags = InvalidationFlags.ALL;
    /**
     * Хранит агрегированные флаги инвалидации всего поддерева виджета.
     */
    private int subtreeInvalidationFlags = InvalidationFlags.ALL;
    /**
     * Хранит ограничения layout, которые влияют на доступный размер виджета.
     */
    private LayoutConstraints layoutConstraints = LayoutConstraints.DEFAULT;
    /**
     * Хранит изменяемые layout-настройки виджета и сообщает об их изменениях.
     */
    private final LayoutStyle layoutStyle = new LayoutStyle(this::onLayoutStyleChanged);
    /**
     * Флаг защищает синхронизацию layout-настроек от рекурсивных обновлений.
     */
    private boolean syncingLayoutStyle;
    /**
     * Хранит режим видимости, определяющий участие виджета в layout, вводе и render-проходе.
     */
    private Visibility visibility = Visibility.VISIBLE;
    /**
     * Флаг показывает, доступен ли виджет для взаимодействия пользователя.
     */
    private boolean enabled = true;
    /**
     * Флаг показывает, находится ли указатель мыши над виджетом.
     */
    private boolean hovered;
    /**
     * Хранит курсор мыши, который нужно показывать над этим виджетом.
     */
    private MouseCursor mouseCursor = MouseCursor.DEFAULT;
    /**
     * Флаг показывает, может ли виджет получать keyboard-focus.
     */
    private boolean focusable;
    /**
     * Флаг показывает, образует ли виджет отдельную область фокусировки.
     */
    private boolean focusScope;
    /**
     * Хранит порядок обхода фокуса относительно соседних виджетов.
     */
    private int focusOrder;
    /**
     * Флаг показывает, ограничивает ли виджет область применения локальных стилей.
     */
    private boolean styleScope;
    /**
     * Хранит локальные стили, переопределяющие оформление для конкретных типов виджетов.
     */
    private final Map<String, Style> localStyles = new HashMap<>();

    /**
     * Создаёт экземпляр {@code WidgetBase} и подготавливает начальное состояние виджета.
     */
    protected WidgetBase() {
        layoutBounds.onChanged(() -> invalidate(InvalidationFlags.LAYOUT));
        transform.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        syncLayoutStyleFromConstraints();
    }

    /**
     * Возвращает UI-контекст, к которому привязан виджет.
     */
    @Override
    public UIContext uiContext() {
        return uiContext;
    }

    /**
     * Внутренне назначает UI-контекст и синхронизирует его с дочерними виджетами.
     */
    public void setUiContextInternal(UIContext uiContext) {
        this.uiContext = uiContext;
    }

    /**
     * Возвращает родительский виджет в UI-дереве.
     */
    @Override
    public Widget parent() {
        return parent;
    }

    /**
     * Внутренне обновляет родителя виджета и связанные состояния дерева.
     */
    public void setParentInternal(Widget parent) {
        if (this.parent == parent) return;
        Widget oldParent = this.parent;
        this.parent = parent;

        recomputeSubtreeInvalidation(oldParent);
        recomputeSubtreeInvalidation(parent);
    }

    /**
     * Возвращает дочерние виджеты, участвующие в layout, input и render-проходах.
     */
    @Override
    public List<Widget> children() {
        return Collections.emptyList();
    }

    /**
     * Возвращает текущие границы виджета после layout-прохода.
     */
    @Override
    public RectView layoutBounds() {
        return layoutBounds;
    }

    /**
     * Возвращает размер, который виджет запросил на этапе измерения.
     */
    @Override
    public LayoutSize desiredSize() {
        return desiredSize;
    }

    /**
     * Записывает желаемый размер, вычисленный во время измерения виджета.
     */
    protected final void setDesiredSize(float width, float height) {
        setDesiredSize(LayoutSize.of(width, height));
    }

    /**
     * Записывает желаемый размер, вычисленный во время измерения виджета.
     */
    protected final void setDesiredSize(LayoutSize desiredSize) {
        this.desiredSize = desiredSize == null ? LayoutSize.ZERO : desiredSize;
    }

    /**
     * Приводит желаемый размер контента к ограничениям текущего layout-контекста.
     */
    protected final LayoutSize resolveDesiredSize(LayoutContext context, float contentWidth, float contentHeight) {
        return LayoutSize.of(contentWidth, contentHeight).resolve(layoutConstraints, context);
    }

    /**
     * Возвращает изменяемые границы для внутренних layout-операций.
     */
    protected MutableRect mutableLayoutBounds() {
        return layoutBounds;
    }

    /**
     * Возвращает трансформацию, применяемую к виджету при отрисовке.
     */
    @Override
    public Transform transform() {
        return transform;
    }

    public TransformOrigin transformOrigin() {
        return transformOrigin;
    }

    public WidgetBase transformOrigin(TransformOrigin origin) {
        TransformOrigin normalized = origin == null ? TransformOrigin.CUSTOM : origin;
        if (transformOrigin == normalized) return this;
        transformOrigin = normalized;
        if (!normalized.custom()) {
            transitions.remove(AnimatedProperty.PIVOT_X);
            transitions.remove(AnimatedProperty.PIVOT_Y);
        }
        applyTransformOrigin();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase transformPivot(float x, float y) {
        transformOrigin = TransformOrigin.CUSTOM;
        transitions.remove(AnimatedProperty.PIVOT_X);
        transitions.remove(AnimatedProperty.PIVOT_Y);
        setAnimatedValue(AnimatedProperty.PIVOT_X, sanitizeFinite(x));
        setAnimatedValue(AnimatedProperty.PIVOT_Y, sanitizeFinite(y));
        return this;
    }

    public WidgetBase animatePivot(float x, float y, float durationSeconds) {
        return animatePivot(x, y, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animatePivot(float x, float y, TransitionSpec spec) {
        transformOrigin = TransformOrigin.CUSTOM;
        animate(AnimatedProperty.PIVOT_X, sanitizeFinite(x), spec);
        animate(AnimatedProperty.PIVOT_Y, sanitizeFinite(y), spec);
        return this;
    }

    public WidgetBase rotationDegrees(float degrees) {
        transitions.remove(AnimatedProperty.ROTATION_DEGREES);
        setAnimatedValue(AnimatedProperty.ROTATION_DEGREES, sanitizeFinite(degrees));
        return this;
    }

    public WidgetBase animateRotation(float targetDegrees, float durationSeconds) {
        return animateRotation(targetDegrees, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateRotation(float targetDegrees, TransitionSpec spec) {
        return animate(AnimatedProperty.ROTATION_DEGREES, sanitizeFinite(targetDegrees), spec);
    }

    /**
     * Возвращает текущую прозрачность виджета.
     */
    public float opacity() {
        return opacity;
    }

    /**
     * Возвращает текущую прозрачность виджета.
     */
    public WidgetBase opacity(float opacity) {
        transitions.remove(AnimatedProperty.OPACITY);
        setAnimatedValue(AnimatedProperty.OPACITY, clamp01(opacity));
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animateOpacity}, меняющую состояние виджета.
     */
    public WidgetBase animateOpacity(float targetOpacity, float durationSeconds) {
        return animateOpacity(targetOpacity, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animateOpacity}, меняющую состояние виджета.
     */
    public WidgetBase animateOpacity(float targetOpacity, TransitionSpec spec) {
        return animate(AnimatedProperty.OPACITY, clamp01(targetOpacity), spec);
    }

    /**
     * Обновляет или выполняет операцию {@code animatePosition}, меняющую состояние виджета.
     */
    public WidgetBase animatePosition(float x, float y, float durationSeconds) {
        return animatePosition(x, y, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animatePosition}, меняющую состояние виджета.
     */
    public WidgetBase animatePosition(float x, float y, TransitionSpec spec) {
        animate(AnimatedProperty.POSITION_X, sanitizeFinite(x), spec);
        animate(AnimatedProperty.POSITION_Y, sanitizeFinite(y), spec);
        return this;
    }

    public WidgetBase animatePositionFrom(float startX, float startY, float endX, float endY, float durationSeconds) {
        return animatePositionFrom(startX, startY, endX, endY, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animatePositionFrom(float startX, float startY, float endX, float endY, TransitionSpec spec) {
        transitions.remove(AnimatedProperty.POSITION_X);
        transitions.remove(AnimatedProperty.POSITION_Y);
        setAnimatedValue(AnimatedProperty.POSITION_X, sanitizeFinite(startX));
        setAnimatedValue(AnimatedProperty.POSITION_Y, sanitizeFinite(startY));
        return animatePosition(endX, endY, spec);
    }

    /**
     * Обновляет или выполняет операцию {@code animateScale}, меняющую состояние виджета.
     */
    public WidgetBase animateScale(float x, float y, float durationSeconds) {
        return animateScale(x, y, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animateScale}, меняющую состояние виджета.
     */
    public WidgetBase animateScale(float x, float y, TransitionSpec spec) {
        animate(AnimatedProperty.SCALE_X, sanitizeFinite(x, 1.0f), spec);
        animate(AnimatedProperty.SCALE_Y, sanitizeFinite(y, 1.0f), spec);
        return this;
    }

    public WidgetBase shake(float amplitude, float durationSeconds) {
        return shake(amplitude, 0.0f, durationSeconds, 4);
    }

    public WidgetBase shake(float amplitudeX, float amplitudeY, float durationSeconds, int cycles) {
        float duration = sanitizeFinite(durationSeconds);
        if (duration <= 0.0f) return this;
        int normalizedCycles = Math.max(1, cycles);
        shakeEffects.add(new ShakeEffect(
                sanitizeFinite(amplitudeX),
                sanitizeFinite(amplitudeY),
                duration,
                normalizedCycles));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopShakeAnimations() {
        restoreTransformEffectOffset();
        shakeEffects.clear();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase animateParameter(Object key,
                                       FloatValueReader reader,
                                       FloatValueWriter writer,
                                       float targetValue,
                                       float durationSeconds) {
        return animateParameter(key, reader, writer, targetValue, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateParameter(Object key,
                                       FloatValueReader reader,
                                       FloatValueWriter writer,
                                       float targetValue,
                                       TransitionSpec spec) {
        if (writer == null) return this;
        float startValue = reader == null ? 0.0f : reader.get();
        return animateParameterFrom(key, startValue, writer, targetValue, spec);
    }

    public WidgetBase animateParameterFrom(Object key,
                                           float startValue,
                                           FloatValueWriter writer,
                                           float targetValue,
                                           float durationSeconds) {
        return animateParameterFrom(key, startValue, writer, targetValue, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateParameterFrom(Object key,
                                           float startValue,
                                           FloatValueWriter writer,
                                           float targetValue,
                                           TransitionSpec spec) {
        if (writer == null) return this;
        Object normalizedKey = animationKey(key, writer);
        TransitionSpec normalizedSpec = spec == null ? TransitionSpec.DEFAULT : spec;
        float start = sanitizeFinite(startValue);
        float target = sanitizeFinite(targetValue);
        if (normalizedSpec.durationSeconds() <= 0.0f || start == target) {
            parameterTransitions.remove(normalizedKey);
            writer.set(target);
            invalidate(InvalidationFlags.VISUAL);
            return this;
        }

        writer.set(start);
        parameterTransitions.put(normalizedKey, new ParameterTransition(new FloatTransition(start, target, normalizedSpec), writer));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopParameterAnimation(Object key) {
        if (key != null) {
            parameterTransitions.remove(key);
        }
        return this;
    }

    public WidgetBase stopParameterAnimations() {
        parameterTransitions.clear();
        return this;
    }

    public WidgetBase animateColor(MutableColor color, float r, float g, float b, float a, float durationSeconds) {
        return animateColor(color, new MutableColor(r, g, b, a), TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateColor(MutableColor color, float r, float g, float b, float a, TransitionSpec spec) {
        return animateColor(color, new MutableColor(r, g, b, a), spec);
    }

    public WidgetBase animateColor(MutableColor color, ColorView targetColor, float durationSeconds) {
        return animateColor(color, targetColor, TransitionSpec.of(durationSeconds));
    }

    public WidgetBase animateColor(MutableColor color, ColorView targetColor, TransitionSpec spec) {
        if (color == null || targetColor == null) return this;
        TransitionSpec normalized = spec == null ? TransitionSpec.DEFAULT : spec;
        MutableColor target = new MutableColor(
                sanitizeFinite(targetColor.r()),
                sanitizeFinite(targetColor.g()),
                sanitizeFinite(targetColor.b()),
                sanitizeFinite(targetColor.a(), 1.0f));
        if (normalized.durationSeconds() <= 0.0f || sameColor(color, target)) {
            colorTransitions.remove(color);
            color.set(target);
            invalidate(InvalidationFlags.VISUAL);
            return this;
        }

        colorTransitions.put(color, new ColorTransition(color.copy(), target, normalized));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopColorAnimation(MutableColor color) {
        if (color != null) {
            colorTransitions.remove(color);
        }
        return this;
    }

    public WidgetBase stopColorAnimations() {
        colorTransitions.clear();
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animate}, меняющую состояние виджета.
     */
    public WidgetBase animate(AnimatedProperty property, float targetValue, float durationSeconds) {
        return animate(property, targetValue, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animate}, меняющую состояние виджета.
     */
    public WidgetBase animate(AnimatedProperty property, float targetValue, TransitionSpec spec) {
        if (property == null) return this;
        if (usesCustomPivot(property)) {
            transformOrigin = TransformOrigin.CUSTOM;
        }
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

    /**
     * Обновляет или выполняет операцию {@code stopAnimation}, меняющую состояние виджета.
     */
    public WidgetBase stopAnimation(AnimatedProperty property) {
        if (property != null) {
            transitions.remove(property);
        }
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code stopAnimations} для виджета.
     */
    public WidgetBase stopAnimations() {
        transitions.clear();
        stopParameterAnimations();
        stopColorAnimations();
        stopShakeAnimations();
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animationRunning}, меняющую состояние виджета.
     */
    public boolean animationRunning(AnimatedProperty property) {
        return property != null && transitions.containsKey(property);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code animationsRunning} для виджета.
     */
    public boolean animationsRunning() {
        return !transitions.isEmpty()
                || !parameterTransitions.isEmpty()
                || !colorTransitions.isEmpty()
                || !shakeEffects.isEmpty();
    }

    /**
     * Возвращает флаги инвалидации самого виджета.
     */
    @Override
    public int invalidationFlags() {
        return invalidationFlags;
    }

    /**
     * Возвращает агрегированные флаги инвалидации виджета и всех его потомков.
     */
    @Override
    public int subtreeInvalidationFlags() {
        return subtreeInvalidationFlags;
    }

    /**
     * Возвращает ограничения layout, применяемые к виджету.
     */
    @Override
    public LayoutConstraints layoutConstraints() {
        return layoutConstraints;
    }

    /**
     * Возвращает изменяемый стиль layout для fluent-настройки размеров и ограничений.
     */
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

    /**
     * Синхронизирует внутреннее состояние виджета через {@code syncLayoutStyleFromConstraints}.
     */
    private void syncLayoutStyleFromConstraints() {
        syncingLayoutStyle = true;
        try {
            layoutStyle.applyLegacyConstraints(layoutConstraints);
        } finally {
            syncingLayoutStyle = false;
        }
    }

    /**
     * Внутренний callback вызывается при изменении layout-стиля и обновляет ограничения виджета.
     */
    private void onLayoutStyleChanged() {
        if (syncingLayoutStyle) return;
        layoutConstraints = layoutStyle.toLegacyConstraints(layoutConstraints);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /**
     * Возвращает или обновляет режим видимости виджета.
     */
    @Override
    public Visibility visibility() {
        return visibility;
    }

    /**
     * Возвращает или обновляет режим видимости виджета.
     */
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

    /**
     * Возвращает или задаёт простую видимость виджета через режим {@code Visibility}.
     */
    @Override
    public boolean visible() {
        return visibility == Visibility.VISIBLE;
    }

    /**
     * Возвращает или задаёт простую видимость виджета через режим {@code Visibility}.
     */
    public WidgetBase visible(boolean visible) {
        return visibility(visible ? Visibility.VISIBLE : Visibility.HIDDEN);
    }

    /**
     * Возвращает или задаёт доступность виджета для пользовательского ввода.
     */
    @Override
    public boolean enabled() {
        return enabled;
    }

    /**
     * Возвращает или задаёт доступность виджета для пользовательского ввода.
     */
    public WidgetBase enabled(boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает состояние наведения указателя на виджет.
     */
    @Override
    public boolean hovered() {
        return hovered;
    }

    /**
     * Возвращает или задаёт курсор мыши для виджета.
     */
    public MouseCursor mouseCursor() {
        return mouseCursor;
    }

    /**
     * Возвращает или задаёт курсор мыши для виджета.
     */
    public WidgetBase mouseCursor(MouseCursor mouseCursor) {
        this.mouseCursor = mouseCursor == null ? MouseCursor.DEFAULT : mouseCursor;
        return this;
    }

    /**
     * Возвращает курсор мыши для указанной локальной точки виджета.
     */
    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        return enabled && visibility == Visibility.VISIBLE ? mouseCursor : MouseCursor.DEFAULT;
    }

    /**
     * Возвращает или задаёт возможность виджета получать фокус.
     */
    @Override
    public boolean focusable() {
        return focusable;
    }

    /**
     * Возвращает или задаёт возможность виджета получать фокус.
     */
    public WidgetBase focusable(boolean focusable) {
        if (this.focusable == focusable) return this;
        this.focusable = focusable;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт режим отдельной области фокусировки для виджета.
     */
    @Override
    public boolean focusScope() {
        return focusScope;
    }

    /**
     * Возвращает или задаёт режим отдельной области фокусировки для виджета.
     */
    public WidgetBase focusScope(boolean focusScope) {
        if (this.focusScope == focusScope) return this;
        this.focusScope = focusScope;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт порядок обхода фокуса для виджета.
     */
    @Override
    public int focusOrder() {
        return focusOrder;
    }

    /**
     * Возвращает или задаёт порядок обхода фокуса для виджета.
     */
    public WidgetBase focusOrder(int focusOrder) {
        if (this.focusOrder == focusOrder) return this;
        this.focusOrder = focusOrder;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт область локального применения стилей.
     */
    @Override
    public boolean styleScope() {
        return styleScope;
    }

    /**
     * Возвращает или задаёт область локального применения стилей.
     */
    public WidgetBase styleScope(boolean styleScope) {
        if (this.styleScope == styleScope) return this;
        this.styleScope = styleScope;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт локальный стиль для указанного типа виджета.
     */
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

    /**
     * Возвращает или задаёт локальный стиль для указанного типа виджета.
     */
    @Override
    public Style localStyle(String widgetType) {
        if (widgetType == null || widgetType.isEmpty()) return Style.EMPTY;
        Style style = localStyles.get(widgetType);
        if (style != null) return style;
        style = localStyles.get(Theme.WILDCARD);
        return style == null ? Style.EMPTY : style;
    }

    /**
     * Помечает часть состояния виджета как требующую пересчёта или перерисовки.
     */
    /**
     * Returns the style type used by theme and local style lookup.
     */
    protected String styleType() {
        return getClass().getSimpleName();
    }

    /**
     * Resolves a style renderer override for this widget type.
     *
     * <p>Per-instance renderer setters should call this only when their local
     * renderer field is {@code null}. The lookup order is theme style, inherited
     * local styles, then {@code fallback}. Values with the wrong renderer type
     * are ignored.</p>
     */
    protected <T> T styleRenderer(Class<T> rendererType, T fallback) {
        if (rendererType == null) return fallback;
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        String type = styleType();
        Object value = theme.styleFor(type).get(StyleKeys.RENDERER, null, null);
        for (Widget current : styleLookupChain()) {
            Style localStyle = current.localStyle(type);
            value = localStyle.get(StyleKeys.RENDERER, null, value);
        }
        return rendererType.isInstance(value) ? rendererType.cast(value) : fallback;
    }

    private List<Widget> styleLookupChain() {
        List<Widget> chain = new ArrayList<>();
        Widget current = this;
        while (current != null) {
            chain.add(current);
            if (current != this && current.styleScope()) {
                break;
            }
            current = current.parent();
        }
        Collections.reverse(chain);
        return chain;
    }
    @Override
    public void invalidate(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags |= flags;
        markSubtreeInvalidation(flags);
    }

    /**
     * Снимает указанные флаги инвалидации после успешного обновления.
     */
    @Override
    public void clearInvalidation(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags &= ~flags;
        recomputeSubtreeInvalidation();
    }

    /**
     * Пересчитывает агрегированную инвалидацию поддерева виджета.
     */
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

    /**
     * Поднимает флаги инвалидации вверх по родительскому дереву.
     */
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

    /**
     * Пересчитывает агрегированную инвалидацию поддерева виджета.
     */
    private static void recomputeSubtreeInvalidation(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.recomputeSubtreeInvalidation();
        } else if (widget != null) {
            widget.invalidate(InvalidationFlags.ALL);
        }
    }

    /**
     * Измеряет желаемый размер виджета с учётом переданного layout-контекста.
     */
    @Override
    public void measure(LayoutContext context) {
        if (visibility == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
    }

    /**
     * Размещает виджет в рассчитанных границах и обновляет его layout-состояние.
     */
    @Override
    public void arrange(RectView bounds) {
        layoutBounds.set(bounds);
        applyTransformOrigin();
    }

    /**
     * Отрисовывает виджет в текущем render-контексте.
     */
    @Override
    public void render(RenderContext context) {
    }

    /**
     * Обновляет состояние виджета на каждом кадре.
     */
    @Override
    public void tick(FrameContext frame) {
        tickAnimations(frame);
    }

    /**
     * Обрабатывает входящее UI-событие и обновляет состояние виджета при необходимости.
     */
    @Override
    public void handle(Event event) {
        if (event instanceof PointerEnteredEvent entered && entered.phase() == EventPhase.TARGET) {
            setHovered(true);
        } else if (event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET) {
            setHovered(false);
        }
        emit(event);
    }

    /**
     * Внутренне обновляет состояние hover и вызывает визуальную инвалидацию.
     */
    private void setHovered(boolean hovered) {
        if (this.hovered == hovered) return;
        this.hovered = hovered;
        invalidate(InvalidationFlags.VISUAL);
    }

    /**
     * Регистрирует обработчик события и возвращает подписку для последующего снятия.
     */
    @Override
    public <T extends Event> EventSubscription on(EventType<T> type, EventListener<? super T> listener) {
        return events.on(type, listener);
    }

    /**
     * Отправляет событие подписчикам этого виджета.
     */
    @Override
    public void emit(Event event) {
        events.emit(event);
    }

    /**
     * Продвигает активные анимации и применяет их значения к виджету.
     */
    protected final void tickAnimations(FrameContext frame) {
        if (transitions.isEmpty() && parameterTransitions.isEmpty() && colorTransitions.isEmpty() && shakeEffects.isEmpty()) return;

        float deltaSeconds = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        restoreTransformEffectOffset();

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

        tickParameterTransitions(deltaSeconds);
        tickColorTransitions(deltaSeconds);
        applyShakeEffects(deltaSeconds);
        invalidate(InvalidationFlags.VISUAL);
    }

    /**
     * Добавляет прозрачность виджета в render-контекст перед отрисовкой.
     */
    protected final void pushOpacity(RenderContext context) {
        if (context != null) {
            context.pushOpacity(opacity);
        }
    }

    /**
     * Восстанавливает предыдущую прозрачность render-контекста после отрисовки.
     */
    protected final void popOpacity(RenderContext context) {
        if (context != null) {
            context.popOpacity();
        }
    }

    /**
     * Возвращает текущее значение свойства, учитывая активную анимацию.
     */
    private float currentAnimatedValue(AnimatedProperty property) {
        return switch (property) {
            case OPACITY -> opacity;
            case POSITION_X -> transform.position().x();
            case POSITION_Y -> transform.position().y();
            case SCALE_X -> transform.scale().x();
            case SCALE_Y -> transform.scale().y();
            case ROTATION_DEGREES -> transform.rotationDegrees();
            case PIVOT_X -> transform.pivot().x();
            case PIVOT_Y -> transform.pivot().y();
        };
    }

    /**
     * Применяет вычисленное значение анимируемого свойства к виджету.
     */
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
            case ROTATION_DEGREES -> transform.setRotationDegrees(normalized);
            case PIVOT_X -> transform.pivot().set(normalized, transform.pivot().y());
            case PIVOT_Y -> transform.pivot().set(transform.pivot().x(), normalized);
        }
    }

    /**
     * Нормализует значение анимируемого свойства перед сохранением.
     */
    private static float normalizedValue(AnimatedProperty property, float value) {
        return switch (property) {
            case OPACITY -> clamp01(value);
            case SCALE_X, SCALE_Y -> sanitizeFinite(value, 1.0f);
            case POSITION_X, POSITION_Y, ROTATION_DEGREES, PIVOT_X, PIVOT_Y -> sanitizeFinite(value);
        };
    }

    private void applyTransformOrigin() {
        if (transformOrigin == null || transformOrigin.custom()) return;
        transform.pivot().set(
                layoutBounds.width() * transformOrigin.relativeX(),
                layoutBounds.height() * transformOrigin.relativeY());
    }

    private void tickParameterTransitions(float deltaSeconds) {
        if (parameterTransitions.isEmpty()) return;

        Iterator<Map.Entry<Object, ParameterTransition>> iterator = parameterTransitions.entrySet().iterator();
        while (iterator.hasNext()) {
            ParameterTransition entry = iterator.next().getValue();
            FloatTransition transition = entry.transition();
            entry.writer().set(transition.tick(deltaSeconds));
            if (transition.finished()) {
                entry.writer().set(transition.end());
                iterator.remove();
            }
        }
    }

    private void tickColorTransitions(float deltaSeconds) {
        if (colorTransitions.isEmpty()) return;

        Iterator<Map.Entry<MutableColor, ColorTransition>> iterator = colorTransitions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MutableColor, ColorTransition> entry = iterator.next();
            ColorTransition transition = entry.getValue();
            transition.tick(deltaSeconds, entry.getKey());
            if (transition.finished()) {
                transition.finish(entry.getKey());
                iterator.remove();
            }
        }
    }

    private void restoreTransformEffectOffset() {
        if (appliedEffectOffsetX == 0.0f && appliedEffectOffsetY == 0.0f) return;
        transform.position().set(
                transform.position().x() - appliedEffectOffsetX,
                transform.position().y() - appliedEffectOffsetY);
        appliedEffectOffsetX = 0.0f;
        appliedEffectOffsetY = 0.0f;
    }

    private void applyShakeEffects(float deltaSeconds) {
        if (shakeEffects.isEmpty()) return;

        float offsetX = 0.0f;
        float offsetY = 0.0f;
        Iterator<ShakeEffect> iterator = shakeEffects.iterator();
        while (iterator.hasNext()) {
            ShakeEffect effect = iterator.next();
            effect.tick(deltaSeconds);
            if (effect.finished()) {
                iterator.remove();
                continue;
            }
            offsetX += effect.offsetX();
            offsetY += effect.offsetY();
        }

        if (offsetX != 0.0f || offsetY != 0.0f) {
            transform.position().set(transform.position().x() + offsetX, transform.position().y() + offsetY);
            appliedEffectOffsetX = offsetX;
            appliedEffectOffsetY = offsetY;
        }
    }

    private static boolean usesCustomPivot(AnimatedProperty property) {
        return property == AnimatedProperty.PIVOT_X || property == AnimatedProperty.PIVOT_Y;
    }

    private static Object animationKey(Object key, FloatValueWriter writer) {
        return key == null ? writer : key;
    }

    private static boolean sameColor(ColorView left, ColorView right) {
        return left.r() == right.r()
                && left.g() == right.g()
                && left.b() == right.b()
                && left.a() == right.a();
    }

    private record ParameterTransition(FloatTransition transition, FloatValueWriter writer) {
    }

    private static final class ShakeEffect {
        private final float amplitudeX;
        private final float amplitudeY;
        private final float durationSeconds;
        private final int cycles;
        private float elapsedSeconds;

        private ShakeEffect(float amplitudeX, float amplitudeY, float durationSeconds, int cycles) {
            this.amplitudeX = amplitudeX;
            this.amplitudeY = amplitudeY;
            this.durationSeconds = Math.max(0.0f, durationSeconds);
            this.cycles = Math.max(1, cycles);
        }

        private void tick(float deltaSeconds) {
            elapsedSeconds = Math.min(durationSeconds, elapsedSeconds + Math.max(0.0f, deltaSeconds));
        }

        private boolean finished() {
            return durationSeconds <= 0.0f || elapsedSeconds >= durationSeconds;
        }

        private float offsetX() {
            return offset(amplitudeX);
        }

        private float offsetY() {
            return offset(amplitudeY);
        }

        private float offset(float amplitude) {
            if (amplitude == 0.0f || durationSeconds <= 0.0f) return 0.0f;
            float progress = Math.max(0.0f, Math.min(1.0f, elapsedSeconds / durationSeconds));
            float decay = 1.0f - progress;
            return (float) Math.sin(progress * cycles * Math.PI * 2.0f) * amplitude * decay;
        }
    }

    /**
     * Ограничивает число диапазоном от 0 до 1.
     */
    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /**
     * Заменяет невалидное или бесконечное число безопасным fallback-значением.
     */
    private static float sanitizeFinite(float value) {
        return sanitizeFinite(value, 0.0f);
    }

    /**
     * Заменяет невалидное или бесконечное число безопасным fallback-значением.
     */
    private static float sanitizeFinite(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }
}
