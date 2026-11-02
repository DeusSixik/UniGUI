package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/**
 * UniGUI texture backed by a Minecraft {@link ResourceLocation}: a resource-pack PNG or a texture registered with
 * {@link GtnhDynamicTextures}. The handle only describes the texture (location, size, sampling); the GL object is
 * owned by Minecraft's {@code TextureManager}. Immutable.
 */
public final class GtnhTextureHandle implements TextureHandle {
    private final ResourceLocation location;
    private final int width;
    private final int height;
    private final TextureOptions options;

    /**
     * @param location texture location, for example {@code modid:textures/gui/icon.png}
     * @param width    texture width in pixels
     * @param height   texture height in pixels
     */
    public GtnhTextureHandle(ResourceLocation location, int width, int height) {
        this(location, width, height, TextureOptions.defaults());
    }

    /**
     * @param location texture location
     * @param width    texture width in pixels
     * @param height   texture height in pixels
     * @param options  sampling options, {@code null} means {@link TextureOptions#defaults()}
     */
    public GtnhTextureHandle(ResourceLocation location, int width, int height, TextureOptions options) {
        this.location = Objects.requireNonNull(location, "location");
        this.width = width;
        this.height = height;
        this.options = options == null ? TextureOptions.defaults() : options;
    }

    /**
     * @return the Minecraft location of the texture
     */
    public ResourceLocation location() {
        return location;
    }

    /**
     * @param options new sampling options, {@code null} means defaults
     * @return this handle if the options are equal, otherwise a copy that samples the same texture differently
     */
    public GtnhTextureHandle withOptions(TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (this.options.equals(normalized)) return this;
        return new GtnhTextureHandle(location, width, height, normalized);
    }

    @Override
    public String id() {
        return location.toString();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public TextureOptions options() {
        return options;
    }

    @Override
    public Object nativeHandle() {
        return location;
    }
}
