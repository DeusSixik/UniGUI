package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.posteffect.UiLayerBounds;
import dev.sixik.unigui.api.posteffect.UiPostEffectBackend;
import dev.sixik.unigui.api.posteffect.UiPostEffectBlendMode;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffectPass;
import dev.sixik.unigui.api.posteffect.UiPostEffectRegistry;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.impl.render.RenderTargetCache;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.*;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.DrawBatcher;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.text.TextEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Immediate-mode renderer for the Minecraft 1.12.2 GUI pipeline.
 *
 * <p>All geometry goes through the vanilla {@link net.minecraft.client.renderer.Tessellator} with per-vertex colors, so no draw leaks a
 * current color into the next one, and all state changes go through {@link GlStateManager}
 * (see {@link CleanroomGlState}). Clip commands are kept on a {@link ScissorStack}, so nested clips intersect
 * and {@code POP_CLIP} restores the parent clip instead of disabling the scissor test.</p>
 */
final class CleanroomGuiRenderBackend implements RenderBackend, UiPostEffectBackend, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final RenderTargetOptions POST_EFFECT_LAYER_TARGET =
            new RenderTargetOptions(false, true, "post_effect_layer");
    private static final RenderTargetOptions POST_EFFECT_PING_TARGET =
            new RenderTargetOptions(false, true, "post_effect_ping");

    /**
     * Vanilla {@code FontRenderer} treats an alpha below 4 as "no alpha given" and draws opaque text.
     */
    private static final int MIN_VANILLA_TEXT_ALPHA = 4;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final CleanroomRenderHost screen;
    private final Pass pass = new Pass();
    private final CleanroomGlState glState = new CleanroomGlState();
    private final DrawBatcher batcher = SimpleDrawBatcher.INSTANCE;
    private final ScissorStack scissorStack = new ScissorStack();
    private final MutableRect clipUi = new MutableRect();
    private final MutableRect clipWindow = new MutableRect();
    private final float[] transformScratch = new float[2];
    private final int[] scissorBox = new int[4];
    private final CleanroomTextRenderer textRenderer;
    private final CleanroomShapeRenderer shapeRenderer;
    private final CleanroomShaderQuadRenderer shaderRenderer = new CleanroomShaderQuadRenderer();
    private final RenderTargetCache postEffectLayerTargets = new RenderTargetCache(this);
    private final RenderTargetCache postEffectPingTargets = new RenderTargetCache(this);
    private final DrawList postEffectPassList = new DrawList();
    private final DrawList postEffectCompositeList = new DrawList();
    private boolean shaderDrawn;
    private boolean postEffectsBroken;
    private FontFace defaultFont = MinecraftFonts.defaultFace();
    private boolean frameActive;

    CleanroomGuiRenderBackend(CleanroomRenderHost screen) {
        this.screen = screen;
        this.textRenderer = new CleanroomTextRenderer(pass);
        this.shapeRenderer = new CleanroomShapeRenderer(pass);
    }

    @Override
    public float measureTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(RichText.of(text, defaultFont, TextRun.DEFAULT_PIXEL_SIZE));
    }

    @Override
    public float measureTextWidth(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(withDefaultFont(text));
    }

    /**
     * Resolves runs without a face to the backend default, so measuring matches what is drawn.
     */
    private RichText withDefaultFont(RichText text) {
        RichText.Builder builder = RichText.builder();
        for (TextRun run : text.runs()) {
            builder.font(run.font() == null ? defaultFont : run.font())
                    .size(run.pixelSize())
                    .color(run.color())
                    .brush(run.brush())
                    .tracking(run.tracking())
                    .transform(run.transform())
                    .append(run.text());
        }
        return builder.build();
    }

    @Override
    public FontFace defaultTextFace() {
        return defaultFont;
    }

    CleanroomGuiRenderBackend defaultFont(FontFace font) {
        defaultFont = font == null ? MinecraftFonts.defaultFace() : font;
        textRenderer.defaultFace(defaultFont);
        return this;
    }

    @Override
    public void beginFrame(FrameContext frame) {
        frameActive = true;
        CleanroomGlCapabilities.get();
        glState.capture();
        glState.applyUiDefaults();
        scissorStack.clear();
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        return new CleanroomRenderTarget(width, height, options);
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        if (target == null) {
            if (drawList == null || drawList.size() == 0) return;
            renderBatches(drawList);
            return;
        }
        if (!(target instanceof CleanroomRenderTarget own)) {
            throw new IllegalArgumentException("Unsupported render target for the Cleanroom backend: "
                    + target.getClass().getName());
        }
        // A window-sized target holds the same picture as the screen, so it keeps the screen's unit scale; any other
        // target is addressed in its own pixels.
        boolean windowSized = own.width() == screen.framebufferWidth();
        renderToTarget(drawList, own, windowSized ? screen.pixelsPerUnitX() : 1.0f,
                windowSized ? screen.pixelsPerUnitY() : 1.0f);
    }

    /**
     * Draws a list into an offscreen target.
     *
     * <p>The target gets a top-left origin orthographic projection of {@code width / scaleX} by
     * {@code height / scaleY} units and an identity modelview, so the list is laid out in units and the framebuffer
     * holds {@code scale} pixels per unit. The previous framebuffer, viewport, matrices and depth state are restored.
     * Clipping, anti-aliasing and glyph sizes use the target's pixel density through {@link Pass}.</p>
     *
     * @param drawList commands to draw; may be empty, the target is still cleared if its options ask for it
     * @param target destination
     * @param scaleX pixels per unit along x
     * @param scaleY pixels per unit along y
     */
    void renderToTarget(DrawList drawList, CleanroomRenderTarget target, float scaleX, float scaleY) {
        if (target.isDisposed()) throw new IllegalStateException("Cannot render into a disposed target");
        float unitX = scaleX > 0.0f && Float.isFinite(scaleX) ? scaleX : 1.0f;
        float unitY = scaleY > 0.0f && Float.isFinite(scaleY) ? scaleY : 1.0f;
        CleanroomRenderTarget.Guard guard = CleanroomRenderTarget.Guard.capture();
        // Widgets may render into a target while the draw list is still being built, before beginFrame: bring GL to
        // the UI defaults for the pass and give the caller its state back.
        boolean standalone = !frameActive;
        if (standalone) {
            glState.capture();
            glState.applyUiDefaults();
        }
        scissorStack.clear();
        CleanroomGlState.disableScissor();
        pass.begin(target, unitX, unitY);
        float depth = Math.max(1000.0f, Math.max(target.width(), target.height()));
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0, target.width() / unitX, target.height() / unitY, 0.0, -depth, depth);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        try {
            target.bindWrite();
            if (drawList != null && drawList.size() > 0) renderBatches(drawList);
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            scissorStack.clear();
            CleanroomGlState.disableScissor();
            pass.end();
            if (standalone) glState.restore();
            guard.restore();
        }
    }

    // ---- post effects -------------------------------------------------------------------------------------------

    @Override
    public boolean supportsPostEffects() {
        return !postEffectsBroken && OpenGlHelper.isFramebufferEnabled() && CleanroomShaderQuadRenderer.supported();
    }

    /**
     * @return bounds of the whole window in draw-list units, with the pixel density as scale: a layer target made from
     *         them has exactly the window's pixel size
     */
    UiLayerBounds postEffectBounds() {
        float scaleX = screen.pixelsPerUnitX();
        return new UiLayerBounds(0.0f, 0.0f, screen.framebufferWidth() / scaleX,
                screen.framebufferHeight() / screen.pixelsPerUnitY(), scaleX);
    }

    /**
     * Renders a list through a chain of full-viewport shader passes: the list is drawn into a layer target, every
     * pass reads the previous result and writes the other of two ping-pong targets, and the last result is drawn
     * over {@code target} (the window if {@code null}) as one image. A chain that cannot run (no shaders, a pass that
     * failed, an exception) shows the unprocessed layer or, as the last resort, draws the list directly.
     */
    @Override
    public void renderWithPostEffect(DrawList drawList, UiLayerBounds bounds, UiPostEffectChain chain,
                                     RenderTarget target) {
        UiPostEffects.ensureRegistered();
        UiLayerBounds layerBounds = bounds == null ? postEffectBounds() : bounds;
        float layerScale = layerBounds.safeScale();
        if (drawList == null || chain == null || chain.isNone() || !supportsPostEffects()) {
            renderDirect(drawList, target, layerScale);
            return;
        }
        List<UiPostEffectPass> passes = chain.resolve(UiPostEffectRegistry.global());
        if (passes.isEmpty()) {
            renderDirect(drawList, target, layerScale);
            return;
        }
        try {
            int targetWidth = layerBounds.targetWidth();
            int targetHeight = layerBounds.targetHeight();
            float scaleX = targetWidth / layerBounds.safeWidth();
            float scaleY = targetHeight / layerBounds.safeHeight();
            CleanroomRenderTarget layer = (CleanroomRenderTarget) postEffectLayerTargets.acquire(
                    targetWidth, targetHeight, POST_EFFECT_LAYER_TARGET);
            CleanroomRenderTarget ping = (CleanroomRenderTarget) postEffectPingTargets.acquire(
                    targetWidth, targetHeight, POST_EFFECT_PING_TARGET);
            renderToTarget(drawList, layer, scaleX, scaleY);
            CleanroomRenderTarget current = layer;
            if (!isPassthroughOnly(passes)) {
                for (UiPostEffectPass effect : passes) {
                    CleanroomRenderTarget destination = current == layer ? ping : layer;
                    if (!renderPostEffectPass(effect, current, destination)) break;
                    current = destination;
                }
            }
            compositePostEffect(current, layerBounds, target, scaleX, scaleY);
        } catch (RuntimeException failure) {
            LOGGER.error("UniGUI post effects failed and are disabled; drawing without them", failure);
            postEffectsBroken = true;
            renderDirect(drawList, target, layerScale);
        }
    }

    private void renderDirect(DrawList drawList, RenderTarget target, float scale) {
        if (target instanceof CleanroomRenderTarget own) renderToTarget(drawList, own, scale, scale);
        else render(drawList, target);
    }

    private static boolean isPassthroughOnly(List<UiPostEffectPass> passes) {
        if (passes.size() != 1) return false;
        UiPostEffectPass only = passes.get(0);
        return only != null && UiPostEffects.PASSTHROUGH.equals(only.shaderId())
                && only.blendMode() == UiPostEffectBlendMode.REPLACE
                && only.uniforms().isEmpty() && only.textures().isEmpty();
    }

    /** Runs one effect pass: a full-viewport shader over {@code source}, written to {@code destination}. */
    private boolean renderPostEffectPass(UiPostEffectPass effect, CleanroomRenderTarget source,
                                         CleanroomRenderTarget destination) {
        if (effect == null) return false;
        DrawCommand command = DrawCommand.shader(effect.shader(),
                        new MutableRect(0.0f, 0.0f, destination.width(), destination.height()),
                        effect.uniforms().resolve())
                .texture(source.colorTexture())
                .shaderTextures(effect.textures())
                .shaderOptions(ShaderDrawOptions.defaults()
                        .blend(effect.blendMode().blendEnabled())
                        .squareVertexOffset(0.0f));
        postEffectPassList.clear();
        postEffectPassList.add(command);
        shaderDrawn = false;
        // Passes work in the destination's pixels, not in the layer's units.
        renderToTarget(postEffectPassList, destination, 1.0f, 1.0f);
        return shaderDrawn;
    }

    private void compositePostEffect(CleanroomRenderTarget source, UiLayerBounds bounds, RenderTarget target,
                                     float scaleX, float scaleY) {
        postEffectCompositeList.clear();
        postEffectCompositeList.add(DrawCommand.texture(source.colorTexture(),
                new MutableRect(bounds.x(), bounds.y(), bounds.safeWidth(), bounds.safeHeight()),
                Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f))));
        if (target instanceof CleanroomRenderTarget own) renderToTarget(postEffectCompositeList, own, scaleX, scaleY);
        else renderBatches(postEffectCompositeList);
    }

    /** Releases the GL objects this backend owns: post-effect targets and shader programs. */
    @Override
    public void close() {
        // GL objects can only be deleted on the client thread; a layer may be closed from anywhere.
        if (minecraft.isCallingFromMinecraftThread()) releaseGlObjects();
        else minecraft.addScheduledTask(this::releaseGlObjects);
    }

    private void releaseGlObjects() {
        postEffectLayerTargets.close();
        postEffectPingTargets.close();
        shaderRenderer.close();
    }

    private void renderBatches(DrawList drawList) {
        List<DrawBatch> batches = batcher.batch(drawList);
        for (int i = 0, size = batches.size(); i < size; i++) {
            renderBatch(batches.get(i));
        }
    }

    /**
     * Renders one batch: shape batches try the SDF shader first and fall back to tessellated geometry; everything
     * else (clips, custom draws, text, textures, paths) is drawn command by command in draw-list order.
     */
    private void renderBatch(DrawBatch batch) {
        if (!batch.isBarrier() && CleanroomSdfShapeInstances.supports(batch.type())
                && CleanroomSdfShapeRenderer.shared().render(batch, pass.opacity())) {
            return;
        }
        if (!batch.isBarrier()) {
            DrawCommandType type = batch.type();
            if (type == DrawCommandType.TEXT) {
                textRenderer.render(batch);
                return;
            }
            if (type == DrawCommandType.MESH || CleanroomShapeTessellator.supportsTexture(type)) {
                shapeRenderer.renderTextured(batch);
                return;
            }
            if (CleanroomShapeTessellator.supports(type)) {
                shapeRenderer.renderShapes(batch);
                return;
            }
        }
        Object[] commands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            renderSingle((DrawCommand) commands[i]);
        }
    }

    private void renderSingle(DrawCommand command) {
        switch (command.type()) {
            case PUSH_CLIP -> {
                pushClip(command);
                return;
            }
            case POP_CLIP -> {
                popClip();
                return;
            }
            default -> {
            }
        }
        boolean transformed = usesGlTransform(command.type()) && CleanroomGlTransforms.has(command);
        if (transformed) {
            GlStateManager.pushMatrix();
            CleanroomGlTransforms.apply(command);
        }
        try {
            renderCommand(command);
        } finally {
            if (transformed) GlStateManager.popMatrix();
        }
    }

    @Override
    public void endFrame() {
        frameActive = false;
        scissorStack.clear();
        CleanroomGlState.disableScissor();
        CleanroomGlState.checkError("UniGUI frame");
        glState.restore();
    }

    /**
     * Shape, mesh and text renderers transform vertices on the CPU (text applies its own GL transform for the vanilla
     * font), so only these commands expect the command transform to be on the modelview stack.
     */
    private static boolean usesGlTransform(DrawCommandType type) {
        return type == DrawCommandType.CUSTOM;
    }

    private void renderCommand(DrawCommand command) {
        CleanroomGlState.blend(command.paint().blendMode());
        switch (command.type()) {
            case RECT, ROUNDED_RECT, LINE, CIRCLE, PATH -> shapeRenderer.renderShape(command);
            case MESH, TEXTURE, TEXTURED_QUAD -> shapeRenderer.renderTexturedCommand(command);
            case TEXT -> textRenderer.render(command);
            case CUSTOM -> {
                if (command.customDraw() != null) command.customDraw().draw(this);
            }
            case PUSH_CLIP, POP_CLIP -> {
                // Handled in render() before any matrix state is touched.
            }
            case SHADER -> shaderDrawn = shaderRenderer.render(command,
                    pass.framebufferWidth() / pass.pixelsPerUnitX(),
                    pass.framebufferHeight() / pass.pixelsPerUnitY(),
                    pass.pixelsPerUnitX());
            case DRAW_CMD -> {
                // Raw draw commands carry backend-specific payloads that have no 1.12.2 counterpart yet.
            }
        }
    }

    private void pushClip(DrawCommand command) {
        CleanroomRenderMath.transformedBounds(command.bounds(), command.transform(),
                command.transformStackElements(), command.transformStackSize(), transformScratch, clipUi);
        CleanroomRenderMath.scaleRect(clipUi, pass.pixelsPerUnitX(), pass.pixelsPerUnitY(), clipWindow);
        applyScissor(scissorStack.push(clipWindow));
    }

    private void popClip() {
        applyScissor(scissorStack.pop());
    }

    private void applyScissor(ScissorStack.Rect rect) {
        if (rect == null) {
            CleanroomGlState.disableScissor();
            return;
        }
        CleanroomRenderMath.glScissor(rect, pass.framebufferWidth(), pass.framebufferHeight(), scissorBox);
        CleanroomGlState.scissor(scissorBox);
    }

    /**
     * The surface of the pass in progress: the screen the backend belongs to, or a render target while one is being
     * drawn. Renderers read their pixel density and framebuffer size from here, so they need no knowledge of targets.
     */
    private final class Pass implements CleanroomRenderHost {
        private CleanroomRenderTarget target;
        private float scaleX = 1.0f;
        private float scaleY = 1.0f;

        private void begin(CleanroomRenderTarget target, float scaleX, float scaleY) {
            this.target = target;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }

        private void end() {
            target = null;
        }

        @Override
        public int logicalWidth() {
            return target == null ? screen.logicalWidth() : Math.max(1, Math.round(target.width() / scaleX));
        }

        @Override
        public int logicalHeight() {
            return target == null ? screen.logicalHeight() : Math.max(1, Math.round(target.height() / scaleY));
        }

        @Override
        public float uiScale() {
            return target == null ? screen.uiScale() : 1.0f;
        }

        @Override
        public float opacity() {
            return screen.opacity();
        }

        @Override
        public int framebufferWidth() {
            return target == null ? screen.framebufferWidth() : target.width();
        }

        @Override
        public int framebufferHeight() {
            return target == null ? screen.framebufferHeight() : target.height();
        }

        @Override
        public float pixelsPerUnitX() {
            return target == null ? screen.pixelsPerUnitX() : scaleX;
        }

        @Override
        public float pixelsPerUnitY() {
            return target == null ? screen.pixelsPerUnitY() : scaleY;
        }
    }
}
