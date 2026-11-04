package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.ChartBarClickEvent;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.xaml.XamlProperty;
import dev.sixik.unigui.xaml.XamlWidget;
import dev.sixik.unigui.widgets.WidgetStyles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * График с осями: линейный или столбчатый, с сеткой, подписями делений, подписями категорий и подсказкой.
 *
 * <h2>Шкала</h2>
 * <p>Ось значений строится по «красивым» числам ({@link PlotSupport#niceScale}): границы кратны шагу вида
 * {1, 2, 5} * 10^k. Столбчатый график всегда включает ноль, а столбцы растут от нулевой линии вверх (положительные)
 * или вниз (отрицательные). Область построения делится на {@code n} равных слотов; точка линии и столбец стоят по
 * центру слота, поэтому подписи категорий совпадают с данными.</p>
 *
 * <h2>Стиль</h2>
 * <p>Оформление берётся из ключей {@code plot.*}, {@code background.*}, {@code border.*} и {@code text.*}; значения
 * по умолчанию — {@link WidgetStyles.Plot#CHART}. Цвета, изменённые на экземпляре ({@link #barColor()} и другие),
 * приоритетнее стиля.</p>
 */
@AiDoc(
        summary = "Data visualization chart widget supporting bar and line visualizations, axes, and interactive hover tooltips.",
        purpose = "Provides full-featured data chart visualization for metrics, histograms, and statistics.",
        useWhen = {
                "Statistical reports, recipe yields, or economy transaction history.",
                "Switching between line graphs and bar histograms."
        },
        criticalRules = {
                "type() selects between Type.LINE and Type.BAR rendering modes.",
                "axisColor configures coordinate axis line and tick markings.",
                "Category label at index i belongs to value at index i; missing labels are allowed.",
                "Bar charts include zero in the value scale, so positive and negative bars share a zero baseline.",
                "Non-finite values create gaps and do not participate in the plotted scale."
        },
        examples = @AiDoc.Example(
                title = "Build a categorized bar chart",
                code = "new Chart().type(Chart.Type.BAR).values(List.of(12, -4, 18))\n        .categories(List.of(\"A\", \"B\", \"C\"));"),
        related = {"Sparkline", "WidgetBase"})
@WikiPage(
        title = "<key:chart.title>",
        slug = "chart",
        category = "<key:core.widgets.display.category>",
        summary = "<key:chart.summary>",
        order = 140)
@WikiSection(
        title = "<key:chart.section.overview.title>",
        markdown = "<key:chart.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:chart.example.basic.title>",
        code = "<key:chart.example.basic.code>",
        order = 10)
@XamlWidget("Chart")
public final class Chart extends Sparkline {
    /**
     * Style registry id used to resolve this widget's default chart style.
     */
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CHART;

    /**
     * Ширина по умолчанию в UI-пикселях.
     */
    public static final float DEFAULT_PREFERRED_WIDTH = 220.0f;
    /**
     * Высота по умолчанию в UI-пикселях.
     */
    public static final float DEFAULT_PREFERRED_HEIGHT = 120.0f;

    /**
     * Минимальная высота столбца: нулевое значение остаётся видимой чертой.
     */
    private static final float ZERO_BAR_HEIGHT = 1.0f;
    /**
     * Внешний отступ области графика от края виджета в UI-пикселях.
     */
    private static final float OUTER_PADDING = 6.0f;
    /**
     * Длина засечки на оси значений в UI-пикселях.
     */
    private static final float TICK_LENGTH = 3.0f;
    /**
     * Зазор между засечкой и подписью в UI-пикселях.
     */
    private static final float LABEL_GAP = 3.0f;
    /**
     * Зазор между столбцом и подписью его значения в UI-пикселях.
     */
    private static final float VALUE_GAP = 2.0f;
    /**
     * Зазор между подсказкой и столбцом в UI-пикселях.
     */
    private static final float TOOLTIP_GAP = 6.0f;

    private final OverridableColor axisColor = new OverridableColor(
            new MutableColor(0.35f, 0.45f, 0.58f, 0.85f), this::repaintChart);
    private final OverridableColor gridColor = new OverridableColor(
            new MutableColor(0.35f, 0.45f, 0.58f, 0.22f), this::repaintChart);
    private final OverridableColor barColor = new OverridableColor(
            new MutableColor(0.25f, 0.72f, 1.0f, 0.72f), this::repaintChart);
    private final OverridableColor hoveredBarColor = new OverridableColor(
            new MutableColor(1.0f, 0.78f, 0.28f, 0.88f), this::repaintChart);

    private Type type = Type.LINE;
    private boolean barValuesVisible = true;
    private BarValuePlacement barValuePlacement = BarValuePlacement.HEAD;
    private BarRenderer barRenderer;
    private BarValueRenderer barValueRenderer;
    private BarTooltipRenderer barTooltipRenderer;
    private List<String> categories = List.of();
    private int categoryVersion;

    // ---- шкала и подписи осей (кэш) ----
    /**
     * Результат {@link PlotSupport#niceScale}: границы шкалы и шаг.
     */
    private final float[] scale = new float[3];
    /**
     * Шаг шкалы значений.
     */
    float tickStep = 1.0f;
    /**
     * Число делений шкалы, включая крайние.
     */
    private int tickCount = 2;
    private RichText[] tickTexts = new RichText[0];
    private float[] tickWidths = new float[0];
    private float tickLabelWidth;
    private float tickLabelHeight;
    private float keyTickMin = Float.NaN;
    private float keyTickMax = Float.NaN;
    private float keyTickStep = Float.NaN;
    private float keyTickSize = -1.0f;
    private RichText[] categoryTexts = new RichText[0];
    private float[] categoryWidths = new float[0];
    private float categoryHeight;
    private int keyCategoryVersion = -1;
    private float keyCategorySize = -1.0f;

    /**
     * Рабочий массив геометрии столбца: x, верх, ширина, высота, основание.
     */
    private final float[] barRect = new float[5];

    @AiDoc(summary = "Creates a chart with the default preferred dimensions and line rendering mode.")
    @WikiMethod(title = "Create a chart", summary = "Creates a chart with its default size and line mode.", order = 10)
    public Chart() {
        super(DEFAULT_PREFERRED_WIDTH, DEFAULT_PREFERRED_HEIGHT);
    }

    /**
     * Возвращает стандартное оформление графика; уступает теме и локальным стилям.
     *
     * @return {@link WidgetStyles.Plot#CHART}
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Plot.CHART;
    }

    private void repaintChart() {
        invalidate(InvalidationFlags.VISUAL);
    }

    /**
     * Rendering modes supported by the chart.
     */
    public enum Type {
        /**
         * Draws values as a connected line.
         */
        LINE,
        /**
         * Draws one bar per value from the shared zero baseline.
         */
        BAR
    }

    // ------------------------------------------------------------------ данные и настройки

    @Override
    @AiDoc(summary = "Replaces the chart data from a list of Number values; null clears the data and null elements become zero.", section = "Data", sectionOrder = 10, order = 10)
    @WikiMethod(title = "Set chart values", summary = "Replaces the plotted values from a Number list.", section = "Data", sectionOrder = 10, order = 10)
    @WikiReturn("This chart for fluent configuration.")
    public Chart values(@WikiParameter(name = "values", description = "Values in data order; null clears the series and null entries are treated as zero.") List<? extends Number> values) {
        super.values(values);
        return this;
    }

    @Override
    @WikiMethod(title = "Set chart values from floats", summary = "Replaces the plotted values without boxing numeric inputs.", section = "Data", sectionOrder = 10, order = 20)
    @WikiReturn("This chart for fluent configuration.")
    public Chart values(@WikiParameter(name = "data", description = "Values in data order; null clears the series and non-finite values create gaps.") float... data) {
        super.values(data);
        return this;
    }

    /**
     * Задаёт подписи категорий под осью X.
     *
     * <p>Подпись {@code i} относится к значению {@code i}. Подписи, не помещающиеся в слот, прореживаются, чтобы не
     * налезать друг на друга. Название категории также попадает в подсказку.</p>
     *
     * @param categories подписи; {@code null} или пустой список скрывает подписи
     * @return этот график
     */
    @AiDoc(summary = "Sets optional x-axis labels; category i labels value i and labels may be omitted or left null.", section = "Data", sectionOrder = 10, order = 30)
    @WikiMethod(title = "Set category labels", summary = "Sets optional labels aligned by index with the plotted values.", section = "Data", sectionOrder = 10, order = 30)
    @WikiReturn("This chart for fluent configuration.")
    public Chart categories(@WikiParameter(name = "categories", description = "Category labels in value order; null or an empty list hides labels.") List<String> categories) {
        this.categories = categories == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(categories));
        categoryVersion++;
        invalidateTexts();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает подписи категорий.
     *
     * @return неизменяемый список подписей
     */
    @WikiMethod(title = "Get category labels", summary = "Returns the current immutable category-label list.", section = "Data", sectionOrder = 10, order = 40)
    @WikiReturn("An immutable list of category labels, empty when labels are not configured.")
    public List<String> categories() {
        return categories;
    }

    /**
     * Сбрасывает кэши текстов, зависящих от категорий.
     *
     * <p>Текст подсказки кэшируется по {@code dataVersion}; увеличение версии заставляет пересоздать его и
     * пересчитать раскладку на следующем кадре.</p>
     */
    private void invalidateTexts() {
        dataVersion++;
        layoutValid = false;
    }

    @XamlProperty(value = "type", category = "Appearance", defaultValue = "line", description = "Chart rendering type.")
    @WikiMethod(
            title = "<key:chart.method.type.title>",
            summary = "<key:chart.method.type.summary>")
    @AiDoc(summary = "Selects line or bar rendering; null resets to line mode.", section = "Appearance", sectionOrder = 20, order = 10)
    @WikiReturn("This chart for fluent configuration.")
    public Chart type(@WikiParameter(name = "type", description = "Rendering mode; null selects LINE.") Type type) {
        Type normalized = type == null ? Type.LINE : type;
        if (this.type == normalized) return this;
        this.type = normalized;
        hoveredIndex = -1;
        layoutValid = false;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Get chart type", summary = "Returns the active line or bar rendering mode.", section = "Appearance", sectionOrder = 20, order = 20)
    @WikiReturn("The current rendering mode; defaults to LINE.")
    public Type type() {
        return type;
    }

    @WikiMethod(
            title = "<key:chart.method.axis_color.title>",
            summary = "<key:chart.method.axis_color.summary>")
    @WikiReturn("The mutable RGBA axis color; changing it overrides the style value.")
    public MutableColor axisColor() {
        return axisColor.value();
    }

    /**
     * Возвращает цвет линий сетки.
     *
     * @return изменяемый цвет; изменение переопределяет стиль {@code plot.grid.color}
     */
    public MutableColor gridColor() {
        return gridColor.value();
    }

    @WikiMethod(
            title = "<key:chart.method.bar_color.title>",
            summary = "<key:chart.method.bar_color.summary>")
    @WikiReturn("The mutable RGBA bar color; changing it overrides the style value.")
    public MutableColor barColor() {
        return barColor.value();
    }

    @WikiMethod(title = "Get hovered bar color", summary = "Returns the mutable color used for the hovered bar.", section = "Appearance", sectionOrder = 20, order = 50)
    @WikiReturn("The mutable RGBA hovered-bar color.")
    public MutableColor hoveredBarColor() {
        return hoveredBarColor.value();
    }

    /**
     * Возвращает цвет подписей значений; совпадает с {@link #labelColor()}.
     *
     * @return изменяемый цвет подписей
     */
    public MutableColor valueColor() {
        return labelColor.value();
    }

    @XamlProperty(value = "barValuesVisible", category = "Appearance", defaultValue = "true", description = "Whether bar value labels are rendered.")
    @AiDoc(summary = "Enables or hides numeric labels on bars; this affects only BAR mode.", section = "Appearance", sectionOrder = 20, order = 60)
    @WikiMethod(title = "Show bar values", summary = "Enables or hides numeric labels rendered on bars.", section = "Appearance", sectionOrder = 20, order = 60)
    @WikiReturn("This chart for fluent configuration.")
    public Chart barValuesVisible(@WikiParameter(name = "barValuesVisible", description = "True to render bar value labels.") boolean barValuesVisible) {
        if (this.barValuesVisible == barValuesVisible) return this;
        this.barValuesVisible = barValuesVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XamlProperty(value = "barValuePlacement", category = "Appearance", defaultValue = "head", description = "Placement of bar value labels.")
    @WikiMethod(title = "Place bar value labels", summary = "Selects where numeric labels appear relative to each bar.", section = "Appearance", sectionOrder = 20, order = 70)
    @WikiReturn("This chart for fluent configuration.")
    public Chart barValuePlacement(@WikiParameter(name = "barValuePlacement", description = "Placement mode; null resets to HEAD.") BarValuePlacement barValuePlacement) {
        this.barValuePlacement = barValuePlacement == null ? BarValuePlacement.HEAD : barValuePlacement;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @AiDoc(summary = "Installs an optional callback that replaces the default bar drawing.", section = "Custom Rendering", sectionOrder = 30, order = 10)
    @WikiMethod(title = "Customize bar rendering", summary = "Installs or clears a custom bar renderer.", section = "Custom Rendering", sectionOrder = 30, order = 10)
    @WikiReturn("This chart for fluent configuration.")
    public Chart barRenderer(@WikiParameter(name = "barRenderer", description = "Custom renderer callback, or null to use the built-in bar rendering.") BarRenderer barRenderer) {
        this.barRenderer = barRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Customize bar value labels", summary = "Installs or clears a custom bar-value label renderer.", section = "Custom Rendering", sectionOrder = 30, order = 20)
    @WikiReturn("This chart for fluent configuration.")
    public Chart barValueRenderer(@WikiParameter(name = "barValueRenderer", description = "Custom renderer callback, or null to use built-in value labels.") BarValueRenderer barValueRenderer) {
        this.barValueRenderer = barValueRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Customize bar tooltips", summary = "Installs or clears a custom tooltip renderer for hovered bars.", section = "Custom Rendering", sectionOrder = 30, order = 30)
    @WikiReturn("This chart for fluent configuration.")
    public Chart barTooltipRenderer(@WikiParameter(name = "barTooltipRenderer", description = "Custom renderer callback, or null to use the built-in tooltip.") BarTooltipRenderer barTooltipRenderer) {
        this.barTooltipRenderer = barTooltipRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    @WikiMethod(title = "Set chart renderer", summary = "Sets the widget-level renderer used for chart rendering hooks.", section = "Custom Rendering", sectionOrder = 30, order = 40)
    @WikiReturn("This chart for fluent configuration.")
    public Chart renderer(@WikiParameter(name = "renderer", description = "Widget renderer callback.") WidgetRender renderer) {
        super.renderer(renderer);
        return this;
    }

    @Override
    @XamlProperty(value = "preferredWidth", category = "Layout", defaultValue = "220", description = "Intrinsic chart width before layout constraints are applied.")
    @WikiMethod(title = "Set preferred chart width", summary = "Sets the preferred chart width in UI pixels.", section = "Layout", sectionOrder = 40, order = 10)
    @WikiReturn("This chart for fluent configuration.")
    public Chart preferredWidth(@WikiParameter(name = "preferredWidth", description = "Preferred width in UI pixels; non-positive or non-finite values use the default.") float preferredWidth) {
        super.preferredWidth(preferredWidth);
        return this;
    }

    @Override
    @XamlProperty(value = "preferredHeight", category = "Layout", defaultValue = "120", description = "Intrinsic chart height before layout constraints are applied.")
    @WikiMethod(title = "Set preferred chart height", summary = "Sets the preferred chart height in UI pixels.", section = "Layout", sectionOrder = 40, order = 20)
    @WikiReturn("This chart for fluent configuration.")
    public Chart preferredHeight(@WikiParameter(name = "preferredHeight", description = "Preferred height in UI pixels; non-positive or non-finite values use the default.") float preferredHeight) {
        super.preferredHeight(preferredHeight);
        return this;
    }

    @Override
    @WikiMethod(title = "Set preferred chart size", summary = "Sets preferred width and height in UI pixels.", section = "Layout", sectionOrder = 40, order = 30)
    @WikiReturn("This chart for fluent configuration.")
    public Chart preferredSize(
            @WikiParameter(name = "width", description = "Preferred width in UI pixels.") float width,
            @WikiParameter(name = "height", description = "Preferred height in UI pixels.") float height) {
        super.preferredSize(width, height);
        return this;
    }

    @AiDoc(summary = "Subscribes to clicks on bars and provides the clicked bar's data and rendered geometry.", section = "Interaction", sectionOrder = 50, order = 10)
    @WikiMethod(title = "Listen for bar clicks", summary = "Registers a listener for clicks on chart bars.", section = "Interaction", sectionOrder = 50, order = 10)
    @WikiReturn("A subscription handle that can be cancelled.")
    public EventSubscription onBarClick(@WikiParameter(name = "listener", description = "Listener receiving the clicked bar event.") EventListener<? super ChartBarClickEvent> listener) {
        return on(ChartBarClickEvent.TYPE, listener);
    }

    // ------------------------------------------------------------------ стиль и раскладка

    @Override
    void resolveStyle() {
        super.resolveStyle();
        PlotPalette p = palette;
        p.axis = axisColor.resolve(styleValue(StyleKeys.PLOT_AXIS_COLOR, (ColorView) null));
        p.grid = gridColor.resolve(styleValue(StyleKeys.PLOT_GRID_COLOR, (ColorView) null));
        p.gridVisible = styleValue(StyleKeys.PLOT_GRID_VISIBLE, true);
        p.bar = barColor.resolve(styleValue(StyleKeys.PLOT_BAR_COLOR, (ColorView) null));
        p.barHover = hoveredBarColor.resolve(styleValue(StyleKeys.PLOT_BAR_HOVER_COLOR, (ColorView) null));
        p.barGap = Math.max(0.0f, styleValue(StyleKeys.PLOT_BAR_GAP, 4.0f));
        p.barRadius = Math.max(0.0f, styleValue(StyleKeys.PLOT_BAR_RADIUS, 2.0f));
    }

    /**
     * Вычисляет шкалу, подписи осей и область построения.
     *
     * <p>Границы шкалы берутся из {@link PlotSupport#niceScale}; для столбцов в диапазон включается ноль. Поля
     * области считаются от измеренных подписей, поэтому текст не обрезается при любом размере шрифта:
     * <pre>
     *   left   = pad + tickLabelWidth + TICK_LENGTH + LABEL_GAP        (если сетка включена, иначе pad)
     *   top    = pad + tickLabelHeight / 2                              (верхняя подпись выступает над линией)
     *   bottom = pad + categoryHeight                                   (если заданы категории)
     *   plot   = bounds - (left, top, pad, bottom)
     * </pre>
     *
     * @param context контекст отрисовки; даёт метрики шрифта для замера подписей
     */
    @Override
    void updatePlot(RenderContext context) {
        PlotPalette p = palette;
        RectView bounds = layoutBounds();
        float min = dataMin;
        float max = dataMax;
        if (type == Type.BAR) {
            min = Math.min(0.0f, min);
            max = Math.max(0.0f, max);
        }
        // Число делений зависит от высоты шрифта: около двух строк на деление, чтобы подписи не слипались.
        float estimatedHeight = Math.max(1.0f, bounds.height() - OUTER_PADDING * 2.0f);
        int target = Math.max(2, Math.min(8, Math.round(estimatedHeight / (p.textSize * 3.2f))));
        PlotSupport.niceScale(min, max, target, scale);
        scaleMin = scale[0];
        scaleMax = scale[1];
        tickStep = scale[2];
        tickCount = Math.max(2, Math.round((scaleMax - scaleMin) / tickStep) + 1);

        refreshTickTexts(context);
        refreshCategoryTexts(context);

        float left = OUTER_PADDING;
        float top = OUTER_PADDING;
        if (p.gridVisible) {
            left += tickLabelWidth + TICK_LENGTH + LABEL_GAP;
            top += tickLabelHeight * 0.5f;
        }
        float bottom = OUTER_PADDING + categoryHeight;
        plotX = bounds.x() + left;
        plotY = bounds.y() + top;
        plotW = Math.max(0.0f, bounds.width() - left - OUTER_PADDING);
        plotH = Math.max(0.0f, bounds.height() - top - bottom);
    }

    private void refreshTickTexts(RenderContext context) {
        PlotPalette p = palette;
        if (keyTickMin == scaleMin && keyTickMax == scaleMax && keyTickStep == tickStep
                && keyTickSize == p.textSize && tickTexts.length >= tickCount) {
            return;
        }
        if (tickTexts.length < tickCount) {
            tickTexts = new RichText[tickCount];
            tickWidths = new float[tickCount];
        }
        float widest = 0.0f;
        float tallest = 0.0f;
        for (int i = 0; i < tickCount; i++) {
            RichText text = PlotSupport.text(PlotSupport.formatTick(scaleMin + i * tickStep, tickStep), p.textSize);
            tickTexts[i] = text;
            tickWidths[i] = PlotSupport.textWidth(context, text);
            widest = Math.max(widest, tickWidths[i]);
            tallest = Math.max(tallest, PlotSupport.textHeight(context, text));
        }
        tickLabelWidth = widest;
        tickLabelHeight = tallest;
        keyTickMin = scaleMin;
        keyTickMax = scaleMax;
        keyTickStep = tickStep;
        keyTickSize = p.textSize;
    }

    private void refreshCategoryTexts(RenderContext context) {
        PlotPalette p = palette;
        int n = categories.size();
        if (n == 0) {
            categoryHeight = 0.0f;
            return;
        }
        if (keyCategoryVersion != categoryVersion || keyCategorySize != p.textSize || categoryTexts.length < n) {
            categoryTexts = new RichText[n];
            categoryWidths = new float[n];
            float tallest = 0.0f;
            for (int i = 0; i < n; i++) {
                RichText text = PlotSupport.text(categories.get(i), p.textSize);
                categoryTexts[i] = text;
                categoryWidths[i] = PlotSupport.textWidth(context, text);
                tallest = Math.max(tallest, PlotSupport.textHeight(context, text));
            }
            categoryHeight = tallest + LABEL_GAP + TICK_LENGTH;
            keyCategoryVersion = categoryVersion;
            keyCategorySize = p.textSize;
        }
    }

    /**
     * Возвращает X точки: центр слота.
     *
     * <p>Формула: {@code x = plotX + plotW * (i + 0.5) / n}, где {@code n} — число значений.</p>
     *
     * @param index индекс значения
     * @return координата X центра слота
     */
    @Override
    float pointX(int index) {
        int n = Math.max(1, values.size());
        return plotX + plotW * (index + 0.5f) / n;
    }

    /**
     * Оценивает индекс по X как номер слота: {@code i = floor((x - plotX) / plotW * n)}.
     *
     * @param rootX X указателя
     * @return индекс слота в диапазоне {@code [0, n - 1]}
     */
    @Override
    int guessIndex(float rootX) {
        int n = values.size();
        if (n <= 1 || plotW <= 0.0f) return 0;
        int slot = (int) Math.floor((rootX - plotX) / plotW * n);
        return Math.max(0, Math.min(n - 1, slot));
    }

    @Override
    String tooltipLabel(int index) {
        String value = PlotSupport.formatValue(values.getFloat(index));
        if (index < categories.size() && categories.get(index) != null && !categories.get(index).isEmpty()) {
            return categories.get(index) + ": " + value;
        }
        return super.tooltipLabel(index);
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    void renderPlot(DrawScope draw) {
        if (!layoutValid) return;
        renderAxes(draw);
        if (type == Type.BAR) {
            renderBars(draw);
        } else {
            renderSparklineVisual(draw);
        }
    }

    /**
     * Рисует сетку, засечки, подписи делений, оси и подписи категорий.
     *
     * <p>Деление {@code t} лежит на значении {@code scaleMin + t * tickStep}; его Y даёт {@link #valueY(float)}.
     * Линия нуля выделяется цветом оси, если шкала пересекает ноль.</p>
     *
     * @param draw область рисования
     */
    private void renderAxes(DrawScope draw) {
        PlotPalette p = palette;
        float left = plotX;
        float right = plotX + plotW;
        float bottom = plotY + plotH;
        RenderContext context = draw.context();
        if (p.gridVisible) {
            for (int t = 0; t < tickCount; t++) {
                float y = valueY(scaleMin + t * tickStep);
                draw.addLine(left, y, right, y, p.grid, 1.0f);
                draw.addLine(left - TICK_LENGTH, y, left, y, p.axis, 1.0f);
                float width = tickWidths[t];
                float height = PlotSupport.textHeight(context, tickTexts[t]);
                PlotSupport.drawText(draw, tickTexts[t], left - TICK_LENGTH - LABEL_GAP - width, y - height * 0.5f,
                        width, height, p.text, Alignment.START, Alignment.CENTER);
            }
        }
        if (scaleMin < 0.0f && scaleMax > 0.0f) {
            float zero = valueY(0.0f);
            draw.addLine(left, zero, right, zero, p.axis, 1.0f);
        }
        draw.addLine(left, plotY, left, bottom, p.axis, 1.0f);
        draw.addLine(left, bottom, right, bottom, p.axis, 1.0f);
        renderCategories(draw);
    }

    private void renderCategories(DrawScope draw) {
        int n = Math.min(categories.size(), values.size());
        if (n == 0 || plotW <= 0.0f) return;
        PlotPalette p = palette;
        float slot = plotW / values.size();
        float widest = 0.0f;
        for (int i = 0; i < n; i++) widest = Math.max(widest, categoryWidths[i]);
        // Подписи прореживаются: показываем каждую k-ю, чтобы соседние не перекрывались.
        int stride = Math.max(1, (int) Math.ceil((widest + 4.0f) / slot));
        float y = plotY + plotH + TICK_LENGTH + LABEL_GAP * 0.5f;
        for (int i = 0; i < n; i += stride) {
            float width = categoryWidths[i];
            float height = categoryHeight - TICK_LENGTH - LABEL_GAP;
            float center = plotX + slot * (i + 0.5f);
            draw.addLine(center, plotY + plotH, center, plotY + plotH + TICK_LENGTH, p.axis, 1.0f);
            PlotSupport.drawText(draw, categoryTexts[i], center - width * 0.5f, y, width, height, p.text,
                    Alignment.CENTER, Alignment.START);
        }
    }

    /**
     * Вычисляет геометрию столбца в {@link #barRect}.
     *
     * <p>Формулы (слот шириной {@code s = plotW / n}):
     * <pre>
     *   gap      = min(barGap, 0.6 * s)
     *   width    = max(1, s - gap)
     *   x        = plotX + i * s + (s - width) / 2
     *   baseline = valueY(0)
     *   top      = min(valueY(v), baseline),   height = max(|valueY(v) - baseline|, ZERO_BAR_HEIGHT)
     * </pre>
     * Столбец нулевого значения превращается в линию толщиной {@link #ZERO_BAR_HEIGHT} по нулевой линии.
     *
     * @param index индекс столбца
     */
    private void computeBar(int index) {
        int n = values.size();
        float slot = plotW / Math.max(1, n);
        float gap = Math.min(palette.barGap, slot * 0.6f);
        float width = Math.max(1.0f, slot - gap);
        float baseline = valueY(0.0f);
        float valueY = valueY(values.getFloat(index));
        float top = Math.min(valueY, baseline);
        float height = Math.max(Math.abs(valueY - baseline), ZERO_BAR_HEIGHT);
        if (values.getFloat(index) == 0.0f) top = baseline - ZERO_BAR_HEIGHT * 0.5f;
        barRect[0] = plotX + index * slot + (slot - width) * 0.5f;
        barRect[1] = top;
        barRect[2] = width;
        barRect[3] = height;
        barRect[4] = baseline;
    }

    private Bar barOf(int index, boolean hovered) {
        computeBar(index);
        return new Bar(index, values.getFloat(index), barRect[0], barRect[1], barRect[2], barRect[3], barRect[4], hovered);
    }

    private void renderBars(DrawScope draw) {
        PlotPalette p = palette;
        int n = values.size();
        for (int i = 0; i < n; i++) {
            float value = values.getFloat(i);
            if (!Float.isFinite(value)) continue;
            boolean hovered = i == hoveredIndex;
            computeBar(i);
            if (barRenderer != null) {
                barRenderer.render(draw, new Bar(i, value, barRect[0], barRect[1], barRect[2], barRect[3],
                        barRect[4], hovered));
            } else {
                drawBar(draw, barRect[0], barRect[1], barRect[2], barRect[3], value >= 0.0f,
                        hovered ? p.barHover : p.bar);
            }
            if (barValuesVisible && barValuePlacement != BarValuePlacement.NONE) {
                renderBarValue(draw, i, hovered);
            }
        }
        if (hoveredIndex >= 0 && hoveredIndex < n && Float.isFinite(values.getFloat(hoveredIndex))) {
            renderTooltip(draw, hoveredIndex);
        }
    }

    /**
     * Рисует столбец; свободный конец скругляется, а конец у нулевой линии остаётся прямым.
     *
     * <p>Скругление получается одной командой: скруглённый прямоугольник рисуется на радиус длиннее в сторону
     * нулевой линии и обрезается областью столбца. Так полупрозрачный цвет не накладывается сам на себя.</p>
     *
     * @param draw   область рисования
     * @param x      левый край столбца
     * @param top    верхний край столбца
     * @param width  ширина столбца
     * @param height высота столбца
     * @param up     {@code true} — столбец растёт вверх от нулевой линии
     * @param color  цвет заливки
     */
    private void drawBar(DrawScope draw, float x, float top, float width, float height, boolean up, ColorView color) {
        float radius = Math.min(palette.barRadius, Math.min(width, height) * 0.5f);
        if (radius < 0.5f) {
            draw.rectFilled(x, top, width, height, color);
            return;
        }
        draw.pushClip(x, top, width, height);
        try {
            if (up) {
                draw.roundedRectFilled(x, top, width, height + radius, radius, color);
            } else {
                draw.roundedRectFilled(x, top - radius, width, height + radius, radius, color);
            }
        } finally {
            draw.popClip();
        }
    }

    private void renderBarValue(DrawScope draw, int index, boolean hovered) {
        if (barValueRenderer != null) {
            barValueRenderer.render(draw, barOf(index, hovered));
            return;
        }
        RenderContext context = draw.context();
        RichText text = labelText(index);
        float width = PlotSupport.textWidth(context, text);
        float height = PlotSupport.textHeight(context, text);
        float slot = plotW / Math.max(1, values.size());
        // Подпись шире слота налезла бы на соседние: показываем её только у наведённого столбца.
        if (width > slot + 2.0f && !hovered) return;
        float value = values.getFloat(index);
        float top = barRect[1];
        float bottom = barRect[1] + barRect[3];
        float baseline = barRect[4];
        float y = switch (barValuePlacement) {
            case CENTER -> top + (barRect[3] - height) * 0.5f;
            case BASE -> value >= 0.0f ? baseline - height - VALUE_GAP : baseline + VALUE_GAP;
            case BELOW -> baseline + VALUE_GAP;
            case HEAD, NONE -> value >= 0.0f ? top - height - VALUE_GAP : bottom + VALUE_GAP;
        };
        RectView bounds = layoutBounds();
        y = PlotSupport.clamp(y, bounds.y() + 1.0f, bounds.y() + bounds.height() - height - 1.0f);
        float x = barRect[0] + (barRect[2] - width) * 0.5f;
        PlotSupport.drawText(draw, text, x, y, width, height, palette.text, Alignment.CENTER, Alignment.CENTER);
    }

    @Override
    void renderTooltip(DrawScope draw, int index) {
        if (type == Type.BAR) {
            if (barTooltipRenderer != null) {
                barTooltipRenderer.render(draw, barOf(index, true));
                return;
            }
            // Подсказка привязывается к свободному концу столбца, а не к центру слота линии.
            computeBar(index);
            float anchorY = values.getFloat(index) >= 0.0f ? barRect[1] : barRect[1] + barRect[3];
            drawTooltipAt(draw, index, barRect[0] + barRect[2] * 0.5f, anchorY);
            return;
        }
        super.renderTooltip(draw, index);
    }

    private void drawTooltipAt(DrawScope draw, int index, float anchorX, float anchorY) {
        PlotPalette p = palette;
        PlotSupport.drawTooltip(draw, PlotSupport.text(tooltipLabel(index), p.textSize), anchorX, anchorY,
                TOOLTIP_GAP, layoutBounds(), p.tooltipBackground, p.tooltipBorder, p.tooltipText);
    }

    // ------------------------------------------------------------------ события

    @Override
    boolean interactive() {
        return type == Type.BAR || super.interactive();
    }

    /**
     * Находит столбец под указателем: любой Y внутри области построения в пределах слота.
     *
     * <p>Наведение по всему столбцу слота (включая зазор и пустую часть над столбцом) удобнее, чем попадание в
     * прямоугольник тонкого столбца с маленьким значением.</p>
     *
     * @param rootX X указателя
     * @param rootY Y указателя
     * @return индекс столбца или {@code -1}
     */
    @Override
    int hitIndex(float rootX, float rootY) {
        if (type != Type.BAR) return super.hitIndex(rootX, rootY);
        int n = values.size();
        if (!layoutValid || n == 0 || plotW <= 0.0f) return -1;
        if (rootX < plotX || rootX > plotX + plotW || rootY < plotY || rootY > plotY + plotH) return -1;
        int slot = guessIndex(rootX);
        return Float.isFinite(values.getFloat(slot)) ? slot : -1;
    }

    @Override
    void activate(int index) {
        if (type != Type.BAR) {
            super.activate(index);
            return;
        }
        Bar bar = barOf(index, true);
        ChartBarClickEvent event = new ChartBarClickEvent(this,
                bar.index(), bar.value(), bar.x(), bar.y(), bar.width(), bar.height(), bar.baseline());
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
    }

    public enum BarValuePlacement {
        NONE,
        HEAD,
        CENTER,
        BASE,
        BELOW
    }

    public record Bar(int index, float value, float x, float y, float width, float height, float baseline,
                      boolean hovered) {
        public float centerX() {
            return x + width * 0.5f;
        }

        public float centerY() {
            return y + height * 0.5f;
        }
    }

    @FunctionalInterface
    public interface BarRenderer {
        void render(DrawScope draw, Bar bar);
    }

    @FunctionalInterface
    public interface BarValueRenderer {
        void render(DrawScope draw, Bar bar);
    }

    @FunctionalInterface
    public interface BarTooltipRenderer {
        void render(DrawScope draw, Bar bar);
    }
}
