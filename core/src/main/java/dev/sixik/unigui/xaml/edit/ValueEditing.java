package dev.sixik.unigui.xaml.edit;

import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Редакторы значений по типам: для каждого свойства определяет, какой способ ввода ему подходит (число, флажок,
 * список, цвет, отступы, размер, ресурс), и приводит введённый текст к канонической записи документа или отказывает с
 * понятной причиной. Панель редактора рисует виджет по {@link Editor}, а разбор и запись значения остаются здесь, без
 * привязки к виджетам.
 */
public final class ValueEditing {
    private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    private ValueEditing() {
    }

    /** Способ ввода значения. */
    public sealed interface Editor permits Text, Number, Toggle, Choice, Color, Insets, Size, Resource {
    }

    /** Свободный текст (строки, привязки, команды). */
    public record Text() implements Editor {
    }

    /**
     * Число.
     *
     * @param integer {@code true}, если допустимы только целые
     */
    public record Number(boolean integer) implements Editor {
    }

    /** Логическое значение. */
    public record Toggle() implements Editor {
    }

    /**
     * Выбор из списка.
     *
     * @param options допустимые значения
     */
    public record Choice(List<String> options) implements Editor {
        public Choice {
            options = List.copyOf(options);
        }
    }

    /** Цвет: {@code #RRGGBB} или {@code #AARRGGBB}. */
    public record Color() implements Editor {
    }

    /** Четыре отступа: верх, право, низ, лево. */
    public record Insets() implements Editor {
    }

    /** Размер: {@code auto}, пиксели или проценты. */
    public record Size() implements Editor {
    }

    /**
     * Ресурс проекта.
     *
     * @param folder     папка ({@code textures/}, {@code fonts/}, {@code shaders/})
     * @param candidates идентификаторы, которые есть в проекте
     */
    public record Resource(String folder, List<String> candidates) implements Editor {
        public Resource {
            candidates = List.copyOf(candidates);
        }
    }

    // ------------------------------------------------------------------ выбор редактора

    /**
     * Выбирает редактор по описанию свойства.
     *
     * @param view    описание свойства из инспектора
     * @param catalog ресурсы проекта; {@code null} — их нет
     * @return редактор
     */
    public static Editor editorFor(Inspector.PropertyView view, ResourceCatalog catalog) {
        ResourceCatalog resources = catalog == null ? ResourceCatalog.NONE : catalog;
        ValueKind kind = view.kind();
        if (kind == null) return new Text();
        return switch (kind) {
            case NUMBER -> new Number(false); // целочисленность проверяет конвертер свойства при загрузке
            case BOOLEAN -> new Toggle();
            case ENUM -> view.options().isEmpty() ? new Text() : new Choice(view.options());
            case COLOR -> new Color();
            case INSETS -> new Insets();
            case SIZE_VALUE -> new Size();
            case RESOURCE_ID -> {
                String folder = folderOf(view.name());
                yield new Resource(folder, resources.ids(folder));
            }
            default -> new Text();
        };
    }

    /**
     * Редактор по типу значения контракта или пина графа ({@code string}, {@code int}, {@code float}, {@code double},
     * {@code bool}, {@code color}, {@code texture}…).
     *
     * @param type    имя типа
     * @param catalog ресурсы проекта для текстур; {@code null} — их нет
     * @return редактор; для остальных типов свободный текст
     */
    public static Editor editorForType(String type, ResourceCatalog catalog) {
        String name = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "bool" -> new Toggle();
            case "int" -> new Number(true);
            case "float", "double" -> new Number(false);
            case "color" -> new Color();
            case "texture" -> new Resource("textures/", (catalog == null ? ResourceCatalog.NONE : catalog).ids("textures/"));
            default -> new Text();
        };
    }

    private static String folderOf(String property) {
        String name = property.toLowerCase(Locale.ROOT);
        if (name.contains("font")) return "fonts/";
        if (name.contains("shader")) return "shaders/";
        return "textures/";
    }

    // ------------------------------------------------------------------ нормализация

    /**
     * Приводит введённый текст к канонической записи.
     *
     * @param editor редактор свойства
     * @param text   текст
     * @return запись для документа; пустая строка означает «не задано»
     * @throws IllegalArgumentException с причиной, если значение не подходит
     */
    public static String normalize(Editor editor, String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) return "";
        if (value.startsWith("{")) return value; // выражение привязки проверяет загрузчик
        if (editor instanceof Number number) return normalizeNumber(number, value);
        if (editor instanceof Toggle) return normalizeToggle(value);
        if (editor instanceof Choice choice) return normalizeChoice(choice, value);
        if (editor instanceof Color) return ColorValue.parse(value).format();
        if (editor instanceof Insets) return InsetsValue.parse(value).format();
        if (editor instanceof Size) return SizeValue.parse(value).format();
        if (editor instanceof Resource resource) return normalizeResource(resource, value);
        return text;
    }

    private static String normalizeNumber(Number editor, String value) {
        double parsed;
        try {
            parsed = Double.parseDouble(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("'" + value + "' is not a number.");
        }
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("'" + value + "' is not a finite number.");
        if (editor.integer()) {
            if (parsed != Math.rint(parsed)) throw new IllegalArgumentException("'" + value + "' must be a whole number.");
            return Long.toString((long) parsed);
        }
        return format(parsed);
    }

    private static String normalizeToggle(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> "true";
            case "false", "no", "off", "0" -> "false";
            default -> throw new IllegalArgumentException("'" + value + "' is not true or false.");
        };
    }

    private static String normalizeChoice(Choice choice, String value) {
        for (String option : choice.options()) if (option.equalsIgnoreCase(value)) return option;
        throw new IllegalArgumentException("'" + value + "' is not one of " + choice.options() + ".");
    }

    private static String normalizeResource(Resource resource, String value) {
        if (resource.candidates().isEmpty() || resource.candidates().contains(value)) return value;
        throw new IllegalArgumentException("The project has no " + resource.folder() + value + ". Available: "
                + resource.candidates() + ".");
    }

    /** Число без хвоста {@code .0}. */
    static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? Long.toString((long) value)
                : Double.toString(Math.round(value * 1000.0) / 1000.0);
    }

    // ------------------------------------------------------------------ значения

    /**
     * Цвет в каналах 0..255.
     *
     * @param red   красный
     * @param green зелёный
     * @param blue  синий
     * @param alpha непрозрачность
     */
    public record ColorValue(int red, int green, int blue, int alpha) {
        public ColorValue {
            red = clamp(red);
            green = clamp(green);
            blue = clamp(blue);
            alpha = clamp(alpha);
        }

        /**
         * Разбирает {@code #RRGGBB}, {@code #AARRGGBB} (решётка необязательна).
         *
         * @param text запись цвета
         * @return цвет
         * @throws IllegalArgumentException если запись не цвет
         */
        public static ColorValue parse(String text) {
            var matcher = HEX.matcher(text.trim());
            if (!matcher.matches()) throw new IllegalArgumentException("'" + text + "' is not a color. Use #RRGGBB or #AARRGGBB.");
            String digits = matcher.group(1);
            int offset = digits.length() == 8 ? 2 : 0;
            int alpha = digits.length() == 8 ? Integer.parseInt(digits.substring(0, 2), 16) : 255;
            return new ColorValue(Integer.parseInt(digits.substring(offset, offset + 2), 16),
                    Integer.parseInt(digits.substring(offset + 2, offset + 4), 16),
                    Integer.parseInt(digits.substring(offset + 4, offset + 6), 16), alpha);
        }

        /** @return {@code #RRGGBB} для непрозрачного цвета, иначе {@code #AARRGGBB}; буквы заглавные */
        public String format() {
            String rgb = String.format("%02X%02X%02X", red, green, blue);
            return alpha == 255 ? "#" + rgb : "#" + String.format("%02X", alpha) + rgb;
        }

        /**
         * @param channel {@code r}, {@code g}, {@code b} или {@code a}
         * @param value   новое значение канала
         * @return цвет с изменённым каналом
         */
        public ColorValue with(char channel, int value) {
            return switch (Character.toLowerCase(channel)) {
                case 'r' -> new ColorValue(value, green, blue, alpha);
                case 'g' -> new ColorValue(red, value, blue, alpha);
                case 'b' -> new ColorValue(red, green, value, alpha);
                case 'a' -> new ColorValue(red, green, blue, value);
                default -> throw new IllegalArgumentException("Unknown channel '" + channel + "'.");
            };
        }

        private static int clamp(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }

    /**
     * Отступы.
     *
     * @param top    верх
     * @param right  право
     * @param bottom низ
     * @param left   лево
     */
    public record InsetsValue(double top, double right, double bottom, double left) {
        /**
         * Разбирает одно, два (вертикаль и горизонталь) или четыре (верх, право, низ, лево) числа через пробел или
         * запятую.
         *
         * @param text запись
         * @return отступы
         */
        public static InsetsValue parse(String text) {
            String[] parts = text.trim().split("[\\s,]+");
            double[] v = new double[parts.length];
            try {
                for (int i = 0; i < parts.length; i++) v[i] = Double.parseDouble(parts[i]);
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("'" + text + "' is not insets. Use 1, 2 or 4 numbers.");
            }
            return switch (v.length) {
                case 1 -> new InsetsValue(v[0], v[0], v[0], v[0]);
                case 2 -> new InsetsValue(v[0], v[1], v[0], v[1]);
                case 4 -> new InsetsValue(v[0], v[1], v[2], v[3]);
                default -> throw new IllegalArgumentException("'" + text + "' is not insets. Use 1, 2 or 4 numbers.");
            };
        }

        /** @return самая короткая запись: одно число, если все равны, иначе четыре */
        public String format() {
            if (top == right && top == bottom && top == left) return ValueEditing.format(top);
            return ValueEditing.format(top) + " " + ValueEditing.format(right) + " " + ValueEditing.format(bottom) + " "
                    + ValueEditing.format(left);
        }
    }

    /**
     * Размер.
     *
     * @param unit  единица
     * @param value число; для {@code AUTO} не используется
     */
    public record SizeValue(Unit unit, double value) {
        /** Единица размера. */
        public enum Unit { AUTO, PIXELS, PERCENT }

        /**
         * Разбирает {@code auto}, {@code 120} (пиксели), {@code 50%}.
         *
         * @param text запись
         * @return размер
         */
        public static SizeValue parse(String text) {
            String value = text.trim().toLowerCase(Locale.ROOT);
            if (value.equals("auto")) return new SizeValue(Unit.AUTO, 0.0);
            try {
                if (value.endsWith("%")) return new SizeValue(Unit.PERCENT, Double.parseDouble(value.substring(0, value.length() - 1)));
                return new SizeValue(Unit.PIXELS, Double.parseDouble(value.endsWith("px") ? value.substring(0, value.length() - 2) : value));
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("'" + text + "' is not a size. Use auto, 120 or 50%.");
            }
        }

        /** @return {@code auto}, {@code 120} или {@code 50%} */
        public String format() {
            return switch (unit) {
                case AUTO -> "auto";
                case PIXELS -> ValueEditing.format(value);
                case PERCENT -> ValueEditing.format(value) + "%";
            };
        }
    }
}
