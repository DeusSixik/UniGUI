package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Locale;

/**
 * Парсер компактного XAML-like формата storyboard.
 *
 * <p>Формат намеренно использует существующую безопасную XML-модель UniGUI и её source locations.
 * Lottie/bodymovin можно добавить отдельным импортёром, не усложняя runtime player.</p>
 *
 * <pre>{@code
 * <Storyboard>
 *   <FloatTrack target="panel" property="RenderTransform.Y">
 *     <Spline time="0" value="0" />
 *     <Spline time="0.25" value="16" easing="cubic-out" />
 *     <Discrete time="0.5" value="0" />
 *   </FloatTrack>
 * </Storyboard>
 * }</pre>
 */
public final class StoryboardXml {
    private StoryboardXml() {
    }

    /** Парсит storyboard из XML-строки. */
    public static Storyboard parse(String xml) {
        return parse(XmlWidgetDocument.parse(xml));
    }

    /** Парсит storyboard из готовой document-модели. */
    public static Storyboard parse(XmlWidgetDocument document) {
        if (document == null) throw new IllegalArgumentException("XML storyboard document не должен быть null.");
        XmlWidgetElement root = document.root();
        if (!"Storyboard".equals(root.name())) {
            throw error(root, "Корневым элементом должен быть <Storyboard>.");
        }

        ObjectArrayList<PropertyTrack<?>> tracks = new ObjectArrayList<>();
        for (XmlWidgetElement child : root.elementChildren()) {
            if (!"FloatTrack".equals(child.name())) {
                throw error(child, "Неизвестный элемент storyboard: <" + child.name() + ">.");
            }
            tracks.add(parseFloatTrack(child));
        }
        return new Storyboard(tracks);
    }

    private static PropertyTrack<Float> parseFloatTrack(XmlWidgetElement element) {
        String target = attribute(element, "target", "targetName");
        String property = attribute(element, "property", "propertyPath");
        ObjectArrayList<Keyframe<Float>> keyframes = new ObjectArrayList<>();
        for (XmlWidgetElement child : element.elementChildren()) {
            float time = number(child, "time");
            float value = number(child, "value");
            switch (child.name()) {
                case "Discrete" -> keyframes.add(new DiscreteKeyframe<>(time, value));
                case "Spline" -> keyframes.add(new SplineKeyframe<>(time, value, easing(child)));
                default -> throw error(child, "FloatTrack поддерживает только <Spline> и <Discrete>.");
            }
        }
        if (keyframes.isEmpty()) throw error(element, "FloatTrack должен содержать keyframe'ы.");

        String interpolation = element.attribute("interpolator").orElse("linear");
        FloatInterpolator interpolator = switch (normalize(interpolation)) {
            case "linear" -> FloatInterpolator.LINEAR;
            case "angle-shortest", "shortest-angle" -> AngleInterpolator.SHORTEST_PATH;
            default -> throw error(element, "Неизвестный float interpolator: " + interpolation);
        };
        return PropertyTrack.floats(target, property, keyframes, interpolator);
    }

    private static Easing easing(XmlWidgetElement element) {
        boolean hasBezier = element.attribute("x1").isPresent()
                || element.attribute("y1").isPresent()
                || element.attribute("x2").isPresent()
                || element.attribute("y2").isPresent();
        if (hasBezier) {
            return Easing.cubicBezier(
                    number(element, "x1"),
                    number(element, "y1"),
                    number(element, "x2"),
                    number(element, "y2"));
        }

        String value = element.attribute("easing").orElse("linear");
        String enumName = normalize(value).replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return AnimationEasing.valueOf(enumName);
        } catch (IllegalArgumentException exception) {
            throw error(element, "Неизвестный easing: " + value);
        }
    }

    private static String attribute(XmlWidgetElement element, String primary, String alias) {
        String value = element.attribute(primary).orElseGet(() -> element.attribute(alias).orElse(""));
        if (value.isBlank()) throw error(element, "Обязательный атрибут '" + primary + "' не задан.");
        return value.trim();
    }

    private static float number(XmlWidgetElement element, String name) {
        String value = element.attribute(name)
                .orElseThrow(() -> error(element, "Обязательный атрибут '" + name + "' не задан."));
        try {
            float parsed = Float.parseFloat(value.trim());
            if (!Float.isFinite(parsed)) throw new NumberFormatException("non-finite");
            return parsed;
        } catch (NumberFormatException exception) {
            throw error(element, "Атрибут '" + name + "' должен быть конечным числом: " + value);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static IllegalArgumentException error(XmlWidgetElement element, String message) {
        String location = element != null && element.hasLocation()
                ? " (строка " + element.line() + ", колонка " + element.column() + ")"
                : "";
        return new IllegalArgumentException(message + location);
    }
}
