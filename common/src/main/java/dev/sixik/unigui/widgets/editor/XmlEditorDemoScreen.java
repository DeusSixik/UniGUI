package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.editor.CommandManager;
import dev.sixik.unigui.api.editor.EditorCommand;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlCommandRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetAssetCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetAssetKind;
import dev.sixik.unigui.api.xml.XmlWidgetAssetProviders;
import dev.sixik.unigui.api.xml.XmlWidgetDiagnosticsPanel;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdits;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNode;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetSerializationOptions;
import dev.sixik.unigui.api.xml.editor.XmlEditorDocumentSources;
import dev.sixik.unigui.api.xml.editor.XmlEditorMode;
import dev.sixik.unigui.api.xml.editor.XmlEditorSession;
import dev.sixik.unigui.api.xml.editor.XmlEditorSessionChange;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshotCodec;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.ToolButton;
import dev.sixik.unigui.widgets.interaction.XmlCodeEditor;
import dev.sixik.unigui.widgets.navigation.Menu;
import dev.sixik.unigui.widgets.navigation.MenuBar;
import dev.sixik.unigui.widgets.navigation.ToolBar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Dev-only XML editor shell that wires the Phase 1 session into a docked workspace. */
@XmlWidgetName("XmlEditorDemoScreen")
public final class XmlEditorDemoScreen extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.XML_EDITOR_DEMO_SCREEN;

    public static final String PANE_DESIGN = "design";
    public static final String PANE_RUNTIME = "runtime";
    public static final String PANE_CODE = "code";
    public static final String PANE_HIERARCHY = "hierarchy";
    public static final String PANE_PROPERTIES = "properties";
    public static final String PANE_DIAGNOSTICS = "diagnostics";
    public static final String PANE_PALETTE = "palette";
    public static final String PANE_ASSETS = "assets";
    public static final String PANE_BINDINGS = "bindings";
    public static final String PANE_CONSOLE = "console";

    public static final String COMMAND_SAVE_PROJECT_AS = "save_project_as";
    public static final String COMMAND_REVERT = "xml.revert";
    public static final String COMMAND_FORMAT_XML = "xml.format";
    public static final String COMMAND_UNDO = "edit.undo";
    public static final String COMMAND_REDO = "edit.redo";
    public static final String COMMAND_DELETE = "edit.delete";
    public static final String COMMAND_DUPLICATE = "edit.duplicate";
    public static final String COMMAND_DESIGN_MODE = "run.design";
    public static final String COMMAND_RUN = "run.runtime";
    public static final String COMMAND_STOP = "run.stop";
    public static final String COMMAND_RELOAD = "run.reload";
    public static final String COMMAND_LAYOUT_SAVE = "layout.save";
    public static final String COMMAND_LAYOUT_RESTORE = "layout.restore";
    public static final String COMMAND_DIAGNOSTICS_REFRESH = "diagnostics.refresh";

    private static final XmlWidgetSerializationOptions EDITOR_XML =
            XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false);

    private final XmlEditorSession session;
    private final CommandManager commands = new CommandManager();
    private final DockingRoot workspace = new DockingRoot();
    private final PaneVisibilityController paneVisibility;
    private final Map<String, DockPane> paneRegistry = new LinkedHashMap<>();
    private final XmlCodeEditor codeEditor = new XmlCodeEditor();
    private final XmlPropertiesPanel propertiesPanel = new XmlPropertiesPanel();
    private final XmlWidgetDiagnosticsPanel diagnosticsPanel = new XmlWidgetDiagnosticsPanel();
    private final StatusBar statusBar = new StatusBar();
    private final XmlHierarchyPanel hierarchyPanel = new XmlHierarchyPanel();
    private final PalettePanel palettePanel = new PalettePanel();
    private final XmlDesignCanvas designCanvas = new XmlDesignCanvas();
    private final XmlRuntimeViewPane runtimeView = new XmlRuntimeViewPane();
    private XmlWidgetAssetCatalog assetCatalog = buildAssetCatalog();
    private final AssetBrowserPanel assetBrowserPanel = new AssetBrowserPanel();
    private final Label statusLine = new Label("XML editor ready.");
    private String savedLayoutSnapshot = "";
    private boolean syncingCodeEditor;

    public XmlEditorDemoScreen() {
        this(XmlEditorSession.open(XmlEditorDocumentSources.memory(
                "demo:xml-editor",
                "XML Editor Demo",
                defaultXml())));
    }

    public XmlEditorDemoScreen(XmlEditorSession session) {
        super(Orientation.VERTICAL);
        this.session = Objects.requireNonNull(session, "session");
        spacing(5.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        if (this.session.selectedPath().isEmpty()) {
            this.session.selectRoot();
        }

        configureAssetBrowser();
        configureRuntimeCommands();
        registerSessionCommands();
        paneVisibility = new PaneVisibilityController(workspace)
                .bindViewCommands(commands)
                .diagnosticsPaneId(PANE_DIAGNOSTICS)
                .autoOpenDiagnosticsOnErrors(true);
        buildWorkspace();

        MenuBar menuBar = buildMenuBar();
        ToolBar toolBar = buildToolBar();
        menuBar.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0.0f).flexShrink(0.0f));
        toolBar.layout(style -> style.size(LayoutConstraints.AUTO, 26.0f).flexGrow(0.0f).flexShrink(0.0f));
        workspace.layout(style -> style.size(LayoutConstraints.AUTO, 260.0f).flexGrow(1.0f).flexShrink(1.0f));
        statusBar.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0.0f).flexShrink(0.0f));
        statusLine.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0.0f).flexShrink(0.0f));

        addChild(menuBar);
        addChild(toolBar);
        addChild(workspace);
        addChild(statusBar);
        addChild(statusLine);
        applyQueuedMutations();

        designCanvas.session(this.session);
        runtimeView.session(this.session);
        codeEditor.onTextChanged(event -> {
            if (syncingCodeEditor) return;
            this.session.mode(XmlEditorMode.CODE);
            this.session.replaceText(event.newText());
        });
        propertiesPanel.session(this.session);
        hierarchyPanel.onSelectionChanged(change -> this.session.select(change.path()));
        hierarchyPanel.onAddChildRequested(request -> this.session.insertChild(
                request.parentPath(),
                request.index(),
                request.element()));
        hierarchyPanel.onDeleteRequested(action -> this.session.applyEdit(XmlWidgetDocumentEdits.removeChild(action.path())));
        hierarchyPanel.onMoveRequested(request -> this.session.applyEdit(XmlWidgetDocumentEdits.moveChild(
                request.parentPath(),
                request.fromIndex(),
                request.toIndex())));
        hierarchyPanel.onReparentRequested(request -> this.session.applyEdit(XmlWidgetDocumentEdits.moveChild(
                request.sourcePath(),
                request.targetParentPath(),
                request.targetIndex())));
        hierarchyPanel.bindPalette(palettePanel);
        this.session.onChanged(change -> {
            refreshFromSession(change);
            if (affectsDiagnostics(change.kind())) {
                paneVisibility.updateDiagnostics(this.session.diagnosticsModel());
            }
        });
        refreshFromSession();
    }

    public XmlEditorSession session() {
        return session;
    }

    public CommandManager commands() {
        return commands;
    }

    public DockingRoot workspace() {
        return workspace;
    }

    public PaneVisibilityController paneVisibility() {
        return paneVisibility;
    }

    public XmlCodeEditor codeEditor() {
        return codeEditor;
    }

    public PropertyGrid propertyGrid() {
        return propertiesPanel.grid();
    }

    public XmlPropertiesPanel propertiesPanel() {
        return propertiesPanel;
    }

    public XmlHierarchyPanel hierarchyPanel() {
        return hierarchyPanel;
    }

    public WidgetPalette palettePanel() {
        return palettePanel;
    }

    public AssetBrowserPanel assetBrowserPanel() {
        return assetBrowserPanel;
    }

    public XmlWidgetAssetCatalog assetCatalog() {
        return assetCatalog;
    }

    public XmlEditorDemoScreen refreshAssetCatalog() {
        assetCatalog = buildAssetCatalog();
        syncAssetCatalog();
        return this;
    }

    public XmlWidgetDiagnosticsPanel diagnosticsPanel() {
        return diagnosticsPanel;
    }

    public StatusBar statusBar() {
        return statusBar;
    }

    public XmlDesignCanvas designCanvas() {
        return designCanvas;
    }

    public XmlRuntimeViewPane runtimeView() {
        return runtimeView;
    }

    public String encodedLayout() {
        return DockLayoutSnapshotCodec.encode(workspace.manager().snapshot());
    }

    public boolean restoreEncodedLayout(String encoded) {
        workspace.restoreLayout(DockLayoutSnapshotCodec.decode(encoded), paneRegistry);
        return true;
    }

    private void registerSessionCommands() {
        commands.register(new EditorCommand(ProjectPickerPanel.COMMAND_NEW_PROJECT, "New Project")
                .action(this::newProject));
        commands.register(new EditorCommand(ProjectPickerPanel.COMMAND_OPEN_PROJECT, "Open Project")
                .action(this::openDemoProject));
        commands.register(new EditorCommand(ProjectPickerPanel.COMMAND_SAVE_PROJECT, "Save Project")
                .enabledWhen(() -> session.dirty() || session.source().map(source -> source.writable()).orElse(false))
                .action(this::saveProject));
        commands.register(new EditorCommand(COMMAND_SAVE_PROJECT_AS, "Save Project As")
                .action(this::saveProjectAs));
        commands.register(new EditorCommand(ProjectPickerPanel.COMMAND_LAST_PROJECTS, "Recent Projects")
                .action(() -> statusLine.text("Recent projects menu requested.")));

        commands.register(new EditorCommand(COMMAND_REVERT, "Revert")
                .enabledWhen(session::dirty)
                .action(() -> {
                    if (session.revert()) statusLine.text("Reverted XML source.");
                }));
        commands.register(new EditorCommand(COMMAND_FORMAT_XML, "Format XML")
                .action(() -> {
                    boolean formatted = session.formatXml();
                    statusLine.text(formatted ? "Formatted XML source." : "Cannot format invalid XML.");
                }));
        commands.register(new EditorCommand(COMMAND_UNDO, "Undo")
                .enabledWhen(session::canUndo)
                .action(session::undo));
        commands.register(new EditorCommand(COMMAND_REDO, "Redo")
                .enabledWhen(session::canRedo)
                .action(session::redo));
        commands.register(new EditorCommand(COMMAND_DELETE, "Delete")
                .enabledWhen(() -> session.selectedPath().filter(path -> !path.rootPath()).isPresent())
                .action(this::deleteSelected));
        commands.register(new EditorCommand(COMMAND_DUPLICATE, "Duplicate")
                .enabledWhen(() -> session.selectedPath().filter(path -> !path.rootPath()).isPresent())
                .action(this::duplicateSelected));

        commands.register(new EditorCommand(COMMAND_DESIGN_MODE, "Design")
                .checkedWhen(() -> session.mode() == XmlEditorMode.DESIGN)
                .action(() -> {
                    runtimeView.stop();
                    paneVisibility.hidePane(PANE_RUNTIME);
                    paneVisibility.showPane(PANE_DESIGN);
                    workspace.selectPane(PANE_DESIGN);
                }));
        commands.register(new EditorCommand(COMMAND_RUN, "Run")
                .checkedWhen(() -> session.mode() == XmlEditorMode.RUNTIME)
                .action(() -> {
                    paneVisibility.showPane(PANE_RUNTIME);
                    runtimeView.start();
                    workspace.selectPane(PANE_RUNTIME);
                    statusLine.text(runtimeView.running()
                            ? "Runtime View started from current XML snapshot."
                            : "Cannot start Runtime View: " + runtimeView.runtimeError());
                }));
        commands.register(new EditorCommand(COMMAND_STOP, "Stop")
                .enabledWhen(() -> session.mode() == XmlEditorMode.RUNTIME)
                .action(() -> {
                    runtimeView.stop();
                    paneVisibility.hidePane(PANE_RUNTIME);
                    paneVisibility.showPane(PANE_DESIGN);
                    workspace.selectPane(PANE_DESIGN);
                    statusLine.text("Runtime View stopped.");
                }));
        commands.register(new EditorCommand(COMMAND_RELOAD, "Reload")
                .action(() -> {
                    if (session.mode() == XmlEditorMode.RUNTIME) {
                        boolean reloaded = runtimeView.reload();
                        statusLine.text(reloaded ? "Reloaded Runtime View from current XML snapshot."
                                : "Cannot reload Runtime View: " + runtimeView.runtimeError());
                    } else {
                        session.replaceText(session.text());
                        statusLine.text("Reloaded XML preview from current text.");
                    }
                }));

        commands.register(new EditorCommand(COMMAND_LAYOUT_SAVE, "Save Layout")
                .action(() -> {
                    savedLayoutSnapshot = encodedLayout();
                    commands.command(COMMAND_LAYOUT_RESTORE).ifPresent(EditorCommand::notifyChanged);
                    statusLine.text("Dock layout snapshot saved.");
                }));
        commands.register(new EditorCommand(COMMAND_LAYOUT_RESTORE, "Restore Layout")
                .enabledWhen(() -> !savedLayoutSnapshot.isEmpty())
                .action(() -> {
                    restoreEncodedLayout(savedLayoutSnapshot);
                    statusLine.text("Dock layout snapshot restored.");
                }));
        commands.register(new EditorCommand(COMMAND_DIAGNOSTICS_REFRESH, "Refresh Diagnostics")
                .action(() -> {
                    refreshFromSession();
                    paneVisibility.updateDiagnostics(session.diagnosticsModel());
                }));
    }

    private void buildWorkspace() {
        registerPane(DockPane.document(PANE_DESIGN, "Design", designPane()), DockArea.CENTER, true);
        registerPane(DockPane.document(PANE_RUNTIME, "Runtime", runtimeView), DockArea.CENTER, false);
        registerPane(DockPane.document(PANE_CODE, "Code", codeEditor), DockArea.CENTER, true);
        registerPane(DockPane.tool(PANE_HIERARCHY, "Hierarchy", hierarchyPanel), DockArea.LEFT, true);
        registerPane(DockPane.tool(PANE_PROPERTIES, "Properties", propertiesPanel), DockArea.RIGHT, true);
        registerPane(DockPane.tool(PANE_DIAGNOSTICS, "Diagnostics", diagnosticsPanel), DockArea.BOTTOM, false);
        registerPane(DockPane.tool(PANE_PALETTE, "Palette", palettePane()), DockArea.LEFT, false);
        registerPane(DockPane.tool(PANE_ASSETS, "Assets", assetsPane()), DockArea.LEFT, false);
        registerPane(DockPane.tool(PANE_BINDINGS, "Bindings", placeholderPane("Bindings", "Binding context and script diagnostics will appear here.")), DockArea.RIGHT, false);
        registerPane(DockPane.tool(PANE_CONSOLE, "Console", consolePane()), DockArea.BOTTOM, false);
        workspace.selectPane(PANE_DESIGN);
    }

    private void registerPane(DockPane pane, DockArea area, boolean visible) {
        paneRegistry.put(pane.id(), pane);
        paneVisibility.registerPane(pane, area, visible);
    }

    private MenuBar buildMenuBar() {
        return new MenuBar().commandManager(commands)
                .menu(new Menu("File")
                        .command(ProjectPickerPanel.COMMAND_NEW_PROJECT)
                        .command(ProjectPickerPanel.COMMAND_OPEN_PROJECT)
                        .separator()
                        .command(ProjectPickerPanel.COMMAND_SAVE_PROJECT)
                        .command(COMMAND_SAVE_PROJECT_AS)
                        .command(COMMAND_REVERT))
                .menu(new Menu("Edit")
                        .command(COMMAND_UNDO)
                        .command(COMMAND_REDO)
                        .separator()
                        .command(COMMAND_DELETE)
                        .command(COMMAND_DUPLICATE)
                        .command(COMMAND_FORMAT_XML))
                .menu(new Menu("View")
                        .command(paneVisibility.viewCommandId(PANE_HIERARCHY), "Hierarchy")
                        .command(paneVisibility.viewCommandId(PANE_PROPERTIES), "Properties")
                        .command(paneVisibility.viewCommandId(PANE_RUNTIME), "Runtime View")
                        .command(paneVisibility.viewCommandId(PANE_CODE), "Code View")
                        .command(paneVisibility.viewCommandId(PANE_PALETTE), "Palette")
                        .command(paneVisibility.viewCommandId(PANE_ASSETS), "Assets")
                        .command(paneVisibility.viewCommandId(PANE_BINDINGS), "Bindings")
                        .command(paneVisibility.viewCommandId(PANE_DIAGNOSTICS), "Diagnostics")
                        .command(paneVisibility.viewCommandId(PANE_CONSOLE), "Console"))
                .menu(new Menu("Project")
                        .command(ProjectPickerPanel.COMMAND_LAST_PROJECTS)
                        .command(ProjectPickerPanel.COMMAND_SAVE_PROJECT)
                        .command(COMMAND_SAVE_PROJECT_AS))
                .menu(new Menu("Run")
                        .command(COMMAND_DESIGN_MODE)
                        .command(COMMAND_RUN)
                        .command(COMMAND_STOP)
                        .command(COMMAND_RELOAD))
                .menu(new Menu("Layout")
                        .command(COMMAND_LAYOUT_SAVE)
                        .command(COMMAND_LAYOUT_RESTORE))
                .menu(new Menu("Diagnostics")
                        .command(paneVisibility.viewCommandId(PANE_DIAGNOSTICS), "Show Diagnostics")
                        .command(COMMAND_DIAGNOSTICS_REFRESH));
    }

    private ToolBar buildToolBar() {
        ToolBar toolBar = new ToolBar().commandManager(commands);
        toolBar.command(ProjectPickerPanel.COMMAND_SAVE_PROJECT, "S", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.command(COMMAND_REVERT, "R", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.command(COMMAND_FORMAT_XML, "F", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.separator();
        toolBar.command(COMMAND_UNDO, "U", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.command(COMMAND_REDO, "D", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.separator();
        toolBar.toggleCommand(COMMAND_DESIGN_MODE, "D", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.toggleCommand(COMMAND_RUN, ">", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.command(COMMAND_STOP, "X", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.command(COMMAND_RELOAD, "*", ToolButton.DisplayMode.ICON_AND_TEXT);
        toolBar.spacer();
        toolBar.toggleCommand(paneVisibility.viewCommandId(PANE_DIAGNOSTICS), "!", ToolButton.DisplayMode.ICON_AND_TEXT);
        return toolBar;
    }

    private Widget designPane() {
        designCanvas.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
        return designCanvas;
    }

    private Widget palettePane() {
        palettePanel.includeInternalWidgets(false).selectedCategory("Controls").search("Button");
        palettePanel.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        return palettePanel;
    }

    private Widget assetsPane() {
        assetBrowserPanel.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
        return assetBrowserPanel;
    }

    private void configureAssetBrowser() {
        assetBrowserPanel.title("Assets")
                .kind(XmlWidgetAssetKind.TEXTURE)
                .targetAttribute("backgroundTexture");
        syncAssetCatalog();
        assetBrowserPanel.onAssetApplied(selection -> {
            String target = selection.targetAttribute();
            boolean applied = !target.isEmpty() && propertiesPanel.setAttributeValue(target, selection.asset().id());
            statusLine.text(applied
                    ? "Applied asset " + selection.asset().id() + " to " + target + "."
                    : "Selected asset " + selection.asset().id() + ".");
        });
    }

    private void syncAssetCatalog() {
        propertiesPanel.assetCatalog(assetCatalog);
        assetBrowserPanel.catalog(assetCatalog);
    }

    private void configureRuntimeCommands() {
        session.commands(XmlCommandRegistry.empty()
                .register("demo.craft", (source, event) ->
                        statusLine.text("Runtime command demo.craft invoked by " + source.id() + ".")));
    }

    private Widget consolePane() {
        TextArea console = new TextArea("Console\n- Runtime output is disabled in the MVP.\n- Diagnostics auto-open this pane group when errors appear.");
        console.visibleLines(4).lineHeight(15.0f);
        console.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        return console;
    }

    private Widget placeholderPane(String title, String body) {
        VBox pane = new VBox();
        pane.spacing(5.0f);
        pane.layout(style -> style.margin(8.0f).size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        pane.addChild(new Label(title));
        TextBlock text = new TextBlock(body);
        text.wrap(true);
        text.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        pane.addChild(text);
        pane.applyQueuedMutations();
        return pane;
    }

    private void refreshFromSession() {
        refreshFromSession(null);
    }

    private void refreshFromSession(XmlEditorSessionChange change) {
        boolean fullRefresh = change == null;
        XmlEditorSessionChange.Kind kind = fullRefresh ? XmlEditorSessionChange.Kind.DOCUMENT_CHANGED : change.kind();

        if (fullRefresh || affectsCodeEditor(kind)) {
            syncingCodeEditor = true;
            if (!codeEditor.text().equals(session.text())) {
                codeEditor.text(session.text());
            }
            codeEditor.dirty(session.dirty());
            codeEditor.xmlDiagnostics(session.diagnostics());
            syncingCodeEditor = false;
        }

        if (fullRefresh || affectsDiagnostics(kind)) {
            diagnosticsPanel.model(session.diagnosticsModel());
        }

        statusBar.dirty(session.dirty())
                .mode(formatMode(session.mode()))
                .selectedNodePath(session.selectedPath().orElse(null))
                .diagnostics(session.diagnosticsModel());

        if (fullRefresh || affectsHierarchyDocument(kind)) {
            hierarchyPanel.document(session.document())
                    .selectedPath(session.selectedPath().orElse(null));
        } else if (kind == XmlEditorSessionChange.Kind.SELECTION_CHANGED) {
            hierarchyPanel.selectedPath(session.selectedPath().orElse(null));
        }

        syncCommandStates();
    }

    private static boolean affectsCodeEditor(XmlEditorSessionChange.Kind kind) {
        return kind == XmlEditorSessionChange.Kind.DOCUMENT_CHANGED
                || kind == XmlEditorSessionChange.Kind.TEXT_CHANGED
                || kind == XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED
                || kind == XmlEditorSessionChange.Kind.SAVED
                || kind == XmlEditorSessionChange.Kind.REVERTED
                || kind == XmlEditorSessionChange.Kind.SOURCE_CHANGED;
    }

    private static boolean affectsDiagnostics(XmlEditorSessionChange.Kind kind) {
        return kind == XmlEditorSessionChange.Kind.DOCUMENT_CHANGED
                || kind == XmlEditorSessionChange.Kind.TEXT_CHANGED
                || kind == XmlEditorSessionChange.Kind.DIAGNOSTICS_CHANGED
                || kind == XmlEditorSessionChange.Kind.REVERTED;
    }

    private static boolean affectsHierarchyDocument(XmlEditorSessionChange.Kind kind) {
        return kind == XmlEditorSessionChange.Kind.DOCUMENT_CHANGED
                || kind == XmlEditorSessionChange.Kind.TEXT_CHANGED
                || kind == XmlEditorSessionChange.Kind.REVERTED
                || kind == XmlEditorSessionChange.Kind.SOURCE_CHANGED;
    }

    private void deleteSelected() {
        session.selectedPath()
                .filter(path -> !path.rootPath())
                .ifPresent(path -> session.applyEdit(XmlWidgetDocumentEdits.removeChild(path)));
    }

    private void duplicateSelected() {
        Optional<XmlWidgetNodePath> path = session.selectedPath().filter(selected -> !selected.rootPath());
        if (path.isEmpty()) return;
        Optional<XmlWidgetNode> node = path.get().resolve(session.document());
        Optional<XmlWidgetNodePath> parent = path.get().parent();
        if (node.isEmpty() || parent.isEmpty()) return;
        List<Integer> indexes = path.get().indexes();
        int index = indexes.get(indexes.size() - 1) + 1;
        session.applyEdit(XmlWidgetDocumentEdits.addChild(parent.get(), index, node.get().copy()));
    }

    private void newProject() {
        session.replaceText(defaultXml());
        session.selectRoot();
        session.markClean();
        statusLine.text("Created new demo XML project.");
    }

    private void openDemoProject() {
        session.replaceText("""
                <VBox id="openedProject" spacing="8" width="420" height="240">
                    <Label id="title" text="Opened XML Project" />
                    <Button id="apply" text="Apply" x="48" y="52" width="128" height="32" />
                    <Button id="cancel" text="Cancel" x="184" y="52" width="128" height="32" />
                </VBox>
                """);
        session.selectRoot();
        session.markClean();
        statusLine.text("Opened demo XML project.");
    }

    private void saveProject() {
        if (session.save()) {
            statusLine.text("Saved XML project.");
        } else {
            session.markClean();
            statusLine.text("Marked in-memory XML project clean.");
        }
    }

    private void saveProjectAs() {
        session.saveAs(XmlEditorDocumentSources.memory(
                "demo:xml-editor-copy",
                "XML Editor Demo Copy",
                session.text()));
        statusLine.text("Saved XML project as an in-memory copy.");
    }

    private void syncCommandStates() {
        for (String commandId : List.of(
                ProjectPickerPanel.COMMAND_SAVE_PROJECT,
                COMMAND_REVERT,
                COMMAND_UNDO,
                COMMAND_REDO,
                COMMAND_DELETE,
                COMMAND_DUPLICATE,
                COMMAND_DESIGN_MODE,
                COMMAND_RUN,
                COMMAND_STOP,
                paneVisibility.viewCommandId(PANE_RUNTIME),
                COMMAND_LAYOUT_RESTORE)) {
            commands.command(commandId).ifPresent(EditorCommand::notifyChanged);
        }
    }

    private static ScrollView scroll(Widget content) {
        ScrollView scroll = new ScrollView(content);
        scroll.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        return scroll;
    }

    private static String formatMode(XmlEditorMode mode) {
        if (mode == null) return "Design";
        return switch (mode) {
            case DESIGN -> "Design";
            case CODE -> "Code";
            case RUNTIME -> "Runtime";
        };
    }

    private static String defaultXml() {
        return """
                <VBox id="recipeMachine" spacing="6" width="360" height="220">
                    <Label id="title" text="Recipe Machine" />
                    <Button id="craft" text="Craft" onClick="demo.craft" x="52" y="48" width="154" height="32" />
                    <Label id="status" text="Ready" x="52" y="92" width="180" height="22" />
                </VBox>
                """;
    }

    private static XmlWidgetAssetCatalog demoAssetCatalog() {
        return XmlWidgetAssetCatalog.builder()
                .texture("minecraft:textures/gui/widgets.png", 256, 256)
                .texture("minecraft:textures/block/crafter_top.png", 16, 16)
                .texture("minecraft:textures/item/redstone.png", 16, 16)
                .font("minecraft:default")
                .shader("unigui:rounded_panel")
                .build();
    }

    private static XmlWidgetAssetCatalog buildAssetCatalog() {
        return XmlWidgetAssetProviders.catalog(demoAssetCatalog());
    }
}
