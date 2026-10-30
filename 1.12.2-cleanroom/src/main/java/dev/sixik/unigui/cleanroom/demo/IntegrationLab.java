package dev.sixik.unigui.cleanroom.demo;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.screen.UniHudRender;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.screen.UniRenderLayerRegistration;
import dev.sixik.unigui.api.screen.UniRenderTarget;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.screen.UniScreenOverlayRender;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.cleanroom.backend.CleanroomClientBootstrap;
import dev.sixik.unigui.cleanroom.backend.CleanroomLoadingScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextField;
import net.minecraft.client.Minecraft;

/**
 * Manual checks of the runtime integration ({@code /unigui lab <name>}): HUD layer, screen overlay with a text field
 * and a pointer blocker, loading-screen layer, pause behaviour and render policies.
 */
public final class IntegrationLab {
    private static UniRenderLayerRegistration hud;
    private static UniRenderLayerRegistration overlay;
    private static UniRenderLayerRegistration loading;
    private static AutoCloseable blocker;

    private IntegrationLab() {
    }

    /** Names accepted by {@link #run}. */
    public static final String USAGE = "hud|overlay|loading|pause|nopause|policy";

    /**
     * Runs one lab on the client thread.
     *
     * @return a line for the chat describing what to look at
     */
    public static String run(String name) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return switch (name) {
            case "hud" -> {
                minecraft.addScheduledTask(IntegrationLab::toggleHud);
                yield "HUD layer toggled: a status panel in the top-left corner of the world view; hidden with F1";
            }
            case "overlay" -> {
                minecraft.addScheduledTask(IntegrationLab::toggleOverlay);
                yield "overlay toggled: open an inventory or the pause menu; type in the field (E and digits must not "
                        + "act), the slot under the panel must not highlight";
            }
            case "loading" -> {
                minecraft.addScheduledTask(IntegrationLab::toggleLoading);
                yield "loading layer toggled: join or create a world and watch the bar over 'Loading level'";
            }
            case "pause" -> {
                minecraft.addScheduledTask(() -> openClock(true));
                yield "screen that pauses the game: the world tick counter must stand still in single player";
            }
            case "nopause" -> {
                minecraft.addScheduledTask(() -> openClock(false));
                yield "screen that does not pause: the world tick counter must keep running";
            }
            case "policy" -> {
                minecraft.addScheduledTask(IntegrationLab::openPolicy);
                yield "ON_DIRTY screen: 'redraws' grows only when you click the button or move the mouse over it";
            }
            default -> "unknown lab, use " + USAGE;
        };
    }

    private static void toggleHud() {
        if (hud != null && !hud.closed()) {
            hud.close();
            hud = null;
            return;
        }
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        StatusPanel panel = new StatusPanel("HUD layer");
        panel.layout(style -> style.size(210.0f, 54.0f).align(Alignment.START, Alignment.START)
                .margin(6.0f, 6.0f, 0.0f, 0.0f));
        root.addChild(panel);
        hud = UniHudRender.register(new UniRenderLayer(root));
    }

    private static void toggleOverlay() {
        if (overlay != null && !overlay.closed()) {
            overlay.close();
            overlay = null;
            closeBlocker();
            return;
        }
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));

        VBox panel = new VBox();
        panel.spacing(4.0f);
        panel.layout(style -> style.size(150.0f, 84.0f).align(Alignment.START, Alignment.CENTER)
                .margin(6.0f, 0.0f, 0.0f, 0.0f));
        panel.addChild(new Label("UniGUI overlay"));
        TextField field = new TextField();
        field.placeholder("type here");
        field.layout(style -> style.height(18.0f));
        panel.addChild(field);
        int[] clicks = new int[1];
        Button button = new Button("clicks: 0");
        button.layout(style -> style.height(18.0f));
        button.onClick(event -> button.text("clicks: " + (++clicks[0])));
        panel.addChild(button);
        root.addChild(panel);

        overlay = UniScreenOverlayRender.register(new UniRenderLayer(root));
        // The layout is 1 unit = 1 GUI pixel at the default scale, so the panel rectangle is also the screen one.
        blocker = CleanroomClientBootstrap.backend().addPointerBlocker((screen, x, y) ->
                x >= panel.layoutBounds().x() && x < panel.layoutBounds().x() + panel.layoutBounds().width()
                        && y >= panel.layoutBounds().y() && y < panel.layoutBounds().y() + panel.layoutBounds().height());
    }

    private static void closeBlocker() {
        if (blocker == null) return;
        try {
            blocker.close();
        } catch (Exception ignored) {
            // The registry removal cannot fail.
        }
        blocker = null;
    }

    private static void toggleLoading() {
        if (loading != null && !loading.closed()) {
            loading.close();
            loading = null;
            return;
        }
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        LoadingBar bar = new LoadingBar();
        bar.layout(style -> style.size(260.0f, 44.0f).align(Alignment.CENTER, Alignment.END)
                .margin(0.0f, 0.0f, 0.0f, 24.0f));
        root.addChild(bar);
        UniRenderLayer layer = new UniRenderLayer(root);
        loading = new UniRenderLayerRegistration(layer, UniGuiRuntime.backend().registerLayer(
                layer, UniRenderTarget.LOADING_SCREEN, ignored -> true, 0));
    }

    private static void openClock(boolean pause) {
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        StatusPanel panel = new StatusPanel(pause ? "pauseGame = true" : "pauseGame = false");
        panel.layout(style -> style.size(210.0f, 54.0f).align(Alignment.CENTER, Alignment.CENTER));
        root.addChild(panel);
        new UniScreen("Pause lab (Esc closes)", root, new DefaultUIContext(new MemoryClipboardService()))
                .pauseGame(pause).openGui();
    }

    private static void openPolicy() {
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        VBox column = new VBox();
        column.spacing(6.0f);
        column.layout(style -> style.align(Alignment.CENTER, Alignment.CENTER));
        RedrawCounter counter = new RedrawCounter();
        counter.layout(style -> style.size(160.0f, 18.0f));
        column.addChild(counter);
        int[] clicks = new int[1];
        Button button = new Button("clicks: 0");
        button.layout(style -> style.size(160.0f, 20.0f));
        button.onClick(event -> button.text("clicks: " + (++clicks[0])));
        column.addChild(button);
        root.addChild(column);
        new UniScreen("Render policy lab (ON_DIRTY)", root, new DefaultUIContext(new MemoryClipboardService()))
                .renderPolicy(UiRenderPolicy.onDirty()).openGui();
    }

    /** Draws a frame with a title and the live loading / world state. */
    private static final class StatusPanel extends WidgetBase {
        private final String title;

        private StatusPanel(String title) {
            this.title = title;
        }

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 210.0f, 54.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            var bounds = layoutBounds();
            context.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.fill(MutableColor.rgba(0.05f, 0.08f, 0.15f, 0.8f)));
            context.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.stroke(MutableColor.rgba(0.4f, 0.8f, 1.0f, 1.0f), 1.0f));
            Minecraft minecraft = Minecraft.getMinecraft();
            String world = minecraft.world == null ? "no world" : "world ticks: " + minecraft.world.getTotalWorldTime();
            CleanroomLoadingScreen.Context loadingState = CleanroomLoadingScreen.context();
            String loadingLine = loadingState == null ? "not loading" : loadingState.kind().name();
            Paint white = Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f));
            context.text(title, bounds.x() + 6.0f, bounds.y() + 4.0f, bounds.width() - 12.0f, 12.0f, white);
            context.text(world, bounds.x() + 6.0f, bounds.y() + 18.0f, bounds.width() - 12.0f, 12.0f, white);
            context.text(loadingLine, bounds.x() + 6.0f, bounds.y() + 32.0f, bounds.width() - 12.0f, 12.0f, white);
        }
    }

    /** Progress bar for the loading screen: kind, message and a filled bar. */
    private static final class LoadingBar extends WidgetBase {
        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 260.0f, 44.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            CleanroomLoadingScreen.Context state = CleanroomLoadingScreen.context();
            var bounds = layoutBounds();
            context.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.fill(MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.75f)));
            context.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.stroke(MutableColor.rgba(1.0f, 0.8f, 0.2f, 1.0f), 1.0f));
            Paint white = Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f));
            String line = state == null ? "idle" : state.kind() + ": " + state.title() + " " + state.message();
            context.text(line, bounds.x() + 6.0f, bounds.y() + 5.0f, bounds.width() - 12.0f, 12.0f, white);
            float progress = state == null ? 0.0f : Math.max(0.0f, state.progress());
            context.rect(bounds.x() + 6.0f, bounds.y() + 24.0f, bounds.width() - 12.0f, 8.0f,
                    Paint.fill(MutableColor.rgba(0.2f, 0.2f, 0.2f, 1.0f)));
            context.rect(bounds.x() + 6.0f, bounds.y() + 24.0f, (bounds.width() - 12.0f) * progress, 8.0f,
                    Paint.fill(MutableColor.rgba(0.3f, 0.9f, 0.4f, 1.0f)));
        }
    }

    /** Counts how many times the widget was asked to draw itself. */
    private static final class RedrawCounter extends WidgetBase {
        private int redraws;

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 160.0f, 18.0f));
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            redraws++;
            var bounds = layoutBounds();
            context.text("redraws: " + redraws, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));
        }
    }
}
