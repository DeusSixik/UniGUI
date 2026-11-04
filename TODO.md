# Core
- [ ] Auto test framework

# Widgets Style
- [ ] Rework the library's base visual style to look more modern

# Widgets API
- [ ] Add Signal support for widgets
- [ ] In-world widget rendering
- [ ] 3D widget rendering (like [Noesis GUI](https://www.noesisengine.com/xamltoy/8ab948fb3b5a3edaaee5bb6cd7017a8b))
- [ ] Support for rendering 3D elements inside UI
- [ ] Better style pack & style API. It should allow building most things without writing custom render code for a widget
- [x] Bring the XAML API out of its Legacy state: own parser, lazy schema, markup extensions (`{Binding}`, `{StaticResource}`, `{Command}`), `x:Name`, resources, diagnostics with source positions
- [x] Finish XAML action bindings: command parameters (`{Command name, Parameter=...}`) and TwoWay bindings (`Mode=TwoWay` for text, checked, slider value)

# Serialization
- [ ] Rework and extend widget serialization. Design it so that any serialization language can be plugged in: XAML (priority), Java, JS (KubeJS), ZS (CraftTweaker)
  - Done: language-neutral markup tree (`UiDocument`), `UiFormat` SPI, XAML format, snapshotter (live widgets back to a document)
  - Todo: Java, JS (KubeJS) and ZS (CraftTweaker) formats

# Asset Packs
- [x] Assets Pack mechanism: UI as serializable data packaged into a single archive containing assets, fonts, shaders, and the UI code itself (like Unity AssetBundle)
  - Done in core: archive format (`pack.xaml` manifest, structs, components, screens, textures), safe reading with limits, deterministic writer, installation that can be undone, pack texture resolver
  - Done: the editor-side project model (`Project`): files by kind, create/rename/delete with tag updates, shared edit sessions, whole-project check, folder and pack round trip, live install
  - Todo: export from the in-game editor (UI over `Project.toPack()` / `Project.save()`)

# Tooling
- [ ] In-game UI editor, in the spirit of the Unity / Unreal Engine editors (UMG Designer, UI Toolkit Builder), running directly inside Minecraft ***(Required (Assets Pack & XAML API))***
  - Viewport: WYSIWYG canvas with selection, move/resize/anchor gizmos, snapping and guides, zoom/pan, and resolution/GUI-scale presets
  - Hierarchy panel: widget tree with drag & drop reparenting, reordering, visibility/lock toggles, and search
  - Palette: list of all registered widgets, drag onto the canvas to instantiate
  - Inspector (Details panel): edit widget properties and layout style (`LayoutStyle`), with type-aware editors (color, font, enum, number, texture picker) and per-property reset to default
  - Style editor: edit style packs and the style API visually, with live preview
  - Bindings editor: wire up Signals, action bindings, and events without code
  - Asset browser: manage textures, fonts, and shaders from the Assets Pack
  - Undo/redo: command-based history for all edits
  - Live preview and hot reload: edits apply instantly in the running game, plus a play mode to test interactions without leaving the editor
  - Serialization integration: open and save UI through the serialization layer (XAML first), and export to an Assets Pack
  - Done (mechanisms, covered by tests): grouped edit transactions, rename refactoring with reference updates (params, variables, events, graph nodes, component tags), struct and style edits, graph edits with node catalog, debug trace of graph runs, component contract edits and preview
  - Done (first UI pass, to be reworked): panels for hierarchy tree, tile palette, inspector groups, framed viewport with Play mode, contract, graph, drag and drop, anchors for StackPanel children
  - Todo (UI): project/asset browser panel, struct and style editors, typed value editors (color, texture, font), anchors for other containers, rename dialogs
  - Dogfooding: the editor is built with UniGUI itself, which doubles as a large-scale test of the library

# Text & Layout
- [ ] Text measurement does not match rendering. `TextWidget.measure()` (and ~16 other widgets in `core/.../widgets`) calls `TextEngine.measureTextHeight/measureLineWidth/wrapLines` without the backend font and falls back to a default metric (`fallbackLineHeight`, 10px per line at size 10), while rendering uses `backend.defaultTextFace()` (12.6px per line in the test, +26%). As a result, multiline `Label`s and auto-height containers are measured too short: in a `ScrollView` the bottom of the content can't be scrolled to, and the last line is clipped. `LayoutContext` only knows `availableWidth/Height` and has no font.
  - Solution: pass the backend font into measurement (via `LayoutContext` or `UIContext`) instead of the fallback metric. A global default face for measurement is simpler, but it is global state.
  - Risk: the size of everything measured by text will change (buttons without an explicit height, `TreeView` rows, `Toast`, input fields), and layouts in the demos and `TestMod` may shift. Tests with exact numbers (`BasicControlsSelfTest`) will likely fail. First run the full test suite with a trial change and see how many fail.
  - Current workaround: set an explicit font on multiline text, `label.font(Fonts.defaultFace(), size)` (done for code in `DemoCategory.samplePanel`). Add a warning about this to the `TextWidget` wiki/docs.

# Render API
- [ ] `renderToTexture()` method for widgets, like in Unigine: renders a widget or a widget tree into a texture for use in shaders, etc.

# Layer API (Minecraft Integration)
- [ ] Screen overwrite: a mechanism to fully replace a Minecraft screen with a UniGUI screen

# Wiki
- [ ] Add more examples
- [ ] Add a `Get started` section