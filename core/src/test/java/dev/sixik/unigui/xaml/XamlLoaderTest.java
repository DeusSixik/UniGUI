package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.SizeUnit;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.text.InlineContentSpan;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.xaml.bind.XamlBindingContext;
import dev.sixik.unigui.xaml.bind.XamlBindingStatus;
import dev.sixik.unigui.xaml.bind.XamlCommandRegistry;
import dev.sixik.unigui.xaml.bind.XamlMutableObservableValue;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.widgets.display.ImageView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.graph.GraphView;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.navigation.Accordion;
import dev.sixik.unigui.widgets.navigation.Breadcrumb;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.PageView;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.widgets.navigation.TreeList;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Поведение загрузчика. Основа — проверки прежнего загрузчика ({@code XmlLegacyBehaviorTest}), перенесённые на
 * новый API с теми же ожиданиями, плюс проверки новых возможностей.
 */
final class XamlLoaderTest {
    private static final String X = "xmlns:x=\"https://unigui.dev/xaml\"";

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.01f;
    }

    private static XamlLoadException fails(String xaml, String part) {
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.load(xaml), xaml);
        assertTrue(failure.getMessage().contains(part), "expected '" + part + "' in: " + failure.getMessage());
        return failure;
    }

    private static <T extends Widget> void fails(String xaml, Class<T> type, String part) {
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.load(xaml, type), xaml);
        assertTrue(failure.getMessage().contains(part), "expected '" + part + "' in: " + failure.getMessage());
    }

    // ------------------------------------------------------------------ перенесено из прежних проверок

    @Test
    void typedRootAttributesAndChildren() {
        VBox root = Xaml.load("""
                <VBox id="root" spacing="6" width="240" height="120" alignItems="center" padding="10 8 10 8">
                    <Label id="title" text="Video Settings" color="#11223344" />
                    <Button id="apply" text="Apply" enabled="false" />
                    <Slider id="gamma" value="5" step="0.5" max="10" min="0" />
                    <TextArea id="notes" text="Line A&#10;Line B" visibleLines="3" />
                </VBox>
                """, VBox.class);

        assertEquals("root", root.id());
        assertEquals(4, root.children().size(), "children attach in document order");
        assertTrue(near(root.spacing(), 6.0f));
        assertEquals(SizeUnit.PIXELS, root.layoutStyle().width().unit());
        assertTrue(near(root.layoutStyle().width().value(), 240.0f));
        assertTrue(near(root.layoutStyle().height().value(), 120.0f));
        assertEquals(Align.CENTER, root.layoutStyle().alignItems(), "enums parse case-insensitively");
        assertTrue(near(root.layoutStyle().padding().top(), 10.0f) && near(root.layoutStyle().padding().right(), 8.0f)
                && near(root.layoutStyle().padding().bottom(), 10.0f) && near(root.layoutStyle().padding().left(), 8.0f),
                "CSS-like top/right/bottom/left");

        Label title = Xaml.get(root, "title", Label.class);
        assertEquals("Video Settings", title.text());
        assertTrue(near(title.color().r(), 0x11 / 255.0f) && near(title.color().a(), 0x44 / 255.0f));

        Button apply = Xaml.get(root, "apply", Button.class);
        assertTrue(apply.text().equals("Apply") && !apply.enabled());

        Slider gamma = Xaml.get(root, "gamma", Slider.class);
        assertTrue(near(gamma.min(), 0.0f) && near(gamma.max(), 10.0f) && near(gamma.value(), 5.0f),
                "min/max must be applied before value regardless of the attribute order in the document");

        TextArea notes = Xaml.get(root, "notes", TextArea.class);
        assertEquals("Line A\nLine B", notes.text());
        assertEquals(3, notes.visibleLines());
        assertTrue(Xaml.find(root, "missing").isEmpty());

        VBox surfaced = Xaml.load("""
                <VBox background="#11223344" border="#FFEEDDCC" borderWidth="2" radius="3">
                    <Label id="inner" text="Hi" />
                </VBox>
                """, VBox.class);
        assertTrue(surfaced.boxVisualEnabled() && surfaced.backgroundVisible() && surfaced.borderVisible());
        assertTrue(near(surfaced.background().r(), 0x11 / 255.0f) && near(surfaced.background().a(), 0x44 / 255.0f));
        assertTrue(near(surfaced.borderWidth(), 2.0f) && near(surfaced.radius(), 3.0f));
        assertEquals(1, surfaced.children().size());
    }

    @Test
    void streamAndResourceLoading() {
        Box fromResource = Xaml.loadResource("assets/unigui/xml/xml_demo.xml", Box.class);
        assertEquals("reactorPanel", fromResource.id());
        assertTrue(Xaml.get(fromResource, "logText", TextWidget.class).text().contains("Core initialized"));
        assertTrue(Xaml.get(fromResource, "safeMode", Checkbox.class).checked());
        assertEquals(4, Xaml.get(fromResource, "metricCards", WrapPanel.class).children().size());
        assertTrue(near(Xaml.get(fromResource, "temperatureBar", ProgressBar.class).value(), 58.0f));
        assertEquals(66.0f, Xaml.get(fromResource, "powerLimit", Slider.class).value());

        VBox overview = Xaml.loadResource("assets/unigui/xml/overview.xml", VBox.class);
        assertEquals("Overview", Xaml.get(overview, "overviewTitle", Label.class).text());
        assertTrue(Xaml.get(overview, "quickFactoriesSlot", VBox.class).children().isEmpty());

        XamlLoadException missing = assertThrows(XamlLoadException.class,
                () -> Xaml.loadResource("assets/unigui/xml/missing.xml"));
        assertTrue(missing.getMessage().contains("'assets/unigui/xml/missing.xml' was not found"));
    }

    @Test
    void loaderOptions() {
        fails("<Label futureEditorAttr=\"todo\" text=\"Strict\" />", "Unknown attribute 'futureEditorAttr' on Label");

        Label lenient = Xaml.load("<Label id=\"lenient\" futureEditorAttr=\"todo\" text=\"Lenient\" />",
                Label.class, LoadOptions.lenient());
        assertTrue(lenient.id().equals("lenient") && lenient.text().equals("Lenient"));

        XamlLoadException deep = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<VBox><VBox><Label text=\"Too deep\" /></VBox></VBox>", LoadOptions.DEFAULT.maxDepth(1)));
        assertTrue(deep.getMessage().contains("deeper than the configured limit"), deep.getMessage());
        // Ровно на пределе — загружается.
        assertNotNull(Xaml.load("<VBox><VBox><Label text=\"ok\" /></VBox></VBox>", LoadOptions.DEFAULT.maxDepth(2)));
    }

    @Test
    void screenScaleProviders() {
        LoadedScreen<VBox> fixed = Xaml.loadScreen("""
                <Screen uiScale="2.5" scaleWithMinecraftGui="false">
                    <VBox id="screenRoot" spacing="4" />
                </Screen>
                """, VBox.class);
        assertEquals("screenRoot", fixed.root().id());
        assertTrue(near(fixed.scaleProvider().scale(), 2.5f));
        assertFalse(fixed.scaleWithMinecraftGui());

        LoadedScreen<Box> unity = Xaml.loadScreen("""
                <UIScreen scaleProvider="unity" referenceResolution="960x540" match="balanced"
                          userScale="2" scaleRange="0.5 8">
                    <Box id="panel" />
                </UIScreen>
                """, Box.class);
        UnityLikeUIScaleProvider provider = assertInstanceOf(UnityLikeUIScaleProvider.class, unity.scaleProvider());
        provider.viewportSize(1920.0f, 1080.0f);
        assertTrue(near(provider.scale(), 4.0f));
        assertTrue(unity.scaleWithMinecraftGui(), "multiplies by the Minecraft GUI scale by default");

        // Объектная форма: property-элемент ScaleProvider содержит элемент провайдера.
        LoadedScreen<Label> property = Xaml.loadScreen("""
                <Screen independentScale="true">
                    <Screen.ScaleProvider><Mutable scale="3"/></Screen.ScaleProvider>
                    <Screen.Content>
                        <Label id="caption" text="Scaled" />
                    </Screen.Content>
                </Screen>
                """, Label.class);
        assertEquals("Scaled", property.root().text());
        assertTrue(near(property.scaleProvider().scale(), 3.0f));
        assertFalse(property.scaleWithMinecraftGui());

        LoadedScreen<Label> plain = Xaml.loadScreen("<Label id=\"plain\" text=\"Plain\" />", Label.class);
        assertSame(UIScaleProvider.IDENTITY, plain.scaleProvider());

        fails("<Screen><Screen.ScaleProvider/></Screen>", "must contain one provider element");
        fails("<Screen uiScale=\"abc\"><Label/></Screen>", "Cannot apply attribute on Screen");
        fails("<Screen bogus=\"1\"><Label/></Screen>", "Unknown attribute 'bogus' on Screen");
        fails("<Screen><Label/><Label/></Screen>", "exactly one root widget");
        fails("<Screen/>", "exactly one root widget");
    }

    @Test
    void xNameNameAndTextContent() {
        Widget root = Xaml.load("""
                <VBox xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" x:Name="root">
                    <Label x:Name="title">Hello from XML</Label>
                </VBox>
                """);
        assertEquals("root", root.id(), "x:Name works as an id alias (WPF language namespace)");
        assertEquals("Hello from XML", Xaml.get(root, "title", Label.class).text());

        assertEquals("caption", Xaml.load("<Label Name=\"caption\" text=\"Named\" />", Label.class).id());
        assertEquals("own", Xaml.load("<Label " + X + " x:Name=\"own\" />", Label.class).id(),
                "the UniGUI language namespace is accepted too");
        fails("<VBox id=\"a\" Name=\"b\" />", "Conflicting id attributes on VBox");
        fails("<VBox>text<Label/></VBox>", "cannot contain text content");
    }

    @Test
    void aliasesAndPropertyElements() {
        Box card = Xaml.load("""
                <Border id="card" background="#202936FF" border="#7AA2FFFF" borderWidth="1">
                    <Border.Children>
                        <Label id="caption" text="Alias card" />
                    </Border.Children>
                </Border>
                """, Box.class);
        assertEquals("card", card.id());
        assertEquals(1, card.children().size(), "no wrapper widget is created for the property element");
        assertEquals("Alias card", Xaml.get(card, "caption", Label.class).text());

        ScrollView scroll = Xaml.load("""
                <ScrollViewer id="scroll" width="120" height="48" overflowY="auto">
                    <ScrollViewer.Content>
                        <VBox id="content" spacing="3">
                            <VBox.Children>
                                <Label id="row" text="Wrapped row" />
                            </VBox.Children>
                        </VBox>
                    </ScrollViewer.Content>
                </ScrollViewer>
                """, ScrollView.class);
        assertInstanceOf(VBox.class, scroll.content());
        assertEquals(3.0f, Xaml.get(scroll, "content", VBox.class).spacing());
        assertEquals("Wrapped row", Xaml.get(scroll, "row", Label.class).text());

        ScrollView direct = Xaml.load("""
                <ScrollView id="scroll"><VBox id="content"><Label id="row" text="Row"/></VBox></ScrollView>
                """, ScrollView.class);
        assertEquals("Row", Xaml.get(direct, "row", Label.class).text());
    }

    @Test
    void commandsFromTheLoadContext() {
        int[] clicks = {0};
        String[] seen = {""};
        XamlCommandRegistry commands = XamlCommandRegistry.empty().register("video.apply", (source, event) -> {
            clicks[0]++;
            seen[0] = source.id() + ":" + event.type().id();
        });
        LoadOptions options = LoadOptions.DEFAULT.commands(commands);

        Button apply = Xaml.load("<Button id=\"apply\" text=\"Apply\" onClick=\"video.apply\" />", Button.class, options);
        apply.click();
        assertEquals(1, clicks[0]);
        assertEquals("apply:button.click", seen[0]);

        Button explicit = Xaml.load("<Button id=\"b2\" onClick=\"{Command video.apply}\" />", Button.class, options);
        explicit.click();
        assertEquals(2, clicks[0], "{Command} is an explicit spelling of the same action");

        fails("<Button onClick=\"video.missing\" />", Button.class, "Unknown XML command 'video.missing'");
        fails("<Button onClick=\"\" />", Button.class, "onClick command name must not be blank");
        fails("<Button onClick=\"{Command}\" />", Button.class, "Command requires a name");
    }

    @Test
    void textureAttributes() {
        Box textured = Xaml.load("""
                <Box id="textured"
                     backgroundTexture="test_mod:uniformclouds-1"
                     backgroundTextureWidth="256" backgroundTextureHeight="128"
                     backgroundTextureFit="cover" backgroundTextureTint="#80A0C0CC"
                     backgroundTextureSource="0.25 0 0.5 1"
                     backgroundTextureSampling="linear" backgroundTextureMipmaps="true" />
                """, Box.class);
        assertEquals("test_mod:uniformclouds-1", textured.backgroundTexture().id());
        assertTrue(textured.backgroundTexture().width() == 256 && textured.backgroundTexture().height() == 128);
        assertEquals(TextureFilter.LINEAR, textured.backgroundTexture().options().minFilter());
        assertTrue(textured.backgroundTexture().options().mipmaps());
        assertEquals(ImageFit.COVER, textured.backgroundTextureFit());
        assertTrue(near(textured.backgroundTextureTint().r(), 0x80 / 255.0f));
        assertTrue(near(textured.backgroundTextureSource().x(), 0.25f) && near(textured.backgroundTextureSource().width(), 0.5f));

        Box sliced = Xaml.load("""
                <Box backgroundTexture="test_mod:panel" backgroundTextureWidth="20" backgroundTextureHeight="20"
                     backgroundTextureFit="nine-slice" sliceBorders="6" />
                """, Box.class);
        assertEquals(ImageFit.NINE_SLICE, sliced.backgroundTextureFit());
        assertEquals(new SliceBorders(6.0f, 6.0f, 6.0f, 6.0f), sliced.sliceBorders());

        ImageView image = Xaml.load("""
                <Image id="preview" texture="minecraft:textures/item/diamond.png" textureWidth="32" textureHeight="32"
                       fit="contain" tint="#FFFFFFFF" source="0 0 1 1" radius="2" textureSampling="nearest" />
                """, ImageView.class);
        assertEquals("minecraft:textures/item/diamond.png", image.texture().id());
        assertTrue(image.texture().width() == 32 && image.texture().height() == 32);
        assertEquals(ImageFit.CONTAIN, image.fit());
        assertTrue(near(image.radius(), 2.0f));
        assertEquals(TextureFilter.NEAREST, image.texture().options().minFilter());

        Object nativeTexture = new Object();
        LoadOptions resolver = LoadOptions.DEFAULT.textureResolver((id, width, height, options) ->
                new SimpleTextureHandle(id, width, height, nativeTexture, options));
        ImageView resolved = Xaml.load("""
                <Image texture="test_mod:resolved" textureWidth="48" textureHeight="24" textureSampling="linear" />
                """, ImageView.class, resolver);
        assertSame(nativeTexture, resolved.texture().nativeHandle(), "the resolver hook supplies runtime handles");
        assertTrue(resolved.texture().width() == 48 && resolved.texture().height() == 24);
        assertEquals(TextureFilter.LINEAR, resolved.texture().options().minFilter());

        TextWidget inline = Xaml.load("<Text text=\"A {icon:test_mod:inline_text@9x7} B\" />", TextWidget.class, resolver);
        assertEquals("A {icon:test_mod:inline_text@9x7} B", inline.text());
        assertTrue(inline.richText().hasInlineContent(), "{icon:...} markers resolve through the configured resolver");
        assertEquals("A " + InlineContentSpan.DEFAULT_FALLBACK_TEXT + " B", inline.richText().plainText());

        fails("<Image texture=\"\" />", "Texture id must not be blank");
        fails("<Image texture=\"test:bad\" source=\"0 0 1\" />", "Expected 4 rect values");
    }

    @Test
    void scrollViewContentPolicy() {
        ScrollView scroll = Xaml.load("""
                <ScrollView id="scroll" width="120" height="48" overflowY="auto">
                    <VBox id="content" spacing="2"><Label id="row" text="Row" /></VBox>
                </ScrollView>
                """, ScrollView.class);
        assertInstanceOf(VBox.class, scroll.content());
        assertEquals("Row", Xaml.get(scroll, "row", Label.class).text());
    }

    // ------------------------------------------------------------------ схема и аннотации

    public static class Badge extends TextWidget {
        private float importance;

        public float importance() {
            return importance;
        }

        public Badge importance(float importance) {
            this.importance = importance;
            return this;
        }
    }

    @XamlWidget("AnnotatedBadge")
    public static class AnnotatedBadge extends TextWidget {
        enum Tone {INFO, WARNING}

        private float importance;
        private boolean active;
        private Tone tone = Tone.INFO;
        private final MutableColor accent = new MutableColor();

        public float importance() {
            return importance;
        }

        @XamlProperty(value = "importance", category = "Behavior", defaultValue = "0")
        public AnnotatedBadge importance(float importance) {
            this.importance = importance;
            return this;
        }

        public boolean active() {
            return active;
        }

        @XamlProperty(value = "active", category = "Behavior", defaultValue = "false")
        public AnnotatedBadge active(boolean active) {
            this.active = active;
            return this;
        }

        public Tone tone() {
            return tone;
        }

        @XamlProperty(value = "tone", category = "Content", defaultValue = "info")
        public AnnotatedBadge tone(Tone tone) {
            this.tone = tone == null ? Tone.INFO : tone;
            return this;
        }

        public MutableColor accent() {
            return accent;
        }

        @XamlProperty(value = "accent", category = "Appearance", defaultValue = "#FFFFFFFF")
        public AnnotatedBadge accent(MutableColor accent) {
            this.accent.set(accent == null ? new MutableColor() : accent);
            return this;
        }
    }

    @Test
    void customSchemaWithManualRegistrationAndAlias() {
        Schema schema = Schema.builtIns();
        schema.define("Badge", () -> TypeInfo.of(Schema.name("Badge"), Badge.class, Badge::new)
                .property("text", Converters.STRING, Badge::text)
                .property("importance", Converters.FLOAT, Badge::importance)
                .build());
        schema.alias("Pill", "Badge");

        Badge badge = Xaml.load("<Pill id=\"customBadge\" text=\"Custom\" importance=\"7.5\" />",
                Badge.class, schema, LoadOptions.DEFAULT);
        assertEquals("customBadge", badge.id());
        assertEquals("Custom", badge.text());
        assertTrue(near(badge.importance(), 7.5f));
        assertTrue(schema.contains(UiName.of("Pill")));

        fails("<Badge text=\"Missing schema\" />", "Unknown widget type 'Badge'");
        assertThrows(XamlLoadException.class, () -> Xaml.load("<Badge/>", Schema.empty(), LoadOptions.DEFAULT));
    }

    @Test
    void annotatedSchemaMaterializesTypedSetters() {
        Schema schema = Schema.builtIns().defineAnnotated(AnnotatedBadge.class);
        AnnotatedBadge badge = Xaml.load("""
                <AnnotatedBadge id="ann" text="Annotated" importance="4.5" active="true"
                                tone="warning" accent="#336699CC" width="88" />
                """, AnnotatedBadge.class, schema, LoadOptions.DEFAULT);
        assertEquals("Annotated", badge.text());
        assertTrue(near(badge.importance(), 4.5f));
        assertTrue(badge.active());
        assertEquals(AnnotatedBadge.Tone.WARNING, badge.tone());
        assertTrue(near(badge.accent().r(), 0x33 / 255.0f) && near(badge.accent().a(), 0xCC / 255.0f));
        assertTrue(near(badge.layoutStyle().width().value(), 88.0f), "shared layout properties come with the schema");

        Schema factorySchema = Schema.empty().defineAnnotated(AnnotatedBadge.class,
                () -> new AnnotatedBadge().importance(9.0f));
        AnnotatedBadge made = Xaml.load("<AnnotatedBadge text=\"Factory\" />", AnnotatedBadge.class,
                factorySchema, LoadOptions.DEFAULT);
        assertTrue(near(made.importance(), 9.0f) && made.text().equals("Factory"), "explicit factories are honoured");

        assertThrows(IllegalArgumentException.class, () -> Schema.empty().defineAnnotated(Badge.class),
                "@XamlWidget is required");
    }

    @Test
    void annotatedBuiltInWidgetFamilies() {
        Schema schema = Schema.empty();
        for (Class<? extends Widget> type : java.util.List.of(VirtualListView.class, VirtualTableView.class,
                Accordion.class, Breadcrumb.class, ExpandablePanel.class, PageView.class, TabControl.class,
                TreeView.class, TreeList.class, DockingRoot.class, GraphView.class, NodeGraph.class,
                WorldCanvas.class, MapCanvas.class, MapMarker.class)) {
            defineAnnotated(schema, type);
        }
        LoadOptions options = LoadOptions.DEFAULT;

        VirtualListView list = Xaml.load("""
                <VirtualListView itemCount="12" itemHeight="22" overscan="3" selectionMode="multiple"
                                 viewMode="grid" gridCellWidth="40" gridCellHeight="48" gridColumns="3" />
                """, VirtualListView.class, schema, options);
        assertEquals(12, list.itemCount());
        assertTrue(near(list.itemHeight(), 22.0f) && near(list.gridCellWidth(), 40.0f) && near(list.gridCellHeight(), 48.0f));
        assertEquals(VirtualListView.ViewMode.GRID, list.viewMode());
        assertEquals(3, list.gridColumns());
        assertEquals(3, list.overscan());
        assertEquals(SelectionMode.MULTIPLE, list.selectionMode());

        VirtualTableView table = Xaml.load("""
                <VirtualTableView rowCount="7" rowHeight="19" headerHeight="24" overscan="2" editable="true"
                                  selectionMode="multiple" />
                """, VirtualTableView.class, schema, options);
        assertEquals(7, table.rowCount());
        assertTrue(near(table.rowHeight(), 19.0f) && near(table.headerHeight(), 24.0f));
        assertTrue(table.editable());
        assertEquals(SelectionMode.MULTIPLE, table.selectionMode());

        WorldCanvas world = Xaml.load("""
                <WorldCanvas clippingEnabled="false" panningEnabled="false" wheelPanStep="48" panButton="middle"
                             preferredWidth="420" />
                """, WorldCanvas.class, schema, options);
        assertFalse(world.clippingEnabled());
        assertFalse(world.panningEnabled());
        assertTrue(near(world.wheelPanStep(), 48.0f) && near(world.preferredWidth(), 420.0f));
        assertEquals(PointerButton.MIDDLE, world.panButton());

        NodeGraph graph = Xaml.load("""
                <NodeGraph selectionMode="multiple" gridSize="32" panningEnabled="false" preferredHeight="180" />
                """, NodeGraph.class, schema, options);
        assertEquals(NodeGraphSelectionMode.MULTIPLE, graph.selectionMode());
        assertTrue(near(graph.gridSize(), 32.0f) && near(graph.preferredHeight(), 180.0f));
        assertFalse(graph.panningEnabled());

        MapCanvas map = Xaml.load("""
                <MapCanvas backgroundVisible="false" gridVisible="false" gridSize="64" wheelPanStep="12" />
                """, MapCanvas.class, schema, options);
        assertFalse(map.backgroundVisible() || map.gridVisible());
        assertTrue(near(map.gridSize(), 64.0f) && near(map.wheelPanStep(), 12.0f));

        MapMarker marker = Xaml.load("<MapMarker label=\"Spawn\" selected=\"true\" highlighted=\"true\" />",
                MapMarker.class, schema, options);
        assertEquals("Spawn", marker.label().text());
        assertTrue(marker.selected() && marker.highlighted());

        ExpandablePanel panel = Xaml.load("<ExpandablePanel title=\"Advanced\" expanded=\"false\" />",
                ExpandablePanel.class, schema, options);
        assertEquals("Advanced", panel.title());
        assertFalse(panel.expanded());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void defineAnnotated(Schema schema, Class<? extends Widget> type) {
        schema.defineAnnotated((Class) type);
    }

    // ------------------------------------------------------------------ binding

    @Test
    void bindingExpressionsFollowTheirSources() {
        XamlBindingContext context = XamlBindingContext.empty();
        XamlMutableObservableValue<String> name = context.mutable("user.name", String.class, "Initial");
        XamlMutableObservableValue<Boolean> shown = context.mutable("ui.showSettings", Boolean.class, true);
        XamlMutableObservableValue<Float> gamma = context.mutable("settings.gamma", Float.class, 0.25f);

        LoadedScreen<VBox> screen = Xaml.loadScreen("""
                <VBox id="root" visible="{Binding ui.showSettings}">
                    <Label id="title" text="{Binding user.name}" />
                    <Slider id="gamma" value="{Binding Path=settings.gamma, Mode=OneWay}" />
                </VBox>
                """, VBox.class, LoadOptions.DEFAULT.bindingContext(context));

        Label label = Xaml.get(screen.root(), "title", Label.class);
        Slider slider = Xaml.get(screen.root(), "gamma", Slider.class);
        assertEquals("Initial", label.text());
        assertTrue(near(slider.value(), 0.25f));
        assertTrue(screen.root().visible());

        name.set("Updated");
        gamma.set(0.75f);
        shown.set(false);
        assertEquals("Updated", label.text());
        assertTrue(near(slider.value(), 0.75f));
        assertFalse(screen.root().visible());
        assertTrue(screen.bindingSession().diagnostics().isEmpty());

        screen.close();
        name.set("After close");
        assertEquals("Updated", label.text(), "closing the screen unsubscribes every binding");
        assertTrue(screen.bindingSession().closed());

        XamlBindingContext other = XamlBindingContext.empty();
        other.mutable("settings.gamma", Float.class, 0.5f);
        LoadedScreen<Label> mismatch = Xaml.loadScreen("<Label text=\"{Binding settings.gamma}\" />",
                Label.class, LoadOptions.DEFAULT.bindingContext(other));
        assertEquals(1, mismatch.bindingSession().diagnostics().size());
        assertEquals(XamlBindingStatus.State.ERROR, mismatch.bindingSession().diagnostics().get(0).state());
        mismatch.close();
    }

    @Test
    void bindingModesAndErrors() {
        XamlBindingContext context = XamlBindingContext.empty();
        XamlMutableObservableValue<String> name = context.mutable("user.name", String.class, "first");
        LoadOptions options = LoadOptions.DEFAULT.bindingContext(context);

        Label once = Xaml.load("<Label text=\"{Binding Path=user.name, Mode=OneTime}\" />", Label.class, options);
        name.set("second");
        assertEquals("first", once.text(), "OneTime takes the value once and then detaches");

        XamlLoadException toSource = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Label text=\"{Binding user.name, Mode=OneWayToSource}\" />", options));
        assertTrue(toSource.getMessage().contains("not supported yet"), toSource.getMessage());
        XamlLoadException twoWay = assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Label text=\"{Binding user.name, Mode=TwoWay}\" />", options));
        assertTrue(twoWay.getMessage().contains("does not support Mode=TwoWay"), twoWay.getMessage());
        assertTrue(assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Label text=\"{Binding user.name, Converter=X}\" />", options)).getMessage()
                .contains("Unsupported Binding option 'Converter'"));
        assertTrue(assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Label width=\"{Binding user.name}\" />", options)).getMessage()
                .contains("does not support XML binding"));
        assertTrue(assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Label text=\"{Binding}\" />", options)).getMessage().contains("requires a Path"));
        assertTrue(assertThrows(XamlLoadException.class, () -> Xaml.load(
                "<Label text=\"{Nope x}\" />", options)).getMessage().contains("Unknown markup extension 'Nope'"));
    }

    @Test
    void eachLoadGetsItsOwnDefaultBindingContext() {
        LoadedScreen<Label> first = Xaml.loadScreen("<Label text=\"{Binding some.path}\" />", Label.class);
        LoadedScreen<Label> second = Xaml.loadScreen("<Label text=\"{Binding some.path}\" />", Label.class);
        assertEquals(1, first.bindingSession().diagnostics().size(), "missing source is reported, not thrown");
        assertEquals(1, second.bindingSession().diagnostics().size());
        first.close();
        second.close();
    }

    // ------------------------------------------------------------------ новые возможности

    @Test
    void staticResourcesAreScopedAndConvertedByTheTargetProperty() {
        VBox root = Xaml.load("<VBox " + X + """
                  spacing="{StaticResource Gap}">
                    <VBox.Resources>
                        <x:String x:Key="Accent">#336699CC</x:String>
                        <x:String x:Key="Gap">7</x:String>
                    </VBox.Resources>
                    <Label id="tinted" color="{StaticResource Accent}" text="x" />
                    <HBox id="outer" spacing="{StaticResource Gap}">
                        <VBox id="shadow">
                            <VBox.Resources><x:String x:Key="Gap">3</x:String></VBox.Resources>
                            <HBox id="inner" spacing="{StaticResource Gap}" />
                        </VBox>
                    </HBox>
                </VBox>
                """, VBox.class);

        assertEquals(7.0f, root.spacing(), "resources declared on the element itself are visible to its own attributes");
        Label tinted = Xaml.get(root, "tinted", Label.class);
        assertTrue(near(tinted.color().r(), 0x33 / 255.0f) && near(tinted.color().a(), 0xCC / 255.0f),
                "the same text resource is parsed as a colour here...");
        assertEquals(7.0f, Xaml.get(root, "outer", HBox.class).spacing(), "...and as a number there");
        assertEquals(3.0f, Xaml.get(root, "inner", HBox.class).spacing(), "the nearest scope shadows outer resources");

        fails("<Label text=\"{StaticResource Nope}\" />", "Resource 'Nope' was not found");
        fails("<VBox " + X + "><VBox.Resources><x:String>1</x:String></VBox.Resources></VBox>", "requires x:Key");
        fails("<VBox " + X + "><VBox.Resources><x:String x:Key=\"a\">1</x:String><x:String x:Key=\"a\">2</x:String>"
                + "</VBox.Resources></VBox>", "Duplicate resource key 'a'");
        fails("<VBox><VBox.Resources><Label x:Key=\"a\" xmlns:x=\"https://unigui.dev/xaml\"/></VBox.Resources></VBox>",
                "Use <Style> for property sets");
    }

    @Test
    void regularPropertiesAcceptElementSyntax() {
        Label label = Xaml.load("<Label><Label.Text>From element</Label.Text></Label>", Label.class);
        assertEquals("From element", label.text());

        XamlBindingContext context = XamlBindingContext.empty();
        context.mutable("user.name", String.class, "bound");
        Label bound = Xaml.load("<Label><Label.Text>{Binding user.name}</Label.Text></Label>", Label.class,
                LoadOptions.DEFAULT.bindingContext(context));
        assertEquals("bound", bound.text());
    }

    @Test
    void duplicatePropertyDefinitionsAreRejected() {
        fails("<ScrollView><ScrollView.Content><VBox/></ScrollView.Content>"
                + "<ScrollView.Content><Label/></ScrollView.Content></ScrollView>", "is set more than once");
        fails("<Label text=\"a\"><Label.Text>b</Label.Text></Label>", "is set more than once");
    }

    @Test
    void sameDocumentLoadsIdenticallyFromTreeAndFromText() {
        String text = "<VBox spacing=\"2\"><Label id=\"a\" text=\"x\"/><Button id=\"b\" text=\"y\"/></VBox>";
        Widget fromText = Xaml.load(text);
        Widget fromTree = new dev.sixik.unigui.xaml.load.XamlLoader(Xaml.defaultSchema(), LoadOptions.DEFAULT)
                .loadScreen(dev.sixik.unigui.xaml.format.XamlFormat.INSTANCE.read(text)).root();
        assertEquals(TreeDump.dump(fromText), TreeDump.dump(fromTree));

        // Дерево, собранное вручную (как это сделает Java-DSL или редактор), грузится тем же загрузчиком.
        UiNode built = UiNode.of("VBox").set("spacing", "2");
        built.add(UiNode.of("Label").set("id", "a").set("text", "x"));
        built.add(UiNode.of("Button").set("id", "b").set("text", "y"));
        Widget fromBuilder = new dev.sixik.unigui.xaml.load.XamlLoader(Xaml.defaultSchema(), LoadOptions.DEFAULT)
                .loadScreen(UiDocument.of(built)).root();
        assertEquals(TreeDump.dump(fromText), TreeDump.dump(fromBuilder));
    }

    // ------------------------------------------------------------------ ошибки

    @Test
    void failureDiagnostics() {
        fails("<Missing />", "Unknown widget type 'Missing'");
        fails("<VBox><Label id=\"same\" /><Button id=\"same\" /></VBox>", "Duplicate widget id 'same'");
        fails("<VBox spcaing=\"6\" />", "Unknown attribute 'spcaing' on VBox");
        fails("<VBox spacing=\"large\" />", "Cannot apply attribute on VBox");
        fails("<Button><Label text=\"Nope\" /></Button>", "Widget Button cannot contain child Label");
        fails("<VBox><VBox.Content><Label /></VBox.Content></VBox>", "Unknown property element 'VBox.Content' on VBox");
        fails("<VBox><ScrollView.Content><Label /></ScrollView.Content></VBox>",
                "Property element 'ScrollView.Content' cannot be used inside VBox");
        fails("<VBox><VBox.Children unexpected=\"true\"><Label /></VBox.Children></VBox>",
                "Property element 'VBox.Children' cannot have attributes");
        fails("""
                <ScrollView>
                    <ScrollView.Content>
                        <VBox />
                        <Label />
                    </ScrollView.Content>
                </ScrollView>
                """, "Widget ScrollView can contain only one content child");
        fails("<VBox />", Button.class, "XAML root is VBox, expected Button");
        fails("<VBox>", "Cannot parse XAML source");
        fails("   ", "must not be blank");

        VBox root = Xaml.load("<VBox id=\"root\" />", VBox.class);
        XamlLoadException lookup = assertThrows(XamlLoadException.class, () -> Xaml.get(root, "missing"));
        assertTrue(lookup.getMessage().contains("Widget id 'missing' was not found under root 'root'"));
        XamlLoadException wrongType = assertThrows(XamlLoadException.class, () -> Xaml.get(root, "root", Label.class));
        assertTrue(wrongType.getMessage().contains("exists, but is VBox, not Label"));
    }

    private static void located(String xaml, String part, int minimumLine) {
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> Xaml.load(xaml));
        assertTrue(failure.getMessage().contains(part), failure.getMessage());
        UiDiagnostic diagnostic = failure.diagnostics().get(0);
        assertTrue(diagnostic.hasLocation(), "diagnostic must carry a position: " + diagnostic);
        assertTrue(diagnostic.line() >= minimumLine && diagnostic.column() > 0, diagnostic.toString());
        assertTrue(failure.getMessage().contains("line") && failure.getMessage().contains("column"), failure.getMessage());
    }

    @Test
    void diagnosticLocations() {
        located("<VBox>\n    <Missing />\n</VBox>", "Unknown widget type 'Missing'", 2);
        located("<VBox>\n    <Label spcaing=\"6\" />\n</VBox>", "Unknown attribute 'spcaing' on Label", 2);
        located("<VBox>\n    <Label color=\"not-a-color\" />\n</VBox>", "Cannot apply attribute on Label", 2);
        located("<VBox>\n    <VBox.Content><Label /></VBox.Content>\n</VBox>", "Unknown property element", 2);
        located("<VBox>\n    <Label>\n</VBox>", "Cannot parse XAML source", 3);

        XamlLoadException precise = assertThrows(XamlLoadException.class,
                () -> Xaml.load("<Label\n  text=\"ok\"\n  color=\"nope\"/>"));
        assertEquals(3, precise.diagnostics().get(0).line(), "the position points at the offending attribute, not the element");
    }
}
