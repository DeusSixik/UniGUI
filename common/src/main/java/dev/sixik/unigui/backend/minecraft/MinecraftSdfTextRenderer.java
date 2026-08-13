package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.impl.text.SdfGlyph;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** GPU renderer for backend-independent single-channel SDF fonts. */
public final class MinecraftSdfTextRenderer implements AutoCloseable {
    private static final int BASE_PIXEL_SIZE = 48;
    private static final int SPREAD = 8;
    private static final int ATLAS_PADDING = 2;
    private static final int ATLAS_SIZE = 1024;
    private static final int FLOATS_PER_VERTEX = 9;
    private static final boolean DEBUG_GL = Boolean.getBoolean("unigui.sdf.debugGl");
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftSdfTextRenderer.class);

    private static final String VERTEX_SHADER = """
            #version 150
            in vec3 Position;
            in vec2 UV;
            in vec4 Color;
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            out vec2 vertexUV;
            out vec4 vertexColor;
            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                vertexUV = UV;
                vertexColor = Color;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 150
            uniform sampler2D GlyphAtlas;
            uniform float SdfSpread;
            in vec2 vertexUV;
            in vec4 vertexColor;
            out vec4 fragColor;
            void main() {
                float distance = texture(GlyphAtlas, vertexUV).r;
                vec2 unitRange = vec2(SdfSpread) / vec2(textureSize(GlyphAtlas, 0));
                vec2 screenTexSize = vec2(1.0) / fwidth(vertexUV);
                float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
                float alpha = clamp(screenPxRange * (distance - 0.5) + 0.5, 0.0, 1.0);
                fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
            }
            """;

    private final DefaultFontRegistry fonts;
    private final Map<FontFace, FontAtlas> atlases = new IdentityHashMap<>();
    private FontFace defaultFace;
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private FloatBuffer vertexUpload = BufferUtils.createFloatBuffer(FLOATS_PER_VERTEX * 6 * 64);
    private ByteBuffer glyphUpload = BufferUtils.createByteBuffer(64 * 64);
    private int program;
    private int vertexArray;
    private int vertexBuffer;
    private int modelViewLocation;
    private int projectionLocation;
    private int sdfSpreadLocation;
    private boolean initialized;
    private boolean unavailable;
    private boolean firstSubmitLogged;

    public MinecraftSdfTextRenderer(DefaultFontRegistry fonts) {
        this.fonts = fonts == null ? DefaultFontRegistry.global() : fonts;
        this.defaultFace = this.fonts.defaultFace();
    }

    public FontFace defaultFace() {
        return defaultFace;
    }

    public MinecraftSdfTextRenderer defaultFace(FontFace defaultFace) {
        this.defaultFace = defaultFace == null ? fonts.defaultFace() : defaultFace;
        return this;
    }

    public boolean render(GuiGraphics graphics, List<DrawCommand> commands, PoseStack pose) {
        return render(graphics, commands, pose, false);
    }

    public boolean render(GuiGraphics graphics, List<DrawCommand> commands, PoseStack pose,
                          boolean renderingToPremultipliedTarget) {
        if (graphics == null || commands == null || commands.isEmpty() || pose == null || unavailable) return false;
        for (DrawCommand command : commands) {
            if (!canRender(command)) return false;
        }

        graphics.flush();
        GlState state = GlState.capture();
        try {
            if (DEBUG_GL) {
                while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                    // Isolate renderer errors from stale errors left by foreign render code.
                }
            }
            if (!initialize()) return false;
            List<Batch> batches = layout(commands, pose.last().pose());
            if (batches.isEmpty()) return true;

            GL20.glUseProgram(program);
            uploadMatrix(modelViewLocation, RenderSystem.getModelViewMatrix());
            uploadMatrix(projectionLocation, RenderSystem.getProjectionMatrix());
            GL20.glUniform1f(sdfSpreadLocation, SPREAD);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(renderingToPremultipliedTarget);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            for (Batch batch : batches) {
                if (batch.vertices.size == 0) continue;
                RenderSystem.bindTexture(batch.page.textureId);
                FloatBuffer vertices = uploadBuffer(batch.vertices);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, batch.vertices.size / FLOATS_PER_VERTEX);
            }
            if (DEBUG_GL) {
                int error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) {
                    throw new IllegalStateException("OpenGL error after SDF text submit: 0x"
                            + Integer.toHexString(error));
                }
            }
            if (!firstSubmitLogged) {
                int vertices = 0;
                for (Batch batch : batches) vertices += batch.vertices.size / FLOATS_PER_VERTEX;
                LOGGER.info("UniGUI SDF text active: commands={}, batches={}, vertices={}",
                        commands.size(), batches.size(), vertices);
                firstSubmitLogged = true;
            }
            return true;
        } catch (Throwable failure) {
            unavailable = true;
            LOGGER.error("Disabling UniGUI SDF text renderer; Minecraft font fallback will be used", failure);
            return false;
        } finally {
            state.restore();
        }
    }

    public boolean render(GuiGraphics graphics, DrawBatch batch, PoseStack pose,
                          boolean renderingToPremultipliedTarget) {
        if (batch == null) return false;
        return renderRaw(graphics, batch.commandElements(), batch.size(), pose, renderingToPremultipliedTarget);
    }

    private boolean renderRaw(GuiGraphics graphics, Object[] rawCommands, int commandCount, PoseStack pose,
                              boolean renderingToPremultipliedTarget) {
        if (graphics == null || rawCommands == null || commandCount == 0 || pose == null || unavailable) return false;
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (!canRender(command)) return false;
        }

        graphics.flush();
        GlState state = GlState.capture();
        try {
            if (DEBUG_GL) {
                while (GL11.glGetError() != GL11.GL_NO_ERROR) {
                    // Isolate renderer errors from stale errors left by foreign render code.
                }
            }
            if (!initialize()) return false;
            ObjectArrayList<Batch> batches = layout(rawCommands, commandCount, pose.last().pose());
            if (batches.isEmpty()) return true;

            GL20.glUseProgram(program);
            uploadMatrix(modelViewLocation, RenderSystem.getModelViewMatrix());
            uploadMatrix(projectionLocation, RenderSystem.getProjectionMatrix());
            GL20.glUniform1f(sdfSpreadLocation, SPREAD);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(renderingToPremultipliedTarget);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Object[] rawBatches = batches.elements();
            for (int i = 0, size = batches.size(); i < size; i++) {
                Batch batch = (Batch) rawBatches[i];
                if (batch.vertices.size == 0) continue;
                RenderSystem.bindTexture(batch.page.textureId);
                FloatBuffer vertices = uploadBuffer(batch.vertices);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, batch.vertices.size / FLOATS_PER_VERTEX);
            }
            if (DEBUG_GL) {
                int error = GL11.glGetError();
                if (error != GL11.GL_NO_ERROR) {
                    throw new IllegalStateException("OpenGL error after SDF text submit: 0x"
                            + Integer.toHexString(error));
                }
            }
            if (!firstSubmitLogged) {
                int vertices = 0;
                for (int i = 0, size = batches.size(); i < size; i++) {
                    Batch batch = (Batch) rawBatches[i];
                    vertices += batch.vertices.size / FLOATS_PER_VERTEX;
                }
                LOGGER.info("UniGUI SDF text active: commands={}, batches={}, vertices={}",
                        commandCount, batches.size(), vertices);
                firstSubmitLogged = true;
            }
            return true;
        } catch (Throwable failure) {
            unavailable = true;
            LOGGER.error("Disabling UniGUI SDF text renderer; Minecraft font fallback will be used", failure);
            return false;
        } finally {
            state.restore();
        }
    }
    public boolean unavailable() {
        return unavailable;
    }

    private boolean canRender(DrawCommand command) {
        if (command == null || command.text() == null || command.text().isEmpty()) return true;
        RichText text = richText(command);
        for (TextRun run : text.runs()) {
            if (!(resolvedFace(run) instanceof SdfGlyphProvider)) return false;
        }
        return true;
    }

    private boolean initialize() {
        if (initialized) return true;
        program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (program == 0) {
            unavailable = true;
            return false;
        }

        vertexArray = GL30.glGenVertexArrays();
        vertexBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        int position = GL20.glGetAttribLocation(program, "Position");
        int uv = GL20.glGetAttribLocation(program, "UV");
        int color = GL20.glGetAttribLocation(program, "Color");
        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        GL20.glEnableVertexAttribArray(position);
        GL20.glVertexAttribPointer(position, 3, GL11.GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(uv);
        GL20.glVertexAttribPointer(uv, 2, GL11.GL_FLOAT, false, stride, 3L * Float.BYTES);
        GL20.glEnableVertexAttribArray(color);
        GL20.glVertexAttribPointer(color, 4, GL11.GL_FLOAT, false, stride, 5L * Float.BYTES);

        modelViewLocation = GL20.glGetUniformLocation(program, "ModelViewMat");
        projectionLocation = GL20.glGetUniformLocation(program, "ProjMat");
        sdfSpreadLocation = GL20.glGetUniformLocation(program, "SdfSpread");
        int atlasLocation = GL20.glGetUniformLocation(program, "GlyphAtlas");
        GL20.glUseProgram(program);
        GL20.glUniform1i(atlasLocation, 0);
        GL20.glUniform1f(sdfSpreadLocation, SPREAD);
        initialized = true;
        return true;
    }

    private ObjectArrayList<Batch> layout(Object[] rawCommands, int commandCount, org.joml.Matrix4f basePose) {
        ObjectArrayList<Batch> batches = new ObjectArrayList<>();
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (!hasText(command) || !visibleAlpha(command.paint().color(), null)) continue;
            layoutCommand(batches, command, richText(command), basePose);
        }
        return batches;
    }
    private List<Batch> layout(List<DrawCommand> commands, org.joml.Matrix4f basePose) {
        List<Batch> batches = new ObjectArrayList<>();
        for (DrawCommand command : commands) {
            if (!hasText(command) || !visibleAlpha(command.paint().color(), null)) continue;
            layoutCommand(batches, command, richText(command), basePose);
        }
        return batches;
    }

    private void layoutCommand(List<Batch> batches, DrawCommand command, RichText text,
                               org.joml.Matrix4f basePose) {
        RectView bounds = command.bounds();
        List<LineInfo> lines = lineInfo(text);
        TransformState transformState = TransformState.from(command, basePose);
        float penX = bounds.x();
        float lineTop = bounds.y();
        int lineIndex = 0;
        float baseline = lineTop + lines.get(0).ascent;

        for (TextRun run : text.runs()) {
            FontFace face = resolvedFace(run);
            SdfGlyphProvider provider = (SdfGlyphProvider) face;
            FontAtlas atlas = atlases.computeIfAbsent(face, ignored -> new FontAtlas(provider));
            float scale = run.pixelSize() / BASE_PIXEL_SIZE;
            ColorView runColor = run.color();
            boolean runVisible = visibleAlpha(command.paint().color(), runColor);
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    penX = bounds.x();
                    lineTop += lines.get(lineIndex).height;
                    lineIndex = Math.min(lineIndex + 1, lines.size() - 1);
                    baseline = lineTop + lines.get(lineIndex).ascent;
                    continue;
                }

                GlyphPlacement placement = atlas.glyph(codePoint);
                if (placement == null) {
                    penX += Math.max(0.0f, face.advance(codePoint, run.pixelSize()));
                    continue;
                }
                float left = penX + placement.bearingX * scale;
                float top = baseline + placement.bearingY * scale;
                float width = placement.width * scale;
                float height = placement.height * scale;
                if (runVisible) {
                    Batch batch = nextBatch(batches, placement.page);
                    addQuad(batch.vertices, left, top, width, height,
                            placement.u0, placement.v0, placement.u1, placement.v1,
                            command.paint(), runColor, transformState);
                }
                penX += placement.advance * scale;
            }
        }
    }

    private List<LineInfo> lineInfo(RichText text) {
        List<LineInfo> lines = new ObjectArrayList<>();
        float ascent = 0.0f;
        float height = 0.0f;
        for (TextRun run : text.runs()) {
            FontMetrics metrics = resolvedFace(run).metrics(run.pixelSize());
            ascent = Math.max(ascent, metrics.ascent());
            height = Math.max(height, metrics.lineHeight());
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    lines.add(new LineInfo(positive(ascent), positive(height)));
                    ascent = metrics.ascent();
                    height = metrics.lineHeight();
                }
            }
        }
        lines.add(new LineInfo(positive(ascent), positive(height)));
        return lines;
    }

    private RichText richText(DrawCommand command) {
        RichText richText = command.richText();
        return richText == null
                ? RichText.of(command.text(), defaultFace, TextRun.DEFAULT_PIXEL_SIZE)
                : richText;
    }

    private static boolean hasText(DrawCommand command) {
        if (command == null) return false;
        if (command.richText() != null) return !command.richText().isEmpty();
        return command.text() != null && !command.text().isEmpty();
    }

    private FontFace resolvedFace(TextRun run) {
        return run.font() == null ? defaultFace : run.font();
    }

    private static Batch nextBatch(List<Batch> batches, AtlasPage page) {
        if (!batches.isEmpty() && batches.get(batches.size() - 1).page == page) {
            return batches.get(batches.size() - 1);
        }
        Batch batch = new Batch(page);
        batches.add(batch);
        return batch;
    }

    private static void addQuad(FloatArray vertices, float x, float y, float width, float height,
                                float u0, float v0, float u1, float v1, Paint paint,
                                ColorView runColor, TransformState transformState) {
        ColorView base = paint.color();
        float r = clamp01(base.r()) * (runColor == null ? 1.0f : clamp01(runColor.r()));
        float g = clamp01(base.g()) * (runColor == null ? 1.0f : clamp01(runColor.g()));
        float b = clamp01(base.b()) * (runColor == null ? 1.0f : clamp01(runColor.b()));
        float a = clamp01(base.a()) * (runColor == null ? 1.0f : clamp01(runColor.a()));

        addVertex(vertices, x, y, u0, v0, r, g, b, a, transformState);
        addVertex(vertices, x, y + height, u0, v1, r, g, b, a, transformState);
        addVertex(vertices, x + width, y + height, u1, v1, r, g, b, a, transformState);
        addVertex(vertices, x, y, u0, v0, r, g, b, a, transformState);
        addVertex(vertices, x + width, y + height, u1, v1, r, g, b, a, transformState);
        addVertex(vertices, x + width, y, u1, v0, r, g, b, a, transformState);
    }

    private static void addVertex(FloatArray vertices, float x, float y, float u, float v,
                                  float r, float g, float b, float a, TransformState state) {
        float poseX = state.m00 * x + state.m10 * y + state.m30;
        float poseY = state.m01 * x + state.m11 * y + state.m31;
        float poseZ = state.m02 * x + state.m12 * y + state.m32;
        vertices.add(poseX, poseY, poseZ, u, v, r, g, b, a);
    }

    private void uploadMatrix(int location, org.joml.Matrix4f matrix) {
        matrixBuffer.clear();
        matrixBuffer.limit(16);
        matrix.get(matrixBuffer);
        GL20.glUniformMatrix4fv(location, false, matrixBuffer);
    }

    private FloatBuffer uploadBuffer(FloatArray values) {
        if (vertexUpload.capacity() < values.size) {
            int capacity = Math.max(values.size, vertexUpload.capacity() * 2);
            vertexUpload = BufferUtils.createFloatBuffer(capacity);
        }
        vertexUpload.clear();
        vertexUpload.put(values.values, 0, values.size);
        vertexUpload.flip();
        return vertexUpload;
    }

    private ByteBuffer uploadGlyphBuffer(byte[] pixels) {
        if (glyphUpload.capacity() < pixels.length) {
            glyphUpload = BufferUtils.createByteBuffer(Math.max(pixels.length, glyphUpload.capacity() * 2));
        }
        glyphUpload.clear();
        glyphUpload.put(pixels);
        glyphUpload.flip();
        return glyphUpload;
    }

    private static int linkProgram(String vertexSource, String fragmentSource) {
        int vertex = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            return 0;
        }
        int linkedProgram = GL20.glCreateProgram();
        GL20.glAttachShader(linkedProgram, vertex);
        GL20.glAttachShader(linkedProgram, fragment);
        GL20.glBindAttribLocation(linkedProgram, 0, "Position");
        GL20.glBindAttribLocation(linkedProgram, 1, "UV");
        GL20.glBindAttribLocation(linkedProgram, 2, "Color");
        GL20.glLinkProgram(linkedProgram);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(linkedProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to link UniGUI SDF shader: {}", GL20.glGetProgramInfoLog(linkedProgram));
            GL20.glDeleteProgram(linkedProgram);
            return 0;
        }
        return linkedProgram;
    }

    private static int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to compile UniGUI SDF {} shader: {}",
                    type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment",
                    GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static float positive(float value) {
        return value > 0.0f ? value : TextRun.DEFAULT_PIXEL_SIZE;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static boolean visibleAlpha(ColorView base, ColorView run) {
        if (base == null) return true;
        float alpha = clamp01(base.a()) * (run == null ? 1.0f : clamp01(run.a()));
        return Math.max(0, Math.min(255, Math.round(alpha * 255.0f))) > 0;
    }

    @Override
    public void close() {
        for (FontAtlas atlas : atlases.values()) atlas.close();
        atlases.clear();
        if (vertexBuffer != 0) GL15.glDeleteBuffers(vertexBuffer);
        if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
        if (program != 0) GL20.glDeleteProgram(program);
        vertexBuffer = 0;
        vertexArray = 0;
        program = 0;
        initialized = false;
    }

    private final class FontAtlas implements AutoCloseable {
        private final SdfGlyphProvider provider;
        private final Map<Integer, GlyphPlacement> glyphs = new HashMap<>();
        private final ObjectArrayList<AtlasPage> pages = new ObjectArrayList<>();

        private FontAtlas(SdfGlyphProvider provider) {
            this.provider = provider;
        }

        private GlyphPlacement glyph(int codePoint) {
            GlyphPlacement cached = glyphs.get(codePoint);
            if (cached != null) return cached;
            SdfGlyph glyph = provider.sdfGlyph(codePoint, BASE_PIXEL_SIZE, SPREAD);
            if (glyph == null) return null;
            AtlasPage page = pages.isEmpty() ? newPage() : pages.get(pages.size() - 1);
            if (!page.canFit(glyph.width(), glyph.height())) page = newPage();
            GlyphPlacement placement = page.add(glyph);
            glyphs.put(codePoint, placement);
            return placement;
        }

        private AtlasPage newPage() {
            AtlasPage page = new AtlasPage(ATLAS_SIZE);
            pages.add(page);
            return page;
        }

        @Override
        public void close() {
            Object[] rawPages = pages.elements();
            for (int i = 0, size = pages.size(); i < size; i++) {
                AtlasPage page = (AtlasPage) rawPages[i];
                page.close();
            }
            pages.clear();
            glyphs.clear();
        }
    }

    private final class AtlasPage implements AutoCloseable {
        private final int size;
        private final int textureId;
        private int cursorX = ATLAS_PADDING;
        private int cursorY = ATLAS_PADDING;
        private int rowHeight;

        private AtlasPage(int size) {
            this.size = size;
            textureId = GL11.glGenTextures();
            PixelUnpackState unpackState = PixelUnpackState.capture();
            try {
                unpackState.applyTight();
                RenderSystem.bindTexture(textureId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
                ByteBuffer emptyAtlas = BufferUtils.createByteBuffer(size * size);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, size, size, 0,
                        GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, emptyAtlas);
            } finally {
                unpackState.restore();
            }
        }

        private boolean canFit(int width, int height) {
            if (width + ATLAS_PADDING > size || height + ATLAS_PADDING > size) return false;
            if (cursorX + width > size) return cursorY + rowHeight + ATLAS_PADDING + height <= size;
            return cursorY + height <= size;
        }

        private GlyphPlacement add(SdfGlyph glyph) {
            if (cursorX + glyph.width() > size) {
                cursorX = ATLAS_PADDING;
                cursorY += rowHeight + ATLAS_PADDING;
                rowHeight = 0;
            }
            int x = cursorX;
            int y = cursorY;
            cursorX += glyph.width() + ATLAS_PADDING;
            rowHeight = Math.max(rowHeight, glyph.height());
            ByteBuffer pixels = uploadGlyphBuffer(glyph.pixels());
            PixelUnpackState unpackState = PixelUnpackState.capture();
            try {
                unpackState.applyTight();
                RenderSystem.bindTexture(textureId);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, x, y,
                        glyph.width(), glyph.height(), GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, pixels);
            } finally {
                unpackState.restore();
            }
            return new GlyphPlacement(this, glyph.width(), glyph.height(), glyph.advance(),
                    glyph.bearingX(), glyph.bearingY(),
                    x / (float) size, y / (float) size,
                    (x + glyph.width()) / (float) size,
                    (y + glyph.height()) / (float) size);
        }

        @Override
        public void close() {
            if (textureId != 0) GL11.glDeleteTextures(textureId);
        }
    }

    private record GlyphPlacement(AtlasPage page, int width, int height, float advance,
                                  float bearingX, float bearingY,
                                  float u0, float v0, float u1, float v1) {
    }

    private record LineInfo(float ascent, float height) {
    }

    private record TransformState(float m00, float m01, float m02,
                                  float m10, float m11, float m12,
                                  float m30, float m31, float m32) {
        private static TransformState from(DrawCommand command, org.joml.Matrix4f pose) {
            org.joml.Matrix4f matrix = MinecraftTransform.commandMatrix(pose, command);
            return new TransformState(
                    matrix.m00(), matrix.m01(), matrix.m02(),
                    matrix.m10(), matrix.m11(), matrix.m12(),
                    matrix.m30(), matrix.m31(), matrix.m32());
        }
    }

    private static final class Batch {
        private final AtlasPage page;
        private final FloatArray vertices = new FloatArray();

        private Batch(AtlasPage page) {
            this.page = page;
        }
    }

    private static final class FloatArray {
        private float[] values = new float[FLOATS_PER_VERTEX * 6 * 64];
        private int size;

        private void add(float v0, float v1, float v2, float v3, float v4,
                         float v5, float v6, float v7, float v8) {
            ensure(size + FLOATS_PER_VERTEX);
            values[size++] = v0;
            values[size++] = v1;
            values[size++] = v2;
            values[size++] = v3;
            values[size++] = v4;
            values[size++] = v5;
            values[size++] = v6;
            values[size++] = v7;
            values[size++] = v8;
        }

        private void ensure(int required) {
            if (required <= values.length) return;
            values = java.util.Arrays.copyOf(values, Math.max(required, values.length * 2));
        }
    }

    private record GlState(int program, int vertexArray, int arrayBuffer, int activeTexture,
                           int texture, boolean blend, int blendSourceRgb, int blendDestinationRgb,
                           int blendSourceAlpha, int blendDestinationAlpha, boolean depthTest,
                           boolean depthMask, boolean cull) {
        private static GlState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            return new GlState(
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
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
            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
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

    private record PixelUnpackState(int alignment, int rowLength, int skipPixels, int skipRows,
                                    int imageHeight, int skipImages, int buffer) {
        private static PixelUnpackState capture() {
            return new PixelUnpackState(
                    GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT),
                    GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH),
                    GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS),
                    GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS),
                    GL11.glGetInteger(GL12.GL_UNPACK_IMAGE_HEIGHT),
                    GL11.glGetInteger(GL12.GL_UNPACK_SKIP_IMAGES),
                    GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING));
        }

        private void applyTight() {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, 0);
        }

        private void restore() {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL12.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
            GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_IMAGES, skipImages);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        }
    }
}
