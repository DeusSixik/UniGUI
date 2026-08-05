package dev.sixik.unigui.api.core;

public final class FrameContext {
    private final long frameIndex;
    private final float deltaSeconds;
    private final float partialTick;
    private final FramePhase phase;

    public FrameContext(long frameIndex, float deltaSeconds, float partialTick, FramePhase phase) {
        this.frameIndex = frameIndex;
        this.deltaSeconds = deltaSeconds;
        this.partialTick = partialTick;
        this.phase = phase;
    }

    public long frameIndex() {
        return frameIndex;
    }

    public float deltaSeconds() {
        return deltaSeconds;
    }

    public float partialTick() {
        return partialTick;
    }

    public FramePhase phase() {
        return phase;
    }
}
