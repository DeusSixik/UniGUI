package dev.sixik.unigui.xaml.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Реестр функций Java, доступных графам. Автор мода регистрирует функции, автор интерфейса вызывает их по имени.
 *
 * <p>Реестр общий для процесса; редактор строит палитру нод из него, поэтому добавленная функция сразу появляется
 * в списке доступных нод.</p>
 */
public final class GraphFunctions {
    private static final Map<String, GraphFunction> FUNCTIONS = new LinkedHashMap<>();
    private static final AtomicLong VERSION = new AtomicLong();

    private GraphFunctions() {
    }

    /**
     * Регистрирует функцию; функция с тем же именем заменяется.
     *
     * @param function функция
     * @return {@link AutoCloseable}, снимающий регистрацию
     */
    public static synchronized AutoCloseable register(GraphFunction function) {
        FUNCTIONS.put(function.name(), function);
        VERSION.incrementAndGet();
        return () -> unregister(function.name());
    }

    /**
     * Снимает регистрацию.
     *
     * @param name имя
     * @return {@code true}, если функция была
     */
    public static synchronized boolean unregister(String name) {
        boolean removed = FUNCTIONS.remove(name) != null;
        if (removed) VERSION.incrementAndGet();
        return removed;
    }

    /**
     * @param name имя
     * @return функция или пусто
     */
    public static synchronized Optional<GraphFunction> find(String name) {
        return Optional.ofNullable(FUNCTIONS.get(name));
    }

    /**
     * @return зарегистрированные функции по порядку регистрации
     */
    public static synchronized List<GraphFunction> all() {
        return new ArrayList<>(FUNCTIONS.values());
    }

    /**
     * @return счётчик изменений реестра: по нему кэши скомпилированных графов узнают, что функции сменились
     */
    public static long version() {
        return VERSION.get();
    }
}
