package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.text.InlineContentResolverScope;
import dev.sixik.unigui.api.text.InlineContentResolvers;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlBindingSession;
import dev.sixik.unigui.xaml.bind.XamlCommand;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.bind.XamlParameterCommand;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.xaml.SourceLocation;
import dev.sixik.unigui.xaml.UiContent;
import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.UiNode;
import dev.sixik.unigui.xaml.UiParseException;
import dev.sixik.unigui.xaml.UiProperty;
import dev.sixik.unigui.xaml.UiValue;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.format.MarkupExtensions;
import dev.sixik.unigui.xaml.format.UiFormat;
import dev.sixik.unigui.xaml.format.XamlFormat;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.PropertyInfo;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.Template;
import dev.sixik.unigui.xaml.schema.TypeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Превращает {@link UiDocument} в дерево виджетов по {@link Schema}.
 *
 * <p>Загрузчик не знает ни о каком текстовом формате и не парсит XML: он работает с деревом разметки, поэтому
 * одинаково грузит документы, полученные из XAML, из Java-билдера или из скрипта. Он потокобезопасен и не
 * хранит состояния между вызовами; всё изменяемое живёт в объекте одного прогона.</p>
 *
 * <p>Порядок применения: свойства-атрибуты (в порядке схемы, а не документа), затем property-элементы,
 * затем текстовое содержимое, затем вложенные виджеты.</p>
 */
public final class XamlLoader {
    private static final Set<String> ID_NAMES = Set.of("id", "name", "Name");
    private static final Set<String> SCREEN_NAMES = Set.of("screen", "uiscreen", "xmlscreen");

    private final Schema schema;
    private final LoadOptions options;

    /**
     * Создаёт загрузчик.
     *
     * @param schema  реестр типов
     * @param options настройки; {@code null} заменяется {@link LoadOptions#DEFAULT}
     */
    public XamlLoader(Schema schema, LoadOptions options) {
        if (schema == null) throw new IllegalArgumentException("schema must not be null");
        this.schema = schema;
        this.options = options == null ? LoadOptions.DEFAULT : options;
    }

    /**
     * Читает XAML и загружает корневой виджет.
     *
     * @param source текст документа
     * @return корневой виджет
     */
    public Widget load(CharSequence source) {
        return loadScreen(source).root();
    }

    /**
     * Читает XAML и загружает экран.
     *
     * @param source текст документа
     * @return корень, масштаб и сессия привязок
     */
    public LoadedScreen<Widget> loadScreen(CharSequence source) {
        return loadScreen(parse(source));
    }

    /**
     * Загружает экран из готового дерева разметки.
     *
     * @param document документ
     * @return корень, масштаб и сессия привязок
     */
    public LoadedScreen<Widget> loadScreen(UiDocument document) {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        return new Run().screen(document);
    }

    private UiDocument parse(CharSequence source) {
        if (source == null || source.toString().isBlank()) {
            throw new XamlLoadException("XAML source must not be blank.");
        }
        try {
            // Корень имеет глубину 0, поэтому maxDepth = N допускает N + 1 уровней.
            return XamlFormat.INSTANCE.read(source, new UiFormat.ReadOptions(options.maxDepth() + 1));
        } catch (UiParseException failure) {
            throw new XamlLoadException("Cannot parse XAML source: " + failure.reason(), failure.location(), failure);
        }
    }

    // ------------------------------------------------------------------ вспомогательное

    private static XamlLoadException fail(SourceLocation at, String message) {
        return new XamlLoadException(message, at, null);
    }

    private static XamlLoadException fail(SourceLocation at, String message, Throwable cause) {
        return new XamlLoadException(message, at, cause);
    }

    private static boolean isIdProperty(UiProperty property) {
        return property.owner() == null
                && ID_NAMES.contains(property.name().local())
                && (property.name().namespace().isEmpty() || XamlNamespaces.isLanguage(property.name().namespace()));
    }

    private static boolean isElementForm(UiProperty property) {
        return property.form() == UiProperty.Form.ELEMENT || property.value() instanceof UiValue.Elements;
    }

    private static String valueText(UiValue value) {
        return value instanceof UiValue.Elements ? "<elements>" : MarkupExtensions.write(value);
    }

    /**
     * Один прогон загрузки: всё изменяемое состояние живёт здесь.
     */
    private final class Run {
        private final XamlBindingSession session = new XamlBindingSession();
        private final XamlBindingContext bindings = options.bindingContext() == null
                ? XamlBindingContext.empty() : options.bindingContext();
        /**
         * Не final: параметризованная команда подменяет реестр команд на слой с привязанными копиями.
         */
        private LoadContext load = new LoadContext(options.textureResolver(), options.commands());
        private XamlCommandRegistry commandLayer;
        private int boundCommands;
        private final Map<String, Widget> ids = new HashMap<>();
        private final MarkupExtensionRegistry extensions;

        Run() {
            this(options.extensions());
        }

        /**
         * Прогон с особым набором расширений: шаблон получает {@code Item} конкретного элемента.
         */
        Run(MarkupExtensionRegistry extensions) {
            this.extensions = extensions;
        }

        LoadedScreen<Widget> screen(UiDocument document) {
            // Текстовые атрибуты могут содержать маркеры {icon:...}; на время загрузки они разрешаются
            // через резолвер текстур из настроек.
            InlineContentResolverScope inline = InlineContentResolvers.push(InlineContentResolvers.textureMarkers(
                    (id, width, height) -> options.textureResolver().resolve(id, width, height, TextureOptions.defaults())));
            // Setter-ы виджетов перепривязывают текстуры (textureWidth, textureSampling…) тем же резолвером.
            TextureResolution.Scope textures = TextureResolution.push(options.textureResolver());
            try {
                UiNode root = document.root();
                if (isScreen(root)) return readScreen(root);
                Widget widget = readNode(root, ResourceScope.EMPTY, 0);
                return new LoadedScreen<>(widget, UIScaleProvider.IDENTITY, true, session);
            } catch (RuntimeException failure) {
                session.close();
                throw failure;
            } finally {
                textures.close();
                inline.close();
            }
        }

        // -------------------------------------------------------------- виджеты

        private Widget readNode(UiNode node, ResourceScope scope, int depth) {
            if (depth > options.maxDepth()) {
                throw fail(node.location(), "XAML widget tree is deeper than the configured limit of "
                        + options.maxDepth() + ".");
            }
            TypeInfo<?> type = schema.type(node.type()).orElseThrow(
                    () -> fail(node.location(), "Unknown widget type '" + node.type().qualified() + "'."));
            return build(node, type, scope, depth);
        }

        private <T extends Widget> T build(UiNode node, TypeInfo<T> type, ResourceScope parentScope, int depth) {
            T widget = type.create();
            if (options.nodeObserver() != null) options.nodeObserver().created(node, widget);
            ResourceScope scope = resourcesOf(node, parentScope);

            String id = idOf(node);
            if (!id.isEmpty()) {
                if (ids.putIfAbsent(id, widget) != null) {
                    throw fail(node.location(), "Duplicate widget id '" + id + "'.");
                }
                widget.id(id);
            }

            applyAttributes(node, type, widget, scope);
            applyElementProperties(node, type, widget, scope, depth);
            applyText(node, type, widget);

            for (UiNode childNode : node.children()) {
                Widget child = readNode(childNode, scope, depth + 1);
                try {
                    type.addChild(widget, child);
                } catch (RuntimeException failure) {
                    throw fail(childNode.location(), failure.getMessage(), failure);
                }
            }
            try {
                type.finishLoad(widget, load);
            } catch (XamlLoadException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw fail(node.location(), "Cannot finish loading " + type.name().local() + ": "
                        + failure.getMessage(), failure);
            }
            return widget;
        }

        private String idOf(UiNode node) {
            String id = null;
            for (UiProperty property : node.properties()) {
                if (!isIdProperty(property)) continue;
                if (!(property.value() instanceof UiValue.Literal literal)) {
                    throw fail(property.location(), "The id of " + node.type().qualified() + " must be plain text.");
                }
                String value = literal.text().trim();
                if (id != null && !id.equals(value)) {
                    throw fail(property.location(), "Conflicting id attributes on " + node.type().qualified() + ".");
                }
                id = value;
            }
            return id == null ? "" : id;
        }

        // -------------------------------------------------------------- атрибуты

        private <T extends Widget> void applyAttributes(UiNode node, TypeInfo<T> type, T widget, ResourceScope scope) {
            List<Pending<T>> pending = new ArrayList<>();
            for (UiProperty property : attributesWithStyle(node, type, widget, scope)) {
                PropertyInfo<? super T, ?> info = resolve(type, property);
                if (info == null) {
                    if (options.strictAttributes()) {
                        throw fail(property.location(), "Unknown attribute '" + property.displayName()
                                + "' on " + type.name().local() + ".");
                    }
                    continue;
                }
                pending.add(new Pending<>(info, property, type.order(info.name())));
            }
            if (pending.size() > 1) pending.sort(java.util.Comparator.comparingInt(Pending::order));
            for (Pending<T> item : pending) applyProperty(widget, type, item.info, item.property, scope);
        }

        /**
         * Атрибуты узла вместе со значениями его стиля: сначала значения стиля, затем явные атрибуты, которые
         * заменяют одноимённые значения стиля.
         */
        private <T extends Widget> Iterable<UiProperty> attributesWithStyle(UiNode node, TypeInfo<T> type, T widget,
                                                                            ResourceScope scope) {
            UiProperty reference = null;
            List<UiProperty> own = new ArrayList<>();
            for (UiProperty property : node.properties()) {
                if (isElementForm(property) || isIdProperty(property)) continue;
                if (property.owner() == null && property.name().namespace().isEmpty()
                        && property.name().local().equals("Style")) {
                    reference = property;
                    continue;
                }
                own.add(property);
            }
            if (reference == null) return own;

            String key = StyleDef.keyOf(reference.value());
            if (key == null || key.isEmpty()) {
                throw fail(reference.location(), "Style must be a style key or {StaticResource key}.");
            }
            List<StyleDef.Setter> setters;
            try {
                setters = scope.resolveStyle(key);
                StyleDef direct = scope.findStyle(key).orElseThrow();
                if (direct.targetType() != null) {
                    TypeInfo<?> target = schema.type(direct.targetType()).orElse(null);
                    if (target == null || !target.javaType().isInstance(widget)) {
                        throw new IllegalArgumentException("Style '" + key + "' targets " + direct.targetType().local()
                                + " but is applied to " + type.name().local());
                    }
                }
            } catch (IllegalArgumentException failure) {
                throw fail(reference.location(), failure.getMessage());
            }
            Map<String, UiProperty> merged = new LinkedHashMap<>();
            for (StyleDef.Setter setter : setters) {
                merged.put(setter.name(), new UiProperty(UiName.of(setter.name()), null, setter.value(),
                        UiProperty.Form.ATTRIBUTE, setter.location()));
            }
            for (UiProperty property : own) merged.put(property.displayName(), property);
            return merged.values();
        }

        /**
         * Находит свойство типа для записи в документе; {@code null}, если такого нет.
         */
        private <T extends Widget> PropertyInfo<? super T, ?> resolve(TypeInfo<T> type, UiProperty property) {
            if (property.owner() != null) {
                // Owner.Prop: либо свойство самого типа, либо attached-свойство (Grid.Row на потомке сетки).
                if (schema.type(property.owner()).orElse(null) != type) {
                    @SuppressWarnings("unchecked")
                    PropertyInfo<? super T, ?> attached = (PropertyInfo<? super T, ?>)
                            schema.attachedProperty(type, property.owner(), property.name().local()).orElse(null);
                    return attached;
                }
            } else if (!property.name().namespace().isEmpty()) {
                return null;
            }
            return type.property(property.name().local()).orElse(null);
        }

        private <T extends Widget> void applyProperty(T widget, TypeInfo<T> type, PropertyInfo<? super T, ?> info,
                                                      UiProperty property, ResourceScope scope) {
            try {
                UiValue value = property.value();
                if (XamlSources.worthKeeping(value, info.readable())) {
                    XamlSources.record(widget, info.name(), value);
                }
                if (value instanceof UiValue.Literal literal) {
                    info.apply(widget, literal.text(), load);
                } else if (value instanceof UiValue.Extension extension) {
                    MarkupExtension handler = extensions.find(extension.name()).orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Unknown markup extension '" + extension.name().qualified() + "'."));
                    handler.apply(new ExtensionContext(scope, bindings, session, new Host(scope)), extension,
                            new Target<>(widget, info, property.displayName()));
                } else {
                    throw new IllegalArgumentException("Property '" + property.displayName()
                            + "' cannot contain nested elements.");
                }
            } catch (XamlLoadException failure) {
                throw failure;
            } catch (RuntimeException failure) {
                throw fail(property.location(), "Cannot apply attribute on " + type.name().local()
                        + ": Cannot parse/apply attribute '" + property.displayName()
                        + "' value '" + valueText(property.value()) + "': " + failure.getMessage(), failure);
            }
        }

        // -------------------------------------------------------------- property-элементы

        private <T extends Widget> void applyElementProperties(UiNode node, TypeInfo<T> type, T widget,
                                                               ResourceScope scope, int depth) {
            for (UiProperty property : node.properties()) {
                if (!isElementForm(property)) continue;
                String member = property.name().local();
                String shown = (property.owner() != null ? property.owner().qualified() : type.name().local())
                        + "." + member;
                if (property.owner() != null && schema.type(property.owner()).orElse(null) != type) {
                    throw fail(property.location(), "Property element '" + shown + "' cannot be used inside "
                            + type.name().local() + ".");
                }
                if (member.equals("Resources")) continue;

                SlotInfo<T> slot = type.slot(member).orElse(null);
                if (slot != null) {
                    feedSlot(widget, slot, property, shown, scope, depth);
                    continue;
                }
                // В элементной форме принято писать имена с заглавной (Label.Text), а свойства у нас camelCase.
                PropertyInfo<? super T, ?> info = type.property(member)
                        .or(() -> type.property(Character.toLowerCase(member.charAt(0)) + member.substring(1)))
                        .orElse(null);
                if (info != null && !(property.value() instanceof UiValue.Elements)) {
                    applyProperty(widget, type, info, property, scope);
                    continue;
                }
                throw fail(property.location(), "Unknown property element '" + shown + "' on "
                        + type.name().local() + ".");
            }
        }

        private <T extends Widget> void feedSlot(T widget, SlotInfo<T> slot, UiProperty property, String shown,
                                                 ResourceScope scope, int depth) {
            if (slot.template() != null) {
                feedTemplate(widget, slot, property, shown, scope);
                return;
            }
            UiValue value = property.value();
            if (value instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode childNode) {
                        Widget child = readNode(childNode, scope, depth + 1);
                        try {
                            slot.sink().add(widget, child);
                        } catch (RuntimeException failure) {
                            throw fail(childNode.location(), failure.getMessage(), failure);
                        }
                    } else if (item instanceof UiContent.Text text && !text.blank()) {
                        throw fail(property.location(), "Property element '" + shown + "' cannot contain text content.");
                    }
                }
            } else if (!(value instanceof UiValue.Literal literal) || !literal.text().isBlank()) {
                throw fail(property.location(), "Property element '" + shown + "' cannot contain text content.");
            }
        }

        // -------------------------------------------------------------- шаблоны

        /**
         * Передаёт слоту шаблон вместо готового виджета. Шаблон сразу пробно создаётся один раз с пустой подстановкой
         * {@code {Item}}, чтобы опечатки в нём находились при загрузке документа, а не при показе первой строки.
         */
        private <T extends Widget> void feedTemplate(T widget, SlotInfo<T> slot, UiProperty property, String shown,
                                                     ResourceScope scope) {
            UiNode templateNode = null;
            if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode node) {
                        if (templateNode != null) {
                            throw fail(node.location(), "Property element '" + shown + "' takes exactly one element.");
                        }
                        templateNode = node;
                    } else if (item instanceof UiContent.Text text && !text.blank()) {
                        throw fail(property.location(), "Property element '" + shown + "' cannot contain text content.");
                    }
                }
            }
            if (templateNode == null) {
                throw fail(property.location(), "Property element '" + shown + "' takes exactly one element.");
            }
            final UiNode node = templateNode;
            Template template = new Template(node, property.location(), overrides -> {
                MarkupExtensionRegistry registry = extensions.copy();
                overrides.forEach(registry::replace);
                return new Run(registry).instantiate(node, scope);
            });
            template.instantiate(Map.of("Item", ItemExtension.probe())).close();
            try {
                slot.template().accept(widget, template);
            } catch (RuntimeException failure) {
                throw fail(property.location(), failure.getMessage(), failure);
            }
        }

        /**
         * Создаёт виджет из узла шаблона; привязки экземпляра живут в сессии этого прогона.
         */
        Template.Instance instantiate(UiNode node, ResourceScope scope) {
            InlineContentResolverScope inline = InlineContentResolvers.push(InlineContentResolvers.textureMarkers(
                    (id, width, height) -> options.textureResolver().resolve(id, width, height, TextureOptions.defaults())));
            TextureResolution.Scope textures = TextureResolution.push(options.textureResolver());
            try {
                Widget root = readNode(node, scope, 0);
                return new Template.Instance(root, session::close);
            } catch (RuntimeException failure) {
                session.close();
                throw failure;
            } finally {
                textures.close();
                inline.close();
            }
        }

        // -------------------------------------------------------------- текст

        private <T extends Widget> void applyText(UiNode node, TypeInfo<T> type, T widget) {
            String text = node.text().trim();
            if (text.isEmpty()) return;
            boolean textLike = widget instanceof TextWidget || widget instanceof Button;
            if (!textLike) {
                throw fail(node.location(), "Widget " + type.name().local() + " cannot contain text content.");
            }
            for (UiProperty property : node.properties()) {
                if (property.name().local().equals("text")) return;
            }
            if (widget instanceof TextWidget textWidget) textWidget.text(text);
            else if (widget instanceof Button button) button.text(text);
        }

        // -------------------------------------------------------------- ресурсы

        private ResourceScope resourcesOf(UiNode node, ResourceScope parent) {
            for (UiProperty property : node.properties()) {
                if (!isElementForm(property) || !property.name().local().equals("Resources")) continue;
                Map<String, String> entries = new LinkedHashMap<>();
                Map<String, StyleDef> styles = new LinkedHashMap<>();
                if (property.value() instanceof UiValue.Elements elements) {
                    for (UiContent item : elements.content()) {
                        if (item instanceof UiNode entry) readResource(entry, entries, styles);
                        else if (item instanceof UiContent.Text text && !text.blank()) {
                            throw fail(property.location(), "Property element 'Resources' cannot contain text content.");
                        }
                    }
                } else if (!(property.value() instanceof UiValue.Literal literal) || !literal.text().isBlank()) {
                    throw fail(property.location(), "Property element 'Resources' cannot contain text content.");
                }
                return entries.isEmpty() && styles.isEmpty() ? parent : new ResourceScope(parent, entries, styles);
            }
            return parent;
        }

        private void readResource(UiNode entry, Map<String, String> entries, Map<String, StyleDef> styles) {
            if (entry.type().local().equals("Style") && (entry.type().namespace().isEmpty()
                    || entry.type().namespace().equals(Schema.UI_NAMESPACE))) {
                StyleDef style = StyleDef.read(entry, XamlLoader::fail);
                if (styles.putIfAbsent(style.key(), style) != null) {
                    throw fail(entry.location(), "Duplicate resource key '" + style.key() + "'.");
                }
                return;
            }
            if (!XamlNamespaces.isLanguage(entry.type().namespace())) {
                throw fail(entry.location(), "Resources can only hold x: entries such as <x:String>, got '<"
                        + entry.type().qualified() + ">'. Use <Style> for property sets and a component for reusable widgets.");
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
                throw fail(entry.location(), "Resource entry <" + entry.type().qualified() + "> requires x:Key.");
            }
            if (entries.putIfAbsent(key, entry.text().trim()) != null) {
                throw fail(entry.location(), "Duplicate resource key '" + key + "'.");
            }
        }

        // -------------------------------------------------------------- экран

        private boolean isScreen(UiNode root) {
            UiName name = root.type();
            return (name.namespace().isEmpty() || name.namespace().equals(Schema.UI_NAMESPACE))
                    && SCREEN_NAMES.contains(ScreenConfig.normalize(name.local()))
                    && !schema.contains(name);
        }

        private LoadedScreen<Widget> readScreen(UiNode node) {
            ResourceScope scope = resourcesOf(node, ResourceScope.EMPTY);
            ScreenConfig config = ScreenConfig.read(screenAttributes(node.properties(), node.type().local(), true));

            if (!node.text().isBlank()) {
                throw fail(node.location(), "Screen element cannot contain text content.");
            }
            Widget root = null;
            for (UiProperty property : node.properties()) {
                if (!isElementForm(property)) continue;
                String shown = node.type().local() + "." + property.name().local();
                if (property.owner() != null && !SCREEN_NAMES.contains(ScreenConfig.normalize(property.owner().local()))) {
                    throw fail(property.location(), "Property element '" + shown + "' cannot be used inside Screen.");
                }
                switch (property.name().local()) {
                    case "Resources" -> {
                    }
                    case "ScaleProvider" -> config = config.merge(readScaleProvider(property));
                    case "Content" -> root = readScreenContent(property, root, scope);
                    default -> throw fail(property.location(), "Unknown property element '" + shown + "' on Screen.");
                }
            }
            for (UiNode child : node.children()) {
                if (root != null) throw fail(child.location(), "Screen element must contain exactly one root widget.");
                root = readNode(child, scope, 0);
            }
            if (root == null) throw fail(node.location(), "Screen element must contain exactly one root widget.");
            return new LoadedScreen<>(root, config.scaleProvider(), config.scaleWithMinecraftGui(), session);
        }

        private Widget readScreenContent(UiProperty property, Widget current, ResourceScope scope) {
            Widget root = current;
            if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode child) {
                        if (root != null)
                            throw fail(child.location(), "Screen element must contain exactly one root widget.");
                        root = readNode(child, scope, 0);
                    } else if (item instanceof UiContent.Text text && !text.blank()) {
                        throw fail(property.location(), "Property element 'Screen.Content' cannot contain text content.");
                    }
                }
            } else if (!(property.value() instanceof UiValue.Literal literal) || !literal.text().isBlank()) {
                throw fail(property.location(), "Property element 'Screen.Content' cannot contain text content.");
            }
            return root;
        }

        /**
         * {@code <Screen.ScaleProvider><Mutable scale="3"/></Screen.ScaleProvider>}.
         */
        private ScreenConfig readScaleProvider(UiProperty property) {
            UiNode provider = null;
            if (property.value() instanceof UiValue.Elements elements) {
                for (UiContent item : elements.content()) {
                    if (item instanceof UiNode node) {
                        if (provider != null) {
                            throw fail(node.location(), "Screen.ScaleProvider must contain exactly one provider element.");
                        }
                        provider = node;
                    }
                }
            }
            if (provider == null) {
                throw fail(property.location(),
                        "Screen.ScaleProvider must contain one provider element: Fixed, Mutable, Unity or Identity.");
            }
            Map<String, ScreenConfig.Attr> attributes =
                    screenAttributes(provider.properties(), "Screen.ScaleProvider." + provider.type().local(), true);
            if (!attributes.containsKey("scaleProvider") && !attributes.containsKey("scaleProviderType")
                    && !attributes.containsKey("type")) {
                attributes.put("scaleProvider", new ScreenConfig.Attr("scaleProvider",
                        provider.type().local(), provider.location()));
            }
            return ScreenConfig.read(attributes);
        }

        private Map<String, ScreenConfig.Attr> screenAttributes(List<UiProperty> properties, String owner,
                                                                boolean strictOwner) {
            Map<String, ScreenConfig.Attr> values = new LinkedHashMap<>();
            for (UiProperty property : properties) {
                if (isElementForm(property)) continue;
                String name = property.name().local();
                boolean known = property.owner() == null && property.name().namespace().isEmpty()
                        && ScreenConfig.ATTRIBUTE_NAMES.contains(name);
                if (!known) {
                    if (options.strictAttributes() && strictOwner) {
                        throw fail(property.location(), "Unknown attribute '" + property.displayName() + "' on " + owner + ".");
                    }
                    continue;
                }
                if (!(property.value() instanceof UiValue.Literal literal)) {
                    throw fail(property.location(), "Screen attribute '" + name + "' must be plain text.");
                }
                values.put(name, new ScreenConfig.Attr(property.displayName(), literal.text(), property.location()));
            }
            return values;
        }

        // -------------------------------------------------------------- возможности для расширений

        /**
         * Даёт расширениям вычислять вложенные значения и привязывать параметры к командам.
         */
        private final class Host implements ExtensionHost {
            private final ResourceScope scope;

            private Host(ResourceScope scope) {
                this.scope = scope;
            }

            @Override
            public Object evaluate(UiValue value) {
                if (value instanceof UiValue.Literal literal) return literal.text();
                if (!(value instanceof UiValue.Extension extension)) {
                    throw new IllegalArgumentException("The value cannot contain nested elements");
                }
                if (extension.name().local().equals("Binding")) {
                    throw new IllegalArgumentException(
                            "A live Binding cannot be used here; use {Item ...}, {Param ...} or {StaticResource ...}");
                }
                MarkupExtension handler = extensions.find(extension.name()).orElseThrow(
                        () -> new IllegalArgumentException(
                                "Unknown markup extension '" + extension.name().qualified() + "'."));
                Capture capture = new Capture();
                handler.apply(new ExtensionContext(scope, bindings, session, this), extension, capture);
                return capture.value;
            }

            @Override
            public String bindCommand(String command, Object parameter) {
                XamlCommand base = load.commands().command(command).orElseThrow(
                        () -> new IllegalArgumentException("Unknown XML command '" + command + "'"));
                if (!(base instanceof XamlParameterCommand withParameter)) {
                    throw new IllegalArgumentException("Command '" + command + "' does not take a parameter; "
                            + "register it as a XamlParameterCommand");
                }
                if (commandLayer == null) {
                    XamlCommandRegistry layer = XamlCommandRegistry.empty();
                    for (String name : load.commands().names()) {
                        layer.register(name, load.commands().command(name).orElseThrow());
                    }
                    commandLayer = layer;
                    load = new LoadContext(options.textureResolver(), layer);
                }
                String bound = command + "#" + (++boundCommands);
                commandLayer.register(bound, (source, event) -> withParameter.execute(source, event, parameter));
                return bound;
            }
        }

        /**
         * Цель, которая не пишет в виджет, а запоминает значение: так вычисляются вложенные расширения.
         */
        private static final class Capture implements PropertyTarget {
            private Object value;

            @Override
            public Widget widget() {
                return null;
            }

            @Override
            public String name() {
                return "Parameter";
            }

            @Override
            public String describe() {
                return "command parameter";
            }

            @Override
            public boolean bindable() {
                return true;
            }

            @Override
            public Class<?> valueType() {
                return Object.class;
            }

            @Override
            public Class<?> bindingType() {
                return Object.class;
            }

            @Override
            public void setText(String text) {
                this.value = text;
            }

            @Override
            public void setValue(Object value) {
                this.value = value;
            }

            @Override
            public dev.sixik.unigui.api.event.EventSubscription observe(java.util.function.Consumer<Object> onChange) {
                return null;
            }
        }

        // -------------------------------------------------------------- цель для расширений

        private final class Target<T extends Widget> implements PropertyTarget {
            private final T widget;
            private final PropertyInfo<? super T, ?> info;
            private final String name;

            private Target(T widget, PropertyInfo<? super T, ?> info, String name) {
                this.widget = widget;
                this.info = info;
                this.name = name;
            }

            @Override
            public Widget widget() {
                return widget;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String describe() {
                return widget.getClass().getSimpleName() + "." + name;
            }

            @Override
            public boolean bindable() {
                return info.bindable();
            }

            @Override
            public Class<?> valueType() {
                return info.valueType();
            }

            @Override
            public Class<?> bindingType() {
                return info.bindingType();
            }

            @Override
            public void setText(String text) {
                info.apply(widget, text, load);
            }

            @Override
            public void setValue(Object value) {
                info.set(widget, value, load);
            }

            @Override
            public dev.sixik.unigui.api.event.EventSubscription observe(java.util.function.Consumer<Object> onChange) {
                return info.observe(widget, onChange);
            }
        }
    }

    private record Pending<T extends Widget>(PropertyInfo<? super T, ?> info, UiProperty property, int order) {
    }
}
