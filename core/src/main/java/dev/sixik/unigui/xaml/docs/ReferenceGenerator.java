package dev.sixik.unigui.xaml.docs;

import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Генерирует markdown-справочник по XAML-виджетам из {@link Schema}.
 *
 * <p>Справочник строится из той же схемы, по которой работают загрузчик, снимок и редактор, поэтому он не может
 * разойтись с реальным поведением: если свойство читается из документа, оно есть и в таблице.</p>
 */
public final class ReferenceGenerator {
    private static final String DEFAULT_OUTPUT = "XAML_WIDGET_REFERENCE.md";

    private ReferenceGenerator() {
    }

    /**
     * Записывает справочник общей схемы ({@link Xaml#defaultSchema()}) в файл.
     *
     * @param args первый аргумент — путь к выходному файлу, по умолчанию {@code XAML_WIDGET_REFERENCE.md}
     * @throws IOException если файл не записан
     */
    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of(DEFAULT_OUTPUT) : Path.of(args[0]);
        Schema schema = Xaml.defaultSchema();
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, render(schema), StandardCharsets.UTF_8);
        System.out.println("Wrote " + output.toAbsolutePath() + " (types: " + schema.types().size()
                + ", aliases: " + schema.aliases().size() + ")");
    }

    /**
     * Строит текст справочника.
     *
     * @param schema схема, по которой строится справочник
     * @return markdown; не содержит даты, чтобы повторная генерация без изменений давала тот же текст
     */
    public static String render(Schema schema) {
        List<TypeInfo<?>> types = new ArrayList<>(schema.types());
        types.sort(Comparator.comparing((TypeInfo<?> type) -> type.category().toLowerCase(Locale.ROOT))
                .thenComparing(type -> type.name().qualified().toLowerCase(Locale.ROOT)));

        StringBuilder out = new StringBuilder(64_000);
        out.append("# Справочник XAML-виджетов\n\n");
        out.append("> Сгенерировано автоматически из `Xaml.defaultSchema()`. Не редактируй таблицы вручную; ")
                .append("обновляй через `dumpXamlReference`.\n\n");
        out.append("- Команда Windows: `.\\gradlew.bat :1.20.1:common:dumpXamlReference`\n");
        out.append("- Команда Unix: `./gradlew :1.20.1:common:dumpXamlReference`\n\n");

        out.append("## Сводка\n\n| Поверхность | Количество |\n| --- | ---: |\n");
        out.append("| Типы виджетов | ").append(types.size()).append(" |\n");
        out.append("| Aliases | ").append(schema.aliases().size()).append(" |\n\n");

        renderAliases(out, new TreeMap<>(schema.aliases()));

        Map<String, List<TypeInfo<?>>> byCategory = new LinkedHashMap<>();
        for (TypeInfo<?> type : types) {
            byCategory.computeIfAbsent(type.category(), ignored -> new ArrayList<>()).add(type);
        }
        out.append("## Виджеты\n\n");
        byCategory.forEach((category, entries) -> {
            out.append("### ").append(category).append("\n\n");
            for (TypeInfo<?> type : entries) renderType(out, type);
        });
        return out.toString();
    }

    private static void renderAliases(StringBuilder out, Map<String, String> aliases) {
        out.append("## Aliases\n\n");
        if (aliases.isEmpty()) {
            out.append("Нет зарегистрированных aliases.\n\n");
            return;
        }
        out.append("| Alias | Цель |\n| --- | --- |\n");
        aliases.forEach((alias, target) ->
                out.append("| `").append(escape(alias)).append("` | `").append(escape(target)).append("` |\n"));
        out.append('\n');
    }

    private static <T extends dev.sixik.unigui.api.widget.Widget> void renderType(StringBuilder out, TypeInfo<T> type) {
        out.append("#### `<").append(type.name().qualified()).append(">`\n\n");
        out.append("- Display name: ").append(code(type.displayName())).append('\n');
        out.append("- Прямые children: ").append(type.acceptsChildren() ? "да" : "нет").append('\n');
        if (!type.description().isBlank()) out.append("- Описание: ").append(type.description()).append('\n');
        out.append('\n');

        List<SlotInfo<T>> slots = new ArrayList<>(type.slots());
        if (!slots.isEmpty()) {
            out.append("Слоты:\n\n| Слот | Принимает | Описание |\n| --- | --- | --- |\n");
            for (SlotInfo<T> slot : slots) {
                out.append("| `").append(escape(slot.name())).append("` | ")
                        .append(slot.single() ? "один виджет" : "много виджетов")
                        .append(" | ").append(cell(slot.description())).append(" |\n");
            }
            out.append('\n');
        }

        out.append("Свойства:\n\n");
        List<PropertyInfo<? super T, ?>> properties = new ArrayList<>(type.properties());
        if (properties.isEmpty()) {
            out.append("Нет свойств.\n\n");
            return;
        }
        out.append("| Свойство | Категория | Тип | По умолчанию | Описание |\n| --- | --- | --- | --- | --- |\n");
        for (PropertyInfo<? super T, ?> property : properties) {
            out.append("| `").append(escape(property.name())).append("` | ")
                    .append(cell(property.category()))
                    .append(" | ").append(cell(property.kind().name()))
                    .append(" | ").append(cell(property.defaultValue()))
                    .append(" | ").append(cell(property.description()))
                    .append(" |\n");
        }
        out.append('\n');
    }

    private static String code(String value) {
        return value == null || value.isBlank() ? "—" : "`" + value + "`";
    }

    private static String cell(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "—" : escape(normalized);
    }

    private static String escape(String value) {
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }
}
