package dev.sixik.unigui.xaml.format;

import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Читатель XAML на собственном токенизаторе.
 *
 * <p>Зачем не JAXP: инициализация SAX/DOM стоит десятки миллисекунд на первой загрузке, а SAX не сообщает
 * позицию отдельных атрибутов, которая нужна диагностике редактора. Здесь разбор идёт за один проход по
 * строке, каждая ошибка и каждое свойство получают точную позицию {@code строка:колонка}.</p>
 *
 * <p>Безопасность: {@code <!DOCTYPE>} запрещён, пользовательские entity не поддерживаются, раскрываются
 * только пять предопределённых и числовые ссылки, поэтому XXE и «billion laughs» невозможны по построению.</p>
 */
final class XamlReader {
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

    private final String source;
    private final int maxDepth;
    private int pos;
    private int line = 1;
    private int column = 1;

    XamlReader(CharSequence source, int maxDepth) {
        this.source = source.toString();
        this.maxDepth = maxDepth;
    }

    UiDocument read() {
        if (source.isBlank()) throw fail("XAML source must not be blank", SourceLocation.UNKNOWN);
        if (peek() == '﻿') advance();

        List<String> prolog = new ArrayList<>();
        skipMisc(prolog);
        if (eof() || peek() != '<') throw fail("Document does not contain a root element", location());
        UiNode root = readElement(Scope.ROOT, 0);
        List<String> epilog = new ArrayList<>();
        skipMisc(epilog);
        if (!eof()) throw fail("Unexpected content after the root element", location());
        return new UiDocument(root, prolog);
    }

    // ------------------------------------------------------------------ prolog / misc

    private void skipMisc(List<String> comments) {
        while (true) {
            skipSpaces();
            if (startsWith("<?")) {
                skipProcessingInstruction();
            } else if (startsWith("<!--")) {
                comments.add(readComment());
            } else if (startsWith("<!DOCTYPE") || startsWith("<!doctype")) {
                throw fail("DOCTYPE declarations are not allowed", location());
            } else {
                return;
            }
        }
    }

    private void skipProcessingInstruction() {
        SourceLocation start = location();
        int close = source.indexOf("?>", pos);
        if (close < 0) throw fail("Unterminated processing instruction", start);
        advanceTo(close + 2);
    }

    private String readComment() {
        SourceLocation start = location();
        int close = source.indexOf("-->", pos + 4);
        if (close < 0) throw fail("Unterminated comment", start);
        String value = source.substring(pos + 4, close);
        advanceTo(close + 3);
        return value;
    }

    // ------------------------------------------------------------------ elements

    private UiNode readElement(Scope parent, int depth) {
        SourceLocation elementLocation = location();
        if (depth >= maxDepth) {
            throw fail("Widget tree is deeper than the configured limit of " + maxDepth, elementLocation);
        }
        expect('<');
        String qualifiedName = readName("element name");

        List<RawAttribute> attributes = new ArrayList<>();
        boolean selfClosing = false;
        while (true) {
            skipSpaces();
            if (eof()) throw fail("Unexpected end of document inside <" + qualifiedName + ">", elementLocation);
            char c = peek();
            if (c == '>') {
                advance();
                break;
            }
            if (c == '/') {
                advance();
                expect('>');
                selfClosing = true;
                break;
            }
            attributes.add(readAttribute(attributes));
        }

        Scope scope = parent;
        Map<String, String> declarations = new java.util.LinkedHashMap<>();
        for (RawAttribute attribute : attributes) {
            if (attribute.name.equals("xmlns")) {
                declarations.put("", attribute.value);
            } else if (attribute.name.startsWith("xmlns:")) {
                String prefix = attribute.name.substring(6);
                if (prefix.isEmpty()) throw fail("Empty namespace prefix", attribute.location);
                declarations.put(prefix, attribute.value);
            }
        }
        if (!declarations.isEmpty()) scope = new Scope(parent, declarations);

        UiName type = resolveElementName(qualifiedName, scope, elementLocation);
        UiNode node = new UiNode(type).location(elementLocation);
        declarations.forEach(node::declareNamespace);

        for (RawAttribute attribute : attributes) {
            if (attribute.name.equals("xmlns") || attribute.name.startsWith("xmlns:")) continue;
            node.set(toProperty(attribute, scope));
        }

        if (selfClosing) return node;
        readContent(node, qualifiedName, scope, depth, elementLocation);
        return node;
    }

    private void readContent(UiNode node, String qualifiedName, Scope scope, int depth, SourceLocation start) {
        StringBuilder text = new StringBuilder();
        // Property-элементы не попадают в content(), но тоже делают окружающие пробелы форматированием.
        boolean structural = false;
        while (true) {
            if (eof()) throw fail("Unterminated element <" + qualifiedName + ">", start);
            char c = peek();
            if (c != '<') {
                readText(text);
                continue;
            }
            if (startsWith("</")) {
                flushText(node, text);
                SourceLocation endLocation = location();
                advance();
                advance();
                String endName = readName("closing tag name");
                skipSpaces();
                expect('>');
                if (!endName.equals(qualifiedName)) {
                    throw fail("Closing tag </" + endName + "> does not match <" + qualifiedName + ">", endLocation);
                }
                dropFormattingWhitespace(node, structural);
                return;
            }
            if (startsWith("<!--")) {
                flushText(node, text);
                node.add(new UiContent.Comment(readComment()));
                structural = true;
            } else if (startsWith("<![CDATA[")) {
                int close = source.indexOf("]]>", pos);
                if (close < 0) throw fail("Unterminated CDATA section", location());
                text.append(source, pos + 9, close);
                advanceTo(close + 3);
            } else if (startsWith("<?")) {
                skipProcessingInstruction();
            } else if (startsWith("<!")) {
                throw fail("Unsupported markup declaration", location());
            } else {
                flushText(node, text);
                UiNode child = readElement(scope, depth + 1);
                structural = true;
                if (child.type().local().indexOf('.') >= 0) {
                    appendPropertyElement(node, child, scope);
                } else {
                    node.add(child);
                }
            }
        }
    }

    /** Превращает дочерний {@code <Owner.Member>} в свойство родителя. */
    private void appendPropertyElement(UiNode parent, UiNode child, Scope scope) {
        UiName full = child.type();
        int dot = full.local().indexOf('.');
        if (dot == 0 || dot == full.local().length() - 1 || full.local().indexOf('.', dot + 1) >= 0) {
            throw fail("Invalid property element '" + full.qualified() + "'. Expected Owner.Property.",
                    child.location());
        }
        if (!child.properties().isEmpty()) {
            throw fail("Property element '" + full.qualified() + "' cannot have attributes: '"
                    + child.properties().get(0).displayName() + "'", child.properties().get(0).location());
        }
        UiName owner = new UiName(full.namespace(), full.local().substring(0, dot), full.prefix());
        UiName member = new UiName(full.namespace(), full.local().substring(dot + 1), full.prefix());

        // Одно свойство нельзя задать дважды: ни двумя property-элементами, ни атрибутом и элементом сразу.
        for (UiProperty existing : parent.properties()) {
            if (existing.name().local().equalsIgnoreCase(member.local())
                    && (existing.owner() == null || existing.owner().local().equals(owner.local()))) {
                throw fail("Property '" + full.qualified() + "' is set more than once", child.location());
            }
        }

        boolean hasNodes = false;
        for (UiContent item : child.content()) {
            if (!(item instanceof UiContent.Text)) hasNodes = true;
        }
        UiValue value;
        if (hasNodes) {
            value = new UiValue.Elements(child.content());
        } else {
            try {
                value = MarkupExtensions.parse(child.text().trim(), prefix -> {
                    String declared = child.namespaces().get(prefix);
                    return declared != null ? declared : scope.resolve(prefix);
                });
            } catch (IllegalArgumentException failure) {
                throw fail(failure.getMessage(), child.location());
            }
        }
        parent.set(new UiProperty(member, owner, value, UiProperty.Form.ELEMENT, child.location()));
    }

    private UiProperty toProperty(RawAttribute attribute, Scope scope) {
        String qualified = attribute.name;
        String prefix = "";
        String local = qualified;
        int colon = qualified.indexOf(':');
        if (colon >= 0) {
            prefix = qualified.substring(0, colon);
            local = qualified.substring(colon + 1);
            if (prefix.isEmpty() || local.isEmpty() || local.indexOf(':') >= 0) {
                throw fail("Invalid attribute name '" + qualified + "'", attribute.location);
            }
        }
        String namespace = "";
        if (!prefix.isEmpty()) {
            namespace = scope.resolve(prefix);
            if (namespace == null) throw fail("Unknown namespace prefix '" + prefix + "'", attribute.location);
        }

        UiName owner = null;
        UiName name;
        int dot = local.indexOf('.');
        if (dot >= 0) {
            if (dot == 0 || dot == local.length() - 1 || local.indexOf('.', dot + 1) >= 0) {
                throw fail("Invalid attached property '" + qualified + "'. Expected Owner.Property.", attribute.location);
            }
            String ownerNamespace = prefix.isEmpty() ? scope.defaultNamespace() : namespace;
            owner = new UiName(ownerNamespace, local.substring(0, dot), prefix);
            name = new UiName(ownerNamespace, local.substring(dot + 1), prefix);
        } else {
            name = new UiName(namespace, local, prefix);
        }

        UiValue value;
        try {
            value = MarkupExtensions.parse(attribute.value, scope::resolve);
        } catch (IllegalArgumentException failure) {
            throw fail("Invalid value of '" + qualified + "': " + failure.getMessage(), attribute.location);
        }
        return new UiProperty(name, owner, value, UiProperty.Form.ATTRIBUTE, attribute.location);
    }

    private UiName resolveElementName(String qualified, Scope scope, SourceLocation at) {
        int colon = qualified.indexOf(':');
        if (colon < 0) return new UiName(scope.defaultNamespace(), qualified, "");
        String prefix = qualified.substring(0, colon);
        String local = qualified.substring(colon + 1);
        if (prefix.isEmpty() || local.isEmpty() || local.indexOf(':') >= 0) {
            throw fail("Invalid element name '" + qualified + "'", at);
        }
        String namespace = scope.resolve(prefix);
        if (namespace == null) throw fail("Unknown namespace prefix '" + prefix + "'", at);
        return new UiName(namespace, local, prefix);
    }

    // ------------------------------------------------------------------ attributes

    private RawAttribute readAttribute(List<RawAttribute> existing) {
        SourceLocation at = location();
        String name = readName("attribute name");
        skipSpaces();
        if (eof() || peek() != '=') throw fail("Expected '=' after attribute '" + name + "'", location());
        advance();
        skipSpaces();
        if (eof() || (peek() != '"' && peek() != '\'')) {
            throw fail("Attribute '" + name + "' value must be quoted", location());
        }
        for (RawAttribute other : existing) {
            if (other.name.equals(name)) throw fail("Duplicate attribute '" + name + "'", at);
        }
        char quote = advance();
        StringBuilder value = new StringBuilder();
        while (true) {
            if (eof()) throw fail("Unterminated value of attribute '" + name + "'", at);
            char c = peek();
            if (c == quote) {
                advance();
                break;
            }
            if (c == '<') throw fail("'<' is not allowed in attribute values", location());
            if (c == '&') {
                value.append(readEntity());
            } else if (c == '\r') {
                advance();
                if (!eof() && peek() == '\n') advance();
                value.append(' ');
            } else if (c == '\n' || c == '\t') {
                advance();
                value.append(' ');
            } else {
                value.append(advance());
            }
        }
        return new RawAttribute(name, value.toString(), at);
    }

    // ------------------------------------------------------------------ text

    private void readText(StringBuilder out) {
        while (!eof() && peek() != '<') {
            char c = peek();
            if (c == '&') {
                out.append(readEntity());
            } else if (c == '\r') {
                advance();
                if (!eof() && peek() == '\n') advance();
                out.append('\n');
            } else {
                out.append(advance());
            }
        }
    }

    private static void flushText(UiNode node, StringBuilder text) {
        if (text.length() == 0) return;
        node.add(new UiContent.Text(text.toString()));
        text.setLength(0);
    }

    /** Если рядом с текстом есть элементы или комментарии, пробельный текст между ними — это форматирование. */
    private static void dropFormattingWhitespace(UiNode node, boolean structural) {
        if (!structural) return;
        List<UiContent> blank = new ArrayList<>();
        for (UiContent item : node.content()) {
            if (item instanceof UiContent.Text text && text.blank()) blank.add(item);
        }
        for (UiContent item : blank) node.remove(item);
    }

    private String readEntity() {
        SourceLocation at = location();
        int semicolon = source.indexOf(';', pos);
        if (semicolon < 0 || semicolon - pos > 12) throw fail("Unterminated entity reference", at);
        String body = source.substring(pos + 1, semicolon);
        String replacement;
        switch (body) {
            case "lt" -> replacement = "<";
            case "gt" -> replacement = ">";
            case "amp" -> replacement = "&";
            case "quot" -> replacement = "\"";
            case "apos" -> replacement = "'";
            default -> {
                if (!body.startsWith("#")) throw fail("Unknown entity '&" + body + ";'", at);
                try {
                    int codePoint = body.startsWith("#x") || body.startsWith("#X")
                            ? Integer.parseInt(body.substring(2), 16)
                            : Integer.parseInt(body.substring(1));
                    if (!Character.isValidCodePoint(codePoint)) throw new NumberFormatException();
                    replacement = new String(Character.toChars(codePoint));
                } catch (NumberFormatException failure) {
                    throw fail("Invalid character reference '&" + body + ";'", at);
                }
            }
        }
        advanceTo(semicolon + 1);
        return replacement;
    }

    // ------------------------------------------------------------------ low level

    private String readName(String what) {
        int begin = pos;
        SourceLocation at = location();
        while (!eof()) {
            char c = peek();
            if (Character.isWhitespace(c) || c == '/' || c == '>' || c == '=' || c == '<' || c == '"' || c == '\'') break;
            advance();
        }
        if (pos == begin) throw fail("Expected " + what, at);
        return source.substring(begin, pos);
    }

    private void expect(char expected) {
        if (eof() || peek() != expected) throw fail("Expected '" + expected + "'", location());
        advance();
    }

    private void skipSpaces() {
        while (!eof() && Character.isWhitespace(peek())) advance();
    }

    private boolean startsWith(String prefix) {
        return source.startsWith(prefix, pos);
    }

    private boolean eof() {
        return pos >= source.length();
    }

    private char peek() {
        return pos < source.length() ? source.charAt(pos) : '\0';
    }

    private char advance() {
        char c = source.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void advanceTo(int target) {
        while (pos < target) advance();
    }

    private SourceLocation location() {
        return new SourceLocation(line, column);
    }

    private static UiParseException fail(String message, SourceLocation at) {
        return new UiParseException(message, at);
    }

    private record RawAttribute(String name, String value, SourceLocation location) {
    }

    /** Цепочка объявлений namespace. */
    private static final class Scope {
        static final Scope ROOT = new Scope(null, Map.of("xml", XML_NAMESPACE));

        private final Scope parent;
        private final Map<String, String> declarations;

        Scope(Scope parent, Map<String, String> declarations) {
            this.parent = parent;
            this.declarations = new HashMap<>(declarations);
        }

        String resolve(String prefix) {
            for (Scope scope = this; scope != null; scope = scope.parent) {
                String uri = scope.declarations.get(prefix);
                if (uri != null) return uri;
            }
            return prefix.isEmpty() ? "" : null;
        }

        String defaultNamespace() {
            String uri = resolve("");
            return uri == null ? "" : uri;
        }
    }
}
