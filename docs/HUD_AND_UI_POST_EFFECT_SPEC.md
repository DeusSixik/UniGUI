# HUD Overlay and UI PostEffect SPEC

## Context

UniGUI is a general UI library and must not model HUD rendering as a Minecraft-only screen/window feature. Future game integrations should be able to render UI as normal screens, embedded widgets, debug overlays, HUD overlays, or mod-specific layers without leaking host-specific objects into core APIs.

The immediate priority is **UI PostEffect**: render any UI layer into an offscreen target, run one or more post-processing passes, then composite the result back. HUD Overlay is a later consumer of the same pipeline. This matters for effects like helmet HUD distortion, glass refraction, scanlines, chromatic aberration, vignette, damaged visor noise, heat haze, or low-health warning pulses.

Core principle:

```text
Widgets build DrawLists.
Render targets isolate layers.
PostEffects transform rendered pixels.
Hosts decide where the final layer is presented.
```

No class in `api.overlay`, `api.posteffect`, `api.render`, `api.style`, or XML support may depend on Minecraft classes. Minecraft integration should be only one backend/host implementation.

---

## Goals

- Add backend-neutral UI PostEffect contracts for offscreen UI processing.
- Allow effects to be registered by stable ids and referenced by Java, XML/XAML, and StylePack data.
- Keep post-processing layer-level by default, not per-widget.
- Support animated uniforms through suppliers/bindings without tying them to Minecraft player state.
- Define a HUD Overlay API that renders regular UniGUI widget trees outside normal windows/screens.
- Let HUD visibility be controlled by `BooleanSupplier` / binding ids, not `Player` predicates.
- Make Minecraft HUD integration a host adapter, not a core concept.
- Keep the system usable on backends that do not support post-processing by graceful fallback.

## Non-Goals For First Slice

- Do not implement a full shader graph editor.
- Do not add Minecraft-specific `Player`, `ItemStack`, helmet, world, or client types to core APIs.
- Do not require every widget to become an offscreen layer.
- Do not run post effects per widget by default; that is too expensive and hard to reason about.
- Do not make XML execute arbitrary code or arbitrary file paths.
- Do not make HUD overlays interactive in the first slice unless the host explicitly opts in.
- Do not require post effects to be available on every backend.

---

## Terminology

- **UI Layer** - a renderable UniGUI tree or DrawList with its own bounds, scale and optional post-processing.
- **PostEffect** - a named effect definition that transforms an input texture into an output target.
- **PostEffect Pass** - one shader/fullscreen pass inside an effect chain.
- **PostEffect Chain** - ordered list of passes applied to a layer.
- **Overlay** - a backend-neutral UI layer rendered by a host outside the normal screen/window stack.
- **HUD Overlay** - an overlay intended for game HUD space; core does not know what game/player means.
- **Host** - platform integration that owns the render hook, viewport and input dispatch.

---

## Priority Order

### Phase 1 - UIPostEffect Core

Build the effect model and render pipeline first:

- `UiPostEffectDefinition`
- `UiPostEffectPass`
- `UiPostEffectChain`
- `UiPostEffectRegistry`
- `UiPostEffectUniforms`
- backend capability checks and no-op fallback

This phase should work for normal screen/UI layers before HUD exists.

### Phase 2 - Backend PostEffect Implementation

Implement post effect execution in the Minecraft backend as an adapter over the core API:

- render layer DrawList into `RenderTarget`;
- bind source texture;
- run pass shader(s);
- ping-pong between temporary targets for multi-pass effects;
- composite final texture to the active target/screen.

The Minecraft implementation may use Minecraft/OpenGL classes internally, but those types must not appear in core signatures.

### Phase 3 - Data-Driven Effects

Expose registered effect ids to XML/XAML and StylePack:

- layer-level `postEffect="effect-id"` references;
- optional inline effect chain definitions later;
- uniform bindings by id, not Java expressions inside XML;
- editor metadata for available effects and uniforms.

### Phase 4 - HUD Overlay

Add overlay host and registry once PostEffect is stable:

- `OverlayLayer`
- `OverlayRegistry`
- `OverlayHost`
- `OverlayInputPolicy`
- XML/XAML overlay root loading
- host adapter for Minecraft HUD render event

---

## UIPostEffect Core API

### Effect Definition

```java
public final class UiPostEffectDefinition {
    private final String id;
    private final List<UiPostEffectPass> passes;
    private final boolean enabledByDefault;
}
```

Rules:

- `id` is stable and namespaced, for example `mymod:helmet_distortion`.
- definitions are immutable after registration;
- definitions are data, not renderer callbacks;
- a definition may contain one pass or many passes;
- empty definitions are invalid.

### Effect Pass

```java
public final class UiPostEffectPass {
    private final String shaderId;
    private final UiPostEffectUniforms uniforms;
    private final UiPostEffectBlendMode blendMode;
}
```

Rules:

- `shaderId` references a shader already known to the backend/resource system;
- XML should reference shader ids, not raw GLSL source, in the first implementation;
- uniforms are typed values or binding references;
- pass order is significant;
- passes should not mutate widget state.

### Effect Chain

```java
public final class UiPostEffectChain {
    public static UiPostEffectChain none();
    public static UiPostEffectChain of(String effectId);
    public static UiPostEffectChain of(List<UiPostEffectPass> passes);
}
```

Rules:

- `none()` means direct render with no offscreen pass;
- `of(effectId)` resolves through `UiPostEffectRegistry` at render time or layer creation time;
- chain instances should be cheap to copy/reference;
- invalid ids should produce diagnostics in editor/XML contexts and no-op fallback at runtime.

### Uniforms

```java
public sealed interface UiPostEffectUniform permits FloatUniform, Vec2Uniform, Vec4Uniform, TextureUniform, BindingUniform {
}
```

Minimum first-slice uniform types:

- `float`
- `int`
- `boolean`
- `vec2`
- `vec3`
- `vec4`
- `color`
- `texture`
- `bindingRef`

Uniform update rules:

- static uniforms are copied into the effect instance;
- dynamic uniforms are resolved through supplier/binding ids before each pass;
- core uses plain suppliers such as `FloatSupplier`, `BooleanSupplier`, or generic `Supplier<T>`;
- host-specific state access belongs inside user-provided suppliers, not in UniGUI core.

Example:

```java
UiPostEffectDefinition helmet = UiPostEffectDefinition.create("mymod:helmet_distortion")
        .pass("unigui:barrel_distortion", uniforms -> uniforms
                .floatValue("strength", 0.085f)
                .vec2("center", 0.5f, 0.5f))
        .pass("unigui:chromatic_aberration", uniforms -> uniforms
                .floatValue("amount", 0.004f));
```

---

## Render Pipeline

### Normal Render

```text
Widget tree -> DrawList -> Backend -> Active target/screen
```

### PostEffect Render

```text
Widget tree -> DrawList
          -> offscreen RenderTarget A
          -> pass 1: A -> B
          -> pass 2: B -> A
          -> ...
          -> composite final target -> Active target/screen
```

Rules:

- post effects are applied after widgets have rendered to a layer target;
- effects must preserve UI alpha unless a pass explicitly changes it;
- target size is based on layer bounds and current UI scale;
- target size should clamp to backend limits;
- targets should be pooled/reused to avoid per-frame allocation;
- if any pass fails, backend should skip the effect and composite the original layer where possible.

### Layer Bounds

A post-effect layer needs explicit bounds:

```java
public record UiLayerBounds(float x, float y, float width, float height, float scale) {
}
```

Rules:

- full-screen layers use the current viewport bounds;
- subtree/layer effects use the layer's layout bounds;
- bounds are UI-space, not physical pixels;
- backend converts UI-space to physical target dimensions.

### Fallback Behavior

If the backend does not support post effects:

- render the UI layer normally;
- emit a diagnostic once per effect id in debug/editor mode;
- do not crash normal runtime rendering;
- preserve layout and input behavior.

---

## Backend Contract

Core should expose a small capability surface:

```java
public interface UiPostEffectBackend {
    boolean supportsPostEffects();
    void renderWithPostEffect(DrawList drawList, UiLayerBounds bounds, UiPostEffectChain chain);
}
```

The actual shape can live on `RenderBackend` or an extension interface. The important rule is that core passes only UniGUI concepts:

- `DrawList`
- `RenderTarget`
- `TextureHandle`
- `UiPostEffectChain`
- `UiLayerBounds`

No backend-specific window, screen, player, entity, GL context, or Minecraft class belongs in the public core contract.

---

## Built-In Effects

First useful built-ins:

### `unigui:passthrough`

No visual change. Useful for testing the offscreen pipeline.

Uniforms: none.

### `unigui:tint`

Applies a color multiplier/overlay to a UI layer.

Uniforms:

- `color` - RGBA color
- `amount` - `0..1`

### `unigui:vignette`

Darkens edges around a center point.

Uniforms:

- `center` - vec2, default `(0.5, 0.5)`
- `radius` - float
- `softness` - float
- `color` - color
- `amount` - float

### `unigui:barrel_distortion`

Distorts UVs from a center point. This is the main helmet/visor base effect.

Uniforms:

- `center` - vec2
- `strength` - float
- `zoom` - float

### `unigui:chromatic_aberration`

Offsets RGB channels slightly.

Uniforms:

- `amount` - float
- `direction` - vec2

### `unigui:scanline`

Adds horizontal scanlines or visor display lines.

Uniforms:

- `density` - float
- `amount` - float
- `time` - float or binding

---

## XML/XAML Syntax

### Referencing Registered Effects

First-slice XML should prefer effect ids:

```xml
<Screen postEffect="mymod:helmet_distortion">
    <Panel>
        <Label text="OXYGEN 87%" textBrush="linear-gradient(#60D8FF, #F7C45A, 20)" />
    </Panel>
</Screen>
```

If `Screen` is not a widget in a given loader path, this can be represented by host options instead:

```java
XmlWidgetScreen screen = XmlWidgetScreen.from(xml)
        .postEffect("mymod:helmet_distortion");
```

### Inline Effect Chain Later

Later syntax may allow data-defined chains:

```xml
<PostEffects>
    <PostEffect id="mymod:helmet_distortion">
        <Pass shader="unigui:barrel_distortion" strength="0.085" center="0.5 0.5" />
        <Pass shader="unigui:chromatic_aberration" amount="0.004" />
        <Pass shader="unigui:vignette" amount="0.35" radius="0.82" />
    </PostEffect>
</PostEffects>
```

Rules:

- XML references ids and scalar values only;
- XML does not contain Java code;
- raw shader code in XML is out of scope for first slice;
- unknown effect ids become diagnostics, not silent failures in editor mode.

---

## StylePack Integration

PostEffect should be a style/layer property, not a widget renderer replacement.

Candidate style keys:

```java
StyleKeys.POST_EFFECT
StyleKeys.POST_EFFECT_ENABLED
StyleKeys.POST_EFFECT_OPACITY
```

Usage:

```xml
<Style id="helmet-hud-root" target="Panel">
    <Property name="postEffect" value="mymod:helmet_distortion" />
</Style>
```

Rules:

- style can select effect id;
- style can override uniforms later;
- procedural `WidgetRenderer` still generates normal DrawCommands before the effect stage;
- post effects consume final layer pixels and do not need to know which renderer produced them.

---

## HUD Overlay Core API

HUD Overlay is a later feature built on regular widget trees and optional post effects.

### Overlay Layer

```java
public final class OverlayLayer {
    private final String id;
    private final Widget root;
    private final BooleanSupplier visible;
    private final int zIndex;
    private final OverlayInputPolicy inputPolicy;
    private final UiPostEffectChain postEffect;
}
```

Rules:

- `id` is stable and namespaced;
- `root` is a normal UniGUI widget tree;
- `visible` is `BooleanSupplier`, not host-specific context;
- `zIndex` orders overlays within the host;
- default input policy is `PASS_THROUGH`;
- post effect is optional;
- overlay state must not depend on `WindowWidget` behavior.

Example:

```java
OverlayLayer helmetHud = OverlayLayer.create("mymod:helmet_hud")
        .root(helmetRoot)
        .visible(ClientHudState::hasHelmet)
        .zIndex(100)
        .inputPolicy(OverlayInputPolicy.PASS_THROUGH)
        .postEffect("mymod:helmet_distortion");
```

### Overlay Registry

```java
public final class OverlayRegistry {
    public void register(OverlayLayer layer);
    public boolean remove(String id);
    public List<OverlayLayer> visibleLayers();
}
```

Rules:

- registry is core and host-neutral;
- host adapters decide when to render registered overlays;
- overlay ordering is deterministic by `zIndex`, then registration order;
- hidden overlays should not render or allocate post-effect targets.

### Input Policy

```java
public enum OverlayInputPolicy {
    PASS_THROUGH,
    CAPTURE_POINTER,
    CAPTURE_KEYBOARD,
    CAPTURE_ALL
}
```

First slice should implement only `PASS_THROUGH` for HUD overlays. Other modes should be specified but can throw/diagnose until the host supports them.

---

## HUD XML/XAML

HUD overlay XML should describe UI and references, not host state:

```xml
<HudOverlay id="mymod:helmet_hud"
            visibleBinding="helmet.visible"
            zIndex="100"
            inputPolicy="pass-through"
            postEffect="mymod:helmet_distortion">
    <Panel>
        <Label text="OXYGEN 87%" textBrush="linear-gradient(#60D8FF, #F7C45A, 20)" />
    </Panel>
</HudOverlay>
```

Java registers the binding:

```java
OverlayBindings.register("helmet.visible", ClientHudState::hasHelmet);
```

Rules:

- XML uses `visibleBinding`, not inline Java;
- missing binding defaults to visible or produces a diagnostic depending on strict mode;
- `HudOverlay` root element should be optional sugar around `OverlayLayer`, not a widget subclass;
- the child content is still a normal UniGUI widget tree.

---

## Host Integration

### Generic Host Responsibilities

A host implementation must provide:

- current viewport size;
- UI scale;
- render hook timing;
- render target creation and pooling;
- final composition to active surface;
- optional input dispatch for interactive overlays.

### Minecraft Host Responsibilities

Minecraft integration should live outside core and provide:

- registration bridge from mod init/client init;
- HUD render event hook;
- current window GUI scale;
- `RenderTarget` implementation;
- shader binding and pass execution;
- optional debug diagnostics.

Minecraft-specific state access belongs in user code:

```java
OverlayLayer.create("mymod:helmet_hud")
        .visible(() -> Minecraft.getInstance().player != null && ClientHudState.hasHelmet())
```

This keeps core clean while still allowing mods to query whatever client state they need.

---

## Performance Requirements

- No per-frame allocation of render targets.
- No post-effect target allocation for invisible overlays.
- Reuse ping-pong targets by size/format.
- Avoid running effects for fully transparent layers when detectable.
- Allow disabling post effects globally for low-end clients.
- Provide debug counters: target count, pass count, total post-effect time if backend supports timers.
- Multi-pass effects should be opt-in and clearly visible in editor diagnostics.

---

## Diagnostics

Editor/runtime diagnostics should cover:

- unknown effect id;
- unsupported backend post effects;
- missing shader id;
- missing required uniform;
- invalid uniform type;
- render target allocation failure;
- pass execution failure;
- recursive or invalid effect chain definition;
- missing overlay visible binding.

Diagnostics should include the effect id, pass index, shader id and source location when loaded from XML.

---

## Testing Plan

### Unit Tests

- Parse effect ids and uniform values.
- Validate effect definitions reject blank ids and empty pass lists.
- Resolve registered effects by id.
- Sort overlays by z-index deterministically.
- Verify `BooleanSupplier` visibility gates rendering.

### Backend Smoke Tests

- `unigui:passthrough` renders identical UI through offscreen target.
- `unigui:tint` visibly affects a layer.
- Multi-pass chain ping-pongs without losing alpha.
- Unsupported backend falls back to normal render.

### Editor Tests

- XML with `postEffect="missing:id"` reports a diagnostic.
- XML with `visibleBinding="missing.binding"` reports a diagnostic in strict mode.
- Effect metadata appears in Properties/Inspector.
- Toggle post effect on/off without changing widget XML structure.

---

## Open Questions

- Should `postEffect` be allowed on any widget through an `EffectLayer` wrapper, or only on screen/overlay/layer roots first?
- Should StylePack define full post-effect chains or only reference registered effect ids in the first version?
- Should raw GLSL be allowed in dev tools later, or should all shaders always be registered assets?
- How should interactive HUD overlays coexist with game input on each backend?
- Do we need separate target formats for HDR/bloom-like UI effects, or is normal RGBA enough for first slice?

---

## Definition Of Done

First useful `UIPostEffect` slice is complete when:

- core has effect definition, pass, registry and chain objects;
- backend can render a UI layer to an offscreen target and composite it back;
- at least `passthrough`, `tint`, `vignette`, and `barrel_distortion` are available;
- XML/StylePack can reference a registered effect id;
- unsupported backends gracefully render without effects;
- compile/tests pass;
- no core API imports Minecraft or other host-specific classes.

HUD Overlay slice is complete when:

- overlays are registered through a backend-neutral registry;
- overlay root is a normal widget tree;
- visibility uses `BooleanSupplier` or binding id;
- host renders visible overlays in deterministic order;
- default input policy is pass-through;
- overlays can optionally reference a `UiPostEffectChain`.