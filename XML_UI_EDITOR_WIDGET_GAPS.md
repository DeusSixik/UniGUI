# XML UI Editor Widget Gaps

## Context

Before starting the Unity-like XML UI editor, UniGUI should have the editor-facing widgets needed to build the editor itself and the XML-facing descriptors needed to expose user widgets in the palette/properties inspector.

This checklist separates three things:

- widgets that already exist and can be reused;
- widgets/editor controls that are missing or too narrow;
- existing runtime widgets that still need XML registration/descriptor coverage.

The goal is not to finish every possible widget before the editor. The goal is to unblock a compact editor MVP without painting the architecture into a corner.

---

## Already Available And Reusable

### Layout / Shell

- [x] `DockingRoot` / `DockingManager` - IDE-style panes, tabs, splits, floating windows and snapshots.
- [x] `DockPanel` - simple edge docking for app shell rows/bars.
- [x] `SplitPanel` / `Splitter` - two-pane resizable layout.
- [x] `ScrollView` - scrollable pane host.
- [x] `StackPanel` - overlay layering inside a pane/canvas.
- [x] `VBox` / `HBox` / `LinearBox` / `WrapPanel` / `GridBox` - general layout composition.
- [x] `OverlayLayer` - modal/popup/window host.
- [x] `WindowWidget` / `WindowManager` - floating, draggable and resizable windows.

### Navigation / Data

- [x] `TreeView` / `TreeViewNode` - hierarchy-style tree with keyboard navigation.
- [x] `TreeList` / `TreeListPicker` - tree list and picker direction.
- [x] `VirtualListView` - scalable list for palettes, diagnostics and recent projects.
- [x] `VirtualTableView` - property/diagnostic table base, including editable cells.
- [x] `TabControl` / `PageView` - document tabs and mode switching direction.
- [x] `Breadcrumb` - project/file path display direction.

### Inputs / Controls

- [x] `Button`, `ToggleButton`, `ToggleSwitch`, `Checkbox`, `RadioButton`, `RadioGroup`.
- [x] `TextInput` / `TextField` - single-line text editing.
- [x] `NumberField`, `Slider`, `ComboBox`, `DropDownBox`, `SearchField`.
- [x] `ColorPicker`, `DatePicker`, `TimeSpanField`, `PasswordField`.
- [x] `ContextMenu`, `Popup`, `Tooltip`, `Toast`, `NotificationView`.
- [x] `MinecraftItemPickerWidget`, `MinecraftTexturePickerWidget` for Minecraft-specific assets.

### Display / Rendering

- [x] `Text`, `TextBlock`, `Label`, `RichTextView`.
- [x] `TextureWidget`, `ImageView`, `Shape`, `Separator`, `CanvasWidget`, `Path`.
- [x] `ProgressBar`, `LoadingIndicator`, `Spinner`.
- [x] `Chart`, `Sparkline` for debug/profiling panels if needed later.

### XML / Editor Models

- [x] `XmlWidgetDocument`, `XmlWidgetHierarchy`, `XmlWidgetInspector`, `XmlWidgetSelectionModel`.
- [x] `XmlWidgetDocumentEdits`, `XmlWidgetLayoutHandles`, `XmlWidgetDiagnosticsModel`.
- [x] `XmlWidgetHotReloadPreview`, `XmlWidgetAssetPickerPanel`.

---

## Critical Missing Widgets Before Editor MVP

These should be implemented or intentionally scoped down before starting the real editor screen.

### P0 - Must Have

- [x] `TextArea` - multiline editable text widget.
  - Reason: `TextInput` is explicitly single-line and sanitizes control characters; XML Code View needs newline-aware editing.
  - Minimum: multiline model, cursor, selection, clipboard, scroll, line height, Home/End/Up/Down/Page keys.
  - Later: syntax highlighting, minimap, folding.

- [x] `CodeEditor` or `XmlCodeEditor` - XML-aware wrapper over `TextArea`.
  - Reason: Code View needs line numbers, parse diagnostics line/column markers, dirty state and format action.
  - Minimum: read/write text, line numbers, error underline/row highlight, scroll-to-line.
  - Can start as editor-only widget and later become general-purpose.

- [x] `MenuBar` / `Menu` / `MenuItem`.
  - Reason: top toolbar should hide most panes behind File/Edit/View/Project/Run/Layout menus.
  - Existing `ContextMenu` can power popup behavior, but there is no first-class app menu bar.
  - Minimum: horizontal menu headers, nested popup menus, separators, disabled items, checkable items.

- [x] `ToolBar`.
  - Reason: project actions and run controls need a compact top command area.
  - Minimum: grouped rows of command buttons, overflow behavior, separators, alignment left/right.

- [x] `ToolButton` / `IconButton` / `ToggleToolButton`.
  - Reason: toolbar buttons should be compact and icon-friendly instead of full text buttons everywhere.
  - Minimum: icon-only, text-only and icon+text modes; tooltip; checked/active state.

- [x] `CommandManager` / `EditorCommand` / `KeyBinding`.
  - Reason: menus, toolbar buttons and keyboard shortcuts must invoke the same action ids.
  - Minimum: command id, label, enabled/checked supplier, run callback, shortcut text, changed notification.
  - Examples: `project.new`, `project.open`, `project.save`, `view.properties.toggle`, `run.play`, `run.stop`.

- [x] `PropertyGrid` / `PropertyInspectorPanel`.
  - Reason: Unity-like Properties should not be handcrafted for every selected XML node.
  - Minimum: category groups, label/value rows, reset/remove attr buttons, changed validation state.
  - Backing data: `XmlWidgetInspector.Inspection` and `XmlAttributeDescriptor`.
  - Done: `PropertyGrid` builds grouped rows from `XmlWidgetInspector.Inspection`, writes back to `XmlWidgetElement`, and exposes reset/remove/change callbacks.

- [x] Reusable property field widgets.
  - Reason: XML attributes need type-aware editing.
  - Minimum field types: string, number, boolean, enum, color, insets, size value, resource id, binding/action string.
  - Existing controls can be composed, but the editor needs a consistent property-row contract.
  - Done: `PropertyFieldRow` provides the reusable row contract and infers string, number, boolean, enum, color, insets, size value, resource id, and binding/action field kinds.

- [x] `DesignCanvasOverlay` / `SelectionOverlay`.
  - Reason: Design View needs selection bounds without letting runtime controls consume edit-mode clicks.
  - Minimum: hover outline, selected outline, resize handles, move handle, cursor feedback.
  - Backing data: selected `XmlWidgetNodePath`, rendered bounds, `XmlWidgetLayoutHandles`.
  - Done: `SelectionOverlay` renders source-XML hover/selection frames, exposes move/resize handles, updates `x/y/width/height` through `XmlWidgetLayoutHandles`, and `DesignCanvasOverlay` is registered as an XML-visible alias.

- [x] `PaneVisibilityController` for docking workspace.
  - Reason: the editor should not show every pane at once.
  - Minimum: show/hide/toggle pane by id, pin state, auto-open diagnostics on errors, restore from View menu.
  - Done: `PaneVisibilityController` registers dock panes, hides/restores them by id, tracks pin state, binds checked View-menu toggle commands through `CommandManager`, and auto-opens diagnostics when XML errors appear.

- [x] Project/recent-project picker panel.
  - Reason: toolbar must support `new_project`, `open_project`, `save_project`, `last_projects`.
  - Minimum: recent project list, path/resource label, open/save callbacks, unsaved-change prompt integration.
  - Done: `ProjectPickerPanel` exposes current project path/resource labels, capped recent project rows, host callbacks for new/open/save/recent actions, unsaved-change confirmation, and `CommandManager` command ids for the toolbar flow.

### P1 - Should Have Shortly After MVP Starts

- [x] `StatusBar` / `DiagnosticsStrip`.
  - Reason: compact default layout needs a bottom summary instead of opening diagnostics constantly.
  - Minimum: dirty marker, current mode, error/warning count, selected node path, scale indicator.
  - Done: `StatusBar` provides XML-visible dirty/mode/error/warning/selected-path/view-scale state labels, `DiagnosticsStrip` aliases the compact diagnostics footer, and both are registered for editor XML.

- [x] `PalettePanel` / `WidgetPalette`.
  - Reason: users need to insert widgets without writing XML manually.
  - Minimum: search, categories, descriptor-backed items, insert selected into hierarchy/canvas.
  - Done: `WidgetPalette` builds descriptor-backed category/search lists from `XmlWidgetRegistry`, tracks selected widget descriptors, emits insert requests with `XmlWidgetElement` payloads and undoable document edits, and `PalettePanel` is registered as an XML-visible editor alias.

- [x] `AssetBrowserPanel`.
  - Reason: SDM Shop and texture/resource attributes need discoverable assets.
  - Minimum: category list, search, preview, select asset into current property field.
  - Done: `AssetBrowserPanel` browses `XmlWidgetAssetCatalog` texture/font/shader categories, filters by search, renders asset rows and preview text, and emits apply callbacks with the target property attribute for inspector integration.

- [x] `CommandPalette`.
  - Reason: large editors need fast keyboard-driven access to hidden windows and commands.
  - Minimum: searchable command list backed by `CommandManager`.
  - Done: `CommandPalette` filters `CommandManager` commands by id/label/shortcut, tracks selected commands, executes enabled commands, surfaces disabled attempts, emits host invocation callbacks, and is registered for editor XML.

- [x] `Dialog` base widget.
  - Reason: new/open/save, unsaved changes, validation errors and project settings need consistent modal surfaces.
  - Existing `WindowWidget` can host this, but a dialog wrapper should standardize buttons/results.
  - Done: `Dialog` extends the overlay-managed `WindowWidget` shell with message/content slots, XML-configurable result buttons, default/cancel result helpers, close-on-result behavior, and `Dialog.Content` XML support.

- [x] `ResizablePanelHeader` / `PaneHeader`.
  - Reason: custom editor panes need title, close, pin, menu and dirty indicators outside full docking tabs.
  - Done: `PaneHeader` provides XML-visible title/dirty/pin/menu/close controls with optional `DockPane` state sync and action callbacks; `ResizablePanelHeader` adds resize edge metadata, clamped panel size state, resize enable/visibility controls and resize request callbacks for custom splitter-backed panes.

- [x] Drag source/drop target helper widgets.
  - Reason: palette-to-canvas and hierarchy reorder need reusable drag payload handling.
  - Minimum: payload id/type, drag preview, target validation, drop result.
  - Done: `DragSource` and `DropTarget` are XML-visible editor helper containers with shared `DragPayload`, source drag lifecycle callbacks, pointer threshold/capture behavior, preview text metadata, target accepted-type filters, validator callbacks, accepted/rejected/ignored drop results and XML child support.

- [x] `SearchBoxWithFilterChips`.
  - Reason: palette/assets/bindings/diagnostics all need search + category filters.
  - Done: `SearchBoxWithFilterChips` combines a zero-debounce `SearchField` with XML-configurable filter chips, active filter state, toggle/clear helpers, and filter-change callbacks for reusable editor panels.

### P2 - Later / Nice To Have

- [ ] `Ruler` / `GuideOverlay` / `GridOverlay` for design canvas.
- [ ] `TransformGizmo` with snap/grid/align/distribute helpers.
- [ ] `Timeline` / `AnimationCurveEditor` for future animation editing.
- [ ] `BindingGraphEditor` if bindings become visual instead of plain text.
- [ ] `ScriptEditor` with syntax highlighting and sandbox diagnostics.
- [ ] `MiniMap` / `Overview` for very large screens.

---

## XML Registration / Descriptor Coverage Gaps

The editor palette and Properties panel depend on `XmlWidgetRegistry` descriptors. Many runtime widgets already exist but are not yet registered in the built-in XML registry.

Current built-in XML coverage is mostly:

- containers: `Panel`, `Box`, `VBox`, `HBox`, `StackPanel`, `WrapPanel`, `ScrollView`, `GridBox`, `SettingRow`;
- display: `TextWidget`, `Text`, `TextBlock`, `Label`, `TextureWidget`, `ImageView`, `Separator`, `Shape`, `Sparkline`, `Chart`;
- controls: `Button`, `ToggleButton`, `Checkbox`, `Slider`, `ProgressBar`, `Spinner`, `TextField`.

### P0 XML Coverage

- [x] Register `TextInput`, `NumberField`, `SearchField`, `PasswordField`, `TimeSpanField`.
- [x] Register `ToggleSwitch` and `RadioButton`; `RadioGroup` remains Java-host wiring until a retained group/container widget exists.
- [x] Register `ComboBox` and `DropDownBox` with item/content child policies.
- [x] Register `ContextMenu` with `Button`/`Separator` item property-child structure; `MenuBar` menu model remains Java-side until non-widget property elements exist.
- [x] Register `DockingRoot` enough for editor workspace demos; XML supports document/tool-pane content widgets mapped into dock panes.
- [x] Register `OverlayLayer`, `Popup`, `Tooltip`, `WindowWidget` with content/overlay property children; popup/tooltip anchors remain code-wired.
- [x] Register `TreeView` / `TreeList`; hierarchy data is exposed through a compact `nodes` path attribute because `TreeViewNode` is not a widget.
- [x] Register `VirtualListView` / `VirtualTableView`; table columns are exposed through a compact string descriptor attribute.
- [x] Add descriptor for remaining P0 editor widget: `PropertyGrid`.

### P1 XML Coverage

- [x] Register `TabControl`, `PageView`, `Accordion`, `ExpandablePanel`, `Carousel`.
  - Done: navigation XML registration now covers `TabControl`, `PageView`, `Accordion`, `ExpandablePanel` and `Carousel` with descriptor metadata, selected-index/expanded/single-open attributes and property-child content/page/panel policies.
- [x] Register `DatePicker`, `ColorPicker`, `TreeListPicker`.
  - Done: picker XML registration now covers `DatePicker`, `ColorPicker` and `TreeListPicker` with descriptor metadata, ISO date values, color/ARGB mode attributes and simple path-list picker items.
- [x] Register `LoadingIndicator`, `Toast`, `NotificationView` if desired in user screens.
  - Done: feedback XML registration now covers `LoadingIndicator`, `Toast` and `NotificationView` with descriptor metadata, animation/state attributes and XML `open` support for visible toast/notification demos.
- [x] Register Minecraft widgets: item/block/entity preview, item picker, texture picker.
  - Done: Minecraft XML registration now exposes item/block/entity previews, item picker and texture picker descriptors for editor palette/inspector use; non-registry-backed texture picker XML materialization is covered by self-test, while item/block/entity registry materialization remains Minecraft-runtime-backed.
- [x] Register `CanvasWidget`, `Path` and custom draw limitations explicitly.
  - Done: display XML registration now covers `CanvasWidget` with explicit code-only draw callback limitations and `Path` with limited M/L/Q/C/Z path data, color, stroke and stroke-width metadata.
- [x] Register `CachedSubtreeWidget` only if cache control should be user-configurable.
  - Done: `CachedSubtreeWidget` is registered as an advanced performance wrapper with one content child, tint and target-options metadata for explicit user opt-in cache control.

### Descriptor Quality Checklist

- [x] Every XML-visible widget has category, display name and description.
  - Done: `XmlWidgetDescriptor` now normalizes missing descriptions into palette-safe fallback descriptions by display name/category, and XML self-test asserts built-in descriptors expose display/category/description coverage.
- [x] Every XML-visible attribute has category, display name, default value and description.
  - Done: `XmlAttributeDescriptor` now provides name-based fallback descriptions, shared style/layout descriptors include defaults and descriptions, and XML self-test asserts built-in attribute descriptors expose display/category/default/description coverage.
- [x] Attribute descriptors expose enough type information for the inspector field factory.
  - Done: `XmlAttributeDescriptor` exposes `XmlAttributeValueType`; annotated setters infer value types from Java parameter types, manual descriptors can override hints, and `PropertyFieldRow` uses descriptor hints before name/value fallback inference.
- [x] Property-child descriptors are available for content/item collections.
  - Done: built-in and annotation-backed container/content slots expose `XmlPropertyChildDescriptor` metadata; XML self-test now asserts all built-in property-child descriptors have display/category/description coverage.
- [x] Child policy errors are editor-friendly diagnostics, not only thrown exceptions.
  - Done: single-child property slots expose descriptor cardinality metadata; editor validation reports repeated single-content property elements and property elements with more than one direct widget child before runtime materialization.
- [x] User-facing editor can filter out unsafe/internal widget types.
  - Done: `WidgetPalette` hides `Editor` category descriptors by default for user-facing insertion, rejects hidden selections, and exposes explicit `includeInternalWidgets` opt-in for editor layout/demo palettes.

---

## Implementation Order Before UI Editor

Recommended order:

1. `TextArea` + tests.
2. `CodeEditor` thin XML wrapper or read-only Code View fallback.
3. `CommandManager` + `EditorCommand` + keybindings.
4. `MenuBar` backed by commands and `ContextMenu` popups.
5. `ToolBar` + `ToolButton` / `IconButton`.
6. `PropertyGrid` + property field factory.
7. `DesignCanvasOverlay` selection/handles.
8. `PaneVisibilityController` for docking show/hide/pin behavior.
9. Project/recent-project panel and persistence model.
10. XML registration pass for controls needed by the palette/inspector.

After these are in place, `XmlEditorScreen` can be implemented without large temporary UI hacks.

---

## Minimum Widget Definition Of Done

For every widget added specifically for the editor:

- [ ] Works as a normal retained UniGUI widget.
- [ ] Has keyboard and pointer behavior where relevant.
- [ ] Uses routed events / `EventSubscription` for public actions.
- [ ] Has renderer/state or clearly composes existing rendered widgets.
- [ ] Has XML registration if it should appear in user XML or editor XML demos.
- [ ] Has descriptor metadata good enough for `XmlWidgetInspector`.
- [ ] Has focused self-tests for behavior and basic layout.
- [ ] Has at least one demo usage in `TestCommands` or `UniGuiDemo`.

---

## What Not To Block On

These are useful, but should not block the first editor:

- [ ] Full visual scripting.
- [ ] Animation timeline.
- [ ] Advanced code editor features such as autocomplete, folding and syntax themes.
- [ ] Full asset browser with thumbnails for every Minecraft resource type.
- [ ] Perfect drag/drop reparenting in every layout container.
- [ ] Full WYSIWYG behavior for flex layouts.
