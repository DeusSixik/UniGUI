package dev.sixik.unigui.api.xml;

import java.util.List;

/** Видимый в редакторе статус успешного или неуспешного XML-binding подключения. */
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

    public static XmlBindingStatus active(String path, Class<?> valueType) {
        return new XmlBindingStatus(State.ACTIVE, path, valueType, valueType, List.of());
    }

    public static XmlBindingStatus active(String path, Class<?> expectedType, Class<?> actualType) {
        return new XmlBindingStatus(State.ACTIVE, path, expectedType, actualType, List.of());
    }

    public static XmlBindingStatus closed(String path, Class<?> valueType) {
        return new XmlBindingStatus(State.CLOSED, path, valueType, valueType, List.of());
    }

    public static XmlBindingStatus missing(String path, Class<?> expectedType) {
        return error(path, expectedType, null,
                "Missing XML binding value '" + normalizePath(path) + "'");
    }

    public static XmlBindingStatus typeMismatch(String path, Class<?> expectedType, Class<?> actualType) {
        return error(path, expectedType, actualType,
                "XML binding value '" + normalizePath(path) + "' expected " + typeName(expectedType)
                        + ", got " + typeName(actualType));
    }

    public static XmlBindingStatus error(String path, Class<?> expectedType, Class<?> actualType, String message) {
        return new XmlBindingStatus(State.ERROR, path, expectedType, actualType,
                List.of(new XmlWidgetDiagnostic(message == null ? "XML binding failed" : message)));
    }

    public State state() {
        return state;
    }

    public String path() {
        return path;
    }

    public Class<?> expectedType() {
        return expectedType;
    }

    public Class<?> actualType() {
        return actualType;
    }

    public List<XmlWidgetDiagnostic> diagnostics() {
        return diagnostics;
    }

    public boolean active() {
        return state == State.ACTIVE;
    }

    public boolean valid() {
        return state != State.ERROR;
    }

    public boolean hasDiagnostics() {
        return !diagnostics.isEmpty();
    }

    public String summary() {
        if (hasDiagnostics()) return diagnostics.get(0).message();
        if (state == State.CLOSED) return "XML binding '" + path + "' is closed.";
        return "XML binding '" + path + "' is active.";
    }

    @Override
    public String toString() {
        return summary();
    }

    public enum State {
        ACTIVE,
        CLOSED,
        ERROR
    }

    static String normalizePath(String path) {
        return path == null ? "" : path.trim();
    }

    static String typeName(Class<?> type) {
        return type == null ? "<missing>" : type.getSimpleName();
    }
}
