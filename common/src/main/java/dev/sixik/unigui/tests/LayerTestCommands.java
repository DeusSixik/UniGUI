package dev.sixik.unigui.tests;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.HudRender;
import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenContext;
import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenRender;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftRenderLayerRegistration;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Widgets;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftWidgets;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Тестовые команды для слоёв вне экранов UniGUI: HUD, overlay поверх любого экрана и экран загрузки.
 *
 * <ul>
 *     <li>{@code /unigui hud} — включает/выключает HUD-слой (живой текст, анимация, ванильные предметы,
 *     в том числе полупрозрачный).</li>
 *     <li>{@code /unigui overlay} — включает/выключает слой поверх любого Minecraft Screen (кнопка-счётчик
 *     для проверки ввода и предмет). Слой виден, например, в инвентаре или меню паузы.</li>
 *     <li>{@code /unigui loading} — регистрирует слой экрана загрузки и запускает перезагрузку ресурсов
 *     (как F3+T), чтобы его увидеть. Повторный вызов снова запускает перезагрузку.</li>
 *     <li>{@code /unigui loading off} — убирает слой экрана загрузки.</li>
 *     <li>{@code /unigui layers clear} — убирает все три слоя.</li>
 * </ul>
 *
 * <p>Пока слой экрана загрузки зарегистрирован, vanilla {@code LoadingOverlay} не снимается сам: слой снимает
 * его через {@link LoadingScreenRender#releaseOverlay()} после своей анимации исчезновения.</p>
 */
public final class LayerTestCommands {
    private static MinecraftRenderLayerRegistration<?> hud;
    private static MinecraftRenderLayerRegistration<?> overlay;
    private static MinecraftRenderLayerRegistration<?> loading;
    private static final long[] HUD_STARTED_AT = {0L};

    private LayerTestCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unigui")
                .then(Commands.literal("hud").executes(ctx -> run(ctx, LayerTestCommands::toggleHud)))
                .then(Commands.literal("overlay").executes(ctx -> run(ctx, LayerTestCommands::toggleOverlay)))
                .then(Commands.literal("loading")
                        .executes(ctx -> run(ctx, LayerTestCommands::showLoading))
                        .then(Commands.literal("off").executes(ctx -> run(ctx, LayerTestCommands::closeLoading))))
                .then(Commands.literal("layers")
                        .then(Commands.literal("clear").executes(ctx -> run(ctx, LayerTestCommands::clearAll)))));
    }

    private static int run(CommandContext<CommandSourceStack> context, java.util.function.Supplier<String> action) {
        // Слои живут в клиентском потоке рендера.
        CommandSourceStack source = context.getSource();
        Minecraft.getInstance().execute(() -> source.sendSystemMessage(Component.literal("[UniGUI] " + action.get())));
        return 1;
    }

    /**
     * Включает слой по имени без команды (для отладочного захвата кадра): {@code hud}, {@code overlay},
     * {@code loading}.
     *
     * @param name имя слоя
     * @return сообщение, как у команды
     */
    public static String enable(String name) {
        return switch (name) {
            case "hud" -> isOpen(hud) ? "HUD already on" : toggleHud();
            case "overlay" -> isOpen(overlay) ? "overlay already on" : toggleOverlay();
            case "loading" -> showLoading();
            default -> throw new IllegalArgumentException("Unknown layer " + name);
        };
    }

    // ---------------------------------------------------------------- HUD

    private static String toggleHud() {
        if (isOpen(hud)) {
            hud.close();
            hud = null;
            return "HUD layer removed";
        }
        HUD_STARTED_AT[0] = System.currentTimeMillis();
        hud = HudRender.register(transparentRoot(hudPanel()));
        hud.layer().defaultFont(Fonts.defaultFace());
        return "HUD layer added (bottom right)";
    }

    private static Widget hudPanel() {
        Box panel = panel(0.025f, 0.035f, 0.050f, 0.88f);
        panel.layout(style -> style.position(PositionType.ABSOLUTE).right(8.0f).bottom(8.0f)
                .padding(6.0f).flexGrow(0.0f).flexShrink(0.0f));

        VBox content = new VBox();
        content.layout(style -> style.gap(4.0f));

        Label title = label("UNIGUI HUD", 0.45f, 0.90f, 1.0f);
        Widget stats = new LiveText(() -> {
            long seconds = (System.currentTimeMillis() - HUD_STARTED_AT[0]) / 1000L;
            return "FPS " + Minecraft.getInstance().getFps() + "  uptime " + seconds + " s";
        }, 150.0f);

        HBox items = new HBox();
        items.layout(style -> style.gap(4.0f));
        items.addChild(item("diamond", new ItemStack(Items.DIAMOND), 1.0f));
        items.addChild(item("64", new ItemStack(Items.IRON_INGOT, 64), 1.0f));
        items.addChild(item("40%", new ItemStack(Items.EMERALD), 0.4f));
        items.addChild(item("glint", new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1.0f));

        Box pulse = panel(0.20f, 0.78f, 0.92f, 0.9f);
        pulse.layout(style -> style.width(150.0f).height(3.0f));
        pulse.animateOpacity(0.2f, dev.sixik.unigui.api.animation.TransitionSpec
                .of(0.9f, dev.sixik.unigui.api.animation.AnimationEasing.EASE_IN_OUT).loop().yoyo());

        content.addChild(title);
        content.addChild(stats);
        content.addChild(items);
        content.addChild(pulse);
        panel.addChild(content);
        return panel;
    }

    // ---------------------------------------------------------------- Screen overlay

    private static String toggleOverlay() {
        if (isOpen(overlay)) {
            overlay.close();
            overlay = null;
            return "Screen overlay removed";
        }
        overlay = ScreenOverlayRender.register(transparentRoot(overlayPanel()));
        overlay.layer().defaultFont(Fonts.defaultFace());
        return "Screen overlay added (top left of any screen: open the inventory or press Esc)";
    }

    private static Widget overlayPanel() {
        Box panel = panel(0.025f, 0.035f, 0.050f, 0.94f);
        panel.layout(style -> style.position(PositionType.ABSOLUTE).left(8.0f).top(8.0f)
                .padding(6.0f).flexGrow(0.0f).flexShrink(0.0f));

        VBox content = new VBox();
        content.layout(style -> style.gap(4.0f));

        int[] clicks = {0};
        Label counter = label("Clicks: 0", 0.78f, 0.86f, 0.92f);
        Button button = Widgets.button("Click me");
        button.layout(style -> style.width(120.0f).height(18.0f));
        button.onClick(event -> counter.text("Clicks: " + (++clicks[0])));

        HBox items = new HBox();
        items.layout(style -> style.gap(4.0f));
        items.addChild(item("apple", new ItemStack(Items.APPLE), 1.0f));
        items.addChild(item("50%", new ItemStack(Items.GOLDEN_APPLE), 0.5f));

        content.addChild(label("SCREEN OVERLAY", 0.45f, 0.90f, 1.0f));
        content.addChild(button);
        content.addChild(counter);
        content.addChild(items);
        panel.addChild(content);
        return panel;
    }

    // ---------------------------------------------------------------- Loading screen

    private static String showLoading() {
        if (!isOpen(loading)) {
            loading = LoadingScreenRender.register(loadingRoot());
            loading.layer().defaultFont(Fonts.defaultFace());
        }
        Minecraft.getInstance().reloadResourcePacks();
        return "Loading screen layer registered; reloading resources";
    }

    private static String closeLoading() {
        if (!isOpen(loading)) return "Loading screen layer is not registered";
        loading.close();
        loading = null;
        return "Loading screen layer removed";
    }

    private static Widget loadingRoot() {
        Box root = new LoadingRoot();
        root.themeEnabled(false);
        root.backgroundVisible(true);
        root.borderVisible(false);
        root.background().set(0.008f, 0.012f, 0.018f, 1.0f);
        root.layout(style -> style.sizePercent(100.0f, 100.0f)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER));

        Box panel = panel(0.025f, 0.045f, 0.060f, 0.98f);
        panel.borderVisible(true);
        panel.borderColor().set(0.16f, 0.62f, 0.76f, 0.90f);
        panel.layout(style -> style.size(300.0f, 90.0f).align(Alignment.CENTER, Alignment.CENTER));

        VBox content = new VBox();
        content.layout(style -> style.sizePercent(100.0f, 100.0f)
                .padding(14.0f)
                .gap(6.0f)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER));

        Label title = label("UNIGUI LOADING", 0.45f, 0.90f, 1.0f);
        title.textAlignment(Alignment.CENTER);
        title.layout(style -> style.width(220.0f).height(12.0f).align(Alignment.CENTER, Alignment.CENTER));
        Widget status = new LiveText(() -> {
            LoadingScreenContext context = LoadingScreenRender.context();
            if (context == null) return "";
            return context.done() ? "DONE" : "RELOADING RESOURCES " + Math.round(context.progress() * 100.0f) + "%";
        }, 220.0f);
        ((LiveText) status).centered();

        ProgressBar progress = new LoadingProgressBar();
        progress.themeEnabled(false);
        progress.trackColor().set(0.08f, 0.16f, 0.20f, 1.0f);
        progress.fillColor().set(0.20f, 0.78f, 0.92f, 0.90f);
        progress.layout(style -> style.width(220.0f).height(3.0f).align(Alignment.CENTER, Alignment.CENTER));

        content.addChild(title);
        content.addChild(status);
        content.addChild(progress);
        panel.addChild(content);
        root.addChild(panel);
        return root;
    }

    /** Полоса прогресса, привязанная к прогрессу перезагрузки ресурсов. */
    private static final class LoadingProgressBar extends ProgressBar {
        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            LoadingScreenContext context = LoadingScreenRender.context();
            if (context != null) value(context.progress());
        }
    }

    /**
     * Корень экрана загрузки: после окончания перезагрузки плавно исчезает и снимает vanilla overlay.
     * Состояние сбрасывается для каждого нового {@code LoadingOverlay}, поэтому слой переживает повторные загрузки.
     */
    private static final class LoadingRoot extends Box {
        private static final long HOLD_MILLIS = 600L;
        private static final float FADE_SECONDS = 0.8f;
        private static final long FADE_MILLIS = 800L;
        private Object overlay;
        private long doneAt;
        private long fadeStartedAt;

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            LoadingScreenContext context = LoadingScreenRender.context();
            if (context == null) return;
            long now = System.currentTimeMillis();
            if (context.overlay() != overlay) {
                overlay = context.overlay();
                doneAt = -1L;
                fadeStartedAt = -1L;
                opacity(1.0f);
            }
            if (!context.done()) return;
            if (doneAt < 0L) doneAt = now;
            if (fadeStartedAt < 0L && now - doneAt >= HOLD_MILLIS) {
                fadeStartedAt = now;
                animateOpacity(0.0f, FADE_SECONDS);
            }
            if (fadeStartedAt >= 0L && now - fadeStartedAt >= FADE_MILLIS) {
                LoadingScreenRender.releaseOverlay();
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String clearAll() {
        if (isOpen(hud)) hud.close();
        if (isOpen(overlay)) overlay.close();
        if (isOpen(loading)) loading.close();
        hud = null;
        overlay = null;
        loading = null;
        return "All test layers removed";
    }

    private static boolean isOpen(MinecraftRenderLayerRegistration<?> registration) {
        return registration != null && !registration.closed();
    }

    private static Box transparentRoot(Widget child) {
        Box root = new Box();
        root.themeEnabled(false);
        root.backgroundVisible(false);
        root.borderVisible(false);
        root.addChild(child);
        return root;
    }

    private static Box panel(float r, float g, float b, float a) {
        Box panel = new Box();
        panel.themeEnabled(false);
        panel.backgroundVisible(true);
        panel.borderVisible(false);
        panel.radius(4.0f);
        panel.background().set(r, g, b, a);
        return panel;
    }

    private static Label label(String text, float r, float g, float b) {
        Label label = new Label(text);
        label.color().set(r, g, b, 1.0f);
        label.layout(style -> style.width(150.0f).height(12.0f));
        return label;
    }

    private static MinecraftItemPreviewWidget item(String label, ItemStack stack, float opacity) {
        MinecraftItemPreviewWidget preview = MinecraftWidgets.itemPreview(label, stack);
        preview.previewSize(20.0f);
        if (opacity < 1.0f) preview.opacity(opacity);
        return preview;
    }

    /** Подпись, текст которой обновляется каждый кадр ({@link Label} финальный, поэтому обёртка). */
    private static final class LiveText extends Box {
        private final java.util.function.Supplier<String> source;
        private final Label label;
        private String last = "";

        private LiveText(java.util.function.Supplier<String> source, float width) {
            this.source = source;
            themeEnabled(false);
            backgroundVisible(false);
            borderVisible(false);
            label = new Label("");
            label.color().set(0.78f, 0.86f, 0.92f, 1.0f);
            label.layout(style -> style.width(width).height(12.0f));
            layout(style -> style.width(width).height(12.0f).align(Alignment.CENTER, Alignment.CENTER));
            addChild(label);
        }

        void centered() {
            label.textAlignment(Alignment.CENTER);
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            String next = source.get();
            if (!next.equals(last)) {
                last = next;
                label.text(next);
            }
        }
    }
}
