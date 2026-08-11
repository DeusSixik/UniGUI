package dev.sixik.unigui.api.render.shaders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Loads shader files from the Java classpath.
 */
public final class ClasspathShaderProvider implements ShaderProvider {
    private final ClassLoader classLoader;

    public ClasspathShaderProvider() {
        this(defaultClassLoader());
    }

    public ClasspathShaderProvider(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    @Override
    public Optional<ShaderSource> load(ShaderHandle handle) {
        if (handle == null || handle.hasEmbeddedFragmentSource()) return Optional.empty();

        String fragment = readFirst(ShaderResourcePaths.classpathFragmentCandidates(handle.id()));
        if (fragment == null || fragment.isBlank()) return Optional.empty();

        String vertex = readFirst(ShaderResourcePaths.classpathVertexCandidates(handle.id()));
        return Optional.of(ShaderSource.source(handle.id(), vertex, fragment));
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