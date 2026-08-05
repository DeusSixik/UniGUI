package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.api.render.TextureHandle;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class MinecraftTextureHandle implements TextureHandle {
    private final ResourceLocation location;
    private final int width;
    private final int height;

    public MinecraftTextureHandle(ResourceLocation location, int width, int height) {
        this.location = Objects.requireNonNull(location, "location");
        this.width = width;
        this.height = height;
    }

    public ResourceLocation location() {
        return location;
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
    public Object nativeHandle() {
        return location;
    }
}
