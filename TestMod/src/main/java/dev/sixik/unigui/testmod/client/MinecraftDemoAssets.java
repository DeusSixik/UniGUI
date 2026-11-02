package dev.sixik.unigui.testmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.backend.minecraft_impl.UniGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

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
            if (stream != null) return UniGuiTextures.replace(id, NativeImage.read(stream), options);
        } catch (IOException | RuntimeException failure) {
            return loadMinecraftResource(id, resourcePath, options);
        }
        return loadMinecraftResource(id, resourcePath, options);
    }

    private static TextureHandle loadMinecraftResource(String id, String resourcePath, TextureOptions options) {
        ResourceLocation location = ResourceLocation.tryParse(resourcePath);
        if (location == null) return placeholder(id, options);
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);
            if (resource == null) return placeholder(id, options);
            try (InputStream stream = resource.open()) {
                return UniGuiTextures.replace(id, NativeImage.read(stream), options);
            }
        } catch (IOException | RuntimeException failure) {
            return placeholder(id, options);
        }
    }

    private static TextureHandle placeholder(String id, TextureOptions options) {
        return new SimpleTextureHandle(id, 1, 1, null, options);
    }
}
