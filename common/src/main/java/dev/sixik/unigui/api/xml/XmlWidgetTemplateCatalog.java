package dev.sixik.unigui.api.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Реестр переиспользуемых редакторских шаблонов для item/control XML исходного документа.
 *
 * <p>Используется редактором как палитра готовых элементов и настроек,
 * из которых создаются новые узлы исходного XML-документа. Порядок регистрации сохраняется,
 * чтобы palette UI мог показывать templates предсказуемо.</p>
 */
public final class XmlWidgetTemplateCatalog {
    private final Map<String, XmlWidgetTemplate> templates = new LinkedHashMap<>();

    /**
     * Создаёт пустой catalog.
     *
     * @return новый template catalog
     */
    public static XmlWidgetTemplateCatalog empty() {
        return new XmlWidgetTemplateCatalog();
    }

    /**
     * Регистрирует template.
     *
     * @param template template descriptor; не может быть {@code null}
     * @return этот catalog для chained-регистрации
     */
    public XmlWidgetTemplateCatalog register(XmlWidgetTemplate template) {
        if (template == null) throw new IllegalArgumentException("XML widget template must not be null");
        templates.put(template.id(), template);
        return this;
    }

    /**
     * Регистрирует control template из XML-строки.
     *
     * @param id id template-а
     * @param xml XML source template-а
     * @return этот catalog для chained-регистрации
     */
    public XmlWidgetTemplateCatalog control(String id, String xml) {
        return register(XmlWidgetTemplate.control(id, xml));
    }

    /**
     * Регистрирует item template из XML-строки.
     *
     * @param id id template-а
     * @param xml XML source template-а
     * @return этот catalog для chained-регистрации
     */
    public XmlWidgetTemplateCatalog item(String id, String xml) {
        return register(XmlWidgetTemplate.item(id, xml));
    }

    /**
     * Удаляет template по id.
     *
     * @param id id template-а
     * @return этот catalog для chained-настройки
     */
    public XmlWidgetTemplateCatalog remove(String id) {
        templates.remove(normalizeId(id));
        return this;
    }

    /**
     * Проверяет наличие template-а.
     *
     * @param id id template-а
     * @return {@code true}, если template зарегистрирован
     */
    public boolean contains(String id) {
        return templates.containsKey(normalizeId(id));
    }

    /**
     * Возвращает template по id.
     *
     * @param id id template-а
     * @return template или empty
     */
    public Optional<XmlWidgetTemplate> template(String id) {
        return Optional.ofNullable(templates.get(normalizeId(id)));
    }

    /**
     * Возвращает ids templates в порядке регистрации.
     *
     * @return immutable список ids
     */
    public List<String> ids() {
        return List.copyOf(templates.keySet());
    }

    /**
     * Возвращает все templates в порядке регистрации.
     *
     * @return immutable список templates
     */
    public List<XmlWidgetTemplate> templates() {
        return List.copyOf(templates.values());
    }

    /**
     * Возвращает templates указанного kind.
     *
     * @param kind назначение template-а; {@code null} означает control
     * @return список templates
     */
    public List<XmlWidgetTemplate> templates(XmlWidgetTemplateKind kind) {
        XmlWidgetTemplateKind normalized = kind == null ? XmlWidgetTemplateKind.CONTROL : kind;
        return templates.values().stream().filter(template -> template.kind() == normalized).toList();
    }

    /**
     * Создаёт экземпляр template-а по id.
     *
     * @param id id template-а
     * @param values attribute overrides
     * @return result instantiate
     */
    public XmlWidgetDocumentResult instantiate(String id, XmlWidgetTemplateValues values) {
        XmlWidgetTemplate template = requireTemplate(id);
        return template.instantiate(values);
    }

    /**
     * Создаёт экземпляр template-а по id и валидирует его.
     *
     * @param id id template-а
     * @param values attribute overrides
     * @param registry реестр XML descriptor-ов
     * @return result instantiate + validation
     */
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
