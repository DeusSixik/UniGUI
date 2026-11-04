package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Свойства, общие для всех наследников {@link WidgetBase}: состояние, трансформация и layout.
 *
 * <p>Раньше setter, парсер и описание каждого свойства были записаны в трёх разных местах
 * ({@code BuiltInWidgetXmlSupport}, {@code XmlWidgetAnnotations} и таблица descriptor-ов), и их приходилось
 * править синхронно. Теперь это одна таблица, которая строится один раз на JVM и разделяется всеми типами.</p>
 */
final class CommonProperties {
    private static final List<PropertyInfo<WidgetBase, ?>> ALL = build();

    private CommonProperties() {
    }

    /**
     * Возвращает общие свойства.
     *
     * @return неизменяемый список
     */
    static List<PropertyInfo<WidgetBase, ?>> all() {
        return ALL;
    }

    private static List<PropertyInfo<WidgetBase, ?>> build() {
        List<PropertyInfo<WidgetBase, ?>> out = new ArrayList<>(64);

        // --- Состояние и стиль
        text(out, "class", "Common", "", "Optional style class alias preserved for editor/theme integration.",
                (widget, value) -> { });
        text(out, "styleClass", "Common", "", "Optional style class name preserved for editor/theme integration.",
                (widget, value) -> { });
        bindable(out, "enabled", Converters.BOOLEAN, WidgetBase::enabled, "Behavior", "true",
                "Whether the widget can receive user interaction.");
        bindable(out, "visible", Converters.BOOLEAN, WidgetBase::visible, "Behavior", "true",
                "Whether the widget is visible without collapsing layout space.");
        add(out, "visibility", Converters.enumOf(Visibility.class), WidgetBase::visibility, "Behavior", "visible",
                "Visibility mode: visible, hidden or collapsed.");
        bindable(out, "opacity", Converters.FLOAT, WidgetBase::opacity, "Appearance", "1",
                "Widget opacity clamped between 0 and 1.");

        // --- Трансформация
        bindable(out, "rotation", Converters.FLOAT, WidgetBase::rotationDegrees, "Appearance", "0",
                "Rotation in degrees applied to the widget transform.");
        bindable(out, "x", Converters.FLOAT,
                (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()),
                "Layout", "0", "Local transform offset on the X axis, in pixels.");
        bindable(out, "y", Converters.FLOAT,
                (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value),
                "Layout", "0", "Local transform offset on the Y axis, in pixels.");
        bindable(out, "scale", Converters.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, value),
                "Appearance", "1", "Uniform local transform scale applied on both axes.");
        bindable(out, "scaleX", Converters.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()),
                "Appearance", "1", "Local transform scale on the X axis.");
        bindable(out, "scaleY", Converters.FLOAT,
                (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value),
                "Appearance", "1", "Local transform scale on the Y axis.");

        // --- Размеры и отступы
        layout(out, "width", Converters.SIZE, (w, v) -> w.layout(s -> s.width(v)), "auto",
                "Preferred layout width; accepts px, percent or auto values.");
        layout(out, "height", Converters.SIZE, (w, v) -> w.layout(s -> s.height(v)), "auto",
                "Preferred layout height; accepts px, percent or auto values.");
        layout(out, "minWidth", Converters.SIZE, (w, v) -> w.layout(s -> s.minWidth(v)), "0",
                "Minimum layout width constraint.");
        layout(out, "minHeight", Converters.SIZE, (w, v) -> w.layout(s -> s.minHeight(v)), "0",
                "Minimum layout height constraint.");
        layout(out, "maxWidth", Converters.SIZE, (w, v) -> w.layout(s -> s.maxWidth(v)), "auto",
                "Maximum layout width constraint; auto means unlimited.");
        layout(out, "maxHeight", Converters.SIZE, (w, v) -> w.layout(s -> s.maxHeight(v)), "auto",
                "Maximum layout height constraint; auto means unlimited.");
        layout(out, "padding", Converters.INSETS, (w, v) -> w.layout(s -> s.padding(v)), "0",
                "Inner content padding; accepts one, two, three or four inset values.");
        layout(out, "margin", Converters.INSETS, (w, v) -> w.layout(s -> s.margin(v)), "0",
                "Outer layout margin; accepts one, two, three or four inset values.");
        layout(out, "marginAuto", Converters.AUTO_MARGINS, (w, v) -> w.layout(s -> s.marginAuto(v)), "none",
                "Automatic margins: sides, horizontal, vertical, all or none.");

        // --- Grid
        layout(out, "gridTemplateColumns", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridTemplateColumns(v)), "",
                "Grid column tracks: px, percent, fr, auto, minmax() or repeat().");
        layout(out, "gridTemplateRows", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridTemplateRows(v)), "",
                "Grid row tracks: px, percent, fr, auto, minmax() or repeat().");
        layout(out, "gridAutoFlow", Converters.enumOf(GridAutoFlow.class), (w, v) -> w.layout(s -> s.gridAutoFlow(v)), "row",
                "Grid auto-placement flow: row, column, row-dense or column-dense.");
        layout(out, "gridAutoColumns", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridAutoColumns(v)), "",
                "Sizes for implicit grid columns.");
        layout(out, "gridAutoRows", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridAutoRows(v)), "",
                "Sizes for implicit grid rows.");
        layout(out, "gridColumn", Converters.INT,
                (w, v) -> w.layout(s -> s.gridColumn(v, w.layoutStyle().gridColumnSpan())), "0",
                "Grid column start line (1-based, 0 means auto).");
        layout(out, "gridColumnSpan", Converters.INT,
                (w, v) -> w.layout(s -> s.gridColumn(w.layoutStyle().gridColumnStart(), v)), "1",
                "Number of grid columns spanned.");
        layout(out, "gridRow", Converters.INT,
                (w, v) -> w.layout(s -> s.gridRow(v, w.layoutStyle().gridRowSpan())), "0",
                "Grid row start line (1-based, 0 means auto).");
        layout(out, "gridRowSpan", Converters.INT,
                (w, v) -> w.layout(s -> s.gridRow(w.layoutStyle().gridRowStart(), v)), "1",
                "Number of grid rows spanned.");

        // --- Flex
        layout(out, "zIndex", Converters.INT, (w, v) -> w.layout(s -> s.zIndex(v)), "0",
                "Paint and hit-test order among siblings.");
        layout(out, "flexGrow", Converters.FLOAT, (w, v) -> w.layout(s -> s.flexGrow(v)), "0",
                "Flex grow weight inside flex-capable parent layouts.");
        layout(out, "flexShrink", Converters.FLOAT, (w, v) -> w.layout(s -> s.flexShrink(v)), "0",
                "Flex shrink weight inside flex-capable parent layouts; 0 means no shrinking.");
        layout(out, "flexBasis", Converters.SIZE, (w, v) -> w.layout(s -> s.flexBasis(v)), "auto",
                "Flex base size; accepts px, percent, content, min/max-content or auto values.");
        layout(out, "order", Converters.INT, (w, v) -> w.layout(s -> s.order(v)), "0",
                "Layout order among siblings; lower values go first.");
        layout(out, "aspectRatio", Converters.FLOAT, (w, v) -> w.layout(s -> s.aspectRatio(v)), "",
                "Preferred width-to-height ratio, for example 1.78 for 16:9.");
        layout(out, "flexDirection", Converters.enumOf(FlexDirection.class), (w, v) -> w.layout(s -> s.flexDirection(v)), "column",
                "Primary child layout direction for flex-capable widgets.");
        layout(out, "flexWrap", Converters.enumOf(FlexWrap.class), (w, v) -> w.layout(s -> s.flexWrap(v)), "nowrap",
                "Whether flex children wrap onto additional rows or columns.");
        layout(out, "rowGap", Converters.FLOAT, (w, v) -> w.layout(s -> s.rowGap(v)), "0",
                "Spacing between layout rows.");
        layout(out, "columnGap", Converters.FLOAT, (w, v) -> w.layout(s -> s.columnGap(v)), "0",
                "Spacing between layout columns.");

        // --- Выравнивание
        layout(out, "align", Converters.enumOf(Alignment.class), (w, v) -> w.layout(s -> s.align(v, v)), "stretch",
                "Legacy shorthand that applies the same alignment to both axes.");
        layout(out, "alignItems", Converters.enumOf(Align.class), (w, v) -> w.layout(s -> s.alignItems(v)), "stretch",
                "Cross-axis alignment applied to child widgets.");
        layout(out, "alignSelf", Converters.enumOf(Align.class), (w, v) -> w.layout(s -> s.alignSelf(v)), "auto",
                "Per-widget alignment override inside the parent layout.");
        layout(out, "alignContent", Converters.enumOf(AlignContent.class), (w, v) -> w.layout(s -> s.alignContent(v)), "stretch",
                "Distribution of wrapping flex lines on the cross axis.");
        layout(out, "justifyContent", Converters.enumOf(Justify.class), (w, v) -> w.layout(s -> s.justifyContent(v)), "start",
                "Main-axis distribution for children in flex-capable layouts.");

        // --- Overflow и позиционирование
        layout(out, "overflow", Converters.enumOf(Overflow.class), (w, v) -> w.layout(s -> s.overflow(v)), "visible",
                "Overflow mode applied to both axes.");
        layout(out, "overflowX", Converters.enumOf(Overflow.class), (w, v) -> w.layout(s -> s.overflowX(v)), "visible",
                "Horizontal overflow mode.");
        layout(out, "overflowY", Converters.enumOf(Overflow.class), (w, v) -> w.layout(s -> s.overflowY(v)), "visible",
                "Vertical overflow mode.");
        layout(out, "position", Converters.enumOf(PositionType.class), (w, v) -> w.layout(s -> s.position(v)), "relative",
                "Layout positioning mode: static, relative, absolute or fixed.");
        layout(out, "left", Converters.SIZE, (w, v) -> w.layout(s -> s.left(v)), "auto",
                "Absolute-position left inset; used when position is absolute.");
        layout(out, "top", Converters.SIZE, (w, v) -> w.layout(s -> s.top(v)), "auto",
                "Absolute-position top inset; used when position is absolute.");
        layout(out, "right", Converters.SIZE, (w, v) -> w.layout(s -> s.right(v)), "auto",
                "Absolute-position right inset; used when position is absolute.");
        layout(out, "bottom", Converters.SIZE, (w, v) -> w.layout(s -> s.bottom(v)), "auto",
                "Absolute-position bottom inset; used when position is absolute.");

        return Collections.unmodifiableList(out);
    }

    private static void text(List<PropertyInfo<WidgetBase, ?>> out, String name, String category, String defaultValue,
                             String description, BiConsumer<WidgetBase, String> setter) {
        add(out, name, Converters.STRING, setter, category, defaultValue, description);
    }

    private static <V> void layout(List<PropertyInfo<WidgetBase, ?>> out, String name, ValueConverter<V> converter,
                                   BiConsumer<WidgetBase, V> setter, String defaultValue, String description) {
        add(out, name, converter, setter, "Layout", defaultValue, description);
    }

    private static <V> void bindable(List<PropertyInfo<WidgetBase, ?>> out, String name, ValueConverter<V> converter,
                                     BiConsumer<WidgetBase, V> setter, String category, String defaultValue,
                                     String description) {
        out.add(new PropertyInfo<>(name, converter, (target, value, context) -> setter.accept(target, value),
                null, converter.type(),
                Meta.category(category).defaultValue(defaultValue).description(description)));
    }

    private static <V> void add(List<PropertyInfo<WidgetBase, ?>> out, String name, ValueConverter<V> converter,
                                BiConsumer<WidgetBase, V> setter, String category, String defaultValue,
                                String description) {
        out.add(new PropertyInfo<>(name, converter, (target, value, context) -> setter.accept(target, value),
                null, null, Meta.category(category).defaultValue(defaultValue).description(description)));
    }
}
