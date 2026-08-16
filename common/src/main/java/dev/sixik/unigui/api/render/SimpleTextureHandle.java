package dev.sixik.unigui.api.render;

import java.util.Objects;

public final class SimpleTextureHandle implements TextureHandle {
    private final String id;
    private final int width;
    private final int height;
    private final Object nativeHandle;
    private final TextureOptions options;

    public SimpleTextureHandle(String id, int width, int height) {
        this(id, width, height, null);
    }

    public SimpleTextureHandle(String id, int width, int height, Object nativeHandle) {
        this(id, width, height, nativeHandle, TextureOptions.defaults());
    }

    public SimpleTextureHandle(String id, int width, int height, Object nativeHandle, TextureOptions options) {
        this.id = Objects.requireNonNull(id, "id");
        this.width = width;
        this.height = height;
        this.nativeHandle = nativeHandle;
        this.options = options == null ? TextureOptions.defaults() : options;
    }

    public SimpleTextureHandle withOptions(TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (this.options.equals(normalized)) return this;
        return new SimpleTextureHandle(id, width, height, nativeHandle, normalized);
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
    public TextureOptions options() {
        return options;
    }

    @Override
    public Object nativeHandle() {
        return nativeHandle;
    }
}
