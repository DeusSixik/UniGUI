package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetAnnotations;
import dev.sixik.unigui.api.xml.XmlWidgetDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Генерирует markdown reference по XML-виджетам из runtime registry и XML-аннотаций.
 */
public final class XmlWidgetReferenceDump {
    private static final String WIDGET_PACKAGE = "dev.sixik.unigui.widgets";
    private static final String DEFAULT_OUTPUT = "XML_WIDGET_REFERENCE.md";

    private XmlWidgetReferenceDump() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0 ? Path.of(DEFAULT_OUTPUT) : Path.of(args[0]);
        XmlWidgetRegistry registry = XMLWidget.registry();
        List<WidgetEntry> builtIns = registry.descriptors().stream()
                .map(descriptor -> new WidgetEntry(descriptor, "built-in registry"))
                .sorted(WIDGET_ORDER)
                .toList();
        Set<String> builtInNames = new TreeSet<>();
        for (WidgetEntry entry : builtIns) {
            builtInNames.add(entry.descriptor().xmlName());
        }

        List<WidgetEntry> annotatedOnly = annotatedWidgetEntries().stream()
                .filter(entry -> !builtInNames.contains(entry.descriptor().xmlName()))
                .sorted(WIDGET_ORDER)
                .toList();

        String markdown = renderMarkdown(builtIns, new TreeMap<>(registry.aliases()), annotatedOnly);
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, markdown, StandardCharsets.UTF_8);
        System.out.println("Wrote " + output.toAbsolutePath()
                + " (built-ins: " + builtIns.size()
                + ", aliases: " + registry.aliases().size()
                + ", annotated-only: " + annotatedOnly.size() + ")");
    }

    private static String renderMarkdown(List<WidgetEntry> builtIns,
                                         Map<String, String> aliases,
                                         List<WidgetEntry> annotatedOnly) {
        StringBuilder builder = new StringBuilder(64_000);
        builder.append("# Справочник XML-виджетов\n\n");
        builder.append("> Сгенерировано автоматически из `XmlWidgetRegistry.builtIns()` и `@XmlWidgetName` аннотаций. ")
                .append("Не редактируй таблицы вручную; обновляй через `dumpXmlWidgetReference`.\n\n");
        builder.append("- Дата генерации: ").append(LocalDate.now()).append("\n");
        builder.append("- Файл по умолчанию: `").append(DEFAULT_OUTPUT).append("`\n");
        builder.append("- Команда Windows: `.\\gradlew.bat :1.20.1:common:dumpXmlWidgetReference`\n");
        builder.append("- Команда Unix: `./gradlew :1.20.1:common:dumpXmlWidgetReference`\n\n");

        builder.append("## Сводка\n\n");
        builder.append("| Поверхность | Количество |\n");
        builder.append("| --- | ---: |\n");
        builder.append("| Built-in XML-виджеты | ").append(builtIns.size()).append(" |\n");
        builder.append("| Built-in aliases | ").append(aliases.size()).append(" |\n");
        builder.append("| Аннотированные кандидаты вне built-ins | ").append(annotatedOnly.size()).append(" |\n\n");

        renderAliases(builder, aliases);
        renderWidgetSection(builder,
                "Built-In XML-виджеты",
                "Эти виджеты доступны в стандартном `XMLWidget.registry()` / `XmlWidgetRegistry.builtIns()`.",
                builtIns);
        renderWidgetSection(builder,
                "Аннотированные XML-кандидаты",
                "Эти классы помечены `@XmlWidgetName`, но не входят в стандартный built-in registry этого дампа. "
                        + "Их можно подключить через `registry.registerAnnotated(...)` или global registry contribution.",
                annotatedOnly);
        return builder.toString();
    }

    private static void renderAliases(StringBuilder builder, Map<String, String> aliases) {
        builder.append("## Built-In Aliases\n\n");
        if (aliases.isEmpty()) {
            builder.append("Нет зарегистрированных aliases.\n\n");
            return;
        }
        builder.append("| Alias | Цель |\n");
        builder.append("| --- | --- |\n");
        aliases.forEach((alias, target) -> builder
                .append("| `").append(escapeCell(alias)).append("` | `")
                .append(escapeCell(target)).append("` |\n"));
        builder.append('\n');
    }

    private static void renderWidgetSection(StringBuilder builder,
                                            String title,
                                            String description,
                                            List<WidgetEntry> entries) {
        builder.append("## ").append(title).append("\n\n");
        builder.append(description).append("\n\n");
        if (entries.isEmpty()) {
            builder.append("Нет виджетов в этой секции.\n\n");
            return;
        }

        Map<String, List<WidgetEntry>> byCategory = new LinkedHashMap<>();
        for (WidgetEntry entry : entries) {
            byCategory.computeIfAbsent(entry.descriptor().category(), ignored -> new ArrayList<>()).add(entry);
        }

        byCategory.forEach((category, categoryEntries) -> {
            builder.append("### ").append(category).append("\n\n");
            for (WidgetEntry entry : categoryEntries) {
                renderWidget(builder, entry);
            }
        });
    }

    private static void renderWidget(StringBuilder builder, WidgetEntry entry) {
        XmlWidgetDescriptor descriptor = entry.descriptor();
        builder.append("#### `<").append(descriptor.xmlName()).append(">`\n\n");
        builder.append("- Display name: ").append(code(descriptor.displayName())).append("\n");
        builder.append("- Источник: ").append(code(entry.source())).append("\n");
        builder.append("- Прямые children: ").append(descriptor.acceptsChildren() ? "да" : "нет").append("\n");
        if (!descriptor.description().isBlank()) {
            builder.append("- Описание: ").append(descriptor.description()).append("\n");
        }
        builder.append('\n');

        renderPropertyChildren(builder, descriptor.propertyChildren());
        renderAttributes(builder, descriptor.attributes());
    }

    private static void renderPropertyChildren(StringBuilder builder, List<XmlPropertyChildDescriptor> propertyChildren) {
        if (propertyChildren.isEmpty()) return;
        builder.append("Property children:\n\n");
        builder.append("| Slot | Display | Категория | Описание |\n");
        builder.append("| --- | --- | --- | --- |\n");
        for (XmlPropertyChildDescriptor child : propertyChildren) {
            builder.append("| `").append(escapeCell(child.name())).append("` | ")
                    .append(cell(child.displayName()))
                    .append(" | ").append(cell(child.category()))
                    .append(" | ").append(cell(child.description()))
                    .append(" |\n");
        }
        builder.append('\n');
    }

    private static void renderAttributes(StringBuilder builder, List<XmlAttributeDescriptor> attributes) {
        builder.append("Attributes:\n\n");
        if (attributes.isEmpty()) {
            builder.append("Нет XML-атрибутов.\n\n");
            return;
        }
        builder.append("| Attribute | Категория | По умолчанию | Описание |\n");
        builder.append("| --- | --- | --- | --- |\n");
        for (XmlAttributeDescriptor attribute : attributes) {
            builder.append("| `").append(escapeCell(attribute.name())).append("` | ")
                    .append(cell(attribute.category()))
                    .append(" | ").append(cell(attribute.defaultValue()))
                    .append(" | ").append(cell(attribute.description()))
                    .append(" |\n");
        }
        builder.append('\n');
    }

    private static List<WidgetEntry> annotatedWidgetEntries() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = XmlWidgetReferenceDump.class.getClassLoader();
        List<WidgetEntry> entries = new ArrayList<>();
        for (String className : annotatedWidgetClassNames(loader)) {
            loadWidgetClass(loader, className)
                    .flatMap(type -> XmlWidgetAnnotations.descriptor(type)
                            .map(descriptor -> new WidgetEntry(descriptor, type.getName())))
                    .ifPresent(entries::add);
        }
        return deduplicate(entries);
    }

    private static List<WidgetEntry> deduplicate(List<WidgetEntry> entries) {
        Map<String, WidgetEntry> byName = new LinkedHashMap<>();
        for (WidgetEntry entry : entries.stream().sorted(WIDGET_ORDER).toList()) {
            byName.putIfAbsent(entry.descriptor().xmlName(), entry);
        }
        return List.copyOf(byName.values());
    }

    private static Set<String> annotatedWidgetClassNames(ClassLoader loader) throws IOException {
        Set<String> names = new TreeSet<>();
        String packagePath = WIDGET_PACKAGE.replace('.', '/');
        Enumeration<URL> resources = loader.getResources(packagePath);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                scanFileResource(names, resource);
            } else if ("jar".equals(resource.getProtocol())) {
                scanJarResource(names, resource, packagePath);
            }
        }
        return names;
    }

    private static void scanFileResource(Set<String> names, URL resource) throws IOException {
        try {
            Path root = Path.of(resource.toURI());
            if (!Files.isDirectory(root)) return;
            try (var stream = Files.walk(root)) {
                stream.filter(path -> path.toString().endsWith(".class"))
                        .filter(path -> !path.getFileName().toString().contains("$"))
                        .forEach(path -> names.add(toClassName(root, path)));
            }
        } catch (URISyntaxException ignored) {
            // Если classpath URL нельзя превратить в Path, пропускаем этот resource.
        }
    }

    private static void scanJarResource(Set<String> names, URL resource, String packagePath) throws IOException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jar = connection.getJarFile()) {
            String prefix = packagePath + "/";
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(prefix) || !name.endsWith(".class") || name.contains("$")) continue;
                names.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
            }
        }
    }

    private static String toClassName(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString().replace('\\', '/');
        String suffix = relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
        return WIDGET_PACKAGE + "." + suffix;
    }

    @SuppressWarnings("unchecked")
    private static Optional<Class<? extends Widget>> loadWidgetClass(ClassLoader loader, String className) {
        try {
            Class<?> raw = Class.forName(className, false, loader);
            if (!Widget.class.isAssignableFrom(raw)) return Optional.empty();
            if (XmlWidgetAnnotations.widgetName(raw).isEmpty()) return Optional.empty();
            return Optional.of((Class<? extends Widget>) raw);
        } catch (ClassNotFoundException | LinkageError failure) {
            return Optional.empty();
        }
    }

    private static String code(String value) {
        return value == null || value.isBlank() ? "—" : "`" + value + "`";
    }

    private static String cell(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? "—" : escapeCell(normalized);
    }

    private static String escapeCell(String value) {
        return value.replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private record WidgetEntry(XmlWidgetDescriptor descriptor, String source) {
    }

    private static final Comparator<WidgetEntry> WIDGET_ORDER = Comparator
            .comparing((WidgetEntry entry) -> entry.descriptor().category().toLowerCase(Locale.ROOT))
            .thenComparing(entry -> entry.descriptor().xmlName().toLowerCase(Locale.ROOT));
}
