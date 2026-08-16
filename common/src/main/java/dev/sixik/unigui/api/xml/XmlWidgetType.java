package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Set;

/**
 * Builder XML-описания одного зарегистрированного типа виджета.
 *
 * <p>Экземпляр создаётся через {@link XmlWidgetRegistry#register(String, java.util.function.Supplier)}
 * или возвращается реестром для уже существующего типа. Через этот builder задаются
 * атрибуты, metadata для инспектора и правила вложенных XML-элементов.</p>
 *
 * <p>Каждый XML-атрибут состоит из двух частей: parser преобразует строку из XML
 * в типизированное значение, setter применяет это значение к виджету. Такой контракт
 * позволяет держать XML-слой независимым от конкретного backend-а рендера.</p>
 *
 * <pre>{@code
 * registry.register("Badge", Badge::new)
 *         .describe("Status Badge", "Custom", "Small status indicator.")
 *         .attribute("text", XmlValueParsers.STRING, Badge::text)
 *         .attribute("tone", XmlValueParsers.enumValue(Tone.class), Badge::tone);
 * }</pre>
 *
 * @param <T> runtime-тип виджета, который создаётся XML-loader'ом
 */
public final class XmlWidgetType<T extends Widget> {
    private final dev.sixik.unigui.impl.xml.WidgetXmlType<T> delegate;

    XmlWidgetType(dev.sixik.unigui.impl.xml.WidgetXmlType<T> delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    /**
     * Возвращает XML-имя зарегистрированного типа.
     *
     * @return имя элемента, например {@code "Button"}
     */
    public String xmlName() {
        return delegate.xmlName();
    }

    /**
     * Регистрирует XML-атрибут с автоматически созданным descriptor-ом.
     *
     * <p>Descriptor получит имя, display name и category по эвристикам
     * {@link XmlAttributeDescriptor}. Для user-facing редактора лучше использовать
     * перегрузку с явным descriptor-ом.</p>
     *
     * @param name имя XML-атрибута
     * @param parser преобразователь строкового значения
     * @param setter действие, применяющее значение к виджету
     * @param <V> тип распарсенного значения
     * @return этот builder для chained-настройки
     */
    public <V> XmlWidgetType<T> attribute(String name,
                                          XmlValueParser<V> parser,
                                          XmlPropertySetter<? super T, ? super V> setter) {
        return attribute(name, parser, setter, XmlAttributeDescriptor.of(name));
    }

    /**
     * Регистрирует XML-атрибут с явной metadata для редактора.
     *
     * <p>Если XML содержит этот атрибут, loader сначала вызовет {@code parser},
     * затем {@code setter}. Исключения parser/setter превращаются в
     * {@link XmlWidgetLoadException} с координатами XML-атрибута, если они доступны.</p>
     *
     * @param name имя XML-атрибута
     * @param parser преобразователь строкового значения
     * @param setter действие, применяющее значение к виджету
     * @param descriptor display/category/default/description для inspector UI
     * @param <V> тип распарсенного значения
     * @return этот builder для chained-настройки
     */
    public <V> XmlWidgetType<T> attribute(String name,
                                          XmlValueParser<V> parser,
                                          XmlPropertySetter<? super T, ? super V> setter,
                                          XmlAttributeDescriptor descriptor) {
        if (parser == null || setter == null) {
            throw new IllegalArgumentException("XML attribute parser and setter must not be null");
        }
        delegate.attribute(name, parser::parse, setter::set, descriptor);
        return this;
    }

    /**
     * Задаёт metadata самого XML-типа для палитры редактора.
     *
     * @param displayName человекочитаемое имя типа
     * @param category группа в палитре, например {@code "Controls"}
     * @param description короткое описание назначения виджета
     * @return этот builder для chained-настройки
     */
    public XmlWidgetType<T> describe(String displayName, String category, String description) {
        delegate.describe(displayName, category, description);
        return this;
    }

    /**
     * Обновляет descriptor уже зарегистрированного атрибута.
     *
     * <p>Метод полезен, когда атрибут добавлен автоматически аннотациями, но
     * конкретный registry хочет уточнить описание или category.</p>
     *
     * @param name имя существующего XML-атрибута
     * @param descriptor новый descriptor
     * @return этот builder для chained-настройки
     */
    public XmlWidgetType<T> describeAttribute(String name, XmlAttributeDescriptor descriptor) {
        delegate.describeAttribute(name, descriptor);
        return this;
    }

    /**
     * Задаёт правило добавления обычных дочерних XML-элементов.
     *
     * <p>Если policy не задана, XML-loader запретит вложенные элементы. Для
     * контейнеров policy обычно вызывает {@code addChild}, а для специальных типов
     * может маршрутизировать child в отдельный слот.</p>
     *
     * @param childPolicy правило добавления дочернего виджета
     * @return этот builder для chained-настройки
     */
    public XmlWidgetType<T> childPolicy(XmlChildPolicy<T> childPolicy) {
        if (childPolicy == null) throw new IllegalArgumentException("XML child policy must not be null");
        delegate.childPolicy(childPolicy::addChild);
        return this;
    }

    /**
     * Регистрирует property-child слот с descriptor-ом по умолчанию.
     *
     * <p>Property-child соответствует XML-элементу вида {@code <ScrollView.Content>}
     * и позволяет отличать обычных детей от именованных слотов.</p>
     *
     * @param name имя слота без имени родительского типа
     * @param childPolicy правило применения child-виджета к слоту
     * @return этот builder для chained-настройки
     */
    public XmlWidgetType<T> propertyChild(String name, XmlChildPolicy<T> childPolicy) {
        return propertyChild(name, childPolicy, XmlPropertyChildDescriptor.of(name));
    }

    /**
     * Регистрирует property-child слот с явной metadata.
     *
     * @param name имя слота, например {@code "Content"}
     * @param childPolicy правило применения child-виджета к слоту
     * @param descriptor display/category/description для inspector UI
     * @return этот builder для chained-настройки
     */
    public XmlWidgetType<T> propertyChild(String name, XmlChildPolicy<T> childPolicy, XmlPropertyChildDescriptor descriptor) {
        if (childPolicy == null) throw new IllegalArgumentException("XML property child policy must not be null");
        delegate.propertyChild(name, childPolicy::addChild, descriptor);
        return this;
    }

    /**
     * Обновляет descriptor уже зарегистрированного property-child слота.
     *
     * @param name имя существующего слота
     * @param descriptor новый descriptor слота
     * @return этот builder для chained-настройки
     */
    public XmlWidgetType<T> describePropertyChild(String name, XmlPropertyChildDescriptor descriptor) {
        delegate.describePropertyChild(name, descriptor);
        return this;
    }

    /**
     * Проверяет наличие XML-атрибута.
     *
     * @param name имя атрибута
     * @return {@code true}, если атрибут зарегистрирован
     */
    public boolean hasAttribute(String name) {
        return delegate.hasAttribute(name);
    }

    /**
     * Возвращает имена зарегистрированных атрибутов.
     *
     * @return immutable snapshot имён атрибутов
     */
    public Set<String> attributeNames() {
        return Set.copyOf(delegate.attributeNames());
    }

    /**
     * Возвращает descriptor-ы зарегистрированных атрибутов.
     *
     * @return immutable snapshot metadata атрибутов
     */
    public List<XmlAttributeDescriptor> attributeDescriptors() {
        return delegate.attributeDescriptors();
    }

    /**
     * Проверяет наличие property-child слота.
     *
     * @param name имя слота без префикса типа
     * @return {@code true}, если слот зарегистрирован
     */
    public boolean hasPropertyChild(String name) {
        return delegate.hasPropertyChild(name);
    }

    /**
     * Возвращает имена property-child слотов.
     *
     * @return immutable snapshot имён слотов
     */
    public Set<String> propertyChildNames() {
        return Set.copyOf(delegate.propertyChildNames());
    }

    /**
     * Возвращает descriptor-ы property-child слотов.
     *
     * @return immutable snapshot metadata слотов
     */
    public List<XmlPropertyChildDescriptor> propertyChildDescriptors() {
        return delegate.propertyChildDescriptors();
    }

    /**
     * Собирает полный read-only descriptor этого XML-типа.
     *
     * @return descriptor для палитры, инспектора и document validator'а
     */
    public XmlWidgetDescriptor descriptor() {
        return delegate.descriptor();
    }
}
