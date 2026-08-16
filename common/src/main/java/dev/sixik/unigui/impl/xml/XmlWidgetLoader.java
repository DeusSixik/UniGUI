package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DOM-загрузчик, который превращает XML-описание в обычное дерево виджетов UniGUI.
 *
 * <p>Класс отвечает за безопасный парсинг XML, проверку типов/атрибутов,
 * применение значений и сборку иерархии с диагностикой по строкам исходника.</p>
 */
public final class XmlWidgetLoader {
    private static final String XMLNS_URI = XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    private static final String LOCATION_KEY = XmlWidgetLoader.class.getName() + ".location";

    private final WidgetXmlRegistry registry;
    private final int maxDepth;
    private final boolean strictAttributes;
    private final dev.sixik.unigui.api.xml.XmlTextureResolver textureResolver;
    private final dev.sixik.unigui.api.xml.XmlCommandRegistry commands;

    public XmlWidgetLoader(WidgetXmlRegistry registry) {
        this(registry, XmlWidgetOptions.DEFAULT);
    }

    public XmlWidgetLoader(WidgetXmlRegistry registry, int maxDepth) {
        this(registry, XmlWidgetOptions.DEFAULT.maxDepth(maxDepth));
    }

    public XmlWidgetLoader(WidgetXmlRegistry registry, XmlWidgetOptions options) {
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        XmlWidgetOptions normalizedOptions = options == null ? XmlWidgetOptions.DEFAULT : options;
        this.registry = registry;
        this.maxDepth = normalizedOptions.maxDepth();
        this.strictAttributes = normalizedOptions.strictAttributes();
        this.textureResolver = normalizedOptions.textureResolver();
        this.commands = normalizedOptions.commands();
    }

    public Widget load(String xml) {
        if (xml == null || xml.isBlank()) {
            throw fail("XML widget source must not be blank.");
        }
        try (XmlValueParsers.TextureResolverScope ignored = XmlValueParsers.pushLoadContext(textureResolver, commands)) {
            Document document = parse(xml);
            Element root = document.getDocumentElement();
            if (root == null) throw fail("XML document does not contain a root widget.");
            return readElement(root, new HashMap<>(), 0);
        }
    }

    private Widget readElement(Element element, Map<String, Widget> ids, int depth) {
        if (depth > maxDepth) {
            throw fail(element, "XML widget tree is deeper than the configured limit of " + maxDepth + ".");
        }

        String xmlName = elementName(element);
        if (propertyElementName(xmlName, element) != null) {
            throw fail(element, "Property element '" + xmlName + "' cannot be used as XML root.");
        }
        WidgetXmlType<? extends Widget> rawType = registry.type(xmlName);
        if (rawType == null) {
            throw fail(element, "Unknown widget type '" + xmlName + "'.");
        }

        return readTypedElement(element, rawType, ids, depth);
    }

    private <T extends Widget> T readTypedElement(Element element,
                                                  WidgetXmlType<T> type,
                                                  Map<String, Widget> ids,
                                                  int depth) {
        T widget = type.create();
        Map<String, XmlAttributeValue> values = collectAttributes(element, type);
        String id = idAttribute(element);
        if (id != null && !id.isEmpty()) {
            if (ids.putIfAbsent(id, widget) != null) {
                throw fail(element, "Duplicate widget id '" + id + "'.");
            }
            widget.id(id);
        }

        try {
            type.applyAttributes(widget, values);
        } catch (WidgetXmlType.XmlAttributeApplyException failure) {
            throw fail("Cannot apply attribute on " + type.xmlName() + ": " + failure.getMessage(),
                    failure.line(), failure.column(), failure);
        } catch (RuntimeException failure) {
            throw fail(element, "Cannot apply attribute on " + type.xmlName() + ": " + failure.getMessage(), failure);
        }

        TextContent textContent = textContent(element);
        if (!values.containsKey("text") && !textContent.value().isEmpty()) {
            applyTextContent(element, widget, type, textContent.value());
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element childElement) {
                if (propertyElementName(elementName(childElement), childElement) != null) {
                    readPropertyElement(childElement, type, widget, ids, depth);
                    continue;
                }
                Widget child = readElement(childElement, ids, depth + 1);
                try {
                    type.addChild(widget, child);
                } catch (RuntimeException failure) {
                    throw fail(childElement, failure.getMessage(), failure);
                }
            }
        }

        if (!type.acceptsChildren() && textContent.hasNonWhitespace() && !isTextLike(widget)) {
            throw fail(element, "Widget " + type.xmlName() + " cannot contain text content.");
        }

        return widget;
    }

    private <T extends Widget> void readPropertyElement(Element element,
                                                        WidgetXmlType<T> type,
                                                        T widget,
                                                        Map<String, Widget> ids,
                                                        int depth) {
        String xmlName = elementName(element);
        XmlPropertyElement propertyElement = propertyElementName(xmlName, element);
        if (propertyElement == null) return;

        WidgetXmlType<? extends Widget> ownerType = registry.type(propertyElement.ownerName());
        if (ownerType != type) {
            throw fail(element, "Property element '" + xmlName + "' cannot be used inside " + type.xmlName() + ".");
        }
        if (!type.hasPropertyChild(propertyElement.propertyName())) {
            throw fail(element, "Unknown property element '" + xmlName + "' on " + type.xmlName() + ".");
        }

        validatePropertyElementAttributes(element, xmlName);
        TextContent textContent = textContent(element);
        if (textContent.hasNonWhitespace()) {
            throw fail(element, "Property element '" + xmlName + "' cannot contain text content.");
        }

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element childElement) {
                String childXmlName = elementName(childElement);
                if (propertyElementName(childXmlName, childElement) != null) {
                    throw fail(childElement, "Property element '" + xmlName
                            + "' must contain widget elements, got property element '" + childXmlName + "'.");
                }
                Widget child = readElement(childElement, ids, depth + 1);
                try {
                    type.addPropertyChild(widget, propertyElement.propertyName(), child);
                } catch (RuntimeException failure) {
                    throw fail(childElement, failure.getMessage(), failure);
                }
            }
        }
    }

    private static void validatePropertyElementAttributes(Element element, String xmlName) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (isNamespaceDeclaration(attribute)) continue;
            throw fail(attribute, "Property element '" + xmlName + "' cannot have attribute '" + displayName(attribute) + "'.");
        }
    }

    private Map<String, XmlAttributeValue> collectAttributes(Element element, WidgetXmlType<?> type) {
        Map<String, XmlAttributeValue> values = new LinkedHashMap<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (isNamespaceDeclaration(attribute)) continue;
            if (isIdAttribute(attribute)) continue;

            String name = attributeName(attribute);
            if (!type.hasAttribute(name)) {
                if (strictAttributes) {
                    throw fail(attribute, "Unknown attribute '" + displayName(attribute) + "' on " + type.xmlName() + ".");
                }
                continue;
            }
            XmlSourceLocation location = location(attribute);
            if (values.putIfAbsent(name, new XmlAttributeValue(
                    displayName(attribute),
                    attribute.getNodeValue(),
                    location.line(),
                    location.column())) != null) {
                throw fail(attribute, "Duplicate attribute '" + displayName(attribute) + "' on " + type.xmlName() + ".");
            }
        }
        return values;
    }

    private static void applyTextContent(Element element, Widget widget, WidgetXmlType<?> type, String text) {
        if (widget instanceof TextWidget textWidget) {
            textWidget.text(text);
        } else if (widget instanceof Button button) {
            button.text(text);
        } else if (!text.isBlank()) {
            throw fail(element, "Widget " + type.xmlName() + " cannot contain text content.");
        }
    }

    private static TextContent textContent(Element element) {
        StringBuilder directText = new StringBuilder();
        boolean hasNonWhitespace = false;
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            short type = node.getNodeType();
            if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                String value = node.getNodeValue();
                if (value != null) {
                    directText.append(value);
                    hasNonWhitespace |= !value.isBlank();
                }
            }
        }
        return new TextContent(directText.toString().trim(), hasNonWhitespace);
    }

    private static boolean isTextLike(Widget widget) {
        return widget instanceof TextWidget || widget instanceof Button;
    }

    private Document parse(String xml) {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setNamespaceAware(true);
            Document document = documentFactory.newDocumentBuilder().newDocument();

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

            LocationDomBuilder handler = new LocationDomBuilder(document);
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            reader.parse(new InputSource(new StringReader(xml)));
            return document;
        } catch (SAXParseException failure) {
            throw fail("Cannot parse XML widget source: " + failure.getMessage(),
                    failure.getLineNumber(), failure.getColumnNumber(), failure);
        } catch (ParserConfigurationException | SAXException | IOException failure) {
            throw fail("Cannot parse XML widget source: " + failure.getMessage(), failure);
        } catch (IllegalArgumentException failure) {
            throw fail("Cannot configure XML parser securely: " + failure.getMessage(), failure);
        }
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

    private static String idAttribute(Element element) {
        String id = null;
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (!isIdAttribute(attribute)) continue;
            String value = attribute.getNodeValue() == null ? "" : attribute.getNodeValue().trim();
            if (id != null && !id.equals(value)) {
                throw fail(attribute, "Conflicting id attributes on " + elementName(element) + ".");
            }
            id = value;
        }
        return id;
    }

    private static boolean isIdAttribute(Node attribute) {
        String name = attributeName(attribute);
        if ("id".equals(name)) return true;
        if ("name".equals(name)) return true;
        return "Name".equals(name);
    }

    private static boolean isNamespaceDeclaration(Node attribute) {
        return XMLNS_URI.equals(attribute.getNamespaceURI())
                || "xmlns".equals(attribute.getNodeName())
                || "xmlns".equals(attribute.getPrefix());
    }

    private static String elementName(Element element) {
        String localName = element.getLocalName();
        if (localName != null && !localName.isEmpty()) return localName;
        String name = element.getNodeName();
        int prefix = name.indexOf(':');
        return prefix >= 0 ? name.substring(prefix + 1) : name;
    }

    private static XmlPropertyElement propertyElementName(String xmlName, Node locationNode) {
        int dot = xmlName.indexOf('.');
        if (dot < 0) return null;
        if (dot == 0 || dot == xmlName.length() - 1 || xmlName.indexOf('.', dot + 1) >= 0) {
            throw fail(locationNode, "Invalid property element '" + xmlName + "'. Expected Owner.Property.");
        }
        return new XmlPropertyElement(xmlName.substring(0, dot), xmlName.substring(dot + 1));
    }

    private static String attributeName(Node attribute) {
        String localName = attribute.getLocalName();
        if (localName != null && !localName.isEmpty()) return localName;
        String name = attribute.getNodeName();
        int prefix = name.indexOf(':');
        return prefix >= 0 ? name.substring(prefix + 1) : name;
    }

    private static String displayName(Node attribute) {
        return attribute.getNodeName() == null ? attributeName(attribute) : attribute.getNodeName();
    }

    private static XmlSourceLocation location(Node node) {
        if (node == null) return XmlSourceLocation.UNKNOWN;
        Object value = node.getUserData(LOCATION_KEY);
        if (value instanceof XmlSourceLocation location) return location;
        if (node instanceof Attr attr) return location(attr.getOwnerElement());
        return XmlSourceLocation.UNKNOWN;
    }

    private static XmlWidgetLoadException fail(String message) {
        return new XmlWidgetLoadException(java.util.List.of(new XmlWidgetDiagnostic(message)));
    }

    private static XmlWidgetLoadException fail(String message, Throwable cause) {
        return new XmlWidgetLoadException(java.util.List.of(new XmlWidgetDiagnostic(message)), cause);
    }

    private static XmlWidgetLoadException fail(Node node, String message) {
        XmlSourceLocation location = location(node);
        return new XmlWidgetLoadException(java.util.List.of(new XmlWidgetDiagnostic(message, location.line(), location.column())));
    }

    private static XmlWidgetLoadException fail(Node node, String message, Throwable cause) {
        XmlSourceLocation location = location(node);
        return new XmlWidgetLoadException(java.util.List.of(new XmlWidgetDiagnostic(message, location.line(), location.column())), cause);
    }

    private static XmlWidgetLoadException fail(String message, int line, int column, Throwable cause) {
        return new XmlWidgetLoadException(java.util.List.of(new XmlWidgetDiagnostic(message, line, column)), cause);
    }

    record XmlAttributeValue(String displayName, String value, int line, int column) {
    }

    private record TextContent(String value, boolean hasNonWhitespace) {
    }

    private record XmlPropertyElement(String ownerName, String propertyName) {
    }

    private record XmlSourceLocation(int line, int column) {
        private static final XmlSourceLocation UNKNOWN = new XmlSourceLocation(-1, -1);
    }

    private static final class LocationDomBuilder extends DefaultHandler {
        private final Document document;
        private final ArrayDeque<Element> stack = new ArrayDeque<>();
        private Locator locator;

        private LocationDomBuilder(Document document) {
            this.document = document;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            XmlSourceLocation location = currentLocation();
            String qualifiedName = qualifiedName(localName, qName);
            Element element = uri == null || uri.isEmpty()
                    ? document.createElement(qualifiedName)
                    : document.createElementNS(uri, qualifiedName);
            element.setUserData(LOCATION_KEY, location, null);

            for (int i = 0; i < attributes.getLength(); i++) {
                String attributeUri = attributes.getURI(i);
                String attributeName = qualifiedName(attributes.getLocalName(i), attributes.getQName(i));
                if (attributeUri == null || attributeUri.isEmpty()) {
                    element.setAttribute(attributeName, attributes.getValue(i));
                    tagAttribute(element.getAttributeNode(attributeName), location);
                } else {
                    element.setAttributeNS(attributeUri, attributeName, attributes.getValue(i));
                    tagAttribute(element.getAttributeNodeNS(attributeUri, attributes.getLocalName(i)), location);
                }
            }

            append(element);
            stack.push(element);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            stack.pop();
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (length <= 0) return;
            append(document.createTextNode(new String(ch, start, length)));
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) {
            characters(ch, start, length);
        }

        private void append(Node node) {
            if (stack.isEmpty()) {
                document.appendChild(node);
            } else {
                stack.peek().appendChild(node);
            }
        }

        private XmlSourceLocation currentLocation() {
            if (locator == null) return XmlSourceLocation.UNKNOWN;
            return new XmlSourceLocation(locator.getLineNumber(), locator.getColumnNumber());
        }

        private static void tagAttribute(Attr attribute, XmlSourceLocation location) {
            if (attribute != null) attribute.setUserData(LOCATION_KEY, location, null);
        }

        private static String qualifiedName(String localName, String qName) {
            if (qName != null && !qName.isEmpty()) return qName;
            if (localName != null && !localName.isEmpty()) return localName;
            return "element";
        }
    }
}
