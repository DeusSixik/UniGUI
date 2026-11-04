package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Фиксирует, какие свойства снимок записать не может. Список хранится эталоном
 * ({@code golden/snapshot-unwritable.txt}): появление или исчезновение строки — это изменение возможностей
 * сохранения UI, и оно должно быть осознанным.
 *
 * <p>Законно не записываются свойства без собственного состояния ({@code align}, {@code overflow},
 * {@code class}), обработчики событий ({@code onClick}), переходные действия ({@code open}) и значения, у которых
 * у виджета нет читаемого представления.</p>
 */
final class SnapshotCoverageTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void unwritablePropertiesAreTrackedByAGoldenFile() {
        Schema schema = Schema.builtIns();
        StringBuilder out = new StringBuilder();
        int writable = 0;
        int total = 0;
        for (TypeInfo type : schema.types()) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (Object entry : type.properties()) {
                PropertyInfo property = (PropertyInfo) entry;
                if (property.name().equals("id")) continue;
                total++;
                if (property.writable()) writable++;
                else names.add(property.name());
            }
            java.util.Collections.sort(names);
            for (String name : names) out.append(type.name().local()).append('.').append(name).append('\n');
        }
        System.out.println("[snapshot] coverage: " + writable + " of " + total + " properties can be saved");
        assertTrue(writable * 100 / total >= 90, "snapshot coverage fell below 90%: " + writable + "/" + total);
        Golden.assertMatches("snapshot-unwritable.txt", out.toString());
    }
}
