package dev.sixik.unigui.api.xml;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Внутренний validator source XML tree против descriptor metadata реестра.
 *
 * <p>Validator используется только editor-mode загрузкой. Он не материализует runtime widgets,
 * а проверяет структуру документа: известность типов, атрибуты, property-child элементы,
 * допустимость children и повторяющиеся id/name.</p>
 */
final class XmlWidgetDocumentValidator {
    private XmlWidgetDocumentValidator() {
    }

    /**
     * Валидирует source tree и возвращает immutable список diagnostics.
     *
     * @param root корневой XML-элемент документа
     * @param registry реестр XML descriptor-ов
     * @return список найденных diagnostics
     */
    static List<XmlWidgetDiagnostic> validate(XmlWidgetElement root, XmlWidgetRegistry registry) {
        List<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        validateElement(root, registry, diagnostics, new HashMap<>(), null, 0);
        return List.copyOf(diagnostics);
    }

    private static void validateElement(XmlWidgetElement element,
                                        XmlWidgetRegistry registry,
                                        List<XmlWidgetDiagnostic> diagnostics,
                                        Map<String, XmlWidgetElement> ids,
                                        XmlWidgetDescriptor parentDescriptor,
                                        int depth) {
        if (depth == 0 && propertyElementName(element.name()) != null) {
            add(diagnostics, element, "Property element '" + element.name() + "' cannot be used as XML root.");
        }

        XmlPropertyElement propertyElement = propertyElementName(element.name());
        if (propertyElement != null) {
            // Property-element синтаксис валиден только внутри owner widget-а и описывается descriptor-ом родителя.
            validatePropertyElement(element, propertyElement, parentDescriptor, registry, diagnostics, ids, depth);
            return;
        }

        Optional<XmlWidgetDescriptor> descriptor = registry.descriptor(element.name());
        if (descriptor.isEmpty()) {
            add(diagnostics, element, "Unknown widget type '" + element.name() + "'.");
            validateUnknownSubtree(element, registry, diagnostics, ids, depth);
            return;
        }

        validateAttributes(element, descriptor.get(), diagnostics, ids);
        validateNormalChildren(element, descriptor.get(), registry, diagnostics, ids, depth);
    }

    private static void validatePropertyElement(XmlWidgetElement element,
                                                XmlPropertyElement propertyElement,
                                                XmlWidgetDescriptor parentDescriptor,
                                                XmlWidgetRegistry registry,
                                                List<XmlWidgetDiagnostic> diagnostics,
                                                Map<String, XmlWidgetElement> ids,
                                                int depth) {
        if (parentDescriptor == null) {
            add(diagnostics, element, "Property element '" + element.name() + "' must be nested inside a widget element.");
        } else {
            Optional<XmlWidgetDescriptor> owner = registry.descriptor(propertyElement.ownerName());
            if (owner.isEmpty() || !owner.get().xmlName().equals(parentDescriptor.xmlName())) {
                add(diagnostics, element, "Property element '" + element.name()
                        + "' cannot be used inside " + parentDescriptor.xmlName() + ".");
            } else if (!hasPropertyChild(parentDescriptor, propertyElement.propertyName())) {
                add(diagnostics, element, "Unknown property element '" + element.name() + "' on "
                        + parentDescriptor.xmlName() + ".");
            }
        }

        for (XmlWidgetAttribute attribute : element.attributes()) {
            if (!isNamespaceDeclaration(attribute)) {
                add(diagnostics, attribute, "Property element '" + element.name()
                        + "' cannot have attribute '" + attribute.name() + "'.");
            }
        }

        for (XmlWidgetNode child : element.children()) {
            if (child instanceof XmlWidgetText text && !text.text().isBlank()) {
                add(diagnostics, element, "Property element '" + element.name() + "' cannot contain text content.");
            } else if (child instanceof XmlWidgetElement childElement) {
                if (propertyElementName(childElement.name()) != null) {
                    add(diagnostics, childElement, "Property element '" + element.name()
                            + "' must contain widget elements, got property element '" + childElement.name() + "'.");
                    validateElement(childElement, registry, diagnostics, ids, parentDescriptor, depth + 1);
                } else {
                    validateElement(childElement, registry, diagnostics, ids, null, depth + 1);
                }
            }
        }
    }

    private static void validateNormalChildren(XmlWidgetElement element,
                                               XmlWidgetDescriptor descriptor,
                                               XmlWidgetRegistry registry,
                                               List<XmlWidgetDiagnostic> diagnostics,
                                               Map<String, XmlWidgetElement> ids,
                                               int depth) {
        for (XmlWidgetNode child : element.children()) {
            if (child instanceof XmlWidgetElement childElement) {
                if (propertyElementName(childElement.name()) != null) {
                    validateElement(childElement, registry, diagnostics, ids, descriptor, depth + 1);
                } else {
                    if (!descriptor.acceptsChildren()) {
                        add(diagnostics, childElement, "Widget " + descriptor.xmlName()
                                + " cannot contain child " + childElement.name() + ".");
                    }
                    validateElement(childElement, registry, diagnostics, ids, null, depth + 1);
                }
            }
        }
    }

    private static void validateUnknownSubtree(XmlWidgetElement element,
                                               XmlWidgetRegistry registry,
                                               List<XmlWidgetDiagnostic> diagnostics,
                                               Map<String, XmlWidgetElement> ids,
                                               int depth) {
        validateIdAttributes(element, diagnostics, ids);
        for (XmlWidgetNode child : element.children()) {
            if (child instanceof XmlWidgetElement childElement) {
                validateElement(childElement, registry, diagnostics, ids, null, depth + 1);
            }
        }
    }

    private static void validateAttributes(XmlWidgetElement element,
                                           XmlWidgetDescriptor descriptor,
                                           List<XmlWidgetDiagnostic> diagnostics,
                                           Map<String, XmlWidgetElement> ids) {
        Set<String> knownAttributes = new HashSet<>();
        for (XmlAttributeDescriptor attribute : descriptor.attributes()) {
            knownAttributes.add(attribute.name());
        }

        Set<String> seenAttributes = new HashSet<>();
        for (XmlWidgetAttribute attribute : element.attributes()) {
            // Namespace declarations разрешены даже если descriptor о них не знает.
            if (isNamespaceDeclaration(attribute)) continue;
            // id/name являются editor-level identity attributes и не требуют регистрации на каждом виджете.
            if (isIdAttribute(attribute)) continue;

            String normalizedName = localName(attribute.name());
            if (!knownAttributes.contains(normalizedName)) {
                add(diagnostics, attribute, "Unknown attribute '" + attribute.name() + "' on " + descriptor.xmlName() + ".");
            } else if (!seenAttributes.add(normalizedName)) {
                add(diagnostics, attribute, "Duplicate attribute '" + attribute.name() + "' on " + descriptor.xmlName() + ".");
            }
        }
        validateIdAttributes(element, diagnostics, ids);
    }

    private static void validateIdAttributes(XmlWidgetElement element,
                                             List<XmlWidgetDiagnostic> diagnostics,
                                             Map<String, XmlWidgetElement> ids) {
        String id = null;
        for (XmlWidgetAttribute attribute : element.attributes()) {
            if (!isIdAttribute(attribute)) continue;
            String value = attribute.value().trim();
            if (id != null && !id.equals(value)) {
                add(diagnostics, attribute, "Conflicting id attributes on " + element.name() + ".");
            }
            id = value;
        }
        if (id == null || id.isEmpty()) return;
        XmlWidgetElement previous = ids.putIfAbsent(id, element);
        if (previous != null && previous != element) {
            add(diagnostics, element, "Duplicate widget id '" + id + "'.");
        }
    }

    private static boolean hasPropertyChild(XmlWidgetDescriptor descriptor, String propertyName) {
        for (XmlPropertyChildDescriptor propertyChild : descriptor.propertyChildren()) {
            if (propertyChild.name().equals(propertyName)) return true;
        }
        return false;
    }

    private static XmlPropertyElement propertyElementName(String xmlName) {
        int dot = xmlName.indexOf('.');
        if (dot < 0) return null;
        if (dot == 0 || dot == xmlName.length() - 1 || xmlName.indexOf('.', dot + 1) >= 0) {
            return new XmlPropertyElement("", "");
        }
        return new XmlPropertyElement(xmlName.substring(0, dot), xmlName.substring(dot + 1));
    }

    private static boolean isIdAttribute(XmlWidgetAttribute attribute) {
        String name = localName(attribute.name());
        return "id".equals(name) || "name".equals(name) || "Name".equals(name);
    }

    private static boolean isNamespaceDeclaration(XmlWidgetAttribute attribute) {
        String name = attribute.name();
        return XMLConstants.XMLNS_ATTRIBUTE.equals(name)
                || name.startsWith(XMLConstants.XMLNS_ATTRIBUTE + ":");
    }

    private static String localName(String name) {
        int prefix = name.indexOf(':');
        return prefix >= 0 ? name.substring(prefix + 1) : name;
    }

    private static void add(List<XmlWidgetDiagnostic> diagnostics, XmlWidgetElement element, String message) {
        diagnostics.add(new XmlWidgetDiagnostic(message, element.line(), element.column()));
    }

    private static void add(List<XmlWidgetDiagnostic> diagnostics, XmlWidgetAttribute attribute, String message) {
        diagnostics.add(new XmlWidgetDiagnostic(message, attribute.line(), attribute.column()));
    }

    private record XmlPropertyElement(String ownerName, String propertyName) {
    }
}
