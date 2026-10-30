package dev.sixik.unigui.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads Minecraft-style flat language JSON files and resolves {@code <key:...>} references. */
final class WikiLocalization {
    static final String DEFAULT_LOCALE = "en";
    private static final Pattern KEY_REFERENCE = Pattern.compile("<key:([A-Za-z0-9_.-]+)>");

    private final Map<String, Map<String, String>> translations;
    private final Consumer<String> warningSink;
    private final Set<String> warnedFallbacks = new HashSet<>();

    private WikiLocalization(Map<String, Map<String, String>> translations, Consumer<String> warningSink) {
        this.translations = translations;
        this.warningSink = warningSink;
    }

    static WikiLocalization literalOnly() {
        return new WikiLocalization(Map.of(DEFAULT_LOCALE, Map.of(
                "wiki.index.title", "UniGUI Wiki",
                "wiki.links.title", "Related links",
                "wiki.api.title", "API",
                "wiki.api.parameter", "Parameter",
                "wiki.api.type", "Type",
                "wiki.api.description", "Description",
                "wiki.api.returns", "Returns",
                "wiki.api.throws", "Throws")), ignored -> { });
    }

    static WikiLocalization load(Path directory, Consumer<String> warningSink) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("Wiki language directory does not exist: " + directory.toAbsolutePath());
        }
        Map<String, Map<String, String>> translations = new LinkedHashMap<>();
        try (var files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList()) {
                String name = file.getFileName().toString();
                String locale = name.substring(0, name.length() - ".json".length());
                if (!locale.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
                    throw new IllegalStateException("Invalid wiki locale filename: " + name);
                }
                translations.put(locale, parseFlatJson(Files.readString(file, StandardCharsets.UTF_8), file));
            }
        }
        if (!translations.containsKey(DEFAULT_LOCALE)) {
            throw new IllegalStateException("Missing default wiki language file: "
                    + directory.resolve(DEFAULT_LOCALE + ".json").toAbsolutePath());
        }
        return new WikiLocalization(Map.copyOf(translations), warningSink == null ? ignored -> { } : warningSink);
    }

    List<String> locales() {
        List<String> locales = new ArrayList<>(translations.keySet());
        locales.sort((left, right) -> {
            if (left.equals(DEFAULT_LOCALE)) return right.equals(DEFAULT_LOCALE) ? 0 : -1;
            if (right.equals(DEFAULT_LOCALE)) return 1;
            return left.compareTo(right);
        });
        return List.copyOf(locales);
    }

    String resolve(String value, String locale, String context) {
        if (value == null || value.isEmpty()) return value == null ? "" : value;
        Matcher matcher = KEY_REFERENCE.matcher(value);
        StringBuilder result = new StringBuilder(value.length());
        while (matcher.find()) {
            String key = matcher.group(1);
            String english = translations.get(DEFAULT_LOCALE).get(key);
            if (english == null) {
                throw new MissingWikiTranslationException(key, DEFAULT_LOCALE, context);
            }
            Map<String, String> localeValues = translations.get(locale);
            String replacement = localeValues == null ? null : localeValues.get(key);
            if (replacement == null) {
                if (warnedFallbacks.add(locale + '\0' + key)) {
                    warningSink.accept("Missing wiki translation '" + key + "' for locale '" + locale
                            + "' at " + context + "; using English fallback.");
                }
                replacement = english;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static Map<String, String> parseFlatJson(String source, Path file) {
        JsonCursor cursor = new JsonCursor(source, file);
        return cursor.parseObject();
    }

    static final class MissingWikiTranslationException extends IllegalStateException {
        MissingWikiTranslationException(String key, String locale, String context) {
            super("Missing required wiki translation '" + key + "' in locale '" + locale + "' at " + context);
        }
    }

    private static final class JsonCursor {
        private final String source;
        private final Path file;
        private int index;

        private JsonCursor(String source, Path file) {
            this.source = source == null ? "" : source;
            this.file = file;
        }

        private Map<String, String> parseObject() {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            whitespace();
            expect('{');
            whitespace();
            if (take('}')) return Map.of();
            while (true) {
                String key = string();
                whitespace();
                expect(':');
                whitespace();
                String value = string();
                if (result.putIfAbsent(key, value) != null) error("Duplicate key '" + key + "'");
                whitespace();
                if (take('}')) break;
                expect(',');
                whitespace();
            }
            whitespace();
            if (index != source.length()) error("Unexpected content after JSON object");
            return Map.copyOf(result);
        }

        private String string() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (index < source.length()) {
                char character = source.charAt(index++);
                if (character == '"') return value.toString();
                if (character != '\\') {
                    if (character < 0x20) error("Control character in string");
                    value.append(character);
                    continue;
                }
                if (index >= source.length()) error("Incomplete escape sequence");
                char escape = source.charAt(index++);
                switch (escape) {
                    case '"', '\\', '/' -> value.append(escape);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(unicode());
                    default -> error("Unsupported escape sequence \\" + escape + "'");
                }
            }
            error("Unterminated string");
            return "";
        }

        private char unicode() {
            if (index + 4 > source.length()) error("Incomplete unicode escape");
            String digits = source.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(digits, 16);
            } catch (NumberFormatException exception) {
                error("Invalid unicode escape '" + digits + "'");
                return 0;
            }
        }

        private void whitespace() {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) index++;
        }

        private boolean take(char expected) {
            if (index < source.length() && source.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) error("Expected '" + expected + "'");
        }

        private void error(String message) {
            throw new IllegalArgumentException(message + " in " + file + " at character " + index);
        }
    }
}
