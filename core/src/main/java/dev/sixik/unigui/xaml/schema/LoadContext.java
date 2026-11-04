package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.xml.XmlCommandRegistry;
import dev.sixik.unigui.api.xml.XmlTextureResolver;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;

/**
 * Окружение загрузки: всё, что конвертерам и setter-ам нужно знать о внешнем мире.
 *
 * <p>В прежней реализации текстуры и команды передавались через {@code ThreadLocal}, из-за чего поведение
 * зависело от того, в каком потоке и внутри какого вызова загрузчика выполняется setter. Здесь контекст
 * явный параметр, поэтому схему можно использовать из любого потока.</p>
 *
 * <p>Классы {@link XmlTextureResolver} и {@link XmlCommandRegistry} пока взяты из прежнего API и будут
 * перенесены в пакет {@code xaml} на этапе удаления legacy.</p>
 */
public final class LoadContext {
    /** Контекст по умолчанию: простой резолвер текстур и пустой набор команд. */
    public static final LoadContext DEFAULT =
            new LoadContext(XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER, XmlWidgetOptions.DEFAULT_COMMANDS);

    private final XmlTextureResolver textures;
    private final XmlCommandRegistry commands;

    /**
     * Создаёт контекст.
     *
     * @param textures резолвер идентификаторов текстур; {@code null} заменяется простым
     * @param commands реестр именованных команд; {@code null} заменяется пустым
     */
    public LoadContext(XmlTextureResolver textures, XmlCommandRegistry commands) {
        this.textures = textures == null ? XmlWidgetOptions.DEFAULT_TEXTURE_RESOLVER : textures;
        this.commands = commands == null ? XmlWidgetOptions.DEFAULT_COMMANDS : commands;
    }

    /**
     * Возвращает резолвер текстур.
     *
     * @return резолвер
     */
    public XmlTextureResolver textures() {
        return textures;
    }

    /**
     * Возвращает реестр команд.
     *
     * @return реестр
     */
    public XmlCommandRegistry commands() {
        return commands;
    }
}
