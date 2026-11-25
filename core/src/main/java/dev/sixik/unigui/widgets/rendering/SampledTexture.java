package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

import java.util.Objects;

/**
 * Текстура offscreen-цели с выбранной фильтрацией.
 *
 * <p>Backend'ы привязывают текстуру по {@link #nativeHandle()}, а фильтрацию берут из {@link #options()}, поэтому
 * обёртка меняет только опции и передаёт остальное исходной текстуре цели.</p>
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
     * @return исходная текстура цели
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
