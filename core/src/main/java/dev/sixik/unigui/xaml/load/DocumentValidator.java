package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDiagnostic;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.format.MarkupExtensions;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Проверяет документ по схеме, не создавая ни одного виджета.
 *
 * <p>В отличие от загрузчика, который останавливается на первой ошибке, валидатор собирает все проблемы за один
 * проход и каждой даёт позицию, поэтому редактор может подсветить их сразу. Значения свойств проверяются теми же
 * конвертерами, что и при загрузке: {@code color="not-a-color"} находится без создания виджета.</p>
 *
 * <p>Чего валидатор проверить не может: то, что решает сам setter во время загрузки (например, неизвестная
 * команда в {@code onClick} или второй ребёнок у {@code ScrollView}).</p>
 */
public final class DocumentValidator {
    private static final Set<String> ID_NAMES = Set.of("id", "name", "Name");
    private static final Set<String> SCREEN_NAMES = Set.of("screen", "uiscreen", "xmlscreen");

    private final Schema schema;
    private final MarkupExtensionRegistry extensions;
    private final boolean strictAttributes;

    /**
     * Результат проверки.
     *
     * @param document    разобранный документ; {@code null}, если текст синтаксически некорректен
     * @param diagnostics найденные проблемы в порядке обхода документа
     */
    public record Result(UiDocument document, List<UiDiagnostic> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }

        /** @return {@code true}, если документ разобран и проблем нет */
        public boolean valid() {
            return document != null && diagnostics.isEmpty();
        }

        /** @return {@code true}, если есть хотя бы одна проблема */
        public boolean hasDiagnostics() {
            return !diagnostics.isEmpty();
        }
    }

    /**
     * Создаёт строгий валидатор со встроенными расширениями.
     *
     * @param schema реестр типов
     */
    public DocumentValidator(Schema schema) {
        this(schema, MarkupExtensionRegistry.DEFAULT, true);
    }

    /**
     * Создаёт валидатор.
     *
     * @param schema           реестр типов
     * @param extensions       известные markup extension
     * @param strictAttributes {@code true} — неизвестный атрибут считается проблемой
     */
    public DocumentValidator(Schema schema, MarkupExtensionRegistry extensions, boolean strictAttributes) {
        if (schema == null) throw new IllegalArgumentException("schema must not be null");
        this.schema = schema;
        this.extensions = extensions == null ? MarkupExtensionRegistry.DEFAULT : extensions;
        this.strictAttributes = strictAttributes;
    }

    /**
     * Разбирает и проверяет текст. Синтаксическая ошибка становится диагностикой, а не исключением.
     *
     * @param source текст документа
     * @return результат; {@link Result#document()} равен {@code null} при синтаксической ошибке
     */
    public Result validate(CharSequence source) {
        if (source == null || source.toString().isBlank()) {
            return new Result(null, List.of(new UiDiagnostic("XAML source must not be blank.")));
        }
        UiDocument document;
        try {
            document = XamlFormat.INSTANCE.read(source);
        } catch (UiParseException failure) {
            return new Result(null, List.of(new UiDiagnostic("Cannot parse XAML source: " + failure.reason(),
                    failure.location())));
        }
        return validate(document);
    }

    /**
     * Проверяет готовое дерево разметки.
     *
     * @param document документ
     * @return результат со всеми найденными проблемами
     */
    public Result validate(UiDocument document) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        Run run = new Run();
        UiNode root = document.root();
        if (run.isScreen(root)) run.screen(root);
        else run.node(root, ResourceScope.EMPTY);
        return new Result(document, run.problems);
    }

    private static boolean isElementForm(UiProperty property) {
        return property.form() == UiProperty.Form.ELEMENT || property.value() instanceof UiValue.Elements;
    }

    private static boolean isIdProperty(UiProperty property) {
        return property.owner() == null
                && ID_NAMES.contains(property.name().local())
                && (property.name().namespace().isEmpty() || XamlNamespaces.isLanguage(property.name().namespace()));
    }

    private final class Run {
        private final List<UiDiagnostic> problems = new ArrayList<>();
        private final Set<String> ids = new HashSet<>();

        private void add(SourceLocation at, String message) {
            problems.add(new UiDiagnostic(message, at));
        }

        // -------------------------------------------------------------- виджеты

        void node(UiNode node, ResourceScope parentScope) {
            TypeInfo<?> type = schema.type(node.type()).orElse(null);
            ResourceScope scope = resources(node, parentScope);
            if (type == null) {
                add(node.location(), "Unknown widget type '" + node.type().qualified() + "'.");
                descendUnknown(node, scope);
                return;
            }
            check(node, type, scope);
        }

        /** У неизвестного типа проверить нечего, но вложенные виджеты всё равно проверяются. */
        private void descendUnknown(UiNode node, ResourceScope scope) {
            for (UiProperty property : node.properties()) {
                if (property.value() instanceof UiValue.Elements elements) {
                    for (UiContent item : elements.content()) if (item instanceof UiNode child) node(child, scope);
                }
            }
            for (UiNode child : node.children()) node(child, scope);
        }

        private <T extends Widget> void check(UiNode node, TypeInfo<T> type, ResourceScope scope) {
            checkId(node);
            for (UiProperty property : node.properties()) {
                if (isIdProperty(property)) continue;
                if (isElementForm(property)) {
                    checkElementProperty(property, type, scope);
                } else {
                    checkAttribute(property, type, scope);
                }
            }
            checkText(node, type);
            for (UiNode child : node.children()) {
                TypeInfo<?> childType = schema.type(child.type()).orElse(null);
                if (childType != null && !type.acceptsChildren()) {
                    add(child.location(), "Widget " + type.name().local() + " cannot contain child "
                            + childType.javaType().getSimpleName() + ".");
                }
                node(child, scope);
            }
        }

        private void checkId(UiNode node) {
            String id = null;
            for (UiProperty property : node.properties()) {
                if (!isIdProperty(property)) continue;
                if (!(property.value() instanceof UiValue.Literal literal)) {
                    add(property.location(), "The id of " + node.type().qualified() + " must be plain text.");
                    continue;
                }
                String value = literal.text().trim();
                if (id != null && !id.equals(value)) {
                    add(property.location(), "Conflicting id attributes on " + node.type().qualified() + ".");
                }
                id = value;
            }
            if (id != null && !id.isEmpty() && !ids.add(id)) add(node.location(), "Duplicate widget id '" + id + "'.");
        }

        private <T extends Widget> void checkAttribute(UiProperty property, TypeInfo<T> type, ResourceScope scope) {
            if (property.owner() == null && property.name().namespace().isEmpty()
                    && property.name().local().equals("Style")) {
                String key = StyleDef.keyOf(property.value());
                if (key == null || key.isEmpty()) {
                    add(property.location(), "Style must be a style key or {StaticResource key}.");
                    return;
                }
                try {
                    scope.resolveStyle(key);
                } catch (IllegalArgumentException failure) {
                    add(property.location(), failure.getMessage());
                }
                return;
            }
            PropertyInfo<? super T, ?> info = null;
            boolean qualifiedOk = property.owner() == null && property.name().namespace().isEmpty();
            if (property.owner() != null) {
                qualifiedOk = schema.type(property.owner()).orElse(null) == type;
                if (!qualifiedOk) {
                    @SuppressWarnings("unchecked")
                    PropertyInfo<? super T, ?> attached = (PropertyInfo<? super T, ?>)
                            schema.attachedProperty(type, property.owner(), property.name().local()).orElse(null);
                    info = attached;
                }
            }
            if (qualifiedOk) info = type.property(property.name().local()).orElse(null);
            if (info == null) {
                if (strictAttributes) {
                    add(property.location(), "Unknown attribute '" + property.displayName() + "' on "
                            + type.name().local() + ".");
                }
                return;
            }
            checkValue(info, property, type, scope);
        }

        private <T extends Widget> void checkValue(PropertyInfo<? super T, ?> info, UiProperty property,
                                                   TypeInfo<T> type, ResourceScope scope) {
            UiValue value = property.value();
            String where = "Cannot apply attribute on " + type.name().local() + ": ";
            if (value instanceof UiValue.Literal literal) {
                convert(info, literal.text(), property, where);
            } else if (value instanceof UiValue.Extension extension) {
                if (extensions.find(extension.name()).isEmpty()) {
                    add(property.location(), where + "Unknown markup extension '" + extension.name().qualified() + "'.");
                    return;
                }
                String name = extension.name().local();
                if (name.equals("Binding") && !info.bindable()) {
                    add(property.location(), where + "Attribute '" + property.displayName()
                            + "' does not support XML binding");
                } else if (name.equals("StaticResource")) {
                    UiValue key = extension.argument("ResourceKey", 0);
                    if (key instanceof UiValue.Literal keyText) {
                        String resource = scope.find(keyText.text().trim()).orElse(null);
                        if (resource == null) {
                            add(property.location(), where + "Resource '" + keyText.text().trim() + "' was not found");
                        } else {
                            convert(info, resource, property, where);
                        }
                    }
                }
            }
        }

        private void convert(PropertyInfo<?, ?> info, String text, UiProperty property, String where) {
            try {
                info.convert(text, LoadContext.DEFAULT);
            } catch (RuntimeException failure) {
                add(property.location(), where + "Cannot parse/apply attribute '" + property.displayName()
                        + "' value '" + MarkupExtensions.write(new UiValue.Literal(text)) + "': " + failure.getMessage());
            }
        }

        private <T extends Widget> void checkElementProperty(UiProperty property, TypeInfo<T> type, ResourceScope scope) {
            String member = property.name().local();
            String shown = (property.owner() != null ? property.owner().qualified() : type.name().local()) + "." + member;
            if (property.owner() != null && schema.type(property.owner()).orElse(null) != type) {
                add(property.location(), "Property element '" + shown + "' cannot be used inside "
                        + type.name().local() + ".");
                descend(property, scope);
                return;
            }
            if (member.equals("Resources")) return;

            SlotInfo<T> slot = type.slot(member).orElse(null);
            if (slot != null) {
                checkSlot(property, shown, scope);
                return;
            }
            PropertyInfo<? super T, ?> info = type.property(member)
                    .or(() -> type.property(Character.toLowerCase(member.charAt(0)) + member.substring(1)))
                    .orElse(null);
            if (info != null && !(property.value() instanceof UiValue.Elements)) {
                checkValue(info, property, type, scope);
                return;
            }
            add(property.location(), "Unknown property element '" + shown + "' on " + type.name().local() + ".");
            descend(property, scope);
        }

        private void checkSlot(UiProperty property, String shown, ResourceScope scope) {
            UiValue value = property.value();
            if (value instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode child) node(child, scope);
                    else if (item instanceof UiContent.Text text && !text.blank()) {
                        add(property.location(), "Property element '" + shown + "' cannot contain text content.");
                    }
                }
            } else if (!(value instanceof UiValue.Literal literal) || !literal.text().isBlank()) {
                add(property.location(), "Property element '" + shown + "' cannot contain text content.");
            }
        }

        private void descend(UiProperty property, ResourceScope scope) {
            if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) if (item instanceof UiNode child) node(child, scope);
            }
        }

        private <T extends Widget> void checkText(UiNode node, TypeInfo<T> type) {
            if (node.text().isBlank()) return;
            boolean textLike = TextWidget.class.isAssignableFrom(type.javaType())
                    || Button.class.isAssignableFrom(type.javaType());
            if (!textLike) add(node.location(), "Widget " + type.name().local() + " cannot contain text content.");
        }

        // -------------------------------------------------------------- ресурсы

        private ResourceScope resources(UiNode node, ResourceScope parent) {
            for (UiProperty property : node.properties()) {
                if (!isElementForm(property) || !property.name().local().equals("Resources")) continue;
                Map<String, String> entries = new LinkedHashMap<>();
                Map<String, StyleDef> styles = new LinkedHashMap<>();
                if (property.value() instanceof UiValue.Elements elements) {
                    for (UiContent item : elements.content()) {
                        if (item instanceof UiNode entry) resource(entry, entries, styles);
                    }
                }
                return entries.isEmpty() && styles.isEmpty() ? parent : new ResourceScope(parent, entries, styles);
            }
            return parent;
        }

        /** Ошибка разбора стиля вместе с позицией: проверка собирает все проблемы, а не останавливается на первой. */
        private static final class StyleProblem extends RuntimeException {
            private final SourceLocation at;

            private StyleProblem(SourceLocation at, String message) {
                super(message);
                this.at = at;
            }
        }

        private void resource(UiNode entry, Map<String, String> entries, Map<String, StyleDef> styles) {
            if (entry.type().local().equals("Style") && (entry.type().namespace().isEmpty()
                    || entry.type().namespace().equals(Schema.UI_NAMESPACE))) {
                try {
                    StyleDef style = StyleDef.read(entry, StyleProblem::new);
                    if (styles.putIfAbsent(style.key(), style) != null) {
                        add(entry.location(), "Duplicate resource key '" + style.key() + "'.");
                    }
                } catch (StyleProblem problem) {
                    add(problem.at, problem.getMessage());
                }
                return;
            }
            if (!XamlNamespaces.isLanguage(entry.type().namespace())) {
                add(entry.location(), "Resources can only hold x: entries such as <x:String>, got '<"
                        + entry.type().qualified() + ">'. Use <Style> for property sets and a component for reusable widgets.");
                return;
            }
            String key = null;
            for (UiProperty property : entry.properties()) {
                if (property.owner() == null && property.name().local().equals("Key")
                        && XamlNamespaces.isLanguage(property.name().namespace())
                        && property.value() instanceof UiValue.Literal literal) {
                    key = literal.text().trim();
                }
            }
            if (key == null || key.isEmpty()) {
                add(entry.location(), "Resource entry <" + entry.type().qualified() + "> requires x:Key.");
            } else if (entries.putIfAbsent(key, entry.text().trim()) != null) {
                add(entry.location(), "Duplicate resource key '" + key + "'.");
            }
        }

        // -------------------------------------------------------------- экран

        boolean isScreen(UiNode root) {
            return (root.type().namespace().isEmpty() || root.type().namespace().equals(Schema.UI_NAMESPACE))
                    && SCREEN_NAMES.contains(ScreenConfig.normalize(root.type().local()))
                    && !schema.contains(root.type());
        }

        void screen(UiNode node) {
            ResourceScope scope = resources(node, ResourceScope.EMPTY);
            screenAttributes(node.properties(), node.type().local(), node.location());
            if (!node.text().isBlank()) add(node.location(), "Screen element cannot contain text content.");

            int roots = 0;
            for (UiProperty property : node.properties()) {
                if (!isElementForm(property)) continue;
                String shown = node.type().local() + "." + property.name().local();
                switch (property.name().local()) {
                    case "Resources" -> { }
                    case "ScaleProvider" -> scaleProvider(property);
                    case "Content" -> {
                        if (property.value() instanceof UiValue.Elements elements) {
                            for (UiContent item : elements.content()) {
                                if (item instanceof UiNode child) {
                                    roots++;
                                    node(child, scope);
                                }
                            }
                        }
                    }
                    default -> add(property.location(), "Unknown property element '" + shown + "' on Screen.");
                }
            }
            for (UiNode child : node.children()) {
                roots++;
                node(child, scope);
            }
            if (roots != 1) add(node.location(), "Screen element must contain exactly one root widget.");
        }

        private void scaleProvider(UiProperty property) {
            UiNode provider = null;
            if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) if (item instanceof UiNode node) provider = node;
            }
            if (provider == null) {
                add(property.location(),
                        "Screen.ScaleProvider must contain one provider element: Fixed, Mutable, Unity or Identity.");
                return;
            }
            Map<String, ScreenConfig.Attr> attributes = screenAttributes(provider.properties(),
                    "Screen.ScaleProvider." + provider.type().local(), provider.location());
            attributes.putIfAbsent("scaleProvider", new ScreenConfig.Attr("scaleProvider",
                    provider.type().local(), provider.location()));
            readScreenConfig(attributes);
        }

        private Map<String, ScreenConfig.Attr> screenAttributes(List<UiProperty> properties, String owner,
                                                                SourceLocation at) {
            Map<String, ScreenConfig.Attr> values = new LinkedHashMap<>();
            for (UiProperty property : properties) {
                if (isElementForm(property)) continue;
                boolean known = property.owner() == null && property.name().namespace().isEmpty()
                        && ScreenConfig.ATTRIBUTE_NAMES.contains(property.name().local());
                if (!known) {
                    if (strictAttributes) {
                        add(property.location(), "Unknown attribute '" + property.displayName() + "' on " + owner + ".");
                    }
                } else if (property.value() instanceof UiValue.Literal literal) {
                    values.put(property.name().local(),
                            new ScreenConfig.Attr(property.displayName(), literal.text(), property.location()));
                } else {
                    add(property.location(), "Screen attribute '" + property.name().local() + "' must be plain text.");
                }
            }
            if (owner.equals("Screen") || SCREEN_NAMES.contains(ScreenConfig.normalize(owner))) readScreenConfig(values);
            return values;
        }

        private void readScreenConfig(Map<String, ScreenConfig.Attr> attributes) {
            try {
                ScreenConfig.read(attributes);
            } catch (XamlLoadException failure) {
                problems.addAll(failure.diagnostics());
            }
        }
    }
}
