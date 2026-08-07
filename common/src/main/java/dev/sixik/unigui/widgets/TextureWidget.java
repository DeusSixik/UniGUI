package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.impl.widget.WidgetBase;

public class TextureWidget extends WidgetBase {
    private TextureHandle texture;
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableRect source = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private ImageFit fit = ImageFit.STRETCH;
    private float radius;

    public TextureWidget() {
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        source.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextureWidget(TextureHandle texture) {
        this();
        this.texture = texture;
    }

    public TextureHandle texture() {
        return texture;
    }

    public TextureWidget texture(TextureHandle texture) {
        if (this.texture == texture) return this;
        this.texture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    public MutableRect source() {
        return source;
    }

    public TextureWidget source(float u, float v, float width, float height) {
        source.set(u, v, width, height);
        return this;
    }

    public ImageFit fit() {
        return fit;
    }

    public TextureWidget fit(ImageFit fit) {
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;
        if (this.fit == effectiveFit) return this;
        this.fit = effectiveFit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    public TextureWidget radius(float radius) {
        float effectiveRadius = Float.isFinite(radius) ? Math.max(0.0f, radius) : 0.0f;
        if (this.radius == effectiveRadius) return this;
        this.radius = effectiveRadius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        if (texture == null) return;
        pushOpacity(context);
        try {
            TexturePlacement placement = TexturePlacement.fit(texture, source, layoutBounds(), fit);
            context.texture(texture, placement, radius, Paint.fill(tint), transform());
        } finally {
            popOpacity(context);
        }
    }
}
