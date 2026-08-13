package dev.sixik.unigui.api.render.shaders;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global shader provider registry used by render backends.
 */
public final class ShaderProviders {
    private static final CopyOnWriteArrayList<ShaderProvider> REGISTERED = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ShaderProvider> SERVICES = new CopyOnWriteArrayList<>();
    private static final ShaderProvider CLASSPATH = new ClasspathShaderProvider();
    private static volatile boolean servicesLoaded;

    private ShaderProviders() {
    }

    public static AutoCloseable register(ShaderProvider provider) {
        ShaderProvider normalized = Objects.requireNonNull(provider, "provider");
        REGISTERED.addIfAbsent(normalized);
        return () -> unregister(normalized);
    }

    public static boolean unregister(ShaderProvider provider) {
        return provider != null && REGISTERED.remove(provider);
    }

    public static List<ShaderProvider> registeredProviders() {
        return List.copyOf(REGISTERED);
    }

    public static Optional<ShaderSource> resolve(ShaderHandle handle, ShaderProvider... preferredProviders) {
        if (handle == null) return Optional.empty();

        if (handle.hasEmbeddedFragmentSource()) {
            return Optional.of(ShaderSource.source(handle.id(), handle.vertexSource(), handle.fragmentSource()));
        }

        if (preferredProviders != null) {
            for (ShaderProvider provider : preferredProviders) {
                Optional<ShaderSource> resolved = load(provider, handle);
                if (resolved.isPresent()) return resolved;
            }
        }

        ensureServicesLoaded();

        for (ShaderProvider provider : REGISTERED) {
            Optional<ShaderSource> resolved = load(provider, handle);
            if (resolved.isPresent()) return resolved;
        }
        for (ShaderProvider provider : SERVICES) {
            Optional<ShaderSource> resolved = load(provider, handle);
            if (resolved.isPresent()) return resolved;
        }

        return load(CLASSPATH, handle);
    }

    public static Optional<ShaderSource> resolve(ShaderHandle handle) {
        return resolve(handle, new ShaderProvider[0]);
    }

    public static List<ShaderProvider> allProvidersSnapshot() {
        ensureServicesLoaded();
        List<ShaderProvider> providers = new ObjectArrayList<>(REGISTERED.size() + SERVICES.size() + 1);
        providers.addAll(REGISTERED);
        providers.addAll(SERVICES);
        providers.add(CLASSPATH);
        return List.copyOf(providers);
    }

    public static void reloadServices() {
        SERVICES.clear();
        servicesLoaded = false;
        ensureServicesLoaded();
    }

    private static Optional<ShaderSource> load(ShaderProvider provider, ShaderHandle handle) {
        if (provider == null) return Optional.empty();
        try {
            return provider.load(handle).map(ShaderSource::copy);
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private static void ensureServicesLoaded() {
        if (servicesLoaded) return;
        synchronized (ShaderProviders.class) {
            if (servicesLoaded) return;
            loadServices(Thread.currentThread().getContextClassLoader());
            loadServices(ShaderProvider.class.getClassLoader());
            servicesLoaded = true;
        }
    }

    private static void loadServices(ClassLoader classLoader) {
        if (classLoader == null) return;
        try {
            for (ShaderProvider provider : ServiceLoader.load(ShaderProvider.class, classLoader)) {
                SERVICES.addIfAbsent(provider);
            }
        } catch (Throwable ignored) {
            // Service discovery is best-effort; manual registration and classpath fallback remain available.
        }
    }
}