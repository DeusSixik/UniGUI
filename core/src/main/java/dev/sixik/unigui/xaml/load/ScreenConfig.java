package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.LoadContext;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Настройки масштаба экрана, прочитанные из атрибутов {@code <Screen>} или объекта
 * {@code <Screen.ScaleProvider>}.
 *
 * <p>Логика разбора перенесена из прежнего загрузчика без изменения поддерживаемых имён и значений.</p>
 */
record ScreenConfig(
        UIScaleProvider scaleProvider,
        boolean hasScaleProvider,
        boolean scaleWithMinecraftGui,
        boolean hasScaleWithMinecraftGui) {

    static final Set<String> ATTRIBUTE_NAMES = Set.of(
            "type", "scaleProvider", "scaleProviderType", "uiScale", "scale",
            "referenceWidth", "referenceHeight", "referenceResolution", "match", "userScale",
            "minScale", "maxScale", "scaleRange", "viewportWidth", "viewportHeight", "viewport",
            "scaleWithMinecraftGui", "independentScale");

    /** Атрибут с позицией, чтобы ошибки значения указывали на конкретное место. */
    record Attr(String displayName, String value, SourceLocation location) {
    }

    ScreenConfig {
        scaleProvider = scaleProvider == null ? UIScaleProvider.IDENTITY : scaleProvider;
    }

    /** Накладывает {@code other} поверх этой конфигурации: заданное в {@code other} побеждает. */
    ScreenConfig merge(ScreenConfig other) {
        if (other == null) return this;
        return new ScreenConfig(
                other.hasScaleProvider ? other.scaleProvider : scaleProvider,
                hasScaleProvider || other.hasScaleProvider,
                other.hasScaleWithMinecraftGui ? other.scaleWithMinecraftGui : scaleWithMinecraftGui,
                hasScaleWithMinecraftGui || other.hasScaleWithMinecraftGui);
    }

    static ScreenConfig read(Map<String, Attr> attributes) {
        boolean hasScaleProvider = containsAny(attributes,
                "type", "scaleProvider", "scaleProviderType", "uiScale", "scale", "userScale",
                "referenceWidth", "referenceHeight", "referenceResolution", "match",
                "minScale", "maxScale", "scaleRange", "viewportWidth", "viewportHeight", "viewport");
        boolean hasScaleWithMinecraftGui = containsAny(attributes, "scaleWithMinecraftGui", "independentScale");
        boolean scaleWithMinecraftGui = !booleanValue(attributes, false, "independentScale");
        scaleWithMinecraftGui = booleanValue(attributes, scaleWithMinecraftGui, "scaleWithMinecraftGui");
        return new ScreenConfig(scaleProvider(attributes), hasScaleProvider, scaleWithMinecraftGui, hasScaleWithMinecraftGui);
    }

    private static UIScaleProvider scaleProvider(Map<String, Attr> attributes) {
        String type = stringValue(attributes, null, "scaleProvider", "scaleProviderType", "type");
        String normalizedType = normalize(type == null ? inferredType(attributes) : type);
        return switch (normalizedType) {
            case "", "identity", "none", "default" -> UIScaleProvider.IDENTITY;
            case "fixed" -> UIScaleProvider.fixed(floatValue(attributes, 1.0f, "uiScale", "scale", "userScale"));
            case "mutable" -> new MutableUIScaleProvider(floatValue(attributes, 1.0f, "uiScale", "scale", "userScale"));
            case "unity", "unitylike", "unityscale", "canvasscaler", "reference" -> unityLike(attributes);
            default -> throw new XamlLoadException("Unknown UIScaleProvider type '" + type + "'.",
                    locationOf(attributes, "scaleProvider", "scaleProviderType", "type"), null);
        };
    }

    private static UnityLikeUIScaleProvider unityLike(Map<String, Attr> attributes) {
        UnityLikeUIScaleProvider provider = new UnityLikeUIScaleProvider();
        float[] reference = pairValue(attributes, "referenceResolution");
        if (reference != null) provider.referenceResolution(reference[0], reference[1]);
        provider.referenceResolution(
                floatValue(attributes, provider.referenceWidth(), "referenceWidth"),
                floatValue(attributes, provider.referenceHeight(), "referenceHeight"));

        float[] viewport = pairValue(attributes, "viewport");
        if (viewport != null) provider.viewport(viewport[0], viewport[1]);
        provider.viewport(
                floatValue(attributes, provider.viewportWidth(), "viewportWidth"),
                floatValue(attributes, provider.viewportHeight(), "viewportHeight"));

        provider.match(matchValue(attributes, provider.match()));
        provider.userScale(floatValue(attributes, provider.userScale(), "userScale", "uiScale", "scale"));

        float[] range = pairValue(attributes, "scaleRange");
        float min = range == null ? provider.minScale() : range[0];
        float max = range == null ? provider.maxScale() : range[1];
        provider.scaleRange(floatValue(attributes, min, "minScale"), floatValue(attributes, max, "maxScale"));
        return provider;
    }

    private static String inferredType(Map<String, Attr> attributes) {
        if (containsAny(attributes, "referenceWidth", "referenceHeight", "referenceResolution", "match",
                "minScale", "maxScale", "scaleRange", "viewportWidth", "viewportHeight", "viewport")) {
            return "unity";
        }
        if (containsAny(attributes, "uiScale", "scale", "userScale")) return "fixed";
        return "identity";
    }

    private static boolean containsAny(Map<String, Attr> attributes, String... names) {
        for (String name : names) if (attributes.containsKey(name)) return true;
        return false;
    }

    private static Attr attr(Map<String, Attr> attributes, String... names) {
        for (String name : names) {
            Attr value = attributes.get(name);
            if (value != null) return value;
        }
        return null;
    }

    private static SourceLocation locationOf(Map<String, Attr> attributes, String... names) {
        Attr value = attr(attributes, names);
        return value == null ? SourceLocation.UNKNOWN : value.location();
    }

    private static String stringValue(Map<String, Attr> attributes, String fallback, String... names) {
        Attr value = attr(attributes, names);
        if (value == null || value.value() == null) return fallback;
        String normalized = value.value().trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static boolean booleanValue(Map<String, Attr> attributes, boolean fallback, String... names) {
        Attr value = attr(attributes, names);
        if (value == null) return fallback;
        try {
            return Converters.BOOLEAN.convert(value.value(), LoadContext.DEFAULT);
        } catch (RuntimeException failure) {
            throw invalid(value, failure);
        }
    }

    private static float floatValue(Map<String, Attr> attributes, float fallback, String... names) {
        Attr value = attr(attributes, names);
        if (value == null) return fallback;
        try {
            return Converters.FLOAT.convert(value.value(), LoadContext.DEFAULT);
        } catch (RuntimeException failure) {
            throw invalid(value, failure);
        }
    }

    private static float matchValue(Map<String, Attr> attributes, float fallback) {
        Attr value = attr(attributes, "match");
        if (value == null) return fallback;
        return switch (normalize(value.value())) {
            case "width", "horizontal", "x" -> 0.0f;
            case "balanced", "balance", "center", "middle" -> 0.5f;
            case "height", "vertical", "y" -> 1.0f;
            default -> floatValue(attributes, fallback, "match");
        };
    }

    private static float[] pairValue(Map<String, Attr> attributes, String name) {
        Attr value = attr(attributes, name);
        if (value == null) return null;
        String[] parts = value.value() == null ? new String[0] : value.value().trim().split("[xX,\\s]+", -1);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw invalid(value, new IllegalArgumentException("Expected two numeric values."));
        }
        try {
            return new float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1])};
        } catch (RuntimeException failure) {
            throw invalid(value, failure);
        }
    }

    private static XamlLoadException invalid(Attr value, RuntimeException failure) {
        return new XamlLoadException("Cannot apply attribute on Screen: Cannot parse/apply attribute '"
                + value.displayName() + "' value '" + value.value() + "': " + failure.getMessage(),
                value.location(), failure);
    }

    /** Приводит имя к виду без регистра, дефисов, подчёркиваний и пробелов. */
    static String normalize(String value) {
        if (value == null) return "";
        return value.trim().replace("-", "").replace("_", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }
}
