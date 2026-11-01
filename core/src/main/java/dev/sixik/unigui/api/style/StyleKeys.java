package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureGroup;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.text.TextTransform;

/**
 * Стандартные типизированные ключи style-системы.
 *
 * <p>Ключи используются Java builders, StylePack XML и editor inspector'ом.
 * Строковые id лежат отдельно в {@link StyleIds.Key}, а этот класс добавляет Java-тип значения.</p>
 */
public final class StyleKeys {
    /**
     * Опциональный renderer override для типа виджета, которому принадлежит стиль.
     *
     * <p>Значение — {@link dev.sixik.unigui.api.widget.render.WidgetRender} или его
     * строковый id из registry. Renderer, назначенный прямо на instance виджета,
     * остаётся приоритетнее этого ключа.</p>
     */
    public static final StyleKey<Object> RENDERER = StyleKey.of(StyleIds.Key.RENDERER, Object.class);

    /**
     * Источник фона поверхности: цвет, текстура или шейдер.
     */
    public static final StyleKey<BackgroundKind> BACKGROUND_KIND = StyleKey.of(StyleIds.Key.BACKGROUND_KIND, BackgroundKind.class);

    /**
     * Цвет заливки фона базового прямоугольника виджета.
     */
    public static final StyleKey<ColorView> BACKGROUND_COLOR = StyleKey.of(StyleIds.Key.BACKGROUND_COLOR, ColorView.class);

    /**
     * Текстура фона виджета.
     */
    public static final StyleKey<TextureHandle> BACKGROUND_TEXTURE = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE, TextureHandle.class);

    /**
     * Упорядоченная группа текстурных слоёв фоновой поверхности.
     */
    public static final StyleKey<TextureGroup> BACKGROUND_TEXTURE_GROUP =
            StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_GROUP, TextureGroup.class);

    /**
     * Tint-цвет фоновой текстуры.
     */
    public static final StyleKey<ColorView> BACKGROUND_TEXTURE_TINT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_TINT, ColorView.class);

    /**
     * Режим подгонки фоновой текстуры в bounds виджета.
     */
    public static final StyleKey<ImageFit> BACKGROUND_TEXTURE_FIT = StyleKey.of(StyleIds.Key.BACKGROUND_TEXTURE_FIT, ImageFit.class);

    /**
     * Рамки фоновой текстуры для режима nine-slice.
     */
    public static final StyleKey<SliceBorders> SLICE_BORDERS = StyleKey.of(StyleIds.Key.SLICE_BORDERS, SliceBorders.class);

    /**
     * Признак отрисовки фоновой поверхности.
     */
    public static final StyleKey<Boolean> BACKGROUND_VISIBLE = StyleKey.of(StyleIds.Key.BACKGROUND_VISIBLE, Boolean.class);

    /**
     * Шейдер фона виджета.
     */
    public static final StyleKey<ShaderHandle> BACKGROUND_SHADER = StyleKey.of(StyleIds.Key.BACKGROUND_SHADER, ShaderHandle.class);

    /**
     * Цвет рамки виджета.
     */
    public static final StyleKey<ColorView> BORDER_COLOR = StyleKey.of(StyleIds.Key.BORDER_COLOR, ColorView.class);

    /**
     * Признак отрисовки рамки поверхности.
     */
    public static final StyleKey<Boolean> BORDER_VISIBLE = StyleKey.of(StyleIds.Key.BORDER_VISIBLE, Boolean.class);

    /**
     * Основной цвет текста.
     */
    public static final StyleKey<ColorView> TEXT_COLOR = StyleKey.of(StyleIds.Key.TEXT_COLOR, ColorView.class);

    /**
     * Размер текста в UI-пикселях для текстовых контролов.
     */
    public static final StyleKey<Float> TEXT_SIZE = StyleKey.of(StyleIds.Key.TEXT_SIZE, Float.class);

    /**
     * Дополнительный межсимвольный интервал в UI-пикселях.
     */
    public static final StyleKey<Float> TEXT_TRACKING = StyleKey.of(StyleIds.Key.TEXT_TRACKING, Float.class);

    /**
     * Преобразование текста перед измерением и отрисовкой.
     */
    public static final StyleKey<TextTransform> TEXT_TRANSFORM = StyleKey.of(StyleIds.Key.TEXT_TRANSFORM, TextTransform.class);

    /**
     * Цвет заливки индикатора Checkbox для каждого состояния.
     */
    public static final StyleKey<ColorView> CHECKBOX_BACKGROUND_COLOR =
            StyleKey.of(StyleIds.Key.CHECKBOX_BACKGROUND_COLOR, ColorView.class);

    /**
     * Цвет метки (точки, галочки, крестика) включённого Checkbox. Без значения метка берёт акцентный цвет.
     */
    public static final StyleKey<ColorView> CHECKBOX_MARK_COLOR =
            StyleKey.of(StyleIds.Key.CHECKBOX_MARK_COLOR, ColorView.class);

    /**
     * Форма метки включённого Checkbox: точка, квадрат, галочка или крестик.
     */
    public static final StyleKey<CheckboxMarkShape> CHECKBOX_MARK_SHAPE =
            StyleKey.of(StyleIds.Key.CHECKBOX_MARK_SHAPE, CheckboxMarkShape.class);

    /**
     * Цвет курсора выбора и маркера hue для ColorPicker.
     */
    public static final StyleKey<ColorView> COLOR_PICKER_SELECTION_COLOR =
            StyleKey.of(StyleIds.Key.COLOR_PICKER_SELECTION_COLOR, ColorView.class);

    /**
     * Цвет рамки цветовой плоскости ColorPicker.
     */
    public static final StyleKey<ColorView> COLOR_PICKER_PLANE_BORDER_COLOR =
            StyleKey.of(StyleIds.Key.COLOR_PICKER_PLANE_BORDER_COLOR, ColorView.class);

    /**
     * Цвет рамок preview/current/original swatches ColorPicker.
     */
    public static final StyleKey<ColorView> COLOR_PICKER_SWATCH_BORDER_COLOR =
            StyleKey.of(StyleIds.Key.COLOR_PICKER_SWATCH_BORDER_COLOR, ColorView.class);

    /**
     * Цвет placeholder-текста в input-like виджетах.
     */
    public static final StyleKey<ColorView> PLACEHOLDER_COLOR = StyleKey.of(StyleIds.Key.PLACEHOLDER_COLOR, ColorView.class);

    /**
     * Цвет каретки однострочных и многострочных текстовых редакторов.
     */
    public static final StyleKey<ColorView> CARET_COLOR = StyleKey.of(StyleIds.Key.CARET_COLOR, ColorView.class);

    /**
     * Цвет внешнего кольца фокуса текстового поля.
     */
    public static final StyleKey<ColorView> FOCUS_RING_COLOR = StyleKey.of(StyleIds.Key.FOCUS_RING_COLOR, ColorView.class);

    /**
     * Акцентный цвет для selected/focused/active частей виджета.
     */
    public static final StyleKey<ColorView> ACCENT_COLOR = StyleKey.of(StyleIds.Key.ACCENT_COLOR, ColorView.class);

    /**
     * Цвет track-части slider/progress/scrollbar виджетов.
     */
    public static final StyleKey<ColorView> TRACK_COLOR = StyleKey.of(StyleIds.Key.TRACK_COLOR, ColorView.class);

    /**
     * Цвет thumb/handle-части slider/scrollbar виджетов.
     */
    public static final StyleKey<ColorView> THUMB_COLOR = StyleKey.of(StyleIds.Key.THUMB_COLOR, ColorView.class);

    /**
     * Радиус скругления бегунка в UI-пикселях; половина размера образует круг.
     */
    public static final StyleKey<Float> THUMB_RADIUS = StyleKey.of(StyleIds.Key.THUMB_RADIUS, Float.class);

    /**
     * Толщина дорожки ползунка в логических пикселях.
     */
    public static final StyleKey<Float> TRACK_THICKNESS = StyleKey.of(StyleIds.Key.TRACK_THICKNESS, Float.class);

    /**
     * Ширина бегунка Slider либо толщина тамба ScrollBar в логических пикселях.
     */
    public static final StyleKey<Float> THUMB_THICKNESS = StyleKey.of(StyleIds.Key.THUMB_THICKNESS, Float.class);

    /**
     * Цвет заливки прогресса HoldButton.
     */
    public static final StyleKey<ColorView> HOLD_COLOR = StyleKey.of(StyleIds.Key.HOLD_COLOR, ColorView.class);

    /**
     * Внешний размер индикатора RadioButton в логических пикселях.
     */
    public static final StyleKey<Float> RADIO_OUTER_SIZE = StyleKey.of(StyleIds.Key.RADIO_OUTER_SIZE, Float.class);

    /**
     * Размер внутренней отметки RadioButton в логических пикселях.
     */
    public static final StyleKey<Float> RADIO_INNER_SIZE = StyleKey.of(StyleIds.Key.RADIO_INNER_SIZE, Float.class);

    /**
     * Зазор между индикатором и подписью RadioButton в логических пикселях.
     */
    public static final StyleKey<Float> RADIO_TEXT_GAP = StyleKey.of(StyleIds.Key.RADIO_TEXT_GAP, Float.class);

    /**
     * Толщина рамки в UI-пикселях.
     */
    public static final StyleKey<Float> BORDER_WIDTH = StyleKey.of(StyleIds.Key.BORDER_WIDTH, Float.class);

    /**
     * Радиус скругления в UI-пикселях.
     */
    public static final StyleKey<Float> RADIUS = StyleKey.of(StyleIds.Key.RADIUS, Float.class);

    /**
     * Признак отрисовки Shape как обводки вместо заливки.
     */
    public static final StyleKey<Boolean> SHAPE_STROKE = StyleKey.of(StyleIds.Key.SHAPE_STROKE, Boolean.class);

    /**
     * Цвет заливки фигуры Shape.
     */
    public static final StyleKey<ColorView> SHAPE_COLOR = StyleKey.of(StyleIds.Key.SHAPE_COLOR, ColorView.class);

    /**
     * Тип геометрического примитива Shape: rect, rounded-rect, circle или line.
     */
    public static final StyleKey<String> SHAPE_TYPE = StyleKey.of(StyleIds.Key.SHAPE_TYPE, String.class);

    /**
     * Цвет заливки или обводки векторного Path.
     */
    public static final StyleKey<ColorView> PATH_COLOR = StyleKey.of(StyleIds.Key.PATH_COLOR, ColorView.class);

    /**
     * Признак отрисовки Path как обводки вместо заливки.
     */
    public static final StyleKey<Boolean> PATH_STROKE = StyleKey.of(StyleIds.Key.PATH_STROKE, Boolean.class);

    /**
     * Толщина обводки векторного Path.
     */
    public static final StyleKey<Float> PATH_STROKE_WIDTH = StyleKey.of(StyleIds.Key.PATH_STROKE_WIDTH, Float.class);

    /**
     * Режим линейного списка либо сетки для VirtualListView.
     */
    public static final StyleKey<VirtualListViewMode> VIRTUAL_LIST_VIEW_MODE =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_VIEW_MODE, VirtualListViewMode.class);
    /**
     * Высота одного элемента в режиме списка.
     */
    public static final StyleKey<Float> VIRTUAL_LIST_ITEM_HEIGHT =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_ITEM_HEIGHT, Float.class);
    /**
     * Предпочтительная ширина grid-ячейки для автоматического расчёта колонок.
     */
    public static final StyleKey<Float> VIRTUAL_LIST_GRID_CELL_WIDTH =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_GRID_CELL_WIDTH, Float.class);
    /**
     * Фиксированная высота grid-ячейки.
     */
    public static final StyleKey<Float> VIRTUAL_LIST_GRID_CELL_HEIGHT =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_GRID_CELL_HEIGHT, Float.class);
    /**
     * Фиксированное число grid-колонок; ноль включает адаптивный auto-fit.
     */
    public static final StyleKey<Integer> VIRTUAL_LIST_GRID_COLUMNS =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_GRID_COLUMNS, Integer.class);
    /**
     * Вертикальный промежуток между строками.
     */
    public static final StyleKey<Float> VIRTUAL_LIST_ROW_GAP =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_ROW_GAP, Float.class);
    /**
     * Горизонтальный промежуток между grid-ячейками.
     */
    public static final StyleKey<Float> VIRTUAL_LIST_COLUMN_GAP =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_COLUMN_GAP, Float.class);
    /**
     * Внутренние отступы виртуального viewport.
     */
    public static final StyleKey<EdgeInsets> VIRTUAL_LIST_PADDING =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_PADDING, EdgeInsets.class);
    /**
     * Число дополнительных виртуальных строк за границами viewport.
     */
    public static final StyleKey<Integer> VIRTUAL_LIST_OVERSCAN =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_OVERSCAN, Integer.class);
    /**
     * Максимальное число сохранённых невидимых item-виджетов.
     */
    public static final StyleKey<Integer> VIRTUAL_LIST_OFFSCREEN_CACHE_SIZE =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_OFFSCREEN_CACHE_SIZE, Integer.class);
    /**
     * Шаг вертикальной прокрутки в UI-пикселях.
     */
    public static final StyleKey<Float> VIRTUAL_LIST_SCROLL_STEP =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_SCROLL_STEP, Float.class);
    /**
     * Нужно ли поглощать wheel-событие на границе прокрутки.
     */
    public static final StyleKey<Boolean> VIRTUAL_LIST_CONSUME_WHEEL_AT_BOUNDS =
            StyleKey.of(StyleIds.Key.VIRTUAL_LIST_CONSUME_WHEEL_AT_BOUNDS, Boolean.class);

    /**
     * Высота строки TreeView.
     */
    public static final StyleKey<Float> TREE_ROW_HEIGHT = StyleKey.of(StyleIds.Key.TREE_ROW_HEIGHT, Float.class);
    /**
     * Форма фона строки TreeView.
     */
    public static final StyleKey<TreeRowStyle> TREE_ROW_STYLE = StyleKey.of(StyleIds.Key.TREE_ROW_STYLE, TreeRowStyle.class);
    /**
     * Форма маркера раскрытия TreeView.
     */
    public static final StyleKey<TreeDisclosureStyle> TREE_DISCLOSURE_STYLE =
            StyleKey.of(StyleIds.Key.TREE_DISCLOSURE_STYLE, TreeDisclosureStyle.class);
    /**
     * Радиус фона строки TreeView.
     */
    public static final StyleKey<Float> TREE_ROW_RADIUS = StyleKey.of(StyleIds.Key.TREE_ROW_RADIUS, Float.class);
    /**
     * Горизонтальный отступ фона строки TreeView.
     */
    public static final StyleKey<Float> TREE_ROW_INSET_X = StyleKey.of(StyleIds.Key.TREE_ROW_INSET_X, Float.class);
    /**
     * Вертикальный отступ фона строки TreeView.
     */
    public static final StyleKey<Float> TREE_ROW_INSET_Y = StyleKey.of(StyleIds.Key.TREE_ROW_INSET_Y, Float.class);
    /**
     * Шаг отступа дочерней строки TreeView.
     */
    public static final StyleKey<Float> TREE_INDENT_WIDTH = StyleKey.of(StyleIds.Key.TREE_INDENT_WIDTH, Float.class);
    /**
     * Заполнять ли встроенные иконки папок.
     */
    public static final StyleKey<Boolean> TREE_FOLDER_FILL_ENABLED =
            StyleKey.of(StyleIds.Key.TREE_FOLDER_FILL_ENABLED, Boolean.class);
    /**
     * Цвет подсветки наведённой строки.
     */
    public static final StyleKey<ColorView> TREE_HOVER_COLOR = StyleKey.of(StyleIds.Key.TREE_HOVER_COLOR, ColorView.class);
    /**
     * Цвет выбранной строки.
     */
    public static final StyleKey<ColorView> TREE_SELECTED_COLOR = StyleKey.of(StyleIds.Key.TREE_SELECTED_COLOR, ColorView.class);
    /**
     * Цвет обычного текста строки.
     */
    public static final StyleKey<ColorView> TREE_TEXT_COLOR = StyleKey.of(StyleIds.Key.TREE_TEXT_COLOR, ColorView.class);
    /**
     * Цвет текста выбранной строки.
     */
    public static final StyleKey<ColorView> TREE_SELECTED_TEXT_COLOR =
            StyleKey.of(StyleIds.Key.TREE_SELECTED_TEXT_COLOR, ColorView.class);
    /**
     * Цвет текста недоступной строки.
     */
    public static final StyleKey<ColorView> TREE_DISABLED_TEXT_COLOR =
            StyleKey.of(StyleIds.Key.TREE_DISABLED_TEXT_COLOR, ColorView.class);
    /**
     * Цвет маркера раскрытия.
     */
    public static final StyleKey<ColorView> TREE_DISCLOSURE_COLOR =
            StyleKey.of(StyleIds.Key.TREE_DISCLOSURE_COLOR, ColorView.class);
    /**
     * Цвет встроенной иконки папки или файла.
     */
    public static final StyleKey<ColorView> TREE_ICON_COLOR = StyleKey.of(StyleIds.Key.TREE_ICON_COLOR, ColorView.class);

    /**
     * Цвет фона шапки VirtualTableView.
     */
    public static final StyleKey<ColorView> TABLE_HEADER_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_HEADER_COLOR, ColorView.class);
    /**
     * Цвет фона заголовка колонки под курсором либо перетаскиваемой колонки.
     */
    public static final StyleKey<ColorView> TABLE_HEADER_HOVER_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_HEADER_HOVER_COLOR, ColorView.class);
    /**
     * Цвет текста заголовков колонок; без значения используется {@link #TEXT_COLOR}.
     */
    public static final StyleKey<ColorView> TABLE_HEADER_TEXT_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_HEADER_TEXT_COLOR, ColorView.class);
    /**
     * Цвет фона чётных строк таблицы.
     */
    public static final StyleKey<ColorView> TABLE_ROW_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_ROW_COLOR, ColorView.class);
    /**
     * Цвет фона нечётных строк таблицы (zebra); прозрачный цвет отключает чередование.
     */
    public static final StyleKey<ColorView> TABLE_ALTERNATE_ROW_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_ALTERNATE_ROW_COLOR, ColorView.class);
    /**
     * Цвет подсветки строки под курсором.
     */
    public static final StyleKey<ColorView> TABLE_HOVER_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_HOVER_COLOR, ColorView.class);
    /**
     * Цвет фона выбранных строк таблицы.
     */
    public static final StyleKey<ColorView> TABLE_SELECTED_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_SELECTED_COLOR, ColorView.class);
    /**
     * Цвет линий сетки и разделителей колонок.
     */
    public static final StyleKey<ColorView> TABLE_GRID_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_GRID_COLOR, ColorView.class);
    /**
     * Набор линий сетки между ячейками.
     */
    public static final StyleKey<TableGridLines> TABLE_GRID_LINES =
            StyleKey.of(StyleIds.Key.TABLE_GRID_LINES, TableGridLines.class);
    /**
     * Цвет рамки активной ячейки и маркера перетаскиваемой колонки.
     */
    public static final StyleKey<ColorView> TABLE_ACTIVE_CELL_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_ACTIVE_CELL_COLOR, ColorView.class);
    /**
     * Горизонтальный внутренний отступ текста в ячейках и заголовках в UI-пикселях.
     */
    public static final StyleKey<Float> TABLE_CELL_PADDING =
            StyleKey.of(StyleIds.Key.TABLE_CELL_PADDING, Float.class);
    /**
     * Форма индикатора направления сортировки в заголовке.
     */
    public static final StyleKey<TableSortIndicatorStyle> TABLE_SORT_INDICATOR_STYLE =
            StyleKey.of(StyleIds.Key.TABLE_SORT_INDICATOR_STYLE, TableSortIndicatorStyle.class);
    /**
     * Цвет индикатора сортировки; без значения используется цвет текста заголовка.
     */
    public static final StyleKey<ColorView> TABLE_SORT_INDICATOR_COLOR =
            StyleKey.of(StyleIds.Key.TABLE_SORT_INDICATOR_COLOR, ColorView.class);

    /**
     * Цвет линии ряда у Sparkline и линейного Chart.
     */
    public static final StyleKey<ColorView> PLOT_LINE_COLOR = StyleKey.of(StyleIds.Key.PLOT_LINE_COLOR, ColorView.class);

    /**
     * Толщина линии ряда в логических пикселях.
     */
    public static final StyleKey<Float> PLOT_LINE_WIDTH = StyleKey.of(StyleIds.Key.PLOT_LINE_WIDTH, Float.class);

    /**
     * Цвет заливки области под линией ряда.
     */
    public static final StyleKey<ColorView> PLOT_FILL_COLOR = StyleKey.of(StyleIds.Key.PLOT_FILL_COLOR, ColorView.class);

    /**
     * Заливается ли область под линией ряда.
     */
    public static final StyleKey<Boolean> PLOT_FILL_VISIBLE = StyleKey.of(StyleIds.Key.PLOT_FILL_VISIBLE, Boolean.class);

    /**
     * Цвет маркера точки данных.
     */
    public static final StyleKey<ColorView> PLOT_POINT_COLOR = StyleKey.of(StyleIds.Key.PLOT_POINT_COLOR, ColorView.class);

    /**
     * Цвет маркера наведённой точки и направляющей.
     */
    public static final StyleKey<ColorView> PLOT_POINT_HOVER_COLOR = StyleKey.of(StyleIds.Key.PLOT_POINT_HOVER_COLOR, ColorView.class);

    /**
     * Радиус маркера точки данных в логических пикселях.
     */
    public static final StyleKey<Float> PLOT_POINT_RADIUS = StyleKey.of(StyleIds.Key.PLOT_POINT_RADIUS, Float.class);

    /**
     * Цвет линий осей и засечек у Chart.
     */
    public static final StyleKey<ColorView> PLOT_AXIS_COLOR = StyleKey.of(StyleIds.Key.PLOT_AXIS_COLOR, ColorView.class);

    /**
     * Цвет горизонтальных линий сетки у Chart.
     */
    public static final StyleKey<ColorView> PLOT_GRID_COLOR = StyleKey.of(StyleIds.Key.PLOT_GRID_COLOR, ColorView.class);

    /**
     * Рисует ли Chart горизонтальную сетку и подписи оси значений.
     */
    public static final StyleKey<Boolean> PLOT_GRID_VISIBLE = StyleKey.of(StyleIds.Key.PLOT_GRID_VISIBLE, Boolean.class);

    /**
     * Цвет столбцов у столбчатого Chart.
     */
    public static final StyleKey<ColorView> PLOT_BAR_COLOR = StyleKey.of(StyleIds.Key.PLOT_BAR_COLOR, ColorView.class);

    /**
     * Цвет наведённого столбца.
     */
    public static final StyleKey<ColorView> PLOT_BAR_HOVER_COLOR = StyleKey.of(StyleIds.Key.PLOT_BAR_HOVER_COLOR, ColorView.class);

    /**
     * Зазор между соседними столбцами в логических пикселях.
     */
    public static final StyleKey<Float> PLOT_BAR_GAP = StyleKey.of(StyleIds.Key.PLOT_BAR_GAP, Float.class);

    /**
     * Радиус скругления свободного конца столбца.
     */
    public static final StyleKey<Float> PLOT_BAR_RADIUS = StyleKey.of(StyleIds.Key.PLOT_BAR_RADIUS, Float.class);

    /**
     * Цвет фона всплывающей подсказки.
     */
    public static final StyleKey<ColorView> PLOT_TOOLTIP_BACKGROUND_COLOR = StyleKey.of(StyleIds.Key.PLOT_TOOLTIP_BACKGROUND_COLOR, ColorView.class);

    /**
     * Цвет рамки всплывающей подсказки.
     */
    public static final StyleKey<ColorView> PLOT_TOOLTIP_BORDER_COLOR = StyleKey.of(StyleIds.Key.PLOT_TOOLTIP_BORDER_COLOR, ColorView.class);

    /**
     * Цвет текста всплывающей подсказки.
     */
    public static final StyleKey<ColorView> PLOT_TOOLTIP_TEXT_COLOR = StyleKey.of(StyleIds.Key.PLOT_TOOLTIP_TEXT_COLOR, ColorView.class);

    /**
     * Цвет заливки узла GraphView.
     */
    public static final StyleKey<ColorView> GRAPH_NODE_COLOR = StyleKey.of(StyleIds.Key.GRAPH_NODE_COLOR, ColorView.class);

    /**
     * Цвет заливки наведённого узла.
     */
    public static final StyleKey<ColorView> GRAPH_NODE_HOVER_COLOR = StyleKey.of(StyleIds.Key.GRAPH_NODE_HOVER_COLOR, ColorView.class);

    /**
     * Цвет обводки узла.
     */
    public static final StyleKey<ColorView> GRAPH_NODE_BORDER_COLOR = StyleKey.of(StyleIds.Key.GRAPH_NODE_BORDER_COLOR, ColorView.class);

    /**
     * Радиус узла в логических пикселях.
     */
    public static final StyleKey<Float> GRAPH_NODE_RADIUS = StyleKey.of(StyleIds.Key.GRAPH_NODE_RADIUS, Float.class);

    /**
     * Цвет линий рёбер и стрелок.
     */
    public static final StyleKey<ColorView> GRAPH_EDGE_COLOR = StyleKey.of(StyleIds.Key.GRAPH_EDGE_COLOR, ColorView.class);

    /**
     * Цвет рёбер, связанных с наведённым узлом.
     */
    public static final StyleKey<ColorView> GRAPH_EDGE_HOVER_COLOR = StyleKey.of(StyleIds.Key.GRAPH_EDGE_HOVER_COLOR, ColorView.class);

    /**
     * Толщина линий рёбер в логических пикселях.
     */
    public static final StyleKey<Float> GRAPH_EDGE_WIDTH = StyleKey.of(StyleIds.Key.GRAPH_EDGE_WIDTH, Float.class);

    /**
     * Длина стрелки направленного ребра в логических пикселях.
     */
    public static final StyleKey<Float> GRAPH_ARROW_SIZE = StyleKey.of(StyleIds.Key.GRAPH_ARROW_SIZE, Float.class);

    /**
     * Длительность автоматического перехода style-свойств в секундах. Ноль отключает переходы.
     */
    public static final StyleKey<Float> TRANSITION_DURATION = StyleKey.of(StyleIds.Key.TRANSITION_DURATION, Float.class);

    /**
     * Стандартная easing-кривая автоматического перехода style-свойств.
     */
    public static final StyleKey<AnimationEasing> TRANSITION_EASING =
            StyleKey.of(StyleIds.Key.TRANSITION_EASING, AnimationEasing.class);

    private StyleKeys() {
    }
}
