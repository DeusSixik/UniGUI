package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.text.RichText;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Высокоуровневый API записи draw-команд текущего UI кадра.
 *
 * <p>{@code RenderContext} является основной точкой, через которую виджеты и {@link DrawScope}
 * добавляют primitives в {@link DrawList}. Методы по умолчанию строят {@link DrawCommand}, применяют
 * opacity/text pixel snap/transform stack и оставляют backend'у только исполнение готового списка.</p>
 *
 * <p>Интерфейс содержит два слоя API: простые primitives ({@code rect}, {@code text}, {@code texture})
 * и ImGui-like helpers ({@code addRectFilled}, {@code pathLineTo}, {@code channelsSplit}). Backend
 * реализации обычно переопределяют только {@link #drawList()}, {@link #backend()} и состояние stack'ов.</p>
 */
public interface RenderContext {
    /** Полный круг в радианах, используется path helpers. */
    float TAU = (float) (Math.PI * 2.0);

    /**
     * Возвращает draw list текущего кадра.
     *
     * @return список команд, куда пишут виджеты
     */
    DrawList drawList();

    /**
     * Возвращает активный backend, если он доступен context'у.
     *
     * @return backend или {@code null}
     */
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

    /**
     * Временно включает или выключает pixel snap для text commands.
     *
     * @param enabled новое состояние pixel snap
     */
    default void pushTextPixelSnap(boolean enabled) {
    }

    default void popTextPixelSnap() {
    }

    default boolean textPixelSnapEnabled() {
        return true;
    }

    /**
     * Добавляет transform layer для потомков.
     *
     * @param bounds bounds виджета, которому принадлежит transform
     * @param transform transform слоя
     */
    default void pushTransform(RectView bounds, Transform transform) {
    }

    default void popTransform() {
    }

    default List<TransformLayer> transformStack() {
        return List.of();
    }

    /**
     * Отправляет команду в draw list с учётом текущего transform stack.
     *
     * @param command команда; {@code null} игнорируется
     */
    default void submit(DrawCommand command) {
        if (command == null) return;
        List<TransformLayer> stack = transformStack();
        if (stack == null || stack.isEmpty()) {
            drawList().add(command);
            return;
        }
        drawList().add(command.copy().prependTransformStack(stack));
    }

    /** Передаёт команду, созданную через DrawList.obtain(), без промежуточной копии. */
    default void submitOwned(DrawCommand command) {
        if (command == null) return;
        List<TransformLayer> stack = transformStack();
        if (stack != null && !stack.isEmpty()) {
            command.prependTransformStack(stack);
        }
        drawList().addOwned(command);
    }

    default void rect(float x, float y, float width, float height, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.RECT)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint)));
    }

    default void rect(float x, float y, float width, float height, Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.RECT)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .transform(transform));
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.ROUNDED_RECT)
                .bounds(x, y, width, height)
                .radius(radius)
                .paintOwned(effectivePaint(paint)));
    }

    default void roundedRect(float x, float y, float width, float height, float radius, Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.ROUNDED_RECT)
                .bounds(x, y, width, height)
                .radius(radius)
                .paintOwned(effectivePaint(paint))
                .transform(transform));
    }

    default void circle(float x, float y, float width, float height, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.CIRCLE)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint)));
    }

    default void circle(float x, float y, float width, float height, Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.CIRCLE)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .transform(transform));
    }

    default void line(float x1, float y1, float x2, float y2, Paint paint) {
        Paint effective = effectivePaint(paint);
        if (effective.dashed()) {
            emitDashedLine(x1, y1, x2, y2, effective, null);
            return;
        }
        submitOwned(drawList().obtain(DrawCommandType.LINE)
                .bounds(x1, y1, x2 - x1, y2 - y1)
                .paintOwned(effective));
    }

    default void line(float x1, float y1, float x2, float y2, Paint paint, Transform transform) {
        Paint effective = effectivePaint(paint);
        if (effective.dashed()) {
            emitDashedLine(x1, y1, x2, y2, effective, transform);
            return;
        }
        submitOwned(drawList().obtain(DrawCommandType.LINE)
                .bounds(x1, y1, x2 - x1, y2 - y1)
                .paintOwned(effective)
                .transform(transform));
    }

    default void path(VectorPath path, float x, float y, float width, float height, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.PATH)
                .path(path)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint)));
    }

    default void path(VectorPath path, float x, float y, float width, float height, Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.PATH)
                .path(path)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .transform(transform));
    }

    default void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.TEXTURE)
                .texture(texture)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint)));
    }

    default void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.TEXTURE)
                .texture(texture)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .transform(transform));
    }

    default void texture(TextureHandle texture, TexturePlacement placement, float radius, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.TEXTURE)
                .texture(texture)
                .bounds(placement.x(), placement.y(), placement.width(), placement.height())
                .paintOwned(effectivePaint(paint))
                .uv(placement.u(), placement.v(), placement.uWidth(), placement.vHeight())
                .radius(radius));
    }

    default void texture(TextureHandle texture, TexturePlacement placement, float radius,
                         Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.TEXTURE)
                .texture(texture)
                .bounds(placement.x(), placement.y(), placement.width(), placement.height())
                .paintOwned(effectivePaint(paint))
                .uv(placement.u(), placement.v(), placement.uWidth(), placement.vHeight())
                .radius(radius)
                .transform(transform));
    }

    default void shader(ShaderHandle shader, float x, float y, float width, float height,
                        ShaderUniforms uniforms) {
        shader(shader, x, y, width, height, uniforms, ShaderDrawOptions.defaults(), null);
    }

    default void shader(ShaderHandle shader, float x, float y, float width, float height,
                        ShaderUniforms uniforms, Transform transform) {
        shader(shader, x, y, width, height, uniforms, ShaderDrawOptions.defaults(), transform);
    }

    default void shader(ShaderHandle shader, float x, float y, float width, float height,
                        ShaderUniforms uniforms, ShaderDrawOptions options) {
        shader(shader, x, y, width, height, uniforms, options, null);
    }

    default void shader(ShaderHandle shader, float x, float y, float width, float height,
                        ShaderUniforms uniforms, ShaderDrawOptions options, Transform transform) {
        if (shader == null || width == 0.0f || height == 0.0f) return;
        DrawCommand command = drawList().obtain(DrawCommandType.SHADER)
                .shader(shader)
                .bounds(x, y, width, height)
                .shaderUniforms(uniforms)
                .shaderOptions(options);
        if (transform != null) {
            command.transform(transform);
        }
        submitOwned(command);
    }

    default void shader(String shaderResource, float x, float y, float width, float height,
                        ShaderUniforms uniforms) {
        shader(ShaderHandle.resource(shaderResource), x, y, width, height, uniforms);
    }
    default void text(String text, float x, float y, float width, float height, Paint paint) {
        submitOwned(drawList().obtain(DrawCommandType.TEXT)
                .text(text)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .textPixelSnap(textPixelSnapEnabled()));
    }

    default void text(String text, float x, float y, float width, float height, Paint paint, Transform transform) {
        submitOwned(drawList().obtain(DrawCommandType.TEXT)
                .text(text)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .transform(transform)
                .textPixelSnap(textPixelSnapEnabled()));
    }

    default void text(RichText text, float x, float y, float width, float height, Paint paint) {
        text(text, x, y, width, height, paint, true);
    }

    default void text(RichText text, float x, float y, float width, float height,
                      Paint paint, boolean textPixelSnap) {
        submitOwned(drawList().obtain(DrawCommandType.TEXT)
                .richText(text)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .textPixelSnap(textPixelSnap && textPixelSnapEnabled()));
    }

    default void text(RichText text, float x, float y, float width, float height,
                      Paint paint, Transform transform) {
        text(text, x, y, width, height, paint, transform, true);
    }

    default void text(RichText text, float x, float y, float width, float height,
                      Paint paint, Transform transform, boolean textPixelSnap) {
        submitOwned(drawList().obtain(DrawCommandType.TEXT)
                .richText(text)
                .bounds(x, y, width, height)
                .paintOwned(effectivePaint(paint))
                .transform(transform)
                .textPixelSnap(textPixelSnap && textPixelSnapEnabled()));
    }

    default void custom(CustomDraw customDraw) {
        submitOwned(drawList().obtain(DrawCommandType.CUSTOM).customDraw(customDraw));
    }

    default void addDrawCmd(DrawCommand command) {
        addDrawCmd(command, null);
    }

    default void addDrawCmd(DrawCommand command, Transform transform) {
        DrawCommand effective = command == null ? DrawCommand.drawCmd() : command.copy();
        if (transform != null) {
            effective.transform(transform);
        }
        submit(effective);
    }

    default void addCallback(CustomDraw callback) {
        custom(callback);
    }

    default void addLine(float x1, float y1, float x2, float y2, ColorView color, float thickness) {
        addLine(x1, y1, x2, y2, color, thickness, null);
    }

    default void addLine(float x1, float y1, float x2, float y2, ColorView color, float thickness, Transform transform) {
        if (transform == null) {
            line(x1, y1, x2, y2, Paint.stroke(color, thickness));
        } else {
            line(x1, y1, x2, y2, Paint.stroke(color, thickness), transform);
        }
    }

    default void addRect(float x, float y, float width, float height, ColorView color, float thickness) {
        addRect(x, y, width, height, color, thickness, null);
    }

    default void addRect(float x, float y, float width, float height, ColorView color, float thickness, Transform transform) {
        if (transform == null) {
            rect(x, y, width, height, Paint.stroke(color, thickness));
        } else {
            rect(x, y, width, height, Paint.stroke(color, thickness), transform);
        }
    }

    default void addRectFilled(float x, float y, float width, float height, ColorView color) {
        addRectFilled(x, y, width, height, color, null);
    }

    default void addRectFilled(float x, float y, float width, float height, ColorView color, Transform transform) {
        if (transform == null) {
            rect(x, y, width, height, Paint.fill(color));
        } else {
            rect(x, y, width, height, Paint.fill(color), transform);
        }
    }

    default void addRect(float x, float y, float width, float height, float radius, ColorView color, float thickness) {
        addRect(x, y, width, height, radius, color, thickness, null);
    }

    default void addRect(float x, float y, float width, float height, float radius,
                         ColorView color, float thickness, Transform transform) {
        if (transform == null) {
            roundedRect(x, y, width, height, radius, Paint.stroke(color, thickness));
        } else {
            roundedRect(x, y, width, height, radius, Paint.stroke(color, thickness), transform);
        }
    }

    default void addRectFilled(float x, float y, float width, float height, float radius, ColorView color) {
        addRectFilled(x, y, width, height, radius, color, null);
    }

    default void addRectFilled(float x, float y, float width, float height, float radius,
                               ColorView color, Transform transform) {
        if (transform == null) {
            roundedRect(x, y, width, height, radius, Paint.fill(color));
        } else {
            roundedRect(x, y, width, height, radius, Paint.fill(color), transform);
        }
    }

    default void addRectFilledMultiColor(float x, float y, float width, float height,
                                         ColorView topLeft, ColorView topRight,
                                         ColorView bottomRight, ColorView bottomLeft) {
        addRectFilledMultiColor(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, null);
    }

    default void addRectFilledMultiColor(float x, float y, float width, float height,
                                         ColorView topLeft, ColorView topRight,
                                         ColorView bottomRight, ColorView bottomLeft,
                                         Transform transform) {
        addMesh(DrawMesh.triangles(List.of(
                vertex(x, y, topLeft),
                vertex(x, y + height, bottomLeft),
                vertex(x + width, y + height, bottomRight),
                vertex(x, y, topLeft),
                vertex(x + width, y + height, bottomRight),
                vertex(x + width, y, topRight)
        )), null, transform);
    }

    default void addQuad(DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4, ColorView color, float thickness) {
        addQuad(p1, p2, p3, p4, color, thickness, null);
    }

    default void addQuad(DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4,
                         ColorView color, float thickness, Transform transform) {
        addPolyline(List.of(p1, p2, p3, p4), color, true, thickness, transform);
    }

    default void addQuadFilled(DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4, ColorView color) {
        addQuadFilled(p1, p2, p3, p4, color, null);
    }

    default void addQuadFilled(DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4,
                               ColorView color, Transform transform) {
        addMesh(DrawMesh.triangles(List.of(
                vertex(p1, color), vertex(p2, color), vertex(p3, color),
                vertex(p1, color), vertex(p3, color), vertex(p4, color)
        )), null, transform);
    }

    default void addTriangle(DrawPoint p1, DrawPoint p2, DrawPoint p3, ColorView color, float thickness) {
        addTriangle(p1, p2, p3, color, thickness, null);
    }

    default void addTriangle(DrawPoint p1, DrawPoint p2, DrawPoint p3,
                             ColorView color, float thickness, Transform transform) {
        addPolyline(List.of(p1, p2, p3), color, true, thickness, transform);
    }

    default void addTriangleFilled(DrawPoint p1, DrawPoint p2, DrawPoint p3, ColorView color) {
        addTriangleFilled(p1, p2, p3, color, null);
    }

    default void addTriangleFilled(DrawPoint p1, DrawPoint p2, DrawPoint p3,
                                   ColorView color, Transform transform) {
        addMesh(DrawMesh.triangles(List.of(vertex(p1, color), vertex(p2, color), vertex(p3, color))), null, transform);
    }

    default void addCircle(float centerX, float centerY, float radius, ColorView color, int segments, float thickness) {
        addCircle(centerX, centerY, radius, color, segments, thickness, null);
    }

    default void addCircle(float centerX, float centerY, float radius, ColorView color,
                           int segments, float thickness, Transform transform) {
        addEllipse(centerX, centerY, radius, radius, color, segments, thickness, transform);
    }

    default void addCircleFilled(float centerX, float centerY, float radius, ColorView color, int segments) {
        addCircleFilled(centerX, centerY, radius, color, segments, null);
    }

    default void addCircleFilled(float centerX, float centerY, float radius,
                                 ColorView color, int segments, Transform transform) {
        addEllipseFilled(centerX, centerY, radius, radius, color, segments, transform);
    }

    default void addNgon(float centerX, float centerY, float radius, ColorView color, int segments, float thickness) {
        addNgon(centerX, centerY, radius, color, segments, thickness, null);
    }

    default void addNgon(float centerX, float centerY, float radius, ColorView color,
                         int segments, float thickness, Transform transform) {
        addPolyline(regularPolygon(centerX, centerY, radius, Math.max(3, segments)), color, true, thickness, transform);
    }

    default void addNgonFilled(float centerX, float centerY, float radius, ColorView color, int segments) {
        addNgonFilled(centerX, centerY, radius, color, segments, null);
    }

    default void addNgonFilled(float centerX, float centerY, float radius,
                               ColorView color, int segments, Transform transform) {
        addConvexPolyFilled(regularPolygon(centerX, centerY, radius, Math.max(3, segments)), color, transform);
    }

    default void addEllipse(float centerX, float centerY, float radiusX, float radiusY,
                            ColorView color, int segments, float thickness) {
        addEllipse(centerX, centerY, radiusX, radiusY, color, segments, thickness, null);
    }

    default void addEllipse(float centerX, float centerY, float radiusX, float radiusY,
                            ColorView color, int segments, float thickness, Transform transform) {
        int count = normalizedSegments(Math.max(Math.abs(radiusX), Math.abs(radiusY)), segments);
        DrawCommand command = drawList().obtain(DrawCommandType.CIRCLE)
                .bounds(centerX - radiusX, centerY - radiusY, radiusX * 2.0f, radiusY * 2.0f)
                .segments(count)
                .paintOwned(effectivePaint(Paint.stroke(color, thickness)));
        if (transform != null) command.transform(transform);
        submitOwned(command);
    }

    default void addEllipseFilled(float centerX, float centerY, float radiusX, float radiusY,
                                  ColorView color, int segments) {
        addEllipseFilled(centerX, centerY, radiusX, radiusY, color, segments, null);
    }

    default void addEllipseFilled(float centerX, float centerY, float radiusX, float radiusY,
                                  ColorView color, int segments, Transform transform) {
        int count = normalizedSegments(Math.max(Math.abs(radiusX), Math.abs(radiusY)), segments);
        DrawCommand command = drawList().obtain(DrawCommandType.CIRCLE)
                .bounds(centerX - radiusX, centerY - radiusY, radiusX * 2.0f, radiusY * 2.0f)
                .segments(count)
                .paintOwned(effectivePaint(Paint.fill(color)));
        if (transform != null) command.transform(transform);
        submitOwned(command);
    }

    default void addText(String text, float x, float y, float width, float height, ColorView color) {
        addText(text, x, y, width, height, color, null);
    }

    default void addText(String text, float x, float y, float width, float height,
                         ColorView color, Transform transform) {
        if (transform == null) {
            text(text, x, y, width, height, Paint.fill(color));
        } else {
            text(text, x, y, width, height, Paint.fill(color), transform);
        }
    }

    default void addText(RichText text, float x, float y, float width, float height, ColorView color) {
        addText(text, x, y, width, height, color, null);
    }

    default void addText(RichText text, float x, float y, float width, float height,
                         ColorView color, Transform transform) {
        if (transform == null) {
            text(text, x, y, width, height, Paint.fill(color));
        } else {
            text(text, x, y, width, height, Paint.fill(color), transform);
        }
    }

    default void addPolyline(List<DrawPoint> points, ColorView color, boolean closed, float thickness) {
        addPolyline(points, color, closed, thickness, null);
    }

    default void addPolyline(List<DrawPoint> points, ColorView color, boolean closed,
                             float thickness, Transform transform) {
        if (points == null || points.size() < 2) return;
        int segmentCount = closed ? points.size() : points.size() - 1;
        for (int i = 0; i < segmentCount; i++) {
            DrawPoint current = points.get(i);
            DrawPoint next = points.get((i + 1) % points.size());
            if (current != null && next != null) {
                addLine(current.x(), current.y(), next.x(), next.y(), color, thickness, transform);
            }
        }
    }

    default void addConvexPolyFilled(List<DrawPoint> points, ColorView color) {
        addConvexPolyFilled(points, color, null);
    }

    default void addConvexPolyFilled(List<DrawPoint> points, ColorView color, Transform transform) {
        if (points == null || points.size() < 3) return;
        List<DrawVertex> vertices = new ObjectArrayList<>((points.size() - 2) * 3);
        DrawPoint first = points.get(0);
        for (int i = 1; i < points.size() - 1; i++) {
            DrawPoint current = points.get(i);
            DrawPoint next = points.get(i + 1);
            if (first != null && current != null && next != null) {
                vertices.add(vertex(first, color));
                vertices.add(vertex(current, color));
                vertices.add(vertex(next, color));
            }
        }
        addMesh(DrawMesh.triangles(vertices), null, transform);
    }

    default void addConcavePolyFilled(List<DrawPoint> points, ColorView color) {
        addConcavePolyFilled(points, color, null);
    }

    default void addConcavePolyFilled(List<DrawPoint> points, ColorView color, Transform transform) {
        List<DrawVertex> vertices = triangulate(points, effectiveColor(color));
        if (!vertices.isEmpty()) {
            addMesh(DrawMesh.triangles(vertices), null, transform);
        }
    }

    default void addImage(TextureHandle texture, float x, float y, float width, float height, ColorView tint) {
        addImage(texture, x, y, width, height, tint, null);
    }

    default void addImage(TextureHandle texture, float x, float y, float width, float height,
                          ColorView tint, Transform transform) {
        if (transform == null) {
            texture(texture, x, y, width, height, Paint.fill(tint));
        } else {
            texture(texture, x, y, width, height, Paint.fill(tint), transform);
        }
    }

    default void addImage(TextureHandle texture, float x, float y, float width, float height,
                          DrawPoint uvMin, DrawPoint uvMax, ColorView tint) {
        addImage(texture, x, y, width, height, uvMin, uvMax, tint, null);
    }

    default void addImage(TextureHandle texture, float x, float y, float width, float height,
                          DrawPoint uvMin, DrawPoint uvMax, ColorView tint, Transform transform) {
        TexturePlacement placement = placement(texture, x, y, width, height, uvMin, uvMax);
        if (transform == null) {
            texture(texture, placement, 0.0f, Paint.fill(tint));
        } else {
            texture(texture, placement, 0.0f, Paint.fill(tint), transform);
        }
    }

    default void addImageQuad(TextureHandle texture,
                              DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4,
                              DrawPoint uv1, DrawPoint uv2, DrawPoint uv3, DrawPoint uv4,
                              ColorView tint) {
        addImageQuad(texture, p1, p2, p3, p4, uv1, uv2, uv3, uv4, tint, null);
    }

    default void addImageQuad(TextureHandle texture,
                              DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4,
                              DrawPoint uv1, DrawPoint uv2, DrawPoint uv3, DrawPoint uv4,
                              ColorView tint, Transform transform) {
        addTexturedQuad(texture,
                pointX(p1), pointY(p1), pointX(p2), pointY(p2),
                pointX(p3), pointY(p3), pointX(p4), pointY(p4),
                pointX(uv1), pointY(uv1), pointX(uv2), pointY(uv2),
                pointX(uv3), pointY(uv3), pointX(uv4), pointY(uv4),
                tint, transform);
    }

    default void addTexturedQuad(TextureHandle texture,
                                 float x1, float y1, float x2, float y2,
                                 float x3, float y3, float x4, float y4,
                                 float u1, float v1, float u2, float v2,
                                 float u3, float v3, float u4, float v4,
                                 ColorView tint) {
        addTexturedQuad(texture, x1, y1, x2, y2, x3, y3, x4, y4,
                u1, v1, u2, v2, u3, v3, u4, v4, tint, null);
    }

    default void addTexturedQuad(TextureHandle texture,
                                 float x1, float y1, float x2, float y2,
                                 float x3, float y3, float x4, float y4,
                                 float u1, float v1, float u2, float v2,
                                 float u3, float v3, float u4, float v4,
                                 ColorView tint, Transform transform) {
        float minX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
        float minY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
        float maxX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
        float maxY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
        DrawCommand command = drawList().obtain(DrawCommandType.TEXTURED_QUAD)
                .texture(texture)
                .bounds(minX, minY, maxX - minX, maxY - minY)
                .texturedQuad(x1, y1, x2, y2, x3, y3, x4, y4,
                        u1, v1, u2, v2, u3, v3, u4, v4)
                .paintColorOwned(tint, opacityMultiplier());
        if (transform != null) command.transform(transform);
        submitOwned(command);
    }

    default void addImageRounded(TextureHandle texture, float x, float y, float width, float height,
                                 DrawPoint uvMin, DrawPoint uvMax, ColorView tint, float radius) {
        addImageRounded(texture, x, y, width, height, uvMin, uvMax, tint, radius, null);
    }

    default void addImageRounded(TextureHandle texture, float x, float y, float width, float height,
                                 DrawPoint uvMin, DrawPoint uvMax, ColorView tint, float radius,
                                 Transform transform) {
        TexturePlacement placement = placement(texture, x, y, width, height, uvMin, uvMax);
        if (transform == null) {
            texture(texture, placement, radius, Paint.fill(tint));
        } else {
            texture(texture, placement, radius, Paint.fill(tint), transform);
        }
    }

    default void addMesh(DrawMesh mesh, TextureHandle texture) {
        addMesh(mesh, texture, null);
    }

    default void addMesh(DrawMesh mesh, TextureHandle texture, Transform transform) {
        if (mesh == null || mesh.isEmpty()) return;
        DrawCommand command = drawList().obtain(DrawCommandType.MESH)
                .meshOwned(mesh)
                .texture(texture)
                .bounds(meshBounds(mesh));
        if (transform != null) {
            command.transform(transform);
        }
        submitOwned(command);
    }

    default void channelsSplit(int count) {
        drawList().channelsSplit(count);
    }

    default void channelsMerge() {
        drawList().channelsMerge();
    }

    default void channelsSetCurrent(int channel) {
        drawList().channelsSetCurrent(channel);
    }

    default VectorPath path() {
        return drawList().path();
    }

    default void pathClear() {
        drawList().pathClear();
    }

    default void pathLineTo(DrawPoint point) {
        if (point != null) {
            pathLineTo(point.x(), point.y());
        }
    }

    default void pathLineTo(float x, float y) {
        VectorPath current = path();
        if (current.isEmpty()) {
            current.moveTo(x, y);
        } else {
            current.lineTo(x, y);
        }
    }

    default void pathLineToMergeDuplicate(DrawPoint point) {
        if (point != null) {
            pathLineToMergeDuplicate(point.x(), point.y());
        }
    }

    default void pathLineToMergeDuplicate(float x, float y) {
        DrawPoint last = pathLastPoint(path());
        if (last != null && samePoint(last.x(), last.y(), x, y)) return;
        pathLineTo(x, y);
    }

    default void pathFillConvex(ColorView color) {
        pathFillConvex(color, null);
    }

    default void pathFillConvex(ColorView color, Transform transform) {
        emitPath(path(), Paint.fill(color), transform);
        pathClear();
    }

    default void pathStroke(ColorView color, boolean closed, float thickness) {
        pathStroke(color, closed, thickness, null);
    }

    default void pathStroke(ColorView color, boolean closed, float thickness, Transform transform) {
        VectorPath source = path().copy();
        if (closed && !source.isEmpty()) {
            source.close();
        }
        emitPath(source, Paint.stroke(color, thickness), transform);
        pathClear();
    }

    default void pathArcTo(float centerX, float centerY, float radius,
                           float minAngle, float maxAngle, int segments) {
        if (radius <= 0.0f) {
            pathLineTo(centerX, centerY);
            return;
        }
        int count = normalizedArcSegments(radius, minAngle, maxAngle, segments);
        for (int i = 0; i <= count; i++) {
            float t = i / (float) count;
            float angle = minAngle + (maxAngle - minAngle) * t;
            pathLineToMergeDuplicate(centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius);
        }
    }

    default void pathArcToFast(float centerX, float centerY, float radius,
                               int minSampleOf12, int maxSampleOf12) {
        if (radius <= 0.0f) {
            pathLineTo(centerX, centerY);
            return;
        }
        int step = maxSampleOf12 >= minSampleOf12 ? 1 : -1;
        for (int sample = minSampleOf12; ; sample += step) {
            float angle = TAU * sample / 12.0f;
            pathLineToMergeDuplicate(centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius);
            if (sample == maxSampleOf12) break;
        }
    }

    default void pathBezierCubicCurveTo(float controlX1, float controlY1,
                                        float controlX2, float controlY2,
                                        float x, float y, int segments) {
        DrawPoint start = pathLastPoint(path());
        if (start == null) {
            pathLineTo(x, y);
            return;
        }
        int count = normalizedBezierSegments(start.x(), start.y(), x, y, segments);
        for (int i = 1; i <= count; i++) {
            float t = i / (float) count;
            pathLineTo(cubic(start.x(), controlX1, controlX2, x, t),
                    cubic(start.y(), controlY1, controlY2, y, t));
        }
    }

    default void pathBezierQuadraticCurveTo(float controlX, float controlY,
                                            float x, float y, int segments) {
        DrawPoint start = pathLastPoint(path());
        if (start == null) {
            pathLineTo(x, y);
            return;
        }
        int count = normalizedBezierSegments(start.x(), start.y(), x, y, segments);
        for (int i = 1; i <= count; i++) {
            float t = i / (float) count;
            pathLineTo(quadratic(start.x(), controlX, x, t),
                    quadratic(start.y(), controlY, y, t));
        }
    }

    default void pathRect(float x, float y, float width, float height) {
        pathRect(x, y, width, height, 0.0f);
    }

    default void pathRect(float x, float y, float width, float height, float radius) {
        float x1 = Math.min(x, x + width);
        float y1 = Math.min(y, y + height);
        float x2 = Math.max(x, x + width);
        float y2 = Math.max(y, y + height);
        float r = Math.max(0.0f, Math.min(Math.abs(radius), Math.min(x2 - x1, y2 - y1) * 0.5f));
        if (r <= 0.0f) {
            pathLineTo(x1, y1);
            pathLineTo(x2, y1);
            pathLineTo(x2, y2);
            pathLineTo(x1, y2);
            return;
        }
        int segments = Math.max(2, Math.min(8, Math.round(r * 0.35f)));
        pathArcTo(x1 + r, y1 + r, r, (float) Math.PI, (float) (Math.PI * 1.5), segments);
        pathArcTo(x2 - r, y1 + r, r, (float) (Math.PI * 1.5), TAU, segments);
        pathArcTo(x2 - r, y2 - r, r, 0.0f, (float) (Math.PI * 0.5), segments);
        pathArcTo(x1 + r, y2 - r, r, (float) (Math.PI * 0.5), (float) Math.PI, segments);
    }

    /**
     * Открывает clip/scissor область.
     *
     * @param x X-граница clip-области
     * @param y Y-граница clip-области
     * @param width ширина clip bounds
     * @param height высота clip bounds
     */
    default void pushClip(float x, float y, float width, float height) {
        submitOwned(drawList().obtain(DrawCommandType.PUSH_CLIP)
                .bounds(x, y, width, height));
    }

    default void pushClip(float x, float y, float width, float height, Transform transform) {
        DrawCommand command = drawList().obtain(DrawCommandType.PUSH_CLIP)
                .bounds(x, y, width, height);
        if (transform != null) {
            command.transform(transform);
        }
        submitOwned(command);
    }

    default void popClip() {
        submitOwned(drawList().obtain(DrawCommandType.POP_CLIP));
    }

    default Paint effectivePaint(Paint paint) {
        float opacity = clamp01(opacityMultiplier());
        if (paint == null) {
            return new Paint();
        }
        if (opacity < 0.999f) {
            Paint copy = paint.copy();
            copy.color().set(copy.color().r(), copy.color().g(), copy.color().b(), copy.color().a() * opacity);
            return copy;
        }
        return paint;
    }

    default ColorView effectiveColor(ColorView color) {
        Paint paint = effectivePaint(Paint.fill(color));
        return paint.color().copy();
    }

    private void emitDashedLine(float x1, float y1, float x2, float y2, Paint paint, Transform transform) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) return;

        float dash = Math.max(0.0f, paint.dashLength());
        float gap = Math.max(0.0f, paint.dashGap());
        float pattern = dash + gap;
        if (dash <= 0.0f || gap <= 0.0f || pattern <= 0.0f) {
            Paint solid = paint.copy().clearDash();
            DrawCommand command = drawList().obtain(DrawCommandType.LINE)
                    .bounds(x1, y1, dx, dy)
                    .paintOwned(solid);
            if (transform != null) command.transform(transform);
            submitOwned(command);
            return;
        }

        float invLength = 1.0f / length;
        float cursor = -positiveModulo(paint.dashOffset(), pattern);
        Paint solid = paint.copy().clearDash();
        while (cursor < length) {
            float start = Math.max(0.0f, cursor);
            float end = Math.min(length, cursor + dash);
            if (end > start) {
                float sx = x1 + dx * start * invLength;
                float sy = y1 + dy * start * invLength;
                float ex = x1 + dx * end * invLength;
                float ey = y1 + dy * end * invLength;
                DrawCommand command = drawList().obtain(DrawCommandType.LINE)
                        .bounds(sx, sy, ex - sx, ey - sy)
                        .paintOwned(solid);
                if (transform != null) command.transform(transform);
                submitOwned(command);
            }
            cursor += pattern;
        }
    }

    private static float positiveModulo(float value, float modulo) {
        if (modulo <= 0.0f) return 0.0f;
        float result = value % modulo;
        return result < 0.0f ? result + modulo : result;
    }
    private void emitPath(VectorPath source, Paint paint, Transform transform) {
        if (source == null || source.isEmpty()) return;
        MutableRect bounds = pathBounds(source);
        VectorPath relative = relativePath(source, bounds);
        DrawCommand command = drawList().obtain(DrawCommandType.PATH)
                .pathOwned(relative)
                .bounds(bounds)
                .paintOwned(effectivePaint(paint));
        if (transform != null) {
            command.transform(transform);
        }
        submitOwned(command);
    }

    private static float pointX(DrawPoint point) {
        return point == null ? 0.0f : point.x();
    }

    private static float pointY(DrawPoint point) {
        return point == null ? 0.0f : point.y();
    }

    private DrawVertex vertex(DrawPoint point, ColorView color) {
        return vertex(point == null ? 0.0f : point.x(), point == null ? 0.0f : point.y(), color);
    }

    private DrawVertex vertex(float x, float y, ColorView color) {
        return new DrawVertex(x, y, effectiveColor(color));
    }

    private DrawVertex vertex(DrawPoint point, DrawPoint uv, ColorView color) {
        return new DrawVertex(
                point == null ? 0.0f : point.x(),
                point == null ? 0.0f : point.y(),
                uv == null ? 0.0f : uv.x(),
                uv == null ? 0.0f : uv.y(),
                effectiveColor(color));
    }

    private static TexturePlacement placement(TextureHandle texture, float x, float y, float width, float height,
                                              DrawPoint uvMin, DrawPoint uvMax) {
        float minU = uvMin == null ? 0.0f : uvMin.x();
        float minV = uvMin == null ? 0.0f : uvMin.y();
        float maxU = uvMax == null ? 1.0f : uvMax.x();
        float maxV = uvMax == null ? 1.0f : uvMax.y();
        return new TexturePlacement(x, y, width, height, minU, minV, maxU - minU, maxV - minV);
    }

    private static List<DrawPoint> regularPolygon(float centerX, float centerY, float radius, int segments) {
        List<DrawPoint> points = new ObjectArrayList<>(segments);
        for (int i = 0; i < segments; i++) {
            float angle = TAU * i / segments;
            points.add(new DrawPoint(centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius));
        }
        return points;
    }

    private static int normalizedSegments(float radius, int requested) {
        if (requested > 0) return Math.max(3, requested);
        return Math.max(12, Math.min(64, Math.round(Math.abs(radius) * 0.75f)));
    }

    private static int normalizedArcSegments(float radius, float minAngle, float maxAngle, int requested) {
        if (requested > 0) return Math.max(1, requested);
        float arc = Math.abs(maxAngle - minAngle);
        return Math.max(1, Math.round(normalizedSegments(radius, 0) * arc / TAU));
    }

    private static int normalizedBezierSegments(float x1, float y1, float x2, float y2, int requested) {
        if (requested > 0) return Math.max(1, requested);
        float dx = x2 - x1;
        float dy = y2 - y1;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return Math.max(4, Math.min(48, Math.round(distance * 0.25f)));
    }

    private static MutableRect meshBounds(DrawMesh mesh) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (DrawVertex vertex : mesh.vertices()) {
            minX = Math.min(minX, vertex.x());
            minY = Math.min(minY, vertex.y());
            maxX = Math.max(maxX, vertex.x());
            maxY = Math.max(maxY, vertex.y());
        }
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX) || !Float.isFinite(maxY)) {
            return new MutableRect();
        }
        return new MutableRect(minX, minY, maxX - minX, maxY - minY);
    }

    private static MutableRect pathBounds(VectorPath path) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        Object[] rawPathElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawPathElements[i];
            switch (element.verb()) {
                case MOVE_TO, LINE_TO -> {
                    minX = Math.min(minX, element.x1());
                    minY = Math.min(minY, element.y1());
                    maxX = Math.max(maxX, element.x1());
                    maxY = Math.max(maxY, element.y1());
                }
                case QUADRATIC_TO -> {
                    minX = Math.min(minX, Math.min(element.x1(), element.x2()));
                    minY = Math.min(minY, Math.min(element.y1(), element.y2()));
                    maxX = Math.max(maxX, Math.max(element.x1(), element.x2()));
                    maxY = Math.max(maxY, Math.max(element.y1(), element.y2()));
                }
                case CUBIC_TO -> {
                    minX = Math.min(minX, Math.min(element.x1(), Math.min(element.x2(), element.x3())));
                    minY = Math.min(minY, Math.min(element.y1(), Math.min(element.y2(), element.y3())));
                    maxX = Math.max(maxX, Math.max(element.x1(), Math.max(element.x2(), element.x3())));
                    maxY = Math.max(maxY, Math.max(element.y1(), Math.max(element.y2(), element.y3())));
                }
                case CLOSE -> {
                }
            }
        }
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(maxX) || !Float.isFinite(maxY)) {
            return new MutableRect();
        }
        return new MutableRect(minX, minY, maxX - minX, maxY - minY);
    }

    private static VectorPath relativePath(VectorPath path, RectView bounds) {
        VectorPath relative = new VectorPath();
        Object[] rawPathElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawPathElements[i];
            switch (element.verb()) {
                case MOVE_TO -> relative.moveTo(element.x1() - bounds.x(), element.y1() - bounds.y());
                case LINE_TO -> relative.lineTo(element.x1() - bounds.x(), element.y1() - bounds.y());
                case QUADRATIC_TO -> relative.quadraticTo(
                        element.x1() - bounds.x(), element.y1() - bounds.y(),
                        element.x2() - bounds.x(), element.y2() - bounds.y());
                case CUBIC_TO -> relative.cubicTo(
                        element.x1() - bounds.x(), element.y1() - bounds.y(),
                        element.x2() - bounds.x(), element.y2() - bounds.y(),
                        element.x3() - bounds.x(), element.y3() - bounds.y());
                case CLOSE -> relative.close();
            }
        }
        return relative;
    }

    private static DrawPoint pathLastPoint(VectorPath path) {
        if (path == null || path.isEmpty()) return null;
        DrawPoint current = null;
        DrawPoint start = null;
        Object[] rawPathElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawPathElements[i];
            switch (element.verb()) {
                case MOVE_TO -> {
                    current = new DrawPoint(element.x1(), element.y1());
                    start = current;
                }
                case LINE_TO -> current = new DrawPoint(element.x1(), element.y1());
                case QUADRATIC_TO -> current = new DrawPoint(element.x2(), element.y2());
                case CUBIC_TO -> current = new DrawPoint(element.x3(), element.y3());
                case CLOSE -> current = start;
            }
        }
        return current;
    }

    private static List<DrawVertex> triangulate(List<DrawPoint> points, ColorView color) {
        if (points == null || points.size() < 3) return List.of();
        List<DrawPoint> polygon = new ObjectArrayList<>();
        for (DrawPoint point : points) {
            if (point != null) polygon.add(point);
        }
        if (polygon.size() < 3) return List.of();
        if (signedArea(polygon) < 0.0f) {
            Collections.reverse(polygon);
        }

        List<DrawVertex> vertices = new ObjectArrayList<>((polygon.size() - 2) * 3);
        int guard = polygon.size() * polygon.size();
        while (polygon.size() > 3 && guard-- > 0) {
            boolean clipped = false;
            for (int i = 0; i < polygon.size(); i++) {
                DrawPoint previous = polygon.get((i + polygon.size() - 1) % polygon.size());
                DrawPoint current = polygon.get(i);
                DrawPoint next = polygon.get((i + 1) % polygon.size());
                if (!isConvex(previous, current, next)) continue;
                if (containsAnyPoint(polygon, previous, current, next, i)) continue;
                vertices.add(new DrawVertex(previous.x(), previous.y(), color));
                vertices.add(new DrawVertex(current.x(), current.y(), color));
                vertices.add(new DrawVertex(next.x(), next.y(), color));
                polygon.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) break;
        }
        if (polygon.size() == 3) {
            vertices.add(new DrawVertex(polygon.get(0).x(), polygon.get(0).y(), color));
            vertices.add(new DrawVertex(polygon.get(1).x(), polygon.get(1).y(), color));
            vertices.add(new DrawVertex(polygon.get(2).x(), polygon.get(2).y(), color));
        }
        return vertices;
    }

    private static float signedArea(List<DrawPoint> points) {
        float area = 0.0f;
        for (int i = 0; i < points.size(); i++) {
            DrawPoint a = points.get(i);
            DrawPoint b = points.get((i + 1) % points.size());
            area += a.x() * b.y() - b.x() * a.y();
        }
        return area * 0.5f;
    }

    private static boolean isConvex(DrawPoint a, DrawPoint b, DrawPoint c) {
        return cross(a, b, c) > 0.0001f;
    }

    private static boolean containsAnyPoint(List<DrawPoint> polygon, DrawPoint a, DrawPoint b, DrawPoint c, int earIndex) {
        int previousIndex = (earIndex + polygon.size() - 1) % polygon.size();
        int nextIndex = (earIndex + 1) % polygon.size();
        for (int i = 0; i < polygon.size(); i++) {
            if (i == previousIndex || i == earIndex || i == nextIndex) continue;
            if (pointInTriangle(polygon.get(i), a, b, c)) return true;
        }
        return false;
    }

    private static boolean pointInTriangle(DrawPoint p, DrawPoint a, DrawPoint b, DrawPoint c) {
        float c1 = cross(p, a, b);
        float c2 = cross(p, b, c);
        float c3 = cross(p, c, a);
        boolean hasNegative = c1 < 0.0f || c2 < 0.0f || c3 < 0.0f;
        boolean hasPositive = c1 > 0.0f || c2 > 0.0f || c3 > 0.0f;
        return !(hasNegative && hasPositive);
    }

    private static float cross(DrawPoint a, DrawPoint b, DrawPoint c) {
        return (b.x() - a.x()) * (c.y() - a.y()) - (b.y() - a.y()) * (c.x() - a.x());
    }

    private static boolean samePoint(float leftX, float leftY, float rightX, float rightY) {
        return Math.abs(leftX - rightX) < 0.0001f && Math.abs(leftY - rightY) < 0.0001f;
    }

    private static float quadratic(float x0, float x1, float x2, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * x0 + 2.0f * inverse * t * x1 + t * t * x2;
    }

    private static float cubic(float x0, float x1, float x2, float x3, float t) {
        float inverse = 1.0f - t;
        float inverse2 = inverse * inverse;
        float t2 = t * t;
        return inverse2 * inverse * x0 + 3.0f * inverse2 * t * x1 + 3.0f * inverse * t2 * x2 + t2 * t * x3;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
