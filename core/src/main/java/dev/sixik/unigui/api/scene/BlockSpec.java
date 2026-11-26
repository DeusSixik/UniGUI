package dev.sixik.unigui.api.scene;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Разобранная строка блока, общая для всех версий Minecraft.
 *
 * <p>Формат: {@code namespace:path[name=value,...]} или {@code namespace:path@meta}.</p>
 *
 * <ul>
 *     <li>Пространство имён можно опустить: {@code stone} читается как {@code minecraft:stone}.</li>
 *     <li>Свойства в квадратных скобках задают состояние блока на 1.12.2 и новее,
 *     например {@code minecraft:furnace[facing=north,lit=true]}.</li>
 *     <li>Метаданные после {@code @} нужны 1.7.10 и поддерживаются на 1.12.2,
 *     например {@code minecraft:wool@14}.</li>
 * </ul>
 *
 * <p>Синтаксис проверяется здесь, а существование блока и свойств проверяет {@link ScenePlatform} версии игры.</p>
 *
 * @param text       исходная строка без пробелов по краям.
 * @param id         идентификатор блока с пространством имён.
 * @param properties свойства в порядке записи, неизменяемая карта.
 * @param meta       метаданные или {@code -1}, если они не заданы.
 */
public record BlockSpec(String text, String id, Map<String, String> properties, int meta) {
    /**
     * Пространство имён для идентификатора без двоеточия.
     */
    private static final String DEFAULT_NAMESPACE = "minecraft";

    /**
     * Проверяет поля и делает карту свойств неизменяемой.
     *
     * @throws NullPointerException     если строка, идентификатор или карта равны null.
     * @throws IllegalArgumentException если метаданные меньше {@code -1}.
     */
    public BlockSpec {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(id, "id");
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(properties, "properties")));
        if (meta < -1) throw new IllegalArgumentException("Block metadata must be non-negative: " + meta);
    }

    /**
     * Разбирает строку блока.
     *
     * @param value строка вида {@code minecraft:furnace[facing=north]} или {@code minecraft:wool@14}.
     * @return разобранная строка.
     * @throws NullPointerException     если value равен null.
     * @throws IllegalArgumentException если синтаксис неверен.
     */
    public static BlockSpec parse(String value) {
        String text = Objects.requireNonNull(value, "value").trim();
        if (text.isEmpty()) throw invalid(value, "empty block id");

        int bracket = text.indexOf('[');
        int at = text.indexOf('@');
        int idEnd = text.length();
        if (bracket >= 0) idEnd = bracket;
        if (at >= 0 && at < idEnd) idEnd = at;
        String id = normalizeId(value, text.substring(0, idEnd));

        Map<String, String> properties = new LinkedHashMap<>();
        int cursor = idEnd;
        if (cursor < text.length() && text.charAt(cursor) == '[') {
            int close = text.indexOf(']', cursor);
            if (close < 0) throw invalid(value, "missing ']'");
            parseProperties(value, text.substring(cursor + 1, close), properties);
            cursor = close + 1;
        }

        int meta = -1;
        if (cursor < text.length() && text.charAt(cursor) == '@') {
            meta = parseMeta(value, text.substring(cursor + 1));
            cursor = text.length();
        }
        if (cursor != text.length()) throw invalid(value, "unexpected text after block id");
        if (meta >= 0 && !properties.isEmpty()) throw invalid(value, "use either [properties] or @meta");
        return new BlockSpec(text, id, properties, meta);
    }

    /**
     * Проверяет, заданы ли метаданные.
     *
     * @return true для строки с {@code @meta}.
     */
    public boolean hasMeta() {
        return meta >= 0;
    }

    /**
     * Возвращает исходную строку.
     *
     * @return {@link #text()}.
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Добавляет пространство имён по умолчанию и проверяет символы идентификатора.
     *
     * @param source исходная строка для сообщения об ошибке.
     * @param id     идентификатор до свойств и метаданных.
     * @return идентификатор с пространством имён.
     */
    private static String normalizeId(String source, String id) {
        String trimmed = id.trim();
        if (trimmed.isEmpty()) throw invalid(source, "empty block id");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isWhitespace(c) || c == ']' || c == '=' || c == ',') {
                throw invalid(source, "illegal character '" + c + "' in block id");
            }
        }
        int colon = trimmed.indexOf(':');
        if (colon != trimmed.lastIndexOf(':')) throw invalid(source, "block id has more than one ':'");
        if (colon == 0 || colon == trimmed.length() - 1) throw invalid(source, "empty namespace or path");
        return colon < 0 ? DEFAULT_NAMESPACE + ":" + trimmed : trimmed;
    }

    /**
     * Разбирает список {@code name=value} через запятую.
     *
     * @param source исходная строка для сообщения об ошибке.
     * @param body   текст между скобками.
     * @param out    карта для найденных свойств.
     */
    private static void parseProperties(String source, String body, Map<String, String> out) {
        if (body.isBlank()) return;
        for (String entry : body.split(",", -1)) {
            int equals = entry.indexOf('=');
            if (equals < 0) throw invalid(source, "property '" + entry.trim() + "' has no value");
            String name = entry.substring(0, equals).trim();
            String propertyValue = entry.substring(equals + 1).trim();
            if (name.isEmpty() || propertyValue.isEmpty()) throw invalid(source, "empty property name or value");
            if (out.put(name, propertyValue) != null) throw invalid(source, "duplicate property '" + name + "'");
        }
    }

    /**
     * Разбирает неотрицательные метаданные.
     *
     * @param source исходная строка для сообщения об ошибке.
     * @param value  текст после {@code @}.
     * @return метаданные.
     */
    private static int parseMeta(String source, String value) {
        try {
            int meta = Integer.parseInt(value.trim());
            if (meta < 0) throw invalid(source, "metadata must be non-negative");
            return meta;
        } catch (NumberFormatException failure) {
            throw invalid(source, "metadata is not a number");
        }
    }

    /**
     * Создаёт ошибку разбора с исходной строкой.
     *
     * @param source исходная строка.
     * @param reason причина.
     * @return исключение для броска.
     */
    private static IllegalArgumentException invalid(String source, String reason) {
        return new IllegalArgumentException("Invalid block '" + source + "': " + reason);
    }
}
