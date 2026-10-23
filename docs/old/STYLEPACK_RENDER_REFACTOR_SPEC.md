# StylePack and Declarative Render Refactor SPEC

## Context

UniGUI currently renders most widgets through widget-specific renderer interfaces:

```java
MyWidget widget = ...;
widget.renderer(MyWidgetRenderer renderer);
```

This is practical for Java code, but it is not a good authoring model for a visual editor:

- arbitrary renderer Java code cannot be edited safely in an inspector;
- animation behavior is split between renderer code, widget events and ad-hoc transition helpers;
- every widget grows its own visual knobs and renderer-specific state shape;
- future mod/user style customization would require recompilation or hardcoded presets;
- XAML/XML can describe data, but not imperative renderer lambdas.

The target is a Noesis-like model where normal visual customization is represented as data:

```xml
<StylePack id="sdm.dark">
    <Style id="PrimaryButton" target="Button">
        <Setter property="background.color" value="#1A497AFF" />
        <Setter property="text.color" value="#FFFFFFFF" />
        <State name="HOVERED">
            <Setter property="background.color" value="#256AA8FF" />
        </State>
        <Event name="onClick" animation="button.press" />
    </Style>
</StylePack>
```

Core principle:

```text
WidgetRenderer remains possible, but declarative StylePack becomes the default path.
Visual editor edits StylePack data, not renderer Java code.
Runtime rendering consumes resolved style state and emits normal DrawCommands.
```

This SPEC allows a deep renderer refactor. Compatibility is desirable but not sacred: if the current renderer/state architecture blocks a clean declarative model, it may be reworked.

---

## Goals

- Introduce a first-class `StylePack` model for named, reusable render presets.
- Make style values inspectable, serializable and editor-editable.
- Support widget states such as normal, hovered, pressed, focused, disabled, selected and checked.
- Support event-linked declarative animations, e.g. `onClick -> button.press`.
- Keep custom Java renderers as an explicit escape hatch, not the primary styling API.
- Unify widget visual properties around typed `StyleKey` definitions.
- Support future XAML/XML style authoring and hot reload.
- Allow visual editor tooling to list styles, properties, states, animations and custom renderer ids.
- Reduce per-widget duplication in render state snapshots.
- Keep final output backend-neutral: widgets still produce `DrawCommand` streams.
- Make render state, clipping, layers and animation behavior composable rather than hidden in renderer lambdas.

## Non-Goals For First Implementation

- Do not implement a full Noesis/XAML engine in one pass.
- Do not require every existing widget to migrate at once.
- Do not deserialize arbitrary Java lambdas or executable code from style files.
- Do not build a shader/material graph yet.
- Do not make Minecraft-only render concepts part of the core style API.
- Do not make the visual style editor mandatory before runtime style packs work.
- Do not preserve every existing renderer API if it blocks the new model.
- Do not add CSS selector complexity until the simple target/class/id model is proven.

---

## Current State

### Existing Style Layer

UniGUI already has a small style/theme foundation:

```java
Style
MutableStyle
StyleKey<T>
StyleKeys
WidgetState
Theme
MutableTheme
DefaultTheme
```

Useful existing behavior:

- `MutableStyle` stores values by `WidgetState`.
- `StyleKey<T>` gives typed property lookup.
- `Theme.styleFor(widgetType)` resolves a style by widget type.
- `WidgetBase` has local style lookup and style scoping.
- Several widgets already call `styleValue(...)` and `styleRenderer(...)`.

Gaps:

- no named style collection / pack model;
- no explicit style target bindings;
- no style classes or ids;
- no editor-friendly property metadata registry;
- no serialization contract for styles;
- no declarative animation model;
- no custom renderer registry by id;
- no clear migration path from widget-specific renderers to declarative render plans.

### Existing Renderer Layer

Most widgets have one or more of:

```java
WidgetSpecificRenderer extends WidgetRenderer<WidgetSpecificState>
WidgetSpecificState record(...)
WidgetSpecificRenderers.DEFAULT
widget.renderer(...)
```

Useful existing behavior:

- renderer state is snapshot-like and backend-neutral;
- renderers produce `DrawScope` calls and final `DrawCommand`s;
- per-instance renderer setters are straightforward escape hatches;
- default renderers are easy to test.

Gaps:

- every widget invents its own state record shape;
- common concepts such as background, border, radius and text are duplicated;
- stateful visuals are often applied by mutating widget fields during render;
- renderer choice is not generally serializable;
- custom renderers have no stable string ids;
- visual editor cannot inspect renderer internals.

---

## Target Architecture

```text
StylePack
  -> StyleDefinition[]
       -> id
       -> target selector / bindings
       -> StyleBackend
            -> Declarative style values
            -> Custom renderer id escape hatch
       -> state overrides
       -> event animation links
  -> StyleAnimationDefinition[]
       -> PropertyTween[]
  -> optional tokens/resources

Runtime
  Widget tree
    -> StyleResolver
       -> ResolvedStyle for current widget + state
    -> RenderModel / RenderPlan
       -> declarative primitives or custom renderer adapter
    -> DrawCommands
```

The renderer refactor should separate three concerns:

1. **Style resolution** - determine the effective values for a widget and state.
2. **Render model construction** - convert resolved values and widget content into render primitives.
3. **Backend drawing** - emit `DrawCommand`s from render primitives.

---

## Core Data Model

### StylePack

`StylePack` is a named collection of reusable styles and animations. It should be usable directly as a `Theme` so existing widgets can consume it during migration.

Target API shape:

```java
public final class StylePack implements Theme {
    String id();

    Map<String, StyleDefinition> styles();
    Optional<StyleDefinition> styleDefinition(String id);
    StylePack put(StyleDefinition definition);
    StylePack removeStyle(String id);

    Map<String, String> widgetBindings();       // widgetType -> styleId
    StylePack bind(String widgetType, String styleId);
    StylePack unbind(String widgetType);

    Map<String, StyleAnimationDefinition> animations();
    Optional<StyleAnimationDefinition> animation(String id);
    StylePack putAnimation(StyleAnimationDefinition animation);

    @Override Style styleFor(String widgetType);
}
```

Resolution order for `styleFor(widgetType)`:

1. explicit widget binding, e.g. `Button -> PrimaryButton`;
2. convention style id matching widget type, e.g. `Button`;
3. pack fallback style;
4. `Style.EMPTY`.

### StyleDefinition

A `StyleDefinition` owns one named style entry.

```java
public record StyleDefinition(
    String id,
    StyleSelector selector,
    StyleBackend backend,
    Map<String, String> eventAnimations
) { }
```

The first implementation may use explicit bindings instead of full selectors. Add selector support after style packs are proven.

### StyleBackend

Renderer Java code remains possible but moves behind an explicit backend model.

```java
public sealed interface StyleBackend {
    record Declarative(Style style) implements StyleBackend { }
    record Custom(String rendererId, Style fallbackStyle) implements StyleBackend { }
}
```

Rules:

- `Declarative` is the default and is fully editor-editable.
- `Custom` references a renderer registered by stable id.
- A custom renderer may still receive resolved style values as input.
- The editor may show custom renderer id and fallback style values, but must not pretend to edit Java renderer internals.

### Style Values

Style values should continue using typed keys:

```java
StyleKey<ColorView> BACKGROUND_COLOR;
StyleKey<Float> RADIUS;
StyleKey<ImageFit> BACKGROUND_TEXTURE_FIT;
```

Required additions:

- `Style.values()` snapshot for editor inspection.
- `StyleKeyRegistry` for metadata and parsing.
- `StylePropertyDescriptor` for editor fields.

```java
public record StylePropertyDescriptor<T>(
    StyleKey<T> key,
    String displayName,
    String category,
    Class<T> type,
    T defaultValue,
    StyleValueCodec<T> codec,
    String description
) { }
```

A style value is valid only if it can be round-tripped through its descriptor codec.

---

## StyleKey Registry

The registry is required before serious XAML/style editing.

```java
public final class StyleKeyRegistry {
    Optional<StylePropertyDescriptor<?>> descriptor(String propertyId);
    List<StylePropertyDescriptor<?>> descriptors();
    List<StylePropertyDescriptor<?>> descriptorsForWidget(String widgetType);

    <T> StyleKeyRegistry register(StylePropertyDescriptor<T> descriptor);
}
```

Rules:

- Core style keys are registered by UniGUI.
- Mods can register custom style keys without editing UniGUI internals.
- Widget-specific keys are allowed, but common keys should be reused first.
- Unknown style properties from XML should produce diagnostics, not crashes.
- Unknown properties may be preserved in editor documents if round-trip preservation is enabled.

Required core descriptors:

- `background.color`
- `background.texture`
- `background.texture.tint`
- `background.texture.fit`
- `border.color`
- `border.width`
- `radius`
- `text.color`
- `placeholder.color`
- `accent.color`
- `track.color`
- `thumb.color`
- `renderer`
- `opacity`
- `scale.x`
- `scale.y`
- `rotation`

---

## Selectors And Cascade

### Phase 1: Explicit Bindings

Start simple:

```java
pack.bind(StyleIds.Widget.BUTTON, "PrimaryButton");
pack.bind(StyleIds.Widget.TEXT_INPUT, "Input.Dark");
```

This is enough for default themes and editor-authored packs.

### Phase 2: Style Classes And Ids

Add widget-facing style identity:

```java
widget.styleId("settings.apply");
widget.styleClass("primary");
widget.addStyleClass("danger");
```

Target selectors:

```text
Button
.primary
Button.primary
#settings.apply
Button#settings.apply
```

Specificity order:

1. local inline widget style;
2. widget id selector;
3. widget type + class selector;
4. class selector;
5. widget type selector;
6. fallback.

### Phase 3: Scoped Packs

Support local packs on subtree roots:

```java
root.stylePack(pack);
root.styleScope(true);
```

This extends current local style scoping into full style pack scoping.

---

## Declarative Render Model

The long-term goal is to move default renderers from widget-specific Java lambdas toward reusable render primitives.

### RenderPrimitive

```java
sealed interface RenderPrimitive {
    record FillRect(...)
    record StrokeRect(...)
    record Text(...)
    record Texture(...)
    record Icon(...)
    record Path(...)
    record Shader(...)
    record Group(...)
}
```

Each primitive should carry:

- bounds or anchor policy;
- paint/material information;
- optional clip/mask/layer/render state;
- optional state-driven visibility;
- enough source metadata for debug/editor inspection.

### RenderPlan

A `RenderPlan` is a list/tree of primitives generated from widget data and resolved style.

```java
public interface RenderPlanBuilder<W extends Widget> {
    RenderPlan build(W widget, ResolvedStyle style, RenderPlanContext context);
}
```

Rules:

- A render plan is data, not a renderer lambda.
- It can be inspected by debug tools.
- It can be cached if style version + widget visual state are unchanged.
- It emits the same DrawCommands as the old renderer pipeline.

### Widget Rendering Flow

Target retained rendering flow:

```text
Widget.render(context)
  -> ResolvedStyle style = resolver.resolve(widget, widgetState)
  -> RenderPlan plan = widget.renderPlan(style, context)
  -> RenderPlanRenderer.render(plan, drawScope)
  -> DrawCommands
```

Transitional flow:

```text
Widget.render(context)
  -> applyTheme()
  -> effectiveRenderer().render(drawScope, widgetSpecificState)
```

The transitional flow can stay while widgets migrate.

---

## Custom Renderer Registry

Custom renderers need stable ids before style packs can reference them.

```java
public final class WidgetRendererRegistry {
    <S> void register(String id, Class<S> stateType, WidgetRenderer<S> renderer);
    Optional<WidgetRendererRegistration<?>> renderer(String id);
}
```

Rules:

- ids are resource-like strings: `modid:name`.
- renderer registrations are code-side only.
- style files reference renderer ids, never class names.
- missing renderer ids produce diagnostics and fall back to declarative/default rendering.
- custom renderers should receive resolved style values when possible.

Example:

```java
WidgetRendererRegistry.global().register(
    "sdmshop:pixel_button",
    ButtonState.class,
    SdmShopRenderers.PIXEL_BUTTON
);
```

```xml
<Style id="PixelButton" target="Button" renderer="sdmshop:pixel_button">
    <Setter property="background.color" value="#222222FF" />
</Style>
```

---

## Animation Model

Animations must be data, not event callback code.

```java
public record StyleAnimationDefinition(
    String id,
    List<StylePropertyTween> tweens
) { }

public record StylePropertyTween(
    String propertyName,
    String fromValue,
    String toValue,
    TransitionSpec transition
) { }
```

### Event Links

Styles can bind events to animation ids:

```xml
<Event name="onClick" animation="button.press" />
<Event name="onError" animation="field.shake" />
```

Common event names:

- `onPointerEnter`
- `onPointerExit`
- `onPressed`
- `onReleased`
- `onClick`
- `onFocus`
- `onBlur`
- `onChecked`
- `onUnchecked`
- `onError`
- `onSuccess`

### State Transitions

State changes should also be declarative:

```xml
<StateTransition from="NORMAL" to="HOVERED" duration="0.12" easing="EASE_OUT" />
```

First implementation can map state changes to existing `TransitionSpec` and widget animation helpers.

### Property Targets

Initial property target names:

- `opacity`
- `scale.x`
- `scale.y`
- `position.x`
- `position.y`
- `rotation`
- `background.color`
- `border.color`
- `text.color`
- `accent.color`
- `radius`

Rules:

- unknown animation properties produce diagnostics;
- invalid value parsing cancels only that tween, not the whole pack;
- animation values should use the same `StyleValueCodec` as normal setters;
- event animations must not mutate source style definitions.

---

## XAML / XML Serialization

### StylePack Document

Suggested source shape:

```xml
<StylePack id="demo.dark">
    <Animation id="button.press">
        <Tween property="scale.x" from="current" to="0.96" duration="0.08" easing="EASE_OUT" yoyo="true" />
        <Tween property="scale.y" from="current" to="0.96" duration="0.08" easing="EASE_OUT" yoyo="true" />
    </Animation>

    <Style id="PrimaryButton" target="Button">
        <Setter property="background.color" value="#123456FF" />
        <Setter property="text.color" value="#FFFFFFFF" />
        <Setter property="radius" value="3" />

        <State name="HOVERED">
            <Setter property="background.color" value="#1E5E99FF" />
        </State>

        <Event name="onClick" animation="button.press" />
    </Style>

    <Style id="PixelButton" target="Button" renderer="sdmshop:pixel_button">
        <Setter property="background.color" value="#222222FF" />
    </Style>
</StylePack>
```

### Inline Widget Usage

Widget XML can reference style ids/classes later:

```xml
<Button text="Apply" style="PrimaryButton" />
<Button text="Delete" class="danger" />
```

Rules:

- `style="..."` references a style id from active packs.
- `class="primary danger"` participates in selector matching.
- inline visual attributes remain possible and should have highest priority.
- editor should preserve unknown style ids and show diagnostics.

---

## Editor Requirements

The future style editor should be a normal UniGUI app over these data models.

Required panes:

- StylePack list.
- Style list grouped by target widget.
- Style inspector with property rows generated from `StyleKeyRegistry`.
- State override editor.
- Event animation links editor.
- Animation timeline / tween list.
- Live preview widget tree.
- Diagnostics panel for unknown keys, invalid values and missing renderer ids.

Inspector must show:

- property id;
- display name;
- category;
- current value;
- default value;
- value source: default, style, state override, local override, inline XML;
- parser/validation diagnostics.

The editor must never edit Java renderer code. For `StyleBackend.Custom`, it may edit:

- renderer id;
- fallback declarative properties;
- event animation links.

---

## Migration Strategy

### Phase 0: Stabilize Data Contracts

- Add `StylePack`, `StyleDefinition`, `StyleBackend`, `StyleAnimationDefinition`.
- Add `Style.values()` / `MutableStyle.values()` for inspection.
- Keep existing `Theme`, `MutableTheme` and renderer APIs working.
- Add tests proving `StylePack` can be used as a `Theme`.

### Phase 1: StyleKey Registry And Serialization

- Add `StyleKeyRegistry` and `StyleValueCodec`.
- Register existing core style keys.
- Implement StylePack XML parser/serializer.
- Add diagnostics for unknown keys and invalid values.
- Add style pack hot reload source.

### Phase 2: Renderer Registry

- Add `WidgetRendererRegistry`.
- Register existing default/custom renderers by ids.
- Allow `StyleBackend.Custom(rendererId)` to resolve renderers.
- Keep per-instance renderer setter priority for compatibility.

### Phase 3: Widget Style Identity

- Add `styleId`, `styleClasses` and selector matching.
- Add XML attributes: `style`, `class`.
- Add cascade resolver with specificity.
- Extend local style scoping to local style packs.

### Phase 4: Declarative Render Plans

- Introduce `RenderPrimitive` and `RenderPlan`.
- Migrate simple widgets first:
  - `Box`
  - `Button`
  - `Label/TextWidget`
  - `ProgressBar`
  - `ScrollBar`
  - `ToggleSwitch`
- Keep complex widgets on old renderer path until equivalent primitives exist.

### Phase 5: Declarative Animations

- Resolve event animation ids from `StyleDefinition`.
- Map property tweens to existing `WidgetBase` animation engine.
- Add color style transitions.
- Add diagnostics for unsupported animated properties.
- Add editor timeline UI later.

### Phase 6: Editor Integration

- Add StylePack editor panes.
- Add live preview and hot reload.
- Allow assigning styles/classes to widgets in XML editor.
- Surface renderer id diagnostics and missing property diagnostics.

---

## Compatibility Rules

### Existing Renderer Setters

Per-instance renderer setters may remain but should be treated as highest-priority overrides:

```text
instance renderer > style custom renderer > declarative render plan > default renderer
```

If the renderer layer is deeply refactored, old setters may become adapters:

```java
button.renderer(renderer)
```

internally becomes:

```java
button.renderOverride(RenderOverride.custom(renderer))
```

### Existing Themes

`MutableTheme` and `DefaultTheme` should continue to work until `StylePack` fully replaces them.

`StylePack` should implement `Theme` during migration.

### Existing DrawCommands

The backend should not care whether commands came from:

- old `WidgetRenderer`;
- new `RenderPlan`;
- custom renderer;
- style animation.

Batching and clipping should operate on final DrawCommands / RenderState.

---

## Render State Alignment

This SPEC should align with `RENDER_STATE_SPEC.md`.

Style values should be able to set future render-state properties:

- blend mode;
- layer;
- depth mode;
- mask mode;
- alpha mode;
- shader id/material id.

Do not bake these into every widget-specific state record. Prefer reusable `RenderPrimitive` / `RenderState` data.

---

## Diagnostics

Style loading and resolution must be diagnostic-first.

Diagnostic examples:

- unknown style key: `background.colour`;
- invalid value: `radius="abc"`;
- missing style id: `PrimaryButon`;
- missing custom renderer id: `mod:fancy_button`;
- unsupported animation property: `shadow.blur`;
- invalid target widget type: `Buton`.

Diagnostics should include:

- severity;
- source file/path if available;
- line/column if parsed from XML;
- style id;
- property/event/animation id;
- human-readable message.

Runtime should gracefully fall back whenever possible.

---

## Acceptance Criteria

### Phase 0 Acceptance

- `StylePack` can be passed to `DefaultUIContext.theme(pack, root)`.
- A bound `Button -> PrimaryButton` style changes rendered button background in tests.
- `MutableStyle.values()` exposes normal and state override values for editor inspection.
- A `StyleDefinition` can preserve a custom renderer id without resolving it yet.
- A `StyleAnimationDefinition` can store multiple property tweens.
- Existing renderer tests continue passing.

### Phase 1 Acceptance

- A style pack XML file can round-trip parse -> serialize -> parse.
- Unknown style keys produce diagnostics without throwing.
- Core style keys parse colors, floats, resource ids and enums through codecs.
- StylePack editor can list styles/properties from registry metadata.

### Phase 2 Acceptance

- Existing default renderers can be registered by stable ids.
- Missing renderer ids fall back to declarative/default rendering and report diagnostics.
- Per-instance renderer setters still override style renderer ids.

### Phase 3 Acceptance

- `style="PrimaryButton"` and `class="primary"` work in XML widgets.
- Selector specificity is deterministic and tested.
- Local style pack scoping works for subtrees.

### Phase 4 Acceptance

- At least `Box` and `Button` can render through declarative render plans.
- Render plans emit the same visual DrawCommands as old defaults for baseline states.
- Debug tooling can inspect generated primitives before DrawCommand emission.

### Phase 5 Acceptance

- Event animation links trigger existing widget animation engine.
- State transitions animate at least color, opacity and scale.
- Unsupported tween properties are diagnostic-only failures.

---

## Open Questions

- Should style values use only `StyleKey<T>` or also support dynamic custom keys with late-bound types?
- Should selector matching live in `api.style` or in an `impl.style` resolver layer?
- Should renderer ids be global or scoped by active `StylePack` / mod namespace?
- Should style packs support imports/includes?
- Should render plans be cached per widget or rebuilt every frame from resolved style?
- How much of layout should be styleable versus remaining in `LayoutStyle`?
- Should inline XML attributes override style values automatically, or only for properties explicitly marked as style-backed?

---

## Recommended Immediate Next Step

Do not start by rewriting every widget renderer.

Next implementation slice should be:

1. Add `StyleKeyRegistry` and `StyleValueCodec`.
2. Register current `StyleKeys` with metadata.
3. Implement a minimal `StylePackXml` parser/serializer.
4. Add XML tests for one pack with `Button`, state override and click animation link.
5. Only after that, begin renderer migration with `Box` and `Button`.