package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Стандартные конвертеры значений свойств.
 *
 * <p>Логика разбора перенесена из прежнего {@code XmlValueParsers} без изменения поддерживаемого синтаксиса
 * (CSS-подобные отступы, {@code repeat(n, track)}, {@code fit-content(...)} и т. д.). Отличие одно: текстуры
 * разрешаются через явный {@link LoadContext}, а не через {@code ThreadLocal}.</p>
 */
public final class Converters {
    public static final ValueConverter<String> STRING =
            ValueConverter.of(String.class, ValueKind.STRING, value -> value == null ? "" : value);
    public static final ValueConverter<Boolean> BOOLEAN =
            ValueConverter.of(Boolean.class, ValueKind.BOOLEAN, Converters::parseBoolean);
    public static final ValueConverter<Integer> INT =
            ValueConverter.of(Integer.class, ValueKind.NUMBER, value -> Integer.parseInt(required(value).trim()));
    public static final ValueConverter<Float> FLOAT =
            ValueConverter.of(Float.class, ValueKind.NUMBER, value -> Float.parseFloat(required(value).trim()));
    public static final ValueConverter<Float> FLOAT_OR_AUTO =
            ValueConverter.of(Float.class, ValueKind.NUMBER, Converters::parseFloatOrAuto);
    public static final ValueConverter<Double> DOUBLE =
            ValueConverter.of(Double.class, ValueKind.NUMBER, value -> Double.parseDouble(required(value).trim()));
    public static final ValueConverter<MutableColor> COLOR =
            ValueConverter.of(MutableColor.class, ValueKind.COLOR, value -> MutableColor.fromHex(required(value).trim()));
    public static final ValueConverter<MutableRect> RECT =
            ValueConverter.of(MutableRect.class, ValueKind.STRING, Converters::parseRect);
    public static final ValueConverter<TextureHandle> TEXTURE =
            ValueConverter.withContext(TextureHandle.class, ValueKind.RESOURCE_ID, Converters::parseTexture);
    public static final ValueConverter<ShaderHandle> SHADER =
            ValueConverter.of(ShaderHandle.class, ValueKind.RESOURCE_ID, value -> ShaderHandle.resource(required(value).trim()));
    public static final ValueConverter<SizeValue> SIZE =
            ValueConverter.of(SizeValue.class, ValueKind.SIZE_VALUE, Converters::parseSize);
    public static final ValueConverter<EdgeInsets> INSETS =
            ValueConverter.of(EdgeInsets.class, ValueKind.INSETS, Converters::parseInsets);
    public static final ValueConverter<SliceBorders> SLICE_BORDERS =
            ValueConverter.of(SliceBorders.class, ValueKind.INSETS, Converters::parseSliceBorders);
    public static final ValueConverter<AutoMargins> AUTO_MARGINS =
            ValueConverter.of(AutoMargins.class, ValueKind.STRING, Converters::parseAutoMargins);
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final ValueConverter<List<GridTrack>> GRID_TRACKS =
            ValueConverter.of((Class) List.class, ValueKind.STRING, Converters::parseGridTracks);

    private static final ConcurrentMap<Class<?>, ValueConverter<?>> ENUMS = new ConcurrentHashMap<>();

    private Converters() {
    }

    /**
     * Возвращает конвертер для enum-типа; регистр, пробелы и дефисы в тексте игнорируются.
     *
     * @param type enum-класс
     * @param <E>  enum-тип
     * @return кэшированный конвертер
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> ValueConverter<E> enumOf(Class<E> type) {
        return (ValueConverter<E>) ENUMS.computeIfAbsent(type, key -> {
            E[] constants = type.getEnumConstants();
            return ValueConverter.of(type, ValueKind.ENUM, value -> parseEnum(type, constants, value));
        });
    }

    /**
     * Подбирает конвертер по типу параметра Java-setter-а.
     *
     * @param type      тип параметра (примитивный или ссылочный)
     * @param autoFloat разрешить ли для float значение {@code auto}
     * @return конвертер или {@code null}, если тип не поддерживается
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ValueConverter<?> forType(Class<?> type, boolean autoFloat) {
        if (type == String.class) return STRING;
        if (type == boolean.class || type == Boolean.class) return BOOLEAN;
        if (type == int.class || type == Integer.class) return INT;
        if (type == float.class || type == Float.class) return autoFloat ? FLOAT_OR_AUTO : FLOAT;
        if (type == double.class || type == Double.class) return DOUBLE;
        if (type == SizeValue.class) return SIZE;
        if (type == EdgeInsets.class) return INSETS;
        if (type == SliceBorders.class) return SLICE_BORDERS;
        if (type == MutableColor.class || type == ColorView.class) return COLOR;
        if (type == MutableRect.class || type == RectView.class) return RECT;
        if (type == TextureHandle.class) return TEXTURE;
        if (type == ShaderHandle.class) return SHADER;
        if (type.isEnum()) return enumOf((Class) type);
        return null;
    }

    // ------------------------------------------------------------------ parsing

    private static boolean parseBoolean(String value) {
        String normalized = required(value).trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> throw new IllegalArgumentException("Expected boolean, got: " + value);
        };
    }

    private static float parseFloatOrAuto(String value) {
        String normalized = required(value).trim();
        if (normalized.equalsIgnoreCase("auto")) return LayoutConstraints.AUTO;
        return Float.parseFloat(normalized);
    }

    private static SizeValue parseSize(String value) {
        String normalized = required(value).trim();
        if (normalized.equalsIgnoreCase("auto")) return SizeValue.auto();
        if (normalized.equalsIgnoreCase("content")) return SizeValue.content();
        if (normalized.equalsIgnoreCase("min-content")) return SizeValue.minContent();
        if (normalized.equalsIgnoreCase("max-content")) return SizeValue.maxContent();
        if (normalized.regionMatches(true, 0, "fit-content(", 0, 12) && normalized.endsWith(")")) {
            String inner = normalized.substring(12, normalized.length() - 1).trim();
            if (inner.endsWith("px")) inner = inner.substring(0, inner.length() - 2).trim();
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

    /** Парсит grid template: треки через пробел, {@code repeat(n, track)} разворачивается. */
    private static List<GridTrack> parseGridTracks(String value) {
        String normalized = required(value).trim();
        List<GridTrack> output = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int depth = 0;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == '(') depth++;
            else if (current == ')') depth--;
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

    private static void flushGridToken(List<GridTrack> output, String token) {
        String normalized = token.trim();
        if (normalized.isEmpty()) return;
        if (normalized.regionMatches(true, 0, "repeat(", 0, 7) && normalized.endsWith(")")) {
            String inner = normalized.substring(7, normalized.length() - 1);
            int comma = inner.indexOf(',');
            if (comma < 0) throw new IllegalArgumentException("Expected repeat(count, track), got: " + token);
            int count = Integer.parseInt(inner.substring(0, comma).trim());
            output.addAll(GridTrack.repeat(count, GridTrack.parse(inner.substring(comma + 1))));
            return;
        }
        output.add(GridTrack.parse(normalized));
    }

    /** Парсит {@code marginAuto}: набор из left/top/right/bottom/horizontal/vertical/all/none. */
    private static AutoMargins parseAutoMargins(String value) {
        String normalized = required(value).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.equals("none")) return AutoMargins.NONE;
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

    private static TextureHandle parseTexture(String value, LoadContext context) {
        String id = required(value).trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Texture id must not be blank");
        return context.textures().resolve(id, 16, 16, TextureOptions.defaults());
    }

    private static MutableRect parseRect(String value) {
        String[] parts = required(value).trim().split("\\s+");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Expected 4 rect values: x y width height, got: " + value);
        }
        return new MutableRect(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]),
                Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
    }

    /** Парсит CSS-подобные отступы: одно значение, вертикаль/горизонталь или верх/право/низ/лево. */
    private static EdgeInsets parseInsets(String value) {
        String[] parts = required(value).trim().split("\\s+");
        if (parts.length == 1) return EdgeInsets.all(parseInset(parts[0]));
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
        if (normalized.endsWith("px")) normalized = normalized.substring(0, normalized.length() - 2).trim();
        return Float.parseFloat(normalized);
    }

    /** Парсит nine-slice рамки: одно значение, горизонталь/вертикаль или left/top/right/bottom. */
    private static SliceBorders parseSliceBorders(String value) {
        String[] parts = required(value).trim().split("\\s+");
        if (parts.length == 1) return SliceBorders.all(parseInset(parts[0]));
        if (parts.length == 2) return SliceBorders.symmetric(parseInset(parts[0]), parseInset(parts[1]));
        if (parts.length == 4) {
            return new SliceBorders(parseInset(parts[0]), parseInset(parts[1]), parseInset(parts[2]), parseInset(parts[3]));
        }
        throw new IllegalArgumentException("Expected 1, 2 or 4 slice border values: left top right bottom, got: " + value);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, E[] constants, String value) {
        String normalized = normalizeEnumName(required(value));
        for (E constant : constants) {
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
}
