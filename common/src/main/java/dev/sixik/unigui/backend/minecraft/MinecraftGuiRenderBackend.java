package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.DrawBatcher;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MinecraftGuiRenderBackend implements RenderBackend {
    private final Minecraft minecraft;
    private final DrawBatcher batcher;
    private final ScissorStack scissorStack = new ScissorStack();
    private GuiGraphics graphics;
    private int appliedScissorDepth;
    private int gpuTimerQueryId;
    private boolean gpuTimerQueryInFlight;
    private boolean gpuTimerUnavailable;
    private boolean gpuTimerUsesArb;
    private float lastFrameGpuMillis = -1.0f;

    public MinecraftGuiRenderBackend(GuiGraphics graphics) {
        this(graphics, Minecraft.getInstance(), SimpleDrawBatcher.INSTANCE);
    }

    public MinecraftGuiRenderBackend(GuiGraphics graphics, Minecraft minecraft, DrawBatcher batcher) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
        this.batcher = batcher == null ? SimpleDrawBatcher.INSTANCE : batcher;
    }

    public MinecraftGuiRenderBackend graphics(GuiGraphics graphics) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        return this;
    }

    @Override
    public void beginFrame(FrameContext frame) {
        clearScissorStack();
        pollGpuTimer();
    }

    public float lastFrameGpuMillis() {
        pollGpuTimer();
        return lastFrameGpuMillis;
    }

    @Override
    public float measureTextWidth(String text) {
        return text == null || text.isEmpty() ? 0.0f : minecraft.font.width(text);
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        return new MinecraftRenderTarget(width, height, options);
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        clearScissorStack();
        if (target == null) {
            boolean gpuTimerActive = beginGpuTimer();
            try {
                renderBatches(drawList);
            } finally {
                endGpuTimer(gpuTimerActive);
                clearScissorStack();
            }
            return;
        }

        if (!(target instanceof MinecraftRenderTarget minecraftTarget)) {
            throw new IllegalArgumentException("Unsupported render target for Minecraft backend: " + target.getClass().getName());
        }

        renderToTarget(drawList, minecraftTarget);
    }

    @Override
    public void endFrame() {
        graphics.flush();
    }

    private void renderToTarget(DrawList drawList, MinecraftRenderTarget target) {
        graphics.flush();
        target.bindWrite();
        try {
            renderBatches(drawList);
            graphics.flush();
        } finally {
            clearScissorStack();
            target.unbindWrite();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private void renderBatches(DrawList drawList) {
        for (DrawBatch batch : batcher.batch(drawList)) {
            renderBatch(batch);
            if (batch.isBarrier()) {
                graphics.flush();
            }
        }
    }

    private boolean beginGpuTimer() {
        pollGpuTimer();
        TimerQuerySupport support = timerQuerySupport();
        if (gpuTimerUnavailable || gpuTimerQueryInFlight || support == TimerQuerySupport.NONE) {
            return false;
        }

        try {
            if (gpuTimerQueryId == 0) {
                gpuTimerQueryId = GL15.glGenQueries();
            }
            gpuTimerUsesArb = support == TimerQuerySupport.ARB;
            GL15.glBeginQuery(timeElapsedTarget(), gpuTimerQueryId);
            return true;
        } catch (Throwable ignored) {
            gpuTimerUnavailable = true;
            lastFrameGpuMillis = -1.0f;
            return false;
        }
    }

    private void endGpuTimer(boolean active) {
        if (!active) return;
        try {
            GL15.glEndQuery(timeElapsedTarget());
            gpuTimerQueryInFlight = true;
        } catch (Throwable ignored) {
            gpuTimerUnavailable = true;
            gpuTimerQueryInFlight = false;
            lastFrameGpuMillis = -1.0f;
        }
    }

    private void pollGpuTimer() {
        if (!gpuTimerQueryInFlight || gpuTimerQueryId == 0 || gpuTimerUnavailable) {
            return;
        }

        try {
            int available = GL15.glGetQueryObjecti(gpuTimerQueryId, GL15.GL_QUERY_RESULT_AVAILABLE);
            if (available != GL11.GL_TRUE) {
                return;
            }
            long elapsedNanos = gpuTimerUsesArb
                    ? ARBTimerQuery.glGetQueryObjecti64(gpuTimerQueryId, GL15.GL_QUERY_RESULT)
                    : GL33.glGetQueryObjecti64(gpuTimerQueryId, GL15.GL_QUERY_RESULT);
            lastFrameGpuMillis = Math.max(0.0f, elapsedNanos / 1_000_000.0f);
            gpuTimerQueryInFlight = false;
        } catch (Throwable ignored) {
            gpuTimerUnavailable = true;
            gpuTimerQueryInFlight = false;
            lastFrameGpuMillis = -1.0f;
        }
    }

    private int timeElapsedTarget() {
        return gpuTimerUsesArb ? ARBTimerQuery.GL_TIME_ELAPSED : GL33.GL_TIME_ELAPSED;
    }

    private static TimerQuerySupport timerQuerySupport() {
        try {
            if (GL.getCapabilities() == null) {
                return TimerQuerySupport.NONE;
            }
            if (GL.getCapabilities().OpenGL33) {
                return TimerQuerySupport.OPENGL33;
            }
            if (GL.getCapabilities().GL_ARB_timer_query) {
                return TimerQuerySupport.ARB;
            }
            return TimerQuerySupport.NONE;
        } catch (Throwable ignored) {
            return TimerQuerySupport.NONE;
        }
    }

    private enum TimerQuerySupport {
        NONE,
        OPENGL33,
        ARB
    }

    private void renderBatch(DrawBatch batch) {
        for (DrawCommand command : batch.commands()) {
            renderCommand(command);
        }
    }

    private void renderCommand(DrawCommand command) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            applyTransform(command.bounds(), command.transform(), pose);
            switch (command.type()) {
                case RECT -> renderRect(command);
                case ROUNDED_RECT -> renderRoundedRect(command);
                case LINE -> renderLine(command);
                case CIRCLE -> renderCircle(command);
                case PATH -> renderPath(command);
                case TEXTURE -> renderTexture(command);
                case TEXT -> renderText(command);
                case PUSH_CLIP -> pushClip(command);
                case POP_CLIP -> popClip();
                case CUSTOM -> {
                    if (command.customDraw() != null) {
                        command.customDraw().draw(this);
                    }
                }
                case MESH -> {
                }
            }
        } finally {
            pose.popPose();
        }
    }

    private void applyTransform(RectView bounds, Transform transform, PoseStack pose) {
        if (transform == null) return;

        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();

        pose.translate(transform.position().x(), transform.position().y(), 0.0f);
        pose.translate(pivotX, pivotY, 0.0f);
        if (transform.rotationDegrees() != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(transform.rotationDegrees()));
        }
        pose.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        pose.translate(-pivotX, -pivotY, 0.0f);
    }

    private void renderRect(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        int color = argb(paint.color());
        if (paint.isStroke()) {
            graphics.renderOutline(round(bounds.x()), round(bounds.y()), round(bounds.width()), round(bounds.height()), color);
        } else {
            graphics.fill(round(bounds.x()), round(bounds.y()), round(bounds.x() + bounds.width()), round(bounds.y() + bounds.height()), color);
        }
    }

    private void pushClip(DrawCommand command) {
        RectView bounds = command.bounds();
        ScissorStack.Rect next = scissorStack.push(bounds);
        applyScissor(next);
    }

    private void popClip() {
        if (scissorStack.isEmpty()) {
            clearScissorStack();
            return;
        }

        scissorStack.pop();
        disableOneScissor();
    }

    private void applyScissor(ScissorStack.Rect rect) {
        graphics.flush();
        graphics.enableScissor(rect.x1(), rect.y1(), rect.x2(), rect.y2());
        appliedScissorDepth++;
    }

    private void clearScissorStack() {
        scissorStack.clear();
        if (appliedScissorDepth <= 0) return;

        graphics.flush();
        while (appliedScissorDepth > 0) {
            try {
                graphics.disableScissor();
            } catch (IllegalStateException ignored) {
                appliedScissorDepth = 0;
                return;
            }
            appliedScissorDepth--;
        }
    }

    private void disableOneScissor() {
        if (appliedScissorDepth <= 0) {
            clearScissorStack();
            return;
        }

        graphics.flush();
        try {
            graphics.disableScissor();
        } catch (IllegalStateException ignored) {
            appliedScissorDepth = 0;
            scissorStack.clear();
            return;
        }
        appliedScissorDepth--;
    }

    private void renderRoundedRect(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        float radius = clamp(Math.abs(command.radius()), 0.0f, Math.min(width, height) * 0.5f);

        if (width <= 0.0f || height <= 0.0f) return;
        if (radius <= 0.5f) {
            renderRect(command);
            return;
        }

        if (paint.isStroke()) {
            renderRoundedRectStroke(x1, y1, x2, y2, radius, paint);
        } else {
            renderRoundedRectFill(x1, y1, x2, y2, radius, paint);
        }
    }

    private void renderLine(DrawCommand command) {
        RectView bounds = command.bounds();
        float x1 = bounds.x();
        float y1 = bounds.y();
        float x2 = bounds.x() + bounds.width();
        float y2 = bounds.y() + bounds.height();
        drawLine(x1, y1, x2, y2, command.paint());
    }

    private void renderCircle(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        float cx = bounds.x() + bounds.width() * 0.5f;
        float cy = bounds.y() + bounds.height() * 0.5f;
        float rx = Math.abs(bounds.width()) * 0.5f;
        float ry = Math.abs(bounds.height()) * 0.5f;
        if (rx <= 0.0f || ry <= 0.0f) return;

        if (paint.isStroke()) {
            int segments = segmentsFor(Math.max(rx, ry));
            float prevX = cx + rx;
            float prevY = cy;
            for (int i = 1; i <= segments; i++) {
                double angle = Math.PI * 2.0 * i / segments;
                float x = cx + (float) Math.cos(angle) * rx;
                float y = cy + (float) Math.sin(angle) * ry;
                drawLine(prevX, prevY, x, y, paint);
                prevX = x;
                prevY = y;
            }
            return;
        }

        drawArcFan(cx, cy, rx, ry, 0.0f, (float) (Math.PI * 2.0), argb(paint.color()), segmentsFor(Math.max(rx, ry)));
    }

    private void renderPath(DrawCommand command) {
        VectorPath path = command.path();
        if (path == null || path.isEmpty()) return;

        if (!command.paint().isStroke()) {
            renderFilledPath(command);
            return;
        }

        RectView bounds = command.bounds();
        float currentX = bounds.x();
        float currentY = bounds.y();
        float startX = currentX;
        float startY = currentY;

        for (VectorPath.Element element : path.elements()) {
            switch (element.verb()) {
                case MOVE_TO -> {
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    startX = currentX;
                    startY = currentY;
                }
                case LINE_TO -> {
                    float nextX = bounds.x() + element.x1();
                    float nextY = bounds.y() + element.y1();
                    drawLine(currentX, currentY, nextX, nextY, command.paint());
                    currentX = nextX;
                    currentY = nextY;
                }
                case QUADRATIC_TO -> {
                    float controlX = bounds.x() + element.x1();
                    float controlY = bounds.y() + element.y1();
                    float nextX = bounds.x() + element.x2();
                    float nextY = bounds.y() + element.y2();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    float lastX = currentX;
                    float lastY = currentY;
                    for (int i = 1; i <= segments; i++) {
                        float t = i / (float) segments;
                        float x = quadraticX(currentX, controlX, nextX, t);
                        float y = quadraticX(currentY, controlY, nextY, t);
                        drawLine(lastX, lastY, x, y, command.paint());
                        lastX = x;
                        lastY = y;
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CUBIC_TO -> {
                    float controlX1 = bounds.x() + element.x1();
                    float controlY1 = bounds.y() + element.y1();
                    float controlX2 = bounds.x() + element.x2();
                    float controlY2 = bounds.y() + element.y2();
                    float nextX = bounds.x() + element.x3();
                    float nextY = bounds.y() + element.y3();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    float lastX = currentX;
                    float lastY = currentY;
                    for (int i = 1; i <= segments; i++) {
                        float t = i / (float) segments;
                        float x = cubicX(currentX, controlX1, controlX2, nextX, t);
                        float y = cubicX(currentY, controlY1, controlY2, nextY, t);
                        drawLine(lastX, lastY, x, y, command.paint());
                        lastX = x;
                        lastY = y;
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CLOSE -> {
                    drawLine(currentX, currentY, startX, startY, command.paint());
                    currentX = startX;
                    currentY = startY;
                }
            }
        }
    }

    private void renderRoundedRectFill(float x1, float y1, float x2, float y2, float radius, Paint paint) {
        int color = argb(paint.color());
        float innerLeft = x1 + radius;
        float innerRight = x2 - radius;
        float innerTop = y1 + radius;
        float innerBottom = y2 - radius;
        int cornerSegments = Math.max(4, segmentsFor(radius) / 4);

        drawColoredQuad(innerLeft, y1, innerRight, y2, color);
        drawColoredQuad(x1, innerTop, innerLeft, innerBottom, color);
        drawColoredQuad(innerRight, innerTop, x2, innerBottom, color);
        drawArcFan(innerLeft, innerTop, radius, radius, (float) Math.PI, (float) (Math.PI * 1.5), color, cornerSegments);
        drawArcFan(innerRight, innerTop, radius, radius, (float) (Math.PI * 1.5), (float) (Math.PI * 2.0), color, cornerSegments);
        drawArcFan(innerRight, innerBottom, radius, radius, 0.0f, (float) (Math.PI * 0.5), color, cornerSegments);
        drawArcFan(innerLeft, innerBottom, radius, radius, (float) (Math.PI * 0.5), (float) Math.PI, color, cornerSegments);
    }

    private void renderRoundedRectStroke(float x1, float y1, float x2, float y2, float radius, Paint paint) {
        float innerLeft = x1 + radius;
        float innerRight = x2 - radius;
        float innerTop = y1 + radius;
        float innerBottom = y2 - radius;
        int cornerSegments = Math.max(4, segmentsFor(radius) / 4);

        drawLine(innerLeft, y1, innerRight, y1, paint);
        drawLine(x2, innerTop, x2, innerBottom, paint);
        drawLine(innerRight, y2, innerLeft, y2, paint);
        drawLine(x1, innerBottom, x1, innerTop, paint);
        drawArcStroke(innerLeft, innerTop, radius, radius, (float) Math.PI, (float) (Math.PI * 1.5), paint, cornerSegments);
        drawArcStroke(innerRight, innerTop, radius, radius, (float) (Math.PI * 1.5), (float) (Math.PI * 2.0), paint, cornerSegments);
        drawArcStroke(innerRight, innerBottom, radius, radius, 0.0f, (float) (Math.PI * 0.5), paint, cornerSegments);
        drawArcStroke(innerLeft, innerBottom, radius, radius, (float) (Math.PI * 0.5), (float) Math.PI, paint, cornerSegments);
    }

    private void renderFilledPath(DrawCommand command) {
        RectView bounds = command.bounds();
        FloatPathBuilder points = new FloatPathBuilder();
        float currentX = bounds.x();
        float currentY = bounds.y();
        int color = argb(command.paint().color());

        for (VectorPath.Element element : command.path().elements()) {
            switch (element.verb()) {
                case MOVE_TO -> {
                    drawPolygonFan(points, color);
                    points.clear();
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    points.add(currentX, currentY);
                }
                case LINE_TO -> {
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    points.add(currentX, currentY);
                }
                case QUADRATIC_TO -> {
                    float controlX = bounds.x() + element.x1();
                    float controlY = bounds.y() + element.y1();
                    float nextX = bounds.x() + element.x2();
                    float nextY = bounds.y() + element.y2();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    for (int i = 1; i <= segments; i++) {
                        float t = i / (float) segments;
                        points.add(quadraticX(currentX, controlX, nextX, t), quadraticX(currentY, controlY, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CUBIC_TO -> {
                    float controlX1 = bounds.x() + element.x1();
                    float controlY1 = bounds.y() + element.y1();
                    float controlX2 = bounds.x() + element.x2();
                    float controlY2 = bounds.y() + element.y2();
                    float nextX = bounds.x() + element.x3();
                    float nextY = bounds.y() + element.y3();
                    int segments = curveSegments(currentX, currentY, nextX, nextY);
                    for (int i = 1; i <= segments; i++) {
                        float t = i / (float) segments;
                        points.add(cubicX(currentX, controlX1, controlX2, nextX, t), cubicX(currentY, controlY1, controlY2, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CLOSE -> {
                    drawPolygonFan(points, color);
                    points.clear();
                }
            }
        }

        drawPolygonFan(points, color);
    }

    private void renderTexture(DrawCommand command) {
        TextureHandle texture = command.texture();
        if (texture == null) return;

        Object nativeHandle = texture.nativeHandle();
        if (nativeHandle instanceof MinecraftRenderTarget.ColorTextureHandle colorTexture) {
            renderTextureId(colorTexture.textureId(), colorTexture.flipY(), command);
            return;
        }
        if (nativeHandle instanceof Integer textureId) {
            renderTextureId(textureId, false, command);
            return;
        }

        ResourceLocation location = resolveTexture(command.texture());
        if (location == null) return;

        RectView bounds = command.bounds();
        int textureWidth = Math.max(1, texture.width());
        int textureHeight = Math.max(1, texture.height());
        ColorView tint = command.paint().color();
        graphics.setColor(tint.r(), tint.g(), tint.b(), tint.a());
        try {
            graphics.blit(location,
                    round(bounds.x()),
                    round(bounds.y()),
                    round(bounds.width()),
                    round(bounds.height()),
                    0.0f,
                    0.0f,
                    textureWidth,
                    textureHeight,
                    textureWidth,
                    textureHeight);
        } finally {
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void renderTextureId(int textureId, boolean flipY, DrawCommand command) {
        RectView bounds = command.bounds();
        ColorView tint = command.paint().color();

        float minV = flipY ? 1.0f : 0.0f;
        float maxV = flipY ? 0.0f : 1.0f;
        int x1 = round(bounds.x());
        int y1 = round(bounds.y());
        int x2 = round(bounds.x() + bounds.width());
        int y2 = round(bounds.y() + bounds.height());

        graphics.flush();
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        addTextureVertex(buffer, matrix, x1, y1, 0.0f, minV, tint);
        addTextureVertex(buffer, matrix, x1, y2, 0.0f, maxV, tint);
        addTextureVertex(buffer, matrix, x2, y2, 1.0f, maxV, tint);
        addTextureVertex(buffer, matrix, x2, y1, 1.0f, minV, tint);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void renderText(DrawCommand command) {
        String text = command.text();
        if (text == null || text.isEmpty()) return;
        RectView bounds = command.bounds();
        graphics.drawString(minecraft.font, text, round(bounds.x()), round(bounds.y()), argb(command.paint().color()), false);
    }

    private void drawLine(float x1, float y1, float x2, float y2, Paint paint) {
        int color = argb(paint.color());
        int thickness = Math.max(1, round(paint.strokeWidth()));
        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, Math.max(Math.abs(round(dx)), Math.abs(round(dy))));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            int x = round(x1 + dx * t);
            int y = round(y1 + dy * t);
            graphics.fill(x, y, x + thickness, y + thickness, color);
        }
    }

    private void drawArcStroke(float cx, float cy, float rx, float ry, float startAngle, float endAngle, Paint paint, int segments) {
        float prevX = cx + (float) Math.cos(startAngle) * rx;
        float prevY = cy + (float) Math.sin(startAngle) * ry;
        for (int i = 1; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float x = cx + (float) Math.cos(angle) * rx;
            float y = cy + (float) Math.sin(angle) * ry;
            drawLine(prevX, prevY, x, y, paint);
            prevX = x;
            prevY = y;
        }
    }

    private void drawArcFan(float cx, float cy, float rx, float ry, float startAngle, float endAngle, int color, int segments) {
        if (rx <= 0.0f || ry <= 0.0f || segments <= 0) return;

        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float prevX = cx + (float) Math.cos(startAngle) * rx;
        float prevY = cy + (float) Math.sin(startAngle) * ry;
        for (int i = 1; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float x = cx + (float) Math.cos(angle) * rx;
            float y = cy + (float) Math.sin(angle) * ry;
            addColorVertex(buffer, matrix, cx, cy, color);
            addColorVertex(buffer, matrix, prevX, prevY, color);
            addColorVertex(buffer, matrix, x, y, color);
            prevX = x;
            prevY = y;
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void drawColoredQuad(float x1, float y1, float x2, float y2, int color) {
        if (x2 <= x1 || y2 <= y1) return;

        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addColorVertex(buffer, matrix, x1, y1, color);
        addColorVertex(buffer, matrix, x1, y2, color);
        addColorVertex(buffer, matrix, x2, y2, color);
        addColorVertex(buffer, matrix, x2, y1, color);
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private void drawPolygonFan(FloatPathBuilder points, int color) {
        if (points.size() < 3) return;

        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float originX = points.x(0);
        float originY = points.y(0);
        for (int i = 1; i < points.size() - 1; i++) {
            addColorVertex(buffer, matrix, originX, originY, color);
            addColorVertex(buffer, matrix, points.x(i), points.y(i), color);
            addColorVertex(buffer, matrix, points.x(i + 1), points.y(i + 1), color);
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableBlend();
    }

    private static void addTextureVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float u, float v, ColorView tint) {
        buffer.vertex(matrix, x, y, 0.0f)
                .uv(u, v)
                .color(channel(tint.r()), channel(tint.g()), channel(tint.b()), channel(tint.a()))
                .endVertex();
    }

    private static void addColorVertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        buffer.vertex(matrix, x, y, 0.0f)
                .color((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF)
                .endVertex();
    }

    private ResourceLocation resolveTexture(TextureHandle texture) {
        if (texture == null) return null;
        Object nativeHandle = texture.nativeHandle();
        if (nativeHandle instanceof ResourceLocation location) {
            return location;
        }
        return ResourceLocation.tryParse(texture.id());
    }

    private static int argb(ColorView color) {
        int a = channel(color.a());
        int r = channel(color.r());
        int g = channel(color.g());
        int b = channel(color.b());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    private static int round(float value) {
        return Math.round(value);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int segmentsFor(float radius) {
        return Math.max(16, Math.min(96, Math.round(radius * 0.5f)));
    }

    private static int curveSegments(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return Math.max(8, Math.min(48, Math.round(distance * 0.25f)));
    }

    private static float quadraticX(float x0, float x1, float x2, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * x0 + 2.0f * inverse * t * x1 + t * t * x2;
    }

    private static float cubicX(float x0, float x1, float x2, float x3, float t) {
        float inverse = 1.0f - t;
        float inverse2 = inverse * inverse;
        float t2 = t * t;
        return inverse2 * inverse * x0 + 3.0f * inverse2 * t * x1 + 3.0f * inverse * t2 * x2 + t2 * t * x3;
    }

    private static final class FloatPathBuilder {
        private float[] values = new float[16];
        private int size;

        void add(float x, float y) {
            ensureCapacity(size + 2);
            values[size++] = x;
            values[size++] = y;
        }

        void clear() {
            size = 0;
        }

        int size() {
            return size / 2;
        }

        float x(int index) {
            return values[index * 2];
        }

        float y(int index) {
            return values[index * 2 + 1];
        }

        private void ensureCapacity(int required) {
            if (required <= values.length) return;
            values = Arrays.copyOf(values, Math.max(required, values.length * 2));
        }
    }
}
