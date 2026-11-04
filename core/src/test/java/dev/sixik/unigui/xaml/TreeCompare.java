package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Сравнивает два живых дерева виджетов по всем свойствам, которые схема умеет читать.
 *
 * <p>Это сильнее, чем сравнение дампа: проверяется каждое читаемое значение каждого виджета, а не заранее
 * выбранные поля.</p>
 */
final class TreeCompare {
    /** Результат сравнения. */
    record Outcome(int compared, List<String> differences) {
    }

    private TreeCompare() {
    }

    static Outcome compare(Widget expected, Widget actual, Schema schema) {
        List<String> differences = new ArrayList<>();
        int[] compared = {0};
        walk(expected, actual, "", schema, differences, compared);
        return new Outcome(compared[0], differences);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void walk(Widget expected, Widget actual, String where, Schema schema,
                             List<String> differences, int[] compared) {
        String here = where + "/" + actual.getClass().getSimpleName()
                + (actual.id().isBlank() ? "" : "#" + actual.id());
        if (expected.getClass() != actual.getClass()) {
            differences.add(here + ": class " + expected.getClass().getSimpleName());
            return;
        }
        TypeInfo type = schema.typeOf(actual.getClass()).orElse(null);
        if (type != null) {
            for (Object entry : type.properties()) {
                PropertyInfo info = (PropertyInfo) entry;
                if (!info.readable()) continue;
                Object before = info.get(expected);
                Object after = info.get(actual);
                compared[0]++;
                if (!same(before, after)) {
                    differences.add(here + "." + info.name() + ": expected=" + before + " actual=" + after);
                }
            }
        }
        List<Widget> left = children(expected);
        List<Widget> right = children(actual);
        if (left.size() != right.size()) {
            differences.add(here + ": " + left.size() + " vs " + right.size() + " children");
            return;
        }
        for (int i = 0; i < right.size(); i++) walk(left.get(i), right.get(i), here, schema, differences, compared);
    }

    private static List<Widget> children(Widget widget) {
        if (widget instanceof ScrollView scroll) {
            return scroll.content() == null ? List.of() : List.of(scroll.content());
        }
        return widget.children();
    }

    /** Значения без собственного equals (изменяемые обёртки) сравниваются по представлению. */
    private static boolean same(Object left, Object right) {
        if (Objects.equals(left, right)) return true;
        if (left == null || right == null) return false;
        String a = String.valueOf(left);
        String b = String.valueOf(right);
        return a.equals(b)
                || (a.contains("@") && b.contains("@") && a.substring(0, a.indexOf('@')).equals(b.substring(0, b.indexOf('@'))));
    }
}
