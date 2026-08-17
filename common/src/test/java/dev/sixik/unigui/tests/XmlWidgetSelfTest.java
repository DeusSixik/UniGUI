package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.SizeUnit;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.selection.SelectionMode;
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
import dev.sixik.unigui.widgets.editor.AssetBrowserPanel;
import dev.sixik.unigui.widgets.editor.CommandPalette;
import dev.sixik.unigui.widgets.editor.DesignCanvasOverlay;
import dev.sixik.unigui.widgets.editor.DiagnosticsStrip;
import dev.sixik.unigui.widgets.editor.Dialog;
import dev.sixik.unigui.widgets.editor.DragSource;
import dev.sixik.unigui.widgets.editor.DropTarget;
import dev.sixik.unigui.widgets.editor.GridOverlay;
import dev.sixik.unigui.widgets.editor.PalettePanel;
import dev.sixik.unigui.widgets.editor.PaneHeader;
import dev.sixik.unigui.widgets.editor.PropertyFieldRow;
import dev.sixik.unigui.widgets.editor.PropertyGrid;
import dev.sixik.unigui.widgets.editor.ProjectPickerPanel;
import dev.sixik.unigui.widgets.editor.ResizablePanelHeader;
import dev.sixik.unigui.widgets.editor.SearchBoxWithFilterChips;
import dev.sixik.unigui.widgets.editor.SelectionOverlay;
import dev.sixik.unigui.widgets.editor.StatusBar;
import dev.sixik.unigui.widgets.editor.WidgetPalette;
import dev.sixik.unigui.widgets.editor.XmlDesignCanvas;
import dev.sixik.unigui.widgets.editor.XmlEditorDemoScreen;
import dev.sixik.unigui.widgets.editor.XmlHierarchyPanel;
import dev.sixik.unigui.widgets.editor.XmlPropertiesPanel;
import dev.sixik.unigui.widgets.editor.XmlRuntimeViewPane;
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
import net.minecraft.resources.ResourceLocation;

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
        testDescriptorMetadata();
        testXmlCodeEditorWidgetContracts();
        testDescriptorBackedInspectorModel();
        testTextureXmlAttributes();
        testXmlAssetCatalogAndPickerModel();
        testScrollViewContentChildPolicy();
        testDocumentRoundTripModel();
        testRuntimeWidgetSnapshotSerializer();
        testPrefabIncludeExpansionModel();
        testItemAndControlTemplateModel();
        testEditorDocumentIo();
        testHotReloadPreviewModel();
        testDiagnosticsPanelWidget();
        testEditorHierarchyAndSelectionModels();
        testEditorLayoutDragResizeHandles();
        testGridOverlayWidgetContracts();
        testXmlDesignCanvasWidgetContracts();
        testXmlRuntimeViewPaneContracts();
        testUndoableDocumentEdits();
        testEditorDiagnosticsCollection();
        testXmlEditorSessionParseFailurePreservesLastValidDocument();
        testXmlEditorSessionUndoRedoAndSelectionSurvival();
        testXmlEditorSessionSaveRevertAndEvents();
        testXmlEditorDemoScreenWorkspaceContracts();
        testXmlHierarchyPanelWidgetContracts();
        testXmlPropertiesPanelSessionContracts();
        testXmlPropertiesPanelObjectPickerContracts();
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

    private void testDescriptorMetadata() {
        XmlWidgetRegistry registry = XMLWidget.registry();
        XmlWidgetDescriptor box = registry.descriptor("Box").orElseThrow();
        expect(box.xmlName().equals("Box") && box.category().equals("Containers"),
                "Built-in widget descriptors should expose palette metadata");
        expect(box.acceptsChildren(), "Container descriptors should expose child support");
        expect(registry.descriptors().stream()
                        .allMatch(descriptor -> !descriptor.displayName().isBlank()
                                && !descriptor.category().isBlank()
                                && !descriptor.description().isBlank()),
                "Built-in widget descriptors should include display/category/description metadata");
        expect(registry.descriptors().stream()
                        .flatMap(descriptor -> descriptor.attributes().stream())
                        .allMatch(attribute -> !attribute.displayName().isBlank()
                                && !attribute.category().isBlank()
                                && attribute.defaultValue() != null
                                && !attribute.description().isBlank()),
                "Built-in XML attribute descriptors should include display/category/default/description metadata");
        XmlPropertyChildDescriptor boxChildren = propertyChild(box.propertyChildren(), "Children");
        expect(boxChildren.category().equals("Content") && !boxChildren.description().isBlank(),
                "Container descriptors should expose property-child metadata for explicit Children slots");
        XmlPropertyChildDescriptor scrollContent = propertyChild(
                registry.descriptor("ScrollView").orElseThrow().propertyChildren(),
                "Content");
        expect(scrollContent.category().equals("Content") && !scrollContent.description().isBlank(),
                "ScrollView descriptor should expose property-child metadata for the Content slot");
        expect(scrollContent.singleChild(),
                "ScrollView Content descriptor should expose single-child cardinality metadata");
        expect(registry.descriptors().stream()
                        .flatMap(descriptor -> descriptor.propertyChildren().stream())
                        .allMatch(property -> !property.displayName().isBlank()
                                && !property.category().isBlank()
                                && !property.description().isBlank()),
                "Built-in property-child descriptors should include display/category/description metadata");
        XmlAttributeDescriptor backgroundTexture = descriptor(box.attributes(), "backgroundTexture");
        expect(backgroundTexture.displayName().equals("Background Texture"),
                "Attribute descriptors should expose human-readable names");
        expect(backgroundTexture.category().equals("Assets")
                        && backgroundTexture.valueType() == XmlAttributeValueType.RESOURCE_ID
                        && backgroundTexture.description().contains("textureResolver"),
                "Texture attributes should expose asset-picker friendly metadata");
        XmlAttributeDescriptor imageTexture = descriptor(registry.descriptor("ImageView").orElseThrow().attributes(), "texture");
        expect(imageTexture.category().equals("Assets")
                        && imageTexture.valueType() == XmlAttributeValueType.RESOURCE_ID
                        && imageTexture.defaultValue().isEmpty(),
                "ImageView texture metadata should identify editable asset ids");
        XmlWidgetDescriptor canvas = registry.descriptor("CanvasWidget").orElseThrow();
        expect(canvas.category().equals("Display")
                        && canvas.description().contains("draw callbacks")
                        && descriptor(canvas.attributes(), "width").category().equals("Layout")
                        && descriptor(canvas.attributes(), "width").valueType() == XmlAttributeValueType.SIZE_VALUE,
                "CanvasWidget descriptor should expose editor-facing custom draw limitations");
        XmlWidgetDescriptor path = registry.descriptor("Path").orElseThrow();
        expect(path.category().equals("Display")
                        && descriptor(path.attributes(), "data").category().equals("Content")
                        && descriptor(path.attributes(), "data").valueType() == XmlAttributeValueType.STRING
                        && descriptor(path.attributes(), "color").category().equals("Appearance")
                        && descriptor(path.attributes(), "color").valueType() == XmlAttributeValueType.COLOR
                        && descriptor(path.attributes(), "strokeWidth").category().equals("Appearance"),
                "Path descriptor should expose limited vector path XML metadata");
        dev.sixik.unigui.widgets.display.Path xmlPath = XMLWidget.create("""
                <Path id="vectorArrow"
                      data="M 0 0 L 1 0 Q 1 0.5 0.5 1 C 0.25 1 0 0.75 0 0 Z"
                      color="#336699CC" stroke="false" strokeWidth="2"
                      width="32" height="32" />
                """, dev.sixik.unigui.widgets.display.Path.class);
        expect(xmlPath.id().equals("vectorArrow")
                        && xmlPath.path().size() == 5
                        && !xmlPath.stroke()
                        && near(xmlPath.strokeWidth(), 2.0f)
                        && near(xmlPath.color().r(), 0x33 / 255.0f)
                        && near(xmlPath.color().a(), 0xCC / 255.0f)
                        && near(xmlPath.layoutStyle().width().value(), 32.0f),
                "Path should materialize limited XML path data, color and stroke attributes");
        expectFails("<Path data=\"H 2\" />", "Unsupported path command 'H'");
        XmlWidgetDescriptor cachedSubtree = registry.descriptor("CachedSubtreeWidget").orElseThrow();
        expect(cachedSubtree.category().equals("Performance")
                        && cachedSubtree.acceptsChildren()
                        && descriptor(cachedSubtree.attributes(), "targetOptions").category().equals("Performance")
                        && descriptor(cachedSubtree.attributes(), "targetOptions").valueType() == XmlAttributeValueType.ENUM
                        && descriptor(cachedSubtree.attributes(), "tint").category().equals("Appearance")
                        && descriptor(cachedSubtree.attributes(), "tint").valueType() == XmlAttributeValueType.COLOR
                        && propertyChild(cachedSubtree.propertyChildren(), "Content").singleChild(),
                "CachedSubtreeWidget descriptor should expose advanced opt-in cache controls");
        CachedSubtreeWidget xmlCachedSubtree = XMLWidget.create("""
                <CachedSubtreeWidget id="cachedPreview" tint="#80FFFFFF" targetOptions="colorDepth">
                    <CachedSubtreeWidget.Content>
                        <Label id="cachedContent" text="Cached" />
                    </CachedSubtreeWidget.Content>
                </CachedSubtreeWidget>
                """, CachedSubtreeWidget.class);
        expect(xmlCachedSubtree.id().equals("cachedPreview")
                        && near(xmlCachedSubtree.tint().r(), 0x80 / 255.0f)
                        && near(xmlCachedSubtree.tint().a(), 1.0f)
                        && xmlCachedSubtree.targetOptions().equals(RenderTargetOptions.COLOR_DEPTH)
                        && xmlCachedSubtree.content() instanceof Label cachedContent
                        && cachedContent.text().equals("Cached"),
                "CachedSubtreeWidget should materialize tint, target options and one content subtree from XML");
        expectFails("""
                <CachedSubtreeWidget>
                    <Label text="One" />
                    <Label text="Two" />
                </CachedSubtreeWidget>
                """, "Widget CachedSubtreeWidget can contain only one content child");
        expect(registry.descriptor("ScrollViewer").orElseThrow().xmlName().equals("ScrollView"),
                "Registry descriptors should resolve aliases to target descriptors");
        XmlWidgetDescriptor textArea = registry.descriptor("TextArea").orElseThrow();
        expect(textArea.category().equals("Controls")
                        && descriptor(textArea.attributes(), "visibleLines").category().equals("Layout"),
                "TextArea descriptor should expose editor-facing multiline metadata");
        XmlWidgetDescriptor textInput = registry.descriptor("TextInput").orElseThrow();
        expect(textInput.category().equals("Controls")
                        && descriptor(textInput.attributes(), "placeholder").category().equals("Content"),
                "TextInput descriptor should expose editor-facing single-line metadata");
        XmlWidgetDescriptor numberField = registry.descriptor("NumberField").orElseThrow();
        expect(numberField.category().equals("Controls")
                        && descriptor(numberField.attributes(), "step").category().equals("Behavior"),
                "NumberField descriptor should expose numeric editing metadata");
        expect(descriptor(registry.descriptor("SearchField").orElseThrow().attributes(), "searchChangeDebounceSeconds")
                        .category().equals("Behavior"),
                "SearchField descriptor should expose debounce metadata");
        expect(descriptor(registry.descriptor("PasswordField").orElseThrow().attributes(), "mask")
                        .category().equals("Appearance"),
                "PasswordField descriptor should expose mask metadata");
        expect(descriptor(registry.descriptor("TimeSpanField").orElseThrow().attributes(), "value")
                        .category().equals("Behavior"),
                "TimeSpanField descriptor should expose duration value metadata");
        XmlWidgetDescriptor toggleSwitch = registry.descriptor("ToggleSwitch").orElseThrow();
        expect(toggleSwitch.category().equals("Controls")
                        && descriptor(toggleSwitch.attributes(), "trackWidth").category().equals("Layout"),
                "ToggleSwitch descriptor should expose switch-specific metadata");
        XmlWidgetDescriptor radioButton = registry.descriptor("RadioButton").orElseThrow();
        expect(radioButton.category().equals("Controls")
                        && descriptor(radioButton.attributes(), "value").category().equals("Behavior"),
                "RadioButton descriptor should expose radio-option metadata");
        XmlWidgetDescriptor comboBox = registry.descriptor("ComboBox").orElseThrow();
        expect(comboBox.category().equals("Controls")
                        && descriptor(comboBox.attributes(), "items").category().equals("Content"),
                "ComboBox descriptor should expose simple option-list metadata");
        XmlWidgetDescriptor dropDownBox = registry.descriptor("DropDownBox").orElseThrow();
        expect(dropDownBox.category().equals("Controls")
                        && propertyChild(dropDownBox.propertyChildren(), "Content").singleChild(),
                "DropDownBox descriptor should expose its dropdown content property child");
        XmlWidgetDescriptor treeListPicker = registry.descriptor("TreeListPicker").orElseThrow();
        expect(treeListPicker.category().equals("Controls")
                        && descriptor(treeListPicker.attributes(), "items").category().equals("Content")
                        && descriptor(treeListPicker.attributes(), "selectedIndex").category().equals("Behavior"),
                "TreeListPicker descriptor should expose simple path-list picker metadata");
        XmlWidgetDescriptor datePicker = registry.descriptor("DatePicker").orElseThrow();
        expect(datePicker.category().equals("Controls")
                        && descriptor(datePicker.attributes(), "value").category().equals("Behavior"),
                "DatePicker descriptor should expose ISO date value metadata");
        XmlWidgetDescriptor colorPicker = registry.descriptor("ColorPicker").orElseThrow();
        expect(colorPicker.category().equals("Controls")
                        && descriptor(colorPicker.attributes(), "color").category().equals("Appearance")
                        && descriptor(colorPicker.attributes(), "argb").category().equals("Appearance")
                        && descriptor(colorPicker.attributes(), "type").category().equals("Behavior"),
                "ColorPicker descriptor should expose color value and editor mode metadata");
        TextInput rawInput = XMLWidget.create(
                "<TextInput id=\"rawInput\" text=\"abc\" placeholder=\"Type...\" maxLength=\"8\" cursorIndex=\"2\" />",
                TextInput.class);
        expect(rawInput.id().equals("rawInput")
                        && rawInput.text().equals("abc")
                        && rawInput.placeholder().equals("Type...")
                        && rawInput.maxLength() == 8
                        && rawInput.cursorIndex() == 2,
                "TextInput should materialize from built-in XML registration");
        NumberField xmlNumber = XMLWidget.create(
                "<NumberField value=\"12.5\" min=\"0\" max=\"10\" step=\"0.25\" />",
                NumberField.class);
        expect(xmlNumber.value() == 10.0d && xmlNumber.text().equals("10"),
                "NumberField should materialize and clamp XML values through min/max attributes");
        SearchField xmlSearch = XMLWidget.create(
                "<SearchField text=\"reactor\" placeholder=\"Find\" searchChangeDebounceSeconds=\"0.1\" />",
                SearchField.class);
        expect(xmlSearch.text().equals("reactor")
                        && xmlSearch.placeholder().equals("Find")
                        && near(xmlSearch.searchChangeDebounceSeconds(), 0.1f),
                "SearchField should materialize from built-in XML registration");
        PasswordField xmlPassword = XMLWidget.create(
                "<PasswordField text=\"secret\" mask=\"*\" />",
                PasswordField.class);
        expect(xmlPassword.text().equals("secret") && xmlPassword.mask() == '*',
                "PasswordField should materialize text and mask attributes from XML");
        TimeSpanField xmlTimeSpan = XMLWidget.create(
                "<TimeSpanField value=\"01:02:03\" />",
                TimeSpanField.class);
        expect(xmlTimeSpan.value().equals(Duration.ofSeconds(3723L)) && xmlTimeSpan.text().equals("01:02:03"),
                "TimeSpanField should materialize duration values from XML");
        ToggleSwitch xmlSwitch = XMLWidget.create(
                "<ToggleSwitch text=\"Enabled\" checked=\"true\" trackWidth=\"42\" thumbSize=\"12\" labelLeft=\"true\" />",
                ToggleSwitch.class);
        expect(xmlSwitch.text().equals("Enabled")
                        && xmlSwitch.checked()
                        && near(xmlSwitch.trackWidth(), 42.0f)
                        && near(xmlSwitch.thumbSize(), 12.0f)
                        && xmlSwitch.labelLeft(),
                "ToggleSwitch should materialize switch-specific XML attributes");
        RadioButton xmlRadio = XMLWidget.create(
                "<RadioButton text=\"Compact\" value=\"compact\" checked=\"true\" outerSize=\"14\" innerSize=\"7\" labelLeft=\"true\" />",
                RadioButton.class);
        expect(xmlRadio.text().equals("Compact")
                        && xmlRadio.value().equals("compact")
                        && xmlRadio.checked()
                        && near(xmlRadio.outerSize(), 14.0f)
                        && near(xmlRadio.innerSize(), 7.0f)
                        && xmlRadio.labelLeft(),
                "RadioButton should materialize radio-option XML attributes");
        ComboBox xmlComboBox = XMLWidget.create(
                "<ComboBox items=\"Small|Medium|Large\" selectedIndex=\"1\" placeholder=\"Size\" dropDownMode=\"inline\" opened=\"true\" />",
                ComboBox.class);
        expect(xmlComboBox.itemCount() == 3
                        && xmlComboBox.selectedIndex() == 1
                        && xmlComboBox.selectedItem().equals("Medium")
                        && xmlComboBox.placeholder().equals("Size")
                        && xmlComboBox.dropDownMode() == ComboBox.DropDownMode.INLINE
                        && xmlComboBox.opened(),
                "ComboBox should materialize option-list XML attributes");
        DropDownBox xmlDropDownBox = XMLWidget.create("""
                <DropDownBox headerText="Filters" dropDownMode="inline" opened="true" maxContentHeight="80">
                    <DropDownBox.Content>
                        <Label id="filtersLabel" text="Enabled filters" />
                    </DropDownBox.Content>
                </DropDownBox>
                """, DropDownBox.class);
        expect(xmlDropDownBox.headerText().equals("Filters")
                        && xmlDropDownBox.dropDownMode() == ComboBox.DropDownMode.INLINE
                        && xmlDropDownBox.opened()
                        && near(xmlDropDownBox.maxContentHeight(), 80.0f)
                        && xmlDropDownBox.content() instanceof Label label
                        && label.text().equals("Enabled filters"),
                "DropDownBox should materialize dropdown content property children from XML");
        TreeListPicker<?> xmlTreeListPicker = XMLWidget.create(
                "<TreeListPicker id=\"pathPicker\" items=\"Project/Screens/Main|Project/Screens/Settings|Assets\" selectedIndex=\"1\" placeholder=\"Path\" dropDownMode=\"inline\" opened=\"true\" />",
                TreeListPicker.class);
        expect(xmlTreeListPicker.id().equals("pathPicker")
                        && xmlTreeListPicker.itemCount() == 3
                        && xmlTreeListPicker.selectedIndex() == 1
                        && xmlTreeListPicker.selectedItem().equals("Project/Screens/Settings")
                        && xmlTreeListPicker.placeholder().equals("Path")
                        && xmlTreeListPicker.dropDownMode() == ComboBox.DropDownMode.INLINE
                        && xmlTreeListPicker.opened(),
                "TreeListPicker should materialize simple path-list XML attributes");
        DatePicker xmlDatePicker = XMLWidget.create(
                "<DatePicker id=\"launchDate\" value=\"2026-08-17\" />",
                DatePicker.class);
        expect(xmlDatePicker.id().equals("launchDate")
                        && xmlDatePicker.value().equals(LocalDate.of(2026, 8, 17))
                        && xmlDatePicker.field().text().equals("2026-08-17"),
                "DatePicker should materialize ISO date values from XML");
        ColorPicker xmlColorPicker = XMLWidget.create(
                "<ColorPicker id=\"accentColor\" color=\"#336699CC\" type=\"argb\" />",
                ColorPicker.class);
        expect(xmlColorPicker.id().equals("accentColor")
                        && near(xmlColorPicker.color().r(), 0x33 / 255.0f)
                        && near(xmlColorPicker.color().g(), 0x66 / 255.0f)
                        && near(xmlColorPicker.color().b(), 0x99 / 255.0f)
                        && near(xmlColorPicker.color().a(), 0xCC / 255.0f)
                        && xmlColorPicker.type() == ColorPicker.Type.ARGB,
                "ColorPicker should materialize color and picker mode from XML");
        XmlWidgetDescriptor loadingIndicator = registry.descriptor("LoadingIndicator").orElseThrow();
        expect(loadingIndicator.category().equals("Feedback")
                        && descriptor(loadingIndicator.attributes(), "mode").category().equals("Behavior")
                        && descriptor(loadingIndicator.attributes(), "segments").category().equals("Appearance")
                        && descriptor(loadingIndicator.attributes(), "preferredWidth").category().equals("Layout"),
                "LoadingIndicator descriptor should expose animation, appearance and sizing metadata");
        LoadingIndicator xmlLoadingIndicator = XMLWidget.create("""
                <LoadingIndicator id="busyIndicator" mode="bar" running="false"
                                  phase="1.25" speed="2" segments="12"
                                  preferredWidth="120" preferredHeight="10" />
                """, LoadingIndicator.class);
        expect(xmlLoadingIndicator.id().equals("busyIndicator")
                        && xmlLoadingIndicator.mode() == LoadingIndicator.Mode.BAR
                        && !xmlLoadingIndicator.running()
                        && near(xmlLoadingIndicator.phase(), 0.25f)
                        && near(xmlLoadingIndicator.speed(), 2.0f)
                        && xmlLoadingIndicator.segments() == 12
                        && near(xmlLoadingIndicator.preferredWidth(), 120.0f)
                        && near(xmlLoadingIndicator.preferredHeight(), 10.0f),
                "LoadingIndicator should materialize animation and sizing attributes from XML");
        XmlWidgetDescriptor toast = registry.descriptor("Toast").orElseThrow();
        expect(toast.category().equals("Feedback")
                        && descriptor(toast.attributes(), "text").category().equals("Content")
                        && descriptor(toast.attributes(), "duration").category().equals("Behavior")
                        && descriptor(toast.attributes(), "open").category().equals("Behavior"),
                "Toast descriptor should expose message, duration and open state metadata");
        Toast xmlToast = XMLWidget.create("""
                <Toast id="saveToast" text="Saved" duration="0" margin="14" open="true" />
                """, Toast.class);
        expect(xmlToast.id().equals("saveToast")
                        && xmlToast.text().equals("Saved")
                        && near(xmlToast.duration(), 0.0f)
                        && near(xmlToast.margin(), 14.0f)
                        && xmlToast.opened(),
                "Toast should materialize visible transient message state from XML");
        XmlWidgetDescriptor notificationView = registry.descriptor("NotificationView").orElseThrow();
        expect(notificationView.category().equals("Feedback")
                        && descriptor(notificationView.attributes(), "text").category().equals("Content")
                        && descriptor(notificationView.attributes(), "maxVisible").category().equals("Behavior")
                        && descriptor(notificationView.attributes(), "placement").category().equals("Layout")
                        && descriptor(notificationView.attributes(), "open").category().equals("Behavior"),
                "NotificationView descriptor should expose stacked notification metadata");
        NotificationView xmlNotificationView = XMLWidget.create("""
                <NotificationView id="notifications" text="Build complete" duration="0"
                                  spacing="6" margin="12" maxVisible="2"
                                  placement="bottom_right" open="true" />
                """, NotificationView.class);
        expect(xmlNotificationView.id().equals("notifications")
                        && xmlNotificationView.text().equals("Build complete")
                        && near(xmlNotificationView.duration(), 0.0f)
                        && near(xmlNotificationView.spacing(), 6.0f)
                        && near(xmlNotificationView.margin(), 12.0f)
                        && xmlNotificationView.maxVisible() == 2
                        && xmlNotificationView.placement() == NotificationView.Placement.BOTTOM_RIGHT
                        && xmlNotificationView.opened()
                        && xmlNotificationView.activeCount() == 1
                        && xmlNotificationView.notifications().get(0).text().equals("Build complete"),
                "NotificationView should materialize a visible pending notification from XML");
        XmlWidgetDescriptor contextMenu = registry.descriptor("ContextMenu").orElseThrow();
        expect(contextMenu.category().equals("Feedback")
                        && contextMenu.acceptsChildren()
                        && descriptor(contextMenu.attributes(), "open").category().equals("Behavior")
                        && contextMenu.propertyChildren().stream().anyMatch(property -> property.name().equals("Items")),
                "ContextMenu descriptor should expose menu item child metadata");
        ContextMenu xmlContextMenu = XMLWidget.create("""
                <ContextMenu open="true" selectedItem="1">
                    <ContextMenu.Items>
                        <Button id="copyAction" text="Copy" />
                        <Separator id="contextDivider" />
                        <Button id="pasteAction" text="Paste" enabled="false" />
                    </ContextMenu.Items>
                </ContextMenu>
                """, ContextMenu.class);
        expect(xmlContextMenu.opened()
                        && xmlContextMenu.itemCount() == 2
                        && xmlContextMenu.selectedItemIndex() == 1
                        && xmlContextMenu.itemButton(0).text().equals("Copy")
                        && xmlContextMenu.itemButton(0) == XMLWidget.getWidget(xmlContextMenu, "copyAction", Button.class)
                        && !xmlContextMenu.itemButton(1).enabled()
                        && XMLWidget.getWidget(xmlContextMenu, "contextDivider", Separator.class).parent() != null,
                "ContextMenu should materialize XML Button and Separator children as retained menu items");
        XmlWidgetDescriptor overlayLayer = registry.descriptor("OverlayLayer").orElseThrow();
        expect(overlayLayer.category().equals("Feedback")
                        && propertyChild(overlayLayer.propertyChildren(), "Content").singleChild()
                        && overlayLayer.propertyChildren().stream().anyMatch(property -> property.name().equals("Overlays")),
                "OverlayLayer descriptor should expose content and overlays property children");
        XmlWidgetDescriptor popup = registry.descriptor("Popup").orElseThrow();
        expect(popup.category().equals("Feedback")
                        && descriptor(popup.attributes(), "closeOnOutsideClick").category().equals("Behavior")
                        && propertyChild(popup.propertyChildren(), "Content").singleChild(),
                "Popup descriptor should expose behavior attributes and content child metadata");
        XmlWidgetDescriptor tooltip = registry.descriptor("Tooltip").orElseThrow();
        expect(tooltip.category().equals("Feedback")
                        && descriptor(tooltip.attributes(), "maxWidth").category().equals("Layout"),
                "Tooltip descriptor should expose text tooltip metadata");
        XmlWidgetDescriptor windowWidget = registry.descriptor("WindowWidget").orElseThrow();
        expect(windowWidget.category().equals("Feedback")
                        && descriptor(windowWidget.attributes(), "title").category().equals("Content")
                        && descriptor(windowWidget.attributes(), "windowX").category().equals("Layout")
                        && propertyChild(windowWidget.propertyChildren(), "Content").singleChild(),
                "WindowWidget descriptor should expose window content and placement metadata");
        OverlayLayer xmlOverlayLayer = XMLWidget.create("""
                <OverlayLayer id="overlayHost" width="320" height="200" modalScrimColor="#00000080">
                    <OverlayLayer.Content>
                        <VBox id="overlayContent" />
                    </OverlayLayer.Content>
                    <OverlayLayer.Overlays>
                        <WindowWidget id="inspectorWindow" title="Inspector" open="true" modal="true"
                                      closeButtonVisible="false" windowX="12" windowY="18">
                            <WindowWidget.Content>
                                <Label id="windowBody" text="Properties" />
                            </WindowWidget.Content>
                        </WindowWidget>
                        <Popup id="inlinePopup" open="true" closeOnOutsideClick="false" padding="4">
                            <Popup.Content>
                                <Label id="popupBody" text="Popup body" />
                            </Popup.Content>
                        </Popup>
                        <Tooltip id="helpTip" text="Hover help" maxWidth="180" />
                    </OverlayLayer.Overlays>
                </OverlayLayer>
                """, OverlayLayer.class);
        WindowWidget xmlWindow = XMLWidget.getWidget(xmlOverlayLayer, "inspectorWindow", WindowWidget.class);
        Popup xmlPopup = XMLWidget.getWidget(xmlOverlayLayer, "inlinePopup", Popup.class);
        Tooltip xmlTooltip = XMLWidget.getWidget(xmlOverlayLayer, "helpTip", Tooltip.class);
        expect(xmlOverlayLayer.content() instanceof VBox
                        && XMLWidget.getWidget(xmlOverlayLayer, "overlayContent", VBox.class) == xmlOverlayLayer.content()
                        && near(xmlOverlayLayer.modalScrimColor().a(), 0x80 / 255.0f)
                        && xmlWindow.opened()
                        && xmlWindow.modal()
                        && !xmlWindow.closeButtonVisible()
                        && near(xmlWindow.windowX(), 12.0f)
                        && near(xmlWindow.windowY(), 18.0f)
                        && xmlWindow.content() instanceof Label windowBody
                        && windowBody.text().equals("Properties")
                        && xmlPopup.opened()
                        && !xmlPopup.closeOnOutsideClick()
                        && xmlPopup.content() instanceof Label popupBody
                        && popupBody.text().equals("Popup body")
                        && xmlTooltip.text().equals("Hover help")
                        && near(xmlTooltip.maxWidth(), 180.0f),
                "OverlayLayer should materialize content, windows, popups and tooltips from XML property children");
        OverlayLayer zLayer = new OverlayLayer(new VBox());
        Popup zPopup = new Popup(null, new Label("Popup"));
        WindowWidget zWindow = new WindowWidget().title("Window").open();
        zLayer.addOverlay(zPopup);
        zLayer.addOverlay(zWindow);
        zLayer.applyQueuedMutations();
        zPopup.open();
        expect(zLayer.children().get(zLayer.children().size() - 1) == zPopup,
                "Opening a popup should bring it above existing overlay windows");
        XmlWidgetDescriptor virtualListView = registry.descriptor("VirtualListView").orElseThrow();
        expect(virtualListView.category().equals("Data")
                        && descriptor(virtualListView.attributes(), "itemCount").category().equals("Data")
                        && descriptor(virtualListView.attributes(), "selectionMode").category().equals("Behavior"),
                "VirtualListView descriptor should expose virtual list metadata");
        VirtualListView xmlVirtualList = XMLWidget.create("""
                <VirtualListView itemCount="12" itemHeight="22" overscan="3" selectionMode="multiple" activeIndex="5" />
                """, VirtualListView.class);
        expect(xmlVirtualList.itemCount() == 12
                        && near(xmlVirtualList.itemHeight(), 22.0f)
                        && xmlVirtualList.overscan() == 3
                        && xmlVirtualList.selectionMode() == SelectionMode.MULTIPLE
                        && xmlVirtualList.activeIndex() == 5,
                "VirtualListView should materialize from built-in XML registration");
        XmlWidgetDescriptor virtualTableView = registry.descriptor("VirtualTableView").orElseThrow();
        expect(virtualTableView.category().equals("Data")
                        && descriptor(virtualTableView.attributes(), "rowCount").category().equals("Data")
                        && descriptor(virtualTableView.attributes(), "columns").category().equals("Data"),
                "VirtualTableView descriptor should expose virtual table metadata");
        VirtualTableView xmlVirtualTable = XMLWidget.create("""
                <VirtualTableView columns="Name:120|Value:64" rowCount="7" rowHeight="19" headerHeight="24"
                                  overscan="2" editable="true" selectionMode="multiple" />
                """, VirtualTableView.class);
        expect(xmlVirtualTable.rowCount() == 7
                        && xmlVirtualTable.columns().size() == 2
                        && xmlVirtualTable.columns().get(0).header().equals("Name")
                        && near(xmlVirtualTable.columns().get(0).width(), 120.0f)
                        && near(xmlVirtualTable.rowHeight(), 19.0f)
                        && near(xmlVirtualTable.headerHeight(), 24.0f)
                        && xmlVirtualTable.overscan() == 2
                        && xmlVirtualTable.editable()
                        && xmlVirtualTable.selectionMode() == SelectionMode.MULTIPLE,
                "VirtualTableView should materialize rows, columns and editable state from built-in XML registration");
        XmlWidgetDescriptor treeView = registry.descriptor("TreeView").orElseThrow();
        expect(treeView.category().equals("Navigation")
                        && descriptor(treeView.attributes(), "nodes").category().equals("Data")
                        && descriptor(treeView.attributes(), "rowTextHoverScrollSpeed").category().equals("Behavior"),
                "TreeView descriptor should expose path-backed XML node metadata");
        TreeView xmlTreeView = XMLWidget.create("""
                <TreeView nodes="Project/Screens/Main|Project/Screens/Settings|Assets" rowTextHoverScrollSpeed="30" />
                """, TreeView.class);
        expect(xmlTreeView.rootCount() == 2
                        && xmlTreeView.root(0).value().equals("Project")
                        && xmlTreeView.root(0).child(0).value().equals("Screens")
                        && xmlTreeView.root(0).child(0).childCount() == 2
                        && xmlTreeView.root(1).value().equals("Assets")
                        && near(xmlTreeView.rowTextHoverScrollSpeed(), 30.0f),
                "TreeView should materialize simple path-backed XML nodes");
        XmlWidgetDescriptor tabControl = registry.descriptor("TabControl").orElseThrow();
        expect(tabControl.category().equals("Navigation")
                        && descriptor(tabControl.attributes(), "selectedIndex").category().equals("Behavior")
                        && tabControl.propertyChildren().stream().anyMatch(property -> property.name().equals("Tabs")),
                "TabControl descriptor should expose selected tab and XML tab content metadata");
        TabControl xmlTabs = XMLWidget.create("""
                <TabControl id="tabs" selectedIndex="1">
                    <TabControl.Tabs>
                        <Label id="firstTab" text="First" />
                        <Label id="secondTab" text="Second" />
                    </TabControl.Tabs>
                </TabControl>
                """, TabControl.class);
        expect(xmlTabs.id().equals("tabs")
                        && xmlTabs.tabCount() == 2
                        && xmlTabs.selectedIndex() == 1
                        && XMLWidget.getWidget(xmlTabs, "secondTab", Label.class).text().equals("Second"),
                "TabControl should materialize XML tab content and selected index");
        XmlWidgetDescriptor pageView = registry.descriptor("PageView").orElseThrow();
        expect(pageView.category().equals("Navigation")
                        && descriptor(pageView.attributes(), "selectedIndex").category().equals("Behavior")
                        && pageView.propertyChildren().stream().anyMatch(property -> property.name().equals("Pages")),
                "PageView descriptor should expose page content metadata");
        PageView xmlPageView = XMLWidget.create("""
                <PageView id="pages" selectedIndex="1">
                    <PageView.Pages>
                        <Label id="pageOne" text="One" />
                        <Label id="pageTwo" text="Two" />
                    </PageView.Pages>
                </PageView>
                """, PageView.class);
        expect(xmlPageView.id().equals("pages")
                        && xmlPageView.pageCount() == 2
                        && xmlPageView.selectedIndex() == 1
                        && XMLWidget.getWidget(xmlPageView, "pageTwo", Label.class).text().equals("Two"),
                "PageView should materialize XML pages and selected index");
        XmlWidgetDescriptor expandablePanel = registry.descriptor("ExpandablePanel").orElseThrow();
        expect(expandablePanel.category().equals("Navigation")
                        && descriptor(expandablePanel.attributes(), "title").category().equals("Content")
                        && descriptor(expandablePanel.attributes(), "expanded").category().equals("Behavior")
                        && expandablePanel.propertyChildren().stream().anyMatch(property -> property.name().equals("Content")),
                "ExpandablePanel descriptor should expose title, expanded state and content slot metadata");
        ExpandablePanel xmlExpandable = XMLWidget.create("""
                <ExpandablePanel id="advancedPanel" title="Advanced" expanded="false">
                    <ExpandablePanel.Content>
                        <Label id="advancedBody" text="Options" />
                    </ExpandablePanel.Content>
                </ExpandablePanel>
                """, ExpandablePanel.class);
        expect(xmlExpandable.id().equals("advancedPanel")
                        && xmlExpandable.title().equals("Advanced")
                        && !xmlExpandable.expanded()
                        && XMLWidget.getWidget(xmlExpandable, "advancedBody", Label.class).text().equals("Options"),
                "ExpandablePanel should materialize title, expanded state and content from XML");
        XmlWidgetDescriptor accordion = registry.descriptor("Accordion").orElseThrow();
        expect(accordion.category().equals("Navigation")
                        && descriptor(accordion.attributes(), "singleOpen").category().equals("Behavior")
                        && accordion.propertyChildren().stream().anyMatch(property -> property.name().equals("Panels")),
                "Accordion descriptor should expose single-open behavior and panel child metadata");
        Accordion xmlAccordion = XMLWidget.create("""
                <Accordion id="settingsAccordion" singleOpen="true">
                    <Accordion.Panels>
                        <ExpandablePanel id="generalPanel" title="General" expanded="true">
                            <Label id="generalBody" text="General body" />
                        </ExpandablePanel>
                        <ExpandablePanel id="advancedAccordionPanel" title="Advanced" expanded="true">
                            <Label id="advancedAccordionBody" text="Advanced body" />
                        </ExpandablePanel>
                    </Accordion.Panels>
                </Accordion>
                """, Accordion.class);
        expect(xmlAccordion.id().equals("settingsAccordion")
                        && xmlAccordion.singleOpen()
                        && xmlAccordion.panels().size() == 2
                        && !xmlAccordion.panels().get(0).expanded()
                        && xmlAccordion.panels().get(1).expanded()
                        && XMLWidget.getWidget(xmlAccordion, "advancedAccordionBody", Label.class).text().equals("Advanced body"),
                "Accordion should materialize expandable panel children and enforce single-open state");
        XmlWidgetDescriptor carousel = registry.descriptor("Carousel").orElseThrow();
        expect(carousel.category().equals("Navigation")
                        && descriptor(carousel.attributes(), "selectedIndex").category().equals("Behavior")
                        && carousel.propertyChildren().stream().anyMatch(property -> property.name().equals("Pages")),
                "Carousel descriptor should expose selected page and page child metadata");
        Carousel xmlCarousel = XMLWidget.create("""
                <Carousel id="carousel" selectedIndex="1">
                    <Carousel.Pages>
                        <Label id="slideOne" text="Slide 1" />
                        <Label id="slideTwo" text="Slide 2" />
                    </Carousel.Pages>
                </Carousel>
                """, Carousel.class);
        expect(xmlCarousel.id().equals("carousel")
                        && xmlCarousel.pageView().pageCount() == 2
                        && xmlCarousel.selectedIndex() == 1
                        && xmlCarousel.indicator().text().equals("2 / 2")
                        && XMLWidget.getWidget(xmlCarousel, "slideTwo", Label.class).text().equals("Slide 2"),
                "Carousel should materialize XML pages, selected index and indicator state");
        XmlWidgetDescriptor dockingRoot = registry.descriptor("DockingRoot").orElseThrow();
        expect(dockingRoot.category().equals("Containers")
                        && descriptor(dockingRoot.attributes(), "tabHeight").category().equals("Layout")
                        && dockingRoot.propertyChildren().stream().anyMatch(property -> property.name().equals("Documents"))
                        && dockingRoot.propertyChildren().stream().anyMatch(property -> property.name().equals("ToolPanes")),
                "DockingRoot descriptor should expose document/tool-pane XML child metadata");
        DockingRoot xmlDockingRoot = XMLWidget.create("""
                <DockingRoot id="workspace" allowFloatingOutsideHost="true" floatingWindowsRedockLocked="true"
                             tabHeight="24" splitHandleThickness="6">
                    <DockingRoot.Documents>
                        <Label id="mainDoc" text="Main XML" />
                    </DockingRoot.Documents>
                    <DockingRoot.ToolPanes>
                        <Label id="palettePane" text="Palette" />
                    </DockingRoot.ToolPanes>
                </DockingRoot>
                """, DockingRoot.class);
        expect(xmlDockingRoot.id().equals("workspace")
                        && xmlDockingRoot.allowFloatingOutsideHost()
                        && xmlDockingRoot.floatingWindowsRedockLocked()
                        && near(xmlDockingRoot.tabHeight(), 24.0f)
                        && near(xmlDockingRoot.splitHandleThickness(), 6.0f)
                        && xmlDockingRoot.dockingManager().paneCount() == 2
                        && XMLWidget.getWidget(xmlDockingRoot, "mainDoc", Label.class).text().equals("Main XML")
                        && XMLWidget.getWidget(xmlDockingRoot, "palettePane", Label.class).text().equals("Palette"),
                "DockingRoot should materialize XML child widgets as document and tool panes");
        XmlWidgetDescriptor xmlCodeEditor = registry.descriptor("XmlCodeEditor").orElseThrow();
        expect(xmlCodeEditor.category().equals("Controls")
                        && descriptor(xmlCodeEditor.attributes(), "lineNumbers").category().equals("Appearance"),
                "XmlCodeEditor descriptor should expose editor-facing source metadata");
        XmlWidgetDescriptor menuBar = registry.descriptor("MenuBar").orElseThrow();
        expect(menuBar.category().equals("Navigation")
                        && descriptor(menuBar.attributes(), "height").category().equals("Layout"),
                "MenuBar descriptor should expose editor-facing navigation metadata");
        expect(XMLWidget.create("<MenuBar id=\"mainMenu\" height=\"22\" />", MenuBar.class).id().equals("mainMenu"),
                "MenuBar should materialize from built-in XML registration");
        XmlWidgetDescriptor propertyGrid = registry.descriptor("PropertyGrid").orElseThrow();
        expect(propertyGrid.category().equals("Editor")
                        && descriptor(propertyGrid.attributes(), "showUnsetAttributes").category().equals("Behavior")
                        && descriptor(propertyGrid.attributes(), "labelWidth").category().equals("Layout"),
                "PropertyGrid descriptor should expose editor inspector metadata");
        PropertyGrid xmlPropertyGrid = XMLWidget.create("""
                <PropertyGrid id="inspector" showUnsetAttributes="false"
                              showPropertyChildren="false" labelWidth="140" />
                """, PropertyGrid.class);
        expect(xmlPropertyGrid.id().equals("inspector")
                        && !xmlPropertyGrid.showUnsetAttributes()
                        && !xmlPropertyGrid.showPropertyChildren()
                        && near(xmlPropertyGrid.labelWidth(), 140.0f),
                "PropertyGrid should materialize inspector options from built-in XML registration");
        XmlWidgetDescriptor xmlPropertiesPanel = registry.descriptor("XmlPropertiesPanel").orElseThrow();
        expect(xmlPropertiesPanel.category().equals("Editor")
                        && descriptor(xmlPropertiesPanel.attributes(), "showUnsetAttributes").category().equals("Behavior")
                        && descriptor(xmlPropertiesPanel.attributes(), "labelWidth").category().equals("Layout"),
                "XmlPropertiesPanel descriptor should expose session-aware inspector metadata");
        XmlPropertiesPanel materializedPropertiesPanel = XMLWidget.create("""
                <XmlPropertiesPanel id="propertiesPanel" showUnsetAttributes="false"
                                    showPropertyChildren="false" labelWidth="136" />
                """, XmlPropertiesPanel.class);
        expect(materializedPropertiesPanel.id().equals("propertiesPanel")
                        && !materializedPropertiesPanel.showUnsetAttributes()
                        && !materializedPropertiesPanel.showPropertyChildren()
                        && near(materializedPropertiesPanel.labelWidth(), 136.0f),
                "XmlPropertiesPanel should materialize inspector options from built-in XML registration");
        XmlWidgetDescriptor selectionOverlay = registry.descriptor("SelectionOverlay").orElseThrow();
        expect(selectionOverlay.category().equals("Editor")
                        && descriptor(selectionOverlay.attributes(), "editMode").category().equals("Behavior")
                        && descriptor(selectionOverlay.attributes(), "handleSize").category().equals("Layout")
                        && descriptor(selectionOverlay.attributes(), "selectionColor").category().equals("Appearance"),
                "SelectionOverlay descriptor should expose editor canvas handle metadata");
        SelectionOverlay xmlSelectionOverlay = XMLWidget.create("""
                <SelectionOverlay id="overlay" editMode="false" resizeHandlesVisible="false"
                                  moveHandleVisible="false" handleSize="8" outlineThickness="2"
                                  minResizeWidth="12" minResizeHeight="6" />
                """, SelectionOverlay.class);
        expect(xmlSelectionOverlay.id().equals("overlay")
                        && !xmlSelectionOverlay.editMode()
                        && !xmlSelectionOverlay.resizeHandlesVisible()
                        && !xmlSelectionOverlay.moveHandleVisible()
                        && near(xmlSelectionOverlay.handleSize(), 8.0f)
                        && near(xmlSelectionOverlay.outlineThickness(), 2.0f)
                        && near(xmlSelectionOverlay.minResizeWidth(), 12.0f)
                        && near(xmlSelectionOverlay.minResizeHeight(), 6.0f),
                "SelectionOverlay should materialize editor canvas options from XML");
        XmlWidgetDescriptor designCanvasOverlay = registry.descriptor("DesignCanvasOverlay").orElseThrow();
        DesignCanvasOverlay xmlDesignCanvasOverlay = XMLWidget.create(
                "<DesignCanvasOverlay id=\"designOverlay\" handleSize=\"9\" />",
                DesignCanvasOverlay.class);
        expect(designCanvasOverlay.category().equals("Editor")
                        && descriptor(designCanvasOverlay.attributes(), "editMode").category().equals("Behavior")
                        && xmlDesignCanvasOverlay.id().equals("designOverlay")
                        && near(xmlDesignCanvasOverlay.handleSize(), 9.0f),
                "DesignCanvasOverlay should be an XML-visible editor overlay alias");
        XmlWidgetDescriptor xmlDesignCanvas = registry.descriptor("XmlDesignCanvas").orElseThrow();
        XmlDesignCanvas materializedDesignCanvas = XMLWidget.create(
                "<XmlDesignCanvas id=\"designCanvas\" gridVisible=\"false\" editMode=\"false\" />",
                XmlDesignCanvas.class);
        expect(xmlDesignCanvas.category().equals("Editor")
                        && descriptor(xmlDesignCanvas.attributes(), "gridVisible").category().equals("Behavior")
                        && descriptor(xmlDesignCanvas.attributes(), "editMode").category().equals("Behavior")
                        && materializedDesignCanvas.id().equals("designCanvas")
                        && !materializedDesignCanvas.gridVisible()
                        && !materializedDesignCanvas.editMode(),
                "XmlDesignCanvas should expose XML-visible design host metadata");
        XmlWidgetDescriptor xmlRuntimeViewPane = registry.descriptor("XmlRuntimeViewPane").orElseThrow();
        XmlRuntimeViewPane materializedRuntimeViewPane = XMLWidget.create(
                "<XmlRuntimeViewPane id=\"runtimePane\" />",
                XmlRuntimeViewPane.class);
        expect(xmlRuntimeViewPane.category().equals("Editor")
                        && materializedRuntimeViewPane.id().equals("runtimePane")
                        && !materializedRuntimeViewPane.running(),
                "XmlRuntimeViewPane should expose an XML-visible play-mode host descriptor");
        XmlWidgetDescriptor gridOverlay = registry.descriptor("GridOverlay").orElseThrow();
        expect(gridOverlay.category().equals("Editor")
                        && descriptor(gridOverlay.attributes(), "spacing").category().equals("Layout")
                        && descriptor(gridOverlay.attributes(), "spacing").valueType() == XmlAttributeValueType.NUMBER
                        && descriptor(gridOverlay.attributes(), "lineColor").category().equals("Appearance")
                        && descriptor(gridOverlay.attributes(), "lineColor").valueType() == XmlAttributeValueType.COLOR
                        && descriptor(gridOverlay.attributes(), "snapEnabled").category().equals("Behavior"),
                "GridOverlay descriptor should expose editor canvas grid and snap metadata");
        XmlWidgetDescriptor projectPicker = registry.descriptor("ProjectPickerPanel").orElseThrow();
        expect(projectPicker.category().equals("Editor")
                        && descriptor(projectPicker.attributes(), "title").category().equals("Content")
                        && descriptor(projectPicker.attributes(), "dirty").category().equals("State")
                        && descriptor(projectPicker.attributes(), "maxRecentProjects").category().equals("Behavior"),
                "ProjectPickerPanel descriptor should expose editor project picker metadata");
        ProjectPickerPanel xmlProjectPicker = XMLWidget.create("""
                <ProjectPickerPanel id="projectPicker"
                                    title="Projects"
                                    currentProjectName="Shop UI"
                                    currentPath="E:/projects/shop.xml"
                                    dirty="true"
                                    maxRecentProjects="3" />
                """, ProjectPickerPanel.class);
        expect(xmlProjectPicker.id().equals("projectPicker")
                        && xmlProjectPicker.title().equals("Projects")
                        && xmlProjectPicker.dirty()
                        && xmlProjectPicker.maxRecentProjects() == 3
                        && xmlProjectPicker.currentProjectLabel().text().contains("Shop UI *")
                        && xmlProjectPicker.currentProjectLabel().text().contains("E:/projects/shop.xml"),
                "ProjectPickerPanel should materialize project labels and picker options from XML");
        XmlWidgetDescriptor statusBar = registry.descriptor("StatusBar").orElseThrow();
        expect(statusBar.category().equals("Editor")
                        && descriptor(statusBar.attributes(), "dirty").category().equals("State")
                        && descriptor(statusBar.attributes(), "mode").category().equals("State")
                        && descriptor(statusBar.attributes(), "viewScale").category().equals("State"),
                "StatusBar descriptor should expose editor footer state metadata");
        StatusBar xmlStatusBar = XMLWidget.create("""
                <StatusBar id="footer" dirty="true" mode="Preview"
                           selectedNodePath="/0/1" viewScale="1.5" errorCount="2" warningCount="1" />
                """, StatusBar.class);
        expect(xmlStatusBar.id().equals("footer")
                        && xmlStatusBar.dirtyLabel().text().equals("Unsaved *")
                        && xmlStatusBar.modeLabel().text().equals("Mode: Preview")
                        && xmlStatusBar.diagnosticsLabel().text().equals("Errors: 2, Warnings: 1")
                        && xmlStatusBar.selectedPathLabel().text().contains("/0/1")
                        && xmlStatusBar.scaleLabel().text().equals("Scale: 150%"),
                "StatusBar should materialize compact editor footer state from XML");
        XmlWidgetDescriptor diagnosticsStrip = registry.descriptor("DiagnosticsStrip").orElseThrow();
        DiagnosticsStrip xmlDiagnosticsStrip = XMLWidget.create(
                "<DiagnosticsStrip id=\"diagnosticsStrip\" errorCount=\"1\" />",
                DiagnosticsStrip.class);
        expect(diagnosticsStrip.category().equals("Editor")
                        && descriptor(diagnosticsStrip.attributes(), "errorCount").category().equals("State")
                        && xmlDiagnosticsStrip.id().equals("diagnosticsStrip")
                        && xmlDiagnosticsStrip.mode().equals("Diagnostics")
                        && xmlDiagnosticsStrip.diagnosticsLabel().text().equals("Errors: 1, Warnings: 0"),
                "DiagnosticsStrip should be an XML-visible status footer alias");
        XmlWidgetDescriptor widgetPalette = registry.descriptor("WidgetPalette").orElseThrow();
        expect(widgetPalette.category().equals("Editor")
                        && descriptor(widgetPalette.attributes(), "search").category().equals("Behavior")
                        && descriptor(widgetPalette.attributes(), "category").category().equals("Behavior")
                        && descriptor(widgetPalette.attributes(), "includeInternalWidgets").category().equals("Behavior")
                        && descriptor(widgetPalette.attributes(), "selectedWidget").category().equals("State"),
                "WidgetPalette descriptor should expose editor palette filter and selection metadata");
        WidgetPalette xmlWidgetPalette = XMLWidget.create("""
                <WidgetPalette id="widgetPalette" title="Widgets" category="Controls"
                               search="button" selectedWidget="Button" />
                """, WidgetPalette.class);
        expect(xmlWidgetPalette.id().equals("widgetPalette")
                        && xmlWidgetPalette.title().equals("Widgets")
                        && xmlWidgetPalette.selectedCategory().equals("Controls")
                        && xmlWidgetPalette.search().equals("button")
                        && xmlWidgetPalette.selectedXmlName().equals("Button")
                        && !xmlWidgetPalette.includeInternalWidgets()
                        && xmlWidgetPalette.visibleItems().stream().anyMatch(item -> item.xmlName().equals("Button"))
                        && xmlWidgetPalette.visibleItems().stream().noneMatch(item -> item.category().equals("Editor"))
                        && xmlWidgetPalette.insertButton().enabled(),
                "WidgetPalette should materialize search, category and selected descriptor state from XML");
        WidgetPalette xmlInternalPalette = XMLWidget.create("""
                <WidgetPalette id="internalPalette" includeInternalWidgets="true"
                               category="Editor" search="palette" selectedWidget="WidgetPalette" />
                """, WidgetPalette.class);
        expect(xmlInternalPalette.id().equals("internalPalette")
                        && xmlInternalPalette.includeInternalWidgets()
                        && xmlInternalPalette.selectedCategory().equals("Editor")
                        && xmlInternalPalette.selectedXmlName().equals("WidgetPalette")
                        && xmlInternalPalette.visibleItems().stream().anyMatch(item -> item.xmlName().equals("WidgetPalette"))
                        && xmlInternalPalette.insertButton().enabled(),
                "WidgetPalette should allow explicit XML opt-in for editor/internal widget descriptors");
        XmlWidgetDescriptor palettePanel = registry.descriptor("PalettePanel").orElseThrow();
        PalettePanel xmlPalettePanel = XMLWidget.create(
                "<PalettePanel id=\"palettePanel\" search=\"label\" />",
                PalettePanel.class);
        expect(palettePanel.category().equals("Editor")
                        && descriptor(palettePanel.attributes(), "search").category().equals("Behavior")
                        && xmlPalettePanel.id().equals("palettePanel")
                        && xmlPalettePanel.title().equals("Palette")
                        && xmlPalettePanel.search().equals("label"),
                "PalettePanel should be an XML-visible widget palette alias");
        XmlWidgetDescriptor commandPalette = registry.descriptor("CommandPalette").orElseThrow();
        expect(commandPalette.category().equals("Editor")
                        && descriptor(commandPalette.attributes(), "search").category().equals("Behavior")
                        && descriptor(commandPalette.attributes(), "selectedCommand").category().equals("State"),
                "CommandPalette descriptor should expose editor command search and selection metadata");
        CommandPalette xmlCommandPalette = XMLWidget.create("""
                <CommandPalette id="commandPalette" title="Commands" search="open" selectedCommand="file.open" />
                """, CommandPalette.class);
        expect(xmlCommandPalette.id().equals("commandPalette")
                        && xmlCommandPalette.title().equals("Commands")
                        && xmlCommandPalette.search().equals("open")
                        && xmlCommandPalette.selectedCommandId().equals("file.open"),
                "CommandPalette should materialize command palette shell state from XML");
        XmlWidgetDescriptor assetBrowser = registry.descriptor("AssetBrowserPanel").orElseThrow();
        expect(assetBrowser.category().equals("Editor")
                        && descriptor(assetBrowser.attributes(), "kind").category().equals("Behavior")
                        && descriptor(assetBrowser.attributes(), "search").category().equals("Behavior")
                        && descriptor(assetBrowser.attributes(), "selectedAsset").category().equals("State")
                        && descriptor(assetBrowser.attributes(), "targetAttribute").category().equals("Behavior"),
                "AssetBrowserPanel descriptor should expose editor asset browser metadata");
        AssetBrowserPanel xmlAssetBrowser = XMLWidget.create("""
                <AssetBrowserPanel id="assetBrowser" title="Assets" kind="SHADER"
                                   search="glow" selectedAsset="unigui:glow"
                                   targetAttribute="backgroundTexture" maxVisibleAssets="4" />
                """, AssetBrowserPanel.class);
        expect(xmlAssetBrowser.id().equals("assetBrowser")
                        && xmlAssetBrowser.title().equals("Assets")
                        && xmlAssetBrowser.kind() == XmlWidgetAssetKind.SHADER
                        && xmlAssetBrowser.search().equals("glow")
                        && xmlAssetBrowser.selectedAssetId().equals("unigui:glow")
                        && xmlAssetBrowser.targetAttribute().equals("backgroundTexture")
                        && xmlAssetBrowser.maxVisibleAssets() == 4
                        && xmlAssetBrowser.previewLabel().text().contains("unigui:glow"),
                "AssetBrowserPanel should materialize editor asset browser shell state from XML");
        XmlWidgetDescriptor filterChips = registry.descriptor("SearchBoxWithFilterChips").orElseThrow();
        expect(filterChips.category().equals("Editor")
                        && descriptor(filterChips.attributes(), "search").category().equals("Behavior")
                        && descriptor(filterChips.attributes(), "filters").category().equals("Content")
                        && descriptor(filterChips.attributes(), "activeFilters").category().equals("State"),
                "SearchBoxWithFilterChips descriptor should expose reusable editor filter metadata");
        SearchBoxWithFilterChips xmlFilterChips = XMLWidget.create("""
                <SearchBoxWithFilterChips id="searchFilters" search="button"
                                          filters="errors:Errors|warnings:Warnings|assets:Assets"
                                          activeFilters="errors,assets" />
                """, SearchBoxWithFilterChips.class);
        expect(xmlFilterChips.id().equals("searchFilters")
                        && xmlFilterChips.search().equals("button")
                        && xmlFilterChips.filters().size() == 3
                        && xmlFilterChips.activeFilters().size() == 2
                        && xmlFilterChips.filterActive("errors")
                        && xmlFilterChips.filterActive("assets")
                        && xmlFilterChips.chipRow().children().size() == 3,
                "SearchBoxWithFilterChips should materialize search text and filter chips from XML");
        XmlWidgetDescriptor dialog = registry.descriptor("Dialog").orElseThrow();
        expect(dialog.category().equals("Editor")
                        && descriptor(dialog.attributes(), "message").category().equals("Content")
                        && descriptor(dialog.attributes(), "buttons").category().equals("Content")
                        && descriptor(dialog.attributes(), "defaultResult").category().equals("Behavior")
                        && descriptor(dialog.attributes(), "cancelResult").category().equals("Behavior")
                        && descriptor(dialog.attributes(), "windowX").category().equals("Layout")
                        && propertyChild(dialog.propertyChildren(), "Content").singleChild(),
                "Dialog descriptor should expose editor dialog message, result button and content metadata");
        Dialog xmlDialog = XMLWidget.create("""
                <Dialog id="saveDialog" title="Save Changes" message="Save before closing?"
                        open="true" buttons="save:Save|discard:Discard|cancel:Cancel"
                        defaultResult="save" cancelResult="cancel"
                        closeOnResult="false" windowX="30" windowY="42">
                    <Dialog.Content>
                        <Label id="dialogBody" text="Project settings changed" />
                    </Dialog.Content>
                </Dialog>
                """, Dialog.class);
        expect(xmlDialog.id().equals("saveDialog")
                        && xmlDialog.title().equals("Save Changes")
                        && xmlDialog.message().equals("Save before closing?")
                        && xmlDialog.opened()
                        && xmlDialog.modal()
                        && !xmlDialog.resizable()
                        && xmlDialog.buttons().size() == 3
                        && xmlDialog.defaultResult().equals("save")
                        && xmlDialog.cancelResult().equals("cancel")
                        && !xmlDialog.closeOnResult()
                        && near(xmlDialog.windowX(), 30.0f)
                        && near(xmlDialog.windowY(), 42.0f)
                        && xmlDialog.content() instanceof Label dialogBody
                        && dialogBody.text().equals("Project settings changed")
                        && XMLWidget.getWidget(xmlDialog, "dialogBody", Label.class) == dialogBody,
                "Dialog should materialize modal shell state, result buttons and content from XML");
        XmlWidgetDescriptor paneHeader = registry.descriptor("PaneHeader").orElseThrow();
        expect(paneHeader.category().equals("Editor")
                        && descriptor(paneHeader.attributes(), "title").category().equals("Content")
                        && descriptor(paneHeader.attributes(), "dirty").category().equals("State")
                        && descriptor(paneHeader.attributes(), "pinned").category().equals("State")
                        && descriptor(paneHeader.attributes(), "closable").category().equals("Behavior"),
                "PaneHeader descriptor should expose editor pane title, dirty, pin and close metadata");
        PaneHeader xmlPaneHeader = XMLWidget.create("""
                <PaneHeader id="propertiesHeader" paneId="properties" title="Properties"
                            dirty="true" pinned="false" closable="false"
                            pinVisible="true" menuVisible="false" closeVisible="true"
                            headerHeight="24" />
                """, PaneHeader.class);
        expect(xmlPaneHeader.id().equals("propertiesHeader")
                        && xmlPaneHeader.paneId().equals("properties")
                        && xmlPaneHeader.title().equals("Properties")
                        && xmlPaneHeader.dirty()
                        && !xmlPaneHeader.pinned()
                        && !xmlPaneHeader.closable()
                        && xmlPaneHeader.pinButton().visible()
                        && !xmlPaneHeader.menuButton().visible()
                        && !xmlPaneHeader.closeButton().visible()
                        && near(xmlPaneHeader.headerHeight(), 24.0f),
                "PaneHeader should materialize pane state and action visibility from XML");
        XmlWidgetDescriptor resizableHeader = registry.descriptor("ResizablePanelHeader").orElseThrow();
        expect(resizableHeader.category().equals("Editor")
                        && descriptor(resizableHeader.attributes(), "resizeEdge").category().equals("Behavior")
                        && descriptor(resizableHeader.attributes(), "panelSize").category().equals("State")
                        && descriptor(resizableHeader.attributes(), "minPanelSize").category().equals("Layout")
                        && descriptor(resizableHeader.attributes(), "maxPanelSize").category().equals("Layout"),
                "ResizablePanelHeader descriptor should expose resize edge and panel size metadata");
        ResizablePanelHeader xmlResizableHeader = XMLWidget.create("""
                <ResizablePanelHeader id="assetsHeader" paneId="assets" title="Assets"
                                      resizeEdge="left" panelSize="180"
                                      minPanelSize="120" maxPanelSize="260"
                                      resizeHandleVisible="true" resizingEnabled="false" />
                """, ResizablePanelHeader.class);
        expect(xmlResizableHeader.id().equals("assetsHeader")
                        && xmlResizableHeader.paneId().equals("assets")
                        && xmlResizableHeader.title().equals("Assets")
                        && xmlResizableHeader.resizeEdge() == ResizablePanelHeader.ResizeEdge.LEFT
                        && near(xmlResizableHeader.panelSize(), 180.0f)
                        && near(xmlResizableHeader.minPanelSize(), 120.0f)
                        && near(xmlResizableHeader.maxPanelSize(), 260.0f)
                        && xmlResizableHeader.resizeButton().visible()
                        && !xmlResizableHeader.resizingEnabled()
                        && !xmlResizableHeader.resizeButton().enabled(),
                "ResizablePanelHeader should materialize resize affordance state from XML");
        XmlWidgetDescriptor dragSource = registry.descriptor("DragSource").orElseThrow();
        expect(dragSource.category().equals("Editor")
                        && dragSource.acceptsChildren()
                        && descriptor(dragSource.attributes(), "payloadId").category().equals("State")
                        && descriptor(dragSource.attributes(), "payloadType").category().equals("Behavior")
                        && descriptor(dragSource.attributes(), "dragPreview").category().equals("Content")
                        && descriptor(dragSource.attributes(), "dragThreshold").category().equals("Behavior"),
                "DragSource descriptor should expose payload metadata and drag behavior attributes");
        DragSource xmlDragSource = XMLWidget.create("""
                <DragSource id="paletteButtonDrag" payloadId="Button" payloadType="widget"
                            dragPreview="&lt;Button /&gt;" dragThreshold="2">
                    <Label id="dragSourceLabel" text="Button" />
                </DragSource>
                """, DragSource.class);
        expect(xmlDragSource.id().equals("paletteButtonDrag")
                        && xmlDragSource.payload().id().equals("Button")
                        && xmlDragSource.payload().type().equals("widget")
                        && xmlDragSource.payload().preview().equals("<Button />")
                        && near(xmlDragSource.dragThreshold(), 2.0f)
                        && XMLWidget.getWidget(xmlDragSource, "dragSourceLabel", Label.class).text().equals("Button"),
                "DragSource should materialize payload metadata and child preview content from XML");
        XmlWidgetDescriptor dropTarget = registry.descriptor("DropTarget").orElseThrow();
        expect(dropTarget.category().equals("Editor")
                        && dropTarget.acceptsChildren()
                        && descriptor(dropTarget.attributes(), "acceptedPayloadTypes").category().equals("Behavior")
                        && descriptor(dropTarget.attributes(), "dropEnabled").category().equals("Behavior"),
                "DropTarget descriptor should expose payload validation metadata");
        DropTarget xmlDropTarget = XMLWidget.create("""
                <DropTarget id="canvasDrop" acceptedPayloadTypes="widget,node" dropEnabled="true">
                    <Label id="dropTargetLabel" text="Canvas" />
                </DropTarget>
                """, DropTarget.class);
        expect(xmlDropTarget.id().equals("canvasDrop")
                        && xmlDropTarget.acceptedPayloadTypes().equals("widget,node")
                        && xmlDropTarget.accepts(xmlDragSource.payload())
                        && !xmlDropTarget.accepts(dev.sixik.unigui.widgets.editor.DragPayload.of("Texture", "asset", "Texture"))
                        && XMLWidget.getWidget(xmlDropTarget, "dropTargetLabel", Label.class).text().equals("Canvas"),
                "DropTarget should materialize accepted payload types and child drop-zone content from XML");
        XmlWidgetDescriptor itemPreview = registry.descriptor("MinecraftItemPreviewWidget").orElseThrow();
        expect(itemPreview.category().equals("Minecraft")
                        && descriptor(itemPreview.attributes(), "label").category().equals("Content")
                        && descriptor(itemPreview.attributes(), "item").category().equals("Minecraft")
                        && descriptor(itemPreview.attributes(), "item").valueType() == XmlAttributeValueType.RESOURCE_ID
                        && descriptor(itemPreview.attributes(), "decorations").category().equals("Appearance"),
                "MinecraftItemPreviewWidget descriptor should expose item id and preview metadata");
        XmlWidgetDescriptor blockPreview = registry.descriptor("MinecraftBlockPreviewWidget").orElseThrow();
        expect(blockPreview.category().equals("Minecraft")
                        && descriptor(blockPreview.attributes(), "block").category().equals("Minecraft")
                        && descriptor(blockPreview.attributes(), "block").valueType() == XmlAttributeValueType.RESOURCE_ID
                        && descriptor(blockPreview.attributes(), "previewSize").category().equals("Layout"),
                "MinecraftBlockPreviewWidget descriptor should expose block id and inherited preview metadata");
        XmlWidgetDescriptor entityPreview = registry.descriptor("MinecraftEntityPreviewWidget").orElseThrow();
        expect(entityPreview.category().equals("Minecraft")
                        && descriptor(entityPreview.attributes(), "entityType").category().equals("Minecraft")
                        && descriptor(entityPreview.attributes(), "entityType").valueType() == XmlAttributeValueType.RESOURCE_ID
                        && descriptor(entityPreview.attributes(), "labelVisible").category().equals("Appearance"),
                "MinecraftEntityPreviewWidget descriptor should expose entity id and preview metadata");
        XmlWidgetDescriptor itemPicker = registry.descriptor("MinecraftItemPickerWidget").orElseThrow();
        expect(itemPicker.category().equals("Minecraft")
                        && descriptor(itemPicker.attributes(), "items").category().equals("Minecraft")
                        && descriptor(itemPicker.attributes(), "items").valueType() == XmlAttributeValueType.RESOURCE_ID
                        && descriptor(itemPicker.attributes(), "query").category().equals("Behavior")
                        && descriptor(itemPicker.attributes(), "selectedIndex").category().equals("Behavior"),
                "MinecraftItemPickerWidget descriptor should expose deterministic item list metadata");
        XmlWidgetDescriptor texturePicker = registry.descriptor("MinecraftTexturePickerWidget").orElseThrow();
        expect(texturePicker.category().equals("Minecraft")
                        && descriptor(texturePicker.attributes(), "textures").category().equals("Minecraft")
                        && descriptor(texturePicker.attributes(), "textures").valueType() == XmlAttributeValueType.RESOURCE_ID
                        && descriptor(texturePicker.attributes(), "selectedIndex").category().equals("Behavior"),
                "MinecraftTexturePickerWidget descriptor should expose deterministic texture list metadata");
        MinecraftTexturePickerWidget xmlTexturePicker = XMLWidget.create("""
                <MinecraftTexturePickerWidget id="texturePicker"
                                              textures="minecraft:textures/block/stone.png|minecraft:textures/item/diamond.png"
                                              selectedIndex="1" />
                """, MinecraftTexturePickerWidget.class);
        expect(xmlTexturePicker.id().equals("texturePicker")
                        && xmlTexturePicker.textureCount() == 2
                        && xmlTexturePicker.selectedId() != null
                        && xmlTexturePicker.selectedId().toString().equals("minecraft:textures/item/diamond.png"),
                "MinecraftTexturePickerWidget should materialize XML texture ids and selection");
        expect(registry.descriptors().stream().anyMatch(descriptor -> descriptor.xmlName().equals("ImageView")),
                "Registry should expose read-only descriptor snapshots for editor palettes");
        expectFailsUnsupported(() -> box.attributes().add(XmlAttributeDescriptor.of("oops")),
                "Widget descriptor attribute lists should be immutable snapshots");

        XmlWidgetRegistry custom = XMLWidget.emptyRegistry();
        custom.register("Badge", Badge::new)
                .describe("Status Badge", "Custom", "Small status label for tests.")
                .attribute("text", XmlValueParsers.STRING, Badge::text,
                        XmlAttributeDescriptor.of("text")
                                .category("Content")
                                .defaultValue("")
                                .description("Badge label."))
                .attribute("importance", XmlValueParsers.FLOAT, Badge::importance,
                        XmlAttributeDescriptor.of("importance")
                                .displayName("Importance")
                                .category("Behavior")
                                .defaultValue("0")
                                .description("Sort priority for editor tests."));

        XmlWidgetDescriptor badge = custom.descriptor("Badge").orElseThrow();
        expect(badge.displayName().equals("Status Badge") && badge.category().equals("Custom"),
                "Custom widget descriptors should preserve explicit widget metadata");
        XmlAttributeDescriptor importance = descriptor(badge.attributes(), "importance");
        expect(importance.defaultValue().equals("0") && importance.description().contains("Sort priority"),
                "Custom attribute descriptors should preserve defaults and descriptions");
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
                <XmlCodeEditor id="source" text="&lt;VBox /&gt;" lineNumbers="false" visibleLines="4" />
                """, XmlCodeEditor.class);
        expect(fromXml.id().equals("source")
                        && fromXml.text().equals("<VBox />")
                        && !fromXml.lineNumbersVisible()
                        && fromXml.visibleLines() == 4,
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

        UnsupportedXmlWidget unsupported = new UnsupportedXmlWidget();
        unsupported.id("custom");
        XmlWidgetDocumentResult unsupportedSnapshot = XmlWidgetRuntimeSerializer.snapshot(unsupported);
        expect(unsupportedSnapshot.hasDiagnostics()
                        && unsupportedSnapshot.firstDiagnostic().orElseThrow().message().contains("does not have a complete XML snapshot exporter")
                        && unsupportedSnapshot.document().root().attribute("id").orElseThrow().equals("custom"),
                "Runtime serializer should preserve unsupported widget ids while reporting export diagnostics");
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

    private void testGridOverlayWidgetContracts() {
        GridOverlay grid = XMLWidget.create("""
                <GridOverlay id="grid"
                             spacing="8"
                             majorEvery="4"
                             offsetX="2"
                             offsetY="1"
                             lineThickness="0.5"
                             majorLineThickness="2"
                             lineColor="#40608055"
                             majorLineColor="#80C8FFFF"
                             snapEnabled="true"
                             snapSize="8" />
                """, GridOverlay.class);

        expect(grid.id().equals("grid")
                        && near(grid.spacing(), 8.0f)
                        && grid.majorEvery() == 4
                        && near(grid.offsetX(), 2.0f)
                        && near(grid.offsetY(), 1.0f)
                        && near(grid.lineThickness(), 0.5f)
                        && near(grid.majorLineThickness(), 2.0f)
                        && grid.snapEnabled()
                        && near(grid.snapSize(), 8.0f)
                        && near(grid.lineColor().r(), 0x40 / 255.0f)
                        && near(grid.majorLineColor().a(), 1.0f),
                "GridOverlay should materialize grid and snap options from XML");
        expect(near(grid.snapX(13.0f), 10.0f)
                        && near(grid.snapY(14.0f), 17.0f)
                        && near(grid.snapPoint(13.0f, 14.0f).x(), 10.0f)
                        && near(grid.snapPoint(13.0f, 14.0f).y(), 17.0f),
                "GridOverlay snap helpers should quantize coordinates against axis offsets");

        grid.arrange(new MutableRect(4.0f, 6.0f, 34.0f, 18.0f));
        NoopRenderContext renderContext = new NoopRenderContext();
        grid.render(renderContext);
        long lineCommands = renderContext.drawList().commands().stream()
                .filter(command -> command.type() == DrawCommandType.LINE)
                .count();
        expect(lineCommands >= 8
                        && renderContext.drawList().commands().stream()
                        .anyMatch(command -> command.paint() != null && near(command.paint().strokeWidth(), 2.0f)),
                "GridOverlay should render minor and major grid line commands inside its layout bounds");

        grid.gridVisible(false);
        NoopRenderContext hiddenContext = new NoopRenderContext();
        grid.render(hiddenContext);
        expect(hiddenContext.drawList().size() == 0, "GridOverlay should skip rendering when gridVisible is false");

        grid.snapEnabled(false);
        expect(near(grid.snapX(13.0f), 13.0f), "GridOverlay should leave coordinates unchanged when snapping is disabled");
    }

    private void testXmlDesignCanvasWidgetContracts() {
        XmlEditorSession session = XmlEditorSession.create("""
                <VBox id="root" width="220" height="140">
                    <Box id="panel" x="10" y="20" width="80" height="30" />
                </VBox>
                """);
        XmlDesignCanvas canvas = new XmlDesignCanvas().session(session);

        expect(canvas.previewRoot().isPresent()
                        && canvas.previewRoot().orElseThrow().id().equals("root")
                        && canvas.previewWidget(XmlWidgetNodePath.of(1)).orElseThrow().id().equals("panel")
                        && canvas.previewHost().children().size() == 1
                        && canvas.overlay().document() == session.document(),
                "XmlDesignCanvas should rebuild a live preview, map XML paths to runtime widgets and bind overlay to the session document");
        XmlEditorSession actionSession = XmlEditorSession.create("""
                <VBox id="actionRoot" width="180" height="96">
                    <Button id="actionButton" text="Run" onClick="demo.design" />
                </VBox>
                """).commands(XmlCommandRegistry.empty()
                .register("demo.design", (source, event) -> {
                }));
        XmlDesignCanvas actionCanvas = new XmlDesignCanvas().session(actionSession);
        expect(actionCanvas.previewRoot().isPresent()
                        && actionCanvas.previewError().isEmpty()
                        && actionCanvas.previewWidget(XmlWidgetNodePath.of(1)).orElseThrow().id().equals("actionButton"),
                "XmlDesignCanvas should use session commands when building Design preview for onClick XML");
        XmlEditorSession offsetSession = XmlEditorSession.create(
                "<VBox id=\"offsetRoot\" x=\"40\" y=\"32\" width=\"220\" height=\"140\"><Box id=\"offsetPanel\" x=\"10\" y=\"20\" width=\"80\" height=\"30\" /></VBox>");
        XmlDesignCanvas offsetCanvas = new XmlDesignCanvas().session(offsetSession);
        offsetCanvas.measure(new LayoutContext(320.0f, 180.0f));
        offsetCanvas.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));
        offsetCanvas.overlay().selectedPath(XmlWidgetNodePath.of(0));
        Widget offsetRoot = offsetCanvas.previewWidget(XmlWidgetNodePath.root()).orElseThrow();
        Widget offsetPanel = offsetCanvas.previewWidget(XmlWidgetNodePath.of(0)).orElseThrow();
        XmlWidgetLayoutFrame offsetFrame = offsetCanvas.overlay().selectedFrame().orElseThrow();
        float expectedPanelX = offsetPanel.layoutBounds().x() - offsetCanvas.overlay().layoutBounds().x()
                + offsetRoot.transform().position().x()
                + offsetPanel.transform().position().x();
        float expectedPanelY = offsetPanel.layoutBounds().y() - offsetCanvas.overlay().layoutBounds().y()
                + offsetRoot.transform().position().y()
                + offsetPanel.transform().position().y();
        expect(near(offsetFrame.x(), expectedPanelX)
                        && near(offsetFrame.y(), expectedPanelY)
                        && near(offsetFrame.width(), offsetPanel.layoutBounds().width())
                        && near(offsetFrame.height(), offsetPanel.layoutBounds().height())
                        && offsetCanvas.overlay().pathAt(offsetFrame.x() + 1.0f, offsetFrame.y() + 1.0f)
                        .filter(XmlWidgetNodePath.of(0)::equals)
                        .isPresent(),
                "XmlDesignCanvas overlay frames should follow rendered preview bounds, including parent and child transforms");
        XmlEditorSession scaledSession = XmlEditorSession.create(
                "<VBox id=\"scaledRoot\" scale=\"2\" width=\"220\" height=\"140\"><Box id=\"scaledPanel\" x=\"10\" y=\"20\" width=\"80\" height=\"30\" /></VBox>");
        XmlDesignCanvas scaledCanvas = new XmlDesignCanvas().session(scaledSession);
        scaledCanvas.measure(new LayoutContext(320.0f, 180.0f));
        scaledCanvas.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));
        scaledCanvas.overlay().selectedPath(XmlWidgetNodePath.of(0));
        XmlWidgetLayoutFrame scaledFrame = scaledCanvas.overlay().selectedFrame().orElseThrow();
        expect(near(scaledFrame.x(), 20.0f)
                        && near(scaledFrame.y(), 40.0f)
                        && near(scaledFrame.width(), 160.0f)
                        && near(scaledFrame.height(), 60.0f)
                        && scaledCanvas.overlay().pathAt(21.0f, 41.0f)
                        .filter(XmlWidgetNodePath.of(0)::equals)
                        .isPresent(),
                "XmlDesignCanvas overlay frames should apply child transforms before parent transforms");
        expect(canvas.selectAt(12.0f, 22.0f).orElseThrow().equals(XmlWidgetNodePath.of(1))
                        && session.selectedPath().orElseThrow().equals(XmlWidgetNodePath.of(1)),
                "XmlDesignCanvas should map hit-tested frames back to XmlWidgetNodePath selection");

        XmlWidgetDocumentResult moved = XmlWidgetLayoutHandles.move(
                session.document(),
                XmlWidgetNodePath.of(1),
                5.0f,
                -4.0f);
        expect(canvas.applyDesignResult(moved, XmlWidgetNodePath.of(1)),
                "XmlDesignCanvas should accept valid design-handle document results");
        XmlWidgetElement movedPanel = session.document().root().elementChildren().get(0);
        expect(movedPanel.attribute("x").orElseThrow().equals("15")
                        && movedPanel.attribute("y").orElseThrow().equals("16")
                        && canvas.previewRoot().orElseThrow().id().equals("root")
                        && canvas.lastDesignResult().orElseThrow().valid(),
                "XmlDesignCanvas should push move/resize results back through the editor session and preview");

        WidgetPalette palette = new WidgetPalette();
        java.util.List<WidgetPalette.PaletteInsertRequest> paletteRequests = new java.util.ArrayList<>();
        EventSubscription paletteSubscription = palette.onInsertRequested(paletteRequests::add);
        expect(palette.requestInsert("Label", XmlWidgetNodePath.root(), Integer.MAX_VALUE),
                "WidgetPalette should be able to create a palette insert request for canvas drop");
        DropTarget.DropResult dropResult = canvas.dropPaletteRequest(paletteRequests.get(0), 1.0f, 1.0f);
        XmlWidgetNodePath insertedPath = session.selectedPath().orElseThrow();
        expect(dropResult == DropTarget.DropResult.ACCEPTED
                        && insertedPath.resolveElement(session.document()).orElseThrow().name().equals("Label")
                        && canvas.previewWidget(insertedPath).isPresent()
                        && session.text().contains("<Label"),
                "XmlDesignCanvas should accept palette drops on valid container targets and select the inserted XML node");
        closeUnchecked(paletteSubscription);

        Widget previewBefore = canvas.previewRoot().orElseThrow();
        session.replaceText("<VBox><Box></VBox>");
        expect(canvas.previewRoot().orElseThrow() == previewBefore
                        && session.document().root().attribute("id").orElseThrow().equals("root"),
                "XmlDesignCanvas should keep the previous valid preview when Code View text is syntactically invalid");

        session.mode(XmlEditorMode.RUNTIME);
        expect(!canvas.editMode(), "XmlDesignCanvas should disable editor overlay input while the session is in runtime mode");
        session.mode(XmlEditorMode.DESIGN);
        expect(canvas.editMode(), "XmlDesignCanvas should re-enable editor overlay input in design mode");
    }

    private void testXmlRuntimeViewPaneContracts() {
        XmlEditorSession session = XmlEditorSession.create("""
                <Screen uiScale="2.5" scaleWithMinecraftGui="false">
                    <VBox id="runtimeRoot" width="160" height="96">
                        <Button id="play" text="Play" />
                    </VBox>
                </Screen>
                """);
        XmlRuntimeViewPane pane = new XmlRuntimeViewPane().session(session);

        expect(!pane.running()
                        && pane.runtimeRoot().isEmpty()
                        && pane.runtimeHost().children().isEmpty(),
                "XmlRuntimeViewPane should stay idle until the editor enters runtime mode");
        expect(pane.start(), "XmlRuntimeViewPane should enter runtime mode from the current XML snapshot");
        expect(session.mode() == XmlEditorMode.RUNTIME
                        && pane.running()
                        && pane.runtimeRoot().orElseThrow().id().equals("runtimeRoot")
                        && pane.runtimeHost().children().size() == 1
                        && near(pane.scaleProvider().scale(), 2.5f)
                        && !pane.scaleWithMinecraftGui(),
                "XmlRuntimeViewPane should materialize a runtime root and preserve screen scale settings");

        Widget previousRuntimeRoot = pane.runtimeRoot().orElseThrow();
        expect(session.replaceText("<VBox id=\"changed\"><Label text=\"Live\" /></VBox>"),
                "Runtime session should still accept valid XML text changes");
        expect(pane.running()
                        && pane.runtimeRoot().orElseThrow() != previousRuntimeRoot
                        && pane.runtimeRoot().orElseThrow().id().equals("changed")
                        && pane.runtimeXml().contains("changed")
                        && session.diagnostics(XmlEditorDiagnosticChannel.RUNTIME).isEmpty(),
                "XmlRuntimeViewPane should rebuild the runtime tree from the latest valid XML snapshot");

        XmlEditorSession brokenSession = XmlEditorSession.create("<MissingRuntimeWidget id=\"bad\" />");
        XmlRuntimeViewPane brokenPane = new XmlRuntimeViewPane().session(brokenSession);
        expect(!brokenPane.start()
                        && brokenSession.mode() == XmlEditorMode.RUNTIME
                        && !brokenPane.runtimeError().isEmpty()
                        && brokenSession.diagnostics(XmlEditorDiagnosticChannel.RUNTIME).size() == 1,
                "XmlRuntimeViewPane should surface runtime materialization failures through the session diagnostics channel");
        expect(brokenSession.replaceText("<VBox id=\"recovered\"><Label text=\"Recovered\" /></VBox>")
                        && brokenPane.running()
                        && brokenPane.runtimeRoot().orElseThrow().id().equals("recovered")
                        && brokenSession.diagnostics(XmlEditorDiagnosticChannel.RUNTIME).isEmpty(),
                "XmlRuntimeViewPane should clear runtime diagnostics after a successful runtime rebuild");
        expect(brokenPane.stop()
                        && brokenSession.mode() == XmlEditorMode.DESIGN
                        && brokenSession.diagnostics(XmlEditorDiagnosticChannel.RUNTIME).isEmpty(),
                "XmlRuntimeViewPane should clear runtime diagnostics when leaving runtime mode");

        int[] runtimeClicks = {0};
        XmlEditorSession actionSession = XmlEditorSession.create("""
                <VBox id="actions">
                    <Button id="play" text="Play" onClick="demo.play" />
                </VBox>
                """).commands(XmlCommandRegistry.empty()
                .register("demo.play", (source, event) -> runtimeClicks[0]++));
        XmlRuntimeViewPane actionPane = new XmlRuntimeViewPane().session(actionSession);
        expect(actionPane.start(), "XmlRuntimeViewPane should start when XML action commands are registered on the session");
        XMLWidget.getWidget(actionPane.runtimeRoot().orElseThrow(), "play", Button.class).click();
        expect(runtimeClicks[0] == 1,
                "XmlRuntimeViewPane should pass the session XmlCommandRegistry into runtime XML onClick handlers");
        actionSession.commands(XmlCommandRegistry.empty()
                .register("demo.play", (source, event) -> runtimeClicks[0] += 10));
        XMLWidget.getWidget(actionPane.runtimeRoot().orElseThrow(), "play", Button.class).click();
        expect(runtimeClicks[0] == 11,
                "Changing session runtime commands should rebuild Runtime View with the new command handlers");
        actionPane.stop();

        String textBeforeStop = session.text();
        expect(pane.stop(), "XmlRuntimeViewPane should leave runtime mode on stop");
        expect(session.mode() == XmlEditorMode.DESIGN
                        && !pane.running()
                        && pane.runtimeRoot().isEmpty()
                        && pane.runtimeHost().children().isEmpty()
                        && session.text().equals(textBeforeStop),
                "Stopping Runtime View should dispose the runtime tree without writing runtime state back into XML");
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

    private void testXmlEditorDemoScreenWorkspaceContracts() {
        AutoCloseable assetProvider = XmlWidgetAssetProviders.register(builder -> builder.add(
                XmlWidgetAsset.texture("test_mod:shop_icon", 32, 32).displayName("Shop Icon")));
        XmlEditorDemoScreen screen;
        try {
            screen = new XmlEditorDemoScreen();
            expect(screen.assetCatalog().find(XmlWidgetAssetKind.TEXTURE, "test_mod:shop_icon").isPresent(),
                    "XML editor demo screen should include registered mod asset providers in its catalog");
        } finally {
            closeUnchecked(assetProvider);
        }
        screen.refreshAssetCatalog();
        expect(screen.assetCatalog().find(XmlWidgetAssetKind.TEXTURE, "test_mod:shop_icon").isEmpty(),
                "XML editor demo screen should refresh its catalog after provider unload");

        expect(screen.session().document().root().name().equals("VBox"),
                "XML editor demo screen should own a valid default editor session");
        expect(screen.workspace().manager().containsPane(XmlEditorDemoScreen.PANE_DESIGN)
                        && screen.workspace().manager().containsPane(XmlEditorDemoScreen.PANE_CODE)
                        && screen.workspace().manager().containsPane(XmlEditorDemoScreen.PANE_HIERARCHY)
                        && screen.workspace().manager().containsPane(XmlEditorDemoScreen.PANE_PROPERTIES),
                "XML editor demo screen should create the default design/code/hierarchy/properties workspace panes");
        expect(screen.paneVisibility().isRegistered(XmlEditorDemoScreen.PANE_DIAGNOSTICS)
                        && screen.paneVisibility().isRegistered(XmlEditorDemoScreen.PANE_PALETTE)
                        && screen.paneVisibility().isRegistered(XmlEditorDemoScreen.PANE_ASSETS)
                        && screen.paneVisibility().isRegistered(XmlEditorDemoScreen.PANE_BINDINGS)
                        && screen.paneVisibility().isRegistered(XmlEditorDemoScreen.PANE_CONSOLE),
                "XML editor demo screen should register secondary view-toggle panes");
        expect(!screen.paneVisibility().isVisible(XmlEditorDemoScreen.PANE_DIAGNOSTICS),
                "Diagnostics pane should start hidden in the compact workspace");
        expect(screen.assetBrowserPanel().catalog().assets().size() >= 5
                        && screen.assetBrowserPanel().visibleAssets().stream()
                        .anyMatch(asset -> asset.kind() == XmlWidgetAssetKind.TEXTURE)
                        && screen.propertiesPanel().assetCatalog().assets().size()
                        == screen.assetBrowserPanel().catalog().assets().size(),
                "XML editor demo screen should back the Assets pane and Properties picker with the same asset catalog");
        expect(screen.commands().command(ProjectPickerPanel.COMMAND_NEW_PROJECT).isPresent()
                        && screen.commands().command(ProjectPickerPanel.COMMAND_OPEN_PROJECT).isPresent()
                        && screen.commands().command(ProjectPickerPanel.COMMAND_SAVE_PROJECT).isPresent()
                        && screen.commands().command(XmlEditorDemoScreen.COMMAND_SAVE_PROJECT_AS).isPresent()
                        && screen.commands().command(ProjectPickerPanel.COMMAND_LAST_PROJECTS).isPresent(),
                "XML editor demo screen should expose project commands required by the editor shell");
        expect(screen.commands().command(screen.paneVisibility().viewCommandId(XmlEditorDemoScreen.PANE_CONSOLE)).isPresent(),
                "XML editor demo screen should expose View menu commands through PaneVisibilityController");

        String encodedLayout = screen.encodedLayout();
        expect(encodedLayout.startsWith("DLS1|"),
                "XML editor demo screen should persist dock layout through DockLayoutSnapshotCodec");
        screen.commands().execute(XmlEditorDemoScreen.COMMAND_LAYOUT_SAVE);
        expect(screen.commands().command(XmlEditorDemoScreen.COMMAND_LAYOUT_RESTORE).orElseThrow().enabled(),
                "Saving the dock layout should enable restore-layout command");
        expect(screen.restoreEncodedLayout(encodedLayout),
                "XML editor demo screen should restore an encoded dock layout snapshot");

        screen.commands().execute(XmlEditorDemoScreen.COMMAND_RUN);
        expect(screen.session().mode() == XmlEditorMode.RUNTIME,
                "Run command should switch the editor session into runtime mode");
        screen.commands().execute(XmlEditorDemoScreen.COMMAND_STOP);
        expect(screen.session().mode() == XmlEditorMode.DESIGN,
                "Stop command should return the editor session to design mode");

        screen.codeEditor().text("<VBox id=\"codeEdited\"><Label text=\"OK\" /></VBox>");
        expect(screen.session().mode() == XmlEditorMode.CODE
                        && screen.session().document().root().attribute("id").orElseThrow().equals("codeEdited")
                        && screen.session().diagnostics(XmlEditorDiagnosticChannel.PARSE).isEmpty(),
                "Valid Code View edits should switch to code mode and update the editor document");
        screen.commands().execute(XmlEditorDemoScreen.COMMAND_FORMAT_XML);
        expect(screen.codeEditor().text().contains("\n    <Label")
                        && screen.session().document().root().attribute("id").orElseThrow().equals("codeEdited"),
                "Format XML command should pretty-print the code view through XmlWidgetDocument serialization");

        screen.codeEditor().text("<VBox><Label></VBox>");
        expect(!screen.session().diagnostics(XmlEditorDiagnosticChannel.PARSE).isEmpty(),
                "Code-view parse failures should flow into session parse diagnostics");
        expect(screen.session().document().root().attribute("id").orElseThrow().equals("codeEdited"),
                "Code-view parse failures should preserve the last valid editor document");
        expect(screen.paneVisibility().isVisible(XmlEditorDemoScreen.PANE_DIAGNOSTICS),
                "Diagnostics pane should auto-open when parse diagnostics appear");
    }

    private void testXmlHierarchyPanelWidgetContracts() {
        XmlWidgetDocument document = XmlWidgetDocument.parse(
                "<VBox id=\"root\"><Label id=\"first\" /><Button id=\"second\" /></VBox>");
        XmlHierarchyPanel panel = new XmlHierarchyPanel().document(document);

        expect(panel.rowCount() == 3,
                "XML hierarchy panel should display root and child XML element rows");
        expect(panel.visibleItems().get(0).label().equals("VBox#root")
                        && panel.visibleItems().get(1).label().equals("Label#first")
                        && panel.visibleItems().get(2).label().equals("Button#second"),
                "XML hierarchy panel should display element labels as Tag#id where ids are available");
        expectFailsUnsupported(() -> panel.visibleItems().clear(),
                "XML hierarchy panel visible item snapshot should be immutable");

        List<XmlHierarchyPanel.SelectionChange> selectionChanges = new java.util.ArrayList<>();
        EventSubscription selectionSubscription = panel.onSelectionChanged(selectionChanges::add);
        panel.selectPath(XmlWidgetNodePath.of(1));
        expect(panel.selectedPath().orElseThrow().equals(XmlWidgetNodePath.of(1))
                        && selectionChanges.size() == 1
                        && selectionChanges.get(0).path().equals(XmlWidgetNodePath.of(1)),
                "XML hierarchy panel should select nodes by XmlWidgetNodePath and emit selection changes");
        closeUnchecked(selectionSubscription);

        List<XmlHierarchyPanel.AddChildRequest> addRequests = new java.util.ArrayList<>();
        EventSubscription addSubscription = panel.onAddChildRequested(addRequests::add);
        panel.selectedPath(XmlWidgetNodePath.root());
        expect(panel.requestAddChild(new XmlWidgetElement("Label").attribute("id", "third")),
                "XML hierarchy panel should request child insertion under the selected XML element");
        expect(addRequests.size() == 1
                        && addRequests.get(0).parentPath().equals(XmlWidgetNodePath.root())
                        && addRequests.get(0).element().name().equals("Label")
                        && addRequests.get(0).element().attribute("id").orElseThrow().equals("third"),
                "XML hierarchy panel add-child request should carry parent path, index and copied element");

        WidgetPalette palette = new WidgetPalette();
        EventSubscription paletteSubscription = panel.bindPalette(palette);
        addRequests.clear();
        expect(palette.requestInsert("Button", XmlWidgetNodePath.of(1), 0),
                "Bound widget palette should be able to request hierarchy insertion");
        expect(addRequests.size() == 1
                        && addRequests.get(0).parentPath().equals(XmlWidgetNodePath.root())
                        && addRequests.get(0).element().name().equals("Button"),
                "XML hierarchy panel should insert palette widgets under the current selected XML element");
        closeUnchecked(paletteSubscription);
        closeUnchecked(addSubscription);

        List<XmlHierarchyPanel.NodeAction> deleteRequests = new java.util.ArrayList<>();
        EventSubscription deleteSubscription = panel.onDeleteRequested(deleteRequests::add);
        panel.selectedPath(XmlWidgetNodePath.of(0));
        expect(panel.requestDeleteSelected(),
                "XML hierarchy panel should request deletion for non-root selections");
        expect(deleteRequests.size() == 1 && deleteRequests.get(0).path().equals(XmlWidgetNodePath.of(0)),
                "XML hierarchy panel delete request should carry the selected path");
        panel.selectedPath(XmlWidgetNodePath.root());
        expect(!panel.requestDeleteSelected(),
                "XML hierarchy panel should not request root deletion");
        closeUnchecked(deleteSubscription);

        List<XmlHierarchyPanel.MoveRequest> moveRequests = new java.util.ArrayList<>();
        EventSubscription moveSubscription = panel.onMoveRequested(moveRequests::add);
        panel.selectedPath(XmlWidgetNodePath.of(1));
        expect(panel.requestMoveSelectedUp(),
                "XML hierarchy panel should request moving the selected child up within its parent");
        panel.selectedPath(XmlWidgetNodePath.of(0));
        expect(panel.requestMoveSelectedDown(),
                "XML hierarchy panel should request moving the selected child down within its parent");
        expect(moveRequests.size() == 2
                        && moveRequests.get(0).parentPath().equals(XmlWidgetNodePath.root())
                        && moveRequests.get(0).fromIndex() == 1
                        && moveRequests.get(0).toIndex() == 0
                        && moveRequests.get(1).fromIndex() == 0
                        && moveRequests.get(1).toIndex() == 1,
                "XML hierarchy panel move requests should carry parent path and sibling indexes");
        closeUnchecked(moveSubscription);

        XmlWidgetDocumentEdits.moveChild(XmlWidgetNodePath.root(), 0, 1).apply(document);
        panel.document(document);
        expect(panel.visibleItems().get(1).label().equals("Button#second")
                        && panel.visibleItems().get(2).label().equals("Label#first"),
                "XML hierarchy panel should rebuild rows when the same document instance is reordered");
    }

    private void testXmlPropertiesPanelSessionContracts() {
        XmlEditorSession session = XmlEditorSession.create(
                "<VBox id=\"root\" x=\"2\" spacing=\"4\"><Label id=\"title\" text=\"Hello\" futureEditorAttr=\"keep\" /></VBox>");
        session.selectRoot();
        XmlPropertiesPanel panel = new XmlPropertiesPanel().session(session);

        expect(panel.rowCount() > 0
                        && panel.grid().categories().stream().anyMatch(category -> category.name().equals("Common"))
                        && panel.grid().categories().stream().anyMatch(category -> category.name().equals("Layout")),
                "XML properties panel should group fields by descriptor category");
        expect(panel.row("x").orElseThrow().fieldKind() == PropertyFieldRow.FieldKind.NUMBER,
                "XML properties panel should render numeric fields from descriptor metadata");

        expect(panel.setAttributeValue("x", "8"),
                "XML properties panel should apply simple attribute edits through the editor session");
        expect(session.document().root().attribute("x").orElseThrow().equals("8")
                        && session.text().contains("x=\"8\"")
                        && session.undoCount() == 1,
                "XML properties panel edits should update document text and undo stack");

        panel.grid().setAttributeValue("x", "wide");
        expect(session.document().root().attribute("x").orElseThrow().equals("8")
                        && panel.lastRejectedAttribute().equals("x")
                        && panel.lastValidationMessage().contains("number"),
                "XML properties panel should validate typed values before committing invalid edits");

        expect(panel.addAvailableAttribute("height"),
                "XML properties panel should add available descriptor attributes with their default value");
        expect(session.document().root().attribute("height").orElseThrow().equals(
                        panel.row("height").orElseThrow().defaultValue()),
                "XML properties panel add-attribute should use descriptor defaults");

        expect(panel.resetAttribute("spacing"),
                "XML properties panel should reset attributes to descriptor defaults");
        expect(session.document().root().attribute("spacing").orElseThrow().equals(
                        panel.row("spacing").orElseThrow().defaultValue()),
                "XML properties panel reset should write the descriptor default value");

        expect(panel.removeAttribute("height"),
                "XML properties panel should remove optional attributes through undoable document edits");
        expect(session.document().root().attribute("height").isEmpty(),
                "XML properties panel remove should delete the selected XML attribute");
        expect(session.undo(), "XML properties panel remove edit should be undoable");
        expect(session.document().root().attribute("height").orElseThrow().equals(
                        panel.row("height").orElseThrow().defaultValue()),
                "Undo should restore the removed XML attribute");

        session.select(XmlWidgetNodePath.of(0));
        expect(panel.row("futureEditorAttr").orElseThrow().fieldKind() == PropertyFieldRow.FieldKind.STRING
                        && panel.row("futureEditorAttr").orElseThrow().category().equals("Unknown"),
                "XML properties panel should fall back unknown source attributes to string fields");
        expect(panel.setAttributeValue("text", "Updated"),
                "XML properties panel should edit selected child attributes after hierarchy selection changes");
        expect(XmlWidgetNodePath.of(0).resolveElement(session.document())
                        .orElseThrow()
                        .attribute("text")
                        .orElseThrow()
                        .equals("Updated"),
                "XML properties panel should follow the current editor selection");
    }

    private void testXmlPropertiesPanelObjectPickerContracts() {
        XmlEditorSession session = XmlEditorSession.create(
                "<VBox id=\"root\" shaderId=\"old:shader\" item=\"minecraft:apple\">"
                        + "<Label id=\"title\" text=\"Hello\" color=\"#11223344\" />"
                        + "<TextureWidget id=\"texture\" texture=\"minecraft:textures/block/stone.png\" />"
                        + "</VBox>");
        XmlPropertiesPanel panel = new XmlPropertiesPanel().session(session)
                .assetCatalog(XmlWidgetAssetCatalog.builder()
                        .shader("test:glow")
                        .texture("test:icon", 16, 16)
                        .font("test:font")
                        .build());

        session.selectRoot();
        expect(panel.openObjectPicker("shaderId")
                        && panel.activePicker() == XmlPropertiesPanel.ObjectPickerKind.ASSET
                        && panel.assetPicker().kind() == XmlWidgetAssetKind.SHADER
                        && panel.assetPicker().targetAttribute().equals("shaderId"),
                "XML properties panel should route generic asset resource attributes to the asset picker");
        int undoBeforeAsset = session.undoCount();
        expect(panel.assetPicker().selectAsset("test:glow") && panel.applySelectedAsset(),
                "XML properties panel asset picker should apply the selected catalog asset");
        expect(session.document().root().attribute("shaderId").orElseThrow().equals("test:glow")
                        && session.undoCount() == undoBeforeAsset + 1,
                "XML properties panel asset picker should write an undoable XML attribute edit");

        ResourceLocation stoneTexture = ResourceLocation.tryParse("minecraft:textures/block/stone.png");
        ResourceLocation diamondTexture = ResourceLocation.tryParse("minecraft:textures/item/diamond.png");
        panel.texturePicker().textureIds(List.of(stoneTexture, diamondTexture));
        session.select(XmlWidgetNodePath.of(1));
        expect(panel.openObjectPicker("texture")
                        && panel.activePicker() == XmlPropertiesPanel.ObjectPickerKind.TEXTURE
                        && panel.pickerAttributeName().equals("texture"),
                "XML properties panel should route texture resource attributes to the Minecraft texture picker");
        panel.texturePicker().selectId(diamondTexture);
        expect(XmlWidgetNodePath.of(1).resolveElement(session.document())
                        .orElseThrow()
                        .attribute("texture")
                        .orElseThrow()
                        .equals("minecraft:textures/item/diamond.png"),
                "XML properties panel texture picker should write selected texture ids to XML");

        session.selectRoot();
        panel.itemPickerValues(List.of("minecraft:apple", "minecraft:diamond"));
        expect(panel.openObjectPicker("item")
                        && panel.activePicker() == XmlPropertiesPanel.ObjectPickerKind.ITEM,
                "XML properties panel should route item attributes to the item resource picker");
        panel.itemPicker().selectedIndex(1);
        expect(session.document().root()
                        .attribute("item")
                        .orElseThrow()
                        .equals("minecraft:diamond"),
                "XML properties panel item picker should write selected item ids to XML");

        session.select(XmlWidgetNodePath.of(0));
        expect(panel.openObjectPicker("color")
                        && panel.activePicker() == XmlPropertiesPanel.ObjectPickerKind.COLOR,
                "XML properties panel should route color attributes to the color picker");
        panel.colorPicker().rgba255(0x22, 0x44, 0x66, 0x88);
        expect(panel.applySelectedColor(),
                "XML properties panel color picker should apply the selected color value");
        expect(XmlWidgetNodePath.of(0).resolveElement(session.document())
                        .orElseThrow()
                        .attribute("color")
                        .orElseThrow()
                        .equals("#22446688"),
                "XML properties panel color picker should write #RRGGBBAA XML color values");

        expect(!panel.openObjectPicker("text")
                        && panel.activePicker() == XmlPropertiesPanel.ObjectPickerKind.NONE,
                "XML properties panel should reject object pickers for plain string attributes");
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
                <VirtualListView itemCount="12" itemHeight="22" overscan="3" selectionMode="multiple" />
                """, VirtualListView.class, registry);
        expect(list.itemCount() == 12
                        && near(list.itemHeight(), 22.0f)
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
