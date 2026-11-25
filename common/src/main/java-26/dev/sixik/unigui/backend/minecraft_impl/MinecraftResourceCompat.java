package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.resource.UniResource;
import net.minecraft.resources.Identifier;

final class MinecraftResourceCompat {
    private MinecraftResourceCompat() {
    }

    static Identifier toMinecraft(UniResource resource) {
        Identifier location = Identifier.tryBuild(resource.namespace(), resource.path());
        if (location == null) throw new IllegalArgumentException("Invalid resource id: " + resource);
        return location;
    }
}
