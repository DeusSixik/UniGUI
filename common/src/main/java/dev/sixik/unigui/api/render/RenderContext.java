package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;

public interface RenderContext {
    DrawList drawList();

    default RenderBackend backend() {
        return null;
    }

    default void rect(float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.rect(new MutableRect(x, y, width, height), paint));
    }

    default void rect(float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.rect(new MutableRect(x, y, width, height), paint).transform(transform));
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Paint paint) {
        drawList().add(new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(x, y, width, height))
                .radius(radius)
                .paint(paint));
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Paint paint, Transform transform) {
        drawList().add(new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(x, y, width, height))
                .radius(radius)
                .paint(paint)
                .transform(transform));
    }

    default void circle(float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.circle(new MutableRect(x, y, width, height), paint));
    }

    default void circle(float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.circle(new MutableRect(x, y, width, height), paint).transform(transform));
    }

    default void line(float x1, float y1, float x2, float y2, Paint paint) {
        drawList().add(DrawCommand.line(new MutableRect(x1, y1, x2 - x1, y2 - y1), paint));
    }

    default void line(float x1, float y1, float x2, float y2, Paint paint, Transform transform) {
        drawList().add(DrawCommand.line(new MutableRect(x1, y1, x2 - x1, y2 - y1), paint).transform(transform));
    }

    default void path(VectorPath path, float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.path(path, new MutableRect(x, y, width, height), paint));
    }

    default void path(VectorPath path, float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.path(path, new MutableRect(x, y, width, height), paint).transform(transform));
    }

    default void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.texture(texture, new MutableRect(x, y, width, height), paint));
    }

    default void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.texture(texture, new MutableRect(x, y, width, height), paint).transform(transform));
    }

    default void text(String text, float x, float y, float width, float height, Paint paint) {
        drawList().add(DrawCommand.text(text, new MutableRect(x, y, width, height), paint));
    }

    default void text(String text, float x, float y, float width, float height, Paint paint, Transform transform) {
        drawList().add(DrawCommand.text(text, new MutableRect(x, y, width, height), paint).transform(transform));
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
}
