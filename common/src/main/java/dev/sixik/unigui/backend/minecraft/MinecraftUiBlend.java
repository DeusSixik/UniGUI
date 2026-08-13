package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sixik.unigui.api.render.BlendMode;
import org.lwjgl.opengl.GL11;

final class MinecraftUiBlend {
    private MinecraftUiBlend() {
    }

    static void applyStraightAlpha(boolean renderingToPremultipliedTarget) {
        applyStraightAlpha(renderingToPremultipliedTarget, BlendMode.NORMAL);
    }

    static void applyStraightAlpha(boolean renderingToPremultipliedTarget, BlendMode blendMode) {
        if (blendMode == BlendMode.ADDITIVE) {
            RenderSystem.blendFuncSeparate(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ONE,
                    GL11.GL_ONE);
            return;
        }

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
        applyTextureAlpha(premultipliedSource, renderingToPremultipliedTarget, BlendMode.NORMAL);
    }

    static void applyTextureAlpha(boolean premultipliedSource, boolean renderingToPremultipliedTarget, BlendMode blendMode) {
        if (blendMode == BlendMode.ADDITIVE) {
            RenderSystem.blendFuncSeparate(
                    GL11.GL_SRC_ALPHA,
                    GL11.GL_ONE,
                    GL11.GL_ONE,
                    GL11.GL_ONE);
            return;
        }

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
