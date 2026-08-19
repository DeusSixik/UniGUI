package dev.sixik.unigui.api.style;

/**
 * Central string identifiers used by the style/theme system.
 *
 * <p>Use {@link Key} for {@link StyleKey} ids and {@link Widget} for widget
 * type ids passed to {@link Theme#styleFor(String)}, {@link MutableTheme#put}
 * and widget local-style APIs. Keeping these ids in one place avoids fragile
 * magic strings in themes and custom skins.</p>
 */
public final class StyleIds {
    /** Wildcard style id used for fallback/local styles. */
    public static final String WILDCARD = Theme.WILDCARD;

    private StyleIds() {
    }

    /** Style property ids used to create {@link StyleKey} constants. */
    public static final class Key {
        public static final String RENDERER = "renderer";
        public static final String BACKGROUND_COLOR = "background.color";
        public static final String BACKGROUND_TEXTURE = "background.texture";
        public static final String BACKGROUND_TEXTURE_TINT = "background.texture.tint";
        public static final String BACKGROUND_TEXTURE_FIT = "background.texture.fit";
        public static final String BORDER_COLOR = "border.color";
        public static final String TEXT_COLOR = "text.color";
        public static final String PLACEHOLDER_COLOR = "placeholder.color";
        public static final String ACCENT_COLOR = "accent.color";
        public static final String TRACK_COLOR = "track.color";
        public static final String THUMB_COLOR = "thumb.color";
        public static final String BORDER_WIDTH = "border.width";
        public static final String RADIUS = "radius";

        private Key() {
        }
    }

    /** Widget type ids used by theme and local style lookup. */
    public static final class Widget {
        public static final String ACCORDION = "Accordion";
        public static final String BORDER = "Border";
        public static final String BOX = "Box";
        public static final String BREADCRUMB = "Breadcrumb";
        public static final String BREADCRUMB_ITEM = "BreadcrumbItem";
        public static final String BUTTON = "Button";
        public static final String CACHED_SUBTREE_WIDGET = "CachedSubtreeWidget";
        public static final String CANVAS_WIDGET = "CanvasWidget";
        public static final String CAROUSEL = "Carousel";
        public static final String CHART = "Chart";
        public static final String CHECKBOX = "Checkbox";
        public static final String COLOR_PICKER = "ColorPicker";
        public static final String COMBO_BOX = "ComboBox";
        public static final String CONTEXT_MENU = "ContextMenu";
        public static final String DATE_PICKER = "DatePicker";
        public static final String DOCK_PANEL = "DockPanel";
        public static final String DOCKING_ROOT = "DockingRoot";
        public static final String DROP_DOWN_BOX = "DropDownBox";
        public static final String EXPANDABLE_PANEL = "ExpandablePanel";
        public static final String GRAPH_VIEW = "GraphView";
        public static final String GRID_BOX = "GridBox";
        public static final String HBOX = "HBox";
        public static final String IMAGE_VIEW = "ImageView";
        public static final String LABEL = "Label";
        public static final String LINEAR_BOX = "LinearBox";
        public static final String LOADING_INDICATOR = "LoadingIndicator";
        public static final String NODE_GRAPH = "NodeGraph";
        public static final String NOTIFICATION_VIEW = "NotificationView";
        public static final String NUMBER_FIELD = "NumberField";
        public static final String OVERLAY_LAYER = "OverlayLayer";
        public static final String PAGE_VIEW = "PageView";
        public static final String PANEL_WIDGET = "PanelWidget";
        public static final String PASSWORD_FIELD = "PasswordField";
        public static final String PATH = "Path";
        public static final String POPUP = "Popup";
        public static final String PROGRESS_BAR = "ProgressBar";
        public static final String RADIO_BUTTON = "RadioButton";
        public static final String RADIO_GROUP = "RadioGroup";
        public static final String RICH_TEXT_VIEW = "RichTextView";
        public static final String SCROLL_BAR = "ScrollBar";
        public static final String SCROLL_VIEW = "ScrollView";
        public static final String SEARCH_FIELD = "SearchField";
        public static final String SEPARATOR = "Separator";
        public static final String SHAPE = "Shape";
        public static final String SLIDER = "Slider";
        public static final String SPARKLINE = "Sparkline";
        public static final String SPINNER = "Spinner";
        public static final String SPLIT_PANEL = "SplitPanel";
        public static final String SPLITTER = "Splitter";
        public static final String STACK_PANEL = "StackPanel";
        public static final String TAB_CONTROL = "TabControl";
        public static final String TEXT = "Text";
        public static final String TEXT_BLOCK = "TextBlock";
        public static final String TEXT_FIELD = "TextField";
        public static final String TEXT_INPUT = "TextInput";
        public static final String TEXT_WIDGET = "TextWidget";
        public static final String TEXTURE_WIDGET = "TextureWidget";
        public static final String TIME_SPAN_FIELD = "TimeSpanField";
        public static final String TOAST = "Toast";
        public static final String TOGGLE_BUTTON = "ToggleButton";
        public static final String TOGGLE_SWITCH = "ToggleSwitch";
        public static final String TOOLTIP = "Tooltip";
        public static final String TREE_LIST = "TreeList";
        public static final String TREE_LIST_PICKER = "TreeListPicker";
        public static final String TREE_VIEW = "TreeView";
        public static final String VBOX = "VBox";
        public static final String VIEW = "View";
        public static final String VIRTUAL_LIST_VIEW = "VirtualListView";
        public static final String VIRTUAL_TABLE_VIEW = "VirtualTableView";
        public static final String WINDOW_WIDGET = "WindowWidget";
        public static final String WRAP_PANEL = "WrapPanel";
        // XML/editor widget ids that do not belong to the initial core control set.
        public static final String ASSET_BROWSER_PANEL = "AssetBrowserPanel";
        public static final String CODE_EDITOR = "CodeEditor";
        public static final String COMMAND_PALETTE = "CommandPalette";
        public static final String DESIGN_CANVAS_OVERLAY = "DesignCanvasOverlay";
        public static final String DIAGNOSTICS_STRIP = "DiagnosticsStrip";
        public static final String DIALOG = "Dialog";
        public static final String DRAG_SOURCE = "DragSource";
        public static final String DROP_TARGET = "DropTarget";
        public static final String GRID_OVERLAY = "GridOverlay";
        public static final String HOLD_BUTTON = "HoldButton";
        public static final String ICON_BUTTON = "IconButton";
        public static final String MAP_CANVAS = "MapCanvas";
        public static final String MAP_MARKER = "MapMarker";
        public static final String MENU_BAR = "MenuBar";
        public static final String MINECRAFT_ITEM_PICKER_WIDGET = "MinecraftItemPickerWidget";
        public static final String MINECRAFT_ITEM_TOOLTIP = "MinecraftItemTooltip";
        public static final String MINECRAFT_TEXTURE_PICKER_WIDGET = "MinecraftTexturePickerWidget";
        public static final String PALETTE_PANEL = "PalettePanel";
        public static final String PANE_HEADER = "PaneHeader";
        public static final String PANEL_ROW_WIDGET = "PanelRowWidget";
        public static final String PROJECT_PICKER_PANEL = "ProjectPickerPanel";
        public static final String PROPERTY_FIELD_ROW = "PropertyFieldRow";
        public static final String PROPERTY_GRID = "PropertyGrid";
        public static final String RESIZABLE_PANEL_HEADER = "ResizablePanelHeader";
        public static final String SEARCH_BOX_WITH_FILTER_CHIPS = "SearchBoxWithFilterChips";
        public static final String SELECTION_OVERLAY = "SelectionOverlay";
        public static final String SETTING_ROW = "SettingRow";
        public static final String STATUS_BAR = "StatusBar";
        public static final String TEXT_AREA = "TextArea";
        public static final String TOGGLE_TOOL_BUTTON = "ToggleToolButton";
        public static final String TOOL_BAR = "ToolBar";
        public static final String TOOL_BUTTON = "ToolButton";
        public static final String WIDGET_PALETTE = "WidgetPalette";
        public static final String WORLD_CANVAS = "WorldCanvas";
        public static final String XML_CODE_EDITOR = "XmlCodeEditor";
        public static final String XML_DESIGN_CANVAS = "XmlDesignCanvas";
        public static final String XML_EDITOR_DEMO_SCREEN = "XmlEditorDemoScreen";
        public static final String XML_HIERARCHY_PANEL = "XmlHierarchyPanel";
        public static final String XML_PROPERTIES_PANEL = "XmlPropertiesPanel";
        public static final String XML_RUNTIME_VIEW_PANE = "XmlRuntimeViewPane";


        private Widget() {
        }
    }
}