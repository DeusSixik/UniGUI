package dev.sixik.unigui.backend.minecraft_impl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

/**
 * State shared between indexed scene draws and the enclosing game renderer.
 *
 * @param program       OpenGL program
 * @param activeTexture active GL texture enum
 * @param texture       texture on unit zero
 * @param blend         blend enable flag
 * @param src           RGB source factor
 * @param dst           RGB destination factor
 * @param srcAlpha      alpha source factor
 * @param dstAlpha      alpha destination factor
 * @param depth         depth test enable flag
 * @param write         depth write mask
 * @param cull          face culling enable flag
 */
record GlSceneState(int program, int activeTexture, int texture, boolean blend, int src, int dst,
                    int srcAlpha, int dstAlpha, boolean depth, boolean write, boolean cull) {
    /**
     * @param gl tracked platform state
     * @return snapshot of the state used by both renderers
     */
    static GlSceneState capture(GlScenePlatform.State gl) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        gl.activeTexture(GL13.GL_TEXTURE0);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        gl.activeTexture(active);
        return new GlSceneState(GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), active, texture,
                GL11.glIsEnabled(GL11.GL_BLEND), GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_DST_RGB), GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA), GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK), GL11.glIsEnabled(GL11.GL_CULL_FACE));
    }

    /**
     * @param gl tracked platform state to restore
     */
    void restore(GlScenePlatform.State gl) {
        GL20.glUseProgram(program);
        gl.activeTexture(GL13.GL_TEXTURE0);
        gl.bindTexture(texture);
        gl.blendFuncSeparate(src, dst, srcAlpha, dstAlpha);
        gl.blend(blend);
        gl.depthMask(write);
        gl.depthTest(depth);
        gl.cull(cull);
        gl.activeTexture(activeTexture);
    }
}
