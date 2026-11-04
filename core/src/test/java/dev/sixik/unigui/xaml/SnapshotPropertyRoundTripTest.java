package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.XamlLoader;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.snapshot.Snapshotter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Для каждого записываемого свойства каждого встроенного типа: подставить нетипичное значение, сохранить виджет,
 * загрузить документ обратно и проверить, что именно это свойство вернулось тем же.
 *
 * <p>Свойства проверяются по одному, чтобы побочные эффекты соседних setter-ов не маскировали ошибку.</p>
 */
final class SnapshotPropertyRoundTripTest {
    private static final Schema SCHEMA = Schema.builtIns();
    private static final Snapshotter SNAPSHOTTER = new Snapshotter(SCHEMA);
    private static final LoadContext CONTEXT = LoadContext.DEFAULT;

    /** Значение, отличное от того, что у свежего виджета; {@code null}, если подобрать не удалось. */
    private static String sample(PropertyInfo<?, ?> property, String defaultText) {
        Class<?> type = property.valueType();
        String name = property.name().toLowerCase(Locale.ROOT);
        if (type == Boolean.class) return "true".equals(defaultText) ? "false" : "true";
        if (type == Integer.class) return "2";
        if (type == Float.class || type == Double.class) return "0.5";
        if (type == String.class) return "sample";
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) {
                String text = ((Enum<?>) constant).name().toLowerCase(Locale.ROOT).replace('_', '-');
                if (!text.equals(defaultText)) return text;
            }
            return null;
        }
        return switch (type.getSimpleName()) {
            case "MutableColor" -> "#11223344";
            case "SizeValue" -> "37";
            case "EdgeInsets", "SliceBorders" -> "1 2 3 4";
            case "AutoMargins" -> "left";
            case "List" -> "100 1fr";
            case "MutableRect" -> "0.1 0.2 0.3 0.4";
            case "TextureHandle" -> "test:texture";
            case "ShaderHandle" -> "test:shader";
            default -> name.isEmpty() ? null : null;
        };
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void aFreshWidgetOfAnyTypeHasNoLogicalChildren() {
        List<String> offenders = new ArrayList<>();
        for (TypeInfo type : SCHEMA.types()) {
            Widget fresh = type.create();
            // Загрузчик применяет отложенные мутации после добавления детей; в таком виде виджет и попадает в снимок.
            if (fresh instanceof dev.sixik.unigui.widgets.containers.PanelWidget panel) panel.applyQueuedMutations();
            Snapshotter.Result result = SNAPSHOTTER.snapshot(fresh);
            int exported = result.document().root().children().size();
            if (exported > 0) offenders.add(type.name().local() + " exports " + exported + " children by default");
        }
        assertTrue(offenders.isEmpty(),
                "children() of these widgets returns internal parts, they need an explicit child source:\n"
                        + String.join("\n", offenders));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void everyWritablePropertyOfEveryBuiltInTypeRoundTrips() {
        List<String> failures = new ArrayList<>();
        int tested = 0;
        int noEffect = 0;
        int rejected = 0;
        int writable = 0;
        int unreadable = 0;

        for (TypeInfo type : SCHEMA.types()) {
            Widget fresh = type.create();
            for (Object entry : type.properties()) {
                PropertyInfo property = (PropertyInfo) entry;
                if (property.name().equals("id")) continue;
                if (!property.writable()) {
                    unreadable++;
                    continue;
                }
                writable++;
                String where = type.name().local() + "." + property.name();

                String defaultText = property.readText(fresh);
                String sample = sample(property, defaultText);
                if (sample == null) {
                    noEffect++;
                    continue;
                }
                Widget original = type.create();
                try {
                    property.apply(original, sample, CONTEXT);
                } catch (RuntimeException rejection) {
                    rejected++;
                    continue;
                }
                String before = property.readText(original);
                if (before == null || before.equals(defaultText)) {
                    noEffect++;
                    continue;
                }

                tested++;
                try {
                    Snapshotter.Result result = SNAPSHOTTER.snapshot(original);
                    String text = XamlFormat.INSTANCE.write(result.document());
                    Widget reloaded = new XamlLoader(SCHEMA, LoadOptions.DEFAULT).load(text);
                    String after = property.readText(reloaded);
                    if (!before.equals(after)) {
                        failures.add(where + ": " + before + " -> " + after);
                    }
                } catch (RuntimeException failure) {
                    failures.add(where + ": " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
                }
            }
        }

        System.out.println("[snapshot-matrix] writable=" + writable + " tested=" + tested + " noEffect/noSample=" + noEffect
                + " rejectedByWidget=" + rejected + " notWritable=" + unreadable + " failures=" + failures.size());
        failures.forEach(line -> System.out.println("[snapshot-matrix] FAIL " + line));
        assertTrue(tested > 1500, "the matrix must really exercise the properties, tested=" + tested);
        assertTrue(failures.isEmpty(), failures.size() + " properties do not survive a snapshot round trip:\n"
                + String.join("\n", failures.subList(0, Math.min(40, failures.size()))));
    }
}
