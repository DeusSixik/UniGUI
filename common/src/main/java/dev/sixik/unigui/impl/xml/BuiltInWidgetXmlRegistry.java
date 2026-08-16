package dev.sixik.unigui.impl.xml;

/**
 * Встроенный XML-реестр стандартных виджетов UniGUI.
 */
public final class BuiltInWidgetXmlRegistry {
    private BuiltInWidgetXmlRegistry() {
    }

    public static WidgetXmlRegistry create() {
        WidgetXmlRegistry registry = new WidgetXmlRegistry();

        BuiltInContainerXml.register(registry);
        BuiltInDisplayXml.register(registry);
        BuiltInControlXml.register(registry);
        registerAliases(registry);

        return registry;
    }

    private static void registerAliases(WidgetXmlRegistry registry) {
        registry.alias("Border", "Box")
                .alias("Stack", "StackPanel")
                .alias("ScrollViewer", "ScrollView")
                .alias("TextView", "TextWidget")
                .alias("Texture", "TextureWidget")
                .alias("Image", "ImageView");
    }
}
