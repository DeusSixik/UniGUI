package dev.sixik.unigui.xaml.pack;

import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.format.XamlFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Настройки UI: плоский набор пар «ключ — значение» в {@code settings.xaml}.
 *
 * <pre>{@code
 * <Settings>
 *   <Setting Key="scale" Value="1.5"/>
 *   <Setting Key="editor.theme" Value="dark"/>
 * </Settings>
 * }</pre>
 *
 * <p>Ключи, которых нет в файле, дают значение по умолчанию, поэтому файл хранит только то, что изменил игрок.
 * Повреждённый файл не ломает запуск: настройки считаются пустыми, а причина доступна через {@link #problem()}.
 * Запись детерминирована (ключи по алфавиту), чтобы файл удобно хранить в системе контроля версий.</p>
 */
public final class UiSettings {
    private final Path file;
    private final Map<String, String> values = new TreeMap<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private String problem = "";

    private UiSettings(Path file) {
        this.file = file;
    }

    /**
     * Читает файл настроек.
     *
     * @param file путь файла; его может не быть
     * @return настройки
     */
    public static UiSettings load(Path file) {
        UiSettings settings = new UiSettings(file);
        if (!Files.isRegularFile(file)) return settings;
        try {
            UiDocument document = XamlFormat.INSTANCE.read(Files.readString(file, StandardCharsets.UTF_8));
            for (UiNode node : document.root().children()) {
                if (!node.type().local().equals("Setting")) continue;
                String key = node.attributeText("Key", "").trim();
                if (!key.isEmpty()) settings.values.put(key, node.attributeText("Value", ""));
            }
        } catch (IOException | RuntimeException failure) {
            settings.values.clear();
            settings.problem = file.getFileName() + ": " + failure.getMessage();
        }
        return settings;
    }

    /**
     * @return причина, по которой файл не прочитан, или пустая строка
     */
    public String problem() {
        return problem;
    }

    /**
     * @param key ключ
     * @return {@code true}, если ключ задан явно
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /**
     * @param key      ключ
     * @param fallback значение по умолчанию
     * @return значение
     */
    public String get(String key, String fallback) {
        return values.getOrDefault(key, fallback);
    }

    /**
     * @param key      ключ
     * @param fallback значение по умолчанию; возвращается и тогда, когда записанное не число
     * @return целое
     */
    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, "").trim());
        } catch (NumberFormatException failure) {
            return fallback;
        }
    }

    /**
     * @param key      ключ
     * @param fallback значение по умолчанию; возвращается и тогда, когда записанное не число
     * @return число
     */
    public float getFloat(String key, float fallback) {
        try {
            float parsed = Float.parseFloat(values.getOrDefault(key, "").trim());
            return Float.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException failure) {
            return fallback;
        }
    }

    /**
     * @param key      ключ
     * @param fallback значение по умолчанию
     * @return {@code true} или {@code false}; всё, кроме {@code true}/{@code false}, даёт значение по умолчанию
     */
    public boolean getBoolean(String key, boolean fallback) {
        return switch (values.getOrDefault(key, "").trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> fallback;
        };
    }

    /**
     * Задаёт значение.
     *
     * @param key   ключ: непустой
     * @param value значение; {@code null} убирает ключ
     * @return эти настройки
     */
    public UiSettings set(String key, String value) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("A setting key must not be blank.");
        boolean changed = value == null ? values.remove(key) != null : !value.equals(values.put(key, value));
        if (changed) for (Runnable listener : List.copyOf(listeners)) listener.run();
        return this;
    }

    /**
     * @return все заданные ключи по алфавиту
     */
    public Map<String, String> all() {
        return Map.copyOf(values);
    }

    /**
     * @param listener вызывается после каждого изменения значения
     */
    public void onChanged(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * Записывает файл через временный файл.
     *
     * @throws IOException при ошибке записи
     */
    public void save() throws IOException {
        StringBuilder text = new StringBuilder("<Settings>\n");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            text.append("  <Setting Key=\"").append(escape(entry.getKey())).append("\" Value=\"")
                    .append(escape(entry.getValue())).append("\"/>\n");
        }
        text.append("</Settings>\n");
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent() == null ? Path.of(".") : file.getParent(), "settings", ".tmp");
        try {
            Files.writeString(temp, text.toString(), StandardCharsets.UTF_8);
            Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        problem = "";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "&#10;");
    }
}
