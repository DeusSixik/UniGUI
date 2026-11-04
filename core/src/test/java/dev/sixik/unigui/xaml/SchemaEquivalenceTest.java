package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.schema.Schema;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Доказывает, что новая схема не потеряла ничего из прежнего реестра ({@code golden/registry-schema.txt}), а
 * все отличия — осознанные улучшения.
 *
 * <p>Допустимые отличия: тип редактора теперь берётся из конвертера (прежняя эвристика по имени ошибалась,
 * например считала {@code interactionTransitions} командой из-за подстроки «action»), атрибуты, объявленные
 * аннотациями на самом виджете, теперь видны, а метаданные могут только пополняться.</p>
 */
final class SchemaEquivalenceTest {
    /** Атрибуты, которых не было в прежнем реестре из-за неполной ручной регистрации; список проверен вручную. */
    private static final Set<String> NEWLY_EXPOSED = Set.of(
            "Button.focusRingVisible", "Button.hoverShader",
            "Checkbox.focusRingVisible", "Checkbox.hoverShader",
            "ToggleButton.focusRingVisible", "ToggleButton.hoverShader",
            "ProgressBar.orientation", "Slider.orientation",
            "ScrollView.overlayInset", "ScrollView.overlayScrollBars", "ScrollView.scrollbarSize",
            "ScrollView.tickCulling",
            // Список с шаблоном строки (этап шаблонов): данные приходят привязкой, строка описывается ItemTemplate.
            "VirtualListView.items", "VirtualListView..ItemTemplate");

    private static final Map<String, Type> LEGACY = parse(Golden.read("registry-schema.txt"));
    private static final Map<String, Type> CURRENT = parse(SchemaDump.dump(Schema.builtIns()));

    @Test
    void newSchemaMatchesItsOwnGolden() {
        Golden.assertMatches("schema.txt", SchemaDump.dump(Schema.builtIns()));
    }

    @Test
    void sameTypesCategoriesAndChildRules() {
        assertEquals(LEGACY.keySet(), CURRENT.keySet(), "set of types (and the alias block)");
        for (String name : LEGACY.keySet()) {
            assertEquals(LEGACY.get(name).header, CURRENT.get(name).header, name + ": category/children flag");
        }
    }

    @Test
    void noAttributeOrSlotIsLost() {
        List<String> lost = new ArrayList<>();
        for (Map.Entry<String, Type> type : LEGACY.entrySet()) {
            Set<String> now = names(CURRENT.get(type.getKey()).lines.keySet());
            for (String name : names(type.getValue().lines.keySet())) {
                if (!now.contains(name)) lost.add(type.getKey() + "." + name);
            }
        }
        assertTrue(lost.isEmpty(), "attributes/slots present in the legacy registry but missing now: " + lost);
    }

    @Test
    void onlyReviewedAttributesAreNew() {
        Set<String> added = new TreeSet<>();
        for (Map.Entry<String, Type> type : LEGACY.entrySet()) {
            Set<String> before = names(type.getValue().lines.keySet());
            for (String name : names(CURRENT.get(type.getKey()).lines.keySet())) {
                if (!before.contains(name)) added.add(type.getKey() + "." + name.replaceFirst("^@", ""));
            }
        }
        assertEquals(new TreeSet<>(NEWLY_EXPOSED), added,
                "attributes that appeared compared to the legacy registry must be reviewed and listed");
    }

    @Test
    void metadataIsNeverPoorerThanBefore() {
        List<String> poorer = new ArrayList<>();
        for (Map.Entry<String, Type> type : LEGACY.entrySet()) {
            Map<String, String> now = byName(CURRENT.get(type.getKey()).lines);
            for (Map.Entry<String, String> line : byName(type.getValue().lines).entrySet()) {
                String after = now.get(line.getKey());
                if (after == null) continue;
                String oldDefault = field(line.getValue(), "default=");
                String newDefault = field(after, "default=");
                String oldCategory = field(line.getValue(), "category=");
                String newCategory = field(after, "category=");
                if (!oldDefault.isEmpty() && !oldDefault.equals(newDefault)) {
                    poorer.add(type.getKey() + "." + line.getKey() + " default '" + oldDefault + "' -> '" + newDefault + "'");
                }
                if (!newCategory.equals(oldCategory) && newCategory.equals("Common")) {
                    poorer.add(type.getKey() + "." + line.getKey() + " category " + oldCategory + " -> Common");
                }
            }
        }
        assertTrue(poorer.isEmpty(), "metadata regressed: " + poorer);
    }

    // ------------------------------------------------------------------ разбор снимков

    private record Type(String header, Map<String, String> lines) {
    }

    /** Имя без суффикса типа редактора: {@code @flexBasis:STRING -> @flexBasis}; слоты остаются как есть. */
    private static Set<String> names(Set<String> keys) {
        Set<String> result = new TreeSet<>();
        for (String key : keys) {
            int colon = key.indexOf(':');
            result.add(colon > 0 ? key.substring(0, colon) : key);
        }
        return result;
    }

    private static Map<String, String> byName(Map<String, String> lines) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> line : lines.entrySet()) {
            String key = line.getKey();
            int colon = key.indexOf(':');
            result.put(colon > 0 ? key.substring(0, colon) : key, line.getValue());
        }
        return result;
    }

    private static String field(String line, String name) {
        int start = line.indexOf(name);
        if (start < 0) return "";
        start += name.length();
        int end = line.indexOf(" category=", start);
        if (name.equals("category=")) end = line.length();
        return end < 0 ? line.substring(start) : line.substring(start, end);
    }

    private static Map<String, Type> parse(String text) {
        Map<String, Type> result = new TreeMap<>();
        Type current = null;
        for (String line : text.replace("\r\n", "\n").split("\n")) {
            if (line.isBlank()) continue;
            if (line.startsWith("alias ")) {
                result.computeIfAbsent("alias", key -> new Type("", new LinkedHashMap<>())).lines.put(line, "");
            } else if (line.startsWith("  ")) {
                int space = line.indexOf(' ', 3);
                String key = space < 0 ? line.trim() : line.substring(0, space).trim();
                current.lines.put(key, space < 0 ? "" : line.substring(space + 1));
            } else {
                int space = line.indexOf(' ');
                current = new Type(line.substring(space + 1), new LinkedHashMap<>());
                result.put(line.substring(0, space), current);
            }
        }
        return result;
    }
}
