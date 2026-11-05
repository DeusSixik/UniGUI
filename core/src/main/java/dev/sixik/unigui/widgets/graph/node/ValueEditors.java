package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.ColorChangedEvent;
import dev.sixik.unigui.api.event.NumberValueChangedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.event.TextChangedEvent;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Готовые {@link ValueEditor}: текст, число, целое, флажок, цвет, выбор из списка и вектор.
 */
@AiDoc(
        summary = "Factory of ready-made ValueEditors: text, number, integer, bool, color, choice and vector.",
        purpose = "Covers the common default-value editors of Blueprint and Shader Graph inputs.",
        useWhen = {
                "Attach a typed default value to a node input: `NodeBuilder.input(id, label, type, ValueEditors.number(5))`.",
                "Edit a vec3 in one row via `ValueEditors.vector(3, 0, 64, 0)`."
        },
        criticalRules = {
                "Value types: text String, number Double, integer Integer, bool Boolean, color Integer (ARGB), choice String, vector double[].",
                "Serialized forms are plain text: numbers as decimal, color as #AARRGGBB, vector as space-separated numbers."
        },
        related = {"ValueEditor", "PortRowModule", "NodeBuilder"})
@WikiPage(title = "ValueEditors", slug = "value-editors", category = "<key:graph.category>", summary = "<key:value.editors.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:value.editors.overview>", order = 10)
public final class ValueEditors {
    private ValueEditors() {
    }

    /**
     * @param initial начальный текст
     * @return однострочный текстовый редактор
     */
    public static ValueEditor text(String initial) {
        return new TextEditor(initial == null ? "" : initial);
    }

    /**
     * @param initial начальное значение
     * @return редактор вещественного числа без ограничений
     */
    public static ValueEditor number(double initial) {
        return new NumberEditor(initial, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false);
    }

    /**
     * @param initial начальное значение
     * @param min     нижняя граница
     * @param max     верхняя граница
     * @return редактор вещественного числа с границами
     */
    public static ValueEditor number(double initial, double min, double max) {
        return new NumberEditor(initial, min, max, false);
    }

    /**
     * @param initial начальное значение
     * @param min     нижняя граница
     * @param max     верхняя граница
     * @return редактор целого числа
     */
    public static ValueEditor integer(int initial, int min, int max) {
        return new NumberEditor(initial, min, max, true);
    }

    /**
     * @param initial начальное состояние
     * @return редактор-флажок
     */
    public static ValueEditor bool(boolean initial) {
        return new BoolEditor(initial);
    }

    /**
     * @param argb начальный цвет {@code 0xAARRGGBB}
     * @return редактор цвета
     */
    public static ValueEditor color(int argb) {
        return new ColorEditor(argb);
    }

    /**
     * @param options  варианты выбора; не пустой
     * @param selected индекс выбранного варианта
     * @return редактор выбора из списка, значение — текст варианта
     */
    public static ValueEditor choice(List<String> options, int selected) {
        return new ChoiceEditor(options, selected);
    }

    /**
     * Редактор вектора в одну строку: по числовому полю на компонент.
     *
     * @param size    число компонент, от 2 до 4
     * @param initial начальные значения; недостающие равны нулю
     * @return редактор вектора, значение — {@code double[]}
     */
    public static ValueEditor vector(int size, double... initial) {
        return new VectorEditor(size, initial);
    }

    /**
     * Общая логика: слушатели, тихая установка и гарантированное уведомление об изменении.
     */
    private abstract static class Base implements ValueEditor {
        private final List<Runnable> listeners = new ArrayList<>();
        private boolean silent;
        private boolean notified;

        @Override
        public final void onChange(Runnable listener) {
            if (listener != null) listeners.add(listener);
        }

        /**
         * Вызывается слушателями событий виджета; уведомляет, если запись не тихая.
         */
        final void changed() {
            if (silent) return;
            notified = true;
            for (Runnable listener : List.copyOf(listeners)) {
                listener.run();
            }
        }

        @Override
        public final void value(Object value) {
            String before = serialize();
            notified = false;
            apply(value);
            // Виджет мог не породить событие при программной записи: тогда уведомляем сами.
            if (!silent && !notified && !Objects.equals(before, serialize())) {
                changed();
            }
        }

        @Override
        public final void silentValue(Object value) {
            boolean previous = silent;
            silent = true;
            try {
                apply(value);
            } finally {
                silent = previous;
            }
        }

        @Override
        public final void deserialize(String text) {
            if (text == null) return;
            try {
                silentValue(parse(text));
            } catch (RuntimeException malformed) {
                // Некорректный текст или неподходящее значение (например, неизвестный вариант выбора) игнорируются.
            }
        }

        abstract void apply(Object value);

        abstract Object parse(String text);
    }

    private static final class TextEditor extends Base {
        private final TextField field;

        private TextEditor(String initial) {
            field = new TextField(initial);
            field.on(TextChangedEvent.TYPE, event -> changed());
        }

        @Override
        public Widget widget() {
            return field;
        }

        @Override
        public Object value() {
            return field.text();
        }

        @Override
        public String serialize() {
            return field.text();
        }

        @Override
        void apply(Object value) {
            field.text(value == null ? "" : String.valueOf(value));
        }

        @Override
        Object parse(String text) {
            return text;
        }
    }

    private static final class NumberEditor extends Base {
        private final NumberField field = new NumberField();
        private final boolean integer;
        private final double min;
        private final double max;

        private NumberEditor(double initial, double min, double max, boolean integer) {
            this.integer = integer;
            this.min = min;
            this.max = max;
            if (Double.isFinite(min)) field.min(min);
            if (Double.isFinite(max)) field.max(max);
            if (integer) field.step(1.0);
            field.silentValue(clamp(initial));
            field.on(NumberValueChangedEvent.TYPE, event -> changed());
        }

        private double clamp(double value) {
            double rounded = integer ? Math.round(value) : value;
            return Math.max(min, Math.min(max, rounded));
        }

        @Override
        public Widget widget() {
            return field;
        }

        @Override
        public Object value() {
            return integer ? (Object) (int) Math.round(field.value()) : (Object) field.value();
        }

        @Override
        public String serialize() {
            return integer ? Integer.toString((int) Math.round(field.value())) : Double.toString(field.value());
        }

        @Override
        void apply(Object value) {
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("Number expected, got " + value);
            }
            field.value(clamp(number.doubleValue()));
        }

        @Override
        Object parse(String text) {
            return Double.parseDouble(text.trim());
        }
    }

    private static final class BoolEditor extends Base {
        private final Checkbox box = new Checkbox();

        private BoolEditor(boolean initial) {
            box.silentState(initial ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
            box.on(CheckedChangedEvent.TYPE, event -> changed());
        }

        @Override
        public Widget widget() {
            return box;
        }

        @Override
        public Object value() {
            return box.state() == CheckboxState.CHECKED;
        }

        @Override
        public String serialize() {
            return Boolean.toString(box.state() == CheckboxState.CHECKED);
        }

        @Override
        void apply(Object value) {
            if (!(value instanceof Boolean flag)) {
                throw new IllegalArgumentException("Boolean expected, got " + value);
            }
            box.state(flag ? CheckboxState.CHECKED : CheckboxState.UNCHECKED);
        }

        @Override
        Object parse(String text) {
            String trimmed = text.trim();
            if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException(text);
            }
            return Boolean.parseBoolean(trimmed);
        }

        @Override
        public float preferredWidth() {
            return 24.0f;
        }
    }

    private static final class ColorEditor extends Base {
        private final ColorPicker picker = new ColorPicker();

        private ColorEditor(int argb) {
            picker.argb(argb);
            picker.on(ColorChangedEvent.TYPE, event -> changed());
        }

        @Override
        public Widget widget() {
            return picker;
        }

        @Override
        public Object value() {
            return picker.argb();
        }

        @Override
        public String serialize() {
            return String.format(Locale.ROOT, "#%08X", picker.argb());
        }

        @Override
        void apply(Object value) {
            if (!(value instanceof Number number)) {
                throw new IllegalArgumentException("ARGB int expected, got " + value);
            }
            picker.argb(number.intValue());
        }

        @Override
        Object parse(String text) {
            String trimmed = text.trim();
            if (!trimmed.startsWith("#")) throw new IllegalArgumentException(text);
            return (int) Long.parseLong(trimmed.substring(1), 16);
        }
    }

    private static final class ChoiceEditor extends Base {
        private final ComboBox box = new ComboBox();
        private final List<String> options;

        private ChoiceEditor(List<String> options, int selected) {
            if (options == null || options.isEmpty()) {
                throw new IllegalArgumentException("A choice editor needs at least one option");
            }
            this.options = List.copyOf(options);
            box.items(this.options);
            box.silentSelectedIndex(Math.max(0, Math.min(this.options.size() - 1, selected)));
            box.on(SelectionChangedEvent.TYPE, event -> changed());
        }

        @Override
        public Widget widget() {
            return box;
        }

        @Override
        public Object value() {
            return box.selectedItem();
        }

        @Override
        public String serialize() {
            String item = box.selectedItem();
            return item == null ? "" : item;
        }

        @Override
        void apply(Object value) {
            int index = options.indexOf(String.valueOf(value));
            if (index < 0) {
                throw new IllegalArgumentException("Unknown option: " + value);
            }
            box.selectedIndex(index);
        }

        @Override
        Object parse(String text) {
            return text;
        }
    }

    private static final class VectorEditor extends Base {
        private final HBox row = new HBox();
        private final NumberField[] fields;

        private VectorEditor(int size, double[] initial) {
            if (size < 2 || size > 4) {
                throw new IllegalArgumentException("Vector size must be 2..4, got " + size);
            }
            row.spacing(2.0f);
            fields = new NumberField[size];
            for (int i = 0; i < size; i++) {
                NumberField field = new NumberField();
                field.silentValue(initial != null && i < initial.length ? initial[i] : 0.0);
                field.layout(style -> style.flexGrow(1).flexShrink(1.0f));
                field.on(NumberValueChangedEvent.TYPE, event -> changed());
                fields[i] = field;
                row.addChild(field);
            }
        }

        @Override
        public Widget widget() {
            return row;
        }

        @Override
        public Object value() {
            double[] values = new double[fields.length];
            for (int i = 0; i < fields.length; i++) values[i] = fields[i].value();
            return values;
        }

        @Override
        public String serialize() {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) text.append(' ');
                text.append(Double.toString(fields[i].value()));
            }
            return text.toString();
        }

        @Override
        void apply(Object value) {
            if (!(value instanceof double[] values) || values.length != fields.length) {
                throw new IllegalArgumentException("double[" + fields.length + "] expected");
            }
            for (int i = 0; i < fields.length; i++) fields[i].value(values[i]);
        }

        @Override
        Object parse(String text) {
            String[] parts = text.trim().split("\\s+");
            if (parts.length != fields.length) throw new IllegalArgumentException(text);
            double[] values = new double[parts.length];
            for (int i = 0; i < parts.length; i++) values[i] = Double.parseDouble(parts[i]);
            return values;
        }

        @Override
        public float preferredWidth() {
            return 40.0f * fields.length + 2.0f * (fields.length - 1);
        }
    }
}
