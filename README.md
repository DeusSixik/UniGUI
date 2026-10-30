<p align="center">
  <img src="./.idea/icon.png" alt="UniGUI logo" width="160">
</p>

<h1 align="center">UniGUI</h1>

<p align="center">
  A retained UI toolkit for Minecraft mods.<br>
  Build screens as a widget tree. The Minecraft renderer is just one backend.
</p>

<p align="center">
  <a href="https://jitpack.io/#DeusSixik/UniGui">
    <img src="https://jitpack.io/v/DeusSixik/UniGui.svg" alt="JitPack">
  </a>
  <img src="https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1-242629?style=flat" alt="Minecraft versions">
  <img src="https://img.shields.io/badge/Fabric%20%7C%20Forge%20%7C%20NeoForge-supported-5CA424?style=flat" alt="Loaders">
  <img src="https://img.shields.io/badge/license-LGPL--3.0-242629?style=flat" alt="License">
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/uni-gui">
    <img src="https://img.shields.io/curseforge/dt/1649763?logo=curseforge&label=&suffix=%20&style=flat&color=242629&labelColor=F16436&logoColor=1C1C1C" alt="CurseForge">
  </a>
</p>

---

Vanilla `Screen` drawing is immediate: every widget paints itself, every frame, straight into the Minecraft GUI. UniGUI does the opposite.

You compose a tree of retained widgets — layout, input, focus and animation live on the tree. Once a frame is ready, the tree emits a list of draw commands. Those commands are grouped into batch buckets and flushed by a backend. The public API never imports a Minecraft renderer type, so a different host can consume the same list without rewriting widgets.

Today the shipping backend is Minecraft. `MinecraftWidgetScreen` is a normal `Screen`: it owns the widget tree, routes mouse and keyboard, and can follow the vanilla GUI scale or ignore it. Static screens can skip redraws. Animated ones, shader canvases and editor previews stay on a continuous policy.

```java
VBox page = new VBox();
page.spacing(8.0f);

Label title = new Label("Recipe Machine");
Button craft = new Button("Craft");
craft.onClick(event -> title.text("Queued"));

page.addChild(title);
page.addChild(craft);

MinecraftWidgetScreen screen = new MinecraftWidgetScreen(Component.literal("Craft"), page);
screen.independentUiScale(2.0f);
screen.renderPolicy(UiRenderPolicy.vsync());
Minecraft.getInstance().setScreen(screen);
```

## What you get

**Layout.** `HBox`, `VBox`, `GridBox`, `WrapPanel`, `DockPanel`, `SplitPanel`, `ScrollView`, `StackPanel`. Flex grow and shrink, alignment, margins, docking.

**Controls.** Buttons, toggles, checkboxes (including tri-state), sliders, text fields, password and search fields, number fields, combo boxes, radio groups, date, color and duration pickers.

**Text.** Vanilla font faces and an SDF face in the same line. `RichText` runs carry their own font, size and color, and can embed custom inline draws. Overflow can clip, shrink to fit, or marquee on hover.

**Data.** `VirtualListView` and `VirtualTableView` build only the visible rows. Selection is single or multiple. `Chart` and `Sparkline` cover the small visualizations.

**Overlays.** Popups, context menus, tooltips, toasts and floating windows sit on an `OverlayLayer` and do not push the layout underneath them.

**Bigger surfaces.**

| Widget                                            | What it is                                                 |
|---------------------------------------------------|------------------------------------------------------------|
| `TabControl`, `Accordion`, `Carousel`, `TreeView` | Navigation that keeps its pages instead of rebuilding them |
| `DockingRoot`                                     | Drag-and-drop panes, splits and layout snapshots           |
| `NodeGraph`                                       | Nodes, typed ports, connections, pan and zoom              |
| `MapCanvas`                                       | A projected map: pan, zoom, markers, custom backgrounds    |
| `WorldCanvas`                                     | World-space anchors, ribbons and screen-space layers       |
| `CodeEditor`                                      | Tokenized text, completions, diagnostics                   |
| XML                                               | Declarative screens, named widget lookup, hot reload       |

**Minecraft-native widgets.** Item, block and entity previews, item and texture pickers, vanilla tooltips. They live next to the rest of the tree; they are not a second UI system.

**Rendering extras.** Custom GLSL through `ShaderHandle`, screen-wide and per-layer post effects, and subtree caches for parts of the tree that do not change every frame.

Render policies on a screen:

| Policy          | When                               |
|-----------------|------------------------------------|
| `continuous()`  | Animations, shaders, live previews |
| `onDirty()`     | Redraw only after an invalidation  |
| `vsync()`       | Follow the window refresh          |
| `fixedFps(fps)` | A hard cap                         |

## Try it

`TestMod` is a Fabric client that drops UniGUI screens onto the Minecraft title screen. Run the client for `1.20.1` or `1.21.1`, then use the buttons in the top-right.

| Button                                  | What opens                                                                           |
|-----------------------------------------|--------------------------------------------------------------------------------------|
| UniGUI Demo                             | The widget catalog: controls, text, layout, virtualized data, graphs, maps, overlays |
| Retro Terminal                          | A terminal-styled screen                                                             |
| Level Map                               | `MapCanvas` over a station layout                                                    |
| Solar Navigation                        | A full navigation UI: procedural map, markers, docked-station panels                 |
| Docking Windows                         | A draggable docking workspace                                                        |
| Plug, Wrench, Wire, Sync, Spanner, Fork | Small interaction minigames built only from widgets                                  |
| Tutorial                                | A scripted walkthrough                                                               |
| Post Effect Test                        | Layer and screen post-processing                                                     |
| Marker Culling / Marker Scaling         | Marker volume behavior on a map                                                      |
| Ribbon Roads                            | `WorldCanvas` ribbons                                                                |

The same catalog is also a command inside the demo screen:

```text
/unigui
/unigui animations
/unigui complex
```

The **Debug** toggle on the demo header turns on the profiler overlay plus draw-command and batch counters, so you can see what a frame actually submitted.

## Use it in a mod

Published from [JitPack](https://jitpack.io/#DeusSixik/UniGui). Full coordinates, version rules and the wrapper fallback are in [`docs/JITPACK.md`](docs/old/JITPACK.md).

```groovy
repositories {
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.fabricmc.net/") } // Fabric artifacts only
}

dependencies {
    implementation "com.github.DeusSixik.UniGui:unigui-common-1.21.1:<TAG>"
    modImplementation "com.github.DeusSixik.UniGui:unigui-fabric-1.21.1:<TAG>"
}
```

Swap the platform artifact for the loader you ship: `unigui-forge-*`, or `unigui-neoforge-1.21.1`. Depend on `unigui-common-*` for the API, and on exactly one platform module for the Minecraft backend.

| Minecraft | Java | Platforms               |
|-----------|------|-------------------------|
| 1.20.1    | 17   | Fabric, Forge           |
| 1.21.1    | 21   | Fabric, Forge, NeoForge |

Both profiles are built with Stonecutter. From the repo root:

```powershell
.\gradlew.bat :1.20.1:fabric:build
.\gradlew.bat :1.21.1:fabric:build
.\gradlew.bat :1.21.1:neoforge:build
```

Launch the sandbox client:

```powershell
.\gradlew.bat :1.21.1:TestMod:runClient
```

## Layout

```text
common/     widgets, layout, XML, draw commands   (no Minecraft renderer types)
fabric/     Fabric entrypoint
forge/      Forge entrypoint
neoforge/   NeoForge entrypoint, 1.21.1 only
TestMod/    Fabric sandbox: title-screen buttons and the demo screens
```

Deeper design notes live under [`docs/`](docs): architecture, XML, text and the code editor, render state, HUD and post effects.

Wiki pages for the website are generated from API annotations as Markdown. See [`docs/wiki-generation.md`](docs/wiki-generation.md) for annotations, media paths and the Gradle task.

## License

[LGPL-3.0](LICENSE)
