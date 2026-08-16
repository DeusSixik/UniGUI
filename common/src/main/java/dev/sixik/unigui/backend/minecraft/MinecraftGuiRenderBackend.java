package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.DrawBatcher;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class MinecraftGuiRenderBackend implements RenderBackend, AutoCloseable {
    /**
     * Vanilla Font treats colors with alpha 0..3 as "alpha not specified" and promotes them
     * to opaque, which causes fade-in text to flash on the first visible frame.
     */
    private static final int MIN_VANILLA_TEXT_ALPHA_CHANNEL = 4;

    private final Minecraft minecraft;
    private final DrawBatcher batcher;
    private final ScissorStack scissorStack = new ScissorStack();
    private final MinecraftSdfTextRenderer sdfTextRenderer =
            new MinecraftSdfTextRenderer(DefaultFontRegistry.global());
    private final MinecraftMixedTextRenderer mixedTextRenderer;
    private final MinecraftShapeBatchRenderer shapeBatchRenderer = new MinecraftShapeBatchRenderer();
    private final MinecraftTextureBatchRenderer textureBatchRenderer = new MinecraftTextureBatchRenderer();
    private final MinecraftShaderQuadRenderer shaderQuadRenderer = new MinecraftShaderQuadRenderer();
    private final FastItemRenderer fastItemRenderer;
    private GuiGraphics graphics;
    private int appliedScissorDepth;
    private MinecraftRenderTarget activeRenderTarget;
    private float activeRenderTargetScaleX = 1.0f;
    private float activeRenderTargetScaleY = 1.0f;
    private float nestedClipCoordinateScale = 1.0f;
    private boolean renderTargetScissorEnabled;
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
        this.sdfTextRenderer.defaultFace(MinecraftFonts.defaultFace());
        this.mixedTextRenderer = new MinecraftMixedTextRenderer(this.minecraft, sdfTextRenderer);
        this.fastItemRenderer = new FastItemRenderer(this.minecraft);
    }

    public MinecraftGuiRenderBackend graphics(GuiGraphics graphics) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        return this;
    }

    @Override
    public void beginFrame(FrameContext frame) {
        clearScissorStack();
        pollGpuTimer();
        fastItemRenderer.beginFrame();
    }

    public float lastFrameGpuMillis() {
        pollGpuTimer();
        return lastFrameGpuMillis;
    }

    public Minecraft minecraft() {
        return minecraft;
    }

    public GuiGraphics graphics() {
        return graphics;
    }

    /** Font used by plain String text commands that do not provide a RichText face. */
    public FontFace defaultFont() {
        return sdfTextRenderer.defaultFace();
    }

    @Override
    public FontFace defaultTextFace() {
        return defaultFont();
    }

    public MinecraftGuiRenderBackend defaultFont(FontFace font) {
        sdfTextRenderer.defaultFace(font);
        return this;
    }

    public MinecraftGuiRenderBackend useVanillaDefaultFont() {
        return defaultFont(MinecraftFonts.defaultFace());
    }

    public void renderItemPreview(ItemStack stack, float x, float y, float size, float opacity, boolean decorations) {
        if (stack == null || stack.isEmpty() || size <= 0.0f) return;
        if (!decorations) {
            TextureHandle cached = fastItemRenderer.cachedTexture(stack, size);
            if (cached != null) {
                renderItemPreviewTexture(cached, x, y, size, opacity);
                return;
            }

            if (fastItemRenderer.prefersCachedPath(stack)) {
                TextureHandle baked = fastItemRenderer.bakeIfBudget(stack, size, activeRenderTarget);
                if (baked != null) {
                    renderItemPreviewTexture(baked, x, y, size, opacity);
                    return;
                }
            }
        }
        renderVanillaItemPreview(stack, x, y, size, opacity, decorations);
    }

    public boolean renderItemPreviewLazy(ItemStack stack, float x, float y, float size, float opacity) {
        if (stack == null || stack.isEmpty() || size <= 0.0f) return false;

        TextureHandle cached = fastItemRenderer.cachedTexture(stack, size);
        if (cached != null) {
            renderItemPreviewTexture(cached, x, y, size, opacity);
            return true;
        }

        if (fastItemRenderer.prefersCachedPath(stack)) {
            TextureHandle baked = fastItemRenderer.bakeIfBudget(stack, size, activeRenderTarget);
            if (baked != null) {
                renderItemPreviewTexture(baked, x, y, size, opacity);
                return true;
            }
            return false;
        }

        renderVanillaItemPreview(stack, x, y, size, opacity, false);
        return true;
    }

    public TextureHandle cachedItemPreviewTexture(ItemStack stack, float size) {
        return fastItemRenderer.cachedTexture(stack, size);
    }

    public void clearItemPreviewCache() {
        fastItemRenderer.clear();
    }

    public void renderVanillaTooltip(Component line, float x, float y) {
        if (line == null) return;
        renderVanillaTooltip(List.of(line), x, y);
    }

    public void renderVanillaTooltip(List<Component> lines, float x, float y) {
        if (lines == null || lines.isEmpty()) return;

        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(activeRenderTarget != null);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            graphics.renderComponentTooltip(minecraft.font, lines, round(x), round(y));
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            state.restore();
        }
    }

    public void renderVanillaItemTooltip(ItemStack stack, float x, float y) {
        renderVanillaTooltip(stack, x, y);
    }

    public void renderVanillaTooltip(ItemStack stack, float x, float y) {
        if (stack == null || stack.isEmpty()) return;

        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(activeRenderTarget != null);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            graphics.renderTooltip(minecraft.font, stack, round(x), round(y));
            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            state.restore();
        }
    }

    private void renderItemPreviewTexture(TextureHandle texture, float x, float y, float size, float opacity) {
        DrawCommand command = DrawCommand.texture(
                texture,
                new MutableRect(x, y, size, size),
                Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, clamp01(opacity))));
        renderTexture(command);
    }

    private void renderVanillaItemPreview(ItemStack stack, float x, float y, float size, float opacity, boolean decorations) {
        float scale = Math.max(0.01f, size / 16.0f);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, clamp01(opacity));
            pose.translate(x, y, 160.0f);
            pose.scale(scale, scale, 1.0f);
            graphics.renderItem(stack, 0, 0);
            if (decorations) {
                graphics.renderItemDecorations(minecraft.font, stack, 0, 0);
            }
            clearPreviewDepthBuffer();
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            pose.popPose();
        }
    }

    public boolean renderEntityPreview(EntityType<? extends LivingEntity> entityType, float x, float y, float size,
                                       float mouseX, float mouseY, float opacity) {
        if (entityType == null || minecraft.level == null || size <= 0.0f) return false;
        LivingEntity entity = entityType.create(minecraft.level);
        if (entity == null) return false;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, clamp01(opacity));
        try {
            int centerX = Math.round(x + size * 0.5f);
            int bottomY = Math.round(y + size * 0.92f);
            int entityScale = Math.max(1, Math.round(size * 0.72f));
            renderEntityInInventory(centerX, bottomY, entityScale, size, x + mouseX, y + mouseY, entity);
            clearPreviewDepthBuffer();
            return true;
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void clearPreviewDepthBuffer() {
        graphics.flush();
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.depthMask(depthMask);
    }

    private void renderEntityInInventory(int centerX, int bottomY, int entityScale, float size,
                                         float mouseX, float mouseY, LivingEntity entity) {
        MinecraftEntityPreviewCompat.render(graphics, centerX, bottomY, entityScale, size, mouseX, mouseY, entity);
    }

    @Override
    public float measureTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(
                RichText.of(text, defaultFont(), TextRun.DEFAULT_PIXEL_SIZE));
    }

    @Override
    public float measureTextWidth(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(resolveDefaultFont(text));
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
        activeRenderTarget = target;
        activeRenderTargetScaleX = renderTargetScaleX(target);
        activeRenderTargetScaleY = renderTargetScaleY(target);
        try {
            renderBatches(drawList);
            graphics.flush();
        } finally {
            clearScissorStack();
            activeRenderTarget = null;
            activeRenderTargetScaleX = 1.0f;
            activeRenderTargetScaleY = 1.0f;
            target.unbindWrite();
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private float renderTargetScaleX(MinecraftRenderTarget target) {
        if (minecraft.getWindow() == null || minecraft.getWindow().getGuiScaledWidth() <= 0) return 1.0f;
        return target.width() == minecraft.getWindow().getWidth()
                ? target.width() / (float) minecraft.getWindow().getGuiScaledWidth()
                : 1.0f;
    }

    private float renderTargetScaleY(MinecraftRenderTarget target) {
        if (minecraft.getWindow() == null || minecraft.getWindow().getGuiScaledHeight() <= 0) return 1.0f;
        return target.height() == minecraft.getWindow().getHeight()
                ? target.height() / (float) minecraft.getWindow().getGuiScaledHeight()
                : 1.0f;
    }

    private void renderBatches(DrawList drawList) {
        List<DrawBatch> batches = batcher.batch(drawList);
        if (batches instanceof it.unimi.dsi.fastutil.objects.ObjectArrayList<?> objectBatches) {
            Object[] rawBatches = objectBatches.elements();
            for (int i = 0, size = objectBatches.size(); i < size; i++) {
                DrawBatch batch = (DrawBatch) rawBatches[i];
                renderBatch(batch);
                if (batch.isBarrier()) {
                    graphics.flush();
                }
            }
            return;
        }

        for (int i = 0, size = batches.size(); i < size; i++) {
            DrawBatch batch = batches.get(i);
            renderBatch(batch);
            if (batch.isBarrier()) {
                graphics.flush();
            }
        }
    }

    void renderNested(DrawList drawList) {
        renderNested(drawList, 1.0f);
    }

    public void renderNested(DrawList drawList, float coordinateScale) {
        float previousScale = nestedClipCoordinateScale;
        nestedClipCoordinateScale *= sanitizeScale(coordinateScale);
        try {
            renderBatches(drawList);
            graphics.flush();
        } finally {
            nestedClipCoordinateScale = previousScale;
        }
    }

    private boolean beginGpuTimer() {
        pollGpuTimer();
        TimerQuerySupport support = timerQuerySupport();
        if (gpuTimerUnavailable || gpuTimerQueryInFlight || support == TimerQuerySupport.NONE) {
            return false;
        }

        try {
            if (gpuTimerQueryId != 0 && !isGpuTimerQueryObject()) {
                disableGpuTimer();
                return false;
            }
            if (gpuTimerQueryId == 0) {
                gpuTimerQueryId = GL15.glGenQueries();
                if (gpuTimerQueryId == 0) {
                    disableGpuTimer();
                    return false;
                }
            }
            gpuTimerUsesArb = support == TimerQuerySupport.ARB;
            GL15.glBeginQuery(timeElapsedTarget(), gpuTimerQueryId);
            return true;
        } catch (Throwable ignored) {
            disableGpuTimer();
            return false;
        }
    }

    private void endGpuTimer(boolean active) {
        if (!active) return;
        try {
            GL15.glEndQuery(timeElapsedTarget());
            gpuTimerQueryInFlight = true;
        } catch (Throwable ignored) {
            disableGpuTimer();
        }
    }

    private void pollGpuTimer() {
        if (!gpuTimerQueryInFlight || gpuTimerQueryId == 0 || gpuTimerUnavailable) {
            return;
        }

        try {
            if (!isGpuTimerQueryObject()) {
                disableGpuTimer();
                return;
            }
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
            disableGpuTimer();
        }
    }

    private boolean isGpuTimerQueryObject() {
        try {
            return gpuTimerQueryId != 0 && GL15.glIsQuery(gpuTimerQueryId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void disableGpuTimer() {
        gpuTimerUnavailable = true;
        gpuTimerQueryId = 0;
        gpuTimerQueryInFlight = false;
        lastFrameGpuMillis = -1.0f;
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
        boolean renderingToPremultipliedTarget = activeRenderTarget != null;
        if (MinecraftShapeBatchRenderer.supports(batch.type())
                && shapeBatchRenderer.render(graphics, batch, renderingToPremultipliedTarget)) {
            return;
        }
        if (batch.type() == DrawCommandType.TEXT
                && mixedTextRenderer.render(graphics, batch, graphics.pose(), renderingToPremultipliedTarget)) {
            return;
        }
        if (batch.type() == DrawCommandType.TEXT
                && sdfTextRenderer.render(graphics, batch, graphics.pose(), renderingToPremultipliedTarget)) {
            return;
        }
        if (batch.type() == DrawCommandType.TEXTURE
                && textureBatchRenderer.render(graphics, batch, renderingToPremultipliedTarget)) {
            return;
        }
        Object[] rawCommands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            renderCommand((DrawCommand) rawCommands[i]);
        }
    }

    @Override
    public void close() {
        clearScissorStack();
        fastItemRenderer.close();
        shapeBatchRenderer.close();
        shaderQuadRenderer.close();
        sdfTextRenderer.close();
        if (gpuTimerQueryId != 0 && isGpuTimerQueryObject()) {
            GL15.glDeleteQueries(gpuTimerQueryId);
        }
        gpuTimerQueryId = 0;
        gpuTimerQueryInFlight = false;
    }

    private void renderCommand(DrawCommand command) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            MinecraftTransform.apply(command, pose);
            switch (command.type()) {
                case RECT -> renderRect(command);
                case ROUNDED_RECT -> renderRoundedRect(command);
                case LINE -> renderLine(command);
                case CIRCLE -> renderCircle(command);
                case PATH -> renderPath(command);
                case TEXTURE -> renderTexture(command);
                case SHADER -> renderShader(command);
                case TEXT -> renderText(command);
                case PUSH_CLIP -> pushClip(command);
                case POP_CLIP -> popClip();
                case DRAW_CMD -> {
                }
                case CUSTOM -> {
                    if (command.customDraw() != null) {
                        command.customDraw().draw(this);
                    }
                }
                case MESH -> renderMesh(command);
            }
        } finally {
            pose.popPose();
        }
    }

    private void renderShader(DrawCommand command) {
        shaderQuadRenderer.render(
                graphics,
                minecraft,
                command,
                activeRenderTarget != null,
                shaderScreenWidth(),
                shaderScreenHeight(),
                shaderGuiScale());
    }

    private float shaderScreenWidth() {
        if (activeRenderTarget != null) {
            return Math.max(1.0f, activeRenderTarget.width() / sanitizeScale(activeRenderTargetScaleX));
        }
        return minecraft.getWindow() == null
                ? 1.0f
                : Math.max(1.0f, minecraft.getWindow().getGuiScaledWidth());
    }

    private float shaderScreenHeight() {
        if (activeRenderTarget != null) {
            return Math.max(1.0f, activeRenderTarget.height() / sanitizeScale(activeRenderTargetScaleY));
        }
        return minecraft.getWindow() == null
                ? 1.0f
                : Math.max(1.0f, minecraft.getWindow().getGuiScaledHeight());
    }

    private float shaderGuiScale() {
        if (activeRenderTarget != null) return sanitizeScale(activeRenderTargetScaleX);
        if (minecraft.getWindow() == null || minecraft.getWindow().getGuiScaledWidth() <= 0) return 1.0f;
        return sanitizeScale(minecraft.getWindow().getWidth() / (float) minecraft.getWindow().getGuiScaledWidth());
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
        RectView bounds = scaledClipBounds(command.bounds());
        ScissorStack.Rect next = scissorStack.push(bounds);
        applyScissor(next);
    }

    private RectView scaledClipBounds(RectView bounds) {
        float scale = nestedClipCoordinateScale;
        if (scale == 1.0f) return bounds;
        return new MutableRect(
                bounds.x() * scale,
                bounds.y() * scale,
                bounds.width() * scale,
                bounds.height() * scale);
    }

    private static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }

    private void popClip() {
        if (scissorStack.isEmpty()) {
            clearScissorStack();
            return;
        }

        ScissorStack.Rect parent = scissorStack.pop();
        if (activeRenderTarget != null) {
            if (parent == null) {
                disableRenderTargetScissor();
            } else {
                applyRenderTargetScissor(parent);
            }
            return;
        }
        disableOneScissor();
    }

    private void applyScissor(ScissorStack.Rect rect) {
        if (activeRenderTarget != null) {
            applyRenderTargetScissor(rect);
            return;
        }
        graphics.flush();
        graphics.enableScissor(rect.x1(), rect.y1(), rect.x2(), rect.y2());
        appliedScissorDepth++;
    }

    private void clearScissorStack() {
        scissorStack.clear();
        disableRenderTargetScissor();
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

    private void applyRenderTargetScissor(ScissorStack.Rect rect) {
        int targetWidth = activeRenderTarget.width();
        int targetHeight = activeRenderTarget.height();
        int x1 = Math.max(0, Math.min(targetWidth, Math.round(rect.x1() * activeRenderTargetScaleX)));
        int y1 = Math.max(0, Math.min(targetHeight, Math.round(rect.y1() * activeRenderTargetScaleY)));
        int x2 = Math.max(x1, Math.min(targetWidth, Math.round(rect.x2() * activeRenderTargetScaleX)));
        int y2 = Math.max(y1, Math.min(targetHeight, Math.round(rect.y2() * activeRenderTargetScaleY)));

        graphics.flush();
        RenderSystem.enableScissor(x1, targetHeight - y2, x2 - x1, y2 - y1);
        renderTargetScissorEnabled = true;
    }

    private void disableRenderTargetScissor() {
        if (!renderTargetScissorEnabled) return;
        graphics.flush();
        RenderSystem.disableScissor();
        renderTargetScissorEnabled = false;
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

        Object[] rawPathElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawPathElements[i];
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
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
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
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
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
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
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
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
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

        TextureBinding binding = TextureBinding.resolve(texture);
        if (binding != null) {
            renderTextureBinding(binding, command);
        }
    }

    private void renderTextureBinding(TextureBinding binding, DrawCommand command) {
        RectView bounds = command.bounds();
        RectView uv = command.uv();
        ColorView tint = command.paint().color();

        float minU = uv.x();
        float maxU = uv.x() + uv.width();
        float minV = binding.flipY() ? uv.y() + uv.height() : uv.y();
        float maxV = binding.flipY() ? uv.y() : uv.y() + uv.height();
        int x1 = round(bounds.x());
        int y1 = round(bounds.y());
        int x2 = round(bounds.x() + bounds.width());
        int y2 = round(bounds.y() + bounds.height());

        graphics.flush();
        RenderState state = RenderState.capture();
        MinecraftTextureSamplerState.Scope sampler = null;
        try {
            binding.bind();
            sampler = MinecraftTextureSamplerState.apply(binding.options());
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyTextureAlpha(binding.premultipliedAlpha(), activeRenderTarget != null);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            Matrix4f matrix = graphics.pose().last().pose();
            Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            addTextureVertex(buffer, matrix, x1, y1, minU, minV, tint);
            addTextureVertex(buffer, matrix, x1, y2, minU, maxV, tint);
            addTextureVertex(buffer, matrix, x2, y2, maxU, maxV, tint);
            addTextureVertex(buffer, matrix, x2, y1, maxU, minV, tint);
            MinecraftBufferCompat.drawWithShader(buffer);
        } finally {
            if (sampler != null) sampler.close();
            state.restore();
        }
    }

    private void renderMesh(DrawCommand command) {
        DrawMesh mesh = command.mesh();
        if (mesh == null || mesh.isEmpty()) return;
        if (command.texture() == null) {
            renderColoredMesh(mesh);
            return;
        }
        TextureBinding binding = TextureBinding.resolve(command.texture());
        if (binding != null) {
            renderTexturedMesh(mesh, binding);
        }
    }

    private void renderColoredMesh(DrawMesh mesh) {
        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(activeRenderTarget != null);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Matrix4f matrix = graphics.pose().last().pose();
            Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            Object[] rawVertices = mesh.vertexElements();
            for (int i = 0, size = mesh.vertexCount(); i < size; i++) {
                DrawVertex vertex = (DrawVertex) rawVertices[i];
                addColorVertex(buffer, matrix, vertex.x(), vertex.y(), argb(vertex.color()));
            }
            MinecraftBufferCompat.drawWithShader(buffer);
        } finally {
            state.restore();
        }
    }

    private void renderTexturedMesh(DrawMesh mesh, TextureBinding binding) {
        graphics.flush();
        RenderState state = RenderState.capture();
        MinecraftTextureSamplerState.Scope sampler = null;
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            binding.bind();
            sampler = MinecraftTextureSamplerState.apply(binding.options());
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyTextureAlpha(binding.premultipliedAlpha(), activeRenderTarget != null);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Matrix4f matrix = graphics.pose().last().pose();
            Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
            Object[] rawVertices = mesh.vertexElements();
            for (int i = 0, size = mesh.vertexCount(); i < size; i++) {
                DrawVertex vertex = (DrawVertex) rawVertices[i];
                float v = binding.flipY() ? 1.0f - vertex.v() : vertex.v();
                addTextureVertex(buffer, matrix, vertex.x(), vertex.y(), vertex.u(), v, vertex.color());
            }
            MinecraftBufferCompat.drawWithShader(buffer);
        } finally {
            if (sampler != null) sampler.close();
            state.restore();
        }
    }

    private void renderText(DrawCommand command) {
        String text = command.text() != null
                ? command.text()
                : command.richText() == null ? null : command.richText().plainText();
        if (text == null || text.isEmpty()) return;
        if (alphaChannel(command.paint().color()) < MIN_VANILLA_TEXT_ALPHA_CHANNEL) return;
        RectView bounds = command.bounds();
        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            graphics.drawString(minecraft.font, text, round(bounds.x()), round(bounds.y()), argb(command.paint().color()), false);
            graphics.flush();
        } finally {
            state.restore();
        }
    }

    private RichText resolveDefaultFont(RichText text) {
        RichText.Builder builder = RichText.builder();
        for (TextRun run : text.runs()) {
            builder.font(run.font() == null ? defaultFont() : run.font())
                    .size(run.pixelSize())
                    .color(run.color())
                    .tracking(run.tracking())
                    .transform(run.transform())
                    .append(run.text());
        }
        return builder.build();
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
        Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
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
        MinecraftBufferCompat.drawWithShader(buffer);
        RenderSystem.disableBlend();
    }

    private void drawColoredQuad(float x1, float y1, float x2, float y2, int color) {
        if (x2 <= x1 || y2 <= y1) return;

        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addColorVertex(buffer, matrix, x1, y1, color);
        addColorVertex(buffer, matrix, x1, y2, color);
        addColorVertex(buffer, matrix, x2, y2, color);
        addColorVertex(buffer, matrix, x2, y1, color);
        MinecraftBufferCompat.drawWithShader(buffer);
        RenderSystem.disableBlend();
    }

    private void drawPolygonFan(FloatPathBuilder points, int color) {
        if (points.size() < 3) return;

        graphics.flush();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix = graphics.pose().last().pose();
        Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float originX = points.x(0);
        float originY = points.y(0);
        for (int i = 1; i < points.size() - 1; i++) {
            addColorVertex(buffer, matrix, originX, originY, color);
            addColorVertex(buffer, matrix, points.x(i), points.y(i), color);
            addColorVertex(buffer, matrix, points.x(i + 1), points.y(i + 1), color);
        }
        MinecraftBufferCompat.drawWithShader(buffer);
        RenderSystem.disableBlend();
    }

    private static void addTextureVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v, ColorView tint) {
        MinecraftBufferCompat.textureColorVertex(buffer, matrix, x, y, u, v, argb(tint));
    }

    private static void addColorVertex(Object buffer, Matrix4f matrix, float x, float y, int color) {
        MinecraftBufferCompat.colorVertex(buffer, matrix, x, y, color);
    }

    private static int argb(ColorView color) {
        int a = channel(color.a());
        int r = channel(color.r());
        int g = channel(color.g());
        int b = channel(color.b());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int alphaChannel(ColorView color) {
        return color == null ? 255 : channel(color.a());
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

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
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

    private record TextureBinding(Integer textureId, ResourceLocation location, boolean flipY,
                                  boolean premultipliedAlpha, TextureOptions options) {
        private static TextureBinding resolve(TextureHandle texture) {
            if (texture == null) return null;
            TextureOptions options = texture.options() == null ? TextureOptions.defaults() : texture.options();
            Object nativeHandle = texture.nativeHandle();
            if (nativeHandle instanceof MinecraftRenderTarget.ColorTextureHandle colorTexture) {
                return new TextureBinding(colorTexture.textureId(), null, colorTexture.flipY(), true, options);
            }
            if (nativeHandle instanceof Integer textureId) {
                return new TextureBinding(textureId, null, false, options.premultipliedAlpha(), options);
            }
            ResourceLocation location = nativeHandle instanceof ResourceLocation resourceLocation
                    ? resourceLocation
                    : ResourceLocation.tryParse(texture.id());
            return location == null ? null : new TextureBinding(null, location, false, options.premultipliedAlpha(), options);
        }

        private void bind() {
            if (textureId != null) {
                RenderSystem.setShaderTexture(0, textureId);
            } else {
                RenderSystem.setShaderTexture(0, location);
            }
        }
    }

    private record RenderState(int activeTexture, int texture, boolean blend,
                               int blendSourceRgb, int blendDestinationRgb,
                               int blendSourceAlpha, int blendDestinationAlpha,
                               boolean depthTest, boolean depthMask, boolean cull) {
        private static RenderState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(activeTexture);
            return new RenderState(
                    activeTexture,
                    texture,
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE));
        }

        private void restore() {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(texture);
            RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                    blendSourceAlpha, blendDestinationAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.depthMask(depthMask);
            if (depthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.activeTexture(activeTexture);
        }
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
