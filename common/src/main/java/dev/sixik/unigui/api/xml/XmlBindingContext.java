package dev.sixik.unigui.api.xml;

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
 * XmlBindingContext context = XmlBindingContext.empty();
 * XmlMutableObservableValue<String> title = context.mutable("screen.title", String.class, "Home");
 * XmlBinding<String> binding = context.bind("screen.title", String.class, label::text);
 * title.set("Settings");
 * }</pre>
 */
public final class XmlBindingContext {
    private final Map<String, XmlObservableValue<?>> values = new LinkedHashMap<>();

    /**
     * Создаёт пустой binding context.
     *
     * @return новый context без зарегистрированных значений
     */
    public static XmlBindingContext empty() {
        return new XmlBindingContext();
    }

    /**
     * Создаёт mutable observable и сразу регистрирует его в context.
     *
     * @param path имя/path значения
     * @param valueType runtime-тип значения
     * @param value начальное значение
     * @param <T> тип значения
     * @return созданный observable, которым можно дальше управлять из code-behind
     */
    public <T> XmlMutableObservableValue<T> mutable(String path, Class<T> valueType, T value) {
        XmlMutableObservableValue<T> observable = XmlMutableObservableValue.of(path, valueType, value);
        value(path, observable);
        return observable;
    }

    /**
     * Регистрирует готовый observable source.
     *
     * @param path имя/path, по которому source будет доступен
     * @param value observable source
     * @return этот context для chained-настройки
     */
    public XmlBindingContext value(String path, XmlObservableValue<?> value) {
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
    public Optional<XmlObservableValue<?>> value(String path) {
        return Optional.ofNullable(values.get(normalizePath(path)));
    }

    /**
     * Возвращает observable source только если он существует и совместим с ожидаемым типом.
     *
     * @param path имя/path значения
     * @param expectedType ожидаемый runtime-тип
     * @param <T> тип значения
     * @return typed observable или empty при missing/type mismatch
     */
    public <T> Optional<XmlObservableValue<T>> resolve(String path, Class<T> expectedType) {
        if (!status(path, expectedType).active()) return Optional.empty();
        @SuppressWarnings("unchecked")
        XmlObservableValue<T> observable = (XmlObservableValue<T>) values.get(normalizePath(path));
        return Optional.of(observable);
    }

    /**
     * Проверяет статус binding source без создания активной подписки.
     *
     * @param path имя/path значения
     * @param expectedType ожидаемый runtime-тип
     * @return active, missing или type-mismatch статус
     */
    public XmlBindingStatus status(String path, Class<?> expectedType) {
        if (expectedType == null) throw new IllegalArgumentException("XML binding expected type must not be null");
        String normalized = normalizePath(path);
        XmlObservableValue<?> value = values.get(normalized);
        if (value == null) return XmlBindingStatus.missing(normalized, expectedType);
        if (!expectedType.isAssignableFrom(value.valueType())) {
            return XmlBindingStatus.typeMismatch(normalized, expectedType, value.valueType());
        }
        return XmlBindingStatus.active(normalized, expectedType, value.valueType());
    }

    /**
     * Привязывает observable source к setter-у/consumer'у target-а.
     *
     * <p>Если source найден и тип совместим, target сразу получает текущее значение,
     * а затем будет получать все изменения до {@link XmlBinding#close()}.</p>
     *
     * @param path имя/path значения
     * @param targetType ожидаемый тип target-а
     * @param target consumer, принимающий текущее и будущие значения
     * @param <T> тип target-значения
     * @return active binding или inactive binding со статусом ошибки
     */
    public <T> XmlBinding<T> bind(String path, Class<T> targetType, Consumer<? super T> target) {
        if (targetType == null) throw new IllegalArgumentException("XML binding target type must not be null");
        if (target == null) throw new IllegalArgumentException("XML binding target must not be null");
        String normalized = normalizePath(path);
        XmlObservableValue<?> value = values.get(normalized);
        if (value == null) return XmlBinding.inactive(normalized, targetType, XmlBindingStatus.missing(normalized, targetType));
        return XmlBinding.bind(value, targetType, target, normalized);
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
    public List<XmlBindingStatus> statuses() {
        return values.entrySet().stream()
                .map(entry -> XmlBindingStatus.active(entry.getKey(), entry.getValue().valueType()))
                .toList();
    }

    private static String requirePath(String path) {
        String normalized = normalizePath(path);
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML binding path must not be blank");
        return normalized;
    }

    private static String normalizePath(String path) {
        return XmlBindingStatus.normalizePath(path);
    }
}
