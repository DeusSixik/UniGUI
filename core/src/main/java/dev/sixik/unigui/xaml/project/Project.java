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
    private String lastSignature = "";
    private final java.util.ArrayDeque<Snapshot> undoStack = new java.util.ArrayDeque<>();
    private final java.util.ArrayDeque<Snapshot> redoStack = new java.util.ArrayDeque<>();

    /** Глубина истории структурных правок проекта. */
    public static final int HISTORY_LIMIT = 50;

    /** Состояние проекта до структурной правки: паспорт и файлы (массивы не меняются на месте, поэтому копируются ссылки). */
    private record Snapshot(String label, PackManifest manifest, Map<String, byte[]> files) {
    }

    private Project(PackManifest manifest) {
        this.manifest = manifest;
    }

    // ------------------------------------------------------------------ отмена структурных правок

    /**
     * Запоминает состояние перед структурной правкой. Правки внутри документов в историю проекта не входят: у каждого
     * документа своя история в его сессии.
     */
    private void record(String label) {
        sync();
        undoStack.push(new Snapshot(label, manifest, new TreeMap<>(files)));
        while (undoStack.size() > HISTORY_LIMIT) undoStack.removeLast();
        redoStack.clear();
    }

    /** @return {@code true}, если есть структурная правка, которую можно отменить */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** @return {@code true}, если есть отменённая правка, которую можно вернуть */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** @return описание правки, которую отменит {@link #undo()} */
    public Optional<String> undoLabel() {
        return Optional.ofNullable(undoStack.peek()).map(Snapshot::label);
    }

    /** @return описание правки, которую вернёт {@link #redo()} */
    public Optional<String> redoLabel() {
        return Optional.ofNullable(redoStack.peek()).map(Snapshot::label);
    }

    /**
     * Отменяет последнюю структурную правку: создание, удаление и переименование файлов, добавление ресурса, смену
     * паспорта. Проект возвращается к состоянию до неё целиком; правки документов, сделанные после неё и не
     * сохранённые, попадают в состояние для {@link #redo()}, но в откатываемом состоянии их нет. Открытые сессии
     * затронутых файлов закрываются: их надо открыть заново.
     *
     * @return {@code true}, если было что отменять
     */
    public boolean undo() {
        Snapshot previous = undoStack.peek();
        if (previous == null) return false;
        sync();
        undoStack.pop();
        redoStack.push(new Snapshot(previous.label(), manifest, new TreeMap<>(files)));
        restore(previous);
        return true;
    }

    /**
     * Возвращает последнюю отменённую структурную правку.
     *
     * @return {@code true}, если было что возвращать
     */
    public boolean redo() {
        Snapshot next = redoStack.peek();
        if (next == null) return false;
        sync();
        redoStack.pop();
        undoStack.push(new Snapshot(next.label(), manifest, new TreeMap<>(files)));
        restore(next);
        return true;
    }

    private void restore(Snapshot snapshot) {
        manifest = snapshot.manifest();
        files.clear();
        files.putAll(snapshot.files());
        // Сессии файлов, чьё содержимое изменилось или которых больше нет, устарели.
        sessions.entrySet().removeIf(entry -> {
            byte[] now = files.get(entry.getKey());
            return now == null || !java.util.Arrays.equals(now, entry.getValue().text().getBytes(StandardCharsets.UTF_8));
        });
        structureDirty = true;
        changed();
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
        lastSignature = signatureOf(folder);
    }

    /**
     * Собирает пак из проекта (с несохранёнными правками документов).
     *
     * @return пак
     */
    public AssetPack toPack() {
        return AssetPack.read(toArchive());
    }

    /**
     * Собирает архив пака (с несохранёнными правками документов) в виде байтов, готовых к записи в файл.
     *
     * @return содержимое zip; один и тот же проект всегда даёт одни и те же байты
     */
    public byte[] toArchive() {
        sync();
        AssetPackWriter writer = new AssetPackWriter(manifest);
        files.forEach(writer::add);
        return writer.toBytes();
    }

    /**
     * @param path путь документа
     * @return {@code true}, если у открытого документа есть несохранённые правки
     */
    public boolean dirty(String path) {
        EditorSession session = sessions.get(path);
        return session != null && session.dirty();
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
        record("Change manifest");
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

    // ------------------------------------------------------------------ обновление с диска

    /**
     * Итог обновления с диска.
     *
     * @param added     файлы, которых в проекте не было
     * @param changed   файлы, содержимое которых заменено
     * @param removed   файлы, которых на диске больше нет
     * @param conflicts документы с несохранёнными правками, которые на диске тоже изменились: они не тронуты
     */
    public record Refresh(List<String> added, List<String> changed, List<String> removed, List<String> conflicts) {
        /** @return {@code true}, если проект изменился */
        public boolean any() {
            return !added.isEmpty() || !changed.isEmpty() || !removed.isEmpty();
        }
    }

    /**
     * Подтягивает изменения из папки проекта: так проект перечитывается, когда файлы правит внешний редактор или
     * игра запущена с живой папкой. Документ с несохранёнными правками не перезаписывается, а попадает в
     * {@link Refresh#conflicts()}. Если проект установлен в рантайм, установка обновляется.
     *
     * @param folder папка с раскладкой пака
     * @return что изменилось
     * @throws IOException если папка не читается
     */
    public Refresh refreshFrom(Path folder) throws IOException {
        List<String> added = new ArrayList<>();
        List<String> changedFiles = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        Path manifestFile = folder.resolve(PackManifest.FILE);
        if (Files.isRegularFile(manifestFile)) {
            PackManifest fresh = PackManifest.parse(Files.readString(manifestFile, StandardCharsets.UTF_8));
            if (!fresh.equals(manifest)) {
                manifest = fresh;
                changedFiles.add(PackManifest.FILE);
            }
        }
        java.util.Set<String> onDisk = new java.util.HashSet<>();
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                String name = folder.relativize(file).toString().replace('\\', '/');
                if (name.equals(PackManifest.FILE)) continue;
                onDisk.add(name);
                byte[] data = Files.readAllBytes(file);
                byte[] known = files.get(name);
                if (known == null) {
                    files.put(name, data);
                    added.add(name);
                } else if (!java.util.Arrays.equals(known, data)) {
                    EditorSession open = sessions.get(name);
                    if (open != null && open.dirty()) {
                        conflicts.add(name);
                    } else {
                        files.put(name, data);
                        sessions.remove(name);
                        changedFiles.add(name);
                    }
                }
            }
        }
        for (String name : new ArrayList<>(files.keySet())) {
            if (onDisk.contains(name)) continue;
            EditorSession open = sessions.get(name);
            if (open != null && open.dirty()) {
                conflicts.add(name);
                continue;
            }
            files.remove(name);
            sessions.remove(name);
            removed.add(name);
        }
        Refresh result = new Refresh(added, changedFiles, removed, conflicts);
        if (result.any()) {
            if (installation != null) install();
            changed();
        }
        return result;
    }

    /**
     * Дешёвая проверка папки на изменения: сравнивает имена, размеры и время изменения файлов, содержимое не читает, и
     * только при отличии вызывает {@link #refreshFrom}. Подходит для вызова раз в секунду из тика игры.
     *
     * @param folder папка проекта
     * @return что изменилось или пусто, если папка не менялась с прошлого вызова
     * @throws IOException если папка не читается
     */
    public Optional<Refresh> poll(Path folder) throws IOException {
        String now = signatureOf(folder);
        if (now.equals(lastSignature)) return Optional.empty();
        Refresh refresh = refreshFrom(folder);
        lastSignature = now;
        return Optional.of(refresh);
    }

    private static String signatureOf(Path folder) throws IOException {
        StringBuilder text = new StringBuilder();
        try (Stream<Path> walk = Files.walk(folder)) {
            for (Path file : (Iterable<Path>) walk.filter(Files::isRegularFile)
                    .sorted(java.util.Comparator.comparing(Path::toString))::iterator) {
                text.append(folder.relativize(file)).append('|').append(Files.size(file)).append('|')
                        .append(Files.getLastModifiedTime(file).toMillis()).append(';');
            }
        }
        return text.toString();
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
        record("Add " + normalized);
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
        record("Create " + path);
        files.put(path, text.getBytes(StandardCharsets.UTF_8));
        structureDirty = true;
        changed();
        return entry(path);
    }

    /**
     * Путь файла документа по виду и имени.
     *
     * @param kind вид документа
     * @param name имя
     * @return путь, под которым проект хранит такой документ
     */
    public static String pathOf(Kind kind, String name) {
        return kind.folder() + fileName(name, kind);
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
        if (!files.containsKey(path)) return false;
        record("Delete " + path);
        files.remove(path);
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
        if (entry.kind() == Kind.STRUCT) return renameStruct(path, newName);
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

        record("Rename " + path + " to " + target);
        byte[] data = files.remove(path);
        sessions.remove(path);
        files.put(target, data);
        if (entry.kind() == Kind.COMPONENT && !oldTag.equals(newName)) retag(target, oldTag, newName);
        structureDirty = true;
        changed();
        return "";
    }

    // ------------------------------------------------------------------ ссылки

    /**
     * Место, где файл используется другим документом.
     *
     * @param path  документ, который ссылается
     * @param where что именно ссылается: тег, тип поля, нода графа, значение свойства
     */
    public record Usage(String path, String where) {
    }

    /**
     * Находит все ссылки на файл из других документов: теги компонента, типы {@code struct:Имя} и ноды
     * {@code Struct} структуры, идентификаторы ресурсов в значениях свойств.
     *
     * @param path путь файла
     * @return ссылки; пустой список, если файл никому не нужен
     */
    public List<Usage> usages(String path) {
        if (!files.containsKey(path)) return List.of();
        Entry target = entry(path);
        sync();
        List<Usage> found = new ArrayList<>();
        String key = switch (target.kind()) {
            case COMPONENT -> tagOf(path);
            case STRUCT -> structName(path);
            case TEXTURE, OTHER -> resourceId(path);
            default -> null;
        };
        if (key == null) return List.of();
        for (Entry entry : entries()) {
            if (!entry.kind().document() || entry.path().equals(path)) continue;
            UiDocument document;
            try {
                document = XamlFormat.INSTANCE.read(text(entry.path()).orElse(""));
            } catch (RuntimeException failure) {
                continue;
            }
            scan(document.root(), target.kind(), key, entry.path(), found);
        }
        return found;
    }

    private static void scan(UiNode node, Kind kind, String key, String path, List<Usage> found) {
        String label = node.type().local() + node.attributeText("id").map(id -> " #" + id)
                .or(() -> node.attributeText("Id").map(id -> " #" + id)).orElse("");
        if (kind == Kind.COMPONENT && node.type().local().equals(key)) found.add(new Usage(path, "<" + label.trim() + ">"));
        for (var property : node.properties()) {
            if (property.value() instanceof dev.sixik.unigui.xaml.UiValue.Literal literal && mentions(kind, key, literal.text())) {
                if (kind != Kind.COMPONENT) found.add(new Usage(path, label + "." + property.name().local()));
            } else if (property.value() instanceof dev.sixik.unigui.xaml.UiValue.Elements elements) {
                for (var item : elements.content()) if (item instanceof UiNode nested) scan(nested, kind, key, path, found);
            }
        }
        for (UiNode child : node.children()) scan(child, kind, key, path, found);
    }

    private static boolean mentions(Kind kind, String key, String text) {
        if (kind == Kind.STRUCT) {
            return text.matches("(?s).*(^|[\\s,:])struct:" + Pattern.quote(key) + "($|[\\s,]).*") || text.equals(key);
        }
        if (kind == Kind.TEXTURE || kind == Kind.OTHER) {
            return text.equals(key) || text.matches("(?s).*[\\s,(\"']" + Pattern.quote(key) + "($|[\\s,)\"']).*");
        }
        return false;
    }

    private static String resourceId(String path) {
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private String structName(String path) {
        try {
            for (var property : XamlFormat.INSTANCE.read(text(path).orElse("")).root().properties()) {
                if (property.name().local().equals("Name") && property.owner() == null
                        && property.value() instanceof dev.sixik.unigui.xaml.UiValue.Literal literal) {
                    return literal.text().trim();
                }
            }
        } catch (RuntimeException failure) {
            return null;
        }
        return null;
    }

    /**
     * Удаляет файл, только если на него никто не ссылается.
     *
     * @param path путь файла
     * @return пустая строка при успехе или причина отказа со списком ссылок
     */
    public String deleteIfUnused(String path) {
        if (!files.containsKey(path)) return "No file '" + path + "'.";
        List<Usage> users = usages(path);
        if (!users.isEmpty()) {
            StringBuilder reason = new StringBuilder("'" + path + "' is used by ");
            for (int i = 0; i < Math.min(5, users.size()); i++) {
                if (i > 0) reason.append(", ");
                reason.append(users.get(i).path()).append(' ').append(users.get(i).where());
            }
            if (users.size() > 5) reason.append(" and ").append(users.size() - 5).append(" more");
            return reason.append('.').toString();
        }
        delete(path);
        return "";
    }

    /** Переименовывает структуру: файл, {@code x:Name}, типы {@code struct:Имя} и атрибуты {@code Struct} нод. */
    private String renameStruct(String path, String newName) {
        sync();
        String oldName = structName(path);
        if (oldName == null) return "The struct document cannot be read.";
        if (oldName.equals(newName)) return "";
        for (Entry entry : entries(Kind.STRUCT)) {
            if (!entry.path().equals(path) && newName.equals(structName(entry.path()))) {
                return "The struct '" + newName + "' already exists.";
            }
        }
        String target = Kind.STRUCT.folder() + fileName(newName, Kind.STRUCT);
        if (!target.equals(path) && files.containsKey(target)) return "'" + target + "' already exists.";

        record("Rename struct " + oldName + " to " + newName);
        for (Entry entry : entries()) {
            if (!entry.kind().document()) continue;
            UiDocument document;
            try {
                document = XamlFormat.INSTANCE.read(text(entry.path()).orElse(""));
            } catch (RuntimeException failure) {
                continue;
            }
            boolean[] changed = {false};
            boolean self = entry.path().equals(path);
            retype(document.root(), oldName, newName, self, changed);
            if (changed[0]) files.put(entry.path(), XamlFormat.INSTANCE.write(document).getBytes(StandardCharsets.UTF_8));
            sessions.remove(entry.path());
        }
        byte[] data = files.remove(path);
        sessions.remove(path);
        files.put(target, data);
        structureDirty = true;
        changed();
        return "";
    }

    /**
     * Переименовывает поле структуры и обновляет граф каждого компонента проекта: ноды {@code Struct.Make} и
     * {@code Struct.Break} этой структуры (провода и значения по умолчанию на пинах поля) и {@code Struct.With}
     * (атрибут {@code Field}).
     *
     * @param path     путь документа структуры
     * @param oldField прежнее имя поля
     * @param newField новое имя поля
     * @return пустая строка при успехе или причина отказа
     */
    public String renameStructField(String path, String oldField, String newField) {
        if (!files.containsKey(path) || entry(path).kind() != Kind.STRUCT) return "'" + path + "' is not a struct document.";
        String struct = structName(path);
        if (struct == null) return "The struct document cannot be read.";
        record("Rename field " + oldField + " to " + newField);
        EditorSession session = session(path);
        if (!dev.sixik.unigui.xaml.struct.StructEdits.renameField(session, oldField, newField)) {
            undoStack.pop(); // правка не состоялась: запись в истории не нужна
            var found = session.diagnostics(EditorSession.Channel.EDIT);
            return found.isEmpty() ? "The field cannot be renamed." : found.get(0).message();
        }
        session.save();
        for (Entry entry : entries(Kind.COMPONENT)) {
            UiDocument document;
            try {
                document = XamlFormat.INSTANCE.read(text(entry.path()).orElse(""));
            } catch (RuntimeException failure) {
                continue;
            }
            boolean[] changed = {false};
            for (var property : document.root().properties()) {
                if (property.value() instanceof dev.sixik.unigui.xaml.UiValue.Elements elements
                        && property.name().local().equals("Graph")) {
                    renameFieldInGraph(elements.content(), struct, oldField, newField, changed);
                }
            }
            if (changed[0]) {
                files.put(entry.path(), XamlFormat.INSTANCE.write(document).getBytes(StandardCharsets.UTF_8));
                sessions.remove(entry.path());
            }
        }
        structureDirty = true;
        changed();
        return "";
    }

    private static void renameFieldInGraph(List<dev.sixik.unigui.xaml.UiContent> items, String struct, String oldField,
                                           String newField, boolean[] changed) {
        java.util.Map<String, String> byId = new java.util.HashMap<>(); // нода структуры -> тип
        for (var item : items) {
            if (item instanceof UiNode node && node.type().local().equals("Node")
                    && struct.equals(node.attributeText("Struct", ""))) {
                byId.put(node.attributeText("Id", ""), node.attributeText("Type", ""));
            }
        }
        for (var item : items) {
            if (!(item instanceof UiNode node)) continue;
            if (node.type().local().equals("Node") && byId.containsKey(node.attributeText("Id", ""))) {
                String type = node.attributeText("Type", "");
                if (type.equals("Struct.With") && oldField.equals(node.attributeText("Field", ""))) {
                    node.set("Field", newField);
                    changed[0] = true;
                } else if (type.equals("Struct.Make") && node.attributeText(oldField).isPresent()) {
                    String value = node.attributeText(oldField).orElse("");
                    node.remove(null, UiName.of(oldField));
                    node.set(newField, value);
                    changed[0] = true;
                }
            } else if (node.type().local().equals("Wire")) {
                for (String end : new String[]{"From", "To"}) {
                    String value = node.attributeText(end, "");
                    int dot = value.indexOf('.');
                    if (dot <= 0 || !byId.containsKey(value.substring(0, dot))) continue;
                    String type = byId.get(value.substring(0, dot));
                    boolean makeInput = end.equals("To") && type.equals("Struct.Make");
                    boolean breakOutput = end.equals("From") && type.equals("Struct.Break");
                    if ((makeInput || breakOutput) && value.substring(dot + 1).equals(oldField)) {
                        node.set(end, value.substring(0, dot + 1) + newField);
                        changed[0] = true;
                    }
                }
            }
        }
    }

    private static void retype(UiNode node, String oldName, String newName, boolean declaration, boolean[] changed) {
        for (var property : List.copyOf(node.properties())) {
            if (property.value() instanceof dev.sixik.unigui.xaml.UiValue.Literal literal) {
                String text = literal.text();
                String next = text.replaceAll("(^|[\\s,:])struct:" + Pattern.quote(oldName) + "(?=$|[\\s,])",
                        "$1struct:" + newName);
                boolean isDeclaration = declaration && property.name().local().equals("Name") && property.owner() == null
                        && node.type().local().equals("Struct") && text.trim().equals(oldName);
                boolean isNodeRef = property.name().local().equals("Struct") && text.trim().equals(oldName);
                if (isDeclaration || isNodeRef) next = newName;
                if (!next.equals(text)) {
                    node.set(new dev.sixik.unigui.xaml.UiProperty(property.name(), property.owner(),
                            new dev.sixik.unigui.xaml.UiValue.Literal(next), property.form(), property.location()));
                    changed[0] = true;
                }
            } else if (property.value() instanceof dev.sixik.unigui.xaml.UiValue.Elements elements) {
                for (var item : elements.content()) {
                    if (item instanceof UiNode nested) retype(nested, oldName, newName, declaration, changed);
                }
            }
        }
        for (UiNode child : node.children()) retype(child, oldName, newName, declaration, changed);
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

    /**
     * Ресурсы проекта для выбора в значениях свойств.
     *
     * @return каталог по папкам {@code textures/}, {@code fonts/}, {@code shaders/}
     */
    public dev.sixik.unigui.xaml.edit.ResourceCatalog resources() {
        return folder -> files.keySet().stream().filter(path -> path.startsWith(folder))
                .map(path -> path.substring(folder.length())).toList();
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
