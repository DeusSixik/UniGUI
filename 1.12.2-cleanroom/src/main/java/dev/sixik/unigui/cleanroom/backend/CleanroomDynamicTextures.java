package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Textures generated at runtime (maps, previews, procedural art), registered under an id and drawn through the
 * returned {@link TextureHandle}. Port of the 1.20 {@code UniGuiTextures}.
 *
 * <p>The pixels are RGBA bytes, red first, rows top to bottom. The GL texture is owned by Minecraft's
 * {@code TextureManager} under {@code id}; the registry owns the handle. Calls may come from any thread: the registry
 * is updated immediately, the GL upload runs on the client thread (at once when called from it, otherwise queued, so a
 * handle returned from another thread may draw as "missingno" for a frame or two).</p>
 */
public final class CleanroomDynamicTextures {
    private static final Map<String, Registered> REGISTRY = new LinkedHashMap<>();

    private CleanroomDynamicTextures() {
    }

    /**
     * Registers a new texture.
     *
     * @param id texture id, {@code modid:path}
     * @param width width in pixels
     * @param height height in pixels
     * @param rgba at least {@code width * height * 4} bytes of RGBA pixels; copied, the buffer stays yours
     * @param options sampling options, {@code null} means defaults
     * @return handle to draw the texture with
     * @throws IllegalStateException if {@code id} is already registered
     */
    public static synchronized TextureHandle register(String id, int width, int height, ByteBuffer rgba,
                                                      TextureOptions options) {
        String normalized = normalizeId(id);
        if (REGISTRY.containsKey(normalized)) {
            throw new IllegalStateException("UniGUI texture is already registered: " + normalized);
        }
        return put(normalized, width, height, CleanroomPixels.rgbaToArgb(width, height, rgba), options);
    }

    /**
     * Registers a texture or replaces the pixels of an existing one. Replacing with the same size uploads into the
     * existing GL texture, so handles drawn earlier keep working.
     *
     * @param id texture id, {@code modid:path}
     * @param width width in pixels
     * @param height height in pixels
     * @param rgba at least {@code width * height * 4} bytes of RGBA pixels; copied
     * @param options sampling options, {@code null} means defaults
     * @return handle to draw the texture with
     */
    public static synchronized TextureHandle replace(String id, int width, int height, ByteBuffer rgba,
                                                     TextureOptions options) {
        return put(normalizeId(id), width, height, CleanroomPixels.rgbaToArgb(width, height, rgba), options);
    }

    /**
     * @param id texture id
     * @return the registered handle, or {@code null}
     */
    public static synchronized TextureHandle get(String id) {
        if (id == null || id.isBlank()) return null;
        Registered registered = REGISTRY.get(new ResourceLocation(id).toString());
        return registered == null ? null : registered.handle;
    }

    /**
     * Frees a texture. Handles to it draw as "missingno" afterwards.
     *
     * @param id texture id
     * @return {@code true} if it was registered
     */
    public static synchronized boolean unregister(String id) {
        if (id == null || id.isBlank()) return false;
        Registered removed = REGISTRY.remove(new ResourceLocation(id).toString());
        if (removed == null) return false;
        onClientThread(() -> release(removed.location));
        return true;
    }

    /** Frees every registered texture. */
    public static synchronized void clear() {
        Registered[] all = REGISTRY.values().toArray(new Registered[0]);
        REGISTRY.clear();
        onClientThread(() -> {
            for (Registered registered : all) release(registered.location);
        });
    }

    /** @return number of registered textures */
    public static synchronized int size() {
        return REGISTRY.size();
    }

    private static TextureHandle put(String id, int width, int height, int[] argb, TextureOptions options) {
        TextureOptions normalizedOptions = options == null ? TextureOptions.defaults() : options;
        ResourceLocation location = new ResourceLocation(id);
        CleanroomTextureHandle handle = new CleanroomTextureHandle(location, width, height, normalizedOptions);
        REGISTRY.put(id, new Registered(location, handle));
        onClientThread(() -> upload(location, width, height, argb, normalizedOptions));
        return handle;
    }

    private static void upload(ResourceLocation location, int width, int height, int[] argb, TextureOptions options) {
        TextureManager manager = Minecraft.getMinecraft().getTextureManager();
        ITextureObject existing = manager.getTexture(location);
        if (existing instanceof ManagedTexture managed && managed.matches(width, height)) {
            managed.replacePixels(argb, options);
            return;
        }
        if (existing instanceof AbstractTexture old) {
            CleanroomTextures.forgetMipmapped(old.getGlTextureId());
            old.deleteGlTexture();
        }
        manager.loadTexture(location, new ManagedTexture(width, height, argb, options));
    }

    private static void release(ResourceLocation location) {
        TextureManager manager = Minecraft.getMinecraft().getTextureManager();
        if (manager.getTexture(location) instanceof ManagedTexture managed) {
            CleanroomTextures.forgetMipmapped(managed.getGlTextureId());
            managed.deleteGlTexture();
            manager.loadTexture(location, TextureUtil.MISSING_TEXTURE);
        }
    }

    private static void onClientThread(Runnable task) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.isCallingFromMinecraftThread()) task.run();
        else minecraft.addScheduledTask(task);
    }

    private static String normalizeId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("Invalid UniGUI texture id: '" + id + "'");
        return new ResourceLocation(id).toString();
    }

    private record Registered(ResourceLocation location, TextureHandle handle) {
    }

    /**
     * A texture that keeps its pixels, so a resource reload (which calls {@link #loadTexture}) can upload them again.
     */
    private static final class ManagedTexture extends AbstractTexture {
        private final int width;
        private final int height;
        private int[] argb;
        private TextureOptions options;

        private ManagedTexture(int width, int height, int[] argb, TextureOptions options) {
            this.width = width;
            this.height = height;
            this.argb = argb;
            this.options = options;
        }

        private boolean matches(int width, int height) {
            return this.width == width && this.height == height;
        }

        private void replacePixels(int[] argb, TextureOptions options) {
            this.argb = argb;
            this.options = options;
            upload();
        }

        @Override
        public void loadTexture(IResourceManager resourceManager) {
            upload();
        }

        private void upload() {
            int id = getGlTextureId();
            TextureUtil.allocateTexture(id, width, height);
            TextureUtil.uploadTexture(id, argb, width, height);
            GlStateManager.bindTexture(id);
            CleanroomSamplerState.applyToBoundTexture(options);
            CleanroomTextures.forgetMipmapped(id);
            if (options.mipmaps()) {
                // Allocating the texture dropped any old chain; new pixels need a new one.
                CleanroomSamplerState.generateMipmaps();
                CleanroomTextures.markMipmapped(id);
            }
        }
    }
}
