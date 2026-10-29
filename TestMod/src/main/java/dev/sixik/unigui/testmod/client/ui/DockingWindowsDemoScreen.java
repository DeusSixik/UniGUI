package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockPaneKind;
import dev.sixik.unigui.widgets.docking.DockPolicy;
import dev.sixik.unigui.widgets.docking.DockTheme;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.BORDER;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.PANEL;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.TEXT;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.button;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.checkbox;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.fullWidth;
import static dev.sixik.unigui.testmod.client.ui.DemoScaffold.label;

/**
 * Демо drag-to-dock для {@link WindowWidget}.
 *
 * <p>Слева — {@link DockingRoot} с иконками на вкладках, справа — спавн окон. Докинг —
 * свойство любого окна по умолчанию: тащите заголовок над workspace или над другим окном
 * (оно само станет хостом с вкладками). Вкладку можно вытянуть из дока: она превращается в
 * компактное окно 128x128 и принимает свой размер, если её никуда не пристыковали.</p>
 */
public final class DockingWindowsDemoScreen {
    private static final MutableColor BG = MutableColor.rgba(0.020f, 0.026f, 0.038f, 1.0f);

    /** Процедурный куб: показывает, что иконка вкладки — обычная лямбда, а не виджет. */
    private static final UiIcon CUBE_ICON = (draw, x, y, size, tint) -> {
        float cx = x + size * 0.5f;
        float cy = y + size * 0.5f;
        float r = size * 0.46f;
        MutableColor top = MutableColor.rgba(0.55f, 0.75f, 1.0f, tint.a());
        MutableColor left = MutableColor.rgba(0.28f, 0.52f, 0.92f, tint.a());
        MutableColor right = MutableColor.rgba(0.18f, 0.36f, 0.74f, tint.a());
        draw.pathClear();
        draw.pathLineTo(cx, cy - r);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathFillConvex(top);
        draw.pathClear();
        draw.pathLineTo(cx - r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx, cy + r);
        draw.pathLineTo(cx - r * 0.866f, cy + r * 0.5f);
        draw.pathFillConvex(left);
        draw.pathClear();
        draw.pathLineTo(cx, cy);
        draw.pathLineTo(cx + r * 0.866f, cy - r * 0.5f);
        draw.pathLineTo(cx + r * 0.866f, cy + r * 0.5f);
        draw.pathLineTo(cx, cy + r);
        draw.pathFillConvex(right);
    };

    /** Монохромная иконка «i»: красится цветом темы (приглушённый на неактивной вкладке). */
    private static final UiIcon INFO_ICON = (draw, x, y, size, tint) -> {
        float d = size * 0.86f;
        draw.circleStroke(x + (size - d) * 0.5f, y + (size - d) * 0.5f, d, d, tint, 1.4f);
        draw.rectFilled(x + size * 0.46f, y + size * 0.42f, size * 0.1f, size * 0.28f, tint);
        draw.rectFilled(x + size * 0.46f, y + size * 0.26f, size * 0.1f, size * 0.1f, tint);
    };

    /** Монохромная иконка «строки лога». */
    private static final UiIcon LINES_ICON = (draw, x, y, size, tint) -> {
        for (int i = 0; i < 3; i++) {
            float ly = y + size * (0.22f + i * 0.26f);
            draw.rectFilled(x + size * 0.12f, ly, size * (i == 1 ? 0.5f : 0.76f), size * 0.1f, tint);
        }
    };

    private DockingWindowsDemoScreen() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.0f));

        Runnable[] close = new Runnable[1];
        Widget root = root(() -> close[0].run());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("Docking Windows Demo"), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        close[0] = screen::onClose;
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.vsync());
        minecraft.setScreen(screen);
    }

    private static Widget root(Runnable closeAction) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));

        Box background = new Box();
        background.themeEnabled(false);
        background.backgroundVisible(true);
        background.borderVisible(false);
        background.background().set(BG);
        background.layout(style -> style.sizePercent(100.0f, 100.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        viewport.addChild(background);

        OverlayLayer overlays = new OverlayLayer(viewportBody(closeAction));
        overlays.layout(style -> style.sizePercent(100.0f, 100.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        viewport.addChild(overlays);
        return viewport;
    }

    private static Widget viewportBody(Runnable closeAction) {
        DockingRoot docking = new DockingRoot();
        docking.addPane(DockPane.document("scene", "Scene",
                paneBody("Scene document. Drag window headers over this area.")).icon(CUBE_ICON))
                .addToolPane("inspector", "Inspector", paneBody("Inspector tool pane."), DockArea.RIGHT)
                .addToolPane("log", "Log", paneBody("Log tool pane."), DockArea.BOTTOM)
                .selectPane("scene");
        docking.manager().findPane("inspector").icon(INFO_ICON);
        docking.manager().findPane("log").icon(LINES_ICON).dirty(true);
        docking.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        Label status = fullWidth(label("Panes: 3. Drag a dockable window header over the workspace.", TEXT), 20.0f);
        docking.onLayoutChanged(event -> status.text(
                "Docking layout: " + event.operation() + " (" + event.paneId() + "), panes: "
                        + docking.manager().paneCount()));
        docking.onDropPreviewChanged(event -> {
            if (event.newIntent().valid()) {
                status.text("Preview: " + event.newIntent().area() + " -> " + event.newIntent().targetPaneId());
            }
        });

        VBox side = new VBox();
        side.spacing(8.0f);
        side.layout(style -> style.width(230.0f).heightPercent(100.0f).padding(10.0f)
                .gap(8.0f).flexGrow(0.0f).flexShrink(0.0f));

        Label title = fullWidth(label("DOCKING WINDOWS", TEXT), 22.0f);
        Label hint = fullWidth(label("Every window docks by default: drag a header over the workspace or over another window. Drag a tab out to get a 128x128 ghost.", DemoScaffold.MUTED), 46.0f);

        Checkbox lockCheck = checkbox("Lock redock", false);
        Checkbox guestOnlyCheck = checkbox("Spawn as guest only (no drops accepted)", false);
        Checkbox classicCheck = checkbox("Classic theme", false);
        classicCheck.onStateChanged(event -> {
            DockTheme.defaultTheme(classicCheck.checked() ? DockTheme.classic() : DockTheme.editor());
            docking.theme(null);
        });

        int[] counter = {1};
        Button spawn = fullWidth(button("Spawn window", () -> {
            OverlayLayer layer = overlayOf(docking);
            if (layer == null) return;
            String id = "demo-" + counter[0]++;
            WindowWidget window = new WindowWidget("Demo " + id, paneBody("Content of " + id + "."))
                    .position(260.0f + counter[0] * 14.0f % 160.0f, 90.0f + counter[0] * 11.0f % 120.0f)
                    .icon(counter[0] % 2 == 0 ? INFO_ICON : LINES_ICON)
                    .dockPaneId(id)
                    .dockKind(DockPaneKind.TOOL)
                    .dockPolicy(guestOnlyCheck.checked() ? DockPolicy.GUEST_ONLY : DockPolicy.FULL)
                    .dockRedockLocked(lockCheck.checked())
                    .closeOnOutsideClick(false);
            window.layout(style -> style.size(220.0f, 130.0f).flexGrow(0.0f).flexShrink(0.0f));
            window.onClosed(event -> status.text("Window closed: " + id
                    + ", panes: " + docking.manager().paneCount()));
            layer.addOverlay(window);
            window.open();
            status.text("Spawned " + id + " (policy=" + window.dockPolicy() + ")");
        }), 28.0f);

        Button close = fullWidth(button("Close", closeAction), 28.0f);

        side.addChild(title);
        side.addChild(hint);
        side.addChild(lockCheck);
        side.addChild(guestOnlyCheck);
        side.addChild(classicCheck);
        side.addChild(spawn);
        side.addChild(status);
        side.addChild(close);

        Box sidePanel = new Box();
        sidePanel.themeEnabled(false);
        sidePanel.backgroundVisible(true);
        sidePanel.borderVisible(true);
        sidePanel.background().set(PANEL);
        sidePanel.borderColor().set(BORDER);
        sidePanel.layout(style -> style.width(250.0f).heightPercent(100.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        sidePanel.addChild(side);

        HBox body = new HBox();
        body.spacing(10.0f);
        body.layout(style -> style.sizePercent(96.0f, 90.0f)
                .align(Alignment.CENTER, Alignment.CENTER).gap(10.0f));
        body.addChild(docking);
        body.addChild(sidePanel);
        return body;
    }

    private static OverlayLayer overlayOf(Widget widget) {
        Widget current = widget;
        while (current != null) {
            if (current instanceof OverlayLayer overlay) return overlay;
            current = current.parent();
        }
        return null;
    }

    private static Widget paneBody(String text) {
        Label body = label(text, TEXT);
        body.layout(style -> style.sizePercent(100.0f, 100.0f));
        return body;
    }
}
