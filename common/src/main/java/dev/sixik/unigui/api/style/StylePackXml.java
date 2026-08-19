package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * XML codec для декларативных ресурсов {@link StylePack}.
 *
 * <p>Codec поддерживает strict parse для runtime и tolerant parse для редактора. Strict-методы
 * бросают ошибку при diagnostics, а editor-методы возвращают {@link StylePackResult}, чтобы UI мог
 * показать частично загруженный pack и список ошибок без потери данных.</p>
 */
public final class StylePackXml {
    private StylePackXml() {
    }

    /**
     * Парсит XML через стандартный registry свойств.
     *
     * @param xml XML-документ StylePack
     * @return загруженный pack
     */
    public static StylePack parse(String xml) {
        return parse(xml, StyleKeyRegistry.builtIns());
    }

    /**
     * Strict-парсинг XML через указанный registry свойств.
     *
     * @param xml XML-документ StylePack
     * @param registry registry известных style-свойств
     * @return загруженный pack
     */
    public static StylePack parse(String xml, StyleKeyRegistry registry) {
        return parseEditor(xml, registry).throwIfDiagnostics().pack();
    }

    /**
     * Tolerant-парсинг XML для editor/hot-reload сценариев.
     *
     * @param xml XML-документ StylePack
     * @return pack и diagnostics
     */
    public static StylePackResult parseEditor(String xml) {
        return parseEditor(xml, StyleKeyRegistry.builtIns());
    }

    /**
     * Tolerant-парсинг XML через указанный registry свойств.
     *
     * @param xml XML-документ StylePack
     * @param registry registry известных style-свойств
     * @return pack и diagnostics
     */
    public static StylePackResult parseEditor(String xml, StyleKeyRegistry registry) {
        StyleKeyRegistry effectiveRegistry = registry == null ? StyleKeyRegistry.builtIns() : registry;
        XmlWidgetDocument document;
        try {
            document = XmlWidgetDocument.parse(xml);
        } catch (XmlWidgetLoadException exception) {
            return new StylePackResult(StylePack.create("style-pack"), exception.diagnostics());
        }

        XmlWidgetElement root = document.root();
        List<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        if (!root.name().equals("StylePack")) {
            diagnostics.add(diagnostic(root, "Expected root element <StylePack>, got <" + root.name() + ">."));
            return new StylePackResult(StylePack.create("style-pack"), diagnostics);
        }

        StylePack pack = StylePack.create(root.attributeOrDefault("id", "style-pack"));
        for (XmlWidgetElement child : root.elementChildren()) {
            switch (child.name()) {
                case "Style" -> parseStyle(pack, child, effectiveRegistry, diagnostics);
                case "Binding" -> parseBinding(pack, child, diagnostics);
                case "Animation" -> parseAnimation(pack, child, diagnostics);
                default -> diagnostics.add(diagnostic(child, "Unknown StylePack child <" + child.name() + ">."));
            }
        }
        return new StylePackResult(pack, diagnostics);
    }

    /**
     * Парсит одиночный XML-элемент {@code <Style>} через стандартный registry свойств.
     *
     * <p>Метод возвращает только декларативные значения {@link Style}: {@code <Setter>} и
     * {@code <State>}. Атрибуты {@code id}, {@code target}, {@code class}, {@code widgetId},
     * {@code renderer} и дочерние {@code <Event>} относятся к {@link StyleDefinition} и при таком
     * разборе не сохраняются. Для полного формата используй {@link StylePack#from(String)}.</p>
     *
     * @param xml XML с корневым элементом {@code <Style>}
     * @return загруженный стиль
     */
    public static Style parseStyle(String xml) {
        return parseStyle(xml, StyleKeyRegistry.builtIns());
    }

    /**
     * Парсит одиночный XML-элемент {@code <Style>} через указанный registry свойств.
     *
     * @param xml XML с корневым элементом {@code <Style>}
     * @param registry registry известных style-свойств
     * @return загруженный стиль
     */
    public static Style parseStyle(String xml, StyleKeyRegistry registry) {
        StyleKeyRegistry effectiveRegistry = registry == null ? StyleKeyRegistry.builtIns() : registry;
        XmlWidgetDocument document = XmlWidgetDocument.parse(xml);
        XmlWidgetElement root = document.root();
        List<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        if (!root.name().equals("Style")) {
            diagnostics.add(diagnostic(root, "Expected root element <Style>, got <" + root.name() + ">."));
            throw new XmlWidgetLoadException(diagnostics);
        }

        Style style = parseStyleValues(root, effectiveRegistry, diagnostics, null);
        if (!diagnostics.isEmpty()) {
            throw new XmlWidgetLoadException(diagnostics);
        }
        return style;
    }

    /**
     * Сериализует style pack обратно в XML.
     *
     * @param pack pack для сохранения; {@code null} сериализуется как пустой pack
     * @return XML-строка
     */
    public static String toXmlString(StylePack pack) {
        StylePack source = pack == null ? StylePack.create("style-pack") : pack;
        StringBuilder xml = new StringBuilder();
        xml.append("<StylePack id=\"").append(escape(source.id())).append("\">\n");
        for (StyleAnimationDefinition animation : source.animations().values()) {
            writeAnimation(xml, animation, 1);
        }

        Map<String, List<String>> targetsByStyle = targetsByStyle(source);
        for (StyleDefinition definition : source.styles().values()) {
            writeStyle(xml, definition, targetsByStyle.getOrDefault(definition.id(), List.of()), 1);
        }

        for (Map.Entry<String, String> binding : source.widgetBindings().entrySet()) {
            List<String> targets = targetsByStyle.getOrDefault(binding.getValue(), List.of());
            if (!targets.isEmpty() && targets.get(0).equals(binding.getKey())) continue;
            indent(xml, 1)
                    .append("<Binding target=\"").append(escape(binding.getKey()))
                    .append("\" style=\"").append(escape(binding.getValue()))
                    .append("\" />\n");
        }
        xml.append("</StylePack>");
        return xml.toString();
    }

    private static void parseStyle(StylePack pack,
                                   XmlWidgetElement element,
                                   StyleKeyRegistry registry,
                                   List<XmlWidgetDiagnostic> diagnostics) {
        String id = element.attributeOrDefault("id", "").trim();
        if (id.isEmpty()) {
            diagnostics.add(diagnostic(element, "Style requires non-empty id attribute."));
            return;
        }

        Map<String, String> events = new LinkedHashMap<>();
        MutableStyle style = parseStyleValues(element, registry, diagnostics, events);

        String renderer = element.attributeOrDefault("renderer", "").trim();
        String target = element.attributeOrDefault("target", "").trim();
        String styleClass = element.attributeOrDefault("class", "").trim();
        String widgetId = element.attributeOrDefault("widgetId", "").trim();
        StyleDefinition definition = renderer.isEmpty()
                ? StyleDefinition.of(id, style)
                : StyleDefinition.custom(id, renderer, style);
        definition = definition.selector(new StyleSelector(target, styleClass, widgetId));
        for (Map.Entry<String, String> event : events.entrySet()) {
            definition = definition.eventAnimation(event.getKey(), event.getValue());
        }
        pack.put(definition);

        if (!target.isEmpty() && styleClass.isEmpty() && widgetId.isEmpty()) {
            pack.bind(target, id);
        }
    }

    private static MutableStyle parseStyleValues(XmlWidgetElement element,
                                                 StyleKeyRegistry registry,
                                                 List<XmlWidgetDiagnostic> diagnostics,
                                                 Map<String, String> events) {
        MutableStyle style = new MutableStyle();
        for (XmlWidgetElement child : element.elementChildren()) {
            switch (child.name()) {
                case "Setter" -> parseSetter(style, WidgetState.NORMAL, child, registry, diagnostics);
                case "State" -> parseState(style, child, registry, diagnostics);
                case "Event" -> {
                    if (events != null) {
                        parseEvent(events, child, diagnostics);
                    }
                }
                default -> diagnostics.add(diagnostic(child, "Unknown Style child <" + child.name() + ">."));
            }
        }
        return style;
    }

    private static void parseBinding(StylePack pack, XmlWidgetElement element, List<XmlWidgetDiagnostic> diagnostics) {
        String target = element.attributeOrDefault("target", "").trim();
        String style = element.attributeOrDefault("style", "").trim();
        if (target.isEmpty()) {
            diagnostics.add(diagnostic(element, "Binding requires non-empty target attribute."));
        }
        if (style.isEmpty()) {
            diagnostics.add(diagnostic(element, "Binding requires non-empty style attribute."));
        }
        if (!target.isEmpty() && !style.isEmpty()) {
            pack.bind(target, style);
        }
    }

    private static void parseState(MutableStyle style,
                                   XmlWidgetElement element,
                                   StyleKeyRegistry registry,
                                   List<XmlWidgetDiagnostic> diagnostics) {
        String name = element.attributeOrDefault("name", "").trim();
        WidgetState state = parseEnum(WidgetState.class, name);
        if (state == null) {
            diagnostics.add(diagnostic(element, "Unknown widget state '" + name + "'."));
            return;
        }
        for (XmlWidgetElement child : element.elementChildren()) {
            if (child.name().equals("Setter")) {
                parseSetter(style, state, child, registry, diagnostics);
            } else {
                diagnostics.add(diagnostic(child, "Unknown State child <" + child.name() + ">."));
            }
        }
    }

    private static void parseSetter(MutableStyle style,
                                    WidgetState state,
                                    XmlWidgetElement element,
                                    StyleKeyRegistry registry,
                                    List<XmlWidgetDiagnostic> diagnostics) {
        String property = element.attributeOrDefault("property", "").trim();
        String value = element.attributeOrDefault("value", "").trim();
        if (property.isEmpty()) {
            diagnostics.add(diagnostic(element, "Setter requires non-empty property attribute."));
            return;
        }
        if (value.isEmpty()) {
            diagnostics.add(diagnostic(element, "Setter '" + property + "' requires non-empty value attribute."));
            return;
        }

        StylePropertyDescriptor<?> descriptor = registry.descriptor(property).orElse(null);
        if (descriptor == null) {
            diagnostics.add(diagnostic(element, "Unknown style property '" + property + "'."));
            return;
        }
        try {
            putParsed(style, state, descriptor, value);
        } catch (RuntimeException exception) {
            diagnostics.add(diagnostic(element, "Invalid value for style property '" + property + "': " + exception.getMessage()));
        }
    }

    private static <T> void putParsed(MutableStyle style,
                                      WidgetState state,
                                      StylePropertyDescriptor<T> descriptor,
                                      String value) {
        style.put(descriptor.key(), state, descriptor.parse(value));
    }

    private static void parseEvent(Map<String, String> events,
                                   XmlWidgetElement element,
                                   List<XmlWidgetDiagnostic> diagnostics) {
        String name = element.attributeOrDefault("name", "").trim();
        String animation = element.attributeOrDefault("animation", "").trim();
        if (name.isEmpty()) {
            diagnostics.add(diagnostic(element, "Event requires non-empty name attribute."));
        }
        if (animation.isEmpty()) {
            diagnostics.add(diagnostic(element, "Event requires non-empty animation attribute."));
        }
        if (!name.isEmpty() && !animation.isEmpty()) {
            events.put(name, animation);
        }
    }

    private static void parseAnimation(StylePack pack,
                                       XmlWidgetElement element,
                                       List<XmlWidgetDiagnostic> diagnostics) {
        String id = element.attributeOrDefault("id", "").trim();
        if (id.isEmpty()) {
            diagnostics.add(diagnostic(element, "Animation requires non-empty id attribute."));
            return;
        }

        List<StylePropertyTween> tweens = new ArrayList<>();
        for (XmlWidgetElement child : element.elementChildren()) {
            if (child.name().equals("Tween")) {
                StylePropertyTween tween = parseTween(child, diagnostics);
                if (tween != null) tweens.add(tween);
            } else {
                diagnostics.add(diagnostic(child, "Unknown Animation child <" + child.name() + ">."));
            }
        }
        pack.putAnimation(new StyleAnimationDefinition(id, tweens));
    }

    private static StylePropertyTween parseTween(XmlWidgetElement element, List<XmlWidgetDiagnostic> diagnostics) {
        String property = element.attributeOrDefault("property", "").trim();
        String to = element.attributeOrDefault("to", "").trim();
        if (property.isEmpty()) {
            diagnostics.add(diagnostic(element, "Tween requires non-empty property attribute."));
            return null;
        }
        if (to.isEmpty()) {
            diagnostics.add(diagnostic(element, "Tween '" + property + "' requires non-empty to attribute."));
            return null;
        }

        String from = element.attributeOrDefault("from", StylePropertyTween.CURRENT).trim();
        TransitionSpec transition = parseTransition(element, diagnostics);
        return new StylePropertyTween(property, from, to, transition);
    }

    private static TransitionSpec parseTransition(XmlWidgetElement element, List<XmlWidgetDiagnostic> diagnostics) {
        float duration = TransitionSpec.DEFAULT.durationSeconds();
        String durationText = element.attributeOrDefault("duration", "").trim();
        String durationMsText = element.attributeOrDefault("durationMs", "").trim();
        try {
            if (!durationText.isEmpty()) {
                duration = Float.parseFloat(durationText);
            } else if (!durationMsText.isEmpty()) {
                duration = Float.parseFloat(durationMsText) / 1000.0f;
            }
        } catch (NumberFormatException exception) {
            diagnostics.add(diagnostic(element, "Invalid Tween duration: " + exception.getMessage()));
        }

        AnimationEasing easing = TransitionSpec.DEFAULT.easing();
        String easingText = element.attributeOrDefault("easing", "").trim();
        if (!easingText.isEmpty()) {
            AnimationEasing parsed = parseEnum(AnimationEasing.class, easingText);
            if (parsed == null) {
                diagnostics.add(diagnostic(element, "Unknown easing '" + easingText + "'."));
            } else {
                easing = parsed;
            }
        }

        int repeat = 0;
        String repeatText = element.attributeOrDefault("repeat", "").trim();
        if (!repeatText.isEmpty()) {
            try {
                repeat = Integer.parseInt(repeatText);
            } catch (NumberFormatException exception) {
                diagnostics.add(diagnostic(element, "Invalid Tween repeat: " + exception.getMessage()));
            }
        }

        TransitionSpec transition = repeat < 0 ? new TransitionSpec(duration, easing).repeatForever() : new TransitionSpec(duration, easing).repeat(repeat);
        if (parseBoolean(element.attributeOrDefault("yoyo", "false"))) {
            transition = transition.yoyo();
        }
        return transition;
    }

    private static void writeAnimation(StringBuilder xml, StyleAnimationDefinition animation, int level) {
        indent(xml, level).append("<Animation id=\"").append(escape(animation.id())).append("\">\n");
        for (StylePropertyTween tween : animation.tweens()) {
            indent(xml, level + 1)
                    .append("<Tween property=\"").append(escape(tween.propertyName()))
                    .append("\" from=\"").append(escape(tween.fromValue()))
                    .append("\" to=\"").append(escape(tween.toValue()))
                    .append("\" duration=\"").append(formatFloat(tween.transition().durationSeconds()))
                    .append("\" easing=\"").append(tween.transition().easing().name()).append("\"");
            if (tween.transition().repeatCount() != 0) {
                xml.append(" repeat=\"").append(tween.transition().repeatCount()).append("\"");
            }
            if (tween.transition().autoReverse()) {
                xml.append(" yoyo=\"true\"");
            }
            xml.append(" />\n");
        }
        indent(xml, level).append("</Animation>\n");
    }

    private static void writeStyle(StringBuilder xml, StyleDefinition definition, List<String> targets, int level) {
        indent(xml, level).append("<Style id=\"").append(escape(definition.id())).append("\"");
        String target = definition.selector().target().isEmpty()
                ? targets.isEmpty() ? "" : targets.get(0)
                : definition.selector().target();
        if (!target.isEmpty()) {
            xml.append(" target=\"").append(escape(target)).append("\"");
        }
        if (!definition.selector().styleClass().isEmpty()) {
            xml.append(" class=\"").append(escape(definition.selector().styleClass())).append("\"");
        }
        if (!definition.selector().widgetId().isEmpty()) {
            xml.append(" widgetId=\"").append(escape(definition.selector().widgetId())).append("\"");
        }
        if (definition.customRenderer()) {
            xml.append(" renderer=\"").append(escape(definition.rendererId())).append("\"");
        }
        xml.append(">\n");

        Map<WidgetState, Map<StyleKey<?>, Object>> values = definition.style().values();
        writeSetters(xml, values.getOrDefault(WidgetState.NORMAL, Map.of()), WidgetState.NORMAL, level + 1);
        for (WidgetState state : WidgetState.values()) {
            if (state == WidgetState.NORMAL) continue;
            Map<StyleKey<?>, Object> stateValues = values.get(state);
            if (stateValues == null || stateValues.isEmpty()) continue;
            indent(xml, level + 1).append("<State name=\"").append(state.name()).append("\">\n");
            writeSetters(xml, stateValues, state, level + 2);
            indent(xml, level + 1).append("</State>\n");
        }
        for (Map.Entry<String, String> event : definition.eventAnimations().entrySet()) {
            indent(xml, level + 1)
                    .append("<Event name=\"").append(escape(event.getKey()))
                    .append("\" animation=\"").append(escape(event.getValue()))
                    .append("\" />\n");
        }
        indent(xml, level).append("</Style>\n");
    }

    private static void writeSetters(StringBuilder xml,
                                     Map<StyleKey<?>, Object> values,
                                     WidgetState state,
                                     int level) {
        List<Map.Entry<StyleKey<?>, Object>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().id()));
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();
        for (Map.Entry<StyleKey<?>, Object> entry : entries) {
            StylePropertyDescriptor<?> descriptor = registry.descriptor(entry.getKey().id()).orElse(null);
            if (descriptor == null) continue;
            String value = descriptor.format(entry.getValue());
            indent(xml, level)
                    .append("<Setter property=\"").append(escape(entry.getKey().id()))
                    .append("\" value=\"").append(escape(value))
                    .append("\" />\n");
        }
    }

    private static Map<String, List<String>> targetsByStyle(StylePack pack) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> binding : pack.widgetBindings().entrySet()) {
            result.computeIfAbsent(binding.getValue(), ignored -> new ArrayList<>()).add(binding.getKey());
        }
        return result;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim()
                .replace('-', '_')
                .replace('.', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("y");
    }

    private static String formatFloat(float value) {
        if (!Float.isFinite(value)) return "0";
        String text = Float.toString(value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }

    private static XmlWidgetDiagnostic diagnostic(XmlWidgetElement element, String message) {
        return new XmlWidgetDiagnostic(message, element.line(), element.column());
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static StringBuilder indent(StringBuilder builder, int level) {
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
        return builder;
    }
}
