package dev.sixik.unigui.api.xml;

/** Настройки XML-загрузки виджетов; строгие значения по умолчанию рассчитаны на runtime-экраны. */
public record XmlWidgetOptions(
        boolean strictAttributes,
        int maxDepth,
        XmlTextureResolver textureResolver,
        XmlCommandRegistry commands) {
    public static final XmlTextureResolver DEFAULT_TEXTURE_RESOLVER = XmlTextureResolver.simple();
    public static final XmlCommandRegistry DEFAULT_COMMANDS = XmlCommandRegistry.none();
    public static final XmlWidgetOptions DEFAULT = new XmlWidgetOptions(true, 128, DEFAULT_TEXTURE_RESOLVER, DEFAULT_COMMANDS);

    public XmlWidgetOptions(boolean strictAttributes, int maxDepth) {
        this(strictAttributes, maxDepth, DEFAULT_TEXTURE_RESOLVER, DEFAULT_COMMANDS);
    }

    public XmlWidgetOptions(boolean strictAttributes, int maxDepth, XmlTextureResolver textureResolver) {
        this(strictAttributes, maxDepth, textureResolver, DEFAULT_COMMANDS);
    }

    public XmlWidgetOptions {
        maxDepth = Math.max(1, maxDepth);
        textureResolver = textureResolver == null ? DEFAULT_TEXTURE_RESOLVER : textureResolver;
        commands = commands == null ? DEFAULT_COMMANDS : commands;
    }

    public static XmlWidgetOptions strict() {
        return DEFAULT;
    }

    public static XmlWidgetOptions lenient() {
        return DEFAULT.strictAttributes(false);
    }

    public XmlWidgetOptions strictAttributes(boolean strictAttributes) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }

    public XmlWidgetOptions maxDepth(int maxDepth) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }

    public XmlWidgetOptions textureResolver(XmlTextureResolver textureResolver) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }

    public XmlWidgetOptions commands(XmlCommandRegistry commands) {
        return new XmlWidgetOptions(strictAttributes, maxDepth, textureResolver, commands);
    }
}
