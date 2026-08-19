package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3SplitAdapter;
import dev.sixik.unigui.widgets.core.Orientation;

/**
 * Контейнер из двух областей с перетаскиваемым разделителем.
 *
 * <p>{@code SplitPanel} держит два пользовательских слота: {@link #first()} и
 * {@link #second()}. Между ними всегда находится внутренний {@link Splitter},
 * который получает pointer capture во время drag'а и меняет
 * {@link #splitRatio(float)}.</p>
 *
 * <p>При горизонтальной ориентации первая область слева, вторая справа. При
 * вертикальной ориентации первая область сверху, вторая снизу. Минимальные
 * размеры слотов задаются через {@link #minFirstSize(float)} и
 * {@link #minSecondSize(float)}.</p>
 *
 * <pre>{@code
 * SplitPanel split = new SplitPanel(sidebar, editor)
 *         .orientation(Orientation.HORIZONTAL)
 *         .splitRatio(0.28f)
 *         .minFirstSize(96.0f)
 *         .minSecondSize(160.0f);
 * }</pre>
 */
@XmlWidgetName("SplitPanel")
public class SplitPanel extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SPLIT_PANEL;

    private final Splitter splitter = new Splitter(this);
    private Widget first;
    private Widget second;
    private Orientation orientation = Orientation.HORIZONTAL;
    private float splitRatio = 0.5f;
    private float splitterThickness = 5.0f;
    private float minFirstSize = 32.0f;
    private float minSecondSize = 32.0f;
    private float dragStartRoot;
    private float dragStartRatio;

    /**
     * Создаёт пустую split-панель с внутренним splitter'ом.
     */
    public SplitPanel() {
        super.addChild(splitter);
    }

    /**
     * Создаёт split-панель с двумя начальными областями.
     *
     * @param first виджет первой области
     * @param second виджет второй области
     */
    public SplitPanel(Widget first, Widget second) {
        this();
        first(first);
        second(second);
    }

    /**
     * Возвращает виджет первой области.
     *
     * @return первый слот или {@code null}
     */
    public Widget first() {
        return first;
    }

    /**
     * Заменяет виджет первой области.
     *
     * @param first новый первый виджет или {@code null}
     * @return эта split-панель для fluent-настройки
     */
    public SplitPanel first(Widget first) {
        if (this.first == first || first == splitter) return this;
        if (this.first != null) {
            super.removeChild(this.first);
        }
        this.first = first;
        if (first != null) {
            super.addChild(first);
        }
        ensureSplitterOnTop();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает виджет второй области.
     *
     * @return второй слот или {@code null}
     */
    public Widget second() {
        return second;
    }

    /**
     * Заменяет виджет второй области.
     *
     * @param second новый второй виджет или {@code null}
     * @return эта split-панель для fluent-настройки
     */
    public SplitPanel second(Widget second) {
        if (this.second == second || second == splitter) return this;
        if (this.second != null) {
            super.removeChild(this.second);
        }
        this.second = second;
        if (second != null) {
            super.addChild(second);
        }
        ensureSplitterOnTop();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает внутренний разделитель.
     *
     * @return splitter, управляющий drag resize
     */
    public Splitter splitter() {
        return splitter;
    }

    /**
     * Возвращает ориентацию split'а.
     *
     * @return горизонтальная или вертикальная ориентация
     */
    public Orientation orientation() {
        return orientation;
    }

    /**
     * Задаёт ориентацию split'а.
     *
     * @param orientation новая ориентация; {@code null} трактуется как {@link Orientation#HORIZONTAL}
     * @return эта split-панель для fluent-настройки
     */
    @XmlAttribute(value = "orientation", category = "Layout", defaultValue = "horizontal", description = "Split axis; horizontal places slots left/right, vertical places top/bottom.")
    public SplitPanel orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает долю доступной области, занимаемую первым слотом.
     *
     * @return ratio в диапазоне {@code [0, 1]}
     */
    public float splitRatio() {
        return splitRatio;
    }

    /**
     * Задаёт долю доступной области, занимаемую первым слотом.
     *
     * <p>Значение зажимается в диапазон {@code [0, 1]}, а итоговая геометрия
     * дополнительно ограничивается {@link #minFirstSize()} и
     * {@link #minSecondSize()}.</p>
     *
     * @param splitRatio новая доля первого слота
     * @return эта split-панель для fluent-настройки
     */
    public SplitPanel splitRatio(float splitRatio) {
        float normalized = clamp01(splitRatio);
        if (this.splitRatio == normalized) return this;
        this.splitRatio = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт split ratio тем же clamped-путём, что и {@link #splitRatio(float)}.
     *
     * <p>Метод оставлен как удобная точка для внутренних/controlled обновлений
     * и сейчас не отличается по invalidation-поведению от обычного setter'а.</p>
     *
     * @param splitRatio новая доля первого слота
     * @return эта split-панель для fluent-настройки
     */
    @XmlAttribute(value = "splitRatio", category = "Layout", defaultValue = "0.5", description = "Fraction of available space assigned to the first slot.")
    public SplitPanel silentSplitRatio(float splitRatio) {
        return splitRatio(splitRatio);
    }

    /**
     * Возвращает толщину разделителя.
     *
     * @return толщина splitter'а в пикселях UI-пространства
     */
    public float splitterThickness() {
        return splitterThickness;
    }

    /**
     * Задаёт толщину разделителя.
     *
     * @param splitterThickness толщина в пикселях; минимум {@code 1}
     * @return эта split-панель для fluent-настройки
     */
    @XmlAttribute(value = "splitterThickness", category = "Layout", defaultValue = "5", description = "Splitter thickness in UI pixels.")
    public SplitPanel splitterThickness(float splitterThickness) {
        float normalized = Float.isFinite(splitterThickness) ? Math.max(1.0f, splitterThickness) : 5.0f;
        if (this.splitterThickness == normalized) return this;
        this.splitterThickness = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает минимальный размер первой области на главной оси.
     *
     * @return минимальная ширина или высота первого слота
     */
    public float minFirstSize() {
        return minFirstSize;
    }

    /**
     * Задаёт минимальный размер первой области на главной оси.
     *
     * @param minFirstSize минимальный размер; невалидные значения заменяются на {@code 0}
     * @return эта split-панель для fluent-настройки
     */
    @XmlAttribute(value = "minFirstSize", category = "Layout", defaultValue = "32", description = "Minimum size of the first slot on the split axis.")
    public SplitPanel minFirstSize(float minFirstSize) {
        this.minFirstSize = sanitizeMin(minFirstSize);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает минимальный размер второй области на главной оси.
     *
     * @return минимальная ширина или высота второго слота
     */
    public float minSecondSize() {
        return minSecondSize;
    }

    /**
     * Задаёт минимальный размер второй области на главной оси.
     *
     * @param minSecondSize минимальный размер; невалидные значения заменяются на {@code 0}
     * @return эта split-панель для fluent-настройки
     */
    @XmlAttribute(value = "minSecondSize", category = "Layout", defaultValue = "32", description = "Minimum size of the second slot on the split axis.")
    public SplitPanel minSecondSize(float minSecondSize) {
        this.minSecondSize = sanitizeMin(minSecondSize);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает, находится ли разделитель в состоянии drag'а.
     *
     * @return {@code true}, пока пользователь перетаскивает splitter
     */
    public boolean dragging() {
        return splitter.dragging();
    }

    /**
     * Добавляет ребёнка в первый свободный пользовательский слот.
     *
     * <p>Первый вызов заполняет {@link #first(Widget)}, второй —
     * {@link #second(Widget)}. Остальные вызовы игнорируются: split-панель
     * намеренно поддерживает только два пользовательских слота.</p>
     *
     * @param child виджет для добавления; {@code null} игнорируется
     */
    @Override
    public void addChild(Widget child) {
        if (child == null) return;
        if (first == null) {
            first(child);
        } else if (second == null) {
            second(child);
        }
    }

    /**
     * Удаляет ребёнка из первого или второго пользовательского слота.
     *
     * @param child виджет для удаления; splitter удалить через этот API нельзя
     */
    @Override
    public void removeChild(Widget child) {
        if (child == null || child == splitter) return;
        if (child == first) {
            first(null);
        } else if (child == second) {
            second(null);
        } else {
            super.removeChild(child);
        }
    }

    /**
     * Очищает только пользовательские слоты, оставляя внутренний splitter.
     */
    @Override
    public void clearChildren() {
        first(null);
        second(null);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        float fallbackWidth = orientation == Orientation.HORIZONTAL ? 200.0f : 120.0f;
        float fallbackHeight = orientation == Orientation.HORIZONTAL ? 120.0f : 160.0f;
        float width = finiteOr(context == null ? Float.NaN : context.availableWidth(), fallbackWidth);
        float height = finiteOr(context == null ? Float.NaN : context.availableHeight(), fallbackHeight);
        LayoutSize desired = resolveDesiredSize(context, width, height);
        LayoutV3SplitAdapter.measure(first, second, splitter,
                desired.width(), desired.height(),
                orientation, splitRatio, splitterThickness, minFirstSize, minSecondSize);
        setDesiredSize(desired);
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3SplitAdapter.arrange(first, second, splitter, bounds,
                orientation, splitRatio, splitterThickness, minFirstSize, minSecondSize);
    }

    @Override
    protected void renderChildren(RenderContext context) {
        applyQueuedMutations();
        renderChildWithInheritedTransform(context, first);
        renderChildWithInheritedTransform(context, second);
        renderChildWithInheritedTransform(context, splitter);
    }

    void beginSplitterDrag(float rootX, float rootY) {
        dragStartRoot = orientation == Orientation.HORIZONTAL ? rootX : rootY;
        dragStartRatio = splitRatio;
    }

    void dragSplitterTo(float rootX, float rootY) {
        float current = orientation == Orientation.HORIZONTAL ? rootX : rootY;
        float total = orientation == Orientation.HORIZONTAL ? layoutBounds().width() : layoutBounds().height();
        float available = Math.max(1.0f, total - splitterThickness);
        splitRatio(dragStartRatio + (current - dragStartRoot) / available);
    }

    @Override
    public void dispose() {
        splitter.cancelDrag();
        super.dispose();
    }

    private void ensureSplitterOnTop() {
        super.removeChild(splitter);
        super.addChild(splitter);
    }

    private LayoutRects layoutRects(float x, float y, float width, float height) {
        float available = Math.max(0.0f, (orientation == Orientation.HORIZONTAL ? width : height) - splitterThickness);
        float firstSize = available * splitRatio;
        firstSize = clamp(firstSize, Math.min(minFirstSize, available), Math.max(0.0f, available - minSecondSize));
        float secondSize = Math.max(0.0f, available - firstSize);

        if (orientation == Orientation.HORIZONTAL) {
            return new LayoutRects(
                    new MutableRect(x, y, firstSize, height),
                    new MutableRect(x + firstSize, y, splitterThickness, height),
                    new MutableRect(x + firstSize + splitterThickness, y, secondSize, height));
        }
        return new LayoutRects(
                new MutableRect(x, y, width, firstSize),
                new MutableRect(x, y + firstSize, width, splitterThickness),
                new MutableRect(x, y + firstSize + splitterThickness, width, secondSize));
    }

    private static float sanitizeMin(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.5f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float clamp(float value, float min, float max) {
        if (max < min) return min;
        return Math.max(min, Math.min(max, value));
    }

    private record LayoutRects(MutableRect first, MutableRect splitter, MutableRect second) {
    }
}
