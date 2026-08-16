package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.xml.BuiltInWidgetXmlRegistry;
import dev.sixik.unigui.impl.xml.WidgetXmlRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Изменяемый XML-реестр descriptor-ов виджетов.
 *
 * <p>Через него регистрируются встроенные и модовые XML-типы, алиасы и
 * аннотированные классы виджетов, доступные загрузчику. Реестр используется
 * как runtime-фабрика для {@link XMLWidget}, но одновременно отдаёт metadata
 * для палитр, инспекторов и валидаторов редактора.</p>
 *
 * <p>Обычный путь — взять {@link #builtIns()}, добавить свои типы через
 * {@link #register(String, Supplier)} или {@link #registerAnnotated(Class)},
 * а затем передать реестр в {@link XMLWidget#create(String, XmlWidgetRegistry)}.</p>
 *
 * <pre>{@code
 * XmlWidgetRegistry registry = XmlWidgetRegistry.builtIns()
 *         .alias("PrimaryButton", "Button");
 * registry.registerAnnotated(StatusBadge.class);
 *
 * Widget root = XMLWidget.create(xml, registry);
 * }</pre>
 *
 * <p>Методы descriptor-доступа возвращают snapshots. Их можно безопасно отдавать
 * UI редактора без риска изменить внутреннее состояние registry.</p>
 */
public final class XmlWidgetRegistry {
    private final WidgetXmlRegistry delegate;

    private XmlWidgetRegistry(WidgetXmlRegistry delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    /**
     * Создаёт пустой реестр без встроенных виджетов.
     *
     * <p>Полезно для тестов, sandbox-редакторов и узких XML-DSL, где нужно явно
     * разрешить только небольшой набор widget types.</p>
     *
     * @return новый пустой XML-реестр
     */
    public static XmlWidgetRegistry empty() {
        return new XmlWidgetRegistry(new WidgetXmlRegistry());
    }

    /**
     * Создаёт новый реестр со стандартными UniGUI виджетами и глобальными contributions.
     *
     * <p>Каждый вызов возвращает независимый экземпляр. Глобальные расширения из
     * {@link XmlWidgetRegistryContributions} применяются поверх built-ins.</p>
     *
     * @return новый реестр со встроенными XML-типами
     */
    public static XmlWidgetRegistry builtIns() {
        XmlWidgetRegistry registry = new XmlWidgetRegistry(BuiltInWidgetXmlRegistry.create());
        XmlWidgetRegistryContributions.applyTo(registry);
        return registry;
    }

    /**
     * Регистрирует XML-тип вручную.
     *
     * <p>После регистрации через возвращённый {@link XmlWidgetType} добавляются
     * XML-атрибуты, правила детей и metadata для редактора.</p>
     *
     * @param xmlName имя элемента в XML, например {@code "Button"}
     * @param factory фабрика нового экземпляра виджета при загрузке XML
     * @param <T> runtime-тип виджета
     * @return builder зарегистрированного XML-типа
     */
    public <T extends Widget> XmlWidgetType<T> register(String xmlName, Supplier<T> factory) {
        return new XmlWidgetType<>(delegate.register(xmlName, factory));
    }

    /**
     * Регистрирует виджет по {@link XmlWidgetName} и {@link XmlAttribute} аннотациям.
     *
     * <p>Класс должен иметь no-arg constructor. Если constructor нестандартный,
     * используй {@link #registerAnnotated(Class, Supplier)}.</p>
     *
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @param <T> runtime-тип виджета
     * @return builder зарегистрированного XML-типа
     */
    public <T extends Widget> XmlWidgetType<T> registerAnnotated(Class<T> widgetType) {
        return XmlWidgetAnnotations.register(this, widgetType);
    }

    /**
     * Регистрирует аннотированный виджет с явной фабрикой экземпляров.
     *
     * <p>Это удобно для виджетов без публичного no-arg constructor или для тестов,
     * где нужно заранее задать часть runtime-состояния.</p>
     *
     * @param widgetType класс виджета с {@link XmlWidgetName}
     * @param factory фабрика экземпляров для XML-loader'а
     * @param <T> runtime-тип виджета
     * @return builder зарегистрированного XML-типа
     */
    public <T extends Widget> XmlWidgetType<T> registerAnnotated(Class<T> widgetType, Supplier<T> factory) {
        return XmlWidgetAnnotations.register(this, widgetType, factory);
    }

    /**
     * Добавляет альтернативное XML-имя для уже зарегистрированного типа.
     *
     * <p>Alias не создаёт отдельный descriptor: запрос descriptor-а по alias вернёт
     * descriptor целевого типа. Это сохраняет палитру редактора без дублей.</p>
     *
     * @param aliasName новое имя XML-элемента
     * @param targetName существующее XML-имя, на которое указывает alias
     * @return этот реестр для chained-настройки
     */
    public XmlWidgetRegistry alias(String aliasName, String targetName) {
        delegate.alias(aliasName, targetName);
        return this;
    }

    /**
     * Проверяет, известен ли XML-тип или alias.
     *
     * @param xmlName имя XML-элемента
     * @return {@code true}, если loader сможет найти тип по этому имени
     */
    public boolean contains(String xmlName) {
        return delegate.type(xmlName) != null;
    }

    /**
     * Возвращает builder зарегистрированного типа.
     *
     * <p>Метод также разрешает aliases. Изменения builder-а применяются к реальному
     * типу в реестре, поэтому его можно использовать для позднего расширения metadata.</p>
     *
     * @param xmlName XML-имя типа или alias
     * @return зарегистрированный тип, если он найден
     */
    public Optional<XmlWidgetType<? extends Widget>> type(String xmlName) {
        dev.sixik.unigui.impl.xml.WidgetXmlType<? extends Widget> type = delegate.type(xmlName);
        return type == null ? Optional.empty() : Optional.of(new XmlWidgetType<>(type));
    }

    /**
     * Возвращает read-only descriptor одного XML-типа.
     *
     * @param xmlName XML-имя типа или alias
     * @return descriptor для редактора/валидатора, если тип найден
     */
    public Optional<XmlWidgetDescriptor> descriptor(String xmlName) {
        dev.sixik.unigui.impl.xml.WidgetXmlType<? extends Widget> type = delegate.type(xmlName);
        return type == null ? Optional.empty() : Optional.of(type.descriptor());
    }

    /**
     * Возвращает descriptor-ы всех основных типов реестра.
     *
     * <p>Aliases в список не добавляются отдельно, чтобы палитра виджетов не
     * показывала один и тот же runtime-тип несколько раз.</p>
     *
     * @return immutable snapshot descriptor-ов
     */
    public List<XmlWidgetDescriptor> descriptors() {
        return delegate.types().stream()
                .map(dev.sixik.unigui.impl.xml.WidgetXmlType::descriptor)
                .toList();
    }

    /**
     * Возвращает aliases, зарегистрированные в реестре.
     *
     * <p>Ключ map — alias XML-имя, значение — основное XML-имя descriptor-а.
     * Snapshot immutable и подходит для генераторов документации, палитр редактора
     * и подсказок автодополнения.</p>
     *
     * @return immutable snapshot соответствий alias-to-target
     */
    public Map<String, String> aliases() {
        return delegate.aliases();
    }

    WidgetXmlRegistry delegate() {
        return delegate;
    }
}
