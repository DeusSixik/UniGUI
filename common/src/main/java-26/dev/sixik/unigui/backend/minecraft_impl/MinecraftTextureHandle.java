package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.resource.UniResource;
import net.minecraft.resources.Identifier;

import java.util.Objects;

public final class MinecraftTextureHandle implements TextureHandle {
    private final Identifier location;
    private final int width;
    private final int height;
    private final TextureOptions options;

    public MinecraftTextureHandle(Identifier location, int width, int height) {
        this(location, width, height, TextureOptions.defaults());
    }

    public MinecraftTextureHandle(UniResource resource, int width, int height) {
        this(MinecraftResourceCompat.toMinecraft(Objects.requireNonNull(resource, "resource")), width, height);
    }

    public MinecraftTextureHandle(UniResource resource, int width, int height, TextureOptions options) {
        this(MinecraftResourceCompat.toMinecraft(Objects.requireNonNull(resource, "resource")), width, height, options);
    }

    public MinecraftTextureHandle(Identifier location, int width, int height, TextureOptions options) {
        this.location = Objects.requireNonNull(location, "location");
        this.width = width;
        this.height = height;
        this.options = options == null ? TextureOptions.defaults() : options;
    }

    public Identifier location() {
        return location;
    }

    public MinecraftTextureHandle withOptions(TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (this.options.equals(normalized)) return this;
        return new MinecraftTextureHandle(location, width, height, normalized);
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
