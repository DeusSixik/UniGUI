package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.load.XamlLoadException;
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
        try {
            return parse(XamlFormat.INSTANCE.read(xml));
        } catch (UiParseException failure) {
            throw new XamlLoadException("Cannot parse storyboard XML: " + failure.reason(), failure.location(), failure);
        }
    }

    /** Парсит storyboard из готовой document-модели. */
    public static Storyboard parse(UiDocument document) {
        if (document == null) throw new IllegalArgumentException("XML storyboard document не должен быть null.");
        UiNode root = document.root();
        if (!"Storyboard".equals(root.type().local())) {
            throw error(root, "Корневым элементом должен быть <Storyboard>.");
        }

        ObjectArrayList<PropertyTrack<?>> tracks = new ObjectArrayList<>();
        for (UiNode child : root.children()) {
            if (!"FloatTrack".equals(child.type().local())) {
                throw error(child, "Неизвестный элемент storyboard: <" + child.type().local() + ">.");
            }
            tracks.add(parseFloatTrack(child));
        }
        return new Storyboard(tracks);
    }

    private static PropertyTrack<Float> parseFloatTrack(UiNode element) {
        String target = attribute(element, "target", "targetName");
        String property = attribute(element, "property", "propertyPath");
        ObjectArrayList<Keyframe<Float>> keyframes = new ObjectArrayList<>();
        for (UiNode child : element.children()) {
            float time = number(child, "time");
            float value = number(child, "value");
            switch (child.type().local()) {
                case "Discrete" -> keyframes.add(new DiscreteKeyframe<>(time, value));
                case "Spline" -> keyframes.add(new SplineKeyframe<>(time, value, easing(child)));
                default -> throw error(child, "FloatTrack поддерживает только <Spline> и <Discrete>.");
            }
        }
        if (keyframes.isEmpty()) throw error(element, "FloatTrack должен содержать keyframe'ы.");

        String interpolation = element.attributeText("interpolator").orElse("linear");
        FloatInterpolator interpolator = switch (normalize(interpolation)) {
            case "linear" -> FloatInterpolator.LINEAR;
            case "angle-shortest", "shortest-angle" -> AngleInterpolator.SHORTEST_PATH;
            default -> throw error(element, "Неизвестный float interpolator: " + interpolation);
        };
        return PropertyTrack.floats(target, property, keyframes, interpolator);
    }

    private static Easing easing(UiNode element) {
        boolean hasBezier = element.attributeText("x1").isPresent()
                || element.attributeText("y1").isPresent()
                || element.attributeText("x2").isPresent()
                || element.attributeText("y2").isPresent();
        if (hasBezier) {
            return Easing.cubicBezier(
                    number(element, "x1"),
                    number(element, "y1"),
                    number(element, "x2"),
                    number(element, "y2"));
        }

        String value = element.attributeText("easing").orElse("linear");
        String enumName = normalize(value).replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return AnimationEasing.valueOf(enumName);
        } catch (IllegalArgumentException exception) {
            throw error(element, "Неизвестный easing: " + value);
        }
    }

    private static String attribute(UiNode element, String primary, String alias) {
        String value = element.attributeText(primary).orElseGet(() -> element.attributeText(alias).orElse(""));
        if (value.isBlank()) throw error(element, "Обязательный атрибут '" + primary + "' не задан.");
        return value.trim();
    }

    private static float number(UiNode element, String name) {
        String value = element.attributeText(name)
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

    private static IllegalArgumentException error(UiNode element, String message) {
        String location = element != null && element.location().known()
                ? " (строка " + element.location().line() + ", колонка " + element.location().column() + ")"
                : "";
        return new IllegalArgumentException(message + location);
    }
}
