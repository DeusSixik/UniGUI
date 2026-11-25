package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class UniGuiTextures {
    private static final Map<String, RegisteredTexture> REGISTRY = new LinkedHashMap<>();

    private UniGuiTextures() {
    }

    public static synchronized TextureHandle register(String id, NativeImage image, TextureOptions options) {
        String normalizedId = normalizeId(id);
        if (REGISTRY.containsKey(normalizedId)) {
            throw new IllegalStateException("UniGUI texture is already registered: " + normalizedId);
        }
        return registerOrReplace(normalizedId, image, options);
    }

    public static TextureHandle register(String id, int width, int height, ByteBuffer rgba, TextureOptions options) {
        return register(id, imageFromRgba(width, height, rgba), options);
    }

    public static synchronized TextureHandle replace(String id, NativeImage image, TextureOptions options) {
        return registerOrReplace(normalizeId(id), image, options);
    }

    public static TextureHandle replace(String id, int width, int height, ByteBuffer rgba, TextureOptions options) {
        return replace(id, imageFromRgba(width, height, rgba), options);
    }

    public static synchronized TextureHandle get(String id) {
        String normalizedId = tryNormalizeId(id);
        if (normalizedId == null) return null;
        RegisteredTexture texture = REGISTRY.get(normalizedId);
        return texture == null ? null : texture.handle();
    }

    public static synchronized boolean unregister(String id) {
        String normalizedId = tryNormalizeId(id);
        if (normalizedId == null) return false;
        RegisteredTexture texture = REGISTRY.remove(normalizedId);
        if (texture == null) return false;
        textureManager().release(texture.location());
        return true;
    }

    public static synchronized void clear() {
        for (RegisteredTexture texture : REGISTRY.values()) {
            textureManager().release(texture.location());
        }
        REGISTRY.clear();
    }

    public static synchronized int size() {
        return REGISTRY.size();
    }

    private static TextureHandle registerOrReplace(String id, NativeImage image, TextureOptions options) {
        Objects.requireNonNull(image, "image");
        ResourceLocation location = parseLocation(id);
        TextureOptions normalizedOptions = options == null ? TextureOptions.defaults() : options;
        UniGuiManagedTexture texture = new UniGuiManagedTexture(image, normalizedOptions);
        MinecraftTextureHandle handle = new MinecraftTextureHandle(
                location,
                Math.max(1, image.getWidth()),
                Math.max(1, image.getHeight()),
                normalizedOptions);

        try {
            textureManager().register(location, texture);
        } catch (RuntimeException failure) {
            texture.close();
            throw failure;
        }

        REGISTRY.put(id, new RegisteredTexture(location, texture, handle));
        return handle;
    }

    private static TextureManager textureManager() {
        return Minecraft.getInstance().getTextureManager();
    }

    private static String normalizeId(String id) {
        ResourceLocation location = parseLocation(Objects.requireNonNull(id, "id"));
        return location.toString();
    }

    private static String tryNormalizeId(String id) {
        if (id == null) return null;
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : location.toString();
    }

    private static ResourceLocation parseLocation(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            throw new IllegalArgumentException("Invalid UniGUI texture id: " + id);
        }
        return location;
    }

    private static NativeImage imageFromRgba(int width, int height, ByteBuffer rgba) {
        Objects.requireNonNull(rgba, "rgba");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive: " + width + "x" + height);
        }

        long required = (long) width * (long) height * 4L;
        if (required > Integer.MAX_VALUE || rgba.remaining() < required) {
            throw new IllegalArgumentException("RGBA buffer must contain at least " + required + " bytes");
        }

        ByteBuffer source = rgba.slice();
        NativeImage image = new NativeImage(width, height, false);
        try {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int offset = (y * width + x) * 4;
                    int r = source.get(offset) & 0xFF;
                    int g = source.get(offset + 1) & 0xFF;
                    int b = source.get(offset + 2) & 0xFF;
                    int a = source.get(offset + 3) & 0xFF;
                    image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            return image;
        } catch (RuntimeException failure) {
            image.close();
            throw failure;
        }
    }

    private record RegisteredTexture(ResourceLocation location, UniGuiManagedTexture texture, TextureHandle handle) {
    }

    private static final class UniGuiManagedTexture extends DynamicTexture {
        private TextureOptions options;

        private UniGuiManagedTexture(NativeImage image, TextureOptions options) {
            super(image);
            this.options = options == null ? TextureOptions.defaults() : options;
            applyOptionsWhenReady();
        }

        @Override
        public void load(ResourceManager manager) {
            upload();
        }

        @Override
        public void upload() {
            super.upload();
            applyOptionsWhenReady();
        }

        private void applyOptionsWhenReady() {
            TextureOptions current = options == null ? TextureOptions.defaults() : options;
            if (current.isDefault()) return;

            MinecraftTextureSamplerState.applyOwnedTextureOptions(getId(), current);
        }
    }
}
