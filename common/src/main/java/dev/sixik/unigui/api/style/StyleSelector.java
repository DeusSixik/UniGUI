package dev.sixik.unigui.api.style;

import java.util.Collection;
import java.util.Locale;

/**
 * Простой selector по типу виджета, style-class и явному style id.
 *
 * <p>Это минимальный resolver до полноценной CSS-подобной системы. Selector может ограничить стиль
 * типом виджета ({@link #target()}), class'ом ({@link #styleClass()}) и/или style id виджета
 * ({@link #widgetId()}). Чем конкретнее selector, тем выше его {@link #specificity()}.</p>
 *
 * @param target id типа виджета, например {@link StyleIds.Widget#BUTTON}
 * @param styleClass class виджета без точки
 * @param widgetId явный style id виджета
 */
public record StyleSelector(String target, String styleClass, String widgetId) {
    /** Пустой selector, который сам по себе не матчится. */
    public static final StyleSelector EMPTY = new StyleSelector("", "", "");

    /** Нормализует части selector'а. */
    public StyleSelector {
        target = normalize(target);
        styleClass = normalizeClass(styleClass);
        widgetId = normalize(widgetId);
    }

    /**
     * Создаёт selector только по типу виджета.
     *
     * @param target id типа виджета
     * @return selector для указанного target
     */
    public static StyleSelector target(String target) {
        return new StyleSelector(target, "", "");
    }

    /**
     * Создаёт selector только по style-class.
     *
     * @param styleClass class виджета
     * @return selector для указанного class
     */
    public static StyleSelector styleClass(String styleClass) {
        return new StyleSelector("", styleClass, "");
    }

    /**
     * Создаёт selector только по явному style id виджета.
     *
     * @param widgetId style id виджета
     * @return selector для указанного widget style id
     */
    public static StyleSelector widgetId(String widgetId) {
        return new StyleSelector("", "", widgetId);
    }

    /**
     * @return {@code true}, если selector не содержит условий
     */
    public boolean empty() {
        return target.isEmpty() && styleClass.isEmpty() && widgetId.isEmpty();
    }

    /**
     * Проверяет, подходит ли selector к виджету.
     *
     * @param widgetType тип виджета
     * @param widgetStyleId явный style id виджета
     * @param widgetClasses class'ы виджета
     * @return {@code true}, если все непустые части selector'а совпали
     */
    public boolean matches(String widgetType, String widgetStyleId, Collection<String> widgetClasses) {
        String normalizedType = normalize(widgetType);
        String normalizedStyleId = normalize(widgetStyleId);
        if (!target.isEmpty() && !target.equals(normalizedType)) return false;
        if (!widgetId.isEmpty() && !widgetId.equals(normalizedStyleId)) return false;
        if (!styleClass.isEmpty() && !hasClass(widgetClasses, styleClass)) return false;
        return !empty();
    }

    /**
     * Возвращает вес selector'а для сортировки каскада.
     *
     * @return числовая специфичность; id сильнее class, class сильнее type
     */
    public int specificity() {
        int value = 0;
        if (!target.isEmpty()) value += 10;
        if (!styleClass.isEmpty()) value += 100;
        if (!widgetId.isEmpty()) value += 1000;
        return value;
    }

    private static boolean hasClass(Collection<String> classes, String styleClass) {
        if (classes == null || classes.isEmpty()) return false;
        for (String value : classes) {
            if (normalizeClass(value).equals(styleClass)) return true;
        }
        return false;
    }

    private static String normalizeClass(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}