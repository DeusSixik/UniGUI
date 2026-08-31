package dev.sixik.unigui.api.render.shaders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Загружает shader-файлы из Java classpath.
 *
 * <p>Provider ищет fragment/vertex sources по candidates из {@link ShaderResourcePaths}. Он нужен как
 * дефолтный fallback для модов и приложений, которые кладут shader resources рядом с Java assets.</p>
 */
public final class ClasspathShaderProvider implements ShaderProvider {
    private final ClassLoader classLoader;

    /**
     * Создаёт provider с context class loader текущего потока.
     */
    public ClasspathShaderProvider() {
        this(defaultClassLoader());
    }

    /**
     * Создаёт provider с явным class loader.
     *
     * @param classLoader loader для поиска resources
     */
    public ClasspathShaderProvider(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    /**
     * Пытается загрузить source для shader handle из classpath.
     *
     * @param handle handle шейдера
     * @return shader source или empty
     */
    @Override
    public Optional<ShaderSource> load(ShaderHandle handle) {
        return Optional.ofNullable(loadOrNull(handle));
    }

    @Override
    public ShaderSource loadOrNull(ShaderHandle handle) {
        if (handle == null || handle.hasEmbeddedFragmentSource()) return null;

        String fragment = readFirst(ShaderResourcePaths.classpathFragmentCandidates(handle.id()));
        if (fragment == null || fragment.isBlank()) return null;

        String vertex = readFirst(ShaderResourcePaths.classpathVertexCandidates(handle.id()));
        return ShaderSource.source(handle.id(), vertex, fragment);
    }

    private String readFirst(Iterable<String> paths) {
        for (String path : paths) {
            String source = read(path);
            if (source != null) return source;
        }
        return null;
    }

    private String read(String path) {
        try (InputStream stream = classLoader.getResourceAsStream(stripLeadingSlash(path))) {
            if (stream == null) return null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String stripLeadingSlash(String path) {
        String normalized = path == null ? "" : path;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static ClassLoader defaultClassLoader() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        return context == null ? ClasspathShaderProvider.class.getClassLoader() : context;
    }
}
