# UniGUI Cleanroom 1.12.2

This is an isolated CleanroomGradle build because CleanroomGradle requires a newer Gradle and JDK than the main Loom/Stonecutter build.

## Requirements

- JDK 25 to run this build.
- JDK 21 to build the shared `core` and `demo` projects with the root Gradle wrapper.
- Foojay toolchain resolution downloads JDK 8 for the Cleanroom reobfuscation task.

## Build and run

From the repository root, build the shared core and portable demo first:

```powershell
./gradlew.bat :core:jar :demo:jar
```

Then switch the Gradle JVM to JDK 25 and run this module's wrapper from this directory:

```powershell
./gradlew.bat build
./gradlew.bat runClient
```

The produced mod jar includes the core jar, including its bundled fastutil dependency.

The produced mod jar also embeds the portable `demo` library. After joining a world, use
`/unigui demo <name>` to activate an example. Available names are `widgets`, `docking`, `dominion`,
`retro`, `ribbon`, `tutorial`, `unity`, `overlay`, `plug`, `wrench`, `wire`, `sync`, `spanner`, `fork`,
`postprocess`, `culling`, and `scaling`. The Cleanroom host supplies the texture provider;
the demo classes themselves remain Minecraft-independent and can be reused by another host.

## 1.12.2 backend status

The backend installs the neutral `UniGuiBackend` on the client and supports `UniScreen` open/close,
core layout/tick/render, normalized mouse and keyboard events, and immediate rendering of rectangles, lines,
circles, text, textures, textured quads, and clipping. HUD and screen-overlay layers are available; overlay
input is dispatched before the active screen. Managed render targets, loading-screen layers, post effects,
rich-text styling, and advanced path/mesh/shader commands are not implemented yet. The shared core
`InputHandle` routes normalized pointer, scroll, keyboard, and text input for version adapters.

Register layers through the neutral core API:

```java
UniRenderLayerRegistration hud = UniHudRender.register(new UniRenderLayer(hudRoot));
UniRenderLayerRegistration overlay = UniScreenOverlayRender.register(
        new UniRenderLayer(overlayRoot), screen -> true, 100);
```

Close each registration when its owner is unloaded. HUD layers render after the vanilla HUD; screen overlays
render after normal screens and container screens. Overlay input is routed first and blocks vanilla input only
when a UniGUI widget consumes the event. Cleanroom loading-screen layers remain unsupported.
