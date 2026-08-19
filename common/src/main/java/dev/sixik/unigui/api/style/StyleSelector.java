package dev.sixik.unigui.api.style;

import java.util.Collection;
import java.util.Locale;

/** Simple target/class/id selector used by StylePack before a full CSS-like resolver exists. */
public record StyleSelector(String target, String styleClass, String widgetId) {
    public static final StyleSelector EMPTY = new StyleSelector("", "", "");

    public StyleSelector {
        target = normalize(target);
        styleClass = normalizeClass(styleClass);
        widgetId = normalize(widgetId);
    }

    public static StyleSelector target(String target) {
        return new StyleSelector(target, "", "");
    }

    public static StyleSelector styleClass(String styleClass) {
        return new StyleSelector("", styleClass, "");
    }

    public static StyleSelector widgetId(String widgetId) {
        return new StyleSelector("", "", widgetId);
    }

    public boolean empty() {
        return target.isEmpty() && styleClass.isEmpty() && widgetId.isEmpty();
    }

    public boolean matches(String widgetType, String widgetStyleId, Collection<String> widgetClasses) {
        String normalizedType = normalize(widgetType);
        String normalizedStyleId = normalize(widgetStyleId);
        if (!target.isEmpty() && !target.equals(normalizedType)) return false;
        if (!widgetId.isEmpty() && !widgetId.equals(normalizedStyleId)) return false;
        if (!styleClass.isEmpty() && !hasClass(widgetClasses, styleClass)) return false;
        return !empty();
    }

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