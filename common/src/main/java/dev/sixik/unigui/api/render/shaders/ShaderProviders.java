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
        return Optional.ofNullable(resolveOrNull(handle, preferredProviders));
    }

    /**
     * Разрешает shader source без создания {@link Optional}.
     *
     * <p>Метод предназначен для backend'ов и других горячих путей рендера.
     * При отсутствии source возвращается {@code null}.</p>
     *
     * @param handle handle шейдера
     * @param preferredProviders providers с максимальным приоритетом
     * @return shader source или {@code null}
     */
    public static ShaderSource resolveOrNull(ShaderHandle handle, ShaderProvider... preferredProviders) {
        return resolveOrNullInternal(handle, null, preferredProviders);
    }

    /**
     * Разрешает shader source через один preferred provider без создания varargs-массива.
     *
     * @param handle handle шейдера
     * @param preferredProvider provider с максимальным приоритетом
     * @return shader source или {@code null}
     */
    public static ShaderSource resolveOrNull(ShaderHandle handle, ShaderProvider preferredProvider) {
        return resolveOrNullInternal(handle, preferredProvider, null);
    }

    /**
     * Разрешает shader source через стандартный provider chain без {@link Optional}.
     *
     * @param handle handle шейдера
     * @return shader source или {@code null}
     */
    public static ShaderSource resolveOrNull(ShaderHandle handle) {
        return resolveOrNullInternal(handle, null, null);
    }

    /**
     * Разрешает shader source через стандартный provider chain.
     *
     * @param handle handle шейдера
     * @return shader source или empty
     */
    public static Optional<ShaderSource> resolve(ShaderHandle handle) {
        return Optional.ofNullable(resolveOrNull(handle));
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

    private static ShaderSource resolveOrNullInternal(ShaderHandle handle,
                                                      ShaderProvider preferredProvider,
                                                      ShaderProvider[] preferredProviders) {
        if (handle == null) return null;

        if (handle.hasEmbeddedFragmentSource()) {
            return ShaderSource.source(handle.id(), handle.vertexSource(), handle.fragmentSource());
        }

        if (preferredProvider != null) {
            ShaderSource resolved = loadOrNull(preferredProvider, handle);
            if (resolved != null) return resolved;
        }
        if (preferredProviders != null) {
            for (ShaderProvider provider : preferredProviders) {
                ShaderSource resolved = loadOrNull(provider, handle);
                if (resolved != null) return resolved;
            }
        }

        ensureServicesLoaded();

        for (ShaderProvider provider : REGISTERED) {
            ShaderSource resolved = loadOrNull(provider, handle);
            if (resolved != null) return resolved;
        }
        for (ShaderProvider provider : SERVICES) {
            ShaderSource resolved = loadOrNull(provider, handle);
            if (resolved != null) return resolved;
        }

        return loadOrNull(CLASSPATH, handle);
    }

    private static ShaderSource loadOrNull(ShaderProvider provider, ShaderHandle handle) {
        if (provider == null) return null;
        try {
            return provider.loadOrNull(handle);
        } catch (Throwable ignored) {
            return null;
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
