package dev.sixik.unigui.api.xml;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель исходного XML-документа для редактора и round-trip сценариев.
 *
 * <p>Модель сохраняет структуру, атрибуты, текстовые узлы и комментарии так,
 * чтобы документ можно было редактировать и сериализовать обратно без потери контекста.
 * В отличие от {@link XMLWidget}, этот класс не создаёт runtime-виджеты, а хранит именно
 * редактируемое source-представление XML.</p>
 *
 * <p>Парсер настроен в безопасном режиме: внешние entity и DTD отключаются, а комментарии
 * сохраняются только если конкретная SAX-реализация отдаёт lexical events.</p>
 */
public final class XmlWidgetDocument {
    private final XmlWidgetElement root;

    /**
     * Создаёт документ с указанным корневым XML-элементом.
     *
     * @param root корневой элемент документа; не может быть {@code null}
     */
    public XmlWidgetDocument(XmlWidgetElement root) {
        if (root == null) throw new IllegalArgumentException("XML widget document root must not be null");
        this.root = root;
    }

    /**
     * Создаёт документ из готового корневого элемента.
     *
     * @param root корневой элемент документа
     * @return новый XML document wrapper
     */
    public static XmlWidgetDocument of(XmlWidgetElement root) {
        return new XmlWidgetDocument(root);
    }

    /**
     * Парсит XML-строку в редактируемую модель документа.
     *
     * <p>Метод строгий: пустая строка, синтаксическая ошибка или отсутствие root element
     * приводят к {@link XmlWidgetLoadException}.</p>
     *
     * @param xml исходный XML widget document
     * @return document tree с root element, атрибутами, текстом и комментариями
     */
    public static XmlWidgetDocument parse(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new XmlWidgetLoadException("XML widget document source must not be blank.");
        }
        return new XmlWidgetDocument(parseRoot(xml));
    }

    /**
     * Парсит XML для редактора и валидирует его против встроенного реестра виджетов.
     *
     * @param xml исходный XML widget document
     * @return результат с документом и нефатальными editor diagnostics
     */
    public static XmlWidgetDocumentResult parseEditor(String xml) {
        return parseEditor(xml, XmlWidgetRegistry.builtIns());
    }

    /**
     * Парсит XML для редактора и валидирует его против указанного реестра.
     *
     * <p>Синтаксические ошибки остаются фатальными, а ошибки уровня schema/descriptor
     * возвращаются как диагностики в {@link XmlWidgetDocumentResult}.</p>
     *
     * @param xml исходный XML widget document
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return результат editor parse + validation
     */
    public static XmlWidgetDocumentResult parseEditor(String xml, XmlWidgetRegistry registry) {
        return parse(xml).validate(registry);
    }

    /**
     * Читает UTF-8 XML из stream-а и парсит его в документ.
     *
     * @param xml input stream с XML-содержимым; не может быть {@code null}
     * @return document tree
     */
    public static XmlWidgetDocument parse(InputStream xml) {
        if (xml == null) {
            throw new XmlWidgetLoadException("XML widget document input stream must not be null.");
        }
        try {
            return parse(new String(xml.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot read XML widget document input stream.", failure);
        }
    }

    /**
     * Парсит stream для редактора и валидирует его против встроенного реестра.
     *
     * @param xml input stream с UTF-8 XML
     * @return результат editor parse + validation
     */
    public static XmlWidgetDocumentResult parseEditor(InputStream xml) {
        return parseEditor(xml, XmlWidgetRegistry.builtIns());
    }

    /**
     * Парсит stream для редактора и валидирует его против указанного реестра.
     *
     * @param xml input stream с UTF-8 XML
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return результат editor parse + validation
     */
    public static XmlWidgetDocumentResult parseEditor(InputStream xml, XmlWidgetRegistry registry) {
        return parse(xml).validate(registry);
    }

    /**
     * Загружает XML-документ из classpath resource.
     *
     * @param resourcePath путь resource-а, с ведущим {@code /} или без него
     * @return document tree
     */
    public static XmlWidgetDocument parseResource(String resourcePath) {
        try (InputStream stream = openResource(resourcePath)) {
            return parse(stream);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget document resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Загружает XML resource для редактора и валидирует его против встроенного реестра.
     *
     * @param resourcePath путь classpath resource-а
     * @return результат editor parse + validation
     */
    public static XmlWidgetDocumentResult parseEditorResource(String resourcePath) {
        return parseEditorResource(resourcePath, XmlWidgetRegistry.builtIns());
    }

    /**
     * Загружает XML resource для редактора и валидирует его против указанного реестра.
     *
     * @param resourcePath путь classpath resource-а
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return результат editor parse + validation
     */
    public static XmlWidgetDocumentResult parseEditorResource(String resourcePath, XmlWidgetRegistry registry) {
        try (InputStream stream = openResource(resourcePath)) {
            return parseEditor(stream, registry);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot close XML widget document resource '" + resourcePath + "'.", failure);
        }
    }

    /**
     * Возвращает корневой XML-элемент документа.
     *
     * @return root element
     */
    public XmlWidgetElement root() {
        return root;
    }

    /**
     * Создаёт глубокую копию документа.
     *
     * @return новый документ с копиями всех узлов
     */
    public XmlWidgetDocument copy() {
        return new XmlWidgetDocument(root.copy());
    }

    /**
     * Валидирует source tree против XML descriptor-ов реестра.
     *
     * @param registry реестр XML-типов; {@code null} заменяется built-ins
     * @return результат с этим документом и списком найденных diagnostics
     */
    public XmlWidgetDocumentResult validate(XmlWidgetRegistry registry) {
        XmlWidgetRegistry normalized = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        return new XmlWidgetDocumentResult(this, XmlWidgetDocumentValidator.validate(root, normalized));
    }

    /**
     * Раскрывает prefab-ссылки в документе.
     *
     * <p>Исходный документ не мутируется: catalog возвращает новый результат с расширенной копией
     * или с diagnostics, если prefab нельзя найти или применить.</p>
     *
     * @param catalog каталог prefab-шаблонов; {@code null} означает пустой каталог
     * @return результат раскрытия prefabs
     */
    public XmlWidgetDocumentResult expandPrefabs(XmlWidgetPrefabCatalog catalog) {
        XmlWidgetPrefabCatalog normalized = catalog == null ? XmlWidgetPrefabCatalog.empty() : catalog;
        return normalized.expand(this);
    }

    /**
     * Сериализует документ с pretty-настройками по умолчанию.
     *
     * @return XML-строка без потери значимых узлов документа
     */
    public String toXmlString() {
        return toXmlString(XmlWidgetSerializationOptions.PRETTY);
    }

    /**
     * Сериализует документ с указанными настройками форматирования.
     *
     * @param options настройки XML declaration, indent и обработки whitespace text
     * @return XML-строка
     */
    public String toXmlString(XmlWidgetSerializationOptions options) {
        XmlWidgetSerializationOptions normalized = options == null ? XmlWidgetSerializationOptions.PRETTY : options;
        StringBuilder builder = new StringBuilder();
        if (normalized.xmlDeclaration()) {
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            newline(builder, normalized);
        }
        writeElement(builder, root, 0, normalized);
        return builder.toString();
    }

    private static XmlWidgetElement parseRoot(String xml) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeatureIfSupported(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-parameter-entities", false);

            SAXParser parser = factory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            setFeatureIfSupported(reader, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureIfSupported(reader, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureIfSupported(reader, "http://xml.org/sax/features/external-parameter-entities", false);
            setFeatureIfSupported(reader, "http://xml.org/sax/features/namespace-prefixes", true);

            DocumentBuilder handler = new DocumentBuilder();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            try {
                reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            } catch (SAXNotRecognizedException | SAXNotSupportedException ignored) {
                // Комментарии сохраняются по возможности: некоторые XML-реализации не отдают lexical events.
            }
            reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            reader.parse(new InputSource(new StringReader(xml)));

            XmlWidgetElement root = handler.root();
            if (root == null) throw new XmlWidgetLoadException("XML widget document does not contain a root element.");
            return root;
        } catch (SAXParseException failure) {
            throw new XmlWidgetLoadException(List.of(new XmlWidgetDiagnostic(
                    "Cannot parse XML widget document: " + failure.getMessage(),
                    failure.getLineNumber(),
                    failure.getColumnNumber())), failure);
        } catch (ParserConfigurationException | SAXException | IOException failure) {
            throw new XmlWidgetLoadException("Cannot parse XML widget document: " + failure.getMessage(), failure);
        }
    }

    private static void writeElement(StringBuilder builder,
                                     XmlWidgetElement element,
                                     int depth,
                                     XmlWidgetSerializationOptions options) {
        indent(builder, depth, options);
        builder.append('<').append(element.name());
        for (XmlWidgetAttribute attribute : element.attributes()) {
            builder.append(' ')
                    .append(attribute.name())
                    .append("=\"")
                    .append(escapeAttribute(attribute.value()))
                    .append('"');
        }

        List<XmlWidgetNode> children = significantChildren(element, options);
        if (children.isEmpty()) {
            builder.append(" />");
            newline(builder, options);
            return;
        }

        XmlWidgetText simpleText = simpleText(children);
        if (simpleText != null) {
            builder.append('>')
                    .append(escapeText(simpleText.text()))
                    .append("</")
                    .append(element.name())
                    .append('>');
            newline(builder, options);
            return;
        }

        builder.append('>');
        newline(builder, options);
        for (XmlWidgetNode child : children) {
            switch (child.kind()) {
                case ELEMENT -> writeElement(builder, (XmlWidgetElement) child, depth + 1, options);
                case COMMENT -> writeComment(builder, (XmlWidgetComment) child, depth + 1, options);
                case TEXT -> writeText(builder, (XmlWidgetText) child, depth + 1, options);
            }
        }
        indent(builder, depth, options);
        builder.append("</").append(element.name()).append('>');
        newline(builder, options);
    }

    private static void writeComment(StringBuilder builder,
                                     XmlWidgetComment comment,
                                     int depth,
                                     XmlWidgetSerializationOptions options) {
        indent(builder, depth, options);
        builder.append("<!--").append(comment.text()).append("-->");
        newline(builder, options);
    }

    private static void writeText(StringBuilder builder,
                                  XmlWidgetText text,
                                  int depth,
                                  XmlWidgetSerializationOptions options) {
        String value = options.preserveWhitespaceText() ? text.text() : text.text().trim();
        if (value.isEmpty() && !options.preserveWhitespaceText()) return;
        indent(builder, depth, options);
        builder.append(escapeText(value));
        newline(builder, options);
    }

    private static List<XmlWidgetNode> significantChildren(XmlWidgetElement element, XmlWidgetSerializationOptions options) {
        List<XmlWidgetNode> children = new ArrayList<>();
        for (XmlWidgetNode child : element.children()) {
            if (!options.preserveWhitespaceText()
                    && child instanceof XmlWidgetText text
                    && text.text().isBlank()) {
                continue;
            }
            children.add(child);
        }
        return children;
    }

    private static XmlWidgetText simpleText(List<XmlWidgetNode> children) {
        if (children.size() != 1 || !(children.get(0) instanceof XmlWidgetText text)) return null;
        return text.text().contains("\n") || text.text().contains("\r") ? null : text;
    }

    private static void indent(StringBuilder builder, int depth, XmlWidgetSerializationOptions options) {
        if (options.indent().isEmpty()) return;
        builder.append(options.indent().repeat(Math.max(0, depth)));
    }

    private static void newline(StringBuilder builder, XmlWidgetSerializationOptions options) {
        if (!options.indent().isEmpty()) builder.append('\n');
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;");
    }

    private static String escapeText(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static InputStream openResource(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new XmlWidgetLoadException("XML widget document resource path must not be blank.");
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextLoader == null ? null : contextLoader.getResourceAsStream(normalized);
        if (stream == null) {
            stream = XmlWidgetDocument.class.getClassLoader().getResourceAsStream(normalized);
        }
        if (stream == null) {
            throw new XmlWidgetLoadException("XML widget document resource '" + resourcePath + "' was not found.");
        }
        return stream;
    }

    private static void setFeatureIfSupported(SAXParserFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException ignored) {
            // Не все XML-реализации поддерживают каждый флаг защиты парсера.
        }
    }

    private static void setFeatureIfSupported(XMLReader reader, String feature, boolean value) {
        try {
            reader.setFeature(feature, value);
        } catch (SAXNotRecognizedException | SAXNotSupportedException ignored) {
            // Не все XML-реализации поддерживают каждый флаг защиты парсера.
        }
    }

    private static final class DocumentBuilder extends DefaultHandler implements LexicalHandler {
        private final ArrayDeque<XmlWidgetElement> stack = new ArrayDeque<>();
        private XmlWidgetElement root;
        private Locator locator;
        private boolean inDtd;

        private XmlWidgetElement root() {
            return root;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            XmlSourceLocation location = currentLocation();
            XmlWidgetElement element = new XmlWidgetElement(qualifiedName(localName, qName), location.line(), location.column());
            for (int i = 0; i < attributes.getLength(); i++) {
                element.attribute(qualifiedName(attributes.getLocalName(i), attributes.getQName(i)),
                        attributes.getValue(i),
                        location.line(),
                        location.column());
            }
            if (stack.isEmpty()) {
                root = element;
            } else {
                stack.peek().addElement(element);
            }
            stack.push(element);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            stack.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (length <= 0 || stack.isEmpty() || inDtd) return;
            stack.peek().addText(new String(ch, start, length));
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            characters(ch, start, length);
        }

        @Override
        public void comment(char[] ch, int start, int length) {
            if (stack.isEmpty() || inDtd) return;
            stack.peek().addComment(new String(ch, start, length));
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) {
            inDtd = true;
        }

        @Override
        public void endDTD() {
            inDtd = false;
        }

        @Override
        public void startEntity(String name) {
        }

        @Override
        public void endEntity(String name) {
        }

        @Override
        public void startCDATA() {
        }

        @Override
        public void endCDATA() {
        }

        private static String qualifiedName(String localName, String qName) {
            if (qName != null && !qName.isEmpty()) return qName;
            if (localName != null && !localName.isEmpty()) return localName;
            return "element";
        }

        private XmlSourceLocation currentLocation() {
            if (locator == null) return XmlSourceLocation.UNKNOWN;
            return new XmlSourceLocation(locator.getLineNumber(), locator.getColumnNumber());
        }
    }

    private record XmlSourceLocation(int line, int column) {
        private static final XmlSourceLocation UNKNOWN = new XmlSourceLocation(-1, -1);
    }
}
