package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Три реальных демо-документа, загруженные новым загрузчиком, должны дать то же дерево виджетов, что и
 * старый (golden снят со старого загрузчика до начала миграции).
 */
final class LoaderParityTest {
    private static void assertSameTree(String name) {
        String expected = Golden.read("tree-" + name + ".txt").replace("\r\n", "\n");
        String actual = TreeDump.dump(Xaml.loadResource("assets/unigui/xml/" + name + ".xml"));
        assertEquals(expected, actual, "widget tree of " + name + " differs from the legacy loader");
    }

    /**
     * Прямое сравнение двух живых деревьев: по каждому виджету сверяются все свойства, которые схема умеет
     * читать (цвета, рамки, текстуры, режимы…), а не только поля, попавшие в golden.
     */
    private static void assertSameProperties(String name) {
        String path = "assets/unigui/xml/" + name + ".xml";
        Widget legacy = XMLWidget.createResource(path);
        Widget current = Xaml.loadResource(path);
        List<String> differences = new ArrayList<>();
        int[] compared = {0};
        compare(legacy, current, name, differences, compared);
        System.out.println("[parity] " + name + ": compared " + compared[0] + " property values");
        assertTrue(compared[0] > 200, "the comparison must actually inspect many values, inspected " + compared[0]);
        assertTrue(differences.isEmpty(), differences.size() + " property differences:\n"
                + String.join("\n", differences.subList(0, Math.min(15, differences.size()))));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void compare(Widget legacy, Widget current, String where, List<String> differences, int[] compared) {
        String here = where + "/" + current.getClass().getSimpleName()
                + (current.id().isBlank() ? "" : "#" + current.id());
        if (legacy.getClass() != current.getClass()) {
            differences.add(here + ": class " + legacy.getClass().getSimpleName());
            return;
        }
        TypeInfo type = Xaml.defaultSchema().type(current.getClass().getSimpleName()).orElse(null);
        if (type != null) {
            for (Object entry : type.properties()) {
                PropertyInfo info = (PropertyInfo) entry;
                if (!info.readable()) continue;
                Object before = info.get(legacy);
                Object after = info.get(current);
                compared[0]++;
                if (!sameValue(before, after)) {
                    differences.add(here + "." + info.name() + ": legacy=" + before + " new=" + after);
                }
            }
        }
        List<Widget> legacyChildren = children(legacy);
        List<Widget> currentChildren = children(current);
        if (legacyChildren.size() != currentChildren.size()) {
            differences.add(here + ": " + legacyChildren.size() + " vs " + currentChildren.size() + " children");
            return;
        }
        for (int i = 0; i < currentChildren.size(); i++) {
            compare(legacyChildren.get(i), currentChildren.get(i), here, differences, compared);
        }
    }

    private static List<Widget> children(Widget widget) {
        if (widget instanceof ScrollView scroll) {
            return scroll.content() == null ? List.of() : List.of(scroll.content());
        }
        return widget.children();
    }

    /** Значения без собственного equals (старые изменяемые обёртки) сравниваются по представлению. */
    private static boolean sameValue(Object left, Object right) {
        if (Objects.equals(left, right)) return true;
        if (left == null || right == null) return false;
        String a = String.valueOf(left);
        String b = String.valueOf(right);
        return a.equals(b) || (a.contains("@") && b.contains("@") && a.substring(0, a.indexOf('@')).equals(b.substring(0, b.indexOf('@'))));
    }

    @Test
    void overviewPropertiesMatchLegacyLoader() {
        assertSameProperties("overview");
    }

    @Test
    void demoPropertiesMatchLegacyLoader() {
        assertSameProperties("xml_demo");
    }

    @Test
    void secondDemoPropertiesMatchLegacyLoader() {
        assertSameProperties("xml_demo_2");
    }

    @Test
    void overviewMatchesLegacyLoader() {
        assertSameTree("overview");
    }

    @Test
    void demoMatchesLegacyLoader() {
        assertSameTree("xml_demo");
    }

    @Test
    void secondDemoMatchesLegacyLoader() {
        assertSameTree("xml_demo_2");
    }
}
