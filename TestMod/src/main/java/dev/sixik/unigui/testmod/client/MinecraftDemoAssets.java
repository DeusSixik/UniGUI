package dev.sixik.unigui.testmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.backend.minecraft_impl.UniGuiTextures;

import java.io.IOException;
import java.io.InputStream;

/** Minecraft asset adapter for the backend-neutral texture demos. */
final class MinecraftDemoAssets {
    private MinecraftDemoAssets() {
    }

    static TextureHandle load(String id, String resourcePath, TextureOptions options) {
        TextureHandle cached = UniGuiTextures.get(id);
        if (cached != null) return cached;
        try (InputStream stream = MinecraftDemoAssets.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) return placeholder(id, options);
            return UniGuiTextures.replace(id, NativeImage.read(stream), options);
        } catch (IOException | RuntimeException failure) {
            return placeholder(id, options);
        }
    }

    private static TextureHandle placeholder(String id, TextureOptions options) {
        return new SimpleTextureHandle(id, 1, 1, null, options);
    }
}
