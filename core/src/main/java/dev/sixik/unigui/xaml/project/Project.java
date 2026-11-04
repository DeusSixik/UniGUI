package dev.sixik.unigui.xaml.project;

import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.edit.DocumentSource;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import dev.sixik.unigui.xaml.pack.AssetPack;
import dev.sixik.unigui.xaml.pack.AssetPackWriter;
import dev.sixik.unigui.xaml.pack.PackInstallation;
import dev.sixik.unigui.xaml.pack.PackManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Проект интерфейса: набор документов и ресурсов, из которого собирается пак. Это модель редактора без привязки к
 * экрану: список файлов по видам, создание, переименование и удаление, открытые сессии правок, проверка всего проекта
 * разом, живая установка в рантайм и обмен с диском и паками.
 *
 * <p>Раскладка та же, что у {@link AssetPack}: {@code screens/}, {@code components/}, {@code structs/},
 * {@code textures/}, паспорт {@code pack.xaml}. Проект, открытый из папки, и проект, открытый из пака, неотличимы, а
 * {@link #toPack()} собирает пак тем же детерминированным писателем.</p>
 *
 * <p>Сессии правок кэшируются: панели редактора, открывшие один документ, видят одни и те же правки и одну историю
 * отмены. Правки живут в сессии, пока их не сохранят ({@link #save(Path)}, {@link #toPack()} или
 * {@link #sync()}).</p>
 */
public final class Project {
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

    /** Вид файла проекта; определяется папкой. */
    public enum Kind {
        /** Экран: корень — виджет. */
        SCREEN("screens/"),
        /** Компонент: корень {@code <Component>}. */
        COMPONENT("components/"),
        /** Структура данных: корень {@code <Struct>}. */
        STRUCT("structs/"),
        /** Текстура и другие бинарные ресурсы. */
        TEXTURE("textures/"),
        /** Любой другой файл. */
        OTHER("");

        private final String folder;

        Kind(String folder) {
            this.folder = folder;
        }

        /** @return папка вида с завершающим {@code /} */
        public String folder() {
            return folder;
        }

        /** @return {@code true}, если файлы этого вида — документы XAML */
        public boolean document() {
            return this == SCREEN || this == COMPONENT || this == STRUCT;
        }

        static Kind of(String path) {
            for (Kind kind : values()) {
                if (!kind.folder.isEmpty() && path.startsWith(kind.folder)) return kind;
            }
            return OTHER;
        }
    }

    /**
     * Запись списка файлов.
     *
     * @param path путь внутри проекта
     * @param kind вид
     * @param name имя без папки и расширения
     */
    public record Entry(String path, Kind kind, String name) {
    }

    /**
     * Проблема проекта.
     *
     * @param path    файл
     * @param message описание с позицией, если она известна
     */
    public record Problem(String path, String message) {
    }

    private PackManifest manifest;
    private final Map<String, byte[]> files = new TreeMap<>();
    private final Map<String, EditorSession> sessions = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private PackInstallation installation;
    private boolean structureDirty;

    private Project(PackManifest manifest) {
        this.manifest = manifest;
    }

    // ------------------------------------------------------------------ создание и открытие

    /**
     * Создаёт пустой проект.
     *
     * @param manifest паспорт
     * @return проект
     */
    public static Project create(PackManifest manifest) {
        Project project = new Project(manifest);
        project.structureDirty = true;
        return project;
    }

    /**
     * Открывает проект из пака.
     *
     * @param pack пак
     * @return проект с копией всех файлов
     */
    public static Project of(AssetPack pack) {
        Project project = new Project(pack.manifest());
        for (String name : pack.names()) {
            if (name.equals(PackManifest.FILE)) continue;
            project.files.put(name, pack.bytes(name).orElseThrow());
        }
        return project;
    }

    /**
     * Открывает проект из папки с раскладкой пака.
     *
     * @param folder папка, в корне которой лежит {@code pack.xaml}
     * @return проект
     * @throws IOException если файлы не читаются
     */
    public static Project open(Path folder) throws IOException {
        Path manifestFile = folder.resolve(PackManifest.FILE);
        if (!Files.isRegularFile(manifestFile)) throw new IOException("No " + PackManifest.FILE + " in " + folder);
        Project project = new Project(PackManifest.parse(Files.readString(manifestFile, StandardCharsets.UTF_8)));
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String name = folder.relativize(file).toString().replace('\\', '/');
                if (name.equals(PackManifest.FILE)) continue;
                project.files.put(name, Files.readAllBytes(file));
            }
        }
        return project;
    }

    /**
     * Записывает проект в папку: паспорт, документы (с несохранёнными правками) и ресурсы.
     *
     * @param folder папка; создаётся
     * @throws IOException при ошибке записи
     */
    public void save(Path folder) throws IOException {
        sync();
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(PackManifest.FILE), manifest.toXaml(), StandardCharsets.UTF_8);
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            Path target = folder.resolve(file.getKey());
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue());
        }
        structureDirty = false;
    }

    /**
     * Собирает пак из проекта (с несохранёнными правками документов).
     *
     * @return пак
     */
    public AssetPack toPack() {
        sync();
        AssetPackWriter writer = new AssetPackWriter(manifest);
        files.forEach(writer::add);
        return AssetPack.read(writer.toBytes());
    }

    // ------------------------------------------------------------------ содержимое

    /** @return паспорт проекта */
    public PackManifest manifest() {
        return manifest;
    }

    /**
     * Меняет паспорт.
     *
     * @param next новый паспорт
     */
    public void manifest(PackManifest next) {
        this.manifest = next;
        structureDirty = true;
        changed();
    }

    /** @return все файлы по алфавиту */
    public List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (String path : files.keySet()) entries.add(entry(path));
        return entries;
    }

    /**
     * @param kind вид
     * @return файлы этого вида по алфавиту
     */
    public List<Entry> entries(Kind kind) {
        return entries().stream().filter(entry -> entry.kind() == kind).toList();
    }

    /**
     * @param path путь
     * @return {@code true}, если файл есть
     */
    public boolean has(String path) {
        return files.containsKey(path);
    }

    /**
     * @param path путь файла
     * @return текст документа с учётом несохранённых правок или пусто
     */
    public Optional<String> text(String path) {
        EditorSession session = sessions.get(path);
        if (session != null) return Optional.of(session.text());
        byte[] data = files.get(path);
        return data == null ? Optional.empty() : Optional.of(new String(data, StandardCharsets.UTF_8));
    }

    /**
     * @param path путь файла
     * @return содержимое файла или пусто
     */
    public Optional<byte[]> bytes(String path) {
        byte[] data = files.get(path);
        return data == null ? Optional.empty() : Optional.of(data.clone());
    }

    /**
     * Сессия правок документа. Сессия одна на документ: все панели работают с одной историей.
     *
     * @param path путь документа XAML
     * @return сессия
     * @throws IllegalArgumentException если файла нет или он не документ
     */
    public EditorSession session(String path) {
        EditorSession open = sessions.get(path);
        if (open != null) return open;
        if (!files.containsKey(path) || !entry(path).kind().document()) {
            throw new IllegalArgumentException("'" + path + "' is not a document of this project.");
        }
        DocumentSource source = new DocumentSource() {
            @Override
            public String id() {
                return manifest.id() + "/" + path;
            }

            @Override
            public String title() {
                return path.substring(path.lastIndexOf('/') + 1);
            }

            @Override
            public String readText() {
                byte[] data = files.get(path);
                return data == null ? "" : new String(data, StandardCharsets.UTF_8);
            }

            @Override
            public boolean writable() {
                return true;
            }

            @Override
            public void writeText(String text) {
                if (files.containsKey(path)) files.put(path, text.getBytes(StandardCharsets.UTF_8));
            }
        };
        EditorSession session = EditorSession.open(source);
        sessions.put(path, session);
        return session;
    }

    /**
     * Записывает правки всех открытых документов в проект.
     */
    public void sync() {
        for (EditorSession session : sessions.values()) if (session.dirty()) session.save();
    }

    /** @return {@code true}, если есть несохранённые правки документов или структурные изменения */
    public boolean dirty() {
        if (structureDirty) return true;
        for (EditorSession session : sessions.values()) if (session.dirty()) return true;
        return false;
    }

    // ------------------------------------------------------------------ создание, переименование, удаление

    /**
     * Создаёт экран.
     *
     * @param name имя файла без расширения
     * @return запись
     */
    public Entry createScreen(String name) {
        return create(Kind.SCREEN, name, "<VBox id=\"root\" spacing=\"6\" padding=\"10\">\n</VBox>\n");
    }

    /**
     * Создаёт компонент; тег компонента совпадает с именем.
     *
     * @param name имя в виде идентификатора (оно же имя тега)
     * @return запись
     */
    public Entry createComponent(String name) {
        return create(Kind.COMPONENT, name, "<Component x:Name=\"" + name + "\" xmlns:x=\"" + XamlNamespaces.LANGUAGE
                + "\">\n  <Component.Contract>\n  </Component.Contract>\n  <VBox id=\"root\">\n  </VBox>\n</Component>\n");
    }

    /**
     * Создаёт структуру.
     *
     * @param name имя структуры
     * @return запись
     */
    public Entry createStruct(String name) {
        return create(Kind.STRUCT, name, "<Struct x:Name=\"" + name + "\" xmlns:x=\"" + XamlNamespaces.LANGUAGE
                + "\">\n</Struct>\n");
    }

    /**
     * Добавляет ресурс (текстуру, шрифт…) под путём внутри проекта.
     *
     * @param path путь, например {@code textures/icons/sword.png}
     * @param data содержимое
     * @return запись
     */
    public Entry addResource(String path, byte[] data) {
        String normalized = path.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("..") || normalized.equals(PackManifest.FILE)) {
            throw new IllegalArgumentException("'" + path + "' is not a valid resource path.");
        }
        files.put(normalized, data.clone());
        structureDirty = true;
        changed();
        return entry(normalized);
    }

    private Entry create(Kind kind, String name, String text) {
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("'" + name + "' is not a valid name. Start with a letter; use letters, digits and underscores.");
        }
        String path = kind.folder() + fileName(name, kind);
        if (files.containsKey(path)) throw new IllegalArgumentException("'" + path + "' already exists.");
        files.put(path, text.getBytes(StandardCharsets.UTF_8));
        structureDirty = true;
        changed();
        return entry(path);
    }

    /** Файл компонента и структуры носит имя в нижнем регистре, как принято в паке; экран — как введено. */
    private static String fileName(String name, Kind kind) {
        String base = kind == Kind.SCREEN ? name : name.substring(0, 1).toLowerCase(Locale.ROOT) + name.substring(1);
        return base + ".xaml";
    }

    /**
     * Удаляет файл вместе с открытой сессией.
     *
     * @param path путь
     * @return {@code true}, если файл был
     */
    public boolean delete(String path) {
        if (files.remove(path) == null) return false;
        sessions.remove(path);
        structureDirty = true;
        changed();
        return true;
    }

    /**
     * Переименовывает документ. У экрана меняется только файл. У компонента меняются файл, {@code x:Name} и все
     * использования тега в остальных документах проекта. Структуры переименовывать нельзя: на них ссылаются типы
     * параметров и полей.
     *
     * @param path    путь
     * @param newName новое имя
     * @return пустая строка при успехе или причина отказа
     */
    public String rename(String path, String newName) {
        if (!files.containsKey(path)) return "No file '" + path + "'.";
        Entry entry = entry(path);
        if (!entry.kind().document()) return "Only documents can be renamed.";
        if (entry.kind() == Kind.STRUCT) return "A struct cannot be renamed: field and parameter types refer to it.";
        if (!NAME.matcher(newName).matches()) return "'" + newName + "' is not a valid name.";
        String target = entry.kind().folder() + fileName(newName, entry.kind());
        if (!target.equals(path) && files.containsKey(target)) return "'" + target + "' already exists.";

        String oldTag = null;
        if (entry.kind() == Kind.COMPONENT) {
            sync();
            oldTag = tagOf(path);
            if (oldTag == null) return "The component document cannot be read.";
            if (!oldTag.equals(newName) && isTagUsedOrDeclared(newName)) return "The tag '" + newName + "' is already in use.";
        }

        sync();
        byte[] data = files.remove(path);
        sessions.remove(path);
        files.put(target, data);
        if (entry.kind() == Kind.COMPONENT && !oldTag.equals(newName)) retag(target, oldTag, newName);
        structureDirty = true;
        changed();
        return "";
    }

    // ------------------------------------------------------------------ теги компонентов

    private String tagOf(String path) {
        try {
            UiDocument document = XamlFormat.INSTANCE.read(text(path).orElse(""));
            for (var property : document.root().properties()) {
                if (property.name().local().equals("Name") && property.owner() == null
                        && property.value() instanceof dev.sixik.unigui.xaml.UiValue.Literal literal) {
                    return literal.text().trim();
                }
            }
            return null;
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private boolean isTagUsedOrDeclared(String tag) {
        for (Entry entry : entries(Kind.COMPONENT)) if (tag.equals(tagOf(entry.path()))) return true;
        return false;
    }

    /** Меняет {@code x:Name} компонента и тег во всех документах проекта. */
    private void retag(String componentPath, String oldTag, String newTag) {
        for (Entry entry : entries()) {
            if (!entry.kind().document() || entry.kind() == Kind.STRUCT) continue;
            String text = text(entry.path()).orElse("");
            UiDocument document;
            try {
                document = XamlFormat.INSTANCE.read(text);
            } catch (RuntimeException failure) {
                continue; // документ с синтаксической ошибкой остаётся как есть
            }
            boolean[] changed = {false};
            if (entry.path().equals(componentPath)) {
                for (var property : document.root().properties()) {
                    if (property.name().local().equals("Name") && property.owner() == null
                            && property.value() instanceof dev.sixik.unigui.xaml.UiValue.Literal) {
                        document.root().set(new dev.sixik.unigui.xaml.UiProperty(property.name(), property.owner(),
                                new dev.sixik.unigui.xaml.UiValue.Literal(newTag), property.form(), property.location()));
                        changed[0] = true;
                        break;
                    }
                }
            }
            visit(document.root(), oldTag, newTag, changed);
            if (changed[0]) files.put(entry.path(), XamlFormat.INSTANCE.write(document).getBytes(StandardCharsets.UTF_8));
            sessions.remove(entry.path());
        }
    }

    private static void visit(UiNode node, String oldTag, String newTag, boolean[] changed) {
        if (node.type().local().equals(oldTag) && !node.type().local().equals("Component")) {
            node.type(UiName.of(newTag));
            changed[0] = true;
        }
        for (var property : node.properties()) {
            if (property.value() instanceof dev.sixik.unigui.xaml.UiValue.Elements elements) {
                for (var item : elements.content()) if (item instanceof UiNode nested) visit(nested, oldTag, newTag, changed);
            }
        }
        for (UiNode child : node.children()) visit(child, oldTag, newTag, changed);
    }

    // ------------------------------------------------------------------ проверка и рантайм

    /**
     * Проверяет все документы проекта. Сначала проект ставится в рантайм, чтобы компоненты и структуры видели друг
     * друга, затем каждый документ проходит {@link DocumentValidator}.
     *
     * @return найденные проблемы; пустой список, если проект чист
     */
    public List<Problem> check() {
        sync();
        List<Problem> problems = new ArrayList<>();
        PackInstallation installed = toPack().install();
        try {
            for (String problem : installed.problems()) {
                int colon = problem.indexOf(": ");
                problems.add(colon < 0 ? new Problem("", problem)
                        : new Problem(problem.substring(0, colon), problem.substring(colon + 2)));
            }
            DocumentValidator validator = new DocumentValidator(Xaml.defaultSchema());
            for (Entry entry : entries()) {
                if (entry.kind() != Kind.SCREEN && entry.kind() != Kind.COMPONENT) continue;
                var result = validator.validate(text(entry.path()).orElse(""));
                for (var diagnostic : result.diagnostics()) {
                    problems.add(new Problem(entry.path(), diagnostic.toString()));
                }
            }
        } finally {
            installed.close();
        }
        return problems;
    }

    /**
     * Ставит проект в рантайм: структуры и компоненты становятся доступны по именам, документы могут создавать
     * экземпляры компонентов проекта. Прежняя установка этого проекта снимается.
     *
     * @return установка; закрывать не обязательно, её заменит следующий вызов
     */
    public PackInstallation install() {
        if (installation != null) installation.close();
        installation = toPack().install();
        return installation;
    }

    /** Снимает установку проекта, если она есть. */
    public void uninstall() {
        if (installation != null) installation.close();
        installation = null;
    }

    // ------------------------------------------------------------------ события

    /**
     * Подписывается на структурные изменения: создание, переименование, удаление, смена паспорта.
     *
     * @param listener вызывается после каждого такого изменения
     */
    public void onChanged(Runnable listener) {
        listeners.add(listener);
    }

    private void changed() {
        for (Runnable listener : List.copyOf(listeners)) listener.run();
    }

    private static Entry entry(String path) {
        Kind kind = Kind.of(path);
        String file = path.substring(path.lastIndexOf('/') + 1);
        int dot = file.lastIndexOf('.');
        return new Entry(path, kind, dot < 0 ? file : file.substring(0, dot));
    }
}
