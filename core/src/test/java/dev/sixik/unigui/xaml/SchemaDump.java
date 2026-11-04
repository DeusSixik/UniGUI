package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Текстовый снимок схемы в том же формате, что и {@code golden/registry-schema.txt} старого реестра.
 * Один формат позволяет сравнивать оба реестра построчно.
 */
final class SchemaDump {
    private SchemaDump() {
    }

    static String dump(Schema schema) {
        StringBuilder out = new StringBuilder();
        schema.types().stream()
                .sorted(Comparator.comparing(type -> type.name().local()))
                .forEach(type -> dumpType(out, type));
        for (Map.Entry<String, String> alias : new TreeMap<>(schema.aliases()).entrySet()) {
            out.append("alias ").append(alias.getKey()).append(" -> ").append(alias.getValue()).append('\n');
        }
        return out.toString();
    }

    private static void dumpType(StringBuilder out, TypeInfo<?> type) {
        out.append(type.name().local())
                .append(" category=").append(type.category())
                .append(" children=").append(type.acceptsChildren())
                .append('\n');
        type.properties().stream()
                .sorted(Comparator.comparing(PropertyInfo::name))
                .forEach(property -> out.append("  @").append(property.name())
                        .append(':').append(property.kind())
                        .append(" default=").append(property.defaultValue())
                        .append(" category=").append(property.category())
                        .append('\n'));
        type.slots().stream()
                .sorted(Comparator.comparing(SlotInfo::name))
                .forEach(slot -> out.append("  .").append(slot.name())
                        .append(" single=").append(slot.single())
                        .append('\n'));
    }
}
