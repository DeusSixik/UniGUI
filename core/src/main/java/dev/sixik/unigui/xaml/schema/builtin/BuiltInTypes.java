package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.xaml.schema.Schema;

/**
 * Встроенные типы UniGUI.
 *
 * <p>Регистрация только запоминает имена и поставщиков схем, поэтому её стоимость не зависит от числа
 * свойств: типы строятся лениво при первом обращении через {@link Schema#type}.</p>
 */
public final class BuiltInTypes {
    private BuiltInTypes() {
    }

    /**
     * Регистрирует все встроенные типы и их алиасы.
     *
     * @param schema реестр
     */
    public static void register(Schema schema) {
        ContainerTypes.register(schema);
        DisplayTypes.register(schema);
        ControlTypes.register(schema);
        DataTypes.register(schema);
        NavigationTypes.register(schema);
        DockingTypes.register(schema);

        schema.alias("Border", "Box")
                .alias("Stack", "StackPanel")
                .alias("ScrollViewer", "ScrollView")
                .alias("TextView", "TextWidget")
                .alias("Texture", "TextureWidget")
                .alias("Image", "ImageView");
    }
}
