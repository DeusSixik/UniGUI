package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.text.InlineContentResolverScope;
import dev.sixik.unigui.api.text.InlineContentResolvers;
import dev.sixik.unigui.api.xml.XmlCommandRegistry;
import dev.sixik.unigui.api.xml.XmlTextureResolver;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;

import java.util.Locale;

/**
 * Общие парсеры значений для встроенных XML-атрибутов виджетов.
 */
public final class XmlValueParsers {
    private static final ThreadLocal<XmlTextureResolver> TEXTURE_RESOLVER = new ThreadLocal<>();
    private static final ThreadLocal<XmlCommandRegistry> COMMANDS = new ThreadLocal<>();

    public static final XmlValueParser<String> STRING = value -> value == null ? "" : value;
    public static final XmlValueParser<Boolean> BOOLEAN = XmlValueParsers::parseBoolean;
    public static final XmlValueParser<Integer> INT = value -> Integer.parseInt(required(value).trim());
    public static final XmlValueParser<Float> FLOAT = value -> Float.parseFloat(required(value).trim());
    public static final XmlValueParser<Float> FLOAT_OR_AUTO = XmlValueParsers::parseFloatOrAuto;
    public static final XmlValueParser<Double> DOUBLE = value -> Double.parseDouble(required(value).trim());
    public static final XmlValueParser<MutableColor> COLOR = value -> MutableColor.fromHex(required(value).trim());
    public static final XmlValueParser<MutableRect> RECT = XmlValueParsers::parseRect;
    public static final XmlValueParser<TextureHandle> TEXTURE = XmlValueParsers::parseTexture;
    public static final XmlValueParser<SizeValue> SIZE = XmlValueParsers::parseSize;
    public static final XmlValueParser<EdgeInsets> INSETS = XmlValueParsers::parseInsets;
    public static final XmlValueParser<AutoMargins> AUTO_MARGINS = XmlValueParsers::parseAutoMargins;
    public static final XmlValueParser<java.util.List<GridTrack>> GRID_TRACKS = XmlValueParsers::parseGridTracks;

    private XmlValueParsers() {
    }

    public static <E extends Enum<E>> XmlValueParser<E> enumValue(Class<E> type) {
        return value -> parseEnum(type, value);
    }

    static TextureResolverScope pushTextureResolver(XmlTextureResolver resolver) {
        return pushLoadContext(resolver, commandRegistry());
    }

    static TextureResolverScope pushLoadContext(XmlTextureResolver resolver, XmlCommandRegistry commands) {
        XmlTextureResolver previousResolver = TEXTURE_RESOLVER.get();
        XmlCommandRegistry previousCommands = COMMANDS.get();
        XmlTextureResolver normalizedResolver = resolver == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : resolver;
        TEXTURE_RESOLVER.set(normalizedResolver);
        COMMANDS.set(commands == null ? XmlWidgetOptions.DEFAULT_COMMANDS : commands);
        // XML text-атрибуты остаются строками. На время загрузки включаем resolver,
        // который превращает marker'ы {icon:...}/{texture:...} в inline texture span'ы.
        InlineContentResolverScope inlineScope = InlineContentResolvers.push(InlineContentResolvers.textureMarkers(
                (id, width, height) -> normalizedResolver.resolve(id, width, height, TextureOptions.defaults())));
        return new TextureResolverScope(previousResolver, previousCommands, inlineScope);
    }

    public static TextureHandle resolveTexture(String id, int width, int height, TextureOptions options) {
        XmlTextureResolver resolver = TEXTURE_RESOLVER.get();
        XmlTextureResolver normalizedResolver = resolver == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : resolver;
        return normalizedResolver.resolve(id, Math.max(1, width), Math.max(1, height),
                options == null ? TextureOptions.defaults() : options);
    }

    static XmlCommandRegistry commandRegistry() {
        XmlCommandRegistry commands = COMMANDS.get();
        return commands == null ? XmlWidgetOptions.DEFAULT_COMMANDS : commands;
    }

    private static boolean parseBoolean(String value) {
        String normalized = required(value).trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> throw new IllegalArgumentException("Expected boolean, got: " + value);
        };
    }

    private static SizeValue parseSize(String value) {
        String normalized = required(value).trim();
        if (normalized.equalsIgnoreCase("auto")) return SizeValue.auto();
        if (normalized.equalsIgnoreCase("content")) return SizeValue.content();
        if (normalized.equalsIgnoreCase("min-content")) return SizeValue.minContent();
        if (normalized.equalsIgnoreCase("max-content")) return SizeValue.maxContent();
        if (normalized.regionMatches(true, 0, "fit-content(", 0, 12) && normalized.endsWith(")")) {
            String inner = normalized.substring(12, normalized.length() - 1).trim();
            if (inner.endsWith("px")) {
                inner = inner.substring(0, inner.length() - 2).trim();
            }
            return SizeValue.fitContent(Float.parseFloat(inner));
        }
        if (normalized.endsWith("%")) {
            return SizeValue.percent(Float.parseFloat(normalized.substring(0, normalized.length() - 1).trim()));
        }
        if (normalized.endsWith("px")) {
            return SizeValue.px(Float.parseFloat(normalized.substring(0, normalized.length() - 2).trim()));
        }
        return SizeValue.px(Float.parseFloat(normalized));
    }

    private static float parseFloatOrAuto(String value) {
        String normalized = required(value).trim();
        if (normalized.equalsIgnoreCase("auto")) return LayoutConstraints.AUTO;
        return Float.parseFloat(normalized);
    }

    /**
     * Парсит grid template: треки через пробел, {@code repeat(n, track)} разворачивается.
     * Пример: {@code 100px repeat(2, 1fr) auto}.
     */
    private static java.util.List<GridTrack> parseGridTracks(String value) {
        String normalized = required(value).trim();
        java.util.List<GridTrack> output = new java.util.ArrayList<>();
        StringBuilder token = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            }
            if (Character.isWhitespace(current) && depth == 0) {
                flushGridToken(output, token.toString());
                token.setLength(0);
            } else {
                token.append(current);
            }
        }
        flushGridToken(output, token.toString());
        return output;
    }

    private static void flushGridToken(java.util.List<GridTrack> output, String token) {
        String normalized = token.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (normalized.regionMatches(true, 0, "repeat(", 0, 7) && normalized.endsWith(")")) {
            String inner = normalized.substring(7, normalized.length() - 1);
            int comma = inner.indexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("Expected repeat(count, track), got: " + token);
            }
            int count = Integer.parseInt(inner.substring(0, comma).trim());
            output.addAll(GridTrack.repeat(count, GridTrack.parse(inner.substring(comma + 1))));
            return;
        }
        output.add(GridTrack.parse(normalized));
    }

    /**
     * Парсит {@code marginAuto}: пробелом разделённый набор из
     * {@code left/top/right/bottom/horizontal/vertical/all/none}.
     */
    private static AutoMargins parseAutoMargins(String value) {
        String normalized = required(value).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.equals("none")) {
            return AutoMargins.NONE;
        }
        boolean left = false;
        boolean top = false;
        boolean right = false;
        boolean bottom = false;
        for (String part : normalized.split("[\\s,]+")) {
            switch (part) {
                case "all" -> {
                    left = true;
                    top = true;
                    right = true;
                    bottom = true;
                }
                case "horizontal" -> {
                    left = true;
                    right = true;
                }
                case "vertical" -> {
                    top = true;
                    bottom = true;
                }
                case "left" -> left = true;
                case "top" -> top = true;
                case "right" -> right = true;
                case "bottom" -> bottom = true;
                default -> throw new IllegalArgumentException("Expected marginAuto sides, got: " + value);
            }
        }
        return AutoMargins.of(left, top, right, bottom);
    }

    private static TextureHandle parseTexture(String value) {
        String id = required(value).trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Texture id must not be blank");
        }
        return resolveTexture(id, 16, 16, TextureOptions.defaults());
    }

    private static MutableRect parseRect(String value) {
        String[] parts = required(value).trim().split("\\s+");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Expected 4 rect values: x y width height, got: " + value);
        }
        return new MutableRect(
                Float.parseFloat(parts[0]),
                Float.parseFloat(parts[1]),
                Float.parseFloat(parts[2]),
                Float.parseFloat(parts[3]));
    }

    /**
     * Парсит CSS-подобные отступы: одно значение, вертикаль/горизонталь или верх/право/низ/лево.
     */
    private static EdgeInsets parseInsets(String value) {
        String[] parts = required(value).trim().split("\\s+");
        if (parts.length == 1) {
            return EdgeInsets.all(parseInset(parts[0]));
        }
        if (parts.length == 2) {
            float vertical = parseInset(parts[0]);
            float horizontal = parseInset(parts[1]);
            return new EdgeInsets(horizontal, vertical, horizontal, vertical);
        }
        if (parts.length == 4) {
            float top = parseInset(parts[0]);
            float right = parseInset(parts[1]);
            float bottom = parseInset(parts[2]);
            float left = parseInset(parts[3]);
            return new EdgeInsets(left, top, right, bottom);
        }
        throw new IllegalArgumentException("Expected 1, 2 or 4 inset values, got: " + value);
    }

    private static float parseInset(String value) {
        String normalized = value.trim();
        if (normalized.endsWith("px")) {
            normalized = normalized.substring(0, normalized.length() - 2).trim();
        }
        return Float.parseFloat(normalized);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        String normalized = normalizeEnumName(required(value));
        for (E constant : type.getEnumConstants()) {
            if (normalizeEnumName(constant.name()).equals(normalized)) return constant;
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + value);
    }

    private static String normalizeEnumName(String value) {
        return value.trim().replace("-", "_").replace(" ", "_").toUpperCase(Locale.ROOT);
    }

    private static String required(String value) {
        if (value == null) throw new IllegalArgumentException("Value must not be null");
        return value;
    }

    static final class TextureResolverScope implements AutoCloseable {
        private final XmlTextureResolver previousResolver;
        private final XmlCommandRegistry previousCommands;
        private final InlineContentResolverScope inlineScope;

        private TextureResolverScope(XmlTextureResolver previousResolver, XmlCommandRegistry previousCommands,
                                     InlineContentResolverScope inlineScope) {
            this.previousResolver = previousResolver;
            this.previousCommands = previousCommands;
            this.inlineScope = inlineScope;
        }

        @Override
        public void close() {
            if (inlineScope != null) inlineScope.close();
            if (previousResolver == null) {
                TEXTURE_RESOLVER.remove();
            } else {
                TEXTURE_RESOLVER.set(previousResolver);
            }
            if (previousCommands == null) {
                COMMANDS.remove();
            } else {
                COMMANDS.set(previousCommands);
            }
        }
    }
}
