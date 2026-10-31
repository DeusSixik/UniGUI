package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.TextureHandle;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps UniGUI texture handles to Minecraft resource locations and remembers which GL textures already have mipmaps.
 *
 * <p>A missing resource needs no special handling: {@code TextureManager} logs the failure once and binds vanilla's
 * "missingno" texture. The mipmap record is cleared on every resource reload because reloaded textures get new GL
 * objects, and GL names are reused.</p>
 */
final class GtnhTextures {
    private static final ResourceLocation MISSING = new ResourceLocation("minecraft", "missingno");
    private static final Map<String, ResourceLocation> PARSED = new ConcurrentHashMap<>();
    private static final IntOpenHashSet MIPMAPPED = new IntOpenHashSet();
    private static boolean reloadListenerRegistered;

    private GtnhTextures() {
    }

    /**
     * @param texture handle of any kind; {@code null} gives the missing texture
     * @return resource location to bind. {@link GtnhTextureHandle} carries its location, other handles are parsed
     * from their id once and cached
     */
    static ResourceLocation location(TextureHandle texture) {
        if (texture == null) return MISSING;
        if (texture instanceof GtnhTextureHandle own) return own.location();
        return location(texture.id());
    }

    /**
     * @param id texture id such as {@code modid:textures/gui/icon.png}; {@code null} or blank gives the missing texture
     * @return resource location to bind
     */
    static ResourceLocation location(String id) {
        if (id == null || id.trim().isEmpty()) return MISSING;
        return PARSED.computeIfAbsent(id, ResourceLocation::new);
    }

    /**
     * Records that a GL texture holds a mipmap chain.
     *
     * @param glTextureId GL texture name
     * @return {@code true} if it was not recorded before, that is, mipmaps still have to be generated
     */
    static synchronized boolean markMipmapped(int glTextureId) {
        registerReloadListener();
        return MIPMAPPED.add(glTextureId);
    }

    /**
     * Forgets a GL texture, for example when it is deleted.
     */
    static synchronized void forgetMipmapped(int glTextureId) {
        MIPMAPPED.remove(glTextureId);
    }

    private static void registerReloadListener() {
        if (reloadListenerRegistered) return;
        reloadListenerRegistered = true;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null && minecraft.getResourceManager() instanceof IReloadableResourceManager manager) {
            manager.registerReloadListener(resourceManager -> {
                synchronized (GtnhTextures.class) {
                    MIPMAPPED.clear();
                }
            });
        }
    }
}