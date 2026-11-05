package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.load.XamlTextureResolver;

/**
 * Окружение загрузки: всё, что конвертерам и setter-ам нужно знать о внешнем мире.
 *
 * <p>В прежней реализации текстуры и команды передавались через {@code ThreadLocal}, из-за чего поведение
 * зависело от того, в каком потоке и внутри какого вызова загрузчика выполняется setter. Здесь контекст
 * явный параметр, поэтому схему можно использовать из любого потока.</p>
 *
 * <p>Классы {@link XamlTextureResolver} и {@link XamlCommandRegistry} пока взяты из прежнего API и будут
 * перенесены в пакет {@code xaml} на этапе удаления legacy.</p>
 */
public final class LoadContext {
    /**
     * Контекст по умолчанию: простой резолвер текстур и пустой набор команд.
     */
    public static final LoadContext DEFAULT =
            new LoadContext(XamlTextureResolver.DEFAULT, XamlCommandRegistry.none());

    private final XamlTextureResolver textures;
    private final XamlCommandRegistry commands;

    /**
     * Создаёт контекст.
     *
     * @param textures резолвер идентификаторов текстур; {@code null} заменяется простым
     * @param commands реестр именованных команд; {@code null} заменяется пустым
     */
    public LoadContext(XamlTextureResolver textures, XamlCommandRegistry commands) {
        this.textures = textures == null ? XamlTextureResolver.DEFAULT : textures;
        this.commands = commands == null ? XamlCommandRegistry.none() : commands;
    }

    /**
     * Возвращает резолвер текстур.
     *
     * @return резолвер
     */
    public XamlTextureResolver textures() {
        return textures;
    }

    /**
     * Возвращает реестр команд.
     *
     * @return реестр
     */
    public XamlCommandRegistry commands() {
        return commands;
    }
}
