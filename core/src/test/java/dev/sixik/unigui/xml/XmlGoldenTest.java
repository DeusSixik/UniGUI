package dev.sixik.unigui.xml;

import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden-снимки, которые фиксируют наблюдаемое поведение XML/XAML-подсистемы независимо от её реализации.
 *
 * <p>Снимки специально не используют ни старый загрузчик, ни старый сериализатор в формате вывода:
 * схема реестра описывается компактным текстом, а дерево виджетов — обходом публичных аксессоров.
 * Благодаря этому тот же файл можно сравнивать с результатом нового ядра при миграции.</p>
 *
 * <p>Чтобы осознанно обновить эталон, запусти тесты с переменной окружения {@code UNIGUI_UPDATE_GOLDEN=1}.</p>
 */
final class XmlGoldenTest {
    private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE = "1".equals(System.getenv("UNIGUI_UPDATE_GOLDEN"));

    @Test
    void registrySchemaMatchesGolden() {
        XmlWidgetRegistry registry = XmlWidgetRegistry.builtIns();
        StringBuilder out = new StringBuilder();
        registry.descriptors().stream()
                .sorted(Comparator.comparing(XmlWidgetDescriptor::xmlName))
                .forEach(descriptor -> {
                    out.append(descriptor.xmlName())
                            .append(" category=").append(descriptor.category())
                            .append(" children=").append(descriptor.acceptsChildren())
                            .append('\n');
                    descriptor.attributes().stream()
                            .sorted(Comparator.comparing(XmlAttributeDescriptor::name))
                            .forEach(attribute -> out.append("  @").append(attribute.name())
                                    .append(':').append(attribute.valueType())
                                    .append(" default=").append(attribute.defaultValue())
                                    .append(" category=").append(attribute.category())
                                    .append('\n'));
                    descriptor.propertyChildren().stream()
                            .sorted(Comparator.comparing(XmlPropertyChildDescriptor::name))
                            .forEach(child -> out.append("  .").append(child.name())
                                    .append(" single=").append(child.singleChild())
                                    .append('\n'));
                });
        for (Map.Entry<String, String> alias : new TreeMap<>(registry.aliases()).entrySet()) {
            out.append("alias ").append(alias.getKey()).append(" -> ").append(alias.getValue()).append('\n');
        }
        assertGolden("registry-schema.txt", out.toString());
    }

    @Test
    void overviewDocumentTreeMatchesGolden() {
        assertDocumentGolden("overview");
    }

    @Test
    void demoDocumentTreeMatchesGolden() {
        assertDocumentGolden("xml_demo");
    }

    @Test
    void secondDemoDocumentTreeMatchesGolden() {
        assertDocumentGolden("xml_demo_2");
    }

    private static void assertDocumentGolden(String name) {
        Widget root = XMLWidget.createResource("assets/unigui/xml/" + name + ".xml");
        StringBuilder out = new StringBuilder();
        dumpTree(root, 0, out);
        assertGolden("tree-" + name + ".txt", out.toString());
    }

    /**
     * Реализационно-независимый дамп: тип, id, ключевые layout-значения и текст, затем дети.
     */
    private static void dumpTree(Widget widget, int depth, StringBuilder out) {
        out.append("  ".repeat(depth)).append(widget.getClass().getSimpleName());
        if (!widget.id().isBlank()) out.append(" #").append(widget.id());
        if (!widget.enabled()) out.append(" disabled");
        out.append(" vis=").append(widget.visibility());
        if (widget instanceof WidgetBase base) {
            LayoutStyle style = base.layoutStyle();
            out.append(" w=").append(style.width())
                    .append(" h=").append(style.height())
                    .append(" pad=").append(style.padding())
                    .append(" mar=").append(style.margin())
                    .append(" grow=").append(style.flexGrow())
                    .append(" shrink=").append(style.flexShrink())
                    .append(" opacity=").append(base.opacity());
        }
        if (widget instanceof TextWidget text) out.append(" text=\"").append(text.text()).append('"');
        if (widget instanceof Button button) out.append(" button=\"").append(button.text()).append('"');
        out.append('\n');
        if (widget instanceof ScrollView scroll) {
            if (scroll.content() != null) dumpTree(scroll.content(), depth + 1, out);
            return;
        }
        for (Widget child : widget.children()) dumpTree(child, depth + 1, out);
    }

    private static void assertGolden(String fileName, String actual) {
        String normalized = actual.replace("\r\n", "\n");
        Path file = GOLDEN_DIR.resolve(fileName);
        try {
            if (UPDATE || !Files.exists(file)) {
                Files.createDirectories(GOLDEN_DIR);
                Files.writeString(file, normalized, StandardCharsets.UTF_8);
                assertTrue(UPDATE, "Golden file " + file + " did not exist and was created; re-run to verify.");
                return;
            }
            String expected = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
            if (!expected.equals(normalized)) {
                List<String> expectedLines = expected.lines().toList();
                List<String> actualLines = normalized.lines().toList();
                for (int i = 0; i < Math.max(expectedLines.size(), actualLines.size()); i++) {
                    String left = i < expectedLines.size() ? expectedLines.get(i) : "<missing>";
                    String right = i < actualLines.size() ? actualLines.get(i) : "<missing>";
                    if (!left.equals(right)) {
                        assertEquals(left, right, fileName + " differs at line " + (i + 1)
                                + " (set UNIGUI_UPDATE_GOLDEN=1 to accept)");
                    }
                }
            }
        } catch (IOException failure) {
            throw new AssertionError("Cannot access golden file " + file, failure);
        }
    }
}
