package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed Minecraft block specification shared across game versions.
 *
 * <p>Use {@code namespace:path[name=value,...]} or {@code namespace:path@meta}.</p>
 * <ul>
 * <li>Omit the namespace to default to {@code minecraft}.</li>
 * <li>Use bracketed block-state properties on 1.12.2 and later, for example {@code minecraft:furnace[facing=north]}.</li>
 * <li>Use {@code @meta} for legacy metadata on 1.7.10 and 1.12.2, for example {@code minecraft:wool@14}.</li>
 * </ul>
 * <p>Parsing checks syntax. The installed {@link ScenePlatform} resolves IDs, properties and values.</p>
 *
 * @param text trimmed source string.
 * @param id namespaced block ID.
 * @param properties immutable map of properties in source order.
 * @param meta legacy metadata, or {@code -1} when omitted.
 */
@WikiPage(
        title = "BlockSpec", slug = "block-spec",
        category = "API/3D Scene", order = 125,
        summary = "Parsed block ID, state properties and legacy metadata.")
@WikiSection(title = "Syntax", order = 10, markdown = """
        Use `namespace:block`, `namespace:block[property=value,...]` or `namespace:block@meta`. The property map is
        read-only and preserves input order. `meta()` returns `-1` when omitted.
        """)
@WikiExample(title = "Parse a state and a legacy block", code = """
        BlockSpec furnace = BlockSpec.parse("minecraft:furnace[facing=south]");
        BlockSpec wool = BlockSpec.parse("minecraft:wool@14");
        """)
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@AiDoc(
        summary = "Parsed block ID, state properties and legacy metadata.",
        purpose = "Decode a portable block specification before a platform resolves it to a native state.",
        criticalRules = {
                "An unqualified ID uses the `minecraft` namespace.",
                "Use state properties in brackets on platforms with block states. Use `@meta` on 1.7.10 or 1.12.2.",
                "Parsing checks syntax. The platform validates registered block IDs, properties and values."
        },
        related = {"BlockNode", "StructureNode", "ScenePlatform"},
        examples = @AiDoc.Example(title = "Parse a state and a legacy block", code = """
                BlockSpec furnace = BlockSpec.parse("minecraft:furnace[facing=south]");
                BlockSpec wool = BlockSpec.parse("minecraft:wool@14");
                """))
public record BlockSpec(String text, String id, Map<String, String> properties, int meta) {
    /**
     * Default namespace for an ID without a colon.
     */
    private static final String DEFAULT_NAMESPACE = "minecraft";

    /**
     * Validates the fields and makes the property map immutable.
     *
     * @param text source block specification.
     * @param id namespaced block ID.
     * @param properties block-state properties.
     * @param meta nonnegative metadata, or -1 when omitted.
     * @throws NullPointerException if text, id or properties is null.
     * @throws IllegalArgumentException if meta is below -1.
     */
    @WikiMethod(section = "Parsing", sectionOrder = 10, order = 10,
            summary = "Validates the fields and makes the property map immutable.")
    @WikiThrows(type = NullPointerException.class, description = "if text, id or properties is null.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if meta is below -1.")
    @AiDoc(section = "Parsing", sectionOrder = 10, order = 10,
            summary = "Validates the fields and makes the property map immutable.")
    public BlockSpec {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(id, "id");
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(properties, "properties")));
        if (meta < -1) throw new IllegalArgumentException("Block metadata must be non-negative: " + meta);
    }

    /**
     * Parses a block specification.
     *
     * @param value text such as {@code minecraft:furnace[facing=north]} or {@code minecraft:wool@14}.
     * @return parsed block specification.
     * @throws NullPointerException if value is null.
     * @throws IllegalArgumentException if the syntax is invalid.
     */
    @WikiMethod(section = "Parsing", sectionOrder = 10, order = 20,
            summary = "Parses a block specification.")
    @WikiReturn("parsed block specification")
    @WikiThrows(type = NullPointerException.class, description = "if value is null.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the syntax is invalid.")
    @AiDoc(section = "Parsing", sectionOrder = 10, order = 20,
            summary = "Parses a block specification.")
    public static BlockSpec parse(
            @WikiParameter(description = "text such as `minecraft:furnace[facing=north]` or `minecraft:wool@14`.") String value) {
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
     * Checks whether the specification includes metadata.
     *
     * @return true if the source includes {@code @meta}.
     */
    @WikiMethod(section = "Parsing", sectionOrder = 10, order = 30,
            summary = "Checks whether the specification includes metadata.")
    @WikiReturn("true if the source includes `@meta`")
    @AiDoc(section = "Parsing", sectionOrder = 10, order = 30,
            summary = "Checks whether the specification includes metadata.")
    public boolean hasMeta() {
        return meta >= 0;
    }

    /**
     * Returns the source string.
     *
     * @return {@link #text()}.
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Adds the default namespace and validates the characters in the ID.
     *
     * @param source source string for error messages.
     * @param id ID without properties or metadata.
     * @return the namespaced ID.
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
     * Parses comma-separated {@code name=value} properties.
     *
     * @param source source string for error messages.
     * @param body text inside the brackets.
     * @param out output map for parsed properties.
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
     * Parses nonnegative legacy metadata.
     *
     * @param source source string for error messages.
     * @param value text after {@code @}.
     * @return the metadata.
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
     * Creates a parse error that identifies the source string.
     *
     * @param source source string.
     * @param reason error explanation.
     * @return the exception to throw.
     */
    private static IllegalArgumentException invalid(String source, String reason) {
        return new IllegalArgumentException("Invalid block '" + source + "': " + reason);
    }
}
