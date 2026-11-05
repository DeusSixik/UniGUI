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
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SparkPointClickEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.xaml.XamlProperty;
import dev.sixik.unigui.xaml.XamlWidget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.WidgetStyles;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Collections;
import java.util.List;

/**
 * Компактный график тренда: линия ряда, заливка с затуханием под ней, маркеры и подсказка при наведении.
 *
 * <h2>Модель данных</h2>
 * <p>Ряд значений равномерно растягивается по ширине области построения, значения масштабируются по вертикали
 * между минимумом и максимумом ряда. Не конечные значения ({@code NaN}, бесконечности) образуют разрыв линии.
 * Ряд из одинаковых значений рисуется по центру, ряд из одного значения — точкой.</p>
 *
 * <h2>Стиль</h2>
 * <p>Оформление берётся из style-системы (ключи {@code plot.*}, {@code background.*}, {@code border.*},
 * {@code text.*}), значения по умолчанию лежат в {@link WidgetStyles.Plot#SPARKLINE}. Цвета, изменённые на
 * экземпляре ({@link #lineColor()} и другие), приоритетнее стиля.</p>
 *
 * <h2>Взаимодействие</h2>
 * <p>При {@link PointMode} отличном от {@code NONE} виджет подсвечивает ближайшую к курсору по горизонтали точку,
 * показывает направляющую и подсказку; нажатие левой кнопки отправляет {@link SparkPointClickEvent}.</p>
 *
 * <h2>Производительность</h2>
 * <p>Геометрия ряда и сетка заливки пересчитываются только при смене данных, размера или стиля и хранятся в
 * массивах; кадр без изменений не создаёт ни точек, ни списков, ни сетки. Остаток аллокаций (около 2 КБ на кадр для
 * ряда из 60 точек) приходит из API рисования ({@code Paint} на каждый {@code add*}) и пользовательских
 * renderer'ов, которым передаются {@link SparkPoint}. Замеры: {@code PlotRenderBenchmark}.</p>
 */
@AiDoc(
        summary = "Compact mini trendline and telemetry chart widget with fill gradient and hover data points.",
        purpose = "Displays high-density numerical trends in compact telemetry graphs.",
        useWhen = {
                "Server TPS, player ping, FPS history, or memory usage monitors.",
                "Compact metric dashboards embedded into status bars."
        },
        criticalRules = {
                "Finite values define the vertical range; NaN and infinities split the line into separate runs.",
                "A series with one value is rendered as one point; a constant series is centered vertically.",
                "PointMode controls which points are drawn and can be hit-tested; NONE disables point interaction.",
                "Hovering chooses the nearest point by horizontal coordinate and click emits SparkPointClickEvent."
        },
        examples = @AiDoc.Example(
                title = "Show a compact metric trend",
                code = "new Sparkline().values(List.of(60, 58, 59, 62, 60)).pointMode(Sparkline.PointMode.ALL);"),
        related = {"Chart", "WidgetBase"})
@WikiPage(
        title = "<key:sparkline.title>",
        slug = "sparkline",
        category = "<key:core.widgets.display.category>",
        summary = "<key:sparkline.summary>",
        order = 130)
@WikiSection(
        title = "<key:sparkline.section.overview.title>",
        markdown = "<key:sparkline.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:sparkline.example.basic.title>",
        code = "<key:sparkline.example.basic.code>",
        order = 10)
@XamlWidget("Sparkline")
public class Sparkline extends WidgetBase {
    /**
     * Style registry id used to resolve this widget's default sparkline style.
     */
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SPARKLINE;

    /**
     * Ширина по умолчанию в UI-пикселях.
     */
    public static final float DEFAULT_PREFERRED_WIDTH = 140.0f;
    /**
     * Высота по умолчанию в UI-пикселях.
     */
    public static final float DEFAULT_PREFERRED_HEIGHT = 34.0f;

    /**
     * Прибавка к радиусу маркера наведённой точки в UI-пикселях.
     */
    private static final float HOVER_RADIUS_BONUS = 1.25f;
    /**
     * Зазор между точкой и подсказкой в UI-пикселях.
     */
    private static final float TOOLTIP_GAP = 7.0f;
    /**
     * Зазор между маркером и подписью значения в UI-пикселях.
     */
    private static final float LABEL_GAP = 3.0f;
    /**
     * Доля исходной непрозрачности заливки у основания: {@code 0} — полное затухание до прозрачного.
     */
    private static final float FILL_FADE = 0.0f;
    /**
     * Доля непрозрачности направляющей относительно цвета наведённой точки.
     */
    private static final float GUIDE_ALPHA = 0.35f;

    /**
     * Значения ряда; нечисловые значения сохраняются и рисуются как разрыв.
     */
    final FloatArrayList values = new FloatArrayList();
    /**
     * Счётчик версий данных: растёт при каждой смене ряда и сбрасывает кэши.
     */
    int dataVersion;
    /**
     * Минимум конечных значений ряда ({@code 0} для пустого ряда).
     */
    float dataMin;
    /**
     * Максимум конечных значений ряда ({@code 1} для пустого ряда).
     */
    float dataMax = 1.0f;
    /**
     * Индекс минимального значения или {@code -1}.
     */
    int argMin = -1;
    /**
     * Индекс максимального значения или {@code -1}.
     */
    int argMax = -1;
    /**
     * Число конечных значений в ряду.
     */
    int validCount;

    /**
     * Значения стиля, разрешённые на текущем проходе отрисовки.
     */
    final PlotPalette palette = new PlotPalette();

    final OverridableColor lineColor = new OverridableColor(
            new MutableColor(0.25f, 0.85f, 1.0f, 1.0f), this::repaint);
    final OverridableColor fillColor = new OverridableColor(
            new MutableColor(0.25f, 0.85f, 1.0f, 0.16f), this::repaint);
    final OverridableColor pointColor = new OverridableColor(
            new MutableColor(0.90f, 0.96f, 1.0f, 0.96f), this::repaint);
    final OverridableColor hoveredPointColor = new OverridableColor(
            new MutableColor(1.0f, 0.84f, 0.34f, 1.0f), this::repaint);
    final OverridableColor labelColor = new OverridableColor(
            new MutableColor(0.92f, 0.96f, 1.0f, 1.0f), this::repaint);
    final OverridableColor tooltipBackground = new OverridableColor(
            new MutableColor(0.025f, 0.030f, 0.040f, 0.96f), this::repaint);
    final OverridableColor tooltipBorder = new OverridableColor(
            new MutableColor(0.25f, 0.78f, 1.0f, 0.92f), this::repaint);

    /**
     * Цвет направляющей: цвет наведённой точки с пониженной непрозрачностью; обновляется на месте.
     */
    private final MutableColor guideColor = new MutableColor();

    /**
     * Явная настройка заливки; {@code null} — брать из стиля.
     */
    private Boolean fillVisibleOverride;
    private PointMode pointMode = PointMode.EXTREMA;
    private boolean pointLabelsVisible;
    private PointLabelPlacement pointLabelPlacement = PointLabelPlacement.ABOVE;
    private SparkPointRenderer pointRenderer;
    private SparkPointLabelRenderer pointLabelRenderer;
    private SparkPointTooltipRenderer pointTooltipRenderer;
    private float preferredWidth;
    private float preferredHeight;

    /**
     * Индекс наведённой точки или {@code -1}.
     */
    int hoveredIndex = -1;

    // ---- кэш раскладки (пересчитывается при смене данных, размера или стиля) ----
    /**
     * Левый край области построения в координатах отрисовки.
     */
    float plotX;
    /**
     * Верхний край области построения.
     */
    float plotY;
    /**
     * Ширина области построения; не отрицательна.
     */
    float plotW;
    /**
     * Высота области построения; не отрицательна.
     */
    float plotH;
    /**
     * Значение, соответствующее нижнему краю области построения.
     */
    float scaleMin;
    /**
     * Значение, соответствующее верхнему краю области построения.
     */
    float scaleMax = 1.0f;
    /**
     * {@code true}, если кэш точек соответствует ключу ниже.
     */
    boolean layoutValid;
    private int keyVersion = -1;
    private float keyX;
    private float keyY;
    private float keyW;
    private float keyH;
    private float keyMin;
    private float keyMax;
    /**
     * Опорные точки линии; {@code null} для нечисловых значений.
     */
    private DrawPoint[] points = new DrawPoint[0];
    /**
     * Проекции точек на нижний край области построения (основание заливки).
     */
    private DrawPoint[] bases = new DrawPoint[0];
    /**
     * Непрерывные участки без разрывов: {@code runStart[i]..runEnd[i]} включительно.
     */
    private int[] runStart = new int[0];
    private int[] runEnd = new int[0];
    private int runCount;
    /**
     * Полилинии участков для {@link DrawScope#addPolyline}; переиспользуются между пересчётами.
     */
    private final List<ObjectArrayList<DrawPoint>> runLines = new ObjectArrayList<>();
    /**
     * Готовая сетка заливки под линией; строится при смене геометрии или цвета и переиспользуется между кадрами.
     * Команда рисования хранит ссылку на сетку и не изменяет её.
     */
    private DrawMesh fillMesh;
    /**
     * Цвет, для которого построена {@link #fillMesh}; сравнение по компонентам.
     */
    private float fillKeyR = Float.NaN;
    private float fillKeyG;
    private float fillKeyB;
    private float fillKeyA;

    // ---- кэш текстов ----
    private RichText[] labelTexts = new RichText[0];
    private int labelVersion = -1;
    private float labelSize = -1.0f;
    private RichText tooltipText;
    private int tooltipIndex = -1;
    private int tooltipVersion = -1;
    private float tooltipSize = -1.0f;

    @AiDoc(summary = "Creates an empty compact trend chart using the default preferred dimensions.")
    @WikiMethod(title = "Create a sparkline", summary = "Creates an empty sparkline with its default preferred size.", order = 10)
    public Sparkline() {
        this(DEFAULT_PREFERRED_WIDTH, DEFAULT_PREFERRED_HEIGHT);
    }

    /**
     * Создаёт sparkline с указанным предпочтительным размером.
     *
     * @param preferredWidth  предпочтительная ширина в UI-пикселях; неположительное или нечисловое значение заменяется шириной по умолчанию
     * @param preferredHeight предпочтительная высота в UI-пикселях; неположительное или нечисловое значение заменяется высотой по умолчанию
     */
    protected Sparkline(float preferredWidth, float preferredHeight) {
        this.preferredWidth = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        this.preferredHeight = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
    }

    /**
     * Возвращает стандартное оформление компонента; уступает теме и локальным стилям.
     *
     * @return {@link WidgetStyles.Plot#SPARKLINE}
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Plot.SPARKLINE;
    }

    private void repaint() {
        invalidate(InvalidationFlags.VISUAL);
    }

    // ------------------------------------------------------------------ данные

    @WikiMethod(
            title = "<key:sparkline.method.values.title>",
            summary = "<key:sparkline.method.values.summary>")
    @AiDoc(summary = "Replaces the sparkline series from Number values; null clears the series and null entries become zero.", section = "Data", sectionOrder = 10, order = 10)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline values(@WikiParameter(name = "values", description = "Values in series order; null clears the series and null entries are treated as zero.") List<? extends Number> values) {
        this.values.clear();
        if (values != null) {
            for (Number value : values) {
                this.values.add(value == null ? 0.0f : value.floatValue());
            }
        }
        onValuesChanged();
        return this;
    }

    /**
     * Заменяет ряд значений без упаковки в {@link Number}.
     *
     * @param data значения; {@code null} очищает ряд; нечисловые значения рисуются как разрыв линии
     * @return этот виджет
     */
    @AiDoc(summary = "Replaces the sparkline series using primitive floats without boxing.", section = "Data", sectionOrder = 10, order = 20)
    @WikiMethod(title = "Set sparkline values from floats", summary = "Replaces the series without boxing numeric inputs.", section = "Data", sectionOrder = 10, order = 20)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline values(@WikiParameter(name = "data", description = "Values in series order; null clears the series and non-finite values create gaps.") float... data) {
        this.values.clear();
        if (data != null) {
            for (float value : data) this.values.add(value);
        }
        onValuesChanged();
        return this;
    }

    /**
     * Возвращает значения ряда.
     *
     * @return неизменяемое представление значений
     */
    @WikiMethod(title = "Get sparkline values", summary = "Returns the current series as an immutable view.", section = "Data", sectionOrder = 10, order = 30)
    @WikiReturn("An unmodifiable view of the current float values.")
    public List<Float> values() {
        return Collections.unmodifiableList(values);
    }

    /**
     * Пересчитывает статистику ряда и сбрасывает кэши после смены данных.
     *
     * <p>Должен вызываться после любой записи в {@link #values}.</p>
     */
    final void onValuesChanged() {
        hoveredIndex = -1;
        dataVersion++;
        layoutValid = false;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        int minIndex = -1;
        int maxIndex = -1;
        int valid = 0;
        for (int i = 0; i < values.size(); i++) {
            float value = values.getFloat(i);
            if (!Float.isFinite(value)) continue;
            valid++;
            if (value < min) {
                min = value;
                minIndex = i;
            }
            if (value > max) {
                max = value;
                maxIndex = i;
            }
        }
        validCount = valid;
        argMin = minIndex;
        argMax = maxIndex;
        dataMin = valid == 0 ? 0.0f : min;
        dataMax = valid == 0 ? 1.0f : max;
        invalidate(InvalidationFlags.VISUAL);
    }

    // ------------------------------------------------------------------ цвета и настройки

    @WikiMethod(
            title = "<key:sparkline.method.line_color.title>",
            summary = "<key:sparkline.method.line_color.summary>")
    @WikiReturn("The mutable RGBA line color; changing it overrides the style value.")
    public MutableColor lineColor() {
        return lineColor.value();
    }

    @WikiMethod(
            title = "<key:sparkline.method.fill_color.title>",
            summary = "<key:sparkline.method.fill_color.summary>")
    @WikiReturn("The mutable RGBA fill color used below the line.")
    public MutableColor fillColor() {
        return fillColor.value();
    }

    @WikiMethod(
            title = "<key:sparkline.method.point_color.title>",
            summary = "<key:sparkline.method.point_color.summary>")
    @WikiReturn("The mutable RGBA color used for data-point markers.")
    public MutableColor pointColor() {
        return pointColor.value();
    }

    @WikiMethod(title = "Get hovered point color", summary = "Returns the mutable color used for the hovered point.", section = "Appearance", sectionOrder = 20, order = 40)
    @WikiReturn("The mutable RGBA hovered-point color.")
    public MutableColor hoveredPointColor() {
        return hoveredPointColor.value();
    }

    @WikiMethod(title = "Get point label color", summary = "Returns the mutable color used for point value labels.", section = "Appearance", sectionOrder = 20, order = 50)
    @WikiReturn("The mutable RGBA point-label color.")
    public MutableColor labelColor() {
        return labelColor.value();
    }

    @WikiMethod(title = "Get tooltip background color", summary = "Returns the mutable sparkline tooltip background color.", section = "Appearance", sectionOrder = 20, order = 60)
    @WikiReturn("The mutable RGBA tooltip background color.")
    public MutableColor tooltipBackground() {
        return tooltipBackground.value();
    }

    @WikiMethod(title = "Get tooltip border color", summary = "Returns the mutable sparkline tooltip border color.", section = "Appearance", sectionOrder = 20, order = 70)
    @WikiReturn("The mutable RGBA tooltip border color.")
    public MutableColor tooltipBorder() {
        return tooltipBorder.value();
    }

    /**
     * Включает или выключает заливку под линией; явное значение приоритетнее стиля {@code plot.fill.visible}.
     *
     * @param fillVisible {@code true} — рисовать заливку
     * @return этот виджет
     */
    @XamlProperty(value = "fillVisible", category = "Appearance", defaultValue = "true", description = "Whether the sparkline area fill is rendered.")
    @WikiMethod(title = "Show sparkline fill", summary = "Enables or disables the area fill beneath the line.", section = "Appearance", sectionOrder = 20, order = 80)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline fillVisible(@WikiParameter(name = "fillVisible", description = "True to render the fill; this explicit value overrides the style setting.") boolean fillVisible) {
        if (fillVisibleOverride != null && fillVisibleOverride == fillVisible) return this;
        fillVisibleOverride = fillVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XamlProperty(value = "pointMode", category = "Appearance", defaultValue = "extrema", description = "Which sparkline points are rendered and hit-tested.")
    @AiDoc(summary = "Controls marker visibility and point hit-testing; null disables point rendering and interaction.", section = "Appearance", sectionOrder = 20, order = 10)
    @WikiMethod(title = "Set point mode", summary = "Selects which data points are rendered and interactive.", section = "Appearance", sectionOrder = 20, order = 90)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline pointMode(@WikiParameter(name = "pointMode", description = "Point mode; null is treated as NONE.") PointMode pointMode) {
        PointMode normalized = pointMode == null ? PointMode.NONE : pointMode;
        if (this.pointMode == normalized) return this;
        this.pointMode = normalized;
        hoveredIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает режим показа точек.
     *
     * @return режим показа точек
     */
    @WikiMethod(title = "Get point mode", summary = "Returns the current marker and hit-testing mode.", section = "Appearance", sectionOrder = 20, order = 100)
    @WikiReturn("The active point mode.")
    public PointMode pointMode() {
        return pointMode;
    }

    @XamlProperty(value = "pointLabelsVisible", category = "Appearance", defaultValue = "false", description = "Whether point value labels are rendered.")
    @WikiMethod(title = "Show point labels", summary = "Enables or disables numeric labels beside data points.", section = "Appearance", sectionOrder = 20, order = 110)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline pointLabelsVisible(@WikiParameter(name = "pointLabelsVisible", description = "True to render point value labels.") boolean pointLabelsVisible) {
        if (this.pointLabelsVisible == pointLabelsVisible) return this;
        this.pointLabelsVisible = pointLabelsVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XamlProperty(value = "pointLabelPlacement", category = "Appearance", defaultValue = "above", description = "Placement of point value labels.")
    @WikiMethod(title = "Set point label placement", summary = "Sets where point value labels are placed relative to markers.", section = "Appearance", sectionOrder = 20, order = 120)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline pointLabelPlacement(@WikiParameter(name = "pointLabelPlacement", description = "Placement mode; null resets to ABOVE.") PointLabelPlacement pointLabelPlacement) {
        this.pointLabelPlacement = pointLabelPlacement == null ? PointLabelPlacement.ABOVE : pointLabelPlacement;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Customize point rendering", summary = "Installs or clears a custom data-point renderer.", section = "Custom Rendering", sectionOrder = 30, order = 10)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline pointRenderer(@WikiParameter(name = "pointRenderer", description = "Custom renderer, or null to use built-in point rendering.") SparkPointRenderer pointRenderer) {
        this.pointRenderer = pointRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Customize point labels", summary = "Installs or clears a custom point-label renderer.", section = "Custom Rendering", sectionOrder = 30, order = 20)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline pointLabelRenderer(@WikiParameter(name = "pointLabelRenderer", description = "Custom renderer, or null to use built-in label rendering.") SparkPointLabelRenderer pointLabelRenderer) {
        this.pointLabelRenderer = pointLabelRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Customize point tooltips", summary = "Installs or clears a custom tooltip renderer.", section = "Custom Rendering", sectionOrder = 30, order = 30)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline pointTooltipRenderer(@WikiParameter(name = "pointTooltipRenderer", description = "Custom renderer, or null to use built-in tooltip rendering.") SparkPointTooltipRenderer pointTooltipRenderer) {
        this.pointTooltipRenderer = pointTooltipRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Get preferred sparkline width", summary = "Returns the preferred width in UI pixels.", section = "Layout", sectionOrder = 40, order = 10)
    @WikiReturn("Preferred width in UI pixels.")
    public float preferredWidth() {
        return preferredWidth;
    }

    @XamlProperty(value = "preferredWidth", category = "Layout", defaultValue = "140", description = "Intrinsic sparkline width before layout constraints are applied.")
    @WikiMethod(title = "Set preferred sparkline width", summary = "Sets the preferred width in UI pixels.", section = "Layout", sectionOrder = 40, order = 20)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline preferredWidth(@WikiParameter(name = "preferredWidth", description = "Preferred width in UI pixels; non-positive or non-finite values use the default.") float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Get preferred sparkline height", summary = "Returns the preferred height in UI pixels.", section = "Layout", sectionOrder = 40, order = 30)
    @WikiReturn("Preferred height in UI pixels.")
    public float preferredHeight() {
        return preferredHeight;
    }

    @XamlProperty(value = "preferredHeight", category = "Layout", defaultValue = "34", description = "Intrinsic sparkline height before layout constraints are applied.")
    @WikiMethod(title = "Set preferred sparkline height", summary = "Sets the preferred height in UI pixels.", section = "Layout", sectionOrder = 40, order = 40)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline preferredHeight(@WikiParameter(name = "preferredHeight", description = "Preferred height in UI pixels; non-positive or non-finite values use the default.") float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(title = "Set preferred sparkline size", summary = "Sets preferred width and height in UI pixels.", section = "Layout", sectionOrder = 40, order = 50)
    @WikiReturn("This sparkline for fluent configuration.")
    public Sparkline preferredSize(
            @WikiParameter(name = "width", description = "Preferred width in UI pixels.") float width,
            @WikiParameter(name = "height", description = "Preferred height in UI pixels.") float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    @AiDoc(summary = "Subscribes to clicks on interactive sparkline points.", section = "Interaction", sectionOrder = 50, order = 10)
    @WikiMethod(title = "Listen for point clicks", summary = "Registers a listener for clicks on data points.", section = "Interaction", sectionOrder = 50, order = 10)
    @WikiReturn("A subscription handle that can be cancelled.")
    public EventSubscription onPointClick(@WikiParameter(name = "listener", description = "Listener receiving the clicked point event.") EventListener<? super SparkPointClickEvent> listener) {
        return on(SparkPointClickEvent.TYPE, listener);
    }

    // ------------------------------------------------------------------ раскладка

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    /**
     * Читает значения стиля в {@link #palette}.
     *
     * <p>Приоритет каждого значения: экземпляр ({@link OverridableColor}), локальный стиль, тема, стандартное
     * оформление компонента. Наследник переопределяет метод, вызывает {@code super} и добавляет свои свойства.</p>
     */
    void resolveStyle() {
        PlotPalette p = palette;
        p.backgroundVisible = styleValue(StyleKeys.BACKGROUND_VISIBLE, false);
        p.borderVisible = styleValue(StyleKeys.BORDER_VISIBLE, false);
        p.background = styleValue(StyleKeys.BACKGROUND_COLOR, (ColorView) null);
        p.border = styleValue(StyleKeys.BORDER_COLOR, (ColorView) null);
        p.borderWidth = Math.max(0.0f, styleValue(StyleKeys.BORDER_WIDTH, 1.0f));
        p.radius = Math.max(0.0f, styleValue(StyleKeys.RADIUS, 0.0f));
        p.textSize = Math.max(4.0f, styleValue(StyleKeys.TEXT_SIZE, 9.0f));
        p.text = labelColor.resolve(styleValue(StyleKeys.TEXT_COLOR, (ColorView) null));
        p.line = lineColor.resolve(styleValue(StyleKeys.PLOT_LINE_COLOR, (ColorView) null));
        p.lineWidth = Math.max(0.5f, styleValue(StyleKeys.PLOT_LINE_WIDTH, 1.5f));
        p.fill = fillColor.resolve(styleValue(StyleKeys.PLOT_FILL_COLOR, (ColorView) null));
        p.fillVisible = fillVisibleOverride != null
                ? fillVisibleOverride
                : styleValue(StyleKeys.PLOT_FILL_VISIBLE, true);
        p.point = pointColor.resolve(styleValue(StyleKeys.PLOT_POINT_COLOR, (ColorView) null));
        p.pointHover = hoveredPointColor.resolve(styleValue(StyleKeys.PLOT_POINT_HOVER_COLOR, (ColorView) null));
        p.pointRadius = Math.max(0.5f, styleValue(StyleKeys.PLOT_POINT_RADIUS, 3.0f));
        p.tooltipBackground = tooltipBackground.resolve(
                styleValue(StyleKeys.PLOT_TOOLTIP_BACKGROUND_COLOR, (ColorView) null));
        p.tooltipBorder = tooltipBorder.resolve(styleValue(StyleKeys.PLOT_TOOLTIP_BORDER_COLOR, (ColorView) null));
        // Цвет подписей, заданный на экземпляре, распространяется и на текст подсказки (как и раньше).
        p.tooltipText = labelColor.resolve(styleValue(StyleKeys.PLOT_TOOLTIP_TEXT_COLOR, (ColorView) null));
    }

    /**
     * Вычисляет область построения и шкалу значений.
     *
     * <p>Область — границы виджета, сжатые на отступ, который не даёт маркерам и толстой линии выходить за
     * край:
     * <pre>
     *   pad    = max(pointRadius + 2, lineWidth)
     *   plot   = bounds shrunk by pad on every side
     *   range  = dataMax - dataMin;  если range около 0 — шкала расширяется на 1 в обе стороны (ряд по центру)
     * </pre>
     * Наследник переопределяет метод, чтобы зарезервировать место под оси и подписи.
     *
     * @param context контекст отрисовки (нужен наследникам для замера текста)
     */
    void updatePlot(RenderContext context) {
        RectView bounds = layoutBounds();
        float pad = Math.max(palette.pointRadius + 2.0f, palette.lineWidth);
        plotX = bounds.x() + pad;
        plotY = bounds.y() + pad;
        plotW = Math.max(0.0f, bounds.width() - pad * 2.0f);
        plotH = Math.max(0.0f, bounds.height() - pad * 2.0f);
        float min = dataMin;
        float max = dataMax;
        if (max - min < 1.0e-6f) {
            min -= 1.0f;
            max += 1.0f;
        }
        scaleMin = min;
        scaleMax = max;
    }

    /**
     * Возвращает X точки с индексом {@code index}.
     *
     * <p>Формула для {@code n} значений: {@code x = plotX + plotW * i / (n - 1)}; единственная точка стоит по
     * центру области.</p>
     *
     * @param index индекс значения, {@code 0 <= index < values.size()}
     * @return координата X в координатах отрисовки
     */
    float pointX(int index) {
        int n = values.size();
        if (n <= 1) return plotX + plotW * 0.5f;
        return plotX + plotW * index / (n - 1.0f);
    }

    /**
     * Возвращает Y значения на текущей шкале.
     *
     * <p>Формула: {@code y = plotY + plotH - (value - scaleMin) / (scaleMax - scaleMin) * plotH}.</p>
     *
     * @param value значение ряда
     * @return координата Y в координатах отрисовки (ось Y направлена вниз)
     */
    final float valueY(float value) {
        float range = Math.max(1.0e-6f, scaleMax - scaleMin);
        return plotY + plotH - (value - scaleMin) / range * plotH;
    }

    /**
     * Приводит кэш раскладки в соответствие с данными, размером и стилем.
     *
     * <p>Вызывается один раз за кадр после {@link #resolveStyle()}. Если ничего не менялось, метод только сравнивает
     * несколько чисел и не выделяет память.</p>
     *
     * @param context контекст отрисовки
     */
    final void prepareLayout(RenderContext context) {
        updatePlot(context);
        if (layoutValid
                && keyVersion == dataVersion
                && keyX == plotX && keyY == plotY && keyW == plotW && keyH == plotH
                && keyMin == scaleMin && keyMax == scaleMax) {
            return;
        }
        rebuildPoints();
        fillMesh = null;
        keyVersion = dataVersion;
        keyX = plotX;
        keyY = plotY;
        keyW = plotW;
        keyH = plotH;
        keyMin = scaleMin;
        keyMax = scaleMax;
        layoutValid = true;
    }

    private void rebuildPoints() {
        int n = values.size();
        if (points.length < n) {
            points = new DrawPoint[n];
            bases = new DrawPoint[n];
            runStart = new int[n];
            runEnd = new int[n];
        }
        float baseline = plotY + plotH;
        for (int i = 0; i < n; i++) {
            float value = values.getFloat(i);
            if (!Float.isFinite(value)) {
                points[i] = null;
                bases[i] = null;
                continue;
            }
            float x = pointX(i);
            points[i] = new DrawPoint(x, valueY(value));
            bases[i] = new DrawPoint(x, baseline);
        }
        for (int i = n; i < points.length; i++) {
            points[i] = null;
            bases[i] = null;
        }

        runCount = 0;
        int start = -1;
        for (int i = 0; i <= n; i++) {
            boolean valid = i < n && points[i] != null;
            if (valid && start < 0) {
                start = i;
            } else if (!valid && start >= 0) {
                runStart[runCount] = start;
                runEnd[runCount] = i - 1;
                runCount++;
                start = -1;
            }
        }
        while (runLines.size() < runCount) runLines.add(new ObjectArrayList<>());
        for (int r = 0; r < runCount; r++) {
            ObjectArrayList<DrawPoint> line = runLines.get(r);
            line.clear();
            for (int i = runStart[r]; i <= runEnd[r]; i++) line.add(points[i]);
        }
    }

    // ------------------------------------------------------------------ отрисовка

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        RectView bounds = layoutBounds();
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return;
        DrawScope draw = new DrawScope(context, transform(), bounds);
        if (renderCustomVisual(draw)) return;
        resolveStyle();
        PlotPalette p = palette;
        PlotSupport.drawSurface(draw, bounds,
                p.backgroundVisible ? p.background : null,
                p.borderVisible ? p.border : null,
                p.borderWidth, p.radius);
        prepareLayout(context);
        renderPlot(draw);
    }

    /**
     * Рисует содержимое области построения после подложки; наследник добавляет оси и сетку.
     *
     * @param draw область рисования
     */
    void renderPlot(DrawScope draw) {
        renderSparklineVisual(draw);
    }

    /**
     * Рисует ряд: заливку, линию, направляющую, маркеры, подписи и подсказку.
     *
     * <p>Раскладка должна быть подготовлена ({@link #prepareLayout}); если нет, она готовится здесь.</p>
     *
     * @param draw область рисования
     */
    protected void renderSparklineVisual(DrawScope draw) {
        if (!layoutValid) {
            resolveStyle();
            prepareLayout(draw.context());
        }
        if (validCount == 0) return;
        PlotPalette p = palette;

        if (p.fillVisible) {
            DrawMesh mesh = fillMesh(p.fill);
            if (mesh != null) draw.addMesh(mesh, null);
        }
        for (int r = 0; r < runCount; r++) {
            if (runEnd[r] > runStart[r]) {
                draw.addPolyline(runLines.get(r), p.line, false, p.lineWidth);
            } else {
                // Одинокая точка между разрывами не образует линию, рисуем её кружком толщины линии.
                DrawPoint lone = points[runStart[r]];
                draw.addCircleFilled(lone.x(), lone.y(), Math.max(1.5f, p.lineWidth), p.line, 12);
            }
        }
        if (hoveredIndex >= 0 && hoveredIndex < values.size() && points[hoveredIndex] != null) {
            ColorView hover = p.pointHover;
            guideColor.set(hover.r(), hover.g(), hover.b(), hover.a() * GUIDE_ALPHA);
            float x = points[hoveredIndex].x();
            draw.addLine(x, plotY, x, plotY + plotH, guideColor, 1.0f);
        }
        renderMarkers(draw);
        if (hoveredIndex >= 0 && hoveredIndex < values.size() && points[hoveredIndex] != null) {
            renderTooltip(draw, hoveredIndex);
        }
    }

    /**
     * Возвращает сетку заливки под линией, строя её при необходимости.
     *
     * <p>Каждый сегмент {@code [i, i + 1]} — трапеция между линией и основанием, разбитая на два треугольника.
     * Цвет вершин задаёт вертикальное затухание:
     * <pre>
     *   вершина на линии:      цвет заливки (альфа = a)
     *   вершина на основании:  тот же цвет, альфа = a * FILL_FADE
     * </pre>
     * Сетка одна на весь ряд, поэтому кадр без изменений не создаёт ни вершин, ни команд на сегмент: раньше
     * каждый сегмент был отдельным {@code addQuadFilled} с шестью вершинами и копиями цвета.
     *
     * @param color цвет заливки из стиля или экземпляра
     * @return сетка или {@code null}, если нечего заливать
     */
    private DrawMesh fillMesh(ColorView color) {
        if (fillMesh != null && fillKeyR == color.r() && fillKeyG == color.g()
                && fillKeyB == color.b() && fillKeyA == color.a()) {
            return fillMesh;
        }
        MutableColor top = new MutableColor(color.r(), color.g(), color.b(), color.a());
        MutableColor bottom = new MutableColor(color.r(), color.g(), color.b(), color.a() * FILL_FADE);
        ObjectArrayList<DrawVertex> vertices = new ObjectArrayList<>();
        for (int r = 0; r < runCount; r++) {
            for (int i = runStart[r]; i < runEnd[r]; i++) {
                DrawPoint left = points[i];
                DrawPoint right = points[i + 1];
                DrawPoint leftBase = bases[i];
                DrawPoint rightBase = bases[i + 1];
                vertices.add(new DrawVertex(left.x(), left.y(), top));
                vertices.add(new DrawVertex(right.x(), right.y(), top));
                vertices.add(new DrawVertex(rightBase.x(), rightBase.y(), bottom));
                vertices.add(new DrawVertex(left.x(), left.y(), top));
                vertices.add(new DrawVertex(rightBase.x(), rightBase.y(), bottom));
                vertices.add(new DrawVertex(leftBase.x(), leftBase.y(), bottom));
            }
        }
        fillMesh = vertices.isEmpty() ? null : DrawMesh.owned(vertices);
        fillKeyR = color.r();
        fillKeyG = color.g();
        fillKeyB = color.b();
        fillKeyA = color.a();
        return fillMesh;
    }

    private void renderMarkers(DrawScope draw) {
        PlotPalette p = palette;
        int n = values.size();
        for (int i = 0; i < n; i++) {
            if (points[i] == null || !markerVisible(i)) continue;
            if (pointRenderer != null) {
                pointRenderer.render(draw, sparkPoint(i));
            } else {
                boolean hovered = i == hoveredIndex;
                float radius = hovered ? p.pointRadius + HOVER_RADIUS_BONUS : p.pointRadius;
                draw.addCircleFilled(points[i].x(), points[i].y(), radius, hovered ? p.pointHover : p.point, 16);
                // Тёмная окантовка отделяет маркер от линии независимо от цвета фона.
                guideColor.set(0.0f, 0.0f, 0.0f, hovered ? 0.65f : 0.35f);
                draw.addCircle(points[i].x(), points[i].y(), radius + 1.0f, guideColor, 16, 1.0f);
            }
            if (pointLabelsVisible && pointLabelPlacement != PointLabelPlacement.NONE) {
                renderPointLabel(draw, i);
            }
        }
    }

    private void renderPointLabel(DrawScope draw, int index) {
        if (pointLabelRenderer != null) {
            pointLabelRenderer.render(draw, sparkPoint(index));
            return;
        }
        PlotPalette p = palette;
        RichText text = labelText(index);
        float width = PlotSupport.textWidth(draw.context(), text);
        float height = PlotSupport.textHeight(draw.context(), text);
        RectView bounds = layoutBounds();
        DrawPoint point = points[index];
        float gap = p.pointRadius + LABEL_GAP;
        float y = switch (pointLabelPlacement) {
            case CENTER -> point.y() - height * 0.5f;
            case BELOW -> point.y() + gap;
            case ABOVE, NONE -> point.y() - gap - height;
        };
        // Над точкой нет места — переносим подпись вниз, чтобы текст не обрезался краем виджета.
        if (pointLabelPlacement == PointLabelPlacement.ABOVE && y < bounds.y()) y = point.y() + gap;
        float x = PlotSupport.clamp(point.x() - width * 0.5f, bounds.x() + 1.0f, bounds.x() + bounds.width() - width - 1.0f);
        PlotSupport.drawText(draw, text, x, y, width, height, p.text, Alignment.CENTER, Alignment.CENTER);
    }

    void renderTooltip(DrawScope draw, int index) {
        if (pointTooltipRenderer != null) {
            pointTooltipRenderer.render(draw, sparkPoint(index));
            return;
        }
        PlotPalette p = palette;
        if (tooltipText == null || tooltipIndex != index || tooltipVersion != dataVersion
                || tooltipSize != p.textSize) {
            tooltipText = PlotSupport.text(tooltipLabel(index), p.textSize);
            tooltipIndex = index;
            tooltipVersion = dataVersion;
            tooltipSize = p.textSize;
        }
        DrawPoint point = points[index];
        PlotSupport.drawTooltip(draw, tooltipText, point.x(), point.y(), TOOLTIP_GAP + p.pointRadius,
                layoutBounds(), p.tooltipBackground, p.tooltipBorder, p.tooltipText);
    }

    /**
     * Возвращает текст подсказки точки; наследник подставляет, например, название категории.
     *
     * @param index индекс точки
     * @return текст подсказки
     */
    String tooltipLabel(int index) {
        return "#" + index + ": " + PlotSupport.formatValue(values.getFloat(index));
    }

    /**
     * Возвращает кэшированный текст значения ряда.
     *
     * @param index индекс значения
     * @return rich-text с форматированным значением и размером шрифта подписей
     */
    final RichText labelText(int index) {
        int n = values.size();
        if (labelVersion != dataVersion || labelSize != palette.textSize || labelTexts.length < n) {
            if (labelTexts.length < n) labelTexts = new RichText[n];
            else java.util.Arrays.fill(labelTexts, null);
            labelVersion = dataVersion;
            labelSize = palette.textSize;
        }
        RichText text = labelTexts[index];
        if (text == null) {
            text = PlotSupport.text(PlotSupport.formatValue(values.getFloat(index)), palette.textSize);
            labelTexts[index] = text;
        }
        return text;
    }

    /**
     * Сообщает, нужно ли рисовать маркер точки.
     *
     * <p>Правила: режим {@code ALL} — все; {@code EXTREMA} — локальные экстремумы, глобальные минимум и максимум
     * (иначе монотонный ряд остался бы без маркеров) и ряды из одной-двух точек; наведённая точка видна всегда;
     * {@code NONE} — никогда.</p>
     *
     * @param index индекс значения
     * @return {@code true}, если маркер нужно нарисовать
     */
    final boolean markerVisible(int index) {
        if (pointMode == PointMode.NONE || index < 0 || index >= values.size() || points[index] == null) return false;
        if (index == hoveredIndex || pointMode == PointMode.ALL) return true;
        return values.size() <= 2 || index == argMin || index == argMax || isLocalExtremum(index);
    }

    private boolean isLocalExtremum(int index) {
        if (index <= 0 || index >= values.size() - 1) return false;
        float previous = values.getFloat(index - 1);
        float current = values.getFloat(index);
        float next = values.getFloat(index + 1);
        if (!Float.isFinite(previous) || !Float.isFinite(next)) return false;
        return (current >= previous && current >= next && (current > previous || current > next))
                || (current <= previous && current <= next && (current < previous || current < next));
    }

    // ------------------------------------------------------------------ события

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled() || visibility() != Visibility.VISIBLE) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;
        if (!interactive()) return;

        if (event instanceof PointerMovedEvent pointer) {
            int next = hitIndex(pointer.rootX(), pointer.rootY());
            if (next != hoveredIndex) {
                hoveredIndex = next;
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerExitedEvent) {
            if (hoveredIndex != -1) {
                hoveredIndex = -1;
                invalidate(InvalidationFlags.VISUAL);
            }
        } else if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            int hit = hitIndex(pointer.rootX(), pointer.rootY());
            if (hit >= 0) {
                activate(hit);
                event.cancel();
            }
        }
    }

    /**
     * Сообщает, реагирует ли виджет на указатель.
     *
     * @return {@code true}, если режим точек не {@code NONE}
     */
    boolean interactive() {
        return pointMode != PointMode.NONE;
    }

    /**
     * Находит элемент под указателем.
     *
     * <p>Ближайшая по горизонтали точка среди конечных значений: наведение работает по всей высоте виджета, а не
     * только в радиусе маркера, поэтому до точки не нужно «целиться».</p>
     *
     * @param rootX X указателя в координатах отрисовки
     * @param rootY Y указателя в координатах отрисовки
     * @return индекс точки или {@code -1}
     */
    int hitIndex(float rootX, float rootY) {
        RectView bounds = layoutBounds();
        if (!layoutValid || values.isEmpty() || validCount == 0) return -1;
        if (rootX < bounds.x() || rootX > bounds.x() + bounds.width()
                || rootY < bounds.y() || rootY > bounds.y() + bounds.height()) {
            return -1;
        }
        return nearestValid(guessIndex(rootX), rootX);
    }

    /**
     * Оценивает индекс по X, не глядя на разрывы.
     *
     * <p>Обратная к {@link #pointX}: {@code i = round((x - plotX) / plotW * (n - 1))}.</p>
     *
     * @param rootX X указателя
     * @return индекс в диапазоне {@code [0, n - 1]}
     */
    int guessIndex(float rootX) {
        int n = values.size();
        if (n <= 1 || plotW <= 0.0f) return 0;
        int guess = Math.round((rootX - plotX) / plotW * (n - 1));
        return Math.max(0, Math.min(n - 1, guess));
    }

    /**
     * Находит ближайшую по X точку с конечным значением.
     *
     * <p>Индексы упорядочены по X, поэтому достаточно найти ближайшие конечные точки слева и справа от оценки
     * {@code guess} и выбрать ту, что ближе к указателю.</p>
     *
     * @param guess оценка индекса
     * @param rootX X указателя
     * @return индекс точки или {@code -1}, если конечных значений нет
     */
    private int nearestValid(int guess, float rootX) {
        int n = values.size();
        int left = guess;
        while (left >= 0 && points[left] == null) left--;
        int right = guess;
        while (right < n && points[right] == null) right++;
        if (left < 0 && right >= n) return -1;
        if (left < 0) return right;
        if (right >= n) return left;
        return Math.abs(points[left].x() - rootX) <= Math.abs(points[right].x() - rootX) ? left : right;
    }

    /**
     * Обрабатывает нажатие на элемент.
     *
     * @param index индекс элемента, полученный из {@link #hitIndex}
     */
    void activate(int index) {
        SparkPoint point = sparkPoint(index, true);
        if (point != null) clickPoint(point);
    }

    private SparkPointClickEvent clickPoint(SparkPoint point) {
        SparkPointClickEvent event = new SparkPointClickEvent(this, point.index(), point.value(), point.x(), point.y());
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    private SparkPoint sparkPoint(int index) {
        return sparkPoint(index, index == hoveredIndex);
    }

    private SparkPoint sparkPoint(int index, boolean hovered) {
        if (index < 0 || index >= values.size() || points[index] == null) return null;
        return new SparkPoint(index, values.getFloat(index), points[index].x(), points[index].y(),
                markerVisible(index), hovered);
    }

    /**
     * Строит список точек ряда для пользовательских renderer'ов и наследников.
     *
     * <p>Выделяет память на каждый вызов; штатная отрисовка этот метод не использует.</p>
     *
     * @return точки с конечными значениями в порядке индексов; пустой список, пока раскладка не вычислена
     */
    protected List<SparkPoint> computePoints() {
        if (!layoutValid) return List.of();
        List<SparkPoint> result = new ObjectArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            SparkPoint point = sparkPoint(i);
            if (point != null) result.add(point);
        }
        return result;
    }

    static String formatValue(float value) {
        return PlotSupport.formatValue(value);
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    public enum PointMode {
        NONE,
        EXTREMA,
        ALL
    }

    public enum PointLabelPlacement {
        NONE,
        CENTER,
        ABOVE,
        BELOW
    }

    public record SparkPoint(int index, float value, float x, float y, boolean renderable, boolean hovered) {
    }

    @FunctionalInterface
    public interface SparkPointRenderer {
        void render(DrawScope draw, SparkPoint point);
    }

    @FunctionalInterface
    public interface SparkPointLabelRenderer {
        void render(DrawScope draw, SparkPoint point);
    }

    @FunctionalInterface
    public interface SparkPointTooltipRenderer {
        void render(DrawScope draw, SparkPoint point);
    }
}
