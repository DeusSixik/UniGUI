package dev.sixik.unigui.api.render;

public interface TextureHandle {
    String id();

    int width();

    int height();

    default Object nativeHandle() {
        return null;
    }
}
