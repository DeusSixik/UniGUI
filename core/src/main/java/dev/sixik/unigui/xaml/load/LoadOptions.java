package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;

/**
 * Настройки загрузки документа.
 *
 * <p>Отличие от прежних опций: {@code bindingContext} по умолчанию {@code null}, и каждая загрузка получает
 * свой пустой контекст. Раньше значение по умолчанию было одним общим изменяемым объектом на весь процесс.</p>
 *
 * @param strictAttributes {@code true} — неизвестный атрибут это ошибка, {@code false} — он пропускается
 * @param maxDepth         максимальная глубина вложенности виджетов (корень имеет глубину 0)
 * @param textureResolver  превращает идентификатор текстуры в runtime-handle; {@code null} — простой резолвер
 * @param commands         именованные команды для {@code onClick} и {@code {Command}}; {@code null} — пустой набор
 * @param bindingContext   источники данных для {@code {Binding}}; {@code null} — пустой контекст на загрузку
 * @param extensions       доступные markup extension; {@code null} — {@link MarkupExtensionRegistry#DEFAULT}
 */
public record LoadOptions(
        boolean strictAttributes,
        int maxDepth,
        XamlTextureResolver textureResolver,
        XamlCommandRegistry commands,
        XamlBindingContext bindingContext,
        MarkupExtensionRegistry extensions,
        NodeObserver nodeObserver) {
    /**
     * Строгая загрузка, глубина 128, всё остальное по умолчанию.
     */
    public static final LoadOptions DEFAULT = new LoadOptions(true, 128, null, null, null, null, null);

    public LoadOptions {
        if (maxDepth < 0) throw new IllegalArgumentException("maxDepth must not be negative");
        textureResolver = textureResolver == null ? XamlTextureResolver.DEFAULT : textureResolver;
        commands = commands == null ? XamlCommandRegistry.none() : commands;
        extensions = extensions == null ? MarkupExtensionRegistry.DEFAULT : extensions;
    }

    /**
     * @return строгие настройки по умолчанию
     */
    public static LoadOptions strict() {
        return DEFAULT;
    }

    /**
     * @return настройки, при которых неизвестные атрибуты пропускаются
     */
    public static LoadOptions lenient() {
        return DEFAULT.strictAttributes(false);
    }

    public LoadOptions strictAttributes(boolean value) {
        return new LoadOptions(value, maxDepth, textureResolver, commands, bindingContext, extensions, nodeObserver);
    }

    public LoadOptions maxDepth(int value) {
        return new LoadOptions(strictAttributes, value, textureResolver, commands, bindingContext, extensions, nodeObserver);
    }

    public LoadOptions textureResolver(XamlTextureResolver value) {
        return new LoadOptions(strictAttributes, maxDepth, value, commands, bindingContext, extensions, nodeObserver);
    }

    public LoadOptions commands(XamlCommandRegistry value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, value, bindingContext, extensions, nodeObserver);
    }

    public LoadOptions bindingContext(XamlBindingContext value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, commands, value, extensions, nodeObserver);
    }

    public LoadOptions extensions(MarkupExtensionRegistry value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext, value, nodeObserver);
    }

    /**
     * Задаёт наблюдателя, которому загрузчик сообщает, какой виджет создан из какого узла. Нужен редакторам:
     * по нему выбранный в иерархии узел находится в живом дереве. Узлы шаблонов сообщаются для каждой строки.
     *
     * @param value наблюдатель или {@code null}
     * @return новые настройки
     */
    public LoadOptions nodeObserver(NodeObserver value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext, extensions, value);
    }

    /**
     * Получает пары «узел разметки — созданный виджет» в порядке создания.
     */
    @FunctionalInterface
    public interface NodeObserver {
        /**
         * @param node   узел документа
         * @param widget виджет, созданный из узла (его свойства ещё не применены)
         */
        void created(UiNode node, Widget widget);
    }
}
