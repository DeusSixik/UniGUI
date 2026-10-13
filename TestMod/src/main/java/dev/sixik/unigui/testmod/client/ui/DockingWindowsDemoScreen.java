package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
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
import dev.sixik.unigui.widgets.docking.DockPaneKind;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Демо drag-to-dock для {@link WindowWidget}.
 *
 * <p>Слева — {@link DockingRoot} с парой панелей, справа — кнопки спавна
 * свободных окон. Окна с включённым флагом {@code dockable} можно тащить
 * за header над docking root: появится drop preview, а на release контент
 * окна станет dock-панелью и окно закроется.</p>
 */
public final class DockingWindowsDemoScreen {
    private static final MutableColor BG = MutableColor.rgba(0.020f, 0.026f, 0.038f, 1.0f);
    private static final MutableColor PANEL = MutableColor.rgba(0.050f, 0.064f, 0.088f, 0.96f);
    private static final MutableColor BORDER = MutableColor.rgba(0.220f, 0.400f, 0.580f, 0.88f);
    private static final MutableColor TEXT = MutableColor.rgba(0.820f, 0.900f, 0.960f, 1.0f);
    private static final MutableColor MUTED = MutableColor.rgba(0.500f, 0.600f, 0.700f, 1.0f);

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
        docking.addDocument("scene", "Scene", paneBody("Scene document. Drag window headers over this area."))
                .addToolPane("inspector", "Inspector", paneBody("Inspector tool pane."), DockArea.RIGHT)
                .addToolPane("log", "Log", paneBody("Log tool pane."), DockArea.BOTTOM)
                .selectPane("scene");
        docking.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        Label status = label("Panes: 3. Drag a dockable window header over the workspace.", TEXT);
        status.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
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

        Label title = label("DOCKING WINDOWS", TEXT);
        title.layout(style -> style.widthPercent(100.0f).height(22.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Label hint = label("Drag headers over the workspace. Locked windows stay floating.", MUTED);
        hint.layout(style -> style.widthPercent(100.0f).height(34.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        Checkbox dockableCheck = new Checkbox("Dockable windows");
        dockableCheck.checked(true);
        Checkbox lockCheck = new Checkbox("Lock redock");
        lockCheck.checked(false);

        int[] counter = {1};
        Button spawn = button("Spawn window", () -> {
            OverlayLayer layer = overlayOf(docking);
            if (layer == null) return;
            String id = "demo-" + counter[0]++;
            WindowWidget window = new WindowWidget("Demo " + id, paneBody("Content of " + id + "."))
                    .position(260.0f + counter[0] * 14.0f % 160.0f, 90.0f + counter[0] * 11.0f % 120.0f)
                    .dockable(dockableCheck.checked())
                    .dockPaneId(id)
                    .dockKind(DockPaneKind.TOOL)
                    .dockRedockLocked(lockCheck.checked())
                    .closeOnOutsideClick(false);
            window.layout(style -> style.size(220.0f, 130.0f).flexGrow(0.0f).flexShrink(0.0f));
            window.onClosed(event -> status.text("Window closed: " + id
                    + ", panes: " + docking.manager().paneCount()));
            layer.addOverlay(window);
            window.open();
            status.text("Spawned " + id + " (dockable=" + window.dockable() + ")");
        });
        spawn.layout(style -> style.widthPercent(100.0f).height(28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        Button close = button("Close", closeAction);
        close.layout(style -> style.widthPercent(100.0f).height(28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        side.addChild(title);
        side.addChild(hint);
        side.addChild(dockableCheck);
        side.addChild(lockCheck);
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

    private static Label label(String text, MutableColor color) {
        Label label = new Label(text);
        label.color(color);
        return label;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.onClick(event -> action.run());
        return button;
    }
}
