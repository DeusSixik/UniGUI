package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3DockAdapter;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockingManager;
import dev.sixik.unigui.widgets.docking.DockingRoot;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Простой dock-layout контейнер для приклеивания детей к краям области.
 *
 * <p>{@code DockPanel} последовательно обходит детей и отдаёт каждому полосу
 * у одной из сторон: {@link DockSide#LEFT}, {@link DockSide#RIGHT},
 * {@link DockSide#TOP} или {@link DockSide#BOTTOM}. После каждого ребёнка
 * доступная область уменьшается. Если {@link #lastChildFill()} включён, последний
 * ребёнок занимает весь оставшийся прямоугольник.</p>
 *
 * <p>Это не IDE-style docking root. Для панелей, вкладок, drag-drop зон и
 * snapshot'ов layout'а используй {@link DockingRoot} и {@link DockingManager}.
 * {@code DockPanel} использует {@link DockSide}, а docking-система —
 * {@link DockArea}.</p>
 *
 * <pre>{@code
 * DockPanel root = new DockPanel();
 * root.addChild(header, DockSide.TOP);
 * root.addChild(sidebar, DockSide.LEFT);
 * root.addChild(content); // заполнит остаток при lastChildFill(true)
 * }</pre>
 */
@XmlWidgetName("DockPanel")
public final class DockPanel extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.DOCK_PANEL;

    private final Map<Widget, DockSide> docks = new IdentityHashMap<>();
    private boolean lastChildFill = true;

    /**
     * Добавляет ребёнка с заданной dock-стороной.
     *
     * @param child дочерний виджет; {@code null} игнорируется
     * @param dockSide сторона приклеивания; {@code null} трактуется как {@link DockSide#LEFT}
     */
    public void addChild(Widget child, DockSide dockSide) {
        if (child == null) return;
        docks.put(child, dockSide == null ? DockSide.LEFT : dockSide);
        super.addChild(child);
    }

    /**
     * Возвращает dock-сторону ребёнка.
     *
     * @param child дочерний виджет
     * @return сохранённая сторона или {@link DockSide#LEFT}, если сторона не задана
     */
    public DockSide dockSide(Widget child) {
        return docks.getOrDefault(child, DockSide.LEFT);
    }

    /**
     * Меняет dock-сторону уже добавленного или будущего ребёнка.
     *
     * @param child виджет, для которого сохраняется сторона; {@code null} игнорируется
     * @param dockSide новая сторона; {@code null} трактуется как {@link DockSide#LEFT}
     * @return этот контейнер для fluent-настройки
     */
    public DockPanel dockSide(Widget child, DockSide dockSide) {
        if (child == null) return this;
        DockSide normalized = dockSide == null ? DockSide.LEFT : dockSide;
        if (dockSide(child) == normalized) return this;
        docks.put(child, normalized);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает, занимает ли последний ребёнок всю оставшуюся область.
     *
     * @return {@code true}, если последний ребёнок fill'ит остаток
     */
    public boolean lastChildFill() {
        return lastChildFill;
    }

    /**
     * Управляет fill-поведением последнего ребёнка.
     *
     * @param lastChildFill {@code true}, чтобы последний ребёнок занимал остаток области
     * @return этот контейнер для fluent-настройки
     */
    @XmlAttribute(value = "lastChildFill", category = "Layout", defaultValue = "true", description = "Whether the last child fills the remaining dock area.")
    public DockPanel lastChildFill(boolean lastChildFill) {
        if (this.lastChildFill == lastChildFill) return this;
        this.lastChildFill = lastChildFill;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Добавляет ребёнка со стороной {@link DockSide#LEFT} по умолчанию.
     *
     * @param child дочерний виджет; {@code null} игнорируется
     */
    @Override
    public void addChild(Widget child) {
        addChild(child, DockSide.LEFT);
    }

    @Override
    public void removeChild(Widget child) {
        docks.remove(child);
        super.removeChild(child);
    }

    @Override
    public void clearChildren() {
        docks.clear();
        super.clearChildren();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();

        LayoutSize measured = LayoutV3DockAdapter.measure(
                children(), this::dockSide, lastChildFill, context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3DockAdapter.arrange(children(), this::dockSide, lastChildFill, bounds, layoutStyle());
    }
}
