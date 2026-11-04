package dev.sixik.unigui.xaml.schema;

import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Глобальный список расширений, применяемых к общему реестру {@code Xaml.defaultSchema()}.
 *
 * <p>Расширения хранятся на уровне процесса. Каждая регистрация возвращает {@link AutoCloseable}: закрытие
 * убирает расширение (нужно при выгрузке мода и в тестах). Любое изменение списка увеличивает {@link #version()},
 * по которому общий реестр понимает, что его пора пересобрать.</p>
 *
 * <p>Расширения, найденные через ServiceLoader, но не загрузившиеся (нет класса, ошибка конструктора), не роняют
 * приложение: они пропускаются, а причина доступна в {@link #loadFailures()}.</p>
 */
public final class SchemaContributions {
    private static final CopyOnWriteArrayList<SchemaContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();
    private static final List<String> LOAD_FAILURES = new CopyOnWriteArrayList<>();
    private static final AtomicLong VERSION = new AtomicLong();

    static {
        var iterator = ServiceLoader.load(SchemaContributor.class).iterator();
        // Битый провайдер пропускается; предел ошибок защищает от реализации, которая не продвигается после сбоя.
        int failures = 0;
        while (failures < 256) {
            try {
                if (!iterator.hasNext()) break;
                CONTRIBUTORS.add(iterator.next());
            } catch (ServiceConfigurationError failure) {
                LOAD_FAILURES.add(failure.getMessage());
                failures++;
            }
        }
    }

    private SchemaContributions() {
    }

    /**
     * Регистрирует расширение.
     *
     * @param contributor расширение
     * @return закрывается, чтобы убрать расширение
     */
    public static AutoCloseable register(SchemaContributor contributor) {
        if (contributor == null) throw new IllegalArgumentException("contributor must not be null");
        CONTRIBUTORS.add(contributor);
        VERSION.incrementAndGet();
        return () -> {
            if (CONTRIBUTORS.remove(contributor)) VERSION.incrementAndGet();
        };
    }

    /**
     * Регистрирует аннотированный виджет.
     *
     * @param type класс с {@code @XmlWidgetName}
     * @param <T>  тип виджета
     * @return закрывается, чтобы убрать расширение
     */
    public static <T extends Widget> AutoCloseable registerAnnotated(Class<T> type) {
        return register(schema -> schema.defineAnnotated(type));
    }

    /**
     * Регистрирует аннотированный виджет с явной фабрикой.
     *
     * @param type    класс с {@code @XmlWidgetName}
     * @param factory фабрика
     * @param <T>     тип виджета
     * @return закрывается, чтобы убрать расширение
     */
    public static <T extends Widget> AutoCloseable registerAnnotated(Class<T> type, Supplier<T> factory) {
        return register(schema -> schema.defineAnnotated(type, factory));
    }

    /**
     * Применяет все расширения к реестру.
     *
     * @param schema реестр
     */
    public static void applyTo(Schema schema) {
        for (SchemaContributor contributor : CONTRIBUTORS) contributor.contribute(schema);
    }

    /**
     * Версия списка: растёт при каждой регистрации и закрытии.
     *
     * @return монотонно возрастающее число
     */
    public static long version() {
        return VERSION.get();
    }

    /** @return копия текущего списка расширений */
    public static List<SchemaContributor> contributors() {
        return List.copyOf(CONTRIBUTORS);
    }

    /** @return сообщения об ошибках загрузки расширений через ServiceLoader */
    public static List<String> loadFailures() {
        return new ArrayList<>(LOAD_FAILURES);
    }
}
