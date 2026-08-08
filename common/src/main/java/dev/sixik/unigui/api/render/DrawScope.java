package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.text.RichText;

import java.util.Objects;

/**
 * Safe draw-list-style facade for widget renderers.
 *
 * <p>DrawScope intentionally exposes backend-neutral primitives instead of the
 * active RenderBackend. Custom widget renderers can use it like a small
 * ImGui-style draw list while UniGUI still records normal DrawCommands.</p>
 */
public final class DrawScope {
    private final RenderContext context;
    private final Transform transform;

    public DrawScope(RenderContext context, Transform transform) {
        this.context = Objects.requireNonNull(context, "context");
        this.transform = transform;
    }

    public DrawScope withTransform(Transform transform) {
        return new DrawScope(context, transform);
    }

    public void rect(float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.rect(x, y, width, height, paint);
        } else {
            context.rect(x, y, width, height, paint, transform);
        }
    }

    public void roundedRect(float x, float y, float width, float height, float radius, Paint paint) {
        if (transform == null) {
            context.roundedRect(x, y, width, height, radius, paint);
        } else {
            context.roundedRect(x, y, width, height, radius, paint, transform);
        }
    }

    public void circle(float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.circle(x, y, width, height, paint);
        } else {
            context.circle(x, y, width, height, paint, transform);
        }
    }

    public void line(float x1, float y1, float x2, float y2, Paint paint) {
        if (transform == null) {
            context.line(x1, y1, x2, y2, paint);
        } else {
            context.line(x1, y1, x2, y2, paint, transform);
        }
    }

    public void path(VectorPath path, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.path(path, x, y, width, height, paint);
        } else {
            context.path(path, x, y, width, height, paint, transform);
        }
    }

    public void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.texture(texture, x, y, width, height, paint);
        } else {
            context.texture(texture, x, y, width, height, paint, transform);
        }
    }

    public void texture(TextureHandle texture, TexturePlacement placement, float radius, Paint paint) {
        if (transform == null) {
            context.texture(texture, placement, radius, paint);
        } else {
            context.texture(texture, placement, radius, paint, transform);
        }
    }

    public void text(String text, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.text(text, x, y, width, height, paint);
        } else {
            context.text(text, x, y, width, height, paint, transform);
        }
    }

    public void text(RichText text, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.text(text, x, y, width, height, paint);
        } else {
            context.text(text, x, y, width, height, paint, transform);
        }
    }

    public void pushClip(float x, float y, float width, float height) {
        context.pushClip(x, y, width, height);
    }

    public void popClip() {
        context.popClip();
    }
}
