package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProvider;
import dev.sixik.unigui.api.render.shaders.ShaderResourcePaths;
import dev.sixik.unigui.api.render.shaders.ShaderSource;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Minecraft resource-manager shader provider.
 *
 * <p>It is intentionally a backend adapter over the generic ShaderProvider API:
 * UniGUI shader handles do not depend on Minecraft resource loading.</p>
 */
final class MinecraftResourceShaderProvider implements ShaderProvider {
    private final Minecraft minecraft;

    MinecraftResourceShaderProvider(Minecraft minecraft) {
        this.minecraft = Objects.requireNonNull(minecraft, "minecraft");
    }

    @Override
    public Optional<ShaderSource> load(ShaderHandle handle) {
        if (handle == null || handle.hasEmbeddedFragmentSource()) return Optional.empty();

        String fragment = readFirst(ShaderResourcePaths.namespacedFragmentCandidates(handle.id()));
        if (fragment == null || fragment.isBlank()) return Optional.empty();

        String vertex = readFirst(ShaderResourcePaths.namespacedVertexCandidates(handle.id()));
        return Optional.of(ShaderSource.source(handle.id(), vertex, fragment));
    }

    private String readFirst(Iterable<ShaderResourcePaths.NamespacedPath> candidates) {
        for (ShaderResourcePaths.NamespacedPath candidate : candidates) {
            String source = read(candidate);
            if (source != null) return source;
        }
        return null;
    }

    private String read(ShaderResourcePaths.NamespacedPath candidate) {
        ResourceLocation location = ResourceLocation.tryParse(candidate.namespace() + ":" + candidate.path());
        if (location == null) return null;
        try {
            Optional<Resource> resource = minecraft.getResourceManager().getResource(location);
            if (resource.isEmpty()) return null;
            try (InputStream stream = resource.get().open()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
            return null;
        }
    }
}