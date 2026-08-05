package dev.sixik.unigui.api.render;

import java.util.Objects;

public final class RenderTargetOptions {
    public static final RenderTargetOptions COLOR = new RenderTargetOptions(false, true, "color");
    public static final RenderTargetOptions COLOR_DEPTH = new RenderTargetOptions(true, true, "color_depth");

    private final boolean useDepth;
    private final boolean clearBeforeRender;
    private final String debugName;

    public RenderTargetOptions(boolean useDepth, boolean clearBeforeRender, String debugName) {
        this.useDepth = useDepth;
        this.clearBeforeRender = clearBeforeRender;
        this.debugName = debugName == null || debugName.isBlank() ? "render_target" : debugName;
    }

    public boolean useDepth() {
        return useDepth;
    }

    public boolean clearBeforeRender() {
        return clearBeforeRender;
    }

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
