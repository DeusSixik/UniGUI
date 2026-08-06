# UniGUI Layout v2 Design

Implementation status:

- Phase 1 data model and WidgetBase compatibility bridge are implemented.
- Phase 2 shared FlexLayoutEngine is implemented for LinearBox, HBox, VBox and WrapPanel.
- Phase 3 overflow clipping and two-axis ScrollView contracts are implemented.
- Phase 4 absolute children and host-constrained Tooltip, Popup and WindowWidget overlays are implemented.
- Phase 5 public API cleanup, compatibility bridge stabilization and JavaDoc examples are implemented.

## Goal

Layout v2 should make UniGUI predictable on small Minecraft GUI scales without adding per-widget hacks.

The current layout model already has useful pieces: preferredSize, minSize, maxSize, margin, Alignment, grow, HBox, VBox, WrapPanel, DockPanel, ScrollView, OverlayLayer. The problem is that the rules are not expressive enough:

- preferredSize sometimes behaves like a desired size and sometimes like a hard size.
- There is grow, but no shrink, so fixed-width children easily overflow.
- WrapPanel can wrap children, but oversized children need a clear shrink/clamp contract.
- ScrollView must support vertical scrolling without accidental horizontal overflow.
- Tooltip, Popup and WindowWidget need absolute positioning, but must still be constrained by host bounds.

LDLib/Taffy is a good inspiration point because it separates layout calculation from rendering and models layout with CSS-like concepts. UniGUI should adopt the useful subset, not clone full CSS.

## Design Principles

- Layout must be deterministic: the same tree and constraints produce the same rectangles.
- Containers own arrangement rules; widgets only declare layout intent.
- Rendering must not affect layout.
- Small screens are first-class: shrink, wrap and scroll must be normal behavior.
- Defaults should be safe: a child should not overflow horizontally unless it explicitly asks for it.
- The old API must keep working while Layout v2 is introduced.

## Concepts to Adapt from LDLib

Use these concepts:

- Relative positioning: normal children participate in parent layout.
- Absolute positioning: overlays, windows and popups do not reserve space in parent layout.
- Size values: auto, pixels and percent.
- Min/max constraints.
- Flex direction: row and column.
- Flex wrapping.
- Flex grow, shrink and basis.
- Gap between children.
- Align items, justify content and align self.
- Margin and padding as a box model.
- Overflow modes: visible, hidden, scroll and auto.

Defer these for later:

- Full CSS Grid.
- Stylesheet/LSS syntax.
- Full Taffy compatibility.
- Complex CSS edge cases such as percentage-in-auto cycles.
- Baseline alignment.
- Multi-pass intrinsic text layout beyond the simple TextEngine metrics.

## Proposed API Shape

Layout v2 should add a new style object beside the current LayoutConstraints.

Example widget layout:

    widget.layout(layout -> layout
            .position(PositionType.RELATIVE)
            .width(SizeValue.px(120.0f))
            .height(SizeValue.auto())
            .minWidth(SizeValue.px(0.0f))
            .maxWidth(SizeValue.percent(100.0f))
            .margin(6.0f)
            .padding(8.0f)
            .overflow(Overflow.HIDDEN)
            .flexGrow(0.0f)
            .flexShrink(1.0f)
            .alignSelf(Align.AUTO));

Example container layout:

    panel.layout(layout -> layout
            .flexDirection(FlexDirection.ROW)
            .flexWrap(FlexWrap.WRAP)
            .gap(8.0f)
            .alignItems(Align.CENTER)
            .justifyContent(Justify.START)
            .overflow(Overflow.HIDDEN));

Compatibility helpers should remain:

- preferredSize(width, height) maps to pixel width/height.
- minSize and maxSize map to min/max constraints.
- grow(value) maps to flexGrow(value). During migration, grow greater than zero should also imply flexShrink(1).
- margin(...) maps to LayoutStyle margin.
- align(...) maps to alignSelf/cross-axis alignment.

## Core Types

Suggested package:

    dev.sixik.unigui.api.layout
      LayoutStyle
      SizeValue
      SizeUnit
      PositionType
      FlexDirection
      FlexWrap
      Overflow
      Align
      Justify
      LayoutResult
      LayoutEngine

### SizeValue

Minimum contract:

    public record SizeValue(SizeUnit unit, float value) {
        public static SizeValue auto();
        public static SizeValue px(float value);
        public static SizeValue percent(float value);
    }

Rules:

- auto means content-driven unless the parent assigns a slot.
- px is a fixed base size but can still shrink if flexShrink is greater than zero.
- percent resolves against the parent content box on the relevant axis.
- min/max always clamp the final size.

### LayoutStyle

Minimum first version:

    public final class LayoutStyle {
        PositionType position;

        SizeValue width;
        SizeValue height;
        SizeValue minWidth;
        SizeValue minHeight;
        SizeValue maxWidth;
        SizeValue maxHeight;

        EdgeInsets margin;
        EdgeInsets padding;

        Overflow overflowX;
        Overflow overflowY;

        FlexDirection flexDirection;
        FlexWrap flexWrap;
        float gap;
        float rowGap;
        float columnGap;

        float flexGrow;
        float flexShrink;
        SizeValue flexBasis;

        Align alignItems;
        Align alignSelf;
        Justify justifyContent;

        float left;
        float top;
        float right;
        float bottom;
    }

## Box Model

Every widget has:

    margin box
      border box
        padding box
          content box

Initial implementation can treat border as visual-only, but layout must support padding as real inner space for containers.

Rules:

- Margin is outside the allocated child rect.
- Padding belongs to the parent/container and reduces the child layout area.
- Hit testing and clipping use the border/layout bounds.
- Child content layout uses the content box.

## Overflow Contract

Overflow values:

- VISIBLE: child rendering may escape parent bounds.
- HIDDEN: parent pushes a clip for children.
- SCROLL: a ScrollView exposes scroll offsets, clips content and always shows the axis scrollbar.
- AUTO: a ScrollView scrolls and shows the axis scrollbar only when content exceeds its viewport.

PanelWidget keeps VISIBLE by default for compatibility. Any non-VISIBLE axis enables a balanced rectangular child clip. ScrollView defaults to HIDDEN on X and AUTO on Y, so implicit content width follows the viewport and common screens do not gain accidental horizontal scrolling.

ScrollView becomes a specialized overflow host, not a separate layout philosophy:

    ScrollView = overflow-y:auto + clip + scrollbars + pointer/wheel handling

When horizontal overflow is enabled, Shift + vertical mouse wheel is routed to the X scroll offset.

## Flex Layout Rules

Initial flex algorithm can support only the subset UniGUI needs:

1. Resolve parent content box after padding.
2. Filter collapsed children.
3. Split children into relative and absolute.
4. Measure relative children with available cross-axis size.
5. Resolve each child flex basis from explicit flexBasis, width/height on main axis, measured content size, or zero for grow-only children.
6. Build flex lines if wrapping is enabled.
7. For each line, compute free space.
8. Distribute positive free space using flexGrow.
9. Distribute negative free space using flexShrink.
10. Clamp by min/max.
11. Align cross-axis with alignItems and alignSelf.
12. Position main-axis with justifyContent.
13. Arrange absolute children after relative children.

Recommended defaults:

- position: relative
- flexWrap: nowrap
- flexGrow: 0
- flexShrink: 1
- minWidth: 0
- minHeight: 0
- alignItems: stretch
- justifyContent: start

The important default is minWidth = 0. Without it, long text and fixed-width children keep forcing horizontal overflow.

## Absolute Layout Rules

Absolute children:

- Do not participate in flex sizing.
- Are arranged relative to the parent content box.
- Can use left, top, right and bottom.
- Are clamped only when the widget requests host constraints.

Overlay widgets should use absolute layout semantics:

    tooltip.layout(l -> l
            .position(PositionType.ABSOLUTE)
            .maxWidth(SizeValue.percent(100.0f))
            .overflow(Overflow.HIDDEN));

Overlay-specific widgets still need behavior:

- Tooltip: anchor placement, flip if needed, wrap text.
- Popup: anchor placement, flip/clamp if needed.
- WindowWidget: draggable absolute rect constrained by host bounds.

WindowWidget is host-constrained by default. `constrainToHost(false)` is the explicit opt-out for overlays that intentionally need to leave the host rectangle.

## Mapping Existing Widgets

### StackPanel

StackPanel can remain as a compatibility container where children get the full content box unless their alignment and size constraints say otherwise.

### VBox, HBox and LinearBox

These should become wrappers over the flex engine:

- VBox = flexDirection column.
- HBox = flexDirection row.

### WrapPanel

WrapPanel should become:

- flexDirection row.
- flexWrap wrap.

Later it can be deprecated or kept as a convenience alias.

### DockPanel

Keep DockPanel as a separate high-level shell container for now. It is useful for screen chrome and is simpler than modeling every shell as nested flex immediately.

### ScrollView

ScrollView should become an overflow host:

- Content is measured with constrained width and unconstrained height for vertical scroll.
- Implicit content width equals viewport width.
- Explicit contentSize(width, height) remains opt-in for virtual/canvas use cases.

### OverlayLayer

OverlayLayer should become:

- One normal relative content child.
- N absolute overlay children.
- Host bounds passed to overlay children for flip/clamp decisions.

## Example: TestCommands Layout

Target structure:

    DockPanel root = new DockPanel();
    root.layout(l -> l.width(percent(100)).height(percent(100)).padding(8));

    Box nav = panelBox(...);
    nav.layout(l -> l.width(px(160)).height(percent(100)).flexShrink(0).overflow(AUTO));

    ScrollView sampleViewport = new ScrollView(sampleHost);
    sampleViewport.layout(l -> l.flexGrow(1).flexShrink(1).minWidth(px(0)).overflowY(AUTO));

    WrapPanel overlayToolbar = new WrapPanel();
    overlayToolbar.layout(l -> l.flexWrap(WRAP).gap(8).minWidth(px(0)));

    TextBlock hint = new TextBlock(...);
    hint.layout(l -> l.flexGrow(1).flexShrink(1).minWidth(px(0)).maxWidth(percent(100)));

Expected behavior:

- Left nav has fixed width and vertical scroll.
- Right sample area gets the remaining width.
- Long text shrinks/wraps instead of expanding the sample area.
- Tooltips and windows stay inside the sample viewport unless explicitly configured otherwise.

## Migration Plan

### Phase 1: Add Types, No Behavior Break

- Add SizeValue, LayoutStyle and enums.
- Add WidgetBase.layoutStyle().
- Keep LayoutConstraints.
- Map old helpers into both systems.

Definition of done:

- Existing tests pass.
- No widget changes required.

### Phase 2: Shared Flex Resolver

- Add internal FlexLayoutEngine.
- Convert LinearBox to use it.
- Add tests for grow, shrink, min/max and wrap.

Definition of done:

- HBox, VBox and WrapPanel produce stable results under small widths.
- Oversized children shrink when flexShrink is greater than zero.
- Fixed controls keep size when flexShrink equals zero.

### Phase 3: Overflow and Scroll Contracts

- Formalize Overflow.
- Make clipping behavior explicit.
- Keep ScrollView as the scrollable host.
- Ensure implicit content width equals viewport width.

Definition of done:

- No accidental horizontal scroll/overflow in common demos.
- Vertical content can exceed viewport and scroll.

### Phase 4: Overlay Absolute Layout

- Convert Tooltip, Popup and WindowWidget to absolute layout semantics.
- Add host clamp/flip helpers.
- Add tests for small host sizes.

Definition of done:

- Tooltips wrap/clamp.
- Popups flip/clamp.
- Windows cannot be dragged outside their host unless explicitly allowed.

### Phase 5: Public API Cleanup

- Legacy preferredSize, minSize, maxSize, margin, align and grow remain supported convenience methods.
- Convenience methods update only their corresponding fields and preserve unrelated Layout v2 metadata.
- Advanced users use layout(...), LayoutStyle and SizeValue.
- Grouped size, min/max, padding/margin, flex and inset helpers are part of the final LayoutStyle API.
- JavaDoc and TestCommands demonstrate the final mixed compatibility/v2 contract.

## Test Matrix

Required layout tests:

- Row flex grow: 2 children split remaining width.
- Row flex shrink: oversized children shrink into available width.
- Fixed + flexible child: fixed control stays fixed, label/text shrinks.
- Wrap: children move to next line.
- Oversized wrap child: clamps to line width.
- Column layout with wrapped toolbar: content starts below measured toolbar height.
- Percent width: child width resolves from parent content box.
- Padding: child layout area excludes parent padding.
- Overflow hidden: child render is clipped.
- Scroll auto: vertical scrollbar appears only when content height exceeds viewport.
- Absolute overlay: does not affect parent desired size.
- Tooltip: long text wraps and stays inside host.
- Window: drag is clamped to host.

## Final Public API Decisions

- Normal containers keep overflow VISIBLE for compatibility; ScrollView defaults to HIDDEN on X and AUTO on Y.
- preferredSize sets only width/height and does not silently rewrite advanced overflow, padding or positioning fields.
- Legacy widgets retain their compatibility shrink behavior; advanced LayoutStyle flexShrink defaults to 1.
- Layout v2 minimum dimensions default to zero so flexible content can shrink.
- HBox, VBox, WrapPanel and DockPanel remain first-class convenience containers.
- Alignment remains the legacy per-axis enum; Align is the Layout v2 flex enum.
- DisplayMode is not exposed: Visibility.COLLAPSED is the single supported way to remove a widget from layout, render and input traversal.
- A stylesheet-like layer is a future feature and is not part of Layout v2.

## Result

Layout v2 is complete as an incremental, backwards-compatible replacement path. Existing code can continue using focused helpers, while new code can opt into percentages, flex sizing, overflow and absolute positioning through layout(...).
