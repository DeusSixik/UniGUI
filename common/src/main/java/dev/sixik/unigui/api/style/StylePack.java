package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.StyleRenderPlanRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 * через {@link StyleSelector} и строит {@link RenderPlan} через {@link StyleRenderPlanRegistry},
 * когда для типа виджета зарегистрирован декларативный render-plan builder.</p>
 */
public final class StylePack implements Theme {
    private String id;
    private final Map<String, StyleDefinition> styles = new LinkedHashMap<>();
    private final Map<String, String> widgetBindings = new LinkedHashMap<>();
    private final Map<String, StyleAnimationDefinition> animations = new LinkedHashMap<>();
    private Style fallback = Style.EMPTY;
    private String fallbackStyleId = "";
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
    public static StylePack create(String id) {
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
    public static StylePack from(String xml) {
        return StylePackXml.parse(xml);
    }

    /**
     * Парсит XML-документ {@code <StylePack>} через указанный registry свойств.
     *
     * @param xml XML-документ StylePack
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
     * @param widgetType тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses style classes виджета
     * @return последний подходящее definition после сортировки specificity
     */
    public Optional<StyleDefinition> styleDefinitionFor(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        List<StyleDefinition> definitions = matchingDefinitions(widgetType, widgetStyleId, styleClasses);
        return definitions.isEmpty() ? Optional.empty() : Optional.of(definitions.get(definitions.size() - 1));
    }

    /**
     * Возвращает custom renderer id для конкретного виджета.
     *
     * @param widgetType тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses style classes виджета
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
     * @param style style-свойства
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
    public StylePack put(StyleDefinition definition) {
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
     * @param styleId id стиля
     * @return этот pack для fluent-настройки
     */
    public StylePack bind(String widgetType, String styleId) {
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
    public StylePack putAnimation(StyleAnimationDefinition animation) {
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
     * @param styleId id стиля
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
     * @param widgetType тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses style classes виджета
     * @return итоговый стиль из fallback и подходящее definitions
     */
    public Style resolveStyleFor(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        List<Style> layers = new ArrayList<>();
        Style fallbackStyle = fallback();
        if (fallbackStyle != Style.EMPTY) layers.add(fallbackStyle);
        for (StyleDefinition definition : matchingDefinitions(widgetType, widgetStyleId, styleClasses)) {
            layers.add(definition.style());
        }
        return ResolvedStyle.of(layers);
    }

    /**
     * Строит RenderPlan для типа виджета без class/style-id условий.
     *
     * @param widgetType тип виджета
     * @param stateType Java-тип render state
     * @param renderState snapshot состояния виджета
     * @param widgetState текущее visual state виджета
     * @return план рендера или empty, если builder не зарегистрирован
     * @param <S> тип render state
     */
    public <S> Optional<RenderPlan> renderPlanFor(String widgetType,
                                                   Class<S> stateType,
                                                   S renderState,
                                                   WidgetState widgetState) {
        return renderPlanFor(widgetType, "", List.of(), stateType, renderState, widgetState);
    }


    /**
     * Строит RenderPlan для конкретного виджета с учётом style id и class'ов.
     *
     * @param widgetType тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses style classes виджета
     * @param stateType Java-тип render state
     * @param renderState snapshot состояния виджета
     * @param widgetState текущее visual state виджета
     * @return план рендера или empty, если builder не зарегистрирован
     * @param <S> тип render state
     */
    public <S> Optional<RenderPlan> renderPlanFor(String widgetType,
                                                   String widgetStyleId,
                                                   Collection<String> styleClasses,
                                                   Class<S> stateType,
                                                   S renderState,
                                                   WidgetState widgetState) {
        Style style = resolveStyleFor(widgetType, widgetStyleId, styleClasses);
        return StyleRenderPlanRegistry.global().plan(widgetType, stateType, renderState, style, widgetState);
    }

    /**
     * Возвращает все matching style definitions в порядке применения.
     *
     * @param widgetType тип виджета
     * @param widgetStyleId явный style id виджета
     * @param styleClasses style classes виджета
     * @return definitions от меньшего приоритета к большему
     */
    public List<StyleDefinition> matchingDefinitions(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        String normalizedWidgetType = normalizeOptional(widgetType, "");
        String normalizedStyleId = normalizeOptional(widgetStyleId, "");
        List<StyleDefinition> result = new ArrayList<>();
        Set<String> added = new LinkedHashSet<>();

        addUnique(result, added, definitionByBindingOrType(normalizedWidgetType).orElse(null));

        List<StyleDefinition> selectorMatches = new ArrayList<>();
        for (StyleDefinition definition : styles.values()) {
            if (definition.selector().matches(normalizedWidgetType, normalizedStyleId, styleClasses)) {
                selectorMatches.add(definition);
            }
        }
        selectorMatches.sort(Comparator.comparingInt(definition -> definition.selector().specificity()));
        for (StyleDefinition definition : selectorMatches) {
            addUnique(result, added, definition);
        }

        if (!normalizedStyleId.isEmpty()) {
            addUnique(result, added, styles.get(normalizedStyleId));
        }
        return List.copyOf(result);
    }

    @Override
    public long version() {
        long result = version + fallback.version();
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
