package dev.sixik.unigui.api.style;

/**
 * Центральные строковые id, используемые style/theme системой.
 *
 * <p>{@link Key} содержит id style-свойств для {@link StyleKey}. {@link Widget} содержит id типов
 * виджетов, которые передаются в {@link Theme#styleFor(String)}, {@link MutableTheme#put(String, Style)}
 * и локальные style APIs виджетов. Хранение id в одном месте уменьшает количество хрупких строковых
 * литералов в темах, XML и custom skin'ах.</p>
 */
public final class StyleIds {
    /**
     * Wildcard id для fallback/local стилей.
     */
    public static final String WILDCARD = Theme.WILDCARD;

    private StyleIds() {
    }

    /**
     * Id style-свойств, на основе которых создаются {@link StyleKey} constants.
     */
    public static final class Key {
        public static final String RENDERER = "renderer";
        public static final String BACKGROUND_KIND = "background.kind";
        public static final String BACKGROUND_COLOR = "background.color";
        public static final String BACKGROUND_TEXTURE = "background.texture";
        public static final String BACKGROUND_TEXTURE_GROUP = "background.texture.group";
        public static final String BACKGROUND_TEXTURE_TINT = "background.texture.tint";
        public static final String BACKGROUND_TEXTURE_FIT = "background.texture.fit";
        public static final String SLICE_BORDERS = "background.texture.slice-borders";
        public static final String BACKGROUND_SHADER = "background.shader";
        public static final String BACKGROUND_VISIBLE = "background.visible";
        public static final String BORDER_COLOR = "border.color";
        public static final String BORDER_VISIBLE = "border.visible";
        public static final String TEXT_COLOR = "text.color";
        public static final String TEXT_SIZE = "text.size";
        public static final String TEXT_TRACKING = "text.tracking";
        public static final String TEXT_TRANSFORM = "text.transform";
        public static final String CHECKBOX_BACKGROUND_COLOR = "checkbox.background.color";
        public static final String PLACEHOLDER_COLOR = "placeholder.color";
        public static final String CARET_COLOR = "caret.color";
        public static final String FOCUS_RING_COLOR = "focus.ring.color";
        public static final String ACCENT_COLOR = "accent.color";
        public static final String TRACK_COLOR = "track.color";
        public static final String THUMB_COLOR = "thumb.color";
        public static final String THUMB_RADIUS = "thumb.radius";
        public static final String TRACK_THICKNESS = "track.thickness";
        public static final String THUMB_THICKNESS = "thumb.thickness";
        public static final String HOLD_COLOR = "hold.color";
        public static final String RADIO_OUTER_SIZE = "radio.outer-size";
        public static final String RADIO_INNER_SIZE = "radio.inner-size";
        public static final String RADIO_TEXT_GAP = "radio.text-gap";
        public static final String BORDER_WIDTH = "border.width";
        public static final String RADIUS = "radius";
        public static final String SHAPE_STROKE = "shape.stroke";
        public static final String SHAPE_COLOR = "shape.color";
        public static final String SHAPE_TYPE = "shape.type";
        public static final String PATH_COLOR = "path.color";
        public static final String PATH_STROKE = "path.stroke";
        public static final String PATH_STROKE_WIDTH = "path.stroke-width";
        public static final String VIRTUAL_LIST_VIEW_MODE = "virtual-list.view-mode";
        public static final String VIRTUAL_LIST_ITEM_HEIGHT = "virtual-list.item-height";
        public static final String VIRTUAL_LIST_GRID_CELL_WIDTH = "virtual-list.grid.cell-width";
        public static final String VIRTUAL_LIST_GRID_CELL_HEIGHT = "virtual-list.grid.cell-height";
        public static final String VIRTUAL_LIST_GRID_COLUMNS = "virtual-list.grid.columns";
        public static final String VIRTUAL_LIST_ROW_GAP = "virtual-list.row-gap";
        public static final String VIRTUAL_LIST_COLUMN_GAP = "virtual-list.column-gap";
        public static final String VIRTUAL_LIST_PADDING = "virtual-list.padding";
        public static final String VIRTUAL_LIST_OVERSCAN = "virtual-list.overscan";
        public static final String VIRTUAL_LIST_OFFSCREEN_CACHE_SIZE = "virtual-list.offscreen-cache-size";
        public static final String VIRTUAL_LIST_SCROLL_STEP = "virtual-list.scroll-step";
        public static final String VIRTUAL_LIST_CONSUME_WHEEL_AT_BOUNDS = "virtual-list.consume-wheel-at-bounds";
        public static final String TREE_ROW_HEIGHT = "tree.row.height";
        public static final String TREE_ROW_STYLE = "tree.row.style";
        public static final String TREE_DISCLOSURE_STYLE = "tree.disclosure.style";
        public static final String TREE_ROW_RADIUS = "tree.row.radius";
        public static final String TREE_ROW_INSET_X = "tree.row.inset-x";
        public static final String TREE_ROW_INSET_Y = "tree.row.inset-y";
        public static final String TREE_INDENT_WIDTH = "tree.indent.width";
        public static final String TREE_FOLDER_FILL_ENABLED = "tree.folder.fill-enabled";
        public static final String TREE_HOVER_COLOR = "tree.hover.color";
        public static final String TREE_SELECTED_COLOR = "tree.selected.color";
        public static final String TREE_TEXT_COLOR = "tree.text.color";
        public static final String TREE_SELECTED_TEXT_COLOR = "tree.selected.text-color";
        public static final String TREE_DISABLED_TEXT_COLOR = "tree.disabled.text-color";
        public static final String TREE_DISCLOSURE_COLOR = "tree.disclosure.color";
        public static final String TREE_ICON_COLOR = "tree.icon.color";
        public static final String TRANSITION_DURATION = "transition.duration";
        public static final String TRANSITION_EASING = "transition.easing";

        private Key() {
        }
    }

    /**
     * Id типов виджетов, используемые Theme и local style lookup.
     */
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
        public static final String FLEX_BOX = "FlexBox";
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
        public static final String POST_PROCESSING_LAYER = "PostProcessingLayer";
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

        // Id XML/editor виджетов, которые не входят в начальный набор core controls.
        public static final String CODE_EDITOR = "CodeEditor";
        public static final String HOLD_BUTTON = "HoldButton";
        public static final String ICON_BUTTON = "IconButton";
        public static final String MAP_CANVAS = "MapCanvas";
        public static final String MAP_MARKER = "MapMarker";
        public static final String MENU_BAR = "MenuBar";
        public static final String MINECRAFT_ITEM_PICKER_WIDGET = "MinecraftItemPickerWidget";
        public static final String MINECRAFT_ITEM_TOOLTIP = "MinecraftItemTooltip";
        public static final String MINECRAFT_TEXTURE_PICKER_WIDGET = "MinecraftTexturePickerWidget";
        public static final String PANEL_ROW_WIDGET = "PanelRowWidget";
        public static final String SETTING_ROW = "SettingRow";
        public static final String TEXT_AREA = "TextArea";
        public static final String TOGGLE_TOOL_BUTTON = "ToggleToolButton";
        public static final String TOOL_BAR = "ToolBar";
        public static final String TOOL_BUTTON = "ToolButton";
        public static final String WORLD_CANVAS = "WorldCanvas";
        public static final String XML_CODE_EDITOR = "XmlCodeEditor";

        private Widget() {
        }
    }
}
