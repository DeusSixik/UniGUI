package dev.sixik.unigui.xaml.struct;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Реестр структур. Структура, зарегистрированная здесь, доступна в контрактах компонентов как тип
 * {@code struct:Имя}, в пинах графа и в нодах {@code Struct.Make}, {@code Struct.Break}, {@code Struct.With}.
 *
 * <p>Структуру нужно зарегистрировать раньше компонентов, которые на неё ссылаются: контракт читается при
 * регистрации компонента.</p>
 */
public final class Structs {
    private static final Map<String, StructType> BY_NAME = new LinkedHashMap<>();
    private static final Map<Class<?>, StructType> BY_CLASS = new LinkedHashMap<>();
    private static final AtomicLong VERSION = new AtomicLong();

    private Structs() {
    }

    /**
     * Регистрирует структуру; структура с тем же именем заменяется.
     *
     * @param type структура
     * @return {@link AutoCloseable}, снимающий регистрацию
     */
    public static synchronized AutoCloseable register(StructType type) {
        StructType previous = BY_NAME.put(type.name(), type);
        if (previous != null && previous.isRecord()) BY_CLASS.remove(previous.valueClass());
        if (type.isRecord()) BY_CLASS.put(type.valueClass(), type);
        VERSION.incrementAndGet();
        return () -> unregister(type.name());
    }

    /**
     * Регистрирует структуру по Java-record.
     *
     * @param type класс record
     * @return структура
     */
    public static StructType registerRecord(Class<?> type) {
        StructType struct = StructType.ofRecord(type);
        register(struct);
        return struct;
    }

    /**
     * Регистрирует структуру из документа.
     *
     * @param source текст {@code <Struct>}
     * @return структура
     */
    public static StructType register(CharSequence source) {
        StructType struct = StructType.parse(source);
        register(struct);
        return struct;
    }

    /**
     * Снимает регистрацию.
     *
     * @param name имя
     * @return {@code true}, если структура была
     */
    public static synchronized boolean unregister(String name) {
        StructType removed = BY_NAME.remove(name);
        if (removed == null) return false;
        if (removed.isRecord()) BY_CLASS.remove(removed.valueClass());
        VERSION.incrementAndGet();
        return true;
    }

    /**
     * @param name имя
     * @return структура или пусто
     */
    public static synchronized Optional<StructType> find(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /**
     * @param valueClass класс record
     * @return структура этого record или пусто
     */
    public static synchronized Optional<StructType> findByClass(Class<?> valueClass) {
        return Optional.ofNullable(BY_CLASS.get(valueClass));
    }

    /**
     * @param name имя
     * @return структура
     * @throws IllegalArgumentException если структуры нет; сообщение перечисляет зарегистрированные
     */
    public static StructType require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException(
                "Unknown struct '" + name + "'. Registered: " + names() + ". Register it before the components that use it."));
    }

    /**
     * @return имена зарегистрированных структур
     */
    public static synchronized List<String> names() {
        return new ArrayList<>(BY_NAME.keySet());
    }

    /**
     * @return счётчик изменений реестра: по нему кэши скомпилированных графов узнают, что структуры сменились
     */
    public static long version() {
        return VERSION.get();
    }
}
