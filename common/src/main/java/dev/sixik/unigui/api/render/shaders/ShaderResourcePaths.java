package dev.sixik.unigui.api.render.shaders;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Общая нормализация путей для shader resource handles.
 *
 * <p>Класс строит candidates для classpath и namespaced resource lookup. Он принимает как простые id,
 * так и {@code namespace:path}, добавляет стандартные {@code shaders/} и extension варианты и убирает
 * дубликаты с сохранением порядка.</p>
 */
public final class ShaderResourcePaths {
    private ShaderResourcePaths() {
    }

    /**
     * Парсит shader id в namespace/path пару.
     *
     * @param id id шейдера
     * @return id ресурса
     */
    public static ResourceId parse(String id) {
        String normalized = normalize(id);
        int separator = normalized.indexOf(':');
        if (separator > 0) {
            String namespace = normalized.substring(0, separator).trim();
            String path = normalized.substring(separator + 1).trim();
            if (!namespace.isEmpty() && !path.isEmpty()) {
                return new ResourceId(namespace, normalizePath(path));
            }
        }
        return new ResourceId(null, normalizePath(normalized));
    }

    /** @return classpath candidates для fragment shader source */
    public static List<String> classpathFragmentCandidates(String id) {
        return classpathCandidates(id, "fsh");
    }

    /** @return classpath candidates для vertex shader source */
    public static List<String> classpathVertexCandidates(String id) {
        return classpathCandidates(id, "vsh");
    }

    /**
     * Строит classpath candidates для shader resource.
     *
     * @param id id шейдера
     * @param extension расширение без точки
     * @return candidates в порядке lookup
     */
    public static List<String> classpathCandidates(String id, String extension) {
        ResourceId parsed = parse(id);
        String ext = normalizeExtension(extension);
        String path = parsed.path();
        Set<String> candidates = new LinkedHashSet<>();

        if (parsed.hasNamespace()) {
            addNamespacedClasspath(candidates, parsed.namespace(), path, ext);
        } else {
            addUnqualifiedClasspath(candidates, path, ext);
        }

        return new ObjectArrayList<>(candidates);
    }

    /** @return namespaced candidates для fragment shader source */
    public static List<NamespacedPath> namespacedFragmentCandidates(String id) {
        return namespacedCandidates(id, "fsh");
    }

    /** @return namespaced candidates для vertex shader source */
    public static List<NamespacedPath> namespacedVertexCandidates(String id) {
        return namespacedCandidates(id, "vsh");
    }

    /**
     * Строит namespaced resource candidates.
     *
     * @param id id шейдера с namespace
     * @param extension расширение без точки
     * @return candidates в порядке lookup
     */
    public static List<NamespacedPath> namespacedCandidates(String id, String extension) {
        ResourceId parsed = parse(id);
        if (!parsed.hasNamespace()) return List.of();

        String ext = normalizeExtension(extension);
        String namespace = parsed.namespace();
        String path = parsed.path();
        Set<NamespacedPath> candidates = new LinkedHashSet<>();
        if (path.startsWith("shaders/")) {
            candidates.add(new NamespacedPath(namespace, withExtension(path, ext)));
        } else {
            candidates.add(new NamespacedPath(namespace, "shaders/" + withExtension(path, ext)));
            candidates.add(new NamespacedPath(namespace, withExtension(path, ext)));
        }
        return new ObjectArrayList<>(candidates);
    }

    private static void addNamespacedClasspath(Set<String> candidates, String namespace, String path, String ext) {
        if (path.startsWith("assets/")) {
            candidates.add(withExtension(path, ext));
            return;
        }

        if (path.startsWith("shaders/")) {
            candidates.add("assets/" + namespace + "/" + withExtension(path, ext));
        } else {
            candidates.add("assets/" + namespace + "/shaders/" + withExtension(path, ext));
            candidates.add("assets/" + namespace + "/" + withExtension(path, ext));
        }
    }

    private static void addUnqualifiedClasspath(Set<String> candidates, String path, String ext) {
        if (path.startsWith("assets/")) {
            candidates.add(withExtension(path, ext));
            return;
        }

        if (path.startsWith("shaders/")) {
            candidates.add(withExtension(path, ext));
            candidates.add("assets/" + withExtension(path, ext));
        } else {
            candidates.add("shaders/" + withExtension(path, ext));
            candidates.add(withExtension(path, ext));
            candidates.add("assets/shaders/" + withExtension(path, ext));
        }
    }

    private static String withExtension(String path, String extension) {
        String normalized = normalizePath(path);
        String suffix = "." + extension;
        return normalized.endsWith(suffix) ? normalized : normalized + suffix;
    }

    private static String normalize(String id) {
        String normalized = id == null ? "" : id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Shader resource id must not be empty");
        }
        return normalized.replace('\\', '/');
    }

    private static String normalizePath(String path) {
        String normalized = path == null ? "" : path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Shader resource path must not be empty");
        }
        return normalized;
    }

    private static String normalizeExtension(String extension) {
        String normalized = extension == null ? "" : extension.trim();
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized.isEmpty() ? "fsh" : normalized;
    }

    /**
     * Разобранный resource id.
     *
     * @param namespace namespace или {@code null}
     * @param path нормализованный путь
     */
    public record ResourceId(String namespace, String path) {
        /** @return {@code true}, если id содержит namespace */
        public boolean hasNamespace() {
            return namespace != null && !namespace.isBlank();
        }
    }

    /**
     * Namespaced путь для backend resource lookup.
     *
     * @param namespace namespace ресурса
     * @param path путь внутри namespace
     */
    public record NamespacedPath(String namespace, String path) {
    }
}