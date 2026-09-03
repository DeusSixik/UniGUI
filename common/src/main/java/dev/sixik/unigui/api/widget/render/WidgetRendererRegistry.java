package dev.sixik.unigui.api.widget.render;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry для Java-renderer'ов, на которые может ссылаться декларативный {@code StylePack}.
 *
 * <p>Основной путь новой style-системы — декларативный {@code RenderPlan}, который можно
 * редактировать как данные. Но не каждый визуальный эффект удобно или возможно описать набором
 * style-свойств. Для таких случаев есть escape hatch: renderer регистрируется под строковым id,
 * а {@code StyleDefinition} хранит этот id через {@code renderer="..."} в XML или через
 * {@code StyleDefinition.custom(...)} в Java.</p>
 *
 * <p>Registry типизированный: вместе с id хранится renderer-интерфейс. Например, renderer кнопки
 * нужно регистрировать как {@code ButtonRenderer.class}. При отрисовке {@code Button} попросит
 * renderer именно этого типа; renderer другого типа будет проигнорирован и не сломает runtime.</p>
 *
 * <pre>{@code
 * WidgetRendererRegistry.global().register(
 *         "testmod:destiny/button",
 *         ButtonRenderer.class,
 *         DestinyLikeButtonRenders.DEFAULT);
 *
 * StyleDefinition destiny = StyleDefinition.custom(
 *         "button.destiny",
 *         "testmod:destiny/button",
 *         style)
 *         .target(Button.STYLE_TYPE);
 * }</pre>
 *
 * <p>Приоритеты renderer'ов у виджета обычно такие: per-instance renderer, renderer из стиля,
 * декларативный {@code RenderPlan}, затем дефолтный renderer из {@code WidgetsRender}.</p>
 *
 * @see dev.sixik.unigui.api.style.StyleDefinition#custom(String, String, dev.sixik.unigui.api.style.Style)
 * @see dev.sixik.unigui.api.style.StyleBackend.Custom
 */
public final class WidgetRendererRegistry {
    private static final WidgetRendererRegistry GLOBAL = new WidgetRendererRegistry();

    private final Map<String, RegisteredRenderer<?>> renderers = new ConcurrentHashMap<>();

    /**
     * Возвращает глобальный registry renderer'ов.
     *
     * <p>Этого достаточно для модов и обычного runtime. Отдельный instance можно создать вручную
     * для тестов или isolated tooling, но виджеты по умолчанию смотрят именно в global registry.</p>
     *
     * @return общий registry процесса
     */
    public static WidgetRendererRegistry global() {
        return GLOBAL;
    }

    /**
     * Регистрирует renderer под стабильным id.
     *
     * <p>Повторная регистрация того же id заменяет старый renderer. Это удобно для hot-reload
     * dev-сценариев, но production-коду лучше держать id стабильными и уникальными.</p>
     *
     * @param id строковый id renderer'а, например {@code testmod:destiny/button}
     * @param type интерфейс renderer'а, который ожидает конкретный виджет
     * @param renderer объект renderer'а
     * @return этот registry для fluent-настройки
     * @param <T> тип renderer-интерфейса
     */
    public <T> WidgetRendererRegistry register(String id, Class<T> type, T renderer) {
        return register(id, WidgetRole.UNSPECIFIED, type, renderer);
    }

    /**
     * Регистрирует renderer с явной семантической ролью.
     *
     * <p>Role не заменяет проверку Java-типа. Она дополнительно запрещает назначить,
     * например, renderer обычной кнопки checkbox-контролу. Старый overload выше оставлен
     * для legacy renderer'ов.</p>
     *
     * @param id строковый id renderer'а
     * @param role semantic role, для которой предназначен renderer
     * @param type интерфейс renderer'а
     * @param renderer объект renderer'а
     * @return этот registry для fluent-настройки
     * @param <T> тип renderer-интерфейса
     */
    public <T> WidgetRendererRegistry register(String id, WidgetRole role, Class<T> type, T renderer) {
        String normalized = normalizeRequired(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(renderer, "renderer");
        if (!type.isInstance(renderer)) {
            throw new IllegalArgumentException("Renderer '" + normalized + "' must be " + type.getName());
        }
        if (renderer instanceof WidgetRenderer<?> typedRenderer
                && !role.accepts(typedRenderer.role())) {
            throw new IllegalArgumentException("Renderer '" + normalized + "' has role "
                    + typedRenderer.role() + ", expected " + role);
        }
        renderers.put(normalized, new RegisteredRenderer<>(normalized, role, type, renderer));
        return this;
    }

    /**
     * Удаляет renderer из registry.
     *
     * @param id id renderer'а; пустой id игнорируется
     * @return этот registry для fluent-настройки
     */
    public WidgetRendererRegistry unregister(String id) {
        String normalized = normalize(id);
        if (!normalized.isEmpty()) {
            renderers.remove(normalized);
        }
        return this;
    }

    /**
     * Возвращает полное описание renderer'а без приведения типа.
     *
     * @param id id renderer'а
     * @return descriptor renderer'а или {@link Optional#empty()}
     */
    public Optional<RegisteredRenderer<?>> descriptor(String id) {
        return Optional.ofNullable(renderers.get(normalize(id)));
    }

    /**
     * Возвращает renderer только если он совместим с ожидаемым типом.
     *
     * @param id id renderer'а
     * @param type ожидаемый renderer-интерфейс
     * @return typed renderer или {@link Optional#empty()}
     * @param <T> тип renderer-интерфейса
     */
    public <T> Optional<T> renderer(String id, Class<T> type) {
        return renderer(id, WidgetRole.UNSPECIFIED, type);
    }

    /**
     * Возвращает renderer только если совпадают Java-тип и semantic role.
     *
     * @param id id renderer'а
     * @param role ожидаемая роль виджета
     * @param type ожидаемый renderer-интерфейс
     * @return совместимый renderer или {@link Optional#empty()}
     * @param <T> тип renderer-интерфейса
     */
    public <T> Optional<T> renderer(String id, WidgetRole role, Class<T> type) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(type, "type");
        RegisteredRenderer<?> descriptor = renderers.get(normalize(id));
        if (descriptor == null
                || !role.accepts(descriptor.role())
                || !type.isAssignableFrom(descriptor.type())) {
            return Optional.empty();
        }
        return Optional.of(type.cast(descriptor.renderer()));
    }

    /**
     * Разрешает значение style-key'а renderer в реальный renderer-объект.
     *
     * <p>{@code value} может быть уже готовым renderer-объектом или строковым id. Если значение
     * пустое, неизвестное или несовместимое с {@code type}, возвращается {@code fallback}.</p>
     *
     * @param type ожидаемый renderer-интерфейс
     * @param value renderer-объект или строковый renderer id
     * @param fallback fallback-значение
     * @return resolved renderer или {@code fallback}
     * @param <T> тип renderer-интерфейса
     */
    public <T> T resolve(Class<T> type, Object value, T fallback) {
        return resolve(WidgetRole.UNSPECIFIED, type, value, fallback);
    }

    /**
     * Разрешает renderer с проверкой semantic role.
     *
     * @param role ожидаемая роль виджета
     * @param type ожидаемый renderer-интерфейс
     * @param value renderer-объект или строковый id
     * @param fallback fallback-значение
     * @return совместимый renderer или {@code fallback}
     * @param <T> тип renderer-интерфейса
     */
    public <T> T resolve(WidgetRole role, Class<T> type, Object value, T fallback) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(type, "type");
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (value instanceof String id) {
            return renderer(id, role, type).orElse(fallback);
        }
        return fallback;
    }

    /**
     * Возвращает read-only snapshot всех зарегистрированных renderer'ов.
     *
     * @return snapshot descriptor'ов в текущем registry
     */
    public Collection<RegisteredRenderer<?>> descriptors() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(renderers).values());
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Описание одного renderer'а в registry.
     *
     * @param id строковый id renderer'а
     * @param role semantic role renderer'а
     * @param type renderer-интерфейс, с которым renderer был зарегистрирован
     * @param renderer renderer-объект
     * @param <T> тип renderer-интерфейса
     */
    public record RegisteredRenderer<T>(String id, WidgetRole role, Class<T> type, T renderer) {
        /** Совместимый конструктор для старого кода без semantic role. */
        public RegisteredRenderer(String id, Class<T> type, T renderer) {
            this(id, WidgetRole.UNSPECIFIED, type, renderer);
        }

        /** Нормализует id и проверяет обязательные поля descriptor'а. */
        public RegisteredRenderer {
            id = normalizeRequired(id, "id");
            role = Objects.requireNonNull(role, "role");
            type = Objects.requireNonNull(type, "type");
            renderer = Objects.requireNonNull(renderer, "renderer");
        }
    }
}
