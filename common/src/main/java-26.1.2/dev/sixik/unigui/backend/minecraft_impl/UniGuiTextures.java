package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

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
        Identifier location = parseLocation(id);
        TextureOptions normalizedOptions = options == null ? TextureOptions.defaults() : options;
        UniGuiManagedTexture texture = new UniGuiManagedTexture("UniGUI " + id, image);
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
        Identifier location = parseLocation(Objects.requireNonNull(id, "id"));
        return location.toString();
    }

    private static String tryNormalizeId(String id) {
        if (id == null) return null;
        Identifier location = Identifier.tryParse(id);
        return location == null ? null : location.toString();
    }

    private static Identifier parseLocation(String id) {
        Identifier location = Identifier.tryParse(id);
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
                    image.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            return image;
        } catch (RuntimeException failure) {
            image.close();
            throw failure;
        }
    }

    private record RegisteredTexture(Identifier location, UniGuiManagedTexture texture, TextureHandle handle) {
    }

    /**
     * Текстура UniGUI. В 26.1 параметры выборки (фильтр, повтор) задаёт сэмплер при отрисовке
     * ({@link UiTextureBinding}), поэтому сама текстура их не хранит.
     */
    private static final class UniGuiManagedTexture extends DynamicTexture {
        private UniGuiManagedTexture(String label, NativeImage image) {
            super(() -> label, image);
        }
    }
}
