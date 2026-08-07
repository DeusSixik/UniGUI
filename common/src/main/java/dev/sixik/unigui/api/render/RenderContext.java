package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.text.RichText;

public interface RenderContext {
    DrawList drawList();

    default RenderBackend backend() {
        return null;
    }

    default void pushOpacity(float opacity) {
    }

    default void popOpacity() {
    }

    default float opacityMultiplier() {
        return 1.0f;
    }

    default void rect(float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.rect(new MutableRect(x, y, width, height), effectivePaint(paint)));
    }

    default void rect(float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.rect(new MutableRect(x, y, width, height), effectivePaint(paint)).transform(transform));
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Paint paint) {
        drawList().add(new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(x, y, width, height))
                .radius(radius)
                .paint(effectivePaint(paint)));
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Paint paint, Transform transform) {
        drawList().add(new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(x, y, width, height))
                .radius(radius)
                .paint(effectivePaint(paint))
                .transform(transform));
    }

    default void circle(float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.circle(new MutableRect(x, y, width, height), effectivePaint(paint)));
    }

    default void circle(float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.circle(new MutableRect(x, y, width, height), effectivePaint(paint)).transform(transform));
    }

    default void line(float x1, float y1, float x2, float y2, Paint paint) {
        drawList().add(DrawCommand.line(new MutableRect(x1, y1, x2 - x1, y2 - y1), effectivePaint(paint)));
    }

    default void line(float x1, float y1, float x2, float y2, Paint paint, Transform transform) {
        drawList().add(DrawCommand.line(new MutableRect(x1, y1, x2 - x1, y2 - y1), effectivePaint(paint)).transform(transform));
    }

    default void path(VectorPath path, float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.path(path, new MutableRect(x, y, width, height), effectivePaint(paint)));
    }

    default void path(VectorPath path, float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.path(path, new MutableRect(x, y, width, height), effectivePaint(paint)).transform(transform));
    }

    default void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.texture(texture, new MutableRect(x, y, width, height), effectivePaint(paint)));
    }

    default void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.texture(texture, new MutableRect(x, y, width, height), effectivePaint(paint)).transform(transform));
    }

    default void texture(TextureHandle texture, TexturePlacement placement, float radius, Paint paint) {
        drawList().add(DrawCommand.texture(texture,
                        new MutableRect(placement.x(), placement.y(), placement.width(), placement.height()),
                        effectivePaint(paint))
                .uv(new MutableRect(placement.u(), placement.v(), placement.uWidth(), placement.vHeight()))
                .radius(radius));
    }

    default void texture(TextureHandle texture, TexturePlacement placement, float radius,
                         Paint paint, Transform transform) {
        drawList().add(DrawCommand.texture(texture,
                        new MutableRect(placement.x(), placement.y(), placement.width(), placement.height()),
                        effectivePaint(paint))
                .uv(new MutableRect(placement.u(), placement.v(), placement.uWidth(), placement.vHeight()))
                .radius(radius)
                .transform(transform));
    }

    default void text(String text, float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.text(text, new MutableRect(x, y, width, height), effectivePaint(paint)));
    }

    default void text(String text, float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.text(text, new MutableRect(x, y, width, height), effectivePaint(paint)).transform(transform));
    }

    default void text(RichText text, float x, float y, float width, float height, Paint paint) {
        drawList().add(new DrawCommand(DrawCommandType.TEXT)
                .richText(text)
                .bounds(new MutableRect(x, y, width, height))
                .paint(effectivePaint(paint)));
    }

    default void text(RichText text, float x, float y, float width, float height,
                      Paint paint, Transform transform) {
        drawList().add(new DrawCommand(DrawCommandType.TEXT)
                .richText(text)
                .bounds(new MutableRect(x, y, width, height))
                .paint(effectivePaint(paint))
                .transform(transform));
    }

    default void custom(CustomDraw customDraw) {
        drawList().add(DrawCommand.custom(customDraw));
    }

    default void pushClip(float x, float y, float width, float height) {
        drawList().add(DrawCommand.pushClip(new MutableRect(x, y, width, height)));
    }

    default void popClip() {
        drawList().add(DrawCommand.popClip());
    }

    default Paint effectivePaint(Paint paint) {
        Paint copy = paint == null ? new Paint() : paint.copy();
        float opacity = clamp01(opacityMultiplier());
        if (opacity < 0.999f) {
            copy.color().set(copy.color().r(), copy.color().g(), copy.color().b(), copy.color().a() * opacity);
        }
        return copy;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
