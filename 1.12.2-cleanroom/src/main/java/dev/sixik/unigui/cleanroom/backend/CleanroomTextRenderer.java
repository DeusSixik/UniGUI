package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.render.DrawBatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Draws {@code TEXT} commands: SDF faces through {@link CleanroomSdfTextRenderer}, the vanilla face through
 * {@code FontRenderer}, and mixed runs in the order they appear.
 *
 * <p>{@link CleanroomTextLayout} decides where each glyph goes; this class is its {@link CleanroomTextLayout.Sink}.
 * SDF glyphs are accumulated and flushed before every vanilla segment and at the end of the batch, so the visual
 * stacking order equals the order of runs. If SDF text is unavailable (GL below the instanced tier, shader failure)
 * SDF runs are drawn with the vanilla font instead of disappearing.</p>
 */
final class CleanroomTextRenderer implements CleanroomTextLayout.Sink {
    /**
     * Vanilla {@code FontRenderer} promotes an alpha below 4 to fully opaque, so such text must be skipped.
     */
    private static final int MIN_VANILLA_ALPHA = 4;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final CleanroomRenderHost host;
    private final CleanroomTextLayout layout = new CleanroomTextLayout();
    private final CleanroomSdfTextRenderer sdf = CleanroomSdfTextRenderer.shared();
    private final float[] transformScratch = new float[2];
    private final float[] affine = new float[6];
    private final float[] rgba = new float[4];
    private FontFace defaultFace = MinecraftFonts.defaultFace();

    private DrawCommand current;
    private boolean sdfActive;

    CleanroomTextRenderer(CleanroomRenderHost host) {
        this.host = host;
    }

    /**
     * @param face face used by runs that do not name one; {@code null} restores the vanilla font
     */
    void defaultFace(FontFace face) {
        defaultFace = face == null ? MinecraftFonts.defaultFace() : face;
    }

    /**
     * Draws a batch of text commands in order.
     *
     * @param batch batch of {@code TEXT} commands
     */
    void render(DrawBatch batch) {
        begin();
        try {
            Object[] commands = batch.commandElements();
            for (int i = 0, size = batch.size(); i < size; i++) layoutCommand((DrawCommand) commands[i]);
        } finally {
            end();
        }
    }

    /**
     * Draws a single text command.
     *
     * @param command {@code TEXT} command
     */
    void render(DrawCommand command) {
        begin();
        try {
            layoutCommand(command);
        } finally {
            end();
        }
    }

    private void begin() {
        sdfActive = sdf.ready();
    }

    private void end() {
        if (sdfActive) sdf.flush();
        current = null;
    }

    private void layoutCommand(DrawCommand command) {
        RichText text = richText(command);
        if (text == null || text.isEmpty()) return;
        if (CleanroomTextColors.maxAlphaChannel(command.paint().color(), null, null, host.opacity()) == 0) return;

        CleanroomRenderMath.affine(command.bounds(), command.transform(), command.transformStackElements(),
                command.transformStackSize(), transformScratch, affine);
        current = command;
        if (sdfActive) sdf.beginCommand(affine, command.paint().color(), command.brushBounds(), host.opacity());

        float scaleX = host.pixelsPerUnitX();
        float scaleY = host.pixelsPerUnitY();
        layout.layout(command, text, defaultFace, MinecraftFonts.defaultFace(),
                sdfActive ? sdf.atlas() : null, affine, scaleX, scaleY, this);
    }

    private RichText richText(DrawCommand command) {
        if (command.richText() != null) return command.richText();
        String plain = command.text();
        if (plain == null || plain.isEmpty()) return null;
        return RichText.of(plain, defaultFace, TextRun.DEFAULT_PIXEL_SIZE);
    }

    @Override
    public void sdfGlyph(Object page, float x, float y, float width, float height,
                         float u0, float v0, float u1, float v1, TextRun run) {
        if (CleanroomTextColors.maxAlphaChannel(current.paint().color(), run.color(), run.brush(), host.opacity()) == 0) {
            return;
        }
        sdf.quad(page, x, y, width, height, u0, v0, u1, v1, run.color(), run.brush());
    }

    @Override
    public void vanilla(String text, FontFace face, TextRun run, float x, float top, boolean perGlyph) {
        if (CleanroomTextColors.maxAlphaChannel(current.paint().color(), run.color(), run.brush(), host.opacity())
                < MIN_VANILLA_ALPHA) {
            return;
        }
        if (sdfActive) sdf.flush();

        float size = run.pixelSize();
        float scale = Math.max(1.0f, size) / MinecraftFontFace.BASE_SIZE;
        // A glyph is colored at its center, a longer segment at its start, like the 1.20 backend.
        float colorX = perGlyph ? x + face.advance(text.codePointAt(0), size) * 0.5f : x;
        CleanroomTextColors.rgba(rgba, 0, colorX, top + size * 0.5f, current.paint().color(), run.color(),
                run.brush(), current.brushBounds(), host.opacity());
        int color = CleanroomTextColors.pack(rgba, 0);
        if ((color >>> 24) < MIN_VANILLA_ALPHA) return;

        boolean transformed = CleanroomGlTransforms.has(current);
        GlStateManager.pushMatrix();
        try {
            if (transformed) CleanroomGlTransforms.apply(current);
            GlStateManager.translate(x, top, 0.0f);
            GlStateManager.scale(scale, scale, 1.0f);
            CleanroomGlState.blend(current.paint().blendMode());
            CleanroomGlState.textured();
            minecraft.fontRenderer.drawString(text, 0.0f, 0.0f, color, false);
        } finally {
            GlStateManager.popMatrix();
        }
    }
}
