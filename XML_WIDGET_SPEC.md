# XML Widget Deserialization SPEC

## Context

UniGUI is moving toward a retained-mode UI system that can eventually support a Unity-like UI editor. A visual editor needs a stable serialized representation of widget trees, layout properties, style state, assets and later animations.

The first useful step is not a full editor and not a full binding engine. The first step is a small XML deserializer that can materialize normal UniGUI widgets from XML, similar in spirit to NoesisGUI/XAML loading.

The important design point:

```text
XML is a source format.
The result is a normal Widget tree.
There is no required XMLWidget runtime wrapper.
```

Example target usage:

```java
VBox root = XMLWidget.create("""
<VBox id="root" spacing="6" width="240" height="120">
    <Label id="title" text="Video Settings" />
    <Button id="apply" text="Apply" />
</VBox>
""", VBox.class);

Button apply = XMLWidget.getWidget(root, "apply", Button.class);
apply.onClick(event -> saveSettings());
```

## Goals

- Deserialize XML into real UniGUI `Widget` instances.
- Keep runtime usage code-first and event-driven.
- Support typed root creation through `XMLWidget.create(xml, RootType.class)`.
- Support id-based lookup after creation through `XMLWidget.getWidget(root, id, Type.class)`.
- Avoid a required document-wrapper object in normal runtime usage.
- Make the first implementation small enough to validate ergonomics quickly.
- Use the same metadata/registry direction that can later power an editor inspector and serializer.
- Keep bindings, commands and advanced animations out of the first slice, but leave clear extension points.

## Non-Goals For Prototype

- Do not implement a full XAML-compatible language.
- Do not implement a full binding engine in the first pass.
- Do not deserialize Java lambdas, raw callbacks or arbitrary code from XML.
- Do not make XML loading depend on Minecraft-specific classes.
- Do not require every existing widget to be annotation-ready immediately.
- Do not make `XMLWidget` itself a visual widget or document host.
- Do not require a visual editor before the runtime loader is useful.

## Core Public API

`XMLWidget` is a static utility/factory class, not a widget subclass.

```java
public final class XMLWidget {
    public static Widget create(String xml);

    public static <T extends Widget> T create(String xml, Class<T> widgetType);

    public static Widget create(String xml, XmlWidgetRegistry registry);

    public static <T extends Widget> T create(String xml, Class<T> widgetType, XmlWidgetRegistry registry);

    public static Widget getWidget(Widget root, String id);

    public static <T extends Widget> T getWidget(Widget root, String id, Class<T> widgetType);

    public static Optional<Widget> findWidget(Widget root, String id);

    public static <T extends Widget> Optional<T> findWidget(Widget root, String id, Class<T> widgetType);
}
```

### Custom Registries

The built-in registry is only the default runtime surface. Mods and editor prototypes must be able to register additional XML names without editing UniGUI internals.

```java
XmlWidgetRegistry registry = XMLWidget.registry();

registry.register("Badge", Badge::new)
        .attribute("text", XmlValueParsers.STRING, Badge::text)
        .attribute("importance", XmlValueParsers.FLOAT, Badge::importance);

registry.alias("Pill", "Badge");

Badge badge = XMLWidget.create("""
<Pill id="status" text="Ready" importance="7.5" />
""", Badge.class, registry);
```

Rules:

- `XMLWidget.registry()` returns a mutable registry with built-in UniGUI descriptors already registered.
- `XMLWidget.emptyRegistry()` returns a mutable registry with no built-ins for isolated tests/tools.
- Custom XML names instantiate only explicitly registered widget factories.
- Custom attributes must use explicit parsers and setters.
- Aliases resolve to registered descriptors and do not create wrapper widgets.
- A custom registry is passed to `XMLWidget.create(...)`; the global default registry is not mutated by custom registrations.

### Descriptor Metadata

The registry should expose immutable metadata snapshots for future editor palettes and inspectors.

```java
XmlWidgetDescriptor button = XMLWidget.registry()
        .descriptor("Button")
        .orElseThrow();

for (XmlAttributeDescriptor attribute : button.attributes()) {
    inspector.addField(
            attribute.category(),
            attribute.displayName(),
            attribute.defaultValue(),
            attribute.description());
}
```

Rules:

- descriptors are read-only snapshots;
- descriptors must not expose internal mutable registry maps;
- every registered attribute has at least a name, display name and category;
- custom registries can provide display names, categories, default values and descriptions;
- descriptor metadata is editor-facing only and must not affect runtime XML loading.

### `create(String xml)`

Parses XML and returns the root widget as `Widget`.

```java
Widget root = XMLWidget.create(xml);
```

Use when the caller does not care about the concrete root type or will inspect/cast manually.

### `create(String xml, Class<T> widgetType)`

Parses XML and returns the root widget cast to the requested type.

```java
Button button = XMLWidget.create("""
<Button id="play" text="Play" />
""", Button.class);
```

If the XML root is not assignable to `widgetType`, loading fails with a clear error.

```text
XML root is VBox, expected Button.
```

### `getWidget(root, id, type)`

Finds a descendant or the root itself by XML id and checks its runtime type.

```java
Slider gamma = XMLWidget.getWidget(root, "gamma", Slider.class);
```

Failure cases must be explicit:

```text
Widget id 'gamma' was not found under root 'settingsRoot'.
Widget id 'gamma' exists, but is Slider, not Button.
```

## Runtime Model

The XML loader materializes widgets exactly as normal Java code would.

```text
XML text
  -> parse DOM / streaming XML events
  -> element name resolves widget type
  -> constructor creates widget
  -> attributes apply properties
  -> child elements are added to parent containers
  -> result is ordinary Widget root
```

The loaded tree should participate in the same lifecycle as code-created widgets:

- normal layout;
- normal rendering;
- normal event dispatch;
- normal invalidation;
- normal parent/child ownership;
- normal deferred mutation behavior.

## Id Model

XML ids are required for code-behind style usage.

Preferred direction:

```java
public interface Widget {
    String id();
    Widget id(String id);
}
```

or equivalent support in the base widget implementation.

Rules:

- `id` is optional.
- `id` must be unique within a loaded XML subtree.
- Duplicate ids should fail fast by default.
- `XMLWidget.getWidget(root, id, type)` should include the root itself in lookup.
- IDs are runtime/debug/editor identifiers, not Minecraft resource ids.

If adding `id` to the public `Widget` API is too invasive for the first prototype, the loader may store ids in a small widget metadata map. However, long-term editor support will be cleaner if id/name is a first-class widget property.

## Code-Behind Pattern

The first version should prefer code-behind over XML bindings.

XML defines structure:

```xml
<VBox id="root" spacing="6">
    <Label id="title" text="Video Settings" />
    <Slider id="gamma" min="0" max="1" value="0.5" />
    <Button id="apply" text="Apply" />
</VBox>
```

Java wires behavior:

```java
VBox root = XMLWidget.create(xml, VBox.class);

Slider gamma = XMLWidget.getWidget(root, "gamma", Slider.class);
Button apply = XMLWidget.getWidget(root, "apply", Button.class);

apply.onClick(event -> options.gamma().set((double) gamma.value()));
```

This keeps the first loader small and avoids designing a weak binding language too early.

## XML Shape

### Element Names

Element names map to registered widget types.

```xml
<VBox>
<HBox>
<Box>
<Label>
<Button>
<Slider>
```

Names should be stable public XML names, not necessarily Java class names.

Prototype aliases may map common authoring names to the same runtime widget types:

```text
Border -> Box
Stack -> StackPanel
ScrollViewer -> ScrollView
TextView -> TextWidget
Texture -> TextureWidget
Image -> ImageView
```

Aliases should not create wrapper widgets. They resolve to the same registered descriptor as the target type.

### Attributes

Attributes map to registered properties.

```xml
<Button id="apply" text="Apply" width="80" height="20" enabled="true" />
```

Prototype supported value kinds:

- `String`
- `boolean`
- `int`
- `float`
- `double`
- enums by name
- colors as hex strings: `#RRGGBB` or `#RRGGBBAA`
- resource ids as strings

### Children

Child elements are added to parent containers.

```xml
<VBox id="root">
    <Label text="Title" />
    <Button text="OK" />
</VBox>
```

If an element does not support children, loading should fail clearly.

```text
Widget Button cannot contain child Label.
```

### Property Elements

The prototype may support a small XAML-like property-element form for child slots that are clearer as named properties than direct children.

```xml
<ScrollView id="settingsScroll" width="232" height="122">
    <ScrollView.Content>
        <VBox id="settingsRows" spacing="4" />
    </ScrollView.Content>
</ScrollView>
```

For normal multi-child containers, `.Children` is equivalent to direct child elements but is useful for editor/XAML-style output.

```xml
<VBox id="root">
    <VBox.Children>
        <Label id="title" text="Title" />
        <Button id="ok" text="OK" />
    </VBox.Children>
</VBox>
```

Rules:

- property elements are XML grouping nodes, not runtime widgets;
- the owner prefix must resolve to the current widget type or one of its aliases;
- unknown property elements fail clearly in strict runtime loading;
- property elements may contain widget elements, not attributes, callbacks or executable code.

### Text Content

Prototype can ignore mixed text content or support it only for text-like widgets.

Recommended first behavior:

```xml
<Label text="Hello" />
```

Later optional behavior:

```xml
<Label>Hello</Label>
```

## Widget XML Registry

The prototype can be manually registered. This is intentionally simple.

```java
registry.register("VBox", VBox::new)
        .attribute("spacing", XmlParsers.FLOAT, VBox::spacing)
        .layoutAttributes();

registry.register("Button", Button::new)
        .attribute("text", XmlParsers.STRING, Button::text)
        .attribute("enabled", XmlParsers.BOOLEAN, Button::enabled)
        .layoutAttributes();
```

Suggested types:

```java
public final class WidgetXmlRegistry {
    public <T extends Widget> WidgetXmlType<T> register(String xmlName, Supplier<T> factory);

    public WidgetXmlType<? extends Widget> type(String xmlName);
}
```

```java
public final class WidgetXmlType<T extends Widget> {
    public WidgetXmlType<T> attribute(String name, XmlValueParser<?> parser, XmlPropertySetter<T, ?> setter);

    public WidgetXmlType<T> childPolicy(XmlChildPolicy<T> policy);
}
```

The manual registry should be considered the prototype bridge, not the final authoring experience.

## Future Annotation Support

Long-term, common widget XML metadata should be generated from annotations or a reflection-like descriptor layer, so widgets do not require repetitive manual registry code.

Possible annotations:

```java
@XmlWidgetName("Button")
public final class Button extends WidgetBase {
    @XmlAttribute("text")
    public Button text(String text) { ... }

    @XmlAttribute("enabled")
    public Button enabled(boolean enabled) { ... }
}
```

For layout/style props, common base annotations or shared descriptors should avoid repeating the same attributes on every widget.

```java
@XmlLayoutAttributes
@XmlStyleAttributes
public abstract class WidgetBase implements Widget {
}
```

Annotation support can be implemented later as one of these approaches:

- runtime reflection scan;
- generated registry at compile time;
- hybrid: reflection in dev, generated descriptors for release.

For Minecraft/mod environments, generated descriptors may be more predictable than broad classpath scanning.

## Layout Attributes

The XML system should expose layout without tying XML to internal layout implementation details.

Prototype layout attributes:

```xml
<Box width="265" height="213" align="center" />
<VBox spacing="6" alignItems="center" />
<HBox spacing="12" justifyContent="center" />
```

Suggested common attributes:

```text
width
height
minWidth
minHeight
maxWidth
maxHeight
flexGrow
flexShrink
align
alignItems
justifyContent
padding
margin
overflowX
overflowY
```

Parsing may support compact syntax later:

```xml
<Box padding="10 8 10 8" margin="4" />
```

The first implementation can keep this smaller and add only the attributes needed for a demo screen.

## Style Attributes

Prototype may support direct visual attributes as inline properties.

```xml
<Box background="#0D1016E6" border="#696D70F5" borderWidth="0.32" radius="0" />
<Label text="VIDEO SETTINGS" color="#F5F7FFFF" />
```

Long-term, XML should support classes/styles/themes:

```xml
<Box id="panel" class="video-panel">
    <Label class="title" text="VIDEO SETTINGS" />
</Box>
```

Style classes should be editor-friendly but should not be required for the first loader.

## Texture / Image Attributes

Texture XML should stay common/runtime-friendly. The XML loader creates `SimpleTextureHandle` values from resource id strings; the Minecraft backend can resolve those ids as `ResourceLocation`s during render.

```xml
<Box id="panel"
     backgroundTexture="test_mod:uniformclouds-1"
     backgroundTextureWidth="256"
     backgroundTextureHeight="128"
     backgroundTextureFit="cover"
     backgroundTextureTint="#FFFFFFFF"
     backgroundTextureSource="0 0 1 1" />

<Image id="icon"
       texture="minecraft:textures/item/diamond.png"
       textureWidth="16"
       textureHeight="16"
       fit="contain"
       tint="#FFFFFFFF" />
```

Prototype attributes:

```text
Box:
  backgroundTexture
  backgroundTextureWidth
  backgroundTextureHeight
  backgroundTextureFit
  backgroundTextureTint
  backgroundTextureSource
  backgroundTextureSampling
  backgroundTextureWrap
  backgroundTextureMipmaps
  backgroundTexturePremultipliedAlpha

TextureWidget / ImageView:
  texture
  textureWidth
  textureHeight
  fit
  tint
  source
  radius
  textureSampling
  textureWrap
  textureMipmaps
  texturePremultipliedAlpha
```

Rules:

- `texture` and `backgroundTexture` are string resource ids.
- width/height default to `16x16` if omitted; this is enough for `stretch`, while `contain`/`cover` should provide real source dimensions for correct aspect ratio.
- source rect syntax is `u v width height` in normalized UV coordinates.
- sampling uses `TextureFilter` names such as `nearest`, `linear`, `nearest-mipmap-nearest`.
- wrap uses `TextureWrap` names such as `clamp-to-edge`, `repeat`, `mirrored-repeat`.
- no image bytes are loaded by the XML loader itself.

## Event And Binding Strategy

### Prototype

The first XML loader should not deserialize events.

Use Java code-behind:

```java
Button apply = XMLWidget.getWidget(root, "apply", Button.class);
apply.onClick(event -> applySettings());
```

### Later Commands

After the runtime loader works, XML may support command names without embedding code.

```xml
<Button id="apply" text="Apply" onClick="video.apply" />
```

The caller provides a command registry:

```java
XMLWidget.create(xml, options -> options.commands(commands));
```

### Later Bindings

Binding syntax should be deferred until there is enough real usage.

Possible future syntax:

```xml
<Slider id="gamma" value="{binding video.gamma}" />
<Label text="{binding video.graphicsLabel}" />
```

Bindings should be typed, observable and debuggable. They should not be stringly typed glue hidden inside widgets.

## Error Handling And Diagnostics

The loader should fail loudly by default.

Examples:

```text
Unknown widget type 'FooPanel' at line 3, column 5.
Unknown attribute 'spcaing' on VBox at line 1, column 15.
Cannot parse float attribute 'spacing' value 'large'.
Duplicate widget id 'apply'.
Widget Button cannot contain child VBox.
XML root is VBox, expected Button.
```

Add an exception type:

```java
public final class XmlWidgetLoadException extends RuntimeException {
    public List<XmlWidgetDiagnostic> diagnostics();
}
```

Diagnostics should include line/column when possible. This matters for future editor integration.

## Security And Safety

XML loading must not execute arbitrary code.

Rules:

- Disable external entity resolution.
- Do not support arbitrary Java class names in XML by default.
- Only instantiate registered widget types.
- Only apply registered attributes.
- Do not deserialize raw event callback names without an explicit command registry.
- Limit recursion/depth in loader to prevent pathological documents.

## Package Proposal

Prototype packages:

```text
dev.sixik.unigui.api.xml
  XMLWidget
  XmlWidgetRegistry
  XmlWidgetType
  XmlWidgetDescriptor
  XmlAttributeDescriptor
  XmlPropertyChildDescriptor
  XmlValueParser
  XmlValueParsers
  XmlPropertySetter
  XmlChildPolicy
  XmlWidgetLoadException
  XmlWidgetDiagnostic
  XmlWidgetOptions

dev.sixik.unigui.impl.xml
  XmlWidgetLoader
  WidgetXmlRegistry
  WidgetXmlType
  XmlValueParsers
  BuiltInWidgetXmlRegistry
```

If the public package name should avoid all-caps class/package style, `XmlWidget` is also acceptable. The API name can still expose `XMLWidget` if that is preferred for ergonomics.

## Prototype Built-In Widgets

Start with a deliberately small set.

```text
Containers:
  Box
  VBox
  HBox
  StackPanel
  ScrollView

Display:
  Label
  TextWidget / TextBlock later
  TextureWidget / ImageView later

Controls:
  Button
  ToggleButton
  Checkbox
  Slider
  ProgressBar
  ComboBox later
```

Do not wait until every widget is covered. A good first test is recreating one existing TestMod screen section from XML.

## Example: Video Settings Panel Slice

```xml
<Box id="panel"
     width="265"
     height="213"
     align="center"
     padding="10 8 10 8"
     background="#0D1016E6"
     border="#696D70F5"
     borderWidth="0.32"
     radius="0">

    <VBox id="content" spacing="6" alignItems="center">
        <Label id="title" text="VIDEO SETTINGS" class="title" />
        <Separator id="topSeparator" width="232" height="0.8" />

        <ScrollView id="settingsScroll" width="232" height="122" overflowX="hidden" overflowY="auto">
            <VBox id="settingsRows" spacing="4" />
        </ScrollView>

        <Separator id="bottomSeparator" width="232" height="0.8" />
        <HBox id="actions" spacing="12" justifyContent="center">
            <Button id="done" text="Done" />
            <Button id="reset" text="Reset" />
        </HBox>
    </VBox>
</Box>
```

```java
Box panel = XMLWidget.create(xml, Box.class);

VBox settingsRows = XMLWidget.getWidget(panel, "settingsRows", VBox.class);
Button done = XMLWidget.getWidget(panel, "done", Button.class);
Button reset = XMLWidget.getWidget(panel, "reset", Button.class);

settingsRows.addChild(graphicsRow(options));
done.onClick(event -> finishVideoSettings(last, options, oldMipmaps));
reset.onClick(event -> resetVideoDefaults(options));
```

This is intentionally hybrid: static UI structure comes from XML, dynamic option rows can still be created in Java until bindings/templates are ready.

## Editor Direction

The runtime XML loader should become the foundation for a future visual editor.

Editor eventually needs:

- hierarchy tree;
- viewport selection;
- inspector based on widget property metadata;
- add/remove/reorder children;
- undo/redo commands;
- drag/resize handles;
- asset picker for textures/fonts/shaders;
- XML save/load;
- hot reload preview;
- prefab/template support;
- diagnostics panel.

The editor should edit the same document/property model that the runtime loader consumes. Avoid building a separate editor-only representation that cannot round-trip to runtime widgets.

## Checklist Roadmap

This checklist is the working plan for the XML widget system. Keep it updated as implementation moves forward.

### Phase 1: Runtime Loader MVP

- [x] Add `XMLWidget` public factory class.
- [x] Add `XMLWidget.create(xml)` for untyped root loading.
- [x] Add `XMLWidget.create(xml, RootType.class)` for typed root loading.
- [x] Add `XMLWidget.createRoot(...)` aliases for Noesis-like naming.
- [x] Add `XMLWidget.create(InputStream, ...)` overloads.
- [x] Add `XMLWidget.createResource(...)` and `createRootResource(...)` overloads.
- [x] Add DOM-based `XmlWidgetLoader` prototype.
- [x] Harden XML parser against external entities and DTD loading.
- [x] Add `XmlWidgetOptions` with strict/lenient attributes and max depth.
- [x] Add `XmlWidgetLoadException` and `XmlWidgetDiagnostic`.
- [x] Add widget `id()` / `id(String)` support to runtime widgets.
- [x] Implement duplicate id detection during XML load.
- [x] Implement `XMLWidget.getWidget(root, id, type)` lookup.
- [x] Include root widget in id lookup.
- [x] Traverse normal panel children and `ScrollView.content()` during id lookup.
- [x] Add self-test task and wire it into common tests.

### Phase 2: Manual Registry And Built-Ins

- [x] Add manual internal `WidgetXmlRegistry` and `WidgetXmlType`.
- [x] Register common containers: `Panel`, `Box`, `VBox`, `HBox`, `StackPanel`, `WrapPanel`, `ScrollView`.
- [x] Register common text widgets: `TextWidget`, `Text`, `TextBlock`, `Label`.
- [x] Register common controls: `Button`, `ToggleButton`, `Checkbox`, `Slider`, `ProgressBar`, `Separator`.
- [x] Add common layout attributes: size, min/max size, padding, margin, flex, align, overflow, absolute edges.
- [x] Add common widget attributes: enabled, visible, visibility, opacity, rotation, scale, class/styleClass placeholders.
- [x] Add control-specific attributes: text, checked/state, slider/progress ranges, colors.
- [x] Add parsers for strings, booleans, ints, floats, doubles, enums, colors, sizes and insets.
- [x] Add parser and surface for `TextureHandle` values.
- [x] Split built-in descriptors into smaller files once registry grows beyond prototype readability.
- [ ] Add more widgets only when a real XML screen needs them.

### Phase 3: XML Language Ergonomics

- [x] Support XML namespaces enough for `x:Name` / `Name` id aliases.
- [x] Support text content for text-like widgets.
- [x] Support strict unknown-attribute errors by default.
- [x] Support lenient unknown-attribute mode for editor/prototyping.
- [x] Add XML aliases: `Border`, `Stack`, `ScrollViewer`, `TextView`, `Texture`, `Image`.
- [x] Add XAML-like property elements: `.Children`, `ScrollView.Content`.
- [x] Validate property elements as grouping nodes, not runtime widgets.
- [x] Add line/column diagnostics for elements and attributes.
- [x] Improve parser error messages with original XML display names and values.
- [x] Decide whether simple XML comments should be preserved for future round-trip/editing.

### Phase 4: Public Extension API

- [x] Add public `XmlWidgetRegistry` facade.
- [x] Add public `XmlWidgetType` descriptor facade.
- [x] Add public `XmlValueParser`, `XmlValueParsers`, `XmlPropertySetter`, `XmlChildPolicy`.
- [x] Add `XMLWidget.registry()` for mutable built-in registry copies.
- [x] Add `XMLWidget.emptyRegistry()` for isolated tools/tests.
- [x] Add `XMLWidget.create(..., registry)` overloads for string, stream, resource and typed root loading.
- [x] Support custom aliases through public registry API.
- [x] Add self-test for custom registry widget and alias.
- [x] Add descriptor metadata for editor inspector: display name, category, default value, description.
- [x] Add read-only descriptor inspection API that does not expose internal mutable maps.
- [x] Add removable global registry contributions for mod-provided descriptors.

### Phase 5: Texture / Image XML

- [x] Register `TextureWidget` and `ImageView` in built-in XML registry.
- [x] Add `Texture` and `Image` aliases.
- [x] Add `texture`, `textureWidth`, `textureHeight`, `fit`, `tint`, `source`, `radius` attributes.
- [x] Add `backgroundTexture`, `backgroundTextureWidth`, `backgroundTextureHeight`, `backgroundTextureFit`, `backgroundTextureTint`, `backgroundTextureSource` attributes for `Box`.
- [x] Add texture sampling/wrap/mipmaps/premultiplied-alpha attributes.
- [x] Keep XML texture loading common-only by storing ids in `SimpleTextureHandle`.
- [x] Register `/unigui xml` demo texture as managed texture before loading XML.
- [x] Add optional texture resolver hook for runtime-specific handles.
- [x] Add asset-picker friendly metadata for texture attributes.
- [x] Add optional asset catalog dimensions as manifest defaults for XML textures.

### Phase 6: TestMod Screen Slice

- [x] Add `/unigui xml` command.
- [x] Add classpath XML resource for demo UI.
- [x] Load demo UI through `XMLWidget.createResource(...)`.
- [x] Wire Java code-behind events using `XMLWidget.getWidget(...)`.
- [x] Verify the XML demo renders in game.
- [x] Verify texture-backed XML demo renders in game.
- [x] Convert one larger existing TestMod screen section to XML once diagnostics improve.
- [ ] Keep dynamic/generated option rows in Java until templates/bindings exist.

### Phase 7: Diagnostics Polish

- [x] Fail clearly on unknown widget type.
- [x] Fail clearly on unknown attributes in strict mode.
- [x] Fail clearly on duplicate ids.
- [x] Fail clearly on wrong typed root.
- [x] Fail clearly on invalid children and duplicate `ScrollView.Content`.
- [x] Fail clearly on invalid texture ids and UV source syntax.
- [x] Add line/column to `XmlWidgetDiagnostic`.
- [x] Attach line/column to elements while parsing.
- [x] Attach best-effort line/column to attributes.
- [x] Add diagnostics list support for collecting multiple editor errors later.
- [x] Add tests for useful diagnostic locations.
- [x] Add diagnostics panel concepts for the future editor.

### Phase 8: Serialization And Round-Trip

- [x] Define XML document model separate from runtime widget instances.
- [x] Add serializer from source document model back to XML.
- [x] Add serializer from runtime widget/descriptor snapshots back to XML if editor export needs it.
- [x] Preserve stable id/name values during round-trip.
- [x] Preserve property elements where editor authored them.
- [x] Preserve simple XML comments for future round-trip/editing.
- [x] Decide formatting style for saved XML.
- [x] Decide how to preserve unsupported/unknown attributes in editor mode.
- [x] Add golden-file tests for load/save round-trip.

### Phase 9: Annotation / Descriptor Generation

- [x] Prototype `@XmlWidgetName` annotation for XML type names.
- [x] Prototype `@XmlAttribute` annotation for fluent setters.
- [x] Prototype shared descriptor blocks for layout/style attributes.
- [x] Add runtime reflection helper for annotation-backed descriptor metadata.
- [x] Add annotation-backed registry registration so widgets and attributes do not require manual XML builder code.
- [x] Decide runtime reflection vs generated descriptors for the prototype/editor metadata bridge.
- [ ] Prefer generated descriptors for release/mod environments if reflection scanning becomes fragile.
- [ ] Reuse generated descriptors for editor inspector and serializer.

### Phase 10: Commands, Bindings And Templates

- [x] Add command registry concept for event names such as `onClick="video.apply"`.
- [x] Keep raw callbacks/lambdas out of XML.
- [x] Define a typed observable binding model before adding `{binding ...}` syntax.
- [x] Add binding diagnostics and editor-visible binding status.
- [x] Add item/control templates only after core layout XML is stable.
- [x] Add prefab/template include story after document round-trip exists.

### Phase 11: Editor Foundation

- [x] Add hierarchy tree model.
- [x] Add selection model for XML/document nodes.
- [x] Add inspector using descriptor metadata.
- [x] Add undoable document mutations.
- [x] Add add/remove/reorder children commands.
- [x] Add drag/resize handles against layout attributes.
- [x] Add asset picker for textures/fonts/shaders.
- [x] Add XML save/load in editor.
- [x] Add hot reload preview.
- [x] Add diagnostics panel.
- [x] Add backend-neutral asset catalog/picker model for textures/fonts/shaders.

## Current Definition Of Done

### Runtime Loader MVP

- [x] `XMLWidget.create(xml)` returns a real widget tree.
- [x] `XMLWidget.create(xml, VBox.class)` returns a typed root.
- [x] Wrong typed root fails clearly.
- [x] `id`, `name`, `Name` and `x:Name` are searchable through `XMLWidget.getWidget(...)`.
- [x] Duplicate ids fail clearly.
- [x] Unknown widget/attribute/parser errors include useful messages.
- [x] Registered attributes can configure basic layout and control state.
- [x] Child elements attach to containers in XML order.
- [x] A simple screen section can be built from XML and wired with Java events.
- [x] No XML path requires direct Minecraft classes in the common XML API.

### Next Definition Of Done

- [x] XML diagnostics include useful line/column for common authoring mistakes.
- [x] Descriptor metadata is rich enough for a basic editor inspector.
- [x] A larger TestMod screen slice can be authored mostly in XML without making debugging painful.
