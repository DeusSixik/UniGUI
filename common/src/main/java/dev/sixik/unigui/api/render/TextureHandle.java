package dev.sixik.unigui.api.render;

public interface TextureHandle {
    String id();

    int width();

    int height();

    default TextureOptions options() {
        return TextureOptions.defaults();
    }

    default Object nativeHandle() {
        return null;
    }
}
