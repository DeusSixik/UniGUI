package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Парсер строкового описания {@link TextBrush} для XML/XAML и других data-driven сценариев.
 *
 * <p>Синтаксис намеренно небольшой и расширяемый: сейчас поддерживаются одноцветная заливка
 * и линейный градиент. Значение {@code none} или пустая строка сбрасывают brush.</p>
 *
 * <pre>{@code
 * textBrush="solid(#FFFFFF)"
 * textBrush="#FFFFFF"
 * textBrush="linear-gradient(#60D8FF, #F7C45A, 35)"
 * }</pre>
 */
public final class TextBrushParser {
    private TextBrushParser() {
    }

    /**
     * Разбирает строковое описание brush'а.
     *
     * @param value XML/XAML-значение; {@code null}, пустая строка, {@code none}, {@code null} или {@code default}
     *              возвращают {@code null}
     * @return brush или {@code null}, если заливку надо сбросить
     */
    public static TextBrush parse(String value) {
        String normalized = value == null ? "" : value.trim();
        if (isNone(normalized)) return null;
        if (isHexColor(normalized)) return TextBrush.solid(parseColor(normalized));

        FunctionCall call = functionCall(normalized);
        String name = normalizeName(call.name());
        List<String> args = call.arguments();
        return switch (name) {
            case "solid" -> solid(args, value);
            case "linear-gradient", "lineargradient", "gradient" -> linearGradient(args, value);
            default -> throw new IllegalArgumentException("Unknown text brush function: " + call.name());
        };
    }

    private static TextBrush solid(List<String> args, String source) {
        requireArgCount(args, 1, 1, source);
        return TextBrush.solid(parseColor(args.get(0)));
    }

    private static TextBrush linearGradient(List<String> args, String source) {
        requireArgCount(args, 2, 3, source);
        float angle = args.size() >= 3 ? parseAngle(args.get(2)) : 0.0f;
        return TextBrush.linearGradient(parseColor(args.get(0)), parseColor(args.get(1)), angle);
    }

    private static ColorView parseColor(String value) {
        String normalized = unquote(value == null ? "" : value.trim());
        if (!isHexColor(normalized)) {
            throw new IllegalArgumentException("Expected hex color #RRGGBB or #RRGGBBAA, got: " + value);
        }
        return MutableColor.fromHex(normalized);
    }

    private static float parseAngle(String value) {
        String normalized = unquote(value == null ? "" : value.trim()).toLowerCase(Locale.ROOT);
        if (normalized.endsWith("deg")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        } else if (normalized.endsWith("°")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        try {
            return Float.parseFloat(normalized);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Expected gradient angle in degrees, got: " + value, failure);
        }
    }

    private static FunctionCall functionCall(String value) {
        int open = value.indexOf('(');
        int close = value.lastIndexOf(')');
        if (open <= 0 || close != value.length() - 1) {
            throw new IllegalArgumentException("Expected text brush function, got: " + value);
        }
        String name = value.substring(0, open).trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Text brush function name must not be blank: " + value);
        }
        return new FunctionCall(name, splitArguments(value.substring(open + 1, close)));
    }

    private static List<String> splitArguments(String value) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
            } else if (current == ',' && depth == 0) {
                addArgument(result, value.substring(start, i));
                start = i + 1;
            }
        }
        addArgument(result, value.substring(start));
        return result;
    }

    private static void addArgument(List<String> result, String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isEmpty()) result.add(normalized);
    }

    private static void requireArgCount(List<String> args, int min, int max, String source) {
        if (args.size() < min || args.size() > max) {
            throw new IllegalArgumentException("Expected " + min + (min == max ? "" : ".." + max)
                    + " text brush arguments, got " + args.size() + ": " + source);
        }
    }

    private static boolean isNone(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty()
                || normalized.equals("none")
                || normalized.equals("null")
                || normalized.equals("default");
    }

    private static boolean isHexColor(String value) {
        String normalized = unquote(value == null ? "" : value.trim());
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (normalized.length() != 6 && normalized.length() != 8) return false;
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.digit(normalized.charAt(i), 16) < 0) return false;
        }
        return true;
    }

    private static String normalizeName(String value) {
        return value.trim().replace("_", "-").toLowerCase(Locale.ROOT);
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private record FunctionCall(String name, List<String> arguments) {
    }
}