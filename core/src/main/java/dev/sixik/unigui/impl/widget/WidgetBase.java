package dev.sixik.unigui.impl.widget;

import dev.sixik.unigui.api.animation.AnimatedProperty;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
import dev.sixik.unigui.api.layout.LayoutStyleLegacyAdapter;
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
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.StyleTextParseResult;
import dev.sixik.unigui.api.style.StyleTextParser;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.render.WidgetRenderRegistry;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlLayoutAttributes;
import dev.sixik.unigui.api.xml.XmlStyleAttributes;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.impl.event.FastEventEmitter;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.utils.reactive.BindingData;
import dev.sixik.unigui.api.utils.reactive.SignalSubscription;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@XmlLayoutAttributes
@XmlStyleAttributes
@AiDoc(
        summary = "Base implementation for UniGUI widgets and the bridge between widget state, layout, styles, rendering, input, and animation.",
        purpose = "Provides common fluent configuration and lifecycle behavior so concrete widgets can focus on their own content, measurement, and visual rendering.",
        useWhen = {
                "Implement a custom widget that should participate in the standard UniGUI layout, style, event, and animation pipelines.",
                "Configure an existing widget through common identity, visibility, focus, style, transform, or caching APIs."
        },
        criticalRules = {
                "Subclass `WidgetBase`; do not instantiate it directly because it is abstract and owns framework lifecycle state.",
                "Use `layout(style -> ...)` for layout properties and `style(...)` for visual/style properties; they update different systems.",
                "Changing layout-related state must invalidate `LAYOUT`; visual-only changes should invalidate `VISUAL`.",
                "`Visibility.HIDDEN` keeps layout space but suppresses rendering and input; `Visibility.COLLAPSED` removes the widget from layout as well.",
                "A per-instance `renderer` replaces the widget's standard visual; children are still handled by the framework.",
                "Retained render caching is intended for stable leaf visuals; widgets with children fall back to normal child traversal.",
                "`styleScope(true)` limits inherited local-style lookup at that widget boundary; it does not remove the global theme.",
                "Call `super` lifecycle behavior when overriding framework methods unless the concrete widget deliberately replaces that contract."
        },
        related = {"Widget", "LayoutStyle", "StylePack", "MutableStyle", "TransitionSpec", "EventSubscription"},
        examples = {
                @AiDoc.Example(
                        title = "Configure a widget",
                        order = 10,
                        code = """
                                WidgetBase widget = new Label("Hello")
                                        .id("greeting")
                                        .styleId("card")
                                        .addStyleClass("interactive")
                                        .layout(style -> style
                                                .padding(12.0f)
                                                .width(240.0f)
                                                .flexNone());
                                """),
                @AiDoc.Example(
                        title = "Animate a visual property",
                        order = 20,
                        code = """
                                widget
                                        .opacity(0.0f)
                                        .animateOpacity(1.0f, 0.20f)
                                        .animateScale(1.0f, 1.0f, 0.20f);
                                """)
        })
@WikiPage(
        title = "<key:widget.base.title>",
        slug = "widget-base",
        category = "<key:widget.base.category>",
        summary = "<key:widget.base.summary>",
        order = 30)
@WikiSection(
        title = "<key:widget.base.section.overview.title>",
        markdown = "<key:widget.base.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:widget.base.section.style.title>",
        markdown = "<key:widget.base.section.style.body>",
        order = 20)
@WikiSection(
        title = "<key:widget.base.section.state.title>",
        markdown = "<key:widget.base.section.state.body>",
        order = 30)
@WikiSection(
        title = "<key:widget.base.section.animation.title>",
        markdown = "<key:widget.base.section.animation.body>",
        order = 40)
@WikiSection(
        title = "<key:widget.base.section.render.title>",
        markdown = "<key:widget.base.section.render.body>",
        order = 50)
@WikiExample(
        title = "<key:widget.base.example.configure.title>",
        code = "<key:widget.base.example.configure.code>",
        order = 10)
@WikiExample(
        title = "<key:widget.base.example.animation.title>",
        code = "<key:widget.base.example.animation.code>",
        order = 20)
public abstract class WidgetBase implements Widget {
    /**
     * Общие animation property id, доступные всем виджетам.
     */
    public static final class AnimationProperties {
        public static final String POSITION_X = StyleAnimationIds.Property.POSITION_X;
        public static final String POSITION_Y = StyleAnimationIds.Property.POSITION_Y;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String SCALE_X = StyleAnimationIds.Property.SCALE_X;
        public static final String SCALE_Y = StyleAnimationIds.Property.SCALE_Y;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final List<String> ALL = StyleAnimationIds.Property.COMMON_WIDGET;

        private AnimationProperties() {
        }
    }

    /**
     * Общие event id для hover/focus/press анимаций.
     */
    public static final class AnimationEvents {
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_HOVER_ENTER = StyleAnimationIds.Event.ON_HOVER_ENTER;
        public static final String ON_HOVER_EXIT = StyleAnimationIds.Event.ON_HOVER_EXIT;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final List<String> ALL = StyleAnimationIds.Event.COMMON_WIDGET;

        private AnimationEvents() {
        }
    }

    /**
     * Хранит подписки и отправку событий, связанных с жизненным циклом и вводом виджета.
     */
    private final FastEventEmitter events = new FastEventEmitter();
    private final Map<BindingData<?>, SignalSubscription> bindings = new HashMap<>();
    /**
     * Единый реестр всех активных анимаций виджета.
     */
    private final AnimationController animations = new AnimationController();
    /**
     * Единственный custom renderer виджета. {@code null} означает стандартный визуал.
     * Custom renderer полностью заменяет стандартный визуал (фон плюс контент).
     */
    private WidgetRender renderer;
    /**
     * Последняя версия каскада стилей, для которой разрешался renderer из Style API.
     */
    private long cachedStyleRendererStyleVersion = Long.MIN_VALUE;
    /**
     * Последняя версия стандартного стиля компонента, учтённая renderer-кэшем.
     */
    private long cachedStyleRendererDefaultVersion = Long.MIN_VALUE;
    /**
     * Версия id/class-селектора, учтённая renderer-кэшем.
     */
    private long cachedStyleRendererSelectorVersion = Long.MIN_VALUE;
    /**
     * Версия реестра renderer'ов, учтённая renderer-кэшем.
     */
    private long cachedStyleRendererRegistryVersion = Long.MIN_VALUE;
    /**
     * Роль, для которой разрешался последний renderer из стилей.
     */
    private WidgetRole cachedStyleRendererRole;
    /**
     * UI-контекст последнего разрешения renderer'а.
     */
    private UIContext cachedStyleRendererContext;
    /**
     * Тема последнего разрешения renderer'а.
     */
    private Theme cachedStyleRendererTheme;
    /**
     * Объект стандартных стилей, учтённый renderer-кэшем.
     */
    private Style cachedStyleRendererDefaults;
    /**
     * Включался ли глобальный theme lookup при последнем разрешении renderer'а.
     */
    private boolean cachedStyleRendererIncludesTheme;
    /**
     * Показывает, что renderer из стилей разрешён хотя бы один раз, включая результат null.
     */
    private boolean styleRendererCacheValid;
    /**
     * Последний renderer из каскада стилей или null для стандартной отрисовки.
     */
    private WidgetRender cachedStyleRenderer;
    /**
     * Версия селекторов виджета; меняется при редактировании style id или классов.
     */
    private long styleSelectorVersion;

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
     * Показывает, что measure-кэш содержит актуальный desiredSize для ключа ниже.
     */
    private boolean measureCacheValid;
    /**
     * Ключ measure-кэша: availableWidth последнего измерения.
     */
    private float measureCacheWidth = Float.NaN;
    /**
     * Ключ measure-кэша: availableHeight последнего измерения.
     */
    private float measureCacheHeight = Float.NaN;
    /**
     * Счётчики используются тестами и профилированием конкретного виджета.
     */
    private long measureCacheHits;
    private long measureCacheMisses;
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
     * Ленивый retained-фрагмент draw-команд виджета без дочерних элементов.
     */
    private DrawList renderCache;
    /**
     * Контекст, записывающий draw-команды в retained-фрагмент.
     */
    private DefaultRenderContext renderCacheContext;
    /**
     * Показывает, что retained-фрагмент нужно пересобрать.
     */
    private boolean renderCacheDirty = true;
    /**
     * Позволяет отключить retained-кэш для заведомо динамического виджета.
     */
    private boolean renderCachingEnabled = true;
    /**
     * Защищает от рекурсивного входа во время записи фрагмента.
     */
    private boolean renderingCache;
    /**
     * Backend, для которого последний раз строился фрагмент.
     */
    private RenderBackend renderCacheBackend;
    /**
     * Агрегированная версия theme и локальных style scope последней сборки.
     */
    private long renderCacheStyleVersion = Long.MIN_VALUE;
    /**
     * Счётчики используются тестами и профилированием конкретного виджета.
     */
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
    private MutableStyle inlineStyle;
    private List<XmlWidgetDiagnostic> styleDiagnostics = List.of();

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
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 80,
            title = "<key:widget.base.method.ui_context.title>", summary = "<key:widget.base.method.ui_context.summary>")
    @WikiReturn("<key:widget.base.method.ui_context.return>")
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
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 70,
            title = "<key:widget.base.method.parent.title>", summary = "<key:widget.base.method.parent.summary>")
    @WikiReturn("<key:widget.base.method.parent.return>")
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
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 72,
            title = "<key:widget.base.method.children.title>", summary = "<key:widget.base.method.children.summary>")
    @WikiReturn("<key:widget.base.method.children.return>")
    public List<Widget> children() {
        return Collections.emptyList();
    }

    /**
     * Возвращает runtime/editor id виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 12,
            title = "<key:widget.base.method.get_id.title>", summary = "<key:widget.base.method.get_id.summary>")
    @WikiReturn("<key:widget.base.method.get_id.return>")
    public String id() {
        return id;
    }

    /**
     * Задаёт runtime/editor id виджета.
     */
    @Override
    @XmlAttribute(value = "id", category = "Common", description = "Runtime/debug/editor identifier for code-behind lookup.")
    @AiDoc(section = "Identity & Style Selectors", sectionOrder = 10, order = 10,
            summary = "Assigns the runtime/editor identifier used by code-behind and lookup systems.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 10,
            title = "<key:widget.base.method.id.title>", summary = "<key:widget.base.method.id.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase id(@WikiParameter(name = "id", description = "<key:widget.base.method.id.parameter>") String id) {
        String normalized = id == null ? "" : id;
        if (this.id.equals(normalized)) return this;
        this.id = normalized;
        return this;
    }

    /**
     * Возвращает явный id стиля виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 22,
            title = "<key:widget.base.method.get_style_id.title>", summary = "<key:widget.base.method.get_style_id.summary>")
    @WikiReturn("<key:widget.base.method.get_style_id.return>")
    public String styleId() {
        return styleId;
    }

    /**
     * Задаёт явный id стиля виджета через XML-атрибут {@code style}.
     */
    @Override
    @XmlAttribute(value = "style", category = "Common", description = "Explicit StylePack style id applied to this widget.")
    @AiDoc(section = "Identity & Style Selectors", sectionOrder = 10, order = 20,
            summary = "Selects the explicit `StylePack` style id for this widget.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 20,
            title = "<key:widget.base.method.style_id.title>", summary = "<key:widget.base.method.style_id.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase styleId(@WikiParameter(name = "styleId", description = "<key:widget.base.method.style_id.parameter>") String styleId) {
        String normalized = styleId == null ? "" : styleId.trim();
        if (this.styleId.equals(normalized)) return this;
        this.styleId = normalized;
        styleSelectorVersion++;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает style classes виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 48,
            title = "<key:widget.base.method.get_style_classes.title>", summary = "<key:widget.base.method.get_style_classes.summary>")
    @WikiReturn("<key:widget.base.method.get_style_classes.return>")
    public List<String> styleClasses() {
        return List.copyOf(styleClasses);
    }

    /**
     * Задаёт style classes через XML-атрибут {@code class}.
     */
    @Override
    @XmlAttribute(value = "class", category = "Common", description = "Space-separated StylePack classes applied to this widget.")
    @AiDoc(section = "Identity & Style Selectors", sectionOrder = 10, order = 30,
            summary = "Replaces the widget's space-separated style-class set.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 40,
            title = "<key:widget.base.method.style_class.title>", summary = "<key:widget.base.method.style_class.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase styleClass(@WikiParameter(name = "styleClasses", description = "<key:widget.base.method.style_class.parameter>") String styleClasses) {
        LinkedHashSet<String> parsed = parseStyleClasses(styleClasses);
        if (this.styleClasses.equals(parsed)) return this;
        this.styleClasses.clear();
        this.styleClasses.addAll(parsed);
        styleSelectorVersion++;
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
    @AiDoc(section = "Identity & Style Selectors", sectionOrder = 10, order = 40,
            summary = "Adds one normalized style class without removing existing classes.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 42,
            title = "<key:widget.base.method.add_style_class.title>", summary = "<key:widget.base.method.add_style_class.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase addStyleClass(@WikiParameter(name = "styleClass", description = "<key:widget.base.method.add_style_class.parameter>") String styleClass) {
        String normalized = normalizeStyleClass(styleClass);
        if (normalized.isEmpty() || !styleClasses.add(normalized)) return this;
        styleSelectorVersion++;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет один style class.
     */
    @Override
    @AiDoc(section = "Identity & Style Selectors", sectionOrder = 10, order = 50,
            summary = "Removes one normalized style class if it is present.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 44,
            title = "<key:widget.base.method.remove_style_class.title>", summary = "<key:widget.base.method.remove_style_class.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase removeStyleClass(@WikiParameter(name = "styleClass", description = "<key:widget.base.method.remove_style_class.parameter>") String styleClass) {
        String normalized = normalizeStyleClass(styleClass);
        if (normalized.isEmpty() || !styleClasses.remove(normalized)) return this;
        styleSelectorVersion++;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Проверяет наличие style class.
     */
    @Override
    @AiDoc(section = "Identity & Style Selectors", sectionOrder = 10, order = 60,
            summary = "Checks whether the widget has a normalized style class.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 46,
            title = "<key:widget.base.method.has_style_class.title>", summary = "<key:widget.base.method.has_style_class.summary>")
    @WikiReturn("<key:widget.base.method.has_style_class.return>")
    public boolean hasStyleClass(@WikiParameter(name = "styleClass", description = "<key:widget.base.method.has_style_class.parameter>") String styleClass) {
        String normalized = normalizeStyleClass(styleClass);
        return !normalized.isEmpty() && styleClasses.contains(normalized);
    }

    /**
     * Объединяет встроенные визуальные объявления с локальным wildcard-стилем виджета.
     *
     * <p>Строка без двоеточия остаётся удобным псевдонимом для {@link #styleId(String)}. Встроенные
     * объявления используют форму {@code property: value; property: value}.</p>
     */
    @Override
    @AiDoc(section = "Inline Styling & Layout", sectionOrder = 20, order = 10,
            summary = "Parses inline style declarations; a string without `:` or `;` is treated as a style id.",
            criticalRules = "Parsing diagnostics are retained in `styleDiagnostics()` and do not silently disappear.")
    @WikiMethod(
            section = "<key:widget.base.api.section.identity>",
            sectionOrder = 10,
            order = 30,
            title = "<key:widget.base.method.style.title>",
            summary = "<key:widget.base.method.style.summary>",
            markdown = "<key:widget.base.method.style.markdown>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase style(@WikiParameter(name = "declarations", description = "<key:widget.base.method.style.parameter>") String declarations) {
        String normalized = declarations == null ? "" : declarations.trim();
        if (normalized.isEmpty()) return this;
        if (normalized.indexOf(':') < 0 && normalized.indexOf(';') < 0) {
            return styleId(normalized);
        }
        StyleTextParseResult result = StyleTextParser.parseEditor(normalized);
        inlineStyle().putAll(result.style());
        if (!result.diagnostics().isEmpty()) {
            java.util.ArrayList<XmlWidgetDiagnostic> next = new java.util.ArrayList<>(styleDiagnostics);
            next.addAll(result.diagnostics());
            styleDiagnostics = List.copyOf(next);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Добавляет типизированные значения в тот же набор встроенных стилей.
     */
    @Override
    @AiDoc(section = "Inline Styling & Layout", sectionOrder = 20, order = 20,
            summary = "Applies typed inline style values through a `MutableStyle` callback.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 32,
            title = "<key:widget.base.method.style_consumer.title>", summary = "<key:widget.base.method.style_consumer.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase style(@WikiParameter(name = "update", description = "<key:widget.base.method.style_consumer.parameter>") Consumer<MutableStyle> update) {
        if (update == null) return this;
        update.accept(inlineStyle());
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет inline-style этого виджета, не меняя style scopes и styles, назначенные извне.
     *
     * @return этот виджет
     */
    @Override
    @AiDoc(section = "Inline Styling & Layout", sectionOrder = 20, order = 30,
            summary = "Clears only the widget's inline style and parser diagnostics; external style scopes remain active.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 35,
            title = "<key:widget.base.method.style_clear.title>", summary = "<key:widget.base.method.style_clear.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase styleClear() {
        if (inlineStyle == null && styleDiagnostics.isEmpty()) return this;
        if (inlineStyle != null && localStyles.get(Theme.WILDCARD) == inlineStyle) {
            localStyles.remove(Theme.WILDCARD);
        }
        inlineStyle = null;
        styleDiagnostics = List.of();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 38,
            title = "<key:widget.base.method.style_diagnostics.title>", summary = "<key:widget.base.method.style_diagnostics.summary>")
    @WikiReturn("<key:widget.base.method.style_diagnostics.return>")
    public List<XmlWidgetDiagnostic> styleDiagnostics() {
        return styleDiagnostics;
    }

    private MutableStyle inlineStyle() {
        if (inlineStyle == null) {
            inlineStyle = new MutableStyle();
            localStyles.put(Theme.WILDCARD, inlineStyle);
        }
        return inlineStyle;
    }

    /**
     * Возвращает текущие границы виджета после layout-прохода.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 20,
            title = "<key:widget.base.method.layout_bounds.title>", summary = "<key:widget.base.method.layout_bounds.summary>")
    @WikiReturn("<key:widget.base.method.layout_bounds.return>")
    public RectView layoutBounds() {
        return layoutBounds;
    }

    /**
     * Возвращает, включены ли FLIP-переходы перемещения после изменения layout.
     *
     * <p>Переходы выключены по умолчанию. Размер виджета не анимируется: FLIP
     * применяется только когда меняются координаты {@code x} или {@code y}.</p>
     */
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 46,
            title = "<key:widget.base.method.get_layout_transitions_enabled.title>", summary = "<key:widget.base.method.get_layout_transitions_enabled.summary>")
    @WikiReturn("<key:widget.base.method.get_layout_transitions_enabled.return>")
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
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 45,
            title = "<key:widget.base.method.layout_transitions_enabled.title>", summary = "<key:widget.base.method.layout_transitions_enabled.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase layoutTransitionsEnabled(@WikiParameter(name = "enabled", description = "<key:widget.base.method.layout_transitions_enabled.parameter>") boolean enabled) {
        if (layoutTransitionsEnabled == enabled) return this;
        layoutTransitionsEnabled = enabled;
        if (!enabled) {
            restoreTransformEffectOffset();
            animations.stop(LAYOUT_TRANSITION_KEY);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /**
     * @return параметры следующего FLIP-перехода
     */
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
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 40,
            title = "<key:widget.base.method.layout_transition.title>", summary = "<key:widget.base.method.layout_transition.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase layoutTransition(@WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.layout_transition.duration>") float durationSeconds) {
        return layoutTransition(durationSeconds, AnimationEasing.EASE_OUT);
    }

    /**
     * Включает FLIP-переход с указанными параметрами.
     *
     * @param durationSeconds длительность перехода в секундах
     * @param easing          функция плавности; {@code null} означает линейный easing
     * @return этот виджет
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 10,
            summary = "Enables FLIP animation for subsequent layout-driven position changes.",
            criticalRules = "Only x/y movement is animated; layout size changes are not interpolated.")
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 42,
            title = "<key:widget.base.method.layout_transition_easing.title>", summary = "<key:widget.base.method.layout_transition_easing.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase layoutTransition(@WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.layout_transition.duration>") float durationSeconds, @WikiParameter(name = "easing", description = "<key:widget.base.method.layout_transition.easing>") Easing easing) {
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
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 30,
            title = "<key:widget.base.method.desired_size.title>", summary = "<key:widget.base.method.desired_size.summary>")
    @WikiReturn("<key:widget.base.method.desired_size.return>")
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
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 29,
            title = "<key:widget.base.method.get_transform.title>", summary = "<key:widget.base.method.get_transform.summary>")
    @WikiReturn("<key:widget.base.method.get_transform.return>")
    public Transform transform() {
        return transform;
    }

    public TransformOrigin transformOrigin() {
        return transformOrigin;
    }

    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 20,
            summary = "Selects a named transform pivot; `CUSTOM` preserves the raw pixel pivot.")
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 25,
            title = "<key:widget.base.method.transform_origin.title>", summary = "<key:widget.base.method.transform_origin.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase transformOrigin(@WikiParameter(name = "origin", description = "<key:widget.base.method.transform_origin.parameter>") TransformOrigin origin) {
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

    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 30,
            summary = "Sets a custom transform pivot in local pixels and cancels active pivot animations.")
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 28,
            title = "<key:widget.base.method.transform_pivot.title>", summary = "<key:widget.base.method.transform_pivot.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase transformPivot(@WikiParameter(name = "x", description = "<key:widget.base.method.transform_pivot.x>") float x, @WikiParameter(name = "y", description = "<key:widget.base.method.transform_pivot.y>") float y) {
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
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 40,
            summary = "Sets rotation in degrees immediately and stops active rotation animations.")
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 20,
            title = "<key:widget.base.method.rotation_degrees.title>", summary = "<key:widget.base.method.rotation_degrees.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase rotationDegrees(@WikiParameter(name = "degrees", description = "<key:widget.base.method.rotation_degrees.parameter>") float degrees) {
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
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 12,
            title = "<key:widget.base.method.get_opacity.title>", summary = "<key:widget.base.method.get_opacity.summary>")
    @WikiReturn("<key:widget.base.method.get_opacity.return>")
    public float opacity() {
        return opacity;
    }

    /**
     * Возвращает текущую прозрачность виджета.
     */
    @XmlAttribute(value = "opacity", category = "Appearance", defaultValue = "1", description = "Widget opacity clamped between 0 and 1.")
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 50,
            summary = "Sets opacity immediately after clamping it to the 0..1 range.")
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 10,
            title = "<key:widget.base.method.opacity.title>", summary = "<key:widget.base.method.opacity.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase opacity(@WikiParameter(name = "opacity", description = "<key:widget.base.method.opacity.parameter>") float opacity) {
        animations.stopScoped(SCOPE_PROPERTY, AnimatedProperty.OPACITY, false);
        animations.stopScoped(SCOPE_SPRING_PROPERTY, AnimatedProperty.OPACITY, false);
        setAnimatedValue(AnimatedProperty.OPACITY, clamp01(opacity));
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animateOpacity}, меняющую состояние виджета.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 40,
            title = "<key:widget.base.method.animate_opacity.title>", summary = "<key:widget.base.method.animate_opacity.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animateOpacity(@WikiParameter(name = "targetOpacity", description = "<key:widget.base.method.animate_opacity.target>") float targetOpacity, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
        return animateOpacity(targetOpacity, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animateOpacity}, меняющую состояние виджета.
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 60,
            summary = "Transitions opacity to a target value using the supplied timing specification.")
    public WidgetBase animateOpacity(float targetOpacity, TransitionSpec spec) {
        return animate(AnimatedProperty.OPACITY, clamp01(targetOpacity), spec);
    }

    /**
     * Обновляет или выполняет операцию {@code animatePosition}, меняющую состояние виджета.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 50,
            title = "<key:widget.base.method.animate_position.title>", summary = "<key:widget.base.method.animate_position.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animatePosition(@WikiParameter(name = "x", description = "<key:widget.base.method.animate_position.x>") float x, @WikiParameter(name = "y", description = "<key:widget.base.method.animate_position.y>") float y, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
        return animatePosition(x, y, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animatePosition}, меняющую состояние виджета.
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 70,
            summary = "Animates visual transform position without changing layout bounds.")
    public WidgetBase animatePosition(float x, float y, TransitionSpec spec) {
        animate(AnimatedProperty.POSITION_X, sanitizeFinite(x), spec);
        animate(AnimatedProperty.POSITION_Y, sanitizeFinite(y), spec);
        return this;
    }

    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 55,
            title = "<key:widget.base.method.animate_position_from.title>", summary = "<key:widget.base.method.animate_position_from.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animatePositionFrom(@WikiParameter(name = "startX", description = "<key:widget.base.method.animate_position_from.start_x>") float startX, @WikiParameter(name = "startY", description = "<key:widget.base.method.animate_position_from.start_y>") float startY, @WikiParameter(name = "endX", description = "<key:widget.base.method.animate_position_from.end_x>") float endX, @WikiParameter(name = "endY", description = "<key:widget.base.method.animate_position_from.end_y>") float endY, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
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
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 60,
            title = "<key:widget.base.method.animate_scale.title>", summary = "<key:widget.base.method.animate_scale.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animateScale(@WikiParameter(name = "x", description = "<key:widget.base.method.animate_scale.x>") float x, @WikiParameter(name = "y", description = "<key:widget.base.method.animate_scale.y>") float y, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
        return animateScale(x, y, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animateScale}, меняющую состояние виджета.
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 80,
            summary = "Animates transform scale on the x and y axes.")
    public WidgetBase animateScale(float x, float y, TransitionSpec spec) {
        animate(AnimatedProperty.SCALE_X, sanitizeFinite(x, 1.0f), spec);
        animate(AnimatedProperty.SCALE_Y, sanitizeFinite(y, 1.0f), spec);
        return this;
    }

    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 70,
            title = "<key:widget.base.method.shake.title>", summary = "<key:widget.base.method.shake.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase shake(@WikiParameter(name = "amplitude", description = "<key:widget.base.method.shake.amplitude>") float amplitude, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
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

    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 75,
            title = "<key:widget.base.method.stop_shake.title>", summary = "<key:widget.base.method.stop_shake.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
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
     * @param key       ключ параметра; повторный вызов заменяет предыдущую пружину
     * @param reader    источник текущего значения
     * @param writer    получатель результата
     * @param target    целевое значение
     * @param stiffness жёсткость пружины
     * @param damping   затухание пружины
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

    /**
     * Останавливает spring-анимацию кастомного параметра.
     */
    public WidgetBase stopSpringParameterAnimation(Object key) {
        animations.stopScoped(SCOPE_SPRING_PARAMETER, key, false);
        return this;
    }

    /**
     * Запускает пружинную анимацию встроенного свойства виджета.
     *
     * @param property    анимируемое свойство
     * @param targetValue целевое значение
     * @param stiffness   жёсткость пружины
     * @param damping     затухание пружины
     * @return этот виджет
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 90,
            summary = "Runs or retargets a spring animation for a built-in animated property.")
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 80,
            title = "<key:widget.base.method.animate_spring.title>", summary = "<key:widget.base.method.animate_spring.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animateSpring(@WikiParameter(name = "property", description = "<key:widget.base.method.animate.property>") AnimatedProperty property, @WikiParameter(name = "targetValue", description = "<key:widget.base.method.animate.target>") float targetValue, @WikiParameter(name = "stiffness", description = "<key:widget.base.method.animate_spring.stiffness>") float stiffness, @WikiParameter(name = "damping", description = "<key:widget.base.method.animate_spring.damping>") float damping) {
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

    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 85,
            title = "<key:widget.base.method.animate_color.title>", summary = "<key:widget.base.method.animate_color.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animateColor(@WikiParameter(name = "color", description = "<key:widget.base.method.animate_color.color>") MutableColor color, @WikiParameter(name = "targetColor", description = "<key:widget.base.method.animate_color.target>") ColorView targetColor, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
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

    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 88,
            title = "<key:widget.base.method.stop_color_animations.title>", summary = "<key:widget.base.method.stop_color_animations.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase stopColorAnimations() {
        animations.stopScope(SCOPE_COLOR);
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animate}, меняющую состояние виджета.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 30,
            title = "<key:widget.base.method.animate_duration.title>", summary = "<key:widget.base.method.animate_duration.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animate(@WikiParameter(name = "property", description = "<key:widget.base.method.animate.property>") AnimatedProperty property, @WikiParameter(name = "targetValue", description = "<key:widget.base.method.animate.target>") float targetValue, @WikiParameter(name = "durationSeconds", description = "<key:widget.base.method.animate.duration>") float durationSeconds) {
        return animate(property, targetValue, TransitionSpec.of(durationSeconds));
    }

    /**
     * Обновляет или выполняет операцию {@code animate}, меняющую состояние виджета.
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 100,
            summary = "Runs or retargets a transition for a built-in animated property.",
            criticalRules = "Rotation uses shortest-path angle interpolation; non-positive duration applies the value immediately.")
    @WikiMethod(
            section = "<key:widget.base.api.section.animation>",
            sectionOrder = 40,
            order = 32,
            title = "<key:widget.base.method.animate.title>",
            summary = "<key:widget.base.method.animate.summary>",
            markdown = "<key:widget.base.method.animate.markdown>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase animate(@WikiParameter(name = "property", description = "<key:widget.base.method.animate.property>") AnimatedProperty property, @WikiParameter(name = "targetValue", description = "<key:widget.base.method.animate.target>") float targetValue, @WikiParameter(name = "spec", description = "<key:widget.base.method.animate.spec>") TransitionSpec spec) {
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
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 100,
            title = "<key:widget.base.method.stop_animation.title>", summary = "<key:widget.base.method.stop_animation.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase stopAnimation(@WikiParameter(name = "property", description = "<key:widget.base.method.animate.property>") AnimatedProperty property) {
        if (property != null) {
            animations.stopScoped(SCOPE_PROPERTY, property, false);
            animations.stopScoped(SCOPE_SPRING_PROPERTY, property, false);
        }
        return this;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code stopAnimations} для виджета.
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 110,
            summary = "Stops every animation owned by the widget and removes temporary effect offsets.")
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 102,
            title = "<key:widget.base.method.stop_animations.title>", summary = "<key:widget.base.method.stop_animations.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
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
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 90,
            title = "<key:widget.base.method.play_storyboard.title>", summary = "<key:widget.base.method.play_storyboard.summary>")
    @WikiReturn("<key:widget.base.method.play_storyboard.return>")
    public StoryboardPlayer playStoryboard(@WikiParameter(name = "storyboard", description = "<key:widget.base.method.play_storyboard.storyboard>") Storyboard storyboard) {
        return playStoryboard(null, storyboard, null);
    }

    /**
     * Запускает storyboard под стабильным ключом, заменяя предыдущую анимацию с тем же ключом.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 92,
            title = "<key:widget.base.method.play_storyboard_key.title>", summary = "<key:widget.base.method.play_storyboard_key.summary>")
    @WikiReturn("<key:widget.base.method.play_storyboard.return>")
    public StoryboardPlayer playStoryboard(@WikiParameter(name = "key", description = "<key:widget.base.method.play_storyboard_key.key>") Object key, @WikiParameter(name = "storyboard", description = "<key:widget.base.method.play_storyboard.storyboard>") Storyboard storyboard) {
        return playStoryboard(key, storyboard, null);
    }

    /**
     * Запускает storyboard с пользовательским resolver'ом property path.
     *
     * @param key        ключ в animation controller; {@code null} использует сам player
     * @param storyboard описание анимации
     * @param resolver   resolver встроенных и пользовательских свойств; {@code null} использует built-ins
     * @return запущенный player
     */
    @AiDoc(section = "Transforms & Animation", sectionOrder = 40, order = 120,
            summary = "Starts a storyboard against the widget tree; a stable key replaces a previous storyboard with the same key.",
            criticalRules = "Call from the UI thread after constructing the target widget tree.")
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

    /**
     * Останавливает storyboard, запущенный с указанным ключом.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 95,
            title = "<key:widget.base.method.stop_storyboard.title>", summary = "<key:widget.base.method.stop_storyboard.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase stopStoryboard(@WikiParameter(name = "key", description = "<key:widget.base.method.play_storyboard_key.key>") Object key) {
        if (key != null) animations.stop(key);
        return this;
    }

    /**
     * Обновляет или выполняет операцию {@code animationRunning}, меняющую состояние виджета.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 105,
            title = "<key:widget.base.method.animation_running.title>", summary = "<key:widget.base.method.animation_running.summary>")
    @WikiReturn("<key:widget.base.method.animation_running.return>")
    public boolean animationRunning(@WikiParameter(name = "property", description = "<key:widget.base.method.animate.property>") AnimatedProperty property) {
        return property != null && (animations.getScoped(SCOPE_PROPERTY, property, false) != null
                || animations.getScoped(SCOPE_SPRING_PROPERTY, property, false) != null);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code animationsRunning} для виджета.
     */
    @WikiMethod(section = "<key:widget.base.api.section.animation>", sectionOrder = 40, order = 106,
            title = "<key:widget.base.method.animations_running.title>", summary = "<key:widget.base.method.animations_running.summary>")
    @WikiReturn("<key:widget.base.method.animations_running.return>")
    public boolean animationsRunning() {
        return animations.hasActiveAnimations();
    }

    /**
     * Возвращает флаги инвалидации самого виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 40,
            title = "<key:widget.base.method.invalidation_flags.title>", summary = "<key:widget.base.method.invalidation_flags.summary>")
    @WikiReturn("<key:widget.base.method.invalidation_flags.return>")
    public int invalidationFlags() {
        return invalidationFlags;
    }

    /**
     * Возвращает агрегированные флаги инвалидации виджета и всех его потомков.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 42,
            title = "<key:widget.base.method.subtree_invalidation_flags.title>", summary = "<key:widget.base.method.subtree_invalidation_flags.summary>")
    @WikiReturn("<key:widget.base.method.subtree_invalidation_flags.return>")
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
    @AiDoc(section = "Inline Styling & Layout", sectionOrder = 20, order = 40,
            summary = "Returns the live `LayoutStyle` owned by this widget.",
            criticalRules = "Mutating the returned style updates layout constraints and invalidates layout/visual state through its callback.")
    @WikiMethod(section = "<key:widget.base.api.section.layout>", sectionOrder = 20, order = 12,
            title = "<key:widget.base.method.get_layout_style.title>", summary = "<key:widget.base.method.get_layout_style.summary>")
    @WikiReturn("<key:widget.base.method.get_layout_style.return>")
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
    @AiDoc(section = "Inline Styling & Layout", sectionOrder = 20, order = 50,
            summary = "Applies one or more layout changes through a batched `LayoutStyle` callback.")
    @WikiMethod(
            section = "<key:widget.base.api.section.layout>",
            sectionOrder = 20,
            order = 10,
            title = "<key:widget.base.method.layout.title>",
            summary = "<key:widget.base.method.layout.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase layout(@WikiParameter(name = "update", description = "<key:widget.base.method.layout.parameter>") Consumer<LayoutStyle> update) {
        layoutStyle.update(update);
        return this;
    }

    /**
     * Синхронизирует внутреннее состояние виджета через {@code syncLayoutStyleFromConstraints}.
     */
    private void syncLayoutStyleFromConstraints() {
        syncingLayoutStyle = true;
        try {
            layoutStyle.copyFrom(LayoutStyleLegacyAdapter.fromConstraints(layoutConstraints));
        } finally {
            syncingLayoutStyle = false;
        }
    }

    /**
     * Внутренний callback вызывается при изменении layout-стиля и обновляет ограничения виджета.
     */
    private void onLayoutStyleChanged() {
        if (syncingLayoutStyle) return;
        layoutConstraints = LayoutStyleLegacyAdapter.toConstraints(layoutStyle, layoutConstraints);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    /**
     * Возвращает или обновляет режим видимости виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 12,
            title = "<key:widget.base.method.get_visibility.title>", summary = "<key:widget.base.method.get_visibility.summary>")
    @WikiReturn("<key:widget.base.method.get_visibility.return>")
    public Visibility visibility() {
        return visibility;
    }

    /**
     * Возвращает или обновляет режим видимости виджета.
     */
    @XmlAttribute(value = "visibility", category = "Behavior", defaultValue = "visible", description = "Visibility mode: visible, hidden or collapsed.")
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 10,
            summary = "Sets `VISIBLE`, `HIDDEN`, or `COLLAPSED` and invalidates layout when the layout footprint changes.",
            criticalRules = "`HIDDEN` preserves layout space; `COLLAPSED` removes the widget from layout.")
    @WikiMethod(
            section = "<key:widget.base.api.section.state>",
            sectionOrder = 30,
            order = 10,
            title = "<key:widget.base.method.visibility.title>",
            summary = "<key:widget.base.method.visibility.summary>",
            markdown = "<key:widget.base.method.visibility.markdown>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase visibility(@WikiParameter(name = "visibility", description = "<key:widget.base.method.visibility.parameter>") Visibility visibility) {
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
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 22,
            title = "<key:widget.base.method.get_visible.title>", summary = "<key:widget.base.method.get_visible.summary>")
    @WikiReturn("<key:widget.base.method.get_visible.return>")
    public boolean visible() {
        return visibility == Visibility.VISIBLE;
    }

    /**
     * Возвращает или задаёт простую видимость виджета через режим {@code Visibility}.
     */
    @XmlAttribute(value = "visible", category = "Behavior", defaultValue = "true", description = "Whether the widget is visible without collapsing layout space.")
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 20,
            summary = "Convenience toggle between `VISIBLE` and `HIDDEN`; it never selects `COLLAPSED`.")
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 20,
            title = "<key:widget.base.method.visible.title>", summary = "<key:widget.base.method.visible.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase visible(@WikiParameter(name = "visible", description = "<key:widget.base.method.visible.parameter>") boolean visible) {
        return visibility(visible ? Visibility.VISIBLE : Visibility.HIDDEN);
    }

    /**
     * Возвращает или задаёт доступность виджета для пользовательского ввода.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 32,
            title = "<key:widget.base.method.get_enabled.title>", summary = "<key:widget.base.method.get_enabled.summary>")
    @WikiReturn("<key:widget.base.method.get_enabled.return>")
    public boolean enabled() {
        return enabled;
    }

    /**
     * Возвращает или задаёт доступность виджета для пользовательского ввода.
     */
    @XmlAttribute(value = "enabled", category = "Behavior", defaultValue = "true", description = "Whether the widget can receive user interaction.")
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 30,
            summary = "Enables or disables user interaction and exposes the disabled style state.")
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 30,
            title = "<key:widget.base.method.enabled.title>", summary = "<key:widget.base.method.enabled.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase enabled(@WikiParameter(name = "enabled", description = "<key:widget.base.method.enabled.parameter>") boolean enabled) {
        if (this.enabled == enabled) return this;
        this.enabled = enabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает состояние наведения указателя на виджет.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 35,
            title = "<key:widget.base.method.hovered.title>", summary = "<key:widget.base.method.hovered.summary>")
    @WikiReturn("<key:widget.base.method.hovered.return>")
    public boolean hovered() {
        return hovered;
    }

    /**
     * Возвращает или задаёт курсор мыши для виджета.
     */
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 42,
            title = "<key:widget.base.method.get_mouse_cursor.title>", summary = "<key:widget.base.method.get_mouse_cursor.summary>")
    @WikiReturn("<key:widget.base.method.get_mouse_cursor.return>")
    public MouseCursor mouseCursor() {
        return mouseCursor;
    }

    /**
     * Возвращает или задаёт курсор мыши для виджета.
     */
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 40,
            summary = "Sets the cursor used while the enabled, visible widget is under the pointer.")
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 40,
            title = "<key:widget.base.method.mouse_cursor.title>", summary = "<key:widget.base.method.mouse_cursor.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase mouseCursor(@WikiParameter(name = "mouseCursor", description = "<key:widget.base.method.mouse_cursor.parameter>") MouseCursor mouseCursor) {
        this.mouseCursor = mouseCursor == null ? MouseCursor.DEFAULT : mouseCursor;
        return this;
    }

    /**
     * Возвращает курсор мыши для указанной локальной точки виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 44,
            title = "<key:widget.base.method.mouse_cursor_at.title>", summary = "<key:widget.base.method.mouse_cursor_at.summary>")
    @WikiReturn("<key:widget.base.method.mouse_cursor_at.return>")
    public MouseCursor mouseCursorAt(@WikiParameter(name = "localX", description = "<key:widget.base.method.mouse_cursor_at.x>") float localX, @WikiParameter(name = "localY", description = "<key:widget.base.method.mouse_cursor_at.y>") float localY) {
        return enabled && visibility == Visibility.VISIBLE ? mouseCursor : MouseCursor.DEFAULT;
    }

    /**
     * Возвращает или задаёт возможность виджета получать фокус.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 52,
            title = "<key:widget.base.method.get_focusable.title>", summary = "<key:widget.base.method.get_focusable.summary>")
    @WikiReturn("<key:widget.base.method.get_focusable.return>")
    public boolean focusable() {
        return focusable;
    }

    /**
     * Возвращает или задаёт возможность виджета получать фокус.
     */
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 50,
            summary = "Enables keyboard focus for the widget.")
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 50,
            title = "<key:widget.base.method.focusable.title>", summary = "<key:widget.base.method.focusable.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase focusable(@WikiParameter(name = "focusable", description = "<key:widget.base.method.focusable.parameter>") boolean focusable) {
        if (this.focusable == focusable) return this;
        this.focusable = focusable;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт режим отдельной области фокусировки для виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 62,
            title = "<key:widget.base.method.get_focus_scope.title>", summary = "<key:widget.base.method.get_focus_scope.summary>")
    @WikiReturn("<key:widget.base.method.get_focus_scope.return>")
    public boolean focusScope() {
        return focusScope;
    }

    /**
     * Возвращает или задаёт режим отдельной области фокусировки для виджета.
     */
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 60,
            summary = "Makes this widget a boundary for keyboard focus traversal.")
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 60,
            title = "<key:widget.base.method.focus_scope.title>", summary = "<key:widget.base.method.focus_scope.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase focusScope(@WikiParameter(name = "focusScope", description = "<key:widget.base.method.focus_scope.parameter>") boolean focusScope) {
        if (this.focusScope == focusScope) return this;
        this.focusScope = focusScope;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт порядок обхода фокуса для виджета.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 72,
            title = "<key:widget.base.method.get_focus_order.title>", summary = "<key:widget.base.method.get_focus_order.summary>")
    @WikiReturn("<key:widget.base.method.get_focus_order.return>")
    public int focusOrder() {
        return focusOrder;
    }

    /**
     * Возвращает или задаёт порядок обхода фокуса для виджета.
     */
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 70,
            summary = "Sets the relative order used by keyboard focus traversal.")
    @WikiMethod(section = "<key:widget.base.api.section.state>", sectionOrder = 30, order = 70,
            title = "<key:widget.base.method.focus_order.title>", summary = "<key:widget.base.method.focus_order.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase focusOrder(@WikiParameter(name = "focusOrder", description = "<key:widget.base.method.focus_order.parameter>") int focusOrder) {
        if (this.focusOrder == focusOrder) return this;
        this.focusOrder = focusOrder;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт область локального применения стилей.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 52,
            title = "<key:widget.base.method.get_style_scope.title>", summary = "<key:widget.base.method.get_style_scope.summary>")
    @WikiReturn("<key:widget.base.method.get_style_scope.return>")
    public boolean styleScope() {
        return styleScope;
    }

    /**
     * Возвращает или задаёт область локального применения стилей.
     */
    @AiDoc(section = "State & Interaction", sectionOrder = 30, order = 80,
            summary = "Limits local style lookup to this widget subtree when enabled; global theme lookup still applies.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 50,
            title = "<key:widget.base.method.style_scope.title>", summary = "<key:widget.base.method.style_scope.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase styleScope(@WikiParameter(name = "styleScope", description = "<key:widget.base.method.style_scope.parameter>") boolean styleScope) {
        if (this.styleScope == styleScope) return this;
        this.styleScope = styleScope;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает или задаёт локальный стиль для указанного типа виджета.
     */
    @AiDoc(section = "Inline Styling & Layout", sectionOrder = 20, order = 60,
            summary = "Adds or removes a local style override for a widget type.")
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 60,
            title = "<key:widget.base.method.local_style.title>", summary = "<key:widget.base.method.local_style.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase localStyle(@WikiParameter(name = "widgetType", description = "<key:widget.base.method.local_style.widget_type>") String widgetType, @WikiParameter(name = "style", description = "<key:widget.base.method.local_style.style>") Style style) {
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
    @WikiMethod(section = "<key:widget.base.api.section.identity>", sectionOrder = 10, order = 62,
            title = "<key:widget.base.method.get_local_style.title>", summary = "<key:widget.base.method.get_local_style.summary>")
    @WikiReturn("<key:widget.base.method.get_local_style.return>")
    public Style localStyle(@WikiParameter(name = "widgetType", description = "<key:widget.base.method.get_local_style.widget_type>") String widgetType) {
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
     * Returns the semantic role used to resolve style renderer overrides.
     *
     * <p>Widgets with role-specific visuals (buttons, sliders, text inputs) override
     * this to their {@link WidgetRole}. The role guards style-declared renderers from
     * being applied to incompatible widgets.</p>
     *
     * @return render role of this widget
     */
    protected WidgetRole renderRole() {
        return WidgetRole.UNSPECIFIED;
    }

    /**
     * Returns the per-instance custom renderer.
     *
     * <p>Each widget has exactly one renderer slot. A custom {@link WidgetRender}
     * fully replaces the standard visual of the widget (background plus content).
     * Children are still rendered by the framework afterwards.</p>
     *
     * @return custom renderer or {@code null} for the standard visual
     */
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 12,
            title = "<key:widget.base.method.get_renderer.title>", summary = "<key:widget.base.method.get_renderer.summary>")
    @WikiReturn("<key:widget.base.method.get_renderer.return>")
    public WidgetRender renderer() {
        return renderer;
    }

    /**
     * Sets the per-instance custom renderer.
     *
     * @param renderer custom renderer or {@code null} for the standard visual
     * @return this widget
     */
    @AiDoc(section = "Rendering & Caching", sectionOrder = 50, order = 10,
            summary = "Sets a per-instance renderer that replaces the widget's standard visual.",
            criticalRules = "The custom renderer replaces background/content drawing, but framework child traversal still occurs.")
    @WikiMethod(
            section = "<key:widget.base.api.section.render>",
            sectionOrder = 50,
            order = 10,
            title = "<key:widget.base.method.renderer.title>",
            summary = "<key:widget.base.method.renderer.summary>",
            markdown = "<key:widget.base.method.renderer.markdown>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase renderer(@WikiParameter(name = "renderer", description = "<key:widget.base.method.renderer.parameter>") WidgetRender renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Resolves the effective custom renderer: instance slot first, then style override.
     *
     * @return custom renderer or {@code null} if the standard visual applies
     */
    protected final WidgetRender customRender() {
        if (renderer != null) return renderer;
        return styleRenderOverride();
    }

    /**
     * Renders the widget through the effective custom renderer, if present.
     *
     * @param draw draw scope of the widget
     * @return {@code true} if a custom renderer handled the visual
     */
    protected final boolean renderCustomVisual(DrawScope draw) {
        WidgetRender custom = customRender();
        if (custom == null) return false;
        custom.render(draw, this);
        return true;
    }

    /**
     * Resolves only an explicit renderer override from style data.
     *
     * <p>Widgets try the instance renderer first, then the style renderer, and only
     * then draw their own hardcoded visual.</p>
     *
     * @return style-declared renderer or {@code null}
     */
    protected WidgetRender styleRenderOverride() {
        return styleRenderOverride(renderRole());
    }

    /**
     * Resolves an explicit renderer override from style data with role check.
     *
     * @param role expected widget role
     * @return compatible style renderer or {@code null}
     */
    protected WidgetRender styleRenderOverride(WidgetRole role) {
        return styleRenderOverride(role, true);
    }

    /**
     * Resolves a renderer override while optionally excluding the global theme.
     * Local styles remain in the lookup chain regardless of the theme flag.
     *
     * @param role         expected widget role
     * @param includeTheme whether global theme and StylePack values participate
     * @return compatible style renderer or {@code null}
     */
    protected WidgetRender styleRenderOverride(WidgetRole role, boolean includeTheme) {
        if (role == null) role = WidgetRole.UNSPECIFIED;
        UIContext context = uiContext();
        Theme theme = includeTheme && context != null ? context.theme() : Theme.EMPTY;
        long styleVersion = currentRenderStyleVersion();
        Style componentDefaults = defaultStyle();
        long defaultVersion = componentDefaults == null ? 0L : componentDefaults.version();
        long registryVersion = WidgetRenderRegistry.global().version();
        if (styleRendererCacheValid
                && cachedStyleRendererStyleVersion == styleVersion
                && cachedStyleRendererDefaultVersion == defaultVersion
                && cachedStyleRendererSelectorVersion == styleSelectorVersion
                && cachedStyleRendererRegistryVersion == registryVersion
                && cachedStyleRendererRole == role
                && cachedStyleRendererContext == context
                && cachedStyleRendererTheme == theme
                && cachedStyleRendererDefaults == componentDefaults
                && cachedStyleRendererIncludesTheme == includeTheme) {
            return cachedStyleRenderer;
        }

        String type = styleType();
        Style themeStyle = theme instanceof StylePack stylePack
                ? stylePack.resolveStyleFor(type, styleId(), styleClasses())
                : theme.styleFor(type);
        Object value = defaultStyle().get(StyleKeys.RENDERER, null, null);
        value = themeStyle.get(StyleKeys.RENDERER, null, value);
        if ((value == null || value.equals("")) && theme instanceof StylePack stylePack) {
            value = stylePack.rendererIdFor(type, styleId(), styleClasses());
        }
        for (Widget current : styleLookupChain()) {
            Style localStyle = current.localStyle(type);
            value = localStyle.get(StyleKeys.RENDERER, null, value);
        }
        WidgetRender resolved = WidgetRenderRegistry.global().resolve(role, value, null);
        cachedStyleRendererStyleVersion = styleVersion;
        cachedStyleRendererDefaultVersion = defaultVersion;
        cachedStyleRendererSelectorVersion = styleSelectorVersion;
        cachedStyleRendererRegistryVersion = registryVersion;
        cachedStyleRendererRole = role;
        cachedStyleRendererContext = context;
        cachedStyleRendererTheme = theme;
        cachedStyleRendererDefaults = componentDefaults;
        cachedStyleRendererIncludesTheme = includeTheme;
        cachedStyleRenderer = resolved;
        styleRendererCacheValid = true;
        return resolved;
    }

    /**
     * Возвращает встроенные defaults компонента, которые лежат ниже темы и локальных стилей.
     *
     * @return defaults виджета или {@link Style#EMPTY}
     */
    protected Style defaultStyle() {
        return Style.EMPTY;
    }

    /**
     * Читает значение стиля с учётом defaults виджета, темы и локальных style-scope.
     * Display-примитивы используют этот общий путь так же, как поверхности контейнеров.
     *
     * @param key      ключ свойства
     * @param state    состояние виджета
     * @param fallback значение при отсутствии объявления
     * @param <S>      тип свойства
     * @return разрешённое значение свойства
     */
    protected <S> S styleValue(StyleKey<S> key,
                               WidgetState state,
                               S fallback) {
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        String type = styleType();
        S value = defaultStyle().get(key, state, fallback);
        Style themeStyle = theme instanceof StylePack stylePack
                ? stylePack.resolveStyleFor(type, styleId(), styleClasses())
                : theme.styleFor(type);
        value = themeStyle.get(key, state, value);
        for (Widget current : styleLookupChain()) {
            value = current.localStyle(type).get(key, state, value);
        }
        return value;
    }

    /**
     * Читает значение стиля для текущего состояния виджета.
     */
    protected <S> S styleValue(StyleKey<S> key, S fallback) {
        return styleValue(key, styleState(), fallback);
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
    @AiDoc(section = "Invalidation & Lifecycle", sectionOrder = 70, order = 10,
            summary = "Marks widget state dirty, invalidates its retained render cache, and propagates subtree flags to ancestors.")
    @WikiMethod(
            section = "<key:widget.base.api.section.lifecycle>",
            sectionOrder = 60,
            order = 30,
            title = "<key:widget.base.method.invalidate.title>",
            summary = "<key:widget.base.method.invalidate.summary>",
            markdown = "<key:widget.base.method.invalidate.markdown>")
    public void invalidate(@WikiParameter(name = "flags", description = "<key:widget.base.method.invalidate.parameter>") int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags |= flags;
        renderCacheDirty = true;
        markSubtreeInvalidation(flags);
    }

    /**
     * Снимает указанные флаги инвалидации после успешного обновления.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 35,
            title = "<key:widget.base.method.clear_invalidation.title>", summary = "<key:widget.base.method.clear_invalidation.summary>")
    public void clearInvalidation(@WikiParameter(name = "flags", description = "<key:widget.base.method.invalidate.parameter>") int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags &= ~flags;
        recomputeSubtreeInvalidation();
    }

    /**
     * Снимает флаги без пузырькового пересчёта поддерева.
     *
     * <p>Используется только рантаймом после полного прохода layout/render,
     * когда известно, что всё поддерево актуально. Обычный
     * {@link #clearInvalidation(int)} на каждом узле обходит всех детей и
     * поднимается к родителю — при вызове рекурсивно сверху вниз это даёт
     * O(N²) на контейнер с N детьми (см. LAYOUT_PERF_CHECKLIST, шаг 1).
     * Прямой сброс обоих полей делает очистку линейной O(N).</p>
     *
     * @param flags bitmask, который был обработан runtime'ом
     */
    @AiDoc(section = "Invalidation & Lifecycle", sectionOrder = 70, order = 20,
            summary = "Clears processed flags without recomputing ancestors; intended only for runtime full-tree passes.",
            criticalRules = "Application and widget code should normally use `clearInvalidation`; incorrect direct clearing can leave ancestor state stale.")
    public void clearInvalidationDirect(int flags) {
        if (flags == InvalidationFlags.NONE) return;
        invalidationFlags &= ~flags;
        subtreeInvalidationFlags &= ~flags;
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
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 40,
            title = "<key:widget.base.method.measure.title>", summary = "<key:widget.base.method.measure.summary>")
    public void measure(@WikiParameter(name = "context", description = "<key:widget.base.method.measure.context>") LayoutContext context) {
        if (visibility == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
    }

    /**
     * Измеряет виджет с пропуском повторных вызовов на чистом поддереве.
     *
     * <p>Если поддерево не имеет висящего LAYOUT-флага и контекст совпадает с
     * последним измерением — {@code desiredSize} уже актуален и повторный
     * measure (часто рекурсивный по всему поддереву) пропускается. Это убирает
     * двойное измерение детей во flex-контейнерах: measure-фаза и arrange-фаза
     * вызывают {@code preparedItems} с одинаковыми контекстами для большинства
     * детей. При смене контекста (wrap/percent-зависимости) или грязном
     * поддереве измерение выполняется как обычно через виртуальный
     * {@code measure}, поэтому работает для всех наследников без их переделки.</p>
     *
     * @param context constraints, scale и служебные данные layout pass
     */
    @Override
    @AiDoc(section = "Rendering & Caching", sectionOrder = 50, order = 20,
            summary = "Reuses the measured desired size when constraints match and the subtree has no pending layout invalidation.")
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 42,
            title = "<key:widget.base.method.measure_cached.title>", summary = "<key:widget.base.method.measure_cached.summary>")
    public void measureCached(@WikiParameter(name = "context", description = "<key:widget.base.method.measure.context>") LayoutContext context) {
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        float availableHeight = context == null ? Float.POSITIVE_INFINITY : context.availableHeight();
        if (measureCacheValid
                && !InvalidationFlags.has(subtreeInvalidationFlags, InvalidationFlags.LAYOUT)
                && Float.compare(measureCacheWidth, availableWidth) == 0
                && Float.compare(measureCacheHeight, availableHeight) == 0) {
            measureCacheHits++;
            return;
        }
        measureCacheMisses++;
        measure(context);
        measureCacheValid = true;
        measureCacheWidth = availableWidth;
        measureCacheHeight = availableHeight;
    }

    /**
     * @return число пропусков повторного measure через {@link #measureCached}
     */
    public long measureCacheHits() {
        return measureCacheHits;
    }

    /**
     * @return число реальных измерений через {@link #measureCached}
     */
    public long measureCacheMisses() {
        return measureCacheMisses;
    }

    /**
     * Сбрасывает диагностические счётчики measure-кэша.
     */
    public void resetMeasureCacheStats() {
        measureCacheHits = 0L;
        measureCacheMisses = 0L;
    }

    /**
     * Сбрасывает measure-кэш. Вызывается автоматически при LAYOUT-инвалидации
     * через флаги; публичный метод нужен виджетам, меняющим размер вне флагов.
     */
    public void invalidateMeasureCache() {
        measureCacheValid = false;
    }

    @Override
    public LayoutSize minContentSize() {
        // Дефолт перезаписывает desiredSize вне кэша — сбрасываем ключ,
        // чтобы следующий measureCached не вернул stale размер.
        LayoutSize size = Widget.super.minContentSize();
        measureCacheValid = false;
        return size;
    }

    @Override
    public LayoutSize maxContentSize() {
        LayoutSize size = Widget.super.maxContentSize();
        measureCacheValid = false;
        return size;
    }

    /**
     * Размещает виджет в рассчитанных границах и обновляет его layout-состояние.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 50,
            title = "<key:widget.base.method.arrange.title>", summary = "<key:widget.base.method.arrange.summary>")
    public void arrange(@WikiParameter(name = "bounds", description = "<key:widget.base.method.arrange.bounds>") RectView bounds) {
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
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 30,
            title = "<key:widget.base.method.render.title>", summary = "<key:widget.base.method.render.summary>")
    public void render(@WikiParameter(name = "context", description = "<key:widget.base.method.render.context>") RenderContext context) {
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
    @AiDoc(section = "Rendering & Caching", sectionOrder = 50, order = 30,
            summary = "Enables or disables retained draw-command caching.",
            criticalRules = "The current retained cache is effective for leaf widgets; widgets with children use normal rendering.")
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 20,
            title = "<key:widget.base.method.render_caching_enabled.title>", summary = "<key:widget.base.method.render_caching_enabled.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase renderCachingEnabled(@WikiParameter(name = "enabled", description = "<key:widget.base.method.render_caching_enabled.parameter>") boolean enabled) {
        if (renderCachingEnabled == enabled) return this;
        renderCachingEnabled = enabled;
        renderCacheDirty = true;
        if (!enabled) releaseRenderCache();
        return this;
    }

    /**
     * @return {@code true}, если для виджета разрешён retained-кэш
     */
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 22,
            title = "<key:widget.base.method.get_render_caching_enabled.title>", summary = "<key:widget.base.method.get_render_caching_enabled.summary>")
    @WikiReturn("<key:widget.base.method.get_render_caching_enabled.return>")
    public boolean renderCachingEnabled() {
        return renderCachingEnabled;
    }

    /**
     * Помечает retained-фрагмент устаревшим без изменения публичных invalidation flags.
     *
     * @return этот виджет
     */
    @AiDoc(section = "Rendering & Caching", sectionOrder = 50, order = 40,
            summary = "Marks the retained draw-command fragment stale without modifying public invalidation flags.")
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 25,
            title = "<key:widget.base.method.invalidate_render_cache.title>", summary = "<key:widget.base.method.invalidate_render_cache.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public WidgetBase invalidateRenderCache() {
        renderCacheDirty = true;
        return this;
    }

    /**
     * @return количество команд в последнем собранном фрагменте или {@code 0}
     */
    public int renderCacheCommandCount() {
        return renderCache == null ? 0 : renderCache.size();
    }

    /**
     * @return число воспроизведений фрагмента без вызова {@link #render(RenderContext)}
     */
    public long renderCacheHits() {
        return renderCacheHits;
    }

    /**
     * @return число полных пересборок retained-фрагмента
     */
    public long renderCacheRebuilds() {
        return renderCacheRebuilds;
    }

    /**
     * Сбрасывает диагностические счётчики retained-кэша.
     */
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
    @WikiMethod(section = "<key:widget.base.api.section.render>", sectionOrder = 50, order = 32,
            title = "<key:widget.base.method.render_cached.title>", summary = "<key:widget.base.method.render_cached.summary>")
    public void renderCached(@WikiParameter(name = "context", description = "<key:widget.base.method.render.context>") RenderContext context) {
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
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 50,
            title = "<key:widget.base.method.tick.title>", summary = "<key:widget.base.method.tick.summary>")
    public void tick(@WikiParameter(name = "frame", description = "<key:widget.base.method.tick.frame>") FrameContext frame) {
        tickAnimations(frame);
    }

    /**
     * Освобождает retained-команды перед удалением виджета из UI-дерева.
     */
    @Override
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 60,
            title = "<key:widget.base.method.dispose.title>", summary = "<key:widget.base.method.dispose.summary>")
    public void dispose() {
        for (SignalSubscription binding : bindings.values()) binding.close();
        bindings.clear();
        restoreTransformEffectOffset();
        animations.clear();
        releaseRenderCache();
    }

    /**
     * Привязывает ключ поддерживаемого свойства к сигналу; неподдерживаемые ключи игнорируются.
     */
    @WikiMethod(section = "<key:widget.base.api.section.binding>", sectionOrder = 50, order = 10,
            title = "<key:widget.base.method.bind.title>", summary = "<key:widget.base.method.bind.summary>")
    @AiDoc(
            section = "Bindings",
            sectionOrder = 50,
            order = 10,
            summary = "Binds a widget property to a supported signal key and replaces any existing binding for that key.",
            criticalRules = {
                    "The signal's current value is applied immediately when the binding is created.",
                    "A supported binding is bidirectional and is closed automatically when this widget is disposed.",
                    "Unsupported keys are ignored; a signal whose type does not match the BindingData key is rejected."
            },
            related = {"BindingData", "BindingKeys", "SignalSubscription"})
    @WikiReturn("<key:widget.base.method.bind.return>")
    public <T> WidgetBase bind(
            @WikiParameter(description = "<key:widget.base.method.bind.param.key>") BindingData<T> key,
            @WikiParameter(description = "<key:widget.base.method.bind.param.signal>") T signal) {
        Objects.requireNonNull(key, "key");
        if (signal == null || !key.type().isInstance(signal)) {
            throw new IllegalArgumentException("Signal must be an instance of " + key.type().getName());
        }

        SignalSubscription previous = bindings.remove(key);
        if (previous != null) previous.close();

        SignalSubscription binding = createBinding(key, signal);
        if (binding == null) return this;

        bindings.put(key, binding);
        return this;
    }

    /**
     * Создаёт привязку для ключа, поддерживаемого данным виджетом, или возвращает null, если он не поддерживается.
     */
    protected <T> SignalSubscription createBinding(BindingData<T> key, T signal) {
        return null;
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
            version = version * 31L + System.identityHashCode(current);
            version = version * 31L + (current.styleScope() ? 1L : 0L);
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
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 25,
            title = "<key:widget.base.method.handle.title>", summary = "<key:widget.base.method.handle.summary>")
    public void handle(@WikiParameter(name = "event", description = "<key:widget.base.method.emit.event>") Event event) {
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
    @AiDoc(section = "Events", sectionOrder = 60, order = 10,
            summary = "Registers a typed event listener and returns a subscription used to unsubscribe.")
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 10,
            title = "<key:widget.base.method.on.title>", summary = "<key:widget.base.method.on.summary>")
    @WikiReturn("<key:widget.base.method.on.return>")
    public <T extends Event> EventSubscription on(@WikiParameter(name = "type", description = "<key:widget.base.method.on.type>") EventType<T> type, @WikiParameter(name = "listener", description = "<key:widget.base.method.on.listener>") EventListener<? super T> listener) {
        return events.on(type, listener);
    }

    @Override
    @AiDoc(section = "Events", sectionOrder = 60, order = 20,
            summary = "Registers a typed listener with explicit control over receiving cancelled events.")
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 12,
            title = "<key:widget.base.method.on_cancelled.title>", summary = "<key:widget.base.method.on_cancelled.summary>")
    @WikiReturn("<key:widget.base.method.on.return>")
    public <T extends Event> EventSubscription on(@WikiParameter(name = "type", description = "<key:widget.base.method.on.type>") EventType<T> type, @WikiParameter(name = "receiveCancelled", description = "<key:widget.base.method.on_cancelled.receive_cancelled>") boolean receiveCancelled, @WikiParameter(name = "listener", description = "<key:widget.base.method.on.listener>") EventListener<? super T> listener) {
        return events.on(type, receiveCancelled, listener);
    }

    /**
     * Отправляет событие подписчикам этого виджета.
     */
    @Override
    @AiDoc(section = "Events", sectionOrder = 60, order = 30,
            summary = "Dispatches an event to listeners registered on this widget.")
    @WikiMethod(section = "<key:widget.base.api.section.lifecycle>", sectionOrder = 60, order = 20,
            title = "<key:widget.base.method.emit.title>", summary = "<key:widget.base.method.emit.summary>")
    public void emit(@WikiParameter(name = "event", description = "<key:widget.base.method.emit.event>") Event event) {
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
        // Смена bounds — результат layout-прохода, а не повод для нового:
        // LAYOUT здесь не ставится, иначе каждый arrange сам требовал бы
        // следующий layout (скролл, ресайз окна — вечный layout каждый кадр).
        // VISUAL обязателен: любой invalidate() взводит renderCacheDirty, и без
        // него сдвинувшийся виджет отдавал бы протухший retained-фрагмент со
        // старой позиции (окна при drag, маркеры карты при pan).
        invalidate(InvalidationFlags.VISUAL);
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
        public boolean isFinished() {
            return transition.finished();
        }

        @Override
        public void cancel() {
            transition.cancel();
        }

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
        public boolean isFinished() {
            return transition.finished();
        }

        @Override
        public void cancel() {
        }
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
