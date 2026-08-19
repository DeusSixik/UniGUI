package dev.sixik.unigui.api.render.shaders;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Глобальный registry shader providers, используемый render backend'ами.
 *
 * <p>Resolve order: embedded source в {@link ShaderHandle}, preferred providers из вызова,
 * вручную зарегистрированные providers, ServiceLoader providers и classpath fallback. Ошибки
 * отдельных providers намеренно подавляются, чтобы один сломанный provider не ломал весь UI.</p>
 */
public final class ShaderProviders {
    private static final CopyOnWriteArrayList<ShaderProvider> REGISTERED = new CopyOnWriteArrayList<>();
    private static final CopyOnWriteArrayList<ShaderProvider> SERVICES = new CopyOnWriteArrayList<>();
    private static final ShaderProvider CLASSPATH = new ClasspathShaderProvider();
    private static volatile boolean servicesLoaded;

    private ShaderProviders() {
    }

    /**
     * Регистрирует provider вручную.
     *
     * @param provider поставщик shader source-ов
     * @return closeable, который удаляет provider из registry
     */
    public static AutoCloseable register(ShaderProvider provider) {
        ShaderProvider normalized = Objects.requireNonNull(provider, "provider");
        REGISTERED.addIfAbsent(normalized);
        return () -> unregister(normalized);
    }

    /**
     * Удаляет provider из ручной регистрации.
     *
     * @param provider provider для удаления
     * @return {@code true}, если provider был зарегистрирован
     */
    public static boolean unregister(ShaderProvider provider) {
        return provider != null && REGISTERED.remove(provider);
    }

    /**
     * @return snapshot вручную зарегистрированных providers
     */
    public static List<ShaderProvider> registeredProviders() {
        return List.copyOf(REGISTERED);
    }

    /**
     * Разрешает shader source с опциональным списком preferred providers.
     *
     * @param handle handle шейдера
     * @param preferredProviders providers с максимальным приоритетом
     * @return shader source или empty
     */
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

    /**
     * Разрешает shader source через стандартный provider chain.
     *
     * @param handle handle шейдера
     * @return shader source или empty
     */
    public static Optional<ShaderSource> resolve(ShaderHandle handle) {
        return resolve(handle, new ShaderProvider[0]);
    }

    /**
     * Возвращает полный snapshot provider chain после ServiceLoader discovery.
     *
     * @return providers в порядке применения
     */
    public static List<ShaderProvider> allProvidersSnapshot() {
        ensureServicesLoaded();
        List<ShaderProvider> providers = new ObjectArrayList<>(REGISTERED.size() + SERVICES.size() + 1);
        providers.addAll(REGISTERED);
        providers.addAll(SERVICES);
        providers.add(CLASSPATH);
        return List.copyOf(providers);
    }

    /** Повторно загружает providers из ServiceLoader. */
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
            // ServiceLoader discovery выполняется best-effort; ручная регистрация и classpath fallback остаются доступными.
        }
    }
}