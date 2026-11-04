package dev.sixik.unigui.xaml.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Собирает пак ресурсов в архив: экспорт из редактора и из сборки мода.
 *
 * <p>Запись детерминирована — файлы идут по алфавиту, время записей фиксировано, — поэтому один и тот же набор
 * файлов всегда даёт побайтно одинаковый архив (удобно для хешей и диффов).</p>
 */
public final class AssetPackWriter {
    /** Фиксированное время записей: 1980-01-01, самое раннее, которое умеет zip. */
    private static final long FIXED_TIME = 315_532_800_000L;

    private final PackManifest manifest;
    private final Map<String, byte[]> files = new TreeMap<>();

    /**
     * Начинает пак.
     *
     * @param manifest паспорт; записывается как {@code pack.xaml}
     */
    public AssetPackWriter(PackManifest manifest) {
        this.manifest = java.util.Objects.requireNonNull(manifest, "manifest");
    }

    /**
     * Добавляет файл.
     *
     * @param path путь внутри пака, например {@code components/card.xaml}
     * @param data содержимое
     * @return этот писатель
     */
    public AssetPackWriter add(String path, byte[] data) {
        String name = AssetPack.normalize(path);
        if (name.equals(PackManifest.FILE)) throw new IllegalArgumentException(PackManifest.FILE + " is written from the manifest");
        files.put(name, data.clone());
        return this;
    }

    /**
     * Добавляет текстовый файл в UTF-8.
     *
     * @param path путь внутри пака
     * @param text содержимое
     * @return этот писатель
     */
    public AssetPackWriter add(String path, String text) {
        return add(path, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Записывает архив.
     *
     * @param out поток; не закрывается
     * @throws IOException при ошибке записи
     */
    public void write(OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        put(zip, PackManifest.FILE, manifest.toXaml().getBytes(StandardCharsets.UTF_8));
        for (Map.Entry<String, byte[]> entry : files.entrySet()) put(zip, entry.getKey(), entry.getValue());
        zip.finish();
    }

    /**
     * @return архив в виде байтов
     */
    public byte[] toBytes() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(out);
            return out.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot write the pack: " + failure.getMessage(), failure);
        }
    }

    /**
     * Записывает архив в файл.
     *
     * @param path путь файла
     * @throws IOException при ошибке записи
     */
    public void write(Path path) throws IOException {
        Files.write(path, toBytes());
    }

    private static void put(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(FIXED_TIME);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }
}
