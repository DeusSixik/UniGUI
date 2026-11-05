# UniGUI portable demos

`demo` contains reusable demo screens and UI test scenes. It is a normal Java 17
library and depends only on `:core`; it has no Minecraft, Fabric, Forge, NeoForge,
LWJGL, mixin, or version-specific source dependency.

Each complete demo exposes a factory returning `UniScreen`. Building a screen does
not require an installed backend:

```java
UniScreen screen = DockingWindowsDemoScreen.createScreen();
```

Opening it delegates to the backend installed by the host project:

```java
screen.openGui();
```

The Minecraft `TestMod` is now one such host. Another game version or project can
depend on the published `unigui-demo` artifact, install its own `UniGuiBackend`, and
reuse the same widget trees unchanged.

Platform-specific examples remain in `TestMod` when their purpose is explicitly to
exercise Minecraft APIs, such as mixins, vanilla video options, resource reload,
NBT/packet codecs, or native texture upload. Those classes may consume portable
widgets from this module, but the dependency never points back from `demo` to
`TestMod`.
