package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.api.animation.AnimatedProperty;
import dev.sixik.unigui.api.animation.AnimationClock;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.animation.FloatInterpolator;
import dev.sixik.unigui.api.animation.ColorTransition;
import dev.sixik.unigui.api.animation.FloatValueReader;
import dev.sixik.unigui.api.animation.FloatValueWriter;
import dev.sixik.unigui.api.animation.FloatTransition;
import dev.sixik.unigui.api.animation.AnimationController;
import dev.sixik.unigui.api.animation.LayoutTransitionAnimation;
import dev.sixik.unigui.api.animation.SpringAnimation;
import dev.sixik.unigui.api.animation.ShakeAnimation;
import dev.sixik.unigui.api.animation.NamedWidgetRegistry;
import dev.sixik.unigui.api.animation.PropertyPathResolver;
import dev.sixik.unigui.api.animation.Storyboard;
import dev.sixik.unigui.api.animation.StoryboardPlayer;
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
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.render.WidgetRendererRegistry;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlLayoutAttributes;
import dev.sixik.unigui.api.xml.XmlStyleAttributes;
import dev.sixik.unigui.impl.event.FastEventEmitter;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.api.style.StyleAnimationIds;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@XmlLayoutAttributes
@XmlStyleAttributes
public abstract class WidgetBase implements Widget {
    /** Общие animation property id, доступные всем виджетам. */
    public static final class AnimationProperties {
        public static final String POSITION_X = StyleAnimationIds.Property.POSITION_X;
        public static final String POSITION_Y = StyleAnimationIds.Property.POSITION_Y;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String SCALE_X = StyleAnimationIds.Property.SCALE_X;
        public static final String SCALE_Y = StyleAnimationIds.Property.SCALE_Y;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.COMMON_WIDGET;

        private AnimationProperties() {
        }
    }

    /** Общие event id для hover/focus/press анимаций. */
    public static final class AnimationEvents {
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_HOVER_ENTER = StyleAnimationIds.Event.ON_HOVER_ENTER;
        public static final String ON_HOVER_EXIT = StyleAnimationIds.Event.ON_HOVER_EXIT;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.COMMON_WIDGET;

        private AnimationEvents() {
        }
    }

    /**
     * Хранит подписки и отправку событий, связанных с жизненным циклом и вводом виджета.
     */
    private final FastEventEmitter events = new FastEventEmitter();
    /** Единый реестр всех активных анимаций виджета. */
    private final AnimationController animations = new AnimationController();

    private static final int SCOPE_PROPERTY = 1;
    private static final int SCOPE_PARAMETER = 2;
    private static final int SCOPE_COLOR = 3;
    private static final int SCOPE_SPRING_PROPERTY = 4;
    private static final int SCOPE_SPRING_PARAMETER = 5;
    private static final Object LAYOUT_TRANSITION_KEY = new Object();
    /**
     * Named transform origin. CUSTOM keeps the raw pivot untouched for manual/custom pivot animations.
     */
    private TransformOrigin transformOrigin = TransformOrigin.CUSTOM;
    private float appliedEffectOffsetX;
    private float appliedEffectOffsetY;
    private boolean layoutTransitionsEnabled;
    private TransitionSpec layoutTransitionSpec = TransitionSpec.DEFAULT;
    private boolean layoutBoundsObserved;
    private float previousLayoutX;
    private float previousLayoutY;
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
    /** Ленивый retained-фрагмент draw-команд виджета без дочерних элементов. */
    private DrawList renderCache;
    /** Контекст, записывающий draw-команды в retained-фрагмент. */
    private DefaultRenderContext renderCacheContext;
    /** Показывает, что retained-фрагмент нужно пересобрать. */
    private boolean renderCacheDirty = true;
    /** Позволяет отключить retained-кэш для заведомо динамического виджета. */
    private boolean renderCachingEnabled = true;
    /** Защищает от рекурсивного входа во время записи фрагмента. */
    private boolean renderingCache;
    /** Backend, для которого последний раз строился фрагмент. */
    private RenderBackend renderCacheBackend;
    /** Агрегированная версия theme и локальных style scope последней сборки. */
    private long renderCacheStyleVersion = Long.MIN_VALUE;
    /** Счётчики используются тестами и профилированием конкретного виджета. */
    private long renderCacheHits;
    private long renderCacheRebuilds;
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
     * Runtime/editor id виджета для XML/code-behind lookup и будущего editor tree.
     */
    private String id = "";
    private String styleId = "";
    private final LinkedHashSet<String> styleClasses = new LinkedHashSet<>();

    /**
     * Создаёт экземпляр {@code WidgetBase} и подготавливает начальное состояние виджета.
     */
    protected WidgetBase() {
        layoutBounds.onChanged(this::layoutBoundsChanged);
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
        animations.dispatcher(uiContext == null ? null : uiContext.dispatcher());
        renderCacheDirty = true;
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
        renderCacheDirty = true;

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
     * Возвращает runtime/editor id виджета.
     */
    @Override
    public String id() {
        return id;
    }

    /**
     * Задаёт runtime/editor id виджета.
     */
    @Override
    @XmlAttribute(value = "id", category = "Common", description = "Runtime/debug/editor identifier for code-behind lookup.")
    public WidgetBase id(String id) {
        String normalized = id == null ? "" : id;
        if (this.id.equals(normalized)) return this;
        this.id = normalized;
        return this;
    }
    /**
     * Возвращает явный id стиля виджета.
     */
    @Override
    public String styleId() {
        return styleId;
    }

    /**
     * Задаёт явный id стиля виджета через XML-атрибут {@code style}.
     */
    @Override
    @XmlAttribute(value = "style", category = "Common", description = "Explicit StylePack style id applied to this widget.")
    public WidgetBase styleId(String styleId) {
        String normalized = styleId == null ? "" : styleId.trim();
        if (this.styleId.equals(normalized)) return this;
        this.styleId = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает style classes виджета.
     */
    @Override
    public List<String> styleClasses() {
        return List.copyOf(styleClasses);
    }

    /**
     * Задаёт style classes через XML-атрибут {@code class}.
     */
    @Override
    @XmlAttribute(value = "class", category = "Common", description = "Space-separated StylePack classes applied to this widget.")
    public WidgetBase styleClass(String styleClasses) {
        LinkedHashSet<String> parsed = parseStyleClasses(styleClasses);
        if (this.styleClasses.equals(parsed)) return this;
        this.styleClasses.clear();
        this.styleClasses.addAll(parsed);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт style classes одной строкой.
     */
    @Override
    public WidgetBase styleClasses(String styleClasses) {
        return styleClass(styleClasses);
    }

    /**
     * Добавляет один style class.
     */
    @Override
    public WidgetBase addStyleClass(String styleClass) {
        String normalized = normalizeStyleClass(styleClass);
        if (normalized.isEmpty() || !styleClasses.add(normalized)) return this;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет один style class.
     */
    @Override
    public WidgetBase removeStyleClass(String styleClass) {
        String normalized = normalizeStyleClass(styleClass);
        if (normalized.isEmpty() || !styleClasses.remove(normalized)) return this;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Проверяет наличие style class.
     */
    @Override
    public boolean hasStyleClass(String styleClass) {
        String normalized = normalizeStyleClass(styleClass);
        return !normalized.isEmpty() && styleClasses.contains(normalized);
    }

    /**
     * Возвращает текущие границы виджета после layout-прохода.
     */
    @Override
    public RectView layoutBounds() {
        return layoutBounds;
    }

    /**
     * Возвращает, включены ли FLIP-переходы перемещения после изменения layout.
     *
     * <p>Переходы выключены по умолчанию. Размер виджета не анимируется: FLIP
     * применяется только когда меняются координаты {@code x} или {@code y}.</p>
     */
    public boolean layoutTransitionsEnabled() {
        return layoutTransitionsEnabled;
    }

    /**
     * Включает или выключает FLIP-переходы перемещения для этого виджета.
     *
     * @param enabled {@code true}, чтобы анимировать последующие перемещения
     * @return этот виджет
     */
    @XmlAttribute(value = "layoutTransitionsEnabled", category = "Animation", defaultValue = "false",
            description = "Включает FLIP-переходы при перемещении виджета layout-системой.")
    public WidgetBase layoutTransitionsEnabled(boolean enabled) {
        if (layoutTransitionsEnabled == enabled) return this;
        layoutTransitionsEnabled = enabled;
        if (!enabled) {
            restoreTransformEffectOffset();
            animations.stop(LAYOUT_TRANSITION_KEY);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /** @return параметры следующего FLIP-перехода */
    public TransitionSpec layoutTransitionSpec() {
        return layoutTransitionSpec;
    }

    /**
     * Устанавливает параметры FLIP-перехода.
     *
     * @param spec параметры перехода; {@code null} заменяется на {@link TransitionSpec#DEFAULT}
     * @return этот виджет
     */
    public WidgetBase layoutTransitionSpec(TransitionSpec spec) {
        layoutTransitionSpec = spec == null ? TransitionSpec.DEFAULT : spec;
        return this;
    }

    /**
     * Включает FLIP-переход с указанной длительностью и стандартным easing.
     *
     * @param durationSeconds длительность перехода в секундах
     * @return этот виджет
     */
    public WidgetBase layoutTransition(float durationSeconds) {
        return layoutTransition(durationSeconds, AnimationEasing.EASE_OUT);
    }

    /**
     * Включает FLIP-переход с указанными параметрами.
     *
     * @param durationSeconds длительность перехода в секундах
     * @param easing функция плавности; {@code null} означает линейный easing
     * @return этот виджет
     */
    public WidgetBase layoutTransition(float durationSeconds, Easing easing) {
        layoutTransitionSpec = TransitionSpec.of(durationSeconds, easing);
        return layoutTransitionsEnabled(true);
    }

    /**
     * Возвращает текущую FLIP-анимацию или {@code null}, если переход не выполняется.
     *
     * @return активная анимация перемещения
     */
    public LayoutTransitionAnimation layoutTransitionAnimation() {
        return animations.get(LAYOUT_TRANSITION_KEY) instanceof LayoutTransitionAnimation animation
                ? animation : null;
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
            animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.PIVOT_X, false);
            animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.PIVOT_Y, false);
        }
        applyTransformOrigin();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase transformPivot(float x, float y) {
        transformOrigin = TransformOrigin.CUSTOM;
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.PIVOT_X, false);
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.PIVOT_Y, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.PIVOT_X, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.PIVOT_Y, false);
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

    @XmlAttribute(value = "rotation", category = "Appearance", defaultValue = "0", description = "Rotation in degrees applied to the widget transform.")
    public WidgetBase rotationDegrees(float degrees) {
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.ROTATION_DEGREES, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.ROTATION_DEGREES, false);
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
    @XmlAttribute(value = "opacity", category = "Appearance", defaultValue = "1", description = "Widget opacity clamped between 0 and 1.")
    public WidgetBase opacity(float opacity) {
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.OPACITY, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.OPACITY, false);
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
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.POSITION_X, false);
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.POSITION_Y, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.POSITION_X, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.POSITION_Y, false);
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
        ShakeAnimation effect = new ShakeAnimation(
                sanitizeFinite(amplitudeX),
                sanitizeFinite(amplitudeY),
                duration,
                normalizedCycles);
        animations.play(effect, effect);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopShakeAnimations() {
        restoreTransformEffectOffset();
        animations.stopAllOf(ShakeAnimation.class);
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
            animations.stopScoped(SCOPE_PARAMETER, normalizedKey, false);
            if (start != target) {
                writer.set(target);
                invalidate(InvalidationFlags.VISUAL);
            }
            return this;
        }

        FloatAnimation current = animations.getScoped(SCOPE_PARAMETER, normalizedKey, false) instanceof FloatAnimation floatAnimation
                ? floatAnimation : null;
        if (current != null
                && current.writer == writer
                && current.transition.matches(normalizedSpec, FloatInterpolator.LINEAR)
                && current.transition.end() == target) {
            return this;
        }
        if (current != null && current.writer == writer && current.transition.matches(normalizedSpec, FloatInterpolator.LINEAR)) {
            current.retarget(target, writer);
        } else {
            writer.set(start);
            animations.playScoped(SCOPE_PARAMETER, normalizedKey, false,
                    new FloatAnimation(new FloatTransition(start, target, normalizedSpec), writer));
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopParameterAnimation(Object key) {
        if (key != null) {
            animations.stopScoped(SCOPE_PARAMETER, key, false);
        }
        return this;
    }

    public WidgetBase stopParameterAnimations() {
        animations.stopScope(SCOPE_PARAMETER);
        return this;
    }

    /**
     * Запускает пружинную анимацию кастомного float-параметра.
     *
     * @param key ключ параметра; повторный вызов заменяет предыдущую пружину
     * @param reader источник текущего значения
     * @param writer получатель результата
     * @param target целевое значение
     * @param stiffness жёсткость пружины
     * @param damping затухание пружины
     * @return этот виджет
     */
    public WidgetBase animateSpringParameter(Object key, FloatValueReader reader, FloatValueWriter writer,
                                             float target, float stiffness, float damping) {
        if (writer == null) return this;
        Object normalizedKey = animationKey(key, writer);
        SpringAnimation current = animations.getScoped(SCOPE_SPRING_PARAMETER, normalizedKey, false) instanceof SpringAnimation spring
                ? spring : null;
        if (current != null && current.matches(stiffness, damping)) {
            current.retarget(target, writer);
        } else {
            animations.playScoped(SCOPE_SPRING_PARAMETER, normalizedKey, false, new SpringAnimation(
                    reader == null ? 0.0f : reader.get(), target, stiffness, damping, writer));
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /** Останавливает spring-анимацию кастомного параметра. */
    public WidgetBase stopSpringParameterAnimation(Object key) {
        animations.stopScoped(SCOPE_SPRING_PARAMETER, key, false);
        return this;
    }

    /**
     * Запускает пружинную анимацию встроенного свойства виджета.
     *
     * @param property анимируемое свойство
     * @param targetValue целевое значение
     * @param stiffness жёсткость пружины
     * @param damping затухание пружины
     * @return этот виджет
     */
    public WidgetBase animateSpring(AnimatedProperty property, float targetValue,
                                    float stiffness, float damping) {
        if (property == null) return this;
        if (usesCustomPivot(property)) transformOrigin = TransformOrigin.CUSTOM;
        float target = normalizedValue(property, targetValue);
        animations.stopScoped(SCOPE_PROPERTY, property, false);
        SpringAnimation current = animations.getScoped(SCOPE_SPRING_PROPERTY, property, false) instanceof SpringAnimation spring
                ? spring : null;
        if (current != null && current.matches(stiffness, damping)) {
            current.retarget(target);
        } else {
            animations.playScoped(SCOPE_SPRING_PROPERTY, property, false, new SpringAnimation(
                    currentAnimatedValue(property), target, stiffness, damping,
                    value -> setAnimatedValue(property, value)));
        }
        invalidate(InvalidationFlags.VISUAL);
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
        if (normalized.durationSeconds() <= 0.0f || sameColor(color, targetColor)) {
            animations.stopScoped(SCOPE_COLOR, color, true);
            if (!sameColor(color, targetColor)) {
                color.set(
                        sanitizeFinite(targetColor.r()),
                        sanitizeFinite(targetColor.g()),
                        sanitizeFinite(targetColor.b()),
                        sanitizeFinite(targetColor.a(), 1.0f));
                invalidate(InvalidationFlags.VISUAL);
            }
            return this;
        }

        ColorAnimation current = animations.getScoped(SCOPE_COLOR, color, true) instanceof ColorAnimation colorAnimation
                ? colorAnimation : null;
        if (current != null && current.matches(targetColor, normalized)) return this;

        MutableColor target = new MutableColor(
                sanitizeFinite(targetColor.r()),
                sanitizeFinite(targetColor.g()),
                sanitizeFinite(targetColor.b()),
                sanitizeFinite(targetColor.a(), 1.0f));
        animations.playScoped(SCOPE_COLOR, color, true,
                new ColorAnimation(new ColorTransition(color.copy(), target, normalized), color, target, normalized));
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetBase stopColorAnimation(MutableColor color) {
        if (color != null) {
            animations.stopScoped(SCOPE_COLOR, color, true);
        }
        return this;
    }

    public WidgetBase stopColorAnimations() {
        animations.stopScope(SCOPE_COLOR);
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
        animations.stopScoped(SCOPE_SPRING_PROPERTY, property, false);
        if (usesCustomPivot(property)) {
            transformOrigin = TransformOrigin.CUSTOM;
        }
        TransitionSpec normalized = spec == null ? TransitionSpec.DEFAULT : spec;
        float target = normalizedValue(property, targetValue);
        if (normalized.durationSeconds() <= 0.0f || currentAnimatedValue(property) == target) {
            animations.stopScoped(SCOPE_PROPERTY, property, false);
            setAnimatedValue(property, target);
            return this;
        }

        FloatInterpolator interpolator = property == AnimatedProperty.ROTATION_DEGREES
                ? dev.sixik.unigui.api.animation.AngleInterpolator.SHORTEST_PATH
                : FloatInterpolator.LINEAR;
        FloatAnimation current = animations.getScoped(SCOPE_PROPERTY, property, false) instanceof FloatAnimation floatAnimation
                ? floatAnimation : null;
        if (current != null && current.transition.matches(normalized, interpolator)) {
            current.transition.retarget(target);
        } else {
            animations.playScoped(SCOPE_PROPERTY, property, false,
                    new FloatAnimation(new FloatTransition(currentAnimatedValue(property), target, normalized, interpolator),
                            value -> setAnimatedValue(property, value)));
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code stopAnimation}, меняющую состояние виджета.
     */
    public WidgetBase stopAnimation(AnimatedProperty property) {
        if (property != null) {
            animations.stopScoped(SCOPE_PROPERTY, property, false);
            animations.stopScoped(SCOPE_SPRING_PROPERTY, property, false);
        }
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code stopAnimations} для виджета.
     */
    public WidgetBase stopAnimations() {
        restoreTransformEffectOffset();
        animations.clear();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Компилирует и запускает storyboard на этом виджете как на корне target-registry.
     *
     * <p>Метод следует вызывать из UI-потока после построения дерева. Player автоматически
     * обновляется обычным {@link #tick(FrameContext)} корневого виджета.</p>
     *
     * @param storyboard описание анимации
     * @return запущенный player для pause/seek/restart
     */
    public StoryboardPlayer playStoryboard(Storyboard storyboard) {
        return playStoryboard(null, storyboard, null);
    }

    /** Запускает storyboard под стабильным ключом, заменяя предыдущую анимацию с тем же ключом. */
    public StoryboardPlayer playStoryboard(Object key, Storyboard storyboard) {
        return playStoryboard(key, storyboard, null);
    }

    /**
     * Запускает storyboard с пользовательским resolver'ом property path.
     *
     * @param key ключ в animation controller; {@code null} использует сам player
     * @param storyboard описание анимации
     * @param resolver resolver встроенных и пользовательских свойств; {@code null} использует built-ins
     * @return запущенный player
     */
    public StoryboardPlayer playStoryboard(Object key,
                                           Storyboard storyboard,
                                           PropertyPathResolver resolver) {
        StoryboardPlayer player = new StoryboardPlayer(
                storyboard,
                NamedWidgetRegistry.from(this),
                resolver == null ? PropertyPathResolver.builtIns() : resolver);
        animations.play(key == null ? player : key, player);
        invalidate(InvalidationFlags.VISUAL);
        return player;
    }

    /** Останавливает storyboard, запущенный с указанным ключом. */
    public WidgetBase stopStoryboard(Object key) {
        if (key != null) animations.stop(key);
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animationRunning}, меняющую состояние виджета.
     */
    public boolean animationRunning(AnimatedProperty property) {
        return property != null && (animations.getScoped(SCOPE_PROPERTY, property, false) != null
                || animations.getScoped(SCOPE_SPRING_PROPERTY, property, false) != null);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code animationsRunning} для виджета.
     */
    public boolean animationsRunning() {
        return animations.hasActiveAnimations();
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
    @XmlAttribute(value = "visibility", category = "Behavior", defaultValue = "visible", description = "Visibility mode: visible, hidden or collapsed.")
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
    @XmlAttribute(value = "visible", category = "Behavior", defaultValue = "true", description = "Whether the widget is visible without collapsing layout space.")
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
    @XmlAttribute(value = "enabled", category = "Behavior", defaultValue = "true", description = "Whether the widget can receive user interaction.")
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
     * Returns the widget state used by declarative style lookup.
     */
    protected WidgetState styleState() {
        if (!enabled()) return WidgetState.DISABLED;
        return hovered() ? WidgetState.HOVERED : WidgetState.NORMAL;
    }
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
        T override = styleRendererOverride(rendererType);
        return override == null ? fallback : override;
    }

    /**
     * Resolves only an explicit Java renderer override from style data.
     *
     * <p>This keeps the new declarative path separate from the old default
     * renderer fallback: widgets can try instance renderer, style renderer,
     * StylePack RenderPlan and only then fall back to {@code WidgetsRender}.</p>
     */
    protected <T> T styleRendererOverride(Class<T> rendererType) {
        if (rendererType == null) return null;
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        String type = styleType();
        Style themeStyle = theme instanceof StylePack stylePack
                ? stylePack.resolveStyleFor(type, styleId(), styleClasses())
                : theme.styleFor(type);
        Object value = themeStyle.get(StyleKeys.RENDERER, null, null);
        if ((value == null || value.equals("")) && theme instanceof StylePack stylePack) {
            value = stylePack.rendererIdFor(type, styleId(), styleClasses());
        }
        for (Widget current : styleLookupChain()) {
            Style localStyle = current.localStyle(type);
            value = localStyle.get(StyleKeys.RENDERER, null, value);
        }
        return WidgetRendererRegistry.global().resolve(rendererType, value, null);
    }

    /**
     * Tries to render the current widget through the active StylePack RenderPlan.
     *
     * @return {@code true}, if a non-empty declarative plan was rendered
     */
    protected <S> boolean renderStylePlan(RenderContext context, Class<S> stateType, S state) {
        if (!stylePlansEnabled() || context == null || stateType == null || state == null) return false;
        UIContext ui = uiContext();
        Theme theme = ui == null ? Theme.EMPTY : ui.theme();
        if (!(theme instanceof StylePack stylePack)) return false;
        RenderPlan plan = stylePack.renderPlanFor(styleType(), styleId(), styleClasses(), stateType, state, styleState())
                .orElse(RenderPlan.EMPTY);
        if (plan.empty()) return false;
        plan.render(new DrawScope(context, transform(), layoutBounds()));
        return true;
    }

    /**
     * Allows widgets with a local theme toggle to suppress StylePack RenderPlans.
     */
    protected boolean stylePlansEnabled() {
        return true;
    }

    private List<Widget> styleLookupChain() {
        List<Widget> chain = new ObjectArrayList<>();
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
        renderCacheDirty = true;
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
        renderCacheDirty = true;
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
        if (!layoutBoundsObserved) {
            previousLayoutX = layoutBounds.x();
            previousLayoutY = layoutBounds.y();
            layoutBoundsObserved = true;
        }
        applyTransformOrigin();
    }

    /**
     * Отрисовывает виджет в текущем render-контексте.
     */
    @Override
    public void render(RenderContext context) {
    }

    /**
     * Включает или отключает retained-кэш draw-команд виджета.
     *
     * <p>На первом этапе кэш применяется только к виджетам без дочерних элементов.
     * Это не дублирует полное поддерево в каждом родителе и сохраняет независимую
     * инвалидацию соседних элементов. Контейнеры продолжат обычный обход детей.</p>
     *
     * @param enabled {@code true}, чтобы переиспользовать стабильный render-фрагмент
     * @return этот виджет
     */
    public WidgetBase renderCachingEnabled(boolean enabled) {
        if (renderCachingEnabled == enabled) return this;
        renderCachingEnabled = enabled;
        renderCacheDirty = true;
        if (!enabled) releaseRenderCache();
        return this;
    }

    /** @return {@code true}, если для виджета разрешён retained-кэш */
    public boolean renderCachingEnabled() {
        return renderCachingEnabled;
    }

    /**
     * Помечает retained-фрагмент устаревшим без изменения публичных invalidation flags.
     *
     * @return этот виджет
     */
    public WidgetBase invalidateRenderCache() {
        renderCacheDirty = true;
        return this;
    }

    /** @return количество команд в последнем собранном фрагменте или {@code 0} */
    public int renderCacheCommandCount() {
        return renderCache == null ? 0 : renderCache.size();
    }

    /** @return число воспроизведений фрагмента без вызова {@link #render(RenderContext)} */
    public long renderCacheHits() {
        return renderCacheHits;
    }

    /** @return число полных пересборок retained-фрагмента */
    public long renderCacheRebuilds() {
        return renderCacheRebuilds;
    }

    /** Сбрасывает диагностические счётчики retained-кэша. */
    public void resetRenderCacheStats() {
        renderCacheHits = 0L;
        renderCacheRebuilds = 0L;
    }

    /**
     * Рендерит виджет через retained-фрагмент.
     *
     * <p>Команды пересобираются после любой invalidation самого виджета. При cache hit
     * метод {@link #render(RenderContext)} не вызывается, а сохранённые команды
     * воспроизводятся через кадровый пул целевого {@link DrawList}.</p>
     */
    @Override
    public void renderCached(RenderContext context) {
        if (context == null) return;
        if (!renderCachingEnabled || renderingCache) {
            render(context);
            return;
        }
        if (!children().isEmpty()) {
            if (renderCache != null) releaseRenderCache();
            render(context);
            return;
        }

        RenderBackend backend = context.backend();
        long styleVersion = currentRenderStyleVersion();
        boolean rebuild = renderCacheDirty
                || renderCache == null
                || renderCacheBackend != backend
                || renderCacheStyleVersion != styleVersion;
        if (rebuild) {
            if (renderCache == null) {
                renderCache = new DrawList();
                renderCacheContext = new DefaultRenderContext(renderCache);
            }
            renderCache.clear();
            renderCacheContext.backend(context.backend());
            renderingCache = true;
            try {
                render(renderCacheContext);
            } finally {
                renderingCache = false;
            }
            renderCacheDirty = false;
            renderCacheBackend = backend;
            renderCacheStyleVersion = styleVersion;
            renderCacheRebuilds++;
        } else {
            renderCacheHits++;
        }

        Object[] rawCommands = renderCache.commandElements();
        for (int i = 0, size = renderCache.size(); i < size; i++) {
            context.replayCached((DrawCommand) rawCommands[i]);
        }
    }

    /**
     * Обновляет состояние виджета на каждом кадре.
     */
    @Override
    public void tick(FrameContext frame) {
        tickAnimations(frame);
    }

    /** Освобождает retained-команды перед удалением виджета из UI-дерева. */
    @Override
    public void dispose() {
        restoreTransformEffectOffset();
        animations.clear();
        releaseRenderCache();
    }

    private void releaseRenderCache() {
        if (renderCache != null) {
            renderCache.clear();
            renderCache = null;
        }
        renderCacheContext = null;
        renderCacheBackend = null;
        renderCacheStyleVersion = Long.MIN_VALUE;
        renderCacheDirty = true;
    }

    private long currentRenderStyleVersion() {
        long version = uiContext == null ? 0L : uiContext.styleVersion();
        String type = styleType();
        Widget current = this;
        while (current != null) {
            version = version * 31L + current.localStyle(type).version();
            if (current != this && current.styleScope()) break;
            current = current.parent();
        }
        return version;
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
        if (!animations.hasActiveAnimations()) return;

        float deltaSeconds = frame == null ? 1.0f / 60.0f : AnimationClock.sanitizeDelta(frame.deltaSeconds());
        if (deltaSeconds <= 0.0f) deltaSeconds = 1.0f / 60.0f;
        restoreTransformEffectOffset();

        animations.update(deltaSeconds);
        applyShakeAnimations();
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
     * Renders a child while inheriting this widget's render transform.
     */
    protected final void renderChildWithInheritedTransform(RenderContext context, Widget child) {
        if (context == null || child == null || child.visibility() != Visibility.VISIBLE) return;
        context.pushTransform(layoutBounds(), transform());
        try {
            child.renderCached(context);
        } finally {
            context.popTransform();
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

    /**
     * Обрабатывает изменение bounds после layout-прохода.
     *
     * <p>Это реализация FLIP: сначала запоминается First-позиция, затем при
     * появлении Last-позиции создаётся обратное визуальное смещение. Сам bounds
     * при этом остаётся новым, поэтому hit-test и последующая компоновка видят
     * актуальную геометрию.</p>
     */
    private void layoutBoundsChanged() {
        invalidate(InvalidationFlags.LAYOUT);

        float newX = layoutBounds.x();
        float newY = layoutBounds.y();
        if (!layoutBoundsObserved) {
            previousLayoutX = newX;
            previousLayoutY = newY;
            layoutBoundsObserved = true;
            return;
        }

        float deltaX = previousLayoutX - newX;
        float deltaY = previousLayoutY - newY;
        previousLayoutX = newX;
        previousLayoutY = newY;
        if (!layoutTransitionsEnabled || (deltaX == 0.0f && deltaY == 0.0f)) return;

        LayoutTransitionAnimation current = layoutTransitionAnimation();
        if (current != null && !current.isFinished()) {
            deltaX += current.offsetX();
            deltaY += current.offsetY();
        }

        LayoutTransitionAnimation transition = new LayoutTransitionAnimation(
                deltaX,
                deltaY,
                layoutTransitionSpec.durationSeconds(),
                layoutTransitionSpec.easing());
        if (transition.isFinished()) {
            animations.stop(LAYOUT_TRANSITION_KEY);
            return;
        }
        animations.play(LAYOUT_TRANSITION_KEY, transition);
        invalidate(InvalidationFlags.VISUAL);
    }

    private void restoreTransformEffectOffset() {
        if (appliedEffectOffsetX == 0.0f && appliedEffectOffsetY == 0.0f) return;
        transform.position().set(
                transform.position().x() - appliedEffectOffsetX,
                transform.position().y() - appliedEffectOffsetY);
        appliedEffectOffsetX = 0.0f;
        appliedEffectOffsetY = 0.0f;
    }

    private void applyTransformEffectOffsets() {
        float offsetX = 0.0f;
        float offsetY = 0.0f;
        if (animations.get(LAYOUT_TRANSITION_KEY) instanceof LayoutTransitionAnimation layoutAnimation
                && !layoutAnimation.isFinished()) {
            offsetX += layoutAnimation.offsetX();
            offsetY += layoutAnimation.offsetY();
        }

        ObjectIterator<dev.sixik.unigui.api.animation.PlayableAnimation> iterator = animations.values().iterator();
        while (iterator.hasNext()) {
            if (!(iterator.next() instanceof ShakeAnimation effect) || effect.isFinished()) continue;
            offsetX += effect.offsetX();
            offsetY += effect.offsetY();
        }

        if (offsetX != 0.0f || offsetY != 0.0f) {
            transform.position().set(transform.position().x() + offsetX, transform.position().y() + offsetY);
            appliedEffectOffsetX = offsetX;
            appliedEffectOffsetY = offsetY;
        }
    }

    private void applyShakeAnimations() {
        applyTransformEffectOffsets();
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

    private static final class FloatAnimation implements dev.sixik.unigui.api.animation.PlayableAnimation {
        private final FloatTransition transition;
        private FloatValueWriter writer;

        private FloatAnimation(FloatTransition transition, FloatValueWriter writer) {
            this.transition = transition;
            this.writer = writer;
            writer.set(transition.value());
        }

        @Override
        public void update(float deltaSeconds) {
            writer.set(transition.tick(deltaSeconds));
            if (transition.finished()) writer.set(transition.finalValue());
        }

        @Override
        public boolean isFinished() { return transition.finished(); }

        @Override
        public void cancel() { transition.cancel(); }

        private void retarget(float target, FloatValueWriter newWriter) {
            writer = newWriter;
            transition.retarget(target);
        }
    }

    private static final class ColorAnimation implements dev.sixik.unigui.api.animation.PlayableAnimation {
        private final ColorTransition transition;
        private final MutableColor output;
        private final float targetR;
        private final float targetG;
        private final float targetB;
        private final float targetA;
        private final TransitionSpec spec;

        private ColorAnimation(ColorTransition transition,
                               MutableColor output,
                               ColorView target,
                               TransitionSpec spec) {
            this.transition = transition;
            this.output = output;
            this.targetR = target.r();
            this.targetG = target.g();
            this.targetB = target.b();
            this.targetA = target.a();
            this.spec = spec;
        }

        private boolean matches(ColorView requestedTarget, TransitionSpec requestedSpec) {
            return spec.equals(requestedSpec)
                    && targetR == requestedTarget.r()
                    && targetG == requestedTarget.g()
                    && targetB == requestedTarget.b()
                    && targetA == requestedTarget.a();
        }

        @Override
        public void update(float deltaSeconds) {
            transition.tick(deltaSeconds, output);
            if (transition.finished()) transition.finish(output);
        }

        @Override
        public boolean isFinished() { return transition.finished(); }

        @Override
        public void cancel() { }
    }

    private static LinkedHashSet<String> parseStyleClasses(String value) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (value == null || value.isBlank()) return result;
        for (String token : value.split("[\\s,]+")) {
            String normalized = normalizeStyleClass(token);
            if (!normalized.isEmpty()) result.add(normalized);
        }
        return result;
    }

    private static String normalizeStyleClass(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
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
