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
 * Named collection of reusable styles and animation presets.
 *
 * <p>{@code StylePack} is also a {@link Theme}: existing widgets can consume it
 * through {@code UIContext.theme(...)} while editors can work with named entries,
 * bindings and event animation links as regular data.</p>
 */
public final class StylePack implements Theme {
    private String id;
    private final Map<String, StyleDefinition> styles = new LinkedHashMap<>();
    private final Map<String, String> widgetBindings = new LinkedHashMap<>();
    private final Map<String, StyleAnimationDefinition> animations = new LinkedHashMap<>();
    private Style fallback = Style.EMPTY;
    private String fallbackStyleId = "";
    private long version;

    public StylePack(String id) {
        this.id = normalizeOptional(id, "style-pack");
    }

    public static StylePack create(String id) {
        return new StylePack(id);
    }

    public String id() {
        return id;
    }

    public StylePack id(String id) {
        String normalized = normalizeOptional(id, "style-pack");
        if (this.id.equals(normalized)) return this;
        this.id = normalized;
        version++;
        return this;
    }

    public Style fallback() {
        Style fallbackStyle = styleById(fallbackStyleId);
        return fallbackStyle == null ? fallback : fallbackStyle;
    }

    public StylePack fallback(Style fallback) {
        this.fallback = fallback == null ? Style.EMPTY : fallback;
        this.fallbackStyleId = "";
        version++;
        return this;
    }

    public StylePack fallbackStyle(String styleId) {
        this.fallbackStyleId = normalizeOptional(styleId, "");
        version++;
        return this;
    }

    public Map<String, StyleDefinition> styles() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(styles));
    }

    public Optional<StyleDefinition> styleDefinition(String styleId) {
        return Optional.ofNullable(styles.get(normalizeOptional(styleId, "")));
    }

    public Optional<StyleDefinition> styleDefinitionFor(String widgetType) {
        return styleDefinitionFor(widgetType, "", List.of());
    }

    public Optional<StyleDefinition> styleDefinitionFor(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        List<StyleDefinition> definitions = matchingDefinitions(widgetType, widgetStyleId, styleClasses);
        return definitions.isEmpty() ? Optional.empty() : Optional.of(definitions.get(definitions.size() - 1));
    }

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

    public StylePack putStyle(String styleId, Style style) {
        return put(StyleDefinition.of(styleId, style));
    }

    public StylePack put(StyleDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        styles.put(definition.id(), definition);
        version++;
        return this;
    }

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

    public Map<String, String> widgetBindings() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(widgetBindings));
    }

    public String binding(String widgetType) {
        return widgetBindings.getOrDefault(normalizeOptional(widgetType, ""), "");
    }

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

    public StylePack unbind(String widgetType) {
        return bind(widgetType, "");
    }

    public Map<String, StyleAnimationDefinition> animations() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(animations));
    }

    public Optional<StyleAnimationDefinition> animation(String animationId) {
        return Optional.ofNullable(animations.get(normalizeOptional(animationId, "")));
    }

    public StylePack putAnimation(StyleAnimationDefinition animation) {
        Objects.requireNonNull(animation, "animation");
        animations.put(animation.id(), animation);
        version++;
        return this;
    }

    public StylePack removeAnimation(String animationId) {
        String normalized = normalizeOptional(animationId, "");
        if (normalized.isEmpty()) return this;
        if (animations.remove(normalized) != null) {
            version++;
        }
        return this;
    }

    public String eventAnimation(String styleId, String eventName) {
        return styleDefinition(styleId)
                .map(definition -> definition.eventAnimation(eventName))
                .orElse("");
    }

    public Style resolveStyleFor(String widgetType, String widgetStyleId, Collection<String> styleClasses) {
        List<Style> layers = new ArrayList<>();
        Style fallbackStyle = fallback();
        if (fallbackStyle != Style.EMPTY) layers.add(fallbackStyle);
        for (StyleDefinition definition : matchingDefinitions(widgetType, widgetStyleId, styleClasses)) {
            layers.add(definition.style());
        }
        return ResolvedStyle.of(layers);
    }

    public <S> Optional<RenderPlan> renderPlanFor(String widgetType,
                                                   Class<S> stateType,
                                                   S renderState,
                                                   WidgetState widgetState) {
        return renderPlanFor(widgetType, "", List.of(), stateType, renderState, widgetState);
    }

    public <S> Optional<RenderPlan> renderPlanFor(String widgetType,
                                                   String widgetStyleId,
                                                   Collection<String> styleClasses,
                                                   Class<S> stateType,
                                                   S renderState,
                                                   WidgetState widgetState) {
        Style style = resolveStyleFor(widgetType, widgetStyleId, styleClasses);
        return StyleRenderPlanRegistry.global().plan(widgetType, stateType, renderState, style, widgetState);
    }

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