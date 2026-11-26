package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL33;

import java.util.Arrays;

final class MinecraftTextureSamplerState {
    private static final RestoreState RESTORE_STATE = new RestoreState();
    private static final Int2IntMap SAMPLERS;
    private static boolean samplerObjectsSupported;
    private static boolean samplerObjectsSupportChecked;

    static {
        SAMPLERS = new Int2IntOpenHashMap();
        SAMPLERS.defaultReturnValue(-1);
    }

    private MinecraftTextureSamplerState() {
    }

    static boolean apply(TextureOptions options) {
        return apply(0, options);
    }

    static boolean apply(int textureUnit, TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (normalized.isDefault()) return false;
        int unit = Math.max(0, textureUnit);
        boolean useSamplerObjects = supportsSamplerObjects();
        RESTORE_STATE.capture(unit, useSamplerObjects);
        if (useSamplerObjects) applySamplerObject(unit, normalized);
        else applyTextureParameters(unit, normalized);
        return true;
    }

    /**
     * Восстанавливает последнее состояние, сохранённое через {@link #apply(TextureOptions)}.
     *
     * <p>Стек хранит только примитивные значения, поэтому во время операции
     * отрисовки не создаётся объект-замыкание для восстановления состояния сэмплера.</p>
     */
    static void restore() {
        RESTORE_STATE.restore();
    }

    /**
     * Возвращает текущую глубину стека восстановления.
     *
     * @return глубина стека, которую нужно передать в {@link #restoreTo(int)}
     */
    static int depth() {
        return RESTORE_STATE.depth();
    }

    /**
     * Восстанавливает все состояния сэмплеров, добавленные после указанной глубины стека.
     *
     * @param depth глубина стека, сохранённая перед операцией рендера
     */
    static void restoreTo(int depth) {
        RESTORE_STATE.restoreTo(depth);
    }

    static void applyOwnedTextureOptions(int textureId, TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (textureId <= 0 || normalized.isDefault()) return;

        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(() -> applyOwnedTextureOptionsNow(textureId, normalized));
            return;
        }

        applyOwnedTextureOptionsNow(textureId, normalized);
    }

    private static void applyOwnedTextureOptionsNow(int textureId, TextureOptions options) {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            RenderSystem.bindTexture(textureId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter(options.minFilter()));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter(options.magFilter()));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap(options.wrapS()));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap(options.wrapT()));
        } finally {
            RenderSystem.bindTexture(previousTexture);
            RenderSystem.activeTexture(activeTexture);
        }
    }

    private static void applySamplerObject(int textureUnit, TextureOptions options) {
        int sampler = sampler(options);
        GL33.glBindSampler(textureUnit, sampler);
    }

    private static void applyTextureParameters(int textureUnit, TextureOptions options) {

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter(options.minFilter()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter(options.magFilter()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap(options.wrapS()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap(options.wrapT()));
    }

    private static int sampler(TextureOptions options) {
        int key = options.packed();
        int existing = SAMPLERS.get(key);
        if (existing != -1) return existing;

        int sampler = GL33.glGenSamplers();
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MIN_FILTER, minFilter(options.minFilter()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MAG_FILTER, magFilter(options.magFilter()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_S, wrap(options.wrapS()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_T, wrap(options.wrapT()));
        SAMPLERS.put(key, sampler);
        return sampler;
    }

    private static boolean supportsSamplerObjects() {
        if (samplerObjectsSupportChecked) return samplerObjectsSupported;
        try {
            samplerObjectsSupported = GL.getCapabilities() != null && GL.getCapabilities().OpenGL33;
        } catch (Throwable ignored) {
            samplerObjectsSupported = false;
        }
        samplerObjectsSupportChecked = true;
        return samplerObjectsSupported;
    }

    private static int minFilter(TextureFilter filter) {
        return switch (filter == null ? TextureFilter.NEAREST : filter) {
            case NEAREST -> GL11.GL_NEAREST;
            case LINEAR -> GL11.GL_LINEAR;
            case NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR_MIPMAP_LINEAR;
        };
    }

    private static int magFilter(TextureFilter filter) {
        return switch (filter == null ? TextureFilter.NEAREST : filter) {
            case NEAREST, NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST;
            case LINEAR, LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR;
        };
    }

    private static int wrap(TextureWrap wrap) {
        return switch (wrap == null ? TextureWrap.CLAMP_TO_EDGE : wrap) {
            case CLAMP_TO_EDGE -> GL12.GL_CLAMP_TO_EDGE;
            case REPEAT -> GL11.GL_REPEAT;
            case MIRRORED_REPEAT -> GL14.GL_MIRRORED_REPEAT;
        };
    }

    private static final class RestoreState {
        private int[] units = new int[8];
        private boolean[] samplerModes = new boolean[8];
        private int[] activeTextures = new int[8];
        private int[] previousSamplers = new int[8];
        private int[] previousMinFilters = new int[8];
        private int[] previousMagFilters = new int[8];
        private int[] previousWrapS = new int[8];
        private int[] previousWrapT = new int[8];
        private int depth;

        private void capture(int textureUnit, boolean samplerObjects) {
            ensureCapacity(depth + 1);
            int index = depth++;
            units[index] = textureUnit;
            samplerModes[index] = samplerObjects;
            activeTextures[index] = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + textureUnit);
            if (samplerObjects) {
                previousSamplers[index] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
            } else {
                previousMinFilters[index] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
                previousMagFilters[index] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
                previousWrapS[index] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
                previousWrapT[index] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);
            }
        }

        private void restore() {
            if (depth == 0) return;
            int index = --depth;
            int textureUnit = units[index];
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + textureUnit);
            if (samplerModes[index]) {
                GL33.glBindSampler(textureUnit, previousSamplers[index]);
            } else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, previousMinFilters[index]);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, previousMagFilters[index]);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, previousWrapS[index]);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, previousWrapT[index]);
            }
            RenderSystem.activeTexture(activeTextures[index]);
        }

        private int depth() {
            return depth;
        }

        private void restoreTo(int targetDepth) {
            int target = Math.max(0, targetDepth);
            while (depth > target) restore();
        }

        private void ensureCapacity(int required) {
            if (required <= units.length) return;
            int capacity = Math.max(required, units.length * 2);
            units = Arrays.copyOf(units, capacity);
            samplerModes = Arrays.copyOf(samplerModes, capacity);
            activeTextures = Arrays.copyOf(activeTextures, capacity);
            previousSamplers = Arrays.copyOf(previousSamplers, capacity);
            previousMinFilters = Arrays.copyOf(previousMinFilters, capacity);
            previousMagFilters = Arrays.copyOf(previousMagFilters, capacity);
            previousWrapS = Arrays.copyOf(previousWrapS, capacity);
            previousWrapT = Arrays.copyOf(previousWrapT, capacity);
        }
    }
}
