package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.docs.ReferenceGenerator;
import dev.sixik.unigui.xaml.schema.Schema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReferenceGeneratorTest {
    @Test
    void everyTypeAndItsPropertiesAppearInTheReference() {
        Schema schema = Schema.builtIns();
        String text = ReferenceGenerator.render(schema);
        assertTrue(schema.types().size() >= 69);
        for (var type : schema.types()) {
            assertTrue(text.contains("#### `<" + type.name().qualified() + ">`"), type.name() + " is missing");
        }
        // общее свойство и свойство конкретного типа
        assertTrue(text.contains("| `opacity` |"));
        assertTrue(text.contains("| `columns` |"));
    }

    @Test
    void generationIsDeterministic() {
        assertEquals(ReferenceGenerator.render(Schema.builtIns()), ReferenceGenerator.render(Schema.builtIns()));
    }

    /** Свойство с {@code |} в описании не должно добавлять колонки: в строке свойства ровно шесть разделителей. */
    @Test
    void tableCellsCannotBreakTheTable() {
        String text = ReferenceGenerator.render(Schema.builtIns());
        int rows = 0;
        for (String line : text.split("\n")) {
            if (!line.startsWith("| `") || line.contains("| Alias")) continue;
            int pipes = line.replace("\\|", "").replaceAll("[^|]", "").length();
            if (pipes == 6) rows++;
        }
        assertTrue(rows > 500, "property rows found: " + rows);
    }
}
