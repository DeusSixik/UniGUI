package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Свойства, общие для всех наследников {@link WidgetBase}: состояние, трансформация и layout.
 *
 * <p>Раньше setter, парсер и описание каждого свойства были записаны в трёх разных местах
 * ({@code BuiltInWidgetXmlSupport}, {@code XmlWidgetAnnotations} и таблица descriptor-ов), и их приходилось
 * править синхронно. Теперь это одна таблица, в которой у свойства есть и запись, и чтение: по ней же
 * сохраняется живой UI обратно в документ. Таблица строится один раз на JVM и разделяется всеми типами.</p>
 *
 * <p>Свойства-сокращения ({@code align}, {@code overflow}, {@code class}, {@code styleClass}) читать нельзя:
 * у них нет собственного состояния. Сокращения {@code scale} и пара {@code scaleX/scaleY} читаются так, чтобы
 * ровно одно из представлений описывало масштаб.</p>
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
        add(out, "class", Converters.STRING, (widget, value) -> {
                }, null, null, "Common", "",
                "Optional style class alias preserved for editor/theme integration.");
        add(out, "styleClass", Converters.STRING, (widget, value) -> {
                }, null, null, "Common", "",
                "Optional style class name preserved for editor/theme integration.");
        add(out, "enabled", Converters.BOOLEAN, WidgetBase::enabled, WidgetBase::enabled, Boolean.class, "Behavior", "true",
                "Whether the widget can receive user interaction.");
        add(out, "visible", Converters.BOOLEAN, WidgetBase::visible, WidgetBase::visible, Boolean.class, "Behavior", "true",
                "Whether the widget is visible without collapsing layout space.");
        add(out, "visibility", Converters.enumOf(Visibility.class), WidgetBase::visibility, WidgetBase::visibility, null,
                "Behavior", "visible", "Visibility mode: visible, hidden or collapsed.");
        add(out, "opacity", Converters.FLOAT, WidgetBase::opacity, WidgetBase::opacity, Float.class, "Appearance", "1",
                "Widget opacity clamped between 0 and 1.");

        // --- Трансформация
        add(out, "rotation", Converters.FLOAT, WidgetBase::rotationDegrees, w -> w.transform().rotationDegrees(),
                Float.class, "Appearance", "0", "Rotation in degrees applied to the widget transform.");
        add(out, "x", Converters.FLOAT,
                (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()),
                w -> w.transform().position().x(), Float.class, "Layout", "0",
                "Local transform offset on the X axis, in pixels.");
        add(out, "y", Converters.FLOAT,
                (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value),
                w -> w.transform().position().y(), Float.class, "Layout", "0",
                "Local transform offset on the Y axis, in pixels.");
        add(out, "scale", Converters.FLOAT, (widget, value) -> widget.transform().scale().set(value, value),
                w -> w.transform().scale().x() == w.transform().scale().y() ? w.transform().scale().x() : null,
                Float.class, "Appearance", "1", "Uniform local transform scale applied on both axes.");
        add(out, "scaleX", Converters.FLOAT,
                (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()),
                w -> w.transform().scale().x() != w.transform().scale().y() ? w.transform().scale().x() : null,
                Float.class, "Appearance", "1", "Local transform scale on the X axis.");
        add(out, "scaleY", Converters.FLOAT,
                (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value),
                w -> w.transform().scale().x() != w.transform().scale().y() ? w.transform().scale().y() : null,
                Float.class, "Appearance", "1", "Local transform scale on the Y axis.");

        // --- Размеры и отступы
        layout(out, "width", Converters.SIZE, (w, v) -> w.layout(s -> s.width(v)), LayoutStyle::width, "auto",
                "Preferred layout width; accepts px, percent or auto values.");
        layout(out, "height", Converters.SIZE, (w, v) -> w.layout(s -> s.height(v)), LayoutStyle::height, "auto",
                "Preferred layout height; accepts px, percent or auto values.");
        layout(out, "minWidth", Converters.SIZE, (w, v) -> w.layout(s -> s.minWidth(v)), LayoutStyle::minWidth, "0",
                "Minimum layout width constraint.");
        layout(out, "minHeight", Converters.SIZE, (w, v) -> w.layout(s -> s.minHeight(v)), LayoutStyle::minHeight, "0",
                "Minimum layout height constraint.");
        layout(out, "maxWidth", Converters.SIZE, (w, v) -> w.layout(s -> s.maxWidth(v)), LayoutStyle::maxWidth, "auto",
                "Maximum layout width constraint; auto means unlimited.");
        layout(out, "maxHeight", Converters.SIZE, (w, v) -> w.layout(s -> s.maxHeight(v)), LayoutStyle::maxHeight, "auto",
                "Maximum layout height constraint; auto means unlimited.");
        layout(out, "padding", Converters.INSETS, (w, v) -> w.layout(s -> s.padding(v)), LayoutStyle::padding, "0",
                "Inner content padding; accepts one, two, three or four inset values.");
        layout(out, "margin", Converters.INSETS, (w, v) -> w.layout(s -> s.margin(v)), LayoutStyle::margin, "0",
                "Outer layout margin; accepts one, two, three or four inset values.");
        layout(out, "marginAuto", Converters.AUTO_MARGINS, (w, v) -> w.layout(s -> s.marginAuto(v)),
                LayoutStyle::marginAuto, "none", "Automatic margins: sides, horizontal, vertical, all or none.");

        // --- Grid
        layout(out, "gridTemplateColumns", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridTemplateColumns(v)),
                LayoutStyle::gridTemplateColumns, "",
                "Grid column tracks: px, percent, fr, auto, minmax() or repeat().");
        layout(out, "gridTemplateRows", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridTemplateRows(v)),
                LayoutStyle::gridTemplateRows, "", "Grid row tracks: px, percent, fr, auto, minmax() or repeat().");
        layout(out, "gridAutoFlow", Converters.enumOf(GridAutoFlow.class), (w, v) -> w.layout(s -> s.gridAutoFlow(v)),
                LayoutStyle::gridAutoFlow, "row", "Grid auto-placement flow: row, column, row-dense or column-dense.");
        layout(out, "gridAutoColumns", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridAutoColumns(v)),
                LayoutStyle::gridAutoColumns, "", "Sizes for implicit grid columns.");
        layout(out, "gridAutoRows", Converters.GRID_TRACKS, (w, v) -> w.layout(s -> s.gridAutoRows(v)),
                LayoutStyle::gridAutoRows, "", "Sizes for implicit grid rows.");
        layout(out, "gridColumn", Converters.INT,
                (w, v) -> w.layout(s -> s.gridColumn(v, w.layoutStyle().gridColumnSpan())),
                LayoutStyle::gridColumnStart, "0", "Grid column start line (1-based, 0 means auto).");
        layout(out, "gridColumnSpan", Converters.INT,
                (w, v) -> w.layout(s -> s.gridColumn(w.layoutStyle().gridColumnStart(), v)),
                LayoutStyle::gridColumnSpan, "1", "Number of grid columns spanned.");
        layout(out, "gridRow", Converters.INT,
                (w, v) -> w.layout(s -> s.gridRow(v, w.layoutStyle().gridRowSpan())),
                LayoutStyle::gridRowStart, "0", "Grid row start line (1-based, 0 means auto).");
        layout(out, "gridRowSpan", Converters.INT,
                (w, v) -> w.layout(s -> s.gridRow(w.layoutStyle().gridRowStart(), v)),
                LayoutStyle::gridRowSpan, "1", "Number of grid rows spanned.");

        // --- Flex
        layout(out, "zIndex", Converters.INT, (w, v) -> w.layout(s -> s.zIndex(v)), LayoutStyle::zIndex, "0",
                "Paint and hit-test order among siblings.");
        layout(out, "flexGrow", Converters.FLOAT, (w, v) -> w.layout(s -> s.flexGrow(v)), LayoutStyle::flexGrow, "0",
                "Flex grow weight inside flex-capable parent layouts.");
        layout(out, "flexShrink", Converters.FLOAT, (w, v) -> w.layout(s -> s.flexShrink(v)), LayoutStyle::flexShrink, "0",
                "Flex shrink weight inside flex-capable parent layouts; 0 means no shrinking.");
        layout(out, "flexBasis", Converters.SIZE, (w, v) -> w.layout(s -> s.flexBasis(v)), LayoutStyle::flexBasis, "auto",
                "Flex base size; accepts px, percent, content, min/max-content or auto values.");
        layout(out, "order", Converters.INT, (w, v) -> w.layout(s -> s.order(v)), LayoutStyle::order, "0",
                "Layout order among siblings; lower values go first.");
        layout(out, "aspectRatio", Converters.FLOAT, (w, v) -> w.layout(s -> s.aspectRatio(v)), LayoutStyle::aspectRatio, "",
                "Preferred width-to-height ratio, for example 1.78 for 16:9.");
        layout(out, "flexDirection", Converters.enumOf(FlexDirection.class), (w, v) -> w.layout(s -> s.flexDirection(v)),
                LayoutStyle::flexDirection, "column", "Primary child layout direction for flex-capable widgets.");
        layout(out, "flexWrap", Converters.enumOf(FlexWrap.class), (w, v) -> w.layout(s -> s.flexWrap(v)),
                LayoutStyle::flexWrap, "nowrap", "Whether flex children wrap onto additional rows or columns.");
        layout(out, "rowGap", Converters.FLOAT, (w, v) -> w.layout(s -> s.rowGap(v)), LayoutStyle::rowGap, "0",
                "Spacing between layout rows.");
        layout(out, "columnGap", Converters.FLOAT, (w, v) -> w.layout(s -> s.columnGap(v)), LayoutStyle::columnGap, "0",
                "Spacing between layout columns.");

        // --- Выравнивание
        layout(out, "align", Converters.enumOf(Alignment.class), (w, v) -> w.layout(s -> s.align(v, v)), null, "stretch",
                "Legacy shorthand that applies the same alignment to both axes.");
        layout(out, "horizontalAlignment", Converters.enumOf(Alignment.class),
                (w, v) -> w.layout(s -> s.horizontalAlignment(v)), LayoutStyle::horizontalAlignment, "stretch",
                "Horizontal anchor of the widget inside its slot (start, center, end, stretch); with a margin it acts as an anchor with offsets.");
        layout(out, "verticalAlignment", Converters.enumOf(Alignment.class),
                (w, v) -> w.layout(s -> s.verticalAlignment(v)), LayoutStyle::verticalAlignment, "stretch",
                "Vertical anchor of the widget inside its slot (start, center, end, stretch).");
        layout(out, "alignItems", Converters.enumOf(Align.class), (w, v) -> w.layout(s -> s.alignItems(v)),
                LayoutStyle::alignItems, "stretch", "Cross-axis alignment applied to child widgets.");
        layout(out, "alignSelf", Converters.enumOf(Align.class), (w, v) -> w.layout(s -> s.alignSelf(v)),
                LayoutStyle::alignSelf, "auto", "Per-widget alignment override inside the parent layout.");
        layout(out, "alignContent", Converters.enumOf(AlignContent.class), (w, v) -> w.layout(s -> s.alignContent(v)),
                LayoutStyle::alignContent, "stretch", "Distribution of wrapping flex lines on the cross axis.");
        layout(out, "justifyContent", Converters.enumOf(Justify.class), (w, v) -> w.layout(s -> s.justifyContent(v)),
                LayoutStyle::justifyContent, "start", "Main-axis distribution for children in flex-capable layouts.");

        // --- Overflow и позиционирование
        layout(out, "overflow", Converters.enumOf(Overflow.class), (w, v) -> w.layout(s -> s.overflow(v)), null, "visible",
                "Overflow mode applied to both axes.");
        layout(out, "overflowX", Converters.enumOf(Overflow.class), (w, v) -> w.layout(s -> s.overflowX(v)),
                LayoutStyle::overflowX, "visible", "Horizontal overflow mode.");
        layout(out, "overflowY", Converters.enumOf(Overflow.class), (w, v) -> w.layout(s -> s.overflowY(v)),
                LayoutStyle::overflowY, "visible", "Vertical overflow mode.");
        layout(out, "position", Converters.enumOf(PositionType.class), (w, v) -> w.layout(s -> s.position(v)),
                LayoutStyle::position, "relative", "Layout positioning mode: static, relative, absolute or fixed.");
        layout(out, "left", Converters.SIZE, (w, v) -> w.layout(s -> s.left(v)), LayoutStyle::left, "auto",
                "Absolute-position left inset; used when position is absolute.");
        layout(out, "top", Converters.SIZE, (w, v) -> w.layout(s -> s.top(v)), LayoutStyle::top, "auto",
                "Absolute-position top inset; used when position is absolute.");
        layout(out, "right", Converters.SIZE, (w, v) -> w.layout(s -> s.right(v)), LayoutStyle::right, "auto",
                "Absolute-position right inset; used when position is absolute.");
        layout(out, "bottom", Converters.SIZE, (w, v) -> w.layout(s -> s.bottom(v)), LayoutStyle::bottom, "auto",
                "Absolute-position bottom inset; used when position is absolute.");

        return Collections.unmodifiableList(out);
    }

    /**
     * Свойство layout: читается из {@link LayoutStyle}; {@code getter == null} — сокращение без состояния.
     */
    private static <V> void layout(List<PropertyInfo<WidgetBase, ?>> out, String name, ValueConverter<V> converter,
                                   BiConsumer<WidgetBase, V> setter, Function<LayoutStyle, ?> getter,
                                   String defaultValue, String description) {
        Function<WidgetBase, ?> read = getter == null ? null : widget -> getter.apply(widget.layoutStyle());
        add(out, name, converter, setter, read, null, "Layout", defaultValue, description);
    }

    /**
     * Добавляет свойство.
     *
     * @param binding тип источника для {@code {Binding}}; {@code null} — привязка не поддерживается
     */
    private static <V> void add(List<PropertyInfo<WidgetBase, ?>> out, String name, ValueConverter<V> converter,
                                BiConsumer<WidgetBase, V> setter, Function<WidgetBase, ?> getter, Class<?> binding,
                                String category, String defaultValue, String description) {
        out.add(new PropertyInfo<>(name, converter, (target, value, context) -> setter.accept(target, value),
                getter, binding, Meta.category(category).defaultValue(defaultValue).description(description)));
    }
}
