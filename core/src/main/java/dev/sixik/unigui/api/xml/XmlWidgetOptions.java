package dev.sixik.unigui.api.xml;

/**
 * Настройки XML-загрузки виджетов.
 *
 * <p>Объект immutable: методы {@code strictAttributes(...)} и другие modifiers
 * возвращают новый экземпляр. Дефолтный профиль рассчитан на runtime-экраны:
 * неизвестные атрибуты считаются ошибкой, глубина дерева ограничена, texture и
 * command resolver-ы используют безопасные no-op/simple реализации.</p>
 *
 * <p>Для редактора или hot-reload preview часто удобен lenient-режим, чтобы один
 * неизвестный атрибут не ломал весь документ.</p>
 *
 * <pre>{@code
 * Widget root = XMLWidget.create(xml, XmlWidgetOptions.lenient()
 *         .textureResolver(myTextures)
 *         .commands(myCommands));
 * }</pre>
 *
 * <pre>{@code
 * XmlBindingContext context = XmlBindingContext.empty();
 * context.mutable("user.name", String.class, "Alex");
 * XmlWidgetScreen<?> screen = XMLWidget.createScreen(
 *         "<Label text=\"{Binding user.name}\" />",
 *         XmlWidgetOptions.DEFAULT.bindingContext(context));
 * }</pre>
 *
 * @param strictAttributes если {@code true}, неизвестные XML-атрибуты вызывают ошибку загрузки
 * @param maxDepth         максимальная глубина XML-дерева; значения меньше 1 нормализуются в 1
 * @param textureResolver  resolver texture id в runtime {@code TextureHandle}
 * @param commands         реестр command handlers для event-атрибутов
 * @param bindingContext   контекст observable-значений для {@code {Binding path}}
 */
public record XmlWidgetOptions(
        boolean strictAttributes,
        int maxDepth,
        XmlTextureResolver textureResolver,
        XmlCommandRegistry commands,
        XmlBindingContext bindingContext) {
    /**
     * Дефолтный resolver, который создаёт simple texture handles без backend native handle.
     */
    public static final XmlTextureResolver DEFAULT_TEXTURE_RESOLVER = XmlTextureResolver.simple();
    /**
     * Дефолтный immutable command registry без разрешённых XML-команд.
     */
    public static final XmlCommandRegistry DEFAULT_COMMANDS = XmlCommandRegistry.none();
    /**
     * Строгие runtime-настройки загрузки XML по умолчанию.
     */
    public static final XmlWidgetOptions DEFAULT = new XmlWidgetOptions(true, 128, DEFAULT_TEXTURE_RESOLVER,
            DEFAULT_COMMANDS, XmlBindingContext.empty());

    /**
     * Создаёт настройки с дефолтными texture resolver и command registry.
     *
     * @param strictAttributes строгая проверка неизвестных атрибутов
     * @param maxDepth         максимальная глубина XML-дерева
     */
    public XmlWidgetOptions(boolean strictAttributes, int maxDepth) {
        this(strictAttributes, maxDepth, DEFAULT_TEXTURE_RESOLVER, DEFAULT_COMMANDS, XmlBindingContext.empty());
    }

    /**
     * Создаёт настройки с явным texture resolver и дефолтным пустым command registry.
     *
     * @param strictAttributes строгая проверка неизвестных атрибутов
     * @param maxDepth         максимальная глубина XML-дерева
     * @param textureResolver  resolver текстур; {@code null} заменяется дефолтным
     */
    public XmlWidgetOptions(boolean strictAttributes, int maxDepth, XmlTextureResolver textureResolver) {
        this(strictAttributes, maxDepth, textureResolver, DEFAULT_COMMANDS, XmlBindingContext.empty());
    }

    /**
     * Создаёт настройки с явными texture и command resolver-ами без binding-контекста.
     *
     * @param strictAttributes строгая проверка неизвестных атрибутов
     * @param maxDepth         максимальная глубина XML-дерева
     * @param textureResolver  resolver текстур
     * @param commands         реестр команд
     */
    public XmlWidgetOptions(boolean strictAttributes, int maxDepth,
                            XmlTextureResolver textureResolver, XmlCommandRegistry commands) {
        this(strictAttributes, maxDepth, textureResolver, commands, XmlBindingContext.empty());
    }

    /**
     * Нормализует nullable зависимости и минимальную глубину дерева.
     */
    public XmlWidgetOptions {
        maxDepth = Math.max(1, maxDepth);
        textureResolver = textureResolver == null ? DEFAULT_TEXTURE_RESOLVER : textureResolver;
        commands = commands == null ? DEFAULT_COMMANDS : commands;
        bindingContext = bindingContext == null ? XmlBindingContext.empty() : bindingContext;
    }

    /**
     * Возвращает строгий runtime-профиль загрузки.
     *
     * @return дефолтные строгие настройки
     */
    public static XmlWidgetOptions strict() {
        return DEFAULT;
    }

    /**
     * Возвращает lenient-профиль для редактора и preview.
     *
     * @return настройки, где неизвестные атрибуты игнорируются loader'ом
     */
    public static XmlWidgetOptions lenient() {
        return DEFAULT.strictAttributes(false);
    }

    /**
     * Меняет режим обработки неизвестных XML-атрибутов.
     *
     * @param strictAttributes {@code true} — ошибка, {@code false} — пропустить неизвестный атрибут
     * @return новая копия настроек
     */
    public XmlWidgetOptions strictAttributes(boolean strictAttributes) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext);
    }

    /**
     * Меняет максимально допустимую глубину XML-дерева.
     *
     * @param maxDepth новая глубина; значения меньше 1 будут нормализованы
     * @return новая копия настроек
     */
    public XmlWidgetOptions maxDepth(int maxDepth) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext);
    }

    /**
     * Подставляет resolver текстур для XML-атрибутов типа {@code texture}.
     *
     * @param textureResolver resolver id/size/options в runtime texture handle
     * @return новая копия настроек
     */
    public XmlWidgetOptions textureResolver(XmlTextureResolver textureResolver) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext);
    }

    /**
     * Подставляет реестр команд для event-атрибутов вроде {@code onClick}.
     *
     * @param commands именованные command handlers
     * @return новая копия настроек
     */
    public XmlWidgetOptions commands(XmlCommandRegistry commands) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext);
    }

    /**
     * Подставляет контекст observable-значений для XML binding-выражений.
     *
     * @param bindingContext контекст значений; {@code null} заменяется пустым
     * @return новая копия настроек
     */
    public XmlWidgetOptions bindingContext(XmlBindingContext bindingContext) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands, bindingContext);
    }
}
