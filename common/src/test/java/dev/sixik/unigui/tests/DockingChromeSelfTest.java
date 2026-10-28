package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.CursorResolver;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockColor;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockPolicy;
import dev.sixik.unigui.widgets.docking.DockTheme;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.docking.ResizeGripStyle;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Проверяет хром докинга: иконки, кнопки закрытия, тему, докинг окон по умолчанию, хост-окна без заголовка
 * и компактное окно-призрак при вытягивании вкладки.
 */
public final class DockingChromeSelfTest {
    public static void main(String[] args) {
        new DockingChromeSelfTest().run();
    }

    private void run() {
        testIconWidensTabAndIsDrawnAtThemeSize();
        testCloseButtonClosesOnReleaseOverSameButton();
        testCloseGuardAndNonClosablePanes();
        testMiddleClickClosesTab();
        testDirtyDotReplacesCloseUntilHover();
        testDefaultWindowBecomesHostOnHoverAndRollsBack();
        testDefaultWindowKeepsHostAfterDrop();
        testCapturedWindowKeepsNonModalOnly();
        testHostWindowDragsByTabStripAndClosesWithLastPane();
        testTearOffKeepsPaneStateAndIcon();
        testClassicAndEditorThemesRenderBalanced();
        testResizeEdgesCursorAndGripOnHostWindow();
        testCustomGripStyleAndVisibility();
        System.out.println("DockingChromeSelfTest passed");
    }

    private void testIconWidensTabAndIsDrawnAtThemeSize() {
        DockingRoot root = new DockingRoot();
        AtomicInteger drawn = new AtomicInteger();
        float[] iconSize = {0.0f};
        UiIcon icon = (draw, x, y, size, tint) -> {
            drawn.incrementAndGet();
            iconSize[0] = size;
        };
        root.addDocument("plain", "Same", label("a"));
        root.tabPane("plain", new DockPane("iconic", "Same", label("b")).icon(icon));
        root.selectPane("plain");
        layout(root, 480.0f, 200.0f);

        RectView plain = root.tabBounds("plain");
        RectView iconic = root.tabBounds("iconic");
        expect(plain != null && iconic != null
                        && iconic.width() - plain.width() >= root.theme().iconSize() - 0.01f,
                "A tab with an icon should be wider by at least the icon size");

        render(root);
        expect(drawn.get() >= 1 && Math.abs(iconSize[0] - root.theme().iconSize()) < 0.01f,
                "The tab icon should be drawn once per frame at the theme icon size");
    }

    private void testCloseButtonClosesOnReleaseOverSameButton() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("a", "Alpha", label("a"));
        root.tabPane("a", new DockPane("b", "Beta", label("b")));
        root.selectPane("b");
        layout(root, 480.0f, 200.0f);

        RectView close = root.tabCloseBounds("b");
        expect(close != null, "A closable tab should expose its close button bounds");
        float cx = center(close)[0];
        float cy = center(close)[1];

        press(context, root, cx, cy, 1);
        expect(root.manager().containsPane("b"),
                "Pressing the close button should only arm it; the tab closes on release");
        // Уводим указатель с кнопки и отпускаем: закрытие отменяется.
        release(context, root, cx + 120.0f, cy + 60.0f, 1);
        expect(root.manager().containsPane("b") && root.manager().paneCount() == 2,
                "Releasing away from the close button should not close the tab");

        press(context, root, cx, cy, 2);
        release(context, root, cx, cy, 2);
        expect(!root.manager().containsPane("b") && root.manager().containsPane("a"),
                "Releasing over the pressed close button should close that tab");
    }

    private void testCloseGuardAndNonClosablePanes() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        AtomicInteger asked = new AtomicInteger();
        root.addDocument("guarded", "Guarded", label("g"));
        root.manager().findPane("guarded").onCloseRequested(pane -> {
            asked.incrementAndGet();
            return false;
        });
        root.tabPane("guarded", new DockPane("pinned", "Pinned", label("p")).closable(false));
        layout(root, 480.0f, 200.0f);

        expect(root.tabCloseBounds("pinned") == null,
                "A non-closable pane should not have a close button");
        RectView close = root.tabCloseBounds("guarded");
        float cx = center(close)[0];
        float cy = center(close)[1];
        press(context, root, cx, cy, 3);
        release(context, root, cx, cy, 3);
        expect(asked.get() == 1 && root.manager().containsPane("guarded"),
                "A close guard that returns false should veto closing");
        expect(!root.closeActivePane() || root.manager().containsPane("guarded"),
                "Ctrl+W path should respect the same close guard");
    }

    private void testMiddleClickClosesTab() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("a", "Alpha", label("a"));
        root.tabPane("a", new DockPane("b", "Beta", label("b")));
        layout(root, 480.0f, 200.0f);
        float[] tab = center(root.tabBounds("b"));

        context.routedEvents().dispatch(new PointerPressedEvent(
                root, tab[0], tab[1], tab[0], tab[1], 4, PointerButton.MIDDLE));
        expect(!root.manager().containsPane("b") && root.manager().containsPane("a"),
                "Middle click on a tab should close it");
    }

    private void testDirtyDotReplacesCloseUntilHover() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        root.addDocument("a", "Alpha", label("a"));
        root.tabPane("a", new DockPane("b", "Beta", label("b")).dirty(true));
        root.selectPane("a");
        layout(root, 480.0f, 200.0f);

        ColorView dirty = root.theme().color(DockColor.TAB_DIRTY);
        expect(countFill(render(root), dirty) >= 1,
                "A dirty tab should draw the unsaved-changes dot while not hovered");

        float[] tab = center(root.tabBounds("b"));
        context.routedEvents().dispatch(new PointerMovedEvent(root, tab[0], tab[1], tab[0], tab[1], 5));
        expect(countFill(render(root), dirty) == 0,
                "Hovering a dirty tab should swap the dot for the close button");
    }

    private void testDefaultWindowBecomesHostOnHoverAndRollsBack() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new StackPanel());
        layer.setUiContextInternal(context);

        WindowWidget target = new WindowWidget("Target", label("Target body")).position(300.0f, 200.0f).open();
        target.layout(style -> style.size(240.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(target);
        WindowWidget guest = new WindowWidget("Guest", label("Guest body")).position(20.0f, 20.0f)
                .dockPaneId("guest").open();
        guest.layout(style -> style.size(160.0f, 100.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(guest);
        layout(layer, 640.0f, 400.0f);

        Widget originalContent = target.content();
        expect(!target.dockingEnabled() && target.dockingRoot() == null,
                "A fresh window should not allocate a docking root before anything is dragged over it");

        float tx = target.layoutBounds().x() + target.layoutBounds().width() * 0.5f;
        float ty = target.layoutBounds().y() + target.layoutBounds().height() * 0.5f;
        DockingRoot.previewForeignWindowDrop(guest, tx, ty);
        expect(target.dockingEnabled() && target.captionless()
                        && target.dockingRoot().manager().paneCount() == 1,
                "Hovering a window over a default window should turn it into a captionless host on the fly");

        DockingRoot.previewForeignWindowDrop(guest, 600.0f, 380.0f);
        expect(!target.dockingEnabled() && !target.captionless() && target.content() == originalContent,
                "Moving away without dropping should roll the temporary host back to the plain window");

        DockingRoot.previewForeignWindowDrop(guest, tx, ty);
        DockingRoot.clearForeignWindowDropPreview(guest);
        expect(!target.dockingEnabled() && target.content() == originalContent,
                "Clearing the preview should also roll the temporary host back");
    }

    private void testDefaultWindowKeepsHostAfterDrop() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new StackPanel());
        layer.setUiContextInternal(context);

        WindowWidget target = new WindowWidget("Target", label("Target body")).position(300.0f, 200.0f).open();
        target.layout(style -> style.size(240.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(target);
        WindowWidget guest = new WindowWidget("Guest", label("Guest body")).position(20.0f, 20.0f)
                .dockPaneId("guest").open();
        guest.layout(style -> style.size(160.0f, 100.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(guest);
        layout(layer, 640.0f, 400.0f);

        float tx = target.layoutBounds().x() + target.layoutBounds().width() * 0.5f;
        float ty = target.layoutBounds().y() + target.layoutBounds().height() * 0.5f;
        DockingRoot.previewForeignWindowDrop(guest, tx, ty);
        float[] centerCell = center(DockingRoot.crossCellRect(target.dockingRoot().layoutBounds(), DockArea.CENTER));
        DockingRoot.previewForeignWindowDrop(guest, centerCell[0], centerCell[1]);
        expect(DockingRoot.finishForeignWindowDrop(guest, centerCell[0], centerCell[1]),
                "Dropping on the center cell of a freshly created host should dock the guest");
        layer.applyQueuedMutations();
        expect(target.dockingEnabled()
                        && target.dockingRoot().manager().paneCount() == 2
                        && target.dockingRoot().manager().containsPane("guest")
                        && !guest.opened(),
                "The host should keep both panes as tabs and the guest window should close");

        // Закреплённый хост не откатывается следующим наведением.
        WindowWidget other = new WindowWidget("Other", label("Other body")).position(20.0f, 250.0f).open();
        other.layout(style -> style.size(100.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(other);
        layout(layer, 640.0f, 400.0f);
        DockingRoot.previewForeignWindowDrop(other, tx, ty);
        DockingRoot.previewForeignWindowDrop(other, 600.0f, 380.0f);
        expect(target.dockingEnabled() && target.dockingRoot().manager().paneCount() == 2,
                "A committed host must survive later hover previews");
    }

    private void testCapturedWindowKeepsNonModalOnly() {
        WindowWidget modal = new WindowWidget("Modal", label("m")).modal(true);
        expect(!modal.dockable() && modal.ensureDockHost() == null,
                "Modal windows neither dock nor accept docks");
        WindowWidget dialog = new WindowWidget("Dialog", label("d")).dockPolicy(DockPolicy.DISABLED).open();
        expect(!dialog.dockable() && dialog.ensureDockHost() == null,
                "A window with a disabled policy should refuse to become a host");
    }

    private void testHostWindowDragsByTabStripAndClosesWithLastPane() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new StackPanel());
        layer.setUiContextInternal(context);

        WindowWidget host = new WindowWidget("Host", label("Host body")).position(40.0f, 40.0f)
                .dockingEnabled(true).open();
        host.layout(style -> style.size(260.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(host);
        layout(layer, 640.0f, 400.0f);
        DockingRoot inner = host.dockingRoot();
        expect(host.captionless() && inner.manager().paneCount() == 1,
                "An explicit host should be captionless with a single pane");

        // Единственная вкладка — это заголовок: нажатие на неё двигает окно.
        float[] tab = center(inner.tabBounds(inner.manager().panes().get(0).id()));
        press(context, inner, tab[0], tab[1], 6);
        expect(host.dragging(), "Pressing the only tab of a host window should drag the window");
        release(context, inner, tab[0], tab[1], 6);

        // С двумя вкладками нажатие на вкладку — это таб-драг, а пустое место полосы двигает окно.
        host.dockTool("extra", "Extra", label("extra"));
        layout(layer, 640.0f, 400.0f);
        float[] extraTab = center(inner.tabBounds("extra"));
        press(context, inner, extraTab[0], extraTab[1], 7);
        expect(!host.dragging() && inner.dragController().active(),
                "With several tabs, pressing a tab should drag the tab, not the window");
        release(context, inner, extraTab[0], extraTab[1], 7);
        layout(layer, 640.0f, 400.0f);
        float emptyX = host.layoutBounds().x() + host.layoutBounds().width() - 14.0f;
        float emptyY = host.layoutBounds().y() + 10.0f;
        press(context, inner, emptyX, emptyY, 8);
        expect(host.dragging(), "Pressing empty tab-strip space should drag the host window");
        release(context, inner, emptyX, emptyY, 8);

        // Закрытие последней вкладки закрывает окно.
        String extraId = "extra";
        String firstId = inner.manager().panes().get(0).id();
        inner.manager().closePane(extraId);
        expect(host.opened(), "Closing one of two tabs should keep the host window open");
        inner.manager().closePane(firstId);
        expect(!host.opened(), "Closing the last tab should close the host window");
    }

    private void testTearOffKeepsPaneStateAndIcon() {
        DefaultUIContext context = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(context);
        UiIcon icon = (draw, x, y, size, tint) -> {
        };
        DockPane scene = new DockPane("scene", "Scene", label("scene")).icon(icon).dirty(true);
        root.addPane(scene);
        root.tabPane("scene", new DockPane("inspector", "Inspector", label("inspector")));
        root.selectPane("scene");
        StackPanel host = new StackPanel();
        root.layout(style -> style.size(320.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        host.addChild(root);
        OverlayLayer layer = new OverlayLayer(host);
        layer.setUiContextInternal(context);
        layout(layer, 640.0f, 400.0f);

        float[] tab = center(root.tabBounds("scene"));
        press(context, root, tab[0], tab[1], 9);
        move(context, root, 500.0f, 300.0f, 9);
        WindowWidget window = root.lastFloatingWindow();
        expect(window != null && window.icon() == icon && window.compactDrag()
                        && window.dockSourcePane() == scene,
                "A torn-off tab should keep its icon and remember its pane while shown as a ghost window");

        // Возврат в док через центральную ячейку: возвращается та же панель с её состоянием.
        float[] center = center(DockingRoot.crossCellRect(root.layoutBounds(), DockArea.CENTER));
        move(context, window, center[0], center[1], 9);
        release(context, window, center[0], center[1], 9);
        layer.applyQueuedMutations();
        DockPane back = root.manager().findPane("scene");
        expect(back == scene && back.dirty() && back.icon() == icon,
                "Redocking a torn-off window should restore the very same pane with its state");
    }

    private void testClassicAndEditorThemesRenderBalanced() {
        for (DockTheme theme : new DockTheme[]{DockTheme.editor(), DockTheme.classic()}) {
            DockingRoot root = new DockingRoot().theme(theme);
            root.addDocument("a", "Alpha", label("a"));
            root.tabPane("a", new DockPane("b", "Beta", label("b")).dirty(true).icon(
                    (draw, x, y, size, tint) -> draw.rectFilled(x, y, size, size, tint)));
            root.addToolPane("c", "Gamma", label("c"), DockArea.RIGHT);
            for (int i = 0; i < 10; i++) {
                root.addDocument("d" + i, "Doc " + i, label("d"));
            }
            layout(root, 200.0f, 180.0f);
            DrawList drawList = render(root);
            long pushes = drawList.commands().stream().filter(c -> c.type() == DrawCommandType.PUSH_CLIP).count();
            long pops = drawList.commands().stream().filter(c -> c.type() == DrawCommandType.POP_CLIP).count();
            expect(pushes == pops && pushes > 0,
                    "Chrome painting should keep clip push/pop balanced for every theme");
            expect(root.splitHandleThickness() == theme.splitThickness(),
                    "A theme should drive the split handle thickness of its root");
        }
    }

    private WindowWidget resizableHost(DefaultUIContext context, OverlayLayer layer) {
        WindowWidget host = new WindowWidget("Host", label("Host body")).position(40.0f, 40.0f)
                .dockingEnabled(true).open();
        host.layout(style -> style.size(260.0f, 160.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(host);
        layout(layer, 640.0f, 400.0f);
        return host;
    }

    private static MouseCursor cursorAt(Widget hit, float rootX, float rootY) {
        return CursorResolver.resolve(hit, rootX - hit.layoutBounds().x(), rootY - hit.layoutBounds().y(),
                rootX, rootY);
    }

    private void testResizeEdgesCursorAndGripOnHostWindow() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new StackPanel());
        layer.setUiContextInternal(context);
        WindowWidget host = resizableHost(context, layer);
        DockingRoot inner = host.dockingRoot();
        RectView b = host.layoutBounds();
        float left = b.x() + 2.0f;
        float right = b.x() + b.width() - 2.0f;
        float top = b.y() + 2.0f;
        float bottom = b.y() + b.height() - 2.0f;
        float midX = b.x() + b.width() * 0.5f;
        float midY = b.y() + b.height() * 0.5f;

        // Курсор спрашивают у виджета под указателем — у внутреннего DockingRoot, а не у окна.
        expect(cursorAt(inner, left, midY) == MouseCursor.RESIZE_HORIZONTAL
                        && cursorAt(inner, right, midY) == MouseCursor.RESIZE_HORIZONTAL,
                "Left/right edges of a docking host window should show the horizontal resize cursor");
        expect(cursorAt(inner, midX, top) == MouseCursor.RESIZE_VERTICAL
                        && cursorAt(inner, midX, bottom) == MouseCursor.RESIZE_VERTICAL,
                "Top/bottom edges of a docking host window should show the vertical resize cursor");
        expect(cursorAt(inner, right, bottom) == MouseCursor.RESIZE_NWSE
                        && cursorAt(inner, left, top) == MouseCursor.RESIZE_NWSE
                        && cursorAt(inner, right, top) == MouseCursor.RESIZE_NESW
                        && cursorAt(inner, left, bottom) == MouseCursor.RESIZE_NESW,
                "Corners should show diagonal resize cursors");
        expect(cursorAt(inner, midX, midY) == MouseCursor.DEFAULT,
                "The window interior should keep the content cursor");

        // Нажатие на край над вкладкой докинга растягивает окно, а не выбирает вкладку.
        press(context, inner, left, b.y() + 11.0f, 10);
        expect(host.resizing() && host.resizeHandle().equals("left"),
                "Pressing the window edge above a dock tab should start resizing");
        expect(cursorAt(inner, midX, midY) == MouseCursor.RESIZE_HORIZONTAL,
                "The resize cursor should stay while a resize is in progress");
        release(context, inner, left, b.y() + 11.0f, 10);
        expect(!host.resizing(), "Release should finish resizing");
    }

    private void testCustomGripStyleAndVisibility() {
        DefaultUIContext context = new DefaultUIContext();
        OverlayLayer layer = new OverlayLayer(new StackPanel());
        layer.setUiContextInternal(context);
        WindowWidget host = resizableHost(context, layer);
        DockingRoot inner = host.dockingRoot();
        RectView b = host.layoutBounds();
        float gx = b.x() + b.width() - 5.0f;
        float gy = b.y() + b.height() - 5.0f;

        List<float[]> calls = new ArrayList<>();
        List<ColorView> colors = new ArrayList<>();
        host.resizeGrip((draw, right, bottom, size, radius, color) -> {
            calls.add(new float[]{right, bottom, size});
            colors.add(new dev.sixik.unigui.api.math.MutableColor().set(color));
        });
        DockTheme theme = host.theme();
        renderWindow(host);
        expect(calls.size() == 1
                        && Math.abs(calls.get(0)[0] - (b.x() + b.width())) < 0.01f
                        && Math.abs(calls.get(0)[1] - (b.y() + b.height())) < 0.01f
                        && Math.abs(calls.get(0)[2] - theme.gripSize()) < 0.01f
                        && same(colors.get(0), theme.color(DockColor.GRIP)),
                "A custom grip style should be painted at the bottom-right corner with the theme size and idle color");

        // Наведение: указатель в зоне ручки и под ним виджет окна.
        context.hoverManager().updateHover(inner, gx, gy, gx - inner.layoutBounds().x(),
                gy - inner.layoutBounds().y(), 0);
        context.routedEvents().dispatch(new PointerMovedEvent(inner, gx, gy,
                gx - inner.layoutBounds().x(), gy - inner.layoutBounds().y(), 0));
        calls.clear();
        colors.clear();
        renderWindow(host);
        expect(!colors.isEmpty() && same(colors.get(0), theme.color(DockColor.GRIP_HOVER)),
                "Hovering the grip should switch to the hover color");

        // Нажатие по ручке (не по 6-пиксельному краю) растягивает окно и красит ручку активным цветом.
        press(context, inner, gx - 4.0f, gy - 4.0f, 11);
        expect(host.resizing() && host.resizeHandle().equals("bottom_right"),
                "Pressing inside the grip square should start a bottom-right resize");
        colors.clear();
        renderWindow(host);
        expect(!colors.isEmpty() && same(colors.get(0), theme.color(DockColor.GRIP_ACTIVE)),
                "The grip should use the active color while resizing");
        release(context, inner, gx, gy, 11);

        // Ручку можно скрыть: зона нажатия исчезает вместе с ней.
        host.resizeGripVisible(false);
        calls.clear();
        renderWindow(host);
        expect(calls.isEmpty(), "A hidden grip should not be painted");
        press(context, inner, gx - 4.0f, gy - 4.0f, 12);
        expect(!host.resizing(), "A hidden grip should not capture presses inside the corner square");
        release(context, inner, gx - 4.0f, gy - 4.0f, 12);

        host.resizeGripVisible(true).resizeGrip(ResizeGripStyle.NONE);
        press(context, inner, gx - 4.0f, gy - 4.0f, 13);
        expect(!host.resizing(), "ResizeGripStyle.NONE should behave like a hidden grip");
        release(context, inner, gx - 4.0f, gy - 4.0f, 13);

        // Нерастягиваемое окно ручки не имеет вовсе.
        host.resizeGrip(null).resizable(false);
        calls.clear();
        host.resizeGrip((draw, right, bottom, size, radius, color) -> calls.add(new float[]{size}));
        renderWindow(host);
        expect(calls.isEmpty(), "A non-resizable window should not show a grip");
    }

    private static boolean same(ColorView actual, ColorView expected) {
        return Math.abs(actual.r() - expected.r()) < 0.01f && Math.abs(actual.g() - expected.g()) < 0.01f
                && Math.abs(actual.b() - expected.b()) < 0.01f && Math.abs(actual.a() - expected.a()) < 0.01f;
    }

    private static void renderWindow(WindowWidget window) {
        window.render(new DefaultRenderContext(new DrawList()));
    }

    // -- helpers -----------------------------------------------------------------------------

    private static Label label(String text) {
        Label label = new Label(text);
        label.layout(style -> style.size(90.0f, 18.0f).flexGrow(1).flexShrink(1.0f));
        return label;
    }

    private static void layout(Widget widget, float width, float height) {
        widget.measure(new LayoutContext(width, height));
        widget.arrange(new MutableRect(0.0f, 0.0f, width, height));
    }

    private static DrawList render(DockingRoot root) {
        DrawList drawList = new DrawList();
        root.render(new DefaultRenderContext(drawList));
        return drawList;
    }

    private static float[] center(RectView rect) {
        return new float[]{rect.x() + rect.width() * 0.5f, rect.y() + rect.height() * 0.5f};
    }

    private static int countFill(DrawList drawList, ColorView color) {
        int count = 0;
        List<DrawCommand> commands = new ArrayList<>(drawList.commands());
        for (DrawCommand command : commands) {
            if (command.paint() == null || command.paint().isStroke()) continue;
            ColorView c = command.paint().color();
            if (Math.abs(c.r() - color.r()) < 0.01f && Math.abs(c.g() - color.g()) < 0.01f
                    && Math.abs(c.b() - color.b()) < 0.01f && Math.abs(c.a() - color.a()) < 0.01f) {
                count++;
            }
        }
        return count;
    }

    private static void press(DefaultUIContext context, Widget target, float x, float y, int pointerId) {
        context.routedEvents().dispatch(new PointerPressedEvent(
                target, x, y, x, y, pointerId, PointerButton.PRIMARY));
    }

    private static void move(DefaultUIContext context, Widget target, float x, float y, int pointerId) {
        context.routedEvents().dispatch(new PointerMovedEvent(target, x, y, x, y, pointerId));
    }

    private static void release(DefaultUIContext context, Widget target, float x, float y, int pointerId) {
        context.routedEvents().dispatch(new PointerReleasedEvent(
                target, x, y, x, y, pointerId, PointerButton.PRIMARY));
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
