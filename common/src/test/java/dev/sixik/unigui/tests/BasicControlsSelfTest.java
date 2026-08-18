package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimatedProperty;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.animation.TransformOrigin;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.MutableUIScaleProvider;
import dev.sixik.unigui.api.core.UIScaleProvider;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.editor.CommandChangedEvent;
import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;
import dev.sixik.unigui.api.editor.KeyBinding;
import dev.sixik.unigui.api.event.ContextMenuItemSelectedEvent;
import dev.sixik.unigui.api.event.ExpandedChangedEvent;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.SearchChangedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.event.SliderValueChangedEvent;
import dev.sixik.unigui.api.event.TableCellEditCancelledEvent;
import dev.sixik.unigui.api.event.TableCellEditCommittedEvent;
import dev.sixik.unigui.api.event.TableCellEditStartedEvent;
import dev.sixik.unigui.api.event.TableColumnMovedEvent;
import dev.sixik.unigui.api.event.TableColumnResizedEvent;
import dev.sixik.unigui.api.event.TableSortChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.input.FocusDirection;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProviders;
import dev.sixik.unigui.api.render.shaders.ShaderSource;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.selection.SelectionMode;
import dev.sixik.unigui.api.sort.SortDirection;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.MutableTheme;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetAsset;
import dev.sixik.unigui.api.xml.XmlWidgetAssetCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetAssetKind;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnostic;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsModel;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutFrame;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutHandle;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.virtualization.FixedRowVirtualizer;
import dev.sixik.unigui.api.virtualization.VirtualRange;
import dev.sixik.unigui.backend.minecraft.MinecraftGuiRenderBackend;
import dev.sixik.unigui.backend.minecraft.MinecraftFontFace;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPickerWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftPreviewWidget;
import dev.sixik.unigui.backend.minecraft.MinecraftTextureHandle;
import dev.sixik.unigui.widgets.minecraft.MinecraftTexturePickerWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftWidgets;
import dev.sixik.unigui.impl.input.TransformHitTester;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.ScissorStack;
import dev.sixik.unigui.impl.render.SimpleDrawBatcher;
import dev.sixik.unigui.impl.text.AwtFontFace;
import dev.sixik.unigui.impl.text.SdfGlyph;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.navigation.Accordion;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.navigation.Breadcrumb;
import dev.sixik.unigui.widgets.navigation.BreadcrumbItem;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.navigation.Carousel;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.CodeEditor;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.containers.DockPanel;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockDropIntent;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshotCodec;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockPaneKind;
import dev.sixik.unigui.widgets.containers.DockSide;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.interaction.DropDownBox;
import dev.sixik.unigui.widgets.interaction.IconButton;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.Menu;
import dev.sixik.unigui.widgets.navigation.MenuBar;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.display.Chart;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.LoadingIndicator;
import dev.sixik.unigui.widgets.graph.GraphView;
import dev.sixik.unigui.widgets.graph.NodeGraph;
import dev.sixik.unigui.widgets.graph.NodeGraphConnection;
import dev.sixik.unigui.widgets.graph.NodeGraphConnectionValidation;
import dev.sixik.unigui.widgets.graph.NodeGraphItem;
import dev.sixik.unigui.widgets.graph.NodeGraphSnapshot;
import dev.sixik.unigui.widgets.graph.NodeGraphPortKind;
import dev.sixik.unigui.widgets.graph.NodeGraphPortRef;
import dev.sixik.unigui.widgets.graph.NodeGraphPortSide;
import dev.sixik.unigui.widgets.feedback.NotificationView;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.navigation.PageView;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.PanelRowWidget;
import dev.sixik.unigui.widgets.interaction.PasswordField;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.RadioGroup;
import dev.sixik.unigui.widgets.interaction.SearchField;
import dev.sixik.unigui.widgets.containers.SettingRow;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.feedback.Spinner;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.navigation.TabControl;
import dev.sixik.unigui.widgets.display.Text;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.editor.AssetBrowserPanel;
import dev.sixik.unigui.widgets.editor.CommandPalette;
import dev.sixik.unigui.widgets.editor.DesignCanvasOverlay;
import dev.sixik.unigui.widgets.editor.Dialog;
import dev.sixik.unigui.widgets.editor.DragPayload;
import dev.sixik.unigui.widgets.editor.DragSource;
import dev.sixik.unigui.widgets.editor.DropTarget;
import dev.sixik.unigui.widgets.editor.PaneHeader;
import dev.sixik.unigui.widgets.editor.PaneVisibilityController;
import dev.sixik.unigui.widgets.editor.PalettePanel;
import dev.sixik.unigui.widgets.editor.PropertyFieldRow;
import dev.sixik.unigui.widgets.editor.PropertyGrid;
import dev.sixik.unigui.widgets.editor.ProjectPickerPanel;
import dev.sixik.unigui.widgets.editor.ResizablePanelHeader;
import dev.sixik.unigui.widgets.editor.SearchBoxWithFilterChips;
import dev.sixik.unigui.widgets.editor.SelectionOverlay;
import dev.sixik.unigui.widgets.editor.StatusBar;
import dev.sixik.unigui.widgets.editor.WidgetPalette;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.display.RichTextView;
import dev.sixik.unigui.widgets.feedback.Toast;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.navigation.TreeView;
import dev.sixik.unigui.widgets.navigation.TreeList;
import dev.sixik.unigui.widgets.navigation.TreeViewNode;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.widgets.core.Widgets;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import dev.sixik.unigui.widgets.containers.Splitter;
import dev.sixik.unigui.widgets.docking.DockingManager;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockSplitOrientation;
import dev.sixik.unigui.widgets.feedback.WindowManager;
import dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.SearchableGridPickerWidget;
import dev.sixik.unigui.widgets.display.Sparkline;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.interaction.ToggleToolButton;
import dev.sixik.unigui.widgets.interaction.ToolButton;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import dev.sixik.unigui.widgets.navigation.ToolBar;

public final class BasicControlsSelfTest {
    public static void main(String[] args) {
        new BasicControlsSelfTest().run();
    }

    private void run() {
        testTextEditorModelCore();
        testUIScaleProviderContracts();
        testTextInputShellAndTextFieldChrome();
        testTextOverflowModes();
        testRichTextAndSdfContracts();
        testTexturePlacementAndBackgrounds();
        testShaderDrawCommandContracts();
        testTextFieldFocusAndEditing();
        testTextFieldSelectionAndClipboard();
        testTextInputClippingMetricsAndSelection();
        testTextAreaMultilineEditingAndScrolling();
        testCodeEditorDecorationsAndDirtyState();
        testEditorCommandManagerAndKeyBindings();
        testMenuBarCommandBackedPopups();
        testToolBarCommandButtonsAndDisplayModes();
        testPropertyGridContracts();
        testSelectionOverlayContracts();
        testPaneVisibilityControllerContracts();
        testProjectPickerPanelContracts();
        testStatusBarContracts();
        testWidgetPaletteContracts();
        testCommandPaletteContracts();
        testAssetBrowserPanelContracts();
        testSearchBoxWithFilterChipsContracts();
        testDialogContracts();
        testPaneHeaderContracts();
        testDragDropHelperContracts();
        testTextWidgetRoleContracts();
        testPasswordAndSearchFields();
        testDefaultThemeContracts();
        testStyleInheritanceAndScopes();
        testKeyboardFocusTraversal();
        testDirectionalFocusNavigation();
        testHoverTrackingAndStyleState();
        testEnabledVisibleStateFlags();
        testDesiredSizeMeasurement();
        testLayoutConstraintsAndSlotSizing();
        testLayoutV2CompatibilityContracts();
        testPublicLayoutApiContracts();
        testFlexLayoutV2Resolver();
        testOverflowAndScrollContracts();
        testAbsoluteLayoutContracts();
        testRicherLayoutContainers();
        testSliderPointerAndKeyboardInput();
        testScrollViewBubbledWheelInput();
        testScrollViewNestedWheelLockAndOptOut();
        testNestedScissorStack();
        testWidgetAnimationTransitions();
        testMinecraftPreviewWidgetFallbacks();
        testMinecraftPickerWidgetsContracts();
        testOverlayLayerAndTooltipBasics();
        testDockingRootContracts();
        testNodeGraphPhaseOneContracts();
        testNodeGraphPhaseTwoConnectionContracts();
        testNodeGraphPhaseThreeEditingContracts();
        testNodeGraphPhaseFourSnapshotContracts();
        testFixedRowVirtualizationCore();
        testVirtualizedSelectionContracts();
        testVirtualListKeyboardNavigation();
        testVirtualListViewRealizationAndScrolling();
        testVirtualListNestedWheelLockAndOptOut();
        testVirtualTableSortingContracts();
        testVirtualTableColumnResizeAndMoveContracts();
        testVirtualTableKeyboardNavigation();
        testVirtualTableCellEditingContracts();
        testVirtualTableViewVirtualRowsAndRendering();
        testVirtualTableNestedWheelLockAndOptOut();
        testTabControlContracts();
        testPageViewAndCarouselContracts();
        testComboBoxAndDropDownBoxContracts();
        testExpandablePanelAndAccordionContracts();
        testTreeViewContracts();
        testLoadingIndicatorContracts();
        testSplitPanelContracts();
        testBreadcrumbContracts();
        testToggleCheckboxProgressAndNumberField();
        System.out.println("BasicControlsSelfTest passed");
    }

    private void testTextEditorModelCore() {
        TextEditorModel editor = new TextEditorModel();
        Counter changes = new Counter();
        editor.onChanged((oldText, newText) -> {
            changes.count++;
            changes.lastText = newText;
        });

        editor.silentText("abcd");
        editor.cursorIndex(editor.text().length());
        editor.select(1, 3);
        expect(editor.hasSelection() && editor.selectedText().equals("bc"), "TextEditorModel should expose selected text");

        editor.insertText("XYZ");
        expect(editor.text().equals("aXYZd"), "TextEditorModel should replace selection with inserted text");
        expect(editor.cursorIndex() == 4 && !editor.hasSelection(), "TextEditorModel should collapse selection after insertion");

        editor.backspace();
        expect(editor.text().equals("aXYd") && editor.cursorIndex() == 3, "TextEditorModel should backspace by code point");

        editor.delete();
        expect(editor.text().equals("aXY"), "TextEditorModel should delete next code point");

        editor.maxLength(2);
        expect(editor.text().equals("aX"), "TextEditorModel should trim text when maxLength shrinks");
        expect(changes.count == 4 && changes.lastText.equals("aX"), "TextEditorModel should emit text change callbacks");
        expect(TextEditorModel.sanitizePrintable("a\nb\tc").equals("abc"), "TextEditorModel should sanitize non-printable input");
    }

    private void testUIScaleProviderContracts() {
        UIScaleProvider fixed = UIScaleProvider.fixed(2.0f);
        expect(near(fixed.scale(), 2.0f)
                        && near(fixed.toBackendPixels(12.0f), 24.0f)
                        && near(fixed.toLogicalPixels(24.0f), 12.0f),
                "UIScaleProvider.fixed should convert logical and backend pixels using its scale");

        MutableUIScaleProvider mutable = new MutableUIScaleProvider(0.5f);
        expect(near(mutable.scale(), 0.5f)
                        && near(mutable.toBackendPixels(20.0f), 10.0f)
                        && near(mutable.toLogicalPixels(10.0f), 20.0f),
                "MutableUIScaleProvider should expose editable UI scale");

        mutable.scale(0.0f);
        expect(near(mutable.scale(), 1.0f)
                        && near(UIScaleProvider.fixed(Float.NaN).scale(), 1.0f),
                "UI scale providers should sanitize invalid scales to identity");

        UnityLikeUIScaleProvider unityLike = new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .viewport(3840.0f, 1080.0f)
                .matchBalanced();
        expect(near(unityLike.scale(), 1.4142f),
                "UnityLikeUIScaleProvider should blend width and height scale logarithmically");

        expect(near(unityLike.matchWidth().scale(), 2.0f)
                        && near(unityLike.matchHeight().scale(), 1.0f),
                "UnityLikeUIScaleProvider should support width and height matching modes");

        unityLike.scaleRange(0.75f, 1.25f).matchWidth();
        expect(near(unityLike.scale(), 1.25f),
                "UnityLikeUIScaleProvider should clamp calculated scale to the configured range");

        unityLike.scaleRange(0.75f, 2.5f).matchBalanced();
        unityLike.viewportSize(Float.NaN, Float.NaN);
        expect(near(unityLike.scale(), 1.0f),
                "UnityLikeUIScaleProvider should fall back to reference viewport for invalid sizes");
    }

    private void testTextInputShellAndTextFieldChrome() {
        TextInput input = new TextInput("core");
        input.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList inputDrawList = new DrawList();
        input.render(new DefaultRenderContext(inputDrawList));
        expect(hasText(inputDrawList, "core"), "TextInput shell should render editor text");
        expect(!hasCommand(inputDrawList, DrawCommandType.ROUNDED_RECT), "TextInput shell should not force field chrome");

        TextField field = new TextField("field");
        field.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList fieldDrawList = new DrawList();
        field.render(new DefaultRenderContext(fieldDrawList));
        expect(hasText(fieldDrawList, "field"), "TextField chrome should preserve TextInput text rendering");
        expect(hasCommand(fieldDrawList, DrawCommandType.ROUNDED_RECT), "TextField should add default field chrome");

        expect(new PasswordField() instanceof TextInput, "PasswordField should reuse TextInput shell directly");
        expect(new NumberField() instanceof TextInput, "NumberField should reuse TextInput shell directly");
        expect(new SearchField() instanceof TextInput, "SearchField should reuse TextInput shell directly");
    }

    private void testTextOverflowModes() {
        TextBlock clipped = new TextBlock("abcdefghijklmnop");
        clipped.overflowMode(TextOverflowMode.CLIP);
        clipped.arrange(new MutableRect(0.0f, 0.0f, 30.0f, 12.0f));
        DrawList clippedDrawList = new DrawList();
        clipped.render(new DefaultRenderContext(clippedDrawList));
        expect(clippedDrawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP,
                "TextWidget CLIP overflow should push a clip before text");
        expect(hasCommand(clippedDrawList, DrawCommandType.POP_CLIP),
                "TextWidget CLIP overflow should pop its clip");

        TextBlock wrapped = new TextBlock("alpha beta gamma");
        wrapped.overflowMode(TextOverflowMode.CLIP);
        wrapped.arrange(new MutableRect(0.0f, 0.0f, 42.0f, 36.0f));
        DrawList wrappedDrawList = new DrawList();
        wrapped.render(new DefaultRenderContext(wrappedDrawList));
        expect(countTextCommands(wrappedDrawList) >= 3,
                "TextWidget wrap(true) should render multiple wrapped lines");

        TextBlock edgeWrapped = new TextBlock("Pane slots are V3 flex items.");
        edgeWrapped.overflowMode(TextOverflowMode.CLIP);
        edgeWrapped.arrange(new MutableRect(0.0f, 0.0f, 162.0f, 36.0f));
        DrawList edgeWrappedDrawList = new DrawList();
        edgeWrapped.render(new DefaultRenderContext(edgeWrappedDrawList)
                .backend(new FixedTextMetricsBackend(new FixedFontFace("wide-default", 6.0f, 10.0f))));
        expect(countTextCommands(edgeWrappedDrawList) >= 2,
                "TextWidget wrap(true) should wrap the final word when only part of it overflows");
        expect(hasText(edgeWrappedDrawList, "Pane slots are V3 flex"),
                "TextWidget wrap(true) should use backend default font metrics for line breaks");

        TextBlock scaledWrapped = new TextBlock("abcd efgh");
        scaledWrapped.overflowMode(TextOverflowMode.CLIP);
        scaledWrapped.transform().scale().set(2.0f, 2.0f);
        scaledWrapped.arrange(new MutableRect(0.0f, 0.0f, 48.0f, 40.0f));
        DrawList scaledWrappedDrawList = new DrawList();
        scaledWrapped.render(new DefaultRenderContext(scaledWrappedDrawList));
        expect(countTextCommands(scaledWrappedDrawList) >= 2,
                "TextWidget wrap(true) should account for transform scale when wrapping");

        TextBlock shrink = new TextBlock("abcdefghijklmnop");
        shrink.overflowMode(TextOverflowMode.SHRINK_TO_FIT);
        shrink.arrange(new MutableRect(0.0f, 0.0f, 24.0f, 12.0f));
        DrawList shrinkDrawList = new DrawList();
        shrink.render(new DefaultRenderContext(shrinkDrawList));
        int shrinkTextIndex = firstCommandIndex(shrinkDrawList, DrawCommandType.TEXT, 0);
        expect(shrinkTextIndex >= 0 && shrinkDrawList.commands().get(shrinkTextIndex).transform().scale().x() < 1.0f,
                "TextWidget SHRINK_TO_FIT should scale overflowing text down");

        TextBlock marquee = new TextBlock("abcdefghijklmnop");
        marquee.overflowMode(TextOverflowMode.MARQUEE_ON_HOVER).marqueeSpeed(30.0f);
        marquee.arrange(new MutableRect(0.0f, 0.0f, 30.0f, 12.0f));
        marquee.handle(new PointerEnteredEvent(marquee, 1.0f, 1.0f, 1.0f, 1.0f, 0));
        marquee.tick(new FrameContext(1, 0.5f, 0.0f, dev.sixik.unigui.api.core.FramePhase.RENDER));
        DrawList marqueeDrawList = new DrawList();
        marquee.render(new DefaultRenderContext(marqueeDrawList));
        expect(marqueeDrawList.commands().stream().filter(command -> command.type() == DrawCommandType.TEXT).count() == 2,
                "TextWidget MARQUEE_ON_HOVER should draw a wrapped marquee copy while hovered");
    }

    private void testTextWidgetRoleContracts() {
        FontFace narrow = new FixedFontFace("text-role-narrow", 5.0f, 10.0f);
        FontFace wide = new FixedFontFace("text-role-wide", 9.0f, 14.0f);
        RichText rich = RichText.builder()
                .font(narrow).size(10.0f).append("Rich ")
                .font(wide).size(14.0f).append("Runs")
                .build();

        Text text = new Text("Generic display");
        Label label = new Label("Name").focusTarget(new TextField("target"));
        TextBlock plainBlock = new TextBlock(rich);
        RichTextView richView = new RichTextView(rich);

        expect(text instanceof TextWidget
                        && label instanceof TextWidget
                        && plainBlock instanceof TextWidget
                        && richView instanceof TextWidget,
                "Text family public widgets should share TextWidget as their base implementation");

        plainBlock.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 40.0f));
        DrawList plainDrawList = new DrawList();
        plainBlock.render(new DefaultRenderContext(plainDrawList));
        int plainIndex = textCommandIndex(plainDrawList, "Rich Runs", 0);
        expect(plainBlock.wrap()
                        && plainIndex < Integer.MAX_VALUE
                        && plainDrawList.commands().get(plainIndex).richText().equals(RichText.plain("Rich Runs")),
                "TextBlock should be plain multiline text and flatten RichText styling");

        richView.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 40.0f));
        DrawList richDrawList = new DrawList();
        richView.render(new DefaultRenderContext(richDrawList));
        int richIndex = textCommandIndex(richDrawList, "Rich Runs", 0);
        expect(richView.wrap()
                        && richIndex < Integer.MAX_VALUE
                        && richDrawList.commands().get(richIndex).richText().equals(rich),
                "RichTextView should preserve RichText runs for rich multiline content");

        DefaultUIContext uiContext = new DefaultUIContext();
        TextField target = new TextField("focus me");
        Label focusLabel = new Label("Focus target").focusTarget(target);
        focusLabel.setUiContextInternal(uiContext);
        target.setUiContextInternal(uiContext);
        focusLabel.handle(new PointerPressedEvent(focusLabel, 2.0f, 2.0f, 2.0f, 2.0f, 0, PointerButton.PRIMARY));
        expect(uiContext.focusManager().focusedWidget() == target,
                "Clicking Label with focusTarget should focus the associated control");

        ToggleSwitch settingControl = new ToggleSwitch().trackSize(20.0f, 10.0f).thumbSize(8.0f);
        SettingRow settingRow = new SettingRow("Crossplay", settingControl)
                .rowHeight(24.0f)
                .gap(12.0f)
                .controlWidth(30.0f);
        settingRow.measure(new LayoutContext(200.0f, 80.0f));
        settingRow.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 24.0f));
        expect(near(settingRow.desiredSize().height(), 24.0f)
                        && near(settingRow.label().layoutBounds().x(), 0.0f)
                        && near(settingControl.layoutBounds().x(), 170.0f)
                        && near(settingControl.layoutBounds().width(), 30.0f),
                "SettingRow should measure from its label/control and pin the control to the right edge");

        Button titleLeft = new Button("Settings");
        Button resetAction = new Button("Reset");
        Button applyAction = new Button("Apply");
        titleLeft.layout(style -> style.size(80.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        resetAction.layout(style -> style.size(70.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        applyAction.layout(style -> style.size(56.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        PanelRowWidget panelRow = new PanelRowWidget()
                .rowHeight(28.0f)
                .gap(20.0f)
                .rightGap(6.0f)
                .addLeft(titleLeft)
                .addRight(resetAction)
                .addRight(applyAction);
        panelRow.measure(new LayoutContext(260.0f, 80.0f));
        panelRow.arrange(new MutableRect(0.0f, 0.0f, 260.0f, 28.0f));
        expect(near(panelRow.desiredSize().height(), 28.0f)
                        && near(titleLeft.layoutBounds().x(), 0.0f)
                        && near(resetAction.layoutBounds().x(), 128.0f)
                        && near(applyAction.layoutBounds().x(), 204.0f),
                "PanelRowWidget should support arbitrary left/right widgets and pin right widgets to the row edge");

        expect(Widgets.text("Display") instanceof Text
                        && Widgets.label("Caption") instanceof Label
                        && Widgets.panelRow(new Label("Left"), new Button("Right")) instanceof PanelRowWidget
                        && Widgets.settingRow("Setting", new ToggleSwitch()) instanceof SettingRow
                        && Widgets.textBlock("Paragraph") instanceof TextBlock
                        && Widgets.richTextView(rich) instanceof RichTextView,
                "Widgets factory should expose separated Text, Label, PanelRowWidget, SettingRow, TextBlock and RichTextView roles");
    }

    private void testRichTextAndSdfContracts() {
        FontFace narrow = new FixedFontFace("narrow", 2.0f, 10.0f);
        FontFace wide = new FixedFontFace("wide", 5.0f, 16.0f);
        MutableColor mutableRunColor = new MutableColor(0.2f, 0.4f, 0.6f, 0.8f);
        RichText text = new RichText(java.util.List.of(
                new TextRun("ab", narrow, 10.0f, mutableRunColor),
                new TextRun("C", wide, 16.0f)));

        mutableRunColor.set(1.0f, 1.0f, 1.0f, 1.0f);
        ColorView snapshot = text.runs().get(0).color();
        expect(near(snapshot.r(), 0.2f) && near(snapshot.a(), 0.8f),
                "TextRun should snapshot mutable run colors");
        expect(near(TextEngine.measureLineWidth(text), 9.0f),
                "RichText width should combine advances from different font faces");
        expect(near(TextEngine.measureTextHeight(text), 16.0f),
                "RichText height should use the tallest run on the line");
        expect(text.equals(new RichText(text.runs())),
                "RichText should have value equality for stable widget updates");
        RichText built = RichText.builder()
                .font(narrow).size(10.0f).append("ab")
                .font(wide).size(16.0f).append("C")
                .build();
        expect(built.equals(new RichText(java.util.List.of(
                        new TextRun("ab", narrow, 10.0f),
                        new TextRun("C", wide, 16.0f)))),
                "RichText builder should switch fonts without manual run positioning");

        MinecraftFontFace vanilla = MinecraftFonts.defaultFace();
        expect(vanilla.id().equals("minecraft-font:minecraft:default"),
                "MinecraftFonts should expose the vanilla default FontFace");
        expect(near(vanilla.metrics(9.0f).lineHeight(), 9.0f),
                "Vanilla FontFace should scale Minecraft's native nine-pixel line height");

        AwtFontFace awtDialog = new AwtFontFace("dialog-test", new java.awt.Font("Dialog", java.awt.Font.PLAIN, 16));
        expect(near(awtDialog.advance('A', 24.0f), awtDialog.advance('A', 12.0f) * 2.0f),
                "AwtFontFace should scale cached unit advances linearly");
        SdfGlyph glyph = awtDialog.sdfGlyph('A', 48, 8);
        int minPixel = 255;
        int maxPixel = 0;
        for (byte pixel : glyph.pixels()) {
            int value = pixel & 0xFF;
            minPixel = Math.min(minPixel, value);
            maxPixel = Math.max(maxPixel, value);
        }
        expect(glyph.width() > 1 && glyph.height() > 1 && glyph.advance() > 0.0f
                        && minPixel < 96 && maxPixel > 160,
                "AwtFontFace SDF glyph should contain a signed distance range");

        RichText mixed = RichText.builder()
                .font(narrow).size(10.0f).append("SDF ")
                .font(vanilla).size(9.0f).append("Vanilla")
                .build();
        expect(mixed.runs().get(1).font() == vanilla,
                "RichText should preserve Minecraft and SDF faces in one value");

        TextBlock block = new TextBlock();
        block.text("plain");
        block.arrange(new MutableRect(0.0f, 0.0f, 40.0f, 20.0f));

        TextWidget richBase = new TextWidget();
        richBase.richText(text);
        richBase.arrange(new MutableRect(0.0f, 0.0f, 40.0f, 20.0f));
        DrawList drawList = new DrawList();
        richBase.render(new DefaultRenderContext(drawList));
        expect(drawList.size() == 1 && drawList.commands().get(0).richText().equals(text),
                "TextWidget should preserve RichText in the render command");

        DrawList batchList = new DrawList();
        batchList.add(drawList.commands().get(0));
        batchList.add(drawList.commands().get(0));
        var batches = SimpleDrawBatcher.INSTANCE.batch(batchList);
        expect(batches.size() == 1 && !batches.get(0).isBarrier() && batches.get(0).size() == 2,
                "Consecutive text commands should form one GPU batch");

        DrawList shapeList = new DrawList();
        DefaultRenderContext shapeContext = new DefaultRenderContext(shapeList);
        shapeContext.rect(0.0f, 0.0f, 20.0f, 10.0f, Paint.fill(new MutableColor()));
        shapeContext.roundedRect(2.0f, 2.0f, 18.0f, 10.0f, 3.0f, Paint.fill(new MutableColor()));
        shapeContext.line(0.0f, 12.0f, 20.0f, 12.0f, Paint.stroke(new MutableColor(), 1.0f));
        var shapeBatches = SimpleDrawBatcher.INSTANCE.batch(shapeList);
        expect(shapeBatches.size() == 1 && !shapeBatches.get(0).isBarrier()
                        && shapeBatches.get(0).size() == 3,
                "Mixed color primitives should share one GPU batch");

        AwtFontFace awt = new AwtFontFace("self-test", new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 16));
        SdfGlyph first = awt.sdfGlyph('A', 32, 6);
        SdfGlyph cached = awt.sdfGlyph('A', 32, 6);
        expect(first == cached, "SDF glyph generation should be cached by face, size and spread");
        int minimum = 255;
        int maximum = 0;
        for (byte pixel : first.pixels()) {
            int value = pixel & 0xFF;
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        expect(minimum < 128 && maximum > 128,
                "SDF glyph should contain both outside and inside distance values");
    }

    private void testTexturePlacementAndBackgrounds() {
        SimpleTextureHandle texture = new SimpleTextureHandle("test:textures/ui/background.png", 200, 100);
        MutableRect destination = new MutableRect(10.0f, 20.0f, 100.0f, 100.0f);

        TexturePlacement contain = TexturePlacement.fit(texture, destination, ImageFit.CONTAIN);
        expect(near(contain.x(), 10.0f) && near(contain.y(), 45.0f)
                        && near(contain.width(), 100.0f) && near(contain.height(), 50.0f),
                "CONTAIN should letterbox the destination without cropping UVs");

        TexturePlacement cover = TexturePlacement.fit(texture, destination, ImageFit.COVER);
        expect(near(cover.x(), 10.0f) && near(cover.y(), 20.0f)
                        && near(cover.width(), 100.0f) && near(cover.height(), 100.0f),
                "COVER should preserve the destination bounds");
        expect(near(cover.u(), 0.25f) && near(cover.v(), 0.0f)
                        && near(cover.uWidth(), 0.5f) && near(cover.vHeight(), 1.0f),
                "COVER should crop the wider texture symmetrically");

        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.background().set(0.1f, 0.2f, 0.3f, 1.0f);
        box.backgroundTexture(texture);
        box.backgroundTextureFit(ImageFit.COVER);
        box.backgroundTextureTint().set(0.8f, 0.7f, 0.6f, 0.5f);
        box.borderVisible(true);
        box.radius(6.0f);
        box.arrange(destination);

        DrawList drawList = new DrawList();
        box.render(new DefaultRenderContext(drawList));
        expect(drawList.size() == 3, "Textured Box should emit color, texture and border commands");
        expect(drawList.commands().get(0).type() == DrawCommandType.ROUNDED_RECT
                        && drawList.commands().get(1).type() == DrawCommandType.TEXTURE
                        && drawList.commands().get(2).type() == DrawCommandType.ROUNDED_RECT,
                "Textured Box should render solid background, texture, then border");

        DrawCommand textureCommand = drawList.commands().get(1);
        expect(textureCommand.texture() == texture && near(textureCommand.radius(), 6.0f),
                "Background texture command should preserve its texture and corner radius");
        expect(near(textureCommand.uv().x(), 0.25f) && near(textureCommand.uv().width(), 0.5f),
                "Background texture command should snapshot COVER UV cropping");
        expect(near(textureCommand.paint().color().r(), 0.8f)
                        && near(textureCommand.paint().color().a(), 0.5f),
                "Background texture command should snapshot tint and opacity");

        DrawCommand copied = textureCommand.copy();
        textureCommand.uv().set(0.0f, 0.0f, 1.0f, 1.0f);
        expect(near(copied.uv().x(), 0.25f) && near(copied.uv().width(), 0.5f),
                "DrawCommand.copy should snapshot texture UVs");

        DrawList batchList = new DrawList();
        batchList.add(copied);
        batchList.add(copied.copy().texture(new SimpleTextureHandle(texture.id(), 200, 100)));
        var batches = SimpleDrawBatcher.INSTANCE.batch(batchList);
        expect(batches.size() == 1 && batches.get(0).size() == 2,
                "Texture commands with the same resource id should share one batch");

        MutableStyle textureStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_TEXTURE, texture)
                .put(StyleKeys.BACKGROUND_TEXTURE_FIT, ImageFit.COVER);
        expect(textureStyle.get(StyleKeys.BACKGROUND_TEXTURE, WidgetState.HOVERED, null) == texture,
                "Background textures should participate in state-aware styles");
    }

    private void testShaderDrawCommandContracts() {
        ShaderHandle shader = ShaderHandle.resource("sdmshop2:pixel_bevel_box");
        ShaderUniforms uniforms = ShaderUniforms.create()
                .setColorArgb("FillColor", 0xFF203040)
                .setColorArgb("HighlightColor", 0xFFFFFFFF)
                .setColorArgb("ShadowColor", 0xFF000000)
                .setFloat("BevelThickness", 2.0f);
        ShaderDrawOptions options = ShaderDrawOptions.defaults().squareVertexOffset(0.0f);

        DrawList drawList = new DrawList();
        RenderContext context = new DefaultRenderContext(drawList);
        context.shader(shader, 4.0f, 6.0f, 32.0f, 14.0f, uniforms, options);

        expect(drawList.size() == 1, "RenderContext.shader should enqueue one draw command");
        DrawCommand command = drawList.commands().get(0);
        expect(command.type() == DrawCommandType.SHADER
                        && command.shader().id().equals("sdmshop2:pixel_bevel_box")
                        && command.bounds().x() == 4.0f
                        && command.bounds().y() == 6.0f
                        && command.bounds().width() == 32.0f
                        && command.bounds().height() == 14.0f
                        && command.shaderUniforms().values().containsKey("BevelThickness")
                        && command.shaderOptions().squareVertexOffset() == 0.0f,
                "Shader draw commands should preserve shader id, bounds, uniforms and options");

        uniforms.setFloat("BevelThickness", 9.0f);
        options.squareVertexOffset(-0.25f);
        expect(command.shaderUniforms().values().get("BevelThickness").floats()[0] == 2.0f
                        && command.shaderOptions().squareVertexOffset() == 0.0f,
                "Shader draw commands should defensively copy uniforms and options");
        expect(ShaderProviders.resolve(ShaderHandle.resource("shaders/unqualified_test_shader")).isPresent(),
                "ClasspathShaderProvider should resolve unqualified shaders/myshader handles");
        expect(ShaderProviders.resolve(ShaderHandle.resource("example:shaders/namespaced_test_shader")).isPresent(),
                "ClasspathShaderProvider should resolve modid:shaders/myshader handles under assets/modid");

        AutoCloseable registration = ShaderProviders.register(handle ->
                handle.id().equals("memory:shaders/inline")
                        ? java.util.Optional.of(ShaderSource.fragment(handle.id(), """
                                #version 150
                                out vec4 fragColor;
                                void main(){ fragColor = vec4(1.0); }
                                """))
                        : java.util.Optional.empty());
        try {
            expect(ShaderProviders.resolve(ShaderHandle.resource("memory:shaders/inline")).isPresent(),
                    "ShaderProviders should resolve manually registered providers before classpath fallback");
        } finally {
            try {
                registration.close();
            } catch (Exception exception) {
                throw new AssertionError("Shader provider registration cleanup should not fail", exception);
            }
        }
    }

    private void testTextFieldFocusAndEditing() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TextField field = new TextField().placeholder("Name");
        field.setUiContextInternal(uiContext);
        field.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 18.0f));

        Counter textChanges = new Counter();
        field.onTextChanged(event -> {
            textChanges.count++;
            textChanges.lastText = event.newText();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(field, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(field.focused(), "TextField should focus on primary pointer press");
        expect(uiContext.focusManager().focusedWidget() == field, "FocusManager should point to focused TextField");

        uiContext.routedEvents().dispatch(new TextInputEvent(field, 'A', 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(field, 'b', 0));
        expect(field.text().equals("Ab"), "TextField should accept routed text input");
        expect(textChanges.count == 2 && textChanges.lastText.equals("Ab"), "TextField should emit text changed events");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.LEFT, 0, 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(field, '!', 0));
        expect(field.text().equals("A!b"), "TextField should insert at cursor");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.BACKSPACE, 0, 0));
        expect(field.text().equals("Ab"), "TextField should backspace before cursor");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.ESCAPE, 0, 0));
        expect(!field.focused(), "TextField should blur on Escape");
        expect(uiContext.focusManager().focusedWidget() == null, "FocusManager should clear after Escape");
    }

    private void testTextFieldSelectionAndClipboard() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TextField field = new TextField("abcd");
        field.setUiContextInternal(uiContext);
        field.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));

        uiContext.routedEvents().dispatch(new PointerPressedEvent(field, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        field.select(1, 3);
        expect(field.hasSelection() && field.selectedText().equals("bc"), "TextField should expose selected text");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.C, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("bc"), "Ctrl+C should copy selected TextField text");
        expect(field.text().equals("abcd"), "Ctrl+C should not mutate TextField text");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.X, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("bc"), "Ctrl+X should copy selected TextField text");
        expect(field.text().equals("ad"), "Ctrl+X should delete selected TextField text");
        expect(!field.hasSelection() && field.cursorIndex() == 1, "Ctrl+X should collapse selection to deleted range start");

        uiContext.clipboard().setText("XYZ");
        field.select(1, 2);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.V, 0, KeyModifiers.CONTROL));
        expect(field.text().equals("aXYZ"), "Ctrl+V should replace selected TextField text");
        expect(field.cursorIndex() == 4, "Ctrl+V should move cursor after inserted text");

        field.text("hello").select(1, 4);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.BACKSPACE, 0, 0));
        expect(field.text().equals("ho"), "Backspace should delete selection");

        field.text("hello").select(1, 4);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(field, KeyCodes.DELETE, 0, 0));
        expect(field.text().equals("ho"), "Delete should delete selection");

        DrawList selectedDrawList = new DrawList();
        field.select(0, 1);
        field.render(new DefaultRenderContext(selectedDrawList));
        int selectionRectIndex = firstCommandIndex(selectedDrawList, DrawCommandType.RECT, 1);
        int textIndex = firstCommandIndex(selectedDrawList, DrawCommandType.TEXT, 0);
        expect(selectionRectIndex >= 0 && textIndex > selectionRectIndex, "TextField should draw selection highlight under text");
    }

    private void testTextInputClippingMetricsAndSelection() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TextField measuredField = new TextField("abcde");
        measuredField.setUiContextInternal(uiContext);
        measuredField.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));
        uiContext.focusManager().requestFocus(measuredField);
        measuredField.cursorIndex(measuredField.text().length());

        DrawList measuredDrawList = new DrawList();
        measuredField.render(new DefaultRenderContext(measuredDrawList).backend(new FixedTextMetricsBackend(2.0f)));
        int caretIndex = lastCommandIndex(measuredDrawList, DrawCommandType.RECT);
        expect(caretIndex >= 0 && near(measuredDrawList.commands().get(caretIndex).bounds().x(), 14.0f),
                "TextInput caret should use backend text metrics instead of fixed approximate width");

        TextField longField = new TextField("abcdefghijklmnopqrstuvwxyz");
        longField.setUiContextInternal(uiContext);
        longField.arrange(new MutableRect(0.0f, 0.0f, 50.0f, 18.0f));
        uiContext.focusManager().requestFocus(longField);
        longField.cursorIndex(longField.text().length());
        DrawList longDrawList = new DrawList();
        longField.render(new DefaultRenderContext(longDrawList));
        int textIndex = firstCommandIndex(longDrawList, DrawCommandType.TEXT, 0);
        int longCaretIndex = lastCommandIndex(longDrawList, DrawCommandType.RECT);
        expect(firstCommandIndex(longDrawList, DrawCommandType.PUSH_CLIP, 0) < textIndex
                        && firstCommandIndex(longDrawList, DrawCommandType.POP_CLIP, textIndex) > textIndex,
                "TextInput should clip long text inside its text viewport");
        expect(longCaretIndex >= 0 && longDrawList.commands().get(longCaretIndex).bounds().x() <= 46.0f,
                "TextInput should horizontally scroll to keep the caret inside the viewport");

        TextField shiftField = new TextField("abcd");
        shiftField.setUiContextInternal(uiContext);
        shiftField.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 18.0f));
        uiContext.focusManager().requestFocus(shiftField);
        shiftField.cursorIndex(2);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(shiftField, KeyCodes.RIGHT, 0, KeyModifiers.SHIFT));
        uiContext.routedEvents().dispatch(new KeyPressedEvent(shiftField, KeyCodes.RIGHT, 0, KeyModifiers.SHIFT));
        expect(shiftField.hasSelection() && shiftField.selectedText().equals("cd") && shiftField.cursorIndex() == 4,
                "Shift+Right should extend TextInput selection from the caret");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(shiftField, KeyCodes.LEFT, 0, KeyModifiers.SHIFT));
        expect(shiftField.hasSelection() && shiftField.selectedText().equals("c") && shiftField.cursorIndex() == 3,
                "Shift+Left should contract TextInput selection toward the anchor");

        TextField dragField = new TextField("abcdef");
        dragField.setUiContextInternal(uiContext);
        dragField.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));
        DrawList dragWarmup = new DrawList();
        dragField.render(new DefaultRenderContext(dragWarmup));
        uiContext.routedEvents().dispatch(new PointerPressedEvent(dragField, 4.0f, 8.0f, 4.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerMovedEvent(dragField, 22.0f, 8.0f, 22.0f, 8.0f, 0));
        expect(dragField.hasSelection() && dragField.selectedText().equals("abc"),
                "Dragging inside TextInput should select the covered character range");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(dragField, 22.0f, 8.0f, 22.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(uiContext.capturedPointer(0) == null, "TextInput should release pointer capture after drag selection");

        dragField.select(0, 2);
        uiContext.focusManager().clearFocus();
        expect(!dragField.focused() && !dragField.hasSelection(), "TextInput should clear selection when focus is lost");
    }

    private void testTextAreaMultilineEditingAndScrolling() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TextArea area = new TextArea().placeholder("XML").visibleLines(2);
        area.setUiContextInternal(uiContext);
        area.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 36.0f));

        Counter textChanges = new Counter();
        area.onTextChanged(event -> {
            textChanges.count++;
            textChanges.lastText = event.newText();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(area, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(area.focused() && uiContext.focusManager().focusedWidget() == area,
                "TextArea should focus on primary pointer press");
        uiContext.routedEvents().dispatch(new TextInputEvent(area, '<', 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(area, 'A', 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(area, '>', 0));
        uiContext.routedEvents().dispatch(new KeyPressedEvent(area, KeyCodes.ENTER, 0, 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(area, 'x', 0));
        expect(area.text().equals("<A>\nx"), "TextArea should preserve newline-aware editing");
        expect(textChanges.count == 5 && textChanges.lastText.equals("<A>\nx"),
                "TextArea should emit text changed events for multiline edits");

        area.text("alpha\nbeta\ngamma").cursorIndex(area.text().length());
        uiContext.routedEvents().dispatch(new KeyPressedEvent(area, KeyCodes.UP, 0, 0));
        expect(area.cursorIndex() == 10, "TextArea Up should keep visual column when moving to a shorter line");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(area, KeyCodes.HOME, 0, 0));
        expect(area.cursorIndex() == 6, "TextArea Home should move to the current line start");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(area, KeyCodes.END, 0, 0));
        expect(area.cursorIndex() == 10, "TextArea End should move to the current line end");

        area.select(0, 5);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(area, KeyCodes.C, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("alpha"), "TextArea Ctrl+C should copy selected text");
        uiContext.clipboard().setText("<VBox>\r\n</VBox>");
        area.selectAll();
        uiContext.routedEvents().dispatch(new KeyPressedEvent(area, KeyCodes.V, 0, KeyModifiers.CONTROL));
        expect(area.text().equals("<VBox>\n</VBox>"), "TextArea paste should normalize and preserve line breaks");

        area.text("one\ntwo\nthree\nfour\nfive").cursorIndex(area.text().length());
        DrawList drawList = new DrawList();
        area.render(new DefaultRenderContext(drawList));
        expect(area.scrollY() > 0.0f, "TextArea render should scroll vertically to keep the caret visible");
        expect(firstCommandIndex(drawList, DrawCommandType.PUSH_CLIP, 0) >= 0 && hasText(drawList, "five"),
                "TextArea should clip and render visible multiline content");

        area.scrollTo(0.0f, 0.0f);
        uiContext.routedEvents().dispatch(new ScrollEvent(area, 8.0f, 8.0f, 8.0f, 8.0f, 0.0f, -1.0f));
        expect(area.scrollY() > 0.0f, "TextArea should handle wheel scrolling");

        expect(Widgets.textArea("factory") instanceof TextArea, "Widgets.textArea should create TextArea instances");
    }

    private void testCodeEditorDecorationsAndDirtyState() {
        CodeEditor editor = new CodeEditor();
        editor.visibleLines(2);
        editor.loadText("one\ntwo\nthree");
        editor.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 40.0f));
        expect(!editor.dirty(), "CodeEditor loadText should establish a clean snapshot");
        editor.text("one\ntwo changed\nthree");
        expect(editor.dirty(), "CodeEditor should track dirty state after edits");
        editor.markClean();
        expect(!editor.dirty(), "CodeEditor markClean should clear dirty state");

        editor.diagnostic(CodeEditor.Severity.ERROR, 2, 2, "Bad token");
        DrawList drawList = new DrawList();
        editor.render(new DefaultRenderContext(drawList));
        expect(hasText(drawList, "1") && hasText(drawList, "2"),
                "CodeEditor should render line numbers in the gutter");
        expect(hasFillColor(drawList, 1.0f, 0.25f, 0.25f, 0.92f),
                "CodeEditor should render diagnostic underline markers");

        editor.scrollToLine(3);
        expect(editor.scrollY() > 0.0f, "CodeEditor scrollToLine should move the viewport to the requested line");
        expect(Widgets.codeEditor("factory") instanceof CodeEditor, "Widgets.codeEditor should create CodeEditor instances");
    }

    private void testEditorCommandManagerAndKeyBindings() {
        CommandManager manager = new CommandManager();
        boolean[] enabled = {true};
        boolean[] checked = {false};
        Counter executions = new Counter();
        Counter commandChanges = new Counter();
        Counter managerChanges = new Counter();

        EditorCommand save = EditorCommand.of("project.save", "Save", () -> executions.count++)
                .enabledWhen(() -> enabled[0])
                .checkedWhen(() -> checked[0]);
        save.onChanged(event -> {
            commandChanges.count++;
            commandChanges.lastText = event.commandId();
            commandChanges.lastOldText = event.kind().name();
        });
        manager.onChanged(event -> {
            managerChanges.count++;
            managerChanges.lastText = event.commandId();
            managerChanges.lastOldText = event.kind().name();
        });

        manager.register(save);
        expect(manager.commands().size() == 1 && manager.requireCommand("project.save") == save,
                "CommandManager should register commands by stable id");
        expect(managerChanges.count == 1 && managerChanges.lastOldText.equals(CommandChangedEvent.Kind.REGISTERED.name()),
                "CommandManager should emit a registration change event");

        save.label("Save Project");
        expect(save.label().equals("Save Project")
                        && commandChanges.lastText.equals("project.save")
                        && managerChanges.lastOldText.equals(CommandChangedEvent.Kind.UPDATED.name()),
                "EditorCommand state changes should notify direct and manager listeners");

        expect(save.enabled() && !save.checked(), "EditorCommand should read enabled and checked suppliers");
        expect(manager.execute("project.save") && executions.count == 1,
                "CommandManager execute should run enabled commands by id");
        enabled[0] = false;
        save.notifyChanged();
        expect(!manager.execute("project.save") && executions.count == 1,
                "CommandManager execute should ignore disabled commands");

        KeyBinding saveShortcut = KeyBinding.ctrl('s');
        expect(saveShortcut.keyCode() == 'S'
                        && saveShortcut.modifiers() == KeyModifiers.CONTROL
                        && saveShortcut.shortcutText().equals("Ctrl+S"),
                "KeyBinding should normalize letter keys and expose shortcut text");
        manager.bind("project.save", saveShortcut);
        expect(save.shortcutText().equals("Ctrl+S") && manager.keyBindings("project.save").size() == 1,
                "CommandManager bind should expose shortcut text on the command");

        enabled[0] = true;
        checked[0] = true;
        save.notifyChanged();
        expect(save.checked(), "EditorCommand checked supplier should update observable command state");
        expect(!manager.handleKey('S', KeyModifiers.CONTROL | KeyModifiers.SHIFT) && executions.count == 1,
                "CommandManager shortcuts should require exact modifiers");
        expect(manager.handleKey('S', KeyModifiers.CONTROL) && executions.count == 2,
                "CommandManager should dispatch matching keyboard shortcuts");

        KeyPressedEvent keyEvent = new KeyPressedEvent(new Button(), 'S', 0, KeyModifiers.CONTROL);
        expect(manager.handleKey(keyEvent) && keyEvent.isCancelled() && executions.count == 3,
                "CommandManager should cancel handled key events");
        expect(manager.unbind("project.save", saveShortcut)
                        && !manager.handleKey('S', KeyModifiers.CONTROL)
                        && manager.remove("project.save"),
                "CommandManager should unbind shortcuts and remove commands cleanly");
    }

    private void testMenuBarCommandBackedPopups() {
        CommandManager manager = new CommandManager();
        Counter commandRuns = new Counter();
        Counter actionRuns = new Counter();
        boolean[] saveEnabled = {true};
        boolean[] propertiesChecked = {true};

        EditorCommand save = EditorCommand.of("project.save", "Save Project", () -> commandRuns.count++)
                .enabledWhen(() -> saveEnabled[0]);
        EditorCommand properties = EditorCommand.of("view.properties.toggle", "Properties", () -> commandRuns.started++)
                .checkedWhen(() -> propertiesChecked[0]);
        manager.register(save).bind("project.save", KeyBinding.ctrl('s'));
        manager.register(properties);

        MenuBar menuBar = Widgets.menuBar().commandManager(manager);
        Menu recent = new Menu("Recent").action("Project A", () -> actionRuns.started++);
        menuBar.menu(new Menu("File")
                .command("project.save")
                .separator()
                .action("Close", () -> actionRuns.count++)
                .submenu(recent));
        menuBar.menu(new Menu("View").command("view.properties.toggle"));

        DefaultUIContext uiContext = new DefaultUIContext();
        menuBar.setUiContextInternal(uiContext);
        menuBar.measure(new LayoutContext(320.0f, 80.0f));
        menuBar.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 22.0f));

        expect(menuBar.menuCount() == 2 && menuBar.children().size() == 2,
                "MenuBar should materialize top-level menu headers as retained buttons");
        menuBar.openMenu(0);
        ContextMenu filePopup = menuBar.openedPopup();
        expect(filePopup != null && filePopup.opened() && filePopup.itemCount() == 3,
                "MenuBar should open a ContextMenu popup for a top-level menu");
        expect(filePopup.itemButton(0).text().contains("Save Project")
                        && filePopup.itemButton(0).text().contains("Ctrl+S"),
                "MenuBar command rows should resolve command labels and shortcut text");
        expect(filePopup.itemButton(2).text().contains("Recent >"),
                "MenuBar should materialize nested submenus");
        filePopup.itemButton(0).click();
        expect(commandRuns.count == 1, "MenuBar command menu items should execute through CommandManager");

        menuBar.openMenu(0);
        menuBar.openedPopup().itemButton(1).click();
        expect(actionRuns.count == 1, "MenuBar action menu items should run direct callbacks");

        menuBar.openMenu(0);
        saveEnabled[0] = false;
        save.notifyChanged();
        expect(!menuBar.openedPopup().itemButton(0).enabled(),
                "MenuBar should refresh open popups when command enabled state changes");

        menuBar.openMenu(1);
        expect(menuBar.openedPopup().itemButton(0).text().startsWith("[x] Properties"),
                "MenuBar should render checked command state in menu rows");

        MenuBar overlayMenuBar = Widgets.menuBar().commandManager(manager);
        overlayMenuBar.menu(new Menu("File").command("project.save"));
        overlayMenuBar.menu(new Menu("View").command("view.properties.toggle"));
        OverlayLayer overlayLayer = new OverlayLayer(overlayMenuBar);
        overlayLayer.applyQueuedMutations();
        overlayLayer.measure(new LayoutContext(320.0f, 120.0f));
        overlayLayer.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 120.0f));
        overlayMenuBar.openMenu(0);
        overlayLayer.applyQueuedMutations();
        ContextMenu overlayPopup = overlayMenuBar.openedPopup();
        expect(overlayPopup != null && overlayPopup.opened() && overlayPopup.parent() == overlayLayer,
                "MenuBar popups should attach to the nearest overlay layer when available");
        overlayLayer.handle(new PointerPressedEvent(
                overlayLayer,
                overlayLayer,
                EventPhase.CAPTURE,
                8.0f,
                48.0f,
                8.0f,
                48.0f,
                7,
                PointerButton.PRIMARY));
        expect(!overlayPopup.opened(),
                "OverlayLayer outside clicks should close MenuBar context menus");
        properties.notifyChanged();
        expect(overlayMenuBar.openedMenuIndex() == -1 && overlayMenuBar.openedPopup() == null,
                "MenuBar should drop stale open state after an overlay-level outside close");
        Widget viewHeader = overlayMenuBar.children().get(1);
        viewHeader.handle(new PointerEnteredEvent(viewHeader, 42.0f, 8.0f, 4.0f, 4.0f, 8));
        expect(overlayMenuBar.openedPopup() == null,
                "MenuBar hover traversal should not reopen a menu after outside close has disarmed the menu bar");

        expect(Widgets.menuBar() instanceof MenuBar, "Widgets.menuBar should create MenuBar instances");
    }

    private void testToolBarCommandButtonsAndDisplayModes() {
        CommandManager manager = new CommandManager();
        Counter commandRuns = new Counter();
        boolean[] saveEnabled = {true};
        boolean[] playChecked = {false};

        EditorCommand save = EditorCommand.of("project.save", "Save Project", () -> commandRuns.count++)
                .enabledWhen(() -> saveEnabled[0]);
        EditorCommand play = new EditorCommand("run.play", "Play").checkedWhen(() -> playChecked[0]);
        play.action(() -> {
            playChecked[0] = !playChecked[0];
            commandRuns.started++;
            play.notifyChanged();
        });
        manager.register(save).bind("project.save", KeyBinding.ctrl('s'));
        manager.register(play);

        ToolBar toolbar = Widgets.toolBar().commandManager(manager);
        ToolButton saveButton = toolbar.command("project.save", "S", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolbar.separator();
        IconButton missingButton = toolbar.iconCommand("run.stop", "X");
        toolbar.spacer();
        ToggleToolButton playButton = toolbar.toggleCommand("run.play", "P", ToolButton.DisplayMode.ICON_ONLY);
        toolbar.measure(new LayoutContext(320.0f, 40.0f));
        toolbar.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 26.0f));

        expect(toolbar.children().size() == 5,
                "ToolBar should compose command buttons, separators and flexible spacer widgets");
        expect(saveButton.text().equals("S Save Project")
                        && saveButton.tooltip().equals("Save Project (Ctrl+S)")
                        && saveButton.enabled(),
                "ToolBar command buttons should resolve command labels, shortcuts and enabled state");
        expect(missingButton.text().equals("X") && !missingButton.enabled(),
                "ToolBar should disable buttons whose command id is not registered");
        expect(playButton.text().equals("P") && !playButton.checked(),
                "ToggleToolButton should support icon-only toolbar display mode");

        saveButton.click();
        expect(commandRuns.count == 1, "ToolBar command buttons should execute through CommandManager");
        saveEnabled[0] = false;
        save.notifyChanged();
        saveButton.click();
        expect(!saveButton.enabled() && commandRuns.count == 1,
                "ToolBar should refresh disabled command buttons and avoid executing them");

        playButton.click();
        expect(commandRuns.started == 1 && playButton.checked(),
                "ToggleToolButton should reflect checked command state after command execution");
        expect(Widgets.toolBar() instanceof ToolBar
                        && Widgets.toolButton("Save") instanceof ToolButton
                        && Widgets.iconButton("S") instanceof IconButton
                        && Widgets.toggleToolButton("Run") instanceof ToggleToolButton,
                "Widgets factories should expose toolbar primitives");
    }

    private void testPropertyGridContracts() {
        XmlWidgetElement buttonElement = new XmlWidgetElement("Button")
                .attribute("id", "saveButton")
                .attribute("text", "Save")
                .attribute("enabled", "false")
                .attribute("width", "120")
                .attribute("backgroundColor", "not-a-color");

        PropertyGrid grid = Widgets.propertyGrid(buttonElement);
        expect(grid.inspection() != null && grid.inspection().knownWidget(),
                "PropertyGrid should inspect selected XML widget metadata");
        expect(grid.row("text").orElseThrow().fieldKind() == PropertyFieldRow.FieldKind.STRING
                        && grid.row("enabled").orElseThrow().fieldKind() == PropertyFieldRow.FieldKind.BOOLEAN
                        && grid.row("width").orElseThrow().fieldKind() == PropertyFieldRow.FieldKind.SIZE_VALUE
                        && grid.row("backgroundColor").orElseThrow().fieldKind() == PropertyFieldRow.FieldKind.COLOR,
                "PropertyGrid should infer reusable property field types from descriptors and values");
        expect(grid.row("backgroundColor").orElseThrow().validationState() == PropertyFieldRow.ValidationState.WARNING,
                "PropertyGrid rows should expose validation state for suspicious values");
        expect(grid.categories().stream().anyMatch(category -> category.name().equals("Content"))
                        && grid.categories().stream().anyMatch(category -> category.name().equals("Behavior"))
                        && grid.row("radius").orElseThrow().present() == false,
                "PropertyGrid should group rows by category and include unset descriptor attributes");

        int radiusIndexBefore = propertyRowIndex(grid, "radius");
        String radiusPreviousName = grid.rows().get(radiusIndexBefore - 1).name();
        String radiusNextName = grid.rows().get(radiusIndexBefore + 1).name();
        grid.setAttributeValue("radius", "6");
        int radiusIndexAfter = propertyRowIndex(grid, "radius");
        expect(radiusIndexAfter == radiusIndexBefore
                        && grid.row("radius").orElseThrow().present()
                        && grid.rows().get(radiusIndexAfter - 1).name().equals(radiusPreviousName)
                        && grid.rows().get(radiusIndexAfter + 1).name().equals(radiusNextName),
                "PropertyGrid should keep descriptor rows in place after writing an unset attribute");

        Counter changes = new Counter();
        grid.onAttributeChanged(change -> {
            changes.count++;
            changes.lastText = change.attributeName();
            changes.lastOldText = change.oldValue();
        });
        grid.setAttributeValue("text", "Apply");
        expect(buttonElement.attribute("text").orElseThrow().equals("Apply")
                        && changes.count == 1
                        && changes.lastText.equals("text")
                        && changes.lastOldText.equals("Save"),
                "PropertyGrid should write changed attribute values back to the XML element");

        grid.resetAttribute("enabled");
        expect(buttonElement.attribute("enabled").orElseThrow().equals("true"),
                "PropertyGrid reset should restore descriptor default values");
        grid.removeAttribute("width");
        expect(buttonElement.attribute("width").isEmpty(),
                "PropertyGrid remove should delete attributes from the XML element");

        PropertyFieldRow booleanRow = Widgets.propertyFieldRow(XmlAttributeDescriptor.of("visible").defaultValue("true"), "false", true);
        PropertyFieldRow invalidNumber = Widgets.propertyFieldRow(XmlAttributeDescriptor.of("opacity").defaultValue("1"), "abc", true);
        expect(booleanRow.fieldKind() == PropertyFieldRow.FieldKind.BOOLEAN
                        && booleanRow.editor() instanceof Checkbox
                        && invalidNumber.validationState() == PropertyFieldRow.ValidationState.ERROR
                        && Widgets.propertyGrid() instanceof PropertyGrid,
                "Widgets factories should expose reusable property grid and property field rows");

        PropertyFieldRow enumRow = Widgets.propertyFieldRow(
                XmlAttributeDescriptor.of("orientation").defaultValue("horizontal"),
                "vertical",
                true);
        VBox propertyPanel = new VBox();
        propertyPanel.addChild(enumRow);
        OverlayLayer propertyOverlay = new OverlayLayer(propertyPanel);
        propertyOverlay.measure(new LayoutContext(240.0f, 120.0f));
        propertyOverlay.arrange(new MutableRect(0.0f, 0.0f, 240.0f, 120.0f));
        ComboBox enumCombo = (ComboBox) enumRow.editor();
        enumCombo.open();
        propertyOverlay.measure(new LayoutContext(240.0f, 120.0f));
        propertyOverlay.arrange(new MutableRect(0.0f, 0.0f, 240.0f, 120.0f));
        DrawList enumOverlayDrawList = new DrawList();
        propertyOverlay.render(new DefaultRenderContext(enumOverlayDrawList));
        expect(enumRow.fieldKind() == PropertyFieldRow.FieldKind.ENUM
                        && enumCombo.dropDownMode() == ComboBox.DropDownMode.OVERLAY
                        && enumCombo.attachedOverlayLayer() == propertyOverlay
                        && enumCombo.dropDownPopup().parent() == propertyOverlay
                        && enumCombo.dropDownPopup().opened()
                        && propertyOverlay.children().get(propertyOverlay.children().size() - 1) == enumCombo.dropDownPopup()
                        && hasText(enumOverlayDrawList, "horizontal"),
                "PropertyFieldRow enum editors should open dropdown options through the top overlay layer");
    }

    private void testSelectionOverlayContracts() {
        XmlWidgetDocument document = XmlWidgetDocument.of(new XmlWidgetElement("VBox")
                .attribute("x", "0")
                .attribute("y", "0")
                .attribute("width", "200")
                .attribute("height", "120")
                .addElement(new XmlWidgetElement("Button")
                        .attribute("x", "10")
                        .attribute("y", "12")
                        .attribute("width", "80")
                        .attribute("height", "20")));
        SelectionOverlay overlay = Widgets.selectionOverlay()
                .document(document)
                .selectedPath(XmlWidgetNodePath.of(0));
        XmlWidgetLayoutFrame selected = overlay.selectedFrame().orElseThrow();
        expect(near(selected.x(), 10.0f)
                        && near(selected.y(), 12.0f)
                        && near(selected.width(), 80.0f)
                        && near(selected.height(), 20.0f),
                "SelectionOverlay should resolve selected XML source frames");
        expect(overlay.pathAt(15.0f, 15.0f).filter(XmlWidgetNodePath.of(0)::equals).isPresent(),
                "SelectionOverlay hit-test should prefer the deepest framed XML element");
        expect(overlay.handleAt(90.0f, 32.0f).filter(XmlWidgetLayoutHandle.SOUTH_EAST::equals).isPresent(),
                "SelectionOverlay should expose resize handles around the selected frame");
        expect(Widgets.designCanvasOverlay() instanceof DesignCanvasOverlay,
                "Widgets factories should expose the design canvas overlay alias");

        DefaultUIContext uiContext = new DefaultUIContext();
        overlay.setUiContextInternal(uiContext);
        overlay.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 120.0f));
        Counter changes = new Counter();
        overlay.onDocumentChanged(change -> {
            changes.count++;
            if (change.finalChange()) {
                changes.committed++;
            } else {
                changes.started++;
            }
            changes.lastChecked = change.finalChange();
            changes.lastValue = change.deltaX();
            changes.lastNewHeight = change.deltaY();
        });
        overlay.handle(new PointerPressedEvent(overlay, 15.0f, 15.0f, 15.0f, 15.0f, 2, PointerButton.PRIMARY));
        expect(uiContext.capturedPointer(2) == overlay,
                "SelectionOverlay should capture the active pointer while dragging source frames");
        overlay.handle(new PointerMovedEvent(overlay, 25.0f, 21.0f, 25.0f, 21.0f, 2));
        overlay.handle(new PointerReleasedEvent(overlay, 25.0f, 21.0f, 25.0f, 21.0f, 2, PointerButton.PRIMARY));
        XmlWidgetElement movedButton = overlay.document().root().elementChildren().get(0);
        expect(overlay.lastResult().orElseThrow().valid()
                        && movedButton.attribute("x").orElseThrow().equals("20")
                        && movedButton.attribute("y").orElseThrow().equals("18")
                        && changes.count == 2
                        && changes.started == 1
                        && changes.committed == 1
                        && changes.lastChecked
                        && near(changes.lastValue, 10.0f)
                        && near(changes.lastNewHeight, 6.0f)
                        && uiContext.capturedPointer(2) == null,
                "SelectionOverlay drag should publish live and final XML frame changes and release pointer capture");

        overlay.handle(new PointerMovedEvent(overlay, 1.0f, 1.0f, 1.0f, 1.0f, 3));
        DrawList drawList = new DrawList();
        overlay.render(new DefaultRenderContext(drawList));
        expect(countCommands(drawList, DrawCommandType.RECT) + countCommands(drawList, DrawCommandType.ROUNDED_RECT) >= 10
                        && drawList.commands().stream().anyMatch(command -> command.paint() != null && command.paint().dashed()),
                "SelectionOverlay should render hover outline, selected outline and transform handles");
    }

    private void testPaneVisibilityControllerContracts() {
        DockingRoot root = new DockingRoot();
        DockPane scene = DockPane.document("scene", "Scene", testDockContent("Scene canvas"));
        DockPane properties = DockPane.tool("properties", "Properties", testDockContent("Property grid"));
        DockPane diagnostics = DockPane.tool("diagnostics", "Diagnostics", testDockContent("Errors"));
        PaneVisibilityController controller = new PaneVisibilityController(root);
        Counter visibilityChanges = new Counter();
        controller.onVisibilityChanged(change -> {
            visibilityChanges.count++;
            visibilityChanges.lastText = change.paneId();
            visibilityChanges.lastChecked = change.visible();
            visibilityChanges.lastOldText = change.reason().name();
        });

        controller.registerPane(scene, DockArea.CENTER)
                .registerPane(properties, DockArea.RIGHT, "scene", true)
                .registerPane(diagnostics, DockArea.BOTTOM, "scene", false)
                .diagnosticsPaneId("diagnostics");
        expect(root.manager().containsPane("scene")
                        && root.manager().containsPane("properties")
                        && !root.manager().containsPane("diagnostics")
                        && controller.visiblePaneIds().contains("scene")
                        && controller.hiddenPaneIds().contains("diagnostics"),
                "PaneVisibilityController should register visible panes and keep hidden panes available for later restore");

        expect(controller.pinned("properties", false)
                        && !controller.pinned("properties")
                        && !properties.pinned(),
                "PaneVisibilityController should synchronize pin state with DockPane metadata");
        expect(controller.hidePane("properties")
                        && !controller.isVisible("properties")
                        && !root.manager().containsPane("properties")
                        && visibilityChanges.lastText.equals("properties")
                        && !visibilityChanges.lastChecked,
                "PaneVisibilityController hide should detach panes from the docking workspace");
        expect(controller.showPane("properties")
                        && controller.isVisible("properties")
                        && root.manager().containsPane("properties")
                        && !properties.pinned(),
                "PaneVisibilityController show should restore panes using their preferred dock area and pin state");

        CommandManager commands = new CommandManager();
        controller.bindViewCommands(commands, "view.");
        String propertiesCommand = controller.viewCommandId("properties");
        expect(commands.command(propertiesCommand).orElseThrow().checked(),
                "PaneVisibilityController should expose checked View-menu toggle commands for visible panes");
        expect(commands.execute(propertiesCommand)
                        && !controller.isVisible("properties")
                        && !commands.command(propertiesCommand).orElseThrow().checked(),
                "View-menu toggle command should hide a visible pane");
        expect(commands.execute(propertiesCommand)
                        && controller.isVisible("properties")
                        && commands.command(propertiesCommand).orElseThrow().checked(),
                "View-menu toggle command should restore a hidden pane");

        XmlWidgetDiagnosticsModel errors = XmlWidgetDiagnosticsModel.errors(java.util.List.of(
                new XmlWidgetDiagnostic("Broken XML")));
        expect(controller.updateDiagnostics(errors)
                        && controller.isVisible("diagnostics")
                        && root.manager().selectedPane().id().equals("diagnostics"),
                "PaneVisibilityController should auto-open the diagnostics pane when XML errors appear");
        expect(!controller.updateDiagnostics(XmlWidgetDiagnosticsModel.empty()),
                "PaneVisibilityController should not hide diagnostics automatically when errors are cleared");
        controller.hidePane("diagnostics");
        controller.autoOpenDiagnosticsOnErrors(false);
        expect(!controller.updateDiagnostics(errors) && !controller.isVisible("diagnostics"),
                "PaneVisibilityController should respect disabled diagnostics auto-open behavior");

        PaneVisibilityController.PaneState propertiesState = controller.panes().stream()
                .filter(state -> state.paneId().equals("properties"))
                .findFirst()
                .orElseThrow();
        expect(!propertiesState.pinned()
                        && propertiesState.preferredArea() == DockArea.RIGHT
                        && propertiesState.kind() == DockPaneKind.TOOL,
                "PaneVisibilityController state snapshots should support View menu and workspace restore UI");
    }

    private void testProjectPickerPanelContracts() {
        ProjectPickerPanel panel = Widgets.projectPicker()
                .maxRecentProjects(2)
                .currentProject(ProjectPickerPanel.ProjectReference.path(
                        "shop", "Shop UI", "E:/projects/shop.xml"))
                .dirty(true);
        panel.addRecentProject(ProjectPickerPanel.ProjectReference.resource(
                "builtin", "Builtin Demo", "unigui:demo"));
        panel.addRecentProject(ProjectPickerPanel.ProjectReference.path(
                "settings", "Settings UI", "E:/projects/settings.xml"));
        expect(panel instanceof ProjectPickerPanel
                        && panel.recentProjects().size() == 2
                        && panel.currentProjectLabel().text().contains("Shop UI *")
                        && panel.currentProjectLabel().text().contains("E:/projects/shop.xml")
                        && panel.recentList().children().size() == 2,
                "ProjectPickerPanel should show current project path/resource labels and a capped recent project list");

        Counter actions = new Counter();
        Counter prompts = new Counter();
        panel.onProjectAction(action -> {
            actions.count++;
            actions.lastText = action.kind().name();
            actions.lastOldText = action.path();
            actions.lastChecked = action.fromRecent();
        });
        panel.unsavedChangePrompt(action -> {
            prompts.count++;
            return false;
        });
        expect(!panel.requestNewProject()
                        && actions.count == 0
                        && prompts.count == 1,
                "ProjectPickerPanel should allow hosts to cancel destructive actions when unsaved changes exist");

        panel.unsavedChangePrompt(action -> {
            prompts.count++;
            return true;
        });
        expect(panel.requestOpenProject("E:/projects/new.xml")
                        && actions.count == 1
                        && actions.lastText.equals("OPEN_PROJECT")
                        && actions.lastOldText.equals("E:/projects/new.xml"),
                "ProjectPickerPanel should emit open callbacks after unsaved-change confirmation");
        expect(panel.openRecentProject("settings")
                        && actions.count == 2
                        && actions.lastChecked
                        && panel.currentProject().orElseThrow().id().equals("settings"),
                "ProjectPickerPanel should open recent projects through the same confirmed callback path");

        CommandManager commands = new CommandManager();
        panel.registerCommands(commands);
        expect(commands.command(ProjectPickerPanel.COMMAND_NEW_PROJECT).isPresent()
                        && commands.command(ProjectPickerPanel.COMMAND_OPEN_PROJECT).isPresent()
                        && commands.command(ProjectPickerPanel.COMMAND_SAVE_PROJECT).orElseThrow().enabled()
                        && commands.command(ProjectPickerPanel.COMMAND_LAST_PROJECTS).orElseThrow().enabled(),
                "ProjectPickerPanel should register toolbar command ids for new/open/save/recent project actions");
        expect(commands.execute(ProjectPickerPanel.COMMAND_SAVE_PROJECT)
                        && actions.lastText.equals("SAVE_PROJECT")
                        && !panel.dirty(),
                "ProjectPickerPanel save command should emit save and clear dirty state");
        expect(commands.execute(ProjectPickerPanel.COMMAND_LAST_PROJECTS)
                        && actions.lastText.equals("LAST_PROJECTS"),
                "ProjectPickerPanel recent command should notify hosts to show the recent project surface");
    }

    private void testStatusBarContracts() {
        StatusBar statusBar = Widgets.statusBar()
                .dirty(true)
                .mode("XML")
                .selectedNodePath(XmlWidgetNodePath.of(0, 2))
                .viewScale(1.25f)
                .diagnostics(XmlWidgetDiagnosticsModel.errors(java.util.List.of(
                        new XmlWidgetDiagnostic("Broken XML"))));
        expect(statusBar.children().size() == 5
                        && statusBar.dirtyLabel().text().equals("Unsaved *")
                        && statusBar.modeLabel().text().equals("Mode: XML")
                        && statusBar.diagnosticsLabel().text().equals("Errors: 1, Warnings: 0")
                        && statusBar.selectedPathLabel().text().contains("/0/2")
                        && statusBar.scaleLabel().text().equals("Scale: 125%"),
                "StatusBar should summarize dirty state, mode, diagnostics, selection and canvas scale");

        statusBar.dirty(false).selectedNodePath("").errorCount(0).warningCount(2).viewScale(-1.0f);
        expect(statusBar.dirtyLabel().text().equals("Saved")
                        && statusBar.selectedPathLabel().text().equals("Selection: none")
                        && statusBar.diagnosticsLabel().text().equals("Errors: 0, Warnings: 2")
                        && statusBar.scaleLabel().text().equals("Scale: 100%"),
                "StatusBar should normalize empty selection, manual diagnostics counts and invalid scale values");

        expect(Widgets.diagnosticsStrip().mode().equals("Diagnostics"),
                "DiagnosticsStrip should be a compact status bar alias for diagnostics-focused footers");
    }

    private void testWidgetPaletteContracts() {
        WidgetPalette palette = Widgets.widgetPalette()
                .selectedCategory("Controls")
                .search("button");
        expect(palette.categories().contains("Controls")
                        && !palette.categories().contains("Editor")
                        && palette.visibleItems().stream().anyMatch(item -> item.xmlName().equals("Button"))
                        && palette.visibleItems().stream().noneMatch(item -> item.category().equals("Display"))
                        && palette.categoryList().children().size() == palette.categories().size()
                        && !palette.itemList().children().isEmpty(),
                "WidgetPalette should expose searchable descriptor-backed items grouped by category");
        expect(!palette.includeInternalWidgets()
                        && palette.visibleItems().stream().noneMatch(item -> item.category().equals("Editor"))
                        && !palette.selectWidget("WidgetPalette"),
                "WidgetPalette should hide editor-only widgets from the user-facing palette by default");

        WidgetPalette editorPalette = Widgets.widgetPalette()
                .includeInternalWidgets(true)
                .selectedCategory("Editor")
                .search("palette");
        expect(editorPalette.includeInternalWidgets()
                        && editorPalette.categories().contains("Editor")
                        && editorPalette.visibleItems().stream().anyMatch(item -> item.xmlName().equals("WidgetPalette"))
                        && editorPalette.selectWidget("WidgetPalette"),
                "WidgetPalette should allow explicit editor/internal widget exposure for editor layout demos");

        Counter selections = new Counter();
        Counter inserts = new Counter();
        WidgetPalette.PaletteInsertRequest[] lastRequest = new WidgetPalette.PaletteInsertRequest[1];
        palette.onSelectionChanged(change -> {
            selections.count++;
            selections.lastText = change.selectedXmlName();
            selections.lastOldText = change.previousXmlName();
        });
        palette.onInsertRequested(request -> {
            inserts.count++;
            inserts.lastText = request.descriptor().xmlName();
            inserts.lastOldText = request.parentPath().toString();
            inserts.lastRow = request.index();
            lastRequest[0] = request;
        });
        expect(palette.selectWidget("Button")
                        && selections.count == 1
                        && selections.lastText.equals("Button")
                        && palette.selectedDescriptor().orElseThrow().xmlName().equals("Button")
                        && palette.insertButton().enabled(),
                "WidgetPalette should track selected widget descriptors and enable insertion");
        expect(palette.requestInsertSelected(XmlWidgetNodePath.root(), 0)
                        && inserts.count == 1
                        && inserts.lastText.equals("Button")
                        && inserts.lastOldText.equals("/")
                        && inserts.lastRow == 0
                        && lastRequest[0].element().name().equals("Button"),
                "WidgetPalette should emit insert requests with target hierarchy path and XML element payload");

        XmlWidgetDocument document = XmlWidgetDocument.of(new XmlWidgetElement("VBox"));
        lastRequest[0].edit().apply(document);
        expect(document.root().elementChildren().size() == 1
                        && document.root().elementChildren().get(0).name().equals("Button"),
                "WidgetPalette insert requests should provide undoable document edits for hierarchy/canvas insertion");

        palette.search("not-a-widget");
        expect(palette.visibleItems().isEmpty()
                        && palette.itemList().children().size() == 1,
                "WidgetPalette should show an empty-state row when search filters out every descriptor");
        PalettePanel panel = Widgets.palettePanel();
        expect(panel instanceof WidgetPalette && panel.title().equals("Palette"),
                "PalettePanel should be a title-adjusted WidgetPalette alias for editor layouts");
    }

    private void testCommandPaletteContracts() {
        Counter commandRuns = new Counter();
        CommandManager commands = new CommandManager();
        commands.register(new EditorCommand("file.open", "Open Project")
                .shortcutText("Ctrl+O")
                .action(() -> commandRuns.count++));
        commands.register(new EditorCommand("file.save", "Save Project")
                .enabledWhen(() -> false)
                .action(() -> commandRuns.cancelled++));
        commands.register(new EditorCommand("view.palette.toggle", "Toggle Palette")
                .checkedWhen(() -> true)
                .action(() -> commandRuns.started++));

        CommandPalette palette = Widgets.commandPalette()
                .commandManager(commands)
                .search("project");
        expect(palette.visibleCommands().size() == 2
                        && palette.visibleCommands().stream().anyMatch(item -> item.id().equals("file.open") && item.enabled())
                        && palette.visibleCommands().stream().anyMatch(item -> item.id().equals("file.save") && !item.enabled())
                        && palette.visibleCommands().stream().noneMatch(item -> item.id().equals("view.palette.toggle"))
                        && palette.commandList().children().size() == 2,
                "CommandPalette should show searchable command rows backed by CommandManager state");

        Counter selections = new Counter();
        Counter invocations = new Counter();
        palette.onSelectionChanged(change -> {
            selections.count++;
            selections.lastText = change.selectedCommandId();
            selections.lastOldText = change.previousCommandId();
        });
        palette.onCommandInvoked(invocation -> {
            invocations.count++;
            invocations.lastText = invocation.commandId();
            invocations.lastChecked = invocation.executed();
        });
        expect(palette.selectCommand("file.open")
                        && selections.count == 1
                        && selections.lastText.equals("file.open")
                        && palette.executeButton().enabled(),
                "CommandPalette should track selected commands and enable execution only when runnable");
        expect(palette.executeSelected()
                        && commandRuns.count == 1
                        && invocations.count == 1
                        && invocations.lastText.equals("file.open")
                        && invocations.lastChecked,
                "CommandPalette should execute the selected command and notify host listeners");
        expect(!palette.executeCommand("file.save")
                        && commandRuns.cancelled == 0
                        && invocations.count == 2
                        && invocations.lastText.equals("file.save")
                        && !invocations.lastChecked,
                "CommandPalette should surface disabled command attempts without running their actions");

        palette.search("missing-command");
        expect(palette.visibleCommands().isEmpty()
                        && palette.commandList().children().size() == 1,
                "CommandPalette should show an empty-state row when no command matches the search");
    }

    private void testAssetBrowserPanelContracts() {
        XmlWidgetAssetCatalog catalog = XmlWidgetAssetCatalog.builder()
                .add(XmlWidgetAsset.texture("test:textures/ui/button.png", 64, 32)
                        .displayName("Button Texture")
                        .description("Nine-slice button background"))
                .add(XmlWidgetAsset.texture("minecraft:textures/block/stone.png", 16, 16)
                        .displayName("Stone"))
                .add(XmlWidgetAsset.font("minecraft:default").displayName("Default Font"))
                .add(XmlWidgetAsset.shader("unigui:glow").displayName("Glow Shader"))
                .build();
        AssetBrowserPanel browser = Widgets.assetBrowser()
                .catalog(catalog)
                .kind(XmlWidgetAssetKind.TEXTURE)
                .targetAttribute("backgroundTexture")
                .search("button");
        expect(browser.categories().size() == XmlWidgetAssetKind.values().length
                        && browser.categoryList().children().size() == XmlWidgetAssetKind.values().length
                        && browser.visibleAssets().size() == 1
                        && browser.visibleAssets().get(0).id().equals("test:textures/ui/button.png")
                        && browser.assetList().children().size() == 1
                        && browser.previewLabel().text().equals("No asset selected"),
                "AssetBrowserPanel should expose category buttons and searchable catalog-backed asset rows");

        Counter selections = new Counter();
        Counter applies = new Counter();
        browser.onAssetSelected(selection -> {
            selections.count++;
            selections.lastText = selection.asset().id();
            selections.lastOldText = selection.targetAttribute();
            selections.lastChecked = selection.applyRequested();
        });
        browser.onAssetApplied(selection -> {
            applies.count++;
            applies.lastText = selection.asset().id();
            applies.lastOldText = selection.targetAttribute();
            applies.lastChecked = selection.applyRequested();
        });
        expect(browser.selectAsset("test:textures/ui/button.png")
                        && selections.count == 1
                        && selections.lastText.equals("test:textures/ui/button.png")
                        && selections.lastOldText.equals("backgroundTexture")
                        && !selections.lastChecked
                        && browser.selectedAsset().orElseThrow().hasDimensions()
                        && browser.previewLabel().text().contains("64x32")
                        && browser.applyButton().enabled(),
                "AssetBrowserPanel should update preview and emit selection callbacks for catalog assets");
        expect(browser.requestApplySelected()
                        && applies.count == 1
                        && applies.lastText.equals("test:textures/ui/button.png")
                        && applies.lastOldText.equals("backgroundTexture")
                        && applies.lastChecked,
                "AssetBrowserPanel should emit apply callbacks for selecting an asset into the current property field");

        browser.kind(XmlWidgetAssetKind.FONT).search("");
        expect(browser.selectedAssetId().isEmpty()
                        && browser.visibleAssets().size() == 1
                        && browser.visibleAssets().get(0).kind() == XmlWidgetAssetKind.FONT
                        && !browser.applyButton().enabled(),
                "AssetBrowserPanel should switch asset categories and clear invalid selections");
        browser.search("missing");
        expect(browser.visibleAssets().isEmpty()
                        && browser.assetList().children().size() == 1,
                "AssetBrowserPanel should show an empty-state row when filters hide every asset");
    }

    private void testSearchBoxWithFilterChipsContracts() {
        SearchBoxWithFilterChips filters = Widgets.searchBoxWithFilterChips()
                .filters("errors:Errors|warnings:Warnings|assets:Assets")
                .activeFilters("errors,warnings")
                .search("button");
        expect(filters.filters().size() == 3
                        && filters.activeFilters().size() == 2
                        && filters.filterActive("errors")
                        && filters.filterActive("warnings")
                        && filters.search().equals("button")
                        && filters.chipRow().children().size() == 3,
                "SearchBoxWithFilterChips should parse XML-style filters and active chip state");

        Counter changes = new Counter();
        filters.onFilterChanged(change -> {
            changes.count++;
            changes.lastText = change.changedFilterId();
            changes.lastOldText = change.search();
            changes.lastChecked = change.active();
            changes.lastSelection = change.activeFilters().stream()
                    .map(String::length)
                    .toList();
        });
        expect(filters.toggleFilter("errors")
                        && !filters.filterActive("errors")
                        && changes.count == 1
                        && changes.lastText.equals("errors")
                        && !changes.lastChecked,
                "SearchBoxWithFilterChips should toggle active chips and emit filter changes");
        expect(filters.setFilterActive("assets", true)
                        && filters.filterActive("assets")
                        && changes.count == 2
                        && changes.lastText.equals("assets")
                        && changes.lastChecked,
                "SearchBoxWithFilterChips should allow direct filter activation");
        filters.search("texture");
        expect(changes.count == 3
                        && changes.lastText.isEmpty()
                        && changes.lastOldText.equals("texture"),
                "SearchBoxWithFilterChips should emit changes when the search query changes");
        filters.clearFilters();
        expect(filters.activeFilters().isEmpty()
                        && changes.count == 4,
                "SearchBoxWithFilterChips should clear active chips as a single filter change");
    }

    private void testDialogContracts() {
        Dialog dialog = Widgets.dialog()
                .title("Unsaved changes")
                .message("Save before closing?")
                .buttons("save:Save|discard:Discard|cancel:Cancel")
                .defaultResult("save")
                .cancelResult("cancel")
                .content(new Label("Project settings changed"));
        expect(dialog instanceof WindowWidget
                        && dialog.window() == dialog
                        && dialog.shellContent() == dialog.body()
                        && dialog.content() instanceof Label
                        && dialog.message().equals("Save before closing?")
                        && dialog.buttonRow().children().size() == 3
                        && dialog.modal()
                        && dialog.fixedModal()
                        && !dialog.resizable(),
                "Dialog should wrap a WindowWidget shell with message, content and standardized result buttons");

        Counter results = new Counter();
        dialog.onDialogResult(result -> {
            results.count++;
            results.lastText = result.resultId();
            results.lastOldText = result.label();
            results.lastChecked = result.defaultResult();
            if (result.cancelResult()) results.cancelled++;
        });
        dialog.open();
        expect(dialog.opened(), "Dialog should open through the inherited WindowWidget lifecycle");
        expect(dialog.requestDefault()
                        && results.count == 1
                        && results.lastText.equals("save")
                        && results.lastOldText.equals("Save")
                        && results.lastChecked
                        && !dialog.opened(),
                "Dialog should emit the configured default result and close by default");

        dialog.open();
        ((Button) dialog.buttonRow().children().get(1)).click();
        expect(results.count == 2
                        && results.lastText.equals("discard")
                        && results.lastOldText.equals("Discard")
                        && !results.lastChecked
                        && !dialog.opened(),
                "Dialog button controls should emit their result id and label");

        dialog.closeOnResult(false).open();
        expect(dialog.requestCancel()
                        && results.count == 3
                        && results.lastText.equals("cancel")
                        && results.cancelled == 1
                        && dialog.opened(),
                "Dialog should support non-closing result callbacks for host-driven validation");

        dialog.closeOnResult(true);
        dialog.closeButton().click();
        expect(results.count == 4
                        && results.lastText.equals("cancel")
                        && !dialog.opened(),
                "Dialog close button should emit the configured cancel result");

        OverlayLayer overlay = Widgets.overlayLayer();
        overlay.addOverlay(dialog);
        overlay.applyQueuedMutations();
        dialog.openModal();
        expect(overlay.windowManager().registered(dialog)
                        && overlay.windowManager().topModalWindow() == dialog,
                "Dialog should participate in OverlayLayer WindowManager modal registration");
    }

    private void testPaneHeaderContracts() {
        DockPane pane = DockPane.tool("properties", "Properties", new Label("Body"))
                .dirty(true)
                .pinned(false)
                .closable(true);
        PaneHeader header = Widgets.paneHeader()
                .pane(pane)
                .menuVisible(true);
        expect(header.paneId().equals("properties")
                        && header.title().equals("Properties")
                        && header.dirty()
                        && !header.pinned()
                        && header.closable()
                        && header.titleLabel().text().equals("Properties")
                        && header.dirtyLabel().visibility() == Visibility.VISIBLE
                        && header.pinButton().visible()
                        && header.menuButton().visible()
                        && header.closeButton().visible(),
                "PaneHeader should mirror DockPane title, dirty, pin and closable state into header controls");

        Counter actions = new Counter();
        header.onAction(action -> {
            actions.count++;
            actions.lastText = action.action().name();
            actions.lastOldText = action.paneId();
            actions.lastChecked = action.pinned();
        });
        expect(header.togglePinned()
                        && header.pinned()
                        && pane.pinned()
                        && actions.count == 1
                        && actions.lastText.equals(PaneHeader.Action.PIN_CHANGED.name())
                        && actions.lastOldText.equals("properties")
                        && actions.lastChecked,
                "PaneHeader should toggle pin state, sync the DockPane and emit pin callbacks");
        expect(header.requestMenu()
                        && actions.count == 2
                        && actions.lastText.equals(PaneHeader.Action.MENU_REQUESTED.name()),
                "PaneHeader should emit menu request callbacks");
        expect(header.requestClose()
                        && actions.count == 3
                        && actions.lastText.equals(PaneHeader.Action.CLOSE_REQUESTED.name()),
                "PaneHeader should emit close request callbacks");
        header.closable(false);
        expect(!header.requestClose()
                        && actions.count == 3
                        && !header.closeButton().visible()
                        && !pane.closable(),
                "PaneHeader should hide and suppress close requests when the pane is not closable");

        PaneHeader minimal = Widgets.paneHeader()
                .title("Hierarchy")
                .dirty(true)
                .pinVisible(false)
                .menuVisible(false)
                .closeVisible(false);
        expect(minimal.titleLabel().text().equals("Hierarchy")
                        && minimal.dirtyLabel().visibility() == Visibility.VISIBLE
                        && !minimal.pinButton().visible()
                        && !minimal.menuButton().visible()
                        && !minimal.closeButton().visible(),
                "PaneHeader should support minimal custom pane headers without action buttons");

        ResizablePanelHeader resizable = Widgets.resizablePanelHeader()
                .paneId("assets")
                .title("Assets")
                .sizeRange(120.0f, 300.0f)
                .panelSize(240.0f)
                .resizeEdge(ResizablePanelHeader.ResizeEdge.LEFT);
        Counter resizes = new Counter();
        resizable.onResizeRequested(request -> {
            resizes.count++;
            resizes.lastText = request.edge().name();
            resizes.lastValue = request.oldSize();
            resizes.lastNewWidth = request.newSize();
        });
        expect(resizable.requestResize(500.0f)
                        && resizes.count == 1
                        && resizes.lastText.equals("LEFT")
                        && near(resizes.lastValue, 240.0f)
                        && near(resizes.lastNewWidth, 300.0f)
                        && near(resizable.panelSize(), 300.0f),
                "ResizablePanelHeader should clamp resize requests and report old/new panel sizes");
        resizable.resizingEnabled(false);
        expect(!resizable.requestResize(180.0f)
                        && resizes.count == 1
                        && !resizable.resizeButton().enabled(),
                "ResizablePanelHeader should suppress resize requests when disabled");
    }

    private void testDragDropHelperContracts() {
        DragSource source = Widgets.dragSource()
                .payloadId("Button")
                .payloadType("widget")
                .dragPreview("<Button />")
                .dragThreshold(3.0f);
        expect(source.payload().id().equals("Button")
                        && source.payload().type().equals("widget")
                        && source.payload().preview().equals("<Button />"),
                "DragSource should expose payload id, type and preview metadata");

        DropTarget target = Widgets.dropTarget().acceptedPayloadTypes("widget|tree-node");
        Counter previews = new Counter();
        Counter drops = new Counter();
        target.onDropPreview(preview -> {
            previews.count++;
            previews.lastChecked = preview.accepted();
            previews.lastText = preview.payload().id();
        });
        target.onDrop(drop -> {
            drops.count++;
            drops.lastText = drop.payload().id();
            drops.lastOldText = drop.result().name();
        });

        DropTarget.DropPreview preview = target.previewDrop(source.payload(), 18.0f, 22.0f);
        expect(preview.accepted()
                        && target.dropPreviewActive()
                        && target.previewPayload().id().equals("Button")
                        && previews.count == 1
                        && previews.lastChecked
                        && previews.lastText.equals("Button"),
                "DropTarget should validate matching payload types and expose accepted previews");

        Counter drags = new Counter();
        source.onDrag(event -> {
            if (event.action() == DragSource.Action.STARTED) drags.started++;
            if (event.action() == DragSource.Action.MOVED) drags.moved++;
            if (event.action() == DragSource.Action.ENDED) drags.committed++;
            if (event.action() == DragSource.Action.CANCELLED) drags.cancelled++;
            drags.lastText = event.dropResult().name();
        });
        expect(source.startDrag(10.0f, 10.0f)
                        && source.moveDrag(14.0f, 10.0f)
                        && source.dropOn(target, 20.0f, 24.0f) == DropTarget.DropResult.ACCEPTED
                        && !source.dragging()
                        && source.lastDropResult() == DropTarget.DropResult.ACCEPTED
                        && drags.started == 1
                        && drags.moved == 1
                        && drags.committed == 1
                        && drops.count == 1
                        && drops.lastText.equals("Button")
                        && drops.lastOldText.equals("ACCEPTED")
                        && !target.dropPreviewActive(),
                "DragSource should emit drag lifecycle callbacks and DropTarget should emit accepted drops");

        target.addValidator(payload -> !payload.id().equals("Blocked"));
        expect(target.requestDrop(DragPayload.of("Blocked", "widget", "Blocked"), 30.0f, 30.0f) == DropTarget.DropResult.REJECTED
                        && drops.count == 1
                        && target.lastDropResult() == DropTarget.DropResult.REJECTED,
                "DropTarget validators should reject payloads without emitting accepted drop callbacks");
        expect(!target.accepts(DragPayload.of("Button", "asset", "Asset")),
                "DropTarget acceptedPayloadTypes should reject mismatched payload types");

        DragSource pointerSource = Widgets.dragSource()
                .payloadId("Label")
                .payloadType("widget")
                .dragThreshold(4.0f);
        DefaultUIContext uiContext = new DefaultUIContext();
        pointerSource.setUiContextInternal(uiContext);
        pointerSource.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 20.0f));
        Counter pointerDrags = new Counter();
        pointerSource.onDrag(event -> {
            if (event.action() == DragSource.Action.STARTED) pointerDrags.started++;
            if (event.action() == DragSource.Action.ENDED) pointerDrags.committed++;
        });
        uiContext.routedEvents().dispatch(new PointerPressedEvent(pointerSource, 2.0f, 2.0f, 2.0f, 2.0f, 4, PointerButton.PRIMARY));
        expect(pointerSource.pointerActive()
                        && !pointerSource.dragging()
                        && uiContext.capturedPointer(4) == pointerSource,
                "DragSource pointer press should capture the active pointer before threshold is crossed");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(pointerSource, 4.0f, 2.0f, 4.0f, 2.0f, 4));
        expect(!pointerSource.dragging() && pointerDrags.started == 0,
                "DragSource should wait for the configured movement threshold before starting drag");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(pointerSource, 8.0f, 2.0f, 8.0f, 2.0f, 4));
        expect(pointerSource.dragging() && pointerDrags.started == 1,
                "DragSource should start dragging once pointer movement crosses the threshold");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(pointerSource, 8.0f, 2.0f, 8.0f, 2.0f, 4, PointerButton.PRIMARY));
        expect(!pointerSource.pointerActive()
                        && !pointerSource.dragging()
                        && pointerDrags.committed == 1
                        && uiContext.capturedPointer(4) == null,
                "DragSource pointer release should end drag and release pointer capture");
    }

    private void testPasswordAndSearchFields() {
        PasswordField passwordField = new PasswordField("secret");
        passwordField.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));
        DrawList passwordDrawList = new DrawList();
        passwordField.render(new DefaultRenderContext(passwordDrawList));
        expect(hasText(passwordDrawList, "\u2022\u2022\u2022\u2022\u2022\u2022"), "PasswordField should render masked text");
        expect(!hasText(passwordDrawList, "secret"), "PasswordField should not render plain password text");

        DefaultUIContext uiContext = new DefaultUIContext();
        passwordField.setUiContextInternal(uiContext);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(passwordField, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        passwordField.selectAll();
        uiContext.clipboard().setText("sentinel");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(passwordField, KeyCodes.C, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("sentinel") && passwordField.text().equals("secret"),
                "PasswordField Ctrl+C should not copy the real password");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(passwordField, KeyCodes.X, 0, KeyModifiers.CONTROL));
        expect(uiContext.clipboard().getText().equals("sentinel") && passwordField.text().isEmpty(),
                "PasswordField Ctrl+X should delete selected text without exposing it to clipboard");

        SearchField searchField = new SearchField("recipe");
        searchField.setUiContextInternal(uiContext);
        searchField.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 18.0f));

        Counter submissions = new Counter();
        searchField.onSearchSubmitted(event -> {
            submissions.count++;
            submissions.lastText = event.query();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(searchField, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new KeyPressedEvent(searchField, KeyCodes.ENTER, 0, 0));
        expect(submissions.count == 1 && submissions.lastText.equals("recipe"), "SearchField should submit query on Enter");

        Counter searchChanges = new Counter();
        searchField.searchChangeDebounceSeconds(0.20f);
        searchField.onSearchChanged((SearchChangedEvent event) -> {
            searchChanges.count++;
            searchChanges.lastOldText = event.oldQuery();
            searchChanges.lastText = event.newQuery();
        });
        searchField.text("rec");
        searchField.tick(new FrameContext(1L, 0.10f, 0.0f, FramePhase.ANIMATION));
        expect(searchChanges.count == 0, "SearchField should debounce search changed events before the delay elapses");
        searchField.text("recipe book");
        searchField.tick(new FrameContext(2L, 0.20f, 0.0f, FramePhase.ANIMATION));
        expect(searchChanges.count == 1
                        && searchChanges.lastOldText.equals("recipe")
                        && searchChanges.lastText.equals("recipe book"),
                "SearchField should emit one debounced SearchChangedEvent with the latest query");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(searchField, 114.0f, 8.0f, 114.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(searchField.text().isEmpty(), "SearchField clear zone should clear query");
    }

    private void testDefaultThemeContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        ColorView buttonText = uiContext.theme().styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.TEXT_COLOR, WidgetState.NORMAL, null);
        ColorView buttonPressed = uiContext.theme().styleFor(StyleIds.Widget.BUTTON).get(StyleKeys.BACKGROUND_COLOR, WidgetState.PRESSED, null);
        ColorView unknownAccent = uiContext.theme().styleFor("UnknownWidget").get(StyleKeys.ACCENT_COLOR, WidgetState.CHECKED, null);
        expect(buttonText != null && buttonText.a() == 1.0f, "DefaultTheme should expose Button text color");
        expect(buttonPressed != null && buttonPressed.a() == 1.0f, "DefaultTheme should expose Button pressed background color");
        expect(unknownAccent != null && unknownAccent.a() == 1.0f, "DefaultTheme fallback should expose accent token");

        StyleKey<Float> radiusKeyCopy = StyleKey.of(StyleIds.Key.RADIUS, Float.class);
        expect(radiusKeyCopy.equals(StyleKeys.RADIUS), "StyleKey equality should use id and type");
        MutableStyle style = new MutableStyle().put(StyleKeys.RADIUS, 7.0f);
        expect(style.get(radiusKeyCopy, WidgetState.PRESSED, 0.0f) == 7.0f, "MutableStyle should fallback to NORMAL state");

        Button themedButton = new Button("Theme");
        themedButton.setUiContextInternal(uiContext);
        themedButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList normalButtonDrawList = new DrawList();
        themedButton.render(new DefaultRenderContext(normalButtonDrawList));
        expect(hasFillColor(normalButtonDrawList, 0.12f, 0.12f, 0.12f, 1.0f), "Button should apply NORMAL theme background at render time");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(themedButton, 4.0f, 4.0f, 4.0f, 4.0f, 0, PointerButton.PRIMARY));
        DrawList pressedButtonDrawList = new DrawList();
        themedButton.render(new DefaultRenderContext(pressedButtonDrawList));
        expect(hasFillColor(pressedButtonDrawList, 0.18f, 0.45f, 0.75f, 1.0f), "Button should apply PRESSED theme background at render time");

        Button manualButton = new Button("Manual");
        manualButton.themeEnabled(false);
        manualButton.background().set(0.9f, 0.1f, 0.2f, 1.0f);
        manualButton.setUiContextInternal(uiContext);
        manualButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        DrawList manualButtonDrawList = new DrawList();
        manualButton.render(new DefaultRenderContext(manualButtonDrawList));
        expect(hasFillColor(manualButtonDrawList, 0.9f, 0.1f, 0.2f, 1.0f), "themeEnabled(false) should preserve manual colors");

        MutableStyle customButtonStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.4f, 0.2f, 0.8f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.9f, 0.9f, 0.2f, 1.0f));
        final int[] themeRendererCalls = {0};
        ButtonRenderer themeButtonRenderer = (draw, state) -> {
            themeRendererCalls[0]++;
            ButtonRenderers.DEFAULT.render(draw, state);
        };
        final int[] localRendererCalls = {0};
        ButtonRenderer localButtonRenderer = (draw, state) -> {
            localRendererCalls[0]++;
            ButtonRenderers.DEFAULT.render(draw, state);
        };
        final int[] instanceRendererCalls = {0};
        ButtonRenderer instanceButtonRenderer = (draw, state) -> {
            instanceRendererCalls[0]++;
            ButtonRenderers.DEFAULT.render(draw, state);
        };

        MutableStyle customRendererStyle = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.4f, 0.2f, 0.8f, 1.0f))
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.9f, 0.9f, 0.2f, 1.0f))
                .renderer(themeButtonRenderer);

        Button rendererThemeButton = new Button("Renderer theme");
        rendererThemeButton.setUiContextInternal(uiContext);
        rendererThemeButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        uiContext.theme(new MutableTheme().put(StyleIds.Widget.BUTTON, customRendererStyle), rendererThemeButton);
        rendererThemeButton.render(new DefaultRenderContext(new DrawList()));
        expect(themeRendererCalls[0] == 1, "theme renderer override should replace default Button renderer");

        Button rendererLocalButton = new Button("Renderer local");
        rendererLocalButton.setUiContextInternal(uiContext);
        rendererLocalButton.localStyle(StyleIds.Widget.BUTTON, new MutableStyle().renderer(localButtonRenderer));
        rendererLocalButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        rendererLocalButton.render(new DefaultRenderContext(new DrawList()));
        expect(localRendererCalls[0] == 1, "local style renderer override should replace default Button renderer");

        Button rendererInstanceButton = new Button("Renderer instance");
        rendererInstanceButton.setUiContextInternal(uiContext);
        rendererInstanceButton.localStyle(StyleIds.Widget.BUTTON, new MutableStyle().renderer(localButtonRenderer));
        rendererInstanceButton.renderer(instanceButtonRenderer);
        rendererInstanceButton.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        rendererInstanceButton.render(new DefaultRenderContext(new DrawList()));
        expect(instanceRendererCalls[0] == 1 && localRendererCalls[0] == 1,
                "per-instance renderer should keep priority over style renderer");
        MutableTheme customTheme = new MutableTheme().fallback(style).put(StyleIds.Widget.BUTTON, customButtonStyle);
        long previousStyleVersion = uiContext.styleVersion();
        uiContext.theme(customTheme, themedButton);
        expect(uiContext.styleVersion() != previousStyleVersion, "DefaultUIContext should bump styleVersion when theme changes");
        DrawList customThemeDrawList = new DrawList();
        themedButton.render(new DefaultRenderContext(customThemeDrawList));
        expect(hasFillColor(customThemeDrawList, 0.4f, 0.2f, 0.8f, 1.0f), "Button should apply newly assigned theme on next render");

        Box root = new Box();
        Button child = new Button("Child");
        root.setUiContextInternal(uiContext);
        root.addChild(child);
        root.applyQueuedMutations();
        root.clearInvalidation(InvalidationFlags.ALL);
        child.clearInvalidation(InvalidationFlags.ALL);
        uiContext.theme(new MutableTheme().put(StyleIds.Widget.BUTTON, customButtonStyle), root);
        expect(hasFlag(root.invalidationFlags(), InvalidationFlags.VISUAL), "theme(root) should invalidate root visuals");
        expect(hasFlag(child.invalidationFlags(), InvalidationFlags.VISUAL), "theme(root) should invalidate child visuals");
    }

    private void testStyleInheritanceAndScopes() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        Box scoped = new Box();
        Button inherited = new Button("Inherited");
        Button local = new Button("Local");
        Button isolated = new Button("Isolated");
        Box boundary = new Box();
        boundary.styleScope(true);

        root.setUiContextInternal(uiContext);
        root.localStyle(StyleIds.WILDCARD, new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.7f, 0.1f, 0.1f, 1.0f)));
        scoped.localStyle(StyleIds.Widget.BUTTON, new MutableStyle()
                .put(StyleKeys.TEXT_COLOR, MutableColor.rgba(0.2f, 0.8f, 0.2f, 1.0f)));
        local.localStyle(StyleIds.Widget.BUTTON, new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.1f, 0.2f, 0.9f, 1.0f)));
        boundary.localStyle(StyleIds.Widget.BUTTON, new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, MutableColor.rgba(0.2f, 0.2f, 0.2f, 1.0f)));

        root.addChild(scoped);
        scoped.addChild(inherited);
        scoped.addChild(local);
        scoped.addChild(boundary);
        boundary.addChild(isolated);
        root.applyQueuedMutations();
        scoped.applyQueuedMutations();
        boundary.applyQueuedMutations();
        inherited.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        local.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        isolated.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));

        DrawList inheritedDrawList = new DrawList();
        inherited.render(new DefaultRenderContext(inheritedDrawList));
        expect(hasFillColor(inheritedDrawList, 0.7f, 0.1f, 0.1f, 1.0f), "Widget should inherit wildcard background from ancestor local style");
        expect(hasFillColor(inheritedDrawList, 0.2f, 0.8f, 0.2f, 1.0f), "Widget should inherit typed text color from ancestor local style");

        DrawList localDrawList = new DrawList();
        local.render(new DefaultRenderContext(localDrawList));
        expect(hasFillColor(localDrawList, 0.1f, 0.2f, 0.9f, 1.0f), "Widget local style should override ancestor local style");

        DrawList isolatedDrawList = new DrawList();
        isolated.render(new DefaultRenderContext(isolatedDrawList));
        expect(hasFillColor(isolatedDrawList, 0.2f, 0.2f, 0.2f, 1.0f), "Nearest style scope should provide local style inside boundary");
        expect(!hasFillColor(isolatedDrawList, 0.7f, 0.1f, 0.1f, 1.0f), "Nearest style scope should stop outer local style inheritance");
    }

    private void testKeyboardFocusTraversal() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        root.setUiContextInternal(uiContext);

        Button first = new Button("First");
        TextField second = new TextField("Second");
        Button skipped = new Button("Skipped");
        skipped.focusable(false);
        first.focusOrder(20);
        second.focusOrder(10);

        root.addChild(first);
        root.addChild(second);
        root.addChild(skipped);
        root.applyQueuedMutations();

        expect(uiContext.focusManager().focusNext(root), "focusNext should focus the first available widget");
        expect(uiContext.focusManager().focusedWidget() == second, "focusNext should respect focusOrder before tree order");
        expect(uiContext.focusManager().focusNext(root), "focusNext should advance to next focusable widget");
        expect(uiContext.focusManager().focusedWidget() == first, "focusNext should advance through ordered focusables");
        expect(uiContext.focusManager().focusNext(root), "focusNext should wrap around");
        expect(uiContext.focusManager().focusedWidget() == second, "focusNext should skip non-focusable widgets");
        uiContext.focusManager().requestFocus(skipped);
        expect(uiContext.focusManager().focusedWidget() == second, "requestFocus should ignore non-focusable widgets");
        expect(uiContext.focusManager().focusPrevious(root), "focusPrevious should move backwards");
        expect(uiContext.focusManager().focusedWidget() == first, "focusPrevious should wrap backwards");

        Box scope = new Box();
        scope.focusScope(true);
        Button scopeA = new Button("Scope A");
        Button scopeB = new Button("Scope B");
        scope.addChild(scopeA);
        scope.addChild(scopeB);
        scope.applyQueuedMutations();
        root.addChild(scope);
        root.applyQueuedMutations();

        uiContext.focusManager().requestFocus(scopeA);
        expect(uiContext.focusManager().focusNext(root), "focusNext should work inside nearest focus scope");
        expect(uiContext.focusManager().focusedWidget() == scopeB, "focusNext should stay inside nearest focus scope");
        expect(uiContext.focusManager().focusNext(root), "focusNext should wrap inside nearest focus scope");
        expect(uiContext.focusManager().focusedWidget() == scopeA, "focusNext should not escape nearest focus scope");
        expect(uiContext.focusManager().focusPrevious(root), "focusPrevious should stay inside nearest focus scope");
        expect(uiContext.focusManager().focusedWidget() == scopeB, "focusPrevious should wrap inside nearest focus scope");
    }

    private void testDirectionalFocusNavigation() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        root.setUiContextInternal(uiContext);

        Button center = new Button("Center");
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button up = new Button("Up");
        Button down = new Button("Down");
        Button hiddenRight = new Button("Hidden right");
        hiddenRight.visibility(Visibility.HIDDEN);

        root.addChild(center);
        root.addChild(left);
        root.addChild(right);
        root.addChild(up);
        root.addChild(down);
        root.addChild(hiddenRight);
        root.applyQueuedMutations();
        center.arrange(new MutableRect(90.0f, 90.0f, 20.0f, 20.0f));
        left.arrange(new MutableRect(40.0f, 90.0f, 20.0f, 20.0f));
        right.arrange(new MutableRect(140.0f, 90.0f, 20.0f, 20.0f));
        up.arrange(new MutableRect(90.0f, 40.0f, 20.0f, 20.0f));
        down.arrange(new MutableRect(90.0f, 140.0f, 20.0f, 20.0f));
        hiddenRight.arrange(new MutableRect(112.0f, 90.0f, 20.0f, 20.0f));

        uiContext.focusManager().requestFocus(center);
        expect(uiContext.focusManager().focusDirectional(root, FocusDirection.RIGHT), "Directional focus should move right");
        expect(uiContext.focusManager().focusedWidget() == right, "Directional focus should skip hidden candidates");
        expect(uiContext.focusManager().focusDirectional(root, FocusDirection.LEFT), "Directional focus should move left from right");
        expect(uiContext.focusManager().focusedWidget() == center, "Directional focus should pick nearest left candidate");
        expect(uiContext.focusManager().focusDown(root), "Directional focus convenience should move down");
        expect(uiContext.focusManager().focusedWidget() == down, "focusDown should pick the lower candidate");
        expect(!uiContext.focusManager().focusDown(root), "Directional focus should not wrap when no candidate exists");
        expect(uiContext.focusManager().focusedWidget() == down, "Directional focus should keep current focus when blocked");

        Box scope = new Box();
        scope.focusScope(true);
        Button scopedCenter = new Button("Scoped center");
        Button scopedRight = new Button("Scoped right");
        Button outsideRight = new Button("Outside right");
        root.addChild(scope);
        root.addChild(outsideRight);
        root.applyQueuedMutations();
        scope.addChild(scopedCenter);
        scope.addChild(scopedRight);
        scope.applyQueuedMutations();
        scopedCenter.arrange(new MutableRect(10.0f, 210.0f, 20.0f, 20.0f));
        scopedRight.arrange(new MutableRect(60.0f, 210.0f, 20.0f, 20.0f));
        outsideRight.arrange(new MutableRect(110.0f, 210.0f, 20.0f, 20.0f));

        uiContext.focusManager().requestFocus(scopedCenter);
        expect(uiContext.focusManager().focusRight(root), "Directional focus should work inside focus scopes");
        expect(uiContext.focusManager().focusedWidget() == scopedRight, "Directional focus should stay inside nearest focus scope");
    }

    private void testHoverTrackingAndStyleState() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        Button first = new Button("First");
        Button second = new Button("Second");
        root.setUiContextInternal(uiContext);
        root.addChild(first);
        root.addChild(second);
        root.applyQueuedMutations();
        first.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        second.arrange(new MutableRect(90.0f, 0.0f, 80.0f, 18.0f));

        Counter firstEnter = new Counter();
        Counter firstExit = new Counter();
        first.on(PointerEnteredEvent.TYPE, event -> firstEnter.count++);
        first.on(PointerExitedEvent.TYPE, event -> firstExit.count++);

        uiContext.hoverManager().updateHover(first, 4.0f, 4.0f, 4.0f, 4.0f, 0);
        expect(uiContext.hoverManager().hoveredWidget() == first && first.hovered(), "HoverManager should mark entered widget hovered");
        expect(firstEnter.count == 1 && firstExit.count == 0, "HoverManager should emit pointer entered once");
        uiContext.hoverManager().updateHover(first, 5.0f, 4.0f, 5.0f, 4.0f, 0);
        expect(firstEnter.count == 1, "HoverManager should not re-enter the same widget");

        DrawList hoveredDrawList = new DrawList();
        first.render(new DefaultRenderContext(hoveredDrawList));
        expect(hasFillColor(hoveredDrawList, 0.16f, 0.16f, 0.16f, 1.0f), "Hovered Button should use HOVERED background style");

        uiContext.hoverManager().updateHover(second, 94.0f, 4.0f, 4.0f, 4.0f, 0);
        expect(!first.hovered() && second.hovered(), "HoverManager should transfer hover between widgets");
        expect(firstExit.count == 1, "HoverManager should emit pointer exited when hover leaves");
        uiContext.hoverManager().clearHover();
        expect(uiContext.hoverManager().hoveredWidget() == null && !second.hovered(), "clearHover should clear current hover state");
    }

    private void testEnabledVisibleStateFlags() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box root = new Box();
        Button disabled = new Button("Disabled");
        Button hidden = new Button("Hidden");
        Button collapsed = new Button("Collapsed");
        Button active = new Button("Active");
        root.setUiContextInternal(uiContext);
        disabled.enabled(false);
        hidden.visible(false);
        collapsed.visibility(Visibility.COLLAPSED);
        collapsed.arrange(new MutableRect(11.0f, 12.0f, 13.0f, 14.0f));
        root.addChild(disabled);
        root.addChild(hidden);
        root.addChild(collapsed);
        root.addChild(active);
        root.applyQueuedMutations();
        root.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));

        expect(!disabled.enabled() && !hidden.visible() && hidden.visibility() == Visibility.HIDDEN, "Widget state flags should be mutable");
        expect(collapsed.visibility() == Visibility.COLLAPSED, "Collapsed visibility should be mutable");
        expect(hidden.layoutBounds().x() == 0.0f && hidden.layoutBounds().width() == 100.0f,
                "Hidden widgets should keep participating in layout");
        expect(collapsed.layoutBounds().x() == 11.0f && collapsed.layoutBounds().width() == 13.0f,
                "Collapsed widgets should be skipped by parent layout");
        uiContext.focusManager().requestFocus(disabled);
        expect(uiContext.focusManager().focusedWidget() == null, "requestFocus should ignore disabled widgets");
        expect(uiContext.focusManager().focusNext(root), "focusNext should find an enabled visible widget");
        expect(uiContext.focusManager().focusedWidget() == active, "focusNext should skip disabled, hidden and collapsed widgets");

        Counter clicks = new Counter();
        disabled.onClick(event -> clicks.count++);
        disabled.handle(new PointerPressedEvent(disabled, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        disabled.handle(new PointerReleasedEvent(disabled, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(clicks.count == 0, "Disabled Button should ignore pointer input");

        DrawList disabledDrawList = new DrawList();
        disabled.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        disabled.render(new DefaultRenderContext(disabledDrawList));
        expect(hasFillColor(disabledDrawList, 0.08f, 0.08f, 0.08f, 0.75f), "Disabled Button should use DISABLED style state");

        DrawList hiddenDrawList = new DrawList();
        hidden.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        hidden.render(new DefaultRenderContext(hiddenDrawList));
        expect(hiddenDrawList.size() == 0, "Hidden widget should not render commands");

        TransformHitTester hitTester = new TransformHitTester();
        root.visible(true);
        root.enabled(true);
        disabled.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        hidden.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        active.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        expect(hitTester.hitTest(disabled, 5.0f, 5.0f).isEmpty(), "Hit-test should skip disabled root widgets");
        expect(hitTester.hitTest(hidden, 5.0f, 5.0f).isEmpty(), "Hit-test should skip hidden root widgets");
        expect(hitTester.hitTest(collapsed, 5.0f, 5.0f).isEmpty(), "Hit-test should skip collapsed root widgets");
    }

    private void testDesiredSizeMeasurement() {
        LayoutContext generous = new LayoutContext(500.0f, 100.0f);

        Label label = new Label("Hello");
        label.measure(generous);
        expect(near(label.desiredSize().width(), 30.0f) && near(label.desiredSize().height(), 10.0f),
                "Label should measure intrinsic single-line text size");

        TextBlock block = new TextBlock("abcdefghijklmnopqrstuvwx");
        block.measure(new LayoutContext(60.0f, 100.0f));
        expect(near(block.desiredSize().width(), 60.0f) && near(block.desiredSize().height(), 30.0f),
                "TextBlock should aggregate wrapped desired height from available width");

        TextField field = new TextField("name");
        field.measure(generous);
        expect(near(field.desiredSize().width(), 32.0f) && near(field.desiredSize().height(), 18.0f),
                "TextInput/TextField should include text padding in desired size");
        field.layout(style -> style.size(100.0f, LayoutConstraints.AUTO));
        field.measure(generous);
        expect(near(field.desiredSize().width(), 100.0f) && near(field.desiredSize().height(), 18.0f),
                "Explicit preferred width should override measured text width");

        RichText compactButtonText = RichText.builder().size(4.0f).tracking(0.3f).uppercase().append("ok").build();
        Button compactButton = new Button().richText(compactButtonText).textPadding(1.5f, 2.0f);
        compactButton.measure(generous);
        expect(near(compactButton.desiredSize().width(), TextEngine.measureLineWidth(compactButtonText) + 3.0f)
                        && near(compactButton.desiredSize().height(), TextEngine.measureTextHeight(compactButtonText) + 4.0f),
                "Button should derive intrinsic size from rich text and configurable padding");

        Checkbox compactCheckbox = new Checkbox();
        compactCheckbox.richText(compactButtonText);
        compactCheckbox.boxSize(5.0f).textGap(1.0f);
        compactCheckbox.measure(generous);
        expect(near(compactCheckbox.desiredSize().width(), 5.0f + 1.0f + TextEngine.measureLineWidth(compactButtonText))
                        && near(compactCheckbox.desiredSize().height(), Math.max(5.0f, TextEngine.measureTextHeight(compactButtonText))),
                "Checkbox should derive intrinsic size from marker, rich text and gap");

        RadioButton compactRadio = new RadioButton();
        compactRadio.richText(compactButtonText);
        compactRadio.outerSize(5.0f).textGap(1.0f);
        compactRadio.measure(generous);
        expect(near(compactRadio.desiredSize().width(), 5.0f + 1.0f + TextEngine.measureLineWidth(compactButtonText))
                        && near(compactRadio.desiredSize().height(), Math.max(5.0f, TextEngine.measureTextHeight(compactButtonText))),
                "RadioButton should derive intrinsic size from marker, rich text and gap");

        ProgressBar compactProgress = new ProgressBar().preferredSize(48.0f, 6.0f);
        compactProgress.measure(generous);
        expect(near(compactProgress.desiredSize().width(), 48.0f)
                        && near(compactProgress.desiredSize().height(), 6.0f),
                "ProgressBar should expose an intrinsic preferred size without external layout size");

        Slider compactSlider = new Slider().preferredSize(64.0f, 10.0f);
        compactSlider.measure(generous);
        expect(near(compactSlider.desiredSize().width(), 64.0f)
                        && near(compactSlider.desiredSize().height(), 10.0f),
                "Slider should expose an intrinsic preferred size without external layout size");

        ScrollBar verticalScrollBar = new ScrollBar().preferredSize(72.0f, 4.0f);
        verticalScrollBar.measure(generous);
        expect(near(verticalScrollBar.desiredSize().width(), 4.0f)
                        && near(verticalScrollBar.desiredSize().height(), 72.0f),
                "Vertical ScrollBar should measure thickness by length from intrinsic preferred size");

        ScrollBar horizontalScrollBar = new ScrollBar()
                .orientation(Orientation.HORIZONTAL)
                .preferredSize(72.0f, 4.0f);
        horizontalScrollBar.measure(generous);
        expect(near(horizontalScrollBar.desiredSize().width(), 72.0f)
                        && near(horizontalScrollBar.desiredSize().height(), 4.0f),
                "Horizontal ScrollBar should swap length and thickness in desired size");

        LoadingIndicator compactSpinner = new LoadingIndicator().indicatorSize(16.0f);
        compactSpinner.measure(generous);
        expect(near(compactSpinner.desiredSize().width(), 16.0f)
                        && near(compactSpinner.desiredSize().height(), 16.0f),
                "LoadingIndicator indicatorSize should set intrinsic square size without external layout size");

        LoadingIndicator compactBarIndicator = new LoadingIndicator()
                .mode(LoadingIndicator.Mode.BAR)
                .preferredSize(64.0f, 5.0f);
        compactBarIndicator.measure(generous);
        expect(near(compactBarIndicator.desiredSize().width(), 64.0f)
                        && near(compactBarIndicator.desiredSize().height(), 5.0f),
                "LoadingIndicator bar mode should expose intrinsic preferred size without external layout size");

        Sparkline compactSparkline = new Sparkline().preferredSize(90.0f, 22.0f);
        compactSparkline.measure(generous);
        expect(near(compactSparkline.desiredSize().width(), 90.0f)
                        && near(compactSparkline.desiredSize().height(), 22.0f),
                "Sparkline should expose an intrinsic preferred size without external layout size");

        Chart compactChart = new Chart().preferredSize(160.0f, 70.0f);
        compactChart.measure(generous);
        expect(near(compactChart.desiredSize().width(), 160.0f)
                        && near(compactChart.desiredSize().height(), 70.0f),
                "Chart should expose an intrinsic preferred size without external layout size");

        GraphView compactGraphView = new GraphView().preferredSize(150.0f, 80.0f);
        compactGraphView.measure(generous);
        expect(near(compactGraphView.desiredSize().width(), 150.0f)
                        && near(compactGraphView.desiredSize().height(), 80.0f),
                "GraphView should expose an intrinsic preferred size without external layout size");

        NodeGraph compactNodeGraph = new NodeGraph().preferredSize(180.0f, 90.0f);
        compactNodeGraph.measure(generous);
        expect(near(compactNodeGraph.desiredSize().width(), 180.0f)
                        && near(compactNodeGraph.desiredSize().height(), 90.0f),
                "NodeGraph should expose an intrinsic preferred size without external layout size");

        WorldCanvas compactWorldCanvas = new WorldCanvas().preferredSize(200.0f, 96.0f);
        compactWorldCanvas.measure(generous);
        expect(near(compactWorldCanvas.desiredSize().width(), 200.0f)
                        && near(compactWorldCanvas.desiredSize().height(), 96.0f),
                "WorldCanvas should expose an intrinsic preferred size without external layout size");

        HBox hbox = new HBox();
        hbox.spacing(5.0f);
        Label first = new Label("AA");
        Label second = new Label("BBBB");
        hbox.addChild(first);
        hbox.addChild(second);
        hbox.measure(new LayoutContext(200.0f, 100.0f));
        expect(near(hbox.desiredSize().width(), 41.0f) && near(hbox.desiredSize().height(), 10.0f),
                "HBox should aggregate measured child widths plus spacing");
        hbox.arrange(new MutableRect(0.0f, 0.0f, hbox.desiredSize().width(), hbox.desiredSize().height()));
        expect(near(first.layoutBounds().width(), 12.0f) && near(second.layoutBounds().x(), 17.0f)
                        && near(second.layoutBounds().width(), 24.0f),
                "HBox should use measured desired width as AUTO main-axis fallback after measure");

        GridBox grid = new GridBox().columns(2).spacing(5.0f);
        grid.addChild(new Label("AA"));
        grid.addChild(new Label("BBBB"));
        grid.measure(new LayoutContext(200.0f, 100.0f));
        expect(near(grid.desiredSize().width(), 53.0f) && near(grid.desiredSize().height(), 10.0f),
                "GridBox should aggregate measured max cell size plus spacing");

        WrapPanel wrap = new WrapPanel().spacing(5.0f).lineSpacing(2.0f);
        wrap.addChild(new Label("AAAAAA"));
        wrap.addChild(new Label("BBBBBB"));
        wrap.addChild(new Label("CCCCCC"));
        wrap.measure(new LayoutContext(80.0f, 100.0f));
        expect(near(wrap.desiredSize().width(), 77.0f) && near(wrap.desiredSize().height(), 22.0f),
                "WrapPanel should aggregate measured wrapped lines under available width");
    }

    private void testLayoutConstraintsAndSlotSizing() {
        HBox hbox = new HBox();
        hbox.spacing(10.0f);
        Button fixed = new Button("Fixed");
        Button growA = new Button("Grow A");
        Button growB = new Button("Grow B");
        fixed.layout(style -> style.size(40.0f, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f).margin(5.0f, 0.0f));
        growA.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        growB.layout(style -> style.flexGrow(2).flexShrink(1.0f));
        hbox.addChild(fixed);
        hbox.addChild(growA);
        hbox.addChild(growB);
        hbox.applyQueuedMutations();
        hbox.arrange(new MutableRect(0.0f, 0.0f, 210.0f, 30.0f));

        expect(near(fixed.layoutBounds().x(), 5.0f) && near(fixed.layoutBounds().width(), 40.0f),
                "HBox should honor preferred width and horizontal margin");
        expect(near(growA.layoutBounds().x(), 60.0f) && near(growA.layoutBounds().width(), 46.67f),
                "HBox should allocate grow weight after fixed slots");
        expect(near(growB.layoutBounds().x(), 116.67f) && near(growB.layoutBounds().width(), 93.33f),
                "HBox should allocate larger grow weight proportionally");

        VBox vbox = new VBox();
        vbox.spacing(5.0f);
        Button top = new Button("Top");
        Button centered = new Button("Centered");
        Button collapsed = new Button("Collapsed");
        top.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        centered.layout(style -> style.size(60.0f, 10.0f).flexGrow(0).flexShrink(0.0f).margin(4.0f).align(Alignment.CENTER, Alignment.CENTER));
        collapsed.visibility(Visibility.COLLAPSED);
        collapsed.arrange(new MutableRect(7.0f, 8.0f, 9.0f, 10.0f));
        vbox.addChild(top);
        vbox.addChild(centered);
        vbox.addChild(collapsed);
        vbox.applyQueuedMutations();
        vbox.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));

        expect(near(top.layoutBounds().height(), 20.0f), "VBox should honor preferred height");
        expect(near(centered.layoutBounds().x(), 20.0f) && near(centered.layoutBounds().y(), 29.0f)
                       && near(centered.layoutBounds().width(), 60.0f) && near(centered.layoutBounds().height(), 10.0f),
                "VBox should apply margin and center alignment inside the child slot");
        expect(near(collapsed.layoutBounds().x(), 7.0f) && near(collapsed.layoutBounds().width(), 9.0f),
                "VBox should skip collapsed children during layout");

        GridBox grid = new GridBox().columns(2).spacing(10.0f);
        Button stretched = new Button("Stretch");
        Button endAligned = new Button("End");
        endAligned.layout(style -> style
                .size(30.0f, 20.0f)
                .margin(5.0f)
                .align(Alignment.END, Alignment.CENTER)
                .flexGrow(0.0f)
                .flexShrink(0.0f));
        grid.addChild(stretched);
        grid.addChild(endAligned);
        grid.applyQueuedMutations();
        grid.arrange(new MutableRect(0.0f, 0.0f, 110.0f, 30.0f));

        expect(near(stretched.layoutBounds().width(), 50.0f) && near(stretched.layoutBounds().height(), 30.0f),
                "GridBox should stretch default children to the cell");
        expect(near(endAligned.layoutBounds().x(), 75.0f) && near(endAligned.layoutBounds().y(), 5.0f)
                        && near(endAligned.layoutBounds().width(), 30.0f) && near(endAligned.layoutBounds().height(), 20.0f),
                "GridBox should honor margin, preferred size and alignment inside cells");
    }

    private void testLayoutV2CompatibilityContracts() {
        Box legacyConfigured = new Box();
        legacyConfigured
                .layout(style -> style.size(120.0f, LayoutConstraints.AUTO).minSize(10.0f, 12.0f).maxSize(180.0f, 90.0f).margin(4.0f, 6.0f).flexGrow(2).flexShrink(1.0f));

        LayoutStyle legacyStyle = legacyConfigured.layoutStyle();
        expect(legacyStyle.width().equals(SizeValue.px(120.0f)) && legacyStyle.height().isAuto(),
                "layout(style -> size(...)) should populate LayoutStyle pixel/auto values");
        expect(legacyStyle.minWidth().equals(SizeValue.px(10.0f))
                        && legacyStyle.minHeight().equals(SizeValue.px(12.0f))
                        && legacyStyle.maxWidth().equals(SizeValue.px(180.0f))
                        && legacyStyle.maxHeight().equals(SizeValue.px(90.0f)),
                "layout(style -> min/max size(...)) should populate LayoutStyle");
        expect(legacyStyle.margin().equals(EdgeInsets.symmetric(4.0f, 6.0f)),
                "layout(style -> margin(...)) should populate LayoutStyle box model values");
        expect(near(legacyStyle.flexGrow(), 2.0f) && near(legacyStyle.flexShrink(), 1.0f),
                "layout(style -> flexGrow/flexShrink(...)) should update LayoutStyle flex values");

        legacyConfigured.layout(style -> style
                .width(90.0f)
                .height(30.0f)
                .minWidth(20.0f)
                .maxWidth(100.0f)
                .margin(3.0f)
                .flexGrow(1.0f)
                .alignSelf(Align.CENTER)
                .overflowY(Overflow.AUTO));

        LayoutConstraints bridged = legacyConfigured.layoutConstraints();
        expect(near(bridged.preferredWidth(), 90.0f) && near(bridged.preferredHeight(), 30.0f),
                "LayoutStyle pixel sizes should bridge into current LayoutConstraints");
        expect(near(bridged.minWidth(), 20.0f) && near(bridged.maxWidth(), 100.0f),
                "LayoutStyle pixel min/max values should bridge into current LayoutConstraints");
        expect(bridged.margin().equals(EdgeInsets.all(3.0f))
                        && near(bridged.grow(), 1.0f)
                        && bridged.horizontalAlignment() == Alignment.CENTER
                        && bridged.verticalAlignment() == Alignment.CENTER,
                "LayoutStyle margin, grow and alignSelf should bridge into current layout behavior");
        expect(legacyConfigured.layoutStyle().overflowY() == Overflow.AUTO,
                "LayoutStyle should retain v2-only overflow metadata before overflow engine migration");

        legacyConfigured.layout(style -> style
                .widthPercent(50.0f)
                .overflowX(Overflow.HIDDEN));
        expect(legacyConfigured.layoutStyle().width().equals(SizeValue.percent(50.0f)),
                "LayoutStyle should retain percent values for the future flex resolver");
        expect(near(legacyConfigured.layoutConstraints().preferredWidth(), 90.0f),
                "Percent width should preserve current legacy width until Layout v2 resolves percentages");
    }

    private void testPublicLayoutApiContracts() {
        LayoutStyle composite = new LayoutStyle()
                .sizePercent(75.0f, 50.0f)
                .minSize(40.0f, 20.0f)
                .maxSizePercent(100.0f, 90.0f)
                .margin(1.0f, 2.0f, 3.0f, 4.0f)
                .padding(5.0f, 6.0f, 7.0f, 8.0f)
                .flex(2.0f, 0.5f, SizeValue.px(60.0f))
                .inset(9.0f, 10.0f, 11.0f, 12.0f);
        expect(composite.width().equals(SizeValue.percent(75.0f))
                        && composite.height().equals(SizeValue.percent(50.0f))
                        && composite.minWidth().equals(SizeValue.px(40.0f))
                        && composite.maxHeight().equals(SizeValue.percent(90.0f)),
                "LayoutStyle composite sizing helpers should map to explicit SizeValue fields");
        expect(composite.margin().equals(new EdgeInsets(1.0f, 2.0f, 3.0f, 4.0f))
                        && composite.padding().equals(new EdgeInsets(5.0f, 6.0f, 7.0f, 8.0f))
                        && near(composite.flexGrow(), 2.0f)
                        && near(composite.flexShrink(), 0.5f)
                        && composite.flexBasis().equals(SizeValue.px(60.0f))
                        && composite.left().equals(SizeValue.px(9.0f))
                        && composite.bottom().equals(SizeValue.px(12.0f)),
                "LayoutStyle box, flex and inset helpers should update their grouped fields");
        composite.align(Alignment.CENTER, Alignment.END).alignSelf(Align.AUTO);
        expect(composite.horizontalAlignment() == Alignment.STRETCH
                        && composite.verticalAlignment() == Alignment.STRETCH,
                "alignSelf(AUTO) should reset explicit compatibility alignment after mixed align(...)");

        Box mixedApi = new Box();
        mixedApi.layout(style -> style
                .widthPercent(50.0f)
                .height(24.0f)
                .padding(7.0f)
                .overflow(Overflow.HIDDEN));
        mixedApi.layout(style -> style.flexGrow(2).flexShrink(1.0f).margin(3.0f).align(Alignment.CENTER, Alignment.END).minSize(30.0f, 12.0f));

        expect(mixedApi.layoutStyle().width().equals(SizeValue.percent(50.0f))
                        && mixedApi.layoutStyle().height().equals(SizeValue.px(24.0f))
                        && mixedApi.layoutStyle().padding().equals(EdgeInsets.all(7.0f))
                        && mixedApi.layoutStyle().overflowX() == Overflow.HIDDEN
                        && mixedApi.layoutStyle().overflowY() == Overflow.HIDDEN,
                "Additional layout(style -> ...) updates should preserve unrelated Layout v2 properties");
        expect(near(mixedApi.layoutStyle().flexGrow(), 2.0f)
                        && near(mixedApi.layoutStyle().flexShrink(), 1.0f)
                        && mixedApi.layoutStyle().margin().equals(EdgeInsets.all(3.0f))
                        && mixedApi.layoutStyle().alignSelf() == Align.AUTO
                        && mixedApi.layoutConstraints().horizontalAlignment() == Alignment.CENTER
                        && mixedApi.layoutConstraints().verticalAlignment() == Alignment.END,
                "layout(style -> flex/margin/alignment(...)) should retain the compatibility projection");

        mixedApi.layout(style -> style.size(90.0f, LayoutConstraints.AUTO));
        expect(mixedApi.layoutStyle().width().equals(SizeValue.px(90.0f))
                        && mixedApi.layoutStyle().height().isAuto()
                        && mixedApi.layoutStyle().padding().equals(EdgeInsets.all(7.0f))
                        && mixedApi.layoutStyle().overflowX() == Overflow.HIDDEN,
                "layout(style -> size(...)) should intentionally replace size fields without resetting v2-only metadata");
    }

    private void testFlexLayoutV2Resolver() {
        HBox shrinkRow = new HBox();
        shrinkRow.spacing(10.0f);
        Box shrinkA = new Box();
        Box shrinkB = new Box();
        shrinkA.layout(style -> style.width(100.0f).height(20.0f).minWidth(20.0f).flexShrink(1.0f));
        shrinkB.layout(style -> style.width(100.0f).height(20.0f).minWidth(20.0f).flexShrink(1.0f));
        shrinkRow.addChild(shrinkA);
        shrinkRow.addChild(shrinkB);
        shrinkRow.arrange(new MutableRect(0.0f, 0.0f, 150.0f, 20.0f));
        expect(near(shrinkA.layoutBounds().width(), 70.0f)
                        && near(shrinkB.layoutBounds().x(), 80.0f)
                        && near(shrinkB.layoutBounds().width(), 70.0f),
                "Flex resolver should shrink oversized row children into available width");

        HBox fixedAndFlexible = new HBox();
        fixedAndFlexible.spacing(10.0f);
        Box fixed = new Box();
        Box flexible = new Box();
        fixed.layout(style -> style.width(80.0f).height(20.0f).flexShrink(0.0f));
        flexible.layout(style -> style.width(100.0f).height(20.0f).minWidth(20.0f).flexShrink(1.0f));
        fixedAndFlexible.addChild(fixed);
        fixedAndFlexible.addChild(flexible);
        fixedAndFlexible.arrange(new MutableRect(0.0f, 0.0f, 130.0f, 20.0f));
        expect(near(fixed.layoutBounds().width(), 80.0f)
                        && near(flexible.layoutBounds().x(), 90.0f)
                        && near(flexible.layoutBounds().width(), 40.0f),
                "Flex shrink should preserve fixed children and reduce flexible siblings");

        HBox basisGrow = new HBox();
        Box basisA = new Box();
        Box basisB = new Box();
        basisA.layout(style -> style.flexBasis(50.0f).flexGrow(1.0f).flexShrink(1.0f));
        basisB.layout(style -> style.flexBasis(50.0f).flexGrow(3.0f).flexShrink(1.0f));
        basisGrow.addChild(basisA);
        basisGrow.addChild(basisB);
        basisGrow.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 20.0f));
        expect(near(basisA.layoutBounds().width(), 75.0f)
                        && near(basisB.layoutBounds().x(), 75.0f)
                        && near(basisB.layoutBounds().width(), 125.0f),
                "Flex basis and grow weights should distribute positive free space");

        HBox percentRow = new HBox();
        Box percentChild = new Box();
        percentChild.layout(style -> style.widthPercent(50.0f).height(12.0f).flexShrink(0.0f));
        percentRow.addChild(percentChild);
        percentRow.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 20.0f));
        expect(near(percentChild.layoutBounds().width(), 100.0f),
                "Flex resolver should resolve percent main-axis sizes against parent content width");

        HBox paddedRow = new HBox();
        paddedRow.layout(style -> style.padding(10.0f));
        Box paddedChild = new Box();
        paddedChild.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        paddedRow.addChild(paddedChild);
        paddedRow.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 40.0f));
        expect(near(paddedChild.layoutBounds().x(), 10.0f)
                        && near(paddedChild.layoutBounds().y(), 10.0f)
                        && near(paddedChild.layoutBounds().width(), 180.0f)
                        && near(paddedChild.layoutBounds().height(), 20.0f),
                "Flex resolver should arrange children inside the container padding box");

        HBox centeredRow = new HBox();
        centeredRow.layout(style -> style
                .justifyContent(Justify.CENTER)
                .alignItems(Align.CENTER));
        Box centeredChild = new Box();
        centeredChild.layout(style -> style.width(40.0f).height(10.0f).flexShrink(0.0f));
        centeredRow.addChild(centeredChild);
        centeredRow.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 40.0f));
        expect(near(centeredChild.layoutBounds().x(), 30.0f)
                        && near(centeredChild.layoutBounds().y(), 15.0f),
                "Flex resolver should apply parent justifyContent and alignItems");
    }

    private void testOverflowAndScrollContracts() {
        PanelWidget visiblePanel = overflowPanel(Overflow.VISIBLE);
        DrawList visibleDrawList = new DrawList();
        visiblePanel.render(new DefaultRenderContext(visibleDrawList));
        expect(!hasCommand(visibleDrawList, DrawCommandType.PUSH_CLIP)
                        && !hasCommand(visibleDrawList, DrawCommandType.POP_CLIP),
                "Overflow.VISIBLE should render panel children without an automatic clip");

        for (Overflow overflow : new Overflow[]{Overflow.HIDDEN, Overflow.AUTO, Overflow.SCROLL}) {
            PanelWidget clippedPanel = overflowPanel(overflow);
            DrawList clippedDrawList = new DrawList();
            clippedPanel.render(new DefaultRenderContext(clippedDrawList));
            expect(clippedDrawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP
                            && clippedDrawList.commands().get(clippedDrawList.size() - 1).type() == DrawCommandType.POP_CLIP,
                    "Non-visible panel overflow should wrap child rendering in a balanced clip: " + overflow);
        }

        PanelWidget outer = new PanelWidget();
        PanelWidget inner = new PanelWidget();
        Box nestedChild = solidTestBox();
        outer.layout(style -> style.overflow(Overflow.HIDDEN));
        inner.layout(style -> style.overflow(Overflow.AUTO));
        inner.addChild(nestedChild);
        outer.addChild(inner);
        inner.applyQueuedMutations();
        outer.applyQueuedMutations();
        outer.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        DrawList nestedDrawList = new DrawList();
        outer.render(new DefaultRenderContext(nestedDrawList));
        expect(countCommands(nestedDrawList, DrawCommandType.PUSH_CLIP) == 2
                        && countCommands(nestedDrawList, DrawCommandType.POP_CLIP) == 2,
                "Nested overflow panels should keep the clip stack balanced");

        ScrollView defaults = new ScrollView();
        expect(defaults.layoutStyle().overflowX() == Overflow.HIDDEN
                        && defaults.layoutStyle().overflowY() == Overflow.AUTO,
                "ScrollView should default to hidden horizontal overflow and automatic vertical overflow");

        DefaultUIContext uiContext = new DefaultUIContext();
        Box autoContent = new Box();
        ScrollView automatic = new ScrollView(autoContent).contentSize(200.0f, 300.0f);
        automatic.layout(style -> style.overflowX(Overflow.AUTO).overflowY(Overflow.AUTO));
        automatic.setUiContextInternal(uiContext);
        automatic.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        expect(automatic.children().size() == 3,
                "AUTO overflow should expose content and both scrollbars when both axes overflow");
        expect(near(autoContent.layoutBounds().width(), 200.0f)
                        && near(automatic.horizontalScrollBar().layoutBounds().width(), 86.0f)
                        && near(automatic.verticalScrollBar().layoutBounds().height(), 86.0f),
                "Visible scrollbars should reduce the opposite viewport axis");
        expect(near(automatic.maxScrollX(), 114.0f) && near(automatic.maxScrollY(), 214.0f),
                "Scroll ranges should use the viewport remaining after both scrollbars");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(automatic.horizontalScrollBar(),
                47.0f, 97.0f, 47.0f, 3.0f, 0, PointerButton.PRIMARY));
        expect(automatic.horizontalScrollBar().dragging()
                        && uiContext.capturedPointer(0) == automatic.horizontalScrollBar()
                        && near(automatic.scrollX(), 57.0f),
                "Dragging the horizontal scrollbar should capture the pointer and update scrollX");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(automatic.horizontalScrollBar(),
                160.0f, 130.0f, 160.0f, 130.0f, 0, PointerButton.PRIMARY));
        expect(!automatic.horizontalScrollBar().dragging() && uiContext.capturedPointer(0) == null,
                "Horizontal scrollbar drag should release capture outside its visual bounds");

        automatic.scrollTo(0.0f, 0.0f);
        boolean shiftWheelConsumed = uiContext.routedEvents().dispatch(new ScrollEvent(
                autoContent, 20.0f, 20.0f, 20.0f, 20.0f,
                0.0f, -1.0f, KeyModifiers.SHIFT));
        expect(shiftWheelConsumed
                        && near(automatic.scrollX(), automatic.scrollStep())
                        && automatic.scrollY() == 0.0f,
                "Shift + vertical wheel should scroll a horizontal ScrollView axis");

        automatic.scrollTo(100.0f, 100.0f);
        automatic.layout(style -> style.overflow(Overflow.HIDDEN));
        automatic.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        expect(automatic.scrollX() == 0.0f && automatic.scrollY() == 0.0f
                        && automatic.children().size() == 1,
                "HIDDEN overflow should disable scrolling, hide bars and reset stale offsets");

        ScrollView forced = new ScrollView(new Box()).contentSize(20.0f, 20.0f);
        forced.layout(style -> style.overflow(Overflow.SCROLL));
        forced.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        expect(forced.children().size() == 3
                        && forced.maxScrollX() == 0.0f
                        && forced.maxScrollY() == 0.0f,
                "SCROLL overflow should keep both bars visible even when content fits");
    }

    private void testAbsoluteLayoutContracts() {
        StackPanel host = new StackPanel();
        host.layout(style -> style.padding(10.0f));

        Box relative = new Box();
        relative.layout(style -> style.size(40.0f, 20.0f).align(Alignment.START, Alignment.START).flexGrow(0).flexShrink(0.0f));

        Box positioned = new Box();
        positioned.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(5.0f)
                .top(6.0f)
                .width(30.0f)
                .height(12.0f));

        Box insetStretch = new Box();
        insetStretch.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(10.0f)
                .right(20.0f)
                .top(5.0f)
                .bottom(15.0f));

        host.addChild(relative);
        host.addChild(positioned);
        host.addChild(insetStretch);
        host.measure(new LayoutContext(200.0f, 100.0f));
        expect(near(host.desiredSize().width(), 60.0f) && near(host.desiredSize().height(), 40.0f),
                "Absolute children should not contribute to parent desired size while padding remains in the box model");

        host.arrange(new MutableRect(100.0f, 50.0f, 200.0f, 100.0f));
        expect(near(relative.layoutBounds().x(), 110.0f) && near(relative.layoutBounds().y(), 60.0f),
                "Relative children should use the parent content box after padding");
        expect(near(positioned.layoutBounds().x(), 115.0f)
                        && near(positioned.layoutBounds().y(), 66.0f)
                        && near(positioned.layoutBounds().width(), 30.0f)
                        && near(positioned.layoutBounds().height(), 12.0f),
                "Absolute left/top offsets should resolve from the parent content box");
        expect(near(insetStretch.layoutBounds().x(), 120.0f)
                        && near(insetStretch.layoutBounds().y(), 65.0f)
                        && near(insetStretch.layoutBounds().width(), 150.0f)
                        && near(insetStretch.layoutBounds().height(), 60.0f),
                "Absolute left/right/top/bottom insets should stretch an auto-sized child");

        Box unclampedHost = new Box();
        Box unclampedChild = new Box();
        unclampedChild.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(90.0f)
                .top(4.0f)
                .width(30.0f)
                .height(10.0f));
        unclampedHost.addChild(unclampedChild);
        unclampedHost.measure(new LayoutContext(100.0f, 50.0f));
        unclampedHost.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        expect(unclampedHost.desiredSize().width() == 0.0f
                        && near(unclampedChild.layoutBounds().x(), 90.0f)
                        && near(unclampedChild.layoutBounds().width(), 30.0f),
                "Generic absolute children should stay out of flow without implicit host clamping");
    }

    private void testRicherLayoutContainers() {
        expect(Widgets.stack() instanceof StackPanel, "Widgets.stack should create StackPanel");
        expect(Widgets.dock() instanceof DockPanel, "Widgets.dock should create DockPanel");
        expect(Widgets.wrap() instanceof WrapPanel, "Widgets.wrap should create WrapPanel");
        expect(Widgets.scrollView() instanceof ScrollView, "Widgets.scrollView should create an empty ScrollView");
        expect(Widgets.overlayLayer() instanceof OverlayLayer, "Widgets.overlayLayer should create an empty OverlayLayer");

        StackPanel stack = new StackPanel();
        Button full = new Button("Full");
        Button overlay = new Button("Overlay");
        overlay.layout(style -> style.size(40.0f, 20.0f).margin(5.0f).align(Alignment.END, Alignment.CENTER));
        stack.addChild(full);
        stack.addChild(overlay);
        stack.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));

        expect(near(full.layoutBounds().x(), 0.0f) && near(full.layoutBounds().y(), 0.0f)
                        && near(full.layoutBounds().width(), 100.0f) && near(full.layoutBounds().height(), 60.0f),
                "StackPanel should stretch default children over the full slot");
        expect(near(overlay.layoutBounds().x(), 55.0f) && near(overlay.layoutBounds().y(), 20.0f)
                        && near(overlay.layoutBounds().width(), 40.0f) && near(overlay.layoutBounds().height(), 20.0f),
                "StackPanel should honor margin, preferred size and alignment for overlays");

        DockPanel dock = new DockPanel();
        Button top = new Button("Top");
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button fill = new Button("Fill");
        top.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f));
        left.layout(style -> style.size(30.0f, LayoutConstraints.AUTO));
        right.layout(style -> style.size(40.0f, LayoutConstraints.AUTO));
        dock.addChild(top, DockSide.TOP);
        dock.addChild(left, DockSide.LEFT);
        dock.addChild(right, DockSide.RIGHT);
        dock.addChild(fill, DockSide.LEFT);
        dock.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));

        expect(near(top.layoutBounds().x(), 0.0f) && near(top.layoutBounds().y(), 0.0f)
                        && near(top.layoutBounds().width(), 200.0f) && near(top.layoutBounds().height(), 20.0f),
                "DockPanel should dock top children across the remaining width");
        expect(near(left.layoutBounds().x(), 0.0f) && near(left.layoutBounds().y(), 20.0f)
                        && near(left.layoutBounds().width(), 30.0f) && near(left.layoutBounds().height(), 80.0f),
                "DockPanel should dock left children and shrink the remaining rect");
        expect(near(right.layoutBounds().x(), 160.0f) && near(right.layoutBounds().y(), 20.0f)
                        && near(right.layoutBounds().width(), 40.0f) && near(right.layoutBounds().height(), 80.0f),
                "DockPanel should dock right children against the remaining edge");
        expect(near(fill.layoutBounds().x(), 30.0f) && near(fill.layoutBounds().y(), 20.0f)
                        && near(fill.layoutBounds().width(), 130.0f) && near(fill.layoutBounds().height(), 80.0f),
                "DockPanel should fill the remaining rect with the last child by default");

        WrapPanel wrap = new WrapPanel().spacing(5.0f).lineSpacing(10.0f);
        Button first = new Button("A");
        Button second = new Button("B");
        Button skipped = new Button("Skipped");
        Button third = new Button("C");
        first.layout(style -> style.size(40.0f, 10.0f));
        second.layout(style -> style.size(40.0f, 20.0f));
        skipped.layout(style -> style.size(100.0f, 100.0f)).visibility(Visibility.COLLAPSED);
        skipped.arrange(new MutableRect(7.0f, 8.0f, 9.0f, 10.0f));
        third.layout(style -> style.size(40.0f, 10.0f));
        wrap.addChild(first);
        wrap.addChild(second);
        wrap.addChild(skipped);
        wrap.addChild(third);
        wrap.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        expect(near(first.layoutBounds().x(), 0.0f) && near(first.layoutBounds().y(), 0.0f)
                        && near(first.layoutBounds().width(), 40.0f) && near(first.layoutBounds().height(), 10.0f),
                "WrapPanel should place the first horizontal item at the line origin");
        expect(near(second.layoutBounds().x(), 45.0f) && near(second.layoutBounds().y(), 0.0f)
                        && near(second.layoutBounds().width(), 40.0f) && near(second.layoutBounds().height(), 20.0f),
                "WrapPanel should apply horizontal spacing inside a line");
        expect(near(third.layoutBounds().x(), 0.0f) && near(third.layoutBounds().y(), 30.0f)
                        && near(third.layoutBounds().width(), 40.0f) && near(third.layoutBounds().height(), 10.0f),
                "WrapPanel should wrap to the next line when the next item exceeds available width");
        expect(near(skipped.layoutBounds().x(), 7.0f) && near(skipped.layoutBounds().width(), 9.0f),
                "WrapPanel should skip collapsed children during layout");

        WrapPanel narrowWrap = new WrapPanel();
        Button oversized = new Button("Oversized");
        oversized.layout(style -> style.size(140.0f, 14.0f).flexGrow(0).flexShrink(0.0f));
        narrowWrap.addChild(oversized);
        narrowWrap.measure(new LayoutContext(80.0f, 40.0f));
        narrowWrap.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        expect(near(oversized.layoutBounds().width(), 80.0f),
                "WrapPanel should clamp oversized horizontal children to the available line width");

        VBox wrappedToolbarLayout = new VBox();
        wrappedToolbarLayout.spacing(8.0f);
        WrapPanel toolbar = new WrapPanel().spacing(6.0f).lineSpacing(4.0f);
        Button search = new Button("Search");
        Button slider = new Button("Slider");
        Button progress = new Button("Progress");
        Button number = new Button("42");
        search.layout(style -> style.size(120.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        slider.layout(style -> style.size(130.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        progress.layout(style -> style.size(100.0f, 12.0f).flexGrow(0).flexShrink(0.0f));
        number.layout(style -> style.size(70.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        toolbar.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        toolbar.addChild(search);
        toolbar.addChild(slider);
        toolbar.addChild(progress);
        toolbar.addChild(number);
        Box main = new Box();
        main.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        wrappedToolbarLayout.addChild(toolbar);
        wrappedToolbarLayout.addChild(main);
        wrappedToolbarLayout.measure(new LayoutContext(260.0f, 140.0f));
        wrappedToolbarLayout.arrange(new MutableRect(0.0f, 0.0f, 260.0f, 140.0f));

        expect(toolbar.layoutBounds().height() >= 44.0f,
                "VBox should reserve measured multi-line WrapPanel height");
        expect(main.layoutBounds().y() >= toolbar.layoutBounds().y() + toolbar.layoutBounds().height() + 8.0f,
                "VBox should arrange grow content below a wrapped toolbar without overlap");

        WrapPanel verticalWrap = new WrapPanel().orientation(Orientation.VERTICAL).spacing(5.0f).lineSpacing(10.0f);
        Button verticalFirst = new Button("VA");
        Button verticalSecond = new Button("VB");
        Button verticalThird = new Button("VC");
        verticalFirst.layout(style -> style.size(10.0f, 40.0f));
        verticalSecond.layout(style -> style.size(20.0f, 40.0f));
        verticalThird.layout(style -> style.size(10.0f, 40.0f));
        verticalWrap.addChild(verticalFirst);
        verticalWrap.addChild(verticalSecond);
        verticalWrap.addChild(verticalThird);
        verticalWrap.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        expect(near(verticalFirst.layoutBounds().x(), 0.0f) && near(verticalFirst.layoutBounds().y(), 0.0f)
                        && near(verticalFirst.layoutBounds().width(), 10.0f) && near(verticalFirst.layoutBounds().height(), 40.0f),
                "Vertical WrapPanel should place the first item at the column origin");
        expect(near(verticalSecond.layoutBounds().x(), 0.0f) && near(verticalSecond.layoutBounds().y(), 45.0f)
                        && near(verticalSecond.layoutBounds().width(), 20.0f) && near(verticalSecond.layoutBounds().height(), 40.0f),
                "Vertical WrapPanel should apply spacing inside a column");
        expect(near(verticalThird.layoutBounds().x(), 30.0f) && near(verticalThird.layoutBounds().y(), 0.0f)
                        && near(verticalThird.layoutBounds().width(), 10.0f) && near(verticalThird.layoutBounds().height(), 40.0f),
                "Vertical WrapPanel should wrap to the next column when the next item exceeds available height");
    }

    private void testSliderPointerAndKeyboardInput() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Slider slider = new Slider().range(0.0f, 100.0f).step(5.0f);
        slider.setUiContextInternal(uiContext);
        slider.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 20.0f));

        Counter changes = new Counter();
        slider.onValueChanged((SliderValueChangedEvent event) -> {
            changes.count++;
            changes.lastValue = event.newValue();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(slider, 50.0f, 10.0f, 50.0f, 10.0f, 0, PointerButton.PRIMARY));
        expect(slider.dragging(), "Slider should start dragging on primary pointer press");
        expect(slider.value() == 25.0f, "Slider should map pointer position to stepped value");
        expect(uiContext.focusManager().focusedWidget() == slider, "Slider should request focus on pointer press");
        expect(uiContext.capturedPointer(0) == slider, "Slider should capture pointer while dragging");

        uiContext.routedEvents().dispatch(new PointerMovedEvent(slider, 260.0f, 10.0f, 260.0f, 10.0f, 0));
        expect(slider.value() == 100.0f, "Captured Slider drag should continue and clamp outside its visual bounds");

        uiContext.routedEvents().dispatch(new PointerReleasedEvent(slider, 260.0f, 10.0f, 260.0f, 10.0f, 0, PointerButton.PRIMARY));
        expect(!slider.dragging(), "Slider should stop dragging on release");
        expect(uiContext.capturedPointer(0) == null, "Slider should release pointer capture after drag release");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(slider, KeyCodes.LEFT, 0, 0));
        expect(slider.value() == 95.0f, "Focused Slider should nudge left by step");
        expect(changes.count >= 3 && changes.lastValue == 95.0f, "Slider should emit value changed events");
    }

    private void testScrollViewBubbledWheelInput() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box content = new Box();
        ScrollView scrollView = new ScrollView(content).contentSize(100.0f, 300.0f).scrollStep(20.0f);
        scrollView.setUiContextInternal(uiContext);
        scrollView.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));

        boolean consumed = uiContext.routedEvents().dispatch(new ScrollEvent(content, 10.0f, 10.0f, 10.0f, 10.0f, 0.0f, -1.0f));
        expect(consumed, "ScrollView should consume bubbled scroll when offset changes");
        expect(scrollView.scrollY() == 20.0f, "ScrollView should scroll down on negative wheel delta");

        scrollView.scrollTo(0.0f, 500.0f);
        expect(scrollView.scrollY() == 200.0f, "ScrollView should clamp to max scroll");

        scrollView.scrollBy(0.0f, -500.0f);
        expect(scrollView.scrollY() == 0.0f, "ScrollView should clamp to zero");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(scrollView.verticalScrollBar(),
                97.0f, 50.0f, 3.0f, 50.0f, 0, PointerButton.PRIMARY));
        expect(scrollView.verticalScrollBar().dragging(), "ScrollBar should start dragging on primary pointer press");
        expect(uiContext.capturedPointer(0) == scrollView.verticalScrollBar(), "ScrollBar should capture pointer while dragging");
        expect(near(scrollView.scrollY(), 100.0f), "Dragging vertical ScrollBar should update ScrollView scrollY");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(scrollView.verticalScrollBar(),
                97.0f, 180.0f, 3.0f, 180.0f, 0));
        expect(near(scrollView.scrollY(), 200.0f), "Captured ScrollBar drag should continue outside its visual bounds");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(scrollView.verticalScrollBar(),
                97.0f, 180.0f, 3.0f, 180.0f, 0, PointerButton.PRIMARY));
        expect(!scrollView.verticalScrollBar().dragging(), "ScrollBar should stop dragging on release");
        expect(uiContext.capturedPointer(0) == null, "ScrollBar should release pointer capture after drag release");

        DrawList drawList = new DrawList();
        scrollView.render(new DefaultRenderContext(drawList));
        expect(drawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP, "ScrollView should push a clip before rendering content");
        expect(hasCommand(drawList, DrawCommandType.POP_CLIP), "ScrollView should pop the clip after rendering content");

        Box measuredContent = new Box();
        measuredContent.layout(style -> style.size(100.0f, 260.0f).flexGrow(0).flexShrink(0.0f));
        ScrollView autoContentScrollView = new ScrollView(measuredContent).scrollStep(10.0f);
        autoContentScrollView.setUiContextInternal(uiContext);
        autoContentScrollView.measure(new LayoutContext(100.0f, 80.0f));
        autoContentScrollView.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));
        autoContentScrollView.scrollTo(0.0f, 500.0f);
        expect(autoContentScrollView.scrollY() == 180.0f, "ScrollView should use measured content height when contentSize is not explicit");

        Box viewportWidthContent = new Box();
        viewportWidthContent.layout(style -> style.size(240.0f, 120.0f).flexGrow(0).flexShrink(0.0f));
        ScrollView viewportWidthScrollView = new ScrollView(viewportWidthContent);
        viewportWidthScrollView.measure(new LayoutContext(240.0f, 80.0f));
        viewportWidthScrollView.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));
        expect(viewportWidthScrollView.maxScrollX() == 0.0f, "ScrollView should not create implicit horizontal overflow without explicit content width");
        expect(viewportWidthContent.layoutBounds().width() == 86.0f,
                "ScrollView should arrange implicit-width content to viewport width after the vertical scrollbar");

        Box hiddenAxisContent = new Box();
        ScrollView hiddenAxisScrollView = new ScrollView(hiddenAxisContent)
                .contentSize(240.0f, 120.0f);
        hiddenAxisScrollView.measure(new LayoutContext(100.0f, 80.0f));
        hiddenAxisScrollView.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 80.0f));
        expect(hiddenAxisScrollView.verticalScrollBar().layoutBounds().width() == 6.0f
                        && hiddenAxisContent.layoutBounds().width() == 86.0f,
                "ScrollView should reserve the scrollbar element when horizontal overflow is hidden");
    }

    private void testScrollViewNestedWheelLockAndOptOut() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Box innerContent = new Box();
        ScrollView inner = new ScrollView(innerContent).contentSize(100.0f, 300.0f).scrollStep(20.0f);
        ScrollView outer = new ScrollView(inner).contentSize(100.0f, 300.0f).scrollStep(20.0f);
        outer.setUiContextInternal(uiContext);
        outer.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        inner.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        inner.scrollTo(0.0f, inner.maxScrollY());
        outer.scrollTo(0.0f, 0.0f);

        boolean consumedAtInnerEdge = uiContext.routedEvents().dispatch(
                new ScrollEvent(innerContent, 5.0f, 5.0f, 5.0f, 5.0f, 0.0f, -1.0f));
        expect(consumedAtInnerEdge && outer.scrollY() == 0.0f,
                "Nested ScrollView should keep wheel ownership while hovered even at its scroll bounds");

        inner.consumeWheelAtScrollBounds(false);
        boolean bubbledAtInnerEdge = uiContext.routedEvents().dispatch(
                new ScrollEvent(innerContent, 5.0f, 5.0f, 5.0f, 5.0f, 0.0f, -1.0f));
        expect(bubbledAtInnerEdge && outer.scrollY() == 20.0f,
                "consumeWheelAtScrollBounds(false) should allow parent ScrollView fallback at inner bounds");
    }

    private void testNestedScissorStack() {
        ScissorStack scissorStack = new ScissorStack();

        ScissorStack.Rect root = scissorStack.push(new MutableRect(10.0f, 10.0f, 100.0f, 80.0f));
        expect(root.equals(new ScissorStack.Rect(10, 10, 110, 90)), "ScissorStack should push root clip bounds");

        ScissorStack.Rect nested = scissorStack.push(new MutableRect(50.0f, 0.0f, 80.0f, 50.0f));
        expect(nested.equals(new ScissorStack.Rect(50, 10, 110, 50)), "Nested scissor should intersect with parent bounds");

        ScissorStack.Rect restored = scissorStack.pop();
        expect(restored.equals(root), "Popping nested scissor should restore parent clip");

        ScissorStack.Rect empty = scissorStack.push(new MutableRect(500.0f, 500.0f, 20.0f, 20.0f));
        expect(empty.equals(new ScissorStack.Rect(500, 500, 500, 500)), "Non-overlapping nested scissor should collapse to empty rect");

        scissorStack.clear();
        expect(scissorStack.isEmpty(), "ScissorStack clear should remove all clip state");
    }

    private void testWidgetAnimationTransitions() {
        TransitionSpec linear = TransitionSpec.of(1.0f, AnimationEasing.LINEAR);
        Box box = new Box();
        box.opacity(0.20f)
                .animateOpacity(1.0f, linear)
                .animatePosition(30.0f, 12.0f, linear)
                .animateScale(2.0f, 0.5f, linear);

        box.tick(new FrameContext(1, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.opacity(), 0.60f), "Widget opacity transition should interpolate with frame delta");
        expect(near(box.transform().position().x(), 15.0f) && near(box.transform().position().y(), 6.0f),
                "Widget position transition should interpolate transform position");
        expect(near(box.transform().scale().x(), 1.5f) && near(box.transform().scale().y(), 0.75f),
                "Widget scale transition should interpolate transform scale");
        expect(box.animationRunning(AnimatedProperty.OPACITY), "Widget transition should report running state before completion");

        box.tick(new FrameContext(2, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.opacity(), 1.0f) && !box.animationsRunning(),
                "Widget transitions should finish and clear running state");


        box.arrange(new MutableRect(10.0f, 20.0f, 100.0f, 40.0f));
        box.transformOrigin(TransformOrigin.CENTER)
                .rotationDegrees(0.0f)
                .animateRotation(90.0f, linear)
                .animatePositionFrom(-10.0f, 5.0f, 10.0f, 15.0f, linear);
        expect(near(box.transform().pivot().x(), 50.0f) && near(box.transform().pivot().y(), 20.0f),
                "Named transform origin should resolve pivot from layout bounds");
        box.tick(new FrameContext(3, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.transform().rotationDegrees(), 45.0f)
                        && near(box.transform().position().x(), 0.0f)
                        && near(box.transform().position().y(), 10.0f),
                "Widget rotation and point-to-point position transitions should interpolate");
        box.tick(new FrameContext(4, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.transform().rotationDegrees(), 90.0f)
                        && near(box.transform().position().x(), 10.0f)
                        && near(box.transform().position().y(), 15.0f),
                "Widget rotation and point-to-point transitions should finish at target values");

        box.shake(6.0f, 0.0f, 1.0f, 1);
        box.tick(new FrameContext(5, 0.25f, 0.0f, FramePhase.ANIMATION));
        expect(Math.abs(box.transform().position().x() - 14.5f) < 0.001f,
                "Shake effect should add a decaying temporary transform offset");
        box.tick(new FrameContext(6, 0.75f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.transform().position().x(), 10.0f) && !box.animationsRunning(),
                "Shake effect should restore base transform position when finished");

        box.background().set(0.0f, 0.0f, 0.0f, 1.0f);
        box.animateBackgroundColor(new MutableColor(1.0f, 0.5f, 0.25f, 0.5f), linear);
        box.animateRadius(8.0f, linear);
        box.tick(new FrameContext(7, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.background().r(), 0.5f)
                        && near(box.background().g(), 0.25f)
                        && near(box.background().b(), 0.125f)
                        && near(box.background().a(), 0.75f)
                        && near(box.radius(), 4.0f),
                "Widget color and generic parameter transitions should interpolate");
        box.tick(new FrameContext(8, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(box.background().r(), 1.0f) && near(box.radius(), 8.0f) && !box.animationsRunning(),
                "Widget color and generic parameter transitions should complete");

        SimpleTextureHandle firstTexture = new SimpleTextureHandle("test:first", 16, 16);
        SimpleTextureHandle secondTexture = new SimpleTextureHandle("test:second", 16, 16);
        TextureWidget textureWidget = new TextureWidget(firstTexture);
        textureWidget.arrange(new MutableRect(0.0f, 0.0f, 16.0f, 16.0f));
        textureWidget.animateTexture(secondTexture, linear);
        textureWidget.tick(new FrameContext(9, 0.5f, 0.0f, FramePhase.ANIMATION));
        DrawList textureFadeDrawList = new DrawList();
        textureWidget.render(new DefaultRenderContext(textureFadeDrawList));
        expect(textureFadeDrawList.commands().size() == 2,
                "TextureWidget crossfade should render old and new textures during transition");
        textureWidget.tick(new FrameContext(10, 0.5f, 0.0f, FramePhase.ANIMATION));
        DrawList textureDoneDrawList = new DrawList();
        textureWidget.render(new DefaultRenderContext(textureDoneDrawList));
        expect(textureDoneDrawList.commands().size() == 1 && textureWidget.texture() == secondTexture,
                "TextureWidget crossfade should render only the new texture after completion");

        StackPanel root = new StackPanel();
        root.opacity(0.50f);
        Box child = new Box();
        child.backgroundVisible(true);
        child.themeEnabled(false);
        child.background().set(0.3f, 0.4f, 0.5f, 0.8f);
        child.opacity(0.50f);
        child.layout(style -> style.size(40.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        root.addChild(child);
        root.measure(new LayoutContext(80.0f, 40.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        DrawList fadedDrawList = new DrawList();
        root.render(new DefaultRenderContext(fadedDrawList));
        expect(hasFillColor(fadedDrawList, 0.3f, 0.4f, 0.5f, 0.20f),
                "Nested widget opacity should multiply rendered paint alpha");

        Label hiddenText = new Label("Invisible");
        hiddenText.opacity(0.0f);
        hiddenText.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 16.0f));
        DrawList hiddenTextDrawList = new DrawList();
        hiddenText.render(new DefaultRenderContext(hiddenTextDrawList));
        expect(hiddenTextDrawList.commands().stream()
                        .filter(command -> command.type() == DrawCommandType.TEXT)
                        .allMatch(command -> near(command.paint().color().a(), 0.0f)),
                "Label opacity(0) should emit fully transparent text paint");

        Box looping = new Box();
        looping.opacity(0.0f).animateOpacity(1.0f, TransitionSpec.of(1.0f, AnimationEasing.LINEAR).loop().yoyo());
        looping.tick(new FrameContext(11, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(looping.opacity(), 0.5f) && looping.animationRunning(AnimatedProperty.OPACITY),
                "Infinite yoyo transition should interpolate forward and keep running");
        looping.tick(new FrameContext(12, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(looping.opacity(), 1.0f) && looping.animationRunning(AnimatedProperty.OPACITY),
                "Infinite yoyo transition should reach the target without finishing");
        looping.tick(new FrameContext(13, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(looping.opacity(), 0.5f) && looping.animationRunning(AnimatedProperty.OPACITY),
                "Infinite yoyo transition should interpolate backward on the next cycle");
        looping.stopAnimation(AnimatedProperty.OPACITY);
        float stoppedOpacity = looping.opacity();
        looping.tick(new FrameContext(14, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(looping.opacity(), stoppedOpacity) && !looping.animationRunning(AnimatedProperty.OPACITY),
                "stopAnimation should stop an infinite transition at its current value");

        Box finiteYoyo = new Box();
        finiteYoyo.opacity(0.0f).animateOpacity(1.0f, TransitionSpec.of(1.0f, AnimationEasing.LINEAR).repeat(1).yoyo());
        finiteYoyo.tick(new FrameContext(15, 0.5f, 0.0f, FramePhase.ANIMATION));
        finiteYoyo.tick(new FrameContext(16, 0.5f, 0.0f, FramePhase.ANIMATION));
        finiteYoyo.tick(new FrameContext(17, 0.5f, 0.0f, FramePhase.ANIMATION));
        finiteYoyo.tick(new FrameContext(18, 0.5f, 0.0f, FramePhase.ANIMATION));
        expect(near(finiteYoyo.opacity(), 0.0f) && !finiteYoyo.animationRunning(AnimatedProperty.OPACITY),
                "Finite yoyo repeat should finish at the original start value");

        Button button = new Button("Animated");
        button.interactionTransitions(true)
                .interactionTransition(TransitionSpec.of(0.10f, AnimationEasing.LINEAR))
                .interactionScales(1.0f, 1.10f, 0.90f)
                .interactionOpacities(1.0f, 0.80f, 0.50f);
        button.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        button.handle(new PointerEnteredEvent(button, 4.0f, 4.0f, 4.0f, 4.0f, 0));
        button.tick(new FrameContext(3, 0.10f, 0.0f, FramePhase.ANIMATION));
        expect(near(button.transform().scale().x(), 1.10f), "Button hover interaction transition should animate scale");

        button.handle(new PointerPressedEvent(button, 4.0f, 4.0f, 4.0f, 4.0f, 0, PointerButton.PRIMARY));
        button.tick(new FrameContext(4, 0.10f, 0.0f, FramePhase.ANIMATION));
        expect(near(button.opacity(), 0.80f) && near(button.transform().scale().x(), 0.90f),
                "Button pressed interaction transition should animate opacity and scale");


        Button rotatedButton = new Button("Pivot text");
        rotatedButton.themeEnabled(false);
        rotatedButton.transformOrigin(TransformOrigin.CENTER).rotationDegrees(18.0f);
        rotatedButton.arrange(new MutableRect(20.0f, 30.0f, 120.0f, 24.0f));
        DrawList rotatedButtonDrawList = new DrawList();
        rotatedButton.render(new DefaultRenderContext(rotatedButtonDrawList));
        DrawCommand rotatedShape = null;
        DrawCommand rotatedText = null;
        for (DrawCommand command : rotatedButtonDrawList.commands()) {
            if (near(command.transform().rotationDegrees(), 18.0f)) {
                if (rotatedShape == null && command.type() != DrawCommandType.TEXT) {
                    rotatedShape = command;
                } else if (rotatedText == null && command.type() == DrawCommandType.TEXT) {
                    rotatedText = command;
                }
            }
        }
        expect(rotatedShape != null && rotatedText != null,
                "Rotated Button should transform both chrome and text draw commands");
        expect(near(rotatedShape.bounds().x() + rotatedShape.transform().pivot().x(),
                        rotatedText.bounds().x() + rotatedText.transform().pivot().x())
                        && near(rotatedShape.bounds().y() + rotatedShape.transform().pivot().y(),
                        rotatedText.bounds().y() + rotatedText.transform().pivot().y()),
                "Rotated Button text should share the widget transform origin with its chrome");

        Box transformedParent = new Box();
        transformedParent.themeEnabled(false);
        transformedParent.backgroundVisible(true);
        transformedParent.transformOrigin(TransformOrigin.CENTER).rotationDegrees(22.0f);
        Label inheritedLabel = new Label("Inherited child transform");
        inheritedLabel.layout(style -> style.margin(6.0f).size(LayoutConstraints.AUTO, 16.0f).flexGrow(0).flexShrink(0.0f));
        transformedParent.addChild(inheritedLabel);
        transformedParent.measure(new LayoutContext(160.0f, 60.0f));
        transformedParent.arrange(new MutableRect(10.0f, 20.0f, 120.0f, 34.0f));
        DrawList inheritedDrawList = new DrawList();
        transformedParent.render(new DefaultRenderContext(inheritedDrawList));
        DrawCommand inheritedText = null;
        for (DrawCommand command : inheritedDrawList.commands()) {
            if (command.type() == DrawCommandType.TEXT
                    && command.richText() != null
                    && command.richText().plainText().equals("Inherited child transform")) {
                inheritedText = command;
                break;
            }
        }
        expect(inheritedText != null && inheritedText.transformStack().size() == 1,
                "Child text command should capture the parent transform stack");
        expect(near(inheritedText.transformStack().get(0).bounds().x(), transformedParent.layoutBounds().x())
                        && near(inheritedText.transformStack().get(0).bounds().y(), transformedParent.layoutBounds().y())
                        && near(inheritedText.transformStack().get(0).transform().rotationDegrees(), 22.0f),
                "Child text command should inherit the parent bounds and rotation");

        Box nestedParent = new Box();
        nestedParent.themeEnabled(false);
        nestedParent.transformOrigin(TransformOrigin.CENTER).rotationDegrees(12.0f);
        Box nestedChild = new Box();
        nestedChild.themeEnabled(false);
        nestedChild.transformOrigin(TransformOrigin.CENTER).rotationDegrees(-6.0f);
        Label nestedLabel = new Label("Nested transform stack");
        nestedLabel.layout(style -> style.margin(4.0f).size(LayoutConstraints.AUTO, 14.0f).flexGrow(0).flexShrink(0.0f));
        nestedChild.addChild(nestedLabel);
        nestedParent.addChild(nestedChild);
        nestedParent.measure(new LayoutContext(180.0f, 80.0f));
        nestedParent.arrange(new MutableRect(0.0f, 0.0f, 140.0f, 42.0f));
        DrawList nestedDrawList = new DrawList();
        nestedParent.render(new DefaultRenderContext(nestedDrawList));
        DrawCommand nestedText = null;
        for (DrawCommand command : nestedDrawList.commands()) {
            if (command.type() == DrawCommandType.TEXT
                    && command.richText() != null
                    && command.richText().plainText().equals("Nested transform stack")) {
                nestedText = command;
                break;
            }
        }
        expect(nestedText != null && nestedText.transformStack().size() == 2,
                "Nested child text should preserve all ancestor transforms in order");
        expect(near(nestedText.transformStack().get(0).transform().rotationDegrees(), 12.0f)
                        && near(nestedText.transformStack().get(1).transform().rotationDegrees(), -6.0f),
                "Nested transform stack should be ordered from root parent to leaf parent");
    }

    private void testMinecraftPreviewWidgetFallbacks() {
        TestMinecraftPreviewWidget preview = new TestMinecraftPreviewWidget("Preview", "fallback");
        preview.previewSize(32.0f).layout(style -> style.size(70.0f, 58.0f).flexGrow(0).flexShrink(0.0f));
        preview.measure(new LayoutContext(100.0f, 80.0f));
        preview.arrange(new MutableRect(0.0f, 0.0f, 70.0f, 58.0f));
        DrawList drawList = new DrawList();
        preview.render(new DefaultRenderContext(drawList));
        expect(hasCommand(drawList, DrawCommandType.CUSTOM), "Minecraft preview shell should emit backend custom draw command");
        expect(hasText(drawList, "fallback") && hasText(drawList, "Preview"),
                "Minecraft preview shell should render fallback text and label without Minecraft backend");

        preview.label("Updated").labelVisible(false).previewSize(40.0f);
        preview.measure(new LayoutContext(100.0f, 80.0f));
        expect(preview.label().equals("Updated") && !preview.labelVisible() && preview.previewSize() == 40.0f,
                "Minecraft preview shell should expose label and sizing contracts without bootstrapping Minecraft registries");
    }

    private void testMinecraftPickerWidgetsContracts() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        ResourceLocation copperId = ResourceLocation.tryBuild("example", "copper_gear");
        ResourceLocation wrenchId = ResourceLocation.tryBuild("example", "wrench");
        ResourceLocation diamondId = ResourceLocation.tryBuild("minecraft", "diamond");
        Item copper = Items.COPPER_INGOT;
        Item wrench = Items.TRIPWIRE_HOOK;
        Item diamond = Items.DIAMOND;

        MinecraftItemPickerWidget itemPicker = new MinecraftItemPickerWidget(false)
                .items(java.util.List.of(
                        new MinecraftItemPickerWidget.ItemEntry(wrenchId, wrench),
                        new MinecraftItemPickerWidget.ItemEntry(diamondId, diamond),
                        new MinecraftItemPickerWidget.ItemEntry(copperId, copper)));
        expect(itemPicker.itemCount() == 3
                        && itemPicker.items().get(0).id().equals(wrenchId)
                        && itemPicker.items().get(1).id().equals(diamondId)
                        && itemPicker.items().get(2).id().equals(copperId),
                "MinecraftItemPickerWidget should accept injected registry entries and preserve registry order");

        itemPicker.query("dia");
        expect(itemPicker.filteredItemCount() == 1
                        && itemPicker.filteredItems().get(0).id().equals(diamondId),
                "MinecraftItemPickerWidget should filter by namespaced id/path");

        OverlayLayer itemPickerLayer = new OverlayLayer(itemPicker);
        itemPickerLayer.setUiContextInternal(new DefaultUIContext());
        itemPickerLayer.measure(new LayoutContext(520.0f, 460.0f));
        itemPickerLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        itemPicker.open();
        itemPickerLayer.measure(new LayoutContext(520.0f, 460.0f));
        itemPickerLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        expect(itemPicker.opened()
                        && itemPicker.attachedOverlayLayer() == itemPickerLayer
                        && itemPickerLayer.windows().topModalWindow() == itemPicker.dialog(),
                "MinecraftItemPickerWidget should open its searchable icon grid as a modal WindowWidget");
        expect(itemPicker.resultGrid().itemCount() == 1
                        && itemPicker.resultGrid().realizedCount() == 1
                        && itemPicker.resultGrid().children().get(0).children().isEmpty(),
                "MinecraftItemPickerWidget modal grid should virtualize filtered item icons into optimized visible rows");

        Widget itemRow = itemPicker.resultGrid().children().get(0);
        float itemX = itemRow.layoutBounds().x() + 2.0f;
        float itemY = itemRow.layoutBounds().y() + 2.0f;
        itemRow.handle(new PointerEnteredEvent(itemRow, itemX, itemY, 2.0f, 2.0f, 0));
        Widget itemTooltipAnchor = itemPicker.idTooltip().anchor();
        expect(itemTooltipAnchor != null
                        && itemTooltipAnchor != itemRow
                        && itemTooltipAnchor.hovered()
                        && near(itemTooltipAnchor.layoutBounds().x(), itemRow.layoutBounds().x())
                        && near(itemTooltipAnchor.layoutBounds().y(), itemY)
                        && itemPicker.idTooltip().text().equals(diamondId.toString()),
                "MinecraftItemPickerWidget item icon tooltip should expose the full registry id from a tile anchor");

        Counter itemChanges = new Counter();
        itemPicker.onSelectionChanged(event -> {
            itemChanges.count++;
            itemChanges.lastSelection = event.newSelection();
        });
        itemRow.handle(new PointerPressedEvent(itemRow, itemX, itemY, 2.0f, 2.0f, 0, PointerButton.PRIMARY));
        expect(itemPicker.selectedId().equals(diamondId)
                        && itemPicker.selectedItem() == diamond
                        && itemPicker.selectedStack().getItem() == diamond
                        && itemPicker.selectedIndex() == 1
                        && itemPicker.selectedFilteredIndex() == 0
                        && itemPicker.selectedPreview().stack().getItem() == diamond
                        && itemPicker.openButton().text().equals(diamondId.toString())
                        && !itemPicker.opened()
                        && itemChanges.count == 1
                        && itemChanges.lastSelection.equals(java.util.List.of(1)),
                "MinecraftItemPickerWidget should map filtered grid selection back to the full item list");

        itemPicker.query("wrench");
        expect(itemPicker.selectedId().equals(diamondId)
                        && itemPicker.selectedFilteredIndex() == -1
                        && itemPicker.resultGrid().itemCount() == 1,
                "MinecraftItemPickerWidget should preserve selected item when filters hide it");

        itemPicker.query("");
        itemPicker.open();
        itemPickerLayer.measure(new LayoutContext(520.0f, 460.0f));
        itemPickerLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        Widget multiItemRow = itemPicker.resultGrid().children().get(0);
        float thirdItemLocalX = 78.0f;
        float thirdItemRootX = multiItemRow.layoutBounds().x() + thirdItemLocalX;
        float thirdItemRootY = multiItemRow.layoutBounds().y() + 2.0f;
        multiItemRow.handle(new PointerMovedEvent(multiItemRow, thirdItemRootX, thirdItemRootY, thirdItemLocalX, 2.0f, 0));
        Widget rightItemTooltipAnchor = itemPicker.idTooltip().anchor();
        expect(rightItemTooltipAnchor != null
                        && rightItemTooltipAnchor != multiItemRow
                        && rightItemTooltipAnchor.hovered()
                        && rightItemTooltipAnchor.layoutBounds().x() > multiItemRow.layoutBounds().x() + 60.0f
                        && itemPicker.idTooltip().text().equals(copperId.toString()),
                "SearchableGridPickerWidget should position tooltip anchor on the hovered tile, not the row");
        itemPicker.close();

        ResourceLocation stoneTexture = ResourceLocation.tryBuild("minecraft", "textures/block/stone.png");
        ResourceLocation zombieTexture = ResourceLocation.tryBuild("minecraft", "textures/entity/zombie/zombie.png");
        MinecraftTexturePickerWidget texturePicker = new MinecraftTexturePickerWidget(false)
                .textureIds(java.util.List.of(zombieTexture, stoneTexture));
        expect(texturePicker.textureCount() == 2
                        && texturePicker.textures().get(0).id().equals(zombieTexture)
                        && texturePicker.textures().get(1).id().equals(stoneTexture),
                "MinecraftTexturePickerWidget should accept injected texture ids and preserve resource order");

        texturePicker.query("block/stone");
        expect(texturePicker.filteredTextureCount() == 1
                        && texturePicker.filteredTextures().get(0).id().equals(stoneTexture),
                "MinecraftTexturePickerWidget should filter full and shortened texture paths");

        OverlayLayer texturePickerLayer = new OverlayLayer(texturePicker);
        texturePickerLayer.setUiContextInternal(new DefaultUIContext());
        texturePickerLayer.measure(new LayoutContext(520.0f, 460.0f));
        texturePickerLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        texturePicker.open();
        texturePickerLayer.measure(new LayoutContext(520.0f, 460.0f));
        texturePickerLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        expect(texturePicker.opened()
                        && texturePicker.attachedOverlayLayer() == texturePickerLayer
                        && texturePickerLayer.windows().topModalWindow() == texturePicker.dialog(),
                "MinecraftTexturePickerWidget should open its searchable icon grid as a modal WindowWidget");
        expect(texturePicker.resultGrid().itemCount() == 1
                        && texturePicker.resultGrid().realizedCount() == 1
                        && texturePicker.resultGrid().children().get(0).children().isEmpty(),
                "MinecraftTexturePickerWidget modal grid should virtualize filtered texture icons into optimized visible rows");

        Widget textureRow = texturePicker.resultGrid().children().get(0);
        float textureX = textureRow.layoutBounds().x() + 2.0f;
        float textureY = textureRow.layoutBounds().y() + 2.0f;
        textureRow.handle(new PointerEnteredEvent(textureRow, textureX, textureY, 2.0f, 2.0f, 0));
        Widget textureTooltipAnchor = texturePicker.idTooltip().anchor();
        expect(textureTooltipAnchor != null
                        && textureTooltipAnchor != textureRow
                        && textureTooltipAnchor.hovered()
                        && near(textureTooltipAnchor.layoutBounds().x(), textureRow.layoutBounds().x())
                        && near(textureTooltipAnchor.layoutBounds().y(), textureY)
                        && texturePicker.idTooltip().text().equals(stoneTexture.toString()),
                "MinecraftTexturePickerWidget texture icon tooltip should expose the full texture id from a tile anchor");

        Counter textureChanges = new Counter();
        texturePicker.onSelectionChanged(event -> {
            textureChanges.count++;
            textureChanges.lastSelection = event.newSelection();
        });
        textureRow.handle(new PointerPressedEvent(textureRow, textureX, textureY, 2.0f, 2.0f, 0, PointerButton.PRIMARY));
        expect(texturePicker.selectedId().equals(stoneTexture)
                        && texturePicker.selectedTexture().location().equals(stoneTexture)
                        && texturePicker.selectedPreview().texture() == texturePicker.selectedTexture()
                        && texturePicker.openButton().text().equals(stoneTexture.toString())
                        && texturePicker.selectedIndex() == 1
                        && texturePicker.selectedFilteredIndex() == 0
                        && !texturePicker.opened()
                        && textureChanges.count == 1
                        && textureChanges.lastSelection.equals(java.util.List.of(1)),
                "MinecraftTexturePickerWidget should expose selected texture id and handle");

        texturePicker.query("zombie");
        expect(texturePicker.selectedId().equals(stoneTexture)
                        && texturePicker.selectedFilteredIndex() == -1
                        && texturePicker.resultGrid().itemCount() == 1,
                "MinecraftTexturePickerWidget should preserve selected texture when filters hide it");

        expect(MinecraftWidgets.texture(stoneTexture, 16, 16).texture() instanceof MinecraftTextureHandle,
                "MinecraftWidgets factory should expose Minecraft texture handles");

        verifyMinecraftPickerGridReflowsOnResize();
    }

    private void verifyMinecraftPickerGridReflowsOnResize() {
        java.util.List<MinecraftItemPickerWidget.ItemEntry> manyItems = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            manyItems.add(new MinecraftItemPickerWidget.ItemEntry(
                    ResourceLocation.tryBuild("example", "registry_item_" + i),
                    Items.STICK));
        }
        MinecraftItemPickerWidget itemPicker = new MinecraftItemPickerWidget(false).items(manyItems);
        OverlayLayer itemLayer = new OverlayLayer(itemPicker);
        itemLayer.setUiContextInternal(new DefaultUIContext());
        itemPicker.open();
        itemLayer.measure(new LayoutContext(520.0f, 460.0f));
        itemLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        int wideItemColumns = itemPicker.gridColumns();
        int wideItemRows = itemPicker.resultGrid().itemCount();

        itemPicker.dialog().layout(style -> style.size(180.0f, 420.0f).flexGrow(0).flexShrink(0.0f));
        itemLayer.measure(new LayoutContext(520.0f, 460.0f));
        itemLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        expect(itemPicker.gridColumns() < wideItemColumns
                        && itemPicker.resultGrid().itemCount() > wideItemRows,
                "MinecraftItemPickerWidget grid should recompute columns and row count after dialog resize");

        java.util.List<ResourceLocation> manyTextures = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            manyTextures.add(ResourceLocation.tryBuild("example", "textures/block/registry_texture_" + i + ".png"));
        }
        MinecraftTexturePickerWidget texturePicker = new MinecraftTexturePickerWidget(false).textureIds(manyTextures);
        OverlayLayer textureLayer = new OverlayLayer(texturePicker);
        textureLayer.setUiContextInternal(new DefaultUIContext());
        texturePicker.open();
        textureLayer.measure(new LayoutContext(520.0f, 460.0f));
        textureLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        int wideTextureColumns = texturePicker.gridColumns();
        int wideTextureRows = texturePicker.resultGrid().itemCount();

        texturePicker.dialog().layout(style -> style.size(180.0f, 420.0f).flexGrow(0).flexShrink(0.0f));
        textureLayer.measure(new LayoutContext(520.0f, 460.0f));
        textureLayer.arrange(new MutableRect(0.0f, 0.0f, 520.0f, 460.0f));
        expect(texturePicker.gridColumns() < wideTextureColumns
                        && texturePicker.resultGrid().itemCount() > wideTextureRows,
                "MinecraftTexturePickerWidget grid should recompute columns and row count after dialog resize");
    }

    private void testOverlayLayerAndTooltipBasics() {
        DefaultUIContext uiContext = new DefaultUIContext();
        TransformHitTester hitTester = new TransformHitTester();

        StackPanel content = new StackPanel();
        Button button = new Button("Hover me");
        button.layout(style -> style.size(80.0f, 20.0f).align(Alignment.START, Alignment.START).flexGrow(0).flexShrink(0.0f));
        content.addChild(button);

        Tooltip tooltip = new Tooltip(button, "Tooltip text");
        Button popupButton = new Button("Popup action");
        popupButton.layout(style -> style.size(90.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        Popup popup = new Popup(button, popupButton);
        OverlayLayer layer = new OverlayLayer(content)
                .addOverlay(tooltip)
                .addOverlay(popup);
        layer.setUiContextInternal(uiContext);
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));

        DrawList idleDrawList = new DrawList();
        layer.render(new DefaultRenderContext(idleDrawList));
        expect(hasText(idleDrawList, "Hover me") && !hasText(idleDrawList, "Tooltip text"),
                "Tooltip should not render while its anchor is not hovered");

        button.handle(new PointerEnteredEvent(button, 4.0f, 4.0f, 4.0f, 4.0f, 0));
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        DrawList hoveredDrawList = new DrawList();
        layer.render(new DefaultRenderContext(hoveredDrawList));
        expect(tooltip.showing() && hasText(hoveredDrawList, "Tooltip text"),
                "Tooltip should render above content while its anchor is hovered");
        expect(hitTester.hitTest(layer, tooltip.layoutBounds().x() + 1.0f, tooltip.layoutBounds().y() + 1.0f).orElseThrow().widget() == content,
                "Disabled tooltip overlay should not capture hit-test input");

        StackPanel narrowContent = new StackPanel();
        Button narrowButton = new Button("Hover");
        narrowButton.layout(style -> style.size(70.0f, 20.0f).align(Alignment.START, Alignment.START).flexGrow(0).flexShrink(0.0f));
        narrowButton.handle(new PointerEnteredEvent(narrowButton, 4.0f, 4.0f, 4.0f, 4.0f, 0));
        narrowContent.addChild(narrowButton);
        Tooltip narrowTooltip = new Tooltip(narrowButton, "Tooltip overlays render above content and do not capture pointer input.");
        narrowTooltip.maxWidth(180.0f);
        OverlayLayer narrowLayer = new OverlayLayer(narrowContent).addOverlay(narrowTooltip);
        narrowLayer.setUiContextInternal(uiContext);
        narrowLayer.measure(new LayoutContext(110.0f, 100.0f));
        narrowLayer.arrange(new MutableRect(0.0f, 0.0f, 110.0f, 100.0f));
        DrawList narrowTooltipDrawList = new DrawList();
        narrowLayer.render(new DefaultRenderContext(narrowTooltipDrawList));
        expect(narrowTooltip.layoutBounds().width() <= 110.0f,
                "Tooltip should clamp wrapped width to the overlay host width");
        expect(narrowTooltip.layoutBounds().height() > TextEngine.LINE_HEIGHT + 8.0f,
                "Tooltip should wrap long text vertically when host width is narrow");
        expect(countTextCommands(narrowTooltipDrawList) >= 2,
                "Tooltip should render wrapped long text as multiple clipped lines");

        StackPanel wideContent = new StackPanel();
        Button wideButton = new Button("Wide");
        wideButton.layout(style -> style.size(70.0f, 20.0f).align(Alignment.START, Alignment.START).flexGrow(0).flexShrink(0.0f));
        wideButton.handle(new PointerEnteredEvent(wideButton, 4.0f, 4.0f, 4.0f, 4.0f, 0));
        wideContent.addChild(wideButton);
        FontFace wideTooltipFont = new FixedFontFace("tooltip-wide", 8.0f, 10.0f);
        Tooltip wideTooltip = new Tooltip(wideButton, RichText.of("abcdefghij", wideTooltipFont, 10.0f));
        wideTooltip.maxWidth(52.0f);
        OverlayLayer wideLayer = new OverlayLayer(wideContent).addOverlay(wideTooltip);
        wideLayer.setUiContextInternal(uiContext);
        wideLayer.measure(new LayoutContext(80.0f, 80.0f));
        wideLayer.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 80.0f));
        DrawList wideTooltipDrawList = new DrawList();
        wideLayer.render(new DefaultRenderContext(wideTooltipDrawList));
        float wideTooltipTextWidth = wideTooltip.layoutBounds().width() - 12.0f;
        long wideTooltipLines = wideTooltipDrawList.commands().stream()
                .filter(command -> command.type() == DrawCommandType.TEXT)
                .filter(command -> command.text() != null && command.text().matches("[a-j]+"))
                .peek(command -> expect(TextEngine.measureLineWidth(command.richText()) <= wideTooltipTextWidth + 0.01f,
                        "Tooltip wrapped line should fit the measured text width when glyph advances are wider than average"))
                .count();
        expect(wideTooltipLines >= 2,
                "Tooltip should wrap using font advances instead of approximate character counts");

        popup.open();
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        DrawList popupDrawList = new DrawList();
        layer.render(new DefaultRenderContext(popupDrawList));
        expect(hasText(popupDrawList, "Popup action"), "Open Popup should render its content above the base content");
        expect(hitTester.hitTest(layer, popupButton.layoutBounds().x() + 2.0f, popupButton.layoutBounds().y() + 2.0f).orElseThrow().widget() == popupButton,
                "Open Popup should participate in hit-test for its interactive content");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(button, 4.0f, 4.0f, 4.0f, 4.0f, 0, PointerButton.PRIMARY));
        expect(popup.opened(), "Clicking an open Popup anchor should not close it before the anchor toggles it");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(content, 120.0f, 70.0f, 120.0f, 70.0f, 0, PointerButton.PRIMARY));
        expect(!popup.opened(), "OverlayLayer should close Popup on outside primary click");

        Toast standaloneToast = new Toast("Saved").duration(0.0f).show();
        OverlayLayer toastLayer = new OverlayLayer(new StackPanel()).addOverlay(standaloneToast);
        toastLayer.measure(new LayoutContext(200.0f, 100.0f));
        toastLayer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        DrawList toastDrawList = new DrawList();
        toastLayer.render(new DefaultRenderContext(toastDrawList));
        expect(standaloneToast.parent() == toastLayer
                        && standaloneToast.layoutBounds().y() > 40.0f
                        && hasText(toastDrawList, "Saved"),
                "Toast should remain a standalone transient card positioned by the overlay host");

        NotificationView notificationView = new NotificationView()
                .duration(0.25f)
                .maxVisible(2)
                .toast("First")
                .toast("Second")
                .toast("Third");
        OverlayLayer notificationLayer = new OverlayLayer(new StackPanel()).addOverlay(notificationView);
        notificationLayer.measure(new LayoutContext(260.0f, 140.0f));
        notificationLayer.arrange(new MutableRect(0.0f, 0.0f, 260.0f, 140.0f));
        DrawList notificationDrawList = new DrawList();
        notificationLayer.render(new DefaultRenderContext(notificationDrawList));
        expect(notificationView.activeCount() == 3
                        && notificationView.notifications().size() == 3
                        && notificationView.notifications().get(0) instanceof Toast
                        && notificationView.notifications().get(2).visibility() == Visibility.HIDDEN
                        && hasText(notificationDrawList, "First")
                        && hasText(notificationDrawList, "Second")
                        && !hasText(notificationDrawList, "Third"),
                "NotificationView should host a Toast queue and render only maxVisible cards");
        notificationView.tick(new FrameContext(10L, 0.30f, 0.0f, FramePhase.ANIMATION));
        notificationLayer.measure(new LayoutContext(260.0f, 140.0f));
        notificationLayer.arrange(new MutableRect(0.0f, 0.0f, 260.0f, 140.0f));
        DrawList notificationAfterTickDrawList = new DrawList();
        notificationLayer.render(new DefaultRenderContext(notificationAfterTickDrawList));
        expect(notificationView.activeCount() == 1
                        && hasText(notificationAfterTickDrawList, "Third")
                        && !hasText(notificationAfterTickDrawList, "First"),
                "NotificationView should advance queued Toast cards after visible notifications expire");

        ContextMenu menu = new ContextMenu()
                .item("Inspect")
                .item("Delete");
        Counter menuSelection = new Counter();
        menu.onItemSelected((ContextMenuItemSelectedEvent event) -> {
            menuSelection.count++;
            menuSelection.lastRow = event.index();
            menuSelection.lastText = event.text();
        });
        menu.openAt(8.0f, 8.0f);
        layer.addOverlay(menu);
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        uiContext.focusManager().requestFocus(menu);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(menu, KeyCodes.DOWN, 0, 0));
        expect(menu.selectedItemIndex() == 1, "ContextMenu Down key should move keyboard selection");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(menu, KeyCodes.ENTER, 0, 0));
        expect(!menu.opened()
                        && menuSelection.count == 1
                        && menuSelection.lastRow == 1
                        && menuSelection.lastText.equals("Delete"),
                "ContextMenu Enter should select the focused item and close the menu");
        menu.openAt(8.0f, 8.0f);
        uiContext.focusManager().requestFocus(menu);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(menu, KeyCodes.ESCAPE, 0, 0));
        expect(!menu.opened(), "ContextMenu Escape should close the menu");

        StackPanel edgeContent = new StackPanel();
        Button edgeAnchor = new Button("Edge");
        edgeAnchor.layout(style -> style.size(20.0f, 10.0f).align(Alignment.END, Alignment.END).flexGrow(0).flexShrink(0.0f));
        edgeContent.addChild(edgeAnchor);
        Tooltip edgeTooltip = new Tooltip(edgeAnchor, "Edge tooltip");
        Button edgePopupContent = new Button("Edge popup action");
        edgePopupContent.layout(style -> style.size(80.0f, 24.0f).flexGrow(0).flexShrink(0.0f));
        Popup edgePopup = new Popup(edgeAnchor, edgePopupContent).open();
        OverlayLayer edgeLayer = new OverlayLayer(edgeContent)
                .addOverlay(edgeTooltip)
                .addOverlay(edgePopup);
        edgeLayer.measure(new LayoutContext(120.0f, 60.0f));
        edgeLayer.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        expect(edgeTooltip.layoutBounds().x() < edgeAnchor.layoutBounds().x()
                        && edgeTooltip.layoutBounds().y() < edgeAnchor.layoutBounds().y()
                        && edgeTooltip.layoutBounds().x() >= 0.0f
                        && edgeTooltip.layoutBounds().y() >= 0.0f,
                "Tooltip should flip left/up and remain clamped near the host bottom-right edge");
        expect(edgePopup.layoutBounds().x() < edgeAnchor.layoutBounds().x()
                        && edgePopup.layoutBounds().y() < edgeAnchor.layoutBounds().y()
                        && edgePopup.layoutBounds().x() + edgePopup.layoutBounds().width() <= 120.0f
                        && edgePopup.layoutBounds().y() + edgePopup.layoutBounds().height() <= 60.0f,
                "Popup should flip and clamp inside a small overlay host");

        Button dialogAction = new Button("Dialog OK");
        dialogAction.layout(style -> style.size(82.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        WindowWidget window = new WindowWidget("Dialog title", dialogAction)
                .position(20.0f, 8.0f)
                .closeOnOutsideClick(true)
                .open();
        window.layout(style -> style.size(150.0f, 70.0f).flexGrow(0).flexShrink(0.0f));
        Counter windowLifecycle = new Counter();
        Counter windowMove = new Counter();
        Counter windowResize = new Counter();
        window.onOpened(event -> windowLifecycle.started++);
        window.onClosed(event -> windowLifecycle.cancelled++);
        window.onActivated(event -> windowLifecycle.count++);
        window.onDeactivated(event -> windowLifecycle.committed++);
        window.onMoveStarted(event -> windowMove.started++);
        window.onMoved(event -> windowMove.moved++);
        window.onMoveEnded(event -> windowMove.committed++);
        window.onResizeStarted(event -> {
            windowResize.started++;
            windowResize.lastText = event.handle();
        });
        window.onResized(event -> {
            windowResize.resized++;
            windowResize.lastNewWidth = event.newWidth();
            windowResize.lastNewHeight = event.newHeight();
        });
        window.onResizeEnded(event -> {
            windowResize.committed++;
            windowResize.lastText = event.handle();
        });
        layer.addOverlay(window);
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        DrawList windowDrawList = new DrawList();
        layer.render(new DefaultRenderContext(windowDrawList));
        expect(hasText(windowDrawList, "Dialog title") && hasText(windowDrawList, "Dialog OK"),
                "Open WindowWidget should render title and content above base content");
        expect(hitTester.hitTest(layer, dialogAction.layoutBounds().x() + 2.0f, dialogAction.layoutBounds().y() + 2.0f).orElseThrow().widget() == dialogAction,
                "WindowWidget content should participate in hit-test");
        expect(layer.windows().registered(window) && layer.windows().activeWindow() == window && window.active(),
                "OverlayLayer WindowManager should register and activate open WindowWidget overlays");

        WindowWidget secondWindow = new WindowWidget("Second", new Label("Second body"))
                .position(24.0f, 12.0f)
                .open();
        secondWindow.layout(style -> style.size(120.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(secondWindow);
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(layer.windows().activeWindow() == secondWindow && secondWindow.active() && !window.active(),
                "WindowManager should activate the most recently opened overlay window");
        expect(hitTester.hitTest(layer, 30.0f, 18.0f).orElseThrow().widget() == secondWindow,
                "WindowManager z-order should put the active window above older overlapping windows");
        layer.windows().activate(window);
        expect(layer.windows().activeWindow() == window && window.active() && !secondWindow.active(),
                "WindowManager.activate should switch active window state");
        expect(hitTester.hitTest(layer, 30.0f, 18.0f).orElseThrow().widget() == window,
                "WindowManager.activate should bring the activated window to the front");
        layer.removeOverlay(secondWindow);

        window.position(500.0f, 500.0f);
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(near(window.layoutBounds().x(), 50.0f) && near(window.layoutBounds().y(), 30.0f),
                "Host-constrained WindowWidget should clamp programmatic positions to the host");
        window.position(20.0f, 8.0f);
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));

        float oldWindowX = window.layoutBounds().x();
        float oldWindowY = window.layoutBounds().y();
        uiContext.routedEvents().dispatch(new PointerPressedEvent(window, oldWindowX + 16.0f, oldWindowY + 6.0f, 16.0f, 6.0f, 0, PointerButton.PRIMARY));
        expect(window.dragging() && uiContext.capturedPointer(0) == window,
                "WindowWidget title press should start drag and capture pointer");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(window, oldWindowX + 46.0f, oldWindowY + 21.0f, 46.0f, 21.0f, 0));
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(window.layoutBounds().x() > oldWindowX && window.layoutBounds().y() > oldWindowY,
                "WindowWidget drag should update arranged position");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(window, window.layoutBounds().x() + 16.0f, window.layoutBounds().y() + 6.0f, 16.0f, 6.0f, 0, PointerButton.PRIMARY));
        expect(!window.dragging() && uiContext.capturedPointer(0) == null,
                "WindowWidget release should stop drag and release pointer capture");
        expect(windowMove.started == 1 && windowMove.moved >= 1 && windowMove.committed == 1,
                "WindowWidget drag should publish typed move lifecycle events");

        window.position(20.0f, 8.0f);
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        float resizeStartX = window.layoutBounds().x();
        float resizeStartY = window.layoutBounds().y();
        float resizeStartWidth = window.layoutBounds().width();
        float resizeStartHeight = window.layoutBounds().height();
        uiContext.routedEvents().dispatch(new PointerPressedEvent(window,
                resizeStartX + resizeStartWidth - 1.0f,
                resizeStartY + resizeStartHeight - 1.0f,
                resizeStartWidth - 1.0f,
                resizeStartHeight - 1.0f,
                1,
                PointerButton.PRIMARY));
        expect(window.resizing() && uiContext.capturedPointer(1) == window,
                "WindowWidget bottom-right edge press should start resize and capture pointer");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(window,
                resizeStartX + resizeStartWidth + 34.0f,
                resizeStartY + resizeStartHeight + 18.0f,
                resizeStartWidth + 34.0f,
                resizeStartHeight + 18.0f,
                1));
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(window.layoutBounds().width() > resizeStartWidth && window.layoutBounds().height() > resizeStartHeight,
                "WindowWidget bottom-right resize should grow the arranged window");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(window,
                window.layoutBounds().x() + window.layoutBounds().width() - 1.0f,
                window.layoutBounds().y() + window.layoutBounds().height() - 1.0f,
                window.layoutBounds().width() - 1.0f,
                window.layoutBounds().height() - 1.0f,
                1,
                PointerButton.PRIMARY));
        expect(!window.resizing() && uiContext.capturedPointer(1) == null,
                "WindowWidget resize release should stop resize and release pointer capture");
        expect(windowResize.started == 1 && windowResize.resized >= 1 && windowResize.committed == 1
                        && windowResize.lastText.equals("bottom_right"),
                "WindowWidget resize should publish typed resize lifecycle events with handle name");

        window.minWindowSize(120.0f, 64.0f);
        float grownWidth = window.layoutBounds().width();
        float grownHeight = window.layoutBounds().height();
        uiContext.routedEvents().dispatch(new PointerPressedEvent(window,
                window.layoutBounds().x() + window.layoutBounds().width() - 1.0f,
                window.layoutBounds().y() + window.layoutBounds().height() - 1.0f,
                window.layoutBounds().width() - 1.0f,
                window.layoutBounds().height() - 1.0f,
                2,
                PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerMovedEvent(window,
                window.layoutBounds().x() - 200.0f,
                window.layoutBounds().y() - 200.0f,
                -200.0f,
                -200.0f,
                2));
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(window.layoutBounds().width() >= 120.0f && window.layoutBounds().height() >= 64.0f
                        && window.layoutBounds().width() <= grownWidth && window.layoutBounds().height() <= grownHeight,
                "WindowWidget resize should clamp to configured minimum size");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(window,
                window.layoutBounds().x() + window.layoutBounds().width() - 1.0f,
                window.layoutBounds().y() + window.layoutBounds().height() - 1.0f,
                window.layoutBounds().width() - 1.0f,
                window.layoutBounds().height() - 1.0f,
                2,
                PointerButton.PRIMARY));

        uiContext.routedEvents().dispatch(new PointerPressedEvent(button, 4.0f, 4.0f, 4.0f, 4.0f, 0, PointerButton.PRIMARY));
        expect(!window.opened(), "OverlayLayer should close WindowWidget on outside primary click when enabled");
        expect(windowLifecycle.cancelled == 1 && !window.active(),
                "WindowWidget close should publish close event and clear active state");

        window.open();
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(windowLifecycle.started == 1 && windowLifecycle.count >= 2 && layer.windows().activeWindow() == window,
                "WindowWidget reopen should publish open event and reactivate through WindowManager");
        Button closeButton = window.closeButton();
        uiContext.routedEvents().dispatch(new PointerPressedEvent(closeButton, closeButton.layoutBounds().x() + 2.0f, closeButton.layoutBounds().y() + 2.0f, 2.0f, 2.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(closeButton, closeButton.layoutBounds().x() + 2.0f, closeButton.layoutBounds().y() + 2.0f, 2.0f, 2.0f, 0, PointerButton.PRIMARY));
        expect(!window.opened(), "WindowWidget close button should close the overlay shell");

        WindowWidget freeWindow = new WindowWidget("Free", null)
                .position(180.0f, 80.0f)
                .constrainToHost(false)
                .open();
        freeWindow.layout(style -> style.size(60.0f, 40.0f).flexGrow(0).flexShrink(0.0f));
        layer.addOverlay(freeWindow);
        layer.measure(new LayoutContext(200.0f, 100.0f));
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(near(freeWindow.layoutBounds().x(), 180.0f) && near(freeWindow.layoutBounds().y(), 80.0f),
                "WindowWidget should allow explicit opt-out from host constraints");
        freeWindow.constrainToHost(true);
        layer.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 100.0f));
        expect(near(freeWindow.layoutBounds().x(), 140.0f) && near(freeWindow.layoutBounds().y(), 60.0f),
                "Re-enabled WindowWidget host constraints should clamp the absolute rect again");

        Button blockedByModal = new Button("Blocked");
        blockedByModal.layout(style -> style.size(70.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
        Counter blockedClicks = new Counter();
        blockedByModal.onClick(event -> blockedClicks.count++);
        WindowWidget modal = new WindowWidget("Modal", new Button("Modal OK"))
                .position(30.0f, 20.0f)
                .modal(true);
        modal.layout(style -> style.size(120.0f, 60.0f).flexGrow(0).flexShrink(0.0f));
        Counter modalLifecycle = new Counter();
        modal.onModalOpened(event -> {
            modalLifecycle.started++;
            modalLifecycle.lastRow = event.stackDepth();
        });
        modal.onModalClosed(event -> {
            modalLifecycle.committed++;
            modalLifecycle.lastColumn = event.stackDepth();
        });
        StackPanel modalContent = new StackPanel();
        modalContent.addChild(blockedByModal);
        OverlayLayer modalLayer = new OverlayLayer(modalContent);
        modalLayer.setUiContextInternal(uiContext);
        modalLayer.addOverlay(modal);
        modal.open();
        modalLayer.measure(new LayoutContext(180.0f, 100.0f));
        modalLayer.arrange(new MutableRect(0.0f, 0.0f, 180.0f, 100.0f));
        DrawList modalDrawList = new DrawList();
        modalLayer.render(new DefaultRenderContext(modalDrawList));
        expect(modal.active() && modalLayer.windows().topModalWindow() == modal && modalLifecycle.started == 1,
                "WindowManager should track active top modal window and publish modal opened event");
        expect(hasCommand(modalDrawList, DrawCommandType.RECT) && hasText(modalDrawList, "Modal"),
                "OverlayLayer should render modal scrim before the top modal window");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(blockedByModal,
                blockedByModal.layoutBounds().x() + 2.0f,
                blockedByModal.layoutBounds().y() + 2.0f,
                2.0f,
                2.0f,
                3,
                PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(blockedByModal,
                blockedByModal.layoutBounds().x() + 2.0f,
                blockedByModal.layoutBounds().y() + 2.0f,
                2.0f,
                2.0f,
                3,
                PointerButton.PRIMARY));
        expect(blockedClicks.count == 0,
                "OverlayLayer should block pointer input to content below the top modal window");
        modal.close();
        expect(modalLifecycle.committed == 1 && modalLifecycle.lastColumn == 0 && modalLayer.windows().topModalWindow() == null,
                "WindowManager should publish modal closed event and clear top modal when closed");
    }

    private void testDockingRootContracts() {
        DefaultUIContext dockContext = new DefaultUIContext();
        DockingRoot root = new DockingRoot();
        root.setUiContextInternal(dockContext);
        Counter changes = new Counter();
        Counter drag = new Counter();
        Counter preview = new Counter();
        root.onLayoutChanged(event -> {
            changes.count++;
            changes.lastText = event.operation();
        });
        root.onDragStarted(event -> {
            drag.started++;
            drag.lastText = event.paneId();
        });
        root.onDragMoved(event -> {
            drag.moved++;
            drag.lastNewColumn = event.intent().area().ordinal();
        });
        root.onDragEnded(event -> {
            drag.committed++;
            drag.lastChecked = event.dropped();
            drag.lastText = event.intent().area().name();
        });
        root.onDropPreviewChanged(event -> {
            preview.count++;
            preview.lastText = event.newIntent().area().name();
        });

        DockPane project = new DockPane("project", "Project", testDockContent("Project body"));
        DockPane inspector = new DockPane("inspector", "Inspector", testDockContent("Inspector body"));
        DockPane console = new DockPane("console", "Console", testDockContent("Console body"));

        root.addPane(project);
        expect(root.manager().paneCount() == 1 && root.manager().selectedPane() == project,
                "DockingRoot should add and select the first pane");

        root.manager().splitPane(project.id(), DockArea.RIGHT, inspector);
        expect(root.rootNode().isSplit() && root.rootNode().orientation() == dev.sixik.unigui.widgets.docking.DockSplitOrientation.HORIZONTAL,
                "DockingManager split RIGHT should create a horizontal split node");
        expect(root.manager().paneCount() == 2 && root.manager().containsPane(inspector.id()),
                "DockingManager split should retain target pane and add the new pane");

        root.manager().tabPane(project.id(), console);
        expect(root.manager().paneCount() == 3 && root.manager().selectedPane() == console,
                "DockingManager tabPane should add the pane to the target tab group and select it");
        DockLayoutSnapshot snapshot = root.manager().snapshot();
        expect(snapshot.root().kind() == dev.sixik.unigui.widgets.docking.DockNode.Kind.SPLIT
                        && snapshot.root().first().paneIds().equals(java.util.List.of(project.id(), console.id()))
                        && snapshot.root().first().selectedPaneId().equals(console.id())
                        && snapshot.root().second().paneIds().equals(java.util.List.of(inspector.id())),
                "DockLayoutSnapshot should capture split/tree pane ids and selected tab without object identity");

        root.measure(new LayoutContext(360.0f, 180.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 360.0f, 180.0f));
        expect(console.content().layoutBounds().width() > 0.0f && console.content().layoutBounds().height() > 0.0f,
                "DockingRoot should arrange the selected tab content into its leaf content area");
        expect(project.content().layoutBounds().width() == 0.0f,
                "DockingRoot should collapse non-selected tab content bounds");

        DrawList drawList = new DrawList();
        root.render(new DefaultRenderContext(drawList));
        expect(hasText(drawList, "Console") && hasText(drawList, "Inspector"),
                "DockingRoot renderer should draw visible tab headers for split leaves");

        root.handle(new PointerPressedEvent(root, 6.0f, 6.0f, 6.0f, 6.0f, 0, PointerButton.PRIMARY));
        expect(root.manager().selectedPane() == project,
                "DockingRoot tab click should select the clicked pane");
        root.handle(new PointerReleasedEvent(root, 6.0f, 6.0f, 6.0f, 6.0f, 0, PointerButton.PRIMARY));
        expect(!root.dragController().active() && dockContext.capturedPointer(0) == null,
                "DockingRoot tab click should release pending dock drag without crossing threshold");

        dockContext.routedEvents().dispatch(new PointerPressedEvent(root, 96.0f, 6.0f, 96.0f, 6.0f, 0, PointerButton.PRIMARY));
        expect(root.dragController().active() && !root.dockDragging() && dockContext.capturedPointer(0) == root,
                "DockingRoot tab press should capture pointer and wait for drag threshold");
        dockContext.routedEvents().dispatch(new PointerMovedEvent(root, 98.0f, 7.0f, 98.0f, 7.0f, 0));
        expect(drag.started == 0 && !root.dockDropPreview().valid(),
                "DockingRoot should not start dock drag before the movement threshold");
        dockContext.routedEvents().dispatch(new PointerMovedEvent(root, 260.0f, 80.0f, 260.0f, 80.0f, 0));
        DockDropIntent activePreview = root.dockDropPreview();
        expect(root.dockDragging()
                        && drag.started == 1
                        && drag.moved >= 1
                        && activePreview.valid()
                        && activePreview.targetPaneId().equals(inspector.id())
                        && activePreview.area() == DockArea.CENTER,
                "DockingRoot dock drag should publish preview intent over a target tab group center");
        DrawList previewDrawList = new DrawList();
        root.render(new DefaultRenderContext(previewDrawList));
        expect(hasCommand(previewDrawList, DrawCommandType.ROUNDED_RECT),
                "DockingRoot should render dock drop preview chrome while dragging");
        dockContext.routedEvents().dispatch(new PointerReleasedEvent(root, 260.0f, 80.0f, 260.0f, 80.0f, 0, PointerButton.PRIMARY));
        expect(!root.dragController().active()
                        && dockContext.capturedPointer(0) == null
                        && drag.committed == 1
                        && drag.lastChecked
                        && preview.count >= 2,
                "DockingRoot dock release should apply drop, clear preview and release pointer capture");
        DockLayoutSnapshot movedSnapshot = root.manager().snapshot();
        expect(movedSnapshot.root().second().paneIds().equals(java.util.List.of(inspector.id(), console.id()))
                        && movedSnapshot.root().second().selectedPaneId().equals(console.id()),
                "DockingManager should move dragged pane into the target tab group on CENTER drop");

        WindowWidget floating = root.manager().floatPane(console.id());
        expect(floating != null && floating.content() == console.content() && floating.title().equals("Console"),
                "DockingManager.floatPane should remove a pane and wrap its content in a WindowWidget");
        expect(root.manager().paneCount() == 2 && !root.manager().containsPane(console.id()),
                "DockingManager.floatPane should remove the floated pane from the dock model");

        root.manager().closePane(inspector.id());
        expect(root.manager().paneCount() == 1 && root.rootNode().isLeaf() && root.manager().containsPane(project.id()),
                "DockingManager should compact empty split branches after closing a pane");
        expect(changes.count >= 5 && changes.lastText.equals("close"),
                "DockingRoot should emit dock layout change events for model mutations");

        DefaultUIContext floatContext = new DefaultUIContext();
        DockingRoot floatingRoot = new DockingRoot();
        floatingRoot.setUiContextInternal(floatContext);
        floatingRoot.allowFloatingOutsideHost(true);
        DockPane floatingPane = new DockPane("floating", "Floating", testDockContent("Floating body"));
        Counter floatingDrag = new Counter();
        floatingRoot.onDragEnded(event -> {
            floatingDrag.committed++;
            floatingDrag.lastChecked = event.dropped();
            floatingDrag.lastText = event.intent().area().name();
        });
        floatingRoot.addPane(floatingPane);
        floatingRoot.measure(new LayoutContext(220.0f, 120.0f));
        floatingRoot.arrange(new MutableRect(0.0f, 0.0f, 220.0f, 120.0f));
        floatContext.routedEvents().dispatch(new PointerPressedEvent(floatingRoot, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        floatContext.routedEvents().dispatch(new PointerMovedEvent(floatingRoot, 260.0f, 48.0f, 260.0f, 48.0f, 0));
        expect(floatingRoot.dockDropPreview().floating(),
                "DockingRoot should create FLOAT drop intent when dragging outside root bounds");
        floatContext.routedEvents().dispatch(new PointerReleasedEvent(floatingRoot, 260.0f, 48.0f, 260.0f, 48.0f, 0, PointerButton.PRIMARY));
        expect(floatingDrag.committed == 1
                        && floatingDrag.lastChecked
                        && floatingDrag.lastText.equals("FLOAT")
                        && floatingRoot.lastFloatingWindow() != null
                        && floatingRoot.lastFloatingWindow().content() == floatingPane.content()
                        && floatingRoot.manager().paneCount() == 0,
                "DockingRoot FLOAT drop should detach the pane and expose a WindowWidget bridge");

        DefaultUIContext workspaceContext = new DefaultUIContext();
        DockingRoot workspace = new DockingRoot();
        workspace.setUiContextInternal(workspaceContext);
        workspace.addDocument("scene", "Scene", testDockContent("Scene body"))
                .addDocument("recipe", "Recipe", testDockContent("Recipe body"))
                .addToolPane("assets", "Assets", testDockContent("Assets body"), DockArea.LEFT)
                .addToolPane("inspector", "Inspector", testDockContent("Inspector body"), DockArea.RIGHT)
                .addToolPane("log", "Log", testDockContent("Log body"), DockArea.BOTTOM)
                .selectPane("scene");
        DockPane scene = workspace.manager().findPane("scene");
        DockPane recipe = workspace.manager().findPane("recipe");
        DockPane assets = workspace.manager().findPane("assets");
        DockPane inspectorTool = workspace.manager().findPane("inspector");
        DockPane log = workspace.manager().findPane("log");
        recipe.dirty(true);
        assets.pinned(false).autoHide(true);
        expect(scene.kind() == DockPaneKind.DOCUMENT
                        && recipe.document()
                        && assets.tool()
                        && inspectorTool.tool()
                        && log.tool(),
                "DockingRoot document/tool helpers should mark document and tool pane kinds");
        expect(workspace.manager().paneCount() == 5
                        && workspace.manager().containsPane("scene")
                        && workspace.manager().containsPane("assets"),
                "DockingRoot document/tool helpers should build center documents plus side tool panes");

        workspace.measure(new LayoutContext(420.0f, 220.0f));
        workspace.arrange(new MutableRect(0.0f, 0.0f, 420.0f, 220.0f));
        DrawList workspaceDrawList = new DrawList();
        workspace.render(new DefaultRenderContext(workspaceDrawList));
        expect(hasText(workspaceDrawList, "Scene")
                        && hasText(workspaceDrawList, "Recipe")
                        && hasText(workspaceDrawList, "Assets")
                        && hasText(workspaceDrawList, "Inspector")
                        && hasText(workspaceDrawList, "Log"),
                "DockingRoot document/tool workspace should render center and side tab headers");

        workspaceContext.routedEvents().dispatch(new KeyPressedEvent(
                workspace, KeyCodes.TAB, 0, KeyModifiers.CONTROL));
        expect(workspace.manager().selectedPane() == recipe,
                "DockingRoot Ctrl+Tab shortcut should select next document tab in the active group");
        workspaceContext.routedEvents().dispatch(new KeyPressedEvent(
                workspace, KeyCodes.TAB, 0, KeyModifiers.CONTROL | KeyModifiers.SHIFT));
        expect(workspace.manager().selectedPane() == scene,
                "DockingRoot Ctrl+Shift+Tab shortcut should select previous document tab in the active group");
        workspaceContext.routedEvents().dispatch(new KeyPressedEvent(
                workspace, KeyCodes.W, 0, KeyModifiers.CONTROL));
        expect(!workspace.manager().containsPane(scene.id())
                        && workspace.manager().containsPane(recipe.id())
                        && workspace.manager().paneCount() == 4,
                "DockingRoot Ctrl+W shortcut should close the active tab without breaking the dock tree");

        DockingRoot overflowRoot = new DockingRoot();
        final dev.sixik.unigui.widgets.render.DockPaneState[] capturedPaneState = new dev.sixik.unigui.widgets.render.DockPaneState[1];
        overflowRoot.paneRenderer((draw, state) -> capturedPaneState[0] = state);
        for (int i = 0; i < 6; i++) {
            overflowRoot.addDocument("doc" + i, "Doc " + i, testDockContent("Doc body " + i));
        }
        overflowRoot.measure(new LayoutContext(120.0f, 80.0f));
        overflowRoot.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 80.0f));
        overflowRoot.render(new DefaultRenderContext(new DrawList()));
        expect(capturedPaneState[0] != null
                        && capturedPaneState[0].overflow()
                        && capturedPaneState[0].firstVisibleTab() == 0
                        && capturedPaneState[0].lastVisibleTab() < capturedPaneState[0].tabs().size() - 1,
                "DockingRoot tab overflow strategy should expose a clipped visible range in renderer state");

        DockLayoutSnapshot workspaceSnapshot = workspace.manager().snapshot();
        String encodedSnapshot = DockLayoutSnapshotCodec.encode(workspaceSnapshot);
        DockLayoutSnapshot decodedSnapshot = DockLayoutSnapshotCodec.decode(encodedSnapshot);
        expect(decodedSnapshot.equals(workspaceSnapshot),
                "DockLayoutSnapshotCodec should round-trip snapshot tree shape, selected tabs and active pane id");

        DockingRoot restoredWorkspace = new DockingRoot();
        Counter restoreCounter = new Counter();
        restoredWorkspace.onLayoutRestored(event -> {
            restoreCounter.count++;
            restoreCounter.resized = event.restoredPaneCount();
            restoreCounter.moved = event.missingPaneCount();
        });
        DockPane restoredScene = DockPane.document("scene", "Scene", testDockContent("Restored scene"));
        DockPane restoredRecipe = DockPane.document("recipe", "Recipe", testDockContent("Restored recipe"));
        DockPane restoredAssets = DockPane.tool("assets", "Assets", testDockContent("Restored assets"));
        DockPane restoredInspector = DockPane.tool("inspector", "Inspector", testDockContent("Restored inspector"));
        DockPane restoredLog = DockPane.tool("log", "Log", testDockContent("Restored log"));
        restoredWorkspace.restoreLayout(decodedSnapshot, java.util.Map.of(
                restoredScene.id(), restoredScene,
                restoredRecipe.id(), restoredRecipe,
                restoredAssets.id(), restoredAssets,
                restoredInspector.id(), restoredInspector,
                restoredLog.id(), restoredLog));
        expect(restoredWorkspace.manager().paneCount() == 4
                        && restoredWorkspace.manager().containsPane("recipe")
                        && restoredWorkspace.manager().containsPane("assets")
                        && restoredWorkspace.manager().selectedPane().id().equals(decodedSnapshot.activePaneId())
                        && restoreCounter.count == 1
                        && restoreCounter.resized == 4
                        && restoreCounter.moved == 0,
                "DockingRoot restore should rebuild known panes, preserve active pane id and publish restore event");
        expect(restoredWorkspace.manager().snapshot().equals(decodedSnapshot),
                "DockingRoot restore should preserve snapshot tree shape when all referenced panes are registered");

        DockingRoot missingWorkspace = new DockingRoot();
        Counter missingRestoreCounter = new Counter();
        missingWorkspace.onLayoutRestored(event -> {
            missingRestoreCounter.count++;
            missingRestoreCounter.resized = event.restoredPaneCount();
            missingRestoreCounter.moved = event.missingPaneCount();
        });
        missingWorkspace.restoreLayout(decodedSnapshot, java.util.Map.of(
                restoredRecipe.id(), DockPane.document("recipe", "Recipe", testDockContent("Only recipe")),
                restoredAssets.id(), DockPane.tool("assets", "Assets", testDockContent("Only assets"))));
        expect(missingWorkspace.manager().paneCount() == 2
                        && missingWorkspace.manager().containsPane("recipe")
                        && missingWorkspace.manager().containsPane("assets")
                        && !missingWorkspace.manager().containsPane("inspector")
                        && missingRestoreCounter.count == 1
                        && missingRestoreCounter.resized == 2
                        && missingRestoreCounter.moved >= 1,
                "DockingRoot restore should ignore missing pane ids without crashing and report them");

        DefaultUIContext uxContext = new DefaultUIContext();
        DockingRoot uxRoot = new DockingRoot();
        uxRoot.setUiContextInternal(uxContext);
        final dev.sixik.unigui.widgets.render.DockPaneState[] uxPaneState = new dev.sixik.unigui.widgets.render.DockPaneState[1];
        final dev.sixik.unigui.widgets.render.DockingRootState[] uxRootState = new dev.sixik.unigui.widgets.render.DockingRootState[1];
        uxRoot.paneRenderer((draw, state) -> uxPaneState[0] = state);
        uxRoot.rootRenderer((draw, state) -> uxRootState[0] = state);
        uxRoot.addDocument("ux-a", "UX A", testDockContent("UX A body"))
                .addDocument("ux-b", "UX B", testDockContent("UX B body"))
                .selectPane("ux-a");
        uxRoot.measure(new LayoutContext(240.0f, 100.0f));
        uxRoot.arrange(new MutableRect(0.0f, 0.0f, 240.0f, 100.0f));
        uxContext.routedEvents().dispatch(new PointerMovedEvent(uxRoot, 8.0f, 6.0f, 8.0f, 6.0f, 0));
        uxRoot.render(new DefaultRenderContext(new DrawList()));
        expect(uxPaneState[0].tabs().get(0).hovered() && uxPaneState[0].tabs().get(0).active(),
                "DockingRoot renderer state should expose hovered and active tab flags");
        uxContext.routedEvents().dispatch(new PointerPressedEvent(uxRoot, 8.0f, 6.0f, 8.0f, 6.0f, 0, PointerButton.PRIMARY));
        uxRoot.render(new DefaultRenderContext(new DrawList()));
        expect(uxPaneState[0].tabs().get(0).pressed(),
                "DockingRoot renderer state should expose pressed tab flag");
        uxContext.routedEvents().dispatch(new PointerMovedEvent(uxRoot, 160.0f, 40.0f, 160.0f, 40.0f, 0));
        uxRoot.render(new DefaultRenderContext(new DrawList()));
        expect(uxPaneState[0].tabs().get(0).dragging()
                        && uxRootState[0].dockDragging()
                        && uxRootState[0].dropPreviewVisible(),
                "DockingRoot renderer state should expose dragging tab and root preview flags");
        uxContext.routedEvents().dispatch(new PointerReleasedEvent(uxRoot, 160.0f, 40.0f, 160.0f, 40.0f, 0, PointerButton.PRIMARY));
    }

    private static Label testDockContent(String text) {
        Label label = new Label(text);
        label.layout(style -> style.size(90.0f, 18.0f).flexGrow(1).flexShrink(1.0f));
        return label;
    }

    private void testNodeGraphPhaseOneContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        NodeGraph graph = Widgets.nodeGraph()
                .viewport(10.0f, 20.0f)
                .gridSize(16.0f);
        graph.setUiContextInternal(uiContext);

        Button button = new Button("Run");
        NodeGraphItem buttonItem = graph.addItem("button", button, 30.0f, 40.0f);
        buttonItem.size(80.0f, 20.0f);

        Label label = new Label("Drag me");
        NodeGraphItem labelItem = graph.addItem("label", label, 120.0f, 30.0f);
        labelItem.size(90.0f, 20.0f);

        graph.measure(new LayoutContext(400.0f, 300.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));
        expect(Widgets.nodeGraph() instanceof NodeGraph && graph.children().contains(button) && graph.children().contains(label),
                "Widgets factory should expose NodeGraph and NodeGraph should expose item contents as children");
        expect(near(button.layoutBounds().x(), 40.0f)
                        && near(button.layoutBounds().y(), 60.0f)
                        && near(button.layoutBounds().width(), 80.0f)
                        && near(label.layoutBounds().x(), 130.0f),
                "NodeGraph should arrange arbitrary child widgets in panned world-space");

        TransformHitTester nodeHitTester = new TransformHitTester();
        graph.viewport(10.0f, 20.0f, 2.0f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));
        expect(nodeHitTester.hitTest(graph, 185.0f, 115.0f)
                        .map(hit -> hit.widget() == button
                                && near(hit.localX(), 57.5f)
                                && near(hit.localY(), 7.5f))
                        .orElse(false),
                "NodeGraph hit-test should expand child hitboxes with zoom-in and return unscaled child locals");
        graph.viewport(10.0f, 20.0f, 0.5f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));
        expect(nodeHitTester.hitTest(graph, 85.0f, 75.0f)
                        .map(hit -> hit.widget() != button)
                        .orElse(true),
                "NodeGraph hit-test should shrink child hitboxes with zoom-out");
        Popup popup = new Popup(button, new Label("Options"));
        popup.open();
        popup.measure(new LayoutContext(300.0f, 180.0f));
        popup.arrangeInHost(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));
        expect(near(popup.layoutBounds().x(), button.layoutBounds().x())
                        && near(popup.layoutBounds().y(), button.layoutBounds().y()
                                + button.layoutBounds().height() * 0.5f + 4.0f),
                "Popup should anchor to the visual scaled child bounds without scaling the popup itself");
        graph.viewport(10.0f, 20.0f, 2.0f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));
        popup.measure(new LayoutContext(300.0f, 180.0f));
        popup.arrangeInHost(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));
        expect(near(popup.layoutBounds().x(), 70.0f)
                        && near(popup.layoutBounds().y(), 144.0f)
                        && near(popup.layoutBounds().width(), popup.desiredSize().width()),
                "Popup should follow zoomed NodeGraph anchors in screen-space while keeping its own size");
        graph.viewport(10.0f, 20.0f, 1.0f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 300.0f, 180.0f));

        Counter buttonClicks = new Counter();
        button.onClick(event -> buttonClicks.count++);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(button,
                48.0f, 68.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(button,
                48.0f, 68.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(buttonClicks.count == 1 && graph.selectedItemIds().isEmpty(),
                "NodeGraph should not steal clicks from interactive child widgets");

        Counter selectionChanges = new Counter();
        Counter moves = new Counter();
        java.util.concurrent.atomic.AtomicReference<java.util.List<String>> lastGraphSelection =
                new java.util.concurrent.atomic.AtomicReference<>(java.util.List.of());
        graph.onSelectionChanged(event -> {
            selectionChanges.count++;
            lastGraphSelection.set(event.newSelection());
        });
        graph.onItemMoved(event -> {
            moves.count++;
            moves.lastText = event.itemId();
            moves.lastValue = event.newX();
            moves.lastNewHeight = event.newY();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(label,
                136.0f, 38.0f, 6.0f, 8.0f, 1, PointerButton.PRIMARY));
        expect(graph.selectedItemIds().equals(java.util.List.of("label"))
                        && selectionChanges.count == 1
                        && lastGraphSelection.get().equals(java.util.List.of("label"))
                        && uiContext.capturedPointer(1) == graph,
                "NodeGraph should select and capture draggable non-input item bodies");

        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                156.0f, 48.0f, 156.0f, 48.0f, 1));
        expect(near(labelItem.x(), 140.0f)
                        && near(labelItem.y(), 40.0f)
                        && near(label.layoutBounds().x(), 150.0f)
                        && moves.count == 1
                        && moves.lastText.equals("label")
                        && near(moves.lastValue, 140.0f)
                        && near(moves.lastNewHeight, 40.0f),
                "NodeGraph should drag selected items in world-space and emit move events");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                156.0f, 48.0f, 156.0f, 48.0f, 1, PointerButton.PRIMARY));
        expect(uiContext.capturedPointer(1) == null && graph.draggingItemId().isEmpty(),
                "NodeGraph should release pointer capture after item drag");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                12.0f, 12.0f, 12.0f, 12.0f, 2, PointerButton.MIDDLE));
        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                22.0f, 32.0f, 22.0f, 32.0f, 2));
        expect(near(graph.viewport().x(), 20.0f)
                        && near(graph.viewport().y(), 40.0f)
                        && near(label.layoutBounds().x(), 160.0f),
                "NodeGraph should pan the viewport with middle-mouse drag and keep children arranged");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                22.0f, 32.0f, 22.0f, 32.0f, 2, PointerButton.MIDDLE));

        final dev.sixik.unigui.widgets.render.NodeGraphState[] backgroundState = new dev.sixik.unigui.widgets.render.NodeGraphState[1];
        final dev.sixik.unigui.widgets.render.NodeGraphState[] foregroundState = new dev.sixik.unigui.widgets.render.NodeGraphState[1];
        graph.renderer((draw, state) -> {
            if (state.phase() == dev.sixik.unigui.widgets.render.NodeGraphRenderPhase.BACKGROUND) {
                backgroundState[0] = state;
            } else {
                foregroundState[0] = state;
            }
        });
        graph.render(new DefaultRenderContext(new DrawList()));
        expect(backgroundState[0] != null
                        && foregroundState[0] != null
                        && foregroundState[0].items().size() == 2
                        && foregroundState[0].items().stream().anyMatch(item -> item.id().equals("label") && item.selected()),
                "NodeGraph renderer should receive immutable background and foreground snapshots");
    }

    private void testNodeGraphPhaseTwoConnectionContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        NodeGraph graph = Widgets.nodeGraph();
        graph.setUiContextInternal(uiContext);

        NodeGraphItem source = graph.addItem("source", new Label("Source"), 20.0f, 20.0f).size(80.0f, 40.0f);
        source.addPort("out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.5f).type("item");
        NodeGraphItem sink = graph.addItem("sink", new Label("Sink"), 180.0f, 20.0f).size(80.0f, 40.0f);
        sink.addPort("in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f).type("item");
        NodeGraphItem blocked = graph.addItem("blocked", new Label("Blocked"), 180.0f, 90.0f).size(80.0f, 40.0f);
        blocked.addPort("in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f).type("fluid");

        graph.measure(new LayoutContext(320.0f, 180.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));

        Counter connectionCounters = new Counter();
        java.util.concurrent.atomic.AtomicReference<NodeGraphPortRef> lastFrom =
                new java.util.concurrent.atomic.AtomicReference<>(new NodeGraphPortRef("", ""));
        java.util.concurrent.atomic.AtomicReference<NodeGraphPortRef> lastTo =
                new java.util.concurrent.atomic.AtomicReference<>(new NodeGraphPortRef("", ""));
        java.util.concurrent.atomic.AtomicReference<String> lastReason =
                new java.util.concurrent.atomic.AtomicReference<>("");
        graph.onConnectionDragStarted(event -> {
            connectionCounters.started++;
            lastFrom.set(event.from());
        });
        graph.onConnectionCreated(event -> {
            connectionCounters.committed++;
            connectionCounters.lastText = event.connectionId();
            lastTo.set(event.to());
        });
        graph.onConnectionDragEnded(event -> {
            connectionCounters.cancelled++;
            connectionCounters.lastChecked = event.valid();
            lastReason.set(event.reason());
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                100.0f, 40.0f, 100.0f, 40.0f, 10, PointerButton.PRIMARY));
        expect(graph.connectionDragging() && uiContext.capturedPointer(10) == graph
                        && connectionCounters.started == 1
                        && lastFrom.get().equals(new NodeGraphPortRef("source", "out")),
                "NodeGraph should start connection drag from output ports and capture pointer");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                180.0f, 40.0f, 180.0f, 40.0f, 10));
        expect(graph.hoveredPort().equals(new NodeGraphPortRef("sink", "in")),
                "NodeGraph connection drag should expose hovered target port");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                180.0f, 40.0f, 180.0f, 40.0f, 10, PointerButton.PRIMARY));
        expect(!graph.connectionDragging()
                        && uiContext.capturedPointer(10) == null
                        && graph.connections().size() == 1
                        && connectionCounters.committed == 1
                        && connectionCounters.cancelled == 1
                        && connectionCounters.lastChecked
                        && lastTo.get().equals(new NodeGraphPortRef("sink", "in")),
                "NodeGraph should create a valid connection on release over a compatible input port");

        final dev.sixik.unigui.widgets.render.NodeGraphState[] foregroundState = new dev.sixik.unigui.widgets.render.NodeGraphState[1];
        graph.renderer((draw, state) -> {
            if (state.phase() == dev.sixik.unigui.widgets.render.NodeGraphRenderPhase.FOREGROUND) {
                foregroundState[0] = state;
            }
        });
        graph.render(new DefaultRenderContext(new DrawList()));
        expect(foregroundState[0] != null
                        && foregroundState[0].ports().size() == 3
                        && foregroundState[0].connections().size() == 1
                        && !foregroundState[0].connectionPreview().visible(),
                "NodeGraph renderer state should expose ports, connections and hidden preview when idle");
        float idlePortRadius = foregroundState[0].ports().get(0).radius();
        graph.viewport(0.0f, 0.0f, 2.0f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));
        graph.render(new DefaultRenderContext(new DrawList()));
        float zoomedInPortRadius = foregroundState[0].ports().get(0).radius();
        graph.viewport(0.0f, 0.0f, 0.5f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));
        graph.render(new DefaultRenderContext(new DrawList()));
        float zoomedOutPortRadius = foregroundState[0].ports().get(0).radius();
        expect(near(zoomedInPortRadius, idlePortRadius * 2.0f)
                        && near(zoomedOutPortRadius, idlePortRadius * 0.5f),
                "NodeGraph ports should scale with zoom so their size stays stable relative to the node");
        graph.viewport(0.0f, 0.0f, 1.0f);
        graph.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));
        graph.useDefaultRenderer();

        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                100.0f, 40.0f, 100.0f, 40.0f, 11, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                180.0f, 40.0f, 180.0f, 40.0f, 11));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                180.0f, 40.0f, 180.0f, 40.0f, 11, PointerButton.PRIMARY));
        expect(graph.connections().size() == 1
                        && connectionCounters.committed == 1
                        && !connectionCounters.lastChecked
                        && lastReason.get().equals("Connection already exists"),
                "NodeGraph should reject duplicate connections through validation");

        graph.connectionPolicy((candidateGraph, from, to) -> to.itemId().equals("blocked")
                ? NodeGraphConnectionValidation.invalid("Blocked by test policy")
                : NodeGraphConnectionValidation.accepted());
        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                100.0f, 40.0f, 100.0f, 40.0f, 12, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                180.0f, 110.0f, 180.0f, 110.0f, 12));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                180.0f, 110.0f, 180.0f, 110.0f, 12, PointerButton.PRIMARY));
        expect(graph.connections().size() == 1
                        && connectionCounters.committed == 1
                        && !connectionCounters.lastChecked
                        && lastReason.get().equals("Blocked by test policy"),
                "NodeGraph should allow custom connection policy to reject UI-created connections");
        graph.connectionPolicy(null);

        Counter connectionSelection = new Counter();
        graph.onConnectionSelectionChanged(event -> {
            connectionSelection.count++;
            connectionSelection.lastOldText = event.newSelection().isEmpty() ? "" : event.newSelection().get(0);
        });
        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                140.0f, 40.0f, 140.0f, 40.0f, 13, PointerButton.PRIMARY));
        expect(graph.selectedConnectionIds().equals(java.util.List.of(connectionCounters.lastText))
                        && connectionSelection.count == 1
                        && connectionSelection.lastOldText.equals(connectionCounters.lastText),
                "NodeGraph should select a connection by clicking near its line");

        Counter removed = new Counter();
        graph.onConnectionRemoved(event -> {
            removed.count++;
            removed.lastText = event.connectionId();
        });
        graph.removeSelectedConnections();
        expect(graph.connections().isEmpty()
                        && removed.count == 1
                        && removed.lastText.equals(connectionCounters.lastText),
                "NodeGraph should remove selected connections and emit typed removal events");
    }

    private void testNodeGraphPhaseThreeEditingContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        NodeGraph graph = Widgets.nodeGraph()
                .selectionMode(dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode.MULTIPLE)
                .viewport(0.0f, 0.0f)
                .zoomRange(0.5f, 3.0f);
        graph.setUiContextInternal(uiContext);

        NodeGraphItem a = graph.addItem("a", new Label("A"), 20.0f, 20.0f).size(50.0f, 30.0f);
        NodeGraphItem b = graph.addItem("b", new Label("B"), 110.0f, 20.0f).size(50.0f, 30.0f);
        NodeGraphItem c = graph.addItem("c", new Label("C"), 220.0f, 120.0f).size(50.0f, 30.0f);
        c.selectable(false);

        graph.measure(new LayoutContext(320.0f, 180.0f));
        graph.arrange(new MutableRect(0.0f, 0.0f, 320.0f, 180.0f));

        uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, 1.0f, KeyModifiers.CONTROL));
        dev.sixik.unigui.api.math.RectView renderedA =
                graph.renderedBoundsForChild(a.content(), a.content().layoutBounds());
        expect(graph.viewport().zoom() > 1.0f
                        && near(a.content().layoutBounds().x(), 12.0f)
                        && near(a.content().layoutBounds().y(), 16.0f)
                        && near(a.content().layoutBounds().width(), 50.0f)
                        && near(a.content().layoutBounds().height(), 30.0f)
                        && renderedA != null
                        && renderedA.width() > a.content().layoutBounds().width()
                        && renderedA.height() > a.content().layoutBounds().height(),
                "NodeGraph Ctrl+wheel should keep child layout unscaled and apply camera zoom to rendered bounds");
        graph.scaleContentWithZoom(false).viewport(0.0f, 0.0f, 1.0f);
        uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, 1.0f, KeyModifiers.CONTROL));
        expect(graph.viewport().zoom() > 1.0f
                        && near(a.content().layoutBounds().width(), 50.0f)
                        && near(a.content().layoutBounds().height(), 30.0f),
                "NodeGraph scaleContentWithZoom(false) should keep node content as screen-space overlay mode");
        graph.scaleContentWithZoom(true);

        graph.viewport(0.0f, 0.0f, 1.0f);
        graph.wheelPanStep(20.0f);
        boolean wheelPanConsumed = uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, -1.0f));
        expect(wheelPanConsumed
                        && near(graph.viewport().x(), 0.0f)
                        && near(graph.viewport().y(), -20.0f),
                "NodeGraph plain wheel should pan the canvas vertically");
        boolean shiftWheelPanConsumed = uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, -1.0f, KeyModifiers.SHIFT));
        expect(shiftWheelPanConsumed
                        && near(graph.viewport().x(), -20.0f)
                        && near(graph.viewport().y(), -20.0f),
                "NodeGraph Shift+wheel should pan the canvas horizontally");

        graph.viewport(0.0f, 0.0f, 1.0f).wheelPanningEnabled(false).consumeWheelWhileHovered(false);
        boolean disabledWheelPanConsumed = uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, -1.0f));
        expect(!disabledWheelPanConsumed
                        && near(graph.viewport().x(), 0.0f)
                        && near(graph.viewport().y(), 0.0f),
                "NodeGraph wheel panning should be independently disableable");
        graph.consumeWheelWhileHovered(true);
        float zoomBeforeDisabled = graph.viewport().zoom();
        boolean disabledZoomConsumed = uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, 1.0f, KeyModifiers.CONTROL));
        expect(disabledZoomConsumed && graph.viewport().zoom() > zoomBeforeDisabled,
                "NodeGraph Ctrl+wheel should still scale when wheel panning is disabled");
        graph.zoomEnabled(false);
        float zoomBeforeBlocked = graph.viewport().zoom();
        boolean blockedZoomConsumed = uiContext.routedEvents().dispatch(new ScrollEvent(graph,
                100.0f, 60.0f, 100.0f, 60.0f, 0.0f, 1.0f, KeyModifiers.CONTROL));
        expect(blockedZoomConsumed && near(graph.viewport().zoom(), zoomBeforeBlocked),
                "NodeGraph zoomEnabled(false) should block Ctrl+wheel scale while preserving wheel capture");
        graph.zoomEnabled(true).wheelPanningEnabled(true);

        graph.viewport(0.0f, 0.0f, 1.0f);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                5.0f, 5.0f, 5.0f, 5.0f, 20, PointerButton.PRIMARY));
        expect(graph.lassoSelecting() && uiContext.capturedPointer(20) == graph,
                "NodeGraph should start lasso selection from empty canvas");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                180.0f, 80.0f, 180.0f, 80.0f, 20));
        expect(graph.selectedItemIds().equals(java.util.List.of("a", "b")),
                "NodeGraph lasso should select intersecting selectable items");

        final dev.sixik.unigui.widgets.render.NodeGraphState[] lassoState = new dev.sixik.unigui.widgets.render.NodeGraphState[1];
        graph.renderer((draw, state) -> {
            if (state.phase() == dev.sixik.unigui.widgets.render.NodeGraphRenderPhase.FOREGROUND) {
                lassoState[0] = state;
            }
        });
        graph.render(new DefaultRenderContext(new DrawList()));
        expect(lassoState[0] != null && lassoState[0].selectionBox().visible(),
                "NodeGraph renderer state should expose active lasso selection box");
        graph.useDefaultRenderer();

        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                180.0f, 80.0f, 180.0f, 80.0f, 20, PointerButton.PRIMARY));
        expect(!graph.lassoSelecting() && uiContext.capturedPointer(20) == null,
                "NodeGraph should release pointer capture after lasso selection");

        Counter resized = new Counter();
        graph.onItemResized(event -> {
            resized.count++;
            resized.lastText = event.itemId();
            resized.lastValue = event.newWidth();
            resized.lastNewHeight = event.newHeight();
        });
        graph.clearSelection().selectItem("a");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(graph,
                70.0f, 50.0f, 70.0f, 50.0f, 21, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerMovedEvent(graph,
                90.0f, 65.0f, 90.0f, 65.0f, 21));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(graph,
                90.0f, 65.0f, 90.0f, 65.0f, 21, PointerButton.PRIMARY));
        expect(near(a.arrangedWidth(), 70.0f)
                        && near(a.arrangedHeight(), 45.0f)
                        && resized.count == 1
                        && resized.lastText.equals("a")
                        && near(resized.lastValue, 70.0f)
                        && near(resized.lastNewHeight, 45.0f),
                "NodeGraph resize handle should resize selected items and emit resize events");

        graph.selectionMode(dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode.MULTIPLE);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(graph, KeyCodes.A, 0, KeyModifiers.CONTROL));
        expect(graph.selectedItemIds().equals(java.util.List.of("a", "b")),
                "NodeGraph Ctrl+A should select all selectable visible items");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(graph, KeyCodes.RIGHT, 0, KeyModifiers.SHIFT));
        expect(near(a.x(), 30.0f) && near(b.x(), 120.0f),
                "NodeGraph keyboard arrows should move selected items in world-space");

        Counter removed = new Counter();
        graph.onItemRemoved(event -> {
            removed.count++;
            removed.lastText = event.itemId();
        });
        uiContext.routedEvents().dispatch(new KeyPressedEvent(graph, KeyCodes.DELETE, 0, 0));
        expect(graph.item("a") == null
                        && graph.item("b") == null
                        && graph.item("c") != null
                        && removed.count == 2,
                "NodeGraph Delete should remove selected items without removing unselectable items");
    }

    private void testNodeGraphPhaseFourSnapshotContracts() {
        NodeGraph graph = Widgets.nodeGraph()
                .selectionMode(dev.sixik.unigui.widgets.graph.NodeGraphSelectionMode.MULTIPLE)
                .viewport(24.0f, -18.0f, 1.4f);
        NodeGraphItem input = graph.addItem("input", new Label("Input"), 32.0f, 48.0f)
                .size(118.0f, 64.0f)
                .resizable(false);
        input.addPort("out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.50f).type("item");
        NodeGraphItem machine = graph.addItem("machine", new Label("Machine"), 230.0f, 68.0f)
                .size(136.0f, 82.0f);
        machine.addPort("in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.35f).type("item");
        machine.addPort("out", NodeGraphPortKind.OUTPUT, NodeGraphPortSide.RIGHT, 0.65f).type("fluid").enabled(false);
        NodeGraphItem hidden = graph.addItem("hidden", new Label("Hidden"), 420.0f, 120.0f)
                .size(80.0f, 40.0f)
                .visible(false)
                .movable(false);
        hidden.addPort("in", NodeGraphPortKind.INPUT, NodeGraphPortSide.LEFT, 0.5f).visible(false);
        NodeGraphConnection connection = graph.addConnection("item-link",
                new NodeGraphPortRef("input", "out"), new NodeGraphPortRef("machine", "in"))
                .type("item");
        graph.selectItem(input).selectItem(machine, true);
        graph.selectConnection(connection);

        NodeGraphSnapshot snapshot = graph.snapshot();
        expect(near(snapshot.viewportX(), 24.0f)
                        && near(snapshot.viewportY(), -18.0f)
                        && near(snapshot.zoom(), 1.4f)
                        && snapshot.items().size() == 3
                        && snapshot.connections().size() == 1
                        && snapshot.selectedItemIds().equals(java.util.List.of("input", "machine"))
                        && snapshot.selectedConnectionIds().equals(java.util.List.of("item-link")),
                "NodeGraphSnapshot should capture viewport, items, connections and selections");

        NodeGraph restored = Widgets.nodeGraph();
        restored.restoreSnapshot(snapshot, (itemId, contentType) -> itemId.equals("hidden") ? null : new Label("Restored " + itemId));
        restored.measure(new LayoutContext(480.0f, 260.0f));
        restored.arrange(new MutableRect(0.0f, 0.0f, 480.0f, 260.0f));

        NodeGraphItem restoredInput = restored.item("input");
        NodeGraphItem restoredMachine = restored.item("machine");
        expect(restored.items().size() == 2
                        && restoredInput != null
                        && restoredMachine != null
                        && restored.item("hidden") == null
                        && near(restored.viewport().x(), 24.0f)
                        && near(restored.viewport().y(), -18.0f)
                        && near(restored.viewport().zoom(), 1.4f),
                "NodeGraph restore should rebuild resolved items, ignore unresolved widgets and restore viewport");
        expect(near(restoredInput.x(), 32.0f)
                        && near(restoredInput.width(), 118.0f)
                        && !restoredInput.resizable()
                        && restoredInput.port("out") != null
                        && restoredInput.port("out").type().equals("item")
                        && restoredMachine.port("out") != null
                        && !restoredMachine.port("out").enabled()
                        && restored.connections().size() == 1
                        && restored.connection("item-link").type().equals("item"),
                "NodeGraph restore should preserve item flags, port metadata and valid connections");
        expect(restored.selectedItemIds().equals(java.util.List.of("input", "machine"))
                        && restored.selectedConnectionIds().equals(java.util.List.of("item-link")),
                "NodeGraph restore should preserve selection for resolved items and connections");
    }

    private void testFixedRowVirtualizationCore() {
        FixedRowVirtualizer virtualizer = new FixedRowVirtualizer()
                .itemCount(1_000)
                .itemExtent(10.0f)
                .overscan(1)
                .viewportExtent(50.0f);

        VirtualRange initial = virtualizer.visibleRange();
        expect(initial.firstIndex() == 0 && initial.lastIndexExclusive() == 8 && initial.count() == 8,
                "FixedRowVirtualizer should calculate the first realized range with overscan");
        expect(virtualizer.contentExtent() == 10_000.0f && virtualizer.maxScrollOffset() == 9_950.0f,
                "FixedRowVirtualizer should expose content and max scroll extents");

        virtualizer.scrollOffset(95.0f);
        VirtualRange scrolled = virtualizer.visibleRange();
        expect(scrolled.firstIndex() == 8 && scrolled.lastIndexExclusive() == 16,
                "FixedRowVirtualizer should shift range with scroll offset");
        expect(near(virtualizer.itemOffset(8), -15.0f),
                "FixedRowVirtualizer should map item index to viewport-relative offset");

        virtualizer.itemCount(10).scrollOffset(20_000.0f);
        expect(virtualizer.scrollOffset() == 50.0f && virtualizer.visibleRange().lastIndexExclusive() == 10,
                "FixedRowVirtualizer should clamp scroll and ranges when item count shrinks");
    }

    private void testVirtualizedSelectionContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualListView list = new VirtualListView()
                .itemCount(20)
                .itemHeight(10.0f)
                .selectionMode(SelectionMode.MULTIPLE);
        list.setUiContextInternal(uiContext);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        Counter listSelectionChanges = new Counter();
        list.onSelectionChanged((SelectionChangedEvent event) -> {
            listSelectionChanges.count++;
            listSelectionChanges.lastSelection = event.newSelection();
        });

        list.selectIndex(2);
        expect(list.selectedIndex() == 2 && list.selectedIndices().equals(java.util.List.of(2)),
                "VirtualListView should expose single selected index");
        list.toggleIndex(4);
        expect(list.selectedIndices().equals(java.util.List.of(2, 4)) && listSelectionChanges.count == 2,
                "VirtualListView should support multi-selection toggle and emit changes");
        expect(listSelectionChanges.lastSelection.equals(java.util.List.of(2, 4)),
                "VirtualListView selection event should expose new selection snapshot");

        DrawList selectedListDrawList = new DrawList();
        list.render(new DefaultRenderContext(selectedListDrawList));
        expect(hasFillColor(selectedListDrawList, 0.18f, 0.45f, 0.75f, 0.35f),
                "VirtualListView should render selected row highlight");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(list, 5.0f, 35.0f, 5.0f, 35.0f, 0, PointerButton.PRIMARY));
        expect(list.selectedIndices().equals(java.util.List.of(3)),
                "VirtualListView primary click should single-select clicked row");
        list.selectIndex(0);
        var row3 = list.children().get(3);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(row3, 5.0f, 35.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(list.selectedIndices().equals(java.util.List.of(3)),
                "VirtualListView bubbled row-child click should use list-local coordinates");
        list.itemCount(3);
        expect(list.selectedIndices().isEmpty(), "VirtualListView should prune selected indices when item count shrinks");

        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Value", 50.0f)
                .rowCount(20)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .selectionMode(SelectionMode.MULTIPLE);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        Counter tableSelectionChanges = new Counter();
        table.onSelectionChanged((SelectionChangedEvent event) -> {
            tableSelectionChanges.count++;
            tableSelectionChanges.lastSelection = event.newSelection();
        });

        table.selectRow(1);
        table.toggleRow(3);
        expect(table.selectedRow() == 1 && table.selectedRows().equals(java.util.List.of(1, 3)),
                "VirtualTableView should support row multi-selection");
        expect(tableSelectionChanges.count == 2 && tableSelectionChanges.lastSelection.equals(java.util.List.of(1, 3)),
                "VirtualTableView should emit row selection changes");

        DrawList selectedTableDrawList = new DrawList();
        table.render(new DefaultRenderContext(selectedTableDrawList));
        expect(hasFillColor(selectedTableDrawList, 0.18f, 0.45f, 0.75f, 0.42f),
                "VirtualTableView should render selected row highlight");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 35.0f, 5.0f, 35.0f, 0, PointerButton.PRIMARY));
        expect(table.selectedRows().equals(java.util.List.of(2)),
                "VirtualTableView primary click should single-select clicked row below header");
        table.rowCount(2);
        expect(table.selectedRows().isEmpty(), "VirtualTableView should prune selected rows when row count shrinks");
    }

    private void testVirtualListViewRealizationAndScrolling() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Counter created = new Counter();
        VirtualListView list = new VirtualListView()
                .itemCount(1_000)
                .itemHeight(10.0f)
                .overscan(1)
                .scrollStep(20.0f)
                .itemFactory(index -> {
                    created.count++;
                    return new Label("Row " + index);
                });
        list.setUiContextInternal(uiContext);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));

        expect(list.contentHeight() == 10_000.0f, "VirtualListView should expose virtual content height");
        expect(list.realizedCount() == 8, "VirtualListView should realize only visible rows plus overscan");
        expect(list.realizedRange().equals(new VirtualRange(0, 8)), "VirtualListView should expose shared realized range");
        expect(created.count == 8, "VirtualListView should not materialize all rows");
        expect(list.firstVisibleIndex() == 0 && list.lastVisibleIndexExclusive() == 8, "VirtualListView should track realized range");
        expect(list.children().size() == 9, "VirtualListView children should include realized rows plus scrollbar");
        expect(list.children().get(0).layoutBounds().y() == 0.0f, "First realized row should be arranged at viewport origin");

        list.scrollTo(95.0f);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        expect(list.scrollY() == 95.0f, "VirtualListView should keep explicit scroll offset");
        expect(list.firstVisibleIndex() == 8 && list.lastVisibleIndexExclusive() == 16, "VirtualListView should shift realized range with scroll");
        expect(list.realizedRange().equals(new VirtualRange(8, 16)), "VirtualListView range should be sourced from shared virtualization core");
        expect(list.realizedCount() == 8, "VirtualListView should keep realized window bounded after scroll");
        expect(created.count < 20, "VirtualListView should create only newly visible rows after scroll");

        boolean consumed = uiContext.routedEvents().dispatch(new ScrollEvent(list, 5.0f, 5.0f, 5.0f, 5.0f, 0.0f, -1.0f));
        expect(consumed && list.scrollY() == 115.0f, "VirtualListView should consume wheel scroll when offset changes");
        list.scrollTo(20_000.0f);
        expect(list.scrollY() == 9_950.0f, "VirtualListView should clamp to max scroll");

        DrawList drawList = new DrawList();
        list.render(new DefaultRenderContext(drawList));
        expect(drawList.commands().get(0).type() == DrawCommandType.PUSH_CLIP, "VirtualListView should clip realized rows");
        expect(hasCommand(drawList, DrawCommandType.POP_CLIP), "VirtualListView should pop row clip");
    }

    private void testVirtualListNestedWheelLockAndOptOut() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualListView list = new VirtualListView()
                .itemCount(100)
                .itemHeight(10.0f)
                .scrollStep(20.0f)
                .itemFactory(index -> new Label("Row " + index));
        ScrollView outer = new ScrollView(list).contentSize(100.0f, 300.0f).scrollStep(20.0f);
        outer.setUiContextInternal(uiContext);
        outer.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 50.0f));
        list.scrollTo(list.maxScrollY());
        outer.scrollTo(0.0f, 0.0f);

        boolean consumedAtListEdge = uiContext.routedEvents().dispatch(
                new ScrollEvent(list, 5.0f, 5.0f, 5.0f, 5.0f, 0.0f, -1.0f));
        expect(consumedAtListEdge && outer.scrollY() == 0.0f,
                "Nested VirtualListView should retain wheel ownership at scroll bounds by default");

        list.consumeWheelAtScrollBounds(false);
        boolean bubbledAtListEdge = uiContext.routedEvents().dispatch(
                new ScrollEvent(list, 5.0f, 5.0f, 5.0f, 5.0f, 0.0f, -1.0f));
        expect(bubbledAtListEdge && outer.scrollY() == 20.0f,
                "VirtualListView consumeWheelAtScrollBounds(false) should allow parent fallback at bounds");
    }

    private void testVirtualListKeyboardNavigation() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualListView list = new VirtualListView()
                .itemCount(20)
                .itemHeight(10.0f)
                .overscan(0)
                .selectionMode(SelectionMode.MULTIPLE)
                .itemFactory(index -> new Label("Row " + index));
        list.setUiContextInternal(uiContext);
        list.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 30.0f));

        expect(list.focusable(), "VirtualListView should be focusable for keyboard/gamepad navigation");
        uiContext.focusManager().requestFocus(list);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(list, KeyCodes.DOWN, 0, 0));
        expect(list.activeIndex() == 1 && list.selectedIndex() == 1,
                "VirtualListView Down should move active row and select it");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(list, KeyCodes.DOWN, 0, KeyModifiers.CONTROL));
        expect(list.activeIndex() == 2 && list.selectedIndex() == 1,
                "VirtualListView Ctrl+Down should move active row without changing selection");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(list, KeyCodes.DOWN, 0, KeyModifiers.SHIFT));
        expect(list.activeIndex() == 3 && list.selectedIndices().equals(java.util.List.of(1, 2, 3)),
                "VirtualListView Shift+Down should extend range selection from the selection anchor");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(list, KeyCodes.PAGE_DOWN, 0, 0));
        expect(list.activeIndex() == 6 && list.selectedIndex() == 6 && list.scrollY() > 0.0f,
                "VirtualListView PageDown should advance by visible rows and scroll active row into view");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(list, KeyCodes.HOME, 0, 0));
        expect(list.activeIndex() == 0 && list.selectedIndex() == 0 && list.scrollY() == 0.0f,
                "VirtualListView Home should jump to the first row and reveal it");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(list, 5.0f, 15.0f, 5.0f, 15.0f, 0, PointerButton.PRIMARY));
        expect(uiContext.focusManager().focusedWidget() == list && list.activeIndex() == 1 && list.selectedIndex() == 1,
                "VirtualListView pointer row click should focus, activate and select the clicked row");

        DrawList focusedDrawList = new DrawList();
        list.render(new DefaultRenderContext(focusedDrawList));
        expect(focusedDrawList.commands().stream().anyMatch(command -> command.type() == DrawCommandType.RECT && command.paint().isStroke()),
                "Focused VirtualListView should draw an active row outline");
    }

    private void testVirtualTableViewVirtualRowsAndRendering() {
        DefaultUIContext uiContext = new DefaultUIContext();
        Counter cellRequests = new Counter();
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Value", 50.0f)
                .rowCount(1_000)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .overscan(1)
                .scrollStep(20.0f)
                .cellTextProvider((row, column) -> {
                    cellRequests.count++;
                    return "R" + row + "C" + column;
                });
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));

        expect(table.contentWidth() == 110.0f && table.contentHeight() == 10_000.0f,
                "VirtualTableView should expose virtual content size");
        expect(table.firstVisibleRow() == 0 && table.lastVisibleRowExclusive() == 8,
                "VirtualTableView should track visible row range with overscan");
        expect(table.realizedRange().equals(new VirtualRange(0, 8)),
                "VirtualTableView should expose shared realized range");
        expect(table.realizedRowCount() == 8, "VirtualTableView should bound realized row count");
        expect(table.children().size() == 1 && table.children().get(0) == table.verticalScrollBar(),
                "VirtualTableView should expose only scrollbar as child, not every cell");

        DrawList initialDrawList = new DrawList();
        table.render(new DefaultRenderContext(initialDrawList));
        expect(initialDrawList.commands().get(0).type() == DrawCommandType.RECT, "VirtualTableView should draw a fixed header first");
        expect(hasText(initialDrawList, "Name") && hasText(initialDrawList, "Value"), "VirtualTableView should render column headers");
        expect(hasText(initialDrawList, "R0C0") && hasText(initialDrawList, "R7C1"), "VirtualTableView should render visible cells");
        expect(!hasText(initialDrawList, "R100C0"), "VirtualTableView should not render offscreen cells");
        expect(cellRequests.count <= table.realizedRowCount() * table.columns().size(),
                "VirtualTableView should request cell text only for rendered virtual rows");

        table.scrollTo(95.0f);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 60.0f));
        expect(table.scrollY() == 95.0f, "VirtualTableView should keep explicit scroll offset");
        expect(table.firstVisibleRow() == 8 && table.lastVisibleRowExclusive() == 16,
                "VirtualTableView should shift visible rows with scroll");
        expect(table.realizedRange().equals(new VirtualRange(8, 16)),
                "VirtualTableView range should be sourced from shared virtualization core");

        DrawList scrolledDrawList = new DrawList();
        table.render(new DefaultRenderContext(scrolledDrawList));
        expect(hasText(scrolledDrawList, "R8C0") && hasText(scrolledDrawList, "R15C1"),
                "VirtualTableView should render scrolled virtual rows");
        expect(!hasText(scrolledDrawList, "R0C0"), "VirtualTableView should stop rendering rows outside the realized window");

        boolean consumed = uiContext.routedEvents().dispatch(new ScrollEvent(table, 4.0f, 14.0f, 4.0f, 4.0f, 0.0f, -1.0f));
        expect(consumed && table.scrollY() == 115.0f, "VirtualTableView should consume wheel scroll when offset changes");
        table.scrollTo(20_000.0f);
        expect(table.scrollY() == 9_950.0f, "VirtualTableView should clamp to max row scroll");
    }

    private void testVirtualTableNestedWheelLockAndOptOut() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 80.0f)
                .rowCount(100)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .scrollStep(20.0f)
                .cellTextProvider((row, column) -> "R" + row);
        ScrollView outer = new ScrollView(table).contentSize(100.0f, 300.0f).scrollStep(20.0f);
        outer.setUiContextInternal(uiContext);
        outer.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 100.0f));
        table.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 60.0f));
        table.scrollTo(table.maxScrollY());
        outer.scrollTo(0.0f, 0.0f);

        boolean consumedAtTableEdge = uiContext.routedEvents().dispatch(
                new ScrollEvent(table, 5.0f, 20.0f, 5.0f, 20.0f, 0.0f, -1.0f));
        expect(consumedAtTableEdge && outer.scrollY() == 0.0f,
                "Nested VirtualTableView should retain wheel ownership at scroll bounds by default");

        table.consumeWheelAtScrollBounds(false);
        boolean bubbledAtTableEdge = uiContext.routedEvents().dispatch(
                new ScrollEvent(table, 5.0f, 20.0f, 5.0f, 20.0f, 0.0f, -1.0f));
        expect(bubbledAtTableEdge && outer.scrollY() == 20.0f,
                "VirtualTableView consumeWheelAtScrollBounds(false) should allow parent fallback at bounds");
    }

    private void testVirtualTableKeyboardNavigation() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Score", 50.0f)
                .rowCount(20)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .selectionMode(SelectionMode.MULTIPLE)
                .cellTextProvider((row, column) -> "R" + row + "C" + column);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 40.0f));

        expect(table.focusable(), "VirtualTableView should be focusable for keyboard/gamepad navigation");
        uiContext.focusManager().requestFocus(table);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.DOWN, 0, 0));
        expect(table.activeRow() == 1 && table.activeColumn() == 0 && table.selectedRow() == 1,
                "VirtualTableView Down should move active cell row and select the source row");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.RIGHT, 0, 0));
        expect(table.activeRow() == 1 && table.activeColumn() == 1 && table.selectedRow() == 1,
                "VirtualTableView Right should move active column without changing row selection");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.DOWN, 0, KeyModifiers.CONTROL));
        expect(table.activeRow() == 2 && table.activeColumn() == 1 && table.selectedRow() == 1,
                "VirtualTableView Ctrl+Down should move active row without changing selection");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.DOWN, 0, KeyModifiers.SHIFT));
        expect(table.activeRow() == 3 && table.selectedRows().equals(java.util.List.of(1, 2, 3)),
                "VirtualTableView Shift+Down should extend row range selection");

        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.PAGE_DOWN, 0, 0));
        expect(table.activeRow() == 6 && table.selectedRow() == 6 && table.scrollY() > 0.0f,
                "VirtualTableView PageDown should advance by visible body rows and reveal the active row");

        table.scrollTo(0.0f);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 65.0f, 25.0f, 65.0f, 25.0f, 0, PointerButton.PRIMARY));
        expect(uiContext.focusManager().focusedWidget() == table && table.activeRow() == 1 && table.activeColumn() == 1,
                "VirtualTableView body click should focus and activate the clicked cell");

        String[] names = {"Bob", "Alice", "Carol"};
        VirtualTableView sorted = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .rowCount(names.length)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .cellTextProvider((row, column) -> names[row])
                .sortKeyProvider((row, column) -> names[row]);
        sorted.setUiContextInternal(uiContext);
        sorted.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        sorted.sortBy(0, SortDirection.ASCENDING);
        uiContext.focusManager().requestFocus(sorted);
        sorted.activeCell(1, 0);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(sorted, KeyCodes.DOWN, 0, 0));
        expect(sorted.activeRow() == 0 && sorted.selectedRow() == 0,
                "VirtualTableView row navigation should follow sorted visual order while selecting source rows");

        uiContext.focusManager().requestFocus(table);
        DrawList focusedDrawList = new DrawList();
        table.render(new DefaultRenderContext(focusedDrawList));
        expect(focusedDrawList.commands().stream().anyMatch(command -> command.type() == DrawCommandType.RECT && command.paint().isStroke()),
                "Focused VirtualTableView should draw an active cell outline");
    }

    private void testVirtualTableCellEditingContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        String[][] cells = {
                {"Alice", "10"},
                {"Bob", "20"},
                {"Carol", "30"}
        };
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Score", 50.0f)
                .rowCount(cells.length)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .editable(true)
                .cellTextProvider((row, column) -> cells[row][column]);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 40.0f));

        Counter editEvents = new Counter();
        table.onCellEditStarted((TableCellEditStartedEvent event) -> {
            editEvents.started++;
            editEvents.lastRow = event.row();
            editEvents.lastColumn = event.column();
            editEvents.lastText = event.text();
        });
        table.onCellEditCommitted((TableCellEditCommittedEvent event) -> {
            editEvents.committed++;
            editEvents.lastRow = event.row();
            editEvents.lastColumn = event.column();
            editEvents.lastOldText = event.oldText();
            editEvents.lastText = event.newText();
            cells[event.row()][event.column()] = event.newText();
        });
        table.onCellEditCancelled((TableCellEditCancelledEvent event) -> {
            editEvents.cancelled++;
            editEvents.lastRow = event.row();
            editEvents.lastColumn = event.column();
            editEvents.lastText = event.text();
        });

        uiContext.focusManager().requestFocus(table);
        table.activeCell(0, 0);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.ENTER, 0, 0));
        expect(table.editing() && table.editingRow() == 0 && table.editingColumn() == 0,
                "VirtualTableView Enter should begin editing the active editable cell");
        expect(editEvents.started == 1 && editEvents.lastText.equals("Alice"),
                "VirtualTableView should emit cell edit started with the original cell text");

        DrawList editingDrawList = new DrawList();
        table.render(new DefaultRenderContext(editingDrawList));
        expect(hasText(editingDrawList, "Alice"),
                "VirtualTableView should render the active TextField editor over the editing cell");

        table.clearInvalidation(InvalidationFlags.ALL);
        uiContext.routedEvents().dispatch(new TextInputEvent(table, 'Z', 0));
        expect(table.editingText().equals("Z"),
                "VirtualTableView should route text input into the active cell editor");
        expect(hasFlag(table.invalidationFlags(), InvalidationFlags.VISUAL)
                        && !hasFlag(table.invalidationFlags(), InvalidationFlags.LAYOUT),
                "VirtualTableView cell editor typing should invalidate visuals without forcing table layout");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.ENTER, 0, 0));
        expect(!table.editing() && editEvents.committed == 1 && editEvents.lastOldText.equals("Alice") && editEvents.lastText.equals("Z"),
                "VirtualTableView Enter inside editor should commit edited text");
        expect(cells[0][0].equals("Z"),
                "VirtualTableView commit event should let data owners update their backing model");

        table.activeCell(1, 1);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.F2, 0, 0));
        expect(table.editing() && table.editingText().equals("20"),
                "VirtualTableView F2 should begin editing the active cell");
        uiContext.routedEvents().dispatch(new TextInputEvent(table, '9', 0));
        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.ESCAPE, 0, 0));
        expect(!table.editing() && editEvents.cancelled == 1 && cells[1][1].equals("20"),
                "VirtualTableView Escape inside editor should cancel without mutating backing data");

        table.editable(false);
        table.activeCell(2, 0);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(table, KeyCodes.ENTER, 0, 0));
        expect(!table.editing(),
                "VirtualTableView should not start editing when editable(false)");
    }

    private void testVirtualTableSortingContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        String[] names = {"Bob", "Alice", "Carol"};
        int[] scores = {20, 10, 30};
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Score", 50.0f)
                .rowCount(names.length)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .cellTextProvider((row, column) -> column == 0 ? names[row] : Integer.toString(scores[row]))
                .sortKeyProvider((row, column) -> column == 0 ? names[row] : scores[row]);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 50.0f));

        Counter sortChanges = new Counter();
        table.onSortChanged((TableSortChangedEvent event) -> {
            sortChanges.count++;
            sortChanges.lastSortColumn = event.newColumnIndex();
            sortChanges.lastSortDirection = event.newDirection();
        });

        table.sortBy(0, SortDirection.ASCENDING);
        DrawList nameAscendingDrawList = new DrawList();
        table.render(new DefaultRenderContext(nameAscendingDrawList));
        expect(table.sortColumnIndex() == 0 && table.sortDirection() == SortDirection.ASCENDING,
                "VirtualTableView should expose ascending sort state");
        expect(hasText(nameAscendingDrawList, "Name ^"), "VirtualTableView should render ascending sort marker in header");
        expect(textCommandIndex(nameAscendingDrawList, "Alice", 0) < textCommandIndex(nameAscendingDrawList, "Bob", 0),
                "VirtualTableView should render rows in ascending sort-key order");

        table.sortBy(1, SortDirection.DESCENDING);
        DrawList scoreDescendingDrawList = new DrawList();
        table.render(new DefaultRenderContext(scoreDescendingDrawList));
        expect(hasText(scoreDescendingDrawList, "Score v"), "VirtualTableView should render descending sort marker in header");
        expect(textCommandIndex(scoreDescendingDrawList, "30", 0) < textCommandIndex(scoreDescendingDrawList, "20", 0),
                "VirtualTableView should render rows in descending numeric sort-key order");
        expect(sortChanges.count == 2 && sortChanges.lastSortColumn == 1 && sortChanges.lastSortDirection == SortDirection.DESCENDING,
                "VirtualTableView should emit sort changed events");

        table.columnComparator(0, (left, right) -> Integer.compare(right, left));
        table.sortBy(0, SortDirection.ASCENDING);
        DrawList customComparatorDrawList = new DrawList();
        table.render(new DefaultRenderContext(customComparatorDrawList));
        expect(textCommandIndex(customComparatorDrawList, "Carol", 0) < textCommandIndex(customComparatorDrawList, "Alice", 0),
                "VirtualTableView should allow per-column row comparator hooks");

        table.clearSort();
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.sortColumnIndex() == 0 && table.sortDirection() == SortDirection.ASCENDING,
                "VirtualTableView header click should start ascending sort for clicked column");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.sortColumnIndex() == 0 && table.sortDirection() == SortDirection.DESCENDING,
                "VirtualTableView repeated header click should cycle to descending sort");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.sortColumnIndex() == -1 && table.sortDirection() == SortDirection.NONE,
                "VirtualTableView third header click should clear sort");
    }

    private void testVirtualTableColumnResizeAndMoveContracts() {
        DefaultUIContext uiContext = new DefaultUIContext();
        VirtualTableView table = new VirtualTableView()
                .addColumn("Name", 60.0f)
                .addColumn("Score", 50.0f)
                .addColumn("Kind", 40.0f)
                .rowCount(2)
                .rowHeight(10.0f)
                .headerHeight(10.0f)
                .minColumnWidth(30.0f)
                .cellTextProvider((row, column) -> "R" + row + "C" + column);
        table.setUiContextInternal(uiContext);
        table.arrange(new MutableRect(0.0f, 0.0f, 180.0f, 40.0f));

        Counter columnEvents = new Counter();
        table.onColumnResized((TableColumnResizedEvent event) -> {
            columnEvents.resized++;
            columnEvents.lastColumn = event.column();
            columnEvents.lastOldWidth = event.oldWidth();
            columnEvents.lastNewWidth = event.newWidth();
        });
        table.onColumnMoved((TableColumnMovedEvent event) -> {
            columnEvents.moved++;
            columnEvents.lastOldColumn = event.oldIndex();
            columnEvents.lastNewColumn = event.newIndex();
        });

        table.resizeColumn(1, 20.0f);
        expect(table.columnWidth(1) == 30.0f,
                "VirtualTableView resizeColumn should clamp to minColumnWidth");
        expect(columnEvents.resized == 1 && columnEvents.lastColumn == 1 && columnEvents.lastOldWidth == 50.0f && columnEvents.lastNewWidth == 30.0f,
                "VirtualTableView resizeColumn should emit old/new width event");

        table.sortBy(2, SortDirection.ASCENDING);
        table.activeCell(0, 2);
        table.moveColumn(2, 0);
        expect(table.columns().get(0).header().equals("Kind") && table.sortColumnIndex() == 0 && table.activeColumn() == 0,
                "VirtualTableView moveColumn should reorder headers and remap sort/active column indices");
        expect(columnEvents.moved == 1 && columnEvents.lastOldColumn == 2 && columnEvents.lastNewColumn == 0,
                "VirtualTableView moveColumn should emit moved event");

        table.clearSort();
        table.resizeColumn(0, 60.0f);
        columnEvents.resized = 0;
        expect(table.mouseCursorAt(60.0f, 5.0f) == MouseCursor.RESIZE_HORIZONTAL,
                "VirtualTableView divider should expose a horizontal resize cursor");
        expect(table.mouseCursorAt(45.0f, 5.0f) == MouseCursor.DEFAULT,
                "VirtualTableView header body should retain the default cursor");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(table, 60.0f, 5.0f, 60.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(table.resizingColumn() && table.resizingColumnIndex() == 0 && uiContext.capturedPointer(0) == table,
                "VirtualTableView divider press should start column resize and capture pointer");
        expect(table.mouseCursorAt(25.0f, 25.0f) == MouseCursor.RESIZE_HORIZONTAL,
                "VirtualTableView should retain the resize cursor while dragging outside the header divider");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(table, 85.0f, 5.0f, 85.0f, 5.0f, 0));
        expect(table.columnWidth(0) == 85.0f && columnEvents.resized == 1,
                "VirtualTableView divider drag should resize the captured column");
        expect(table.sortColumnIndex() == -1,
                "VirtualTableView divider drag should not trigger header sorting");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(table, 85.0f, 5.0f, 85.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(!table.resizingColumn() && uiContext.capturedPointer(0) == null,
                "VirtualTableView divider release should end resize and release pointer capture");
        table.columnResizeEnabled(false);
        expect(table.mouseCursorAt(85.0f, 5.0f) == MouseCursor.DEFAULT,
                "VirtualTableView should not expose a resize cursor when column resizing is disabled");

        expect(new Button("Action").mouseCursorAt(1.0f, 1.0f) == MouseCursor.POINTER,
                "Button should expose the pointer cursor by default");
        expect(new TextField().mouseCursorAt(1.0f, 1.0f) == MouseCursor.TEXT,
                "Text inputs should expose the text cursor by default");
        Box customCursor = new Box();
        customCursor.mouseCursor(MouseCursor.RESIZE_VERTICAL);
        expect(customCursor.mouseCursorAt(1.0f, 1.0f) == MouseCursor.RESIZE_VERTICAL,
                "WidgetBase should allow callers to configure a custom mouse cursor");
    }

    private void testComboBoxAndDropDownBoxContracts() {
        ComboBox combo = new ComboBox()
                .items(java.util.List.of("Small", "Medium", "Large"))
                .silentSelectedIndex(1)
                .dropDownMode(ComboBox.DropDownMode.INLINE);
        combo.measure(new LayoutContext(180.0f, 120.0f));
        combo.arrange(new MutableRect(0.0f, 0.0f, 180.0f, combo.desiredSize().height()));

        DrawList closedDrawList = new DrawList();
        combo.render(new DefaultRenderContext(closedDrawList));
        expect(combo.selectedIndex() == 1 && combo.selectedItem().equals("Medium"),
                "ComboBox should expose selected index and selected item");
        expect(hasText(closedDrawList, "Medium ?") && !hasText(closedDrawList, "Small"),
                "Closed ComboBox should render the selected header but not options");

        combo.open();
        combo.measure(new LayoutContext(180.0f, 120.0f));
        combo.arrange(new MutableRect(0.0f, 0.0f, 180.0f, combo.desiredSize().height()));
        DrawList openDrawList = new DrawList();
        combo.render(new DefaultRenderContext(openDrawList));
        expect(combo.opened() && hasText(openDrawList, "Small") && hasText(openDrawList, "Large"),
                "Open ComboBox should render all options");
        expect(combo.optionButton(1).checked(),
                "ComboBox should mark the selected option as checked");

        Counter changes = new Counter();
        combo.onSelectionChanged(event -> {
            changes.count++;
            changes.lastSelection = event.newSelection();
        });
        combo.optionButton(2).click();
        expect(!combo.opened() && combo.selectedIndex() == 2 && combo.selectedItem().equals("Large"),
                "ComboBox option click should select the item and close the drop-down");
        expect(changes.count == 1 && changes.lastSelection.equals(java.util.List.of(2)),
                "ComboBox should emit SelectionChangedEvent when selection changes");

        combo.headerButton().click();
        expect(combo.opened(), "ComboBox header click should reopen the drop-down");
        combo.selectedIndex(99);
        expect(combo.selectedIndex() == 2 && changes.count == 1,
                "Selecting the already selected clamped item should not duplicate events");
        combo.removeItem(2);
        expect(combo.selectedIndex() == 1 && combo.selectedItem().equals("Medium"),
                "ComboBox should keep a valid selection after removing the selected item");

        ComboBox overlayCombo = new ComboBox()
                .items(java.util.List.of("Alpha", "Beta", "Gamma"))
                .silentSelectedIndex(0);
        VBox overlayContent = new VBox();
        OverlayLayer overlayLayer = new OverlayLayer(overlayContent);
        overlayCombo.useOverlay(overlayLayer);
        overlayContent.addChild(overlayCombo);
        overlayCombo.measure(new LayoutContext(160.0f, 120.0f));
        float closedOverlayHeight = overlayCombo.desiredSize().height();
        overlayCombo.open();
        overlayCombo.measure(new LayoutContext(160.0f, 120.0f));
        expect(near(overlayCombo.desiredSize().height(), closedOverlayHeight),
                "Overlay ComboBox should not change its layout height when opened");
        overlayLayer.measure(new LayoutContext(160.0f, 120.0f));
        overlayLayer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 120.0f));
        DrawList overlayDrawList = new DrawList();
        overlayLayer.render(new DefaultRenderContext(overlayDrawList));
        expect(overlayCombo.dropDownMode() == ComboBox.DropDownMode.OVERLAY
                        && overlayCombo.dropDownPopup().opened()
                        && hasText(overlayDrawList, "Beta"),
                "Overlay ComboBox should render options through Popup/OverlayLayer");
        overlayCombo.dropDownPopup().close();
        expect(!overlayCombo.opened(),
                "ComboBox should sync opened state when its overlay Popup is closed externally");

        ComboBox defaultOverlayCombo = new ComboBox()
                .items(java.util.List.of("Default", "Overlay", "Mode"))
                .silentSelectedIndex(0);
        VBox defaultOverlayContent = new VBox();
        defaultOverlayContent.addChild(defaultOverlayCombo);
        OverlayLayer defaultOverlayLayer = new OverlayLayer(defaultOverlayContent);
        defaultOverlayLayer.measure(new LayoutContext(160.0f, 120.0f));
        defaultOverlayLayer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 120.0f));
        float defaultClosedHeight = defaultOverlayCombo.desiredSize().height();
        expect(defaultOverlayCombo.dropDownMode() == ComboBox.DropDownMode.OVERLAY
                        && !defaultOverlayCombo.opened()
                        && !defaultOverlayCombo.dropDownPopup().opened()
                        && defaultOverlayCombo.optionsHost().parent() != defaultOverlayCombo,
                "ComboBox should start closed in overlay mode without inline options");
        defaultOverlayCombo.open();
        defaultOverlayLayer.measure(new LayoutContext(160.0f, 120.0f));
        defaultOverlayLayer.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 120.0f));
        expect(near(defaultOverlayCombo.desiredSize().height(), defaultClosedHeight)
                        && defaultOverlayCombo.dropDownPopup().opened()
                        && defaultOverlayCombo.optionsHost().parent() != defaultOverlayCombo,
                "Default overlay ComboBox should open above layout without changing measured height");

        ComboBox autoOverlayCombo = new ComboBox()
                .items(java.util.List.of("Root", "Nested"))
                .silentSelectedIndex(0);
        VBox nestedContent = new VBox();
        nestedContent.addChild(autoOverlayCombo);
        OverlayLayer nestedOverlay = new OverlayLayer(nestedContent);
        OverlayLayer rootOverlay = new OverlayLayer(nestedOverlay);
        rootOverlay.measure(new LayoutContext(160.0f, 120.0f));
        rootOverlay.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 120.0f));
        autoOverlayCombo.open();
        expect(autoOverlayCombo.attachedOverlayLayer() == rootOverlay,
                "Overlay ComboBox should attach its Popup to the topmost OverlayLayer automatically");

        VBox customPanel = new VBox();
        customPanel.addChild(new Label("Custom content"));
        DropDownBox inlineDropDown = new DropDownBox()
                .headerText("Tools")
                .content(customPanel)
                .dropDownMode(ComboBox.DropDownMode.INLINE);
        inlineDropDown.measure(new LayoutContext(180.0f, 120.0f));
        inlineDropDown.arrange(new MutableRect(0.0f, 0.0f, 180.0f, inlineDropDown.desiredSize().height()));
        DrawList closedDropDownDrawList = new DrawList();
        inlineDropDown.render(new DefaultRenderContext(closedDropDownDrawList));
        expect(inlineDropDown.content() == customPanel
                        && hasText(closedDropDownDrawList, "Tools ?")
                        && !hasText(closedDropDownDrawList, "Custom content"),
                "Closed DropDownBox should render only its header and keep arbitrary content hidden");

        inlineDropDown.open();
        inlineDropDown.measure(new LayoutContext(180.0f, 120.0f));
        inlineDropDown.arrange(new MutableRect(0.0f, 0.0f, 180.0f, inlineDropDown.desiredSize().height()));
        DrawList openDropDownDrawList = new DrawList();
        inlineDropDown.render(new DefaultRenderContext(openDropDownDrawList));
        expect(inlineDropDown.opened()
                        && inlineDropDown.contentHost().parent() == inlineDropDown
                        && hasText(openDropDownDrawList, "Custom content"),
                "Open inline DropDownBox should render arbitrary content in its own content host");

        DropDownBox overlayDropDown = new DropDownBox()
                .headerText("Filters")
                .content(new Label("Overlay content"))
                .dropDownSameWidth();
        VBox dropDownPage = new VBox();
        dropDownPage.addChild(overlayDropDown);
        OverlayLayer dropDownOverlay = new OverlayLayer(dropDownPage);
        dropDownOverlay.measure(new LayoutContext(160.0f, 120.0f));
        dropDownOverlay.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 120.0f));
        float dropDownClosedHeight = overlayDropDown.desiredSize().height();
        overlayDropDown.open();
        dropDownOverlay.measure(new LayoutContext(160.0f, 120.0f));
        dropDownOverlay.arrange(new MutableRect(0.0f, 0.0f, 160.0f, 120.0f));
        DrawList overlayDropDownDrawList = new DrawList();
        dropDownOverlay.render(new DefaultRenderContext(overlayDropDownDrawList));
        expect(overlayDropDown.dropDownMode() == ComboBox.DropDownMode.OVERLAY
                        && overlayDropDown.attachedOverlayLayer() == dropDownOverlay
                        && overlayDropDown.dropDownPopup().opened()
                        && overlayDropDown.contentHost().parent() != overlayDropDown
                        && near(overlayDropDown.desiredSize().height(), dropDownClosedHeight)
                        && hasText(overlayDropDownDrawList, "Overlay content"),
                "Overlay DropDownBox should host arbitrary content through Popup/OverlayLayer without expanding layout");
        overlayDropDown.dropDownPopup().close();
        expect(!overlayDropDown.opened(),
                "DropDownBox should sync opened state when its overlay Popup is closed externally");

        expect(Widgets.comboBox() instanceof ComboBox
                        && Widgets.dropDownBox() instanceof DropDownBox,
                "Widgets factory should expose ComboBox and DropDownBox");
    }

    private void testTabControlContracts() {
        TabControl tabs = new TabControl()
                .addTab("One", new Label("First page"))
                .addTab("Two", new Label("Second page"));
        tabs.measure(new LayoutContext(200.0f, 120.0f));
        tabs.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 80.0f));

        DrawList initialDrawList = new DrawList();
        tabs.render(new DefaultRenderContext(initialDrawList));
        expect(tabs.selectedIndex() == 0 && tabs.selectedContent() instanceof Label,
                "TabControl should select the first tab by default");
        expect(hasText(initialDrawList, "One") && hasText(initialDrawList, "Two")
                        && hasText(initialDrawList, "First page") && !hasText(initialDrawList, "Second page"),
                "TabControl should render all headers and only selected tab content");

        Counter changes = new Counter();
        tabs.onSelectionChanged(event -> {
            changes.count++;
            changes.lastSelection = event.newSelection();
        });
        tabs.tabButton(1).click();
        DrawList selectedDrawList = new DrawList();
        tabs.render(new DefaultRenderContext(selectedDrawList));
        expect(tabs.selectedIndex() == 1 && hasText(selectedDrawList, "Second page") && !hasText(selectedDrawList, "First page"),
                "TabControl tab button click should switch visible content");
        expect(changes.count == 1 && changes.lastSelection.equals(java.util.List.of(1)),
                "TabControl should emit SelectionChangedEvent when selected tab changes");

        tabs.tabButton(1).click();
        expect(tabs.selectedIndex() == 1 && tabs.tabButton(1).checked() && changes.count == 1,
                "Clicking the selected tab should keep it selected without duplicate events");

        tabs.removeTab(1);
        DrawList removedDrawList = new DrawList();
        tabs.render(new DefaultRenderContext(removedDrawList));
        expect(tabs.tabCount() == 1 && tabs.selectedIndex() == 0 && hasText(removedDrawList, "First page"),
                "TabControl should keep a valid selection after removing the selected tab");
        tabs.addTab("Three", new Label("Third page"));
        DefaultUIContext tabUiContext = new DefaultUIContext();
        tabs.setUiContextInternal(tabUiContext);
        tabUiContext.focusManager().requestFocus(tabs);
        tabUiContext.routedEvents().dispatch(new KeyPressedEvent(tabs, KeyCodes.RIGHT, 0, 0));
        DrawList rightKeyDrawList = new DrawList();
        tabs.render(new DefaultRenderContext(rightKeyDrawList));
        expect(tabs.selectedIndex() == 1 && hasText(rightKeyDrawList, "Third page"),
                "Focused TabControl Right key should select the next tab");
        tabUiContext.routedEvents().dispatch(new KeyPressedEvent(tabs, KeyCodes.LEFT, 0, 0));
        expect(tabs.selectedIndex() == 0,
                "Focused TabControl Left key should select the previous tab");
        expect(Widgets.tabControl() instanceof TabControl,
                "Widgets factory should expose TabControl");
    }

    private void testPageViewAndCarouselContracts() {
        Label first = new Label("First standalone page");
        Label second = new Label("Second standalone page");
        Label third = new Label("Third standalone page");
        PageView pages = new PageView()
                .addPage(first)
                .addPage(second)
                .addPage(third);
        pages.measure(new LayoutContext(220.0f, 100.0f));
        pages.arrange(new MutableRect(0.0f, 0.0f, 220.0f, 80.0f));
        DrawList initialDrawList = new DrawList();
        pages.render(new DefaultRenderContext(initialDrawList));
        expect(pages.pageCount() == 3
                        && pages.selectedIndex() == 0
                        && pages.selectedPage() == first
                        && first.parent() == pages
                        && second.parent() == pages
                        && third.parent() == pages
                        && second.visibility() == Visibility.COLLAPSED
                        && hasText(initialDrawList, "First standalone page")
                        && !hasText(initialDrawList, "Second standalone page")
                        && !hasText(initialDrawList, "<")
                        && !hasText(initialDrawList, "1 / 3"),
                "PageView should own retained pages without carousel controls or indicator chrome");

        Counter pageChanges = new Counter();
        pages.onSelectionChanged(event -> {
            pageChanges.count++;
            pageChanges.lastSelection = event.newSelection();
        });
        pages.selectedIndex(1);
        DrawList selectedDrawList = new DrawList();
        pages.render(new DefaultRenderContext(selectedDrawList));
        expect(pages.selectedIndex() == 1
                        && pageChanges.count == 1
                        && pageChanges.lastSelection.equals(java.util.List.of(1))
                        && hasText(selectedDrawList, "Second standalone page")
                        && !hasText(selectedDrawList, "First standalone page"),
                "PageView selectedIndex should switch the visible page and emit selection events");

        pages.selectRelative(10);
        expect(pages.selectedIndex() == 2, "PageView selectRelative should clamp instead of cycling");
        pages.removePage(third);
        expect(pages.pageCount() == 2
                        && pages.selectedIndex() == 1
                        && pages.selectedPage() == second,
                "PageView should keep selection valid after removing the selected page");

        Carousel carousel = new Carousel()
                .addPage(new Label("Carousel page one"))
                .addPage(new Label("Carousel page two"))
                .addPage(new Label("Carousel page three"));
        carousel.measure(new LayoutContext(220.0f, 120.0f));
        carousel.arrange(new MutableRect(0.0f, 0.0f, 220.0f, 100.0f));
        DrawList carouselDrawList = new DrawList();
        carousel.render(new DefaultRenderContext(carouselDrawList));
        expect(carousel.pageView() instanceof PageView
                        && carousel.header().parent() == carousel
                        && carousel.pageView().parent() == carousel
                        && hasText(carouselDrawList, "<")
                        && hasText(carouselDrawList, ">")
                        && hasText(carouselDrawList, "1 / 3"),
                "Carousel should compose PageView with previous/next controls and indicator chrome");
        carousel.selectRelative(-1);
        DrawList cycledDrawList = new DrawList();
        carousel.render(new DefaultRenderContext(cycledDrawList));
        expect(carousel.selectedIndex() == 2
                        && carousel.pageView().selectedPage() == carousel.pages().get(2)
                        && hasText(cycledDrawList, "3 / 3")
                        && hasText(cycledDrawList, "Carousel page three"),
                "Carousel selectRelative should stay cyclic on top of the PageView selection model");

        expect(Widgets.pageView() instanceof PageView
                        && Widgets.carousel() instanceof Carousel,
                "Widgets factory should expose PageView and Carousel separately");
    }

    private void testExpandablePanelAndAccordionContracts() {
        ExpandablePanel panel = new ExpandablePanel("Advanced");
        panel.addContent(new Label("Inner"));
        panel.measure(new LayoutContext(200.0f, 200.0f));
        float expandedHeight = panel.desiredSize().height();
        panel.arrange(new MutableRect(0.0f, 0.0f, 200.0f, expandedHeight));

        DrawList expandedDrawList = new DrawList();
        panel.render(new DefaultRenderContext(expandedDrawList));
        expect(panel.expanded() && hasText(expandedDrawList, "\u25BE Advanced") && hasText(expandedDrawList, "Inner"),
                "ExpandablePanel should render its expanded header and content");

        Counter changes = new Counter();
        panel.onExpandedChanged((ExpandedChangedEvent event) -> {
            changes.count++;
            changes.lastChecked = event.newValue();
        });
        panel.expanded(false);
        panel.measure(new LayoutContext(200.0f, 200.0f));
        expect(panel.desiredSize().height() < expandedHeight,
                "Collapsed ExpandablePanel should remove content from layout");
        DrawList collapsedDrawList = new DrawList();
        panel.arrange(new MutableRect(0.0f, 0.0f, 200.0f, panel.desiredSize().height()));
        panel.render(new DefaultRenderContext(collapsedDrawList));
        expect(hasText(collapsedDrawList, "\u25B8 Advanced") && !hasText(collapsedDrawList, "Inner"),
                "Collapsed ExpandablePanel should render only its header");
        expect(changes.count == 1 && !changes.lastChecked,
                "ExpandablePanel should emit expanded changed events");

        panel.silentExpanded(true);
        expect(panel.expanded() && changes.count == 1,
                "silentExpanded should update state without emitting an event");

        ExpandablePanel first = new ExpandablePanel("First");
        ExpandablePanel second = new ExpandablePanel("Second");
        Accordion accordion = new Accordion()
                .addPanel(first)
                .addPanel(second);
        expect(!first.expanded() && second.expanded(),
                "Accordion singleOpen mode should keep only the latest expanded panel open");
        first.expanded(true);
        expect(first.expanded() && !second.expanded(),
                "Accordion should collapse other panels when one panel opens");
        accordion.singleOpen(false);
        second.expanded(true);
        expect(first.expanded() && second.expanded(),
                "Accordion multi-open mode should allow multiple expanded panels");
        expect(Widgets.expandablePanel("Factory") instanceof ExpandablePanel
                        && Widgets.accordion() instanceof Accordion,
                "Widgets factory should expose ExpandablePanel and Accordion");
    }

    private void testTreeViewContracts() {
        TreeView tree = new TreeView();
        TreeViewNode root = tree.addRoot("Root");
        TreeViewNode child = root.addChild("Child");
        TreeViewNode leaf = child.addChild("Leaf");
        TreeViewNode other = tree.addRoot("Other");
        other.addChild("Hidden");
        other.silentExpanded(false);
        tree.silentSelect(child);

        tree.measure(new LayoutContext(220.0f, 200.0f));
        tree.arrange(new MutableRect(0.0f, 0.0f, 220.0f, tree.desiredSize().height()));
        DrawList expandedDrawList = new DrawList();
        tree.render(new DefaultRenderContext(expandedDrawList));
        expect(tree.rootCount() == 2
                        && tree.selectedNode() == child
                        && tree.selectedPath().equals(java.util.List.of(0, 0))
                        && tree.visibleNodes().size() == 4,
                "TreeView should expose roots, visible nodes and selected path");
        expect(hasText(expandedDrawList, "▾ Root")
                        && hasText(expandedDrawList, "▾ Child")
                        && hasText(expandedDrawList, "  Leaf")
                        && hasText(expandedDrawList, "▸ Other")
                        && !hasText(expandedDrawList, "  Hidden"),
                "TreeView should render expanded branches and hide collapsed descendants");

        child.expanded(false);
        tree.measure(new LayoutContext(220.0f, 200.0f));
        tree.arrange(new MutableRect(0.0f, 0.0f, 220.0f, tree.desiredSize().height()));
        DrawList collapsedDrawList = new DrawList();
        tree.render(new DefaultRenderContext(collapsedDrawList));
        expect(tree.selectedNode() == child
                        && tree.visibleNodes().size() == 3
                        && hasText(collapsedDrawList, "▸ Child")
                        && !hasText(collapsedDrawList, "  Leaf"),
                "Collapsing a TreeView node should remove descendants from layout and render");

        TreeViewNode quiet = tree.addRoot("Quiet");
        quiet.addChild("QuietChild");
        int visibleBeforeSilentExpand = tree.visibleNodes().size();
        quiet.silentExpanded(false);
        expect(tree.visibleNodes().size() == visibleBeforeSilentExpand,
                "TreeViewNode.silentExpanded should not rebuild visible rows immediately");
        tree.addRoot("ForceRebuild");
        expect(!tree.visibleNodes().contains(quiet.child(0)),
                "TreeView should apply silent expansion state on the next explicit rebuild");

        TreeList treeList = new TreeList();
        treeList.addPath("Assets", "Textures", "Buttons");
        treeList.root(0).text("Renamed Assets");
        treeList.addPath("Assets", "Textures", "Icons");
        expect(treeList.rootCount() == 1
                        && treeList.root(0).child(0).childCount() == 2,
                "TreeList.addPath should match existing nodes by stable value after display text changes");

        Counter changes = new Counter();
        tree.onSelectionChanged(event -> {
            changes.count++;
            changes.lastSelection = event.newSelection();
        });
        child.expanded(true);
        leaf.rowButton().click();
        expect(tree.selectedNode() == leaf
                        && tree.selectedPath().equals(java.util.List.of(0, 0, 0))
                        && changes.count == 1
                        && changes.lastSelection.equals(java.util.List.of(0, 0, 0)),
                "TreeView row click should select a node and emit its path");

        root.expanded(false);
        expect(tree.selectedNode() == root
                        && changes.count == 2
                        && changes.lastSelection.equals(java.util.List.of(0)),
                "Collapsing a branch should move hidden descendant selection to the collapsed node");

        DefaultUIContext uiContext = new DefaultUIContext();
        tree.setUiContextInternal(uiContext);
        uiContext.focusManager().requestFocus(tree);
        root.expanded(true);
        child.expanded(true);
        tree.silentSelect(root);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(tree, KeyCodes.DOWN, 0, 0));
        expect(tree.selectedNode() == child,
                "TreeView Down key should move selection to the next visible node");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(tree, KeyCodes.RIGHT, 0, 0));
        expect(tree.selectedNode() == leaf,
                "TreeView Right key should enter an expanded branch");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(tree, KeyCodes.LEFT, 0, 0));
        expect(tree.selectedNode() == child,
                "TreeView Left key should move leaf selection to its parent");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(tree, KeyCodes.SPACE, 0, 0));
        expect(!child.expanded() && tree.selectedNode() == child,
                "TreeView Space key should toggle the selected branch");

        expect(Widgets.treeView() instanceof TreeView
                        && Widgets.treeNode("Factory") instanceof TreeViewNode,
                "Widgets factory should expose TreeView and TreeViewNode");
    }

    private void testLoadingIndicatorContracts() {
        LoadingIndicator spinner = new LoadingIndicator()
                .indicatorSize(24.0f)
                .segments(6)
                .phase(0.0f)
                .speed(2.0f);
        spinner.measure(new LayoutContext(100.0f, 100.0f));
        spinner.arrange(new MutableRect(0.0f, 0.0f, spinner.desiredSize().width(), spinner.desiredSize().height()));
        DrawList spinnerDrawList = new DrawList();
        spinner.render(new DefaultRenderContext(spinnerDrawList));
        expect(near(spinner.desiredSize().width(), 24.0f)
                        && near(spinner.desiredSize().height(), 24.0f)
                        && countCommands(spinnerDrawList, DrawCommandType.CIRCLE) == 6,
                "LoadingIndicator spinner mode should measure square and render configured segments");

        float firstDotX = spinnerDrawList.commands().get(0).bounds().x();
        spinner.tick(new FrameContext(1, 0.25f, 0.0f, FramePhase.ANIMATION));
        DrawList advancedSpinnerDrawList = new DrawList();
        spinner.render(new DefaultRenderContext(advancedSpinnerDrawList));
        expect(spinner.phase() == 0.5f
                        && !near(advancedSpinnerDrawList.commands().get(0).bounds().x(), firstDotX),
                "Running LoadingIndicator should advance phase and move spinner geometry");

        spinner.stop();
        float stoppedPhase = spinner.phase();
        spinner.tick(new FrameContext(2, 0.25f, 0.0f, FramePhase.ANIMATION));
        expect(spinner.phase() == stoppedPhase,
                "Stopped LoadingIndicator should keep its animation phase stable");

        LoadingIndicator dots = new LoadingIndicator()
                .mode(LoadingIndicator.Mode.DOTS)
                .phase(0.25f);
        dots.layout(style -> style.size(72.0f, 24.0f));
        dots.measure(new LayoutContext(100.0f, 100.0f));
        dots.arrange(new MutableRect(0.0f, 0.0f, 72.0f, 24.0f));
        DrawList dotsDrawList = new DrawList();
        dots.render(new DefaultRenderContext(dotsDrawList));
        expect(countCommands(dotsDrawList, DrawCommandType.CIRCLE) == 3,
                "LoadingIndicator dots mode should render three animated dots");

        LoadingIndicator bar = new LoadingIndicator()
                .mode(LoadingIndicator.Mode.BAR)
                .phase(0.25f);
        bar.measure(new LayoutContext(120.0f, 20.0f));
        bar.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 8.0f));
        DrawList barDrawList = new DrawList();
        bar.render(new DefaultRenderContext(barDrawList));
        expect(countCommands(barDrawList, DrawCommandType.ROUNDED_RECT) == 2,
                "LoadingIndicator bar mode should render a track and moving thumb");

        expect(Widgets.loadingIndicator() instanceof LoadingIndicator
                        && Widgets.spinner() instanceof Spinner,
                "Widgets factory should expose LoadingIndicator and Spinner");
    }

    private void testSplitPanelContracts() {
        Box first = solidTestBox();
        Box second = solidTestBox();
        SplitPanel split = new SplitPanel(first, second)
                .splitRatio(0.25f)
                .splitterThickness(10.0f)
                .minFirstSize(0.0f)
                .minSecondSize(0.0f);
        split.measure(new LayoutContext(200.0f, 80.0f));
        split.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 80.0f));
        expect(near(first.layoutBounds().width(), 47.5f)
                        && near(split.splitter().layoutBounds().x(), 47.5f)
                        && near(split.splitter().layoutBounds().width(), 10.0f)
                        && near(second.layoutBounds().x(), 57.5f)
                        && near(second.layoutBounds().width(), 142.5f),
                "Horizontal SplitPanel should arrange first pane, splitter and second pane from split ratio");
        expect(split.splitter().mouseCursorAt(1.0f, 1.0f) == MouseCursor.RESIZE_HORIZONTAL,
                "Horizontal Splitter should expose horizontal resize cursor");

        DefaultUIContext uiContext = new DefaultUIContext();
        split.setUiContextInternal(uiContext);
        uiContext.routedEvents().dispatch(new PointerPressedEvent(split.splitter(),
                52.5f, 20.0f, 5.0f, 20.0f, 0, PointerButton.PRIMARY));
        expect(split.dragging() && uiContext.capturedPointer(0) == split.splitter(),
                "Splitter press should start SplitPanel drag and capture pointer");
        uiContext.routedEvents().dispatch(new PointerMovedEvent(split.splitter(),
                71.5f, 20.0f, 24.0f, 20.0f, 0));
        expect(near(split.splitRatio(), 0.35f),
                "Dragging Splitter should update SplitPanel ratio based on pointer delta");
        split.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 80.0f));
        expect(near(first.layoutBounds().width(), 66.5f),
                "SplitPanel should apply dragged ratio on next arrange");
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(split.splitter(),
                71.5f, 20.0f, 24.0f, 20.0f, 0, PointerButton.PRIMARY));
        expect(!split.dragging() && uiContext.capturedPointer(0) == null,
                "Splitter release should end drag and release pointer capture");

        SplitPanel vertical = new SplitPanel(solidTestBox(), solidTestBox())
                .orientation(Orientation.VERTICAL)
                .splitRatio(0.50f)
                .splitterThickness(6.0f)
                .minFirstSize(0.0f)
                .minSecondSize(0.0f);
        vertical.measure(new LayoutContext(120.0f, 106.0f));
        vertical.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 106.0f));
        expect(near(vertical.first().layoutBounds().height(), 50.0f)
                        && near(vertical.splitter().layoutBounds().y(), 50.0f)
                        && near(vertical.second().layoutBounds().y(), 56.0f)
                        && vertical.splitter().mouseCursorAt(1.0f, 1.0f) == MouseCursor.RESIZE_VERTICAL,
                "Vertical SplitPanel should arrange panes top/bottom and expose vertical resize cursor");

        DrawList drawList = new DrawList();
        split.render(new DefaultRenderContext(drawList));
        expect(countCommands(drawList, DrawCommandType.ROUNDED_RECT) >= 2,
                "SplitPanel should render splitter chrome and handle");
        expect(Widgets.splitPanel() instanceof SplitPanel
                        && Widgets.splitPanel(new Label("A"), new Label("B")) instanceof SplitPanel,
                "Widgets factory should expose SplitPanel");
    }

    private void testBreadcrumbContracts() {
        Breadcrumb breadcrumb = new Breadcrumb()
                .items(java.util.List.of("Home", "Projects", "UniGUI"))
                .silentSelectedIndex(2);
        breadcrumb.measure(new LayoutContext(260.0f, 40.0f));
        breadcrumb.arrange(new MutableRect(0.0f, 0.0f, breadcrumb.desiredSize().width(), breadcrumb.desiredSize().height()));

        DrawList drawList = new DrawList();
        breadcrumb.render(new DefaultRenderContext(drawList));
        expect(breadcrumb.itemCount() == 3
                        && breadcrumb.selectedIndex() == 2
                        && breadcrumb.selectedItem().text().equals("UniGUI")
                        && breadcrumb.selectedPath().size() == 3,
                "Breadcrumb should expose items, selected item and selected path");
        expect(hasText(drawList, "Home")
                        && hasText(drawList, "Projects")
                        && hasText(drawList, "UniGUI")
                        && hasText(drawList, "\u203A"),
                "Breadcrumb should render segment buttons and separators");

        Counter changes = new Counter();
        breadcrumb.onSelectionChanged(event -> {
            changes.count++;
            changes.lastSelection = event.newSelection();
        });
        breadcrumb.itemButton(1).click();
        expect(breadcrumb.selectedIndex() == 1
                        && breadcrumb.selectedItem().text().equals("Projects")
                        && changes.count == 1
                        && changes.lastSelection.equals(java.util.List.of(1)),
                "Breadcrumb segment click should select that segment and emit SelectionChangedEvent");

        BreadcrumbItem disabled = new BreadcrumbItem("Disabled").enabled(false);
        breadcrumb.addItem(disabled);
        int beforeDisabledClick = breadcrumb.selectedIndex();
        breadcrumb.itemButton(3).click();
        expect(breadcrumb.selectedIndex() == beforeDisabledClick && changes.count == 1,
                "Breadcrumb disabled segments should not change selection");

        breadcrumb.separator("/");
        DrawList slashDrawList = new DrawList();
        breadcrumb.render(new DefaultRenderContext(slashDrawList));
        expect(hasText(slashDrawList, "/"),
                "Breadcrumb should allow custom separator text");

        breadcrumb.removeItem(3);
        breadcrumb.removeItem(2);
        expect(breadcrumb.selectedIndex() == 1 && breadcrumb.selectedItem().text().equals("Projects"),
                "Breadcrumb should keep selection valid when trailing items are removed");

        expect(Widgets.breadcrumb() instanceof Breadcrumb
                        && Widgets.breadcrumbItem("Factory") instanceof BreadcrumbItem,
                "Widgets factory should expose Breadcrumb and BreadcrumbItem");
    }

    private void testToggleCheckboxProgressAndNumberField() {
        DefaultUIContext uiContext = new DefaultUIContext();

        ToggleButton toggle = new ToggleButton("Power");
        toggle.setUiContextInternal(uiContext);
        toggle.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 20.0f));
        DrawList toggleDrawList = new DrawList();
        toggle.render(new DefaultRenderContext(toggleDrawList));
        int toggleTextIndex = textCommandIndex(toggleDrawList, "Power", 0);
        expect(toggleTextIndex < Integer.MAX_VALUE && near(toggleDrawList.commands().get(toggleTextIndex).bounds().y(), 5.0f),
                "Button text should be vertically centered in the control bounds");

        Button movedButton = new Button("Moved");
        movedButton.arrange(new MutableRect(0.0f, 0.0f, 72.0f, 20.0f));
        movedButton.transform().position().set(28.0f, 24.0f);
        DrawList movedButtonDrawList = new DrawList();
        movedButton.render(new DefaultRenderContext(movedButtonDrawList));
        int movedButtonClipIndex = firstCommandIndex(movedButtonDrawList, DrawCommandType.PUSH_CLIP, 0);
        int movedButtonTextIndex = textCommandIndex(movedButtonDrawList, "Moved", 0);
        expect(movedButtonClipIndex >= 0
                        && movedButtonTextIndex < Integer.MAX_VALUE
                        && movedButtonClipIndex < movedButtonTextIndex
                        && near(movedButtonDrawList.commands().get(movedButtonClipIndex).transform().position().x(), 28.0f)
                        && near(movedButtonDrawList.commands().get(movedButtonClipIndex).transform().position().y(), 24.0f)
                        && near(movedButtonDrawList.commands().get(movedButtonTextIndex).transform().position().x(), 28.0f)
                        && near(movedButtonDrawList.commands().get(movedButtonTextIndex).transform().position().y(), 24.0f),
                "Button text clip should follow widget transform on both axes");

        Counter checkedChanges = new Counter();
        toggle.onCheckedChanged(event -> {
            checkedChanges.count++;
            checkedChanges.lastChecked = event.newValue();
        });

        uiContext.routedEvents().dispatch(new PointerPressedEvent(toggle, 8.0f, 8.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(toggle, 8.0f, 8.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(toggle.checked(), "ToggleButton should toggle on click");
        expect(checkedChanges.count == 1 && checkedChanges.lastChecked, "ToggleButton should emit checked changed event");

        ToggleSwitch toggleSwitch = new ToggleSwitch("Online");
        toggleSwitch.setUiContextInternal(uiContext);
        toggleSwitch.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 20.0f));
        DrawList switchDrawList = new DrawList();
        toggleSwitch.render(new DefaultRenderContext(switchDrawList));
        expect(countCommands(switchDrawList, DrawCommandType.ROUNDED_RECT) == 1
                        && countCommands(switchDrawList, DrawCommandType.CIRCLE) == 1
                        && hasText(switchDrawList, "Online"),
                "ToggleSwitch should render a rounded track, thumb and optional label");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(toggleSwitch, 8.0f, 8.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(toggleSwitch, 8.0f, 8.0f, 8.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(toggleSwitch.checked(), "ToggleSwitch should reuse ToggleButton checked behavior");
        toggleSwitch.tick(new FrameContext(1L, 0.08f, 0.0f, FramePhase.ANIMATION));
        expect(toggleSwitch.switchProgress() > 0.0f && toggleSwitch.switchProgress() < 1.0f,
                "ToggleSwitch should animate switch progress after toggling");
        toggleSwitch.tick(new FrameContext(2L, 0.20f, 0.0f, FramePhase.ANIMATION));
        expect(near(toggleSwitch.switchProgress(), 1.0f), "ToggleSwitch animation should settle at checked progress");

        ToggleSwitch leftLabelSwitch = new ToggleSwitch("Left").labelLeft(true);
        leftLabelSwitch.setUiContextInternal(uiContext);
        leftLabelSwitch.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 20.0f));
        DrawList leftLabelDrawList = new DrawList();
        leftLabelSwitch.render(new DefaultRenderContext(leftLabelDrawList));
        int leftLabelTextIndex = textCommandIndex(leftLabelDrawList, "Left", 0);
        int leftLabelTrackIndex = firstCommandIndex(leftLabelDrawList, DrawCommandType.ROUNDED_RECT, 0);
        expect(leftLabelTextIndex >= 0
                        && leftLabelTrackIndex >= 0
                        && leftLabelDrawList.commands().get(leftLabelTextIndex).bounds().x()
                        < leftLabelDrawList.commands().get(leftLabelTrackIndex).bounds().x(),
                "ToggleSwitch labelLeft should render text before the switch track");
        expect(Widgets.toggleSwitch("Factory") instanceof ToggleSwitch, "Widgets.toggleSwitch should create ToggleSwitch instances");

        Checkbox checkbox = new Checkbox("Enabled");
        checkbox.setUiContextInternal(uiContext);
        checkbox.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        DrawList checkboxDrawList = new DrawList();
        checkbox.render(new DefaultRenderContext(checkboxDrawList));
        int checkboxTextIndex = textCommandIndex(checkboxDrawList, "Enabled", 0);
        expect(checkboxTextIndex < Integer.MAX_VALUE && near(checkboxDrawList.commands().get(checkboxTextIndex).bounds().y(), 6.0f),
                "Checkbox label should be vertically centered against the check mark");

        Checkbox leftLabelCheckbox = new Checkbox("Left").labelLeft(true);
        leftLabelCheckbox.setUiContextInternal(uiContext);
        leftLabelCheckbox.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 20.0f));
        DrawList leftLabelCheckboxDrawList = new DrawList();
        leftLabelCheckbox.render(new DefaultRenderContext(leftLabelCheckboxDrawList));
        int leftLabelCheckboxTextIndex = textCommandIndex(leftLabelCheckboxDrawList, "Left", 0);
        int leftLabelCheckboxBoxIndex = firstCommandIndex(leftLabelCheckboxDrawList, DrawCommandType.ROUNDED_RECT, 0);
        expect(leftLabelCheckboxTextIndex < Integer.MAX_VALUE
                        && leftLabelCheckboxBoxIndex < Integer.MAX_VALUE
                        && leftLabelCheckboxDrawList.commands().get(leftLabelCheckboxTextIndex).bounds().x()
                        < leftLabelCheckboxDrawList.commands().get(leftLabelCheckboxBoxIndex).bounds().x(),
                "Checkbox labelLeft should render text before the checkbox box");

        uiContext.routedEvents().dispatch(new PointerPressedEvent(checkbox, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(checkbox, 6.0f, 8.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(checkbox.checked(), "Checkbox should reuse ToggleButton checked behavior");
        checkbox.tick(new FrameContext(3L, 0.06f, 0.0f, FramePhase.ANIMATION));
        expect(checkbox.checkProgress() > 0.0f && checkbox.checkProgress() < 1.0f,
                "Checkbox should animate check progress after checking");
        checkbox.tick(new FrameContext(4L, 0.20f, 0.0f, FramePhase.ANIMATION));
        expect(near(checkbox.checkProgress(), 1.0f), "Checkbox check animation should settle at checked progress");
        checkbox.silentChecked(false);
        checkbox.tick(new FrameContext(5L, 0.06f, 0.0f, FramePhase.ANIMATION));
        expect(checkbox.checkProgress() > 0.0f && checkbox.checkProgress() < 1.0f,
                "Checkbox should animate check progress backward after unchecking");
        checkbox.tick(new FrameContext(6L, 0.20f, 0.0f, FramePhase.ANIMATION));
        expect(near(checkbox.checkProgress(), 0.0f), "Checkbox check animation should settle at unchecked progress");

        Checkbox triStateCheckbox = new Checkbox("Partial").triState(true);
        triStateCheckbox.setUiContextInternal(uiContext);
        triStateCheckbox.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 20.0f));
        Counter checkboxStateChanges = new Counter();
        java.util.concurrent.atomic.AtomicReference<CheckboxState> lastCheckboxState =
                new java.util.concurrent.atomic.AtomicReference<>(CheckboxState.UNCHECKED);
        triStateCheckbox.onStateChanged(event -> {
            checkboxStateChanges.count++;
            lastCheckboxState.set(event.newState());
        });
        triStateCheckbox.click();
        expect(triStateCheckbox.state() == CheckboxState.CHECKED && triStateCheckbox.checked(),
                "Tri-state Checkbox first click should move unchecked -> checked");
        triStateCheckbox.click();
        expect(triStateCheckbox.state() == CheckboxState.INDETERMINATE
                        && triStateCheckbox.indeterminate()
                        && !triStateCheckbox.checked(),
                "Tri-state Checkbox second click should move checked -> indeterminate");
        DrawList indeterminateDrawList = new DrawList();
        triStateCheckbox.render(new DefaultRenderContext(indeterminateDrawList));
        expect(countCommands(indeterminateDrawList, DrawCommandType.RECT) >= 1,
                "Indeterminate Checkbox should render a dash through checkbox renderer state");
        triStateCheckbox.click();
        expect(triStateCheckbox.state() == CheckboxState.UNCHECKED
                        && checkboxStateChanges.count == 3
                        && lastCheckboxState.get() == CheckboxState.UNCHECKED,
                "Tri-state Checkbox third click should move indeterminate -> unchecked and emit state events");

        RadioButton compact = new RadioButton("Compact", "compact");
        RadioButton detailed = new RadioButton("Detailed", "detailed");
        RadioGroup radioGroup = new RadioGroup()
                .add(compact)
                .add(detailed)
                .silentSelectedValue("compact");
        compact.setUiContextInternal(uiContext);
        detailed.setUiContextInternal(uiContext);
        compact.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 20.0f));
        detailed.arrange(new MutableRect(0.0f, 24.0f, 90.0f, 20.0f));
        expect(compact.checked() && !detailed.checked() && radioGroup.selectedButton() == compact,
                "RadioGroup should keep exactly one selected button");
        compact.tick(new FrameContext(7L, 0.20f, 0.0f, FramePhase.ANIMATION));
        DrawList radioDrawList = new DrawList();
        compact.render(new DefaultRenderContext(radioDrawList));
        int radioTextIndex = textCommandIndex(radioDrawList, "Compact", 0);
        expect(radioTextIndex < Integer.MAX_VALUE
                        && near(radioDrawList.commands().get(radioTextIndex).bounds().y(), 6.0f)
                        && countCommands(radioDrawList, DrawCommandType.CIRCLE) == 2,
                "Selected RadioButton should render its label, ring and inner dot");

        RadioButton leftLabelRadio = new RadioButton("Left", "left").labelLeft(true);
        leftLabelRadio.setUiContextInternal(uiContext);
        leftLabelRadio.arrange(new MutableRect(0.0f, 0.0f, 90.0f, 20.0f));
        DrawList leftLabelRadioDrawList = new DrawList();
        leftLabelRadio.render(new DefaultRenderContext(leftLabelRadioDrawList));
        int leftLabelRadioTextIndex = textCommandIndex(leftLabelRadioDrawList, "Left", 0);
        int leftLabelRadioCircleIndex = firstCommandIndex(leftLabelRadioDrawList, DrawCommandType.CIRCLE, 0);
        expect(leftLabelRadioTextIndex < Integer.MAX_VALUE
                        && leftLabelRadioCircleIndex < Integer.MAX_VALUE
                        && leftLabelRadioDrawList.commands().get(leftLabelRadioTextIndex).bounds().x()
                        < leftLabelRadioDrawList.commands().get(leftLabelRadioCircleIndex).bounds().x(),
                "RadioButton labelLeft should render text before the radio circle");

        Counter radioChanges = new Counter();
        detailed.onCheckedChanged(event -> {
            radioChanges.count++;
            radioChanges.lastChecked = event.newValue();
        });
        uiContext.routedEvents().dispatch(new PointerPressedEvent(detailed, 6.0f, 32.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(detailed, 6.0f, 32.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(!compact.checked() && detailed.checked() && radioGroup.selectedValue().equals("detailed"),
                "RadioButton click should select its group value and clear the previous option");
        compact.tick(new FrameContext(8L, 0.06f, 0.0f, FramePhase.ANIMATION));
        detailed.tick(new FrameContext(8L, 0.06f, 0.0f, FramePhase.ANIMATION));
        expect(compact.selectionProgress() > 0.0f && compact.selectionProgress() < 1.0f,
                "RadioButton should animate selection progress backward when cleared by its group");
        expect(detailed.selectionProgress() > 0.0f && detailed.selectionProgress() < 1.0f,
                "RadioButton should animate selection progress after selecting");
        compact.tick(new FrameContext(9L, 0.20f, 0.0f, FramePhase.ANIMATION));
        detailed.tick(new FrameContext(9L, 0.20f, 0.0f, FramePhase.ANIMATION));
        expect(near(compact.selectionProgress(), 0.0f) && near(detailed.selectionProgress(), 1.0f),
                "RadioButton selection animation should settle after group selection changes");
        expect(radioChanges.count == 1 && radioChanges.lastChecked,
                "RadioButton should emit checked changed only when its state changes");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(detailed, 6.0f, 32.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new PointerReleasedEvent(detailed, 6.0f, 32.0f, 6.0f, 8.0f, 0, PointerButton.PRIMARY));
        expect(detailed.checked() && radioChanges.count == 1,
                "Clicking an already selected RadioButton should keep it selected without a duplicate change event");
        uiContext.focusManager().requestFocus(compact);
        uiContext.routedEvents().dispatch(new KeyPressedEvent(compact, KeyCodes.SPACE, 0, 0));
        expect(compact.checked() && !detailed.checked() && radioGroup.selectedValue().equals("compact"),
                "Focused RadioButton should support keyboard selection");

        ProgressBar progressBar = new ProgressBar().range(0.0f, 200.0f).value(250.0f);
        progressBar.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 12.0f));
        expect(progressBar.value() == 200.0f && progressBar.progress() == 1.0f, "ProgressBar should clamp value to max");
        progressBar.value(50.0f);
        expect(progressBar.progress() == 0.25f, "ProgressBar should expose normalized progress");
        DrawList progressDrawList = new DrawList();
        progressBar.render(new DefaultRenderContext(progressDrawList));
        expect(progressDrawList.size() >= 2, "ProgressBar should render track and fill commands");

        ProgressBar indeterminateProgress = new ProgressBar().indeterminate(true);
        indeterminateProgress.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 12.0f));
        indeterminateProgress.tick(new FrameContext(1L, 0.5f, 0.0f, FramePhase.ANIMATION));
        DrawList indeterminateProgressDrawList = new DrawList();
        indeterminateProgress.render(new DefaultRenderContext(indeterminateProgressDrawList));
        expect(indeterminateProgress.indeterminate()
                        && indeterminateProgress.indeterminateOffset() > 0.0f
                        && indeterminateProgressDrawList.size() >= 2,
                "Indeterminate ProgressBar should animate and render a moving fill segment");

        ProgressBar lowProgressBar = new ProgressBar().range(0.0f, 100.0f);
        lowProgressBar.arrange(new MutableRect(0.0f, 0.0f, 100.0f, 12.0f));
        for (int value = 1; value <= 13; value++) {
            lowProgressBar.value(value);
            DrawList lowProgressDrawList = new DrawList();
            lowProgressBar.render(new DefaultRenderContext(lowProgressDrawList));
            expect(lowProgressDrawList.size() >= 2, "ProgressBar should render small non-zero fill values");
            var fill = lowProgressDrawList.commands().get(1);
            expect(fill.type() == DrawCommandType.RECT, "ProgressBar fill should use stable rectangular geometry");
            expect(near(fill.bounds().x(), 0.0f), "ProgressBar fill should stay anchored to the track origin");
            expect(near(fill.bounds().width(), value), "ProgressBar fill width should match normalized low percent values");
        }

        NumberField numberField = new NumberField().range(0.0d, 10.0d).step(2.0d);
        numberField.setUiContextInternal(uiContext);
        numberField.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 18.0f));
        Counter numberChanges = new Counter();
        numberField.onValueChanged(event -> {
            numberChanges.count++;
            numberChanges.lastNumber = event.newValue();
        });
        numberField.text("");
        uiContext.routedEvents().dispatch(new PointerPressedEvent(numberField, 4.0f, 8.0f, 4.0f, 8.0f, 0, PointerButton.PRIMARY));
        uiContext.routedEvents().dispatch(new TextInputEvent(numberField, '4', 0));
        uiContext.routedEvents().dispatch(new TextInputEvent(numberField, 'x', 0));
        expect(numberField.text().equals("4"), "NumberField should reject non-numeric text input");
        expect(numberField.value() == 4.0d, "NumberField should sync value from text");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(numberField, KeyCodes.UP, 0, 0));
        expect(numberField.value() == 6.0d, "NumberField should nudge up by step");
        numberField.text("");
        uiContext.clipboard().setText("12x.3");
        uiContext.routedEvents().dispatch(new KeyPressedEvent(numberField, KeyCodes.V, 0, KeyModifiers.CONTROL));
        expect(numberField.text().equals("10"), "NumberField should sanitize pasted text and clamp synced value");
        expect(numberField.value() == 10.0d, "NumberField should sync sanitized pasted numeric value");
        numberField.value(99.0d);
        expect(numberField.value() == 10.0d, "NumberField should clamp programmatic value to max");
        expect(numberChanges.count >= 2 && numberChanges.lastNumber == 10.0d, "NumberField should emit value changed events");
    }

    private static int firstCommandIndex(DrawList drawList, DrawCommandType type, int startIndex) {
        for (int i = Math.max(0, startIndex); i < drawList.commands().size(); i++) {
            if (drawList.commands().get(i).type() == type) {
                return i;
            }
        }
        return -1;
    }

    private static int lastCommandIndex(DrawList drawList, DrawCommandType type) {
        for (int i = drawList.commands().size() - 1; i >= 0; i--) {
            if (drawList.commands().get(i).type() == type) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasCommand(DrawList drawList, DrawCommandType type) {
        return drawList.commands().stream().anyMatch(command -> command.type() == type);
    }

    private static long countCommands(DrawList drawList, DrawCommandType type) {
        return drawList.commands().stream().filter(command -> command.type() == type).count();
    }

    private static PanelWidget overflowPanel(Overflow overflow) {
        PanelWidget panel = new PanelWidget();
        panel.layout(style -> style.overflow(overflow));
        panel.addChild(solidTestBox());
        panel.applyQueuedMutations();
        panel.arrange(new MutableRect(0.0f, 0.0f, 80.0f, 40.0f));
        return panel;
    }

    private static Box solidTestBox() {
        Box box = new Box();
        box.themeEnabled(false);
        box.backgroundVisible(true);
        box.background().set(0.25f, 0.50f, 0.75f, 1.0f);
        return box;
    }

    private static boolean hasText(DrawList drawList, String text) {
        return drawList.commands().stream().anyMatch(command -> text.equals(command.text()));
    }

    private static long countTextCommands(DrawList drawList) {
        return drawList.commands().stream().filter(command -> command.type() == DrawCommandType.TEXT).count();
    }

    private static int propertyRowIndex(PropertyGrid grid, String name) {
        for (int i = 0; i < grid.rows().size(); i++) {
            if (grid.rows().get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static int textCommandIndex(DrawList drawList, String text, int startIndex) {
        for (int i = Math.max(0, startIndex); i < drawList.commands().size(); i++) {
            if (text.equals(drawList.commands().get(i).text())) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private static boolean hasFillColor(DrawList drawList, float r, float g, float b, float a) {
        return drawList.commands().stream()
                .filter(command -> command.paint() != null && !command.paint().isStroke())
                .map(command -> command.paint().color())
                .anyMatch(color -> near(color.r(), r) && near(color.g(), g) && near(color.b(), b) && near(color.a(), a));
    }

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.01f;
    }

    private static boolean hasFlag(int flags, int flag) {
        return (flags & flag) == flag;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FixedTextMetricsBackend implements RenderBackend {
        private final float charWidth;
        private final FontFace defaultFace;

        private FixedTextMetricsBackend(float charWidth) {
            this.charWidth = charWidth;
            this.defaultFace = null;
        }

        private FixedTextMetricsBackend(FontFace defaultFace) {
            this.charWidth = 0.0f;
            this.defaultFace = defaultFace;
        }

        @Override
        public float measureTextWidth(String text) {
            return text == null ? 0.0f : text.length() * effectiveCharWidth();
        }

        @Override
        public FontFace defaultTextFace() {
            return defaultFace == null ? RenderBackend.super.defaultTextFace() : defaultFace;
        }

        private float effectiveCharWidth() {
            return defaultFace == null ? charWidth : defaultFace.advance('A', TextRun.DEFAULT_PIXEL_SIZE);
        }

        @Override
        public void beginFrame(FrameContext frame) {
        }

        @Override
        public void render(DrawList drawList, RenderTarget target) {
        }

        @Override
        public void endFrame() {
        }
    }

    private record FixedFontFace(String id, float advance, float lineHeight) implements FontFace {
        @Override
        public FontMetrics metrics(float pixelSize) {
            return new FontMetrics(lineHeight * 0.75f, lineHeight * 0.25f, 0.0f, lineHeight);
        }

        @Override
        public float advance(int codePoint, float pixelSize) {
            return advance;
        }
    }

    private static final class Counter {
        private int count;
        private String lastText = "";
        private String lastOldText = "";
        private float lastValue;
        private boolean lastChecked;
        private double lastNumber;
        private java.util.List<Integer> lastSelection = java.util.List.of();
        private int lastSortColumn = -1;
        private SortDirection lastSortDirection = SortDirection.NONE;
        private int started;
        private int committed;
        private int cancelled;
        private int lastRow = -1;
        private int lastColumn = -1;
        private int resized;
        private int moved;
        private int lastOldColumn = -1;
        private int lastNewColumn = -1;
        private float lastOldWidth;
        private float lastNewWidth;
        private float lastNewHeight;
    }

    private static final class TestMinecraftPreviewWidget extends MinecraftPreviewWidget {
        private final String fallback;

        private TestMinecraftPreviewWidget(String label, String fallback) {
            super(label);
            this.fallback = fallback;
        }

        @Override
        protected void renderMinecraftPreview(MinecraftGuiRenderBackend backend, float x, float y, float size, float opacity) {
        }

        @Override
        protected String fallbackText() {
            return fallback;
        }
    }
}
