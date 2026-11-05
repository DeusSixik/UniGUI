package dev.sixik.unigui.xaml.format;

import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Записывает {@link UiDocument} в текст XAML.
 *
 * <p>Результат читается {@link XamlReader} обратно в эквивалентное по смыслу дерево
 * ({@link UiNode#sameStructure(UiNode)}). Не сохраняется только то, что XAML считает форматированием:
 * пробельный текст между элементами, внутренние пробелы смешанного текста и порядок между атрибутной и
 * элементной формами свойств (элементные идут первыми в теле).</p>
 */
final class XamlWriter {
    private XamlWriter() {
    }

    static String write(UiDocument document, UiFormat.WriteOptions options) {
        StringBuilder out = new StringBuilder();
        if (options.declaration()) out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        for (String comment : document.prologComments()) out.append("<!--").append(comment).append("-->\n");
        writeNode(out, document.root(), 0, options);
        out.append('\n');
        return out.toString();
    }

    /**
     * Пишет узел начиная с текущей позиции: отступ перед ним ставит вызывающий.
     */
    private static void writeNode(StringBuilder out, UiNode node, int depth, UiFormat.WriteOptions options) {
        String name = node.type().qualified();

        List<String> attributes = new ArrayList<>();
        for (Map.Entry<String, String> declaration : node.namespaces().entrySet()) {
            String attribute = declaration.getKey().isEmpty() ? "xmlns" : "xmlns:" + declaration.getKey();
            attributes.add(attribute + "=\"" + escapeAttribute(declaration.getValue()) + "\"");
        }
        List<UiProperty> elementProperties = new ArrayList<>();
        for (UiProperty property : node.properties()) {
            if (property.form() == UiProperty.Form.ELEMENT || property.value() instanceof UiValue.Elements) {
                elementProperties.add(property);
            } else {
                attributes.add(property.displayName() + "=\"" + escapeAttribute(MarkupExtensions.write(property.value())) + "\"");
            }
        }

        List<UiContent> content = node.content();
        boolean hasBody = !elementProperties.isEmpty() || !content.isEmpty();

        out.append('<').append(name);
        int inlineLength = depth * options.indent() + 1 + name.length() + (hasBody ? 1 : 2);
        for (String attribute : attributes) inlineLength += 1 + attribute.length();
        boolean wrap = attributes.size() > 1 && inlineLength > options.maxLineLength();
        for (String attribute : attributes) {
            if (wrap) {
                out.append('\n');
                indent(out, depth + 1, options);
            } else {
                out.append(' ');
            }
            out.append(attribute);
        }

        if (!hasBody) {
            out.append("/>");
            return;
        }
        out.append('>');

        boolean onlyText = elementProperties.isEmpty();
        for (UiContent item : content) {
            if (!(item instanceof UiContent.Text)) onlyText = false;
        }
        if (onlyText) {
            for (UiContent item : content) out.append(escapeText(((UiContent.Text) item).value()));
            out.append("</").append(name).append('>');
            return;
        }

        for (UiProperty property : elementProperties) {
            out.append('\n');
            indent(out, depth + 1, options);
            writePropertyElement(out, node, property, depth + 1, options);
        }
        for (UiContent item : content) writeContent(out, item, depth + 1, options);
        out.append('\n');
        indent(out, depth, options);
        out.append("</").append(name).append('>');
    }

    private static void writeContent(StringBuilder out, UiContent item, int depth, UiFormat.WriteOptions options) {
        if (item instanceof UiNode child) {
            out.append('\n');
            indent(out, depth, options);
            writeNode(out, child, depth, options);
        } else if (item instanceof UiContent.Text text) {
            if (text.blank()) return;
            out.append('\n');
            indent(out, depth, options);
            out.append(escapeText(text.value().trim()));
        } else if (item instanceof UiContent.Comment comment) {
            out.append('\n');
            indent(out, depth, options);
            out.append("<!--").append(comment.value()).append("-->");
        }
    }

    private static void writePropertyElement(StringBuilder out, UiNode node, UiProperty property, int depth,
                                             UiFormat.WriteOptions options) {
        String ownerName = property.owner() != null ? property.owner().qualified() : node.type().qualified();
        String tag = ownerName + "." + property.name().local();
        out.append('<').append(tag).append('>');
        if (property.value() instanceof UiValue.Elements elements) {
            for (UiContent item : elements.content()) writeContent(out, item, depth + 1, options);
            out.append('\n');
            indent(out, depth, options);
        } else {
            out.append(escapeText(MarkupExtensions.write(property.value())));
        }
        out.append("</").append(tag).append('>');
    }

    private static void indent(StringBuilder out, int depth, UiFormat.WriteOptions options) {
        out.append(" ".repeat(depth * options.indent()));
    }

    private static String escapeText(String text) {
        StringBuilder out = null;
        for (int i = 0; i < text.length(); i++) {
            String replacement = switch (text.charAt(i)) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                default -> null;
            };
            if (replacement != null && out == null) out = new StringBuilder(text.length() + 16).append(text, 0, i);
            if (out != null) {
                if (replacement != null) out.append(replacement);
                else out.append(text.charAt(i));
            }
        }
        return out == null ? text : out.toString();
    }

    private static String escapeAttribute(String text) {
        StringBuilder out = new StringBuilder(text.length() + 8);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '"' -> out.append("&quot;");
                case '\n' -> out.append("&#10;");
                case '\r' -> out.append("&#13;");
                case '\t' -> out.append("&#9;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
