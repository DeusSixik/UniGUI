package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.posteffect.UiLayerBounds;
import dev.sixik.unigui.api.posteffect.UiPostEffectBackend;
import dev.sixik.unigui.api.posteffect.UiPostEffectBlendMode;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffectPass;
import dev.sixik.unigui.api.posteffect.UiPostEffectRegistry;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.DrawBatcher;
import dev.sixik.unigui.impl.render.RenderTargetCache;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.text.TextEngine;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Immediate-mode renderer for the Minecraft 1.7.10 GUI pipeline.
 *
 * <p>Geometry goes through the vanilla {@link net.minecraft.client.renderer.Tessellator} with per-vertex colors, so
 * no draw leaks a current color into the next one. Clip commands are kept on a {@link ScissorStack}, so nested clips
 * intersect and {@code POP_CLIP} restores the parent clip instead of disabling the scissor test. GL state is captured
 * in {@link #beginFrame} and restored in {@link #endFrame} by {@link GtnhGlState}.</p>
 *
 * <p>Shape batches try the SDF shader first ({@link GtnhSdfShapeRenderer}) and fall back to tessellated geometry when
 * GL is below the {@code BASIC} tier, the shader fails to compile, or {@code -Dunigui.sdf=false} is set. Text goes
 * through {@link GtnhTextRenderer} (vanilla font and SDF faces), {@code SHADER} commands through
 * {@link GtnhShaderQuadRenderer}.</p>
 *
 * <p>Offscreen rendering: {@link #createRenderTarget} makes a {@link GtnhRenderTarget} (vanilla {@code Framebuffer}),
 * {@link #render(DrawList, RenderTarget)} draws into it with its own projection, and {@link #renderWithPostEffect}
 * runs a layer through a chain of full-viewport shader passes.</p>
 */
public final class GtnhGuiRenderBackend implements RenderBackend, UiPostEffectBackend, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final RenderTargetOptions POST_EFFECT_LAYER_TARGET =
        new RenderTargetOptions(true, true, "post_effect_layer");
    private static final RenderTargetOptions POST_EFFECT_PING_TARGET =
        new RenderTargetOptions(false, true, "post_effect_ping");

    private final GtnhRenderHost screen;
    private final Pass pass = new Pass();
    private final GtnhGlState glState = new GtnhGlState();
    private final DrawBatcher batcher = SimpleDrawBatcher.INSTANCE;
    private final ScissorStack scissorStack = new ScissorStack();
    private final MutableRect clipUi = new MutableRect();
    private final MutableRect clipWindow = new MutableRect();
    private final float[] transformScratch = new float[2];
    private final int[] scissorBox = new int[4];
    private final GtnhTextRenderer textRenderer;
    private final GtnhShapeRenderer shapeRenderer;
    private final GtnhShaderQuadRenderer shaderRenderer = new GtnhShaderQuadRenderer();
    private final RenderTargetCache postEffectLayerTargets = new RenderTargetCache(this);
    private final RenderTargetCache postEffectPingTargets = new RenderTargetCache(this);
    private final DrawList postEffectPassList = new DrawList();
    private final DrawList postEffectCompositeList = new DrawList();
    private FontFace defaultFont = MinecraftFonts.defaultFace();
    private boolean frameActive;
    private boolean gpuTimed;
    private boolean shaderDrawn;
    private boolean postEffectsBroken;
    private boolean unsupportedLogged;

    GtnhGuiRenderBackend(GtnhRenderHost screen) {
        this.screen = screen;
        this.textRenderer = new GtnhTextRenderer(pass);
        this.shapeRenderer = new GtnhShapeRenderer(pass);
    }

    @Override
    public float measureTextWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(RichText.of(GtnhLegacyFormatting.strip(text), defaultFont,
            TextRun.DEFAULT_PIXEL_SIZE));
    }

    @Override
    public float measureTextWidth(RichText text) {
        if (text == null || text.isEmpty()) return 0.0f;
        return TextEngine.measureLineWidth(withDefaultFont(text));
    }

    /**
     * Resolves runs without a face to the backend default and drops formatting codes, so measuring matches what is
     * drawn: the codes take no space.
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
                .append(GtnhLegacyFormatting.strip(run.text()));
        }
        return builder.build();
    }

    @Override
    public FontFace defaultTextFace() {
        return defaultFont;
    }

    GtnhGuiRenderBackend defaultFont(FontFace font) {
        defaultFont = font == null ? MinecraftFonts.defaultFace() : font;
        textRenderer.defaultFace(defaultFont);
        return this;
    }

    @Override
    public void beginFrame(FrameContext frame) {
        frameActive = true;
        gpuTimed = GtnhGpuTimer.begin();
        GtnhItemIcons.shared().beginFrame();
        GtnhGlCapabilities.get();
        glState.capture();
        glState.applyUiDefaults();
        scissorStack.clear();
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, RenderTargetOptions options) {
        return new GtnhRenderTarget(width, height, options);
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        if (target == null) {
            if (drawList == null || drawList.size() == 0) return;
            renderBatches(drawList);
            return;
        }
        if (!(target instanceof GtnhRenderTarget own)) {
            throw new IllegalArgumentException("Unsupported render target for the GTNH backend: "
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
     * @param target   destination
     * @param scaleX   pixels per unit along x
     * @param scaleY   pixels per unit along y
     */
    void renderToTarget(DrawList drawList, GtnhRenderTarget target, float scaleX, float scaleY) {
        if (target.isDisposed()) throw new IllegalStateException("Cannot render into a disposed target");
        float unitX = scaleX > 0.0f && Float.isFinite(scaleX) ? scaleX : 1.0f;
        float unitY = scaleY > 0.0f && Float.isFinite(scaleY) ? scaleY : 1.0f;
        GtnhRenderTarget.Guard guard = GtnhRenderTarget.Guard.capture();
        // Widgets may render into a target while the draw list is still being built, before beginFrame: bring GL to
        // the UI defaults for the pass and give the caller its state back.
        boolean standalone = !frameActive;
        if (standalone) {
            glState.capture();
            glState.applyUiDefaults();
        }
        scissorStack.clear();
        GtnhGlState.disableScissor();
        pass.begin(target, unitX, unitY);
        float depth = Math.max(1000.0f, Math.max(target.width(), target.height()));
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, target.width() / unitX, target.height() / unitY, 0.0, -depth, depth);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
            target.bindWrite();
            if (drawList != null && drawList.size() > 0) renderBatches(drawList);
        } finally {
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            scissorStack.clear();
            GtnhGlState.disableScissor();
            pass.end();
            if (standalone) glState.restore();
            guard.restore();
        }
    }

    // ---- Minecraft content: items, entities, tooltips -----------------------------------------------------------

    /**
     * Draws an item stack, scaled to a square.
     *
     * <p>Items with a stable picture come from a baked icon texture (see {@link GtnhItemIcons}); the rest are drawn
     * live by the vanilla item renderer. Vanilla cannot draw an item translucent, so {@code opacity} only affects
     * baked icons; a live item is opaque unless {@code opacity} is {@code 0}.</p>
     *
     * @param stack       stack to draw ({@code null} or empty draws nothing)
     * @param x           left edge in the current coordinate space
     * @param y           top edge
     * @param size        edge of the square
     * @param opacity     {@code 0..1} alpha for baked icons
     * @param decorations whether to add the count and the durability bar
     */
    public void renderItemPreview(ItemStack stack, float x, float y, float size, float opacity,
                                  boolean decorations) {
        if (GtnhItemStacks.isEmpty(stack) || !(size > 0.0f) || !(opacity > 0.0f)) return;
        TextureHandle icon = GtnhItemIcons.shared().bakeIfBudget(stack, size * pass.pixelsPerUnitX());
        if (icon != null) {
            drawIcon(icon, x, y, size, opacity);
            if (decorations) GtnhVanillaDraw.overlay(stack, x, y, size);
            return;
        }
        GtnhVanillaDraw.item(stack, x, y, size, decorations);
    }

    /**
     * Like {@link #renderItemPreview} without decorations, but for lists: a cacheable item whose icon does not fit in
     * this frame's bake budget is not drawn at all.
     *
     * @return {@code false} if nothing was drawn and the caller should try again next frame
     */
    public boolean renderItemPreviewLazy(ItemStack stack, float x, float y, float size, float opacity) {
        if (GtnhItemStacks.isEmpty(stack) || !(size > 0.0f)) return false;
        GtnhItemIcons icons = GtnhItemIcons.shared();
        TextureHandle icon = icons.bakeIfBudget(stack, size * pass.pixelsPerUnitX());
        if (icon != null) {
            drawIcon(icon, x, y, size, opacity);
            return true;
        }
        if (icons.cacheable(stack)) return false;
        GtnhVanillaDraw.item(stack, x, y, size, false);
        return true;
    }

    /**
     * @param stack stack to look up
     * @param size  on-screen edge of the item in UI units
     * @return the baked icon if it exists already (never bakes), otherwise {@code null}
     */
    public TextureHandle cachedItemPreviewTexture(ItemStack stack, float size) {
        return GtnhItemIcons.shared().cached(stack, size * pass.pixelsPerUnitX());
    }

    /**
     * Drops every baked item icon.
     */
    public void clearItemPreviewCache() {
        GtnhItemIcons.shared().clear();
    }

    /**
     * Draws a living entity in the pose of the inventory screen.
     *
     * @return {@code false} if the entity could not be drawn
     */
    public boolean renderEntityPreview(EntityLivingBase entity, float x, float y, float size, float lookX,
                                       float lookY, float opacity) {
        if (!(opacity > 0.0f)) return true;
        return GtnhVanillaDraw.entity(entity, x, y, size, lookX, lookY);
    }

    /**
     * Draws a vanilla tooltip for text lines.
     *
     * @param lines text lines, formatting codes allowed
     * @param x     anchor x; the box sits 12 units to its right
     * @param y     anchor y; the box sits 12 units above it
     */
    public void renderVanillaTooltip(List<String> lines, float x, float y) {
        GtnhVanillaDraw.tooltip(lines, GtnhItemStacks.EMPTY, Math.round(x), Math.round(y), tooltipWidth(),
            tooltipHeight());
    }

    /**
     * Draws the vanilla tooltip of an item stack (name, lore, mod additions, advanced flags).
     */
    public void renderVanillaTooltip(ItemStack stack, float x, float y) {
        if (GtnhItemStacks.isEmpty(stack)) return;
        GtnhVanillaDraw.tooltip(GtnhVanillaDraw.itemTooltipLines(stack), stack, Math.round(x),
            Math.round(y), tooltipWidth(), tooltipHeight());
    }

    /**
     * Draws a draw list nested in the current commands, for example inside a custom draw.
     *
     * @param drawList        commands to draw
     * @param coordinateScale unused on 1.7.10: the modelview matrix already carries the scale
     */
    public void renderNested(DrawList drawList, float coordinateScale) {
        if (drawList == null || drawList.size() == 0) return;
        renderBatches(drawList);
    }

    private int tooltipWidth() {
        return Math.round(pass.logicalWidth() / Math.max(0.01f, pass.uiScale()));
    }

    private int tooltipHeight() {
        return Math.round(pass.logicalHeight() / Math.max(0.01f, pass.uiScale()));
    }

    private void drawIcon(TextureHandle icon, float x, float y, float size, float opacity) {
        DrawCommand command = DrawCommand.texture(icon, new MutableRect(x, y, size, size),
            Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, Math.max(0.0f, Math.min(1.0f, opacity)))));
        GtnhGlState.blend(command.paint().blendMode());
        shapeRenderer.renderTexturedCommand(command);
    }

    // ---- post effects -------------------------------------------------------------------------------------------
    @Override
    public boolean supportsPostEffects() {
        return !postEffectsBroken && OpenGlHelper.isFramebufferEnabled() && GtnhShaderQuadRenderer.supported();
    }

    /**
     * @return bounds of the whole window in draw-list units, with the pixel density as scale: a layer target made from
     * them has exactly the window's pixel size
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
            GtnhRenderTarget layer = (GtnhRenderTarget) postEffectLayerTargets.acquire(
                targetWidth, targetHeight, POST_EFFECT_LAYER_TARGET);
            GtnhRenderTarget ping = (GtnhRenderTarget) postEffectPingTargets.acquire(
                targetWidth, targetHeight, POST_EFFECT_PING_TARGET);
            renderToTarget(drawList, layer, scaleX, scaleY);
            GtnhRenderTarget current = layer;
            if (!isPassthroughOnly(passes)) {
                for (UiPostEffectPass effect : passes) {
                    GtnhRenderTarget destination = current == layer ? ping : layer;
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
        if (target instanceof GtnhRenderTarget own) renderToTarget(drawList, own, scale, scale);
        else render(drawList, target);
    }

    private static boolean isPassthroughOnly(List<UiPostEffectPass> passes) {
        if (passes.size() != 1) return false;
        UiPostEffectPass only = passes.get(0);
        return only != null && UiPostEffects.PASSTHROUGH.equals(only.shaderId())
            && only.blendMode() == UiPostEffectBlendMode.REPLACE
            && only.uniforms().isEmpty() && only.textures().isEmpty();
    }

    /**
     * Runs one effect pass: a full-viewport shader over {@code source}, written to {@code destination}.
     */
    private boolean renderPostEffectPass(UiPostEffectPass effect, GtnhRenderTarget source,
                                         GtnhRenderTarget destination) {
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

    private void compositePostEffect(GtnhRenderTarget source, UiLayerBounds bounds, RenderTarget target,
                                     float scaleX, float scaleY) {
        postEffectCompositeList.clear();
        postEffectCompositeList.add(DrawCommand.texture(source.colorTexture(),
            new MutableRect(bounds.x(), bounds.y(), bounds.safeWidth(), bounds.safeHeight()),
            Paint.fill(new MutableColor(1.0f, 1.0f, 1.0f, 1.0f))));
        if (target instanceof GtnhRenderTarget own) renderToTarget(postEffectCompositeList, own, scaleX, scaleY);
        else renderBatches(postEffectCompositeList);
    }

    /**
     * Releases the GL objects this backend owns: post-effect targets and shader programs.
     */
    @Override
    public void close() {
        // GL objects can only be deleted on the client thread; a layer may be closed from anywhere.
        GtnhClientTasks.runOnClientThread(this::releaseGlObjects);
    }

    private void releaseGlObjects() {
        postEffectLayerTargets.close();
        postEffectPingTargets.close();
        shaderRenderer.close();
    }

    // ---- drawing ------------------------------------------------------------------------------------------------

    private void renderBatches(DrawList drawList) {
        List<DrawBatch> batches = batcher.batch(drawList);
        for (int i = 0, size = batches.size(); i < size; i++) {
            renderBatch(batches.get(i));
        }
    }

    @Override
    public void endFrame() {
        frameActive = false;
        if (gpuTimed) {
            gpuTimed = false;
            GtnhGpuTimer.end();
        }
        scissorStack.clear();
        GtnhGlState.disableScissor();
        GtnhGlState.checkError("UniGUI frame");
        glState.restore();
    }

    /**
     * Renders one batch: shape batches try the SDF shader first and fall back to tessellated geometry; everything
     * else (clips, custom draws, shaders) is drawn command by command in draw-list order.
     */
    private void renderBatch(DrawBatch batch) {
        if (!batch.isBarrier() && GtnhSdfShapeInstances.supports(batch.type())
            && GtnhSdfShapeRenderer.shared().render(batch, pass.opacity())) {
            return;
        }
        if (!batch.isBarrier()) {
            DrawCommandType type = batch.type();
            if (type == DrawCommandType.TEXT) {
                textRenderer.render(batch);
                return;
            }
            if (type == DrawCommandType.MESH || GtnhShapeTessellator.supportsTexture(type)) {
                shapeRenderer.renderTextured(batch);
                return;
            }
            if (GtnhShapeTessellator.supports(type)) {
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
        boolean transformed = command.type() == DrawCommandType.CUSTOM && GtnhGlTransforms.has(command);
        if (transformed) {
            GL11.glPushMatrix();
            GtnhGlTransforms.apply(command);
        }
        try {
            renderCommand(command);
        } finally {
            if (transformed) GL11.glPopMatrix();
        }
    }

    private void renderCommand(DrawCommand command) {
        GtnhGlState.blend(command.paint().blendMode());
        switch (command.type()) {
            case RECT, ROUNDED_RECT, LINE, CIRCLE, PATH -> shapeRenderer.renderShape(command);
            case MESH, TEXTURE, TEXTURED_QUAD -> shapeRenderer.renderTexturedCommand(command);
            case TEXT -> textRenderer.render(command);
            case CUSTOM -> {
                if (command.customDraw() != null) command.customDraw().draw(this);
            }
            case PUSH_CLIP, POP_CLIP -> {
                // Handled in renderSingle() before any matrix state is touched.
            }
            case SHADER -> shaderDrawn = shaderRenderer.render(command,
                pass.framebufferWidth() / pass.pixelsPerUnitX(),
                pass.framebufferHeight() / pass.pixelsPerUnitY(),
                pass.pixelsPerUnitX());
            case DRAW_CMD -> {
                if (!unsupportedLogged) {
                    unsupportedLogged = true;
                    LOGGER.warn("UniGUI GTNH backend skips {} commands: raw draw commands have no 1.7.10 payload",
                        command.type());
                }
            }
        }
    }

    private void pushClip(DrawCommand command) {
        GtnhRenderMath.transformedBounds(command.bounds(), command.transform(),
            command.transformStackElements(), command.transformStackSize(), transformScratch, clipUi);
        GtnhRenderMath.scaleRect(clipUi, pass.pixelsPerUnitX(), pass.pixelsPerUnitY(), clipWindow);
        applyScissor(scissorStack.push(clipWindow));
    }

    private void popClip() {
        applyScissor(scissorStack.pop());
    }

    private void applyScissor(ScissorStack.Rect rect) {
        if (rect == null) {
            GtnhGlState.disableScissor();
            return;
        }
        GtnhRenderMath.glScissor(rect, pass.framebufferWidth(), pass.framebufferHeight(), scissorBox);
        GtnhGlState.scissor(scissorBox);
    }

    /**
     * The surface of the pass in progress: the screen the backend belongs to, or a render target while one is being
     * drawn. Renderers read their pixel density and framebuffer size from here, so they need no knowledge of targets.
     */
    private final class Pass implements GtnhRenderHost {
        private GtnhRenderTarget target;
        private float scaleX = 1.0f;
        private float scaleY = 1.0f;

        private void begin(GtnhRenderTarget target, float scaleX, float scaleY) {
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
