package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenContext;
import dev.sixik.unigui.backend.minecraft_impl.LoadingScreenRender;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftRenderLayerRegistration;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import net.minecraft.Util;

/**
 * Демонстрация кастомного экрана загрузки через UniGUI.
 *
 * <p>Пример регистрируется до первого экрана Minecraft. При загрузке UniGUI
 * рисуется после vanilla {@code LoadingOverlay}, поэтому стандартный reload
 * продолжает выполняться, а полноэкранный корневой {@link Box} перекрывает
 * стандартную визуальную часть.</p>
 */
public final class LoadingScreenDemo {
    private static MinecraftRenderLayerRegistration<?> registration;

    private LoadingScreenDemo() {
    }

    /** Регистрирует демонстрационный загрузочный экран один раз. */
    public static void register() {
        if (registration != null && !registration.closed()) return;
        registration = LoadingScreenRender.register(createRoot());
        registration.layer().defaultFont(Fonts.defaultFace());
    }

    /** Отключает демонстрационный загрузочный экран и освобождает его ресурсы. */
    public static void close() {
        if (registration == null) return;
        registration.close();
        registration = null;
    }

    private static Widget createRoot() {
        Box root = new LoadingRoot();
        root.themeEnabled(false);
        root.backgroundVisible(true);
        root.borderVisible(false);
        root.background().set(0.008f, 0.012f, 0.018f, 1.0f);
        root.layout(style -> style.sizePercent(100.0f, 100.0f)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER));

        Box panel = new Box();
        panel.themeEnabled(false);
        panel.backgroundVisible(true);
        panel.borderVisible(true);
        panel.radius(4.0f);
        panel.background().set(0.025f, 0.045f, 0.060f, 0.98f);
        panel.borderColor().set(0.16f, 0.62f, 0.76f, 0.90f);
        panel.layout(style -> style.size(420.0f, 132.0f)
                .align(Alignment.CENTER, Alignment.CENTER));

        VBox content = new VBox();
        content.layout(style -> style.sizePercent(100.0f, 100.0f)
                .padding(18.0f)
                .gap(6.0f)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.CENTER));

        Label title = new Label("UNIGUI");
        title.color().set(0.45f, 0.90f, 1.0f, 1.0f);
        title.layout(style -> style.width(260.0f).height(22.0f)
                .align(Alignment.CENTER, Alignment.CENTER));

        Label status = new Label("INITIALIZING CLIENT");
        status.color().set(0.72f, 0.82f, 0.88f, 1.0f);
        status.layout(style -> style.width(260.0f).height(18.0f)
                .align(Alignment.CENTER, Alignment.CENTER));

        ProgressBar progress = new LoadingProgressBar();
        progress.themeEnabled(false);
        progress.trackColor().set(0.08f, 0.16f, 0.20f, 1.0f);
        progress.fillColor().set(0.20f, 0.78f, 0.92f, 0.90f);
        progress.layout(style -> style.width(260.0f).height(3.0f)
                .align(Alignment.CENTER, Alignment.CENTER));

        content.addChild(title);
        content.addChild(status);
        content.addChild(progress);
        panel.addChild(content);
        root.addChild(panel);
        return root;
    }

    /**
     * ProgressBar, связанный с фактическим прогрессом resource reload Minecraft.
     *
     * <p>Контекст может быть недоступен в первом или последнем кадре overlay. В
     * таком случае сохраняется последнее полученное значение, чтобы индикатор не
     * сбрасывался визуально.</p>
     */
    private static final class LoadingProgressBar extends ProgressBar {
        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);

            LoadingScreenContext context = LoadingScreenRender.context();
            if (context != null) {
                value(context.progress());
            }
        }
    }

    /** Корневой виджет, который плавно скрывает экран после завершения загрузки. */
    private static final class LoadingRoot extends Box {
        private static final long VANILLA_FADE_IN_MILLIS = 1_000L;
        private static final long VANILLA_FADE_HOLD_MILLIS = 1_000L;
        private static final float FADE_OUT_SECONDS = 5.0f;
        private static final long FADE_OUT_MILLIS = 5_000L;
        private long firstRenderAt = -1L;
        private long vanillaFadeOutStartedAt = -1L;
        private long fadeOutStartedAt = -1L;
        private boolean fadeOutStarted;
        private boolean overlayReleased;

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);

            LoadingScreenContext context = LoadingScreenRender.context();
            if (context == null) return;

            long now = Util.getMillis();
            if (firstRenderAt < 0L) {
                firstRenderAt = now;
            }

            // LoadingOverlay ждёт окончания fade-in перед запуском fade-out.
            if (!context.done()
                    || (context.fadeIn() && now - firstRenderAt < VANILLA_FADE_IN_MILLIS)) {
                return;
            }

            if (vanillaFadeOutStartedAt < 0L) {
                vanillaFadeOutStartedAt = now;
            }

            // В течение первой секунды vanilla overlay ещё полностью непрозрачен.
            if (!fadeOutStarted
                    && now - vanillaFadeOutStartedAt >= VANILLA_FADE_HOLD_MILLIS) {
                fadeOutStarted = true;
                fadeOutStartedAt = now;
                animateOpacity(0.0f, FADE_OUT_SECONDS);
            }

            if (fadeOutStarted
                    && !overlayReleased
                    && now - fadeOutStartedAt >= FADE_OUT_MILLIS) {
                overlayReleased = true;
                LoadingScreenRender.releaseOverlay();
            }
        }
    }
}
