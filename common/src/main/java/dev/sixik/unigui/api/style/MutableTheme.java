package dev.sixik.unigui.api.style;

import java.util.HashMap;
import java.util.Map;

/**
 * Простая изменяемая тема вида {@code widgetType -> Style}.
 *
 * <p>{@code MutableTheme} подходит для Java-настройки без selector'ов и StylePack XML: один стиль
 * назначается на один тип виджета, а отдельный fallback применяется ко всем неизвестным типам.
 * Для более сложного выбора по id/class/state нужно использовать {@link StylePack}.</p>
 */
public final class MutableTheme implements Theme {
    private final Map<String, Style> styles = new HashMap<>();
    private Style fallback = Style.EMPTY;
    private long version;

    /**
     * Задаёт fallback-стиль темы.
     *
     * @param fallback стиль для неизвестных или пустых типов виджетов
     * @return эта тема для fluent-настройки
     */
    public MutableTheme fallback(Style fallback) {
        this.fallback = fallback == null ? Style.EMPTY : fallback;
        version++;
        return this;
    }

    /**
     * Назначает стиль на тип виджета.
     *
     * @param widgetType id типа виджета; пустой id обновляет fallback
     * @param style стиль для типа или {@link Style#EMPTY}
     * @return эта тема для fluent-настройки
     */
    public MutableTheme put(String widgetType, Style style) {
        if (widgetType == null || widgetType.isEmpty()) {
            return fallback(style);
        }
        styles.put(widgetType, style == null ? Style.EMPTY : style);
        version++;
        return this;
    }

    @Override
    public long version() {
        long result = version + fallback.version();
        for (Style style : styles.values()) {
            result += style.version();
        }
        return result;
    }

    @Override
    public Style styleFor(String widgetType) {
        if (widgetType == null || widgetType.isEmpty()) return fallback;
        return styles.getOrDefault(widgetType, fallback);
    }
}