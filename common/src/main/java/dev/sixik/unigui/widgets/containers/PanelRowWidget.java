package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Двусторонняя строка панели: произвольные виджеты слева и справа.
 *
 * <p>Контейнер удобен для title bars, toolbars и строк настроек: заголовки или
 * navigation-кнопки можно класть слева, а действия и controls держать у правого
 * края. Левая группа раскладывается от левого края, правая — от правого.</p>
 *
 * <p>{@link #leftWidth(float)} и {@link #rightWidth(float)} могут зафиксировать
 * ширину группы. Значение {@link LayoutConstraints#AUTO} оставляет ширину по
 * measured-size детей.</p>
 *
 * <pre>{@code
 * PanelRowWidget row = new PanelRowWidget()
 *         .rowHeight(24.0f)
 *         .gap(12.0f);
 * row.left(titleLabel);
 * row.addRight(cancelButton);
 * row.addRight(applyButton);
 * }</pre>
 */
@XmlWidgetName("PanelRowWidget")
public class PanelRowWidget extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.PANEL_ROW_WIDGET;

    /**
     * Дефолтная высота строки в пикселях UI-пространства.
     */
    public static final float DEFAULT_ROW_HEIGHT = 24.0f;
    /**
     * Дефолтное расстояние между группами и элементами.
     */
    public static final float DEFAULT_GAP = 8.0f;

    private final List<Widget> leftWidgets = new ObjectArrayList<>();
    private final List<Widget> rightWidgets = new ObjectArrayList<>();
    private final List<Widget> leftWidgetsView = Collections.unmodifiableList(leftWidgets);
    private final List<Widget> rightWidgetsView = Collections.unmodifiableList(rightWidgets);

    private float rowHeight = DEFAULT_ROW_HEIGHT;
    private float gap = DEFAULT_GAP;
    private float leftGap = DEFAULT_GAP;
    private float rightGap = DEFAULT_GAP;
    private float leftWidth = LayoutConstraints.AUTO;
    private float rightWidth = LayoutConstraints.AUTO;

    /**
     * Создаёт пустую двустороннюю строку.
     */
    public PanelRowWidget() {
    }

    /**
     * Создаёт строку с одним виджетом слева и одним справа.
     *
     * @param left начальный левый виджет
     * @param right начальный правый виджет
     */
    public PanelRowWidget(Widget left, Widget right) {
        left(left);
        right(right);
    }

    /**
     * Возвращает неизменяемое представление левой группы.
     *
     * @return виджеты слева в порядке раскладки
     */
    public List<Widget> leftWidgets() {
        return leftWidgetsView;
    }

    /**
     * Возвращает неизменяемое представление правой группы.
     *
     * @return виджеты справа в порядке добавления
     */
    public List<Widget> rightWidgets() {
        return rightWidgetsView;
    }

    /**
     * Заменяет левую группу одним виджетом.
     *
     * @param widget новый левый виджет; {@code null} просто очищает группу
     * @return эта строка для fluent-настройки
     */
    public PanelRowWidget left(Widget widget) {
        clearLeft();
        return addLeft(widget);
    }

    /**
     * Заменяет правую группу одним виджетом.
     *
     * @param widget новый правый виджет; {@code null} просто очищает группу
     * @return эта строка для fluent-настройки
     */
    public PanelRowWidget right(Widget widget) {
        clearRight();
        return addRight(widget);
    }

    /**
     * Добавляет виджет в левую группу.
     *
     * @param widget виджет для добавления; {@code null} игнорируется
     * @return эта строка для fluent-настройки
     */
    public PanelRowWidget addLeft(Widget widget) {
        return addToSide(widget, leftWidgets, rightWidgets);
    }

    /**
     * Добавляет виджет в правую группу.
     *
     * @param widget виджет для добавления; {@code null} игнорируется
     * @return эта строка для fluent-настройки
     */
    public PanelRowWidget addRight(Widget widget) {
        return addToSide(widget, rightWidgets, leftWidgets);
    }

    /**
     * Очищает левую группу.
     *
     * @return эта строка для fluent-настройки
     */
    public PanelRowWidget clearLeft() {
        return clearSide(leftWidgets);
    }

    /**
     * Очищает правую группу.
     *
     * @return эта строка для fluent-настройки
     */
    public PanelRowWidget clearRight() {
        return clearSide(rightWidgets);
    }

    /**
     * Возвращает предпочитаемую высоту строки.
     *
     * @return высота строки или {@link LayoutConstraints#AUTO}
     */
    public float rowHeight() {
        return rowHeight;
    }

    /**
     * Задаёт предпочитаемую высоту строки.
     *
     * @param rowHeight высота строки; {@link LayoutConstraints#AUTO} оставляет высоту по содержимому
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "rowHeight", category = "Layout", defaultValue = "24", description = "Preferred row height or auto.")
    public PanelRowWidget rowHeight(float rowHeight) {
        float normalized = normalizeSizeOrAuto(rowHeight, DEFAULT_ROW_HEIGHT);
        if (this.rowHeight == normalized || bothAuto(this.rowHeight, normalized)) return this;
        this.rowHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает расстояние между левой и правой группами.
     *
     * @return gap между группами
     */
    public float gap() {
        return gap;
    }

    /**
     * Задаёт расстояние между левой и правой группами.
     *
     * @param gap gap в пикселях UI-пространства; невалидные значения заменяются дефолтом
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "gap", category = "Layout", defaultValue = "8", description = "Gap between left and right widget groups.")
    public PanelRowWidget gap(float gap) {
        float normalized = normalizeGap(gap, DEFAULT_GAP);
        if (this.gap == normalized) return this;
        this.gap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает gap между элементами левой группы.
     *
     * @return left-side item gap
     */
    public float leftGap() {
        return leftGap;
    }

    /**
     * Задаёт gap между элементами левой группы.
     *
     * @param leftGap gap в пикселях UI-пространства
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "leftGap", category = "Layout", defaultValue = "8", description = "Gap between widgets in the left group.")
    public PanelRowWidget leftGap(float leftGap) {
        float normalized = normalizeGap(leftGap, DEFAULT_GAP);
        if (this.leftGap == normalized) return this;
        this.leftGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает gap между элементами правой группы.
     *
     * @return right-side item gap
     */
    public float rightGap() {
        return rightGap;
    }

    /**
     * Задаёт gap между элементами правой группы.
     *
     * @param rightGap gap в пикселях UI-пространства
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "rightGap", category = "Layout", defaultValue = "8", description = "Gap between widgets in the right group.")
    public PanelRowWidget rightGap(float rightGap) {
        float normalized = normalizeGap(rightGap, DEFAULT_GAP);
        if (this.rightGap == normalized) return this;
        this.rightGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает предпочитаемую ширину левой группы.
     *
     * @return ширина или {@link LayoutConstraints#AUTO}
     */
    public float leftWidth() {
        return leftWidth;
    }

    /**
     * Задаёт предпочитаемую ширину левой группы.
     *
     * @param leftWidth ширина группы или {@link LayoutConstraints#AUTO}
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "leftWidth", category = "Layout", defaultValue = "auto", description = "Preferred width of the left widget group.")
    public PanelRowWidget leftWidth(float leftWidth) {
        float normalized = normalizeSizeOrAuto(leftWidth, LayoutConstraints.AUTO);
        if (this.leftWidth == normalized || bothAuto(this.leftWidth, normalized)) return this;
        this.leftWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает предпочитаемую ширину правой группы.
     *
     * @return ширина или {@link LayoutConstraints#AUTO}
     */
    public float rightWidth() {
        return rightWidth;
    }

    /**
     * Задаёт предпочитаемую ширину правой группы.
     *
     * @param rightWidth ширина группы или {@link LayoutConstraints#AUTO}
     * @return эта строка для fluent-настройки
     */
    @XmlAttribute(value = "rightWidth", category = "Layout", defaultValue = "auto", description = "Preferred width of the right widget group.")
    public PanelRowWidget rightWidth(float rightWidth) {
        float normalized = normalizeSizeOrAuto(rightWidth, LayoutConstraints.AUTO);
        if (this.rightWidth == normalized || bothAuto(this.rightWidth, normalized)) return this;
        this.rightWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }

        applyQueuedMutations();
        EdgeInsets padding = layoutStyle().padding();
        float availableWidth = subtractAvailable(context == null ? Float.POSITIVE_INFINITY : context.availableWidth(), padding.horizontal());
        float availableHeight = subtractAvailable(context == null ? Float.POSITIVE_INFINITY : context.availableHeight(), padding.vertical());

        GroupMeasure left = measureGroup(leftWidgets, availableWidth, availableHeight, leftGap, leftWidth);
        GroupMeasure right = measureGroup(rightWidgets, availableWidth, availableHeight, rightGap, rightWidth);
        float contentGap = left.width() > 0.0f && right.width() > 0.0f ? gap : 0.0f;
        float contentWidth = left.width() + contentGap + right.width();
        float contentHeight = Math.max(left.height(), right.height());
        if (!LayoutConstraints.isAuto(rowHeight)) {
            contentHeight = Math.max(contentHeight, rowHeight);
        }

        setDesiredSize(resolveDesiredSize(context,
                contentWidth + padding.horizontal(),
                contentHeight + padding.vertical()));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();

        EdgeInsets padding = layoutStyle().padding();
        float contentX = bounds.x() + padding.left();
        float contentY = bounds.y() + padding.top();
        float contentWidth = Math.max(0.0f, bounds.width() - padding.horizontal());
        float contentHeight = Math.max(0.0f, bounds.height() - padding.vertical());

        GroupMeasure left = groupFromDesired(leftWidgets, leftGap, leftWidth, contentWidth);
        GroupMeasure right = groupFromDesired(rightWidgets, rightGap, rightWidth, contentWidth);
        float contentGap = left.width() > 0.0f && right.width() > 0.0f ? gap : 0.0f;
        float rightSlotWidth = Math.min(right.width(), contentWidth);
        float leftAvailable = Math.max(0.0f, contentWidth - rightSlotWidth - contentGap);
        float leftSlotWidth = Math.min(left.width(), leftAvailable);
        float rightX = contentX + Math.max(0.0f, contentWidth - rightSlotWidth);

        arrangeGroup(leftWidgets, contentX, contentY, leftSlotWidth, contentHeight, leftGap, leftWidth, false);
        arrangeGroup(rightWidgets, rightX, contentY, rightSlotWidth, contentHeight, rightGap, rightWidth, true);
    }

    private PanelRowWidget addToSide(Widget widget, List<Widget> target, List<Widget> other) {
        if (widget == null || target.contains(widget)) return this;
        if (other.remove(widget)) {
            removeChild(widget);
        }
        target.add(widget);
        addChild(widget);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    private PanelRowWidget clearSide(List<Widget> widgets) {
        if (widgets.isEmpty()) return this;
        for (Widget widget : new ObjectArrayList<>(widgets)) {
            removeChild(widget);
        }
        widgets.clear();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    private static GroupMeasure measureGroup(List<Widget> widgets,
                                             float availableWidth,
                                             float availableHeight,
                                             float itemGap,
                                             float preferredWidth) {
        float groupAvailableWidth = LayoutConstraints.isAuto(preferredWidth)
                ? availableWidth
                : Math.min(Math.max(0.0f, preferredWidth), Math.max(0.0f, availableWidth));
        LayoutContext childContext = new LayoutContext(groupAvailableWidth, availableHeight);
        for (Widget widget : widgets) {
            if (widget.visibility() != Visibility.COLLAPSED) {
                widget.measure(childContext);
            }
        }
        return groupFromDesired(widgets, itemGap, preferredWidth, availableWidth);
    }

    private static GroupMeasure groupFromDesired(List<Widget> widgets,
                                                 float itemGap,
                                                 float preferredWidth,
                                                 float availableWidth) {
        float width = 0.0f;
        float height = 0.0f;
        int count = 0;
        for (Widget widget : widgets) {
            if (widget.visibility() == Visibility.COLLAPSED) continue;
            EdgeInsets margin = widget.layoutConstraints().margin();
            if (count > 0) width += itemGap;
            width += widget.desiredSize().width() + margin.horizontal();
            height = Math.max(height, widget.desiredSize().height() + margin.vertical());
            count++;
        }
        if (count > 0 && !LayoutConstraints.isAuto(preferredWidth)) {
            width = Math.max(width, preferredWidth);
        }
        width = Math.min(width, Math.max(0.0f, availableWidth));
        return new GroupMeasure(width, height, count);
    }

    private static void arrangeGroup(List<Widget> widgets,
                                     float slotX,
                                     float slotY,
                                     float slotWidth,
                                     float slotHeight,
                                     float itemGap,
                                     float preferredWidth,
                                     boolean alignEnd) {
        if (slotWidth <= 0.0f) {
            arrangeCollapsed(widgets, slotX, slotY);
            return;
        }
        GroupMeasure measured = groupFromDesired(widgets, itemGap, LayoutConstraints.AUTO, Float.POSITIVE_INFINITY);
        if (measured.count() == 0) return;

        boolean singleExplicit = measured.count() == 1 && !LayoutConstraints.isAuto(preferredWidth);
        float cursor = slotX;
        if (alignEnd && !singleExplicit) {
            cursor += Math.max(0.0f, slotWidth - measured.width());
        }

        for (Widget widget : widgets) {
            if (widget.visibility() == Visibility.COLLAPSED) continue;
            EdgeInsets margin = widget.layoutConstraints().margin();
            float availableForChild = Math.max(0.0f, slotWidth - margin.horizontal());
            float childWidth = singleExplicit
                    ? availableForChild
                    : Math.min(availableForChild, widget.desiredSize().width());
            float childHeight = Math.min(Math.max(0.0f, slotHeight - margin.vertical()), widget.desiredSize().height());
            float childX = cursor + margin.left();
            float childY = slotY + margin.top() + Math.max(0.0f, slotHeight - margin.vertical() - childHeight) * 0.5f;
            widget.arrange(new MutableRect(childX, childY, childWidth, childHeight));
            cursor += childWidth + margin.horizontal() + itemGap;
        }
    }

    private static void arrangeCollapsed(List<Widget> widgets, float x, float y) {
        for (Widget widget : widgets) {
            if (widget.visibility() != Visibility.COLLAPSED) {
                widget.arrange(new MutableRect(x, y, 0.0f, 0.0f));
            }
        }
    }

    private static float subtractAvailable(float available, float amount) {
        if (!Float.isFinite(available)) return Float.POSITIVE_INFINITY;
        return Math.max(0.0f, available - Math.max(0.0f, amount));
    }

    private static float normalizeGap(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : fallback;
    }

    private static float normalizeSizeOrAuto(float value, float fallback) {
        if (LayoutConstraints.isAuto(value)) return LayoutConstraints.AUTO;
        if (Float.isFinite(value)) return Math.max(0.0f, value);
        return fallback;
    }

    private static boolean bothAuto(float a, float b) {
        return LayoutConstraints.isAuto(a) && LayoutConstraints.isAuto(b);
    }

    private record GroupMeasure(float width, float height, int count) {
    }
}
