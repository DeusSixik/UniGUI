package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.text.RichText;

import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Безопасный draw-list facade для renderer'ов виджетов.
 *
 * <p>{@code DrawScope} намеренно отдаёт backend-neutral primitives вместо активного
 * {@link RenderBackend}. Custom renderer может использовать его как компактный immediate-mode API,
 * но UniGUI всё равно записывает обычные {@link DrawCommand} в {@link DrawList}.</p>
 *
 * <p>Scope также применяет transform виджета к создаваемым командам. Если transform привязан к
 * исходным bounds виджета, {@code DrawScope} корректирует pivot для каждой команды, чтобы текст,
 * texture и shape-команды двигались как единый визуальный блок.</p>
 */
@AiDoc(
        summary = "Safe draw-list facade for widget renderers that translates high-level drawing calls into backend-neutral commands, manages transform pivot adjustments, and provides zero-allocation immediate primitives.",
        purpose = "Decouples custom widget visual rendering from direct graphics API calls, enabling automatic command batching, multi-threading, retained render caching, and cross-backend parity.",
        useWhen = {
                "Implement custom procedural visuals inside a widget's `WidgetRender#render(Widget, DrawScope)` method.",
                "Draw standard geometric shapes, textured panels, gradients, vector paths, or procedural ribbons with automatic transform coordination.",
                "Execute zero-allocation drawing routines in high-framerate rendering loops using ColorView primitives."
        },
        criticalRules = {
                "DrawScope records commands into the active RenderContext and DrawList; it does not issue immediate GPU draw calls.",
                "When created with widget layout bounds (`transformBounds`), DrawScope automatically offsets transform pivots so child visuals rotate/scale as a unified unit.",
                "Prefer `*Filled` and `*Stroke` overloads accepting direct `ColorView` in tight render loops to avoid temporary `Paint` allocations.",
                "Always balance `pushClip` or `pushTextClip` with exactly one `popClip()` call.",
                "When clipping text, prefer `pushTextClip` over standard `pushClip` to prevent clipping font antialiasing fringes."
        },
        related = {"WidgetRender", "RenderContext", "DrawList", "DrawCommand", "Paint", "Transform", "NineSlice", "VectorPath", "TextureHandle"},
        examples = {
                @AiDoc.Example(
                        title = "Draw a styled card with background and nine-slice header",
                        order = 10,
                        code = """
                                RectView b = widget.layoutBounds();
                                scope.roundedRectFilled(b.x(), b.y(), b.width(), b.height(), 6.0f, ColorView.rgb(32, 34, 37));
                                scope.roundedRectStroke(b.x(), b.y(), b.width(), b.height(), 6.0f, ColorView.rgb(55, 58, 64), 1.0f);
                                scope.nineSlice(headerSlice, b.x() + 1, b.y() + 1, b.width() - 2, 28.0f, Paint.NONE, true);
                                scope.pushTextClip(b.x() + 10, b.y() + 6, b.width() - 20, 18.0f);
                                try {
                                    scope.addText("Settings Card", b.x() + 10, b.y() + 6, b.width() - 20, 18.0f, ColorView.WHITE);
                                } finally {
                                    scope.popClip();
                                }
                                """),
                @AiDoc.Example(
                        title = "Draw procedural textured ribbon between points",
                        order = 20,
                        code = """
                                List<DrawPoint> path = List.of(new DrawPoint(20.0f, 40.0f), new DrawPoint(140.0f, 90.0f));
                                RibbonOptions options = RibbonOptions.defaults().closed(false).miterLimit(3.0f);
                                scope.ribbon(cableTexture, path, 6.0f, 32.0f, ColorView.rgb(0, 200, 255), options);
                                """)
        })
@WikiPage(
        title = "<key:draw.scope.title>",
        slug = "draw-scope",
        category = "<key:draw.scope.category>",
        summary = "<key:draw.scope.summary>",
        order = 10)
@WikiSection(
        title = "<key:draw.scope.section.overview.title>",
        markdown = "<key:draw.scope.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:draw.scope.section.transforms.title>",
        markdown = "<key:draw.scope.section.transforms.body>",
        order = 20)
@WikiSection(
        title = "<key:draw.scope.section.primitives.title>",
        markdown = "<key:draw.scope.section.primitives.body>",
        order = 30)
@WikiSection(
        title = "<key:draw.scope.section.textures.title>",
        markdown = "<key:draw.scope.section.textures.body>",
        order = 40)
@WikiSection(
        title = "<key:draw.scope.section.vector.title>",
        markdown = "<key:draw.scope.section.vector.body>",
        order = 50)
@WikiSection(
        title = "<key:draw.scope.section.clipping.title>",
        markdown = "<key:draw.scope.section.clipping.body>",
        order = 60)
@WikiExample(
        title = "<key:draw.scope.example.panel.title>",
        code = "<key:draw.scope.example.panel.code>",
        order = 10)
@WikiExample(
        title = "<key:draw.scope.example.ribbon.title>",
        code = "<key:draw.scope.example.ribbon.code>",
        order = 20)
public final class DrawScope {
    private final RenderContext context;
    private final Transform transform;
    private final boolean transformBoundsAnchored;
    private final float transformBoundsX;
    private final float transformBoundsY;

    /**
     * Создаёт scope без anchor bounds для transform pivot.
     *
     * @param context   render context, куда будут попадать команды
     * @param transform transform виджета или {@code null}
     */
    @WikiMethod(
            title = "<key:draw.scope.method.constructor_simple.title>",
            summary = "<key:draw.scope.method.constructor_simple.summary>",
            section = "<key:draw.scope.api.section.scope>",
            sectionOrder = 10,
            order = 10)
    public DrawScope(@WikiParameter(name = "context", description = "<key:draw.scope.param.context>") RenderContext context, @WikiParameter(name = "transform", description = "<key:draw.scope.param.transform>") Transform transform) {
        this(context, transform, null);
    }

    /**
     * Создаёт scope с bounds, относительно которых нужно удерживать transform pivot.
     *
     * @param context         render context, куда будут попадать команды
     * @param transform       transform виджета или {@code null}
     * @param transformBounds исходные bounds виджета для привязки pivot
     */
    @WikiMethod(
            title = "<key:draw.scope.method.constructor_anchored.title>",
            summary = "<key:draw.scope.method.constructor_anchored.summary>",
            section = "<key:draw.scope.api.section.scope>",
            sectionOrder = 10,
            order = 20)
    public DrawScope(@WikiParameter(name = "context", description = "<key:draw.scope.param.context>") RenderContext context, @WikiParameter(name = "transform", description = "<key:draw.scope.param.transform>") Transform transform, @WikiParameter(name = "transformBounds", description = "<key:draw.scope.param.transformBounds>") RectView transformBounds) {
        this.context = Objects.requireNonNull(context, "context");
        this.transform = transform;
        this.transformBoundsAnchored = transformBounds != null;
        this.transformBoundsX = transformBounds == null ? 0.0f : transformBounds.x();
        this.transformBoundsY = transformBounds == null ? 0.0f : transformBounds.y();
    }

    /**
     * @return render context, в который пишет scope
     */
    @WikiMethod(
            title = "<key:draw.scope.method.context.title>",
            summary = "<key:draw.scope.method.context.summary>",
            section = "<key:draw.scope.api.section.scope>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:draw.scope.method.context.return>")
    public RenderContext context() {
        return context;
    }

    /**
     * @return transform, применяемый scope'ом к командам
     */
    @WikiMethod(
            title = "<key:draw.scope.method.transform.title>",
            summary = "<key:draw.scope.method.transform.summary>",
            section = "<key:draw.scope.api.section.scope>",
            sectionOrder = 10,
            order = 40)
    @WikiReturn("<key:draw.scope.method.transform.return>")
    public Transform transform() {
        return transform;
    }

    /**
     * Создаёт новый scope с тем же context и другим transform.
     *
     * @param transform новый transform
     * @return дочерний scope
     */
    @WikiMethod(
            title = "<key:draw.scope.method.with_transform.title>",
            summary = "<key:draw.scope.method.with_transform.summary>",
            section = "<key:draw.scope.api.section.scope>",
            sectionOrder = 10,
            order = 50)
    @WikiReturn("<key:draw.scope.method.with_transform.return>")
    public DrawScope withTransform(@WikiParameter(name = "transform", description = "<key:draw.scope.param.transform>") Transform transform) {
        return transformBoundsAnchored
                ? new DrawScope(context, transform, new AnchorBounds(transformBoundsX, transformBoundsY))
                : new DrawScope(context, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.rect.title>",
            summary = "<key:draw.scope.method.rect.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 10)
    public void rect(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.rect(x, y, width, height, paint);
        } else {
            context.rect(x, y, width, height, paint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.rounded_rect.title>",
            summary = "<key:draw.scope.method.rounded_rect.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 20)
    public void roundedRect(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.roundedRect(x, y, width, height, radius, paint);
        } else {
            context.roundedRect(x, y, width, height, radius, paint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.circle.title>",
            summary = "<key:draw.scope.method.circle.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 30)
    public void circle(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.circle(x, y, width, height, paint);
        } else {
            context.circle(x, y, width, height, paint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.line.title>",
            summary = "<key:draw.scope.method.line.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 40)
    public void line(@WikiParameter(name = "x1", description = "<key:draw.scope.param.x1>") float x1, @WikiParameter(name = "y1", description = "<key:draw.scope.param.y1>") float y1, @WikiParameter(name = "x2", description = "<key:draw.scope.param.x2>") float x2, @WikiParameter(name = "y2", description = "<key:draw.scope.param.y2>") float y2, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.line(x1, y1, x2, y2, paint);
        } else {
            context.line(x1, y1, x2, y2, paint, transformFor(x1, y1));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path.title>",
            summary = "<key:draw.scope.method.path.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 50)
    public void path(@WikiParameter(name = "path", description = "<key:draw.scope.param.path>") VectorPath path, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.path(path, x, y, width, height, paint);
        } else {
            context.path(path, x, y, width, height, paint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.texture_rect.title>",
            summary = "<key:draw.scope.method.texture_rect.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 10)
    public void texture(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.texture(texture, x, y, width, height, paint);
        } else {
            context.texture(texture, x, y, width, height, paint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.texture_placement.title>",
            summary = "<key:draw.scope.method.texture_placement.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 20)
    public void texture(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "placement", description = "<key:draw.scope.param.placement>") TexturePlacement placement, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.texture(texture, placement, radius, paint);
        } else {
            context.texture(texture, placement, radius, paint, transformFor(placement.x(), placement.y()));
        }
    }

    /**
     * Рисует nine-slice текстуру: углы без растяжения, края вдоль одной оси, центр по обеим.
     *
     * <p>Рамки берутся из {@link NineSlice} в пикселях текстуры. Если сумма рамок превышает
     * размер текстуры, они пропорционально ужимаются; если виджет уже рамок — края
     * клампятся к половине размера. Нулевые области пропускаются, backend получает
     * обычные texture-команды и работает везде без нового типа команд.</p>
     *
     * @param slice  nine-slice источник; {@code null} игнорируется
     * @param x      X-граница назначения
     * @param y      Y-граница назначения
     * @param width  ширина назначения
     * @param height высота назначения
     * @param paint  tint paint
     */
    @WikiMethod(
            title = "<key:draw.scope.method.nine_slice.title>",
            summary = "<key:draw.scope.method.nine_slice.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 30)
    public void nineSlice(@WikiParameter(name = "slice", description = "<key:draw.scope.param.slice>") NineSlice slice, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        nineSlice(slice, x, y, width, height, paint, true);
    }

    /**
     * Рисует nine-slice текстуру с выбором заливки центра.
     *
     * @param slice      nine-slice источник; {@code null} игнорируется
     * @param x          X-граница назначения
     * @param y          Y-граница назначения
     * @param width      ширина назначения
     * @param height     высота назначения
     * @param paint      tint paint
     * @param fillCenter {@code false}, чтобы оставить прозрачную середину (рамка)
     */
    @WikiMethod(
            title = "<key:draw.scope.method.nine_slice_fill_center.title>",
            summary = "<key:draw.scope.method.nine_slice_fill_center.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 40)
    public void nineSlice(@WikiParameter(name = "slice", description = "<key:draw.scope.param.slice>") NineSlice slice, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint, @WikiParameter(name = "fillCenter", description = "<key:draw.scope.param.fillCenter>") boolean fillCenter) {
        if (slice == null || slice.texture() == null || width <= 0.0f || height <= 0.0f) return;
        TextureHandle texture = slice.texture();
        float textureWidth = Math.max(1.0f, texture.width());
        float textureHeight = Math.max(1.0f, texture.height());

        float srcLeft = slice.left();
        float srcRight = slice.right();
        float srcTop = slice.top();
        float srcBottom = slice.bottom();
        float horizontalOverflow = srcLeft + srcRight - textureWidth;
        if (horizontalOverflow > 0.0f && srcLeft + srcRight > 0.0f) {
            float scale = textureWidth / (srcLeft + srcRight);
            srcLeft *= scale;
            srcRight *= scale;
        }
        float verticalOverflow = srcTop + srcBottom - textureHeight;
        if (verticalOverflow > 0.0f && srcTop + srcBottom > 0.0f) {
            float scale = textureHeight / (srcTop + srcBottom);
            srcTop *= scale;
            srcBottom *= scale;
        }

        float dstLeft = Math.min(srcLeft, width * 0.5f);
        float dstRight = Math.min(srcRight, width * 0.5f);
        float centerWidth = Math.max(0.0f, width - dstLeft - dstRight);
        float dstTop = Math.min(srcTop, height * 0.5f);
        float dstBottom = Math.min(srcBottom, height * 0.5f);
        float centerHeight = Math.max(0.0f, height - dstTop - dstBottom);

        float u0 = 0.0f;
        float u1 = srcLeft / textureWidth;
        float u2 = (textureWidth - srcRight) / textureWidth;
        float u3 = 1.0f;
        float v0 = 0.0f;
        float v1 = srcTop / textureHeight;
        float v2 = (textureHeight - srcBottom) / textureHeight;
        float v3 = 1.0f;

        float centerX = x + dstLeft;
        float rightX = x + dstLeft + centerWidth;
        float centerY = y + dstTop;
        float bottomY = y + dstTop + centerHeight;

        nineSliceCell(texture, x, y, dstLeft, dstTop, u0, v0, u1 - u0, v1 - v0, paint);
        nineSliceCell(texture, centerX, y, centerWidth, dstTop, u1, v0, u2 - u1, v1 - v0, paint);
        nineSliceCell(texture, rightX, y, dstRight, dstTop, u2, v0, u3 - u2, v1 - v0, paint);
        nineSliceCell(texture, x, centerY, dstLeft, centerHeight, u0, v1, u1 - u0, v2 - v1, paint);
        if (fillCenter && u2 - u1 > 0.0f && v2 - v1 > 0.0f) {
            nineSliceCell(texture, centerX, centerY, centerWidth, centerHeight,
                    u1, v1, u2 - u1, v2 - v1, paint);
        }
        nineSliceCell(texture, rightX, centerY, dstRight, centerHeight, u2, v1, u3 - u2, v2 - v1, paint);
        nineSliceCell(texture, x, bottomY, dstLeft, dstBottom, u0, v2, u1 - u0, v3 - v2, paint);
        nineSliceCell(texture, centerX, bottomY, centerWidth, dstBottom, u1, v2, u2 - u1, v3 - v2, paint);
        nineSliceCell(texture, rightX, bottomY, dstRight, dstBottom, u2, v2, u3 - u2, v3 - v2, paint);
    }

    private void nineSliceCell(TextureHandle texture, float x, float y, float width, float height,
                               float u, float v, float uWidth, float vHeight, Paint paint) {
        if (width <= 0.0f || height <= 0.0f) return;
        texture(texture, new TexturePlacement(x, y, width, height, u, v, uWidth, vHeight), 0.0f, paint);
    }

    /**
     * Garbage-free примитивы: те же фигуры, но цвет напрямую без временного {@link Paint}.
     *
     * <p>Зеркала {@code *Filled/*Stroke} перегрузок {@link RenderContext}: ноль аллокаций
     * на вызов в общем случае. Transform scope применяется как обычно.</p>
     */
    @WikiMethod(
            title = "<key:draw.scope.method.rect_filled.title>",
            summary = "<key:draw.scope.method.rect_filled.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 60)
    public void rectFilled(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        if (transform == null) {
            context.rectFilled(x, y, width, height, color);
        } else {
            context.rectFilled(x, y, width, height, color, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.rect_stroke.title>",
            summary = "<key:draw.scope.method.rect_stroke.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 70)
    public void rectStroke(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "strokeWidth", description = "<key:draw.scope.param.strokeWidth>") float strokeWidth) {
        if (transform == null) {
            context.rectStroke(x, y, width, height, color, strokeWidth);
        } else {
            context.rectStroke(x, y, width, height, color, strokeWidth, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.rounded_rect_filled.title>",
            summary = "<key:draw.scope.method.rounded_rect_filled.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 80)
    public void roundedRectFilled(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        if (transform == null) {
            context.roundedRectFilled(x, y, width, height, radius, color);
        } else {
            context.roundedRectFilled(x, y, width, height, radius, color, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.rounded_rect_stroke.title>",
            summary = "<key:draw.scope.method.rounded_rect_stroke.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 90)
    public void roundedRectStroke(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "strokeWidth", description = "<key:draw.scope.param.strokeWidth>") float strokeWidth) {
        if (transform == null) {
            context.roundedRectStroke(x, y, width, height, radius, color, strokeWidth);
        } else {
            context.roundedRectStroke(x, y, width, height, radius, color, strokeWidth, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.circle_filled.title>",
            summary = "<key:draw.scope.method.circle_filled.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 100)
    public void circleFilled(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        if (transform == null) {
            context.circleFilled(x, y, width, height, color);
        } else {
            context.circleFilled(x, y, width, height, color, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.circle_stroke.title>",
            summary = "<key:draw.scope.method.circle_stroke.summary>",
            section = "<key:draw.scope.api.section.shapes>",
            sectionOrder = 20,
            order = 110)
    public void circleStroke(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "strokeWidth", description = "<key:draw.scope.param.strokeWidth>") float strokeWidth) {
        if (transform == null) {
            context.circleStroke(x, y, width, height, color, strokeWidth);
        } else {
            context.circleStroke(x, y, width, height, color, strokeWidth, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.texture_tinted_rect.title>",
            summary = "<key:draw.scope.method.texture_tinted_rect.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 50)
    public void textureTinted(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        if (transform == null) {
            context.textureTinted(texture, x, y, width, height, tint);
        } else {
            context.textureTinted(texture, x, y, width, height, tint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.texture_tinted_placement.title>",
            summary = "<key:draw.scope.method.texture_tinted_placement.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 60)
    public void textureTinted(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "placement", description = "<key:draw.scope.param.placement>") TexturePlacement placement, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        if (transform == null) {
            context.textureTinted(texture, placement, radius, tint);
        } else {
            context.textureTinted(texture, placement, radius, tint, transformFor(placement.x(), placement.y()));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.shader.title>",
            summary = "<key:draw.scope.method.shader.summary>",
            section = "<key:draw.scope.api.section.textures>",
            sectionOrder = 30,
            order = 70)
    public void shader(@WikiParameter(name = "shader", description = "<key:draw.scope.param.shader>") ShaderHandle shader, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "uniforms", description = "<key:draw.scope.param.uniforms>") ShaderUniforms uniforms, @WikiParameter(name = "options", description = "<key:draw.scope.param.options>") ShaderDrawOptions options) {
        if (transform == null) {
            context.shader(shader, x, y, width, height, uniforms, options);
        } else {
            context.shader(shader, x, y, width, height, uniforms, options, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.text_string.title>",
            summary = "<key:draw.scope.method.text_string.summary>",
            section = "<key:draw.scope.api.section.text>",
            sectionOrder = 40,
            order = 10)
    public void text(@WikiParameter(name = "text", description = "<key:draw.scope.param.text>") String text, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        if (transform == null) {
            context.text(text, x, y, width, height, paint);
        } else {
            context.text(text, x, y, width, height, paint, transformFor(x, y));
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.text_rich.title>",
            summary = "<key:draw.scope.method.text_rich.summary>",
            section = "<key:draw.scope.api.section.text>",
            sectionOrder = 40,
            order = 20)
    public void text(@WikiParameter(name = "text", description = "<key:draw.scope.param.text>") RichText text, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint) {
        text(text, x, y, width, height, paint, true);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.text_rich_snap.title>",
            summary = "<key:draw.scope.method.text_rich_snap.summary>",
            section = "<key:draw.scope.api.section.text>",
            sectionOrder = 40,
            order = 30)
    public void text(@WikiParameter(name = "text", description = "<key:draw.scope.param.text>") RichText text, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "paint", description = "<key:draw.scope.param.paint>") Paint paint, @WikiParameter(name = "textPixelSnap", description = "<key:draw.scope.param.textPixelSnap>") boolean textPixelSnap) {
        if (transform == null) {
            context.text(text, x, y, width, height, paint, textPixelSnap);
        } else {
            context.text(text, x, y, width, height, paint, transformFor(x, y), textPixelSnap);
        }
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_draw_cmd.title>",
            summary = "<key:draw.scope.method.add_draw_cmd.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 10)
    public void addDrawCmd(@WikiParameter(name = "command", description = "<key:draw.scope.param.command>") DrawCommand command) {
        context.addDrawCmd(command, transformFor(command == null ? 0.0f : command.bounds().x(), command == null ? 0.0f : command.bounds().y()));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_callback.title>",
            summary = "<key:draw.scope.method.add_callback.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 20)
    public void addCallback(@WikiParameter(name = "callback", description = "<key:draw.scope.param.callback>") CustomDraw callback) {
        context.addCallback(callback);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_line.title>",
            summary = "<key:draw.scope.method.add_line.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 30)
    public void addLine(@WikiParameter(name = "x1", description = "<key:draw.scope.param.x1>") float x1, @WikiParameter(name = "y1", description = "<key:draw.scope.param.y1>") float y1, @WikiParameter(name = "x2", description = "<key:draw.scope.param.x2>") float x2, @WikiParameter(name = "y2", description = "<key:draw.scope.param.y2>") float y2, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addLine(x1, y1, x2, y2, color, thickness, transformFor(x1, y1));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_rect.title>",
            summary = "<key:draw.scope.method.add_rect.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 40)
    public void addRect(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addRect(x, y, width, height, color, thickness, transformFor(x, y));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_rect_radius.title>",
            summary = "<key:draw.scope.method.add_rect_radius.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 50)
    public void addRect(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addRect(x, y, width, height, radius, color, thickness, transformFor(x, y));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_rect_filled.title>",
            summary = "<key:draw.scope.method.add_rect_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 60)
    public void addRectFilled(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addRectFilled(x, y, width, height, color, transformFor(x, y));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_rect_filled_radius.title>",
            summary = "<key:draw.scope.method.add_rect_filled_radius.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 70)
    public void addRectFilled(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addRectFilled(x, y, width, height, radius, color, transformFor(x, y));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_rect_filled_multi_color.title>",
            summary = "<key:draw.scope.method.add_rect_filled_multi_color.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 80)
    public void addRectFilledMultiColor(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "topLeft", description = "<key:draw.scope.param.topLeft>") ColorView topLeft, @WikiParameter(name = "topRight", description = "<key:draw.scope.param.topRight>") ColorView topRight, @WikiParameter(name = "bottomRight", description = "<key:draw.scope.param.bottomRight>") ColorView bottomRight, @WikiParameter(name = "bottomLeft", description = "<key:draw.scope.param.bottomLeft>") ColorView bottomLeft) {
        context.addRectFilledMultiColor(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, transformFor(x, y));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_quad.title>",
            summary = "<key:draw.scope.method.add_quad.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 90)
    public void addQuad(@WikiParameter(name = "p1", description = "<key:draw.scope.param.p1>") DrawPoint p1, @WikiParameter(name = "p2", description = "<key:draw.scope.param.p2>") DrawPoint p2, @WikiParameter(name = "p3", description = "<key:draw.scope.param.p3>") DrawPoint p3, @WikiParameter(name = "p4", description = "<key:draw.scope.param.p4>") DrawPoint p4, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addQuad(p1, p2, p3, p4, color, thickness, transformFor(p1.x(), p1.y()));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_quad_filled.title>",
            summary = "<key:draw.scope.method.add_quad_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 100)
    public void addQuadFilled(@WikiParameter(name = "p1", description = "<key:draw.scope.param.p1>") DrawPoint p1, @WikiParameter(name = "p2", description = "<key:draw.scope.param.p2>") DrawPoint p2, @WikiParameter(name = "p3", description = "<key:draw.scope.param.p3>") DrawPoint p3, @WikiParameter(name = "p4", description = "<key:draw.scope.param.p4>") DrawPoint p4, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addQuadFilled(p1, p2, p3, p4, color, transformFor(p1.x(), p1.y()));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_triangle.title>",
            summary = "<key:draw.scope.method.add_triangle.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 110)
    public void addTriangle(@WikiParameter(name = "p1", description = "<key:draw.scope.param.p1>") DrawPoint p1, @WikiParameter(name = "p2", description = "<key:draw.scope.param.p2>") DrawPoint p2, @WikiParameter(name = "p3", description = "<key:draw.scope.param.p3>") DrawPoint p3, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addTriangle(p1, p2, p3, color, thickness, transformFor(p1.x(), p1.y()));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_triangle_filled.title>",
            summary = "<key:draw.scope.method.add_triangle_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 120)
    public void addTriangleFilled(@WikiParameter(name = "p1", description = "<key:draw.scope.param.p1>") DrawPoint p1, @WikiParameter(name = "p2", description = "<key:draw.scope.param.p2>") DrawPoint p2, @WikiParameter(name = "p3", description = "<key:draw.scope.param.p3>") DrawPoint p3, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addTriangleFilled(p1, p2, p3, color, transformFor(p1.x(), p1.y()));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_circle.title>",
            summary = "<key:draw.scope.method.add_circle.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 130)
    public void addCircle(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addCircle(centerX, centerY, radius, color, segments, thickness, transformFor(centerX - radius, centerY - radius));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_circle_filled.title>",
            summary = "<key:draw.scope.method.add_circle_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 140)
    public void addCircleFilled(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments) {
        context.addCircleFilled(centerX, centerY, radius, color, segments, transformFor(centerX - radius, centerY - radius));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_ngon.title>",
            summary = "<key:draw.scope.method.add_ngon.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 150)
    public void addNgon(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addNgon(centerX, centerY, radius, color, segments, thickness, transformFor(centerX - radius, centerY - radius));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_ngon_filled.title>",
            summary = "<key:draw.scope.method.add_ngon_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 160)
    public void addNgonFilled(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments) {
        context.addNgonFilled(centerX, centerY, radius, color, segments, transformFor(centerX - radius, centerY - radius));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_ellipse.title>",
            summary = "<key:draw.scope.method.add_ellipse.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 170)
    public void addEllipse(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radiusX", description = "<key:draw.scope.param.radiusX>") float radiusX, @WikiParameter(name = "radiusY", description = "<key:draw.scope.param.radiusY>") float radiusY, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addEllipse(centerX, centerY, radiusX, radiusY, color, segments, thickness, transformFor(centerX - radiusX, centerY - radiusY));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_ellipse_filled.title>",
            summary = "<key:draw.scope.method.add_ellipse_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 180)
    public void addEllipseFilled(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radiusX", description = "<key:draw.scope.param.radiusX>") float radiusX, @WikiParameter(name = "radiusY", description = "<key:draw.scope.param.radiusY>") float radiusY, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments) {
        context.addEllipseFilled(centerX, centerY, radiusX, radiusY, color, segments, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_text_string.title>",
            summary = "<key:draw.scope.method.add_text_string.summary>",
            section = "<key:draw.scope.api.section.text>",
            sectionOrder = 40,
            order = 40)
    public void addText(@WikiParameter(name = "text", description = "<key:draw.scope.param.text>") String text, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addText(text, x, y, width, height, color, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_text_rich.title>",
            summary = "<key:draw.scope.method.add_text_rich.summary>",
            section = "<key:draw.scope.api.section.text>",
            sectionOrder = 40,
            order = 50)
    public void addText(@WikiParameter(name = "text", description = "<key:draw.scope.param.text>") RichText text, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addText(text, x, y, width, height, color, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_polyline.title>",
            summary = "<key:draw.scope.method.add_polyline.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 190)
    public void addPolyline(@WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "closed", description = "<key:draw.scope.param.closed>") boolean closed, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.addPolyline(points, color, closed, thickness, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_convex_poly_filled.title>",
            summary = "<key:draw.scope.method.add_convex_poly_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 200)
    public void addConvexPolyFilled(@WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addConvexPolyFilled(points, color, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_concave_poly_filled.title>",
            summary = "<key:draw.scope.method.add_concave_poly_filled.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 210)
    public void addConcavePolyFilled(@WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.addConcavePolyFilled(points, color, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_image.title>",
            summary = "<key:draw.scope.method.add_image.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 220)
    public void addImage(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        context.addImage(texture, x, y, width, height, tint, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_image_uv.title>",
            summary = "<key:draw.scope.method.add_image_uv.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 230)
    public void addImage(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "uvMin", description = "<key:draw.scope.param.uvMin>") DrawPoint uvMin, @WikiParameter(name = "uvMax", description = "<key:draw.scope.param.uvMax>") DrawPoint uvMax, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        context.addImage(texture, x, y, width, height, uvMin, uvMax, tint, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_image_quad.title>",
            summary = "<key:draw.scope.method.add_image_quad.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 240)
    public void addImageQuad(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "p1", description = "<key:draw.scope.param.p1>") DrawPoint p1, @WikiParameter(name = "p2", description = "<key:draw.scope.param.p2>") DrawPoint p2, @WikiParameter(name = "p3", description = "<key:draw.scope.param.p3>") DrawPoint p3, @WikiParameter(name = "p4", description = "<key:draw.scope.param.p4>") DrawPoint p4, @WikiParameter(name = "uv1", description = "<key:draw.scope.param.uv1>") DrawPoint uv1, @WikiParameter(name = "uv2", description = "<key:draw.scope.param.uv2>") DrawPoint uv2, @WikiParameter(name = "uv3", description = "<key:draw.scope.param.uv3>") DrawPoint uv3, @WikiParameter(name = "uv4", description = "<key:draw.scope.param.uv4>") DrawPoint uv4, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        context.addImageQuad(texture, p1, p2, p3, p4, uv1, uv2, uv3, uv4, tint, transform);
    }

    /**
     * Текстурированный quad с float-позициями без создания DrawPoint/DrawMesh.
     */
    @WikiMethod(
            title = "<key:draw.scope.method.add_textured_quad.title>",
            summary = "<key:draw.scope.method.add_textured_quad.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 250)
    public void addTexturedQuad(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "x1", description = "<key:draw.scope.param.x1>") float x1, @WikiParameter(name = "y1", description = "<key:draw.scope.param.y1>") float y1, @WikiParameter(name = "x2", description = "<key:draw.scope.param.x2>") float x2, @WikiParameter(name = "y2", description = "<key:draw.scope.param.y2>") float y2, @WikiParameter(name = "x3", description = "<key:draw.scope.param.x3>") float x3, @WikiParameter(name = "y3", description = "<key:draw.scope.param.y3>") float y3, @WikiParameter(name = "x4", description = "<key:draw.scope.param.x4>") float x4, @WikiParameter(name = "y4", description = "<key:draw.scope.param.y4>") float y4, @WikiParameter(name = "u1", description = "<key:draw.scope.param.u1>") float u1, @WikiParameter(name = "v1", description = "<key:draw.scope.param.v1>") float v1, @WikiParameter(name = "u2", description = "<key:draw.scope.param.u2>") float u2, @WikiParameter(name = "v2", description = "<key:draw.scope.param.v2>") float v2, @WikiParameter(name = "u3", description = "<key:draw.scope.param.u3>") float u3, @WikiParameter(name = "v3", description = "<key:draw.scope.param.v3>") float v3, @WikiParameter(name = "u4", description = "<key:draw.scope.param.u4>") float u4, @WikiParameter(name = "v4", description = "<key:draw.scope.param.v4>") float v4, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        context.addTexturedQuad(texture, x1, y1, x2, y2, x3, y3, x4, y4,
                u1, v1, u2, v2, u3, v3, u4, v4, tint, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_image_rounded.title>",
            summary = "<key:draw.scope.method.add_image_rounded.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 260)
    public void addImageRounded(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "uvMin", description = "<key:draw.scope.param.uvMin>") DrawPoint uvMin, @WikiParameter(name = "uvMax", description = "<key:draw.scope.param.uvMax>") DrawPoint uvMax, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius) {
        context.addImageRounded(texture, x, y, width, height, uvMin, uvMax, tint, radius, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.add_mesh.title>",
            summary = "<key:draw.scope.method.add_mesh.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 270)
    public void addMesh(@WikiParameter(name = "mesh", description = "<key:draw.scope.param.mesh>") DrawMesh mesh, @WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture) {
        context.addMesh(mesh, texture, transform);
    }

    /**
     * Рисует textured ленту вдоль полилинии одной mesh-командой.
     *
     * <p>Дороги, кабели, маршруты, связи графа. Стыки — miter с клампом, торцы —
     * butt caps. При {@code REPEAT}-wrap текстуры и {@code textureLength},
     * равном длине тайла, текстура повторяется вдоль ленты без растяжения.</p>
     *
     * @param texture текстура ленты; {@code null} даёт сплошную цветную ленту
     * @param points центральная полилиния в координатах scope
     * @param width ширина ленты
     * @param textureLength длина одного текстурного тайла вдоль пути
     * @param tint цвет вершин
     * @param options параметры ленты; {@code null} означает {@link RibbonOptions#defaults()}
     */
    @WikiMethod(
            title = "<key:draw.scope.method.ribbon_options.title>",
            summary = "<key:draw.scope.method.ribbon_options.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 280)
    public void ribbon(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "textureLength", description = "<key:draw.scope.param.textureLength>") float textureLength, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint, @WikiParameter(name = "options", description = "<key:draw.scope.param.options>") RibbonOptions options) {
        context.addRibbon(texture, points, width, textureLength, tint,
                options == null ? RibbonOptions.defaults() : options, transform);
    }

    /**
     * Рисует textured ленту вдоль полилинии одной mesh-командой.
     *
     * <p>Дороги, кабели, маршруты, связи графа. Стыки — miter с клампом, торцы —
     * butt caps. При {@code REPEAT}-wrap текстуры и {@code textureLength},
     * равном длине тайла, текстура повторяется вдоль ленты без растяжения.</p>
     *
     * @param texture текстура ленты; {@code null} даёт сплошную цветную ленту
     * @param points центральная полилиния в координатах scope
     * @param width ширина ленты
     * @param textureLength длина одного текстурного тайла вдоль пути
     * @param tint цвет вершин
     * @deprecated используй {@link #ribbon(TextureHandle, List, float, float, ColorView, RibbonOptions)}
     */
    @Deprecated
    @WikiMethod(
            title = "<key:draw.scope.method.ribbon_simple.title>",
            summary = "<key:draw.scope.method.ribbon_simple.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 290)
    public void ribbon(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "textureLength", description = "<key:draw.scope.param.textureLength>") float textureLength, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint) {
        ribbon(texture, points, width, textureLength, tint, RibbonOptions.defaults());
    }

    /**
     * Рисует textured ленту вдоль полилинии одной mesh-командой.
     *
     * @param texture текстура ленты; {@code null} даёт сплошную цветную ленту
     * @param points центральная полилиния в координатах scope
     * @param width ширина ленты
     * @param textureLength длина одного текстурного тайла вдоль пути
     * @param tint цвет вершин
     * @param closed {@code true}, чтобы замкнуть контур
     * @deprecated используй {@link #ribbon(TextureHandle, List, float, float, ColorView, RibbonOptions)}
     */
    @Deprecated
    @WikiMethod(
            title = "<key:draw.scope.method.ribbon_closed.title>",
            summary = "<key:draw.scope.method.ribbon_closed.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 300)
    public void ribbon(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "textureLength", description = "<key:draw.scope.param.textureLength>") float textureLength, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint, @WikiParameter(name = "closed", description = "<key:draw.scope.param.closed>") boolean closed) {
        ribbon(texture, points, width, textureLength, tint, RibbonOptions.defaults().closed(closed));
    }

    /**
     * Рисует textured ленту с поворотом текстуры на 90 градусов.
     *
     * @param texture текстура ленты; {@code null} даёт сплошную цветную ленту
     * @param points центральная полилиния в координатах scope
     * @param width ширина ленты
     * @param textureLength длина одного текстурного тайла вдоль пути
     * @param tint цвет вершин
     * @param closed {@code true}, чтобы замкнуть контур
     * @param swapUv {@code true}: {@code u} вдоль пути, {@code v} поперёк
     *               (для вертикальных текстур-полосок)
     * @deprecated используй {@link #ribbon(TextureHandle, List, float, float, ColorView, RibbonOptions)}
     */
    @Deprecated
    @WikiMethod(
            title = "<key:draw.scope.method.ribbon_swap_uv.title>",
            summary = "<key:draw.scope.method.ribbon_swap_uv.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 310)
    public void ribbon(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "textureLength", description = "<key:draw.scope.param.textureLength>") float textureLength, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint, @WikiParameter(name = "closed", description = "<key:draw.scope.param.closed>") boolean closed, @WikiParameter(name = "swapUv", description = "<key:draw.scope.param.swapUv>") boolean swapUv) {
        ribbon(texture, points, width, textureLength, tint,
                RibbonOptions.defaults().closed(closed).swapUv(swapUv));
    }

    /**
     * Рисует textured ленту с поворотом и отражением текстуры.
     *
     * @param texture текстура ленты; {@code null} даёт сплошную цветную ленту
     * @param points центральная полилиния в координатах scope
     * @param width ширина ленты
     * @param textureLength длина одного текстурного тайла вдоль пути
     * @param tint цвет вершин
     * @param closed {@code true}, чтобы замкнуть контур
     * @param swapUv {@code true}: {@code u} вдоль пути, {@code v} поперёк
     *               (для вертикальных текстур-полосок)
     * @param flipU {@code true}, чтобы отразить финальную U внутри тайла
     * @param flipV {@code true}, чтобы отразить финальную V внутри тайла
     * @deprecated используй {@link #ribbon(TextureHandle, List, float, float, ColorView, RibbonOptions)}
     */
    @Deprecated
    @WikiMethod(
            title = "<key:draw.scope.method.ribbon_full.title>",
            summary = "<key:draw.scope.method.ribbon_full.summary>",
            section = "<key:draw.scope.api.section.direct>",
            sectionOrder = 50,
            order = 320)
    public void ribbon(@WikiParameter(name = "texture", description = "<key:draw.scope.param.texture>") TextureHandle texture, @WikiParameter(name = "points", description = "<key:draw.scope.param.points>") List<DrawPoint> points, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "textureLength", description = "<key:draw.scope.param.textureLength>") float textureLength, @WikiParameter(name = "tint", description = "<key:draw.scope.param.tint>") ColorView tint, @WikiParameter(name = "closed", description = "<key:draw.scope.param.closed>") boolean closed, @WikiParameter(name = "swapUv", description = "<key:draw.scope.param.swapUv>") boolean swapUv, @WikiParameter(name = "flipU", description = "<key:draw.scope.param.flipU>") boolean flipU, @WikiParameter(name = "flipV", description = "<key:draw.scope.param.flipV>") boolean flipV) {
        ribbon(texture, points, width, textureLength, tint,
                RibbonOptions.defaults().closed(closed).swapUv(swapUv).flipU(flipU).flipV(flipV));
    }

    @WikiMethod(
            title = "<key:draw.scope.method.channels_split.title>",
            summary = "<key:draw.scope.method.channels_split.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 10)
    public void channelsSplit(@WikiParameter(name = "count", description = "<key:draw.scope.param.count>") int count) {
        context.channelsSplit(count);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.channels_merge.title>",
            summary = "<key:draw.scope.method.channels_merge.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 20)
    public void channelsMerge() {
        context.channelsMerge();
    }

    @WikiMethod(
            title = "<key:draw.scope.method.channels_set_current.title>",
            summary = "<key:draw.scope.method.channels_set_current.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 30)
    public void channelsSetCurrent(@WikiParameter(name = "channel", description = "<key:draw.scope.param.channel>") int channel) {
        context.channelsSetCurrent(channel);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.get_path.title>",
            summary = "<key:draw.scope.method.get_path.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 40)
    @WikiReturn("<key:draw.scope.method.get_path.return>")
    public VectorPath path() {
        return context.path();
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_clear.title>",
            summary = "<key:draw.scope.method.path_clear.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 50)
    public void pathClear() {
        context.pathClear();
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_line_to_point.title>",
            summary = "<key:draw.scope.method.path_line_to_point.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 60)
    public void pathLineTo(@WikiParameter(name = "point", description = "<key:draw.scope.param.point>") DrawPoint point) {
        context.pathLineTo(point);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_line_to_xy.title>",
            summary = "<key:draw.scope.method.path_line_to_xy.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 70)
    public void pathLineTo(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y) {
        context.pathLineTo(x, y);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_line_to_merge_point.title>",
            summary = "<key:draw.scope.method.path_line_to_merge_point.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 80)
    public void pathLineToMergeDuplicate(@WikiParameter(name = "point", description = "<key:draw.scope.param.point>") DrawPoint point) {
        context.pathLineToMergeDuplicate(point);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_line_to_merge_xy.title>",
            summary = "<key:draw.scope.method.path_line_to_merge_xy.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 90)
    public void pathLineToMergeDuplicate(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y) {
        context.pathLineToMergeDuplicate(x, y);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_fill_convex.title>",
            summary = "<key:draw.scope.method.path_fill_convex.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 100)
    public void pathFillConvex(@WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color) {
        context.pathFillConvex(color, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_stroke.title>",
            summary = "<key:draw.scope.method.path_stroke.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 110)
    public void pathStroke(@WikiParameter(name = "color", description = "<key:draw.scope.param.color>") ColorView color, @WikiParameter(name = "closed", description = "<key:draw.scope.param.closed>") boolean closed, @WikiParameter(name = "thickness", description = "<key:draw.scope.param.thickness>") float thickness) {
        context.pathStroke(color, closed, thickness, transform);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_arc_to.title>",
            summary = "<key:draw.scope.method.path_arc_to.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 120)
    public void pathArcTo(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "minAngle", description = "<key:draw.scope.param.minAngle>") float minAngle, @WikiParameter(name = "maxAngle", description = "<key:draw.scope.param.maxAngle>") float maxAngle, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments) {
        context.pathArcTo(centerX, centerY, radius, minAngle, maxAngle, segments);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_arc_to_fast.title>",
            summary = "<key:draw.scope.method.path_arc_to_fast.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 130)
    public void pathArcToFast(@WikiParameter(name = "centerX", description = "<key:draw.scope.param.centerX>") float centerX, @WikiParameter(name = "centerY", description = "<key:draw.scope.param.centerY>") float centerY, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius, @WikiParameter(name = "minSampleOf12", description = "<key:draw.scope.param.minSampleOf12>") int minSampleOf12, @WikiParameter(name = "maxSampleOf12", description = "<key:draw.scope.param.maxSampleOf12>") int maxSampleOf12) {
        context.pathArcToFast(centerX, centerY, radius, minSampleOf12, maxSampleOf12);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_bezier_cubic.title>",
            summary = "<key:draw.scope.method.path_bezier_cubic.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 140)
    public void pathBezierCubicCurveTo(@WikiParameter(name = "controlX1", description = "<key:draw.scope.param.controlX1>") float controlX1, @WikiParameter(name = "controlY1", description = "<key:draw.scope.param.controlY1>") float controlY1, @WikiParameter(name = "controlX2", description = "<key:draw.scope.param.controlX2>") float controlX2, @WikiParameter(name = "controlY2", description = "<key:draw.scope.param.controlY2>") float controlY2, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments) {
        context.pathBezierCubicCurveTo(controlX1, controlY1, controlX2, controlY2, x, y, segments);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_bezier_quadratic.title>",
            summary = "<key:draw.scope.method.path_bezier_quadratic.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 150)
    public void pathBezierQuadraticCurveTo(@WikiParameter(name = "controlX", description = "<key:draw.scope.param.controlX>") float controlX, @WikiParameter(name = "controlY", description = "<key:draw.scope.param.controlY>") float controlY, @WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "segments", description = "<key:draw.scope.param.segments>") int segments) {
        context.pathBezierQuadraticCurveTo(controlX, controlY, x, y, segments);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_rect.title>",
            summary = "<key:draw.scope.method.path_rect.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 160)
    public void pathRect(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height) {
        context.pathRect(x, y, width, height);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.path_rect_radius.title>",
            summary = "<key:draw.scope.method.path_rect_radius.summary>",
            section = "<key:draw.scope.api.section.paths>",
            sectionOrder = 60,
            order = 170)
    public void pathRect(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height, @WikiParameter(name = "radius", description = "<key:draw.scope.param.radius>") float radius) {
        context.pathRect(x, y, width, height, radius);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.push_clip.title>",
            summary = "<key:draw.scope.method.push_clip.summary>",
            section = "<key:draw.scope.api.section.clipping>",
            sectionOrder = 70,
            order = 10)
    public void pushClip(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height) {
        if (transform == null) {
            context.pushClip(x, y, width, height);
        } else {
            context.pushClip(x, y, width, height, transformFor(x, y));
        }
    }

    /**
     * Открывает clip область для текста с небольшим bleed по краям glyph'ов.
     *
     * <p>Обычный scissor может срезать антиалиасинг или части glyph'ов на разных UI scale. Этот метод
     * добавляет минимальный запас, сохраняя поведение обычного clip для пустых размеров.</p>
     */
    @WikiMethod(
            title = "<key:draw.scope.method.push_text_clip.title>",
            summary = "<key:draw.scope.method.push_text_clip.summary>",
            section = "<key:draw.scope.api.section.clipping>",
            sectionOrder = 70,
            order = 20)
    public void pushTextClip(@WikiParameter(name = "x", description = "<key:draw.scope.param.x>") float x, @WikiParameter(name = "y", description = "<key:draw.scope.param.y>") float y, @WikiParameter(name = "width", description = "<key:draw.scope.param.width>") float width, @WikiParameter(name = "height", description = "<key:draw.scope.param.height>") float height) {
        if (width <= 0.0f || height <= 0.0f) {
            pushClip(x, y, width, height);
            return;
        }
        float bleedX = 1.0f;
        float bleedTop = 1.0f;
        float bleedBottom = 2.0f;
        pushClip(x - bleedX, y - bleedTop, width + bleedX * 2.0f, height + bleedTop + bleedBottom);
    }

    @WikiMethod(
            title = "<key:draw.scope.method.pop_clip.title>",
            summary = "<key:draw.scope.method.pop_clip.summary>",
            section = "<key:draw.scope.api.section.clipping>",
            sectionOrder = 70,
            order = 30)
    public void popClip() {
        context.popClip();
    }

    private Transform transformFor(float commandX, float commandY) {
        if (transform == null || !transformBoundsAnchored || transform.isIdentity()) return transform;
        Transform adjusted = transform.copy();
        adjusted.pivot().set(
                transform.pivot().x() + transformBoundsX - commandX,
                transform.pivot().y() + transformBoundsY - commandY);
        return adjusted;
    }

    private record AnchorBounds(float x, float y) implements RectView {
        @Override
        public float width() {
            return 0.0f;
        }

        @Override
        public float height() {
            return 0.0f;
        }
    }

}
