package dev.sixik.unigui.api.render;

import java.util.Objects;

/**
 * Immutable параметры создания offscreen render target'а.
 *
 * <p>Options описывают, нужен ли depth buffer, очищать ли target перед рендером и какое debug name
 * показать в backend tools. Равенство учитывает все поля, поэтому options можно использовать как ключ
 * в cache render target'ов.</p>
 */
public final class RenderTargetOptions {
    /** Color-only target без depth buffer. */
    public static final RenderTargetOptions COLOR = new RenderTargetOptions(false, true, "color");

    /** Target с color и depth buffer. */
    public static final RenderTargetOptions COLOR_DEPTH = new RenderTargetOptions(true, true, "color_depth");

    private final boolean useDepth;
    private final boolean clearBeforeRender;
    private final String debugName;

    /**
     * Создаёт options target'а.
     *
     * @param useDepth нужен ли depth buffer
     * @param clearBeforeRender очищать ли target перед каждым render pass
     * @param debugName имя для diagnostics/backend tooling
     */
    public RenderTargetOptions(boolean useDepth, boolean clearBeforeRender, String debugName) {
        this.useDepth = useDepth;
        this.clearBeforeRender = clearBeforeRender;
        this.debugName = debugName == null || debugName.isBlank() ? "render_target" : debugName;
    }

    /** @return {@code true}, если target должен иметь depth buffer */
    public boolean useDepth() {
        return useDepth;
    }

    /** @return {@code true}, если backend должен очистить target перед render pass */
    public boolean clearBeforeRender() {
        return clearBeforeRender;
    }

    /** @return debug name target'а */
    public String debugName() {
        return debugName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof RenderTargetOptions that)) return false;
        return useDepth == that.useDepth
                && clearBeforeRender == that.clearBeforeRender
                && Objects.equals(debugName, that.debugName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useDepth, clearBeforeRender, debugName);
    }
}