package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.opengl.GlSampler;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import java.nio.ByteBuffer;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Прямой OpenGL-путь для собственных GLSL-программ UniGUI (SDF-фигуры, SDF-текст, пользовательские шейдеры).
 *
 * <p>Пайплайны 26.1 принимают только uniform-блоки и шейдеры, загруженные через {@code ShaderSource}. Шейдеры
 * UniGUI и пользовательские шейдеры написаны под GLSL 150 с обычными uniform-переменными, поэтому для них
 * сохранён прежний путь: своя программа, свой VAO и вызовы GL напрямую. В 26.1 бэкенд по-прежнему OpenGL.</p>
 *
 * <p>Чтобы не сломать кэши Minecraft, состояние, которое кэширует {@link GlStateManager} (blend, depth, cull,
 * scissor, активный юнит и привязка текстур, framebuffer), меняется только через него. Состояние, которое
 * {@code GlCommandEncoder} считает своим (текущая программа и VAO), запоминается в {@link #begin()} и
 * возвращается в {@link #end(Session)}.</p>
 */
final class UiGl {
    private static final Map<GpuTexture, FramebufferEntry> FRAMEBUFFERS = new IdentityHashMap<>();
    private static final Map<VertexFormat, ImmediateArray> IMMEDIATE_ARRAYS = new IdentityHashMap<>();
    private static final Session SESSION = new Session();

    private UiGl() {
    }

    /**
     * Состояние GL, которое нужно вернуть после прямой отрисовки.
     */
    static final class Session {
        private int program;
        private int vertexArray;
        private int arrayBuffer;
        /** Состояние сохранено через glGet (запасной путь, если сбросить кеш программы vanilla нельзя). */
        private boolean queried;
        private boolean active;
    }

    /**
     * Сброс кеша программы {@code GlCommandEncoder.lastProgram}.
     *
     * <p>{@code glGetInteger} при каждой отрисовке синхронизирует драйвер с GPU, и под нагрузкой это давало
     * пики кадра. Vanilla 26.1 перед каждой отрисовкой сам привязывает VAO и буферы, а программу пропускает,
     * только если она совпадает с {@code lastProgram}. Поэтому вместо сохранения и восстановления состояния
     * достаточно сбросить этот кеш: следующий проход vanilla привяжет свою программу заново.</p>
     */
    private static final class ProgramCacheReset {
        private static final java.lang.reflect.Field ENCODER_LAST_PROGRAM;
        private static final Object ENCODER;

        static {
            java.lang.reflect.Field field = null;
            Object encoder = null;
            try {
                Object device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
                java.lang.reflect.Field backendField = com.mojang.blaze3d.systems.GpuDevice.class.getDeclaredField("backend");
                backendField.setAccessible(true);
                Object backend = backendField.get(device);
                java.lang.reflect.Field encoderField = backend.getClass().getDeclaredField("encoder");
                encoderField.setAccessible(true);
                encoder = encoderField.get(backend);
                field = encoder.getClass().getDeclaredField("lastProgram");
                field.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException unavailable) {
                field = null;
                encoder = null;
            }
            ENCODER_LAST_PROGRAM = field;
            ENCODER = encoder;
        }

        /** {@code -Dunigui.gl.queryState=true} возвращает старый путь с glGet (для сравнения и на всякий случай). */
        private static final boolean FORCE_QUERY = Boolean.getBoolean("unigui.gl.queryState");

        static boolean available() {
            return ENCODER_LAST_PROGRAM != null && !FORCE_QUERY;
        }

        static void reset() {
            try {
                ENCODER_LAST_PROGRAM.set(ENCODER, null);
            } catch (IllegalAccessException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    /**
     * Начинает прямую отрисовку в текущую цель {@link UiGpu}: привязывает её framebuffer, viewport и scissor,
     * выключает глубину и отсечение граней.
     *
     * @return сессия для {@link #end(Session)}
     */
    static Session begin() {
        Session session = SESSION;
        if (session.active) throw new IllegalStateException("Nested UniGUI GL session");
        session.active = true;
        session.queried = !ProgramCacheReset.available();
        if (session.queried) {
            session.program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
            session.vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            session.arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        }

        UiGpu.Target target = UiGpu.target();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer(target.color().texture()));
        GlStateManager._viewport(0, 0, target.width(), target.height());
        if (UiGpu.scissorEnabled()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(UiGpu.scissorX(), UiGpu.scissorY(), UiGpu.scissorWidth(), UiGpu.scissorHeight());
        } else {
            GlStateManager._disableScissorTest();
        }
        GlStateManager._disableDepthTest();
        GlStateManager._depthMask(false);
        GlStateManager._disableCull();
        GlStateManager._colorMask(15);
        return session;
    }

    /**
     * Завершает прямую отрисовку и возвращает программу, VAO и буфер вершин, которые были до {@link #begin()}.
     */
    static void end(Session session) {
        if (session == null || !session.active) return;
        session.active = false;
        if (session.queried) {
            GlStateManager._glUseProgram(session.program);
            GlStateManager._glBindVertexArray(session.vertexArray);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, session.arrayBuffer);
            return;
        }
        // Не оставляем свои VAO и буфер привязанными; vanilla всё равно привязывает своё перед отрисовкой.
        GlStateManager._glUseProgram(0);
        GlStateManager._glBindVertexArray(0);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        ProgramCacheReset.reset();
    }

    /**
     * Включает смешивание по режиму UniGUI.
     */
    static void blend(MinecraftUiBlend blend) {
        MinecraftGpuCompat.glBlend(blend == null ? null : blend.function());
    }

    /**
     * Привязывает текстуру к юниту для GLSL-сэмплера.
     *
     * @param unit    номер юнита
     * @param view    вид текстуры
     * @param sampler сэмплер или {@code null} (параметры самой текстуры)
     */
    static void bindTexture(int unit, GpuTextureView view, GpuSampler sampler) {
        GlStateManager._activeTexture(GL13Constants.TEXTURE0 + unit);
        if (view == null || view.isClosed()) {
            GlStateManager._bindTexture(0);
            GL33.glBindSampler(unit, 0);
            return;
        }
        GlTexture texture = (GlTexture) view.texture();
        GlStateManager._bindTexture(texture.glId());
        GL33.glBindSampler(unit, sampler instanceof GlSampler glSampler ? glSampler.getId() : 0);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12Constants.TEXTURE_BASE_LEVEL, view.baseMipLevel());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL12Constants.TEXTURE_MAX_LEVEL,
                view.baseMipLevel() + view.mipLevels() - 1);
        GlStateManager._activeTexture(GL13Constants.TEXTURE0);
    }

    /**
     * Привязывает текстуру по GL-имени (атлас глифов UniGUI и другие собственные текстуры).
     */
    static void bindTexture(int unit, int textureId) {
        GlStateManager._activeTexture(GL13Constants.TEXTURE0 + unit);
        GlStateManager._bindTexture(textureId);
        GL33.glBindSampler(unit, 0);
        GlStateManager._activeTexture(GL13Constants.TEXTURE0);
    }

    /**
     * Рисует меш текущей программой. Атрибуты привязаны по порядку элементов формата: элемент {@code i} формата
     * идёт в location {@code i} (так же, как раньше делал {@code BufferUploader}). Меш закрывается.
     */
    static void drawImmediate(MeshData mesh) {
        if (mesh == null) return;
        try {
            MeshData.DrawState state = mesh.drawState();
            VertexFormat format = state.format();
            ImmediateArray array = IMMEDIATE_ARRAYS.computeIfAbsent(format, ImmediateArray::new);
            GlStateManager._glBindVertexArray(array.vertexArray);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, array.vertexBuffer);
            ByteBuffer vertices = mesh.vertexBuffer();
            GlStateManager._glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);
            if (MinecraftGpuCompat.primitive(mesh) == UiPrimitive.QUADS) {
                ByteBuffer indices = quadIndices(state.vertexCount());
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, array.indexBuffer);
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STREAM_DRAW);
                GL11.glDrawElements(GL11.GL_TRIANGLES, state.vertexCount() / 4 * 6, GL11.GL_UNSIGNED_INT, 0L);
            } else {
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, state.vertexCount());
            }
        } finally {
            mesh.close();
        }
    }

    private static ByteBuffer quadIndexBuffer;

    private static ByteBuffer quadIndices(int vertexCount) {
        int quads = vertexCount / 4;
        int bytes = quads * 6 * Integer.BYTES;
        if (quadIndexBuffer == null || quadIndexBuffer.capacity() < bytes) {
            quadIndexBuffer = org.lwjgl.BufferUtils.createByteBuffer(Math.max(bytes, 6 * 4 * 64));
        }
        ByteBuffer buffer = quadIndexBuffer;
        buffer.clear();
        for (int quad = 0; quad < quads; quad++) {
            int base = quad * 4;
            buffer.putInt(base).putInt(base + 1).putInt(base + 2);
            buffer.putInt(base + 2).putInt(base + 3).putInt(base);
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Копирует прямоугольник цветовой текстуры в другую текстуру (координаты в строках GL, снизу вверх).
     *
     * <p>Замена {@code CommandEncoder.copyTextureToTexture}: в 26.1 он передаёт в {@code glBlitFramebuffer}
     * ширину и высоту вместо правой и верхней границы, поэтому копия со смещением растягивается или обрезается.</p>
     */
    static void copyColor(GpuTexture source, GpuTexture destination, int destX, int destY,
                          int sourceX, int sourceY, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int read = framebuffer(source);
        int draw = framebuffer(destination);
        GlStateManager._disableScissorTest();
        // Через GlStateManager: он кеширует привязки framebuffer, и vanilla дальше привяжет нужные сама.
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
        GL30.glBlitFramebuffer(sourceX, sourceY, sourceX + width, sourceY + height,
                destX, destY, destX + width, destY + height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }

    /**
     * Возвращает framebuffer для цветовой текстуры (создаётся при первом обращении).
     */
    static int framebuffer(GpuTexture texture) {
        pruneFramebuffers();
        FramebufferEntry entry = FRAMEBUFFERS.get(texture);
        if (entry != null) return entry.id;
        int id = GL30.glGenFramebuffers();
        int previous = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                ((GlTexture) texture).glId(), 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
        FRAMEBUFFERS.put(texture, new FramebufferEntry(texture, id));
        return id;
    }

    private static void pruneFramebuffers() {
        if (FRAMEBUFFERS.isEmpty()) return;
        Iterator<FramebufferEntry> iterator = FRAMEBUFFERS.values().iterator();
        while (iterator.hasNext()) {
            FramebufferEntry entry = iterator.next();
            if (entry.texture.isClosed()) {
                GlStateManager._glDeleteFramebuffers(entry.id);
                iterator.remove();
            }
        }
    }

    private record FramebufferEntry(GpuTexture texture, int id) {
    }

    /**
     * VAO и буферы для немедленной отрисовки одного формата вершин.
     */
    private static final class ImmediateArray {
        private final int vertexArray;
        private final int vertexBuffer;
        private final int indexBuffer;

        private ImmediateArray(VertexFormat format) {
            vertexArray = GlStateManager._glGenVertexArrays();
            vertexBuffer = GlStateManager._glGenBuffers();
            indexBuffer = GlStateManager._glGenBuffers();
            GlStateManager._glBindVertexArray(vertexArray);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            MinecraftGpuCompat.glVertexAttributes(format);
        }
    }

    /** Константы GL 1.3, чтобы не тянуть весь класс ради двух значений. */
    private static final class GL13Constants {
        private static final int TEXTURE0 = 0x84C0;
    }

    /** Константы GL 1.2. */
    private static final class GL12Constants {
        private static final int TEXTURE_BASE_LEVEL = 0x813C;
        private static final int TEXTURE_MAX_LEVEL = 0x813D;
    }
}
