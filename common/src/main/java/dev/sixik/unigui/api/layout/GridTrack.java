package dev.sixik.unigui.api.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Размер трека грида: фиксированная длина, процент, гибкая доля, размер по содержимому
 * или диапазон minmax между двумя такими значениями.
 *
 * <p>Соответствует поддерживаемому UniGUI-подмножеству CSS {@code <track-size>} для грид-контейнеров.
 * Экземпляры создаются статическими фабриками; {@link #repeat(int, GridTrack)} раскрывает
 * сокращения {@code repeat()} в обычный список треков.</p>
 */
public sealed interface GridTrack permits GridTrack.Fixed, GridTrack.Percent, GridTrack.Flex, GridTrack.Auto, GridTrack.MinMax {
    /**
     * Фиксированный трек в пикселях.
     *
     * @param pixels размер трека, отрицательные значения становятся нулём
     */
    record Fixed(float pixels) implements GridTrack {
        public Fixed {
            pixels = sanitize(pixels);
        }
    }

    /**
     * Процент от содержимого грида.
     *
     * @param percent размер трека в процентах, где 100 означает весь размер содержимого
     */
    record Percent(float percent) implements GridTrack {
        public Percent {
            percent = sanitize(percent);
        }
    }

    /**
     * Гибкая доля оставшегося свободного места.
     *
     * @param weight весовой коэффициент доли, значения ниже нуля становятся нулём
     */
    record Flex(float weight) implements GridTrack {
        public Flex {
            weight = sanitize(weight);
        }
    }

    /**
     * Трек по размеру содержимого: подстраивается под крупнейший размещённый в нём элемент.
     */
    record Auto() implements GridTrack {
    }

    /**
     * Трек, ограниченный минимальным и максимальным размерами.
     *
     * @param min минимальный размер трека
     * @param max максимальный размер трека
     */
    record MinMax(GridTrack min, GridTrack max) implements GridTrack {
        public MinMax {
            min = min == null ? new Auto() : min;
            max = max == null ? new Auto() : max;
        }
    }

    /**
     * Создаёт фиксированный трек в пикселях.
     *
     * @param pixels размер трека
     * @return трек
     */
    static GridTrack px(float pixels) {
        return new Fixed(pixels);
    }

    /**
     * Создаёт трек в процентах.
     *
     * @param percent размер трека в процентах
     * @return трек
     */
    static GridTrack percent(float percent) {
        return new Percent(percent);
    }

    /**
     * Создаёт гибкий долевой трек.
     *
     * @param weight весовой коэффициент доли
     * @return трек
     */
    static GridTrack fr(float weight) {
        return new Flex(weight);
    }

    /**
     * Создаёт трек по размеру содержимого.
     *
     * @return трек
     */
    static GridTrack auto() {
        return new Auto();
    }

    /**
     * Создаёт трек, ограниченный минимальным и максимальным размерами.
     *
     * @param min минимальный размер трека
     * @param max максимальный размер трека
     * @return трек
     */
    static GridTrack minmax(GridTrack min, GridTrack max) {
        return new MinMax(min, max);
    }

    /**
     * Раскрывает сокращение {@code repeat()} в список треков.
     *
     * @param count число повторений, значения ниже единицы дают пустой список
     * @param track повторяемый трек
     * @return список из {@code count} копий трека
     */
    static List<GridTrack> repeat(int count, GridTrack track) {
        List<GridTrack> output = new ArrayList<>(Math.max(0, count));
        GridTrack safe = track == null ? new Auto() : track;
        for (int index = 0; index < count; index++) {
            output.add(safe);
        }
        return output;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    /**
     * Разбирает один токен трека вида {@code 100px}, {@code 50%}, {@code 1fr},
     * {@code auto} или {@code minmax(100px, 1fr)}.
     *
     * @param token один токен трека
     * @return разобранный трек
     * @throws IllegalArgumentException для неизвестных токенов
     */
    static GridTrack parse(String token) {
        Objects.requireNonNull(token, "token");
        String normalized = token.trim();
        if (normalized.equalsIgnoreCase("auto")) {
            return new Auto();
        }
        if (normalized.endsWith("%")) {
            return new Percent(Float.parseFloat(normalized.substring(0, normalized.length() - 1).trim()));
        }
        if (normalized.endsWith("fr")) {
            return new Flex(Float.parseFloat(normalized.substring(0, normalized.length() - 2).trim()));
        }
        if (normalized.regionMatches(true, 0, "minmax(", 0, 7) && normalized.endsWith(")")) {
            String inner = normalized.substring(7, normalized.length() - 1);
            int comma = inner.indexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("Expected minmax(min, max), got: " + token);
            }
            return new MinMax(parse(inner.substring(0, comma)), parse(inner.substring(comma + 1)));
        }
        if (normalized.endsWith("px")) {
            return new Fixed(Float.parseFloat(normalized.substring(0, normalized.length() - 2).trim()));
        }
        return new Fixed(Float.parseFloat(normalized));
    }
}
