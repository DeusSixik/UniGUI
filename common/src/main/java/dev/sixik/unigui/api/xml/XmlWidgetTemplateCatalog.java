package dev.sixik.unigui.api.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр переиспользуемых редакторских шаблонов для item/control XML исходного документа.
 *
 * <p>Используется редактором как палитра готовых элементов и настроек,
 * из которых создаются новые узлы исходного XML-документа.</p>
 */
public final class XmlWidgetTemplateCatalog {
    private final Map<String, XmlWidgetTemplate> templates = new LinkedHashMap<>();

    public static XmlWidgetTemplateCatalog empty() {
        return new XmlWidgetTemplateCatalog();
    }

    public XmlWidgetTemplateCatalog register(XmlWidgetTemplate template) {
        if (template == null) throw new IllegalArgumentException("XML widget template must not be null");
        templates.put(template.id(), template);
        return this;
    }

    public XmlWidgetTemplateCatalog control(String id, String xml) {
        return register(XmlWidgetTemplate.control(id, xml));
    }

    public XmlWidgetTemplateCatalog item(String id, String xml) {
        return register(XmlWidgetTemplate.item(id, xml));
    }

    public XmlWidgetTemplateCatalog remove(String id) {
        templates.remove(normalizeId(id));
        return this;
    }

    public boolean contains(String id) {
        return templates.containsKey(normalizeId(id));
    }

    public Optional<XmlWidgetTemplate> template(String id) {
        return Optional.ofNullable(templates.get(normalizeId(id)));
    }

    public List<String> ids() {
        return List.copyOf(templates.keySet());
    }

    public List<XmlWidgetTemplate> templates() {
        return List.copyOf(templates.values());
    }

    public List<XmlWidgetTemplate> templates(XmlWidgetTemplateKind kind) {
        XmlWidgetTemplateKind normalized = kind == null ? XmlWidgetTemplateKind.CONTROL : kind;
        return templates.values().stream().filter(template -> template.kind() == normalized).toList();
    }

    public XmlWidgetDocumentResult instantiate(String id, XmlWidgetTemplateValues values) {
        XmlWidgetTemplate template = requireTemplate(id);
        return template.instantiate(values);
    }

    public XmlWidgetDocumentResult instantiate(String id, XmlWidgetTemplateValues values, XmlWidgetRegistry registry) {
        XmlWidgetTemplate template = requireTemplate(id);
        return template.instantiate(values, registry);
    }

    private XmlWidgetTemplate requireTemplate(String id) {
        String normalized = normalizeId(id);
        XmlWidgetTemplate template = templates.get(normalized);
        if (template == null) throw new XmlWidgetLoadException("Unknown XML widget template '" + normalized + "'.");
        return template;
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }
}
