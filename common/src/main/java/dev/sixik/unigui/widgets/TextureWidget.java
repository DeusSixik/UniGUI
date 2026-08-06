package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.impl.widget.WidgetBase;

public class TextureWidget extends WidgetBase {
    private TextureHandle texture;
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);

    public TextureWidget() {
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
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

    @Override
    public void render(RenderContext context) {
        if (texture == null) return;
        pushOpacity(context);
        try {
            context.texture(texture,
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    Paint.fill(tint),
                    transform());
        } finally {
            popOpacity(context);
        }
    }
}
