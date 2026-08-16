package dev.sixik.unigui.api.xml;

import java.util.List;

/**
 * Видимый в редакторе статус успешного или неуспешного XML-binding подключения.
 *
 * <p>Статус не бросает исключения наружу, а описывает результат binding попытки как данные:
 * активная связь, закрытая связь или ошибка. Такой объект можно напрямую показывать в inspector,
 * diagnostics panel или логах hot-reload preview.</p>
 *
 * <p>Для ошибок статус хранит ожидаемый и фактический типы, path значения и список диагностик.
 * Это позволяет отличать отсутствие value source от type mismatch без парсинга текста сообщения.</p>
 */
public final class XmlBindingStatus {
    private final State state;
    private final String path;
    private final Class<?> expectedType;
    private final Class<?> actualType;
    private final List<XmlWidgetDiagnostic> diagnostics;

    private XmlBindingStatus(State state, String path, Class<?> expectedType, Class<?> actualType,
                             List<XmlWidgetDiagnostic> diagnostics) {
        this.state = state == null ? State.ERROR : state;
        this.path = normalizePath(path);
        this.expectedType = expectedType;
        this.actualType = actualType;
        this.diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    /**
     * Создаёт active status, когда ожидаемый и фактический тип совпадают.
     *
     * @param path binding path или имя значения
     * @param valueType runtime-тип observable value
     * @return статус активной связи
     */
    public static XmlBindingStatus active(String path, Class<?> valueType) {
        return new XmlBindingStatus(State.ACTIVE, path, valueType, valueType, List.of());
    }

    /**
     * Создаёт active status с отдельными expected/actual типами.
     *
     * <p>Метод нужен для случаев, когда source type assignable к target type, но не равен ему буквально.</p>
     *
     * @param path binding path или имя значения
     * @param expectedType тип, который запросил target
     * @param actualType фактический тип observable source
     * @return статус активной связи
     */
    public static XmlBindingStatus active(String path, Class<?> expectedType, Class<?> actualType) {
        return new XmlBindingStatus(State.ACTIVE, path, expectedType, actualType, List.of());
    }

    /**
     * Создаёт closed status для связи, которую уже отписали.
     *
     * @param path binding path или имя значения
     * @param valueType runtime-тип значения
     * @return статус закрытой связи
     */
    public static XmlBindingStatus closed(String path, Class<?> valueType) {
        return new XmlBindingStatus(State.CLOSED, path, valueType, valueType, List.of());
    }

    /**
     * Создаёт error status для отсутствующего observable value.
     *
     * @param path binding path, который не удалось найти
     * @param expectedType ожидаемый тип значения
     * @return статус ошибки с диагностикой missing value
     */
    public static XmlBindingStatus missing(String path, Class<?> expectedType) {
        return error(path, expectedType, null,
                "Missing XML binding value '" + normalizePath(path) + "'");
    }

    /**
     * Создаёт error status для несовместимого runtime-типа.
     *
     * @param path binding path или имя значения
     * @param expectedType тип, который запросил target
     * @param actualType фактический тип source
     * @return статус ошибки с диагностикой type mismatch
     */
    public static XmlBindingStatus typeMismatch(String path, Class<?> expectedType, Class<?> actualType) {
        return error(path, expectedType, actualType,
                "XML binding value '" + normalizePath(path) + "' expected " + typeName(expectedType)
                        + ", got " + typeName(actualType));
    }

    /**
     * Создаёт произвольный error status.
     *
     * @param path binding path или имя значения
     * @param expectedType ожидаемый тип target-а
     * @param actualType фактический тип source или {@code null}, если source отсутствует
     * @param message текст диагностики; {@code null} заменяется дефолтным сообщением
     * @return статус ошибки
     */
    public static XmlBindingStatus error(String path, Class<?> expectedType, Class<?> actualType, String message) {
        return new XmlBindingStatus(State.ERROR, path, expectedType, actualType,
                List.of(new XmlWidgetDiagnostic(message == null ? "XML binding failed" : message)));
    }

    /**
     * Возвращает машинно-читаемое состояние binding-а.
     *
     * @return active, closed или error
     */
    public State state() {
        return state;
    }

    /**
     * Возвращает нормализованный path значения.
     *
     * @return path без ведущих и хвостовых пробелов
     */
    public String path() {
        return path;
    }

    /**
     * Возвращает тип, который ожидал target callback.
     *
     * @return expected runtime type или {@code null}, если он не был известен
     */
    public Class<?> expectedType() {
        return expectedType;
    }

    /**
     * Возвращает фактический тип observable source.
     *
     * @return actual runtime type или {@code null}, если source отсутствует
     */
    public Class<?> actualType() {
        return actualType;
    }

    /**
     * Возвращает диагностики, связанные с binding-ом.
     *
     * @return immutable список диагностик; пустой список для успешных статусов
     */
    public List<XmlWidgetDiagnostic> diagnostics() {
        return diagnostics;
    }

    /**
     * Проверяет, что связь активна.
     *
     * @return {@code true}, если target подписан на observable source
     */
    public boolean active() {
        return state == State.ACTIVE;
    }

    /**
     * Проверяет, что статус не является ошибкой.
     *
     * @return {@code true} для active и closed статусов
     */
    public boolean valid() {
        return state != State.ERROR;
    }

    /**
     * Проверяет наличие диагностик.
     *
     * @return {@code true}, если список диагностик не пуст
     */
    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    /**
     * Возвращает короткий человекочитаемый текст статуса.
     *
     * @return первая диагностика для ошибок или стандартное сообщение active/closed
     */
    public String summary() {
        if (hasDiagnostics()) return diagnostics.get(0).message();
        if (state == State.CLOSED) return "XML binding '" + path + "' is closed.";
        return "XML binding '" + path + "' is active.";
    }

    /**
     * Возвращает {@link #summary()} как строковое представление статуса.
     *
     * @return summary статуса
     */
    @Override
    public String toString() {
        return summary();
    }

    /**
     * Состояние lifecycle-а XML binding-а.
     */
    public enum State {
        /** Binding успешно подписан на observable source. */
        ACTIVE,
        /** Binding был закрыт и больше не получает изменения. */
        CLOSED,
        /** Binding не удалось создать из-за отсутствия source, несовместимого типа или другой ошибки. */
        ERROR
    }

    static String normalizePath(String path) {
        return path == null ? "" : path.trim();
    }

    static String typeName(Class<?> type) {
        return type == null ? "<missing>" : type.getSimpleName();
    }
}
