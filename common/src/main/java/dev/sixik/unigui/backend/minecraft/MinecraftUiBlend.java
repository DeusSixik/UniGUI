package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

final class MinecraftUiBlend {
    private MinecraftUiBlend() {
    }

    static void applyStraightAlpha(boolean renderingToPremultipliedTarget) {
        if (renderingToPremultipliedTarget) {
            RenderSystem.blendFuncSeparate(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA);
            return;
        }

        RenderSystem.defaultBlendFunc();
    }

    static void applyTextureAlpha(boolean premultipliedSource, boolean renderingToPremultipliedTarget) {
        if (premultipliedSource) {
            RenderSystem.blendFuncSeparate(
                    GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ONE_MINUS_SRC_ALPHA);
            return;
        }

        applyStraightAlpha(renderingToPremultipliedTarget);
    }
}
