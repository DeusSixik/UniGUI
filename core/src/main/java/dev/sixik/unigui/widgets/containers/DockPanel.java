package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(
        summary = "Edge-docking layout container that aligns child widgets along outer boundaries (left, right, top, bottom).",
        purpose = "Creates classic desktop application shells with top menu/toolbars, sidebars, bottom status bars, and central expanding workspace.",
        useWhen = {
                "Arrange peripheral controls around a central main content area.",
                "Build application window chrome: header bar on top, footer at bottom, side navigation panels."
        },
        criticalRules = {
                "Children are laid out sequentially along their assigned DockSide, each consuming available space along that edge.",
                "When lastChildFill is true (default), the final child widget occupies the entire remaining interior area.",
                "For complex IDE docking (tear-off windows, tabs, split docking zones), use DockingRoot and DockingManager instead of DockPanel."
        },
        related = {"PanelWidget", "SplitPanel", "Box"},
        examples = @AiDoc.Example(
                title = "Docking application shell",
                code = "DockPanel shell = new DockPanel();\nshell.addChild(toolbar, DockSide.TOP);\nshell.addChild(sidebar, DockSide.LEFT);\nshell.addChild(mainView);"))
@WikiPage(
        title = "<key:dock.panel.title>",
        slug = "dock-panel",
        category = "<key:dock.panel.category>",
        summary = "<key:dock.panel.summary>",
        order = 120)
@WikiSection(title = "<key:dock.panel.section.overview.title>",
        markdown = "<key:dock.panel.section.overview.body>", order = 10)
@WikiSection(title = "<key:dock.panel.section.fill.title>",
        markdown = "<key:dock.panel.section.fill.body>", order = 20)
@WikiExample(title = "<key:dock.panel.example.shell.title>",
        code = "<key:dock.panel.example.shell.code>", order = 10)
@WikiLink(label = "PanelWidget", target = "page:panel-widget")
@WikiLink(label = "SplitPanel", target = "page:split-panel")
@WikiLink(label = "Box", target = "page:box")
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
    @WikiMethod(order = 10, title = "<key:dock.panel.method.add_child.title>", summary = "<key:dock.panel.method.add_child>")
    @AiDoc(section = "Layout", sectionOrder = 10, order = 10, summary = "Adds a child widget docked to a specific edge.")
    public void addChild(@WikiParameter(description = "<key:dock.panel.param.child>") Widget child,
                         @WikiParameter(description = "<key:dock.panel.param.dock_side>") DockSide dockSide) {
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
    @WikiMethod(order = 20, title = "<key:dock.panel.method.dock_side.title>", summary = "<key:dock.panel.method.dock_side>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Layout", sectionOrder = 10, order = 20, summary = "Sets dock side for an existing or future child.")
    public DockPanel dockSide(@WikiParameter(description = "<key:dock.panel.param.child>") Widget child,
                              @WikiParameter(description = "<key:dock.panel.param.dock_side>") DockSide dockSide) {
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
    @WikiMethod(order = 30, title = "<key:dock.panel.method.last_child_fill.title>", summary = "<key:dock.panel.method.last_child_fill>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    @AiDoc(section = "Layout", sectionOrder = 10, order = 30, summary = "Controls whether the last child expands to occupy remaining space.")
    public DockPanel lastChildFill(@WikiParameter(description = "<key:dock.panel.param.last_child_fill>") boolean lastChildFill) {
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
