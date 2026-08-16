package dev.sixik.unigui.api.xml;

/**
 * Добавляет XML descriptor-ы модов/редактора в свежие копии встроенного реестра.
 *
 * <p>Contributor вызывается из {@link XmlWidgetRegistry#builtIns()} после регистрации стандартных
 * UniGUI widget-ов. Так моды могут один раз зарегистрировать расширение процесса, а каждый новый
 * registry будет получать эти widget types автоматически.</p>
 */
@FunctionalInterface
public interface XmlWidgetRegistryContributor {
    /**
     * Добавляет custom XML-типы, aliases или metadata в реестр.
     *
     * @param registry реестр, который нужно дополнить
     */
    void contribute(XmlWidgetRegistry registry);
}
