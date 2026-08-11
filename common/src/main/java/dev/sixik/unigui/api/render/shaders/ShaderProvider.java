package dev.sixik.unigui.api.render.shaders;

import java.util.Optional;

/**
 * Resolves shader source code for backend-specific or application-specific shader ids.
 *
 * <p>Providers can be registered at runtime through {@link ShaderProviders#register(ShaderProvider)}
 * or discovered with Java {@link java.util.ServiceLoader} by adding a
 * {@code META-INF/services/dev.sixik.unigui.api.render.shaders.ShaderProvider} file.</p>
 */
public interface ShaderProvider {
    Optional<ShaderSource> load(ShaderHandle handle);
}