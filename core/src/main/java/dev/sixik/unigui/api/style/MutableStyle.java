package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Изменяемая реализация {@link Style}.
 *
 * <p>Класс используется в Java builders, XML loader'е и editor tooling, где стиль нужно собирать
 * постепенно: добавлять свойства, state override'ы и renderer override. Все значения проверяются
 * по типу через {@link StyleKey#type()}, поэтому ошибка в значении обнаруживается в момент записи,
 * а не во время рендера виджета.</p>
 *
 * @see StyleKey
 * @see WidgetState
 */
@AiDoc(
        summary = "Fluent mutable implementation used to build typed styles and state overrides.",
        purpose = "Builds or edits a style in Java while validating each value against its `StyleKey` type.",
        useWhen = {
                "Create a reusable style for a `StyleDefinition`, theme, or widget.",
                "Override a property for hover, pressed, selected, disabled, or another widget state."
        },
        criticalRules = {
                "Use `put(key, value)` for `NORMAL`; use the state overload only for an override.",
                "The value must be null or an instance of `key.type()`, otherwise `put` throws `IllegalArgumentException`.",
                "Every write increments `version`; use `putAll` instead of mutating maps returned by `values`.",
                "A renderer set directly on a widget still has priority over a renderer stored in its style."
        },
        related = {"Style", "StyleKeys", "WidgetState", "StyleDefinition", "WidgetRenderRegistry"},
        examples = @AiDoc.Example(
                title = "Build state-aware colors",
                code = """
                        Style buttonStyle = new MutableStyle()
                                .put(StyleKeys.BACKGROUND_COLOR, normalColor)
                                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED, hoverColor)
                                .put(StyleKeys.TEXT_COLOR, textColor);
                        """))
@WikiPage(
        title = "MutableStyle",
        slug = "mutable-style",
        category = "<key:style.category>",
        summary = "<key:mutable.style.summary>",
        order = 20)
@WikiSection(title = "<key:mutable.style.section.overview.title>",
        markdown = "<key:mutable.style.section.overview.body>", order = 10)
@WikiExample(title = "<key:mutable.style.example.states.title>",
        code = "<key:mutable.style.example.states.code>", order = 10)
public final class MutableStyle implements Style {
    private final EnumMap<WidgetState, Map<StyleKey<?>, Object>> values = new EnumMap<>(WidgetState.class);
    private long version;

    /**
     * Записывает значение свойства для обычного состояния {@link WidgetState#NORMAL}.
     *
     * @param key типизированный ключ свойства
     * @param value значение свойства или {@code null}
     * @return этот стиль для fluent-настройки
     * @param <T> Java-тип свойства
     */
    @AiDoc(section = "Properties", sectionOrder = 10, order = 10,
            summary = "Stores a typed property for the `NORMAL` widget state.")
    @WikiMethod(title = "put(StyleKey, T)", summary = "<key:mutable.style.method.put.summary>",
            section = "<key:mutable.style.api.section.properties>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:mutable.style.method.fluent.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:mutable.style.method.put.type_throws>")
    public <T> MutableStyle put(
            @WikiParameter(name = "key", description = "<key:mutable.style.method.put.key>") StyleKey<T> key,
            @WikiParameter(name = "value", description = "<key:mutable.style.method.put.value>") T value) {
        return put(key, WidgetState.NORMAL, value);
    }

    /**
     * Записывает значение свойства для конкретного состояния виджета.
     *
     * @param key типизированный ключ свойства
     * @param state состояние, для которого действует значение
     * @param value значение свойства или {@code null}
     * @return этот стиль для fluent-настройки
     * @param <T> Java-тип свойства
     */
    @AiDoc(section = "Properties", sectionOrder = 10, order = 20,
            summary = "Stores a typed property override for one widget state.")
    @WikiMethod(title = "put(StyleKey, WidgetState, T)", summary = "<key:mutable.style.method.put_state.summary>",
            section = "<key:mutable.style.api.section.properties>", sectionOrder = 10, order = 20)
    @WikiReturn("<key:mutable.style.method.fluent.return>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:mutable.style.method.put.type_throws>")
    public <T> MutableStyle put(
            @WikiParameter(name = "key", description = "<key:mutable.style.method.put.key>") StyleKey<T> key,
            @WikiParameter(name = "state", description = "<key:mutable.style.method.put_state.state>") WidgetState state,
            @WikiParameter(name = "value", description = "<key:mutable.style.method.put.value>") T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        if (value != null && !key.type().isInstance(value)) {
            throw new IllegalArgumentException("Style value for " + key.id() + " must be " + key.type().getName());
        }
        values.computeIfAbsent(state, ignored -> new Object2ObjectOpenHashMap<>()).put(key, value);
        version++;
        return this;
    }

    /**
     * Удаляет значение свойства из обычного состояния.
     *
     * @param key ключ свойства
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle remove(StyleKey<?> key) {
        return remove(key, WidgetState.NORMAL);
    }

    /**
     * Удаляет значение свойства из конкретного состояния.
     *
     * @param key ключ свойства
     * @param state состояние, из которого нужно удалить значение
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle remove(StyleKey<?> key, WidgetState state) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        Map<StyleKey<?>, Object> stateValues = values.get(state);
        if (stateValues == null || !stateValues.containsKey(key)) return this;
        stateValues.remove(key);
        if (stateValues.isEmpty()) {
            values.remove(state);
        }
        version++;
        return this;
    }

    /**
     * Добавляет все значения другого стиля, сохраняя записи для отдельных состояний.
     *
     * @param source исходный стиль; null и пустой стиль игнорируются
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Composition", sectionOrder = 20, order = 10,
            summary = "Copies all normal and state-specific values from another style.")
    @WikiMethod(title = "putAll(Style)", summary = "<key:mutable.style.method.put_all.summary>",
            section = "<key:mutable.style.api.section.composition>", sectionOrder = 20, order = 10)
    @WikiReturn("<key:mutable.style.method.fluent.return>")
    public MutableStyle putAll(
            @WikiParameter(name = "source", description = "<key:mutable.style.method.put_all.source>") Style source) {
        if (source == null || source == Style.EMPTY) return this;
        for (Map.Entry<WidgetState, Map<StyleKey<?>, Object>> stateEntry : source.values().entrySet()) {
            for (Map.Entry<StyleKey<?>, Object> entry : stateEntry.getValue().entrySet()) {
                putUnchecked(entry.getKey(), stateEntry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    /**
     * Задаёт renderer override для виджетов, использующих этот стиль.
     *
     * <p>Конкретный виджет приводит значение к своему renderer-интерфейсу. Локальный renderer,
     * назначенный прямо на instance виджета, остаётся приоритетнее этого style-значения.</p>
     *
     * @param renderer renderer-объект, совместимый с конкретным типом виджета
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle renderer(Object renderer) {
        return put(StyleKeys.RENDERER, renderer);
    }

    /**
     * Задаёт renderer override по id из {@link dev.sixik.unigui.api.widget.render.WidgetRenderRegistry}.
     *
     * <p>Этот вариант нужен для декларативных StylePack/XML стилей, где нельзя хранить Java-объект,
     * но можно сослаться на renderer по стабильной строке.</p>
     *
     * @param rendererId id renderer'а в registry
     * @return этот стиль для fluent-настройки
     */
    @AiDoc(section = "Rendering", sectionOrder = 30, order = 10,
            summary = "Selects a renderer registered by stable string id.")
    @WikiMethod(title = "rendererId(String)", summary = "<key:mutable.style.method.renderer_id.summary>",
            section = "<key:mutable.style.api.section.rendering>", sectionOrder = 30, order = 10)
    @WikiReturn("<key:mutable.style.method.fluent.return>")
    public MutableStyle rendererId(
            @WikiParameter(name = "rendererId", description = "<key:mutable.style.method.renderer_id.id>") String rendererId) {
        return put(StyleKeys.RENDERER, rendererId == null ? "" : rendererId.trim());
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        Objects.requireNonNull(key, "key");
        WidgetState normalizedState = state == null ? WidgetState.NORMAL : state;
        T value = lookup(key, normalizedState);
        if (value != null || normalizedState == WidgetState.NORMAL) {
            return value == null ? fallback : value;
        }
        value = lookup(key, WidgetState.NORMAL);
        return value == null ? fallback : value;
    }

    @Override
    public Map<WidgetState, Map<StyleKey<?>, Object>> values() {
        if (values.isEmpty()) return Map.of();
        EnumMap<WidgetState, Map<StyleKey<?>, Object>> snapshot = new EnumMap<>(WidgetState.class);
        for (Map.Entry<WidgetState, Map<StyleKey<?>, Object>> entry : values.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                snapshot.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private <T> T lookup(StyleKey<T> key, WidgetState state) {
        Map<StyleKey<?>, Object> stateValues = values.get(state);
        if (stateValues == null) return null;
        Object value = stateValues.get(key);
        if (value == null) return null;
        return key.type().cast(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void putUnchecked(StyleKey<?> key, WidgetState state, Object value) {
        put((StyleKey) key, state, value);
    }
}
