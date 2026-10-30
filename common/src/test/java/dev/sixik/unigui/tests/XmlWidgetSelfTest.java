package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeUnit;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.text.InlineContentSpan;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.api.xml.XmlBinding;
import dev.sixik.unigui.api.xml.XmlBindingContext;
import dev.sixik.unigui.api.xml.XmlBindingDiagnosticsModel;
import dev.sixik.unigui.api.xml.XmlBindingStatus;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlAttributeValueType;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetAsset;
import dev.sixik.unigui.api.xml.XmlWidgetAssetCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetAssetKind;
import dev.sixik.unigui.api.xml.XmlWidgetAssetProviders;
import dev.sixik.unigui.api.xml.XmlWidgetAssetPickerModel;
import dev.sixik.unigui.api.xml.XmlWidgetAssetPickerPanel;
import dev.sixik.unigui.api.xml.XmlCommandRegistry;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlLayoutAttributes;
import dev.sixik.unigui.api.xml.XmlStyleAttributes;
import dev.sixik.unigui.api.xml.XmlValueParsers;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdit;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdits;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentIo;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentResult;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetHierarchy;
import dev.sixik.unigui.api.xml.XmlWidgetInspector;
import dev.sixik.unigui.api.xml.XmlWidgetAnnotations;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsModel;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsPanel;
import dev.sixik.unigui.api.xml.XmlWidgetHotReloadPreview;
import dev.sixik.unigui.api.xml.XmlWidgetHotReloadSource;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutFrame;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutHandle;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutHandles;
import dev.sixik.unigui.api.xml.XmlMutableObservableValue;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetNode;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;
import dev.sixik.unigui.api.xml.XmlWidgetPrefabCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetRegistryContributions;
import dev.sixik.unigui.api.xml.XmlWidgetRuntimeSerializer;
import dev.sixik.unigui.api.xml.XmlWidgetSerializationOptions;
import dev.sixik.unigui.api.xml.XmlWidgetScreen;
import dev.sixik.unigui.api.xml.XmlWidgetSelectionModel;
import dev.sixik.unigui.api.xml.XmlWidgetTemplateCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetTemplateKind;
import dev.sixik.unigui.api.xml.XmlWidgetTemplateValues;
import dev.sixik.unigui.api.xml.editor.XmlEditorDiagnosticChannel;
import dev.sixik.unigui.api.xml.editor.XmlEditorDocumentSource;
import dev.sixik.unigui.api.xml.editor.XmlEditorDocumentSources;
import dev.sixik.unigui.api.xml.editor.XmlEditorMode;
import dev.sixik.unigui.api.xml.editor.XmlEditorSession;
import dev.sixik.unigui.api.xml.editor.XmlEditorSessionChange;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.FlexBox;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.caching.CachedSubtreeWidget;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.widgets.display.ImageView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.LoadingIndicator;
import dev.sixik.unigui.widgets.feedback.NotificationView;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.feedback.Toast;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.graph.GraphView;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.CodeLanguagePreset;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.DatePicker;
import dev.sixik.unigui.widgets.interaction.DropDownBox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.PasswordField;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.SearchField;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.interaction.TimeSpanField;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.interaction.TreeListPicker;
import dev.sixik.unigui.widgets.interaction.XmlCodeEditor;
import dev.sixik.unigui.widgets.map.MapCanvas;
import dev.sixik.unigui.widgets.map.MapMarker;
import dev.sixik.unigui.widgets.minecraft.MinecraftTexturePickerWidget;
import dev.sixik.unigui.widgets.navigation.Accordion;
import dev.sixik.unigui.widgets.navigation.Breadcrumb;
import dev.sixik.unigui.widgets.navigation.Carousel;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.MenuBar;
import dev.sixik.unigui.widgets.navigation.PageView;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.widgets.navigation.TreeList;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import dev.sixik.unigui.impl.widget.WidgetBase;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public final class XmlWidgetSelfTest {
    public static void main(String[] args) {
        new XmlWidgetSelfTest().run();
    }

    private void run() {
        testTypedRootAttributesAndChildren();
        testStreamAndResourceLoading();
        testLoaderOptions();
        testScreenScaleProviderXml();
        testNamespaceNameAndTextContent();
        testAliasesAndPropertyElements();
        testCustomRegistryApi();
        testGlobalRegistryContributions();
        testAnnotationBackedRegistryApi();
        testCommandRegistryApi();
        testTypedObservableBindingModel();
        testXmlBindingExpressions();
        testXmlCodeEditorWidgetContracts();
        testDescriptorBackedInspectorModel();
        testTextureXmlAttributes();
        testXmlAssetCatalogAndPickerModel();
        testScrollViewContentChildPolicy();
        testDocumentRoundTripModel();
        testRuntimeWidgetSnapshotSerializer();
        testLayoutAttributesRoundTrip();
        testPrefabIncludeExpansionModel();
        testItemAndControlTemplateModel();
        testEditorDocumentIo();
        testHotReloadPreviewModel();
        testDiagnosticsPanelWidget();
        testEditorHierarchyAndSelectionModels();
        testEditorLayoutDragResizeHandles();
        testUndoableDocumentEdits();
        testEditorDiagnosticsCollection();
        testXmlEditorSessionParseFailurePreservesLastValidDocument();
        testXmlEditorSessionSelectionNoOpDoesNotEmit();
        testXmlEditorSessionUndoRedoAndSelectionSurvival();
        testXmlEditorSessionSaveRevertAndEvents();
        testEditorPreservesUnsupportedAttributes();
        testAnnotationPrototypeMetadata();
        testExpandedAnnotatedWidgetRegistration();
        testFailureDiagnostics();
        testDiagnosticLocations();
        System.out.println("XmlWidgetSelfTest passed");
    }

    private void testTypedRootAttributesAndChildren() {
        VBox root = XMLWidget.create("""
                <VBox id="root" spacing="6" width="240" height="120" alignItems="center" padding="10 8 10 8">
                    <Label id="title" text="Video Settings" color="#11223344" />
                    <Button id="apply" text="Apply" enabled="false" />
                    <Slider id="gamma" min="0" max="10" value="5" step="0.5" />
                    <TextArea id="notes" text="Line A&#10;Line B" visibleLines="3" />
                </VBox>
                """, VBox.class);

        expect(root.id().equals("root"), "XML id should be assigned to the root widget");
        expect(root.children().size() == 4, "Panel children should attach in XML order");
        expect(near(root.spacing(), 6.0f), "VBox spacing attribute should apply");
        expect(root.layoutStyle().width().unit() == SizeUnit.PIXELS && near(root.layoutStyle().width().value(), 240.0f),
                "width attribute should apply as pixel SizeValue");
        expect(root.layoutStyle().height().unit() == SizeUnit.PIXELS && near(root.layoutStyle().height().value(), 120.0f),
                "height attribute should apply as pixel SizeValue");
        expect(root.layoutStyle().alignItems() == Align.CENTER, "alignItems should parse enum values case-insensitively");
        expect(near(root.layoutStyle().padding().top(), 10.0f)
                        && near(root.layoutStyle().padding().right(), 8.0f)
                        && near(root.layoutStyle().padding().bottom(), 10.0f)
                        && near(root.layoutStyle().padding().left(), 8.0f),
                "padding should parse CSS-like top/right/bottom/left syntax");

        Label title = XMLWidget.getWidget(root, "title", Label.class);
        expect(title.text().equals("Video Settings"), "Label text attribute should apply");
        expect(near(title.color().r(), 0x11 / 255.0f) && near(title.color().a(), 0x44 / 255.0f),
                "Label color should parse #RRGGBBAA");

        Button apply = XMLWidget.getWidget(root, "apply", Button.class);
        expect(apply.text().equals("Apply") && !apply.enabled(), "Button text and enabled attributes should apply");

        Slider gamma = XMLWidget.getWidget(root, "gamma", Slider.class);
        expect(near(gamma.min(), 0.0f) && near(gamma.max(), 10.0f) && near(gamma.value(), 5.0f),
                "Slider min/max should apply before value regardless of DOM attribute order");
        TextArea notes = XMLWidget.getWidget(root, "notes", TextArea.class);
        expect(notes.text().equals("Line A\nLine B") && notes.visibleLines() == 3,
                "TextArea should materialize multiline XML attributes");
        expect(XMLWidget.findWidget(root, "missing").isEmpty(), "findWidget should return empty for unknown ids");

        VBox surfaced = XMLWidget.create("""
                <VBox background="#11223344" border="#FFEEDDCC" borderWidth="2" radius="3">
                    <Label id="inner" text="Hi" />
                </VBox>
                """, VBox.class);
        expect(surfaced.boxVisualEnabled() && surfaced.backgroundVisible() && surfaced.borderVisible(),
                "VBox surface XML attributes should enable surface rendering");
        expect(near(surfaced.background().r(), 0x11 / 255.0f) && near(surfaced.background().a(), 0x44 / 255.0f),
                "VBox background XML attribute should apply");
        expect(near(surfaced.borderWidth(), 2.0f) && near(surfaced.radius(), 3.0f),
                "VBox border and radius XML attributes should apply");
        expect(surfaced.children().size() == 1, "VBox surface XML should still attach children");
    }

    private void testStreamAndResourceLoading() {
        ByteArrayInputStream stream = new ByteArrayInputStream("<Label id=\"streamLabel\" text=\"Stream\" />"
                .getBytes(StandardCharsets.UTF_8));
        Label fromStream = XMLWidget.create(stream, Label.class);
        expect(fromStream.id().equals("streamLabel") && fromStream.text().equals("Stream"),
                "XMLWidget.create(InputStream, type) should load UTF-8 XML");

        Box fromResource = XMLWidget.createResource("assets/unigui/xml/xml_demo.xml", Box.class);
        expect(fromResource.id().equals("reactorPanel"), "XMLWidget.createResource should load XML from classpath resources");
        expect(XMLWidget.getWidget(fromResource, "logText", TextWidget.class).text().contains("Core initialized"),
                "Resource-loaded demo XML should materialize annotated display widgets");
        expect(XMLWidget.getWidget(fromResource, "safeMode", Checkbox.class).checked(),
                "Resource-loaded demo XML should materialize annotated toggle controls");
        expect(XMLWidget.getWidget(fromResource, "metricCards", WrapPanel.class).children().size() == 4,
                "Resource-loaded demo XML should materialize annotated container widgets");
        expect(near(XMLWidget.getWidget(fromResource, "temperatureBar", ProgressBar.class).value(), 58.0f),
                "Resource-loaded demo XML should apply nested control attributes");
        expect(XMLWidget.getWidget(fromResource, "powerLimit", Slider.class).value() == 66.0f,
                "Resource-loaded demo XML should apply slider attributes");

        VBox overview = XMLWidget.createResource("assets/unigui/xml/overview.xml", VBox.class);
        expect(XMLWidget.getWidget(overview, "overviewTitle", Label.class).text().equals("Overview"),
                "Larger TestMod overview slice should load its static XML structure");
        expect(XMLWidget.getWidget(overview, "overviewCards", WrapPanel.class).children().size() == 4,
                "Overview XML should materialize the card WrapPanel from resource XML");
        expect(XMLWidget.getWidget(overview, "quickFactoriesSlot", VBox.class).children().isEmpty(),
                "Overview XML should expose an empty code-behind slot for Java-created widgets");
        expectFailsResource("assets/unigui/xml/missing.xml", "XML widget resource 'assets/unigui/xml/missing.xml' was not found");
    }

    private void testLoaderOptions() {
        expectFails("<Label futureEditorAttr=\"todo\" text=\"Strict\" />", "Unknown attribute 'futureEditorAttr' on Label");

        Label lenient = XMLWidget.create(
                "<Label id=\"lenient\" futureEditorAttr=\"todo\" text=\"Lenient\" />",
                Label.class,
                XmlWidgetOptions.lenient());
        expect(lenient.id().equals("lenient") && lenient.text().equals("Lenient"),
                "Lenient XML options should ignore unknown attributes while applying known ones");

        try {
            XMLWidget.create("<VBox><VBox><Label text=\"Too deep\" /></VBox></VBox>", XmlWidgetOptions.DEFAULT.maxDepth(1));
            throw new AssertionError("Expected maxDepth to reject deep XML trees");
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains("deeper than the configured limit"),
                    "maxDepth failure should mention configured limit, got: " + failure.getMessage());
        }
    }

    private void testScreenScaleProviderXml() {
        XmlWidgetScreen<VBox> fixedScreen = XMLWidget.createScreen("""
                <Screen uiScale="2.5" scaleWithMinecraftGui="false">
                    <VBox id="screenRoot" spacing="4" />
                </Screen>
                """, VBox.class);
        expect(fixedScreen.root().id().equals("screenRoot"), "Screen XML should expose its content root widget");
        expect(near(fixedScreen.scaleProvider().scale(), 2.5f), "Screen uiScale should create a fixed UIScaleProvider");
        expect(!fixedScreen.scaleWithMinecraftGui(), "Screen scaleWithMinecraftGui=false should be preserved");

        XmlWidgetScreen<Box> unityScreen = XMLWidget.createScreen("""
                <UIScreen scaleProvider="unity"
                          referenceResolution="960x540"
                          match="balanced"
                          userScale="2"
                          scaleRange="0.5 8">
                    <Box id="panel" />
                </UIScreen>
                """, Box.class);
        expect(unityScreen.scaleProvider() instanceof UnityLikeUIScaleProvider,
                "Screen scaleProvider=unity should create UnityLikeUIScaleProvider");
        UnityLikeUIScaleProvider unity = (UnityLikeUIScaleProvider) unityScreen.scaleProvider();
        unity.viewportSize(1920.0f, 1080.0f);
        expect(near(unity.scale(), 4.0f), "Unity-like XML scale should use reference resolution, match and userScale");
        expect(unityScreen.scaleWithMinecraftGui(), "Screen should multiply by Minecraft GUI scale by default");

        XmlWidgetScreen<Label> propertyScreen = XMLWidget.createScreen("""
                <Screen independentScale="true">
                    <Screen.ScaleProvider type="mutable" scale="3" />
                    <Screen.Content>
                        <Label id="caption" text="Scaled" />
                    </Screen.Content>
                </Screen>
                """, Label.class);
        expect(propertyScreen.root().text().equals("Scaled"), "Screen.Content should provide the typed root widget");
        expect(near(propertyScreen.scaleProvider().scale(), 3.0f), "Screen.ScaleProvider should configure UIScaleProvider");
        expect(!propertyScreen.scaleWithMinecraftGui(), "independentScale=true should disable Minecraft GUI scale multiplication");

        XmlWidgetScreen<Label> plainWidget = XMLWidget.createScreen("<Label id=\"plain\" text=\"Plain\" />", Label.class);
        expect(plainWidget.scaleProvider() == UIScaleProvider.IDENTITY,
                "Plain widget XML should load as a screen payload with identity scale");

        XmlWidgetDocumentResult editorResult = XmlWidgetDocument.parseEditor("""
                <Screen uiScale="2">
                    <Label id="editorCaption" text="Editor" />
                </Screen>
                """);
        expect(editorResult.valid(), "Editor XML validation should accept Screen scale provider wrapper");
    }

    private void testNamespaceNameAndTextContent() {
        Widget root = XMLWidget.create("""
                <VBox xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" x:Name="root">
                    <Label x:Name="title">Hello from XML</Label>
                </VBox>
                """);

        expect(root.id().equals("root"), "x:Name should work as an id alias for Noesis-like XML");
        Label title = XMLWidget.getWidget(root, "title", Label.class);
        expect(title.text().equals("Hello from XML"), "Text-like widgets should accept simple text content");

        Label named = XMLWidget.create("<Label Name=\"caption\" text=\"Named\" />", Label.class);
        expect(named.id().equals("caption"), "Name should work as a friendly id alias");
    }

    private void testAliasesAndPropertyElements() {
        Box card = XMLWidget.create("""
                <Border id="card" background="#202936FF" border="#7AA2FFFF" borderWidth="1">
                    <Border.Children>
                        <Label id="caption" text="Alias card" />
                    </Border.Children>
                </Border>
                """, Box.class);

        expect(card.id().equals("card"), "Alias root should create the target widget type");
        expect(card.children().size() == 1, "Panel property element should add children without creating a wrapper widget");
        expect(XMLWidget.getWidget(card, "caption", Label.class).text().equals("Alias card"),
                "Property element children should participate in id lookup");

        ScrollView scroll = XMLWidget.create("""
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

        expect(scroll.content() instanceof VBox, "ScrollViewer alias should support the Content property element");
        expect(XMLWidget.getWidget(scroll, "content", VBox.class).spacing() == 3.0f,
                "Nested property elements should configure normal child widgets");
        expect(XMLWidget.getWidget(scroll, "row", Label.class).text().equals("Wrapped row"),
                "Nested property element descendants should be searchable by id");
    }

    private void testCustomRegistryApi() {
        XmlWidgetRegistry registry = XMLWidget.registry();
        registry.register("Badge", Badge::new)
                .attribute("text", XmlValueParsers.STRING, Badge::text)
                .attribute("importance", XmlValueParsers.FLOAT, Badge::importance);
        registry.alias("Pill", "Badge");

        Badge badge = XMLWidget.create("""
                <Pill id="customBadge" text="Custom Registry" importance="7.5" />
                """, Badge.class, registry);

        expect(badge.id().equals("customBadge"), "Custom registry should assign ids to custom widget types");
        expect(badge.text().equals("Custom Registry"), "Custom registry should apply public parser attributes");
        expect(near(badge.importance(), 7.5f), "Custom registry should apply custom numeric attributes");
        expect(registry.contains("Pill"), "Custom registry should resolve aliases through contains");
        expect(registry.type("Badge").isPresent(), "Custom registry should expose registered descriptors");

        expectFails("<Badge text=\"Missing registry\" />", "Unknown widget type 'Badge'");
    }

    private void testGlobalRegistryContributions() {
        AutoCloseable contribution = XmlWidgetRegistryContributions.register(registry -> registry.register("GlobalBadge", Badge::new)
                .describe("Global Badge", "Custom", "Mod-provided widget registered globally.")
                .attribute("text", XmlValueParsers.STRING, Badge::text)
                .attribute("importance", XmlValueParsers.FLOAT, Badge::importance));
        try {
            XmlWidgetRegistry registry = XMLWidget.registry();
            expect(registry.contains("GlobalBadge"),
                    "Global XML registry contributions should be applied to fresh built-in registry copies");
            expect(registry.descriptor("GlobalBadge").orElseThrow().displayName().equals("Global Badge"),
                    "Global XML registry contributions should expose descriptor metadata for editor palettes");

            Badge badge = XMLWidget.create("<GlobalBadge id=\"global\" text=\"Global\" importance=\"3\" />", Badge.class);
            expect(badge.id().equals("global") && badge.text().equals("Global") && near(badge.importance(), 3.0f),
                    "Default XML loading should materialize globally contributed widget types");

            XmlWidgetDocumentResult result = XmlWidgetDocument.parseEditor("<GlobalBadge text=\"Editor\" />");
            expect(result.valid(),
                    "Editor validation should use the same globally contributed built-in registry surface");
            expect(!XMLWidget.emptyRegistry().contains("GlobalBadge"),
                    "Empty registries should remain isolated from global built-in contributions");
        } finally {
            closeUnchecked(contribution);
        }

        expectFails("<GlobalBadge text=\"Removed\" />", "Unknown widget type 'GlobalBadge'");
    }

    private void testAnnotationBackedRegistryApi() {
        XmlWidgetRegistry registry = XMLWidget.emptyRegistry();
        var type = registry.registerAnnotated(AnnotatedBadge.class);
        expect(type.hasAttribute("text")
                        && type.hasAttribute("importance")
                        && type.hasAttribute("tone")
                        && type.hasAttribute("width")
                        && type.hasAttribute("enabled"),
                "Annotation-backed registration should include annotated, inherited and shared layout/style attributes");
        List<XmlAttributeDescriptor> annotatedBadgeAttributes = registry.descriptor("AnnotatedBadge").orElseThrow().attributes();
        expect(descriptor(annotatedBadgeAttributes, "importance").category().equals("Behavior")
                        && descriptor(annotatedBadgeAttributes, "importance").defaultValue().equals("0")
                        && descriptor(annotatedBadgeAttributes, "importance").valueType() == XmlAttributeValueType.NUMBER
                        && descriptor(annotatedBadgeAttributes, "active").valueType() == XmlAttributeValueType.BOOLEAN
                        && descriptor(annotatedBadgeAttributes, "tone").valueType() == XmlAttributeValueType.ENUM
                        && descriptor(annotatedBadgeAttributes, "accent").valueType() == XmlAttributeValueType.COLOR,
                "Annotation-backed registration should preserve @XmlAttribute descriptor metadata");

        AnnotatedBadge badge = XMLWidget.create("""
                <AnnotatedBadge id="ann" text="Annotated" importance="4.5" active="true"
                                tone="warning" accent="#336699CC" width="88" />
                """, AnnotatedBadge.class, registry);
        expect(badge.id().equals("ann")
                        && badge.text().equals("Annotated")
                        && near(badge.importance(), 4.5f)
                        && badge.active()
                        && badge.tone() == AnnotatedBadge.Tone.WARNING,
                "Annotation-backed registration should materialize custom widget setters from XML");
        expect(near(badge.accent().r(), 0x33 / 255.0f)
                        && near(badge.accent().a(), 0xCC / 255.0f)
                        && near(badge.layoutStyle().width().value(), 88.0f),
                "Annotation-backed registration should infer color and layout parsers");

        XmlWidgetRegistry factoryRegistry = XMLWidget.emptyRegistry();
        factoryRegistry.registerAnnotated(AnnotatedBadge.class, () -> new AnnotatedBadge().importance(9.0f));
        AnnotatedBadge factoryBadge = XMLWidget.create("<AnnotatedBadge text=\"Factory\" />",
                AnnotatedBadge.class,
                factoryRegistry);
        expect(near(factoryBadge.importance(), 9.0f) && factoryBadge.text().equals("Factory"),
                "Annotation-backed registration should support explicit factories for custom construction");

        AutoCloseable contribution = XmlWidgetRegistryContributions.registerAnnotated(AnnotatedBadge.class);
        try {
            AnnotatedBadge global = XMLWidget.create("<AnnotatedBadge text=\"Global Ann\" />", AnnotatedBadge.class);
            expect(global.text().equals("Global Ann"),
                    "Global annotation-backed contributions should apply to fresh built-in registry copies");
        } finally {
            closeUnchecked(contribution);
        }
        expectFails("<AnnotatedBadge text=\"Removed\" />", "Unknown widget type 'AnnotatedBadge'");

        expectFailsIllegal(() -> XMLWidget.emptyRegistry().registerAnnotated(Badge.class),
                "registerAnnotated should require @XmlWidgetName on the concrete widget type");
    }

    private void testCommandRegistryApi() {
        int[] clicks = {0};
        String[] seen = {""};
        XmlCommandRegistry commands = XmlCommandRegistry.empty()
                .register("video.apply", (source, event) -> {
                    clicks[0]++;
                    seen[0] = source.id() + ":" + event.type().id();
                });

        Button apply = XMLWidget.create(
                "<Button id=\"apply\" text=\"Apply\" onClick=\"video.apply\" />",
                Button.class,
                XmlWidgetOptions.DEFAULT.commands(commands));
        apply.click();

        expect(clicks[0] == 1 && seen[0].equals("apply:button.click"),
                "XML command registry should wire Button onClick to a named Java handler");
        expect(descriptor(XMLWidget.registry().descriptor("Button").orElseThrow().attributes(), "onClick")
                        .valueType() == XmlAttributeValueType.BINDING_OR_ACTION,
                "Button onClick descriptor should expose action metadata for editor inspectors");
        expectFails("<Button onClick=\"video.missing\" />", Button.class, "Unknown XML command 'video.missing'");
        expectFails("<Button onClick=\"\" />", Button.class, "onClick command name must not be blank");
        expectFailsUnsupported(() -> XmlCommandRegistry.none().register("bad", (source, event) -> {}),
                "Default XML command registry should be immutable");
    }

    private void testTypedObservableBindingModel() {
        XmlMutableObservableValue<Float> gamma = XmlMutableObservableValue.of("video.gamma", Float.class, 0.5f);
        int[] changes = {0};
        float[] oldValue = {-1.0f};
        float[] newValue = {-1.0f};

        EventSubscription subscription = gamma.onChanged(change -> {
            changes[0]++;
            oldValue[0] = change.oldValue();
            newValue[0] = change.newValue();
            expect(change.name().equals("video.gamma") && change.valueType() == Float.class,
                    "Observable binding changes should carry stable typed metadata");
        });

        gamma.set(0.75f).set(0.75f);
        expect(changes[0] == 1 && near(oldValue[0], 0.5f) && near(newValue[0], 0.75f),
                "Observable binding values should notify once per effective value change");
        subscription.unsubscribe();
        gamma.set(0.9f);
        expect(changes[0] == 1 && near(gamma.get(), 0.9f),
                "Unsubscribed XML binding listeners should stop receiving updates");

        @SuppressWarnings({"rawtypes", "unchecked"})
        XmlMutableObservableValue rawGamma = gamma;
        expectFailsIllegal(() -> rawGamma.set("bad"),
                "Observable binding values should reject values outside their declared type");

        float[] sliderValue = {-1.0f};
        XmlBinding<Float> binding = XmlBinding.bind(gamma, Float.class, value -> sliderValue[0] = value, "Slider.value");
        expect(binding.active() && near(sliderValue[0], 0.9f),
                "Typed XML bindings should immediately push the current source value into the target");
        gamma.set(1.0f);
        expect(near(sliderValue[0], 1.0f), "Typed XML bindings should push source updates into the target");
        binding.close();
        gamma.set(1.2f);
        expect(near(sliderValue[0], 1.0f) && binding.status().state() == XmlBindingStatus.State.CLOSED,
                "Closing a typed XML binding should unsubscribe and expose closed editor status");

        XmlBinding<String> wrongType = XmlBinding.bind(gamma, String.class, value -> {
            throw new AssertionError("Wrong-type XML binding target should not be invoked");
        }, "Label.text");
        expect(!wrongType.active() && wrongType.status().hasDiagnostics()
                        && wrongType.status().summary().contains("expected String"),
                "Wrong-type XML bindings should produce diagnostics instead of invoking targets");

        XmlBindingContext context = XmlBindingContext.empty();
        context.value("video.gamma", gamma);
        XmlMutableObservableValue<String> graphicsLabel = context.mutable("video.graphicsLabel", String.class, "Fancy");
        expect(context.paths().size() == 2 && context.resolve("video.gamma", Float.class).orElseThrow() == gamma,
                "Binding contexts should resolve named values with type checks");
        expectFailsUnsupported(() -> context.paths().add("oops"),
                "Binding context path snapshots should be immutable");

        String[] labelText = {""};
        XmlBinding<String> labelBinding = context.bind("video.graphicsLabel", String.class, value -> labelText[0] = value);
        graphicsLabel.set("Fast");
        expect(labelBinding.active() && labelText[0].equals("Fast"),
                "Binding contexts should create active typed bindings for registered values");

        XmlBinding<Float> missing = context.bind("video.missing", Float.class, value -> {
            throw new AssertionError("Missing XML binding target should not be invoked");
        });
        XmlBindingStatus mismatch = context.status("video.gamma", String.class);
        XmlBindingDiagnosticsModel diagnostics = XmlBindingDiagnosticsModel.from(List.of(
                labelBinding.status(),
                missing.status(),
                mismatch));

        expect(diagnostics.entries().size() == 3 && diagnostics.hasErrors() && diagnostics.errorCount() == 2,
                "Binding diagnostics model should expose active, missing and mismatched statuses");
        expect(diagnostics.entries().get(1).displayText().contains("video.missing"),
                "Binding diagnostics entries should include the binding path for editor panels");
        expectFailsUnsupported(() -> diagnostics.entries().clear(),
                "Binding diagnostics entries should be immutable snapshots");
        expect(XmlBindingDiagnosticsModel.empty().summary().equals("No XML binding diagnostics."),
                "Empty binding diagnostics model should have a stable summary");
        labelBinding.close();
    }

    private void testXmlBindingExpressions() {
        XmlBindingContext context = XmlBindingContext.empty();
        XmlMutableObservableValue<String> title = context.mutable("user.name", String.class, "Initial");
        XmlMutableObservableValue<Boolean> showSettings = context.mutable("ui.showSettings", Boolean.class, true);
        XmlMutableObservableValue<Float> gamma = context.mutable("settings.gamma", Float.class, 0.25f);

        XmlWidgetScreen<VBox> screen = XMLWidget.createScreen("""
                <VBox id="root" visible="{Binding ui.showSettings}">
                    <Label id="title" text="{Binding user.name}" />
                    <Slider id="gamma" value="{Binding settings.gamma}" />
                </VBox>
                """, VBox.class, XmlWidgetOptions.DEFAULT.bindingContext(context));

        Label label = XMLWidget.findWidget(screen.root(), "title", Label.class).orElseThrow();
        Slider slider = XMLWidget.findWidget(screen.root(), "gamma", Slider.class).orElseThrow();
        expect(label.text().equals("Initial") && near(slider.value(), 0.25f) && screen.root().visible(),
                "XML binding expressions should push initial source values into widget properties");

        title.set("Updated");
        gamma.set(0.75f);
        showSettings.set(false);
        expect(label.text().equals("Updated") && near(slider.value(), 0.75f) && !screen.root().visible(),
                "XML binding expressions should update widget properties when sources change");
        expect(screen.bindingSession().diagnostics().isEmpty(),
                "Valid XML binding expressions should not produce diagnostics");

        screen.close();
        title.set("After close");
        expect(label.text().equals("Updated") && screen.bindingSession().closed(),
                "Closing an XML screen should unsubscribe all binding expressions");

        XmlBindingContext invalidContext = XmlBindingContext.empty();
        invalidContext.mutable("settings.gamma", Float.class, 0.5f);
        XmlWidgetScreen<Label> invalid = XMLWidget.createScreen(
                "<Label text=\"{Binding settings.gamma}\" />",
                Label.class,
                XmlWidgetOptions.DEFAULT.bindingContext(invalidContext));
        expect(invalid.bindingSession().diagnostics().size() == 1
                        && invalid.bindingSession().diagnostics().get(0).state() == XmlBindingStatus.State.ERROR,
                "XML binding type mismatches should remain visible as diagnostics");
        invalid.close();
    }


    private void testXmlCodeEditorWidgetContracts() {
        XmlCodeEditor editor = new XmlCodeEditor().loadText("<VBox><Label text=\"Hi\" /></VBox>");
        expect(editor.lineCount() == 1, "CodeEditor should expose line count for gutter and diagnostics navigation");
        editor.cursorIndex(6);
        expect(editor.cursorIndex() == 6, "XmlCodeEditor should inherit cursor positioning from TextArea");
        editor.select(7, 12);
        expect(editor.selectedText().equals("Label"),
                "XmlCodeEditor should inherit text selection from the multiline TextArea");

        editor.loadText("<VBox>\n    <Label text=\"One\" />\n    <Label text=\"Two\" />\n</VBox>");
        editor.visibleLines(1);
        editor.lineHeight(12.0f);
        expect(editor.lineCount() == 4, "CodeEditor should count multiline XML rows for line-number display");
        editor.scrollToLine(3);
        expect(editor.scrollY() > 0.0f, "CodeEditor should scroll to a requested XML source line");

        editor.loadText("<VBox><Label text=\"Hi\" /></VBox>");
        expect(editor.formatXml(), "XmlCodeEditor should format syntactically valid XML");
        expect(editor.text().contains("\n    <Label") && editor.dirty(),
                "XmlCodeEditor format action should write pretty XML and mark changed text dirty");
        editor.markClean();
        expect(editor.validateXml() && editor.diagnostics().isEmpty(),
                "XmlCodeEditor should validate descriptor-clean XML without diagnostics");

        editor.loadText("""
                <VBox>
                    <Missing />
                </VBox>
                """);
        expect(!editor.validateXml()
                        && editor.diagnostics().size() == 1
                        && editor.firstDiagnostic().orElseThrow().hasLocation()
                        && editor.firstDiagnostic().orElseThrow().line() == 2
                        && editor.firstDiagnostic().orElseThrow().column() > 0,
                "XmlCodeEditor should map XML validation diagnostics onto code diagnostics");
        editor.scrollToFirstDiagnostic();
        expect(editor.scrollY() > 0.0f, "CodeEditor should navigate to the first located diagnostic line");

        editor.loadText("<VBox>");
        expect(!editor.formatXml() && !editor.diagnostics().isEmpty(),
                "XmlCodeEditor format should fail softly and expose parse diagnostics");

        XmlCodeEditor fromXml = XMLWidget.create("""
                <XmlCodeEditor id="source" text="&lt;VBox /&gt;" lineNumbers="false" visibleLines="4" languagePreset="xaml" />
                """, XmlCodeEditor.class);
        expect(fromXml.id().equals("source")
                        && fromXml.text().equals("<VBox />")
                        && !fromXml.lineNumbersVisible()
                        && fromXml.visibleLines() == 4
                        && fromXml.languagePreset() == CodeLanguagePreset.XAML
                        && fromXml.languageId().equals("xaml"),
                "XmlCodeEditor should materialize from built-in XML registration");
    }

    private void testDescriptorBackedInspectorModel() {
        XmlWidgetElement box = new XmlWidgetElement("Box")
                .attribute("id", "panel")
                .attribute("backgroundTexture", "test:texture")
                .attribute("futureEditorAttr", "keep");

        XmlWidgetInspector.Inspection boxInspection = XmlWidgetInspector.inspect(box, XMLWidget.registry());
        expect(boxInspection.knownWidget() && boxInspection.displayName().equals("Box"),
                "Inspector should resolve widget descriptor metadata");
        expect(boxInspection.attribute("id").orElseThrow().known()
                        && boxInspection.attribute("id").orElseThrow().displayName().equals("Id"),
                "Inspector should expose id/name as built-in inspectable metadata");
        expect(boxInspection.attribute("backgroundTexture").orElseThrow().category().equals("Assets"),
                "Inspector should use descriptor metadata for registered attributes");
        expect(!boxInspection.attribute("futureEditorAttr").orElseThrow().known(),
                "Inspector should preserve unknown source attributes for editor UI");
        expect(boxInspection.availableAttributes().stream().anyMatch(attribute -> attribute.name().equals("radius")),
                "Inspector should expose available descriptor attributes for add-field UI");
        expectFailsUnsupported(() -> boxInspection.availableAttributes().clear(),
                "Inspector available attributes should be immutable");

        XmlWidgetElement named = new XmlWidgetElement("Button")
                .attribute("x:Name", "apply")
                .attribute("text", "Apply");
        XmlWidgetInspector.Inspection namedInspection = XmlWidgetInspector.inspect(named);
        expect(namedInspection.attribute("x:Name").orElseThrow().known()
                        && namedInspection.attribute("Name").orElseThrow().value().equals("apply"),
                "Inspector should recognize namespace-prefixed name aliases");

        XmlWidgetInspector.Inspection vboxInspection = XmlWidgetInspector.inspect(new XmlWidgetElement("VBox"));
        expect(vboxInspection.propertyChildren().stream().anyMatch(property -> property.name().equals("Children")),
                "Inspector should expose descriptor property children for editor add-child UI");

        XmlWidgetInspector.Inspection unknown = XmlWidgetInspector.inspect(new XmlWidgetElement("UnknownWidget"));
        expect(!unknown.knownWidget() && unknown.category().equals("Unknown"),
                "Inspector should return a stable result for unknown widget elements");
    }

    private void testTextureXmlAttributes() {
        Box texturedBox = XMLWidget.create("""
                <Box id="textured"
                     backgroundTexture="test_mod:uniformclouds-1"
                     backgroundTextureWidth="256"
                     backgroundTextureHeight="128"
                     backgroundTextureFit="cover"
                     backgroundTextureTint="#80A0C0CC"
                     backgroundTextureSource="0.25 0 0.5 1"
                     backgroundTextureSampling="linear"
                     backgroundTextureMipmaps="true" />
                """, Box.class);

        expect(texturedBox.backgroundTexture() != null, "Box backgroundTexture XML attribute should create a texture handle");
        expect(texturedBox.backgroundTexture().id().equals("test_mod:uniformclouds-1"),
                "Box backgroundTexture should preserve resource id");
        expect(texturedBox.backgroundTexture().width() == 256 && texturedBox.backgroundTexture().height() == 128,
                "Box background texture dimensions should be adjustable from XML");
        expect(texturedBox.backgroundTexture().options().minFilter() == TextureFilter.LINEAR
                        && texturedBox.backgroundTexture().options().mipmaps(),
                "Box background texture options should be adjustable from XML");
        expect(texturedBox.backgroundTextureFit() == ImageFit.COVER, "Box backgroundTextureFit should parse ImageFit");
        expect(near(texturedBox.backgroundTextureTint().r(), 0x80 / 255.0f)
                        && near(texturedBox.backgroundTextureTint().a(), 0xCC / 255.0f),
                "Box backgroundTextureTint should parse #RRGGBBAA");
        expect(near(texturedBox.backgroundTextureSource().x(), 0.25f)
                        && near(texturedBox.backgroundTextureSource().width(), 0.5f),
                "Box backgroundTextureSource should parse UV rect values");

        Box slicedBox = XMLWidget.create("""
                <Box id="sliced"
                     backgroundTexture="test_mod:panel"
                     backgroundTextureWidth="20"
                     backgroundTextureHeight="20"
                     backgroundTextureFit="nine-slice"
                     sliceBorders="6" />
                """, Box.class);
        expect(slicedBox.backgroundTextureFit() == ImageFit.NINE_SLICE,
                "Box backgroundTextureFit should parse nine-slice");
        expect(slicedBox.sliceBorders().equals(new SliceBorders(6.0f, 6.0f, 6.0f, 6.0f)),
                "Box sliceBorders should parse a single uniform value");

        ImageView slicedImage = XMLWidget.create("""
                <Image texture="test_mod:frame"
                       fit="nine-slice"
                       sliceBorders="4 5 6 7" />
                """, ImageView.class);
        expect(slicedImage.fit() == ImageFit.NINE_SLICE, "TextureWidget fit should parse nine-slice");
        expect(slicedImage.sliceBorders().equals(new SliceBorders(4.0f, 5.0f, 6.0f, 7.0f)),
                "TextureWidget sliceBorders should parse left top right bottom");

        ImageView image = XMLWidget.create("""
                <Image id="preview"
                       texture="minecraft:textures/item/diamond.png"
                       textureWidth="32"
                       textureHeight="32"
                       fit="contain"
                       tint="#FFFFFFFF"
                       source="0 0 1 1"
                       radius="2"
                       textureSampling="nearest" />
                """, ImageView.class);

        expect(image.texture() != null && image.texture().id().equals("minecraft:textures/item/diamond.png"),
                "Image alias should create ImageView with a texture handle");
        expect(image.texture().width() == 32 && image.texture().height() == 32,
                "TextureWidget dimensions should be adjustable from XML");
        expect(image.fit() == ImageFit.CONTAIN, "TextureWidget fit should parse ImageFit");
        expect(near(image.radius(), 2.0f), "TextureWidget radius should apply from XML");
        expect(near(image.source().width(), 1.0f) && image.texture().options().minFilter() == TextureFilter.NEAREST,
                "TextureWidget source and sampling should apply from XML");

        Object nativeTexture = new Object();
        XmlWidgetOptions resolverOptions = XmlWidgetOptions.DEFAULT.textureResolver((id, width, height, options) ->
                new SimpleTextureHandle(id, width, height, nativeTexture, options));
        ImageView resolvedImage = XMLWidget.create("""
                <Image texture="test_mod:resolved"
                       textureWidth="48"
                       textureHeight="24"
                       textureSampling="linear" />
                """, ImageView.class, resolverOptions);
        expect(resolvedImage.texture().nativeHandle() == nativeTexture,
                "Texture resolver hook should provide runtime-specific texture handles");
        expect(resolvedImage.texture().width() == 48
                        && resolvedImage.texture().height() == 24
                        && resolvedImage.texture().options().minFilter() == TextureFilter.LINEAR,
                "Texture resolver hook should survive XML dimension and option attributes");

        TextWidget inlineText = XMLWidget.create("""
                <Text text="A {icon:test_mod:inline_text@9x7} B" />
                """, TextWidget.class, resolverOptions);
        expect(inlineText.text().equals("A {icon:test_mod:inline_text@9x7} B")
                        && inlineText.richText().hasInlineContent()
                        && inlineText.richText().plainText().equals("A " + InlineContentSpan.DEFAULT_FALLBACK_TEXT + " B"),
                "XML text attributes should resolve inline icon markers through the configured texture resolver");

        expectFails("<Image texture=\"\" />", "Texture id must not be blank");
        expectFails("<Image texture=\"test:bad\" source=\"0 0 1\" />", "Expected 4 rect values");
    }

    private void testXmlAssetCatalogAndPickerModel() {
        XmlWidgetAssetCatalog catalog = XmlWidgetAssetCatalog.builder()
                .add(XmlWidgetAsset.texture("test:icon", 64, 32).displayName("Test Icon"))
                .texture("minecraft:textures/block/stone.png", 16, 16)
                .font("unigui:default")
                .shader("unigui:glow")
                .build();

        expect(catalog.assets().size() == 4 && catalog.assets(XmlWidgetAssetKind.TEXTURE).size() == 2,
                "XML asset catalog should retain texture/font/shader entries by kind");
        expect(catalog.find(XmlWidgetAssetKind.TEXTURE, "test:icon").orElseThrow().hasDimensions(),
                "Texture asset entries should expose optional manifest dimensions");
        expect(catalog.search(XmlWidgetAssetKind.TEXTURE, "icon").size() == 1,
                "XML asset catalog should support query filtering for picker UIs");
        expectFailsUnsupported(() -> catalog.assets().clear(),
                "XML asset catalog entries should be immutable snapshots");

        XmlWidgetAssetCatalog providerCatalog = XmlWidgetAssetCatalog.builder()
                .texture("test_mod:coin", 16, 16)
                .build();
        AutoCloseable catalogProvider = XmlWidgetAssetProviders.registerCatalog(providerCatalog);
        AutoCloseable directProvider = XmlWidgetAssetProviders.register(builder -> builder
                .add(XmlWidgetAsset.texture("test:icon", 128, 64).displayName("Override Icon"))
                .add(XmlWidgetAsset.texture("test_mod:shop_icon", 32, 32)
                        .displayName("Shop Icon")
                        .description("Registered host mod icon")));
        try {
            XmlWidgetAssetCatalog providerOnly = XmlWidgetAssetProviders.catalog();
            expect(providerOnly.find(XmlWidgetAssetKind.TEXTURE, "test_mod:coin").isPresent()
                            && providerOnly.find(XmlWidgetAssetKind.TEXTURE, "test_mod:shop_icon").isPresent(),
                    "XML asset providers should build a catalog from registered mod contributions");
            expect(providerOnly.search(XmlWidgetAssetKind.TEXTURE, "shop").size() == 1,
                    "XML asset providers should preserve display-name/description search metadata");

            XmlWidgetAssetCatalog mergedCatalog = XmlWidgetAssetProviders.catalog(catalog);
            expect(mergedCatalog.find(XmlWidgetAssetKind.TEXTURE, "test_mod:coin").isPresent()
                            && mergedCatalog.find(XmlWidgetAssetKind.TEXTURE, "test:icon").orElseThrow().width() == 128,
                    "XML asset providers should merge with base catalogs and override duplicate ids last");
        } finally {
            closeUnchecked(directProvider);
            closeUnchecked(catalogProvider);
        }
        expect(XmlWidgetAssetProviders.catalog().find(XmlWidgetAssetKind.TEXTURE, "test_mod:coin").isEmpty()
                        && XmlWidgetAssetProviders.catalog().find(XmlWidgetAssetKind.TEXTURE, "test_mod:shop_icon").isEmpty(),
                "Closing XML asset provider handles should unregister mod contributions");

        XmlWidgetAssetPickerModel picker = new XmlWidgetAssetPickerModel(catalog, XmlWidgetAssetKind.TEXTURE)
                .query("stone")
                .select("minecraft:textures/block/stone.png");
        expect(picker.visibleAssets().size() == 1
                        && picker.selectedAsset().orElseThrow().id().equals("minecraft:textures/block/stone.png"),
                "XML asset picker model should filter and select assets for editor controls");
        picker.kind(XmlWidgetAssetKind.SHADER).query("glow").select("unigui:glow");
        expect(picker.visibleAssets().size() == 1 && picker.selectedId().equals("unigui:glow"),
                "XML asset picker model should switch between texture/font/shader categories");

        XmlWidgetAssetPickerPanel panel = new XmlWidgetAssetPickerPanel(
                new XmlWidgetAssetPickerModel(catalog, XmlWidgetAssetKind.TEXTURE))
                .entryLimit(1);
        expect(panel.kindBar().children().size() == 3
                        && panel.entriesHost().children().size() == 2
                        && panel.summaryLabel().text().contains("2 texture assets"),
                "XML asset picker panel should render kind tabs, asset rows and overflow summary rows");
        ((Button) panel.entriesHost().children().get(0)).click();
        expect(panel.model().selectedAsset().orElseThrow().id().equals("test:icon")
                        && panel.summaryLabel().text().contains("selected test:icon"),
                "XML asset picker panel rows should select catalog assets");
        panel.query("stone");
        expect(panel.entriesHost().children().size() == 1
                        && panel.summaryLabel().text().contains("matching 'stone'"),
                "XML asset picker panel should rebuild rows from model query filters");
        panel.query("");
        ((Button) panel.kindBar().children().get(1)).click();
        expect(panel.model().kind() == XmlWidgetAssetKind.FONT
                        && panel.model().selectedId().isEmpty()
                        && panel.entriesHost().children().size() == 1,
                "XML asset picker panel kind tabs should switch picker categories and clear invalid selections");
        panel.select("unigui:default");
        expect(panel.model().selectedAsset().orElseThrow().kind() == XmlWidgetAssetKind.FONT,
                "XML asset picker panel should allow code-driven selection in the active category");
        panel.kind(XmlWidgetAssetKind.SHADER).query("missing");
        expect(panel.entriesHost().children().size() == 1
                        && panel.summaryLabel().text().contains("0 shader assets"),
                "XML asset picker panel should render a stable empty-state row for filtered results");

        ImageView manifestSized = XMLWidget.create("<Image texture=\"test:icon\" />",
                ImageView.class,
                XmlWidgetOptions.DEFAULT.textureResolver(catalog.textureResolver()));
        expect(manifestSized.texture().width() == 64 && manifestSized.texture().height() == 32,
                "XML texture resolver should use catalog dimensions as manifest defaults");

        ImageView explicitSize = XMLWidget.create("""
                <Image texture="test:icon" textureWidth="24" textureHeight="12" />
                """, ImageView.class, XmlWidgetOptions.DEFAULT.textureResolver(catalog.textureResolver()));
        expect(explicitSize.texture().width() == 24 && explicitSize.texture().height() == 12,
                "Explicit XML texture dimensions should override catalog manifest defaults");
    }

    private void testScrollViewContentChildPolicy() {
        ScrollView scroll = XMLWidget.create("""
                <ScrollView id="scroll" width="120" height="48" overflowY="auto">
                    <VBox id="content" spacing="2">
                        <Label id="row" text="Row" />
                    </VBox>
                </ScrollView>
                """, ScrollView.class);

        expect(scroll.content() instanceof VBox, "ScrollView first XML child should become content");
        expect(XMLWidget.getWidget(scroll, "content", VBox.class).spacing() == 2.0f,
                "getWidget should traverse ScrollView content children");
        expect(XMLWidget.getWidget(scroll, "row", Label.class).text().equals("Row"),
                "getWidget should find nested content descendants");
    }

    private void testDocumentRoundTripModel() {
        XmlWidgetDocument document = XmlWidgetDocument.parse("""
                <VBox xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" x:Name="root" spacing="6">
                    <!-- keep me -->
                    <VBox.Children>
                        <Label id="title" text="Hello &amp; XML" />
                    </VBox.Children>
                </VBox>
                """);

        XmlWidgetElement root = document.root();
        expect(root.name().equals("VBox"), "Document model should preserve the root element name");
        expect(root.attribute("x:Name").orElseThrow().equals("root"),
                "Document model should preserve namespace-prefixed id aliases");
        expect(root.children().stream().anyMatch(child -> child.kind() == XmlWidgetNode.Kind.COMMENT),
                "Document model should preserve simple comments for editor round-trip");

        XmlWidgetElement children = root.elementChildren().stream()
                .filter(child -> child.name().equals("VBox.Children"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Property element should be preserved in the document model"));
        expect(children.propertyElement(), "Property elements should be detectable in the source document model");
        expect(children.elementChildren().get(0).name().equals("Label"),
                "Property element widget children should remain normal source elements");

        String pretty = document.toXmlString(XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false));
        expect(pretty.contains("<!-- keep me -->"), "Serializer should keep preserved XML comments");
        expect(pretty.contains("<VBox.Children>"), "Serializer should keep editor-authored property elements");
        expect(pretty.contains("Hello &amp; XML"), "Serializer should escape text and attribute values");

        XmlWidgetDocument reparsed = XmlWidgetDocument.parse(pretty);
        expect(reparsed.root().name().equals("VBox"), "Serialized XML should parse back into a document root");
        expect(reparsed.root().attribute("x:Name").orElseThrow().equals("root"),
                "Serialized XML should keep stable id/name values during reparse");

        XmlWidgetDocument copy = document.copy();
        copy.root().setAttribute("spacing", "9");
        expect(root.attribute("spacing").orElseThrow().equals("6"),
                "Document copies should be deep enough for editor mutations");

        root.setAttribute("spacing", "8");
        children.addElement(new XmlWidgetElement("Button").attribute("id", "apply").attribute("text", "Apply"));
        String mutated = document.toXmlString(XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false));
        VBox loaded = XMLWidget.create(mutated, VBox.class);
        expect(near(loaded.spacing(), 8.0f), "Serialized document mutations should load into runtime widgets");
        expect(XMLWidget.getWidget(loaded, "title", Label.class).text().equals("Hello & XML"),
                "Round-tripped escaped attributes should load as decoded runtime values");
        expect(XMLWidget.getWidget(loaded, "apply", Button.class).text().equals("Apply"),
                "Editor-added document elements should load as runtime widgets");

        XmlWidgetDocument golden = XmlWidgetDocument.of(new XmlWidgetElement("Button")
                .attribute("id", "golden")
                .attribute("text", "Save & Close"));
        expect(golden.toXmlString(XmlWidgetSerializationOptions.COMPACT).equals("<Button id=\"golden\" text=\"Save &amp; Close\" />"),
                "Compact serializer output should be deterministic for golden tests");
    }

    private void testRuntimeWidgetSnapshotSerializer() {
        VBox root = new VBox();
        root.spacing(6.0f);
        root.id("root");
        root.layout(style -> style.size(240.0f, 120.0f).padding(4.0f));
        Label title = new Label("Runtime Title");
        title.id("title");
        Button apply = new Button("Apply");
        apply.id("apply");
        Slider gamma = new Slider().range(0.0f, 10.0f).value(4.5f).step(0.5f);
        gamma.id("gamma");
        root.addChild(title);
        root.addChild(apply);
        root.addChild(gamma);
        root.applyQueuedMutations();

        XmlWidgetDocumentResult snapshot = XmlWidgetRuntimeSerializer.snapshot(root);
        expect(snapshot.valid(), "Runtime serializer should export built-in XML widgets without diagnostics");
        String xml = snapshot.document().toXmlString(XmlWidgetSerializationOptions.COMPACT);
        expect(xml.contains("<VBox")
                        && xml.contains("spacing=\"6\"")
                        && xml.contains("padding=\"4 4 4 4\"")
                        && xml.contains("text=\"Runtime Title\"")
                        && xml.contains("value=\"4.5\""),
                "Runtime serializer should write common layout and widget-specific XML attributes");

        VBox loaded = XMLWidget.create(xml, VBox.class);
        expect(loaded.id().equals("root") && near(loaded.spacing(), 6.0f),
                "Runtime snapshot XML should reload root id and container state");
        expect(XMLWidget.getWidget(loaded, "title", Label.class).text().equals("Runtime Title")
                        && near(XMLWidget.getWidget(loaded, "gamma", Slider.class).value(), 4.5f),
                "Runtime snapshot XML should reload nested text and control state");

        FlexBox flex = new FlexBox();
        flex.layout(style -> style
                .flexDirection(FlexDirection.ROW)
                .flexWrap(FlexWrap.WRAP)
                .gap(4.0f, 8.0f)
                .alignItems(Align.CENTER)
                .justifyContent(Justify.SPACE_BETWEEN));
        flex.addChild(new Label("Flex child"));
        flex.applyQueuedMutations();
        String flexXml = XmlWidgetRuntimeSerializer.snapshot(flex).document()
                .toXmlString(XmlWidgetSerializationOptions.COMPACT);
        expect(flexXml.contains("flexDirection=\"row\"")
                        && flexXml.contains("flexWrap=\"wrap\"")
                        && flexXml.contains("rowGap=\"4\"")
                        && flexXml.contains("columnGap=\"8\"")
                        && flexXml.contains("alignItems=\"center\"")
                        && flexXml.contains("justifyContent=\"space-between\""),
                "Runtime-сериализатор должен сохранять CSS-подобные layout-атрибуты FlexBox");
        FlexBox loadedFlex = XMLWidget.create(flexXml, FlexBox.class);
        expect(loadedFlex.layoutStyle().flexDirection() == FlexDirection.ROW
                        && loadedFlex.layoutStyle().flexWrap() == FlexWrap.WRAP
                        && near(loadedFlex.layoutStyle().rowGap(), 4.0f)
                        && near(loadedFlex.layoutStyle().columnGap(), 8.0f)
                        && loadedFlex.layoutStyle().alignItems() == Align.CENTER
                        && loadedFlex.layoutStyle().justifyContent() == Justify.SPACE_BETWEEN,
                "Сериализованные layout-атрибуты FlexBox должны восстанавливаться в стиле");

        UnsupportedXmlWidget unsupported = new UnsupportedXmlWidget();
        unsupported.id("custom");
        XmlWidgetDocumentResult unsupportedSnapshot = XmlWidgetRuntimeSerializer.snapshot(unsupported);
        expect(unsupportedSnapshot.hasDiagnostics()
                        && unsupportedSnapshot.firstDiagnostic().orElseThrow().message().contains("does not have a complete XML snapshot exporter")
                        && unsupportedSnapshot.document().root().attribute("id").orElseThrow().equals("custom"),
                "Runtime serializer should preserve unsupported widget ids while reporting export diagnostics");
    }

    private void testLayoutAttributesRoundTrip() {
        GridBox grid = new GridBox();
        grid.id("grid");
        grid.columns(3).horizontalSpacing(2.0f).verticalSpacing(4.0f);
        grid.layout(style -> style
                .gridTemplateColumns(List.of(GridTrack.px(100.0f), GridTrack.fr(1.0f), GridTrack.auto()))
                .gridTemplateRows(List.of(GridTrack.minmax(GridTrack.px(20.0f), GridTrack.fr(1.0f))))
                .gridAutoColumns(List.of(GridTrack.px(50.0f)))
                .gridAutoRows(List.of(GridTrack.px(25.0f)))
                .gridAutoFlow(GridAutoFlow.COLUMN_DENSE)
                .justifyContent(Justify.CENTER)
                .alignContent(AlignContent.SPACE_BETWEEN));
        Box cell = new Box();
        cell.id("cell");
        cell.layout(style -> style
                .gridColumn(2, 2)
                .gridRow(1, 3)
                .order(2)
                .aspectRatio(1.5f)
                .marginAuto(AutoMargins.HORIZONTAL)
                .zIndex(5)
                .flexBasis(SizeValue.minContent())
                .minWidth(SizeValue.minContent()));
        grid.addChild(cell);
        grid.applyQueuedMutations();

        FlexBox flex = new FlexBox();
        flex.id("flex");
        flex.layout(style -> style
                .flexDirection(FlexDirection.ROW_REVERSE)
                .flexWrap(FlexWrap.WRAP_REVERSE)
                .alignSelf(Align.BASELINE));
        Label text = new Label("Text");
        text.id("text");
        text.layout(style -> style
                .width(SizeValue.fitContent(30.0f))
                .position(PositionType.FIXED)
                .alignSelf(Align.BASELINE));
        flex.addChild(text);
        flex.applyQueuedMutations();

        VBox root = new VBox();
        root.addChild(grid);
        root.addChild(flex);
        root.applyQueuedMutations();

        XmlWidgetDocumentResult snapshot = XmlWidgetRuntimeSerializer.snapshot(root);
        expect(snapshot.valid(), "Runtime serializer should export new layout attributes without diagnostics");
        String xml = snapshot.document().toXmlString(XmlWidgetSerializationOptions.COMPACT);
        expect(xml.contains("gridTemplateColumns=\"100 1fr auto\"")
                        && xml.contains("gridTemplateRows=\"minmax(20, 1fr)\"")
                        && xml.contains("gridAutoColumns=\"50\"")
                        && xml.contains("gridAutoRows=\"25\"")
                        && xml.contains("gridAutoFlow=\"column-dense\"")
                        && xml.contains("justifyContent=\"center\"")
                        && xml.contains("alignContent=\"space-between\"")
                        && xml.contains("columns=\"3\"")
                        && xml.contains("horizontalSpacing=\"2\"")
                        && xml.contains("verticalSpacing=\"4\""),
                "Runtime serializer should write grid template and alignment attributes");
        expect(xml.contains("gridColumn=\"2\"")
                        && xml.contains("gridColumnSpan=\"2\"")
                        && xml.contains("gridRow=\"1\"")
                        && xml.contains("gridRowSpan=\"3\"")
                        && xml.contains("order=\"2\"")
                        && xml.contains("aspectRatio=\"1.5\"")
                        && xml.contains("marginAuto=\"horizontal\"")
                        && xml.contains("zIndex=\"5\"")
                        && xml.contains("flexBasis=\"min-content\"")
                        && xml.contains("minWidth=\"min-content\""),
                "Runtime serializer should write grid placement and item layout attributes");
        expect(xml.contains("flexDirection=\"row-reverse\"")
                        && xml.contains("flexWrap=\"wrap-reverse\"")
                        && xml.contains("alignSelf=\"baseline\"")
                        && xml.contains("width=\"fit-content(30px)\"")
                        && xml.contains("position=\"fixed\""),
                "Runtime serializer should write reverse, baseline, fit-content and fixed attributes");

        VBox loaded = XMLWidget.create(xml, VBox.class);
        GridBox loadedGrid = (GridBox) loaded.children().get(0);
        expect(loadedGrid.columns() == 3
                        && near(loadedGrid.horizontalSpacing(), 2.0f)
                        && near(loadedGrid.verticalSpacing(), 4.0f),
                "Reloaded grid should restore columns and spacing state");
        expect(loadedGrid.layoutStyle().gridTemplateColumns().equals(
                                List.of(GridTrack.px(100.0f), GridTrack.fr(1.0f), GridTrack.auto()))
                        && loadedGrid.layoutStyle().gridTemplateRows().equals(
                                List.of(GridTrack.minmax(GridTrack.px(20.0f), GridTrack.fr(1.0f))))
                        && loadedGrid.layoutStyle().gridAutoColumns().equals(List.of(GridTrack.px(50.0f)))
                        && loadedGrid.layoutStyle().gridAutoRows().equals(List.of(GridTrack.px(25.0f)))
                        && loadedGrid.layoutStyle().gridAutoFlow() == GridAutoFlow.COLUMN_DENSE
                        && loadedGrid.layoutStyle().justifyContent() == Justify.CENTER
                        && loadedGrid.layoutStyle().alignContent() == AlignContent.SPACE_BETWEEN,
                "Reloaded grid should restore template and alignment styles");
        Box loadedCell = (Box) loadedGrid.children().get(0);
        expect(loadedCell.layoutStyle().gridColumnStart() == 2
                        && loadedCell.layoutStyle().gridColumnSpan() == 2
                        && loadedCell.layoutStyle().gridRowStart() == 1
                        && loadedCell.layoutStyle().gridRowSpan() == 3
                        && loadedCell.layoutStyle().order() == 2
                        && near(loadedCell.layoutStyle().aspectRatio(), 1.5f)
                        && loadedCell.layoutStyle().marginAuto().equals(AutoMargins.HORIZONTAL)
                        && loadedCell.layoutStyle().zIndex() == 5
                        && loadedCell.layoutStyle().flexBasis().equals(SizeValue.minContent())
                        && loadedCell.layoutStyle().minWidth().equals(SizeValue.minContent()),
                "Reloaded cell should restore placement and item layout styles");
        FlexBox loadedFlex = (FlexBox) loaded.children().get(1);
        expect(loadedFlex.layoutStyle().flexDirection() == FlexDirection.ROW_REVERSE
                        && loadedFlex.layoutStyle().flexWrap() == FlexWrap.WRAP_REVERSE,
                "Reloaded flex should restore reverse direction and wrap");
        Label loadedText = (Label) loadedFlex.children().get(0);
        expect(loadedText.layoutStyle().width().equals(SizeValue.fitContent(30.0f))
                        && loadedText.layoutStyle().position() == PositionType.FIXED
                        && loadedText.layoutStyle().alignSelf() == Align.BASELINE,
                "Reloaded text should restore fit-content width, fixed position and baseline");

        VBox parsed = XMLWidget.create("""
                <VBox>
                    <Box id="a" minWidth="auto" marginAuto="left right" flexBasis="content" position="static" />
                    <Box id="b" marginAuto="all" flexBasis="max-content" />
                    <GridBox id="g" gridTemplateColumns="repeat(2, 1fr) 10px" gridTemplateRows="minmax(10px, 1fr)" />
                </VBox>
                """, VBox.class);
        Box parsedA = (Box) parsed.children().get(0);
        expect(parsedA.layoutStyle().minWidth().isAuto()
                        && parsedA.layoutStyle().marginAuto().equals(AutoMargins.of(true, false, true, false))
                        && parsedA.layoutStyle().flexBasis().equals(SizeValue.content())
                        && parsedA.layoutStyle().position() == PositionType.STATIC,
                "XML loader should parse auto min-width, side list marginAuto, content basis and static position");
        Box parsedB = (Box) parsed.children().get(1);
        expect(parsedB.layoutStyle().marginAuto().equals(AutoMargins.ALL)
                        && parsedB.layoutStyle().flexBasis().equals(SizeValue.maxContent()),
                "XML loader should parse all marginAuto and max-content basis");
        GridBox parsedGrid = (GridBox) parsed.children().get(2);
        expect(parsedGrid.layoutStyle().gridTemplateColumns().equals(
                                List.of(GridTrack.fr(1.0f), GridTrack.fr(1.0f), GridTrack.px(10.0f)))
                        && parsedGrid.layoutStyle().gridTemplateRows().equals(
                                List.of(GridTrack.minmax(GridTrack.px(10.0f), GridTrack.fr(1.0f)))),
                "XML loader should expand repeat() and parse minmax() track shorthands");
    }

    private void testPrefabIncludeExpansionModel() {
        XmlWidgetPrefabCatalog catalog = XmlWidgetPrefabCatalog.empty()
                .register("action.button", "<Button text=\"Prefab\" enabled=\"true\" />")
                .register("action.card", """
                        <Box background="#202936FF" padding="4">
                            <Include prefab="action.button" id="apply" text="Apply" />
                        </Box>
                        """);

        XmlWidgetDocument document = XmlWidgetDocument.parse("""
                <VBox id="root">
                    <Include prefab="action.card" id="card" />
                    <Include prefab="missing.card" />
                </VBox>
                """);

        XmlWidgetDocumentResult expanded = document.expandPrefabs(catalog);
        XmlWidgetElement card = expanded.document().root().elementChildren().get(0);
        expect(card.name().equals("Box") && card.attribute("id").orElseThrow().equals("card"),
                "Prefab Include should expand to a copy of the prefab root and apply override attributes");
        XmlWidgetElement apply = card.elementChildren().get(0);
        expect(apply.name().equals("Button")
                        && apply.attribute("id").orElseThrow().equals("apply")
                        && apply.attribute("text").orElseThrow().equals("Apply"),
                "Nested prefab includes should expand and preserve local Include overrides");
        expect(expanded.hasDiagnostics()
                        && expanded.diagnosticMessages().stream().anyMatch(message -> message.contains("missing.card")),
                "Prefab Include expansion should report missing prefab diagnostics without dropping the document");
        expect(expanded.document().root().elementChildren().get(1).name().equals("Include"),
                "Unresolved prefab Include nodes should remain in the expanded source for editor repair");

        XmlWidgetDocumentResult runtimeReady = XmlWidgetDocument.parse("""
                <VBox id="root">
                    <Include prefab="action.card" id="card" />
                </VBox>
                """).expandPrefabs(catalog);
        expect(runtimeReady.valid(), "Fully resolved prefab includes should have no expansion diagnostics");
        String xml = runtimeReady.document().toXmlString(XmlWidgetSerializationOptions.COMPACT);
        expect(!xml.contains("Include") && xml.contains("<Button"),
                "Expanded prefab XML should serialize as normal widget XML before runtime loading");
        VBox loaded = XMLWidget.create(xml, VBox.class);
        expect(XMLWidget.getWidget(loaded, "apply", Button.class).text().equals("Apply"),
                "Expanded prefab XML should materialize through the normal runtime loader");

        XmlWidgetDocument prefabCopy = catalog.document("action.card").orElseThrow();
        prefabCopy.root().setAttribute("id", "mutated");
        expect(catalog.document("action.card").orElseThrow().root().attribute("id").isEmpty(),
                "Prefab catalog documents should be exposed as defensive copies");
        expectFailsUnsupported(() -> catalog.ids().add("oops"),
                "Prefab catalog id snapshots should be immutable");

        catalog.register("loop", "<Include prefab=\"loop\" />");
        XmlWidgetDocumentResult cyclic = XmlWidgetDocument.parse("<VBox><Include prefab=\"loop\" /></VBox>")
                .expandPrefabs(catalog);
        expect(cyclic.hasDiagnostics()
                        && cyclic.diagnosticMessages().stream().anyMatch(message -> message.contains("Cyclic")),
                "Prefab Include expansion should diagnose cyclic prefab references");
    }

    private void testItemAndControlTemplateModel() {
        XmlWidgetTemplateCatalog catalog = XmlWidgetTemplateCatalog.empty()
                .item("option.row", """
                        <HBox id="row" spacing="4">
                            <Label id="caption" text="Option" />
                            <Slider id="value" min="0" max="1" value="0" />
                        </HBox>
                        """)
                .control("primary.button", "<Button id=\"button\" text=\"Action\" enabled=\"true\" />");

        XmlWidgetTemplateValues rowValues = XmlWidgetTemplateValues.empty()
                .rootAttribute("id", "gammaRow")
                .attribute("caption", "text", "Gamma")
                .attribute("value", "value", "0.75");
        XmlWidgetDocumentResult rowResult = catalog.instantiate("option.row", rowValues, XMLWidget.registry());

        expect(rowResult.valid(), "Item template instantiation should validate as normal widget XML");
        HBox row = XMLWidget.create(rowResult.document().toXmlString(XmlWidgetSerializationOptions.COMPACT), HBox.class);
        expect(row.id().equals("gammaRow") && near(row.spacing(), 4.0f),
                "Item templates should apply root overrides while preserving template layout");
        expect(XMLWidget.getWidget(row, "caption", Label.class).text().equals("Gamma")
                        && near(XMLWidget.getWidget(row, "value", Slider.class).value(), 0.75f),
                "Item templates should apply nested attribute overrides by source id");

        XmlWidgetDocumentResult missingTarget = catalog.instantiate("option.row",
                XmlWidgetTemplateValues.empty().attribute("missing", "text", "Nope"));
        expect(missingTarget.hasDiagnostics()
                        && missingTarget.firstDiagnostic().orElseThrow().message().contains("target id 'missing'"),
                "Item templates should diagnose missing override target ids");

        XmlWidgetDocumentResult controlResult = catalog.instantiate("primary.button",
                XmlWidgetTemplateValues.empty().attribute("button", "text", "Apply"),
                XMLWidget.registry());
        Button button = XMLWidget.create(controlResult.document().toXmlString(XmlWidgetSerializationOptions.COMPACT), Button.class);
        expect(button.text().equals("Apply"),
                "Control templates should instantiate reusable controls with nested id overrides");

        XmlWidgetDocumentResult invalidAttribute = catalog.instantiate("primary.button",
                XmlWidgetTemplateValues.empty().attribute("button", "spcaing", "6"),
                XMLWidget.registry());
        expect(invalidAttribute.hasDiagnostics()
                        && invalidAttribute.diagnosticMessages().stream().anyMatch(message -> message.contains("spcaing")),
                "Template validation should surface descriptor diagnostics after applying overrides");

        expect(catalog.templates(XmlWidgetTemplateKind.ITEM).size() == 1
                        && catalog.templates(XmlWidgetTemplateKind.CONTROL).size() == 1,
                "Template catalogs should separate item and control templates for editor palettes");
        expectFailsUnsupported(() -> rowValues.attributes().clear(),
                "Template value override snapshots should be immutable");
        expectFailsUnsupported(() -> catalog.ids().add("oops"),
                "Template catalog id snapshots should be immutable");
        expectFails("missing.template", () -> catalog.instantiate("missing.template", XmlWidgetTemplateValues.empty()));
    }

    private void testEditorDocumentIo() {
        Path path = tempXmlPath();
        try {
            XmlWidgetDocument document = XmlWidgetDocument.of(new XmlWidgetElement("VBox")
                    .attribute("id", "ioRoot")
                    .addElement(new XmlWidgetElement("Label").attribute("id", "ioTitle").attribute("text", "Saved")));

            XmlWidgetDocumentIo.save(path, document, XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false));
            XmlWidgetDocument loaded = XmlWidgetDocumentIo.load(path);
            expect(loaded.root().attribute("id").orElseThrow().equals("ioRoot"),
                    "Editor document IO should load a saved XML source document from UTF-8 file");
            VBox runtime = XMLWidget.create(loaded.toXmlString(XmlWidgetSerializationOptions.COMPACT), VBox.class);
            expect(XMLWidget.getWidget(runtime, "ioTitle", Label.class).text().equals("Saved"),
                    "Saved and reloaded editor XML should still materialize into runtime widgets");

            Files.writeString(path, "<VBox id=\"bad\" spcaing=\"6\" />", StandardCharsets.UTF_8);
            XmlWidgetDocumentResult result = XmlWidgetDocumentIo.loadEditor(path);
            expect(result.hasDiagnostics()
                            && result.diagnosticMessages().stream().anyMatch(message -> message.contains("spcaing")),
                    "Editor document IO should expose diagnostics when loading authoring files");
        } catch (IOException failure) {
            throw new AssertionError("Could not exercise XML document IO", failure);
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Best-effort cleanup for local self-test temp files.
            }
        }
    }

    private void testHotReloadPreviewModel() {
        Path path = tempXmlPath();
        try {
            Files.writeString(path, "<VBox id=\"hotRoot\"><Label id=\"title\" text=\"First\" /></VBox>", StandardCharsets.UTF_8);
            int[] reloads = {0};
            XmlWidgetHotReloadPreview<VBox> preview = new XmlWidgetHotReloadPreview<>(
                    XmlWidgetHotReloadSource.path(path),
                    VBox.class)
                    .autoReload(false)
                    .onReload(root -> reloads[0]++);

            XmlWidgetHotReloadPreview.Status first = preview.reloadNow();
            expect(first.loaded() && first.valid() && reloads[0] == 1,
                    "Hot reload preview should load an initial XML widget tree on demand");
            expect(XMLWidget.getWidget(preview.content(), "title", Label.class).text().equals("First"),
                    "Hot reload preview should expose the loaded runtime content");

            Files.writeString(path, "<VBox id=\"hotRoot\"><Label id=\"title\" text=\"Second\" /></VBox>", StandardCharsets.UTF_8);
            XmlWidgetHotReloadPreview.Status second = preview.checkForReload();
            expect(second.reloadCount() == 2 && reloads[0] == 2,
                    "Hot reload preview should reload when the source version changes");
            expect(XMLWidget.getWidget(preview.content(), "title", Label.class).text().equals("Second"),
                    "Hot reload preview should replace content after a successful reload");

            Files.writeString(path, "<VBox id=\"hotRoot\"><Missing /></VBox>", StandardCharsets.UTF_8);
            XmlWidgetHotReloadPreview.Status failure = preview.checkForReload();
            expect(failure.failed() && failure.loaded() && failure.hasDiagnostics(),
                    "Hot reload preview should report diagnostics and keep the last valid tree on reload failure");
            expect(XMLWidget.getWidget(preview.content(), "title", Label.class).text().equals("Second"),
                    "Hot reload preview should not replace content with a failed XML load");
            expectFailsUnsupported(() -> failure.diagnostics().clear(),
                    "Hot reload failure diagnostics should be immutable snapshots");
        } catch (IOException failure) {
            throw new AssertionError("Could not exercise XML hot reload preview", failure);
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // Best-effort cleanup for local self-test temp files.
            }
        }
    }

    private void testDiagnosticsPanelWidget() {
        XmlWidgetDiagnosticsPanel panel = new XmlWidgetDiagnosticsPanel().entryLimit(2);
        expect(panel.model().isEmpty() && panel.summaryLabel().text().equals("No XML diagnostics."),
                "Diagnostics panel should start with a stable empty model summary");
        expect(panel.entriesHost().children().size() == 1,
                "Diagnostics panel should render a stable empty-state row");

        XmlWidgetDiagnosticsModel model = XmlWidgetDiagnosticsModel.errors(List.of(
                new XmlWidgetDiagnostic("First error", 2, 4),
                new XmlWidgetDiagnostic("Second error", 3, 8),
                new XmlWidgetDiagnostic("Third error", 4, 12)));
        panel.model(model);
        expect(panel.model().errorCount() == 3 && panel.summaryLabel().text().equals("3 XML diagnostics."),
                "Diagnostics panel should expose and summarize its current model");
        expect(panel.entriesHost().children().size() == 3,
                "Diagnostics panel should honor entryLimit and render a remaining-count row");
        expect(panel.entriesHost().children().get(0) instanceof dev.sixik.unigui.widgets.containers.HBox,
                "Diagnostics panel entries should render as stable row widgets");

        XmlWidgetHotReloadPreview.Status status = XmlWidgetHotReloadPreview.Status.failed(
                "test.xml",
                1L,
                1,
                true,
                new XmlWidgetLoadException(List.of(new XmlWidgetDiagnostic("Status error", 5, 6))));
        panel.status(status);
        expect(panel.summaryLabel().text().equals("1 XML diagnostic."),
                "Diagnostics panel should accept hot reload status diagnostics directly");
    }

    private void testEditorHierarchyAndSelectionModels() {
        XmlWidgetDocument document = XmlWidgetDocument.parse("""
                <VBox xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" id="root">
                    <!-- editor marker -->
                    <VBox.Children>
                        <Label id="title" text="Title" />
                        <Button x:Name="apply" text="Apply" />
                    </VBox.Children>
                </VBox>
                """);

        XmlWidgetHierarchy hierarchy = XmlWidgetHierarchy.from(document);
        expect(hierarchy.items().size() == 11 && hierarchy.elementItems().size() == 4,
                "Hierarchy should include elements plus preserved whitespace text and comments");
        expectFailsUnsupported(() -> hierarchy.items().clear(),
                "Hierarchy item list should be immutable");

        XmlWidgetNodePath propertyPath = XmlWidgetNodePath.of(3);
        XmlWidgetNodePath labelPath = propertyPath.child(1);
        XmlWidgetNodePath buttonPath = propertyPath.child(3);

        expect(propertyPath.resolveElement(document).orElseThrow().propertyElement(),
                "Node paths should resolve property elements in the source document");
        expect(hierarchy.item(labelPath).orElseThrow().label().equals("Label#title"),
                "Hierarchy labels should include stable XML ids when present");
        expect(hierarchy.item(buttonPath).orElseThrow().id().equals("apply"),
                "Hierarchy ids should include x:Name aliases");
        expect(buttonPath.parent().orElseThrow().equals(propertyPath),
                "Node paths should expose parent paths");
        expect(XmlWidgetNodePath.root().toString().equals("/") && buttonPath.toString().equals("/3/3"),
                "Node paths should have compact debug strings");

        XmlWidgetSelectionModel selection = new XmlWidgetSelectionModel();
        expect(!selection.hasSelection(), "Selection model should start empty");
        selection.select(buttonPath);
        expect(selection.hasSelection() && selection.validFor(document),
                "Selection model should track a valid source node path");
        expect(selection.selectedElement(document).orElseThrow().attribute("text").orElseThrow().equals("Apply"),
                "Selection model should resolve selected elements from the current document");
        selection.selectIfPresent(document, XmlWidgetNodePath.of(99));
        expect(!selection.hasSelection(), "Selection model should clear missing paths through selectIfPresent");
        selection.selectRoot();
        expect(selection.selectedElement(document).orElseThrow().name().equals("VBox"),
                "Selection model should support selecting the document root");
    }

    private void testEditorLayoutDragResizeHandles() {
        XmlWidgetDocument document = XmlWidgetDocument.parse("""
                <VBox id="root">
                    <Box id="panel" x="10" y="20" width="100" height="50" />
                    <Box id="bad" x="left" width="80" height="30" />
                </VBox>
                """);
        XmlWidgetNodePath panelPath = XmlWidgetNodePath.of(1);

        XmlWidgetDocumentResult moved = XmlWidgetLayoutHandles.move(document, panelPath, 5.0f, -10.0f);
        XmlWidgetElement movedPanel = moved.document().root().elementChildren().get(0);
        expect(moved.valid()
                        && movedPanel.attribute("x").orElseThrow().equals("15")
                        && movedPanel.attribute("y").orElseThrow().equals("10"),
                "Editor move handles should update source x/y layout attributes");
        expect(document.root().elementChildren().get(0).attribute("x").orElseThrow().equals("10"),
                "Editor layout handle operations should return mutated document copies");

        XmlWidgetDocumentResult resized = XmlWidgetLayoutHandles.resize(
                moved.document(),
                panelPath,
                XmlWidgetLayoutHandle.NORTH_WEST,
                -20.0f,
                -5.0f);
        XmlWidgetElement resizedPanel = resized.document().root().elementChildren().get(0);
        expect(resized.valid()
                        && resizedPanel.attribute("x").orElseThrow().equals("-5")
                        && resizedPanel.attribute("y").orElseThrow().equals("5")
                        && resizedPanel.attribute("width").orElseThrow().equals("120")
                        && resizedPanel.attribute("height").orElseThrow().equals("55"),
                "Corner resize handles should update x/y and width/height against fixed opposite edges");

        XmlWidgetDocumentResult clamped = XmlWidgetLayoutHandles.resize(
                resized.document(),
                panelPath,
                XmlWidgetLayoutHandle.WEST,
                500.0f,
                0.0f,
                8.0f,
                8.0f);
        XmlWidgetElement clampedPanel = clamped.document().root().elementChildren().get(0);
        expect(clampedPanel.attribute("x").orElseThrow().equals("107")
                        && clampedPanel.attribute("width").orElseThrow().equals("8"),
                "Resize handles should clamp to requested minimum dimensions");

        XmlWidgetLayoutFrame frame = XmlWidgetLayoutHandles.frame(clampedPanel).orElseThrow();
        expect(near(frame.x(), 107.0f) && near(frame.width(), 8.0f),
                "Editor layout frame snapshots should read numeric source layout attributes");

        Box loaded = XMLWidget.create(XmlWidgetDocument.of(clamped.document().root().elementChildren().get(0).copy())
                .toXmlString(XmlWidgetSerializationOptions.COMPACT), Box.class);
        expect(near(loaded.transform().position().x(), 107.0f)
                        && near(loaded.layoutStyle().width().value(), 8.0f),
                "Editor layout handle output should still materialize through the runtime XML loader");

        XmlWidgetDocumentResult missing = XmlWidgetLayoutHandles.move(document, XmlWidgetNodePath.of(99), 1.0f, 1.0f);
        expect(missing.hasDiagnostics()
                        && missing.firstDiagnostic().orElseThrow().message().contains("target '/99'"),
                "Editor layout handles should diagnose missing source node paths");

        XmlWidgetDocumentResult invalid = XmlWidgetLayoutHandles.move(document, XmlWidgetNodePath.of(3), 1.0f, 1.0f);
        expect(invalid.hasDiagnostics()
                        && invalid.firstDiagnostic().orElseThrow().message().contains("must be a numeric pixel value"),
                "Editor layout handles should diagnose non-numeric layout attributes");
    }




    private void testUndoableDocumentEdits() {
        XmlWidgetDocument document = XmlWidgetDocument.parse("""
                <VBox id="root"><Label id="first" text="First" /><Button id="second" text="Second" /></VBox>
                """);

        XmlWidgetDocumentEdit setText = XmlWidgetDocumentEdits.setAttribute(XmlWidgetNodePath.of(0), "text", "Renamed");
        setText.apply(document);
        expect(document.root().elementChildren().get(0).attribute("text").orElseThrow().equals("Renamed"),
                "Set-attribute edit should mutate target source element");
        setText.undo(document);
        expect(document.root().elementChildren().get(0).attribute("text").orElseThrow().equals("First"),
                "Set-attribute edit should restore previous source value");

        XmlWidgetDocumentEdit addChild = XmlWidgetDocumentEdits.addChild(
                XmlWidgetNodePath.root(),
                1,
                new XmlWidgetElement("Label").attribute("id", "inserted").attribute("text", "Inserted"));
        addChild.apply(document);
        expect(document.root().elementChildren().get(1).attribute("id").orElseThrow().equals("inserted"),
                "Add-child edit should insert at the requested source index");
        addChild.undo(document);
        expect(document.root().elementChildren().size() == 2,
                "Add-child edit should undo by removing the inserted node");

        XmlWidgetDocumentEdit moveChild = XmlWidgetDocumentEdits.moveChild(XmlWidgetNodePath.root(), 0, 1);
        moveChild.apply(document);
        expect(document.root().elementChildren().get(0).attribute("id").orElseThrow().equals("second"),
                "Move-child edit should reorder source children");
        moveChild.undo(document);
        expect(document.root().elementChildren().get(0).attribute("id").orElseThrow().equals("first"),
                "Move-child edit should undo the reorder");

        XmlWidgetDocumentEdit removeChild = XmlWidgetDocumentEdits.removeChild(XmlWidgetNodePath.of(1));
        removeChild.apply(document);
        expect(document.root().elementChildren().size() == 1,
                "Remove-child edit should delete the target child");
        removeChild.undo(document);
        expect(document.root().elementChildren().size() == 2
                        && document.root().elementChildren().get(1).attribute("id").orElseThrow().equals("second"),
                "Remove-child edit should restore the target child at its original index");

        VBox loaded = XMLWidget.create(document.toXmlString(XmlWidgetSerializationOptions.COMPACT), VBox.class);
        expect(XMLWidget.getWidget(loaded, "first", Label.class).text().equals("First")
                        && XMLWidget.getWidget(loaded, "second", Button.class).text().equals("Second"),
                "Edited source document should still materialize into runtime widgets after undo operations");
    }

    private void testEditorDiagnosticsCollection() {
        XmlWidgetDocumentResult result = XmlWidgetDocument.parseEditor("""
                <VBox id="root" spcaing="6">
                    <Label id="dup" future="todo" />
                    <Button id="dup"><Label text="Invalid child" /></Button>
                    <Missing id="ghost"><Label unknown="1" /></Missing>
                </VBox>
                """);

        expect(result.document().root().name().equals("VBox"),
                "Editor parse should still return a source document when semantic diagnostics exist");
        expect(result.hasDiagnostics() && !result.valid(),
                "Editor parse result should expose accumulated diagnostics state");
        expect(result.firstDiagnostic().isPresent(),
                "Editor parse result should expose the first diagnostic for compact UI summaries");

        List<String> messages = result.diagnosticMessages();
        expect(messages.size() >= 5, "Editor validation should collect multiple independent diagnostics");
        expect(messages.stream().anyMatch(message -> message.contains("Unknown attribute 'spcaing' on VBox")),
                "Editor diagnostics should include root attribute errors");
        expect(messages.stream().anyMatch(message -> message.contains("Unknown attribute 'future' on Label")),
                "Editor diagnostics should include child attribute errors");
        expect(messages.stream().anyMatch(message -> message.contains("Duplicate widget id 'dup'")),
                "Editor diagnostics should include duplicate ids");
        expect(messages.stream().anyMatch(message -> message.contains("Widget Button cannot contain child Label")),
                "Editor diagnostics should include invalid child relationships");
        expect(messages.stream().anyMatch(message -> message.contains("Unknown widget type 'Missing'")),
                "Editor diagnostics should include unknown widget types");

        XmlWidgetDocumentResult repeatedContent = XmlWidgetDocument.parseEditor("""
                <ScrollView>
                    <ScrollView.Content><VBox /></ScrollView.Content>
                    <ScrollView.Content><Label /></ScrollView.Content>
                </ScrollView>
                """);
        List<String> repeatedMessages = repeatedContent.diagnosticMessages();
        expect(!repeatedContent.valid()
                        && repeatedMessages.stream().anyMatch(message -> message.contains(
                        "Property element 'ScrollView.Content' can appear only once on ScrollView")),
                "Editor diagnostics should report repeated single-child property elements");

        XmlWidgetDocumentResult oversizedContent = XmlWidgetDocument.parseEditor("""
                <ScrollView>
                    <ScrollView.Content>
                        <VBox />
                        <Label />
                    </ScrollView.Content>
                </ScrollView>
                """);
        List<String> oversizedMessages = oversizedContent.diagnosticMessages();
        expect(!oversizedContent.valid()
                        && oversizedMessages.stream().anyMatch(message -> message.contains(
                        "Property element 'ScrollView.Content' can contain only one widget child")),
                "Editor diagnostics should report too many widgets in single-child property elements");

        expect(result.toString().contains("diagnostics"),
                "Document result should remain a normal inspectable record");
        expectFailsUnsupported(() -> result.diagnostics().add(new XmlWidgetDiagnostic("oops")),
                "Editor diagnostics list should be immutable");

        XmlWidgetDiagnosticsModel diagnosticsModel = XmlWidgetDiagnosticsModel.from(result);
        expect(diagnosticsModel.hasErrors() && diagnosticsModel.errorCount() == result.diagnostics().size(),
                "Diagnostics panel model should expose collected editor diagnostics as errors");
        expect(diagnosticsModel.summary().contains(Integer.toString(result.diagnostics().size())),
                "Diagnostics panel model should expose a compact diagnostic count summary");
        expect(diagnosticsModel.entries().get(0).index() == 1
                        && diagnosticsModel.entries().get(0).severity() == XmlWidgetDiagnosticsModel.Severity.ERROR,
                "Diagnostics panel entries should be stable, one-based and severity-tagged");
        expect(diagnosticsModel.entries().stream().anyMatch(entry -> entry.displayText().contains("line")),
                "Diagnostics panel entries should render line/column labels when available");
        expectFailsUnsupported(() -> diagnosticsModel.entries().clear(),
                "Diagnostics panel entries should be immutable snapshots");
        expect(XmlWidgetDiagnosticsModel.empty().isEmpty()
                        && XmlWidgetDiagnosticsModel.empty().summary().equals("No XML diagnostics."),
                "Empty diagnostics panel model should have a stable no-errors summary");
        try {
            result.throwIfDiagnostics();
            throw new AssertionError("Expected throwIfDiagnostics to fail for editor diagnostics");
        } catch (XmlWidgetLoadException failure) {
            expect(failure.diagnostics().size() == result.diagnostics().size(),
                    "throwIfDiagnostics should preserve the full diagnostic list");
        }
    }

    private void testXmlEditorSessionParseFailurePreservesLastValidDocument() {
        XmlEditorSession session = XmlEditorSession.create(
                "<VBox><Button id=\"ok\" text=\"OK\" /></VBox>");
        session.select(XmlWidgetNodePath.of(0));

        boolean parsed = session.replaceText("<VBox><Button></VBox>");

        expect(!parsed, "Editor session should reject malformed XML text");
        expect(session.text().equals("<VBox><Button></VBox>"),
                "Editor session should retain the raw invalid text buffer for code view");
        expect(session.document().root().name().equals("VBox")
                        && session.document().root().elementChildren().get(0).attribute("id").orElseThrow().equals("ok"),
                "Editor session should preserve the previous valid source document after parse failure");
        expect(session.selectedPath().orElseThrow().equals(XmlWidgetNodePath.of(0)),
                "Editor session should preserve selection path while the code buffer is invalid");
        expect(session.diagnostics(XmlEditorDiagnosticChannel.PARSE).size() == 1
                        && session.diagnostics(XmlEditorDiagnosticChannel.VALIDATION).isEmpty(),
                "Editor session should expose parse diagnostics separately from validation diagnostics");
        expect(session.dirty(), "Invalid code edits should still mark the XML text buffer dirty");
    }

    private void testXmlEditorSessionSelectionNoOpDoesNotEmit() {
        XmlEditorSession session = XmlEditorSession.create(
                "<VBox><Button id=\"ok\" /></VBox>");
        int[] selectionChanges = {0};
        EventSubscription subscription = session.onChanged(change -> {
            if (change.kind() == XmlEditorSessionChange.Kind.SELECTION_CHANGED) {
                selectionChanges[0]++;
            }
        });

        session.select(XmlWidgetNodePath.of(0));
        session.select(XmlWidgetNodePath.of(0));
        expect(selectionChanges[0] == 1,
                "Editor session should not emit selection changes when the selected path is unchanged");

        session.select(null);
        session.select(null);
        expect(selectionChanges[0] == 2,
                "Editor session should not emit selection changes when the selection is already empty");
        closeUnchecked(subscription);
    }

    private void testXmlEditorSessionUndoRedoAndSelectionSurvival() {
        XmlEditorSession session = XmlEditorSession.create(
                "<VBox><Label id=\"first\" /><Button id=\"selected\" /></VBox>");
        session.select(XmlWidgetNodePath.of(1));

        boolean applied = session.applyEdit(XmlWidgetDocumentEdits.addChild(
                XmlWidgetNodePath.root(),
                0,
                new XmlWidgetElement("TextWidget").attribute("id", "inserted")));
        expect(applied, "Editor session should apply undoable document edits");
        expect(session.undoCount() == 1 && session.redoCount() == 0,
                "Editor session should track undo/redo stack sizes after document edits");
        expect(session.selectedPath().orElseThrow().equals(XmlWidgetNodePath.of(2)),
                "Editor session should remap selection to the same source node after insertion before it");
        expect(session.text().contains("inserted"),
                "Editor session should regenerate XML text after inspector-style document edits");

        expect(session.undo(), "Editor session should undo the latest document edit");
        expect(session.undoCount() == 0 && session.redoCount() == 1,
                "Editor session should move edits to redo stack after undo");
        expect(session.selectedPath().orElseThrow().equals(XmlWidgetNodePath.of(1)),
                "Editor session should remap selection back after undo");

        expect(session.redo(), "Editor session should redo the undone document edit");
        expect(session.undoCount() == 1 && session.redoCount() == 0,
                "Editor session should restore undo stack after redo");
        expect(session.selectedPath().orElseThrow().equals(XmlWidgetNodePath.of(2)),
                "Editor session should keep selection on the same source node after redo");

        XmlEditorSession insertionSession = XmlEditorSession.create("<VBox><Button id=\"button\" /></VBox>");
        expect(!insertionSession.insertChild(
                        XmlWidgetNodePath.of(0),
                        Integer.MAX_VALUE,
                        new XmlWidgetElement("Label").attribute("id", "rejected")),
                "Validated editor insertion should reject child nodes that violate widget child policy");
        expect(insertionSession.document().root().elementChildren().get(0).elementChildren().isEmpty()
                        && insertionSession.undoCount() == 0
                        && insertionSession.diagnostics(XmlEditorDiagnosticChannel.EDIT).stream()
                        .anyMatch(diagnostic -> diagnostic.message().contains("Widget Button cannot contain child Label")),
                "Rejected editor insertion should preserve the source document and expose edit diagnostics");
        expect(insertionSession.insertChild(
                        XmlWidgetNodePath.root(),
                        Integer.MAX_VALUE,
                        new XmlWidgetElement("Label").attribute("id", "inserted")),
                "Validated editor insertion should accept child nodes allowed by the target descriptor");
        expect(insertionSession.document().root().elementChildren().size() == 2
                        && insertionSession.selectedPath().orElseThrow().resolveElement(insertionSession.document())
                        .orElseThrow().attribute("id").orElseThrow().equals("inserted")
                        && insertionSession.undoCount() == 1
                        && insertionSession.diagnostics(XmlEditorDiagnosticChannel.EDIT).isEmpty(),
                "Accepted editor insertion should update text, selection, undo stack and clear edit diagnostics");
    }

    private void testXmlEditorSessionSaveRevertAndEvents() {
        XmlEditorDocumentSource source = XmlEditorDocumentSources.memory(
                "demo",
                "Demo XML",
                "<Label id=\"saved\" text=\"Saved\" />");
        XmlEditorSession session = XmlEditorSession.open(source);
        List<XmlEditorSessionChange.Kind> events = new java.util.ArrayList<>();
        EventSubscription subscription = session.onChanged(change -> events.add(change.kind()));

        expect(!session.dirty(), "Opened editor source should start clean");
        session.mode(XmlEditorMode.CODE);
        expect(session.mode() == XmlEditorMode.CODE
                        && events.contains(XmlEditorSessionChange.Kind.MODE_CHANGED),
                "Editor session should emit mode-change events for pane refreshes");

        expect(session.applyEdit(XmlWidgetDocumentEdits.setAttribute(XmlWidgetNodePath.root(), "id", "edited")),
                "Editor session should apply root inspector edits");
        expect(session.dirty(), "Document edits should mark the session dirty by changing the XML text");
        expect(events.contains(XmlEditorSessionChange.Kind.DOCUMENT_CHANGED)
                        && events.contains(XmlEditorSessionChange.Kind.UNDO_STACK_CHANGED),
                "Editor session should emit document and undo-stack events after edits");

        expect(session.save(), "Writable editor sources should save session text");
        expect(!session.dirty() && source.readText().contains("id=\"edited\""),
                "Editor session save should persist XML text and clear dirty state");

        expect(session.replaceText("<Label id=\"unsaved\" text=\"Unsaved\" />"),
                "Editor session should accept valid code-view replacement text");
        expect(session.dirty()
                        && session.document().root().attribute("id").orElseThrow().equals("unsaved"),
                "Valid code-view replacement should update document and dirty state");

        expect(session.revert(), "Editor session should revert from its current source");
        expect(!session.dirty()
                        && session.document().root().attribute("id").orElseThrow().equals("edited")
                        && events.contains(XmlEditorSessionChange.Kind.REVERTED),
                "Editor session revert should restore saved source text and emit a revert event");

        subscription.close();
        session.markClean();
        expect(!events.contains(XmlEditorSessionChange.Kind.SAVED)
                        || java.util.Collections.frequency(events, XmlEditorSessionChange.Kind.SAVED) == 1,
                "Unsubscribed editor session listeners should stop receiving changes");
    }

    private void testEditorPreservesUnsupportedAttributes() {
        XmlWidgetDocumentResult result = XmlWidgetDocument.parseEditor("""
                <VBox xmlns:editor="urn:unigui-editor" id="root" editor:selected="true" futureLayout="snap">
                    <Label id="title" text="Keep editor data" editor:locked="true" />
                </VBox>
                """);

        expect(result.hasDiagnostics(),
                "Editor parse should report unsupported attributes without dropping the source document");
        expect(result.document().root().attribute("futureLayout").orElseThrow().equals("snap"),
                "Editor document should preserve unknown attributes for round-trip");
        expect(result.document().root().attribute("editor:selected").orElseThrow().equals("true"),
                "Editor document should preserve prefixed unknown attributes for round-trip");

        String serialized = result.document().toXmlString(XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false));
        expect(serialized.contains("futureLayout=\"snap\"")
                        && serialized.contains("editor:selected=\"true\"")
                        && serialized.contains("editor:locked=\"true\""),
                "Editor serializer should preserve unsupported attributes while diagnostics explain them");
    }

    private void testAnnotationPrototypeMetadata() {
        XmlWidgetName buttonWidget = Button.class.getAnnotation(XmlWidgetName.class);
        expect(buttonWidget != null && buttonWidget.value().equals("Button"),
                "@XmlWidgetName should expose a prototype XML type name on Button");
        XmlAttribute buttonText = annotation(Button.class, "text", String.class);
        expect(buttonText.value().equals("text") && buttonText.category().equals("Content"),
                "@XmlAttribute should expose prototype setter metadata on Button.text");
        XmlAttribute buttonEnabled = annotation(Button.class, "enabled", boolean.class);
        expect(buttonEnabled.value().equals("enabled") && buttonEnabled.defaultValue().equals("true"),
                "@XmlAttribute should support inherited/common behavior attributes on concrete widgets");

        XmlWidgetName textWidget = TextWidget.class.getAnnotation(XmlWidgetName.class);
        expect(textWidget != null && textWidget.value().equals("TextWidget"),
                "@XmlWidgetName should expose a prototype XML type name on TextWidget");
        XmlAttribute overflow = annotation(TextWidget.class, "overflowMode", dev.sixik.unigui.api.text.TextOverflowMode.class);
        expect(overflow.value().equals("overflowMode") && overflow.description().contains("layout bounds"),
                "@XmlAttribute should preserve editor-facing descriptions");

        expect(WidgetBase.class.isAnnotationPresent(XmlLayoutAttributes.class)
                        && WidgetBase.class.isAnnotationPresent(XmlStyleAttributes.class),
                "Shared descriptor marker annotations should identify common layout/style attribute blocks");

        XmlWidgetDescriptor reflectedButton = XmlWidgetAnnotations.descriptor(Button.class).orElseThrow();
        expect(reflectedButton.xmlName().equals("Button") && reflectedButton.category().equals("Controls"),
                "Annotation reflection helper should build widget descriptor metadata for Button");
        expect(descriptor(reflectedButton.attributes(), "text").category().equals("Content")
                        && descriptor(reflectedButton.attributes(), "text").valueType() == XmlAttributeValueType.STRING,
                "Annotation reflection helper should convert @XmlAttribute metadata into descriptors");
        expect(descriptor(reflectedButton.attributes(), "enabled").defaultValue().equals("true")
                        && descriptor(reflectedButton.attributes(), "enabled").valueType() == XmlAttributeValueType.BOOLEAN,
                "Annotation reflection helper should include inherited/overridden common attributes");
        XmlAttributeDescriptor reflectedTextureWidth = descriptor(
                XmlWidgetAnnotations.descriptor(TextureWidget.class).orElseThrow().attributes(),
                "textureWidth");
        expect(reflectedTextureWidth.category().equals("Assets")
                        && reflectedTextureWidth.displayName().equals("Texture Width")
                        && reflectedTextureWidth.valueType() == XmlAttributeValueType.NUMBER,
                "Annotation reflection helper should expose texture helper attributes from widget setters");
        expect(XmlWidgetAnnotations.contributesLayoutAttributes(Button.class)
                        && XmlWidgetAnnotations.contributesStyleAttributes(Button.class),
                "Annotation reflection helper should see shared descriptor blocks on base widget classes");
        expectFailsUnsupported(() -> reflectedButton.attributes().clear(),
                "Annotation-built descriptor attributes should be immutable snapshots");
    }

    private void testExpandedAnnotatedWidgetRegistration() {
        XmlWidgetRegistry registry = XMLWidget.emptyRegistry();
        registry.registerAnnotated(VirtualListView.class);
        registry.registerAnnotated(VirtualTableView.class);
        registry.registerAnnotated(Accordion.class);
        registry.registerAnnotated(Breadcrumb.class);
        registry.registerAnnotated(ExpandablePanel.class);
        registry.registerAnnotated(PageView.class);
        registry.registerAnnotated(TabControl.class);
        registry.registerAnnotated(TreeView.class);
        registry.registerAnnotated(TreeList.class);
        registry.registerAnnotated(DockingRoot.class);
        registry.registerAnnotated(GraphView.class);
        registry.registerAnnotated(NodeGraph.class);
        registry.registerAnnotated(WorldCanvas.class);
        registry.registerAnnotated(MapCanvas.class);
        registry.registerAnnotated(MapMarker.class);

        expect(registry.descriptor("VirtualListView").orElseThrow().attributes().stream()
                        .anyMatch(attribute -> attribute.name().equals("selectionMode")),
                "Expanded annotated registry should expose VirtualListView selectionMode metadata");
        expect(registry.descriptor("NodeGraph").orElseThrow().attributes().stream()
                        .anyMatch(attribute -> attribute.name().equals("gridSize")),
                "Expanded annotated registry should expose NodeGraph gridSize metadata");

        VirtualListView list = XMLWidget.create("""
                <VirtualListView itemCount="12" itemHeight="22" overscan="3" selectionMode="multiple"
                                 viewMode="grid" gridCellWidth="40" gridCellHeight="48" gridColumns="3" />
                """, VirtualListView.class, registry);
        expect(list.itemCount() == 12
                        && near(list.itemHeight(), 22.0f)
                        && list.viewMode() == VirtualListView.ViewMode.GRID
                        && near(list.gridCellWidth(), 40.0f)
                        && near(list.gridCellHeight(), 48.0f)
                        && list.gridColumns() == 3
                        && list.overscan() == 3
                        && list.selectionMode() == SelectionMode.MULTIPLE,
                "Annotation-backed VirtualListView XML attributes should apply");

        VirtualTableView table = XMLWidget.create("""
                <VirtualTableView rowCount="7" rowHeight="19" headerHeight="24" overscan="2" editable="true" selectionMode="multiple" />
                """, VirtualTableView.class, registry);
        expect(table.rowCount() == 7
                        && near(table.rowHeight(), 19.0f)
                        && near(table.headerHeight(), 24.0f)
                        && table.overscan() == 2
                        && table.editable()
                        && table.selectionMode() == SelectionMode.MULTIPLE,
                "Annotation-backed VirtualTableView XML attributes should apply");

        WorldCanvas world = XMLWidget.create("""
                <WorldCanvas clippingEnabled="false" panningEnabled="false" wheelPanStep="48" panButton="middle" preferredWidth="420" />
                """, WorldCanvas.class, registry);
        expect(!world.clippingEnabled()
                        && !world.panningEnabled()
                        && near(world.wheelPanStep(), 48.0f)
                        && world.panButton() == PointerButton.MIDDLE
                        && near(world.preferredWidth(), 420.0f),
                "Annotation-backed WorldCanvas XML attributes should apply");

        NodeGraph nodeGraph = XMLWidget.create("""
                <NodeGraph selectionMode="multiple" gridSize="32" panningEnabled="false" preferredHeight="180" />
                """, NodeGraph.class, registry);
        expect(nodeGraph.selectionMode() == NodeGraphSelectionMode.MULTIPLE
                        && near(nodeGraph.gridSize(), 32.0f)
                        && !nodeGraph.panningEnabled()
                        && near(nodeGraph.preferredHeight(), 180.0f),
                "Annotation-backed NodeGraph XML attributes should apply");

        MapCanvas map = XMLWidget.create("""
                <MapCanvas backgroundVisible="false" gridVisible="false" gridSize="64" wheelPanStep="12" />
                """, MapCanvas.class, registry);
        expect(!map.backgroundVisible()
                        && !map.gridVisible()
                        && near(map.gridSize(), 64.0f)
                        && near(map.wheelPanStep(), 12.0f),
                "Annotation-backed MapCanvas XML attributes should apply");

        MapMarker marker = XMLWidget.create("""
                <MapMarker label="Spawn" selected="true" highlighted="true" />
                """, MapMarker.class, registry);
        expect(marker.label() != null
                        && marker.label().text().equals("Spawn")
                        && marker.selected()
                        && marker.highlighted(),
                "Annotation-backed MapMarker XML attributes should apply");

        ExpandablePanel panel = XMLWidget.create("""
                <ExpandablePanel title="Advanced" expanded="false" />
                """, ExpandablePanel.class, registry);
        expect(panel.title().equals("Advanced") && !panel.expanded(),
                "Annotation-backed navigation XML attributes should apply");
    }

    private void testFailureDiagnostics() {
        expectFails("<Missing />", "Unknown widget type 'Missing'");
        expectFails("<VBox><Label id=\"same\" /><Button id=\"same\" /></VBox>", "Duplicate widget id 'same'");
        expectFails("<VBox spcaing=\"6\" />", "Unknown attribute 'spcaing' on VBox");
        expectFails("<VBox spacing=\"large\" />", "Cannot apply attribute on VBox");
        expectFails("<Button><Label text=\"Nope\" /></Button>", "Widget Button cannot contain child Label");
        expectFails("<VBox><VBox.Content><Label /></VBox.Content></VBox>", "Unknown property element 'VBox.Content' on VBox");
        expectFails("<VBox><ScrollView.Content><Label /></ScrollView.Content></VBox>",
                "Property element 'ScrollView.Content' cannot be used inside VBox");
        expectFails("<VBox><VBox.Children unexpected=\"true\"><Label /></VBox.Children></VBox>",
                "Property element 'VBox.Children' cannot have attribute 'unexpected'");
        expectFails("""
                <ScrollView>
                    <ScrollView.Content><VBox /></ScrollView.Content>
                    <ScrollView.Content><Label /></ScrollView.Content>
                </ScrollView>
                """, "Widget ScrollView can contain only one content child");
        expectFails("""
                <ScrollView>
                    <ScrollView.Content>
                        <VBox />
                        <Label />
                    </ScrollView.Content>
                </ScrollView>
                """, "Widget ScrollView can contain only one content child");
        expectFails("<VBox />", Button.class, "XML root is VBox, expected Button");
        expectFailsLookup(XMLWidget.create("<VBox id=\"root\" />", VBox.class), "missing",
                "Widget id 'missing' was not found under root 'root'");
    }

    private void testDiagnosticLocations() {
        expectFailsLocated("""
                <VBox>
                    <Missing />
                </VBox>
                """, "Unknown widget type 'Missing'", 2);
        expectFailsLocated("""
                <VBox>
                    <Label spcaing="6" />
                </VBox>
                """, "Unknown attribute 'spcaing' on Label", 2);
        expectFailsLocated("""
                <VBox>
                    <Label color="not-a-color" />
                </VBox>
                """, "Cannot apply attribute on Label", 2);
        expectFailsLocated("""
                <VBox>
                    <VBox.Content><Label /></VBox.Content>
                </VBox>
                """, "Unknown property element 'VBox.Content' on VBox", 2);
        expectFailsLocated("""
                <VBox>
                    <Label>
                </VBox>
                """, "Cannot parse XML widget source", 3);
    }

    private static <T extends Widget> void expectFails(String xml, Class<T> type, String messagePart) {
        try {
            XMLWidget.create(xml, type);
            throw new AssertionError("Expected XML load to fail: " + messagePart);
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains(messagePart),
                    "Failure message should contain '" + messagePart + "', got: " + failure.getMessage());
        }
    }

    private static void expectFails(String xml, String messagePart) {
        try {
            XMLWidget.create(xml);
            throw new AssertionError("Expected XML load to fail: " + messagePart);
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains(messagePart),
                    "Failure message should contain '" + messagePart + "', got: " + failure.getMessage());
        }
    }

    private static void expectFails(String messagePart, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected XML operation to fail: " + messagePart);
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains(messagePart),
                    "Failure message should contain '" + messagePart + "', got: " + failure.getMessage());
        }
    }

    private static void expectFailsLocated(String xml, String messagePart, int minimumLine) {
        try {
            XMLWidget.create(xml);
            throw new AssertionError("Expected XML load to fail: " + messagePart);
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains(messagePart),
                    "Failure message should contain '" + messagePart + "', got: " + failure.getMessage());
            expect(!failure.diagnostics().isEmpty(), "Located failure should contain at least one diagnostic");
            XmlWidgetDiagnostic diagnostic = failure.diagnostics().get(0);
            expect(diagnostic.hasLocation(), "Diagnostic should have line/column, got: " + diagnostic);
            expect(diagnostic.line() >= minimumLine && diagnostic.column() > 0,
                    "Diagnostic location should be useful, got: " + diagnostic);
            expect(failure.getMessage().contains("line") && failure.getMessage().contains("column"),
                    "Exception message should render diagnostic location, got: " + failure.getMessage());
        }
    }

    private static void expectFailsResource(String resourcePath, String messagePart) {
        try {
            XMLWidget.createResource(resourcePath);
            throw new AssertionError("Expected XML resource load to fail: " + messagePart);
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains(messagePart),
                    "Resource failure should contain '" + messagePart + "', got: " + failure.getMessage());
        }
    }

    private static void expectFailsLookup(Widget root, String id, String messagePart) {
        try {
            XMLWidget.getWidget(root, id);
            throw new AssertionError("Expected widget lookup to fail: " + messagePart);
        } catch (XmlWidgetLoadException failure) {
            expect(failure.getMessage().contains(messagePart),
                    "Lookup failure should contain '" + messagePart + "', got: " + failure.getMessage());
        }
    }

    private static XmlAttributeDescriptor descriptor(List<XmlAttributeDescriptor> descriptors, String name) {
        return descriptors.stream()
                .filter(descriptor -> descriptor.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing XML attribute descriptor: " + name));
    }

    private static XmlPropertyChildDescriptor propertyChild(List<XmlPropertyChildDescriptor> descriptors, String name) {
        return descriptors.stream()
                .filter(descriptor -> descriptor.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing XML property-child descriptor: " + name));
    }

    private static void expectFailsUnsupported(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (UnsupportedOperationException expected) {
            // Expected immutable snapshot.
        }
    }

    private static void expectFailsIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected invalid caller input.
        }
    }

    private static void closeUnchecked(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception failure) {
            throw new AssertionError("Could not close test resource", failure);
        }
    }

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.01f;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static XmlAttribute annotation(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            XmlAttribute attribute = type.getMethod(methodName, parameterTypes).getAnnotation(XmlAttribute.class);
            if (attribute == null) {
                throw new AssertionError("Missing @XmlAttribute on " + type.getSimpleName() + "." + methodName);
            }
            return attribute;
        } catch (NoSuchMethodException failure) {
            throw new AssertionError("Missing method " + type.getSimpleName() + "." + methodName, failure);
        }
    }

    private static Path tempXmlPath() {
        try {
            return Files.createTempFile("unigui-xml-widget-", ".xml");
        } catch (IOException failure) {
            throw new AssertionError("Could not create temporary XML file", failure);
        }
    }

    private static final class Badge extends TextWidget {
        private float importance;

        private float importance() {
            return importance;
        }

        private Badge importance(float importance) {
            this.importance = importance;
            return this;
        }
    }

    @XmlWidgetName("AnnotatedBadge")
    private static final class AnnotatedBadge extends TextWidget {
        private float importance;
        private boolean active;
        private Tone tone = Tone.INFO;
        private final MutableColor accent = new MutableColor();

        private float importance() {
            return importance;
        }

        @XmlAttribute(value = "importance", category = "Behavior", defaultValue = "0", description = "Sort priority for annotation registration tests.")
        public AnnotatedBadge importance(float importance) {
            this.importance = importance;
            return this;
        }

        private boolean active() {
            return active;
        }

        @XmlAttribute(value = "active", category = "Behavior", defaultValue = "false")
        public AnnotatedBadge active(boolean active) {
            this.active = active;
            return this;
        }

        private Tone tone() {
            return tone;
        }

        @XmlAttribute(value = "tone", category = "Content", defaultValue = "info")
        public AnnotatedBadge tone(Tone tone) {
            this.tone = tone == null ? Tone.INFO : tone;
            return this;
        }

        private MutableColor accent() {
            return accent;
        }

        @XmlAttribute(value = "accent", category = "Appearance", defaultValue = "#FFFFFFFF")
        public AnnotatedBadge accent(MutableColor accent) {
            this.accent.set(accent == null ? new MutableColor() : accent);
            return this;
        }

        private enum Tone {
            INFO,
            WARNING
        }
    }

    private static final class UnsupportedXmlWidget extends WidgetBase {
        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(0.0f, 0.0f);
        }

        @Override
        public void render(RenderContext context) {
        }

        @Override
        public void handle(Event event) {
        }
    }
}
