package dev.sixik.unigui.xaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Сравнение с эталонными файлами из {@code src/test/resources/golden}.
 *
 * <p>Эталон обновляется осознанно: запусти тесты с переменной окружения {@code UNIGUI_UPDATE_GOLDEN=1}.
 * Отсутствующий эталон создаётся при первом запуске, и этот запуск считается провалом, чтобы новый файл
 * не попал в репозиторий непросмотренным.</p>
 */
final class Golden {
    private static final Path DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE = "1".equals(System.getenv("UNIGUI_UPDATE_GOLDEN"));

    private Golden() {
    }

    static void assertMatches(String fileName, String actual) {
        String normalized = actual.replace("\r\n", "\n");
        Path file = DIR.resolve(fileName);
        try {
            if (UPDATE || !Files.exists(file)) {
                boolean created = !Files.exists(file);
                Files.createDirectories(DIR);
                Files.writeString(file, normalized, StandardCharsets.UTF_8);
                if (created && !UPDATE) {
                    throw new AssertionError("Golden file " + file + " did not exist and was created; review it and re-run.");
                }
                return;
            }
            String expected = Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
            if (expected.equals(normalized)) return;
            List<String> left = expected.lines().toList();
            List<String> right = normalized.lines().toList();
            for (int i = 0; i < Math.max(left.size(), right.size()); i++) {
                String a = i < left.size() ? left.get(i) : "<missing>";
                String b = i < right.size() ? right.get(i) : "<missing>";
                if (!a.equals(b)) {
                    assertEquals(a, b, fileName + " differs at line " + (i + 1)
                            + " (set UNIGUI_UPDATE_GOLDEN=1 to accept)");
                }
            }
        } catch (IOException failure) {
            throw new AssertionError("Cannot access golden file " + file, failure);
        }
    }

    static String read(String fileName) {
        try {
            return Files.readString(DIR.resolve(fileName), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new AssertionError("Cannot read golden file " + fileName, failure);
        }
    }
}
