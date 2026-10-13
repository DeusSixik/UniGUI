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
 * <p>Все renderer'ы реализуют единый {@link WidgetRender}: один интерфейс, один метод.
 * Semantic role отделяет, например, renderer обычной кнопки от checkbox-контрола.
 * Renderer без роли ({@link WidgetRole#UNSPECIFIED}) принимается для любой роли.</p>
 *
 * <pre>{@code
 * WidgetRenderRegistry.global().register(
 *         "testmod:destiny/button",
 *         WidgetRole.BUTTON,
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
public final class WidgetRenderRegistry {
    private static final WidgetRenderRegistry GLOBAL = new WidgetRenderRegistry();

    private final Map<String, RegisteredRenderer> renderers = new ConcurrentHashMap<>();

    /**
     * Возвращает глобальный registry renderer'ов.
     *
     * <p>Этого достаточно для модов и обычного runtime. Отдельный instance можно создать вручную
     * для тестов или isolated tooling, но виджеты по умолчанию смотрят именно в global registry.</p>
     *
     * @return общий registry процесса
     */
    public static WidgetRenderRegistry global() {
        return GLOBAL;
    }

    /**
     * Регистрирует renderer под стабильным id.
     *
     * <p>Повторная регистрация того же id заменяет старый renderer. Это удобно для hot-reload
     * dev-сценариев, но production-коду лучше держать id стабильными и уникальными.</p>
     *
     * @param id строковый id renderer'а, например {@code testmod:destiny/button}
     * @param renderer объект renderer'а
     * @return этот registry для fluent-настройки
     */
    public WidgetRenderRegistry register(String id, WidgetRender renderer) {
        return register(id, WidgetRole.UNSPECIFIED, renderer);
    }

    /**
     * Регистрирует renderer с явной семантической ролью.
     *
     * <p>Role запрещает назначить, например, renderer обычной кнопки checkbox-контролу.
     * Renderer без заявленной роли принимается для любой ожидаемой роли.</p>
     *
     * @param id строковый id renderer'а
     * @param role semantic role, для которой предназначен renderer
     * @param renderer объект renderer'а
     * @return этот registry для fluent-настройки
     */
    public WidgetRenderRegistry register(String id, WidgetRole role, WidgetRender renderer) {
        String normalized = normalizeRequired(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(renderer, "renderer");
        if (!role.accepts(renderer.role())) {
            throw new IllegalArgumentException("Renderer '" + normalized + "' has role "
                    + renderer.role() + ", expected " + role);
        }
        renderers.put(normalized, new RegisteredRenderer(normalized, role, renderer));
        return this;
    }

    /**
     * Удаляет renderer из registry.
     *
     * @param id id renderer'а; пустой id игнорируется
     * @return этот registry для fluent-настройки
     */
    public WidgetRenderRegistry unregister(String id) {
        String normalized = normalize(id);
        if (!normalized.isEmpty()) {
            renderers.remove(normalized);
        }
        return this;
    }

    /**
     * Возвращает полное описание renderer'а.
     *
     * @param id id renderer'а
     * @return descriptor renderer'а или {@link Optional#empty()}
     */
    public Optional<RegisteredRenderer> descriptor(String id) {
        return Optional.ofNullable(renderers.get(normalize(id)));
    }

    /**
     * Возвращает renderer по id.
     *
     * @param id id renderer'а
     * @return renderer или {@link Optional#empty()}
     */
    public Optional<WidgetRender> renderer(String id) {
        return renderer(id, WidgetRole.UNSPECIFIED);
    }

    /**
     * Возвращает renderer только если его роль совместима с ожидаемой.
     *
     * @param id id renderer'а
     * @param role ожидаемая роль виджета
     * @return совместимый renderer или {@link Optional#empty()}
     */
    public Optional<WidgetRender> renderer(String id, WidgetRole role) {
        Objects.requireNonNull(role, "role");
        RegisteredRenderer descriptor = renderers.get(normalize(id));
        if (descriptor == null || !role.accepts(descriptor.role())) {
            return Optional.empty();
        }
        return Optional.of(descriptor.renderer());
    }

    /**
     * Разрешает значение style-key'а renderer в реальный renderer-объект.
     *
     * <p>{@code value} может быть уже готовым renderer-объектом или строковым id. Если значение
     * пустое, неизвестное или несовместимое с ролью, возвращается {@code fallback}.</p>
     *
     * @param value renderer-объект или строковый renderer id
     * @param fallback fallback-значение
     * @return resolved renderer или {@code fallback}
     */
    public WidgetRender resolve(Object value, WidgetRender fallback) {
        return resolve(WidgetRole.UNSPECIFIED, value, fallback);
    }

    /**
     * Разрешает renderer с проверкой semantic role.
     *
     * @param role ожидаемая роль виджета
     * @param value renderer-объект или строковый id
     * @param fallback fallback-значение
     * @return совместимый renderer или {@code fallback}
     */
    public WidgetRender resolve(WidgetRole role, Object value, WidgetRender fallback) {
        Objects.requireNonNull(role, "role");
        if (value instanceof WidgetRender renderer && role.accepts(renderer.role())) {
            return renderer;
        }
        if (value instanceof String id) {
            return renderer(id, role).orElse(fallback);
        }
        return fallback;
    }

    /**
     * Возвращает read-only snapshot всех зарегистрированных renderer'ов.
     *
     * @return snapshot descriptor'ов в текущем registry
     */
    public Collection<RegisteredRenderer> descriptors() {
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
     * @param renderer renderer-объект
     */
    public record RegisteredRenderer(String id, WidgetRole role, WidgetRender renderer) {
        /** Совместимый конструктор для кода без semantic role. */
        public RegisteredRenderer(String id, WidgetRender renderer) {
            this(id, WidgetRole.UNSPECIFIED, renderer);
        }

        /** Нормализует id и проверяет обязательные поля descriptor'а. */
        public RegisteredRenderer {
            id = normalizeRequired(id, "id");
            role = Objects.requireNonNull(role, "role");
            renderer = Objects.requireNonNull(renderer, "renderer");
        }
    }
}
