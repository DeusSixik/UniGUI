package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;

/**
 * Version-specific пекер иконок живых сущностей.
 *
 * <p>Рендерит модель моба в offscreen render target (по образцу
 * {@code InventoryScreen.renderEntityInInventoryFollowsMouse}, но без привязки
 * к текущему фреймбуферу), после чего иконку можно рисовать обычной
 * texture-командой, уважающей transform/clip Z-слоёв UniGUI.</p>
 */
final class MinecraftEntityBakeCompat {
    private MinecraftEntityBakeCompat() {
    }

    /** Рендерит сущность в новый offscreen-таргет {@code pixels x pixels}. */
    static MinecraftRenderTarget bake(LivingEntity entity, int pixels) {
        Minecraft minecraft = Minecraft.getInstance();
        MinecraftRenderTarget target = new MinecraftRenderTarget(
                pixels, pixels, new RenderTargetOptions(true, true, "entity_preview_" + pixels));
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        IntBuffer scissorBox = BufferUtils.createIntBuffer(16);
        if (scissorEnabled) GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
        // Копия RenderPassState.capture() из FastItemRenderer: GL-состояния,
        // изменённые entity-рендером, восстанавливаются после прохода.
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableScissor();
        target.bindWrite();
        RenderSystem.backupProjectionMatrix();
        Object modelView = RenderSystem.getModelViewStack();
        MinecraftFastItemCompat.pushModelView(modelView);
        try {
            MinecraftFastItemCompat.resetModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            float depth = Math.max(1000.0f, pixels * 32.0f);
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0.0f, pixels, pixels, 0.0f, -depth, depth),
                    VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            // Entity-шейдеры требуют 3D-освещения (как setupFor3DItems у предметов).
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();

            GuiGraphics graphics = new GuiGraphics(minecraft, MinecraftBufferCompat.immediate(256));
            // Геометрия как в ванильном inventory-превью: центр по X, ноги у нижней кромки.
            int centerX = pixels / 2;
            int bottomY = Math.round(pixels * 0.92f);
            int entityScale = Math.max(1, Math.round(pixels * 0.72f));
            MinecraftEntityPreviewCompat.render(graphics, centerX, bottomY, entityScale,
                    pixels, pixels * 0.35f, pixels * 0.18f, entity);
            graphics.flush();
        } finally {
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(depthMask);
            if (!depthTest) RenderSystem.disableDepthTest();
            MinecraftFastItemCompat.popModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            target.unbindWrite();
            minecraft.getMainRenderTarget().bindWrite(true);
            if (scissorEnabled) {
                RenderSystem.enableScissor(scissorBox.get(0), scissorBox.get(1),
                        scissorBox.get(2), scissorBox.get(3));
            }
        }
        return target;
    }
}
