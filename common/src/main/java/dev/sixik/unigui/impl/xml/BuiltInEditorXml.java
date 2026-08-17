package dev.sixik.unigui.impl.xml;

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
import dev.sixik.unigui.widgets.editor.PropertyGrid;
import dev.sixik.unigui.widgets.editor.ProjectPickerPanel;
import dev.sixik.unigui.widgets.editor.ResizablePanelHeader;
import dev.sixik.unigui.widgets.editor.SearchBoxWithFilterChips;
import dev.sixik.unigui.widgets.editor.SelectionOverlay;
import dev.sixik.unigui.widgets.editor.StatusBar;
import dev.sixik.unigui.widgets.editor.WidgetPalette;
import dev.sixik.unigui.widgets.editor.XmlDesignCanvas;
import dev.sixik.unigui.widgets.editor.XmlPropertiesPanel;
import dev.sixik.unigui.widgets.editor.XmlRuntimeViewPane;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.widgets.containers.Box;

final class BuiltInEditorXml {
    private BuiltInEditorXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        BuiltInWidgetXmlSupport.commonWidget(registry.register("PropertyGrid", PropertyGrid::new), PropertyGrid.class)
                .describe("Property Grid", "Editor", "Descriptor-backed inspector for XML widget attributes and property-child slots.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("XmlPropertiesPanel", XmlPropertiesPanel::new), XmlPropertiesPanel.class)
                .describe("XML Properties Panel", "Editor", "Session-aware XML inspector pane that applies attribute edits through undoable document edits.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("SelectionOverlay", SelectionOverlay::new), SelectionOverlay.class)
                .describe("Selection Overlay", "Editor", "Editor overlay for XML widget hover, selection, move and resize handles.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("DesignCanvasOverlay", DesignCanvasOverlay::new), DesignCanvasOverlay.class)
                .describe("Design Canvas Overlay", "Editor", "Design-surface overlay alias for XML selection and transform handles.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("XmlDesignCanvas", XmlDesignCanvas::new), XmlDesignCanvas.class)
                .describe("XML Design Canvas", "Editor", "Design-surface host that layers XML preview, grid and selection overlay.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("XmlRuntimeViewPane", XmlRuntimeViewPane::new), XmlRuntimeViewPane.class)
                .describe("XML Runtime View Pane", "Editor", "Play-mode host that materializes the current XML snapshot as a runtime widget tree.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("GridOverlay", GridOverlay::new), GridOverlay.class)
                .describe("Grid Overlay", "Editor", "Design-surface grid overlay with major lines and coordinate snap helpers.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("ProjectPickerPanel", ProjectPickerPanel::new), ProjectPickerPanel.class)
                .describe("Project Picker Panel", "Editor", "Editor panel for current project actions and recent project selection.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("StatusBar", StatusBar::new), StatusBar.class)
                .describe("Status Bar", "Editor", "Compact editor footer for dirty state, mode, diagnostics, selection and canvas scale.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("DiagnosticsStrip", DiagnosticsStrip::new), DiagnosticsStrip.class)
                .describe("Diagnostics Strip", "Editor", "Status bar alias optimized for compact diagnostics summaries.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("WidgetPalette", WidgetPalette::new), WidgetPalette.class)
                .describe("Widget Palette", "Editor", "Descriptor-backed palette for searching and inserting XML-visible widgets.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("PalettePanel", PalettePanel::new), PalettePanel.class)
                .describe("Palette Panel", "Editor", "Editor palette panel alias for descriptor-backed widget insertion.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("CommandPalette", CommandPalette::new), CommandPalette.class)
                .describe("Command Palette", "Editor", "Searchable command surface backed by an editor CommandManager.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("AssetBrowserPanel", AssetBrowserPanel::new), AssetBrowserPanel.class)
                .describe("Asset Browser Panel", "Editor", "Catalog-backed browser for selecting texture, font and shader assets into XML properties.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("SearchBoxWithFilterChips", SearchBoxWithFilterChips::new), SearchBoxWithFilterChips.class)
                .describe("Search Box With Filter Chips", "Editor", "Reusable search box with toggle filter chips for editor panels.");
        dialog(registry.register("Dialog", Dialog::new));
        BuiltInWidgetXmlSupport.commonWidget(registry.register("PaneHeader", PaneHeader::new), PaneHeader.class)
                .describe("Pane Header", "Editor", "Reusable editor pane header with title, dirty, pin, menu and close controls.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("ResizablePanelHeader", ResizablePanelHeader::new), ResizablePanelHeader.class)
                .describe("Resizable Panel Header", "Editor", "Pane header variant with resize affordance and clamped resize requests.");
        editorBox(registry.register("DragSource", DragSource::new), DragSource.class)
                .describe("Drag Source", "Editor", "Container helper that emits XML-configurable drag payload metadata.");
        editorBox(registry.register("DropTarget", DropTarget::new), DropTarget.class)
                .describe("Drop Target", "Editor", "Container helper that validates drag payload types and reports drop results.");
    }

    private static <T extends Box> WidgetXmlType<T> editorBox(WidgetXmlType<T> type, Class<?> widgetType) {
        XmlChildPolicy<T> children = (parent, child) -> {
            parent.addChild(child);
            parent.applyQueuedMutations();
        };
        return BuiltInWidgetXmlSupport.box(type, widgetType)
                .childPolicy(children)
                .propertyChild("Children", children,
                        XmlPropertyChildDescriptor.of("Children")
                                .category("Content")
                                .description("Child widgets hosted by this editor helper container."));
    }

    private static WidgetXmlType<Dialog> dialog(WidgetXmlType<Dialog> type) {
        return BuiltInWidgetXmlSupport.box(type, Dialog.class)
                .describe("Dialog", "Editor", "Modal editor dialog with standardized message, content slot and result buttons.")
                .attribute("windowX", XmlValueParsers.FLOAT, (widget, value) -> widget.position(value, widget.windowY()),
                        XmlAttributeDescriptor.of("windowX")
                                .category("Layout")
                                .defaultValue("32")
                                .description("Dialog position inside its overlay host on the X axis."))
                .attribute("windowY", XmlValueParsers.FLOAT, (widget, value) -> widget.position(widget.windowX(), value),
                        XmlAttributeDescriptor.of("windowY")
                                .category("Layout")
                                .defaultValue("28")
                                .description("Dialog position inside its overlay host on the Y axis."))
                .childPolicy(BuiltInEditorXml::setDialogContent)
                .propertyChild("Content", BuiltInEditorXml::setDialogContent,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single widget shown inside the dialog body.")
                                .singleChildOnly());
    }

    private static void setDialogContent(Dialog dialog, Widget child) {
        if (dialog.content() != null && dialog.content() != child) {
            throw new IllegalArgumentException("Widget Dialog can contain only one content child.");
        }
        dialog.content(child);
        dialog.applyQueuedMutations();
    }
}
