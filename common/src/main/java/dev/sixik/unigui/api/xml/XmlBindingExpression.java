package dev.sixik.unigui.api.xml;

import java.util.Optional;

/**
 * Разобранное однонаправленное выражение XML-binding.
 *
 * <p>В MVP поддерживается только форма {@code {Binding path}}. Класс не
 * выполняет разрешение path и не хранит ссылку на контекст данных.</p>
 *
 * @param path путь observable-значения в {@link XmlBindingContext}
 */
public record XmlBindingExpression(String path) {
    /**
     * Создаёт выражение и проверяет, что путь не пустой.
     *
     * @param path путь observable-значения; не должен быть пустым
     */
    public XmlBindingExpression {
        path = normalizePath(path);
        if (path.isEmpty()) {
            throw new IllegalArgumentException("XML binding path must not be blank");
        }
    }

    /**
     * Проверяет, является ли строка потенциальным binding-выражением.
     *
     * @param value исходное значение XML-атрибута
     * @return {@code true}, если строка начинается с маркера {@code {Binding}
     * и заканчивается фигурной скобкой
     */
    public static boolean looksLike(String value) {
        if (value == null) return false;
        String normalized = value.trim();
        return normalized.startsWith("{Binding") && normalized.endsWith("}");
    }

    /**
     * Разбирает binding-выражение, если оно присутствует.
     *
     * @param value исходное значение XML-атрибута
     * @return выражение или {@link Optional#empty()} для обычного литерала
     * @throws IllegalArgumentException если маркер binding есть, но выражение
     *                                  имеет неверный формат
     */
    public static Optional<XmlBindingExpression> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim();
        if (!normalized.startsWith("{Binding")) return Optional.empty();
        if (!normalized.endsWith("}")) {
            throw new IllegalArgumentException("XML binding expression must end with '}'");
        }
        String body = normalized.substring("{Binding".length(), normalized.length() - 1).trim();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("XML binding expression must contain a path");
        }
        if (body.indexOf(',') >= 0) {
            throw new IllegalArgumentException("XML binding options are not supported yet");
        }
        return Optional.of(new XmlBindingExpression(body));
    }

    private static String normalizePath(String path) {
        return path == null ? "" : path.trim();
    }
}
