package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL33;

import java.util.HashMap;
import java.util.Map;

final class MinecraftTextureSamplerState {
    private static final Scope NOOP = () -> {
    };
    private static final Map<TextureOptions, Integer> SAMPLERS = new HashMap<>();
    private static Boolean samplerObjectsSupported;

    private MinecraftTextureSamplerState() {
    }

    static Scope apply(TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (normalized.isDefault()) return NOOP;
        return supportsSamplerObjects() ? applySamplerObject(normalized) : applyTextureParameters(normalized);
    }

    static void applyOwnedTextureOptions(int textureId, TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (textureId <= 0 || normalized.isDefault()) return;

        Runnable apply = () -> applyOwnedTextureOptionsNow(textureId, normalized);
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(apply::run);
            return;
        }

        apply.run();
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

    private static Scope applySamplerObject(TextureOptions options) {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousSampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        int sampler = sampler(options);
        GL33.glBindSampler(0, sampler);
        RenderSystem.activeTexture(activeTexture);
        return () -> {
            int restoreActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            GL33.glBindSampler(0, previousSampler);
            RenderSystem.activeTexture(restoreActiveTexture);
        };
    }

    private static Scope applyTextureParameters(TextureOptions options) {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previousMin = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
        int previousMag = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
        int previousWrapS = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
        int previousWrapT = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter(options.minFilter()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter(options.magFilter()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap(options.wrapS()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap(options.wrapT()));
        RenderSystem.activeTexture(activeTexture);

        return () -> {
            int restoreActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, previousMin);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, previousMag);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, previousWrapS);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, previousWrapT);
            RenderSystem.activeTexture(restoreActiveTexture);
        };
    }

    private static int sampler(TextureOptions options) {
        Integer existing = SAMPLERS.get(options);
        if (existing != null) return existing;

        int sampler = GL33.glGenSamplers();
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MIN_FILTER, minFilter(options.minFilter()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MAG_FILTER, magFilter(options.magFilter()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_S, wrap(options.wrapS()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_T, wrap(options.wrapT()));
        SAMPLERS.put(options, sampler);
        return sampler;
    }

    private static boolean supportsSamplerObjects() {
        if (samplerObjectsSupported != null) return samplerObjectsSupported;
        try {
            samplerObjectsSupported = GL.getCapabilities() != null && GL.getCapabilities().OpenGL33;
        } catch (Throwable ignored) {
            samplerObjectsSupported = false;
        }
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

    interface Scope {
        void close();
    }
}
