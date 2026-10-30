package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.FrameContext;
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
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

/**
 * Immediate-mode renderer for the Minecraft 1.12.2 GUI pipeline.
 *
 * <p>All geometry goes through the vanilla {@link net.minecraft.client.renderer.Tessellator} with per-vertex colors, so no draw leaks a
 * current color into the next one, and all state changes go through {@link GlStateManager}
 * (see {@link CleanroomGlState}). Clip commands are kept on a {@link ScissorStack}, so nested clips intersect
 * and {@code POP_CLIP} restores the parent clip instead of disabling the scissor test.</p>
 */
final class CleanroomGuiRenderBackend implements RenderBackend {
    /**
     * Vanilla {@code FontRenderer} treats an alpha below 4 as "no alpha given" and draws opaque text.
     */
    private static final int MIN_VANILLA_TEXT_ALPHA = 4;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final CleanroomRenderHost screen;
    private final CleanroomGlState glState = new CleanroomGlState();
    private final DrawBatcher batcher = SimpleDrawBatcher.INSTANCE;
    private final ScissorStack scissorStack = new ScissorStack();
    private final MutableRect clipUi = new MutableRect();
    private final MutableRect clipWindow = new MutableRect();
    private final float[] transformScratch = new float[2];
    private final int[] scissorBox = new int[4];
    private final CleanroomTextRenderer textRenderer;
    private final CleanroomShapeRenderer shapeRenderer;
    private FontFace defaultFont = MinecraftFonts.defaultFace();

    CleanroomGuiRenderBackend(CleanroomRenderHost screen) {
        this.screen = screen;
        this.textRenderer = new CleanroomTextRenderer(screen);
        this.shapeRenderer = new CleanroomShapeRenderer(screen);
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
        CleanroomGlCapabilities.get();
        glState.capture();
        glState.applyUiDefaults();
        scissorStack.clear();
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        if (drawList == null || drawList.size() == 0) return;
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
                && CleanroomSdfShapeRenderer.shared().render(batch, screen.opacity())) {
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
            case SHADER, DRAW_CMD -> {
                // Shader quads and raw draw commands are added with the shader / render-target phase.
            }
        }
    }

    private void pushClip(DrawCommand command) {
        CleanroomRenderMath.transformedBounds(command.bounds(), command.transform(),
                command.transformStackElements(), command.transformStackSize(), transformScratch, clipUi);
        int displayWidth = minecraft.displayWidth;
        int displayHeight = minecraft.displayHeight;
        float scaleX = CleanroomRenderMath.windowScale(screen.uiScale(), displayWidth, screen.logicalWidth());
        float scaleY = CleanroomRenderMath.windowScale(screen.uiScale(), displayHeight, screen.logicalHeight());
        CleanroomRenderMath.scaleRect(clipUi, scaleX, scaleY, clipWindow);
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
        CleanroomRenderMath.glScissor(rect, minecraft.displayWidth, minecraft.displayHeight, scissorBox);
        CleanroomGlState.scissor(scissorBox);
    }
}
