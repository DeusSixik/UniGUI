# XML UI Editor SPEC

## Context

UniGUI already has an XML widget loader, editable XML document model, widget descriptor metadata and an IDE-style docking system. This makes it realistic to build a Unity-like UI editor where users can edit XML, visually inspect layout, change widget properties and run the UI in an interactive test mode.

The long-term target is an editor that can be used by mods such as SDM Shop, where end users can customize screens, rendering, behavior, bindings and later script-driven actions without recompiling Java code.

Before implementing the editor screen itself, complete the widget readiness audit in `XML_UI_EDITOR_WIDGET_GAPS.md`. The editor should not start with temporary ad-hoc controls for menus, toolbars, properties, code editing or canvas handles.

Core principle:

```text
XML is the source of truth.
The editor edits XML.
Runtime View materializes a real widget tree from the current XML snapshot.
```

The editor must not become a second source format. Drag/drop, inspector fields and code edits all produce changes to the same XML document.

---

## Goals

- Provide a Unity-like editor shell with dockable panes.
- Support simultaneous Design View, Code View, Hierarchy, Properties, Assets and Console panes.
- Keep XML as the authoritative editable source.
- Let users select a widget in the canvas or hierarchy and edit its XML attributes in Properties.
- Support safe drag/drop and resize operations for layout attributes.
- Provide Runtime View / Play Mode where the UI receives real input for testing.
- Prepare for Java-backed and script-backed bindings/actions.
- Keep editor state, runtime state and saved XML state clearly separated.
- Make the first MVP useful before a full visual drag/drop system exists.

## Non-Goals For First MVP

- Do not implement a full Unity clone in the first slice.
- Do not add arbitrary unsandboxed scripting.
- Do not save Runtime View mutated widget state back into XML automatically.
- Do not require every widget to support visual handles immediately.
- Do not make XML loading depend on editor-only classes.
- Do not build a complex text editor before the rest of the editor loop is proven.
- Do not hide diagnostics behind exceptions only; editor-facing diagnostics must be visible in the UI.

---

## Target Editor Layout

Default docking layout should stay calm and avoid overwhelming the user. The first screen should prioritize the active canvas/editor and keep secondary panes available through the top toolbar.

```text
+----------------------------- Toolbar -----------------------------+
| File | Edit | View | Project | Run | Layout | Diagnostics          |
+-------------------------------------------------------------------+
|                                                                   |
|                    Design / Runtime / Code Area                   |
|                                                                   |
+-------------------------------------------------------------------+
| Status / diagnostics summary                                      |
+-------------------------------------------------------------------+
```

All panes should be backed by `DockingRoot` so users can rearrange the editor workspace, but most secondary panes should be hidden or collapsed by default.

### Toolbar / Window Management

The editor should behave like common desktop tools and game editors: major commands live in the top toolbar/menu, while optional windows can be opened, hidden, pinned or restored as needed.

Required toolbar groups:

- `File` - `new_project`, `open_project`, `save_project`, `save_project_as`, `last_projects`.
- `Edit` - undo, redo, delete, duplicate, format XML.
- `View` - toggle Hierarchy, Properties, Code View, Palette, Assets, Bindings, Diagnostics, Console.
- `Project` - project settings, resource root, XML files, SDM Shop integration later.
- `Run` - Design Mode, Runtime View / Play, Stop, reload preview.
- `Layout` - reset workspace layout, save workspace layout, compact layout.

Window visibility rules:

- The default workspace should show only the main Design/Code area and a small status/diagnostics strip.
- Hierarchy and Properties are important, but can start collapsed behind toolbar toggles on small screens.
- Diagnostics should open automatically only when an error occurs.
- Console, Bindings, Assets and Palette should remain hidden until requested.
- Any hidden pane can be restored from the `View` menu.
- Users should be able to pin frequently used panes.
- Workspace layout is a user preference and must not modify the edited XML.

Suggested expanded layout after the user opens panes:

```text
+----------------------------- Toolbar -----------------------------+
| Hierarchy |          Design / Runtime View          | Properties  |
|-----------+-----------------------------------------+-------------|
| Palette / Assets       | Diagnostics / Console / Bindings         |
+-------------------------------------------------------------------+
```

Recommended panes:

- `Scene` / `Design View` - visual canvas with selection overlay and layout handles.
- `Runtime View` - interactive play mode for the current XML snapshot.
- `Code View` - XML source editor.
- `Hierarchy` - source XML node tree.
- `Properties` - Unity-like inspector for selected XML element.
- `Palette` - available widgets from `XmlWidgetRegistry` descriptors.
- `Assets` - textures, item ids, resources and host-mod assets registered through generic providers.
- `Bindings` - binding/action list and status.
- `Diagnostics` - parse, validation, loading and binding errors.
- `Console` - editor operation log.

---

## Editor Modes

### Design Mode

Design Mode is for editing source XML.

Rules:

- Pointer clicks select XML nodes, not runtime button actions.
- Dragging selected visual handles changes XML layout attributes.
- Inspector edits write XML attributes.
- Hierarchy reorder writes XML child order.
- Preview may rebuild runtime widgets often, but runtime interactions should be suppressed or captured by the editor.
- Selection is stored as `XmlWidgetNodePath`, not as direct widget identity.

### Code Mode

Code Mode is for direct XML editing.

Rules:

- Code edits update the editor XML document after parse succeeds.
- If parse fails, keep the previous valid document for preview and show diagnostics.
- Formatting should preserve meaningful comments/text where possible.
- Inspector/hierarchy/design panes follow the latest valid document.

### Runtime View / Play Mode

Runtime View is the equivalent of Unity Play Mode.

Rules:

- Create a new runtime widget tree from the current XML snapshot.
- Use the configured `UIScaleProvider` / XML `<Screen>` scale configuration.
- Send pointer, keyboard, focus, hover and scroll input to the runtime UI.
- Enable Java actions, bindings and later script actions.
- Keep runtime mutations isolated from the editor document.
- On exit, dispose the runtime tree and return to the editable XML document.
- If bindings/actions fail, show diagnostics without corrupting XML.

---

## Core Architecture

```text
XmlEditorScreen
  -> DockingRoot workspace
  -> XmlEditorSession
       -> XmlWidgetDocument current valid document
       -> raw XML text buffer
       -> XmlWidgetSelectionModel
       -> dirty flag / undo stack / redo stack
       -> editor mode: DESIGN, CODE, RUNTIME
       -> diagnostics models
       -> registry / options / prefab catalog / asset catalog
  -> Pane widgets
       -> XmlHierarchyPanel
       -> XmlPropertiesPanel
       -> XmlDesignCanvas
       -> XmlCodeEditorPane
       -> XmlRuntimeViewPane
       -> XmlPalettePanel
       -> XmlDiagnosticsPanel
```

`XmlEditorSession` should be the shared state owner. Individual panes should be thin views/controllers over the session.

### Required Session Responsibilities

- Own the current valid `XmlWidgetDocument`.
- Own the raw text buffer used by Code View.
- Track `XmlWidgetSelectionModel`.
- Apply `XmlWidgetDocumentEdit` commands.
- Maintain undo/redo stacks.
- Rebuild hierarchy and inspector snapshots after edits.
- Rebuild preview/runtime widget trees when needed.
- Merge diagnostics from parse, validate, load, bindings and scripts.
- Track dirty state separately from runtime state.
- Save/load XML through a file/resource abstraction.

### Pane Synchronization Rules

- Selecting in Hierarchy updates Properties and Design View overlay.
- Selecting in Design View updates Hierarchy and Properties.
- Editing Properties updates XML, Code View and preview.
- Editing Code View updates XML document only after successful parse.
- Entering Runtime View freezes editor selection editing until Play Mode exits.
- Diagnostics always reflect the latest attempted operation, not only the last successful one.

---

## Existing Building Blocks

These pieces already exist and should be reused instead of replaced.

### XML Runtime

- [x] `XMLWidget` creates real widget trees from XML.
- [x] `XmlWidgetRegistry` provides widget descriptors and metadata.
- [x] `XmlWidgetDescriptor` / `XmlAttributeDescriptor` can power palettes and inspector fields.
- [x] `XmlWidgetOptions` exists for loader options.
- [x] `XmlWidgetLoadException` gives clear runtime loading failures.
- [x] `XmlWidgetScreen` supports XML screen-level config.
- [x] `UIScaleProvider` integration exists through XML screen config.

### XML Editor Model

- [x] `XmlWidgetDocument` parses and serializes editable XML.
- [x] `XmlWidgetDocumentValidator` validates source XML against descriptors.
- [x] `XmlWidgetDocumentResult` carries document + diagnostics.
- [x] `XmlWidgetDiagnostic` exists for editor diagnostics.
- [x] `XmlWidgetNodePath` provides stable source node identity.
- [x] `XmlWidgetSelectionModel` tracks editor selection by source path.
- [x] `XmlWidgetHierarchy` provides a flat tree model for the hierarchy pane.
- [x] `XmlWidgetInspector` provides metadata for a selected XML element.
- [x] `XmlWidgetDocumentEdits` provides basic undoable XML edit commands.
- [x] `XmlWidgetRuntimeSerializer` exists for runtime-to-XML direction.
- [x] `XmlWidgetLayoutHandles` can modify numeric `x`, `y`, `width`, `height` attrs.

### Preview / Diagnostics / Assets

- [x] `XmlWidgetHotReloadPreview` rebuilds XML preview and keeps last valid content.
- [x] `XmlWidgetHotReloadSource` abstracts reloadable XML sources.
- [x] `XmlWidgetDiagnosticsModel` exists for diagnostics state.
- [x] `XmlWidgetDiagnosticsPanel` exists as an editor-facing diagnostics concept.
- [x] `XmlWidgetAssetCatalog` / `XmlWidgetAssetPickerModel` / `XmlWidgetAssetPickerPanel` exist for asset selection direction.

### Docking / Editor Shell

- [x] `DockingRoot` supports IDE-style pane docking.
- [x] `DockingManager` supports pane tree mutation, selection and snapshots.
- [x] `DockLayoutSnapshot` / `DockLayoutSnapshotCodec` support layout persistence direction.
- [x] `DockPane` supports document/tool panes, dirty and pinned state.
- [x] `/unigui docking` demo already shows a docking editor-style shell.

### Binding / Actions Direction

- [x] `XmlBinding`, `XmlBindingContext`, `XmlBindingDiagnosticsModel` exist as binding groundwork.
- [x] `XmlCommand` and `XmlCommandRegistry` exist as a command/action direction.
- [x] Event attributes such as `onClick="shop.buySelected"` need loader/runtime wiring.
- [ ] Script-backed actions need a sandboxed execution model.

---

## Missing Components Checklist

Precondition:

- [ ] Complete the P0 widget gaps from `XML_UI_EDITOR_WIDGET_GAPS.md` or explicitly choose a scoped-down MVP alternative.

### Phase 1 - Editor Session MVP

- [x] Add `XmlEditorMode` enum: `DESIGN`, `CODE`, `RUNTIME`.
- [x] Add `XmlEditorSession` as the single editor state owner.
- [x] Add source abstraction for file/resource/project XML documents.
- [x] Add session-level diagnostics merge: parse, validation, loading, binding, script.
- [x] Add undo/redo stack over `XmlWidgetDocumentEdit`.
- [x] Add dirty state and save/revert operations.
- [x] Add session events/listeners so panes can refresh without tight coupling.
- [x] Add tests for selection survival after document edits.
- [x] Add tests for parse failure preserving last valid document.

### Phase 2 - Docking Editor Screen

- [x] Add `XmlEditorScreen` or dev-only `XmlEditorDemoScreen`.
- [x] Use `DockingRoot` as the workspace root.
- [x] Add a compact default workspace with Design/Code area visible and secondary panes hidden or collapsed.
- [x] Add default panes: Design, Code, Hierarchy, Properties, Diagnostics.
- [x] Add toolbar/menu groups: File, Edit, View, Project, Run, Layout, Diagnostics.
- [x] Add project commands: `new_project`, `open_project`, `save_project`, `save_project_as`, `last_projects`.
- [x] Add View menu toggles for Hierarchy, Properties, Code View, Palette, Assets, Bindings, Diagnostics and Console.
- [x] Add pane pin/unpin or collapsed-state behavior so the editor does not overload small screens.
- [x] Add auto-open Diagnostics when parse/load/runtime errors appear.
- [x] Add quick actions: Save, Revert, Format XML, Design, Run, Stop, Reload.
- [x] Persist/restore editor docking layout through `DockLayoutSnapshotCodec`.
- [x] Add command entry point, for example `/unigui xmleditor`.
- [x] Keep existing `/unigui docking` demo separate or convert it into editor demo deliberately.

### Phase 3 - Hierarchy Pane

- [x] Implement `XmlHierarchyPanel` widget around `XmlWidgetHierarchy`.
- [x] Display element label as `Tag#id` where available.
- [x] Select XML node by `XmlWidgetNodePath`.
- [x] Support rename/id edit through inspector, not inline at first.
- [x] Support add child from palette.
- [x] Support delete selected node.
- [x] Support move up/down within parent.
- [ ] Later: support drag/drop reorder/reparent with child policy validation.

### Phase 4 - Properties Pane

- [x] Implement `XmlPropertiesPanel` widget around `XmlWidgetInspector`.
- [x] Group fields by descriptor category.
- [x] Render editor field type from parser/descriptor metadata where possible.
- [x] Fallback unknown attrs to string fields.
- [x] Apply edits through `XmlWidgetDocumentEdits.setAttribute(...)`.
- [x] Add support for removing/resetting optional attributes.
- [x] Add support for adding available descriptor attributes.
- [x] Validate values before committing when parser metadata is available.
- [x] Add object pickers for assets, textures, items and colors.

### Phase 5 - Code View

- [x] Decide first implementation: simple multiline text area or external file hot-reload.
- [x] Add or promote a multiline `TextArea` / `CodeEditor` widget.
- [x] Support XML text input, cursor, selection, clipboard and scroll.
- [x] Add basic line numbers and parse diagnostics line/column display.
- [x] Add format XML action using `XmlWidgetDocument.toXmlString(...)`.
- [x] Keep previous valid preview on syntax errors.
- [ ] Later: syntax highlighting and autocomplete from `XmlWidgetRegistry` descriptors.

### Phase 6 - Design Canvas

- [x] Implement `XmlDesignCanvas` host for visual preview plus editor overlay.
- [x] Rebuild preview from current valid XML document.
- [x] Map runtime widgets back to `XmlWidgetNodePath`.
- [x] Select widget under cursor without triggering runtime widget actions.
- [x] Draw selected widget bounds.
- [x] Draw hover bounds.
- [ ] Draw margin/padding/layout debug overlays.
- [x] Use `XmlWidgetLayoutHandles` for move/resize where numeric frame attrs are available.
- [x] Add drag/drop insertion from palette.
- [x] Validate child insertion through widget child policy/descriptor metadata.
- [ ] Later: snapping, grid, guides and align/distribute tools.

### Phase 7 - Runtime View / Play Mode

- [x] Implement `XmlRuntimeViewPane` or runtime overlay mode.
- [x] Enter Runtime View from current XML snapshot.
- [x] Build runtime root through `XMLWidget.createScreen(...)` when XML root is `<Screen>`.
- [x] Apply `UIScaleProvider` exactly as runtime screens do.
- [x] Forward pointer, keyboard, scroll, focus and text input to runtime UI.
- [x] Disable design selection/handles while Runtime View is active.
- [ ] Show runtime binding/action diagnostics live.
- [x] Stop Runtime View and dispose runtime tree cleanly.
- [x] Confirm no runtime state is written back into XML without explicit user action.

### Phase 8 - Palette / Assets

- [x] Build widget palette from `XmlWidgetRegistry` descriptors.
- [x] Group widgets by category.
- [x] Insert selected widget into hierarchy/design canvas.
- [x] Support prefab/templates from `XmlWidgetTemplateCatalog` / `XmlWidgetPrefabCatalog`.
- [x] Use asset picker panels (`XmlWidgetAssetPickerPanel` / `AssetBrowserPanel`) for texture/resource fields.
- [x] Add mod-registerable asset providers for host-specific resource/icon catalogs; SDM Shop can plug into this API without editor hardcode.

### Phase 9 - Bindings / Actions / Scripts

- [ ] Define XML binding syntax for values: `{shop.selected.priceText}`.
- [x] Define XML action syntax for events: `onClick="shop.buySelected"`.
- [x] Wire event attributes to `XmlCommandRegistry`.
- [ ] Add binding diagnostics for missing paths, wrong types and failed conversion.
- [ ] Add mock binding context for editor preview.
- [ ] Add Java binding provider API for mods.
- [ ] Add script binding/action provider API.
- [ ] Add script sandbox with whitelisted APIs only.
- [ ] Add script timeout/error handling.
- [ ] Add user-visible permission model for scripts if exposed to end users.

### Phase 10 - Persistence / Packaging

- [ ] Define `XmlEditorProject` model for user projects.
- [ ] Support `new_project` with a minimal screen XML template.
- [ ] Support `open_project` from a project file/directory.
- [ ] Support `save_project` and `save_project_as`.
- [ ] Track `last_projects` for quick reopen from the toolbar.
- [ ] Save XML files to mod config/project directories.
- [ ] Save editor docking layout per user/profile.
- [ ] Save recent files and last selected resource.
- [ ] Support importing XML resources from mod assets.
- [ ] Support exporting user-customized XML for SDM Shop.
- [ ] Add version/migration metadata if XML schema evolves.
- [ ] Add backup/recovery for failed saves.

---

## Runtime Binding Proposal

Value binding example:

```xml
<Label id="price" text="{shop.selected.priceText}" />
```

Action binding example:

```xml
<Button id="buyButton"
        text="Buy"
        enabled="{shop.canBuy}"
        onClick="shop.buySelected" />
```

Rules:

- Binding paths are data/action ids, not raw Java method calls.
- Java can register providers for namespaces like `shop`.
- Scripts can later register providers/actions under allowed namespaces.
- Editor Runtime View can use real providers or mock providers.
- Missing binding paths should produce diagnostics, not silent failures.
- Actions must be explicit commands through `XmlCommandRegistry` or equivalent.

---

## Source Of Truth Rules

- XML text is the saved artifact.
- `XmlWidgetDocument` is the latest valid parsed document.
- Runtime widget tree is disposable output.
- Design overlay state is editor-only.
- Docking layout is user preference, not screen XML.
- Runtime View state is temporary unless an explicit export/apply operation exists.

This prevents confusing cases where dragging a runtime widget changes Java object state but not the XML that will be saved.

---

## Selection And Node Mapping

Editor selection should use `XmlWidgetNodePath`.

Needed mapping:

```text
XmlWidgetNodePath -> XML element -> runtime widget -> rendered bounds
```

The loader/design preview should attach editor metadata during preview creation, for example:

- widget id;
- XML node path;
- XML tag name;
- source line/column;
- descriptor reference.

Open design decision:

- [ ] Decide whether editor metadata lives in widget metadata, side maps, or a dedicated preview build result.

Preferred first implementation:

- use a side map owned by `XmlDesignCanvas` / `XmlEditorSession` so runtime widgets do not permanently depend on editor classes.

---

## Drag And Drop Rules

Hierarchy drag/drop:

- Reorder children through `XmlWidgetDocumentEdits.moveChild(...)`.
- Reparent only after child policy validation.
- Preserve comments/text nodes where possible.

Palette drag/drop:

- Create an `XmlWidgetElement` from descriptor/template.
- Insert into selected parent or drop target.
- Add required/default attributes only when they are meaningful.

Canvas drag/resize:

- Prefer layout-aware edits over absolute positioning.
- Only use `x`, `y`, `width`, `height` handles when the selected element supports or already uses those attributes.
- Do not force absolute layout onto flex/linear containers unless the user explicitly switches layout mode.
- Show diagnostics when a widget cannot be visually moved/resized.

---

## Safety For User-Customizable Mods

For SDM Shop-style user editing, scripts and bindings need a safety boundary.

- [ ] No raw Java reflection from XML or scripts.
- [ ] No filesystem/network access from user scripts unless explicitly allowed.
- [ ] Commands must be registered by the host mod.
- [ ] Data bindings must be read-only by default.
- [ ] Mutating actions must go through named commands.
- [ ] Runtime View must surface script errors and failed bindings.
- [ ] User XML should be validated before being used in real gameplay screens.
- [ ] Host mods should be able to disable dangerous widgets/attributes in user-facing editors.

---

## Definition Of Done For MVP

The first useful editor is done when:

- [ ] `/unigui xmleditor` opens a docking editor screen.
- [ ] The default workspace is compact and does not show every pane at once.
- [ ] Top toolbar exposes project actions: new, open, save and last projects.
- [ ] View menu can show/hide Hierarchy, Properties, Code View, Palette, Assets, Bindings, Diagnostics and Console.
- [ ] Editor can load one XML resource or file.
- [ ] Hierarchy shows XML nodes.
- [ ] Selecting a hierarchy node updates Properties.
- [ ] Properties can change at least simple attributes like `id`, `text`, `width`, `height`, `spacing`.
- [ ] Changes update the XML document and preview.
- [ ] Code View can show the current XML source.
- [ ] Diagnostics show parse/validation/load errors.
- [x] Run button enters Runtime View.
- [x] Stop button exits Runtime View and restores Design Mode.
- [x] Runtime View uses real input and scale provider config.
- [ ] Last valid preview remains visible after invalid XML edits.
- [ ] Basic tests cover parse/edit/rebuild/selection behavior.

---

## Suggested Implementation Order

1. `XmlEditorSession` with document, text, selection, diagnostics and undo/redo.
2. Dev-only `XmlEditorScreen` using `DockingRoot`.
3. `XmlHierarchyPanel` and selection synchronization.
4. `XmlPropertiesPanel` with string/boolean/number fields.
5. Preview pane from current valid XML document.
6. Runtime View toggle using current XML snapshot.
7. Code View as read-only XML first, editable multiline later.
8. Canvas selection overlay and simple numeric move/resize handles.
9. Palette insert and hierarchy reorder.
10. Binding/action diagnostics and SDM Shop integration points.

---

## Open Questions

- [ ] Should the first editor live in `common/src/main/java/dev/sixik/unigui/api/xml/editor` or under test/demo packages first?
- [ ] Should editor UI be part of public API, dev tooling, or a separate optional module?
- [ ] How should runtime widgets expose rendered bounds for editor selection?
- [ ] How much source formatting must be preserved after inspector edits?
- [ ] Should user-facing SDM Shop editor expose raw XML or hide it behind advanced mode?
- [ ] Which scripting engine is acceptable for Minecraft mod distribution and sandboxing?
- [ ] How should host mods restrict widget types/attributes for end-user customization?
