package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/**
 * Немедленный рендер ванильного текста Minecraft 26.1 в текущую цель {@link UiGpu}.
 *
 * <p>{@code GuiGraphicsExtractor.text} в 26.1 только ставит текст в отложенную очередь, а UniGUI должен
 * рисовать текст сразу, в том же порядке, что и остальной draw-list, в том числе в offscreen-цели. Поэтому
 * текст раскладывается штатным {@link Font#prepareText}, а глифы пишутся в буфер {@code POSITION_TEX_COLOR}
 * и рисуются пайплайном UniGUI. Соседние глифы с одной текстурой атласа рисуются одним вызовом.</p>
 *
 * <p>Глифы «intensity»-шрифтов (одноканальные атласы) выводятся как обычные: такие шрифты в GUI редки.</p>
 */
final class UiVanillaText {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final ByteBufferBuilder BYTES = new ByteBufferBuilder(16 * 1024);

    private UiVanillaText() {
    }

    /**
     * Рисует строку.
     *
     * @param font   шрифт
     * @param text   текст
     * @param x      X в координатах позы
     * @param y      Y в координатах позы
     * @param color  цвет ARGB
     * @param shadow рисовать ли тень
     * @param pose   поза
     * @param blend  режим смешивания
     */
    static void draw(Font font, Component text, float x, float y, int color, boolean shadow, Matrix4f pose,
                     MinecraftUiBlend blend) {
        if (font == null || text == null) return;
        draw(font, text.getVisualOrderText(), x, y, color, shadow, pose, blend);
    }

    /**
     * Рисует строку {@link FormattedCharSequence}.
     */
    static void draw(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose,
                     MinecraftUiBlend blend) {
        if (font == null || text == null || !UiGpu.hasTarget()) return;
        Font.PreparedText prepared = font.prepareText(text, x, y, color, shadow, false, 0);
        Collector collector = new Collector(pose, blend);
        prepared.visit(collector);
        collector.flush();
    }

    /**
     * Собирает глифы подряд идущими группами с одной текстурой.
     */
    private static final class Collector implements Font.GlyphVisitor {
        private final Matrix4f pose;
        private final MinecraftUiBlend blend;
        private final GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        private BufferBuilder builder;
        private GpuTextureView texture;

        private Collector(Matrix4f pose, MinecraftUiBlend blend) {
            this.pose = pose;
            this.blend = blend;
        }

        @Override
        public void acceptGlyph(TextRenderable.Styled glyph) {
            add(glyph);
        }

        @Override
        public void acceptEffect(TextRenderable effect) {
            add(effect);
        }

        private void add(TextRenderable glyph) {
            GpuTextureView view = glyph.textureView();
            if (view == null) return;
            if (builder != null && view != texture) flush();
            if (builder == null) {
                builder = new BufferBuilder(BYTES, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                texture = view;
            }
            glyph.render(pose, builder, FULL_BRIGHT, false);
        }

        private void flush() {
            if (builder == null) return;
            MeshData mesh = builder.build();
            builder = null;
            if (mesh != null) {
                UiGpu.drawMesh(UiPipelines.texture(blend, VertexFormat.Mode.QUADS), mesh, texture, sampler);
            }
            texture = null;
        }
    }
}
