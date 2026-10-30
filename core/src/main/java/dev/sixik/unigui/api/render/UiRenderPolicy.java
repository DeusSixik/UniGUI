package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Политика частоты пересборки и рендера retained UI.
 *
 * <p>Policy позволяет runtime не перерисовывать UI каждый кадр, если экран статичен, или наоборот
 * держать непрерывный render для анимаций, shader effects и editor preview.</p>
 */
@AiDoc(
        summary = "Frame-rate and invalidation policy controlling how frequently retained UI trees are rebuilt and rendered.",
        purpose = "Minimizes unnecessary CPU and GPU rendering overhead by switching between continuous rendering, on-dirty invalidation, fixed FPS, or display VSYNC.",
        useWhen = {
                "Configuring render refresh behavior on Window, Screen, or root UI container.",
                "Optimizing static HUDs or dialogs to only render when invalidated (onDirty).",
                "Locking animations or editor viewports to VSYNC or a specific target FPS."
        },
        criticalRules = {
                "In ON_DIRTY mode, the UI will not redraw unless an explicit dirty/invalidation call occurs.",
                "fixedFps requires a positive finite float; non-positive values throw IllegalArgumentException."
        },
        related = {"RenderContext", "DrawScope", "DrawList"}
)
@WikiPage(
        title = "<key:ui.render.policy.title>",
        slug = "ui-render-policy",
        category = "<key:ui.render.policy.category>",
        summary = "<key:ui.render.policy.summary>",
        order = 15)
@WikiSection(
        title = "<key:ui.render.policy.section.overview.title>",
        markdown = "<key:ui.render.policy.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:ui.render.policy.section.modes.title>",
        markdown = "<key:ui.render.policy.section.modes.body>",
        order = 20)
@WikiSection(
        title = "<key:ui.render.policy.section.power.title>",
        markdown = "<key:ui.render.policy.section.power.body>",
        order = 30)
@WikiExample(
        title = "<key:ui.render.policy.example.dirty.title>",
        code = "<key:ui.render.policy.example.dirty.code>",
        order = 10)
@WikiExample(
        title = "<key:ui.render.policy.example.vsync.title>",
        code = "<key:ui.render.policy.example.vsync.code>",
        order = 20)
public final class UiRenderPolicy {
    /** Режим работы render policy. */
    @AiDoc(
            summary = "Operational modes for UiRenderPolicy.",
            purpose = "Enumerates redraw triggers: continuous per-frame, on-dirty signal, fixed FPS interval, or display VSYNC.",
            useWhen = {"Inspecting the active mode of a UiRenderPolicy instance."},
            related = {"UiRenderPolicy"}
    )
    public enum Mode {
        /** UI перестраивается и рендерится каждый кадр. */
        CONTINUOUS,
        /** UI рендерится только после invalidation/dirty signal. */
        ON_DIRTY,
        /** UI рендерится с фиксированной частотой. */
        FIXED_FPS,
        /** UI ориентируется на refresh rate окна/backend'а. */
        VSYNC
    }

    private static final UiRenderPolicy CONTINUOUS = new UiRenderPolicy(Mode.CONTINUOUS, 0.0f);
    private static final UiRenderPolicy ON_DIRTY = new UiRenderPolicy(Mode.ON_DIRTY, 0.0f);
    private static final UiRenderPolicy VSYNC = new UiRenderPolicy(Mode.VSYNC, 0.0f);

    private final Mode mode;
    private final float fps;

    private UiRenderPolicy(Mode mode, float fps) {
        this.mode = mode;
        this.fps = fps;
    }

    /** @return policy непрерывного рендера */
    @WikiMethod(
            title = "<key:ui.render.policy.method.continuous.title>",
            summary = "<key:ui.render.policy.method.continuous.summary>",
            order = 10)
    @WikiReturn("<key:ui.render.policy.method.continuous.return>")
    public static UiRenderPolicy continuous() {
        return CONTINUOUS;
    }

    /** @return policy рендера только по dirty/invalidation signal */
    @WikiMethod(
            title = "<key:ui.render.policy.method.on_dirty.title>",
            summary = "<key:ui.render.policy.method.on_dirty.summary>",
            order = 20)
    @WikiReturn("<key:ui.render.policy.method.on_dirty.return>")
    public static UiRenderPolicy onDirty() {
        return ON_DIRTY;
    }

    /**
     * Создаёт policy фиксированной частоты.
     *
     * @param fps частота кадров UI
     * @return policy с фиксированным fps
     */
    @WikiMethod(
            title = "<key:ui.render.policy.method.fixed_fps.title>",
            summary = "<key:ui.render.policy.method.fixed_fps.summary>",
            order = 30)
    @WikiReturn("<key:ui.render.policy.method.fixed_fps.return>")
    public static UiRenderPolicy fixedFps(
            @WikiParameter(name = "fps", description = "<key:ui.render.policy.method.fixed_fps.fps>")
            float fps) {
        if (!Float.isFinite(fps) || fps <= 0.0f) {
            throw new IllegalArgumentException("fps must be finite and greater than zero");
        }
        return new UiRenderPolicy(Mode.FIXED_FPS, fps);
    }

    /**
     * Использует текущую частоту обновления Minecraft/window как интервал UI render.
     *
     * @return policy, синхронизированная с vsync
     */
    @WikiMethod(
            title = "<key:ui.render.policy.method.vsync.title>",
            summary = "<key:ui.render.policy.method.vsync.summary>",
            order = 40)
    @WikiReturn("<key:ui.render.policy.method.vsync.return>")
    public static UiRenderPolicy vsync() {
        return VSYNC;
    }

    /** @return режим policy */
    @WikiMethod(
            title = "<key:ui.render.policy.method.mode.title>",
            summary = "<key:ui.render.policy.method.mode.summary>",
            order = 15)
    @WikiReturn("<key:ui.render.policy.method.mode.return>")
    public Mode mode() {
        return mode;
    }

    /** @return fps для {@link Mode#FIXED_FPS} или {@code 0.0f} */
    @WikiMethod(
            title = "<key:ui.render.policy.method.fps.title>",
            summary = "<key:ui.render.policy.method.fps.summary>",
            order = 60)
    @WikiReturn("<key:ui.render.policy.method.fps.return>")
    public float fps() {
        return fps;
    }

    /**
     * Возвращает интервал fixed-fps policy в наносекундах.
     *
     * @return интервал или {@code 0}, если режим не {@link Mode#FIXED_FPS}
     */
    @WikiMethod(
            title = "<key:ui.render.policy.method.interval_nanos.title>",
            summary = "<key:ui.render.policy.method.interval_nanos.summary>",
            order = 70)
    @WikiReturn("<key:ui.render.policy.method.interval_nanos.return>")
    public long intervalNanos() {
        return mode == Mode.FIXED_FPS
                ? Math.max(1L, Math.round(1_000_000_000.0 / fps))
                : 0L;
    }
}
