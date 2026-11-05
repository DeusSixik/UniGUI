# XAML, components, graphs and the editor

This page describes the data formats the in-game editor works on and the Java API behind it. Everything here is data:
documents are XAML, graphs are stored inside component documents and run by an interpreter, projects are folders that
pack into one archive.

## Documents

| Kind | Root | Folder in a project | Purpose |
|------|------|---------------------|---------|
| Screen | any widget (`<VBox>`…) | `screens/` | a UI screen |
| Component | `<Component x:Name="Tag">` | `components/` | reusable widget with a contract and behaviour |
| Struct | `<Struct x:Name="Name">` | `structs/` | typed record passed between graphs, params and events |
| Resource | any file | `textures/`, `fonts/`, `shaders/` | assets |

A project also has `pack.xaml`, the manifest (`id`, `version`, `name`, `description`).

## Components

```xml
<Component x:Name="ProductCard" xmlns:x="https://unigui.dev/xaml">
  <Component.Contract>
    <Param Name="Title" Type="string" Default="Iron Sword"/>
    <Var   Name="Seen"  Type="int"/>
    <Event Name="Buy"     Direction="Out" Args="title:string"/>
    <Event Name="Refresh" Direction="In"/>
  </Component.Contract>
  <VBox id="card">
    <Label text="{Param Title}"/>
    <Button text="Buy" onClick="{Raise Buy}"/>
  </VBox>
  <Component.Graph>
    <Node Id="refresh" Type="Event.Refresh" X="0"   Y="0"/>
    <Node Id="reset"   Type="Param.Set" Name="Title" value="Refreshed" X="220" Y="0"/>
    <Wire From="refresh.then" To="reset.exec"/>
  </Component.Graph>
</Component>
```

* **Param** is set from outside and read in the body with `{Param Name}`; types: `string`, `int`, `float`, `double`,
  `bool`, `color`, `texture`, `object`, `struct:Name`.
* **Var** is private state of the graph. The body can show it with `{Var Name}`; the property follows `Var.Set`.
* **Event**: `Direction="In"` is called from outside, `Direction="Out"` is sent to the parent. In the graph every event
  has both forms: `Event.Name` (shown as `On Name`) handles it, `Raise.Name` fires it. `On` of an outgoing event runs when
  the component raises it, so `{Raise Buy}` on a button can be handled in the same graph. `Raise` of an incoming event
  queues it for the component itself. `{Raise Name}` in the body works for outgoing events only.
  `Args="name:type, …"` declares arguments; `Coalesce="true"` merges incoming events within a frame.
* **Custom events** are declared by an `Event.Custom` node and stay inside the graph (`On Name`, `Raise Name`).
* In the editor a right click on an inspector row promotes the property to a variable or a parameter (`Promotions`): the
  current value becomes the default and the property is bound to the new entry. Send buttons and the event log work
  against the instance started by Play; the design preview never receives them.
* Java creates instances with `Xaml.component(id).create(Args)`, sets params with `instance.set(...)`, listens with
  `instance.onEvent(...)` and sends events with `instance.send(...)`.

### Graph node types

| Group | Types |
|-------|-------|
| Entry | `Event.Construct`, `Event.Destruct`, `Event.<Event>` |
| Flow | `Flow.Branch`, `Flow.Sequence` (`Count`), `Flow.Delay`, `Flow.CancelDelay` (`Target`) |
| Values | `Math.Add/Subtract/Multiply/Divide`, `Math.Compare` (`Op`), `Logic.And/Or/Not`, `Text.Concat`, `Value.Literal` (`ValueType`, `Value`) |
| State | `Param.Get/Set` (`Name`), `Var.Get/Set` (`Name`) |
| Output | `Raise.<Event>`, `Call.<function>` (registered with `GraphFunctions.register`) |
| Widgets | `Widget.Set` (`Target`, `Property`), `Widget.AddChild`, `Widget.Clear` |
| Components | `Component.Create`, `Dispatcher.Bind/Unbind`, `Event.Custom`, `Event.Call` |
| Structs | `Struct.Make`, `Struct.Break`, `Struct.With` (`Struct`, `Field`) |

Attributes named like an input pin are that pin's default value (`value="Refreshed"`). `GraphCompiler.compile` checks a
graph strictly; `GraphCompiler.check` is the lenient mode the editor uses while a graph is half built.

## Lists and templates

```xml
<VirtualListView items="{Binding catalog}" itemHeight="48">
  <VirtualListView.ItemTemplate>
    <ProductCard Title="{Item title}" key="{Item id}"/>
  </VirtualListView.ItemTemplate>
</VirtualListView>
```

`{Item}` is the record, `{Item field}` a field, `{Item product.title}` a field of a nested struct.

## Styles

```xml
<VBox.Resources>
  <Style x:Key="Primary" TargetType="Button">
    <Setter Property="text" Value="Go"/>
  </Style>
</VBox.Resources>
<Button Style="{StaticResource Primary}"/>
```

`BasedOn` chains styles. The validator checks every setter against the target type.

## Anchors

Anchors place a widget relative to its parent without flow layout.

* Child of a `StackPanel` or a cell of a `GridBox`: `horizontalAlignment` / `verticalAlignment` (`start`, `center`, `end`, `stretch`) plus
  `margin` as the offset.
* `position="absolute"` in any parent: `left`, `right`, `top`, `bottom` insets (`left` and `right` together stretch).
  Centering needs a stack slot.

`Anchoring.anchor` and `Anchoring.move` are the editing API; moving only changes offsets, so the anchor stays.

## Projects and packs

`Project` is the editor-side model of a folder with the layout above.

```java
Project project = Project.open(folder);          // or Project.create(manifest) / Project.of(pack)
EditorSession screen = project.session("screens/main.xaml");   // one shared history per document
project.createComponent("Card");
project.rename("components/card.xaml", "ProductCard");         // updates tags in every document
project.deleteIfUnused("structs/product.xaml");                // refuses while something refers to it
project.check();                                               // validates the whole project
project.install();                                             // components and structs available at runtime
project.save(folder);                                          // or project.toArchive() for a pack
project.refreshFrom(folder);                                   // pick up external edits; unsaved work is kept
project.undo();                                                // undo create/delete/rename/resource/manifest steps
```

Structural steps (create, delete, rename of files, struct and field renames, resources, manifest) have their own undo
history of up to `Project.HISTORY_LIMIT` steps; `undo()` restores the whole project as it was before the step and
closes the sessions of changed files. Edits inside a document stay in that document's own history.

`AssetPack.read(bytes)` opens an archive with size limits and path checks, `pack.install()` registers its structs and
components, `pack.screen(name, options, decoder)` loads a screen with the pack's textures.

## Config folder and the pack library

The library keeps everything it needs in one folder inside the game config directory (the backend passes the path):

```
config/unigui/
    settings.xaml     UI settings (UiSettings)
    packs/            ready Asset Pack files (*.zip): the editor exports here, the game reads from here
    projects/         editor projects as folders
```

```java
PackLibrary library = PackLibrary.open(configDir.resolve("unigui"))
        .bundled(MyMod.class.getClassLoader(), "assets/mymod/unigui/default.zip");   // default assets shipped in the mod jar
PackLibrary.Live live = new PackLibrary.Live(library);   // installs every pack
// every second or two, from the game tick:
live.poll();                                              // reinstalls only if files in packs/ changed
UiSettings settings = library.settings();
float scale = settings.getFloat("scale", 1.0f);
```

* Packs are installed in this order: packs bundled in the mod first, then packs from `packs/`. For one pack id the higher
  version wins; at equal versions the player's file replaces the bundled one. Replacements and broken files are listed in
  `Installed.problems()` and never stop the other packs.
* `library.export(project)` writes `packs/<id>.zip` through a temporary file, so a running game never reads a half
  written pack. `ProjectEditor.shell(project, library)` does this for its Export button and saves the project folder
  under `projects/<id>`.
* `settings.xaml` stores only what the player changed; missing keys give the default passed in the getter, and a corrupt
  file means defaults plus `UiSettings.problem()`.
* `Project.poll(folder)` is the same cheap check for an editor project folder edited by an outside tool.

## Edit API

All edits go through `EditorSession.apply(edit)` and land in one undo history.

| Area | Class |
|------|-------|
| widget properties, children | `Edits` |
| several edits as one undo step | `EditorSession.transaction(label, body)` |
| contract (params, vars, events) | `ContractEdits` |
| graph (nodes, wires, values) | `GraphEdits`, node catalog `NodeCatalog` |
| rename with references | `Renames` (params, vars, events), `GraphEdits.renameNode`, `Project.rename` |
| structs | `StructEdits` |
| styles | `StyleEdits` |
| list templates | `TemplateEdits` |
| anchors | `Anchoring` |
| typed value input | `ValueEditing` (color, number, toggle, choice, insets, size, resource); `editorForType` for contract defaults and graph pins |

A rejected edit never changes the document; the reason is in `session.diagnostics(EditorSession.Channel.EDIT)`.

## Preview and debugging

* `Preview` shows a screen, `ComponentPreview` shows a component with sample param values and an event log. Both keep
  the last good tree while the document has errors.
* `ComponentPreview.trace()` records every graph run (`GraphTrace`): nodes in order, their outputs, the flow they took
  and the node that failed. The editor steps through the last run.
* `ComponentPreview.debugger()` (`GraphDebugger`) sets breakpoints on flow nodes. A run stops **before** the node, the
  handler returns and the UI keeps running; `resume()` continues to the next breakpoint, `step()` runs one node and stops
  before the next. While a run is stopped the graph's delays are frozen and new events wait in a queue. Pure nodes cannot
  be stopped (they run inside the step of the node that needs them) and nested runs started by custom events do not
  check breakpoints. Breakpoints belong to the editor, so they survive preview rebuilds and disappear with their node.
  In the Graph panel: Breakpoint button or F9 on the selected nodes, Continue, Step node.

## Editor module

`dev.sixik.unigui.editor` is a separate Gradle module.

* `EditorShell` — docking shell with menus, toolbar, layouts and shortcuts; panels implement `EditorPanel`.
* `UiEditor.shell(document)` — screen editor; `ComponentEditor.shell(document)` — component editor;
  `ProjectEditor.shell(project, saveFolder, exportFile)` — everything plus project, struct and style panels.
* Look: `editor.style.EditorStyles` is the Unity dark theme (palette, row height 18, inputs, buttons, combo boxes,
  checkboxes, tree, context menus, scrollbars); `DefaultEditorTheme` uses it for the shell, and `EditorStyles.apply(root)`
  restyles a panel subtree after every rebuild, so the look lives in one place.
* Panels: Project, Hierarchy (a `TreeView` like the Unity scene hierarchy: expandable rows, drag and drop, right-click
  menu, scrolls to the selection), Palette (grouped list of widget types, click or drag), Inspector (groups, typed
  editors, anchor picker), Viewport (the interface fills the panel; Play runs an independent copy built from the current
  document, so clicks and graphs in the copy never touch the edited document or its preview), XAML, Problems,
  Contract, Graph (built-in node menu, breakpoints), Struct, Styles.
