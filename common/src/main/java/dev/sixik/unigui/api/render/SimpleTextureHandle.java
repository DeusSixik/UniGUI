package dev.sixik.unigui.api.render;

import java.util.Objects;

public final class SimpleTextureHandle implements TextureHandle {
    private final String id;
    private final int width;
    private final int height;
    private final Object nativeHandle;

    public SimpleTextureHandle(String id, int width, int height) {
        this(id, width, height, null);
    }

    public SimpleTextureHandle(String id, int width, int height, Object nativeHandle) {
        this.id = Objects.requireNonNull(id, "id");
        this.width = width;
        this.height = height;
        this.nativeHandle = nativeHandle;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public Object nativeHandle() {
        return nativeHandle;
    }
}
