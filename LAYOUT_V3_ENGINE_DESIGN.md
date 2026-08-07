# UniGUI Layout v3 Engine Design

Status: design only. Layout v3 is not implemented yet.

This document describes a future migration from the current custom flex resolver to a pluggable layout architecture with Yoga as the preferred flexbox engine. It does not deprecate the Layout v2 public API and does not require an immediate rewrite.

## Decision Summary

- Keep the public UniGUI layout API independent from Yoga.
- Preserve `LayoutStyle`, `SizeValue`, `PositionType`, `FlexDirection`, `FlexWrap`, `Align`, `Justify`, `Overflow` and the legacy compatibility helpers.
- Introduce an internal layout-engine SPI.
- Use Yoga for standard flexbox calculation after its Java binding and Minecraft runtime packaging have been validated.
- Keep specialized UniGUI layout strategies for `DockPanel`, `GridBox`, scrolling viewports, virtualized content and behavioral overlays.
- Never expose `YogaNode`, Yoga enums or binding-specific types from public widget APIs.
- Migrate incrementally and compare Yoga results against the current engine in tests and an optional verification mode.
- Do not remove the current custom resolver until Yoga passes the complete test matrix on all supported loaders and operating systems.

The target architecture is therefore not "Yoga replaces the whole UI framework". Yoga is a geometry solver inside UniGUI. Rendering, clipping, scrolling, input, invalidation, animation and widget behavior remain owned by UniGUI.

## Motivation

Layout v2 provides the subset currently needed by UniGUI:

- row and column flex layouts;
- wrapping;
- grow, shrink and basis;
- pixels, percentages and automatic sizes;
- min/max constraints;
- margin, padding and gaps;
- alignment and justification;
- relative and absolute positioning;
- overflow contracts and scrollable hosts.

The custom implementation is useful and gives UniGUI predictable Minecraft-specific behavior. However, extending it toward full flexbox compatibility creates a growing number of difficult edge cases:

- nested percentage and automatic sizes;
- repeated min/max clamping during grow and shrink;
- wrapped line sizing and cross-axis alignment;
- intrinsic measurement under exact, limited and unlimited constraints;
- absolute children mixed with flex children;
- rounding and fractional coordinates;
- layout invalidation across deep trees;
- behavior differences between leaf widgets and containers.

Yoga has spent years solving and testing these classes of flexbox problems. Layout v3 should reuse that work while retaining control over the parts that are specific to UniGUI and Minecraft.

## Goals

- Improve correctness for complex and deeply nested flex layouts.
- Keep application and widget code source-compatible with Layout v2 where practical.
- Make the concrete layout engine replaceable.
- Support a controlled fallback to the current custom resolver during migration.
- Keep layout deterministic and independent from rendering.
- Avoid rebuilding and allocating an entire external layout tree every frame.
- Integrate external layout calculation with existing measure/arrange and invalidation contracts.
- Package the selected implementation safely for Fabric, Forge and NeoForge.
- Expose useful layout metrics through the existing profiler/debug overlay.

## Non-Goals

Layout v3 is not intended to provide:

- a complete browser CSS implementation;
- CSS Grid through Yoga;
- stylesheets or an LSS parser;
- text shaping or text rendering;
- clipping and scissor implementation inside Yoga;
- scrollbar rendering or pointer handling inside Yoga;
- animation, transforms or hit testing inside Yoga;
- automatic replacement of specialized graph, canvas or virtual-list layouts;
- public access to binding-specific Yoga objects.

## Current State

Layout v2 currently uses:

- `FlexLayoutEngine` for `LinearBox`, `HBox`, `VBox` and `WrapPanel`;
- `AbsoluteLayoutEngine` for out-of-flow children and host-constrained placement;
- specialized arrangement code in containers such as `DockPanel`, `GridBox`, `StackPanel`, `ScrollView` and `OverlayLayer`;
- `Widget.measure(LayoutContext)` and `Widget.arrange(RectView)` as the public widget lifecycle;
- `LayoutStyle` as the advanced public style contract;
- legacy helpers such as `preferredSize`, `minSize`, `maxSize`, `margin`, `align` and `grow` as compatibility APIs.

The repository currently declares `com.github.somehussar:yoga:1.0.1-java8` as `compileOnly` in `common` and as a Minecraft client runtime library in Fabric and Forge. There are no Yoga API calls in the current Java source. These declarations are historical placeholders and must not be treated as the final Layout v3 dependency decision.

## Architectural Boundary

The intended flow is:

```text
Widget tree
    |
    v
LayoutStyle + intrinsic measurement
    |
    v
UniGUI LayoutEngine SPI
    |
    +--> YogaFlexLayoutEngine
    |
    +--> CustomFlexLayoutEngine
    |
    +--> Dock/Grid/Stack/Virtual custom strategies
    |
    v
UniGUI layout rectangles
    |
    +--> arrange widgets
    +--> clipping and ScrollView
    +--> transforms and hit testing
    +--> rendering
```

Yoga owns only the calculation of standard flex geometry for a Yoga-managed layout island. UniGUI owns the widget tree and the final `layoutBounds`.

## Public API Contract

Layout v3 should initially require no new Yoga-specific public API.

Existing code remains valid:

```java
widget.layout(style -> style
        .widthPercent(100.0f)
        .minWidth(0.0f)
        .padding(8.0f)
        .flex(1.0f, 1.0f, SizeValue.auto())
        .overflowY(Overflow.AUTO));
```

Legacy compatibility remains valid:

```java
widget.preferredSize(120.0f, 24.0f)
        .minSize(0.0f, 20.0f)
        .grow(1.0f);
```

The compatibility bridge continues to update `LayoutStyle`. The chosen engine reads the normalized `LayoutStyle`; it does not read a separate Yoga-facing public configuration.

Public contracts must preserve these rules:

- `Visibility.COLLAPSED` removes a widget from layout, rendering and input traversal.
- `SizeValue.auto()` means content-driven sizing or an engine-assigned flex size.
- percentages resolve against the relevant parent content box.
- min/max constraints clamp final dimensions.
- visual transforms do not alter the layout calculation.
- overflow describes UniGUI clipping/scroll behavior, even when a corresponding Yoga value exists.
- no application code may depend on Yoga rounding, node identity or binding lifecycle.

## Internal Layout Engine SPI

The exact API can be adjusted during implementation, but the boundary should resemble:

```java
interface LayoutEngine {
    LayoutEngineId id();

    void synchronize(LayoutTree tree);

    LayoutPassResult calculate(
            Widget root,
            float availableWidth,
            float availableHeight);

    void markStyleDirty(Widget widget);

    void markMeasureDirty(Widget widget);

    void detach(Widget widget);

    void dispose();
}
```

Supporting internal types may include:

```text
LayoutTree
LayoutNodeHandle
LayoutPassResult
LayoutEngineId
LayoutDiagnostics
IntrinsicMeasureFunction
MeasureMode: EXACTLY | AT_MOST | UNDEFINED
```

The SPI must support at least three development configurations:

- `CUSTOM`: current UniGUI implementation only.
- `YOGA`: Yoga implementation only.
- `VERIFY`: calculate selected test subtrees with both engines and report differences without arranging twice.

`VERIFY` is a development and test feature, not a normal production mode.

## Layout Islands and Mixed Engines

Not every UniGUI container should become a Yoga container. The tree is divided into layout islands.

A layout island is a subtree whose geometry is owned by one layout strategy. A custom container appears as a measurable leaf to its parent island and then arranges its own internal island.

Rules:

- Exactly one strategy owns each widget rectangle during a layout pass.
- A child must never be arranged once by Yoga and again by the custom flex resolver.
- A custom island reports its intrinsic size to its parent through the normal measure contract.
- Parent constraints are passed into the child island explicitly.
- Engine boundaries must not change rendering order, event routing or widget parentage.

Recommended ownership:

| Component | Layout v3 owner |
| --- | --- |
| `HBox`, `VBox`, `LinearBox` | Yoga flex island |
| `WrapPanel` | Yoga flex island with wrapping |
| Generic relative flex children | Yoga |
| Generic absolute inset child | Yoga if binding behavior is validated |
| `DockPanel` | UniGUI custom strategy |
| `GridBox` | UniGUI custom strategy until a separate grid engine is chosen |
| `StackPanel` | UniGUI custom strategy or a trivial dedicated strategy |
| `ScrollView` viewport and scroll state | UniGUI custom strategy |
| Virtual list/table item positioning | virtualization strategy |
| Tooltip/Popup anchor flip and clamp | overlay placement strategy |
| Draggable `WindowWidget` constraints | overlay/window strategy |

Using Yoga for generic `position: absolute` is optional. If enabled, Yoga may calculate the initial inset-based rectangle, while UniGUI still performs behavioral anchor placement, flipping and host clamping. The same widget must not be owned by both paths in one pass.

## Yoga Node Model

The preferred implementation uses persistent Yoga nodes rather than creating a complete Yoga tree every frame.

Each Yoga-managed widget receives an internal handle:

```text
Widget
  -> LayoutNodeHandle
       -> binding-specific Yoga node
       -> last synchronized style revision
       -> last tree revision
       -> measurement cache
       -> diagnostic identity
```

The handle is internal and must be disposed when:

- the widget is disposed;
- the widget permanently leaves the UI tree;
- the engine instance is replaced;
- a screen or UI context is destroyed.

Child insertion, removal and reordering must update the Yoga tree at the same UI-frame safe point used for widget-tree mutations.

Implementation collections should prefer `it.unimi.dsi.fastutil` where primitive IDs or performance-sensitive registries are useful, for example node IDs and dirty-node sets. Standard Java Collections remain the fallback when fastutil has no suitable structure or provides no meaningful benefit.

## Mapping LayoutStyle to Yoga

The adapter is the only place that translates UniGUI concepts into binding-specific values.

| UniGUI value | Yoga mapping |
| --- | --- |
| `SizeValue.auto()` | automatic or undefined dimension, depending on the property |
| `SizeValue.px(v)` | point value `v` in Minecraft GUI units |
| `SizeValue.percent(v)` | percentage value `v` |
| `PositionType.RELATIVE` | relative position |
| `PositionType.ABSOLUTE` | absolute position when owned by Yoga |
| `FlexDirection.ROW/COLUMN` | corresponding Yoga flex direction |
| `FlexWrap.NOWRAP/WRAP` | corresponding Yoga wrap mode |
| `flexGrow` | Yoga grow value |
| `flexShrink` | Yoga shrink value |
| `flexBasis` | Yoga basis value |
| `Align` | corresponding Yoga align value |
| `Justify` | corresponding Yoga justify value |
| margin/padding | per-edge Yoga values |
| row/column gap | Yoga gap API if supported by the selected binding |
| min/max sizes | corresponding Yoga min/max dimensions |
| left/top/right/bottom | corresponding Yoga position edges |

Important translation rules:

- `auto` and `undefined` are not always interchangeable. The adapter must map them per property.
- Non-finite public inputs must be normalized before reaching Yoga.
- Negative dimensions, grow/shrink values and gaps follow UniGUI validation rules, not arbitrary binding behavior.
- If the selected binding lacks row/column gap support, the limitation must be resolved before migration. Silent margin-based emulation is not equivalent for wrapping and outer edges.
- Yoga overflow values must not replace UniGUI clipping and scrolling behavior.
- `Visibility.COLLAPSED` should initially omit the node from the active Yoga tree. It should not require reintroducing a public display mode.

## Intrinsic Measurement

Yoga can calculate flex geometry, but leaf widgets still need UniGUI to report intrinsic content sizes.

Measured leaves include:

- text blocks and labels;
- buttons and controls whose size depends on text;
- images and textures with intrinsic dimensions;
- text fields and number fields;
- custom widgets that explicitly provide content measurement.

The adapter converts Yoga measurement constraints into a UniGUI measurement request:

```text
EXACTLY   -> content must use the supplied size on that axis
AT_MOST   -> content may use up to the supplied size
UNDEFINED -> content is measured without an axis limit
```

Measurement callbacks must:

- be pure with respect to widget-tree structure;
- never render;
- never dispatch input events;
- never add or remove children;
- return finite, non-negative sizes;
- use the same `TextEngine` metrics as rendering;
- be cached by content/style revision and normalized constraints;
- execute on the UI thread unless a future binding and widget contract explicitly support otherwise.

Container widgets should normally be represented by Yoga children rather than custom measure callbacks. A custom layout island is the exception: it exposes a measured outer node to its Yoga parent and lays out its own descendants separately.

## Measure and Arrange Integration

Layout v3 retains the conceptual two-stage lifecycle:

1. Measure determines intrinsic or desired size under constraints.
2. Arrange assigns final rectangles.

For a Yoga island, one Yoga calculation may produce both the desired geometry and final child rectangles. The adapter must still present results through existing UniGUI contracts.

Recommended behavior:

- the island root receives available width and height from its parent;
- leaf measurement callbacks provide intrinsic sizes;
- Yoga calculates local rectangles relative to the island root;
- the adapter converts local rectangles to UniGUI coordinates;
- `arrange` stores the final `layoutBounds` without recalculating unrelated islands;
- visual transforms are applied later by the transform/render pipeline.

The implementation must avoid recursive cycles where Yoga measurement calls `Widget.measure`, which enters the same Yoga island again. Internal measurement entry points should distinguish intrinsic leaf measurement from full subtree layout.

## ScrollView Contract

Yoga must not own scrolling behavior.

`ScrollView` remains responsible for:

- viewport size;
- content extent;
- horizontal and vertical offsets;
- scrollbar visibility and thumb geometry;
- pointer capture and dragging;
- wheel input, including `Shift + wheel` horizontal scrolling;
- clipping through the render backend;
- culling or virtualization cooperation.

Yoga may size the `ScrollView` itself as a child of a flex island. Inside the viewport, UniGUI chooses measurement constraints by axis:

- a non-scrollable axis is constrained to the viewport;
- a scrollable axis may be measured as undefined/unbounded;
- explicit virtual/canvas content sizes remain authoritative;
- scrollbar reservation must be fed back into viewport constraints when required.

This policy prevents a vertical `ScrollView` from accidentally creating infinite horizontal content.

## Absolute Layout and Overlays

Layout v3 separates static absolute geometry from behavioral overlay placement.

Static absolute geometry consists of:

- `left`, `top`, `right`, `bottom`;
- width, height and percentages;
- min/max clamping;
- positioning relative to the parent content box.

Behavioral overlay placement consists of:

- anchoring to another widget or pointer position;
- selecting above/below/left/right placement;
- flipping when space is insufficient;
- clamping to a host or screen;
- dragging a window;
- keeping tooltips and popups above normal content.

Yoga may eventually own static absolute geometry. `AbsoluteLayoutEngine` or a renamed `OverlayPlacementEngine` continues to own behavioral placement. The implementation must define a clear hand-off rectangle so that Yoga and overlay placement do not fight over the same coordinates.

## Invalidation and Dirty Propagation

Layout v3 needs explicit dirty categories:

| Dirty category | Example causes |
| --- | --- |
| Style dirty | width, padding, flex, alignment or position changed |
| Measure dirty | text, font, image or intrinsic content changed |
| Tree dirty | child added, removed, moved or collapsed |
| Constraint dirty | parent size, UI scale or viewport changed |
| Visual only | color, hover state or texture changed without geometry impact |

Rules:

- `LayoutStyle.onChanged` marks the corresponding engine node style-dirty.
- intrinsic content changes mark the measured leaf and its dependent ancestors measure-dirty.
- child-list changes synchronize the external node tree before calculation.
- screen resize and GUI scale changes invalidate root constraints.
- visual-only invalidation must not trigger Yoga calculation.
- Yoga dirty APIs are called only according to the selected binding's rules; some bindings permit explicit dirtying only for measured leaves.
- layout invalidation must remain safe when widgets are mutated through the UI mutation queue.

The first implementation may recalculate an entire affected island. Fine-grained island caching should be introduced only after profiler data shows that it is necessary.

## Rounding and Coordinate Policy

Minecraft GUI coordinates and UniGUI rectangles use floating-point values. Layout v3 must define rounding instead of inheriting accidental binding defaults.

Initial policy:

- keep layout calculations in float GUI units;
- do not round each intermediate child independently in UniGUI;
- apply one consistent final snapping policy only when a renderer or scissor requires integer boundaries;
- use floor for clip origins and ceil for clip extents so visible pixels are not accidentally cut off;
- ensure adjacent children do not accumulate visible one-pixel gaps due to inconsistent rounding;
- test behavior at multiple Minecraft GUI scales.

If the Yoga binding exposes a point-scale-factor configuration, UniGUI must configure it explicitly and document whether it is `0`, `1` or tied to GUI scale. The value must be identical in tests and production unless the test intentionally covers scale behavior.

## Binding Selection

Yoga is the preferred engine family, but no Java binding is approved by this document.

Before implementation, compare available bindings using these criteria:

- supported Yoga version and flexbox errata/configuration options;
- Java and Minecraft version compatibility;
- pure Java versus JNI/native implementation;
- active maintenance and reproducible artifacts;
- license compatibility and required notices;
- support for Windows, Linux and macOS;
- support for x86-64 and ARM64 where UniGUI intends to run;
- Fabric, Forge and NeoForge classloading behavior;
- safe native extraction if JNI is used;
- row/column gap support;
- measure callbacks and measure modes;
- percentages, min/max and absolute positioning;
- deterministic float and rounding behavior;
- memory ownership and explicit node disposal;
- useful diagnostics and assertion behavior;
- shading/relocation feasibility;
- artifact size and startup cost.

The existing `com.github.somehussar:yoga:1.0.1-java8` dependency must be evaluated under the same criteria. Its current presence is not sufficient evidence that it is suitable.

## Runtime Packaging

Layout v3 must not rely on a development-only runtime library declaration.

Possible packaging models:

### Pure Java binding

- bundle and relocate the implementation into the produced mod jar;
- preserve required license files;
- verify that remapping does not alter external package references;
- ensure consumers do not need to install a separate mod or library.

### JNI/native binding

- bundle supported natives by OS and architecture, or publish clearly defined platform artifacts;
- extract natives to a safe versioned cache location;
- prevent duplicate loading across mod classloaders;
- provide useful diagnostics for unsupported platforms;
- verify signed/notarized platform requirements where relevant;
- test loading from both development and production/remapped jars.

### Optional external dependency

This is the least desirable default for UniGUI consumers. If used, metadata must declare the dependency correctly for every loader, and startup must fail with a clear message rather than `ClassNotFoundException` or `UnsatisfiedLinkError` deep inside a frame.

The final packaging choice must be tested from clean Minecraft instances, not only Gradle run configurations.

## Failure and Fallback Policy

During development, Yoga initialization failure may fall back to the custom engine and emit a prominent diagnostic.

For production, the policy must be selected deliberately:

- if the packaged Yoga engine is mandatory, fail early during UI initialization with a clear platform/binding error;
- if the custom engine is an officially supported fallback, both engines must pass the same public behavior tests and documented differences must be minimal;
- never switch engines silently in the middle of an active layout pass;
- never leave partially synchronized external nodes attached after a failed calculation.

A fallback that produces materially different geometry is not a reliable fallback. It is only a debugging escape hatch until parity is demonstrated.

## Verification Mode

Verification mode is the safest migration tool.

For selected deterministic subtrees:

1. Snapshot normalized layout inputs.
2. Calculate geometry with the current custom resolver.
3. Calculate geometry with Yoga.
4. Use only the configured primary result for actual arrangement.
5. Compare desired sizes and child rectangles with a configurable epsilon.
6. Record mismatches in tests and profiler diagnostics.

Expected differences must be explicitly classified:

- a bug in the current custom engine;
- a bug or unsupported behavior in the adapter/binding;
- an intentional Layout v3 semantic change;
- rounding noise below the accepted threshold.

Verification must not execute impure measurement callbacks twice unless the callback contract has first been made pure and deterministic.

## Profiler and Debug Overlay

Add Layout v3 counters to the common profiler/debug overlay:

- active layout engine;
- total layout pass CPU ms;
- Yoga calculation CPU ms;
- adapter synchronization CPU ms;
- number of layout islands calculated;
- total external nodes and peak nodes;
- style-dirty, measure-dirty and tree-dirty node counts;
- intrinsic measurement callback count;
- measurement cache hits and misses;
- full-tree versus incremental calculations;
- verification mismatch count;
- node creation and disposal count;
- last layout failure or fallback reason.

Counters should be cheap when the overlay is disabled. Debug-only tree dumps must be opt-in and bounded so large virtualized UIs do not flood logs.

## Performance Rules

- Reuse persistent nodes and handles.
- Do not rebuild external style state when the `LayoutStyle` revision has not changed.
- Do not allocate wrapper objects per node per frame where primitive or reusable storage is sufficient.
- Prefer fastutil primitive collections for performance-sensitive ID maps and dirty sets when appropriate.
- Do not send virtualized offscreen rows into Yoga if the virtualization strategy does not need them for extent calculation.
- Cache pure intrinsic measurements.
- Keep Yoga calculation on the UI thread for the initial implementation.
- Optimize only against profiler evidence and repeatable layout benchmarks.

The initial success criterion is correctness without unacceptable frame spikes. Micro-optimizing node synchronization before parity tests pass is not a priority.

## Test Matrix

All existing Layout v2 tests remain mandatory. Layout v3 adds engine-independent parameterized tests so the same scenarios can run against `CUSTOM` and `YOGA`.

### Flex sizing

- row and column grow distribution;
- weighted grow;
- shrink with equal and unequal factors;
- fixed plus flexible children;
- flex basis in pixels and percentages;
- min/max clamping during grow and shrink;
- zero available size;
- finite size below total gaps and margins;
- deeply nested flex containers.

### Wrapping and alignment

- row and column wrapping;
- oversized child in a wrapped line;
- multiple wrapped lines with row/column gaps;
- justify start, center, end and space modes supported by the API;
- align start, center, end and stretch;
- per-child `alignSelf`;
- margins combined with alignment.

### Sizing values

- pixel, percent and auto dimensions;
- percent child under parent padding;
- percent combined with min/max;
- auto-sized text under finite and undefined constraints;
- explicit zero versus undefined/auto;
- non-finite and invalid input normalization.

### Absolute and specialized layout

- absolute child does not affect flex desired size;
- all inset combinations;
- percent insets and sizes;
- Yoga-to-overlay hand-off;
- tooltip flip and clamp;
- popup anchor placement;
- window drag constraints;
- nested custom layout island inside Yoga and Yoga island inside custom layout.

### Scrolling and overflow

- vertical-only content does not expand horizontally;
- horizontal-only content uses horizontal extent;
- two-axis scrolling;
- auto scrollbar visibility feedback;
- Shift + wheel horizontal scrolling;
- clipping remains balanced;
- virtualization excludes invisible rows without changing total extent.

### Lifecycle and invalidation

- style mutation dirties the correct island;
- text mutation invalidates intrinsic measurement;
- add/remove/reorder/collapse child synchronization;
- dispose releases all external nodes;
- resize and GUI scale recalculate constraints;
- visual-only changes do not trigger layout;
- repeated open/close cycles do not leak nodes or native memory.

### Platform packaging

- common tests;
- Fabric production/remapped build and clean-client launch;
- Forge production/remapped build and clean-client launch;
- NeoForge production/remapped build and clean-client launch;
- supported operating systems and CPU architectures;
- missing or corrupted binding produces the documented startup behavior.

## TestCommands Sample

Layout v3 implementation work must add a dedicated sample to `TestCommands` while preserving `Checkbox debugTools`.

The sample should provide:

- engine selector when development builds include both engines;
- constrained viewport width/height controls;
- nested row/column and wrapped examples;
- long text and intrinsic measurement examples;
- grow/shrink/basis controls;
- percentage and min/max examples;
- absolute overlay and `ScrollView` examples;
- optional rectangle outlines and node/island IDs;
- verification mismatch status.

The sample itself must fit small Minecraft GUI scales and remain accessible through the scrollable left navigation and scrollable sample viewport.

## Migration Phases

### Phase 0: Binding Spike

- Evaluate candidate Yoga bindings.
- Verify Java/Minecraft compatibility and licenses.
- Run a standalone row/column/wrap/measure prototype.
- Test the binding from Fabric, Forge and NeoForge development runs.
- Decide pure Java versus JNI packaging.
- Record selected version, source, license and platform matrix.

Definition of done:

- one binding is approved or Yoga is explicitly rejected with evidence;
- gap, percentages, measurement callbacks and node disposal are verified;
- a production packaging approach is known.

### Phase 1: Engine-Neutral Internal SPI

- Introduce the internal `LayoutEngine` boundary.
- Adapt the existing custom resolver behind it.
- Add layout island ownership and diagnostics.
- Keep output unchanged.

Definition of done:

- existing tests pass through the SPI;
- no public API references Yoga;
- no widget is owned by two strategies.

### Phase 2: Yoga Adapter and Intrinsic Measurement

- Implement persistent node handles.
- Map `LayoutStyle` to Yoga.
- Implement pure intrinsic measurement callbacks and caching.
- Add lifecycle and disposal tests.

Definition of done:

- representative leaf and nested flex trees calculate correctly;
- no node leaks occur across screen open/close cycles;
- invalid values fail or normalize predictably.

### Phase 3: Dual-Engine Flex Migration

- Run parameterized tests against both engines.
- Add `VERIFY` mode.
- Migrate `LinearBox`, `HBox`, `VBox` and `WrapPanel` to Yoga islands.
- Add profiler counters and the `TestCommands` sample.

Definition of done:

- required flex tests pass;
- mismatches are understood and classified;
- existing UI samples remain usable at small GUI scales.

### Phase 4: Mixed Layout Boundaries

- Integrate `DockPanel`, `GridBox`, `StackPanel` and custom layout islands.
- Formalize `ScrollView` constraint hand-off.
- Decide ownership of generic absolute children.
- Rename or split `AbsoluteLayoutEngine` if needed.

Definition of done:

- mixed engine nesting is deterministic;
- scrolling, overlays and virtualization remain behaviorally unchanged;
- no double arrangement occurs.

### Phase 5: Production Packaging

- Bundle or declare the selected dependency correctly.
- Add required notices and licenses.
- Test clean production clients on supported platforms.
- Add early startup diagnostics.

Definition of done:

- users do not manually install an undeclared library;
- no `ClassNotFoundException` or `UnsatisfiedLinkError` occurs on supported platforms;
- final jars contain exactly the intended implementation and natives.

### Phase 6: Default Switch and Cleanup

- Make Yoga the default flex engine after acceptance criteria pass.
- Keep the custom engine temporarily for verification/fallback.
- Remove obsolete direct static resolver calls.
- Update architecture documents and JavaDoc.
- Decide whether and when the custom flex engine can be retired.

Definition of done:

- Yoga is the production default;
- all platform builds and tests pass;
- profiler data shows acceptable layout cost;
- public migration notes document intentional semantic changes.

## Acceptance Criteria

Yoga may become the default only when all of the following are true:

- the selected binding has a documented version, source and compatible license;
- Fabric, Forge and NeoForge packaging works from clean production instances;
- all engine-independent layout tests pass;
- existing widget and cached-subtree tests pass;
- `TestCommands` samples work at small and automatic GUI scales;
- scrolling, clipping, pointer capture and overlays show no regressions;
- measurement uses the same text metrics as rendering;
- repeated screen lifecycle tests show no node/native-memory leak;
- layout CPU time and frame spikes are acceptable in representative large trees;
- no Yoga type is exposed by the public UniGUI API;
- every intentional difference from Layout v2 is documented.

## Open Decisions

The implementation phase must resolve:

- which Yoga Java binding and Yoga version to use;
- whether native code is acceptable for UniGUI distribution;
- whether generic absolute children are owned by Yoga or the custom absolute strategy;
- whether point-scale rounding is disabled or tied to Minecraft GUI scale;
- whether a supported production fallback is worth its maintenance cost;
- how long verification mode and the custom resolver remain available;
- whether `GridBox` eventually uses a separate mature grid solver;
- which layout semantics are compatibility requirements and which may intentionally move closer to Yoga.

## Risks

| Risk | Mitigation |
| --- | --- |
| Binding is outdated or abandoned | Evaluate multiple bindings; do not commit the public API to one implementation |
| Native library fails on a platform | Clean-client platform matrix, early diagnostics and deliberate fallback policy |
| Yoga semantics change existing screens | Dual-engine tests, verification mode and documented compatibility cases |
| Text measurement becomes recursive or inconsistent | Separate intrinsic measurement entry point and use shared `TextEngine` metrics |
| External nodes leak | Explicit handles, disposal counters and repeated lifecycle tests |
| ScrollView receives unbounded constraints on the wrong axis | Centralized viewport constraint policy and regression tests |
| Custom and Yoga engines arrange the same widget | Explicit layout-island ownership assertions |
| Binding lacks required gap or errata behavior | Reject it or add an explicit API-compatible solution before migration |
| Layout work causes frame spikes | Persistent nodes, dirty synchronization, measurement cache and profiler counters |

## Final Target

The desired Layout v3 architecture is:

- UniGUI owns the API, widget tree and lifecycle.
- Yoga is the preferred implementation for standard flexbox geometry.
- Specialized UniGUI strategies own dock, grid, scrolling, virtualization and behavioral overlays.
- Layout engines are hidden behind an internal replaceable boundary.
- The migration is test-driven, observable and reversible until production acceptance criteria pass.

This approach gains Yoga's mature flexbox behavior without making the rest of UniGUI dependent on Yoga's API, runtime model or platform-specific implementation details.
