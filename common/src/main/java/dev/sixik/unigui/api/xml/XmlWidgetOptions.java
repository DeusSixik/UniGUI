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
 * @param strictAttributes если {@code true}, неизвестные XML-атрибуты вызывают ошибку загрузки
 * @param maxDepth максимальная глубина XML-дерева; значения меньше 1 нормализуются в 1
 * @param textureResolver resolver texture id в runtime {@code TextureHandle}
 * @param commands реестр command handlers для event-атрибутов
 */
public record XmlWidgetOptions(
        boolean strictAttributes,
        int maxDepth,
        XmlTextureResolver textureResolver,
        XmlCommandRegistry commands) {
    /** Дефолтный resolver, который создаёт simple texture handles без backend native handle. */
    public static final XmlTextureResolver DEFAULT_TEXTURE_RESOLVER = XmlTextureResolver.simple();
    /** Дефолтный immutable command registry без разрешённых XML-команд. */
    public static final XmlCommandRegistry DEFAULT_COMMANDS = XmlCommandRegistry.none();
    /** Строгие runtime-настройки загрузки XML по умолчанию. */
    public static final XmlWidgetOptions DEFAULT = new XmlWidgetOptions(true, 128, DEFAULT_TEXTURE_RESOLVER, DEFAULT_COMMANDS);

    /**
     * Создаёт настройки с дефолтными texture resolver и command registry.
     *
     * @param strictAttributes строгая проверка неизвестных атрибутов
     * @param maxDepth максимальная глубина XML-дерева
     */
    public XmlWidgetOptions(boolean strictAttributes, int maxDepth) {
        this(strictAttributes, maxDepth, DEFAULT_TEXTURE_RESOLVER, DEFAULT_COMMANDS);
    }

    /**
     * Создаёт настройки с явным texture resolver и дефолтным пустым command registry.
     *
     * @param strictAttributes строгая проверка неизвестных атрибутов
     * @param maxDepth максимальная глубина XML-дерева
     * @param textureResolver resolver текстур; {@code null} заменяется дефолтным
     */
    public XmlWidgetOptions(boolean strictAttributes, int maxDepth, XmlTextureResolver textureResolver) {
        this(strictAttributes, maxDepth, textureResolver, DEFAULT_COMMANDS);
    }

    /** Нормализует nullable зависимости и минимальную глубину дерева. */
    public XmlWidgetOptions {
        maxDepth = Math.max(1, maxDepth);
        textureResolver = textureResolver == null ? DEFAULT_TEXTURE_RESOLVER : textureResolver;
        commands = commands == null ? DEFAULT_COMMANDS : commands;
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
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }

    /**
     * Меняет максимально допустимую глубину XML-дерева.
     *
     * @param maxDepth новая глубина; значения меньше 1 будут нормализованы
     * @return новая копия настроек
     */
    public XmlWidgetOptions maxDepth(int maxDepth) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }

    /**
     * Подставляет resolver текстур для XML-атрибутов типа {@code texture}.
     *
     * @param textureResolver resolver id/size/options в runtime texture handle
     * @return новая копия настроек
     */
    public XmlWidgetOptions textureResolver(XmlTextureResolver textureResolver) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }

    /**
     * Подставляет реестр команд для event-атрибутов вроде {@code onClick}.
     *
     * @param commands именованные command handlers
     * @return новая копия настроек
     */
    public XmlWidgetOptions commands(XmlCommandRegistry commands) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }
}
