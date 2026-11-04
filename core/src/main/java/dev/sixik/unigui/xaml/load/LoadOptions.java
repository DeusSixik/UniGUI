package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.xml.XmlBindingContext;
import dev.sixik.unigui.api.xml.XmlCommandRegistry;
import dev.sixik.unigui.api.xml.XmlTextureResolver;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;

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
        XmlTextureResolver textureResolver,
        XmlCommandRegistry commands,
        XmlBindingContext bindingContext,
        MarkupExtensionRegistry extensions) {
    /** Строгая загрузка, глубина 128, всё остальное по умолчанию. */
    public static final LoadOptions DEFAULT = new LoadOptions(true, 128, null, null, null, null);

    public LoadOptions {
        if (maxDepth < 0) throw new IllegalArgumentException("maxDepth must not be negative");
        textureResolver = textureResolver == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : textureResolver;
        commands = commands == null ? XmlWidgetOptions.DEFAULT_COMMANDS : commands;
        extensions = extensions == null ? MarkupExtensionRegistry.DEFAULT : extensions;
    }

    /** @return строгие настройки по умолчанию */
    public static LoadOptions strict() {
        return DEFAULT;
    }

    /** @return настройки, при которых неизвестные атрибуты пропускаются */
    public static LoadOptions lenient() {
        return DEFAULT.strictAttributes(false);
    }

    public LoadOptions strictAttributes(boolean value) {
        return new LoadOptions(value, maxDepth, textureResolver, commands, bindingContext, extensions);
    }

    public LoadOptions maxDepth(int value) {
        return new LoadOptions(strictAttributes, value, textureResolver, commands, bindingContext, extensions);
    }

    public LoadOptions textureResolver(XmlTextureResolver value) {
        return new LoadOptions(strictAttributes, maxDepth, value, commands, bindingContext, extensions);
    }

    public LoadOptions commands(XmlCommandRegistry value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, value, bindingContext, extensions);
    }

    public LoadOptions bindingContext(XmlBindingContext value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, commands, value, extensions);
    }

    public LoadOptions extensions(MarkupExtensionRegistry value) {
        return new LoadOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext, value);
    }
}
