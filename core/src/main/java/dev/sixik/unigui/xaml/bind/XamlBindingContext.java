package dev.sixik.unigui.xaml.bind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Реестр именованных observable-значений для XML-binding сценариев.
 *
 * <p>Контекст хранит значения по строковым path'ам и умеет проверять типы до
 * фактической привязки. Сейчас он используется code-behind/editor прототипами,
 * а в будущем может стать основой для полноценного binding expression синтаксиса.</p>
 *
 * <pre>{@code
 * XamlBindingContext context = XamlBindingContext.empty();
 * XamlMutableObservableValue<String> title = context.mutable("screen.title", String.class, "Home");
 * XamlBinding<String> binding = context.bind("screen.title", String.class, label::text);
 * title.set("Settings");
 * }</pre>
 */
public final class XamlBindingContext {
    private final Map<String, XamlObservableValue<?>> values = new LinkedHashMap<>();

    /**
     * Создаёт пустой binding context.
     *
     * @return новый context без зарегистрированных значений
     */
    public static XamlBindingContext empty() {
        return new XamlBindingContext();
    }

    /**
     * Создаёт mutable observable и сразу регистрирует его в context.
     *
     * @param path      имя/path значения
     * @param valueType runtime-тип значения
     * @param value     начальное значение
     * @param <T>       тип значения
     * @return созданный observable, которым можно дальше управлять из code-behind
     */
    public <T> XamlMutableObservableValue<T> mutable(String path, Class<T> valueType, T value) {
        XamlMutableObservableValue<T> observable = XamlMutableObservableValue.of(path, valueType, value);
        value(path, observable);
        return observable;
    }

    /**
     * Публикует список как источник данных для {@code items="{Binding path}"}.
     *
     * @param path путь
     * @param list список; для живых обновлений используйте {@link XamlObservableList}
     * @return этот контекст
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public XamlBindingContext list(String path, java.util.List<?> list) {
        return value(path, XamlMutableObservableValue.of(path, (Class) java.util.List.class, list));
    }

    /**
     * Регистрирует готовый observable source.
     *
     * @param path  имя/path, по которому source будет доступен
     * @param value observable source
     * @return этот context для chained-настройки
     */
    public XamlBindingContext value(String path, XamlObservableValue<?> value) {
        if (value == null) throw new IllegalArgumentException("XML binding value must not be null");
        values.put(requirePath(path), value);
        return this;
    }

    /**
     * Возвращает observable source без проверки типа.
     *
     * @param path имя/path значения
     * @return source, если он зарегистрирован
     */
    public Optional<XamlObservableValue<?>> value(String path) {
        return Optional.ofNullable(values.get(normalizePath(path)));
    }

    /**
     * Возвращает observable source только если он существует и совместим с ожидаемым типом.
     *
     * @param path         имя/path значения
     * @param expectedType ожидаемый runtime-тип
     * @param <T>          тип значения
     * @return typed observable или empty при missing/type mismatch
     */
    public <T> Optional<XamlObservableValue<T>> resolve(String path, Class<T> expectedType) {
        if (!status(path, expectedType).active()) return Optional.empty();
        @SuppressWarnings("unchecked")
        XamlObservableValue<T> observable = (XamlObservableValue<T>) values.get(normalizePath(path));
        return Optional.of(observable);
    }

    /**
     * Проверяет статус binding source без создания активной подписки.
     *
     * @param path         имя/path значения
     * @param expectedType ожидаемый runtime-тип
     * @return active, missing или type-mismatch статус
     */
    public XamlBindingStatus status(String path, Class<?> expectedType) {
        if (expectedType == null) throw new IllegalArgumentException("XML binding expected type must not be null");
        String normalized = normalizePath(path);
        XamlObservableValue<?> value = values.get(normalized);
        if (value == null) return XamlBindingStatus.missing(normalized, expectedType);
        if (!expectedType.isAssignableFrom(value.valueType())) {
            return XamlBindingStatus.typeMismatch(normalized, expectedType, value.valueType());
        }
        return XamlBindingStatus.active(normalized, expectedType, value.valueType());
    }

    /**
     * Привязывает observable source к setter-у/consumer'у target-а.
     *
     * <p>Если source найден и тип совместим, target сразу получает текущее значение,
     * а затем будет получать все изменения до {@link XamlBinding#close()}.</p>
     *
     * @param path       имя/path значения
     * @param targetType ожидаемый тип target-а
     * @param target     consumer, принимающий текущее и будущие значения
     * @param <T>        тип target-значения
     * @return active binding или inactive binding со статусом ошибки
     */
    public <T> XamlBinding<T> bind(String path, Class<T> targetType, Consumer<? super T> target) {
        return bind(path, targetType, target, path);
    }

    /**
     * Привязывает source к target-у с отдельным именем свойства для диагностики.
     *
     * @param path       имя observable source
     * @param targetType ожидаемый тип target-значения
     * @param target     consumer, принимающий текущее и будущие значения
     * @param targetName отображаемое имя свойства виджета
     * @param <T>        тип target-значения
     * @return active binding или inactive binding со статусом ошибки
     */
    public <T> XamlBinding<T> bind(String path, Class<T> targetType,
                                   Consumer<? super T> target, String targetName) {
        if (targetType == null) throw new IllegalArgumentException("XML binding target type must not be null");
        if (target == null) throw new IllegalArgumentException("XML binding target must not be null");
        String normalized = normalizePath(path);
        XamlObservableValue<?> value = values.get(normalized);
        String displayName = targetName == null || targetName.isBlank() ? normalized : targetName.trim();
        if (value == null)
            return XamlBinding.inactive(normalized, targetType, XamlBindingStatus.missing(normalized, targetType));
        return XamlBinding.bind(value, targetType, target, displayName);
    }

    /**
     * Возвращает зарегистрированные path'ы в порядке добавления.
     *
     * @return immutable snapshot path'ов
     */
    public List<String> paths() {
        return List.copyOf(values.keySet());
    }

    /**
     * Возвращает active-статусы всех зарегистрированных values.
     *
     * @return immutable snapshot статусов
     */
    public List<XamlBindingStatus> statuses() {
        return values.entrySet().stream()
                .map(entry -> XamlBindingStatus.active(entry.getKey(), entry.getValue().valueType()))
                .toList();
    }

    private static String requirePath(String path) {
        String normalized = normalizePath(path);
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML binding path must not be blank");
        return normalized;
    }

    private static String normalizePath(String path) {
        return XamlBindingStatus.normalizePath(path);
    }
}
