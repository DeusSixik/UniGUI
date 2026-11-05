package dev.sixik.unigui.xaml.pack;

import dev.sixik.unigui.xaml.project.Project;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Папка настроек библиотеки: где лежат настройки UI и готовые Asset Pack файлы.
 *
 * <pre>
 * &lt;config&gt;/unigui/
 *     settings.xaml     настройки UI ({@link UiSettings})
 *     packs/            готовые паки (*.zip): сюда экспортирует редактор, отсюда их читает игра
 *     projects/         проекты редактора в виде папок (необязательно)
 * </pre>
 *
 * <p>Путь к папке задаёт бэкенд (каталог конфигурации игры). Библиотека читает паки из {@code packs/} и ставит их все
 * разом ({@link #install()}); пак, упакованный в сам мод, ставится раньше ({@link #bundled}), поэтому пак игрока с тем
 * же идентификатором его заменяет. Битый пак не мешает остальным: проблема попадает в отчёт. Для работы без
 * перезапуска служит {@link Live}: он следит за папкой и переустанавливает паки, когда файлы изменились.</p>
 */
public final class PackLibrary {
    /** Имя папки с готовыми паками. */
    public static final String PACKS = "packs";
    /** Имя папки с проектами редактора. */
    public static final String PROJECTS = "projects";
    /** Имя файла настроек. */
    public static final String SETTINGS = "settings.xaml";

    private final Path root;
    private final List<Bundled> bundled = new ArrayList<>();

    private record Bundled(ClassLoader loader, String resource) {
    }

    private PackLibrary(Path root) {
        this.root = root;
    }

    /**
     * Открывает библиотеку в папке. Папки создаются при первой записи, а не при открытии.
     *
     * @param root папка {@code unigui} внутри каталога конфигурации
     * @return библиотека
     */
    public static PackLibrary open(Path root) {
        return new PackLibrary(root);
    }

    /**
     * Добавляет пак, упакованный в ресурсы мода. Такие паки ставятся до паков из папки.
     *
     * @param loader   загрузчик классов мода
     * @param resource путь ресурса, например {@code assets/unigui/packs/default.zip}
     * @return эта библиотека
     */
    public PackLibrary bundled(ClassLoader loader, String resource) {
        bundled.add(new Bundled(loader, resource));
        return this;
    }

    /** @return корневая папка библиотеки */
    public Path root() {
        return root;
    }

    /** @return папка готовых паков */
    public Path packs() {
        return root.resolve(PACKS);
    }

    /** @return папка проектов редактора */
    public Path projects() {
        return root.resolve(PROJECTS);
    }

    /** @return файл настроек UI */
    public Path settingsFile() {
        return root.resolve(SETTINGS);
    }

    /** @return настройки UI из {@code settings.xaml}; пустые, если файла нет */
    public UiSettings settings() {
        return UiSettings.load(settingsFile());
    }

    // ------------------------------------------------------------------ состав

    /**
     * Описание файла пака в папке.
     *
     * @param file     файл
     * @param manifest паспорт или {@code null}, если файл не открылся
     * @param problem  причина, если пак не открылся, иначе пустая строка
     */
    public record Found(Path file, PackManifest manifest, String problem) {
        /** @return {@code true}, если пак открылся */
        public boolean ok() {
            return manifest != null;
        }
    }

    /**
     * Просматривает папку паков, не устанавливая их.
     *
     * @return найденные файлы по имени
     */
    public List<Found> scan() {
        List<Found> found = new ArrayList<>();
        if (!Files.isDirectory(packs())) return found;
        try (Stream<Path> files = Files.list(packs())) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))::iterator) {
                try {
                    found.add(new Found(file, AssetPack.open(file).manifest(), ""));
                } catch (IOException | RuntimeException failure) {
                    found.add(new Found(file, null, failure.getMessage() == null
                            ? failure.getClass().getSimpleName() : failure.getMessage()));
                }
            }
        } catch (IOException failure) {
            found.add(new Found(packs(), null, "Cannot list " + packs() + ": " + failure.getMessage()));
        }
        return found;
    }

    // ------------------------------------------------------------------ установка

    /** Результат установки всех паков библиотеки. */
    public static final class Installed implements AutoCloseable {
        private final List<PackInstallation> installations = new ArrayList<>();
        private final List<String> packs = new ArrayList<>();
        private final List<String> problems = new ArrayList<>();

        /** @return идентификаторы установленных паков в порядке установки */
        public List<String> packs() {
            return List.copyOf(packs);
        }

        /** @return проблемы: битые файлы, замены паков, ошибки документов внутри паков */
        public List<String> problems() {
            return List.copyOf(problems);
        }

        /** @return {@code true}, если проблем нет */
        public boolean clean() {
            return problems.isEmpty();
        }

        @Override
        public void close() {
            for (int i = installations.size() - 1; i >= 0; i--) installations.get(i).close();
            installations.clear();
            packs.clear();
        }
    }

    /**
     * Ставит паки мода и все паки из папки. Если у двух паков один идентификатор, остаётся пак с большей версией, а
     * при равных версиях — пак из папки; замена записывается в проблемы. Пак, который не открылся, пропускается.
     *
     * @return установка; её {@code close()} снимает всё установленное
     */
    public Installed install() {
        Installed result = new Installed();
        Map<String, Candidate> chosen = new LinkedHashMap<>();
        for (Bundled entry : bundled) {
            try (InputStream stream = entry.loader().getResourceAsStream(entry.resource())) {
                if (stream == null) {
                    result.problems.add("Bundled pack '" + entry.resource() + "' is missing.");
                    continue;
                }
                consider(chosen, new Candidate(AssetPack.read(stream), "bundled " + entry.resource()), result);
            } catch (IOException | RuntimeException failure) {
                result.problems.add("Bundled pack '" + entry.resource() + "': " + failure.getMessage());
            }
        }
        for (Found found : scan()) {
            if (!found.ok()) {
                result.problems.add(found.file().getFileName() + ": " + found.problem());
                continue;
            }
            try {
                consider(chosen, new Candidate(AssetPack.open(found.file()), found.file().getFileName().toString()), result);
            } catch (IOException | RuntimeException failure) {
                result.problems.add(found.file().getFileName() + ": " + failure.getMessage());
            }
        }
        for (Candidate candidate : chosen.values()) {
            PackInstallation installation = candidate.pack().install();
            result.installations.add(installation);
            result.packs.add(candidate.pack().manifest().id());
            for (String problem : installation.problems()) {
                result.problems.add(candidate.source() + ": " + problem);
            }
        }
        return result;
    }

    private record Candidate(AssetPack pack, String source) {
    }

    private static void consider(Map<String, Candidate> chosen, Candidate next, Installed result) {
        String id = next.pack().manifest().id();
        Candidate current = chosen.get(id);
        if (current == null) {
            chosen.put(id, next);
            return;
        }
        int order = compareVersions(next.pack().manifest().version(), current.pack().manifest().version());
        if (order >= 0) {
            result.problems.add("Pack '" + id + "' from " + next.source() + " replaces " + current.source() + ".");
            chosen.put(id, next);
        } else {
            result.problems.add("Pack '" + id + "' from " + next.source() + " is older than " + current.source()
                    + " and is ignored.");
        }
    }

    /** Сравнивает версии по числам через точку: {@code 1.10} новее {@code 1.9}; нечисловые части — как текст. */
    static int compareVersions(String a, String b) {
        String[] left = a.split("[.\\-+]");
        String[] right = b.split("[.\\-+]");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            String x = i < left.length ? left[i] : "0";
            String y = i < right.length ? right[i] : "0";
            int order;
            if (x.matches("\\d+") && y.matches("\\d+")) order = Long.compare(Long.parseLong(x), Long.parseLong(y));
            else order = x.compareTo(y);
            if (order != 0) return order;
        }
        return 0;
    }

    // ------------------------------------------------------------------ запись

    /**
     * Экспортирует проект в папку паков под именем {@code <id>.zip}. Запись идёт через временный файл, поэтому
     * читающая сторона не увидит наполовину записанный пак.
     *
     * @param project проект
     * @return записанный файл
     * @throws IOException при ошибке записи
     */
    public Path export(Project project) throws IOException {
        Files.createDirectories(packs());
        Path target = packs().resolve(project.manifest().id() + ".zip");
        Path temp = Files.createTempFile(packs(), project.manifest().id(), ".tmp");
        try {
            Files.write(temp, project.toArchive());
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        return target;
    }

    /**
     * Удаляет пак из папки.
     *
     * @param id идентификатор пака
     * @return {@code true}, если файл был
     * @throws IOException при ошибке удаления
     */
    public boolean remove(String id) throws IOException {
        return Files.deleteIfExists(packs().resolve(id + ".zip"));
    }

    /**
     * Находит пак в папке по идентификатору.
     *
     * @param id идентификатор
     * @return файл или пусто
     */
    public Optional<Path> find(String id) {
        for (Found found : scan()) if (found.ok() && found.manifest().id().equals(id)) return Optional.of(found.file());
        return Optional.empty();
    }

    // ------------------------------------------------------------------ слежение

    /**
     * Живая библиотека: держит установку паков и обновляет её, когда файлы в папке изменились. Вызывайте
     * {@link #poll()} раз в секунду-две (из тика игры): без изменений вызов читает только метаданные файлов.
     */
    public static final class Live implements AutoCloseable {
        private final PackLibrary library;
        private Installed installed;
        private String signature = "";

        /**
         * @param library библиотека
         */
        public Live(PackLibrary library) {
            this.library = library;
            reload();
        }

        /** @return текущая установка */
        public Installed installed() {
            return installed;
        }

        /**
         * Проверяет папку и при изменениях переустанавливает паки.
         *
         * @return {@code true}, если паки переустановлены
         */
        public boolean poll() {
            String now = signatureOf(library.packs());
            if (now.equals(signature)) return false;
            reload();
            return true;
        }

        private void reload() {
            if (installed != null) installed.close();
            signature = signatureOf(library.packs());
            installed = library.install();
        }

        @Override
        public void close() {
            if (installed != null) installed.close();
            installed = null;
        }
    }

    /** Подпись папки: имена, размеры и время изменения файлов; не читает содержимое. */
    static String signatureOf(Path folder) {
        if (!Files.isDirectory(folder)) return "";
        StringBuilder text = new StringBuilder();
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))::iterator) {
                text.append(file.getFileName()).append('|').append(Files.size(file)).append('|')
                        .append(Files.getLastModifiedTime(file).toMillis()).append(';');
            }
        } catch (IOException failure) {
            return "error:" + failure.getMessage();
        }
        return text.toString();
    }
}
