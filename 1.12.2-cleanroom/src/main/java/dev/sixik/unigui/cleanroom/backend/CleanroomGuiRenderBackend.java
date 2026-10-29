package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Basic immediate-mode renderer for the Minecraft 1.12.2 GUI pipeline. */
final class CleanroomGuiRenderBackend implements RenderBackend {
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final CleanroomRenderHost screen;
    private FontFace defaultFont = MinecraftFonts.defaultFace();

    CleanroomGuiRenderBackend(CleanroomRenderHost screen) {
        this.screen = screen;
    }

    @Override
    public float measureTextWidth(String text) {
        return minecraft.fontRenderer.getStringWidth(text == null ? "" : text);
    }

    @Override
    public float measureTextWidth(RichText text) {
        if (text == null) return 0.0f;
        float width = 0.0f;
        for (TextRun run : text.runs()) {
            FontFace face = run.font() == null ? defaultFont : run.font();
            String value = run.text();
            for (int offset = 0; offset < value.length();) {
                int codePoint = value.codePointAt(offset);
                width += face.advance(codePoint, run.pixelSize()) + run.tracking();
                offset += Character.charCount(codePoint);
            }
            if (!value.isEmpty()) width -= run.tracking();
        }
        return width;
    }

    @Override
    public FontFace defaultTextFace() {
        return defaultFont;
    }

    CleanroomGuiRenderBackend defaultFont(FontFace font) {
        defaultFont = font == null ? Fonts.defaultFace() : font;
        return this;
    }

    @Override
    public void beginFrame(FrameContext frame) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_LINE_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_TEXTURE_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void render(DrawList drawList, RenderTarget target) {
        if (drawList == null) return;
        Object[] commands = drawList.commandElements();
        for (int i = 0, size = drawList.size(); i < size; i++) {
            DrawCommand command = (DrawCommand) commands[i];
            GL11.glPushMatrix();
            applyTransforms(command);
            renderCommand(command);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void endFrame() {
        GL11.glPopAttrib();
    }

    private void renderCommand(DrawCommand command) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        int color = CleanroomRenderMath.argb(paint.color(), screen.opacity());
        switch (command.type()) {
            case RECT, ROUNDED_RECT -> drawRect(bounds, color, paint.isStroke(), paint.strokeWidth());
            case LINE -> drawLine(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(),
                    color, Math.max(1.0f, paint.strokeWidth()));
            case CIRCLE -> drawCircle(bounds, color, paint.isStroke(), paint.strokeWidth(), command.segments());
            case TEXT -> drawText(command, color);
            case TEXTURE -> drawTexture(command);
            case TEXTURED_QUAD -> drawTexturedQuad(command);
            case PUSH_CLIP -> pushClip(bounds);
            case POP_CLIP -> GL11.glDisable(GL11.GL_SCISSOR_TEST);
            case CUSTOM -> {
                if (command.customDraw() != null) command.customDraw().draw(this);
            }
            case PATH, MESH, SHADER, DRAW_CMD -> {
                // These advanced command types are added in later legacy-renderer passes.
            }
        }
    }

    private void drawText(DrawCommand command, int color) {
        String text = command.richText() == null ? command.text() : command.richText().plainText();
        if (text != null) {
            minecraft.fontRenderer.drawString(text, Math.round(command.bounds().x()),
                    Math.round(command.bounds().y()), color, false);
        }
    }

    private static void drawRect(RectView bounds, int color, boolean stroke, float strokeWidth) {
        int left = Math.round(bounds.x());
        int top = Math.round(bounds.y());
        int right = Math.round(bounds.x() + bounds.width());
        int bottom = Math.round(bounds.y() + bounds.height());
        if (!stroke) {
            Gui.drawRect(left, top, right, bottom, color);
            return;
        }
        int width = Math.max(1, Math.round(strokeWidth));
        Gui.drawRect(left, top, right, top + width, color);
        Gui.drawRect(left, bottom - width, right, bottom, color);
        Gui.drawRect(left, top + width, left + width, bottom - width, color);
        Gui.drawRect(right - width, top + width, right, bottom - width, color);
    }

    private static void drawLine(float x1, float y1, float x2, float y2, int color, float width) {
        GL11.glLineWidth(width);
        GL11.glColor4ub((byte) (color >> 16), (byte) (color >> 8), (byte) color, (byte) (color >>> 24));
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
    }

    private static void drawCircle(RectView bounds, int color, boolean stroke, float strokeWidth, int segments) {
        int count = segments > 0 ? Math.max(8, segments) : 32;
        float centerX = bounds.x() + bounds.width() * 0.5f;
        float centerY = bounds.y() + bounds.height() * 0.5f;
        float radiusX = Math.max(0.0f, bounds.width() * 0.5f);
        float radiusY = Math.max(0.0f, bounds.height() * 0.5f);
        GL11.glColor4ub((byte) (color >> 16), (byte) (color >> 8), (byte) color, (byte) (color >>> 24));
        if (stroke) {
            GL11.glLineWidth(Math.max(1.0f, strokeWidth));
            GL11.glBegin(GL11.GL_LINE_LOOP);
        } else {
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2f(centerX, centerY);
        }
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            GL11.glVertex2f(centerX + (float) Math.cos(angle) * radiusX,
                    centerY + (float) Math.sin(angle) * radiusY);
        }
        GL11.glEnd();
    }

    private void drawTexture(DrawCommand command) {
        TextureHandle texture = command.texture();
        if (texture == null) return;
        ResourceLocation location = resourceLocation(texture.id());
        minecraft.getTextureManager().bindTexture(location);
        RectView bounds = command.bounds();
        RectView uv = command.uv();
        Gui.drawModalRectWithCustomSizedTexture(Math.round(bounds.x()), Math.round(bounds.y()),
                uv.x() * texture.width(), uv.y() * texture.height(), Math.round(bounds.width()),
                Math.round(bounds.height()), texture.width(), texture.height());
    }

    private void drawTexturedQuad(DrawCommand command) {
        TextureHandle texture = command.texture();
        if (texture == null) return;
        minecraft.getTextureManager().bindTexture(resourceLocation(texture.id()));
        GL11.glBegin(GL11.GL_QUADS);
        texturedVertex(command.quadX1(), command.quadY1(), command.quadU1(), command.quadV1(), texture);
        texturedVertex(command.quadX2(), command.quadY2(), command.quadU2(), command.quadV2(), texture);
        texturedVertex(command.quadX3(), command.quadY3(), command.quadU3(), command.quadV3(), texture);
        texturedVertex(command.quadX4(), command.quadY4(), command.quadU4(), command.quadV4(), texture);
        GL11.glEnd();
    }

    private static void texturedVertex(float x, float y, float u, float v, TextureHandle texture) {
        GL11.glTexCoord2f(u / Math.max(1, texture.width()), v / Math.max(1, texture.height()));
        GL11.glVertex2f(x, y);
    }

    private void pushClip(RectView bounds) {
        double scaleX = screen.backendScale();
        double scaleY = minecraft.displayHeight / (double) Math.max(1, screen.logicalHeight())
                * (scaleX / (minecraft.displayWidth / (double) Math.max(1, screen.logicalWidth())));
        int x = (int) Math.floor(bounds.x() * scaleX);
        int y = (int) Math.floor(minecraft.displayHeight - (bounds.y() + bounds.height()) * scaleY);
        int width = (int) Math.ceil(bounds.width() * scaleX);
        int height = (int) Math.ceil(bounds.height() * scaleY);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, width, height);
    }

    private static ResourceLocation resourceLocation(String id) {
        if (id == null || id.isBlank()) return new ResourceLocation("minecraft", "missingno");
        return new ResourceLocation(id);
    }

    private static void applyTransforms(DrawCommand command) {
        Object[] layers = command.transformStackElements();
        for (int i = 0; i < command.transformStackSize(); i++) {
            TransformLayer layer = (TransformLayer) layers[i];
            applyTransform(layer.bounds(), layer.transform());
        }
        applyTransform(command.bounds(), command.transform());
    }

    private static void applyTransform(RectView bounds, Transform transform) {
        if (transform == null || transform.isIdentity()) return;
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        GL11.glTranslatef(transform.position().x(), transform.position().y(), 0.0f);
        GL11.glTranslatef(pivotX, pivotY, 0.0f);
        GL11.glRotatef(transform.rotationDegrees(), 0.0f, 0.0f, 1.0f);
        GL11.glScalef(transform.scale().x(), transform.scale().y(), 1.0f);
        GL11.glTranslatef(-pivotX, -pivotY, 0.0f);
    }
}
