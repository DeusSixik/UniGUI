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

        // Attached-свойства в привычной записи: <Label Grid.Row="1" FlexBox.Grow="1"/>.
        schema.attach("Grid", "Row", "gridRow")
                .attach("Grid", "Column", "gridColumn")
                .attach("Grid", "RowSpan", "gridRowSpan")
                .attach("Grid", "ColumnSpan", "gridColumnSpan")
                .attach("GridBox", "Row", "gridRow")
                .attach("GridBox", "Column", "gridColumn")
                .attach("GridBox", "RowSpan", "gridRowSpan")
                .attach("GridBox", "ColumnSpan", "gridColumnSpan")
                .attach("Flex", "Grow", "flexGrow")
                .attach("Flex", "Shrink", "flexShrink")
                .attach("Flex", "Basis", "flexBasis")
                .attach("FlexBox", "Grow", "flexGrow")
                .attach("FlexBox", "Shrink", "flexShrink")
                .attach("FlexBox", "Basis", "flexBasis")
                .attach("Panel", "ZIndex", "zIndex")
                .attach("Canvas", "Left", "left")
                .attach("Canvas", "Top", "top");

        schema.alias("Border", "Box")
                .alias("Stack", "StackPanel")
                .alias("ScrollViewer", "ScrollView")
                .alias("TextView", "TextWidget")
                .alias("Texture", "TextureWidget")
                .alias("Image", "ImageView");
    }
}
