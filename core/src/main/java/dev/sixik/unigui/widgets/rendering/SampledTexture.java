package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

import java.util.Objects;

/**
 * Offscreen target texture with a sampling filter override.
 *
 * <p>The backend binds {@link #nativeHandle()} and reads sampling options from {@link #options()}.
 * The wrapper forwards the other properties to the target texture.</p>
 */
final class SampledTexture implements TextureHandle {
    private final TextureHandle source;
    private final TextureOptions options;

    SampledTexture(TextureHandle source, TextureFilter filter) {
        this.source = Objects.requireNonNull(source, "source");
        TextureOptions base = source.options() == null ? TextureOptions.defaults() : source.options();
        this.options = base.sampling(filter);
    }

    /**
     * Reads the original texture.
     *
     * @return the target's source texture.
     */
    TextureHandle source() {
        return source;
    }

    @Override
    public String id() {
        return source.id();
    }

    @Override
    public int width() {
        return source.width();
    }

    @Override
    public int height() {
        return source.height();
    }

    @Override
    public TextureOptions options() {
        return options;
    }

    @Override
    public Object nativeHandle() {
        return source.nativeHandle();
    }
}
