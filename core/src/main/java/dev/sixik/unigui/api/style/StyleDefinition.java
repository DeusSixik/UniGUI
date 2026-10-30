package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Именованное описание одного стиля внутри {@link StylePack}.
 *
 * <p>{@code StyleDefinition} связывает четыре вещи:</p>
 *
 * <ul>
 *     <li>{@link #id()} — стабильное имя стиля, по которому его можно выбрать через {@code widget.styleId(...)};</li>
 *     <li>{@link #selector()} — условие автоматического применения: тип виджета, style-class и/или style-id;</li>
 *     <li>{@link #backend()} — декларативные {@link Style}-свойства или ссылка на custom renderer;</li>
 *     <li>{@link #eventAnimations()} — связи событий виджета с animation preset'ами.</li>
 * </ul>
 *
 * <p>Обычный декларативный стиль создаётся через {@link #of(String, Style)}. Такой стиль можно
 * полностью редактировать в инспекторе: менять цвета, radius, border, text color и другие
 * {@link StyleKeys}. Если нужен сложный Java-render, который нельзя выразить набором свойств,
 * используется {@link #custom(String, String, Style)} или {@link #rendererId(String)}. В этом случае
 * стиль всё ещё может хранить обычные свойства, но отрисовку выполняет renderer из
 * {@link dev.sixik.unigui.api.widget.render.WidgetRenderRegistry}.</p>
 *
 * <p>Пример: применить стиль ко всем кнопкам с class {@code primary}.</p>
 *
 * <pre>{@code
 * StyleDefinition primary = StyleDefinition.of("button.primary", new MutableStyle()
 *         .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.1f, 0.3f, 0.8f, 1.0f))
 *         .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)))
 *         .target(Button.STYLE_TYPE)
 *         .styleClass("primary");
 * }</pre>
 *
 * <p>Пример: выбрать конкретный стиль с runtime-кода или из XML через {@code style="..."}.</p>
 *
 * <pre>{@code
 * Button button = new Button("Launch");
 * button.styleId("button.destiny");
 *
 * StyleDefinition destiny = StyleDefinition.custom(
 *         "button.destiny",
 *         "testmod:destiny/button",
 *         new MutableStyle().put(StyleKeys.TEXT_COLOR, textColor))
 *         .target(Button.STYLE_TYPE)
 *         .widgetId("button.destiny");
 * }</pre>
 *
 * @param id имя стиля внутри {@link StylePack}; не должно быть пустым
 * @param selector selector, по которому StylePack применяет стиль к виджетам
 * @param backend способ рендера и набор style-свойств
 * @param eventAnimations связи {@code eventId -> animationId}
 * @see StylePack
 * @see StyleSelector
 * @see StyleBackend
 * @see StyleAnimationDefinition
 */
@AiDoc(
        summary = "Immutable named style rule containing a selector, backend, properties, and event-to-animation bindings.",
        purpose = "Defines how one reusable rule in a `StylePack` is selected and rendered.",
        useWhen = {
                "Add a declarative style rule targeted by widget type, class, or explicit style id.",
                "Attach a registered custom renderer or animation preset to a style rule."
        },
        criticalRules = {
                "Every modifier returns a new `StyleDefinition`; retain or put the returned instance.",
                "`target` matches a widget style type, `styleClass` matches `addStyleClass`, and `widgetId` matches the widget's explicit `styleId` (not its general-purpose `id`).",
                "A custom renderer id must be registered in `WidgetRenderRegistry` before the style is resolved.",
                "Use stable event ids from `StyleAnimationIds.Event` when binding animation presets."
        },
        related = {"StylePack", "MutableStyle", "StyleSelector", "StyleAnimationDefinition", "WidgetRenderRegistry"},
        examples = @AiDoc.Example(
                title = "Target a primary button rule",
                code = """
                        StyleDefinition primary = StyleDefinition.of(
                                "button.primary",
                                new MutableStyle().put(StyleKeys.BACKGROUND_COLOR, accent))
                                .target(Button.STYLE_TYPE)
                                .styleClass("primary");
                        """))
@WikiPage(
        title = "StyleDefinition",
        slug = "style-definition",
        category = "<key:style.category>",
        summary = "<key:style.definition.summary>",
        order = 30)
@WikiSection(title = "<key:style.definition.section.overview.title>",
        markdown = "<key:style.definition.section.overview.body>", order = 10)
@WikiSection(title = "<key:style.definition.section.selectors.title>",
        markdown = "<key:style.definition.section.selectors.body>", order = 20)
@WikiExample(title = "<key:style.definition.example.rule.title>",
        code = "<key:style.definition.example.rule.code>", order = 10)
public record StyleDefinition(String id,
                              StyleSelector selector,
                              StyleBackend backend,
                              Map<String, String> eventAnimations) {
    /**
     * Создаёт style definition без selector'а.
     *
     * <p>Такой стиль сам по себе не матчится selector-логикой, но может быть выбран через
     * {@link StylePack#bind(String, String)} или через явный {@code widget.styleId(id)}.</p>
     *
     * @param id имя стиля
     * @param backend backend стиля
     * @param eventAnimations связи событий с animation preset'ами
     */
    public StyleDefinition(String id, StyleBackend backend, Map<String, String> eventAnimations) {
        this(id, StyleSelector.EMPTY, backend, eventAnimations);
    }

    /** Нормализует null-значения и отбрасывает пустые связи событий с анимациями. */
    public StyleDefinition {
        id = normalizeRequired(id, "id");
        selector = selector == null ? StyleSelector.EMPTY : selector;
        backend = backend == null ? StyleBackend.declarative(Style.EMPTY) : backend;
        eventAnimations = normalizeEventAnimations(eventAnimations);
    }

    /**
     * Создаёт декларативный стиль.
     *
     * @param id имя стиля
     * @param style набор редактируемых style-свойств
     * @return definition с {@link StyleBackend.Declarative}
     */
    @AiDoc(section = "Creation", sectionOrder = 10, order = 10,
            summary = "Creates a declarative rule backed only by typed style properties.")
    @WikiMethod(title = "of(String, Style)", summary = "<key:style.definition.method.of.summary>",
            section = "<key:style.definition.api.section.creation>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:style.definition.method.factory.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:style.definition.method.id_throws>")
    public static StyleDefinition of(
            @WikiParameter(name = "id", description = "<key:style.definition.method.id>") String id,
            @WikiParameter(name = "style", description = "<key:style.definition.method.style>") Style style) {
        return new StyleDefinition(id, StyleSelector.EMPTY, StyleBackend.declarative(style), Map.of());
    }

    /**
     * Создаёт стиль с custom renderer'ом.
     *
     * <p>{@code rendererId} должен быть зарегистрирован в
     * {@link dev.sixik.unigui.api.widget.render.WidgetRenderRegistry}.</p>
     *
     * @param id имя стиля
     * @param rendererId id renderer'а в registry
     * @param style дополнительные style-свойства, которые renderer может читать через state
     * @return definition с {@link StyleBackend.Custom}
     */
    @AiDoc(section = "Creation", sectionOrder = 10, order = 20,
            summary = "Creates a rule backed by a registered custom renderer plus optional properties.")
    @WikiMethod(title = "custom(String, String, Style)", summary = "<key:style.definition.method.custom.summary>",
            section = "<key:style.definition.api.section.creation>", sectionOrder = 10, order = 20)
    @WikiReturn("<key:style.definition.method.factory.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:style.definition.method.id_throws>")
    public static StyleDefinition custom(
            @WikiParameter(name = "id", description = "<key:style.definition.method.id>") String id,
            @WikiParameter(name = "rendererId", description = "<key:style.definition.method.renderer_id>") String rendererId,
            @WikiParameter(name = "style", description = "<key:style.definition.method.style>") Style style) {
        return new StyleDefinition(id, StyleSelector.EMPTY, StyleBackend.custom(rendererId, style), Map.of());
    }

    /**
     * Возвращает декларативные свойства стиля независимо от backend-типа.
     *
     * @return {@link Style}, вложенный в {@link #backend()}
     */
    public Style style() {
        return backend.style();
    }

    /**
     * Возвращает id custom renderer'а.
     *
     * @return renderer id или пустая строка для декларативного стиля
     */
    public String rendererId() {
        return backend.rendererId();
    }

    /**
     * Проверяет, требует ли стиль Java-renderer из registry.
     *
     * @return {@code true}, если backend содержит непустой renderer id
     */
    public boolean customRenderer() {
        return backend.customRenderer();
    }

    /**
     * Возвращает animation preset, привязанный к событию.
     *
     * @param eventName id события, например {@link StyleAnimationIds.Event#ON_CLICK}
     * @return id animation preset'а или пустая строка
     */
    public String eventAnimation(String eventName) {
        return eventAnimations.getOrDefault(normalizeOptional(eventName), "");
    }

    /**
     * Возвращает копию definition с новым selector'ом.
     *
     * @param selector новый selector; {@code null} превращается в {@link StyleSelector#EMPTY}
     * @return новый {@code StyleDefinition}
     */
    public StyleDefinition selector(StyleSelector selector) {
        return new StyleDefinition(id, selector, backend, eventAnimations);
    }

    /**
     * Ограничивает стиль типом виджета.
     *
     * @param target id типа виджета, обычно из {@link StyleIds.Widget} или {@code Button.STYLE_TYPE}
     * @return новый {@code StyleDefinition}
     */
    @AiDoc(section = "Selectors", sectionOrder = 20, order = 10,
            summary = "Returns a copy restricted to a widget style type.")
    @WikiMethod(title = "target(String)", summary = "<key:style.definition.method.target.summary>",
            section = "<key:style.definition.api.section.selectors>", sectionOrder = 20, order = 10)
    @WikiReturn("<key:style.definition.method.copy.return>")
    public StyleDefinition target(
            @WikiParameter(name = "target", description = "<key:style.definition.method.target.value>") String target) {
        return selector(new StyleSelector(target, selector.styleClass(), selector.widgetId()));
    }

    /**
     * Ограничивает стиль style-class'ом виджета.
     *
     * @param styleClass class из {@code widget.addStyleClass(...)} или XML-атрибута {@code class}
     * @return новый {@code StyleDefinition}
     */
    @AiDoc(section = "Selectors", sectionOrder = 20, order = 20,
            summary = "Returns a copy restricted to a widget style class.")
    @WikiMethod(title = "styleClass(String)", summary = "<key:style.definition.method.style_class.summary>",
            section = "<key:style.definition.api.section.selectors>", sectionOrder = 20, order = 20)
    @WikiReturn("<key:style.definition.method.copy.return>")
    public StyleDefinition styleClass(
            @WikiParameter(name = "styleClass", description = "<key:style.definition.method.style_class.value>") String styleClass) {
        return selector(new StyleSelector(selector.target(), styleClass, selector.widgetId()));
    }

    /**
     * Ограничивает стиль явным style id виджета.
     *
     * <p>Этот selector срабатывает, когда у виджета задан {@code widget.styleId(widgetId)}
     * или XML-атрибут {@code style="..."}.</p>
     *
     * @param widgetId style id виджета
     * @return новый {@code StyleDefinition}
     */
    @AiDoc(section = "Selectors", sectionOrder = 20, order = 30,
            summary = "Returns a copy restricted to a widget's explicit style id.")
    @WikiMethod(title = "widgetId(String)", summary = "<key:style.definition.method.widget_id.summary>",
            section = "<key:style.definition.api.section.selectors>", sectionOrder = 20, order = 30)
    @WikiReturn("<key:style.definition.method.copy.return>")
    public StyleDefinition widgetId(
            @WikiParameter(name = "widgetId", description = "<key:style.definition.method.widget_id.value>") String widgetId) {
        return selector(new StyleSelector(selector.target(), selector.styleClass(), widgetId));
    }

    /**
     * Заменяет декларативные свойства, сохраняя id, selector и связи событий с анимациями.
     *
     * @param style новый набор свойств
     * @return новый {@code StyleDefinition}
     */
    public StyleDefinition style(Style style) {
        return new StyleDefinition(id, selector, StyleBackend.declarative(style), eventAnimations);
    }

    /**
     * Переводит definition на custom renderer, сохраняя текущие style-свойства.
     *
     * @param rendererId id renderer'а в {@link dev.sixik.unigui.api.widget.render.WidgetRenderRegistry}
     * @return новый {@code StyleDefinition}
     */
    public StyleDefinition rendererId(String rendererId) {
        return new StyleDefinition(id, selector, StyleBackend.custom(rendererId, style()), eventAnimations);
    }

    /**
     * Добавляет или удаляет привязку события к animation preset'у.
     *
     * <p>Если {@code animationId} пустой, связь для события удаляется. Event id лучше брать из
     * {@link StyleAnimationIds.Event} или из статических классов виджета, например
     * {@code Button.AnimationEvents.ON_CLICK}.</p>
     *
     * @param eventName id события
     * @param animationId id определения {@link StyleAnimationDefinition}
     * @return новый {@code StyleDefinition}
     */
    @AiDoc(section = "Animations", sectionOrder = 30, order = 10,
            summary = "Returns a copy that adds, replaces, or removes an event-to-animation binding.")
    @WikiMethod(title = "eventAnimation(String, String)", summary = "<key:style.definition.method.event_animation.summary>",
            section = "<key:style.definition.api.section.animations>", sectionOrder = 30, order = 10)
    @WikiReturn("<key:style.definition.method.copy.return>")
    public StyleDefinition eventAnimation(
            @WikiParameter(name = "eventName", description = "<key:style.definition.method.event_animation.event>") String eventName,
            @WikiParameter(name = "animationId", description = "<key:style.definition.method.event_animation.animation>") String animationId) {
        String event = normalizeRequired(eventName, "eventName");
        String animation = normalizeOptional(animationId);
        Map<String, String> next = new LinkedHashMap<>(eventAnimations);
        if (animation.isEmpty()) {
            next.remove(event);
        } else {
            next.put(event, animation);
        }
        return new StyleDefinition(id, selector, backend, next);
    }

    private static Map<String, String> normalizeEventAnimations(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Map.of();
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            String event = normalizeOptional(entry.getKey());
            String animation = normalizeOptional(entry.getValue());
            if (!event.isEmpty() && !animation.isEmpty()) {
                normalized.put(event, animation);
            }
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = normalizeOptional(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
