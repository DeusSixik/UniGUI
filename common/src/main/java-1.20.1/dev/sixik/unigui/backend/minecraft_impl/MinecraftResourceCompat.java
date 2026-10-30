package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.resource.UniResource;
import net.minecraft.resources.ResourceLocation;

final class MinecraftResourceCompat {
    private MinecraftResourceCompat() {
    }

    static ResourceLocation toMinecraft(UniResource resource) {
        ResourceLocation location = ResourceLocation.tryBuild(resource.namespace(), resource.path());
        if (location == null) throw new IllegalArgumentException("Invalid resource id: " + resource);
        return location;
    }
}
