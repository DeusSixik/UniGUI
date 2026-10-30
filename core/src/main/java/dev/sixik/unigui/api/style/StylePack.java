package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Именованная коллекция переиспользуемых стилей и animation preset'ов.
 *
 * <p>{@code StylePack} одновременно является {@link Theme}: существующие виджеты могут получать
 * его через {@code UIContext.theme(...)}, а редактор может работать с именованными стилями,
 * binding'ами и связями событий с анимациями как с обычными данными.</p>
 *
 * <p>Pack решает три задачи: хранит {@link StyleDefinition}, выбирает подходящие определения
 * Значения резолвятся через {@link StyleSelector}.
 * когда для типа виджета зарегистрирован декларативный render-plan builder.</p>
 */
@AiDoc(
        summary = "Named theme containing reusable style rules, widget bindings, design tokens, and animation presets.",
        purpose = "Acts as both a `Theme` and an editable style-sheet model for Java or XML-defined UI styling.",
        useWhen = {
                "Install a set of reusable styles on `UIContext.theme(...)`.",
                "Resolve selectors and explicit style ids for a concrete widget.",
                "Load or edit a complete XML `<StylePack>` including animations and renderer bindings."
        },
        criticalRules = {
                "Use `Style.from` for a single `<Style>`; use `StylePack.from` for a full `<StylePack>` document.",
                "Later matching definitions override earlier ones regardless of selector specificity.",
                "`bind(widgetType, styleId)` supplies a base definition; matching selector rules can still layer over it.",
                "`resolveStyleFor` layers tokens, fallback, and matching definitions in that order.",
                "Adding or removing rules increments the pack version; returned maps and definition lists are snapshots."
        },
        related = {"Theme", "Style", "MutableStyle", "StyleDefinition", "StyleAnimationDefinition", "ThemeTokens"},
        examples = @AiDoc.Example(
                title = "Build and install a style pack",
                code = """
                        StylePack pack = StylePack.create("app")
                                .put(StyleDefinition.of("button.primary", primaryStyle)
                                        .target(Button.STYLE_TYPE)
                                        .styleClass("primary"));
                        context.theme(pack);
                        """))
@WikiPage(
        title = "StylePack",
        slug = "style-pack",
        category = "<key:style.category>",
        summary = "<key:style.pack.summary>",
        order = 40)
@WikiSection(title = "<key:style.pack.section.overview.title>",
        markdown = "<key:style.pack.section.overview.body>", order = 10)
@WikiSection(title = "<key:style.pack.section.cascade.title>",
        markdown = "<key:style.pack.section.cascade.body>", order = 20)
@WikiExample(title = "<key:style.pack.example.java.title>",
        code = "<key:style.pack.example.java.code>", order = 10)
@WikiExample(title = "<key:style.pack.example.xml.title>",
        code = "<key:style.pack.example.xml.code>", language = "xml", order = 20)
public final class StylePack implements Theme {
    private String id;
    private final Map<String, StyleDefinition> styles = new LinkedHashMap<>();
    private final Map<String, String> widgetBindings = new LinkedHashMap<>();
    private final Map<String, StyleAnimationDefinition> animations = new LinkedHashMap<>();
    private Style fallback = Style.EMPTY;
    private String fallbackStyleId = "";
    private ThemeTokens tokens = ThemeTokens.EMPTY;
    private long version;

    /**
     * Создаёт style pack с указанным id.
     *
     * @param id id pack'а; пустое значение заменяется на {@code style-pack}
     */
    public StylePack(String id) {
        this.id = normalizeOptional(id, "style-pack");
    }

    /**
     * Фабрика style pack'а.
     *
     * @param id id pack'а
     * @return новый {@code StylePack}
     */
    @AiDoc(section = "Creation", sectionOrder = 10, order = 10,
            summary = "Creates an empty mutable style pack with a normalized id.")
    @WikiMethod(title = "create(String)", summary = "<key:style.pack.method.create.summary>",
            section = "<key:style.pack.api.section.creation>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:style.pack.method.create.return>")
    public static StylePack create(
            @WikiParameter(name = "id", description = "<key:style.pack.method.create.id>") String id) {
        return new StylePack(id);
    }

    /**
     * Парсит XML-документ {@code <StylePack>} через стандартный registry свойств.
     *
     * <p>Это короткий entrypoint поверх {@link StylePackXml#parse(String)}, чтобы пользовательский
     * код мог писать {@code StylePack.from(xml)} без прямой зависимости от codec-класса.</p>
     *
     * @param xml XML-документ StylePack
     * @return загруженный style pack
     */
    @AiDoc(section = "Creation", sectionOrder = 10, order = 20,
            summary = "Parses a complete XML style pack with the built-in property registry.")
    @WikiMethod(title = "from(String)", summary = "<key:style.pack.method.from.summary>",
            section = "<key:style.pack.api.section.creation>", sectionOrder = 10, order = 20)
    @WikiReturn("<key:style.pack.method.from.return>")
    public static StylePack from(
            @WikiParameter(name = "xml", description = "<key:style.pack.method.from.xml>") String xml) {
        return StylePackXml.parse(xml);
    }

    /**
     * Парсит XML-документ {@code <StylePack>} через указанный registry свойств.
     *
     * @param xml      XML-документ StylePack
     * @param registry registry известных style-свойств
     * @return загруженный style pack
     */
    public static StylePack from(String xml, StyleKeyRegistry registry) {
        return StylePackXml.parse(xml, registry);
    }

    /**
     * @return id style pack'а
     */
    public String id() {
        return id;
    }

    /**
     * Меняет id style pack'а.
     *
     * @param id новый id
     * @return этот pack для fluent-настройки
     */
    public StylePack id(String id) {
        String normalized = normalizeOptional(id, "style-pack");
        if (this.id.equals(normalized)) return this;
        this.id = normalized;
        version++;
        return this;
    }

    /**
     * Возвращает fallback-стиль pack'а.
     *
     * @return стиль из {@link #fallbackStyle(String)} или прямой fallback
     */
    public Style fallback() {
        Style fallbackStyle = styleById(fallbackStyleId);
        return fallbackStyle == null ? fallback : fallbackStyle;
    }

    /**
     * Возвращает token fallback этого pack'а.
     *
     * @return неизменяемый снимок токенов
     */
    @Override
    public ThemeTokens tokens() {
        return tokens;
    }

    /**
     * Назначает token fallback, не меняя каскад StylePack rules.
     *
     * @param tokens снимок токенов или {@link ThemeTokens#EMPTY}
     * @return этот pack для fluent-настройки
     */
    public StylePack tokens(ThemeTokens tokens) {
        this.tokens = tokens == null ? ThemeTokens.EMPTY : tokens;
        version++;
        return this;
    }

    /**
     * Задаёт прямой fallback-стиль.
     *
     * @param fallback стиль для виджетов без совпавших definitions
     * @return этот pack для fluent-настройки
     */
    public StylePack fallback(Style fallback) {
        this.fallback = fallback == null ? Style.EMPTY : fallback;
        this.fallbackStyleId = "";
        version++;
        return this;
    }

    /**
     * Задаёт fallback через id одного из стилей pack'а.
     *
     * @param styleId id определения стиля
     * @return этот pack для fluent-настройки
     */
    public StylePack fallbackStyle(String styleId) {
        this.fallbackStyleId = normalizeOptional(styleId, "");
        version++;
        return this;
    }

    /**
     * Возвращает snapshot зарегистрированных стилей.
     *
     * @return карта {@code styleId -> StyleDefinition}
     */
    public Map<String, StyleDefinition> styles() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(styles));
    }

    /**
     * Ищет style definition по id.
     *
     * @param styleId id стиля
     * @return definition или {@link Optional#empty()}
     */
    public Optional<StyleDefinition> styleDefinition(String styleId) {
        return Optional.ofNullable(styles.get(normalizeOptional(styleId, "")));
    }

    /**
     * Возвращает наиболее приоритетный стиль для типа виджета без class/style-id условий.
     *
     * @param widgetType тип виджета
     * @return подходящее definition или empty
     */
    public Optional<StyleDefinition> styleDefinitionFor(String widgetType) {
        return styleDefinitionFor(widgetType, "", List.of());
    }

    /**
     * Возвращает наиболее приоритетный стиль для конкретного виджета.
     *
     * @param widgetType    тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses  style classes виджета
     * @return последнее подходящее definition в порядке добавления
     */
    public Optional<StyleDefinition> styleDefinitionFor(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        List<StyleDefinition> definitions = matchingDefinitions(widgetType, widgetStyleId, styleClasses);
        return definitions.isEmpty() ? Optional.empty() : Optional.of(definitions.get(definitions.size() - 1));
    }

    /**
     * Возвращает custom renderer id для конкретного виджета.
     *
     * @param widgetType    тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses  style classes виджета
     * @return renderer id или пустая строка
     */
    public String rendererIdFor(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        List<StyleDefinition> definitions = matchingDefinitions(widgetType, widgetStyleId, styleClasses);
        for (int i = definitions.size() - 1; i >= 0; i--) {
            String rendererId = definitions.get(i).rendererId();
            if (!rendererId.isEmpty()) return rendererId;
        }
        if (!fallbackStyleId.isEmpty()) {
            return styleDefinition(fallbackStyleId).map(StyleDefinition::rendererId).orElse("");
        }
        return "";
    }

    /**
     * Добавляет декларативный стиль по id.
     *
     * @param styleId id стиля
     * @param style   style-свойства
     * @return этот pack для fluent-настройки
     */
    public StylePack putStyle(String styleId, Style style) {
        return put(StyleDefinition.of(styleId, style));
    }

    /**
     * Добавляет или заменяет style definition.
     *
     * @param definition definition стиля
     * @return этот pack для fluent-настройки
     */
    @AiDoc(section = "Style Rules", sectionOrder = 20, order = 10,
            summary = "Adds or replaces one named immutable style definition.")
    @WikiMethod(title = "put(StyleDefinition)", summary = "<key:style.pack.method.put.summary>",
            section = "<key:style.pack.api.section.rules>", sectionOrder = 20, order = 10)
    @WikiReturn("<key:style.pack.method.fluent.return>")
    @WikiThrows(type = NullPointerException.class, description = "<key:style.pack.method.put.null_throws>")
    public StylePack put(
            @WikiParameter(name = "definition", description = "<key:style.pack.method.put.definition>") StyleDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        styles.put(definition.id(), definition);
        version++;
        return this;
    }

    /**
     * Удаляет style definition и связанные bindings.
     *
     * @param styleId id удаляемого стиля
     * @return этот pack для fluent-настройки
     */
    public StylePack removeStyle(String styleId) {
        String normalized = normalizeOptional(styleId, "");
        if (normalized.isEmpty()) return this;
        if (styles.remove(normalized) != null) {
            widgetBindings.values().removeIf(normalized::equals);
            if (fallbackStyleId.equals(normalized)) {
                fallbackStyleId = "";
            }
            version++;
        }
        return this;
    }

    /**
     * Возвращает snapshot bindings типа виджета к style id.
     *
     * @return карта {@code widgetType -> styleId}
     */
    public Map<String, String> widgetBindings() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(widgetBindings));
    }

    /**
     * Возвращает style binding для типа виджета.
     *
     * @param widgetType тип виджета
     * @return style id или пустая строка
     */
    public String binding(String widgetType) {
        return widgetBindings.getOrDefault(normalizeOptional(widgetType, ""), "");
    }

    /**
     * Привязывает тип виджета к style id.
     *
     * @param widgetType тип виджета
     * @param styleId    id стиля
     * @return этот pack для fluent-настройки
     */
    @AiDoc(section = "Style Rules", sectionOrder = 20, order = 20,
            summary = "Binds a widget style type to a base definition id; an empty id removes the binding.")
    @WikiMethod(title = "bind(String, String)", summary = "<key:style.pack.method.bind.summary>",
            section = "<key:style.pack.api.section.rules>", sectionOrder = 20, order = 20)
    @WikiReturn("<key:style.pack.method.fluent.return>")
    public StylePack bind(
            @WikiParameter(name = "widgetType", description = "<key:style.pack.method.bind.widget_type>") String widgetType,
            @WikiParameter(name = "styleId", description = "<key:style.pack.method.bind.style_id>") String styleId) {
        String normalizedWidgetType = normalizeOptional(widgetType, "");
        String normalizedStyleId = normalizeOptional(styleId, "");
        if (normalizedWidgetType.isEmpty()) return this;
        if (Theme.WILDCARD.equals(normalizedWidgetType)) {
            return fallbackStyle(normalizedStyleId);
        }
        if (normalizedStyleId.isEmpty()) {
            widgetBindings.remove(normalizedWidgetType);
        } else {
            widgetBindings.put(normalizedWidgetType, normalizedStyleId);
        }
        version++;
        return this;
    }

    /**
     * Удаляет binding для типа виджета.
     *
     * @param widgetType тип виджета
     * @return этот pack для fluent-настройки
     */
    public StylePack unbind(String widgetType) {
        return bind(widgetType, "");
    }

    /**
     * Возвращает snapshot animation preset'ов.
     *
     * @return карта {@code animationId -> definition}
     */
    public Map<String, StyleAnimationDefinition> animations() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(animations));
    }

    /**
     * Ищет animation preset по id.
     *
     * @param animationId id preset'а
     * @return animation definition или empty
     */
    public Optional<StyleAnimationDefinition> animation(String animationId) {
        return Optional.ofNullable(animations.get(normalizeOptional(animationId, "")));
    }

    /**
     * Добавляет или заменяет animation preset.
     *
     * @param animation определение анимации
     * @return этот pack для fluent-настройки
     */
    @AiDoc(section = "Animations", sectionOrder = 30, order = 10,
            summary = "Adds or replaces one named animation preset.")
    @WikiMethod(title = "putAnimation(StyleAnimationDefinition)", summary = "<key:style.pack.method.put_animation.summary>",
            section = "<key:style.pack.api.section.animations>", sectionOrder = 30, order = 10)
    @WikiReturn("<key:style.pack.method.fluent.return>")
    @WikiThrows(type = NullPointerException.class, description = "<key:style.pack.method.put_animation.null_throws>")
    public StylePack putAnimation(
            @WikiParameter(name = "animation", description = "<key:style.pack.method.put_animation.animation>") StyleAnimationDefinition animation) {
        Objects.requireNonNull(animation, "animation");
        animations.put(animation.id(), animation);
        version++;
        return this;
    }

    /**
     * Удаляет animation preset.
     *
     * @param animationId id preset'а
     * @return этот pack для fluent-настройки
     */
    public StylePack removeAnimation(String animationId) {
        String normalized = normalizeOptional(animationId, "");
        if (normalized.isEmpty()) return this;
        if (animations.remove(normalized) != null) {
            version++;
        }
        return this;
    }

    /**
     * Возвращает animation id, привязанный к событию конкретного стиля.
     *
     * @param styleId   id стиля
     * @param eventName id события
     * @return animation id или пустая строка
     */
    public String eventAnimation(String styleId, String eventName) {
        return styleDefinition(styleId)
                .map(definition -> definition.eventAnimation(eventName))
                .orElse("");
    }

    /**
     * Собирает итоговый стиль для конкретного виджета.
     *
     * @param widgetType    тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses  style classes виджета
     * @return итоговый стиль из fallback и подходящее definitions
     */
    @AiDoc(section = "Resolution", sectionOrder = 40, order = 10,
            summary = "Resolves tokens, fallback, bindings, explicit style id, and selector rules into one style.")
    @WikiMethod(title = "resolveStyleFor(String, String, Collection)", summary = "<key:style.pack.method.resolve.summary>",
            section = "<key:style.pack.api.section.resolution>", sectionOrder = 40, order = 10)
    @WikiReturn("<key:style.pack.method.resolve.return>")
    public Style resolveStyleFor(
            @WikiParameter(name = "widgetType", description = "<key:style.pack.method.resolve.widget_type>") String widgetType,
            @WikiParameter(name = "widgetStyleId", description = "<key:style.pack.method.resolve.style_id>") String widgetStyleId,
            @WikiParameter(name = "styleClasses", description = "<key:style.pack.method.resolve.classes>") Collection<String> styleClasses) {
        List<Style> layers = new ArrayList<>();
        if (tokens.asStyle() != Style.EMPTY) layers.add(tokens.asStyle());
        Style fallbackStyle = fallback();
        if (fallbackStyle != Style.EMPTY) layers.add(fallbackStyle);
        for (StyleDefinition definition : matchingDefinitions(widgetType, widgetStyleId, styleClasses)) {
            layers.add(definition.style());
        }
        return ResolvedStyle.of(layers);
    }

    /**
     * Возвращает все matching style definitions в порядке применения.
     *
     * @param widgetType    тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses  style classes виджета
     * @return definitions от меньшего приоритета к большему; более позднее
     * правило перекрывает более раннее независимо от specificity selector'а
     */
    public List<StyleDefinition> matchingDefinitions(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        String normalizedWidgetType = normalizeOptional(widgetType, "");
        String normalizedStyleId = normalizeOptional(widgetStyleId, "");
        List<StyleDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        addUnique(result, added, definitionByBindingOrType(normalizedWidgetType).orElse(null));

        for (StyleDefinition definition : styles.values()) {
            boolean explicitStyle = !normalizedStyleId.isEmpty()
                    && definition.id().equals(normalizedStyleId);
            if (explicitStyle || definition.selector().matches(normalizedWidgetType, normalizedStyleId, styleClasses)) {
                addUnique(result, added, definition);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public long version() {
        long result = version + tokens.version() + fallback.version();
        Style fallbackStyle = styleById(fallbackStyleId);
        if (fallbackStyle != null) {
            result += fallbackStyle.version();
        }
        for (StyleDefinition definition : styles.values()) {
            result += definition.style().version();
        }
        return result;
    }

    @Override
    public Style styleFor(String widgetType) {
        return resolveStyleFor(widgetType, "", List.of());
    }

    private Optional<StyleDefinition> definitionByBindingOrType(String widgetType) {
        if (widgetType == null || widgetType.isEmpty()) return Optional.empty();
        String boundStyleId = widgetBindings.get(widgetType);
        if (boundStyleId != null && !boundStyleId.isEmpty()) {
            return styleDefinition(boundStyleId);
        }
        return Optional.ofNullable(styles.get(widgetType));
    }

    private Style styleById(String styleId) {
        String normalized = normalizeOptional(styleId, "");
        if (normalized.isEmpty()) return null;
        StyleDefinition definition = styles.get(normalized);
        return definition == null ? null : definition.style();
    }

    private static void addUnique(List<StyleDefinition> result, Set<String> added, StyleDefinition definition) {
        if (definition == null) return;
        if (added.add(definition.id())) {
            result.add(definition);
        }
    }

    private static String normalizeOptional(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
