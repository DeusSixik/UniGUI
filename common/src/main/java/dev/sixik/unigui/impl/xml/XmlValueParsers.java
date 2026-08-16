package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
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
        TEXTURE_RESOLVER.set(resolver == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : resolver);
        COMMANDS.set(commands == null ? XmlWidgetOptions.DEFAULT_COMMANDS : commands);
        return new TextureResolverScope(previousResolver, previousCommands);
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

        private TextureResolverScope(XmlTextureResolver previousResolver, XmlCommandRegistry previousCommands) {
            this.previousResolver = previousResolver;
            this.previousCommands = previousCommands;
        }

        @Override
        public void close() {
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
