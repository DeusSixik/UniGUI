package dev.sixik.unigui.api.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Копируемый шаблон виджета исходного документа для повторного создания item/control XML.
 *
 * <p>Template хранит документ-копию и при каждом instantiate возвращает новую копию. Overrides
 * из {@link XmlWidgetTemplateValues} применяются к root element-у или к элементам с matching id/name.</p>
 */
public final class XmlWidgetTemplate {
    private final String id;
    private final XmlWidgetTemplateKind kind;
    private final XmlWidgetDocument document;

    /**
     * Создаёт template из готового документа.
     *
     * @param id стабильный id template-а
     * @param kind назначение template-а; {@code null} заменяется control
     * @param document исходный документ template-а; копируется при создании
     */
    public XmlWidgetTemplate(String id, XmlWidgetTemplateKind kind, XmlWidgetDocument document) {
        this.id = requireId(id);
        this.kind = kind == null ? XmlWidgetTemplateKind.CONTROL : kind;
        if (document == null) throw new IllegalArgumentException("XML widget template document must not be null");
        this.document = document.copy();
    }

    /**
     * Создаёт control template из XML-строки.
     *
     * @param id id template-а
     * @param xml XML source template-а
     * @return control template
     */
    public static XmlWidgetTemplate control(String id, String xml) {
        return new XmlWidgetTemplate(id, XmlWidgetTemplateKind.CONTROL, XmlWidgetDocument.parse(xml));
    }

    /**
     * Создаёт item template из XML-строки.
     *
     * @param id id template-а
     * @param xml XML source template-а
     * @return item template
     */
    public static XmlWidgetTemplate item(String id, String xml) {
        return new XmlWidgetTemplate(id, XmlWidgetTemplateKind.ITEM, XmlWidgetDocument.parse(xml));
    }

    /**
     * Возвращает id template-а.
     *
     * @return stable template id
     */
    public String id() {
        return id;
    }

    /**
     * Возвращает назначение template-а.
     *
     * @return template kind
     */
    public XmlWidgetTemplateKind kind() {
        return kind;
    }

    /**
     * Возвращает копию исходного документа template-а.
     *
     * @return document copy
     */
    public XmlWidgetDocument document() {
        return document.copy();
    }

    /**
     * Создаёт экземпляр template-а без overrides.
     *
     * @return result с document copy
     */
    public XmlWidgetDocumentResult instantiate() {
        return instantiate(XmlWidgetTemplateValues.empty());
    }

    /**
     * Создаёт экземпляр template-а и применяет attribute overrides.
     *
     * @param values набор overrides; {@code null} означает empty values
     * @return result с document copy и diagnostics по ненайденным target ids
     */
    public XmlWidgetDocumentResult instantiate(XmlWidgetTemplateValues values) {
        XmlWidgetTemplateValues normalized = values == null ? XmlWidgetTemplateValues.empty() : values;
        XmlWidgetDocument copy = document.copy();
        ArrayList<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        for (XmlWidgetTemplateValues.AttributeOverride override : normalized.attributes()) {
            Optional<XmlWidgetElement> target = override.rootTarget()
                    ? Optional.of(copy.root())
                    : findById(copy.root(), override.elementId());
            if (target.isEmpty()) {
                diagnostics.add(new XmlWidgetDiagnostic("XML template '" + id
                        + "' target id '" + override.elementId() + "' was not found."));
                continue;
            }
            target.get().setAttribute(override.name(), override.value());
        }
        return new XmlWidgetDocumentResult(copy, diagnostics);
    }

    /**
     * Создаёт экземпляр template-а, применяет overrides и валидирует документ.
     *
     * @param values набор overrides; {@code null} означает empty values
     * @param registry реестр XML descriptor-ов; {@code null} заменяется built-ins
     * @return result с diagnostics instantiate + validation
     */
    public XmlWidgetDocumentResult instantiate(XmlWidgetTemplateValues values, XmlWidgetRegistry registry) {
        XmlWidgetDocumentResult instantiated = instantiate(values);
        XmlWidgetRegistry normalized = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        List<XmlWidgetDiagnostic> validation = instantiated.document().validate(normalized).diagnostics();
        if (instantiated.diagnostics().isEmpty() && validation.isEmpty()) return instantiated;

        ArrayList<XmlWidgetDiagnostic> diagnostics = new ArrayList<>(instantiated.diagnostics());
        diagnostics.addAll(validation);
        return new XmlWidgetDocumentResult(instantiated.document(), diagnostics);
    }

    private static Optional<XmlWidgetElement> findById(XmlWidgetElement element, String id) {
        if (elementId(element).filter(id::equals).isPresent()) return Optional.of(element);
        for (XmlWidgetNode child : element.children()) {
            if (child instanceof XmlWidgetElement childElement) {
                Optional<XmlWidgetElement> found = findById(childElement, id);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> elementId(XmlWidgetElement element) {
        for (XmlWidgetAttribute attribute : element.attributes()) {
            if (idAttribute(attribute.name())) return Optional.of(attribute.value().trim()).filter(value -> !value.isEmpty());
        }
        return Optional.empty();
    }

    private static boolean idAttribute(String name) {
        String localName = localName(name);
        return "id".equals(localName) || "name".equals(localName) || "Name".equals(localName);
    }

    private static String localName(String name) {
        int prefix = name.indexOf(':');
        return prefix >= 0 ? name.substring(prefix + 1) : name;
    }

    private static String requireId(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("XML widget template id must not be blank");
        return normalized;
    }
}
