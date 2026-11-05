package dev.sixik.unigui.xaml.pack;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.load.XamlTextureResolver;
import dev.sixik.unigui.api.widget.Widget;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Пак ресурсов: один архив, в котором лежит интерфейс как данные вместе со всем, что ему нужно, — экраны,
 * компоненты, структуры, текстуры, шрифты, шейдеры.
 *
 * <p>Соглашение о раскладке (всё необязательно, кроме паспорта):</p>
 * <ul>
 *   <li>{@code pack.xaml} — паспорт ({@link PackManifest});</li>
 *   <li>{@code structs/*.xaml} — структуры ({@code <Struct>});</li>
 *   <li>{@code components/*.xaml} — компоненты; регистрируются под идентификатором {@code <packId>/<имя файла>};</li>
 *   <li>{@code screens/*.xaml} — экраны для {@link #screen(String, LoadOptions, TextureDecoder)};</li>
 *   <li>{@code textures/**}, {@code fonts/**}, {@code shaders/**} и любые другие файлы — ресурсы.</li>
 * </ul>
 *
 * <p>Пак читается целиком в память под жёсткими пределами (число и размер файлов, защита от путей с {@code ..}),
 * поэтому архив из чужих рук безопасно открывать. Содержимое пака — только данные: код Java он не несёт, а граф
 * компонента может вызвать лишь функции, которые зарегистрировал сам мод.</p>
 */
public final class AssetPack {
    /**
     * Предел числа файлов в архиве.
     */
    public static final int MAX_ENTRIES = 10_000;
    /**
     * Предел размера одного файла после распаковки, байт.
     */
    public static final long MAX_ENTRY_BYTES = 32L * 1024 * 1024;
    /**
     * Предел суммарного размера после распаковки, байт.
     */
    public static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;

    /**
     * Превращает байты текстуры из пака в рабочий handle; реализует бэкенд.
     */
    @FunctionalInterface
    public interface TextureDecoder {
        /**
         * @param id      путь текстуры в паке
         * @param data    содержимое файла
         * @param width   запрошенная ширина
         * @param height  запрошенная высота
         * @param options параметры выборки
         * @return handle
         */
        TextureHandle decode(String id, byte[] data, int width, int height, TextureOptions options);

        /**
         * @return декодер без бэкенда: handle без нативной текстуры (тесты, предпросмотр без игры)
         */
        static TextureDecoder simple() {
            return (id, data, width, height, options) -> new SimpleTextureHandle(id, width, height, null, options);
        }
    }

    private final PackManifest manifest;
    private final Map<String, byte[]> files;

    private AssetPack(PackManifest manifest, Map<String, byte[]> files) {
        this.manifest = manifest;
        this.files = Collections.unmodifiableMap(files);
    }

    // ------------------------------------------------------------------ открытие

    /**
     * Открывает пак из файла.
     *
     * @param path путь к архиву
     * @return пак
     * @throws IOException       если файл не читается
     * @throws XamlLoadException если архив не пак (нет или неверен паспорт)
     */
    public static AssetPack open(Path path) throws IOException {
        return read(Files.readAllBytes(path));
    }

    /**
     * Открывает пак из байтов архива.
     *
     * @param archive содержимое zip
     * @return пак
     */
    public static AssetPack read(byte[] archive) {
        return read(new ByteArrayInputStream(archive));
    }

    /**
     * Открывает пак из потока.
     *
     * @param stream поток zip; не закрывается
     * @return пак
     */
    public static AssetPack read(InputStream stream) {
        Map<String, byte[]> files = new TreeMap<>();
        long total = 0;
        try {
            ZipInputStream zip = new ZipInputStream(stream, StandardCharsets.UTF_8);
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.isDirectory()) continue;
                if (files.size() >= MAX_ENTRIES) {
                    throw new XamlLoadException("The pack has more than " + MAX_ENTRIES + " files.");
                }
                String name = normalize(entry.getName());
                byte[] data = readLimited(zip, name);
                total += data.length;
                if (total > MAX_TOTAL_BYTES) {
                    throw new XamlLoadException("The pack is larger than " + (MAX_TOTAL_BYTES >> 20) + " MB unpacked.");
                }
                if (files.put(name, data) != null) throw new XamlLoadException("The pack lists '" + name + "' twice.");
            }
        } catch (IOException failure) {
            throw new XamlLoadException("Cannot read the pack archive: " + failure.getMessage(), dev.sixik.unigui.xaml.SourceLocation.UNKNOWN, failure);
        }
        byte[] manifestBytes = files.get(PackManifest.FILE);
        if (manifestBytes == null)
            throw new XamlLoadException("The archive has no " + PackManifest.FILE + " in its root.");
        return new AssetPack(PackManifest.parse(new String(manifestBytes, StandardCharsets.UTF_8)), files);
    }

    /**
     * Приводит имя записи к виду {@code a/b/c.ext} и отвергает пути, выходящие из архива.
     */
    static String normalize(String raw) {
        String name = raw.replace('\\', '/');
        if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new XamlLoadException("Pack entry '" + raw + "' has an absolute path.");
        }
        List<String> parts = new ArrayList<>();
        for (String part : name.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) throw new XamlLoadException("Pack entry '" + raw + "' leaves the archive.");
            parts.add(part);
        }
        if (parts.isEmpty()) throw new XamlLoadException("Pack entry '" + raw + "' has an empty name.");
        return String.join("/", parts);
    }

    private static byte[] readLimited(ZipInputStream zip, String name) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        long size = 0;
        for (int read = zip.read(buffer); read >= 0; read = zip.read(buffer)) {
            size += read;
            if (size > MAX_ENTRY_BYTES) {
                throw new XamlLoadException("Pack entry '" + name + "' is larger than " + (MAX_ENTRY_BYTES >> 20) + " MB.");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ содержимое

    /**
     * @return паспорт
     */
    public PackManifest manifest() {
        return manifest;
    }

    /**
     * @return пути всех файлов по алфавиту
     */
    public List<String> names() {
        return List.copyOf(files.keySet());
    }

    /**
     * @param folder папка с завершающим {@code /}, например {@code components/}
     * @return пути файлов в папке (включая вложенные) по алфавиту
     */
    public List<String> names(String folder) {
        List<String> found = new ArrayList<>();
        for (String name : files.keySet()) if (name.startsWith(folder)) found.add(name);
        return found;
    }

    /**
     * @param path путь файла
     * @return {@code true}, если файл есть
     */
    public boolean has(String path) {
        return files.containsKey(path);
    }

    /**
     * @param path путь файла
     * @return содержимое или пусто
     */
    public Optional<byte[]> bytes(String path) {
        byte[] data = files.get(path);
        return data == null ? Optional.empty() : Optional.of(data.clone());
    }

    /**
     * @param path путь файла
     * @return содержимое как текст UTF-8 или пусто
     */
    public Optional<String> text(String path) {
        byte[] data = files.get(path);
        return data == null ? Optional.empty() : Optional.of(new String(data, StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------ использование

    /**
     * Регистрирует структуры и компоненты пака. Ошибки отдельных файлов не прерывают установку: остальные файлы
     * ставятся, а проблемы перечисляются в {@link PackInstallation#problems()}.
     *
     * @return установка; её {@code close()} снимает всё установленное
     */
    public PackInstallation install() {
        return PackInstallation.install(this);
    }

    /**
     * Загружает экран пака. Текстуры берутся из пака, а чего в нём нет, ищется прежним резолвером.
     *
     * @param name    имя файла в {@code screens/} без расширения
     * @param options настройки загрузки
     * @param decoder превращает байты текстур в handle
     * @return экран
     * @throws XamlLoadException если экрана нет или он неверен
     */
    public LoadedScreen<Widget> screen(String name, LoadOptions options, TextureDecoder decoder) {
        String path = "screens/" + name + ".xaml";
        String text = text(path).orElseThrow(() -> new XamlLoadException(
                "The pack " + manifest.id() + " has no screen '" + name + "'. Screens: " + names("screens/") + "."));
        LoadOptions effective = options == null ? LoadOptions.DEFAULT : options;
        return Xaml.loadScreen(text, effective.textureResolver(textureResolver(decoder, effective.textureResolver())));
    }

    /**
     * Резолвер текстур, который сначала ищет файл в паке: идентификатор {@code icons/sword.png} означает
     * {@code textures/icons/sword.png}. Если файла нет, запрос уходит в {@code fallback}.
     *
     * @param decoder  превращает байты в handle
     * @param fallback резолвер для текстур вне пака (например, игровой)
     * @return резолвер
     */
    public XamlTextureResolver textureResolver(TextureDecoder decoder, XamlTextureResolver fallback) {
        TextureDecoder effective = decoder == null ? TextureDecoder.simple() : decoder;
        XamlTextureResolver next = fallback == null ? XamlTextureResolver.DEFAULT : fallback;
        return (id, width, height, options) -> {
            byte[] data = files.get("textures/" + id);
            return data == null ? next.resolve(id, width, height, options)
                    : effective.decode(id, data, width, height, options);
        };
    }

    /**
     * @return файлы по путям; нужен записи пака обратно
     */
    Map<String, byte[]> files() {
        return files;
    }

    @Override
    public String toString() {
        return "AssetPack[" + manifest.id() + " " + manifest.version() + ", " + files.size() + " files]";
    }

    /**
     * @return копия содержимого, сгруппированная по папкам верхнего уровня (для отчётов и редактора)
     */
    public Map<String, Integer> summary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String name : files.keySet()) {
            int slash = name.indexOf('/');
            counts.merge(slash < 0 ? "(root)" : name.substring(0, slash + 1), 1, Integer::sum);
        }
        return counts;
    }
}
