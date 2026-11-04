package dev.sixik.unigui.xaml.format;

import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Разбор и запись значений атрибутов: литералов и markup extension вида
 * {@code {Binding Path=Health, Mode=OneWay}}.
 *
 * <p>Грамматика близка к WPF: {@code {Имя арг1, арг2, Ключ=значение}}. Значение — вложенное расширение
 * {@code {...}}, строка в одинарных или двойных кавычках (экранирование {@code \}) либо «голый» текст до
 * ближайшей запятой или закрывающей скобки. Литерал, который должен начинаться с {@code '{'}, записывается
 * с префиксом {@code {}}: {@code {}{не расширение}}.</p>
 */
public final class MarkupExtensions {
    /** Разрешает префикс namespace в URI во время разбора имён расширений. */
    @FunctionalInterface
    public interface PrefixResolver {
        /**
         * Возвращает URI для префикса.
         *
         * @param prefix префикс; пустая строка — namespace по умолчанию
         * @return URI или {@code null}, если префикс не объявлен
         */
        String resolve(String prefix);
    }

    private MarkupExtensions() {
    }

    /**
     * Показывает, что значение выглядит как markup extension.
     *
     * @param raw исходное значение атрибута
     * @return {@code true}, если после пробелов идёт {@code '{'}, но не {@code "{}"}
     */
    public static boolean isExtension(String raw) {
        if (raw == null) return false;
        int start = firstNonSpace(raw);
        return start < raw.length() && raw.charAt(start) == '{' && !raw.startsWith("{}", start);
    }

    /**
     * Разбирает значение атрибута.
     *
     * @param raw      исходное значение
     * @param resolver разрешитель префиксов или {@code null} (тогда namespace расширений пустой)
     * @return {@link UiValue.Literal} либо {@link UiValue.Extension}
     * @throws IllegalArgumentException если расширение записано некорректно
     */
    public static UiValue parse(String raw, PrefixResolver resolver) {
        if (raw == null) return new UiValue.Literal("");
        int start = firstNonSpace(raw);
        if (start >= raw.length() || raw.charAt(start) != '{') return new UiValue.Literal(raw);
        if (raw.startsWith("{}", start)) return new UiValue.Literal(raw.substring(start + 2));

        int end = raw.length();
        while (end > start && Character.isWhitespace(raw.charAt(end - 1))) end--;
        if (raw.charAt(end - 1) != '}') {
            throw new IllegalArgumentException("Markup extension must end with '}'");
        }
        Cursor cursor = new Cursor(raw, start, end, resolver);
        UiValue.Extension extension = cursor.extension();
        if (cursor.pos != end) {
            throw cursor.error("Unexpected characters after the markup extension");
        }
        return extension;
    }

    /**
     * Записывает значение в текст атрибута.
     *
     * @param value литерал или расширение
     * @return текст, который {@link #parse(String, PrefixResolver)} разберёт в то же значение
     * @throws IllegalArgumentException если значение — вложенные элементы
     */
    public static String write(UiValue value) {
        if (value instanceof UiValue.Literal literal) {
            String text = literal.text();
            return firstNonSpace(text) < text.length() && text.charAt(firstNonSpace(text)) == '{' ? "{}" + text : text;
        }
        if (value instanceof UiValue.Extension extension) return writeExtension(extension);
        throw new IllegalArgumentException("Element content cannot be written as an attribute value");
    }

    private static String writeExtension(UiValue.Extension extension) {
        StringBuilder builder = new StringBuilder("{").append(extension.name().qualified());
        boolean first = true;
        for (UiValue argument : extension.positional()) {
            builder.append(first ? " " : ", ").append(writeArgument(argument));
            first = false;
        }
        for (Map.Entry<String, UiValue> entry : extension.named().entrySet()) {
            builder.append(first ? " " : ", ").append(entry.getKey()).append('=').append(writeArgument(entry.getValue()));
            first = false;
        }
        return builder.append('}').toString();
    }

    private static String writeArgument(UiValue value) {
        if (value instanceof UiValue.Extension extension) return writeExtension(extension);
        if (!(value instanceof UiValue.Literal literal)) {
            throw new IllegalArgumentException("Element content cannot be a markup extension argument");
        }
        String text = literal.text();
        if (!needsQuotes(text)) return text;
        StringBuilder quoted = new StringBuilder("'");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '\'') quoted.append('\\');
            quoted.append(c);
        }
        return quoted.append('\'').toString();
    }

    private static boolean needsQuotes(String text) {
        if (text.isEmpty() || Character.isWhitespace(text.charAt(0))
                || Character.isWhitespace(text.charAt(text.length() - 1))) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case ',', '{', '}', '=', '\\', '\'', '"' -> {
                    return true;
                }
                default -> {
                }
            }
        }
        return false;
    }

    private static int firstNonSpace(String text) {
        int i = 0;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        return i;
    }

    /** Позиционный разбор одного значения. */
    private static final class Cursor {
        private final String text;
        private final int end;
        private final PrefixResolver resolver;
        private int pos;

        private Cursor(String text, int start, int end, PrefixResolver resolver) {
            this.text = text;
            this.pos = start;
            this.end = end;
            this.resolver = resolver;
        }

        private UiValue.Extension extension() {
            expect('{');
            skipSpaces();
            String rawName = readUntil(true, true, true);
            if (rawName.isEmpty()) throw error("Markup extension name expected");
            UiName name = resolveName(rawName);

            List<UiValue> positional = new ArrayList<>();
            Map<String, UiValue> named = new LinkedHashMap<>();
            while (true) {
                skipSpaces();
                if (pos >= end) throw error("Unterminated markup extension, expected '}'");
                if (text.charAt(pos) == '}') {
                    pos++;
                    break;
                }
                argument(positional, named);
                skipSpaces();
                if (pos >= end) throw error("Unterminated markup extension, expected '}'");
                char next = text.charAt(pos);
                if (next == ',') {
                    pos++;
                } else if (next != '}') {
                    throw error("Expected ',' or '}'");
                }
            }
            return new UiValue.Extension(name, positional, named);
        }

        private void argument(List<UiValue> positional, Map<String, UiValue> named) {
            char first = text.charAt(pos);
            if (first == '{') {
                positional.add(extension());
                return;
            }
            if (first == '\'' || first == '"') {
                positional.add(new UiValue.Literal(quoted()));
                return;
            }
            String token = readUntil(false, true, true);
            if (pos < end && text.charAt(pos) == '=') {
                String key = token.trim();
                if (key.isEmpty()) throw error("Argument name expected before '='");
                pos++;
                named.put(key, value());
            } else {
                String value = token.trim();
                if (value.isEmpty()) throw error("Argument expected");
                positional.add(new UiValue.Literal(value));
            }
        }

        private UiValue value() {
            skipSpaces();
            if (pos >= end) throw error("Argument value expected");
            char first = text.charAt(pos);
            if (first == '{') return extension();
            if (first == '\'' || first == '"') return new UiValue.Literal(quoted());
            String value = readUntil(false, false, false).trim();
            if (value.isEmpty()) throw error("Argument value expected");
            return new UiValue.Literal(value);
        }

        private String quoted() {
            char quote = text.charAt(pos++);
            StringBuilder builder = new StringBuilder();
            while (pos < end) {
                char c = text.charAt(pos++);
                if (c == '\\' && pos < end) {
                    builder.append(text.charAt(pos++));
                } else if (c == quote) {
                    return builder.toString();
                } else {
                    builder.append(c);
                }
            }
            throw error("Unterminated quoted string");
        }

        /**
         * Читает до разделителя.
         *
         * @param stopAtSpace  остановиться на пробеле (для имени расширения)
         * @param stopAtEquals остановиться на {@code '='} (для ключа аргумента)
         * @param stopAtBrace  остановиться на {@code '{'} (вложенное расширение начинает новый аргумент)
         */
        private String readUntil(boolean stopAtSpace, boolean stopAtEquals, boolean stopAtBrace) {
            int begin = pos;
            while (pos < end) {
                char c = text.charAt(pos);
                if (c == ',' || c == '}') break;
                if (stopAtSpace && Character.isWhitespace(c)) break;
                if (stopAtEquals && c == '=') break;
                if (stopAtBrace && c == '{') break;
                pos++;
            }
            return text.substring(begin, pos);
        }

        private UiName resolveName(String raw) {
            int colon = raw.indexOf(':');
            if (colon < 0) return new UiName(resolve(""), raw, "");
            String prefix = raw.substring(0, colon);
            String local = raw.substring(colon + 1);
            if (prefix.isEmpty() || local.isEmpty()) throw error("Invalid markup extension name '" + raw + "'");
            String uri = resolve(prefix);
            if (uri == null) throw error("Unknown namespace prefix '" + prefix + "'");
            return new UiName(uri, local, prefix);
        }

        private String resolve(String prefix) {
            if (resolver == null) return "";
            String uri = resolver.resolve(prefix);
            return uri == null && prefix.isEmpty() ? "" : uri;
        }

        private void expect(char expected) {
            if (pos >= end || text.charAt(pos) != expected) throw error("Expected '" + expected + "'");
            pos++;
        }

        private void skipSpaces() {
            while (pos < end && Character.isWhitespace(text.charAt(pos))) pos++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " (offset " + pos + ")");
        }
    }
}
