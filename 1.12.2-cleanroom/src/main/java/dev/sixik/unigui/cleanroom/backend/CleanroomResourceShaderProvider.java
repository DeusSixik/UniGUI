package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProvider;
import dev.sixik.unigui.api.render.shaders.ShaderResourcePaths;
import dev.sixik.unigui.api.render.shaders.ShaderSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Loads {@code .fsh} / {@code .vsh} files from resource packs and mod assets
 * ({@code assets/<namespace>/shaders/<name>.fsh}) through Minecraft's resource manager. Handles that embed their
 * fragment source are left to the other providers.
 */
final class CleanroomResourceShaderProvider implements ShaderProvider {
    @Override
    public Optional<ShaderSource> load(ShaderHandle handle) {
        return Optional.ofNullable(loadOrNull(handle));
    }

    @Override
    public ShaderSource loadOrNull(ShaderHandle handle) {
        if (handle == null || handle.hasEmbeddedFragmentSource()) return null;
        String fragment = readFirst(ShaderResourcePaths.namespacedFragmentCandidates(handle.id()));
        if (fragment == null || fragment.isBlank()) return null;
        String vertex = readFirst(ShaderResourcePaths.namespacedVertexCandidates(handle.id()));
        return ShaderSource.source(handle.id(), vertex, fragment);
    }

    private static String readFirst(Iterable<ShaderResourcePaths.NamespacedPath> candidates) {
        for (ShaderResourcePaths.NamespacedPath candidate : candidates) {
            String source = read(candidate);
            if (source != null) return source;
        }
        return null;
    }

    private static String read(ShaderResourcePaths.NamespacedPath candidate) {
        try {
            ResourceLocation location = new ResourceLocation(candidate.namespace(), candidate.path());
            String source = read(location);
            if (source != null) return source;
            if ("unigui_testmod".equals(candidate.namespace())) {
                return read(new ResourceLocation("unigui", candidate.path()));
            }
            return null;
        } catch (IOException | RuntimeException missing) {
            return null;
        }
    }

    private static String read(ResourceLocation location) throws IOException {
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
             InputStream stream = resource.getInputStream()) {
            return new String(readAll(stream), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException missing) {
            return null;
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        return stream.readAllBytes();
    }
}
