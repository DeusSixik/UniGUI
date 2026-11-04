package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.load.XamlLoader;
import dev.sixik.unigui.xaml.schema.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Короткий вход для загрузки XAML-документов в дерево виджетов.
 *
 * <pre>{@code
 * VBox root = Xaml.load("""
 *         <VBox spacing="4">
 *             <Button x:Name="save" text="Save" xmlns:x="https://unigui.dev/xaml"/>
 *         </VBox>
 *         """, VBox.class);
 * Button save = Xaml.get(root, "save", Button.class);
 * }</pre>
 *
 * <p>Для нестандартного набора типов передайте свой {@link Schema}; для тонкой настройки — {@link LoadOptions}.</p>
 */
public final class Xaml {
    private static volatile Schema defaultSchema;

    private Xaml() {
    }

    /**
     * Возвращает общий реестр встроенных типов. Типы в нём строятся лениво и потокобезопасно.
     *
     * @return общий реестр; не изменяйте его, создавайте свой через {@link Schema#builtIns()}
     */
    public static Schema defaultSchema() {
        Schema schema = defaultSchema;
        if (schema != null) return schema;
        synchronized (Xaml.class) {
            if (defaultSchema == null) defaultSchema = Schema.builtIns();
            return defaultSchema;
        }
    }

    // ------------------------------------------------------------------ загрузка

    /**
     * Загружает корневой виджет.
     *
     * @param source текст документа
     * @return корень
     */
    public static Widget load(String source) {
        return load(source, defaultSchema(), LoadOptions.DEFAULT);
    }

    /**
     * Загружает корневой виджет с настройками.
     *
     * @param source  текст документа
     * @param options настройки
     * @return корень
     */
    public static Widget load(String source, LoadOptions options) {
        return load(source, defaultSchema(), options);
    }

    /**
     * Загружает корневой виджет и проверяет его тип.
     *
     * @param source текст документа
     * @param type   ожидаемый тип корня
     * @param <T>    тип корня
     * @return корень
     */
    public static <T extends Widget> T load(String source, Class<T> type) {
        return load(source, type, defaultSchema(), LoadOptions.DEFAULT);
    }

    /**
     * Загружает корневой виджет с настройками и проверяет его тип.
     *
     * @param source  текст документа
     * @param type    ожидаемый тип корня
     * @param options настройки
     * @param <T>     тип корня
     * @return корень
     */
    public static <T extends Widget> T load(String source, Class<T> type, LoadOptions options) {
        return load(source, type, defaultSchema(), options);
    }

    /**
     * Базовая перегрузка: свой реестр и свои настройки.
     *
     * @param source  текст документа
     * @param schema  реестр типов
     * @param options настройки
     * @return корень
     */
    public static Widget load(String source, Schema schema, LoadOptions options) {
        return new XamlLoader(schema, options).load(source);
    }

    /**
     * Загружает корневой виджет заданного типа с собственным реестром.
     *
     * @param source  текст документа
     * @param type    ожидаемый тип корня
     * @param schema  реестр типов
     * @param options настройки
     * @param <T>     тип корня
     * @return корень
     */
    public static <T extends Widget> T load(String source, Class<T> type, Schema schema, LoadOptions options) {
        return castRoot(load(source, schema, options), type);
    }

    /**
     * Загружает экран: корень плюс настройки масштаба и сессию привязок.
     *
     * @param source текст документа
     * @return экран; закройте его, когда он больше не нужен
     */
    public static LoadedScreen<Widget> loadScreen(String source) {
        return loadScreen(source, defaultSchema(), LoadOptions.DEFAULT);
    }

    /**
     * Загружает экран с настройками.
     *
     * @param source  текст документа
     * @param options настройки
     * @return экран
     */
    public static LoadedScreen<Widget> loadScreen(String source, LoadOptions options) {
        return loadScreen(source, defaultSchema(), options);
    }

    /**
     * Загружает экран с собственным реестром.
     *
     * @param source  текст документа
     * @param schema  реестр типов
     * @param options настройки
     * @return экран
     */
    public static LoadedScreen<Widget> loadScreen(String source, Schema schema, LoadOptions options) {
        return new XamlLoader(schema, options).loadScreen(source);
    }

    /**
     * Загружает экран и проверяет тип корня.
     *
     * @param source  текст документа
     * @param type    ожидаемый тип корня
     * @param options настройки
     * @param <T>     тип корня
     * @return экран с типизированным корнем
     */
    public static <T extends Widget> LoadedScreen<T> loadScreen(String source, Class<T> type, LoadOptions options) {
        LoadedScreen<Widget> screen = loadScreen(source, options);
        try {
            return new LoadedScreen<>(castRoot(screen.root(), type), screen.scaleProvider(),
                    screen.scaleWithMinecraftGui(), screen.bindingSession());
        } catch (RuntimeException failure) {
            screen.close();
            throw failure;
        }
    }

    /**
     * Загружает экран и проверяет тип корня с настройками по умолчанию.
     *
     * @param source текст документа
     * @param type   ожидаемый тип корня
     * @param <T>    тип корня
     * @return экран с типизированным корнем
     */
    public static <T extends Widget> LoadedScreen<T> loadScreen(String source, Class<T> type) {
        return loadScreen(source, type, LoadOptions.DEFAULT);
    }

    /**
     * Загружает документ из ресурса classpath (UTF-8).
     *
     * @param path путь ресурса, с ведущим {@code /} или без него
     * @return корень
     */
    public static Widget loadResource(String path) {
        return load(readResource(path));
    }

    /**
     * Загружает документ из ресурса classpath и проверяет тип корня.
     *
     * @param path путь ресурса
     * @param type ожидаемый тип корня
     * @param <T>  тип корня
     * @return корень
     */
    public static <T extends Widget> T loadResource(String path, Class<T> type) {
        return castRoot(loadResource(path), type);
    }

    /**
     * Читает ресурс classpath как UTF-8 текст.
     *
     * @param path путь ресурса
     * @return содержимое
     */
    public static String readResource(String path) {
        if (path == null || path.isBlank()) {
            throw new XamlLoadException("XAML resource path must not be blank.");
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = Xaml.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream(normalized);
        if (stream == null && loader != Xaml.class.getClassLoader()) {
            stream = Xaml.class.getClassLoader().getResourceAsStream(normalized);
        }
        if (stream == null) throw new XamlLoadException("XAML resource '" + path + "' was not found.");
        try (InputStream in = stream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XamlLoadException("Cannot read XAML resource '" + path + "'.", SourceLocation.UNKNOWN, failure);
        }
    }

    // ------------------------------------------------------------------ поиск по id

    /**
     * Ищет виджет по {@code id} (или {@code x:Name}) в поддереве.
     *
     * @param root корень поиска
     * @param id   идентификатор
     * @return виджет или пусто
     */
    public static Optional<Widget> find(Widget root, String id) {
        if (root == null || id == null || id.isBlank()) return Optional.empty();
        ArrayDeque<Widget> stack = new ArrayDeque<>();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        stack.push(root);
        while (!stack.isEmpty()) {
            Widget widget = stack.pop();
            if (widget == null || !visited.add(widget)) continue;
            if (id.equals(widget.id())) return Optional.of(widget);
            List<Widget> children = new ArrayList<>(widget.children());
            if (widget instanceof ScrollView scrollView && scrollView.content() != null) {
                children.add(scrollView.content());
            }
            for (int i = children.size() - 1; i >= 0; i--) stack.push(children.get(i));
        }
        return Optional.empty();
    }

    /**
     * Ищет виджет по id и приводит к ожидаемому типу.
     *
     * @param root корень поиска
     * @param id   идентификатор
     * @param type ожидаемый тип
     * @param <T>  тип виджета
     * @return виджет или пусто, если id не найден
     * @throws XamlLoadException если виджет найден, но имеет другой тип
     */
    public static <T extends Widget> Optional<T> find(Widget root, String id, Class<T> type) {
        return find(root, id).map(widget -> {
            if (type.isInstance(widget)) return type.cast(widget);
            throw new XamlLoadException("Widget id '" + id + "' exists, but is "
                    + widget.getClass().getSimpleName() + ", not " + type.getSimpleName() + ".");
        });
    }

    /**
     * Находит виджет по id; отсутствие считается ошибкой.
     *
     * @param root корень поиска
     * @param id   идентификатор
     * @return виджет
     * @throws XamlLoadException если виджета нет
     */
    public static Widget get(Widget root, String id) {
        return find(root, id).orElseThrow(() -> new XamlLoadException(
                "Widget id '" + id + "' was not found under root '" + label(root) + "'."));
    }

    /**
     * Находит виджет по id и приводит к типу; отсутствие считается ошибкой.
     *
     * @param root корень поиска
     * @param id   идентификатор
     * @param type ожидаемый тип
     * @param <T>  тип виджета
     * @return виджет
     * @throws XamlLoadException если виджета нет или тип не совпал
     */
    public static <T extends Widget> T get(Widget root, String id, Class<T> type) {
        return find(root, id, type).orElseThrow(() -> new XamlLoadException(
                "Widget id '" + id + "' was not found under root '" + label(root) + "'."));
    }

    private static <T extends Widget> T castRoot(Widget root, Class<T> type) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (type.isInstance(root)) return type.cast(root);
        throw new XamlLoadException("XAML root is " + root.getClass().getSimpleName()
                + ", expected " + type.getSimpleName() + ".");
    }

    private static String label(Widget root) {
        if (root == null) return "null";
        String id = root.id();
        return id == null || id.isBlank() ? root.getClass().getSimpleName() : id;
    }
}
